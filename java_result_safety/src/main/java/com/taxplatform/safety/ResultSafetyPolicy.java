package com.taxplatform.safety;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import static com.taxplatform.safety.SafetyInput.*;
import static com.taxplatform.safety.PublicResult.*;

/** A pure decision gate; it neither computes tax nor verifies a legal approval signature. */
public final class ResultSafetyPolicy {
    public PublicResult evaluate(Actor actor, Candidate candidate, DeliveryContext context) {
        Objects.requireNonNull(actor); Objects.requireNonNull(candidate); Objects.requireNonNull(context);
        if (!actor.tenantId().equals(candidate.key().tenantId())
                || !context.authorized() || context.deleted()) return new Denied();
        if (!candidate.key().sameResource(context.currentKey())) return new Blocked(Reason.CONTEXT_BINDING_INVALID);
        if (!context.restoreCaughtUp()) return new Blocked(Reason.RESTORE_NOT_CAUGHT_UP);
        if (!context.payloadIntegrityVerified()) return new Blocked(Reason.PAYLOAD_INTEGRITY_FAILED);
        if (!candidate.key().equals(context.currentKey())) return new Blocked(Reason.STALE_RESULT);
        if (context.quarantined()) return new Blocked(Reason.QUARANTINED);
        if (context.ruleState() != RuleState.EFFECTIVE) return new Blocked(Reason.RULE_NOT_EFFECTIVE);
        if (context.referenceState() != ReferenceState.APPROVED) return new Blocked(Reason.INDEPENDENT_REFERENCE_NOT_APPROVED);
        if (candidate.calculations().isEmpty()) return new Blocked(Reason.NO_CALCULATIONS);
        if (candidate.calculations().values().stream().anyMatch(c -> c.verification() == Verification.MISMATCH)) {
            return new Blocked(Reason.VERIFIER_MISMATCH);
        }
        if (candidate.calculations().values().stream().anyMatch(c -> c.verification() != Verification.MATCH)) {
            return new Blocked(Reason.VERIFICATION_NOT_RUN);
        }

        CaseFacts facts = candidate.facts();
        if (facts.scope() != Scope.SUPPORTED) {
            return new ActionRequired(List.of(facts.scope() == Scope.UNKNOWN
                    ? Reason.SCOPE_UNKNOWN : Reason.UNSUPPORTED_SCOPE));
        }
        EnumSet<Reason> issues = EnumSet.noneOf(Reason.class);
        for (Fact<Money> amount : List.of(facts.salary(), facts.savingsInterest(),
                facts.qualifyingDepositInterest(), facts.otherInterest())) {
            switch (amount.state()) {
                case UNKNOWN -> issues.add(Reason.UNKNOWN_INCOME);
                case ESTIMATED -> issues.add(Reason.ESTIMATED_INCOME);
                case CONFLICT -> issues.add(Reason.CONFLICTING_INCOME);
                case CONFIRMED -> { }
            }
        }
        if (!facts.incomeInventoryComplete().confirmedValue().orElse(false)) {
            issues.add(Reason.INCOME_INVENTORY_INCOMPLETE);
        }
        if (!issues.isEmpty()) return new ActionRequired(List.copyOf(issues));

        List<AnnualTax> visible = new ArrayList<>();
        for (Regime regime : Regime.values()) {
            boolean complete = facts.deductionInventoryComplete().get(regime).confirmedValue().orElse(false);
            Availability availability = context.regimeAvailability().get(regime);
            RegimeCalculation calculation = candidate.calculations().get(regime);
            if (!complete) issues.add(Reason.DEDUCTIONS_INCOMPLETE);
            if (availability == Availability.UNKNOWN) issues.add(Reason.REGIME_AVAILABILITY_UNKNOWN);
            if (availability == Availability.NOT_ALLOWED) issues.add(Reason.REGIME_NOT_AVAILABLE);
            if (calculation == null && availability == Availability.ALLOWED) {
                issues.add(Reason.COMPARISON_CALCULATION_MISSING);
            }
            if (complete && availability == Availability.ALLOWED && calculation != null) {
                visible.add(new AnnualTax(regime, calculation.annualTaxBeforeCredits()));
            }
        }
        if (visible.isEmpty()) return new ActionRequired(List.copyOf(issues));
        if (visible.size() == Regime.values().length) {
            Money newTax = candidate.calculations().get(Regime.NEW).annualTaxBeforeCredits();
            Money oldTax = candidate.calculations().get(Regime.OLD).annualTaxBeforeCredits();
            int order = newTax.compareTo(oldTax);
            Recommendation recommendation = order < 0 ? Recommendation.NEW
                    : order > 0 ? Recommendation.OLD : Recommendation.TIE;
            return new Calculation(visible, ComparisonStatus.DEFINITIVE,
                    new Comparison(recommendation, newTax.absoluteDifference(oldTax)), List.of());
        }
        ComparisonStatus status = context.regimeAvailability().containsValue(Availability.NOT_ALLOWED)
                ? ComparisonStatus.NOT_AVAILABLE : ComparisonStatus.PROVISIONAL;
        return new Calculation(visible, status, null, List.copyOf(issues));
    }
}
