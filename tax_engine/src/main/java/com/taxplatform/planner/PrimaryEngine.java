package com.taxplatform.planner;

import com.taxplatform.safety.Money;
import com.taxplatform.safety.SafetyInput.Regime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;

import static com.taxplatform.planner.TaxStages.Stage.*;
import static com.taxplatform.planner.TaxStages.AgeBand.*;

/** BigDecimal implementation. Call through DualPlanner for scope and independent stage comparison. */
final class PrimaryEngine {
    private static final BigDecimal ZERO=new BigDecimal("0.00");
    private record Band(BigDecimal lower,BigDecimal upper,BigDecimal rate) { }
    private static Band band(String lower,String upper,String rate){return new Band(new BigDecimal(lower),upper==null?null:new BigDecimal(upper),new BigDecimal(rate));}
    private static final List<Band> NEW=List.of(band("0","400000","0"),band("400000","800000","0.05"),
            band("800000","1200000","0.10"),band("1200000","1600000","0.15"),band("1600000","2000000","0.20"),
            band("2000000","2400000","0.25"),band("2400000",null,"0.30"));
    TaxStages calculate(PlannerInput input,Regime regime) {
        LocalDate dob=input.dateOfBirth().confirmedValue().orElseThrow();
        LocalDate end=LocalDate.of(2026,3,31);
        TaxStages.AgeBand age=!dob.plusYears(80).minusDays(1).isAfter(end)?SUPER_SENIOR:
                !dob.plusYears(60).minusDays(1).isAfter(end)?SENIOR:UNDER_60;
        EnumMap<TaxStages.Stage,BigDecimal> s=new EnumMap<>(TaxStages.Stage.class);
        BigDecimal salary=input.salaries().confirmedValue().orElseThrow().stream().map(Money::value).reduce(ZERO,BigDecimal::add);
        BigDecimal standard=salary.min(new BigDecimal(regime==Regime.NEW?"75000":"50000"));
        BigDecimal interest=input.savings().confirmedValue().orElseThrow().value().add(input.deposits().confirmedValue().orElseThrow().value())
                .add(input.otherInterest().confirmedValue().orElseThrow().value());
        BigDecimal gti=salary.subtract(standard).add(interest);
        BigDecimal c=ZERO,d=ZERO,n=ZERO,tta=ZERO,ttb=ZERO;
        if(regime==Regime.OLD) {
            c=input.eligible80C().confirmedValue().orElseThrow().value().min(new BigDecimal("150000"));
            d=input.eligible80D().confirmedValue().orElseThrow().value();
            n=input.eligibleNps1B().confirmedValue().orElseThrow().value().min(new BigDecimal("50000"));
            if(age==UNDER_60)tta=input.savings().confirmedValue().orElseThrow().value().min(new BigDecimal("10000"));
            else ttb=input.savings().confirmedValue().orElseThrow().value().add(input.deposits().confirmedValue().orElseThrow().value()).min(new BigDecimal("50000"));
        }
        BigDecimal deduction=c.add(d).add(n).add(tta).add(ttb).min(gti);
        BigDecimal before=gti.subtract(deduction),total=roundTen(before);
        List<Band> bands=regime==Regime.NEW?NEW:age==SUPER_SENIOR?List.of(band("0","500000","0"),band("500000","1000000","0.20"),band("1000000",null,"0.30")):
                List.of(band("0",age==SENIOR?"300000":"250000","0"),band(age==SENIOR?"300000":"250000","500000","0.05"),band("500000","1000000","0.20"),band("1000000",null,"0.30"));
        BigDecimal slab=ZERO;
        for(Band b:bands) {
            BigDecimal upper=b.upper()==null?total:total.min(b.upper());
            slab=slab.add(upper.subtract(b.lower()).max(ZERO).multiply(b.rate()));
        }
        BigDecimal rebate=ZERO,relief=ZERO;
        if(regime==Regime.NEW) {
            if(total.compareTo(new BigDecimal("1200000"))<=0)rebate=slab.min(new BigDecimal("60000"));
            else relief=slab.subtract(total.subtract(new BigDecimal("1200000"))).max(ZERO);
        } else if(total.compareTo(new BigDecimal("500000"))<=0)rebate=slab.min(new BigDecimal("12500"));
        BigDecimal after=slab.subtract(rebate).subtract(relief),cess=after.multiply(new BigDecimal("0.04"));
        s.put(GROSS_SALARY,salary);s.put(STANDARD_DEDUCTION,standard);s.put(TAXABLE_SALARY,salary.subtract(standard));
        s.put(TOTAL_INTEREST,interest);s.put(GROSS_TOTAL_INCOME,gti);s.put(LIMITED_80C_CLAIM,c);s.put(LIMITED_80D_CLAIM,d);
        s.put(LIMITED_NPS_1B_CLAIM,n);s.put(LIMITED_80TTA_CLAIM,tta);s.put(LIMITED_80TTB_CLAIM,ttb);
        s.put(TOTAL_DEDUCTION_ALLOWED,deduction);s.put(TOTAL_INCOME_BEFORE_ROUNDING,before);s.put(ROUNDED_TOTAL_INCOME,total);
        s.put(SLAB_TAX,slab);s.put(REBATE,rebate);s.put(MARGINAL_RELIEF,relief);s.put(TAX_AFTER_RELIEF,after);s.put(CESS,cess);
        s.put(ANNUAL_TAX_BEFORE_CREDITS,after.add(cess));s.put(ZERO_CREDIT_ROUNDED_WORKING,roundTen(after.add(cess)));
        return new TaxStages(regime,age,s);
    }
    /** Sections 288A/288B: ignore paise first, then round the whole-rupee amount to tens. */
    static BigDecimal roundTen(BigDecimal amount) {
        BigDecimal whole=amount.setScale(0,RoundingMode.DOWN);
        return whole.add(new BigDecimal("5")).divideToIntegralValue(BigDecimal.TEN).multiply(BigDecimal.TEN);
    }
}
