package com.taxplatform.safety;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Pattern;

/** Nonnegative transport amount; this class performs no statutory tax rounding. */
public record Money(BigDecimal value) implements Comparable<Money> {
    private static final Pattern CANONICAL = Pattern.compile("(?:0|[1-9][0-9]{0,12})(?:\\.[0-9]{1,2})?");

    public Money {
        Objects.requireNonNull(value, "amount");
        if (value.signum() < 0 || value.scale() < 0 || value.scale() > 2
                || value.precision() - value.scale() > 13) {
            throw new IllegalArgumentException("amount is outside the nonnegative money transport contract");
        }
        value = value.setScale(2, RoundingMode.UNNECESSARY);
    }

    public static Money parse(String text) {
        if (text == null || text.length() > 16 || !CANONICAL.matcher(text).matches()) {
            throw new IllegalArgumentException("canonical decimal amount required");
        }
        return new Money(new BigDecimal(text));
    }

    public String decimal() { return value.toPlainString(); }
    @Override public String toString() { return "Money[redacted]"; }
    @Override public int compareTo(Money other) { return value.compareTo(other.value); }

    public Money absoluteDifference(Money other) {
        return new Money(value.subtract(other.value).abs());
    }
}
