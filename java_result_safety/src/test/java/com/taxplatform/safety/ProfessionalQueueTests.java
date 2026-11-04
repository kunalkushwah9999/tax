package com.taxplatform.safety;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.taxplatform.safety.ProfessionalReview.*;
import static com.taxplatform.safety.SafetyInput.*;
import static com.taxplatform.safety.ProofFixtures.*;

/** Professional-readiness fixtures, not real credentials, client grants, evidence validation or CA approvals. */
public final class ProfessionalQueueTests {
    private static final Actor REVIEWER=new Actor("tenantA","reviewerA");
    private static int passed,failed,generated;
    private ProfessionalQueueTests() { }
    @FunctionalInterface private interface Test { void run() throws Exception; }
    private static void test(String name,Test run) {
        try{run.run();passed++;System.out.println("PASS "+name);}
        catch(Exception|AssertionError error){failed++;System.out.println("FAIL "+name+": "+error.getClass().getSimpleName());}
    }
    private static void check(boolean condition){if(!condition)throw new AssertionError();}
    private static Map<EvidenceKind,EvidenceState> reviewed() {
        Map<EvidenceKind,EvidenceState> map=new EnumMap<>(EvidenceKind.class);
        for(EvidenceKind kind:EvidenceKind.values())map.put(kind,EvidenceState.REVIEWED);return map;
    }
    private static CalculationKey caseKey(String caseId) {
        CalculationKey k=key();return new CalculationKey(k.tenantId(),k.profileId(),caseId,k.snapshotId(),k.periodKey(),
                k.ruleBundleHash(),k.engineDigest(),k.verifierDigest(),k.contractVersion(),k.settingsHash());
    }
    private static final class Fixture implements SafeResultService.ControlPlane,Source {
        Candidate candidate=ProofFixtures.candidate();
        Actor assigned=REVIEWER;CalculationKey grant=key(),evidenceKey=key();
        Instant from=NOW.minusSeconds(60),until=NOW.plusSeconds(60);
        GrantState grantState=GrantState.ACTIVE;CredentialState credential=CredentialState.VERIFIED;
        WorkKind work=WorkKind.ROUTINE_PLANNER;boolean released=true,authorized=true,outage,nullReply,missing,tampered;
        ReferenceState references=ReferenceState.APPROVED;
        final Map<EvidenceKind,EvidenceState> evidence=reviewed();
        final Map<Regime,Availability> availability=new EnumMap<>(Map.of(Regime.NEW,Availability.ALLOWED,Regime.OLD,Availability.ALLOWED));
        int sourceReads,controlReads;
        Access access() {
            SignedCandidate signed=sign(candidate);
            if(tampered){Candidate changed=new Candidate(candidate.key(),new CaseFacts(Scope.SUPPORTED,Fact.confirmed(Money.parse("999999")),candidate.facts().savingsInterest(),candidate.facts().qualifyingDepositInterest(),
                    candidate.facts().otherInterest(),candidate.facts().incomeInventoryComplete(),candidate.facts().deductionInventoryComplete()),candidate.calculations());
                signed=new SignedCandidate(changed,signed.header(),signed.signature());}
            return new Access(assigned,grant,evidenceKey,from,until,grantState,credential,released,work,evidence,signed);
        }
        @Override public Optional<Access> currentFor(Actor reviewer,CalculationKey expected)throws SafeResultService.ControlStateUnavailable{
            sourceReads++;if(outage)throw new SafeResultService.ControlStateUnavailable();if(nullReply)return null;if(missing)return Optional.empty();return Optional.of(access());
        }
        @Override public DeliveryContext loadFor(Actor actor,Candidate c){controlReads++;return new DeliveryContext(candidate.key(),authorized,false,true,true,
                RuleState.EFFECTIVE,references,false,availability);}
        @Override public void quarantine(CalculationKey k){ }
        ProfessionalQueueService service(){return new ProfessionalQueueService(this,this,verifier(),Clock.fixed(NOW,ZoneOffset.UTC));}
        ProfessionalQueue deliver(){return service().deliver(REVIEWER,List.of(key()));}
    }
    private static Map<?,?> first(ProfessionalQueue queue){return (Map<?,?>)((List<?>)queue.toResponse().get("cases")).get(0);}
    private static void route(ProfessionalQueue queue,Route expected){check(first(queue).get("route").equals(expected.name()));}
    private static void hidden(ProfessionalQueue queue){check(!first(queue).containsKey("evidence_checklist"));noAmounts(queue);}
    private static void noAmounts(ProfessionalQueue queue){String json=SafeJson.encode(queue);
        for(String text:List.of("98765.43","420.00","700.00","999999","reviewerA","tenantA","profileA","caseA","annual_taxes","comparison_status"))check(!json.contains(text));}
    private static void money(Fixture fixture,Fact<Money> value){CaseFacts f=fixture.candidate.facts();fixture.candidate=new Candidate(key(),new CaseFacts(f.scope(),value,f.savingsInterest(),f.qualifyingDepositInterest(),f.otherInterest(),
            f.incomeInventoryComplete(),f.deductionInventoryComplete()),fixture.candidate.calculations());}

    public static void main(String[] args)throws Exception{
        if(args.length==2 && args[0].equals("--examples-only")){examples(Path.of(args[1]));return;}
        test("a prepared routine case is ready for review rather than already approved",()->{
            ProfessionalQueue q=new Fixture().deliver();route(q,Route.READY_FOR_REVIEW);check(first(q).get("professional_review_status").equals("NOT_RECORDED"));noAmounts(q);
        });
        test("queue capabilities grant no bulk approval filing or ITR export",()->{
            Map<?,?> capabilities=(Map<?,?>)new Fixture().deliver().toResponse().get("capabilities");
            for(String name:List.of("bulk_approve","approve_cases","submit_return","export_itr"))check(Boolean.FALSE.equals(capabilities.get(name)));
        });
        test("submitted documents are not automatically reviewed evidence",()->{
            Fixture f=new Fixture();f.evidence.put(EvidenceKind.INCOME_SOURCES,EvidenceState.SUBMITTED);route(f.deliver(),Route.EVIDENCE_REQUIRED);
        });
        test("evidence conflicts require resolution before readiness",()->{
            Fixture f=new Fixture();f.evidence.put(EvidenceKind.INTEREST_BREAKDOWN,EvidenceState.CONFLICT);route(f.deliver(),Route.EVIDENCE_CONFLICT);
        });
        test("professional and specialist actions remain explicit even with complete routine calculation data",()->{
            for(WorkKind kind:List.of(WorkKind.STATUTORY_PROFESSIONAL_ACTION,WorkKind.SPECIALIST_REVIEW)){
                Fixture f=new Fixture();f.work=kind;route(f.deliver(),Route.PROFESSIONAL_ACTION_REQUIRED);
            }
        });
        test("unknown work type and unsupported scope require scope review",()->{
            Fixture unknown=new Fixture();unknown.work=WorkKind.UNKNOWN;route(unknown.deliver(),Route.SCOPE_REVIEW_REQUIRED);
            Fixture unsupported=new Fixture();CaseFacts f=unsupported.candidate.facts();unsupported.candidate=new Candidate(key(),new CaseFacts(Scope.UNSUPPORTED,f.salary(),f.savingsInterest(),f.qualifyingDepositInterest(),f.otherInterest(),f.incomeInventoryComplete(),f.deductionInventoryComplete()),unsupported.candidate.calculations());
            route(unsupported.deliver(),Route.SCOPE_REVIEW_REQUIRED);
        });
        test("unknown estimated and conflicting amounts route to client information without leaking values",()->{
            List<Fact<Money>> uncertain=List.of(Fact.<Money>unknown(),Fact.estimated(Money.parse("123")),
                    Fact.conflict(List.of(Money.parse("456"),Money.parse("789"))));
            for(Fact<Money> value:uncertain){
                Fixture f=new Fixture();money(f,value);ProfessionalQueue q=f.deliver();route(q,Route.CLIENT_INFORMATION_REQUIRED);noAmounts(q);
            }
        });
        test("unfinished deductions cannot appear ready for review",()->{
            Fixture x=new Fixture();CaseFacts f=x.candidate.facts();x.candidate=new Candidate(key(),new CaseFacts(f.scope(),f.salary(),f.savingsInterest(),f.qualifyingDepositInterest(),f.otherInterest(),f.incomeInventoryComplete(),
                    Map.of(Regime.NEW,Fact.confirmed(true),Regime.OLD,Fact.unknown())),x.candidate.calculations());route(x.deliver(),Route.DEDUCTION_REVIEW_REQUIRED);
        });
        test("uncertain regime availability remains a review task",()->{
            Fixture f=new Fixture();f.availability.put(Regime.OLD,Availability.UNKNOWN);route(f.deliver(),Route.ELIGIBILITY_REVIEW_REQUIRED);
        });
        test("a missing second regime calculation remains pending",()->{
            Fixture f=new Fixture();f.candidate=new Candidate(key(),f.candidate.facts(),Map.of(Regime.NEW,f.candidate.calculations().get(Regime.NEW)));route(f.deliver(),Route.CALCULATION_PENDING);
        });
        test("unapproved references block professional result readiness and private evidence details",()->{
            Fixture f=new Fixture();f.references=ReferenceState.PENDING;ProfessionalQueue q=f.deliver();route(q,Route.RESULT_BLOCKED);hidden(q);
        });
        test("a professional assignment does not override result-level authorization",()->{
            Fixture f=new Fixture();f.authorized=false;ProfessionalQueue q=f.deliver();route(q,Route.ACCESS_DENIED);hidden(q);
        });
        test("tampered producer payload blocks evidence and review readiness",()->{
            Fixture f=new Fixture();f.tampered=true;ProfessionalQueue q=f.deliver();route(q,Route.RESULT_BLOCKED);hidden(q);
        });
        test("professional credentials and assignment are checked rather than inferred from a paid plan",()->{
            for(CredentialState state:List.of(CredentialState.PENDING,CredentialState.UNVERIFIED)){
                Fixture f=new Fixture();f.credential=state;ProfessionalQueue q=f.deliver();route(q,Route.ACCESS_DENIED);hidden(q);check(f.controlReads==0);
            }
            Fixture wrong=new Fixture();wrong.assigned=new Actor("tenantA","anotherReviewer");route(wrong.deliver(),Route.ACCESS_DENIED);
        });
        test("revoked expired exact-boundary and future grants cannot reveal evidence",()->{
            List<Fixture> denied=new ArrayList<>();Fixture revoked=new Fixture();revoked.grantState=GrantState.REVOKED;denied.add(revoked);
            Fixture expired=new Fixture();expired.until=NOW;denied.add(expired);Fixture future=new Fixture();future.from=NOW.plusNanos(1);denied.add(future);
            for(Fixture f:denied){ProfessionalQueue q=f.deliver();route(q,Route.ACCESS_DENIED);hidden(q);check(f.controlReads==0);}
        });
        test("grants are resource and tax-period bound",()->{
            Fixture wrongCase=new Fixture();wrongCase.grant=caseKey("otherCase");route(wrongCase.deliver(),Route.ACCESS_DENIED);
            Fixture wrongPeriod=new Fixture();CalculationKey k=key();wrongPeriod.grant=new CalculationKey(k.tenantId(),k.profileId(),k.caseId(),k.snapshotId(),"OTHER-PERIOD",k.ruleBundleHash(),k.engineDigest(),k.verifierDigest(),k.contractVersion(),k.settingsHash());
            route(wrongPeriod.deliver(),Route.ACCESS_DENIED);
        });
        test("unreleased workspace cannot expose readiness or evidence",()->{
            Fixture f=new Fixture();f.released=false;ProfessionalQueue q=f.deliver();route(q,Route.FEATURE_DISABLED);hidden(q);check(f.controlReads==0);
        });
        test("evidence metadata is bound to the exact signed snapshot",()->{
            Fixture f=new Fixture();f.evidenceKey=caseKey("oldCase");ProfessionalQueue q=f.deliver();route(q,Route.STALE_CASE);hidden(q);check(f.controlReads==0);
        });
        test("a changed case candidate is not accepted through an earlier queue request",()->{
            Fixture f=new Fixture();f.candidate=new Candidate(caseKey("differentCase"),f.candidate.facts(),f.candidate.calculations());
            route(f.deliver(),Route.STALE_CASE);check(f.controlReads==0);
        });
        test("cross tenant queue calls do not read professional source or result controls",()->{
            Fixture f=new Fixture();ProfessionalQueue q=f.service().deliver(new Actor("tenantB","reviewerA"),List.of(key()));
            route(q,Route.ACCESS_DENIED);hidden(q);check(f.sourceReads==0 && f.controlReads==0);
        });
        test("missing source outage and null response fail closed",()->{
            Fixture missing=new Fixture();missing.missing=true;ProfessionalQueue a=missing.deliver();route(a,Route.ACCESS_DENIED);hidden(a);
            Fixture outage=new Fixture();outage.outage=true;ProfessionalQueue b=outage.deliver();route(b,Route.UNAVAILABLE);hidden(b);
            Fixture nil=new Fixture();nil.nullReply=true;ProfessionalQueue c=nil.deliver();route(c,Route.UNAVAILABLE);hidden(c);
        });
        test("each delivery reloads professional grant revocation",()->{
            Fixture f=new Fixture();ProfessionalQueueService s=f.service();route(s.deliver(REVIEWER,List.of(key())),Route.READY_FOR_REVIEW);
            f.grantState=GrantState.REVOKED;ProfessionalQueue q=s.deliver(REVIEWER,List.of(key()));route(q,Route.ACCESS_DENIED);hidden(q);check(f.sourceReads==2);
        });
        test("duplicate resource references and out-of-bound batch sizes are rejected before lookup",()->{
            Fixture f=new Fixture();List<List<CalculationKey>> invalid=List.of(List.of(),List.of(key(),key()),java.util.Collections.nCopies(51,key()));
            for(List<CalculationKey> cases:invalid){try{f.service().deliver(REVIEWER,cases);throw new AssertionError();}catch(IllegalArgumentException expected){ }}check(f.sourceReads==0);
        });
        test("denied and blocked cases remain in the requested batch denominator",()->{
            Fixture f=new Fixture();CalculationKey otherTenant=new CalculationKey("tenantB","p","c","s",key().periodKey(),key().ruleBundleHash(),key().engineDigest(),key().verifierDigest(),key().contractVersion(),key().settingsHash());
            ProfessionalQueue q=f.service().deliver(REVIEWER,List.of(key(),otherTenant));check(q.toResponse().get("requested_case_count").equals("2"));
            Map<?,?> counts=(Map<?,?>)q.toResponse().get("counts_by_route");check(counts.get("READY_FOR_REVIEW").equals("1") && counts.get("ACCESS_DENIED").equals("1"));
            check(q.toResponse().get("professional_need_rate").equals("NOT_MEASURED"));check(q.toResponse().get("tax_correctness_rate").equals("NOT_ESTABLISHED"));
        });
        test("all evidence kinds require explicit states and queue projections are immutable",()->{
            Fixture f=new Fixture();f.evidence.remove(EvidenceKind.INCOME_SOURCES);
            try{f.access();throw new AssertionError();}catch(NullPointerException expected){ }
            ProfessionalQueue q=new Fixture().deliver();try{q.toResponse().clear();throw new AssertionError();}catch(UnsupportedOperationException expected){ }
            try{first(q).clear();throw new AssertionError();}catch(UnsupportedOperationException expected){ }
            check(q.toString().equals("ProfessionalQueue[redacted]"));
        });
        test("all 1296 evidence-state combinations preserve reviewed versus submitted distinctions",()->{
            EvidenceState[] states=EvidenceState.values();Fixture f=new Fixture();
            for(int mask=0;mask<1296;mask++){
                int value=mask;boolean conflict=false,complete=true;
                for(EvidenceKind kind:EvidenceKind.values()){
                    EvidenceState state=states[value%states.length];value/=states.length;f.evidence.put(kind,state);
                    conflict|=state==EvidenceState.CONFLICT;complete&=state==EvidenceState.REVIEWED || state==EvidenceState.NOT_REQUIRED;
                }
                route(f.deliver(),conflict?Route.EVIDENCE_CONFLICT:complete?Route.READY_FOR_REVIEW:Route.EVIDENCE_REQUIRED);generated++;
            }
        });
        if(args.length==1)examples(Path.of(args[0]));
        System.out.println("SCENARIOS_PASSED="+passed);System.out.println("SCENARIOS_FAILED="+failed);System.out.println("GENERATED_REVIEW_COMBINATIONS="+generated);
        if(failed!=0)throw new AssertionError("professional review checks failed");
    }
    private static void examples(Path path)throws java.io.IOException{
        List<String> samples=new ArrayList<>();Fixture ready=new Fixture();samples.add("{\"name\":\"ready_is_not_approved\",\"response\":"+SafeJson.encode(ready.deliver())+"}");
        Fixture evidence=new Fixture();evidence.evidence.put(EvidenceKind.INCOME_SOURCES,EvidenceState.SUBMITTED);samples.add("{\"name\":\"submitted_needs_review\",\"response\":"+SafeJson.encode(evidence.deliver())+"}");
        Fixture professional=new Fixture();professional.work=WorkKind.STATUTORY_PROFESSIONAL_ACTION;samples.add("{\"name\":\"qualified_professional_action\",\"response\":"+SafeJson.encode(professional.deliver())+"}");
        Fixture pending=new Fixture();pending.references=ReferenceState.PENDING;samples.add("{\"name\":\"validation_cannot_be_overridden\",\"response\":"+SafeJson.encode(pending.deliver())+"}");
        Files.writeString(path,"["+String.join(",",samples)+"]\n",StandardCharsets.UTF_8);
    }
}
