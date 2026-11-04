package com.taxplatform.safety;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Bounded human amount parsing. Every successful result still requires confirmation. */
public final class HumanMoneyParser {
    public static final int MAX_INPUT_CHARACTERS = 64;
    private static final Pattern PREFIX = Pattern.compile("^(?:inr|rs\\.?)\\s*");
    private static final Pattern SUFFIX = Pattern.compile("\\s*(crores?|करोड़|करोड|lakhs?|lacs?|लाख|thousands?|k|l|rupees?|रुपये)$");
    private static final Pattern PLAIN = Pattern.compile("(?:0|[1-9][0-9]*)");
    private static final Pattern WESTERN = Pattern.compile("[1-9][0-9]{0,2}(?:,[0-9]{3})+");
    private static final Pattern INDIAN = Pattern.compile("[1-9][0-9]?(?:,[0-9]{2})*,[0-9]{3}");

    public enum Unit {
        RUPEES(0), THOUSAND(3), LAKH(5), CRORE(7);
        private final int decimalPower;
        Unit(int power) { this.decimalPower = power; }
    }
    public enum Problem {
        MISSING, TOO_LONG, UNIT_REQUIRED, AMBIGUOUS_UNIT, UNSUPPORTED_CURRENCY,
        MIXED_DIGIT_SCRIPTS, INVALID_NUMBER, INVALID_GROUPING, EXCESS_PRECISION, OUT_OF_RANGE
    }
    public sealed interface Outcome permits Parsed, Rejected { }
    public record Parsed(Money amount, Unit unit) implements Outcome {
        public Parsed { Objects.requireNonNull(amount); Objects.requireNonNull(unit); }
        public boolean requiresConfirmation() { return true; }
        public Fact<Money> asUnconfirmedFact() { return Fact.estimated(amount); }
        @Override public String toString() { return "ParsedAmount[redacted; confirmation required]"; }
    }
    public record Rejected(Problem problem) implements Outcome {
        public Rejected { Objects.requireNonNull(problem); }
    }

    public Outcome parse(String raw, Unit selectedMagnitude) {
        if (raw == null) return reject(Problem.MISSING);
        if (raw.length() > MAX_INPUT_CHARACTERS) return reject(Problem.TOO_LONG);
        if (raw.isBlank()) return reject(Problem.MISSING);
        String input = raw.replace('\u00a0', ' ').strip().toLowerCase(Locale.ROOT);
        if (input.isEmpty()) return reject(Problem.MISSING);
        if (input.startsWith("$") || input.startsWith("€") || input.startsWith("£")
                || input.startsWith("usd") || input.startsWith("eur") || input.startsWith("gbp")) {
            return reject(Problem.UNSUPPORTED_CURRENCY);
        }
        boolean rupeeCurrency = false;
        if (input.startsWith("₹")) { rupeeCurrency = true; input = input.substring(1).strip(); }
        else {
            Matcher prefix = PREFIX.matcher(input);
            if (prefix.find()) { rupeeCurrency = true; input = input.substring(prefix.end()).strip(); }
        }
        Unit suffixMagnitude = null;
        Matcher suffix = SUFFIX.matcher(input);
        if (suffix.find()) {
            suffixMagnitude = unitFor(suffix.group(1));
            input = input.substring(0, suffix.start()).strip();
        }
        boolean ascii = false, devanagari = false;
        StringBuilder normalized = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= '0' && c <= '9') ascii = true;
            else if (c >= '\u0966' && c <= '\u096f') { devanagari = true; c = (char) ('0' + c - '\u0966'); }
            normalized.append(c);
        }
        if (ascii && devanagari) return reject(Problem.MIXED_DIGIT_SCRIPTS);
        String number = normalized.toString();
        String[] pieces = number.split("\\.", -1);
        if (pieces.length > 2 || pieces[0].isEmpty()) return reject(Problem.INVALID_NUMBER);
        String integer = pieces[0];
        if (integer.indexOf(',') >= 0) {
            if (!WESTERN.matcher(integer).matches() && !INDIAN.matcher(integer).matches()) {
                return reject(Problem.INVALID_GROUPING);
            }
        } else if (!PLAIN.matcher(integer).matches()) return reject(Problem.INVALID_NUMBER);
        if (pieces.length == 2) {
            if (pieces[1].isEmpty() || !pieces[1].matches("[0-9]+")) return reject(Problem.INVALID_NUMBER);
            if (pieces[1].length() > 9) return reject(Problem.EXCESS_PRECISION);
        }
        if (suffixMagnitude != null && selectedMagnitude != null && suffixMagnitude != selectedMagnitude) {
            return reject(Problem.AMBIGUOUS_UNIT);
        }
        Unit unit = suffixMagnitude != null ? suffixMagnitude : selectedMagnitude;
        if (unit == null && rupeeCurrency) unit = Unit.RUPEES;
        if (unit == null) return reject(Problem.UNIT_REQUIRED);
        BigDecimal amount = new BigDecimal(number.replace(",", "")).movePointRight(unit.decimalPower);
        try { amount = amount.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException excessPaise) { return reject(Problem.EXCESS_PRECISION); }
        try { return new Parsed(new Money(amount), unit); }
        catch (IllegalArgumentException outOfRange) { return reject(Problem.OUT_OF_RANGE); }
    }

    private static Unit unitFor(String suffix) {
        return switch (suffix) {
            case "crore", "crores", "करोड़", "करोड" -> Unit.CRORE;
            case "lakh", "lakhs", "lac", "lacs", "लाख", "l" -> Unit.LAKH;
            case "thousand", "thousands", "k" -> Unit.THOUSAND;
            case "rupee", "rupees", "रुपये" -> Unit.RUPEES;
            default -> throw new IllegalArgumentException("unknown unit in fixed parser vocabulary");
        };
    }

    private static Rejected reject(Problem problem) { return new Rejected(problem); }
}
