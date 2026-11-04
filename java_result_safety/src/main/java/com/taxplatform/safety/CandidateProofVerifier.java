package com.taxplatform.safety;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.EdECPublicKey;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Checks bounded producer proofs against an authoritative current public-key registry. */
public final class CandidateProofVerifier {
    public enum Status { VALID, INVALID, UNAVAILABLE }
    public enum KeyStatus { ACTIVE, REVOKED }
    public enum Purpose { CALCULATION_PRODUCER, RULE_APPROVAL }

    public record TrustedKey(String keyId, PublicKey publicKey, Purpose purpose,
                             KeyStatus status, Instant notBefore, Instant notAfter) {
        public TrustedKey {
            Objects.requireNonNull(keyId); Objects.requireNonNull(publicKey);
            Objects.requireNonNull(purpose); Objects.requireNonNull(status);
            Objects.requireNonNull(notBefore); Objects.requireNonNull(notAfter);
            if (!(publicKey instanceof EdECPublicKey key)
                    || !"Ed25519".equals(key.getParams().getName()) || !notAfter.isAfter(notBefore)) {
                throw new IllegalArgumentException("invalid Ed25519 trust entry");
            }
        }
    }

    @FunctionalInterface public interface KeyRegistry {
        Optional<TrustedKey> resolve(String keyId) throws KeyRegistryUnavailable;
    }

    public static final class KeyRegistryUnavailable extends Exception {
        private static final long serialVersionUID = 1L;
        public KeyRegistryUnavailable() { super("key registry unavailable"); }
    }

    private final KeyRegistry registry;
    private final Clock clock;
    private final Duration maxLifetime;
    private final Duration clockSkew;

    public CandidateProofVerifier(KeyRegistry registry, Clock clock, Duration maxLifetime, Duration clockSkew) {
        this.registry = Objects.requireNonNull(registry);
        this.clock = Objects.requireNonNull(clock);
        this.maxLifetime = Objects.requireNonNull(maxLifetime);
        this.clockSkew = Objects.requireNonNull(clockSkew);
        if (maxLifetime.isNegative() || maxLifetime.isZero() || maxLifetime.compareTo(Duration.ofDays(1)) > 0
                || clockSkew.isNegative() || clockSkew.compareTo(Duration.ofMinutes(1)) > 0) {
            throw new IllegalArgumentException("invalid proof lifetime/skew bounds");
        }
    }

    public Status verify(SignedCandidate signed) {
        Objects.requireNonNull(signed);
        SignedCandidate.Header header = signed.header();
        try {
            Instant now = clock.instant();
            if (!header.expiresAt().isAfter(now) || !header.expiresAt().isAfter(header.issuedAt())
                    || header.issuedAt().isAfter(now.plus(clockSkew))
                    || Duration.between(header.issuedAt(), header.expiresAt()).compareTo(maxLifetime) > 0) {
                return Status.INVALID;
            }
            Optional<TrustedKey> resolved = registry.resolve(header.producerKeyId());
            if (resolved == null) return Status.UNAVAILABLE;
            if (resolved.isEmpty()) return Status.INVALID;
            TrustedKey key = resolved.get();
            if (!key.keyId().equals(header.producerKeyId()) || key.status() != KeyStatus.ACTIVE
                    || key.purpose() != Purpose.CALCULATION_PRODUCER
                    || header.issuedAt().isBefore(key.notBefore())
                    || header.expiresAt().isAfter(key.notAfter())
                    || now.isBefore(key.notBefore()) || !now.isBefore(key.notAfter())) return Status.INVALID;
            Signature verification = Signature.getInstance("Ed25519");
            verification.initVerify(key.publicKey());
            verification.update(CandidateCanonicalizer.signingBytes(signed.candidate(), header));
            return verification.verify(signed.signature()) ? Status.VALID : Status.INVALID;
        } catch (KeyRegistryUnavailable unavailable) {
            return Status.UNAVAILABLE;
        } catch (GeneralSecurityException | DateTimeException | IllegalArgumentException invalid) {
            return Status.INVALID;
        }
    }
}
