package com.taxplatform.safety;

import java.util.Objects;
import java.util.regex.Pattern;

/** Identity supplied by trusted snapshot/release services. No PAN belongs here. */
public record CalculationKey(
        String tenantId, String profileId, String caseId, String snapshotId,
        String periodKey, String ruleBundleHash, String engineDigest,
        String verifierDigest, String contractVersion, String settingsHash) {
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9._:/-]{1,128}");

    public CalculationKey {
        requireId(tenantId); requireId(profileId); requireId(caseId); requireId(snapshotId);
        requireId(periodKey); requireId(contractVersion);
        requireHash(ruleBundleHash); requireHash(engineDigest);
        requireHash(verifierDigest); requireHash(settingsHash);
    }

    static void requireId(String value) {
        Objects.requireNonNull(value, "identity field");
        if (!ID.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid identity field");
        }
    }

    private static void requireHash(String value) {
        if (value == null || !HASH.matcher(value).matches()) {
            throw new IllegalArgumentException("lowercase SHA-256 digest required");
        }
    }

    public boolean sameResource(CalculationKey other) {
        return tenantId.equals(other.tenantId) && profileId.equals(other.profileId)
                && caseId.equals(other.caseId);
    }

    @Override public String toString() { return "CalculationKey[redacted]"; }
}
