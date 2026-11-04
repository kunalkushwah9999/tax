package com.taxplatform.safety;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.taxplatform.safety.SafetyInput.*;
import static com.taxplatform.safety.PublicResult.*;
import static com.taxplatform.safety.GuidedResult.frozen;

/** User guidance derives from one signed, current policy decision; it grants no new authority. */
public final class GuidedResultService {
    private final SignedResultService results;

    public GuidedResultService(SafeResultService.ControlPlane controls, CandidateProofVerifier verifier) {
        results = new SignedResultService(Objects.requireNonNull(controls), Objects.requireNonNull(verifier));
    }

    public GuidedResult deliver(Actor actor, SignedCandidate signed) {
        PublicResult result = results.deliver(actor, signed);
        if (result instanceof Denied || result instanceof Blocked) {
            return new GuidedResult(result, blockedGuide(result));
        }
        // Detailed guidance may only inspect facts after signed delivery has authorized this decision.
        return new GuidedResult(result, inputGuide(result, signed.candidate().facts()));
    }

    static Map<String, Object> blockedGuide(PublicResult result) {
        Reason reason = result.reasons().get(0);
        String heading;
        String message;
        String action;
        switch (reason) {
            case ACCESS_DENIED -> {
                heading = "This result is not available";
                message = "Open a case you are allowed to access.";
                action = "OPEN_ACCESSIBLE_CASE";
            }
            case STALE_RESULT -> {
                heading = "Your inputs or calculation settings changed";
                message = "Calculate again before using an earlier result.";
                action = "RECALCULATE";
            }
            case INDEPENDENT_REFERENCE_NOT_APPROVED, RULE_NOT_EFFECTIVE -> {
                heading = "Calculation is awaiting validation";
                message = "A reviewed calculation is not available yet. Try again after validation is complete.";
                action = "WAIT_FOR_VALIDATION";
            }
            case VERIFIER_MISMATCH, QUARANTINED, PAYLOAD_INTEGRITY_FAILED, CONTEXT_BINDING_INVALID -> {
                heading = "This result needs a system review";
                message = "Do not use an earlier amount. The result is unavailable until the issue is resolved.";
                action = "WAIT_FOR_SYSTEM_REVIEW";
            }
            case VERIFICATION_NOT_RUN, NO_CALCULATIONS -> {
                heading = "Your calculation is not ready";
                message = "A checked calculation is not available yet. Try again when processing is complete.";
                action = "WAIT_FOR_CALCULATION";
            }
            default -> {
                heading = "The result is temporarily unavailable";
                message = "Try again when the service is available. Earlier amounts are not shown.";
                action = "RETRY_LATER";
            }
        }
        // No fact-derived checklist, completion summary, regime cards or note for a blocked/denied result.
        return frozen(Map.of("mode", result instanceof Denied ? "ACCESS_REQUIRED" : "UNAVAILABLE",
                "heading", heading, "message", message,
                "next_action", frozen(Map.of("action", action, "label", actionLabel(action))),
                "capabilities", capabilities(false, false)));
    }

    private static String actionLabel(String action) {
        return switch (action) {
            case "OPEN_ACCESSIBLE_CASE" -> "Open an accessible case";
            case "RECALCULATE" -> "Calculate again";
            case "WAIT_FOR_VALIDATION" -> "Check validation status later";
            case "WAIT_FOR_SYSTEM_REVIEW" -> "Check system review status later";
            case "WAIT_FOR_CALCULATION" -> "Check calculation status later";
            default -> "Try again later";
        };
    }

    private record Step(String id, String label, String status, String action, String help) {
        Map<String, Object> response() {
            return frozen(Map.of("id", id, "label", label, "status", status, "action", action, "help", help));
        }
        boolean complete() { return status.equals("REVIEWED"); }
    }

    private static Step amountStep(String id, String label, Fact<Money> fact, String help) {
        return switch (fact.state()) {
            case UNKNOWN -> new Step(id, label, "MISSING", "ENTER_AMOUNT", help + " Enter an amount or explicitly confirm zero.");
            case ESTIMATED -> new Step(id, label, "NEEDS_CONFIRMATION", "CONFIRM_AMOUNT", help + " Review the proposed value before confirming it.");
            case CONFLICT -> new Step(id, label, "CONFLICT", "RESOLVE_CONFLICT", help + " Resolve the conflicting values; do not choose one automatically.");
            case CONFIRMED -> new Step(id, label, "REVIEWED", "NONE", help);
        };
    }

    private static Step inventoryStep(String id, String label, Fact<Boolean> fact, String help) {
        if (fact.confirmedValue().orElse(false)) return new Step(id, label, "REVIEWED", "NONE", help);
        return new Step(id, label, fact.state() == Fact.State.CONFLICT ? "CONFLICT" : "NEEDS_REVIEW",
                fact.state() == Fact.State.CONFLICT ? "RESOLVE_CONFLICT" : "REVIEW_INVENTORY", help);
    }

    private static List<Step> checklist(CaseFacts facts) {
        List<Step> steps = new ArrayList<>();
        steps.add(facts.scope() == Scope.SUPPORTED
                ? new Step("SUPPORTED_SCOPE", "Supported income types", "REVIEWED", "NONE", "This case is within the supported workflow.")
                : new Step("SUPPORTED_SCOPE", "Supported income types", "NEEDS_REVIEW", "REVIEW_SCOPE",
                        "Check whether all your income types fit this planner. Unsupported income needs an appropriate reviewed workflow."));
        steps.add(amountStep("SALARY", "Annual salary", facts.salary(), "Keep the salary input separate from bank interest."));
        steps.add(amountStep("SAVINGS_INTEREST", "Savings account interest", facts.savingsInterest(), "Keep savings account interest separate from deposit interest."));
        steps.add(amountStep("DEPOSIT_INTEREST", "Qualifying deposit interest", facts.qualifyingDepositInterest(), "Review the deposit classification used for this case."));
        steps.add(amountStep("OTHER_INTEREST", "Other interest", facts.otherInterest(), "Review interest that is not in the two categories above."));
        steps.add(inventoryStep("INCOME_INVENTORY", "All income reviewed", facts.incomeInventoryComplete(),
                "Confirm that all income sources have been reviewed. An entered amount alone does not establish completeness."));
        steps.add(inventoryStep("NEW_DEDUCTIONS", "New regime deduction review", facts.deductionInventoryComplete().get(Regime.NEW),
                "Review the deductions relevant to the new regime before treating its calculation as complete."));
        steps.add(inventoryStep("OLD_DEDUCTIONS", "Old regime deduction review", facts.deductionInventoryComplete().get(Regime.OLD),
                "Review the deductions relevant to the old regime. An unfinished review cannot support a definitive comparison."));
        return List.copyOf(steps);
    }

    private static Map<String, Object> inputGuide(PublicResult result, CaseFacts facts) {
        List<Step> steps = checklist(facts);
        List<Map<String, Object>> responses = steps.stream().map(Step::response).toList();
        long reviewed = steps.stream().filter(Step::complete).count();
        List<Map<String, Object>> cards = regimeCards(result);
        boolean definitive = result instanceof Calculation
                && "DEFINITIVE".equals(result.toResponse().get("comparison_status"));
        boolean scopeReview = facts.scope() != Scope.SUPPORTED;
        String mode = scopeReview ? "SCOPE_REVIEW" : definitive ? "COMPARISON_AVAILABLE"
                : result instanceof Calculation ? "PARTIAL_COMPARISON" : "INFORMATION_REQUIRED";
        String heading = switch (mode) {
            case "SCOPE_REVIEW" -> "Check the supported income types";
            case "COMPARISON_AVAILABLE" -> "Your annual tax comparison is available";
            case "PARTIAL_COMPARISON" -> "Review what is ready and what is missing";
            default -> "Complete your inputs one step at a time";
        };
        String message = definitive ? comparisonMessage(result)
                : scopeReview ? "Some inputs need a different or reviewed workflow. A complete tax amount is not shown."
                : result instanceof Calculation ? "Only complete, permitted regime amounts are shown. A definitive comparison is not available."
                : "The checklist shows the information that needs your attention. Missing information is never treated as zero.";
        List<String> notices = new ArrayList<>(List.of(
                "This checklist tracks input review, not accuracy or filing readiness.",
                "Annual tax before credits is different from a final amount payable or refund.",
                "After changing an input, calculate again before using earlier amounts."));
        Map<String, Object> guide = new java.util.LinkedHashMap<>();
        guide.put("mode", mode); guide.put("heading", heading); guide.put("message", message);
        guide.put("checklist", responses);
        guide.put("progress_label", reviewed + " of " + steps.size() + " input reviews complete");
        guide.put("next_action", nextAction(result, steps));
        guide.put("regime_cards", cards);
        guide.put("notices", List.copyOf(notices));
        guide.put("preparation_note", preparationNote(steps, result.reasons()));
        guide.put("capabilities", capabilities(true, true));
        return frozen(guide);
    }

    static Map<String, Object> draftGuide(CaseFacts facts, DeliveryContext context) {
        List<Step> steps = checklist(facts);
        Map<String, Object> action = null;
        for (Step step : steps) {
            if (!step.complete()) {
                action = frozen(Map.of("action", step.action(), "field", step.id(), "label", step.label(), "help", step.help()));
                break;
            }
        }
        boolean validationReady = context.ruleState() == RuleState.EFFECTIVE && context.referenceState() == ReferenceState.APPROVED;
        if (action == null) {
            action = frozen(Map.of("action", validationReady ? "REQUEST_CALCULATION" : "WAIT_FOR_VALIDATION",
                    "label", validationReady ? "Request a checked calculation" : "Check validation status later",
                    "help", "Reviewed inputs alone do not establish tax correctness or filing readiness."));
        }
        long reviewed = steps.stream().filter(Step::complete).count();
        return frozen(Map.of("mode", facts.scope() == Scope.SUPPORTED ? "INPUT_PREPARATION" : "SCOPE_REVIEW",
                "heading", "Prepare your inputs one step at a time",
                "message", "Review your information before requesting a calculation. No tax amount is shown in input preparation.",
                "checklist", steps.stream().map(Step::response).toList(),
                "progress_label", reviewed + " of " + steps.size() + " input reviews complete",
                "next_action", action,
                "validation_notice", validationReady ? "A calculation must still pass the result checks."
                        : "Input preparation is available. A reviewed tax calculation is not yet available.",
                "preparation_note", preparationNote(steps, List.of()),
                "notices", List.of("Input review progress is not a measure of accuracy or filing readiness.",
                        "Changing inputs requires a new checked calculation before using earlier amounts."),
                "capabilities", capabilities(true, true)));
    }

    private static String comparisonMessage(PublicResult result) {
        Object comparison = result.toResponse().get("comparison");
        if (comparison instanceof Map<?, ?> map) {
            Object recommended = map.get("recommendation");
            if ("TIE".equals(recommended)) return "Both regimes have equal annual tax in this calculation. No tax saving is claimed.";
            if ("NEW".equals(recommended)) return "The new regime has lower annual tax for the confirmed inputs in this calculation.";
            if ("OLD".equals(recommended)) return "The old regime has lower annual tax for the confirmed inputs in this calculation.";
        }
        throw new IllegalStateException("definitive comparison projection is inconsistent");
    }

    private static Map<String, Object> nextAction(PublicResult result, List<Step> steps) {
        for (Step step : steps) {
            if (!step.complete()) return frozen(Map.of("action", step.action(), "field", step.id(),
                    "label", step.label(), "help", step.help()));
        }
        if (result.reasons().contains(Reason.REGIME_NOT_AVAILABLE) || result.reasons().contains(Reason.REGIME_AVAILABILITY_UNKNOWN)) {
            return frozen(Map.of("action", "REVIEW_REGIME_AVAILABILITY", "label", "Review which regimes are available",
                    "help", "A lower calculated amount does not establish that a regime is available for this case."));
        }
        if (result.reasons().contains(Reason.COMPARISON_CALCULATION_MISSING)) {
            return frozen(Map.of("action", "WAIT_FOR_CALCULATION", "label", "Check calculation status later",
                    "help", "A required regime calculation is not available yet."));
        }
        return frozen(Map.of("action", "REVIEW_CALCULATION", "label", "Review the annual tax calculation",
                "help", "Review the calculation before using it for planning. Filing and tax-credit settlement are separate workflows."));
    }

    private static List<Map<String, Object>> regimeCards(PublicResult result) {
        List<Map<String, Object>> cards = new ArrayList<>();
        Object visible = result.toResponse().get("annual_taxes");
        for (Regime regime : Regime.values()) {
            boolean available = false;
            if (visible instanceof List<?> list) {
                for (Object row : list) {
                    if (row instanceof Map<?, ?> map && regime.name().equals(map.get("regime"))) available = true;
                }
            }
            cards.add(frozen(Map.of("regime", regime.name(), "label", regime == Regime.NEW ? "New regime" : "Old regime",
                    "status", available ? "CALCULATION_AVAILABLE" : "NEEDS_REVIEW",
                    "message", available ? "Annual tax before credits is shown in the calculation."
                            : "No amount is shown for this regime. Review the checklist, regime availability and calculation status.")));
        }
        return List.copyOf(cards);
    }

    private static String preparationNote(List<Step> steps, List<Reason> reasons) {
        StringBuilder note = new StringBuilder("Preparation checklist\nThis is not a tax return, approval or proof of filing.\n");
        for (Step step : steps) note.append("- ").append(step.label()).append(": ").append(step.status()).append('\n');
        if (reasons.contains(Reason.REGIME_NOT_AVAILABLE) || reasons.contains(Reason.REGIME_AVAILABILITY_UNKNOWN)) {
            note.append("- Review regime availability.\n");
        }
        if (reasons.contains(Reason.COMPARISON_CALCULATION_MISSING)) note.append("- A regime calculation is still pending.\n");
        note.append("This note omits income amounts, tax amounts and personal identifiers. Recheck the current case before acting.");
        return note.toString();
    }

    private static Map<String, Object> capabilities(boolean checklist, boolean note) {
        return frozen(Map.of("show_input_checklist", checklist, "copy_preparation_note", note,
                "submit_return", false, "export_itr", false));
    }
}
