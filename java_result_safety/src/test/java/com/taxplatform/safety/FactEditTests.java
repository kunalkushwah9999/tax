package com.taxplatform.safety;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.taxplatform.safety.FactEdits.*;
import static com.taxplatform.safety.FactEditResult.Status.*;
import static com.taxplatform.safety.SafetyInput.*;
import static com.taxplatform.safety.ProofFixtures.*;

/** Single-process edit, authorization and real-thread tests. No SQL or tax-law validation. */
public final class FactEditTests {
    private static int passed,failed,transitions,raceRounds,raceRequests;
    private static final Actor OWNER=new Actor("tenantA","ownerA");
    private FactEditTests() { }
    @FunctionalInterface private interface Test { void run() throws Exception; }
    private static void test(String name,Test action) {
        try { action.run(); passed++; System.out.println("PASS "+name); }
        catch(Exception | AssertionError error) { failed++; System.out.println("FAIL "+name+": "+error.getClass().getSimpleName()); }
    }
    private static void check(boolean value) { if(!value)throw new AssertionError(); }
    private static void status(FactEditResult result,FactEditResult.Status expected) { check(result.status()==expected); }
    private static ValidationIdentity validation() {
        CalculationKey k=key(); return new ValidationIdentity(k.periodKey(),k.ruleBundleHash(),k.engineDigest(),k.verifierDigest(),k.contractVersion(),k.settingsHash());
    }
    private static EphemeralFactLedger.PolicyControls approved() {
        return new EphemeralFactLedger.PolicyControls(RuleState.EFFECTIVE,ReferenceState.APPROVED,
                Map.of(Regime.NEW,Availability.ALLOWED,Regime.OLD,Availability.ALLOWED));
    }
    private static final class MutableClock extends Clock {
        final AtomicReference<Instant> now=new AtomicReference<>(NOW);
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { if(!zone.equals(ZoneOffset.UTC))throw new UnsupportedOperationException(); return this; }
        @Override public Instant instant() { return now.get(); }
    }
    private static final class Fixture {
        final MutableClock clock=new MutableClock();
        final AtomicInteger ids=new AtomicInteger();
        final AtomicBoolean failPublish=new AtomicBoolean();
        final EphemeralFactLedger ledger;
        final Candidate initial;
        Fixture() { this(candidate().facts(),100,actorPolicy()); }
        Fixture(CaseFacts facts) { this(facts,100,actorPolicy()); }
        Fixture(CaseFacts facts,int requestLimit,EphemeralFactLedger.PolicyResolver policies) {
            ledger=new EphemeralFactLedger(clock,new EphemeralFactLedger.Limits(100,requestLimit,Duration.ofMinutes(30)),policies,
                    ()->"edit-id-"+ids.incrementAndGet(),()->{if(failPublish.get())throw new SafeResultService.ControlStateUnavailable();});
            initial=ledger.open(OWNER,validation(),facts).orElseThrow();
        }
        static EphemeralFactLedger.PolicyResolver actorPolicy() { return (actor,draft)->approved(); }
        Candidate current() { return ledger.current(OWNER,initial.key()).orElseThrow(); }
        FactEditResult apply(String id,Command command) { return apply(id,current().key(),command); }
        FactEditResult apply(String id,CalculationKey expected,Command command) { return ledger.apply(OWNER,new Request(id,expected,command)); }
        int events() { return ((List<?>)ledger.history(OWNER,initial.key()).orElseThrow().toResponse().get("events")).size(); }
    }
    private static CaseFacts withAmount(CaseFacts f,int index,Fact<Money> value) {
        List<Fact<Money>> amounts=new ArrayList<>(List.of(f.salary(),f.savingsInterest(),f.qualifyingDepositInterest(),f.otherInterest())); amounts.set(index,value);
        return new CaseFacts(f.scope(),amounts.get(0),amounts.get(1),amounts.get(2),amounts.get(3),f.incomeInventoryComplete(),f.deductionInventoryComplete());
    }
    private static Fact<Money> amount(CaseFacts f,int index) {
        return List.of(f.salary(),f.savingsInterest(),f.qualifyingDepositInterest(),f.otherInterest()).get(index);
    }
    private static void reset(CaseFacts f) {
        check(f.incomeInventoryComplete().state()==Fact.State.UNKNOWN);
        for(Regime r:Regime.values())check(f.deductionInventoryComplete().get(r).state()==Fact.State.UNKNOWN);
    }
    private static void noPrivateValues(String text) {
        for(String privateValue:List.of("98765.43","100000.00","420.00","700.00","ownerA","tenantA","private-raw-input")) check(!text.contains(privateValue));
    }
    private static Candidate computed(Candidate draft) { return new Candidate(draft.key(),draft.facts(),candidate().calculations()); }
    private static void poolClose(ExecutorService pool) throws InterruptedException {
        pool.shutdownNow(); check(pool.awaitTermination(5,TimeUnit.SECONDS));
    }

    public static void main(String[] args) throws Exception {
        if(args.length==2 && args[0].equals("--examples-only")) { examples(Path.of(args[1])); return; }
        test("opening a session creates a fresh input-only resource without changing reviewed scope",()->{
            Fixture f=new Fixture(); check(f.initial.calculations().isEmpty()); check(f.initial.facts().equals(candidate().facts()));
            Candidate second=f.ledger.open(OWNER,validation(),candidate().facts()).orElseThrow();
            check(!second.key().sameResource(f.initial.key()) && !second.key().snapshotId().equals(f.initial.key().snapshotId()));
        });
        test("a human amount proposal remains estimated and invalidates all dependent reviews",()->{
            Fixture f=new Fixture(); status(f.apply("proposal",new ProposeAmount(AmountField.SALARY,"1 lakh",null)),APPLIED);
            check(f.current().facts().salary().state()==Fact.State.ESTIMATED); check(f.current().facts().salary().confirmedValue().isEmpty());
            reset(f.current().facts()); check(f.events()==1 && f.current().calculations().isEmpty());
        });
        test("explicit confirmation binds the displayed amount and advances snapshot identity",()->{
            Fixture f=new Fixture(); f.apply("proposal",new ProposeAmount(AmountField.SALARY,"1 lakh",null)); Candidate proposed=f.current();
            status(f.apply("confirm",new ConfirmAmount(AmountField.SALARY,Money.parse("100000"))),APPLIED);
            check(f.current().facts().salary().confirmedValue().orElseThrow().equals(Money.parse("100000")));
            check(!proposed.key().equals(f.current().key())); check(proposed.facts().salary().state()==Fact.State.ESTIMATED); reset(f.current().facts());
        });
        test("confirmation cannot accept a different displayed amount",()->{
            Fixture f=new Fixture(); f.apply("proposal",new ProposeAmount(AmountField.SALARY,"1 lakh",null)); Candidate before=f.current();
            status(f.apply("confirm",new ConfirmAmount(AmountField.SALARY,Money.parse("200000"))),CONFIRMATION_VALUE_CHANGED);
            check(f.current().equals(before) && f.events()==1);
        });
        test("unknown and already-confirmed amounts cannot skip the proposal review state",()->{
            Fixture unknown=new Fixture(withAmount(candidate().facts(),0,Fact.unknown()));
            status(unknown.apply("confirm",new ConfirmAmount(AmountField.SALARY,Money.parse("0"))),REVIEW_STATE_CHANGED);
            Fixture confirmed=new Fixture(); status(confirmed.apply("confirm",new ConfirmAmount(AmountField.SALARY,Money.parse("98765.43"))),REVIEW_STATE_CHANGED);
        });
        test("conflict resolution accepts only an offered value with explicit user intent",()->{
            Fixture f=new Fixture(withAmount(candidate().facts(),1,Fact.conflict(List.of(Money.parse("100"),Money.parse("200")))));
            status(f.apply("bad-choice",new ResolveConflict(AmountField.SAVINGS_INTEREST,Money.parse("300"))),CONFLICT_VALUE_NOT_PRESENT);
            check(f.events()==0); status(f.apply("choice",new ResolveConflict(AmountField.SAVINGS_INTEREST,Money.parse("200"))),APPLIED);
            check(f.current().facts().savingsInterest().confirmedValue().orElseThrow().equals(Money.parse("200"))); reset(f.current().facts());
        });
        test("clearing confirmed zero creates unknown rather than another numeric zero",()->{
            Fixture f=new Fixture(); status(f.apply("clear",new ClearAmount(AmountField.SAVINGS_INTEREST)),APPLIED);
            check(f.current().facts().savingsInterest().state()==Fact.State.UNKNOWN); reset(f.current().facts());
        });
        test("blank input is rejected without clearing or replacing an existing amount",()->{
            Fixture f=new Fixture(); Candidate before=f.current(); status(f.apply("blank",new ProposeAmount(AmountField.SALARY," ",HumanMoneyParser.Unit.RUPEES)),INVALID_AMOUNT);
            check(f.current().equals(before) && f.events()==0);
        });
        test("null proposal text produces a missing-input error without a numeric default",()->{
            Fixture f=new Fixture();FactEditResult result=f.apply("null",new ProposeAmount(AmountField.SALARY,null,HumanMoneyParser.Unit.RUPEES));
            status(result,INVALID_AMOUNT);check(result.toResponse().get("input_problem").equals("MISSING"));
            check(f.current().equals(f.initial) && f.events()==0);
        });
        test("malformed ambiguous and oversized proposals have no state or history side effects",()->{
            Fixture f=new Fixture(); int index=0;
            for(String input:List.of("1","-1","1e5","1.005 rupees","1".repeat(65),"private-raw-input")) {
                status(f.apply("bad-"+index++,new ProposeAmount(AmountField.SALARY,input,null)),INVALID_AMOUNT);
                check(f.current().equals(f.initial) && f.events()==0);
            }
        });
        test("a stale tab cannot overwrite a newer input snapshot",()->{
            Fixture f=new Fixture(); f.apply("first",new ProposeAmount(AmountField.SALARY,"1 lakh",null)); Candidate before=f.current();
            status(f.apply("stale",f.initial.key(),new ProposeAmount(AmountField.SALARY,"2 lakh",null)),STALE_INPUT);
            check(f.current().equals(before) && f.events()==1);
        });
        test("a changed validation identity is rejected even if the resource and snapshot IDs match",()->{
            Fixture f=new Fixture();CalculationKey k=f.initial.key();
            CalculationKey changed=new CalculationKey(k.tenantId(),k.profileId(),k.caseId(),k.snapshotId(),k.periodKey(),
                    k.ruleBundleHash(),"e".repeat(64),k.verifierDigest(),k.contractVersion(),k.settingsHash());
            status(f.apply("changed-engine",changed,new ClearAmount(AmountField.SALARY)),STALE_INPUT);check(f.current().equals(f.initial));
        });
        test("same request retry returns one historical receipt without a second transition",()->{
            Fixture f=new Fixture(); Request r=new Request("once",f.initial.key(),new ProposeAmount(AmountField.SALARY,"1 lakh",null));
            status(f.ledger.apply(OWNER,r),APPLIED); CalculationKey after=f.current().key();
            FactEditResult replay=f.ledger.apply(OWNER,r); status(replay,REPLAYED);
            check(f.events()==1 && f.current().key().equals(after)); check(Boolean.FALSE.equals(replay.toResponse().get("state_changed")));
        });
        test("request ID reuse with changed body operation unit field or expected identity is rejected",()->{
            Fixture f=new Fixture(); f.apply("bound",new ProposeAmount(AmountField.SALARY,"1 lakh",null));
            for(Command c:List.of(new ProposeAmount(AmountField.SALARY,"2 lakh",null),new ClearAmount(AmountField.SALARY),
                    new ProposeAmount(AmountField.SAVINGS_INTEREST,"1 lakh",null),new ProposeAmount(AmountField.SALARY,"1 lakh",HumanMoneyParser.Unit.LAKH)))
                status(f.apply("bound",f.initial.key(),c),IDEMPOTENCY_CONFLICT);
            status(f.apply("bound",new ProposeAmount(AmountField.SALARY,"1 lakh",null)),IDEMPOTENCY_CONFLICT); check(f.events()==1);
        });
        test("replay after later edits never labels the old snapshot as the current draft",()->{
            Fixture f=new Fixture(); Request first=new Request("first",f.initial.key(),new ProposeAmount(AmountField.SALARY,"1 lakh",null));
            FactEditResult receipt=f.ledger.apply(OWNER,first); f.apply("later",new ClearAmount(AmountField.SAVINGS_INTEREST)); Candidate latest=f.current();
            FactEditResult replay=f.ledger.apply(OWNER,first); status(replay,REPLAYED);
            check(replay.toResponse().get("outcome_snapshot_id").equals(receipt.toResponse().get("outcome_snapshot_id")));
            check(!latest.key().snapshotId().equals(replay.toResponse().get("outcome_snapshot_id")));
            check(Boolean.TRUE.equals(replay.toResponse().get("refresh_current_draft"))); noPrivateValues(SafeJson.encode(replay)); check(f.events()==2);
        });
        test("owner checks precede edits and replay for other principals and tenants",()->{
            Fixture f=new Fixture(); Request request=new Request("private",f.initial.key(),new ClearAmount(AmountField.SALARY)); f.ledger.apply(OWNER,request);
            for(Actor actor:List.of(new Actor("tenantA","ownerB"),new Actor("tenantB","ownerA"))) {
                status(f.ledger.apply(actor,request),ACCESS_DENIED); check(f.ledger.current(actor,f.initial.key()).isEmpty());
                check(f.ledger.history(actor,f.initial.key()).isEmpty()); check(!f.ledger.close(actor,f.initial.key()));
            }
            check(f.events()==1);
        });
        test("idempotency keys are scoped to independent cases rather than global strings",()->{
            Fixture f=new Fixture(); Candidate second=f.ledger.open(OWNER,validation(),candidate().facts()).orElseThrow();
            status(f.apply("same-id",new ClearAmount(AmountField.SALARY)),APPLIED);
            status(f.ledger.apply(OWNER,new Request("same-id",second.key(),new ClearAmount(AmountField.SALARY))),APPLIED);
        });
        test("inventory review cannot complete while income remains unconfirmed",()->{
            Fixture f=new Fixture(withAmount(candidate().facts(),0,Fact.unknown()));
            for(Inventory inventory:Inventory.values()) status(f.apply("review-"+inventory,new ReviewInventory(inventory)),INPUT_REVIEW_INCOMPLETE);
            check(f.events()==0);
        });
        test("deduction review requires a completed current income inventory",()->{
            Fixture f=new Fixture(); f.apply("unreview",new UnreviewInventory(Inventory.INCOME));
            status(f.apply("new",new ReviewInventory(Inventory.NEW_DEDUCTIONS)),INPUT_REVIEW_INCOMPLETE);
            status(f.apply("income",new ReviewInventory(Inventory.INCOME)),APPLIED);
            status(f.apply("new",new ReviewInventory(Inventory.NEW_DEDUCTIONS)),APPLIED);
        });
        test("input review never upgrades unsupported scope to supported",()->{
            CaseFacts f=candidate().facts(); Fixture x=new Fixture(new CaseFacts(Scope.PARTIAL,f.salary(),f.savingsInterest(),f.qualifyingDepositInterest(),f.otherInterest(),f.incomeInventoryComplete(),f.deductionInventoryComplete()));
            status(x.apply("review",new ReviewInventory(Inventory.INCOME)),UNSUPPORTED_SCOPE); check(x.current().facts().scope()==Scope.PARTIAL);
        });
        test("proposal confirmation and ordered reviews drive the real intake guide",()->{
            Fixture f=new Fixture(); f.apply("proposal",new ProposeAmount(AmountField.SALARY,"1 lakh",null));
            f.apply("confirm",new ConfirmAmount(AmountField.SALARY,Money.parse("100000")));
            for(Inventory inventory:Inventory.values()) status(f.apply("review-"+inventory,new ReviewInventory(inventory)),APPLIED);
            IntakeGuide guide=new IntakeGuideService(f.ledger,verifier()).deliver(OWNER,sign(f.current()));
            Map<?,?> content=(Map<?,?>)guide.toResponse().get("guide");
            check(content.get("progress_label").equals("8 of 8 input reviews complete"));
            check(((Map<?,?>)content.get("next_action")).get("action").equals("REQUEST_CALCULATION"));
            check(f.current().calculations().isEmpty());
        });
        test("unreviewing income clears both deduction reviews while old-only review changes stay separate",()->{
            Fixture f=new Fixture(); f.apply("income",new UnreviewInventory(Inventory.INCOME)); reset(f.current().facts());
            Fixture separate=new Fixture(); separate.apply("old",new UnreviewInventory(Inventory.OLD_DEDUCTIONS));
            check(separate.current().facts().incomeInventoryComplete().confirmedValue().orElseThrow());
            check(separate.current().facts().deductionInventoryComplete().get(Regime.NEW).confirmedValue().orElseThrow());
            check(separate.current().facts().deductionInventoryComplete().get(Regime.OLD).state()==Fact.State.UNKNOWN);
        });
        test("no-op reviews preserve snapshot identity yet retain safe retry binding",()->{
            Fixture f=new Fixture(); Request r=new Request("noop",f.initial.key(),new ReviewInventory(Inventory.INCOME));
            status(f.ledger.apply(OWNER,r),NO_CHANGE); check(f.current().equals(f.initial) && f.events()==0);
            status(f.ledger.apply(OWNER,r),REPLAYED); check(f.events()==0);
        });
        test("an accepted edit makes an earlier signed cached calculation stale at later delivery",()->{
            Fixture f=new Fixture(); SignedCandidate old=sign(computed(f.current())); SignedResultService results=new SignedResultService(f.ledger,verifier());
            check(results.deliver(OWNER,old) instanceof PublicResult.Calculation);
            f.apply("edit",new ProposeAmount(AmountField.SALARY,"1 lakh",null)); PublicResult result=results.deliver(OWNER,old);
            check(result.reasons().contains(PublicResult.Reason.STALE_RESULT)); check(!SafeJson.encode(result).contains("annual_taxes"));
        });
        test("closing a session revokes reads history retries and cached result delivery",()->{
            Fixture f=new Fixture(); Request r=new Request("edit",f.initial.key(),new ClearAmount(AmountField.SALARY)); f.ledger.apply(OWNER,r);
            check(f.ledger.close(OWNER,f.initial.key())); status(f.ledger.apply(OWNER,r),ACCESS_DENIED);
            check(f.ledger.current(OWNER,f.initial.key()).isEmpty() && f.ledger.history(OWNER,f.initial.key()).isEmpty());
            check(new SignedResultService(f.ledger,verifier()).deliver(OWNER,sign(computed(f.initial))).reasons().contains(PublicResult.Reason.ACCESS_DENIED));
        });
        test("expiry is exclusive at the boundary and clock rollback cannot resurrect an erased session",()->{
            Fixture f=new Fixture(); f.clock.now.set(NOW.plus(Duration.ofMinutes(30)));
            status(f.apply("expired",f.initial.key(),new ClearAmount(AmountField.SALARY)),SESSION_EXPIRED);
            f.clock.now.set(NOW); check(f.ledger.current(OWNER,f.initial.key()).isEmpty());
        });
        test("session capacity is bounded and closing frees a slot",()->{
            MutableClock clock=new MutableClock(); EphemeralFactLedger ledger=new EphemeralFactLedger(clock,
                    new EphemeralFactLedger.Limits(1,10,Duration.ofMinutes(10)),Fixture.actorPolicy());
            Candidate one=ledger.open(OWNER,validation(),candidate().facts()).orElseThrow();
            check(ledger.open(OWNER,validation(),candidate().facts()).isEmpty()); check(ledger.close(OWNER,one.key()));
            check(ledger.open(OWNER,validation(),candidate().facts()).isPresent());
        });
        test("invalid lifetime and capacity configurations are rejected",()->{
            List<Runnable> invalid=List.of(()->new EphemeralFactLedger.Limits(0,1,Duration.ofMinutes(1)),
                    ()->new EphemeralFactLedger.Limits(1,0,Duration.ofMinutes(1)),
                    ()->new EphemeralFactLedger.Limits(10001,1,Duration.ofMinutes(1)),
                    ()->new EphemeralFactLedger.Limits(1,1001,Duration.ofMinutes(1)),
                    ()->new EphemeralFactLedger.Limits(1,1,Duration.ZERO),
                    ()->new EphemeralFactLedger.Limits(1,1,Duration.ofHours(4).plusNanos(1)));
            for(Runnable build:invalid){try{build.run();throw new AssertionError();}catch(IllegalArgumentException expected){ }}
        });
        test("request capacity never evicts accepted retry receipts within a live session",()->{
            Fixture f=new Fixture(candidate().facts(),1,Fixture.actorPolicy()); Request r=new Request("first",f.initial.key(),new ClearAmount(AmountField.SALARY));
            status(f.ledger.apply(OWNER,r),APPLIED); status(f.apply("second",new ClearAmount(AmountField.SAVINGS_INTEREST)),SESSION_LIMIT_REACHED);
            status(f.ledger.apply(OWNER,r),REPLAYED); check(f.events()==1);
        });
        test("failure before publication leaves facts snapshot receipt and history unchanged",()->{
            Fixture f=new Fixture(); Request r=new Request("retry",f.initial.key(),new ProposeAmount(AmountField.SALARY,"1 lakh",null));
            f.failPublish.set(true); status(f.ledger.apply(OWNER,r),CONTROL_STATE_UNAVAILABLE); check(f.current().equals(f.initial) && f.events()==0);
            f.failPublish.set(false); status(f.ledger.apply(OWNER,r),APPLIED); status(f.ledger.apply(OWNER,r),REPLAYED); check(f.events()==1);
        });
        test("a reused snapshot ID fails closed without binding the rejected request",()->{
            Fixture f=new Fixture(); f.ids.set(2); Request r=new Request("retry",f.initial.key(),new ClearAmount(AmountField.SALARY));
            status(f.ledger.apply(OWNER,r),CONTROL_STATE_UNAVAILABLE); check(f.current().equals(f.initial) && f.events()==0);
            f.ids.set(100); status(f.ledger.apply(OWNER,r),APPLIED);
        });
        test("session history and command receipts omit amounts raw text and principals",()->{
            Fixture f=new Fixture(); FactEditResult r=f.apply("proposal",new ProposeAmount(AmountField.SALARY,"1 lakh",null));
            noPrivateValues(SafeJson.encode(r)); FactEditHistory h=f.ledger.history(OWNER,f.initial.key()).orElseThrow(); noPrivateValues(SafeJson.encode(h));
            check(((List<?>)h.toResponse().get("events")).size()==1);
            try {h.toResponse().clear();throw new AssertionError();}catch(UnsupportedOperationException expected) { }
            check(r.toString().contains("redacted") && h.toString().contains("redacted"));
        });
        test("current policy is reloaded and does not get approved by an input edit",()->{
            AtomicBoolean pending=new AtomicBoolean(); Fixture f=new Fixture(candidate().facts(),100,(actor,draft)->pending.get()
                    ?new EphemeralFactLedger.PolicyControls(RuleState.UNAPPROVED,ReferenceState.PENDING,Map.of(Regime.NEW,Availability.UNKNOWN,Regime.OLD,Availability.UNKNOWN)):approved());
            SignedResultService service=new SignedResultService(f.ledger,verifier()); SignedCandidate old=sign(computed(f.initial));
            check(service.deliver(OWNER,old) instanceof PublicResult.Calculation); pending.set(true);
            check(service.deliver(OWNER,old) instanceof PublicResult.Blocked);
            status(f.apply("edit",new ClearAmount(AmountField.SALARY)),APPLIED);
            check(f.ledger.loadFor(OWNER,f.current()).referenceState()==ReferenceState.PENDING);
        });
        test("a policy outage has no cached numerical fallback",()->{
            Fixture f=new Fixture(candidate().facts(),100,(actor,draft)->{throw new SafeResultService.ControlStateUnavailable();});
            PublicResult r=new SignedResultService(f.ledger,verifier()).deliver(OWNER,sign(computed(f.initial)));
            check(r.reasons().contains(PublicResult.Reason.CONTROL_STATE_UNAVAILABLE)); check(!SafeJson.encode(r).contains("annual_taxes"));
        });
        test("policy lookup does not hold an edit lock and a changed draft invalidates its old context",()->{
            CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1); ExecutorService pool=Executors.newFixedThreadPool(2);
            try {
                Fixture f=new Fixture(candidate().facts(),100,(actor,draft)->{entered.countDown();try{if(!release.await(5,TimeUnit.SECONDS))throw new SafeResultService.ControlStateUnavailable();}
                    catch(InterruptedException e){Thread.currentThread().interrupt();throw new SafeResultService.ControlStateUnavailable();}return approved();});
                Future<PublicResult> reading=pool.submit(()->new SignedResultService(f.ledger,verifier()).deliver(OWNER,sign(computed(f.initial))));
                check(entered.await(5,TimeUnit.SECONDS)); Future<FactEditResult> editing=pool.submit(()->f.apply("edit",new ClearAmount(AmountField.SALARY)));
                status(editing.get(3,TimeUnit.SECONDS),APPLIED); release.countDown();
                check(reading.get(5,TimeUnit.SECONDS).reasons().contains(PublicResult.Reason.CONTROL_STATE_UNAVAILABLE));
            } finally {release.countDown();poolClose(pool);}
        });
        test("closing during an external policy lookup is rechecked before result delivery",()->{
            CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1); ExecutorService pool=Executors.newFixedThreadPool(2);
            try {
                Fixture f=new Fixture(candidate().facts(),100,(actor,draft)->{entered.countDown();try{if(!release.await(5,TimeUnit.SECONDS))throw new SafeResultService.ControlStateUnavailable();}
                    catch(InterruptedException e){Thread.currentThread().interrupt();throw new SafeResultService.ControlStateUnavailable();}return approved();});
                Future<PublicResult> reading=pool.submit(()->new SignedResultService(f.ledger,verifier()).deliver(OWNER,sign(computed(f.initial))));
                check(entered.await(5,TimeUnit.SECONDS)); check(pool.submit(()->f.ledger.close(OWNER,f.initial.key())).get(3,TimeUnit.SECONDS));
                release.countDown(); check(reading.get(5,TimeUnit.SECONDS).reasons().contains(PublicResult.Reason.ACCESS_DENIED));
            } finally {release.countDown();poolClose(pool);}
        });
        test("quarantine cannot be cleared by editing retrying or reading a current draft",()->{
            Fixture f=new Fixture(); f.ledger.quarantine(f.initial.key());
            status(f.apply("edit",f.initial.key(),new ClearAmount(AmountField.SALARY)),SYSTEM_REVIEW_REQUIRED);
            check(f.ledger.current(OWNER,f.initial.key()).isEmpty());
            check(new SignedResultService(f.ledger,verifier()).deliver(OWNER,sign(computed(f.initial))).reasons().contains(PublicResult.Reason.QUARANTINED));
        });
        test("unknown quarantine identities are rejected and old identities stay separate",()->{
            Fixture f=new Fixture(); try {f.ledger.quarantine(key());throw new AssertionError();}catch(SafeResultService.ControlStateUnavailable expected) { }
            f.apply("edit",new ClearAmount(AmountField.SALARY)); Candidate current=f.current(); f.ledger.quarantine(f.initial.key());
            check(f.current().equals(current)); check(f.ledger.loadFor(OWNER,f.initial).quarantined()); check(!f.ledger.loadFor(OWNER,current).quarantined());
            status(f.apply("next",new ClearAmount(AmountField.SAVINGS_INTEREST)),APPLIED);
        });
        test("a new process ledger has no saved session or durable replay receipt",()->{
            Fixture f=new Fixture(); Request r=new Request("edit",f.initial.key(),new ClearAmount(AmountField.SALARY)); f.ledger.apply(OWNER,r);
            EphemeralFactLedger restarted=new EphemeralFactLedger(f.clock,new EphemeralFactLedger.Limits(10,10,Duration.ofMinutes(10)),Fixture.actorPolicy());
            status(restarted.apply(OWNER,r),ACCESS_DENIED); check(restarted.current(OWNER,f.initial.key()).isEmpty());
        });
        test("generated field state and magnitude proposals keep estimates and dependent review resets",()->{
            List<Fact<Money>> states=List.of(Fact.confirmed(Money.parse("123")),Fact.<Money>unknown(),Fact.estimated(Money.parse("234")),Fact.conflict(List.of(Money.parse("345"),Money.parse("456"))));
            String[] expected={"1.00","1000.00","100000.00","10000000.00"};
            for(int field=0;field<4;field++)for(Fact<Money> state:states)for(HumanMoneyParser.Unit unit:HumanMoneyParser.Unit.values()) {
                Fixture f=new Fixture(withAmount(candidate().facts(),field,state));
                status(f.apply("edit",new ProposeAmount(AmountField.values()[field],"1",unit)),APPLIED);
                Fact<Money> actual=amount(f.current().facts(),field); check(actual instanceof Fact.Estimated<?> e && ((Money)e.value()).decimal().equals(expected[unit.ordinal()]));
                reset(f.current().facts()); transitions++;
            }
        });
        test("32 real-thread competing edit rounds accept exactly one writer per expected snapshot",()->{
            ExecutorService pool=Executors.newFixedThreadPool(2);
            try {for(int round=0;round<32;round++) {
                Fixture f=new Fixture(); CountDownLatch ready=new CountDownLatch(2),go=new CountDownLatch(1); List<Future<FactEditResult>> futures=new ArrayList<>();
                for(int n=1;n<=2;n++){final int choice=n;futures.add(pool.submit(()->{ready.countDown();check(go.await(5,TimeUnit.SECONDS));return f.apply("writer-"+choice,f.initial.key(),new ProposeAmount(AmountField.SALARY,choice+" lakh",null));}));}
                check(ready.await(5,TimeUnit.SECONDS));go.countDown(); List<FactEditResult.Status> outcomes=new ArrayList<>();
                for(Future<FactEditResult> future:futures)outcomes.add(future.get(5,TimeUnit.SECONDS).status());
                check(outcomes.stream().filter(s->s==APPLIED).count()==1 && outcomes.stream().filter(s->s==STALE_INPUT).count()==1);
                check(f.events()==1);reset(f.current().facts());raceRounds++;raceRequests+=2;
            }} finally {poolClose(pool);}
        });
        test("16 real-thread eight-way retry rounds publish one edit and seven replay receipts",()->{
            ExecutorService pool=Executors.newFixedThreadPool(8);
            try {for(int round=0;round<16;round++) {
                Fixture f=new Fixture(); Request r=new Request("duplicate",f.initial.key(),new ProposeAmount(AmountField.SALARY,"1 lakh",null));
                CountDownLatch ready=new CountDownLatch(8),go=new CountDownLatch(1);List<Future<FactEditResult>> futures=new ArrayList<>();
                for(int n=0;n<8;n++)futures.add(pool.submit(()->{ready.countDown();check(go.await(5,TimeUnit.SECONDS));return f.ledger.apply(OWNER,r);}));
                check(ready.await(5,TimeUnit.SECONDS));go.countDown();List<FactEditResult.Status> outcomes=new ArrayList<>();
                for(Future<FactEditResult> future:futures)outcomes.add(future.get(5,TimeUnit.SECONDS).status());
                check(outcomes.stream().filter(s->s==APPLIED).count()==1 && outcomes.stream().filter(s->s==REPLAYED).count()==7);
                check(f.events()==1);raceRounds++;raceRequests+=8;
            }} finally {poolClose(pool);}
        });
        if(args.length==1)examples(Path.of(args[0]));
        System.out.println("SCENARIOS_PASSED="+passed);System.out.println("SCENARIOS_FAILED="+failed);
        System.out.println("GENERATED_EDIT_TRANSITIONS="+transitions);System.out.println("CONCURRENT_LEDGER_ROUNDS="+raceRounds);
        System.out.println("CONCURRENT_LEDGER_REQUESTS="+raceRequests);
        if(failed!=0)throw new AssertionError("fact edit checks failed");
    }

    private static void examples(Path path) throws java.io.IOException {
        Fixture f=new Fixture();Request request=new Request("first",f.initial.key(),new ProposeAmount(AmountField.SALARY,"1 lakh",null));
        List<String> samples=new ArrayList<>();
        samples.add("{\"name\":\"proposal_accepted\",\"response\":"+SafeJson.encode(f.ledger.apply(OWNER,request))+"}");
        samples.add("{\"name\":\"safe_retry\",\"response\":"+SafeJson.encode(f.ledger.apply(OWNER,request))+"}");
        samples.add("{\"name\":\"stale_tab\",\"response\":"+SafeJson.encode(f.apply("stale",f.initial.key(),new ClearAmount(AmountField.SALARY)))+"}");
        samples.add("{\"name\":\"wrong_confirmation\",\"response\":"+SafeJson.encode(f.apply("confirm",new ConfirmAmount(AmountField.SALARY,Money.parse("200000"))))+"}");
        samples.add("{\"name\":\"history_without_values\",\"response\":"+SafeJson.encode(f.ledger.history(OWNER,f.initial.key()).orElseThrow())+"}");
        Files.writeString(path,"["+String.join(",",samples)+"]\n",StandardCharsets.UTF_8);
    }
}
