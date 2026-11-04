package com.taxplatform.planner;

import com.taxplatform.safety.Fact;
import com.taxplatform.safety.Money;
import com.taxplatform.safety.SafetyInput.Regime;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/** Canonical INR amounts, explicit unknowns and confirmed review intent; no client approval switches. */
public final class PlannerForms {
    private PlannerForms(){ }
    public static final Set<String> FIELDS=Set.of("period","residency","dob","dob_state","salaries","savings","deposits","other_interest",
            "salary_state","interest_state","eligible_80c","eligible_80d","eligible_nps","deduction_state","income_inventory","deduction_inventory",
            "screen_business","screen_special","screen_property_foreign","screen_status","screen_losses","screen_adjustments","regimes");
    public static PlannerInput parse(Map<String,String> form) {
        if(!form.keySet().equals(FIELDS))throw new IllegalArgumentException("exact planning fields required");
        String income=form.get("salary_state"),interest=form.get("interest_state"),deduction=form.get("deduction_state");
        Fact<java.util.List<Money>> salary;
        if(income.equals("UNKNOWN")) {if(!form.get("salaries").equals("UNKNOWN"))throw new IllegalArgumentException("unknown cannot carry salary values");salary=Fact.unknown();}
        else {
            java.util.List<Money> values=Arrays.stream(form.get("salaries").split(";",-1)).map(Money::parse).toList();
            salary=income.equals("CONFIRMED")?Fact.confirmed(values):income.equals("ESTIMATED")?Fact.estimated(values):invalidState();
        }
        Fact<LocalDate> dob;
        if(form.get("dob_state").equals("UNKNOWN")){if(!form.get("dob").equals("UNKNOWN"))throw new IllegalArgumentException("unknown cannot carry DOB");dob=Fact.unknown();}
        else {String date=form.get("dob");if(!date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))throw new IllegalArgumentException("ISO date required");LocalDate parsed;
            try{parsed=LocalDate.parse(date);}catch(java.time.DateTimeException invalid){throw new IllegalArgumentException("valid calendar date required");}
            dob=form.get("dob_state").equals("CONFIRMED")?Fact.confirmed(parsed):form.get("dob_state").equals("ESTIMATED")?Fact.estimated(parsed):invalidState();}
        return new PlannerInput(form.get("period"),PlannerInput.Residency.valueOf(form.get("residency")),dob,salary,
                money(form.get("savings"),interest),money(form.get("deposits"),interest),money(form.get("other_interest"),interest),
                money(form.get("eligible_80c"),deduction),money(form.get("eligible_80d"),deduction),money(form.get("eligible_nps"),deduction),
                inventory(form.get("income_inventory")),inventory(form.get("deduction_inventory")),Map.of(
                    PlannerInput.Exclusion.BUSINESS,answer(form,"screen_business"),PlannerInput.Exclusion.SPECIAL_RATE,answer(form,"screen_special"),
                    PlannerInput.Exclusion.PROPERTY_FOREIGN_AGRICULTURE,answer(form,"screen_property_foreign"),PlannerInput.Exclusion.RESTRICTED_STATUS,answer(form,"screen_status"),
                    PlannerInput.Exclusion.LOSSES,answer(form,"screen_losses"),PlannerInput.Exclusion.OTHER_ADJUSTMENTS,answer(form,"screen_adjustments")),
                switch(form.get("regimes")){case "NEW"->Set.of(Regime.NEW);case "OLD"->Set.of(Regime.OLD);case "BOTH"->Set.of(Regime.NEW,Regime.OLD);default->throw new IllegalArgumentException("invalid regime set");});
    }
    private static PlannerInput.Answer answer(Map<String,String> form,String key){return PlannerInput.Answer.valueOf(form.get(key));}
    private static <T> Fact<T> invalidState(){throw new IllegalArgumentException("unsupported fact state");}
    private static Fact<Money> money(String text,String state) {
        return switch(state){case "UNKNOWN"->{if(!text.equals("UNKNOWN"))throw new IllegalArgumentException("unknown cannot carry amount");yield Fact.unknown();}
            case "CONFIRMED"->Fact.confirmed(Money.parse(text));case "ESTIMATED"->Fact.estimated(Money.parse(text));default->invalidState();};
    }
    private static Fact<Boolean> inventory(String state){return switch(state){case "REVIEWED"->Fact.confirmed(true);case "INCOMPLETE"->Fact.confirmed(false);case "UNKNOWN"->Fact.unknown();default->invalidState();};}
}
