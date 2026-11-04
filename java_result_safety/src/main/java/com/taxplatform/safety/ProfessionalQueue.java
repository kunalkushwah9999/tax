package com.taxplatform.safety;

import java.util.List;
import java.util.Map;

/** Read-only, amount-free professional review queue; not a record of completed professional work. */
public final class ProfessionalQueue {
    private final Map<String,Object> response;
    ProfessionalQueue(List<Map<String,Object>> rows,Map<String,Object> counts) {
        response=GuidedResult.frozen(Map.of("contract_version","professional-queue.v1",
                "requested_case_count",Integer.toString(rows.size()),"cases",List.copyOf(rows),
                "counts_by_route",GuidedResult.frozen(counts),
                "professional_need_rate","NOT_MEASURED","tax_correctness_rate","NOT_ESTABLISHED",
                "capabilities",GuidedResult.frozen(Map.of("bulk_approve",false,"approve_cases",false,"submit_return",false,"export_itr",false)),
                "message","Queue readiness is not professional approval, filing readiness or evidence of a low need for professional review."));
    }
    public Map<String,Object> toResponse() { return response; }
    @Override public String toString() { return "ProfessionalQueue[redacted]"; }
}
