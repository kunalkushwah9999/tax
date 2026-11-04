package com.taxplatform.safety;

import java.util.Objects;

import static com.taxplatform.safety.SafetyInput.*;
import static com.taxplatform.safety.PublicResult.*;

/** Trusted internal policy entry. Use SignedResultService for cached/untrusted candidate delivery. */
public final class SafeResultService {
    public interface ControlPlane {
        DeliveryContext loadFor(Actor actor, Candidate storedCandidate) throws ControlStateUnavailable;
        void quarantine(CalculationKey key) throws ControlStateUnavailable;
    }

    public static final class ControlStateUnavailable extends Exception {
        private static final long serialVersionUID = 1L;
        public ControlStateUnavailable() { super("control state unavailable"); }
    }

    private final ControlPlane controls;
    private final ResultSafetyPolicy policy = new ResultSafetyPolicy();

    public SafeResultService(ControlPlane controls) { this.controls = Objects.requireNonNull(controls); }

    public PublicResult deliver(Actor actor, Candidate storedCandidate) {
        Objects.requireNonNull(actor); Objects.requireNonNull(storedCandidate);
        if (!actor.tenantId().equals(storedCandidate.key().tenantId())) return new Denied();
        try {
            DeliveryContext current = controls.loadFor(actor, storedCandidate);
            if (current == null) return new Blocked(Reason.CONTROL_STATE_UNAVAILABLE);
            return deliverUsing(actor, storedCandidate, current);
        } catch (ControlStateUnavailable unavailable) {
            return new Blocked(Reason.CONTROL_STATE_UNAVAILABLE);
        }
    }

    static PublicResult authorityFailure(Actor actor, Candidate candidate, DeliveryContext context) {
        if (!actor.tenantId().equals(candidate.key().tenantId()) || !context.authorized() || context.deleted()) return new Denied();
        if (!candidate.key().sameResource(context.currentKey())) return new Blocked(Reason.CONTEXT_BINDING_INVALID);
        if (!context.restoreCaughtUp()) return new Blocked(Reason.RESTORE_NOT_CAUGHT_UP);
        if (!context.payloadIntegrityVerified()) return new Blocked(Reason.PAYLOAD_INTEGRITY_FAILED);
        return null;
    }

    PublicResult deliverUsing(Actor actor, Candidate candidate, DeliveryContext context) throws ControlStateUnavailable {
        PublicResult failure = authorityFailure(actor, candidate, context);
        if (failure != null) return failure;
        if (context.quarantined()) return new Blocked(Reason.QUARANTINED);
        // Record authentic mismatch evidence before policy/approval/freshness gates can hide it.
        if (candidate.calculations().values().stream().anyMatch(c -> c.verification() == Verification.MISMATCH)) {
            controls.quarantine(candidate.key());
            return new Blocked(Reason.VERIFIER_MISMATCH);
        }
        return policy.evaluate(actor, candidate, context);
    }
}
