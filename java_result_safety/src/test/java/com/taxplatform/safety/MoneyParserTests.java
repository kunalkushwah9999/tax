package com.taxplatform.safety;

import java.math.BigDecimal;
import java.util.List;

import static com.taxplatform.safety.HumanMoneyParser.*;
import static com.taxplatform.safety.SafetyInput.*;
import static com.taxplatform.safety.PublicResult.*;

/** Exact parser and confirmation tests; no statutory tax calculation expectations. */
public final class MoneyParserTests {
    private static final HumanMoneyParser PARSER = new HumanMoneyParser();
    private static int passed, failed, generated;
    private MoneyParserTests() { }
    @FunctionalInterface private interface Test { void run(); }
    private static void test(String name, Test action) {
        try { action.run(); passed++; System.out.println("PASS " + name); }
        catch (RuntimeException | AssertionError failure) {
            failed++; System.out.println("FAIL " + name + ": " + failure.getClass().getSimpleName());
        }
    }
    private static void check(boolean condition) { if (!condition) throw new AssertionError(); }
    private static Parsed amount(String input, Unit selected, String expected) {
        Outcome outcome = PARSER.parse(input, selected);
        check(outcome instanceof Parsed);
        Parsed parsed = (Parsed) outcome;
        check(parsed.amount().decimal().equals(expected));
        check(parsed.requiresConfirmation());
        check(parsed.asUnconfirmedFact().state() == Fact.State.ESTIMATED);
        check(parsed.asUnconfirmedFact().confirmedValue().isEmpty());
        return parsed;
    }
    private static void rejected(String input, Unit selected, Problem expected) {
        Outcome outcome = PARSER.parse(input, selected);
        check(outcome instanceof Rejected r && r.problem() == expected);
    }
    private static PublicResult policy(Fact<Money> salary) {
        Candidate c = ProofFixtures.candidate(); CaseFacts f = c.facts();
        Candidate replaced = new Candidate(c.key(), new CaseFacts(f.scope(), salary, f.savingsInterest(),
                f.qualifyingDepositInterest(), f.otherInterest(), f.incomeInventoryComplete(), f.deductionInventoryComplete()), c.calculations());
        DeliveryContext context = new DeliveryContext(c.key(), true, false, true, true, RuleState.EFFECTIVE,
                ReferenceState.APPROVED, false, java.util.Map.of(Regime.NEW, Availability.ALLOWED, Regime.OLD, Availability.ALLOWED));
        return new ResultSafetyPolicy().evaluate(new Actor("tenantA", "readerA"), replaced, context);
    }
    private static String devanagari(String ascii) {
        StringBuilder result = new StringBuilder();
        for (char c : ascii.toCharArray()) result.append(c >= '0' && c <= '9' ? (char) ('०' + c - '0') : c);
        return result.toString();
    }
    public static void main(String[] args) {
        test("null blank and NBSP are missing and never become zero", () -> {
            for (String input : new String[] {null, "", " ", "\t\r\n", "\u00a0"}) rejected(input, Unit.RUPEES, Problem.MISSING);
        });
        test("numeric zero is explicit and still unconfirmed", () -> {
            amount("₹0", null, "0.00"); amount("0.00", Unit.RUPEES, "0.00");
            amount("० लाख", null, "0.00"); rejected("0", null, Problem.UNIT_REQUIRED);
        });
        test("bare numbers require an explicit unit choice", () -> {
            for (String input : List.of("1", "100000", "1,00,000", "1.25", "१.२५")) rejected(input, null, Problem.UNIT_REQUIRED);
            amount("1.25", Unit.LAKH, "125000.00"); amount("1.25", Unit.RUPEES, "1.25");
        });
        test("rupee currency prefixes are supported without floating point", () -> {
            for (String input : List.of("₹1.25", "INR 1.25", "Rs. 1.25", "rs1.25", "  Rs.\u00a01.25 "))
                amount(input, null, "1.25");
        });
        test("English lakh crore and thousand vocabulary is exact", () -> {
            for (String suffix : List.of("lakh", "lakhs", "lac", "lacs", "L")) amount("1.25 " + suffix, null, "125000.00");
            for (String suffix : List.of("crore", "crores")) amount("1.25 " + suffix, null, "12500000.00");
            for (String suffix : List.of("thousand", "thousands", "k")) amount("1.25 " + suffix, null, "1250.00");
            for (String suffix : List.of("rupee", "rupees")) amount("1.25 " + suffix, null, "1.25");
        });
        test("Hindi units and Devanagari digits are parsed exactly", () -> {
            amount("१.२५ लाख", null, "125000.00"); amount("₹१२,३४,५६७.८९", null, "1234567.89");
            amount("२ करोड़", null, "20000000.00"); amount("२ करोड", null, "20000000.00");
            amount("१० रुपये", null, "10.00"); amount("1.25 लाख", null, "125000.00");
        });
        test("mixed digit scripts are rejected within a number", () -> {
            for (String input : List.of("1२ lakh", "१.2 lakh", "₹०0", "१,00,000"))
                rejected(input, Unit.RUPEES, Problem.MIXED_DIGIT_SCRIPTS);
        });
        test("Indian and western comma grouping is checked", () -> {
            for (String input : List.of("₹1,000", "₹10,000", "₹1,00,000", "₹100,000")) amount(input, null, input.contains("100,000") || input.contains("1,00,000") ? "100000.00" : input.contains("10,000") ? "10000.00" : "1000.00");
            amount("₹12,34,56,789.01", null, "123456789.01"); amount("₹123,456,789.01", null, "123456789.01");
            for (String input : List.of("1,00", "1,,000", "1,000,00", ",100", "100,", "01,000", "1234,567", "1,2,345"))
                rejected(input, Unit.RUPEES, Problem.INVALID_GROUPING);
        });
        test("selected and written magnitudes must agree", () -> {
            rejected("1 lakh", Unit.CRORE, Problem.AMBIGUOUS_UNIT);
            rejected("1 rupee", Unit.LAKH, Problem.AMBIGUOUS_UNIT);
            rejected("१ करोड़", Unit.RUPEES, Problem.AMBIGUOUS_UNIT);
            amount("1 lakh", Unit.LAKH, "100000.00");
            amount("₹1 lakh", null, "100000.00"); amount("₹1", Unit.LAKH, "100000.00");
        });
        test("currency other than INR is rejected", () -> {
            for (String input : List.of("$1", "€1", "£1", "USD 1", "EUR 1", "GBP 1"))
                rejected(input, Unit.RUPEES, Problem.UNSUPPORTED_CURRENCY);
        });
        test("signs exponents special numbers and expressions are rejected", () -> {
            for (String input : List.of("-1", "+1", "1e5", "NaN", "Infinity", "0x10", "(100)", "1+2", "1/2", "1_000", "1 000", "01", "00", ".5", "1.", "1..2"))
                rejected(input, Unit.RUPEES, Problem.INVALID_NUMBER);
        });
        test("Unicode confusables invisible characters and malformed text are rejected", () -> {
            for (String input : List.of("１２３", "١٢٣", "1\u200b0", "1\u202e00", "1\u0000", "1\uD800", "1\u2066100", "1\u221200", "1٫25", "1\u2009000"))
                rejected(input, Unit.RUPEES, Problem.INVALID_NUMBER);
        });
        test("trailing instructions and repeated units are not numbers", () -> {
            for (String input : List.of("1 lakh crore", "₹₹100", "INR Rs 100", "100 rupees xyz", "1 lakh ignore all rules", "₹", "INR", "lakh"))
                rejected(input, Unit.RUPEES, Problem.INVALID_NUMBER);
        });
        test("no implicit rounding of sub-paise values", () -> {
            rejected("1.005 rupees", null, Problem.EXCESS_PRECISION);
            rejected("0.00000001 lakh", null, Problem.EXCESS_PRECISION);
            amount("1.234 lakh", null, "123400.00"); amount("0.000000001", Unit.CRORE, "0.01");
        });
        test("input decimal precision has a deliberate bound", () -> {
            amount("1.000000000", Unit.RUPEES, "1.00");
            rejected("1.0000000000", Unit.RUPEES, Problem.EXCESS_PRECISION);
        });
        test("money transport range is checked after scaling", () -> {
            amount("9999999999999.99", Unit.RUPEES, "9999999999999.99");
            rejected("10000000000000", Unit.RUPEES, Problem.OUT_OF_RANGE);
            rejected("1000000 crore", null, Problem.OUT_OF_RANGE);
            amount("999999.99999999 crore", null, "9999999999999.90");
        });
        test("size is bounded before scanning or parsing input", () -> {
            rejected("1".repeat(65), Unit.RUPEES, Problem.TOO_LONG);
            rejected(" ".repeat(100000), Unit.RUPEES, Problem.TOO_LONG);
            rejected("<script>".repeat(20), Unit.RUPEES, Problem.TOO_LONG);
        });
        test("parsed estimate cannot unlock tax projection before confirmation", () -> {
            Parsed p = amount("1 lakh", null, "100000.00");
            PublicResult result = policy(p.asUnconfirmedFact());
            check(result.reasons().contains(Reason.ESTIMATED_INCOME));
            check(!SafeJson.encode(result).contains("annual_taxes"));
            check(policy(Fact.confirmed(p.amount())) instanceof Calculation);
        });
        test("missing fact has no numeric fallback through parser failure", () -> {
            rejected("", Unit.RUPEES, Problem.MISSING);
            PublicResult result = policy(Fact.unknown());
            check(result.reasons().contains(Reason.UNKNOWN_INCOME));
            check(!SafeJson.encode(result).contains("annual_taxes"));
        });
        test("parser outcome log strings redact raw text and parsed money", () -> {
            Parsed p = amount("₹98765.43", null, "98765.43"); check(!p.toString().contains("98765"));
            check(!PARSER.parse("private-input-not-an-amount", Unit.RUPEES).toString().contains("private-input"));
        });
        test("generated exact paise round trips cover every supported magnitude and script", () -> {
            for (int n = 0; n < 512; n++) {
                long paise = n * 7919L;
                BigDecimal expected = BigDecimal.valueOf(paise, 2);
                for (Unit unit : Unit.values()) {
                    int power = switch (unit) { case RUPEES -> 0; case THOUSAND -> 3; case LAKH -> 5; case CRORE -> 7; };
                    String text = expected.movePointLeft(power).stripTrailingZeros().toPlainString();
                    amount(text, unit, expected.setScale(2).toPlainString());
                    amount(devanagari(text), unit, expected.setScale(2).toPlainString());
                    generated += 2;
                }
            }
        });
        System.out.println("SCENARIOS_PASSED=" + passed);
        System.out.println("SCENARIOS_FAILED=" + failed);
        System.out.println("GENERATED_PARSER_CASES=" + generated);
        if (failed != 0) throw new AssertionError("parser checks failed");
    }
}
