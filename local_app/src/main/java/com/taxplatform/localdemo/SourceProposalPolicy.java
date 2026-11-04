package com.taxplatform.localdemo;

import com.taxplatform.safety.CalculationKey;
import com.taxplatform.safety.FactEdits;
import com.taxplatform.safety.HumanMoneyParser;
import com.taxplatform.safety.Money;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** A local source observation may create only an estimated whole-category replacement proposal. */
final class SourceProposalPolicy {
    record Snapshot(CalculationKey inputKey,String comparisonVersion,StatementComparison.Group group,
                    StatementComparison.Observation selected,List<StatementComparison.Group> allGroups) {
        Snapshot {Objects.requireNonNull(inputKey);Objects.requireNonNull(comparisonVersion);Objects.requireNonNull(group);Objects.requireNonNull(selected);allGroups=List.copyOf(allGroups);}
        @Override public String toString(){return "SourceSnapshot[redacted]";}
    }
    record Intent(FactEdits.AmountField field,Money displayedAmount,boolean wholeCategory,boolean replaceCurrent) {
        Intent {Objects.requireNonNull(field);Objects.requireNonNull(displayedAmount);}
        @Override public String toString(){return "SourceProposalIntent[redacted]";}
    }
    /** Minimal historical provenance. No monetary value, taxpayer identity or source authenticity claim. */
    record Reference(FactEdits.AmountField field,StatementComparison.Source source,StatementComparison.Kind valueKind,
                     String observationId,String period) {
        Map<String,Object> projection(String currentState){return Map.of("field",field.name(),"source",source.name(),"value_kind",valueKind.name(),
                "observation_id",observationId,"period",period,"current_fact_state",currentState,"source_status","HISTORICAL_UNVERIFIED_OBSERVATION", "source_authenticity_verified",false);}
        @Override public String toString(){return "SourceReference[redacted]";}
    }
    record Prepared(Reference reference,Money amount) {
        FactEdits.Command command(){return new FactEdits.ProposeAmount(reference.field(),amount.decimal(),HumanMoneyParser.Unit.RUPEES);}
        @Override public String toString(){return "PreparedSourceProposal[redacted]";}
    }
    private SourceProposalPolicy(){ }
    static FactEdits.AmountField target(StatementComparison.Category category){return switch(category){
        case SALARY->FactEdits.AmountField.SALARY;case SAVINGS_INTEREST->FactEdits.AmountField.SAVINGS_INTEREST;
        case DEPOSIT_INTEREST->FactEdits.AmountField.DEPOSIT_INTEREST;case OTHER_INTEREST->FactEdits.AmountField.OTHER_INTEREST;
        case OTHER_INCOME->null;};}
    static String barrier(StatementComparison.Group group,List<StatementComparison.Group> all) {
        if(target(group.category())==null)return "CATEGORY_NOT_PLANNER_INPUT";
        if(!group.period().equals("FY2025-26"))return "SOURCE_PERIOD_NOT_SUPPORTED";
        if(group.coverage()!=StatementComparison.Coverage.ANNUAL)return "ANNUAL_SOURCE_REQUIRED";
        if(all.stream().filter(g->g.category()==group.category()).count()!=1)return "CATEGORY_COVERAGE_AMBIGUOUS";
        Set<StatementComparison.Status> allowed=Set.of(StatementComparison.Status.AGREEMENT,StatementComparison.Status.SINGLE_SOURCE);
        if(!allowed.containsAll(StatementComparison.flags(group.observations(),true)))return "GROSS_SOURCE_REVIEW_REQUIRED";
        return "READY_FOR_USER_ATTESTATION";
    }
    static Prepared prepare(Snapshot source,CalculationKey current,Intent intent) {
        if(!source.inputKey().equals(current))reject("STALE_INPUT");
        if(!source.allGroups().contains(source.group()) || !source.group().observations().contains(source.selected()))reject("SOURCE_BINDING_INVALID");
        String barrier=barrier(source.group(),source.allGroups());if(!barrier.equals("READY_FOR_USER_ATTESTATION"))reject(barrier);
        FactEdits.AmountField field=target(source.group().category());if(field!=intent.field())reject("SOURCE_FIELD_MISMATCH");
        if(!intent.wholeCategory())reject("WHOLE_CATEGORY_REVIEW_REQUIRED");if(!intent.replaceCurrent())reject("REPLACEMENT_REVIEW_REQUIRED");
        Money gross=source.selected().gross().orElseThrow(()->new ComparisonWorkspace.Rejected("GROSS_VALUE_MISSING"));
        if(!gross.equals(intent.displayedAmount()))reject("SOURCE_AMOUNT_CHANGED");
        return new Prepared(new Reference(field,source.selected().source(),source.selected().kind(),source.selected().id(),source.group().period()),gross);
    }
    static FactEdits.Command replayCommand(Reference original,Money amount){return new Prepared(original,amount).command();}
    private static void reject(String code){throw new ComparisonWorkspace.Rejected(code);}
}
