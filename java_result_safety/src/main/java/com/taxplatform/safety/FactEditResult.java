package com.taxplatform.safety;

import java.util.LinkedHashMap;
import java.util.Map;

/** Amount-free command receipt. A historical receipt is never presented as a current draft. */
public final class FactEditResult {
    public static final String CONTRACT_VERSION = "fact-edit.v1";
    public enum Status {
        APPLIED, NO_CHANGE, REPLAYED, ACCESS_DENIED, SESSION_EXPIRED, SESSION_LIMIT_REACHED,
        STALE_INPUT, IDEMPOTENCY_CONFLICT, INVALID_AMOUNT, CONFIRMATION_VALUE_CHANGED,
        REVIEW_STATE_CHANGED, CONFLICT_VALUE_NOT_PRESENT, INPUT_REVIEW_INCOMPLETE,
        UNSUPPORTED_SCOPE, SYSTEM_REVIEW_REQUIRED, CONTROL_STATE_UNAVAILABLE
    }
    private final Status status;
    private final String snapshotId;
    private final boolean changed;
    private final HumanMoneyParser.Problem inputProblem;

    private FactEditResult(Status status, String snapshotId, boolean changed, HumanMoneyParser.Problem inputProblem) {
        this.status=status; this.snapshotId=snapshotId; this.changed=changed; this.inputProblem=inputProblem;
    }
    static FactEditResult accepted(Status status, String snapshotId, boolean changed) {
        CalculationKey.requireId(snapshotId);
        return new FactEditResult(status,snapshotId,changed,null);
    }
    static FactEditResult rejected(Status status) { return new FactEditResult(status,null,false,null); }
    static FactEditResult invalid(HumanMoneyParser.Problem problem) { return new FactEditResult(Status.INVALID_AMOUNT,null,false,problem); }
    public Status status() { return status; }
    public Map<String,Object> toResponse() {
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("contract_version",CONTRACT_VERSION); result.put("status",status.name());
        result.put("state_changed",changed);
        result.put("capabilities",GuidedResult.frozen(Map.of("submit_return",false,"export_itr",false)));
        if(snapshotId!=null) {
            result.put("outcome_snapshot_id",snapshotId);
            result.put("refresh_current_draft",true);
            result.put("message",status==Status.REPLAYED
                    ? "This request was already accepted. Refresh the current draft; this receipt may refer to an earlier snapshot."
                    : "Refresh the current draft before the next step. Input edits do not approve a tax calculation.");
        }
        if(inputProblem!=null) result.put("input_problem",inputProblem.name());
        return GuidedResult.frozen(result);
    }
    @Override public String toString() { return "FactEditResult[redacted; "+status.name()+"]"; }
}
