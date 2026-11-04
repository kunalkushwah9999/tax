package com.taxplatform.localdemo;

import com.taxplatform.planner.*;
import com.taxplatform.safety.*;
import com.taxplatform.safety.SafetyInput.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Separate, read-only arithmetic request. Never returns the private workings projection. */
final class PlannerEndpoint {
    static final Set<String> FIELDS=Set.of("view_token","period","dob","residency","eligible_80c","eligible_80d","eligible_nps",
            "profile_confirmed","eligible_confirmed","screen_business","screen_special","screen_property_foreign","screen_status","screen_losses","screen_adjustments","regimes");
    private final DualPlanner planner=new DualPlanner();
    private final String ruleDigest,engineDigest,verifierDigest;
    PlannerEndpoint()throws IOException,GeneralSecurityException {
        try(var stream=DualPlanner.class.getResourceAsStream("/draft-rule-spec.json")) {
            if(stream==null)throw new IOException("draft rule manifest unavailable");ruleDigest=digest(stream.readAllBytes());
        }
        try {
            Path location=Path.of(DualPlanner.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if(!Files.isRegularFile(location))throw new IOException("packaged engine artifact required");
            String artifact=digest(Files.readAllBytes(location));
            engineDigest=digest(("decimal-engine-v1:"+artifact).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            verifierDigest=digest(("paise-verifier-v1:"+artifact).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }catch(java.net.URISyntaxException invalid){throw new IOException("engine artifact identity unavailable");}
    }
    PlannerInput input(Candidate current,Map<String,String> profile) {
        if(!profile.keySet().equals(FIELDS))throw new IllegalArgumentException("exact profile fields required");
        if(!Set.of("YES","NO").contains(profile.get("profile_confirmed")) || !Set.of("YES","NO").contains(profile.get("eligible_confirmed")))
            throw new IllegalArgumentException("explicit confirmation intent required");
        CaseFacts f=current.facts();Map<String,String> form=new TreeMap<>();
        form.put("period",profile.get("period"));form.put("residency",profile.get("residency"));
        String dob=profile.get("dob");form.put("dob",dob.isBlank()?"UNKNOWN":dob);
        form.put("dob_state",dob.isBlank()?"UNKNOWN":profile.get("profile_confirmed").equals("YES")?"CONFIRMED":"ESTIMATED");
        boolean salaryConfirmed=f.salary().state()==Fact.State.CONFIRMED;
        boolean interestConfirmed=List.of(f.savingsInterest(),f.qualifyingDepositInterest(),f.otherInterest()).stream().allMatch(v->v.state()==Fact.State.CONFIRMED);
        form.put("salary_state",salaryConfirmed?"CONFIRMED":"UNKNOWN");
        form.put("salaries",salaryConfirmed?f.salary().confirmedValue().orElseThrow().decimal():"UNKNOWN");
        form.put("interest_state",interestConfirmed?"CONFIRMED":"UNKNOWN");
        form.put("savings",interestConfirmed?f.savingsInterest().confirmedValue().orElseThrow().decimal():"UNKNOWN");
        form.put("deposits",interestConfirmed?f.qualifyingDepositInterest().confirmedValue().orElseThrow().decimal():"UNKNOWN");
        form.put("other_interest",interestConfirmed?f.otherInterest().confirmedValue().orElseThrow().decimal():"UNKNOWN");
        boolean complete=List.of("eligible_80c","eligible_80d","eligible_nps").stream().allMatch(k->!profile.get(k).isBlank());
        form.put("deduction_state",!complete?"UNKNOWN":profile.get("eligible_confirmed").equals("YES")?"CONFIRMED":"ESTIMATED");
        for(String key:List.of("eligible_80c","eligible_80d","eligible_nps"))form.put(key,complete?profile.get(key):"UNKNOWN");
        form.put("income_inventory",f.incomeInventoryComplete().confirmedValue().orElse(false)?"REVIEWED":"INCOMPLETE");
        form.put("deduction_inventory",f.deductionInventoryComplete().get(Regime.OLD).confirmedValue().orElse(false)?"REVIEWED":"INCOMPLETE");
        for(String key:List.of("screen_business","screen_special","screen_property_foreign","screen_status","screen_losses","screen_adjustments","regimes"))form.put(key,profile.get(key));
        return PlannerForms.parse(form);
    }
    CalculationKey key(CalculationKey source,PlannerInput input,Map<String,String> profile)throws GeneralSecurityException {
        Map<String,String> material=new TreeMap<>(profile);material.remove("view_token");
        String settings=digest((source.settingsHash()+"\n"+JsonOutput.encode(material)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return new CalculationKey(source.tenantId(),source.profileId(),source.caseId(),source.snapshotId(),input.period(),ruleDigest,engineDigest,verifierDigest,"planner-v1",settings);
    }
    Map<Regime,DualPlanner.Assessment> evaluate(PlannerInput input){return planner.evaluate(input);}
    Candidate candidate(Candidate source,CalculationKey key,PlannerInput input,Map<Regime,DualPlanner.Assessment> rows) {
        EnumMap<Regime,RegimeCalculation> calculations=new EnumMap<>(Regime.class);
        for(var entry:rows.entrySet())if(entry.getValue().privateWorkings()!=null) {
            var assessment=entry.getValue();calculations.put(entry.getKey(),new RegimeCalculation(entry.getKey(),
                    new Money(assessment.privateWorkings().value(TaxStages.Stage.ANNUAL_TAX_BEFORE_CREDITS)),
                    assessment.status()==DualPlanner.Status.CALCULATED?Verification.MATCH:Verification.MISMATCH));
        }
        Scope scope=rows.values().stream().anyMatch(a->a.status()==DualPlanner.Status.UNSUPPORTED)?Scope.UNSUPPORTED:
                rows.values().stream().anyMatch(a->a.status()==DualPlanner.Status.INFORMATION_REQUIRED)?Scope.UNKNOWN:Scope.SUPPORTED;
        CaseFacts f=source.facts();
        Map<Regime,Fact<Boolean>> inventories=Map.of(Regime.NEW,f.deductionInventoryComplete().get(Regime.NEW),Regime.OLD,input.deductionsComplete());
        return new Candidate(key,new CaseFacts(scope,f.salary(),f.savingsInterest(),f.qualifyingDepositInterest(),f.otherInterest(),f.incomeInventoryComplete(),inventories),calculations);
    }
    Map<String,Object> response(PlannerInput input,Map<Regime,DualPlanner.Assessment> rows,Map<String,Object> guarded) {
        Map<String,Object> checks=new TreeMap<>();rows.forEach((regime,a)->checks.put(regime.name(),Map.of("input_status",a.status().name(),"issues",a.issues(),
                "arithmetic_cross_check",a.status()==DualPlanner.Status.CALCULATED?"MATCH":a.status()==DualPlanner.Status.VERIFIER_MISMATCH?"MISMATCH":"NOT_RUN")));
        return Map.of("contract_version","planning-readiness.v1","period",input.period(),"checks",Map.copyOf(checks),"result",guarded,
                "independent_approved_cases",0,"rule_release_status","UNAPPROVED","calculation_values_released",false,
                "message","Arithmetic agreement is an engineering check. Independently approved rules and reference outputs are still required.");
    }
    private static String digest(byte[] bytes)throws GeneralSecurityException{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
}
