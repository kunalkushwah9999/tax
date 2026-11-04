package com.taxplatform.planner;

import com.taxplatform.safety.Fact;
import com.taxplatform.safety.Money;
import com.taxplatform.safety.SafetyInput.Regime;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/** Private sensitivity analysis over declared eligible additional 80C only, in one-paise steps. */
public final class BreakEven {
    public record Threshold(String status,Optional<Money> additionalEligible80C) {
        public Threshold {additionalEligible80C=java.util.Objects.requireNonNull(additionalEligible80C);}
        public Map<String,Object> reviewProjection(){return additionalEligible80C.<Map<String,Object>>map(v->Map.of("status",status,"additional_eligible_80c",v.decimal()))
                .orElseGet(()->Map.of("status",status));}
    }
    public record Analysis(Threshold weak,Threshold strict,Money declaredCapacity) {
        public Map<String,Object> reviewProjection(){return Map.of("weak_old_at_most_new",weak.reviewProjection(),"strict_old_below_new",strict.reviewProjection(),
                "declared_eligible_capacity",declaredCapacity.decimal(),"metric","ZERO_CREDIT_ROUNDED_WORKING",
                "interpretation","ELIGIBILITY_SENSITIVITY_NOT_A_SPENDING_OR_INVESTMENT_INSTRUCTION");}
    }
    private final DualPlanner planner=new DualPlanner();
    public Analysis analyze(PlannerInput input,Money capacity) {
        if(!input.regimes().containsAll(java.util.Set.of(Regime.NEW,Regime.OLD)))throw new IllegalArgumentException("both regimes required");
        Money existing=input.eligible80C().confirmedValue().orElseThrow(()->new IllegalArgumentException("confirmed eligible 80C required"));
        BigDecimal remaining=new BigDecimal("150000").subtract(existing.value()).max(BigDecimal.ZERO);
        if(capacity.value().compareTo(remaining)>0)throw new IllegalArgumentException("capacity exceeds remaining 80C ceiling");
        checked(input);long limit=capacity.value().movePointRight(2).longValueExact();
        return new Analysis(find(input,limit,false),find(input,limit,true),capacity);
    }
    private Threshold find(PlannerInput base,long maximum,boolean strict) {
        if(wins(base,0,strict))return new Threshold("ALREADY_SATISFIED",Optional.of(Money.parse("0")));
        if(!wins(base,maximum,strict))return new Threshold("NOT_ATTAINABLE_WITHIN_DECLARED_CAPACITY",Optional.empty());
        long low=1,high=maximum;
        while(low<high){long middle=low+(high-low)/2;if(wins(base,middle,strict))high=middle;else low=middle+1;}
        if(!wins(base,low,strict) || wins(base,low-1,strict))throw new IllegalStateException("threshold boundary verification failed");
        return new Threshold("FIRST_ATTAINABLE_POINT",Optional.of(new Money(BigDecimal.valueOf(low,2))));
    }
    private boolean wins(PlannerInput base,long extraPaise,boolean strict) {
        PlannerInput changed=base.with80C(new Money(base.eligible80C().confirmedValue().orElseThrow().value().add(BigDecimal.valueOf(extraPaise,2))));
        Map<Regime,DualPlanner.Assessment> pair=checked(changed);
        int order=pair.get(Regime.OLD).privateWorkings().value(TaxStages.Stage.ZERO_CREDIT_ROUNDED_WORKING)
                .compareTo(pair.get(Regime.NEW).privateWorkings().value(TaxStages.Stage.ZERO_CREDIT_ROUNDED_WORKING));
        return strict?order<0:order<=0;
    }
    private Map<Regime,DualPlanner.Assessment> checked(PlannerInput input) {
        Map<Regime,DualPlanner.Assessment> pair=planner.evaluate(input);
        if(pair.values().stream().anyMatch(a->a.status()!=DualPlanner.Status.CALCULATED))throw new IllegalArgumentException("complete supported matching calculations required");
        return pair;
    }
}
