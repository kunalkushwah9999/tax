package com.taxplatform.safety;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Trusted server inputs. Never bind client JSON directly to these types. */
public final class SafetyInput {
    private SafetyInput() { }

    public enum Regime { NEW, OLD }
    public enum Scope { SUPPORTED, PARTIAL, UNSUPPORTED, UNKNOWN }
    public enum Verification { MATCH, MISMATCH, NOT_RUN }
    public enum RuleState { EFFECTIVE, UNAPPROVED, RECALLED, MISSING }
    public enum ReferenceState { APPROVED, PENDING, REJECTED }
    public enum Availability { ALLOWED, NOT_ALLOWED, UNKNOWN }

    public record Actor(String tenantId, String principalId) {
        public Actor { CalculationKey.requireId(tenantId); CalculationKey.requireId(principalId); }
    }

    public record CaseFacts(
            Scope scope, Fact<Money> salary, Fact<Money> savingsInterest,
            Fact<Money> qualifyingDepositInterest, Fact<Money> otherInterest,
            Fact<Boolean> incomeInventoryComplete,
            Map<Regime, Fact<Boolean>> deductionInventoryComplete) {
        public CaseFacts {
            Objects.requireNonNull(scope, "scope");
            Objects.requireNonNull(salary, "salary");
            Objects.requireNonNull(savingsInterest, "savings interest");
            Objects.requireNonNull(qualifyingDepositInterest, "deposit interest");
            Objects.requireNonNull(otherInterest, "other interest");
            Objects.requireNonNull(incomeInventoryComplete, "income inventory");
            deductionInventoryComplete = completeRegimeMap(deductionInventoryComplete);
        }
    }

    public record RegimeCalculation(Regime regime, Money annualTaxBeforeCredits,
                                    Verification verification) {
        public RegimeCalculation {
            Objects.requireNonNull(regime, "regime");
            Objects.requireNonNull(annualTaxBeforeCredits, "annual tax");
            Objects.requireNonNull(verification, "verification");
        }
    }

    public record Candidate(CalculationKey key, CaseFacts facts,
                            Map<Regime, RegimeCalculation> calculations) {
        public Candidate {
            Objects.requireNonNull(key, "calculation key");
            Objects.requireNonNull(facts, "facts");
            calculations = Map.copyOf(calculations);
            for (Map.Entry<Regime, RegimeCalculation> entry : calculations.entrySet()) {
                if (entry.getKey() != entry.getValue().regime()) {
                    throw new IllegalArgumentException("calculation regime does not match its map key");
                }
            }
        }
        @Override public String toString() { return "Candidate[redacted]"; }
    }

    public record DeliveryContext(
            CalculationKey currentKey, boolean authorized, boolean deleted,
            boolean payloadIntegrityVerified, boolean restoreCaughtUp,
            RuleState ruleState, ReferenceState referenceState, boolean quarantined,
            Map<Regime, Availability> regimeAvailability) {
        public DeliveryContext {
            Objects.requireNonNull(currentKey, "current key");
            Objects.requireNonNull(ruleState, "rule state");
            Objects.requireNonNull(referenceState, "reference state");
            regimeAvailability = completeRegimeMap(regimeAvailability);
        }
    }

    private static <T> Map<Regime, T> completeRegimeMap(Map<Regime, T> original) {
        Objects.requireNonNull(original, "regime map");
        EnumMap<Regime, T> copy = new EnumMap<>(Regime.class);
        copy.putAll(original);
        for (Regime regime : Regime.values()) Objects.requireNonNull(copy.get(regime), "explicit state required for each regime");
        return Map.copyOf(copy);
    }
}
