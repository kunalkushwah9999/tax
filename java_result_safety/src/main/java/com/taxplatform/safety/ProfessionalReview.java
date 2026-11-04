package com.taxplatform.safety;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.taxplatform.safety.SafetyInput.*;

/** Trusted professional-workspace metadata. No field is a browser declaration or verified credential lookup. */
public final class ProfessionalReview {
    private ProfessionalReview() { }
    public enum CredentialState { VERIFIED, PENDING, UNVERIFIED }
    public enum GrantState { ACTIVE, REVOKED }
    public enum WorkKind { ROUTINE_PLANNER, STATUTORY_PROFESSIONAL_ACTION, SPECIALIST_REVIEW, UNKNOWN }
    public enum EvidenceKind { INCOME_SOURCES, INTEREST_BREAKDOWN, DEDUCTION_SUPPORT, RECONCILIATION }
    public enum EvidenceState { UNKNOWN, MISSING, SUBMITTED, REVIEWED, NOT_REQUIRED, CONFLICT }
    public enum Route {
        ACCESS_DENIED, FEATURE_DISABLED, UNAVAILABLE, STALE_CASE, RESULT_BLOCKED,
        PROFESSIONAL_ACTION_REQUIRED, SCOPE_REVIEW_REQUIRED, CLIENT_INFORMATION_REQUIRED,
        EVIDENCE_CONFLICT, EVIDENCE_REQUIRED, ELIGIBILITY_REVIEW_REQUIRED,
        DEDUCTION_REVIEW_REQUIRED, CALCULATION_PENDING, READY_FOR_REVIEW
    }

    /** Fresh assigned-case grant and snapshot-bound evidence metadata supplied by authoritative adapters. */
    public record Access(Actor assignedReviewer, CalculationKey grantedCase, CalculationKey evidenceSnapshot,
                         Instant validFrom, Instant validUntil, GrantState grantState,
                         CredentialState credentialState, boolean featureReleased,
                         WorkKind workKind, Map<EvidenceKind,EvidenceState> evidence, SignedCandidate signedCandidate) {
        public Access {
            Objects.requireNonNull(assignedReviewer); Objects.requireNonNull(grantedCase); Objects.requireNonNull(evidenceSnapshot);
            Objects.requireNonNull(validFrom); Objects.requireNonNull(validUntil); Objects.requireNonNull(grantState);
            Objects.requireNonNull(credentialState); Objects.requireNonNull(workKind); Objects.requireNonNull(signedCandidate);
            evidence=Map.copyOf(evidence);
            for(EvidenceKind kind:EvidenceKind.values()) Objects.requireNonNull(evidence.get(kind),"explicit evidence state required");
            if(!validUntil.isAfter(validFrom)) throw new IllegalArgumentException("invalid professional grant interval");
        }
        @Override public String toString() { return "ProfessionalAccess[redacted]"; }
    }
    @FunctionalInterface public interface Source {
        Optional<Access> currentFor(Actor reviewer,CalculationKey requestedCase) throws SafeResultService.ControlStateUnavailable;
    }
}
