package com.taxplatform.safety;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.taxplatform.safety.SafetyInput.Candidate;

/** Immutable producer proof. A valid signature is not independent tax-law approval. */
public record SignedCandidate(Candidate candidate, Header header, byte[] signature) {
    private static final Pattern KEY_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    public record Header(String producerKeyId, Instant issuedAt, Instant expiresAt) {
        public Header {
            if (producerKeyId == null || !KEY_ID.matcher(producerKeyId).matches()) {
                throw new IllegalArgumentException("invalid producer key identifier");
            }
            Objects.requireNonNull(issuedAt, "issued time");
            Objects.requireNonNull(expiresAt, "expiry time");
        }
    }

    public SignedCandidate {
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(header, "proof header");
        if (signature == null || signature.length != 64) {
            throw new IllegalArgumentException("Ed25519 signature must be 64 bytes");
        }
        signature = signature.clone();
    }

    @Override public byte[] signature() { return signature.clone(); }
    @Override public String toString() { return "SignedCandidate[redacted]"; }
}
