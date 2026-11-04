package com.taxplatform.safety;

import java.util.Objects;

/** Explicit user-intent commands. The HTTP adapter must establish the actor and user action. */
public final class FactEdits {
    private FactEdits() { }
    public enum AmountField { SALARY, SAVINGS_INTEREST, DEPOSIT_INTEREST, OTHER_INTEREST }
    public enum Inventory { INCOME, NEW_DEDUCTIONS, OLD_DEDUCTIONS }

    public sealed interface Command permits ProposeAmount, ConfirmAmount, ResolveConflict,
            ClearAmount, ReviewInventory, UnreviewInventory { }

    public record ProposeAmount(AmountField field, String text, HumanMoneyParser.Unit unit) implements Command {
        public ProposeAmount { Objects.requireNonNull(field); }
        @Override public String toString() { return "ProposeAmount[redacted]"; }
    }
    public record ConfirmAmount(AmountField field, Money displayedAmount) implements Command {
        public ConfirmAmount { Objects.requireNonNull(field); Objects.requireNonNull(displayedAmount); }
        @Override public String toString() { return "ConfirmAmount[redacted]"; }
    }
    public record ResolveConflict(AmountField field, Money selectedAmount) implements Command {
        public ResolveConflict { Objects.requireNonNull(field); Objects.requireNonNull(selectedAmount); }
        @Override public String toString() { return "ResolveConflict[redacted]"; }
    }
    public record ClearAmount(AmountField field) implements Command {
        public ClearAmount { Objects.requireNonNull(field); }
    }
    public record ReviewInventory(Inventory inventory) implements Command {
        public ReviewInventory { Objects.requireNonNull(inventory); }
    }
    public record UnreviewInventory(Inventory inventory) implements Command {
        public UnreviewInventory { Objects.requireNonNull(inventory); }
    }
    public record Request(String requestId, CalculationKey expectedKey, Command command) {
        public Request {
            CalculationKey.requireId(requestId);
            Objects.requireNonNull(expectedKey); Objects.requireNonNull(command);
        }
        @Override public String toString() { return "FactEditRequest[redacted]"; }
    }

    /** Reviewed server release identity; users cannot edit scope or approval through these commands. */
    public record ValidationIdentity(String periodKey, String ruleBundleHash, String engineDigest,
                                     String verifierDigest, String contractVersion, String settingsHash) {
        public ValidationIdentity {
            new CalculationKey("validation", "validation", "validation", "validation", periodKey,
                    ruleBundleHash, engineDigest, verifierDigest, contractVersion, settingsHash);
        }
        CalculationKey key(String tenant, String profile, String caseId, String snapshot) {
            return new CalculationKey(tenant, profile, caseId, snapshot, periodKey, ruleBundleHash,
                    engineDigest, verifierDigest, contractVersion, settingsHash);
        }
        @Override public String toString() { return "ValidationIdentity[redacted]"; }
    }
}
