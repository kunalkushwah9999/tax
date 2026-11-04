package com.taxplatform.localdemo;

import com.taxplatform.safety.CalculationKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/** Bounded, session-owned local review; no tax fact writes, source authentication or durable sign-off. */
final class ComparisonWorkspace {
    enum Reason { CHECKED_SOURCE_RECORD, CORRECTED_CLASSIFICATION, DUPLICATE_REPRESENTATION, INDEPENDENT_WORKING }
    static final class Rejected extends RuntimeException {
        private static final long serialVersionUID=1L;
        final String code;
        Rejected(String code){super(code);this.code=code;}
    }
    private record Selection(String observation,Reason reason) { }
    private record Saved(String fingerprint,Map<String,Object> receipt) { }
    private final Supplier<String> tokens;
    private final Clock clock;
    private final Map<String,Saved> receipts=new HashMap<>();
    private final Map<String,Selection> selections=new HashMap<>();
    private final List<Map<String,Object>> events=new ArrayList<>();
    private List<StatementComparison.Group> groups=List.of();
    private CalculationKey inputKey;
    private String sourceFingerprint,version;
    ComparisonWorkspace(Supplier<String> tokens,Clock clock){this.tokens=Objects.requireNonNull(tokens);this.clock=Objects.requireNonNull(clock);}
    synchronized Map<String,Object> replay(String id,String fingerprint) {
        requestId(id);Saved prior=receipts.get(id);if(prior==null)return null;
        if(!prior.fingerprint().equals(fingerprint))throw new Rejected("IDEMPOTENCY_CONFLICT");
        return Map.of("status","REPLAYED","state_changed",false,"refresh_comparison",true,"outcome_version",prior.receipt().get("outcome_version"));
    }
    synchronized Map<String,Object> importRows(String id,String requestFingerprint,CalculationKey current,byte[] csv) {
        Map<String,Object> replay=replay(id,requestFingerprint);if(replay!=null)return replay;
        capacity();List<StatementComparison.Group> parsed=StatementComparison.group(StatementComparison.parse(csv));
        String digest=StatementComparison.fingerprint(csv);boolean unchanged=current.equals(inputKey)&&digest.equals(sourceFingerprint);
        if(!unchanged){groups=parsed;inputKey=current;sourceFingerprint=digest;version=tokens.get();selections.clear();event("IMPORT","");}
        return save(id,requestFingerprint,unchanged?"NO_CHANGE":"APPLIED",!unchanged);
    }
    synchronized Map<String,Object> select(String id,String fingerprint,CalculationKey current,String expectedVersion,
            String groupId,String observation,Reason reason,boolean reopen) {
        Map<String,Object> replay=replay(id,fingerprint);if(replay!=null)return replay;
        capacity();fresh(current);if(!Objects.equals(version,expectedVersion))throw new Rejected("STALE_RECONCILIATION");
        StatementComparison.Group group=groups.stream().filter(g->g.id().equals(groupId)).findFirst().orElseThrow(()->new Rejected("GROUP_NOT_FOUND"));
        boolean changed;
        if(reopen)changed=selections.remove(groupId)!=null;
        else {
            Objects.requireNonNull(reason);
            StatementComparison.Observation chosen=group.observations().stream().filter(o->o.id().equals(observation)).findFirst().orElseThrow(()->new Rejected("OBSERVATION_NOT_FOUND"));
            if(chosen.gross().isEmpty())throw new Rejected("GROSS_VALUE_MISSING");
            Selection selection=new Selection(chosen.id(),reason);changed=!selection.equals(selections.get(groupId));
            if(changed)selections.put(groupId,selection);
        }
        if(changed){version=tokens.get();event(reopen?"REOPEN_LOCAL_REVIEW":"SELECT_SOURCE_OBSERVATION",groupId);}
        return save(id,fingerprint,changed?"APPLIED":"NO_CHANGE",changed);
    }
    synchronized Map<String,Object> projection(CalculationKey current) {
        if(inputKey==null)return Map.of("status","NO_COMPARISON","filing_ready",false,"income_facts_modified",false);
        fresh(current);List<Map<String,Object>> projected=new ArrayList<>();
        for(StatementComparison.Group group:groups){Map<String,Object> row=new HashMap<>();
            row.put("group_id",group.id());row.put("category",group.category().name());row.put("payer_alias",group.payer());row.put("period",group.period());
            row.put("coverage",group.coverage().name());row.put("reference",group.reference());row.put("income_check",group.incomeStatus().name());row.put("tds_check",group.tdsStatus().name());
            row.put("income_flags",StatementComparison.flags(group.observations(),true).stream().map(Enum::name).toList());row.put("tds_flags",StatementComparison.flags(group.observations(),false).stream().map(Enum::name).toList());
            row.put("proposal_policy",SourceProposalPolicy.barrier(group,groups));
            if(SourceProposalPolicy.target(group.category())!=null)row.put("proposal_field",SourceProposalPolicy.target(group.category()).name());
            row.put("observations",group.observations().stream().map(StatementComparison.Observation::projection).toList());
            Selection chosen=selections.get(group.id());row.put("local_review",chosen==null?"UNREVIEWED":"OBSERVATION_SELECTED");
            if(chosen!=null){row.put("selected_observation_id",chosen.observation());row.put("review_reason",chosen.reason().name());}
            projected.add(Map.copyOf(row));
        }
        return Map.of("status","COMPARISON_AVAILABLE","comparison_token",version,"groups",List.copyOf(projected),"groups_reviewed",selections.size(),
                "group_count",groups.size(),"events",List.copyOf(events),"filing_ready",false,"income_facts_modified",false,
                "source_authenticity_verified",false,"scope","NORMALIZED_SAMPLE_OBSERVATIONS_ONLY_NO_CATEGORY_TOTALS");
    }
    synchronized SourceProposalPolicy.Snapshot sourceForProposal(CalculationKey current,String expectedVersion,String groupId,String observationId) {
        fresh(current);if(!Objects.equals(version,expectedVersion))throw new Rejected("STALE_RECONCILIATION");
        StatementComparison.Group group=groups.stream().filter(g->g.id().equals(groupId)).findFirst().orElseThrow(()->new Rejected("GROUP_NOT_FOUND"));
        Selection selected=selections.get(groupId);
        if(selected==null || !selected.observation().equals(observationId))throw new Rejected("SOURCE_SELECTION_REQUIRED");
        StatementComparison.Observation observation=group.observations().stream().filter(o->o.id().equals(selected.observation())).findFirst().orElseThrow(()->new Rejected("SOURCE_BINDING_INVALID"));
        return new SourceProposalPolicy.Snapshot(inputKey,version,group,observation,groups);
    }
    private Map<String,Object> save(String id,String fingerprint,String status,boolean changed){Map<String,Object> result=Map.of("status",status,"state_changed",changed,"refresh_comparison",true,"outcome_version",version);receipts.put(id,new Saved(fingerprint,result));return result;}
    private void fresh(CalculationKey key){if(inputKey==null)throw new Rejected("NO_COMPARISON");if(!inputKey.equals(key))throw new Rejected("STALE_RECONCILIATION");}
    private void event(String operation,String group){events.add(Map.of("operation",operation,"group_id",group,"at",clock.instant().toString()));}
    private void capacity(){if(receipts.size()>=512)throw new Rejected("COMPARISON_LIMIT_REACHED");}
    private static void requestId(String id){if(id==null || !id.matches("[A-Za-z0-9._:/-]{1,128}"))throw new IllegalArgumentException("invalid request ID");}
    static String binding(String kind,String inputView,byte[] body){return StatementComparison.fingerprint((kind+"\n"+inputView+"\n"+StatementComparison.fingerprint(body)).getBytes(StandardCharsets.UTF_8));}
    @Override public String toString(){return "ComparisonWorkspace[redacted]";}
}
