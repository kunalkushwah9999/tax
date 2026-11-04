package com.taxplatform.safety;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.taxplatform.safety.SafetyInput.*;
import static com.taxplatform.safety.PublicResult.*;
import static com.taxplatform.safety.CandidateProofVerifier.*;
import static com.taxplatform.safety.ProofFixtures.*;

/** Real JCA signatures with ephemeral keys; these tests certify no tax amounts. */
public final class ProofTests {
    private static int passed, failed, bindings;
    private ProofTests() { }
    @FunctionalInterface private interface Test { void run() throws Exception; }
    private static void test(String name, Test action) {
        try { action.run(); passed++; System.out.println("PASS " + name); }
        catch (Exception | AssertionError failure) {
            failed++; System.out.println("FAIL " + name + ": " + failure.getClass().getSimpleName());
        }
    }
    private static void check(boolean condition) { if (!condition) throw new AssertionError(); }
    private static void invalid(Test action) throws Exception {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("invalid configuration accepted");
    }
    private static CandidateProofVerifier using(KeyRegistry registry) {
        return new CandidateProofVerifier(registry, Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofMinutes(10), Duration.ofSeconds(5));
    }
    private static TrustedKey entry(String id, Purpose purpose, KeyStatus status, Instant from, Instant to) {
        return new TrustedKey(id, KEYS.getPublic(), purpose, status, from, to);
    }
    private static SignedCandidate.Header header(Instant issued, Instant expires) {
        return new SignedCandidate.Header(KEY_ID, issued, expires);
    }
    private static void status(SignedCandidate signed, Status expected) { check(verifier().verify(signed) == expected); }
    private static Candidate withFacts(CaseFacts f) { return new Candidate(key(), f, candidate().calculations()); }
    private static Candidate amountFact(int index, Fact<Money> value) {
        CaseFacts f = candidate().facts();
        List<Fact<Money>> amounts = new ArrayList<>(List.of(f.salary(), f.savingsInterest(),
                f.qualifyingDepositInterest(), f.otherInterest()));
        amounts.set(index, value);
        return withFacts(new CaseFacts(f.scope(), amounts.get(0), amounts.get(1), amounts.get(2), amounts.get(3),
                f.incomeInventoryComplete(), f.deductionInventoryComplete()));
    }
    private static Candidate boolFact(int index, Fact<Boolean> value) {
        CaseFacts f = candidate().facts();
        Map<Regime, Fact<Boolean>> deductions = new EnumMap<>(f.deductionInventoryComplete());
        if (index > 0) deductions.put(Regime.values()[index - 1], value);
        return withFacts(new CaseFacts(f.scope(), f.salary(), f.savingsInterest(), f.qualifyingDepositInterest(),
                f.otherInterest(), index == 0 ? value : f.incomeInventoryComplete(), deductions));
    }
    private static Candidate calculation(Regime regime, String amount, Verification verification) {
        Map<Regime, RegimeCalculation> map = new EnumMap<>(candidate().calculations());
        map.put(regime, new RegimeCalculation(regime, Money.parse(amount), verification));
        return new Candidate(key(), candidate().facts(), map);
    }
    private static void rejectsTamper(SignedCandidate original, Candidate changed) {
        check(!Arrays.equals(CandidateCanonicalizer.signingBytes(original.candidate(), original.header()),
                CandidateCanonicalizer.signingBytes(changed, original.header())));
        status(new SignedCandidate(changed, original.header(), original.signature()), Status.INVALID);
        bindings++;
    }

    private static final class Controls implements SafeResultService.ControlPlane {
        CalculationKey current = key();
        boolean authorized = true, deleted, restored = true, integrity = true, failRead, failWrite, nullRead;
        RuleState rule = RuleState.EFFECTIVE;
        ReferenceState references = ReferenceState.APPROVED;
        final Set<CalculationKey> quarantined = new HashSet<>();
        int reads, writes;
        @Override public DeliveryContext loadFor(Actor actor, Candidate candidate) throws SafeResultService.ControlStateUnavailable {
            reads++;
            if (failRead) throw new SafeResultService.ControlStateUnavailable();
            if (nullRead) return null;
            return new DeliveryContext(current, authorized, deleted, integrity, restored, rule, references,
                    quarantined.contains(candidate.key()), Map.of(Regime.NEW, Availability.ALLOWED, Regime.OLD, Availability.ALLOWED));
        }
        @Override public void quarantine(CalculationKey key) throws SafeResultService.ControlStateUnavailable {
            writes++;
            if (failWrite) throw new SafeResultService.ControlStateUnavailable();
            quarantined.add(key);
        }
        PublicResult deliver(SignedCandidate signed) {
            return new SignedResultService(this, verifier()).deliver(new Actor("tenantA", "readerA"), signed);
        }
    }
    private static void reason(PublicResult result, Reason expected) {
        check(result.reasons().contains(expected));
        String json = SafeJson.encode(result);
        check(!json.contains("\"annual_taxes\"") && !json.contains("\"comparison\""));
        check(!json.contains("98765.43") && !json.contains("420.00") && !json.contains("700.00"));
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && args[0].equals("--canonical-only")) {
            Files.write(Path.of(args[1]), CandidateCanonicalizer.signingBytes(candidate(),
                    header(NOW.minusSeconds(1), NOW.plusSeconds(300))));
            return;
        }
        test("real Ed25519 producer signature verifies", () -> status(sign(candidate()), Status.VALID));
        test("every calculation identity field is signed", () -> {
            CalculationKey k = key();
            List<String> fields = List.of(k.tenantId(), k.profileId(), k.caseId(), k.snapshotId(), k.periodKey(),
                    k.ruleBundleHash(), k.engineDigest(), k.verifierDigest(), k.contractVersion(), k.settingsHash());
            SignedCandidate original = sign(candidate());
            for (int i = 0; i < fields.size(); i++) {
                List<String> changed = new ArrayList<>(fields);
                changed.set(i, i == 5 || i == 6 || i == 7 || i == 9 ? "e".repeat(64) : fields.get(i) + "x");
                CalculationKey key = new CalculationKey(changed.get(0), changed.get(1), changed.get(2), changed.get(3),
                        changed.get(4), changed.get(5), changed.get(6), changed.get(7), changed.get(8), changed.get(9));
                rejectsTamper(original, new Candidate(key, candidate().facts(), candidate().calculations()));
            }
        });
        test("all four monetary fields bind states and all represented values", () -> {
            SignedCandidate original = sign(candidate());
            for (int index = 0; index < 4; index++) {
                for (Fact<Money> fact : List.of(Fact.confirmed(Money.parse("123")), Fact.<Money>unknown(),
                        Fact.estimated(Money.parse("123")), Fact.conflict(List.of(Money.parse("123"), Money.parse("456"))))) {
                    rejectsTamper(original, amountFact(index, fact));
                }
                Candidate left = amountFact(index, Fact.conflict(List.of(Money.parse("123"), Money.parse("456"))));
                rejectsTamper(sign(left), amountFact(index, Fact.conflict(List.of(Money.parse("123"), Money.parse("457")))));
                rejectsTamper(sign(amountFact(index, Fact.estimated(Money.parse("123")))),
                        amountFact(index, Fact.estimated(Money.parse("124"))));
            }
        });
        test("inventory and per-regime deduction states and values are signed", () -> {
            SignedCandidate original = sign(candidate());
            for (int index = 0; index < 3; index++) {
                for (Fact<Boolean> fact : List.of(Fact.confirmed(false), Fact.<Boolean>unknown(),
                        Fact.estimated(true), Fact.conflict(List.of(true, false)))) rejectsTamper(original, boolFact(index, fact));
                rejectsTamper(sign(boolFact(index, Fact.estimated(true))), boolFact(index, Fact.estimated(false)));
                rejectsTamper(sign(boolFact(index, Fact.conflict(List.of(true, false)))),
                        boolFact(index, Fact.conflict(List.of(false, true))));
            }
        });
        test("scope amount verifier state and missing regime cannot be changed", () -> {
            SignedCandidate original = sign(candidate());
            CaseFacts f = candidate().facts();
            for (Scope scope : List.of(Scope.PARTIAL, Scope.UNSUPPORTED, Scope.UNKNOWN))
                rejectsTamper(original, withFacts(new CaseFacts(scope, f.salary(), f.savingsInterest(),
                        f.qualifyingDepositInterest(), f.otherInterest(), f.incomeInventoryComplete(), f.deductionInventoryComplete())));
            for (Regime regime : Regime.values()) {
                rejectsTamper(original, calculation(regime, "421", Verification.MATCH));
                String originalAmount = candidate().calculations().get(regime).annualTaxBeforeCredits().decimal();
                rejectsTamper(original, calculation(regime, originalAmount, Verification.MISMATCH));
                rejectsTamper(original, calculation(regime, originalAmount, Verification.NOT_RUN));
                Map<Regime, RegimeCalculation> map = new EnumMap<>(candidate().calculations()); map.remove(regime);
                rejectsTamper(original, new Candidate(key(), f, map));
            }
        });
        test("proof header key identity and nanosecond timestamps are signed", () -> {
            SignedCandidate original = sign(candidate());
            List<SignedCandidate.Header> headers = List.of(
                    new SignedCandidate.Header("another-key", original.header().issuedAt(), original.header().expiresAt()),
                    header(original.header().issuedAt().plusNanos(1), original.header().expiresAt()),
                    header(original.header().issuedAt(), original.header().expiresAt().minusNanos(1)));
            for (SignedCandidate.Header h : headers) {
                TrustedKey alias = entry(h.producerKeyId(), Purpose.CALCULATION_PRODUCER, KeyStatus.ACTIVE,
                        NOW.minusSeconds(3600), NOW.plusSeconds(3600));
                check(using(id -> Optional.of(alias)).verify(new SignedCandidate(original.candidate(), h, original.signature())) == Status.INVALID);
                bindings++;
            }
        });
        test("map insertion order cannot change signing bytes", () -> {
            Candidate c = candidate();
            Map<Regime, RegimeCalculation> reversed = new LinkedHashMap<>();
            reversed.put(Regime.OLD, c.calculations().get(Regime.OLD)); reversed.put(Regime.NEW, c.calculations().get(Regime.NEW));
            SignedCandidate signed = sign(c);
            status(new SignedCandidate(new Candidate(c.key(), c.facts(), reversed), signed.header(), signed.signature()), Status.VALID);
        });
        test("length prefixes distinguish identity separator placements", () -> {
            CalculationKey a = new CalculationKey("a:b", "c", "caseA", "snap1", key().periodKey(),
                    key().ruleBundleHash(), key().engineDigest(), key().verifierDigest(), key().contractVersion(), key().settingsHash());
            CalculationKey b = new CalculationKey("a", "b:c", "caseA", "snap1", key().periodKey(),
                    key().ruleBundleHash(), key().engineDigest(), key().verifierDigest(), key().contractVersion(), key().settingsHash());
            check(!Arrays.equals(CandidateCanonicalizer.signingBytes(new Candidate(a, candidate().facts(), candidate().calculations()), sign(candidate()).header()),
                    CandidateCanonicalizer.signingBytes(new Candidate(b, candidate().facts(), candidate().calculations()), sign(candidate()).header())));
        });
        test("another private key cannot forge producer signature", () -> status(sign(candidate(), sign(candidate()).header(), keys()), Status.INVALID));
        test("flipped and all-zero signature bytes are rejected", () -> {
            SignedCandidate s = sign(candidate()); byte[] changed = s.signature(); changed[0] ^= 1;
            status(new SignedCandidate(s.candidate(), s.header(), changed), Status.INVALID);
            status(new SignedCandidate(s.candidate(), s.header(), new byte[64]), Status.INVALID);
        });
        test("signature bytes cannot be mutated through constructor or getter", () -> {
            SignedCandidate original = sign(candidate()); byte[] input = original.signature();
            SignedCandidate copy = new SignedCandidate(original.candidate(), original.header(), input);
            input[0] ^= 1; copy.signature()[1] ^= 1; status(copy, Status.VALID);
        });
        test("signature and signing format sizes are bounded", () -> {
            invalid(() -> new SignedCandidate(candidate(), sign(candidate()).header(), new byte[63]));
            invalid(() -> new SignedCandidate(candidate(), sign(candidate()).header(), new byte[65]));
            invalid(() -> new SignedCandidate.Header("bad key", NOW, NOW.plusSeconds(1)));
            invalid(() -> new SignedCandidate.Header("k".repeat(65), NOW, NOW.plusSeconds(1)));
            check(CandidateCanonicalizer.signingBytes(candidate(), sign(candidate()).header()).length < CandidateCanonicalizer.MAX_BYTES);
            invalid(() -> Fact.conflict(java.util.Collections.nCopies(9, Money.parse("1"))));
        });
        test("identity text excludes replacement-prone Unicode and spaces", () -> {
            for (String id : List.of("tenant A", "tenant\uD800", "tenant\uFFFD", "tenant\nA", "टेनेंट"))
                invalid(() -> new Actor(id, "readerA"));
        });
        test("expired and exact expiry proofs are rejected", () -> {
            status(sign(candidate(), header(NOW.minusSeconds(20), NOW), KEYS), Status.INVALID);
            status(sign(candidate(), header(NOW.minusSeconds(20), NOW.minusNanos(1)), KEYS), Status.INVALID);
        });
        test("future issue beyond skew is rejected and exact skew accepted", () -> {
            status(sign(candidate(), header(NOW.plusSeconds(5).plusNanos(1), NOW.plusSeconds(300)), KEYS), Status.INVALID);
            status(sign(candidate(), header(NOW.plusSeconds(5), NOW.plusSeconds(300)), KEYS), Status.VALID);
        });
        test("zero reversed and excessive proof lifetimes are rejected", () -> {
            status(sign(candidate(), header(NOW.plusSeconds(1), NOW.plusSeconds(1)), KEYS), Status.INVALID);
            status(sign(candidate(), header(NOW.plusSeconds(2), NOW.plusSeconds(1)), KEYS), Status.INVALID);
            status(sign(candidate(), header(NOW.minusSeconds(301), NOW.plusSeconds(300)), KEYS), Status.INVALID);
            status(sign(candidate(), header(NOW.minusSeconds(300), NOW.plusSeconds(300)), KEYS), Status.VALID);
        });
        test("missing key and registry outage fail closed with distinct statuses", () -> {
            check(using(id -> Optional.empty()).verify(sign(candidate())) == Status.INVALID);
            check(using(id -> { throw new KeyRegistryUnavailable(); }).verify(sign(candidate())) == Status.UNAVAILABLE);
            check(using(id -> null).verify(sign(candidate())) == Status.UNAVAILABLE);
        });
        test("revoked key and approval-only key cannot sign calculations", () -> {
            check(using(id -> Optional.of(entry(KEY_ID, Purpose.CALCULATION_PRODUCER, KeyStatus.REVOKED,
                    NOW.minusSeconds(3600), NOW.plusSeconds(3600)))).verify(sign(candidate())) == Status.INVALID);
            check(using(id -> Optional.of(entry(KEY_ID, Purpose.RULE_APPROVAL, KeyStatus.ACTIVE,
                    NOW.minusSeconds(3600), NOW.plusSeconds(3600)))).verify(sign(candidate())) == Status.INVALID);
        });
        test("registry must return exact requested key and correct public key", () -> {
            check(using(id -> Optional.of(entry("wrong-id", Purpose.CALCULATION_PRODUCER, KeyStatus.ACTIVE,
                    NOW.minusSeconds(3600), NOW.plusSeconds(3600)))).verify(sign(candidate())) == Status.INVALID);
            TrustedKey wrong = new TrustedKey(KEY_ID, keys().getPublic(), Purpose.CALCULATION_PRODUCER,
                    KeyStatus.ACTIVE, NOW.minusSeconds(3600), NOW.plusSeconds(3600));
            check(using(id -> Optional.of(wrong)).verify(sign(candidate())) == Status.INVALID);
        });
        test("proof issue and expiry must lie inside key validity", () -> {
            for (TrustedKey key : List.of(
                    entry(KEY_ID, Purpose.CALCULATION_PRODUCER, KeyStatus.ACTIVE, NOW, NOW.plusSeconds(3600)),
                    entry(KEY_ID, Purpose.CALCULATION_PRODUCER, KeyStatus.ACTIVE, NOW.minusSeconds(3600), NOW.plusSeconds(299))))
                check(using(id -> Optional.of(key)).verify(sign(candidate())) == Status.INVALID);
            TrustedKey exact = entry(KEY_ID, Purpose.CALCULATION_PRODUCER, KeyStatus.ACTIVE, NOW.minusSeconds(1), NOW.plusSeconds(300));
            check(using(id -> Optional.of(exact)).verify(sign(candidate())) == Status.VALID);
        });
        test("key validity is checked at read time including exact boundary", () -> {
            for (Instant instant : List.of(NOW.minusSeconds(3601), NOW.plusSeconds(3600))) {
                CandidateProofVerifier v = new CandidateProofVerifier(id -> Optional.of(trustedKey()),
                        Clock.fixed(instant, ZoneOffset.UTC), Duration.ofDays(1), Duration.ofMinutes(1));
                check(v.verify(sign(candidate(), header(instant.minusSeconds(1), instant.plusSeconds(1)), KEYS)) == Status.INVALID);
            }
        });
        test("invalid proof clock configuration is rejected", () -> {
            for (Duration lifetime : List.of(Duration.ZERO, Duration.ofSeconds(-1), Duration.ofDays(1).plusNanos(1)))
                invalid(() -> new CandidateProofVerifier(id -> Optional.empty(), Clock.systemUTC(), lifetime, Duration.ZERO));
            for (Duration skew : List.of(Duration.ofNanos(-1), Duration.ofMinutes(1).plusNanos(1)))
                invalid(() -> new CandidateProofVerifier(id -> Optional.empty(), Clock.systemUTC(), Duration.ofSeconds(1), skew));
        });
        test("valid signed result still needs current approval and rule gates", () -> {
            Controls controls = new Controls(); controls.references = ReferenceState.PENDING;
            reason(controls.deliver(sign(candidate())), Reason.INDEPENDENT_REFERENCE_NOT_APPROVED);
            controls.references = ReferenceState.APPROVED; controls.rule = RuleState.RECALLED;
            reason(controls.deliver(sign(candidate())), Reason.RULE_NOT_EFFECTIVE);
            controls.rule = RuleState.EFFECTIVE;
            check(controls.deliver(sign(candidate())) instanceof Calculation);
            check(controls.reads == 3);
        });
        test("tampered cached amounts are hidden with no quarantine side effect", () -> {
            Controls controls = new Controls(); SignedCandidate s = sign(candidate());
            Candidate tampered = calculation(Regime.NEW, "999999", Verification.MISMATCH);
            reason(controls.deliver(new SignedCandidate(tampered, s.header(), s.signature())), Reason.PAYLOAD_INTEGRITY_FAILED);
            check(controls.writes == 0 && controls.quarantined.isEmpty());
        });
        test("authentic mismatch quarantines even while references are pending", () -> {
            Controls controls = new Controls(); controls.references = ReferenceState.PENDING;
            reason(controls.deliver(sign(calculation(Regime.NEW, "420", Verification.MISMATCH))), Reason.VERIFIER_MISMATCH);
            check(controls.writes == 1);
            controls.references = ReferenceState.APPROVED;
            reason(controls.deliver(sign(candidate())), Reason.QUARANTINED);
        });
        test("cross-tenant and unauthorized calls cannot read keys or write quarantine", () -> {
            Controls controls = new Controls(); int[] resolutions = {0};
            SignedResultService service = new SignedResultService(controls, using(id -> { resolutions[0]++; return Optional.of(trustedKey()); }));
            SignedCandidate mismatch = sign(calculation(Regime.NEW, "420", Verification.MISMATCH));
            reason(service.deliver(new Actor("tenantB", "readerA"), mismatch), Reason.ACCESS_DENIED);
            check(controls.reads == 0);
            controls.authorized = false;
            reason(service.deliver(new Actor("tenantA", "readerA"), mismatch), Reason.ACCESS_DENIED);
            check(resolutions[0] == 0 && controls.writes == 0);
        });
        test("deleted wrong-resource unrestored and damaged contexts cannot quarantine", () -> {
            SignedCandidate mismatch = sign(calculation(Regime.NEW, "420", Verification.MISMATCH));
            Controls deleted = new Controls(); deleted.deleted = true; reason(deleted.deliver(mismatch), Reason.ACCESS_DENIED);
            Controls unrestored = new Controls(); unrestored.restored = false; reason(unrestored.deliver(mismatch), Reason.RESTORE_NOT_CAUGHT_UP);
            Controls damaged = new Controls(); damaged.integrity = false; reason(damaged.deliver(mismatch), Reason.PAYLOAD_INTEGRITY_FAILED);
            Controls wrong = new Controls(); CalculationKey k = key();
            wrong.current = new CalculationKey(k.tenantId(), "profileB", k.caseId(), k.snapshotId(), k.periodKey(),
                    k.ruleBundleHash(), k.engineDigest(), k.verifierDigest(), k.contractVersion(), k.settingsHash());
            reason(wrong.deliver(mismatch), Reason.CONTEXT_BINDING_INVALID);
            check(deleted.writes + unrestored.writes + damaged.writes + wrong.writes == 0);
        });
        test("key revocation is reloaded for repeated delivery of cached result", () -> {
            Controls controls = new Controls(); boolean[] revoked = {false}; int[] reads = {0};
            SignedResultService service = new SignedResultService(controls, using(id -> {
                reads[0]++; return Optional.of(entry(KEY_ID, Purpose.CALCULATION_PRODUCER,
                        revoked[0] ? KeyStatus.REVOKED : KeyStatus.ACTIVE, NOW.minusSeconds(3600), NOW.plusSeconds(3600)));
            }));
            SignedCandidate signed = sign(candidate()); Actor actor = new Actor("tenantA", "readerA");
            check(service.deliver(actor, signed) instanceof Calculation); revoked[0] = true;
            reason(service.deliver(actor, signed), Reason.PAYLOAD_INTEGRITY_FAILED);
            check(reads[0] == 2 && controls.reads == 2);
        });
        test("registry outage is blocked without leaking cached amounts", () -> {
            Controls controls = new Controls();
            reason(new SignedResultService(controls, using(id -> { throw new KeyRegistryUnavailable(); }))
                    .deliver(new Actor("tenantA", "readerA"), sign(candidate())), Reason.CONTROL_STATE_UNAVAILABLE);
        });
        test("read and quarantine write outages have no numerical fallback", () -> {
            Controls read = new Controls(); read.failRead = true;
            reason(read.deliver(sign(candidate())), Reason.CONTROL_STATE_UNAVAILABLE);
            Controls empty = new Controls(); empty.nullRead = true;
            reason(empty.deliver(sign(candidate())), Reason.CONTROL_STATE_UNAVAILABLE);
            Controls write = new Controls(); write.failWrite = true;
            reason(write.deliver(sign(calculation(Regime.NEW, "420", Verification.MISMATCH))), Reason.CONTROL_STATE_UNAVAILABLE);
        });
        test("signed wrapper and proof headers do not expose amounts in log strings", () -> {
            String text = sign(candidate()).toString(); check(!text.contains("98765.43") && !text.contains("420"));
        });
        System.out.println("SCENARIOS_PASSED=" + passed);
        System.out.println("SCENARIOS_FAILED=" + failed);
        System.out.println("SIGNED_FIELD_BINDINGS=" + bindings);
        if (failed != 0) throw new AssertionError("proof checks failed");
    }
}
