package com.taxplatform.safety;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static com.taxplatform.safety.SafetyInput.Regime;

/** Explicit API projection. Constructors are package-private; raw candidates are never projected. */
public sealed interface PublicResult permits PublicResult.Denied, PublicResult.Blocked,
        PublicResult.ActionRequired, PublicResult.Calculation {
    String CONTRACT_VERSION = "result-safety.v1";

    enum Reason {
        ACCESS_DENIED, CONTROL_STATE_UNAVAILABLE, CONTEXT_BINDING_INVALID,
        RESTORE_NOT_CAUGHT_UP, PAYLOAD_INTEGRITY_FAILED, STALE_RESULT,
        QUARANTINED, RULE_NOT_EFFECTIVE, INDEPENDENT_REFERENCE_NOT_APPROVED,
        VERIFIER_MISMATCH, VERIFICATION_NOT_RUN, UNSUPPORTED_SCOPE,
        SCOPE_UNKNOWN, UNKNOWN_INCOME, ESTIMATED_INCOME, CONFLICTING_INCOME,
        INCOME_INVENTORY_INCOMPLETE, NO_CALCULATIONS,
        DEDUCTIONS_INCOMPLETE, REGIME_AVAILABILITY_UNKNOWN,
        REGIME_NOT_AVAILABLE, COMPARISON_CALCULATION_MISSING
    }

    enum ComparisonStatus { DEFINITIVE, PROVISIONAL, NOT_AVAILABLE }
    enum Recommendation { NEW, OLD, TIE }

    Map<String, Object> toResponse();
    List<Reason> reasons();

    final class Denied implements PublicResult {
        Denied() { }
        @Override public List<Reason> reasons() { return List.of(Reason.ACCESS_DENIED); }
        @Override public Map<String, Object> toResponse() {
            return immutable(base("ACCESS_DENIED", reasons(), false, false));
        }
    }

    final class Blocked implements PublicResult {
        private final Reason reason;
        Blocked(Reason reason) { this.reason = Objects.requireNonNull(reason); }
        @Override public List<Reason> reasons() { return List.of(reason); }
        @Override public Map<String, Object> toResponse() {
            return immutable(base("BLOCKED", reasons(), false, false));
        }
    }

    final class ActionRequired implements PublicResult {
        private final List<Reason> reasons;
        ActionRequired(List<Reason> reasons) {
            this.reasons = List.copyOf(reasons);
            if (this.reasons.isEmpty()) throw new IllegalArgumentException("action reason required");
        }
        @Override public List<Reason> reasons() { return reasons; }
        @Override public Map<String, Object> toResponse() {
            return immutable(base("INFORMATION_REQUIRED", reasons, false, false));
        }
    }

    record AnnualTax(Regime regime, Money amount) {
        public AnnualTax { Objects.requireNonNull(regime); Objects.requireNonNull(amount); }
    }

    record Comparison(Recommendation recommendation, Money difference) {
        public Comparison { Objects.requireNonNull(recommendation); Objects.requireNonNull(difference); }
    }

    final class Calculation implements PublicResult {
        private final List<AnnualTax> annualTaxes;
        private final ComparisonStatus comparisonStatus;
        private final Comparison comparison;
        private final List<Reason> reasons;

        Calculation(List<AnnualTax> annualTaxes, ComparisonStatus status,
                    Comparison comparison, List<Reason> reasons) {
            this.annualTaxes = List.copyOf(annualTaxes);
            this.comparisonStatus = Objects.requireNonNull(status);
            this.comparison = comparison;
            this.reasons = List.copyOf(reasons);
            if (annualTaxes.isEmpty() || (status == ComparisonStatus.DEFINITIVE) != (comparison != null)) {
                throw new IllegalArgumentException("inconsistent public calculation projection");
            }
            if (annualTaxes.size() > 2 || annualTaxes.stream().map(AnnualTax::regime).distinct().count() != annualTaxes.size()
                    || (status == ComparisonStatus.DEFINITIVE && (annualTaxes.size() != 2 || !reasons.isEmpty()))) {
                throw new IllegalArgumentException("inconsistent regime set in public projection");
            }
        }

        @Override public List<Reason> reasons() { return reasons; }

        @Override public Map<String, Object> toResponse() {
            Map<String, Object> response = base("CALCULATION_AVAILABLE", reasons, true, comparison != null);
            List<Object> taxes = new ArrayList<>();
            for (AnnualTax tax : annualTaxes) {
                taxes.add(immutable(Map.of("regime", tax.regime().name(),
                        "annual_tax_before_credits", tax.amount().decimal())));
            }
            response.put("annual_taxes", List.copyOf(taxes));
            response.put("comparison_status", comparisonStatus.name());
            if (comparison != null) {
                response.put("comparison", immutable(Map.of(
                        "recommendation", comparison.recommendation().name(),
                        "difference_in_annual_tax", comparison.difference().decimal())));
            }
            return immutable(response);
        }
    }

    private static Map<String, Object> base(String status, List<Reason> reasons,
                                            boolean annual, boolean comparison) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contract_version", CONTRACT_VERSION);
        result.put("status", status);
        result.put("capabilities", immutable(Map.of("show_annual_tax", annual,
                "show_definitive_comparison", comparison, "export_itr", false, "submit_return", false)));
        result.put("reasons", reasons.stream().map(Enum::name).toList());
        return result;
    }

    private static Map<String, Object> immutable(Map<String, ?> map) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(new TreeMap<>(map)));
    }
}
