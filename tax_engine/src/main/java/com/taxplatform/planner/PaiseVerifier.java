package com.taxplatform.planner;

import com.taxplatform.safety.Money;
import com.taxplatform.safety.SafetyInput.Regime;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.EnumMap;

import static com.taxplatform.planner.TaxStages.Stage.*;
import static com.taxplatform.planner.TaxStages.AgeBand.*;

/** Integer-paise verification with excess-income equations; no primary slabs or rounding helper. */
final class PaiseVerifier {
    private static long paise(Money money){return money.value().movePointRight(2).longValueExact();}
    private static long above(long income,long threshold){return Math.max(0,income-threshold);}
    private static long tenRupees(long cents){return ((cents/100+5)/10)*1000;}
    TaxStages calculate(PlannerInput in,Regime regime) {
        LocalDate born=in.dateOfBirth().confirmedValue().orElseThrow();
        int age=Period.between(born,LocalDate.of(2026,4,1)).getYears();
        TaxStages.AgeBand band=age>=80?SUPER_SENIOR:age>=60?SENIOR:UNDER_60;
        long salary=0;
        for(Money employer:in.salaries().confirmedValue().orElseThrow())salary=Math.addExact(salary,paise(employer));
        long sd=Math.min(salary,regime==Regime.NEW?7_500_000:5_000_000);
        long savings=paise(in.savings().confirmedValue().orElseThrow()),deposits=paise(in.deposits().confirmedValue().orElseThrow());
        long interest=Math.addExact(Math.addExact(savings,deposits),paise(in.otherInterest().confirmedValue().orElseThrow()));
        long gti=Math.addExact(salary-sd,interest);
        long c=0,d=0,n=0,tta=0,ttb=0;
        if(regime==Regime.OLD) {
            c=Math.min(paise(in.eligible80C().confirmedValue().orElseThrow()),15_000_000);
            d=paise(in.eligible80D().confirmedValue().orElseThrow());
            n=Math.min(paise(in.eligibleNps1B().confirmedValue().orElseThrow()),5_000_000);
            if(age<60)tta=Math.min(savings,1_000_000);else ttb=Math.min(Math.addExact(savings,deposits),5_000_000);
        }
        long deduct=Math.min(gti,c+d+n+tta+ttb),before=gti-deduct,rounded=tenRupees(before),rupees=rounded/100;
        long tax;
        if(regime==Regime.NEW)tax=5*(above(rupees,400_000)+above(rupees,800_000)+above(rupees,1_200_000)
                +above(rupees,1_600_000)+above(rupees,2_000_000)+above(rupees,2_400_000));
        else if(age>=80)tax=20*above(rupees,500_000)+10*above(rupees,1_000_000);
        else tax=5*above(rupees,age>=60?300_000:250_000)+15*above(rupees,500_000)+10*above(rupees,1_000_000);
        long rebate=0,relief=0;
        if(regime==Regime.NEW && rupees<=1_200_000)rebate=Math.min(tax,6_000_000);
        if(regime==Regime.NEW && rupees>1_200_000 && tax>rounded-120_000_000)relief=tax-(rounded-120_000_000);
        if(regime==Regime.OLD && rupees<=500_000)rebate=Math.min(tax,1_250_000);
        long after=tax-rebate-relief;
        // Rounded income is in tens and every supported tax rate is a multiple of 5%.
        // Tax is therefore in multiples of 50 paise; 4% cess is exact to paise.
        if(after%50!=0)throw new IllegalStateException("unexpected supported tax precision");
        long cess=(after/50)*2;
        EnumMap<TaxStages.Stage,BigDecimal> values=new EnumMap<>(TaxStages.Stage.class);
        put(values,GROSS_SALARY,salary);put(values,STANDARD_DEDUCTION,sd);put(values,TAXABLE_SALARY,salary-sd);
        put(values,TOTAL_INTEREST,interest);put(values,GROSS_TOTAL_INCOME,gti);put(values,LIMITED_80C_CLAIM,c);
        put(values,LIMITED_80D_CLAIM,d);put(values,LIMITED_NPS_1B_CLAIM,n);put(values,LIMITED_80TTA_CLAIM,tta);
        put(values,LIMITED_80TTB_CLAIM,ttb);put(values,TOTAL_DEDUCTION_ALLOWED,deduct);put(values,TOTAL_INCOME_BEFORE_ROUNDING,before);
        put(values,ROUNDED_TOTAL_INCOME,rounded);put(values,SLAB_TAX,tax);put(values,REBATE,rebate);put(values,MARGINAL_RELIEF,relief);
        put(values,TAX_AFTER_RELIEF,after);put(values,CESS,cess);put(values,ANNUAL_TAX_BEFORE_CREDITS,after+cess);
        put(values,ZERO_CREDIT_ROUNDED_WORKING,tenRupees(after+cess));return new TaxStages(regime,band,values);
    }
    private static void put(EnumMap<TaxStages.Stage,BigDecimal> into,TaxStages.Stage stage,long paise){into.put(stage,BigDecimal.valueOf(paise,2));}
}
