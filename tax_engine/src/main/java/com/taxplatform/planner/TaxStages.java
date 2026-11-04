package com.taxplatform.planner;

import com.taxplatform.safety.SafetyInput.Regime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Private engineering workings, never a public result projection or a golden expectation. */
public record TaxStages(Regime regime,AgeBand ageBand,Map<Stage,BigDecimal> amounts) {
    public enum AgeBand { UNDER_60, SENIOR, SUPER_SENIOR }
    public enum Stage { GROSS_SALARY, STANDARD_DEDUCTION, TAXABLE_SALARY, TOTAL_INTEREST, GROSS_TOTAL_INCOME,
        LIMITED_80C_CLAIM, LIMITED_80D_CLAIM, LIMITED_NPS_1B_CLAIM, LIMITED_80TTA_CLAIM, LIMITED_80TTB_CLAIM,
        TOTAL_DEDUCTION_ALLOWED, TOTAL_INCOME_BEFORE_ROUNDING, ROUNDED_TOTAL_INCOME,
        SLAB_TAX, REBATE, MARGINAL_RELIEF, TAX_AFTER_RELIEF, CESS, ANNUAL_TAX_BEFORE_CREDITS,
        ZERO_CREDIT_ROUNDED_WORKING }
    public TaxStages {
        Objects.requireNonNull(regime);Objects.requireNonNull(ageBand);
        EnumMap<Stage,BigDecimal> normalized=new EnumMap<>(Stage.class);
        for(Stage stage:Stage.values()) {
            BigDecimal value=Objects.requireNonNull(amounts.get(stage),"every stage required");
            if(value.signum()<0)throw new IllegalArgumentException("negative tax stage");
            normalized.put(stage,value.setScale(2,RoundingMode.UNNECESSARY));
        }
        amounts=Map.copyOf(normalized);
    }
    public BigDecimal value(Stage stage){return amounts.get(stage);}
    public Map<String,Object> reviewProjection(){
        Map<String,Object> stages=new TreeMap<>();amounts.forEach((key,value)->stages.put(key.name().toLowerCase(java.util.Locale.ROOT),value.toPlainString()));
        return Map.of("regime",regime.name(),"age_band",ageBand.name(),"stages",Map.copyOf(stages),
                "use","ENGINEERING_WORKINGS_NOT_APPROVED_REFERENCE","filing_ready",false);
    }
    @Override public String toString(){return "TaxStages[redacted]";}
}
