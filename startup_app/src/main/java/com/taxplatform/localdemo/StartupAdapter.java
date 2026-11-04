package com.taxplatform.localdemo;

import com.taxplatform.safety.*;
import com.taxplatform.safety.SafetyInput.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Trusted adapter for startup_app. Callers must authorize the current saved case before invoking. */
public final class StartupAdapter {
    private final PlannerEndpoint planner;
    private final DemoSigner signer;
    public StartupAdapter()throws IOException,GeneralSecurityException{planner=new PlannerEndpoint();signer=new DemoSigner(Clock.systemUTC());}
    public static List<String> profileFields(){return PlannerEndpoint.FIELDS.stream().filter(f->!f.equals("view_token")).sorted().toList();}
    public interface Current {boolean stillCurrent();void quarantine();boolean quarantined();}
    public Map<String,Object> assess(Candidate source,Map<String,String> fields,Current current)throws GeneralSecurityException{
        Map<String,String> profile=new java.util.TreeMap<>(fields);profile.put("view_token","SERVER_OWNED");var input=planner.input(source,profile);var rows=planner.evaluate(input);CalculationKey key=planner.key(source.key(),input,profile);
        if(rows.values().stream().anyMatch(row->row.status()==com.taxplatform.planner.DualPlanner.Status.VERIFIER_MISMATCH))current.quarantine();
        Candidate candidate=planner.candidate(source,key,input,rows);
        SafeResultService.ControlPlane controls=new SafeResultService.ControlPlane(){
            @Override public DeliveryContext loadFor(Actor actor,Candidate stored)throws SafeResultService.ControlStateUnavailable{
                if(!current.stillCurrent())throw new SafeResultService.ControlStateUnavailable();
                return new DeliveryContext(key,true,false,true,true,RuleState.UNAPPROVED,ReferenceState.PENDING,current.quarantined(),Map.of(Regime.NEW,Availability.UNKNOWN,Regime.OLD,Availability.UNKNOWN));
            }
            @Override public void quarantine(CalculationKey ignored){current.quarantine();}
        };
        var guarded=new SignedResultService(controls,signer.verifier()).deliver(new Actor(source.key().tenantId(),source.key().profileId()),signer.sign(candidate)).toResponse();
        return planner.response(input,rows,guarded);
    }
    public static Map<String,Object> compare(String csv,CalculationKey key){
        ComparisonWorkspace workspace=new ComparisonWorkspace(()->UUID.randomUUID().toString(),Clock.systemUTC());byte[] bytes=csv.getBytes(StandardCharsets.UTF_8);
        workspace.importRows("import","fingerprint",key,bytes);return workspace.projection(key);
    }
    public record Proposal(FactEdits.ProposeAmount command,Map<String,Object> reference){ }
    public static Proposal proposal(String csv,CalculationKey original,CalculationKey current,String groupId,String observationId,String field,String amount,boolean whole,boolean replace){
        var groups=StatementComparison.group(StatementComparison.parse(csv.getBytes(StandardCharsets.UTF_8)));
        var group=groups.stream().filter(g->g.id().equals(groupId)).findFirst().orElseThrow(()->new IllegalArgumentException("GROUP_NOT_FOUND"));
        var observation=group.observations().stream().filter(o->o.id().equals(observationId)).findFirst().orElseThrow(()->new IllegalArgumentException("OBSERVATION_NOT_FOUND"));
        try{
            var prepared=SourceProposalPolicy.prepare(new SourceProposalPolicy.Snapshot(original,"SERVER_OWNED",group,observation,groups),current,
                    new SourceProposalPolicy.Intent(FactEdits.AmountField.valueOf(field),Money.parse(amount),whole,replace));
            return new Proposal((FactEdits.ProposeAmount)prepared.command(),prepared.reference().projection("ESTIMATED"));
        }catch(ComparisonWorkspace.Rejected rejected){throw new IllegalArgumentException(rejected.code);}
    }
}
