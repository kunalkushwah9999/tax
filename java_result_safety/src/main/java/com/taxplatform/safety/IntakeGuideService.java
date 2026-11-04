package com.taxplatform.safety;

import java.util.Map;
import java.util.Objects;

import static com.taxplatform.safety.SafetyInput.*;
import static com.taxplatform.safety.PublicResult.*;
import static com.taxplatform.safety.GuidedResult.frozen;

/** Current authorized input preparation can precede rule approval and numerical computation. */
public final class IntakeGuideService {
    private final SafeResultService.ControlPlane controls;
    private final CandidateProofVerifier verifier;

    public IntakeGuideService(SafeResultService.ControlPlane controls, CandidateProofVerifier verifier) {
        this.controls = Objects.requireNonNull(controls);
        this.verifier = Objects.requireNonNull(verifier);
    }

    public IntakeGuide deliver(Actor actor, SignedCandidate signedDraft) {
        Objects.requireNonNull(actor); Objects.requireNonNull(signedDraft);
        Candidate draft = signedDraft.candidate();
        if (!actor.tenantId().equals(draft.key().tenantId())) return unavailable(new Denied());
        try {
            DeliveryContext context = controls.loadFor(actor, draft);
            if (context == null) return unavailable(new Blocked(Reason.CONTROL_STATE_UNAVAILABLE));
            PublicResult failure = SafeResultService.authorityFailure(actor, draft, context);
            if (failure != null) return unavailable(failure);
            if (!draft.key().equals(context.currentKey())) return unavailable(new Blocked(Reason.STALE_RESULT));
            if (context.quarantined()) return unavailable(new Blocked(Reason.QUARANTINED));
            CandidateProofVerifier.Status proof = verifier.verify(signedDraft);
            if (proof == CandidateProofVerifier.Status.UNAVAILABLE) return unavailable(new Blocked(Reason.CONTROL_STATE_UNAVAILABLE));
            if (proof != CandidateProofVerifier.Status.VALID) return unavailable(new Blocked(Reason.PAYLOAD_INTEGRITY_FAILED));
            // The preparation endpoint accepts an input snapshot, never a numerical result candidate.
            if (!draft.calculations().isEmpty()) {
                return new IntakeGuide("DRAFT_REQUIRED", frozen(Map.of("mode", "UNAVAILABLE",
                        "heading", "Open the current input draft",
                        "message", "Input preparation uses an input-only snapshot.",
                        "next_action", frozen(Map.of("action", "OPEN_INPUT_DRAFT", "label", "Open your input draft")),
                        "capabilities", frozen(Map.of("show_input_checklist", false, "copy_preparation_note", false,
                                "submit_return", false, "export_itr", false)))));
            }
            return new IntakeGuide("INPUT_PREPARATION_AVAILABLE", GuidedResultService.draftGuide(draft.facts(), context));
        } catch (SafeResultService.ControlStateUnavailable unavailable) {
            return unavailable(new Blocked(Reason.CONTROL_STATE_UNAVAILABLE));
        }
    }

    private static IntakeGuide unavailable(PublicResult failure) {
        return new IntakeGuide(failure instanceof Denied ? "ACCESS_DENIED" : "BLOCKED", GuidedResultService.blockedGuide(failure));
    }
}
