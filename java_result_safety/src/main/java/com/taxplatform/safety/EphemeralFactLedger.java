package com.taxplatform.safety;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static com.taxplatform.safety.FactEdits.*;
import static com.taxplatform.safety.FactEditResult.Status.*;
import static com.taxplatform.safety.SafetyInput.*;

/** Bounded single-process anonymous-session edits. No saved-case, SQL or durable acknowledgement. */
public final class EphemeralFactLedger implements SafeResultService.ControlPlane {
    public record Limits(int maxSessions, int maxRequestsPerSession, Duration sessionLifetime) {
        public Limits {
            Objects.requireNonNull(sessionLifetime);
            if(maxSessions<1 || maxSessions>10000 || maxRequestsPerSession<1 || maxRequestsPerSession>1000
                    || sessionLifetime.isNegative() || sessionLifetime.isZero()
                    || sessionLifetime.compareTo(Duration.ofHours(4))>0) throw new IllegalArgumentException("invalid session limits");
        }
    }
    public record PolicyControls(RuleState ruleState, ReferenceState referenceState, Map<Regime,Availability> availability) {
        public PolicyControls {
            Objects.requireNonNull(ruleState); Objects.requireNonNull(referenceState);
            availability=Map.copyOf(availability);
            for(Regime r:Regime.values()) Objects.requireNonNull(availability.get(r));
        }
    }
    @FunctionalInterface public interface PolicyResolver {
        PolicyControls currentFor(Actor actor, Candidate currentDraft) throws SafeResultService.ControlStateUnavailable;
    }
    @FunctionalInterface interface PublishProbe { void beforePublish() throws SafeResultService.ControlStateUnavailable; }

    private record Resource(String tenant,String profile,String id) {
        static Resource of(CalculationKey key) { return new Resource(key.tenantId(),key.profileId(),key.caseId()); }
    }
    private record Saved(String fingerprint,FactEditResult receipt) { }
    private record Event(String operation,String field,Instant time,boolean reviewsReset) {
        Map<String,Object> response(int sequence) {
            return GuidedResult.frozen(Map.of("sequence",Integer.toString(sequence),"operation",operation,
                    "field",field,"at",time.toString(),"dependent_reviews_reset",reviewsReset));
        }
    }
    private record Data(Candidate current,Map<String,Saved> receipts,List<Event> events,
                        Set<CalculationKey> knownKeys,Set<CalculationKey> quarantined) {
        private Data {
            receipts=Map.copyOf(receipts); events=List.copyOf(events);
            knownKeys=Set.copyOf(knownKeys); quarantined=Set.copyOf(quarantined);
        }
    }
    private static final class Session {
        final Actor owner;
        final Instant expiresAt;
        boolean closed;
        Data data;
        Session(Actor owner,Instant expiresAt,Candidate current) {
            this.owner=owner; this.expiresAt=expiresAt;
            data=new Data(current,Map.of(),List.of(),Set.of(current.key()),Set.of());
        }
    }

    private final Clock clock;
    private final Limits limits;
    private final PolicyResolver policies;
    private final Supplier<String> ids;
    private final PublishProbe publishProbe;
    private final ConcurrentHashMap<Resource,Session> sessions=new ConcurrentHashMap<>();
    private final Object creationLock=new Object();

    public EphemeralFactLedger(Clock clock,Limits limits,PolicyResolver policies) {
        this(clock,limits,policies,()->UUID.randomUUID().toString(),()->{});
    }
    EphemeralFactLedger(Clock clock,Limits limits,PolicyResolver policies,Supplier<String> ids,PublishProbe probe) {
        this.clock=Objects.requireNonNull(clock); this.limits=Objects.requireNonNull(limits);
        this.policies=Objects.requireNonNull(policies); this.ids=Objects.requireNonNull(ids); publishProbe=Objects.requireNonNull(probe);
    }

    /** Server-only domain operation. Initial facts and validation identity must not be browser flags. */
    public Optional<Candidate> open(Actor owner,ValidationIdentity validation,CaseFacts initialFacts) {
        Objects.requireNonNull(owner); Objects.requireNonNull(validation); Objects.requireNonNull(initialFacts);
        synchronized(creationLock) {
            purgeExpired();
            if(sessions.size()>=limits.maxSessions()) return Optional.empty();
            CalculationKey key=validation.key(owner.tenantId(),nextId(),nextId(),nextId());
            Candidate initial=new Candidate(key,initialFacts,Map.of());
            Session session=new Session(owner,clock.instant().plus(limits.sessionLifetime()),initial);
            if(sessions.putIfAbsent(Resource.of(key),session)!=null) return Optional.empty();
            return Optional.of(initial);
        }
    }

    /** Owner authorization, request replay, transition, receipt/history and current-key publication share one monitor. */
    public FactEditResult apply(Actor actor,Request request) {
        Objects.requireNonNull(actor); Objects.requireNonNull(request);
        if(!actor.tenantId().equals(request.expectedKey().tenantId())) return FactEditResult.rejected(ACCESS_DENIED);
        Resource resource=Resource.of(request.expectedKey()); Session session=sessions.get(resource);
        if(session==null) return FactEditResult.rejected(ACCESS_DENIED);
        synchronized(session) {
            if(session.closed || !session.owner.equals(actor)) return FactEditResult.rejected(ACCESS_DENIED);
            if(expired(session)) { erase(resource,session); return FactEditResult.rejected(SESSION_EXPIRED); }
            Data before=session.data;
            if(before.quarantined().contains(before.current().key())) return FactEditResult.rejected(SYSTEM_REVIEW_REQUIRED);
            if(request.command() instanceof ProposeAmount p && p.text()!=null
                    && p.text().length()>HumanMoneyParser.MAX_INPUT_CHARACTERS) {
                return FactEditResult.invalid(HumanMoneyParser.Problem.TOO_LONG);
            }
            String fingerprint=FactEditReducer.fingerprint(request);
            Saved prior=before.receipts().get(request.requestId());
            if(prior!=null) {
                if(!prior.fingerprint().equals(fingerprint)) return FactEditResult.rejected(IDEMPOTENCY_CONFLICT);
                String outcome=(String)prior.receipt().toResponse().get("outcome_snapshot_id");
                return FactEditResult.accepted(REPLAYED,outcome,false);
            }
            if(!request.expectedKey().equals(before.current().key())) return FactEditResult.rejected(STALE_INPUT);
            if(before.receipts().size()>=limits.maxRequestsPerSession()) return FactEditResult.rejected(SESSION_LIMIT_REACHED);
            FactEditReducer.Decision decision=FactEditReducer.decide(before.current().facts(),request.command());
            if(decision.rejection()!=null) return decision.rejection();
            boolean changed=!decision.facts().equals(before.current().facts());
            Candidate next=before.current();
            if(changed) {
                try {
                    CalculationKey k=before.current().key();
                    CalculationKey nextKey=new CalculationKey(k.tenantId(),k.profileId(),k.caseId(),nextId(),k.periodKey(),
                            k.ruleBundleHash(),k.engineDigest(),k.verifierDigest(),k.contractVersion(),k.settingsHash());
                    if(before.knownKeys().contains(nextKey)) return FactEditResult.rejected(CONTROL_STATE_UNAVAILABLE);
                    next=new Candidate(nextKey,decision.facts(),Map.of());
                } catch(IllegalArgumentException | NullPointerException invalidId) {
                    return FactEditResult.rejected(CONTROL_STATE_UNAVAILABLE);
                }
            }
            FactEditResult receipt=FactEditResult.accepted(changed?APPLIED:NO_CHANGE,next.key().snapshotId(),changed);
            Map<String,Saved> receipts=new HashMap<>(before.receipts()); receipts.put(request.requestId(),new Saved(fingerprint,receipt));
            List<Event> events=new ArrayList<>(before.events());
            if(changed) events.add(new Event(decision.operation(),decision.field(),clock.instant(),
                    request.command() instanceof ProposeAmount || request.command() instanceof ConfirmAmount
                            || request.command() instanceof ResolveConflict || request.command() instanceof ClearAmount));
            Set<CalculationKey> known=new HashSet<>(before.knownKeys()); known.add(next.key());
            Data after=new Data(next,receipts,events,known,before.quarantined());
            try { publishProbe.beforePublish(); }
            catch(SafeResultService.ControlStateUnavailable unavailable) { return FactEditResult.rejected(CONTROL_STATE_UNAVAILABLE); }
            session.data=after;
            return receipt;
        }
    }

    /** Server domain read for the trusted signer; never serialize Candidate directly to HTTP. */
    public Optional<Candidate> current(Actor actor,CalculationKey resourceKey) {
        Objects.requireNonNull(actor); Objects.requireNonNull(resourceKey);
        Session session=sessions.get(Resource.of(resourceKey));
        if(session==null || !actor.tenantId().equals(resourceKey.tenantId())) return Optional.empty();
        synchronized(session) {
            if(!allowed(actor,session,Resource.of(resourceKey))) return Optional.empty();
            if(session.data.quarantined().contains(session.data.current().key())) return Optional.empty();
            return Optional.of(session.data.current());
        }
    }
    public Optional<FactEditHistory> history(Actor actor,CalculationKey resourceKey) {
        Objects.requireNonNull(actor); Objects.requireNonNull(resourceKey);
        Session session=sessions.get(Resource.of(resourceKey));
        if(session==null || !actor.tenantId().equals(resourceKey.tenantId())) return Optional.empty();
        synchronized(session) {
            if(!allowed(actor,session,Resource.of(resourceKey))) return Optional.empty();
            List<Map<String,Object>> rows=new ArrayList<>(); int sequence=0;
            for(Event event:session.data.events()) rows.add(event.response(++sequence));
            return Optional.of(new FactEditHistory(rows));
        }
    }
    public boolean close(Actor actor,CalculationKey resourceKey) {
        Objects.requireNonNull(actor); Objects.requireNonNull(resourceKey);
        Session session=sessions.get(Resource.of(resourceKey));
        if(session==null || !actor.tenantId().equals(resourceKey.tenantId())) return false;
        synchronized(session) {
            if(!allowed(actor,session,Resource.of(resourceKey))) return false;
            erase(Resource.of(resourceKey),session); return true;
        }
    }
    /** Call from application maintenance if idle physical cleanup is required. Access expiry does not depend on this call. */
    public void purgeExpired() {
        for(Map.Entry<Resource,Session> entry:sessions.entrySet()) {
            Session session=entry.getValue();
            if(clock.instant().isBefore(session.expiresAt)) continue;
            synchronized(session) { if(session.closed || expired(session)) erase(entry.getKey(),session); }
        }
    }
    private boolean allowed(Actor actor,Session session,Resource resource) {
        if(session.closed || !session.owner.equals(actor)) return false;
        if(expired(session)) { erase(resource,session); return false; }
        return true;
    }
    private boolean expired(Session session) { return !clock.instant().isBefore(session.expiresAt); }
    private void erase(Resource resource,Session session) { session.closed=true; session.data=null; sessions.remove(resource,session); }
    private String nextId() { String id=ids.get(); CalculationKey.requireId(id); return id; }

    @Override public DeliveryContext loadFor(Actor actor,Candidate storedCandidate) throws SafeResultService.ControlStateUnavailable {
        Objects.requireNonNull(actor); Objects.requireNonNull(storedCandidate);
        Resource resource=Resource.of(storedCandidate.key()); Session session=sessions.get(resource);
        if(session==null || !actor.tenantId().equals(storedCandidate.key().tenantId())) return deniedContext(storedCandidate.key());
        Candidate observed;
        synchronized(session) {
            if(!allowed(actor,session,resource)) return deniedContext(storedCandidate.key());
            observed=session.data.current();
        }
        // Do not hold an edit/close monitor across an external policy lookup.
        PolicyControls controls=policies.currentFor(actor,observed);
        if(controls==null) throw new SafeResultService.ControlStateUnavailable();
        synchronized(session) {
            if(!allowed(actor,session,resource)) return deniedContext(storedCandidate.key());
            if(!observed.key().equals(session.data.current().key())) throw new SafeResultService.ControlStateUnavailable();
            // Fresh in-memory objects have no restore/import path. Signature verification remains a separate gate.
            return new DeliveryContext(session.data.current().key(),true,false,true,true,controls.ruleState(),controls.referenceState(),
                    session.data.quarantined().contains(storedCandidate.key()),controls.availability());
        }
    }
    @Override public void quarantine(CalculationKey key) throws SafeResultService.ControlStateUnavailable {
        Objects.requireNonNull(key); Resource resource=Resource.of(key); Session session=sessions.get(resource);
        if(session==null) throw new SafeResultService.ControlStateUnavailable();
        synchronized(session) {
            if(session.closed || expired(session)) throw new SafeResultService.ControlStateUnavailable();
            Data data=session.data;
            if(!data.knownKeys().contains(key)) throw new SafeResultService.ControlStateUnavailable();
            Set<CalculationKey> quarantine=new HashSet<>(data.quarantined()); quarantine.add(key);
            session.data=new Data(data.current(),data.receipts(),data.events(),data.knownKeys(),quarantine);
        }
    }
    private static DeliveryContext deniedContext(CalculationKey suppliedKey) {
        return new DeliveryContext(suppliedKey,false,true,false,false,RuleState.MISSING,ReferenceState.PENDING,false,
                Map.of(Regime.NEW,Availability.UNKNOWN,Regime.OLD,Availability.UNKNOWN));
    }
}
