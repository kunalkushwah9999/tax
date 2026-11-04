package com.taxplatform.planner;

import com.taxplatform.safety.Fact;
import com.taxplatform.safety.Money;
import com.taxplatform.safety.SafetyInput.Regime;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Explicit case facts. This record contains no release, payment or approval authority. */
public record PlannerInput(String period, Residency residency, Fact<LocalDate> dateOfBirth,
        Fact<List<Money>> salaries, Fact<Money> savings, Fact<Money> deposits, Fact<Money> otherInterest,
        Fact<Money> eligible80C, Fact<Money> eligible80D, Fact<Money> eligibleNps1B,
        Fact<Boolean> incomeComplete, Fact<Boolean> deductionsComplete,
        Map<Exclusion,Answer> screening, Set<Regime> regimes) {
    public static final String PERIOD="ITA1961-FY2025-26-AY2026-27";
    public enum Residency { ROR, RNOR, NR, UNKNOWN }
    public enum Answer { NO, YES, UNKNOWN }
    public enum Exclusion { BUSINESS, SPECIAL_RATE, PROPERTY_FOREIGN_AGRICULTURE,
        RESTRICTED_STATUS, LOSSES, OTHER_ADJUSTMENTS }
    public PlannerInput {
        Objects.requireNonNull(period);Objects.requireNonNull(residency);Objects.requireNonNull(dateOfBirth);
        Objects.requireNonNull(salaries);Objects.requireNonNull(savings);Objects.requireNonNull(deposits);
        Objects.requireNonNull(otherInterest);Objects.requireNonNull(eligible80C);Objects.requireNonNull(eligible80D);
        Objects.requireNonNull(eligibleNps1B);Objects.requireNonNull(incomeComplete);Objects.requireNonNull(deductionsComplete);
        EnumMap<Exclusion,Answer> copy=new EnumMap<>(Exclusion.class);copy.putAll(screening);
        for(Exclusion key:Exclusion.values())Objects.requireNonNull(copy.get(key),"explicit screening answer required");
        screening=Map.copyOf(copy);regimes=Set.copyOf(regimes);
        if(period.length()>64 || regimes.isEmpty())throw new IllegalArgumentException("invalid planning scope");
        if(salaries instanceof Fact.Confirmed<?> f) salaries=Fact.confirmed(immutableSalaries(f.value()));
        else if(salaries instanceof Fact.Estimated<?> f) salaries=Fact.estimated(immutableSalaries(f.value()));
        else if(salaries.state()==Fact.State.CONFLICT)throw new IllegalArgumentException("resolve employer conflicts before this arithmetic adapter");
    }
    private static List<Money> immutableSalaries(Object object) {
        if(!(object instanceof List<?> values) || values.isEmpty() || values.size()>16
                || values.stream().anyMatch(v->!(v instanceof Money)))throw new IllegalArgumentException("one to sixteen employer amounts required");
        return values.stream().map(Money.class::cast).toList();
    }
    public PlannerInput with80C(Money value) {
        return new PlannerInput(period,residency,dateOfBirth,salaries,savings,deposits,otherInterest,
                Fact.confirmed(value),eligible80D,eligibleNps1B,incomeComplete,deductionsComplete,screening,regimes);
    }
    @Override public String toString(){return "PlannerInput[redacted]";}
}
