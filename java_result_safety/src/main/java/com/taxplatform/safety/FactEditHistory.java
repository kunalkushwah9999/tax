package com.taxplatform.safety;

import java.util.List;
import java.util.Map;

/** Amount-free, authorized session history. This is not a durable/compliance audit log. */
public final class FactEditHistory {
    private final Map<String,Object> response;
    FactEditHistory(List<Map<String,Object>> events) {
        response=GuidedResult.frozen(Map.of("contract_version","fact-history.v1","events",List.copyOf(events),
                "message","This session history is temporary. Refresh your draft after an edit; earlier calculations may be stale."));
    }
    public Map<String,Object> toResponse() { return response; }
    @Override public String toString() { return "FactEditHistory[redacted]"; }
}
