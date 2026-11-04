package com.taxplatform.safety;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** Restricted product projection. Only the signed guided service constructs this response. */
public final class GuidedResult {
    public static final String CONTRACT_VERSION = "guided-result.v1";
    private final Map<String, Object> response;

    GuidedResult(PublicResult result, Map<String, Object> guide) {
        response = frozen(Map.of("contract_version", CONTRACT_VERSION,
                "result", result.toResponse(), "guide", guide));
    }

    static Map<String, Object> frozen(Map<String, ?> value) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(new TreeMap<>(value)));
    }

    public Map<String, Object> toResponse() { return response; }
    @Override public String toString() { return "GuidedResult[redacted]"; }
}
