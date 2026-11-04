package com.taxplatform.safety;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.taxplatform.safety.SafetyInput.*;
import static com.taxplatform.safety.PublicResult.*;
import static com.taxplatform.safety.ProofFixtures.*;

/** Product guidance tests; all affirmative tax/reference states are synthetic fixtures. */
public final class GuidanceTests {
    private static final Actor ACTOR = new Actor("tenantA", "readerA");
    private static int passed, failed, generated;
    private GuidanceTests() { }
    @FunctionalInterface private interface Test { void run() throws Exception; }
    private static void test(String name, Test test) {
        try { test.run(); passed++; System.out.println("PASS " + name); }
        catch (Exception | AssertionError failure) {
            failed++; System.out.println("FAIL " + name + ": " + failure.getClass().getSimpleName());
        }
    }
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    private static void immutable(Runnable change) {
        try { change.run(); } catch (UnsupportedOperationException expected) { return; }
        throw new AssertionError("projection is mutable");
    }
    private static Map<?, ?> guide(GuidedResult response) { return (Map<?, ?>) response.toResponse().get("guide"); }
    private static Map<?, ?> guide(IntakeGuide response) { return (Map<?, ?>) response.toResponse().get("guide"); }
    private static String action(Map<?, ?> guide) { return (String) ((Map<?, ?>) guide.get("next_action")).get("action"); }
    private static String field(Map<?, ?> guide) { return (String) ((Map<?, ?>) guide.get("next_action")).get("field"); }
    private static Map<?, ?> step(Map<?, ?> guide, String id) {
        for (Object value : (List<?>) guide.get("checklist")) {
            Map<?, ?> item = (Map<?, ?>) value; if (id.equals(item.get("id"))) return item;
        }
        throw new AssertionError("missing checklist item");
    }
    private static void noPrivateGuide(Map<?, ?> guide) {
        for (String key : List.of("checklist", "progress_label", "regime_cards", "preparation_note")) check(!guide.containsKey(key));
        Map<?, ?> capabilities = (Map<?, ?>) guide.get("capabilities");
        check(Boolean.FALSE.equals(capabilities.get("show_input_checklist")));
        check(Boolean.FALSE.equals(capabilities.get("copy_preparation_note")));
    }
    private static void noAmounts(String json) {
        for (String value : List.of("98765.43", "420.00", "700.00", "annual_taxes", "difference_in_annual_tax")) check(!json.contains(value));
        for (String value : List.of("tenantA", "profileA", "caseA", "test-producer-1")) check(!json.contains(value));
    }
    private static Candidate draft(Candidate candidate) { return new Candidate(candidate.key(), candidate.facts(), Map.of()); }
    private static Candidate fact(int index, Fact<Money> fact) {
        CaseFacts f = candidate().facts(); List<Fact<Money>> all = new ArrayList<>(List.of(f.salary(), f.savingsInterest(), f.qualifyingDepositInterest(), f.otherInterest()));
        all.set(index, fact);
        return new Candidate(key(), new CaseFacts(f.scope(), all.get(0), all.get(1), all.get(2), all.get(3),
                f.incomeInventoryComplete(), f.deductionInventoryComplete()), candidate().calculations());
    }
    private static Candidate inventory(int index, Fact<Boolean> state) {
        CaseFacts f = candidate().facts(); Map<Regime, Fact<Boolean>> d = new EnumMap<>(f.deductionInventoryComplete());
        if (index > 0) d.put(Regime.values()[index-1], state);
        return new Candidate(key(), new CaseFacts(f.scope(), f.salary(), f.savingsInterest(), f.qualifyingDepositInterest(), f.otherInterest(),
                index == 0 ? state : f.incomeInventoryComplete(), d), candidate().calculations());
    }
    private static Candidate scope(Scope scope) {
        CaseFacts f=candidate().facts();
        return new Candidate(key(), new CaseFacts(scope, f.salary(), f.savingsInterest(), f.qualifyingDepositInterest(), f.otherInterest(),
                f.incomeInventoryComplete(), f.deductionInventoryComplete()), candidate().calculations());
    }
    private static final class Controls implements SafeResultService.ControlPlane {
        CalculationKey current=key(); boolean authorized=true, deleted, integrity=true, restored=true, quarantined, failRead;
        RuleState rule=RuleState.EFFECTIVE; ReferenceState references=ReferenceState.APPROVED;
        final Map<Regime,Availability> availability=new EnumMap<>(Map.of(Regime.NEW,Availability.ALLOWED,Regime.OLD,Availability.ALLOWED));
        int reads,writes;
        @Override public DeliveryContext loadFor(Actor actor,Candidate candidate) throws SafeResultService.ControlStateUnavailable {
            reads++; if(failRead) throw new SafeResultService.ControlStateUnavailable();
            return new DeliveryContext(current,authorized,deleted,integrity,restored,rule,references,quarantined,availability);
        }
        @Override public void quarantine(CalculationKey key) { writes++; quarantined=true; }
        GuidedResult result(Candidate candidate) { return new GuidedResultService(this,verifier()).deliver(ACTOR,sign(candidate)); }
        IntakeGuide intake(Candidate candidate) { return new IntakeGuideService(this,verifier()).deliver(ACTOR,sign(draft(candidate))); }
    }

    public static void main(String[] args) throws Exception {
        if (args.length==2 && args[0].equals("--examples-only")) { examples(Path.of(args[1])); return; }
        test("unknown savings asks one focused question without tax payload", () -> {
            GuidedResult r=new Controls().result(fact(1,Fact.unknown())); Map<?,?> g=guide(r);
            check(field(g).equals("SAVINGS_INTEREST") && action(g).equals("ENTER_AMOUNT"));
            check(step(g,"SAVINGS_INTEREST").get("status").equals("MISSING")); noAmounts(SafeJson.encode(r));
        });
        test("estimated parser value needs confirmation rather than re-entry or silent acceptance", () -> {
            HumanMoneyParser.Parsed parsed=(HumanMoneyParser.Parsed)new HumanMoneyParser().parse("1 lakh",null);
            GuidedResult r=new Controls().result(fact(0,parsed.asUnconfirmedFact()));
            check(action(guide(r)).equals("CONFIRM_AMOUNT") && field(guide(r)).equals("SALARY"));
            check(!SafeJson.encode(r).contains("100000.00"));
        });
        test("conflicts require resolution and never pick an amount", () -> {
            GuidedResult r=new Controls().result(fact(2,Fact.conflict(List.of(Money.parse("111"),Money.parse("222")))));
            check(action(guide(r)).equals("RESOLVE_CONFLICT") && field(guide(r)).equals("DEPOSIT_INTEREST"));
            check(!SafeJson.encode(r).contains("111.00") && !SafeJson.encode(r).contains("222.00"));
        });
        test("confirmed zero is reviewed while unknown is missing", () -> {
            Map<?,?> g=guide(new Controls().result(fact(1,Fact.unknown())));
            check(step(g,"SAVINGS_INTEREST").get("status").equals("MISSING"));
            check(step(g,"OTHER_INTEREST").get("status").equals("REVIEWED"));
        });
        test("scope screening stays first before monetary intake", () -> {
            for(Scope s:List.of(Scope.PARTIAL,Scope.UNSUPPORTED,Scope.UNKNOWN)) {
                GuidedResult r=new Controls().result(scope(s));
                check(field(guide(r)).equals("SUPPORTED_SCOPE") && action(guide(r)).equals("REVIEW_SCOPE"));
                noAmounts(SafeJson.encode(r));
            }
        });
        test("false unknown estimated and conflicting inventory cannot count as complete", () -> {
            for(int index=0;index<3;index++) {
                for(Fact<Boolean> state:List.of(Fact.confirmed(false),Fact.<Boolean>unknown(),Fact.estimated(true),Fact.conflict(List.of(true,false)))) {
                    Map<?,?> g=guide(new Controls().result(inventory(index,state)));
                    String id=List.of("INCOME_INVENTORY","NEW_DEDUCTIONS","OLD_DEDUCTIONS").get(index);
                    check(!step(g,id).get("status").equals("REVIEWED")); check(field(g).equals(id));
                }
            }
        });
        test("unfinished old deduction review preserves only the guarded new amount", () -> {
            GuidedResult r=new Controls().result(inventory(2,Fact.unknown())); Map<?,?> g=guide(r);
            check(field(g).equals("OLD_DEDUCTIONS"));
            List<?> cards=(List<?>)g.get("regime_cards");
            check(((Map<?,?>)cards.get(0)).get("status").equals("CALCULATION_AVAILABLE"));
            check(((Map<?,?>)cards.get(1)).get("status").equals("NEEDS_REVIEW"));
            check(SafeJson.encode(r).contains("420.00") && !SafeJson.encode(r).contains("700.00"));
            check(!((Map<?,?>)r.toResponse().get("result")).containsKey("comparison"));
        });
        test("lower tax message follows the guarded recommendation", () -> {
            Map<?,?> g=guide(new Controls().result(candidate()));
            check(g.get("message").toString().startsWith("The new regime has lower annual tax"));
            check(action(g).equals("REVIEW_CALCULATION"));
        });
        test("an equal tax comparison explicitly claims no tax saving", () -> {
            Candidate c=candidate(); Candidate tie=new Candidate(c.key(),c.facts(),Map.of(
                    Regime.NEW,new RegimeCalculation(Regime.NEW,Money.parse("420"),Verification.MATCH),
                    Regime.OLD,new RegimeCalculation(Regime.OLD,Money.parse("420"),Verification.MATCH)));
            check(guide(new Controls().result(tie)).get("message").toString().contains("equal annual tax"));
            check(guide(new Controls().result(tie)).get("message").toString().contains("No tax saving"));
        });
        test("complete input checklist does not imply regime availability or filing readiness", () -> {
            Controls controls=new Controls(); controls.availability.put(Regime.OLD,Availability.UNKNOWN);
            Map<?,?> g=guide(controls.result(candidate()));
            check(g.get("progress_label").equals("8 of 8 input reviews complete"));
            check(action(g).equals("REVIEW_REGIME_AVAILABILITY"));
            Map<?,?> caps=(Map<?,?>)g.get("capabilities"); check(Boolean.FALSE.equals(caps.get("submit_return")));
            check(Boolean.FALSE.equals(caps.get("export_itr")));
        });
        test("missing comparison calculation asks for processing without fabricating a value", () -> {
            Candidate c=candidate(); Candidate single=new Candidate(c.key(),c.facts(),Map.of(Regime.NEW,c.calculations().get(Regime.NEW)));
            Map<?,?> g=guide(new Controls().result(single)); check(action(g).equals("WAIT_FOR_CALCULATION"));
        });
        test("blocked and denied result journeys have no private checklist or preparation note", () -> {
            Controls pending=new Controls(); pending.references=ReferenceState.PENDING;
            GuidedResult r=pending.result(candidate()); noPrivateGuide(guide(r)); noAmounts(SafeJson.encode(r));
            Controls denied=new Controls(); denied.authorized=false;
            GuidedResult d=denied.result(candidate()); noPrivateGuide(guide(d)); noAmounts(SafeJson.encode(d));
        });
        test("tampered signed result cannot unlock a detailed guide or quarantine write", () -> {
            Controls controls=new Controls(); SignedCandidate s=sign(candidate());
            SignedCandidate changed=new SignedCandidate(fact(0,Fact.unknown()),s.header(),s.signature());
            GuidedResult r=new GuidedResultService(controls,verifier()).deliver(ACTOR,changed);
            noPrivateGuide(guide(r)); noAmounts(SafeJson.encode(r)); check(controls.writes==0);
        });
        test("stale saved result explains recalculation while omitting private facts", () -> {
            Controls controls=new Controls(); CalculationKey k=key();
            controls.current=new CalculationKey(k.tenantId(),k.profileId(),k.caseId(),"snap2",k.periodKey(),k.ruleBundleHash(),k.engineDigest(),k.verifierDigest(),k.contractVersion(),k.settingsHash());
            GuidedResult r=controls.result(candidate()); check(action(guide(r)).equals("RECALCULATE")); noPrivateGuide(guide(r));
        });
        test("guided result uses one current control snapshot rather than re-authorizing a second path", () -> {
            Controls c=new Controls(); c.result(candidate()); check(c.reads==1);
        });
        test("preparation note contains statuses but no values or personal identities", () -> {
            Map<?,?> g=guide(new Controls().result(candidate())); String note=(String)g.get("preparation_note");
            noAmounts(note); check(note.contains("not a tax return") && note.contains("Annual salary: REVIEWED"));
            check(note.length()<2400);
        });
        test("nested guide projections and lists are immutable", () -> {
            GuidedResult result=new Controls().result(candidate()); Map<?,?> g=guide(result);
            immutable(result.toResponse()::clear); immutable(g::clear);
            immutable(((List<?>)g.get("checklist"))::clear); immutable(step(g,"SALARY")::clear);
            immutable(((Map<?,?>)g.get("next_action"))::clear);
        });
        test("input preparation is usable before approval and before any tax calculation exists", () -> {
            Controls controls=new Controls(); controls.references=ReferenceState.PENDING; controls.rule=RuleState.UNAPPROVED;
            IntakeGuide result=controls.intake(fact(1,Fact.unknown()));
            check(result.toResponse().get("status").equals("INPUT_PREPARATION_AVAILABLE"));
            check(field(guide(result)).equals("SAVINGS_INTEREST")); noAmounts(SafeJson.encode(result));
            check(!result.toResponse().containsKey("result") && !guide(result).containsKey("regime_cards"));
        });
        test("all reviewed draft inputs wait for reference validation rather than claim readiness", () -> {
            Controls controls=new Controls(); controls.references=ReferenceState.PENDING;
            Map<?,?> g=guide(controls.intake(candidate())); check(action(g).equals("WAIT_FOR_VALIDATION"));
            check(g.get("validation_notice").toString().contains("not yet available"));
        });
        test("approved draft inputs may request calculation but never expose numerical result or filing capability", () -> {
            IntakeGuide result=new Controls().intake(candidate()); check(action(guide(result)).equals("REQUEST_CALCULATION"));
            noAmounts(SafeJson.encode(result)); Map<?,?> caps=(Map<?,?>)guide(result).get("capabilities");
            check(Boolean.FALSE.equals(caps.get("submit_return")) && Boolean.FALSE.equals(caps.get("export_itr")));
        });
        test("input-only endpoint refuses numerical candidates without revealing their data", () -> {
            Controls c=new Controls(); IntakeGuide result=new IntakeGuideService(c,verifier()).deliver(ACTOR,sign(candidate()));
            check(result.toResponse().get("status").equals("DRAFT_REQUIRED")); noPrivateGuide(guide(result)); noAmounts(SafeJson.encode(result));
        });
        test("draft signature tampering cannot expose private input review states", () -> {
            Controls c=new Controls(); SignedCandidate s=sign(draft(candidate()));
            SignedCandidate bad=new SignedCandidate(draft(fact(1,Fact.unknown())),s.header(),s.signature());
            IntakeGuide r=new IntakeGuideService(c,verifier()).deliver(ACTOR,bad);
            noPrivateGuide(guide(r)); noAmounts(SafeJson.encode(r));
        });
        test("both guidance services reload revoked access on every call", () -> {
            Controls c=new Controls(); GuidedResultService results=new GuidedResultService(c,verifier()); IntakeGuideService intake=new IntakeGuideService(c,verifier());
            check(guide(results.deliver(ACTOR,sign(candidate()))).containsKey("checklist"));
            check(guide(intake.deliver(ACTOR,sign(draft(candidate())))).containsKey("checklist"));
            c.authorized=false;
            noPrivateGuide(guide(results.deliver(ACTOR,sign(candidate()))));
            noPrivateGuide(guide(intake.deliver(ACTOR,sign(draft(candidate()))))); check(c.reads==4);
        });
        test("cross tenant intake denial does not load controls", () -> {
            Controls c=new Controls(); IntakeGuide r=new IntakeGuideService(c,verifier()).deliver(new Actor("tenantB","readerA"),sign(draft(candidate())));
            noPrivateGuide(guide(r)); check(c.reads==0); noAmounts(SafeJson.encode(r));
        });
        test("intake restoration integrity deletion quarantine and outage gates conceal input states", () -> {
            List<Controls> blocked=new ArrayList<>();
            Controls restored=new Controls(); restored.restored=false; blocked.add(restored);
            Controls integrity=new Controls(); integrity.integrity=false; blocked.add(integrity);
            Controls deleted=new Controls(); deleted.deleted=true; blocked.add(deleted);
            Controls quarantine=new Controls(); quarantine.quarantined=true; blocked.add(quarantine);
            Controls outage=new Controls(); outage.failRead=true; blocked.add(outage);
            for(Controls c:blocked) { IntakeGuide r=c.intake(candidate()); noPrivateGuide(guide(r)); noAmounts(SafeJson.encode(r)); }
        });
        test("intake rejects stale snapshot and missing producer trust", () -> {
            Controls c=new Controls(); CalculationKey k=key();
            c.current=new CalculationKey(k.tenantId(),k.profileId(),k.caseId(),"snap2",k.periodKey(),k.ruleBundleHash(),k.engineDigest(),k.verifierDigest(),k.contractVersion(),k.settingsHash());
            noPrivateGuide(guide(c.intake(candidate())));
            Controls fresh=new Controls(); CandidateProofVerifier absent=new CandidateProofVerifier(id->java.util.Optional.empty(),
                    java.time.Clock.fixed(NOW,java.time.ZoneOffset.UTC),java.time.Duration.ofMinutes(10),java.time.Duration.ZERO);
            noPrivateGuide(guide(new IntakeGuideService(fresh,absent).deliver(ACTOR,sign(draft(candidate())))));
        });
        test("generated monetary fact states produce focused guidance without leaking values", () -> {
            List<Fact<Money>> states=List.of(Fact.confirmed(Money.parse("123.45")),Fact.<Money>unknown(),
                    Fact.estimated(Money.parse("234.56")),Fact.conflict(List.of(Money.parse("345.67"),Money.parse("456.78"))));
            for(int n=0;n<256;n++) {
                int bits=n; List<Fact<Money>> fields=new ArrayList<>();
                for(int i=0;i<4;i++) { fields.add(states.get(bits%4)); bits/=4; }
                CaseFacts f=candidate().facts(); Candidate c=new Candidate(key(),new CaseFacts(f.scope(),fields.get(0),fields.get(1),fields.get(2),fields.get(3),
                        f.incomeInventoryComplete(),f.deductionInventoryComplete()),Map.of());
                IntakeGuide response=new Controls().intake(c); String json=SafeJson.encode(response);
                for(String amount:List.of("123.45","234.56","345.67","456.78")) check(!json.contains(amount));
                int first=-1;
                for(int i=0;i<4;i++) if(fields.get(i).state()!=Fact.State.CONFIRMED) {first=i;break;}
                if(first<0) check(action(guide(response)).equals("REQUEST_CALCULATION"));
                else check(field(guide(response)).equals(List.of("SALARY","SAVINGS_INTEREST","DEPOSIT_INTEREST","OTHER_INTEREST").get(first)));
                generated++;
            }
        });
        test("guidance log strings omit the complete private projection", () -> {
            check(new Controls().result(candidate()).toString().equals("GuidedResult[redacted]"));
            check(new Controls().intake(candidate()).toString().equals("IntakeGuide[redacted]"));
        });
        if(args.length==1) examples(Path.of(args[0]));
        System.out.println("SCENARIOS_PASSED="+passed);
        System.out.println("SCENARIOS_FAILED="+failed);
        System.out.println("GENERATED_GUIDANCE_CASES="+generated);
        if(failed!=0) throw new AssertionError("guidance checks failed");
    }

    private static void examples(Path target) throws java.io.IOException {
        List<String> examples=new ArrayList<>();
        Controls pending=new Controls(); pending.references=ReferenceState.PENDING; pending.rule=RuleState.UNAPPROVED;
        examples.add("{\"name\":\"input_preparation_before_approval\",\"response\":"+SafeJson.encode(pending.intake(fact(1,Fact.unknown())))+"}");
        examples.add("{\"name\":\"proposed_salary_needs_confirmation\",\"response\":"+SafeJson.encode(new Controls().intake(fact(0,Fact.estimated(Money.parse("100000")))))+"}");
        examples.add("{\"name\":\"old_deductions_need_review\",\"response\":"+SafeJson.encode(new Controls().result(inventory(2,Fact.unknown())))+"}");
        examples.add("{\"name\":\"complete_synthetic_comparison\",\"response\":"+SafeJson.encode(new Controls().result(candidate()))+"}");
        examples.add("{\"name\":\"result_validation_pending\",\"response\":"+SafeJson.encode(pending.result(candidate()))+"}");
        Controls denied=new Controls(); denied.authorized=false;
        examples.add("{\"name\":\"access_denied\",\"response\":"+SafeJson.encode(denied.intake(candidate()))+"}");
        Files.writeString(target,"["+String.join(",",examples)+"]\n",StandardCharsets.UTF_8);
    }
}
