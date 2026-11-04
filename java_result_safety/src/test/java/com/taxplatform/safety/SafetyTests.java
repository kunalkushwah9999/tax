package com.taxplatform.safety;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import static com.taxplatform.safety.SafetyInput.*;
import static com.taxplatform.safety.PublicResult.*;

/** Policy/serialization tests only. Amounts below are arbitrary synthetic fixtures. */
public final class SafetyTests {
    private static int passed;
    private static int failed;
    private static int generatedCases;

    private SafetyTests() { }

    @FunctionalInterface private interface Test { void run() throws Exception; }

    private static void test(String name, Test action) {
        try { action.run(); passed++; System.out.println("PASS " + name); }
        catch (Exception | AssertionError error) {
            failed++; System.out.println("FAIL " + name + ": " + error.getClass().getSimpleName());
        }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void reason(PublicResult result, Reason reason) {
        check(result.reasons().contains(reason), "expected reason " + reason);
    }

    private static void noNumbers(PublicResult result) {
        String json = SafeJson.encode(result);
        check(!json.contains("\"annual_taxes\""), "tax payload leaked");
        check(!json.contains("\"comparison\""), "comparison leaked");
        check(!json.contains("98765.43"), "private input leaked");
        check(!json.contains("420.00") && !json.contains("700.00"), "candidate amounts leaked");
    }

    private static final class Scenario {
        CalculationKey key = key("tenantA", "profileA", "caseA", "snap1", 'a');
        CalculationKey currentKey = key;
        Actor actor = new Actor("tenantA", "readerA");
        Scope scope = Scope.SUPPORTED;
        Fact<Money> salary = Fact.confirmed(Money.parse("98765.43"));
        Fact<Money> savings = Fact.confirmed(Money.parse("0"));
        Fact<Money> deposits = Fact.confirmed(Money.parse("0"));
        Fact<Money> other = Fact.confirmed(Money.parse("0"));
        Fact<Boolean> inventory = Fact.confirmed(true);
        final Map<Regime, Fact<Boolean>> deductions = new EnumMap<>(Regime.class);
        final Map<Regime, Availability> availability = new EnumMap<>(Regime.class);
        final Map<Regime, RegimeCalculation> calculations = new EnumMap<>(Regime.class);
        boolean authorized = true, deleted, integrity = true, restored = true, quarantined;
        RuleState rule = RuleState.EFFECTIVE;
        ReferenceState reference = ReferenceState.APPROVED;

        Scenario() {
            deductions.put(Regime.NEW, Fact.confirmed(true)); deductions.put(Regime.OLD, Fact.confirmed(true));
            availability.put(Regime.NEW, Availability.ALLOWED); availability.put(Regime.OLD, Availability.ALLOWED);
            calculations.put(Regime.NEW, new RegimeCalculation(Regime.NEW, Money.parse("420"), Verification.MATCH));
            calculations.put(Regime.OLD, new RegimeCalculation(Regime.OLD, Money.parse("700"), Verification.MATCH));
        }

        Candidate candidate() {
            return new Candidate(key, new CaseFacts(scope, salary, savings, deposits, other, inventory, deductions), calculations);
        }
        DeliveryContext context() {
            return new DeliveryContext(currentKey, authorized, deleted, integrity, restored, rule, reference, quarantined, availability);
        }
        PublicResult evaluate() { return new ResultSafetyPolicy().evaluate(actor, candidate(), context()); }
    }

    private static CalculationKey key(String tenant, String profile, String id, String snap, char hash) {
        return new CalculationKey(tenant, profile, id, snap, "ITA1961-AY2026-27",
                String.valueOf(hash).repeat(64), "b".repeat(64), "c".repeat(64), "facts-v1", "d".repeat(64));
    }

    private static final class TestControls implements SafeResultService.ControlPlane {
        final Scenario scenario;
        final Set<CalculationKey> quarantine = new HashSet<>();
        int reads, writes;
        boolean failRead, failWrite, returnNull;

        TestControls(Scenario scenario) { this.scenario = scenario; }
        @Override public DeliveryContext loadFor(Actor actor, Candidate candidate)
                throws SafeResultService.ControlStateUnavailable {
            reads++;
            if (failRead) throw new SafeResultService.ControlStateUnavailable();
            if (returnNull) return null;
            DeliveryContext c = scenario.context();
            return new DeliveryContext(c.currentKey(), c.authorized(), c.deleted(), c.payloadIntegrityVerified(),
                    c.restoreCaughtUp(), c.ruleState(), c.referenceState(),
                    c.quarantined() || quarantine.contains(candidate.key()), c.regimeAvailability());
        }
        @Override public void quarantine(CalculationKey key) throws SafeResultService.ControlStateUnavailable {
            writes++;
            if (failWrite) throw new SafeResultService.ControlStateUnavailable();
            quarantine.add(key);
        }
    }

    private static boolean compiles(String className, String source) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        check(compiler != null, "compiler required for negative compilation tests");
        JavaFileObject unit = new SimpleJavaFileObject(URI.create("string:///" + className + ".java"), JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return source; }
        };
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        return Boolean.TRUE.equals(compiler.getTask(null, null, diagnostics,
                List.of("-proc:none", "--release", "17", "-classpath", System.getProperty("java.class.path")),
                null, List.of(unit)).call());
    }

    private static void expectInvalid(Test operation) throws Exception {
        boolean rejected = false;
        try { operation.run(); } catch (IllegalArgumentException | NullPointerException expected) { rejected = true; }
        check(rejected, "invalid domain state accepted");
    }

    private static void scenarios() {
        test("confirmed zero is distinct from unknown", () -> {
            check(Fact.confirmed(Money.parse("0")).confirmedValue().isPresent(), "zero lost");
            check(Fact.<Money>unknown().confirmedValue().isEmpty(), "unknown invented a value");
        });
        test("null confirmed values rejected", () -> expectInvalid(() -> Fact.confirmed(null)));
        test("domain toString does not disclose financial facts or identity", () -> {
            Scenario s = new Scenario();
            String text = s.candidate().toString() + s.context() + s.salary + Money.parse("98765.43")
                    + Fact.estimated("PRIVATE_INPUT") + Fact.conflict(List.of("PRIVATE_INPUT", "PRIVATE_OTHER"));
            check(!text.contains("98765.43") && !text.contains("420.00") && !text.contains("tenantA")
                    && !text.contains("PRIVATE_INPUT") && !text.contains("PRIVATE_OTHER"), "domain logging leak");
        });
        test("noncanonical money rejected without rounding", () -> {
            for (String text : new String[]{"", " ", "-1", "-0", "NaN", "Infinity", "1e3", "01", "+1", "1.001", "12,000", "12 lakh", "10000000000000"}) {
                expectInvalid(() -> Money.parse(text));
            }
            check(Money.parse("1.2").decimal().equals("1.20"), "canonical amount changed");
        });
        test("unknown has no numeric accessor", () -> check(!compiles("UnknownAccessor",
                "import com.taxplatform.safety.*; class UnknownAccessor { void x() { var f = new Fact.Unknown<Money>(); f.value(); } }"), "unknown accessor compiled"));
        test("signed service has no raw candidate delivery overload", () -> check(!compiles("UnsignedDelivery",
                "import com.taxplatform.safety.*; class UnsignedDelivery { void x(SignedResultService s, SafetyInput.Actor a, SafetyInput.Candidate c) { s.deliver(a,c); } }"), "unsigned overload compiled"));
        test("guided service has no raw candidate delivery overload", () -> check(!compiles("UnsignedGuide",
                "import com.taxplatform.safety.*; class UnsignedGuide { void x(GuidedResultService s, SafetyInput.Actor a, SafetyInput.Candidate c) { s.deliver(a,c); } }"), "unsigned guide compiled"));
        test("intake service has no raw candidate delivery overload", () -> check(!compiles("UnsignedIntake",
                "import com.taxplatform.safety.*; class UnsignedIntake { void x(IntakeGuideService s, SafetyInput.Actor a, SafetyInput.Candidate c) { s.deliver(a,c); } }"), "unsigned intake compiled"));
        test("external caller cannot directly construct guided projections", () -> {
            check(!compiles("ForgedGuide", "import com.taxplatform.safety.*; import java.util.*; class ForgedGuide { void x(PublicResult r) { new GuidedResult(r,Map.of()); } }"), "guide constructor exposed");
            check(!compiles("ForgedIntake", "import com.taxplatform.safety.*; import java.util.*; class ForgedIntake { void x() { new IntakeGuide(\"INPUT_PREPARATION_AVAILABLE\",Map.of()); } }"), "intake constructor exposed");
        });
        test("external caller cannot forge edit receipts or commands", () -> {
            check(!compiles("ForgedEditReceipt", "import com.taxplatform.safety.*; class ForgedEditReceipt { void x() { new FactEditResult(FactEditResult.Status.APPLIED,\"snap\",true,null); } }"), "edit receipt constructor exposed");
            check(!compiles("ForgedEditCommand", "import com.taxplatform.safety.*; class ForgedEditCommand implements FactEdits.Command {}"), "unpermitted command compiled");
        });
        test("external caller cannot forge a professional queue projection", () -> check(!compiles("ForgedProfessionalQueue",
                "import com.taxplatform.safety.*; import java.util.*; class ForgedProfessionalQueue { void x() { new ProfessionalQueue(List.of(),Map.of()); } }"), "professional queue constructor exposed"));
        test("external implementations of result hierarchy rejected", () -> check(!compiles("ForgedResult",
                "import com.taxplatform.safety.*; import java.util.*; final class ForgedResult implements PublicResult { public Map<String,Object> toResponse(){return Map.of();} public List<PublicResult.Reason> reasons(){return List.of();} }"), "unpermitted result compiled"));
        test("public calculation cannot be directly forged from an external package", () -> check(!compiles("ConstructedResult",
                "import com.taxplatform.safety.*; import java.util.*; class ConstructedResult { Object x() { return new PublicResult.Calculation(List.of(), PublicResult.ComparisonStatus.PROVISIONAL, null, List.of()); } }"), "restricted constructor became public"));
        test("all required facts allow synthetic definitive comparison", () -> {
            Map<String, Object> p = new Scenario().evaluate().toResponse();
            check(p.get("status").equals("CALCULATION_AVAILABLE"), "result hidden");
            check(p.get("comparison_status").equals("DEFINITIVE"), "comparison not definitive");
            Object comparison = p.get("comparison");
            check(comparison instanceof Map<?, ?> m && "NEW".equals(m.get("recommendation"))
                    && "280.00".equals(m.get("difference_in_annual_tax")), "comparison arithmetic changed");
        });
        test("tie remains explicit tie", () -> {
            Scenario s = new Scenario();
            s.calculations.put(Regime.OLD, new RegimeCalculation(Regime.OLD, Money.parse("420"), Verification.MATCH));
            Object c = s.evaluate().toResponse().get("comparison");
            check(c instanceof Map<?, ?> m && "TIE".equals(m.get("recommendation"))
                    && "0.00".equals(m.get("difference_in_annual_tax")), "tie converted to saving");
        });
        test("old cheaper is selected only in a definitive eligible comparison", () -> {
            Scenario s = new Scenario();
            s.calculations.put(Regime.OLD, new RegimeCalculation(Regime.OLD, Money.parse("1"), Verification.MATCH));
            Object c = s.evaluate().toResponse().get("comparison");
            check(c instanceof Map<?, ?> m && "OLD".equals(m.get("recommendation")), "lower eligible result not selected");
        });
        test("pending independent references block every amount", () -> {
            Scenario s = new Scenario(); s.reference = ReferenceState.PENDING;
            PublicResult p = s.evaluate(); reason(p, Reason.INDEPENDENT_REFERENCE_NOT_APPROVED); noNumbers(p);
        });
        test("rejected independent references block every amount", () -> {
            Scenario s = new Scenario(); s.reference = ReferenceState.REJECTED;
            noNumbers(s.evaluate());
        });
        test("all noneffective rule states block delivery", () -> {
            for (RuleState state : List.of(RuleState.UNAPPROVED, RuleState.RECALLED, RuleState.MISSING)) {
                Scenario s = new Scenario(); s.rule = state;
                PublicResult p = s.evaluate(); reason(p, Reason.RULE_NOT_EFFECTIVE); noNumbers(p);
            }
        });
        test("unknown salary cannot become zero", () -> {
            Scenario s = new Scenario(); s.salary = Fact.unknown();
            PublicResult p = s.evaluate(); reason(p, Reason.UNKNOWN_INCOME); noNumbers(p);
        });
        test("unknown bank interest cannot become zero", () -> {
            Scenario s = new Scenario(); s.deposits = Fact.unknown();
            PublicResult p = s.evaluate(); reason(p, Reason.UNKNOWN_INCOME); noNumbers(p);
        });
        test("estimated income needs confirmation", () -> {
            Scenario s = new Scenario(); s.other = Fact.estimated(Money.parse("0"));
            PublicResult p = s.evaluate(); reason(p, Reason.ESTIMATED_INCOME); noNumbers(p);
        });
        test("conflicting interest blocks numbers", () -> {
            Scenario s = new Scenario(); s.savings = Fact.conflict(List.of(Money.parse("0"), Money.parse("1")));
            PublicResult p = s.evaluate(); reason(p, Reason.CONFLICTING_INCOME); noNumbers(p);
        });
        test("an unconfirmed income inventory blocks complete result", () -> {
            for (Fact<Boolean> inventory : List.of(Fact.confirmed(false), Fact.<Boolean>unknown(), Fact.estimated(true))) {
                Scenario s = new Scenario(); s.inventory = inventory;
                PublicResult p = s.evaluate(); reason(p, Reason.INCOME_INVENTORY_INCOMPLETE); noNumbers(p);
            }
        });
        test("all unsupported or unknown scope states suppress numbers", () -> {
            for (Scope scope : List.of(Scope.PARTIAL, Scope.UNSUPPORTED, Scope.UNKNOWN)) {
                Scenario s = new Scenario(); s.scope = scope; noNumbers(s.evaluate());
            }
        });
        test("unknown old deductions preserve only confirmed new calculation", () -> {
            Scenario s = new Scenario(); s.deductions.put(Regime.OLD, Fact.unknown());
            Map<String, Object> p = s.evaluate().toResponse();
            check(p.get("comparison_status").equals("PROVISIONAL"), "provisional state lost");
            check(!p.containsKey("comparison"), "definitive saving leaked");
            String json = SafeJson.encode(s.evaluate());
            check(json.contains("420.00") && !json.contains("700.00"), "incomplete regime amount leaked");
        });
        test("all unknown deductions expose no complete taxes", () -> {
            Scenario s = new Scenario();
            for (Regime regime : Regime.values()) s.deductions.put(regime, Fact.unknown());
            noNumbers(s.evaluate());
        });
        test("unavailable old option is never recommended or exposed as actionable", () -> {
            Scenario s = new Scenario(); s.availability.put(Regime.OLD, Availability.NOT_ALLOWED);
            s.calculations.put(Regime.OLD, new RegimeCalculation(Regime.OLD, Money.parse("1"), Verification.MATCH));
            Map<String, Object> p = s.evaluate().toResponse();
            check("NOT_AVAILABLE".equals(p.get("comparison_status")) && !p.containsKey("comparison"), "unavailable regime selected");
            check(!SafeJson.encode(s.evaluate()).contains("1.00"), "unavailable result exposed");
        });
        test("unknown option availability prevents definitive comparison", () -> {
            Scenario s = new Scenario(); s.availability.put(Regime.OLD, Availability.UNKNOWN);
            Map<String, Object> p = s.evaluate().toResponse();
            check("PROVISIONAL".equals(p.get("comparison_status")) && !p.containsKey("comparison"), "unknown legal option treated as allowed");
        });
        test("no available regime exposes no taxes", () -> {
            Scenario s = new Scenario();
            for (Regime regime : Regime.values()) s.availability.put(regime, Availability.NOT_ALLOWED);
            noNumbers(s.evaluate());
        });
        test("either regime mismatch blocks the entire result", () -> {
            for (Regime regime : Regime.values()) {
                Scenario s = new Scenario(); s.calculations.put(regime,
                        new RegimeCalculation(regime, Money.parse("0"), Verification.MISMATCH));
                PublicResult p = s.evaluate(); reason(p, Reason.VERIFIER_MISMATCH); noNumbers(p);
            }
        });
        test("not run verification cannot expose a candidate amount", () -> {
            Scenario s = new Scenario(); s.calculations.put(Regime.NEW,
                    new RegimeCalculation(Regime.NEW, Money.parse("420"), Verification.NOT_RUN));
            PublicResult p = s.evaluate(); reason(p, Reason.VERIFICATION_NOT_RUN); noNumbers(p);
        });
        test("missing calculation cannot create a definitive comparison", () -> {
            Scenario s = new Scenario(); s.calculations.remove(Regime.OLD);
            check(!s.evaluate().toResponse().containsKey("comparison"), "missing regime fabricated");
        });
        test("empty candidate is not zero tax", () -> {
            Scenario s = new Scenario(); s.calculations.clear();
            PublicResult p = s.evaluate(); reason(p, Reason.NO_CALCULATIONS); noNumbers(p);
        });
        test("cross tenant denied even when adapter claims authorization", () -> {
            Scenario s = new Scenario(); s.actor = new Actor("tenantB", "readerA");
            PublicResult p = s.evaluate(); check(p instanceof Denied, "cross tenant allowed"); noNumbers(p);
        });
        test("different profile or case in context is not merely stale", () -> {
            Scenario s = new Scenario(); s.currentKey = key("tenantA", "profileB", "caseB", "snap1", 'a');
            PublicResult p = s.evaluate(); reason(p, Reason.CONTEXT_BINDING_INVALID); noNumbers(p);
        });
        test("snapshot change invalidates cached result", () -> {
            Scenario s = new Scenario(); s.currentKey = key("tenantA", "profileA", "caseA", "snap2", 'a');
            PublicResult p = s.evaluate(); reason(p, Reason.STALE_RESULT); noNumbers(p);
        });
        test("validation release change invalidates cached result", () -> {
            Scenario s = new Scenario(); s.currentKey = key("tenantA", "profileA", "caseA", "snap1", 'e');
            PublicResult p = s.evaluate(); reason(p, Reason.STALE_RESULT); noNumbers(p);
        });
        test("unauthorized and deleted responses are generic", () -> {
            Scenario a = new Scenario(); a.authorized = false;
            Scenario b = new Scenario(); b.deleted = true; b.reference = ReferenceState.PENDING;
            check(SafeJson.encode(a.evaluate()).equals(SafeJson.encode(b.evaluate())), "existence/control state leaked");
            noNumbers(a.evaluate()); noNumbers(b.evaluate());
        });
        test("payload integrity failure blocks every amount", () -> {
            Scenario s = new Scenario(); s.integrity = false;
            PublicResult p = s.evaluate(); reason(p, Reason.PAYLOAD_INTEGRITY_FAILED); noNumbers(p);
        });
        test("restore gate blocks delivery", () -> {
            Scenario s = new Scenario(); s.restored = false;
            PublicResult p = s.evaluate(); reason(p, Reason.RESTORE_NOT_CAUGHT_UP); noNumbers(p);
        });
        test("quarantined identity blocks cached result", () -> {
            Scenario s = new Scenario(); s.quarantined = true;
            PublicResult p = s.evaluate(); reason(p, Reason.QUARANTINED); noNumbers(p);
        });
        test("service reloads permissions on every cached delivery", () -> {
            Scenario s = new Scenario(); TestControls controls = new TestControls(s);
            SafeResultService service = new SafeResultService(controls); Candidate cached = s.candidate();
            check(service.deliver(s.actor, cached) instanceof Calculation, "initial result unavailable");
            s.authorized = false;
            check(service.deliver(s.actor, cached) instanceof Denied, "cached permission reused");
            check(controls.reads == 2, "control state was cached");
        });
        test("service records mismatch quarantine and hides older matching cache", () -> {
            Scenario s = new Scenario(); TestControls controls = new TestControls(s);
            SafeResultService service = new SafeResultService(controls); Candidate prior = s.candidate();
            check(service.deliver(s.actor, prior) instanceof Calculation, "initial result unavailable");
            s.calculations.put(Regime.OLD, new RegimeCalculation(Regime.OLD, Money.parse("700"), Verification.MISMATCH));
            reason(service.deliver(s.actor, s.candidate()), Reason.VERIFIER_MISMATCH);
            PublicResult old = service.deliver(s.actor, prior); reason(old, Reason.QUARANTINED); noNumbers(old);
            check(controls.quarantine.contains(prior.key()) && controls.writes == 1, "identity not quarantined");
        });
        test("mismatch quarantine is not skipped while independent references are pending", () -> {
            Scenario s = new Scenario(); TestControls controls = new TestControls(s);
            SafeResultService service = new SafeResultService(controls); Candidate prior = s.candidate();
            check(service.deliver(s.actor, prior) instanceof Calculation, "initial cache unavailable");
            s.reference = ReferenceState.PENDING;
            s.calculations.put(Regime.OLD, new RegimeCalculation(Regime.OLD, Money.parse("700"), Verification.MISMATCH));
            service.deliver(s.actor, s.candidate());
            check(controls.quarantine.contains(prior.key()), "authentic mismatch evidence lost behind approval gate");
            s.reference = ReferenceState.APPROVED;
            reason(service.deliver(s.actor, prior), Reason.QUARANTINED);
        });
        test("control read outage fails closed", () -> {
            Scenario s = new Scenario(); TestControls controls = new TestControls(s); controls.failRead = true;
            PublicResult p = new SafeResultService(controls).deliver(s.actor, s.candidate());
            reason(p, Reason.CONTROL_STATE_UNAVAILABLE); noNumbers(p);
        });
        test("null control state fails closed", () -> {
            Scenario s = new Scenario(); TestControls controls = new TestControls(s); controls.returnNull = true;
            noNumbers(new SafeResultService(controls).deliver(s.actor, s.candidate()));
        });
        test("quarantine write failure does not leak a fallback answer", () -> {
            Scenario s = new Scenario(); s.calculations.put(Regime.NEW,
                    new RegimeCalculation(Regime.NEW, Money.parse("420"), Verification.MISMATCH));
            TestControls controls = new TestControls(s); controls.failWrite = true;
            PublicResult p = new SafeResultService(controls).deliver(s.actor, s.candidate());
            reason(p, Reason.CONTROL_STATE_UNAVAILABLE); noNumbers(p);
        });
        test("cross tenant service does not even read private control state", () -> {
            Scenario s = new Scenario(); s.actor = new Actor("tenantB", "readerA");
            TestControls controls = new TestControls(s);
            check(new SafeResultService(controls).deliver(s.actor, s.candidate()) instanceof Denied, "cross tenant leak");
            check(controls.reads == 0 && controls.writes == 0, "private adapter invoked");
        });
        test("untrusted or unauthorized mismatch cannot quarantine another case", () -> {
            Scenario s = new Scenario(); s.authorized = false;
            s.calculations.put(Regime.NEW, new RegimeCalculation(Regime.NEW, Money.parse("420"), Verification.MISMATCH));
            TestControls controls = new TestControls(s);
            new SafeResultService(controls).deliver(s.actor, s.candidate());
            check(controls.writes == 0, "unauthorized quarantine side effect");
        });
        test("candidate and fact maps are defensive snapshots", () -> {
            Scenario s = new Scenario(); Candidate original = s.candidate();
            s.calculations.clear(); s.deductions.put(Regime.OLD, Fact.unknown());
            check(original.calculations().size() == 2, "candidate map mutated");
            check(original.facts().deductionInventoryComplete().get(Regime.OLD).confirmedValue().orElse(false), "fact map mutated");
        });
        test("inconsistent regime map rejected", () -> {
            Scenario s = new Scenario(); s.calculations.put(Regime.OLD,
                    new RegimeCalculation(Regime.NEW, Money.parse("1"), Verification.MATCH));
            expectInvalid(s::candidate);
        });
        test("explicit availability and deduction states are mandatory", () -> {
            Scenario s = new Scenario(); s.availability.remove(Regime.OLD); expectInvalid(s::context);
            Scenario t = new Scenario(); t.deductions.remove(Regime.OLD); expectInvalid(t::candidate);
        });
        test("public responses are immutable", () -> {
            Map<String, Object> map = new Scenario().evaluate().toResponse();
            boolean rejected = false;
            try { map.put("submit_return", true); } catch (UnsupportedOperationException expected) { rejected = true; }
            check(rejected, "public projection mutable");
        });
        test("projection field ordering is deterministic", () -> {
            Map<String, Object> p = new Scenario().evaluate().toResponse();
            List<String> keys = new ArrayList<>(p.keySet());
            check(keys.equals(keys.stream().sorted().toList()), "projection fields unsorted");
            check(SafeJson.encode(new Scenario().evaluate()).equals(SafeJson.encode(new Scenario().evaluate())), "identical projections differ");
        });
        test("filing and export remain disabled even for all-green fixture", () -> {
            Object capabilities = new Scenario().evaluate().toResponse().get("capabilities");
            check(capabilities instanceof Map<?, ?> m && Boolean.FALSE.equals(m.get("export_itr"))
                    && Boolean.FALSE.equals(m.get("submit_return")), "calculation became filing authority");
        });
        test("2048 combined guard states preserve no-leak invariant", SafetyTests::guardCombinations);
    }

    private static void guardCombinations() {
        for (int mask = 0; mask < 2048; mask++) {
            Scenario s = new Scenario();
            s.authorized = (mask & 1) != 0; s.deleted = (mask & 2) == 0;
            s.integrity = (mask & 4) != 0; s.restored = (mask & 8) != 0;
            s.quarantined = (mask & 16) == 0;
            if ((mask & 32) == 0) s.currentKey = key("tenantA", "profileA", "caseA", "snap2", 'a');
            if ((mask & 64) == 0) s.rule = RuleState.RECALLED;
            if ((mask & 128) == 0) s.reference = ReferenceState.PENDING;
            if ((mask & 256) == 0) s.scope = Scope.PARTIAL;
            if ((mask & 512) == 0) s.deposits = Fact.unknown();
            if ((mask & 1024) == 0) s.inventory = Fact.confirmed(false);
            PublicResult p = s.evaluate();
            if (mask == 2047) check(p instanceof Calculation, "all-green denied");
            else noNumbers(p);
            generatedCases++;
        }
    }

    private static void writeExamples(Path output) throws IOException {
        List<String> examples = new ArrayList<>();
        Scenario unknown = new Scenario(); unknown.deposits = Fact.unknown();
        examples.add(example("unknown_interest", unknown));
        Scenario provisional = new Scenario(); provisional.deductions.put(Regime.OLD, Fact.unknown());
        examples.add(example("old_deductions_incomplete", provisional));
        Scenario mismatch = new Scenario(); mismatch.calculations.put(Regime.OLD,
                new RegimeCalculation(Regime.OLD, Money.parse("700"), Verification.MISMATCH));
        examples.add(example("verifier_mismatch", mismatch));
        Scenario stale = new Scenario(); stale.currentKey = key("tenantA", "profileA", "caseA", "snap2", 'a');
        examples.add(example("stale_snapshot", stale));
        Scenario pending = new Scenario(); pending.reference = ReferenceState.PENDING;
        examples.add(example("independent_references_pending", pending));
        examples.add(example("synthetic_all_green", new Scenario()));
        Files.writeString(output, "[" + String.join(",", examples) + "]\n", StandardCharsets.UTF_8);
    }

    private static String example(String name, Scenario s) {
        return "{\"fixture\":\"" + name + "\",\"synthetic_policy_fixture_only\":true,\"policy_response\":"
                + SafeJson.encode(s.evaluate()) + "}";
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 2 && args[0].equals("--examples-only")) {
            writeExamples(Path.of(args[1]));
            return;
        }
        scenarios();
        System.out.println("SCENARIOS_PASSED=" + passed);
        System.out.println("SCENARIOS_FAILED=" + failed);
        System.out.println("GENERATED_GATE_COMBINATIONS=" + generatedCases);
        if (failed != 0) System.exit(1);
        if (passed == 0 || generatedCases != 2048) throw new IllegalStateException("test coverage not executed");
        if (args.length == 1) writeExamples(Path.of(args[0]));
    }
}
