package com.taxplatform.safety;

import java.util.Objects;

import static com.taxplatform.safety.SafetyInput.*;
import static com.taxplatform.safety.PublicResult.*;

/** Required signed-payload entry point for cached/untrusted candidate delivery. */
public final class SignedResultService {
    private final SafeResultService.ControlPlane controls;
    private final CandidateProofVerifier proofVerifier;
    private final SafeResultService trustedService;

    public SignedResultService(SafeResultService.ControlPlane controls, CandidateProofVerifier proofVerifier) {
        this.controls = Objects.requireNonNull(controls);
        this.proofVerifier = Objects.requireNonNull(proofVerifier);
        this.trustedService = new SafeResultService(controls);
    }

    public PublicResult deliver(Actor actor, SignedCandidate signed) {
        Objects.requireNonNull(actor); Objects.requireNonNull(signed);
        Candidate candidate = signed.candidate();
        if (!actor.tenantId().equals(candidate.key().tenantId())) return new Denied();
        try {
            DeliveryContext context = controls.loadFor(actor, candidate);
            if (context == null) return new Blocked(Reason.CONTROL_STATE_UNAVAILABLE);
            PublicResult failure = SafeResultService.authorityFailure(actor, candidate, context);
            if (failure != null) return failure;
            CandidateProofVerifier.Status proof = proofVerifier.verify(signed);
            if (proof == CandidateProofVerifier.Status.UNAVAILABLE) return new Blocked(Reason.CONTROL_STATE_UNAVAILABLE);
            if (proof != CandidateProofVerifier.Status.VALID) return new Blocked(Reason.PAYLOAD_INTEGRITY_FAILED);
            return trustedService.deliverUsing(actor, candidate, context);
        } catch (SafeResultService.ControlStateUnavailable unavailable) {
            return new Blocked(Reason.CONTROL_STATE_UNAVAILABLE);
        }
    }
}
