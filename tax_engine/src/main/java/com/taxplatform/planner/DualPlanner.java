package com.taxplatform.planner;

import com.taxplatform.safety.Fact;
import com.taxplatform.safety.Money;
import com.taxplatform.safety.SafetyInput.Regime;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Computes private workings with two implementations. Agreement never grants legal/reference approval. */
public final class DualPlanner {
    public enum Status { CALCULATED, INFORMATION_REQUIRED, UNSUPPORTED, VERIFIER_MISMATCH }
    public record Assessment(Status status,List<String> issues,TaxStages privateWorkings) {
        public Assessment {Objects.requireNonNull(status);issues=List.copyOf(issues);}
        public Map<String,Object> reviewProjection(){
            if(status==Status.CALCULATED)return Map.of("status",status.name(),"issues",issues,"workings",privateWorkings.reviewProjection());
            return Map.of("status",status.name(),"issues",issues);
        }
        @Override public String toString(){return "Assessment["+status+", workings redacted]";}
    }
    @FunctionalInterface interface Arithmetic {TaxStages calculate(PlannerInput input,Regime regime);}
    private final Arithmetic primary,verifier;
    public DualPlanner(){this(new PrimaryEngine()::calculate,new PaiseVerifier()::calculate);}
    DualPlanner(Arithmetic primary,Arithmetic verifier){this.primary=Objects.requireNonNull(primary);this.verifier=Objects.requireNonNull(verifier);}

    public Map<Regime,Assessment> evaluate(PlannerInput input) {
        Objects.requireNonNull(input);EnumMap<Regime,Assessment> result=new EnumMap<>(Regime.class);
        for(Regime regime:input.regimes())result.put(regime,evaluate(input,regime));
        return Map.copyOf(result);
    }
    private Assessment evaluate(PlannerInput input,Regime regime) {
        if(!PlannerInput.PERIOD.equals(input.period()))return unavailable(Status.UNSUPPORTED,"PERIOD_NOT_IMPLEMENTED");
        if(input.residency()==PlannerInput.Residency.UNKNOWN)return unavailable(Status.INFORMATION_REQUIRED,"RESIDENCY_REQUIRED");
        if(input.residency()!=PlannerInput.Residency.ROR)return unavailable(Status.UNSUPPORTED,"RESIDENCY_OUTSIDE_STARTER");
        if(input.screening().containsValue(PlannerInput.Answer.YES))return unavailable(Status.UNSUPPORTED,"EXCLUDED_STARTER_FACT");
        if(input.screening().containsValue(PlannerInput.Answer.UNKNOWN))return unavailable(Status.INFORMATION_REQUIRED,"SCREENING_INCOMPLETE");
        if(input.dateOfBirth().state()!=Fact.State.CONFIRMED)return unavailable(Status.INFORMATION_REQUIRED,"DOB_CONFIRMATION_REQUIRED");
        LocalDate born=input.dateOfBirth().confirmedValue().orElseThrow();
        if(born.isAfter(LocalDate.of(2025,4,1)) || born.isBefore(LocalDate.of(1906,4,1)))return unavailable(Status.UNSUPPORTED,"DOB_OUTSIDE_STARTER");
        if(born.plusYears(18).isAfter(LocalDate.of(2025,4,1)))return unavailable(Status.UNSUPPORTED,"ADULT_THROUGHOUT_FY_REQUIRED");
        if(!input.incomeComplete().confirmedValue().orElse(false))return unavailable(Status.INFORMATION_REQUIRED,"INCOME_INVENTORY_INCOMPLETE");
        if(List.of(input.salaries(),input.savings(),input.deposits(),input.otherInterest()).stream().anyMatch(f->f.state()!=Fact.State.CONFIRMED))
            return unavailable(Status.INFORMATION_REQUIRED,"CONFIRMED_INCOME_REQUIRED");
        if(regime==Regime.OLD) {
            if(!input.deductionsComplete().confirmedValue().orElse(false))return unavailable(Status.INFORMATION_REQUIRED,"OLD_DEDUCTION_INVENTORY_INCOMPLETE");
            if(List.of(input.eligible80C(),input.eligible80D(),input.eligibleNps1B()).stream().anyMatch(f->f.state()!=Fact.State.CONFIRMED))
                return unavailable(Status.INFORMATION_REQUIRED,"CONFIRMED_ELIGIBLE_DEDUCTIONS_REQUIRED");
            // 80D is a precomputed eligible amount, not a premium/expense input. This ceiling alone does not prove eligibility.
            if(input.eligible80D().confirmedValue().orElseThrow().value().compareTo(new BigDecimal("100000"))>0)
                return unavailable(Status.UNSUPPORTED,"ELIGIBLE_80D_ABOVE_AGGREGATE_CEILING");
        }
        TaxStages first=primary.calculate(input,regime),second=verifier.calculate(input,regime);
        List<String> mismatches=new ArrayList<>();
        if(first.regime()!=regime || second.regime()!=regime)mismatches.add("REGIME_BINDING");
        if(first.ageBand()!=second.ageBand())mismatches.add("AGE_BAND");
        for(TaxStages.Stage stage:TaxStages.Stage.values())if(first.value(stage).compareTo(second.value(stage))!=0)mismatches.add(stage.name());
        if(!mismatches.isEmpty())return new Assessment(Status.VERIFIER_MISMATCH,mismatches,first);
        if(first.value(TaxStages.Stage.ROUNDED_TOTAL_INCOME).compareTo(new BigDecimal("5000000"))>0)
            return unavailable(Status.UNSUPPORTED,"SURCHARGE_NOT_IMPLEMENTED");
        return new Assessment(Status.CALCULATED,List.of(),first);
    }
    private static Assessment unavailable(Status status,String issue){return new Assessment(status,List.of(issue),null);}
    public Map<String,Object> privateReview(PlannerInput input) {
        Map<Regime,Assessment> assessments=evaluate(input);
        Map<String,Object> rows=new java.util.TreeMap<>();assessments.forEach((regime,assessment)->rows.put(regime.name(),assessment.reviewProjection()));
        Map<String,Object> response=new java.util.TreeMap<>(Map.of("contract_version","planner-review.v1","period",input.period(),"regimes",Map.copyOf(rows),
                "independent_reference_approved",false,"rule_release_approved",false,"filing_ready",false,
                "purpose","PRIVATE_ENGINEERING_REVIEW_ONLY_NOT_GOLDEN_EXPECTATIONS"));
        if(assessments.size()==2 && assessments.values().stream().allMatch(a->a.status()==Status.CALCULATED)) {
            BigDecimal newer=assessments.get(Regime.NEW).privateWorkings().value(TaxStages.Stage.ANNUAL_TAX_BEFORE_CREDITS);
            BigDecimal older=assessments.get(Regime.OLD).privateWorkings().value(TaxStages.Stage.ANNUAL_TAX_BEFORE_CREDITS);
            int order=newer.compareTo(older);
            response.put("arithmetic_comparison",Map.of("lower_annual_tax",order<0?"NEW":order>0?"OLD":"TIE",
                    "difference_before_credits",newer.subtract(older).abs().toPlainString(),"regime_election_eligibility","NOT_ESTABLISHED"));
        }
        return Map.copyOf(response);
    }
}
