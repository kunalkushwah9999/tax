package com.taxplatform.safety;

import java.util.Map;

/** Input-preparation-only projection; contains no raw facts or calculation payload. */
public final class IntakeGuide {
    public static final String CONTRACT_VERSION = "intake-guide.v1";
    private final Map<String, Object> response;

    IntakeGuide(String status, Map<String, Object> guide) {
        response = GuidedResult.frozen(Map.of("contract_version", CONTRACT_VERSION, "status", status, "guide", guide));
    }

    public Map<String, Object> toResponse() { return response; }
    @Override public String toString() { return "IntakeGuide[redacted]"; }
}
