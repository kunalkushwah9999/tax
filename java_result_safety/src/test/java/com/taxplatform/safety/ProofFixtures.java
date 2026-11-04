package com.taxplatform.safety;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static com.taxplatform.safety.SafetyInput.*;

/** Ephemeral test credentials only; no private key is persisted or placed in the library JAR. */
final class ProofFixtures {
    static final Instant NOW = Instant.parse("2026-10-31T09:00:00Z");
    static final KeyPair KEYS = keys();
    static final String KEY_ID = "test-producer-1";

    private ProofFixtures() { }

    static KeyPair keys() {
        try { return KeyPairGenerator.getInstance("Ed25519").generateKeyPair(); }
        catch (GeneralSecurityException unavailable) { throw new IllegalStateException("test Ed25519 unavailable", unavailable); }
    }

    static CalculationKey key() {
        return new CalculationKey("tenantA", "profileA", "caseA", "snap1", "ITA1961-AY2026-27",
                "a".repeat(64), "b".repeat(64), "c".repeat(64), "facts-v1", "d".repeat(64));
    }

    static Candidate candidate() {
        Map<Regime, Fact<Boolean>> deductions = new EnumMap<>(Regime.class);
        deductions.put(Regime.NEW, Fact.confirmed(true)); deductions.put(Regime.OLD, Fact.confirmed(true));
        CaseFacts facts = new CaseFacts(Scope.SUPPORTED, Fact.confirmed(Money.parse("98765.43")),
                Fact.confirmed(Money.parse("0")), Fact.confirmed(Money.parse("0")), Fact.confirmed(Money.parse("0")),
                Fact.confirmed(true), deductions);
        return new Candidate(key(), facts, Map.of(
                Regime.NEW, new RegimeCalculation(Regime.NEW, Money.parse("420"), Verification.MATCH),
                Regime.OLD, new RegimeCalculation(Regime.OLD, Money.parse("700"), Verification.MATCH)));
    }

    static CandidateProofVerifier.TrustedKey trustedKey() {
        return new CandidateProofVerifier.TrustedKey(KEY_ID, KEYS.getPublic(),
                CandidateProofVerifier.Purpose.CALCULATION_PRODUCER, CandidateProofVerifier.KeyStatus.ACTIVE,
                NOW.minusSeconds(3600), NOW.plusSeconds(3600));
    }

    static CandidateProofVerifier verifier() {
        return new CandidateProofVerifier(id -> Optional.of(trustedKey()),
                Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofMinutes(10), Duration.ofSeconds(5));
    }

    static SignedCandidate sign(Candidate candidate) {
        return sign(candidate, new SignedCandidate.Header(KEY_ID, NOW.minusSeconds(1), NOW.plusSeconds(300)), KEYS);
    }

    static SignedCandidate sign(Candidate candidate, SignedCandidate.Header header, KeyPair keyPair) {
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initSign(keyPair.getPrivate());
            signature.update(CandidateCanonicalizer.signingBytes(candidate, header));
            return new SignedCandidate(candidate, header, signature.sign());
        } catch (GeneralSecurityException failure) { throw new IllegalStateException("test signature failed", failure); }
    }
}
