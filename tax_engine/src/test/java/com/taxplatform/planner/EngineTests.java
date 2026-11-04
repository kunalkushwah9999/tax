package com.taxplatform.planner;

import com.taxplatform.safety.Fact;
import com.taxplatform.safety.Money;
import com.taxplatform.safety.SafetyInput.Regime;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import static com.taxplatform.planner.TaxStages.Stage.*;

public final class EngineTests {
    private static int passed,generated,mutatedStages;
    private EngineTests(){ }
    @FunctionalInterface private interface Check {void run()throws Exception;}
    private static void check(String name,Check test)throws Exception{test.run();passed++;System.out.println("PASS "+name);}
    private static void eq(Object actual,Object expected){if(!java.util.Objects.equals(actual,expected))throw new AssertionError("unexpected value: "+actual+" versus "+expected);}
    private static void amount(TaxStages stages,TaxStages.Stage stage,String expected){eq(stages.value(stage),new BigDecimal(expected).setScale(2));}
    private static void rejects(Runnable operation){try{operation.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError("expected rejection");}
    static Map<String,String> form(){return new TreeMap<>(Map.ofEntries(
        Map.entry("period",PlannerInput.PERIOD),Map.entry("residency","ROR"),Map.entry("dob","2000-10-15"),Map.entry("dob_state","CONFIRMED"),
        Map.entry("salaries","1275005.00"),Map.entry("savings","0.00"),Map.entry("deposits","0.00"),Map.entry("other_interest","0.00"),
        Map.entry("salary_state","CONFIRMED"),Map.entry("interest_state","CONFIRMED"),Map.entry("eligible_80c","0.00"),Map.entry("eligible_80d","0.00"),Map.entry("eligible_nps","0.00"),
        Map.entry("deduction_state","CONFIRMED"),Map.entry("income_inventory","REVIEWED"),Map.entry("deduction_inventory","REVIEWED"),
        Map.entry("screen_business","NO"),Map.entry("screen_special","NO"),Map.entry("screen_property_foreign","NO"),Map.entry("screen_status","NO"),
        Map.entry("screen_losses","NO"),Map.entry("screen_adjustments","NO"),Map.entry("regimes","BOTH")));}
    private static PlannerInput input(String... changes){Map<String,String> f=form();for(int i=0;i<changes.length;i+=2){if(changes[i].equals("income_state")){f.put("salary_state",changes[i+1]);f.put("interest_state",changes[i+1]);}else f.put(changes[i],changes[i+1]);}return PlannerForms.parse(f);}
    private static DualPlanner.Assessment result(Regime regime,String... changes){return new DualPlanner().evaluate(input(changes)).get(regime);}
    private static TaxStages calc(Regime regime,String... changes){DualPlanner.Assessment a=result(regime,changes);eq(a.status(),DualPlanner.Status.CALCULATED);return a.privateWorkings();}
    public static void main(String[] args)throws Exception {
        check("zero salary has no standard deduction or invented tax",()->{TaxStages s=calc(Regime.NEW,"salaries","0.00");amount(s,STANDARD_DEDUCTION,"0");amount(s,ANNUAL_TAX_BEFORE_CREDITS,"0");});
        check("low salary standard deduction is capped to salary",()->{TaxStages s=calc(Regime.NEW,"salaries","50000.00","deposits","500000.00");amount(s,STANDARD_DEDUCTION,"50000");amount(s,ROUNDED_TOTAL_INCOME,"500000");});
        check("no salary does not deduct salary allowance from interest",()->{TaxStages s=calc(Regime.OLD,"salaries","0.00","deposits","600000.00");amount(s,STANDARD_DEDUCTION,"0");amount(s,ROUNDED_TOTAL_INCOME,"600000");});
        check("two employers receive standard deduction once",()->{TaxStages s=calc(Regime.NEW,"salaries","40000.00;600000.00");amount(s,GROSS_SALARY,"640000");amount(s,STANDARD_DEDUCTION,"75000");amount(s,TAXABLE_SALARY,"565000");});
        check("old salary deduction differs from new",()->{amount(calc(Regime.OLD,"salaries","100000.00"),STANDARD_DEDUCTION,"50000");amount(calc(Regime.NEW,"salaries","100000.00"),STANDARD_DEDUCTION,"75000");});
        check("new twelve lakh threshold rebate",()->{TaxStages s=calc(Regime.NEW,"salaries","1275000.00");amount(s,SLAB_TAX,"60000");amount(s,REBATE,"60000");amount(s,ANNUAL_TAX_BEFORE_CREDITS,"0");});
        check("income rounding below threshold step",()->{amount(calc(Regime.NEW,"salaries","1275004.99"),ROUNDED_TOTAL_INCOME,"1200000");});
        check("income rounding at threshold step and marginal relief",()->{TaxStages s=calc(Regime.NEW);amount(s,ROUNDED_TOTAL_INCOME,"1200010");amount(s,SLAB_TAX,"60001.50");amount(s,REBATE,"0");amount(s,MARGINAL_RELIEF,"59991.50");amount(s,TAX_AFTER_RELIEF,"10");amount(s,CESS,"0.40");amount(s,ANNUAL_TAX_BEFORE_CREDITS,"10.40");amount(s,ZERO_CREDIT_ROUNDED_WORKING,"10");});
        check("new 1200010 is relief not an old rebate cliff",()->{amount(calc(Regime.NEW,"salaries","1275010.00"),ANNUAL_TAX_BEFORE_CREDITS,"10.40");});
        check("marginal relief applied before cess",()->{TaxStages s=calc(Regime.NEW,"salaries","1285000.00");amount(s,TAX_AFTER_RELIEF,"10000");amount(s,CESS,"400");amount(s,ANNUAL_TAX_BEFORE_CREDITS,"10400");});
        check("relief upper edge pair",()->{amount(calc(Regime.NEW,"salaries","1345580.00"),MARGINAL_RELIEF,"7");amount(calc(Regime.NEW,"salaries","1345590.00"),MARGINAL_RELIEF,"0");});
        check("old five lakh threshold and cliff are separate",()->{amount(calc(Regime.OLD,"salaries","550000.00"),ANNUAL_TAX_BEFORE_CREDITS,"0");TaxStages s=calc(Regime.OLD,"salaries","550010.00");amount(s,REBATE,"0");amount(s,MARGINAL_RELIEF,"0");amount(s,ANNUAL_TAX_BEFORE_CREDITS,"13002.08");amount(s,ZERO_CREDIT_ROUNDED_WORKING,"13000");});
        check("old under60 ten lakh slab",()->amount(calc(Regime.OLD,"salaries","1050000.00"),SLAB_TAX,"112500"));
        check("old senior ten lakh slab",()->amount(calc(Regime.OLD,"salaries","1050000.00","dob","1965-07-15"),SLAB_TAX,"110000"));
        check("old super senior ten lakh slab",()->amount(calc(Regime.OLD,"salaries","1050000.00","dob","1945-07-15"),SLAB_TAX,"100000"));
        check("80TTA includes savings and excludes FD",()->{TaxStages s=calc(Regime.OLD,"salaries","600000.00","savings","15000.00","deposits","50000.00");amount(s,LIMITED_80TTA_CLAIM,"10000");amount(s,LIMITED_80TTB_CLAIM,"0");amount(s,ROUNDED_TOTAL_INCOME,"605000");});
        check("80TTA whole eligible interest below limit",()->amount(calc(Regime.OLD,"savings","4999.99"),LIMITED_80TTA_CLAIM,"4999.99"));
        check("80TTB includes savings and eligible deposit only",()->{TaxStages s=calc(Regime.OLD,"dob","1965-07-15","savings","15000.00","deposits","60000.00","other_interest","90000.00");amount(s,LIMITED_80TTB_CLAIM,"50000");amount(s,LIMITED_80TTA_CLAIM,"0");});
        check("nonqualifying interest never receives 80TTB",()->amount(calc(Regime.OLD,"dob","1965-07-15","other_interest","90000.00"),LIMITED_80TTB_CLAIM,"0"));
        check("new regime excludes listed old deductions",()->{TaxStages s=calc(Regime.NEW,"dob","1965-07-15","savings","15000.00","deposits","60000.00","eligible_80c","150000.00","eligible_80d","100000.00","eligible_nps","50000.00");for(TaxStages.Stage x:List.of(LIMITED_80C_CLAIM,LIMITED_80D_CLAIM,LIMITED_NPS_1B_CLAIM,LIMITED_80TTA_CLAIM,LIMITED_80TTB_CLAIM,TOTAL_DEDUCTION_ALLOWED))amount(s,x,"0");});
        check("April1 sixty birthday qualifies previous FY",()->eq(calc(Regime.OLD,"dob","1966-04-01").ageBand(),TaxStages.AgeBand.SENIOR));
        check("April2 sixty birthday does not qualify previous FY",()->eq(calc(Regime.OLD,"dob","1966-04-02").ageBand(),TaxStages.AgeBand.UNDER_60));
        check("April1 eighty birthday qualifies previous FY",()->eq(calc(Regime.OLD,"dob","1946-04-01").ageBand(),TaxStages.AgeBand.SUPER_SENIOR));
        check("April2 eighty birthday remains senior previous FY",()->eq(calc(Regime.OLD,"dob","1946-04-02").ageBand(),TaxStages.AgeBand.SENIOR));
        check("60 birthday in FY switches interest deduction",()->amount(calc(Regime.OLD,"dob","1965-12-30","deposits","45000.00"),LIMITED_80TTB_CLAIM,"45000"));
        check("eligible 80C and NPS ceilings are separate",()->{TaxStages s=calc(Regime.OLD,"eligible_80c","200000.00","eligible_nps","75000.00");amount(s,LIMITED_80C_CLAIM,"150000");amount(s,LIMITED_NPS_1B_CLAIM,"50000");amount(s,TOTAL_DEDUCTION_ALLOWED,"200000");});
        check("80D over aggregate maximum unsupported rather than silently allowed",()->eq(result(Regime.OLD,"eligible_80d","100000.01").status(),DualPlanner.Status.UNSUPPORTED));
        check("aggregate deduction cannot exceed gross total income",()->{TaxStages s=calc(Regime.OLD,"salaries","51000.00","eligible_80c","150000.00","eligible_80d","100000.00","eligible_nps","50000.00");amount(s,TOTAL_DEDUCTION_ALLOWED,"1000");amount(s,TOTAL_INCOME_BEFORE_ROUNDING,"0");});
        check("cap checked after deductions and income rounding not on gross salary",()->eq(result(Regime.NEW,"salaries","5075000.00").status(),DualPlanner.Status.CALCULATED));
        check("income above cap is unsupported",()->eq(result(Regime.NEW,"salaries","5075010.00").status(),DualPlanner.Status.UNSUPPORTED));
        check("rounded cap last paise boundary",()->{eq(result(Regime.NEW,"salaries","5075004.99").status(),DualPlanner.Status.CALCULATED);eq(result(Regime.NEW,"salaries","5075005.00").status(),DualPlanner.Status.UNSUPPORTED);});
        check("cap status can differ by requested regime",()->{eq(result(Regime.NEW,"salaries","5075000.00").status(),DualPlanner.Status.CALCULATED);eq(result(Regime.OLD,"salaries","5075000.00").status(),DualPlanner.Status.UNSUPPORTED);});
        check("RNOR and NR do not inherit ROR benefits",()->{eq(result(Regime.NEW,"residency","RNOR").status(),DualPlanner.Status.UNSUPPORTED);eq(result(Regime.OLD,"residency","NR").status(),DualPlanner.Status.UNSUPPORTED);});
        check("unknown residency asks for facts",()->eq(result(Regime.NEW,"residency","UNKNOWN").status(),DualPlanner.Status.INFORMATION_REQUIRED));
        check("new Act period cannot reuse old Act rules",()->eq(result(Regime.NEW,"period","ITA2025-TY2026-27").status(),DualPlanner.Status.UNSUPPORTED));
        check("every excluded screening fact blocks starter",()->{for(String field:List.of("screen_business","screen_special","screen_property_foreign","screen_status","screen_losses","screen_adjustments"))eq(result(Regime.NEW,field,"YES").status(),DualPlanner.Status.UNSUPPORTED);});
        check("every unknown screening fact remains unknown",()->{for(String field:List.of("screen_business","screen_special","screen_property_foreign","screen_status","screen_losses","screen_adjustments"))eq(result(Regime.NEW,field,"UNKNOWN").status(),DualPlanner.Status.INFORMATION_REQUIRED);});
        check("unknown monetary values carry no fallback zeros",()->{eq(result(Regime.NEW,"income_state","UNKNOWN","salaries","UNKNOWN","savings","UNKNOWN","deposits","UNKNOWN","other_interest","UNKNOWN").status(),DualPlanner.Status.INFORMATION_REQUIRED);rejects(()->input("income_state","UNKNOWN"));});
        check("estimated amounts require confirmation",()->eq(result(Regime.NEW,"income_state","ESTIMATED").status(),DualPlanner.Status.INFORMATION_REQUIRED));
        check("income inventory is independent of monetary confirmation",()->eq(result(Regime.NEW,"income_inventory","INCOMPLETE").status(),DualPlanner.Status.INFORMATION_REQUIRED));
        check("unfinished old deductions permit only new private workings",()->{PlannerInput in=input("deduction_inventory","INCOMPLETE","deduction_state","UNKNOWN","eligible_80c","UNKNOWN","eligible_80d","UNKNOWN","eligible_nps","UNKNOWN");Map<Regime,DualPlanner.Assessment> rows=new DualPlanner().evaluate(in);eq(rows.get(Regime.NEW).status(),DualPlanner.Status.CALCULATED);eq(rows.get(Regime.OLD).status(),DualPlanner.Status.INFORMATION_REQUIRED);});
        check("DOB requires explicit confirmation",()->eq(result(Regime.OLD,"dob_state","ESTIMATED").status(),DualPlanner.Status.INFORMATION_REQUIRED));
        check("unknown DOB does not infer adulthood",()->eq(result(Regime.NEW,"dob_state","UNKNOWN","dob","UNKNOWN").status(),DualPlanner.Status.INFORMATION_REQUIRED));
        check("minor during FY and future DOB outside starter",()->{eq(result(Regime.NEW,"dob","2008-01-01").status(),DualPlanner.Status.UNSUPPORTED);eq(result(Regime.NEW,"dob","2026-12-01").status(),DualPlanner.Status.UNSUPPORTED);});
        check("invalid ISO date and negative money rejected",()->{rejects(()->input("dob","2000-02-30"));rejects(()->input("savings","-1"));rejects(()->input("savings","1e3"));rejects(()->input("savings","1,000"));});
        check("unbounded employers rejected",()->rejects(()->input("salaries",String.join(";",java.util.Collections.nCopies(17,"1.00")))));
        check("salary collection defensively copied",()->{ArrayList<Money> employers=new ArrayList<>(List.of(Money.parse("1")));PlannerInput b=input();PlannerInput in=new PlannerInput(b.period(),b.residency(),b.dateOfBirth(),Fact.confirmed(employers),b.savings(),b.deposits(),b.otherInterest(),b.eligible80C(),b.eligible80D(),b.eligibleNps1B(),b.incomeComplete(),b.deductionsComplete(),b.screening(),b.regimes());employers.clear();eq(in.salaries().confirmedValue().orElseThrow().size(),1);});
        check("forms reject unknown authority fields and duplicates",()->{Map<String,String> f=form();f.put("approved","true");rejects(()->PlannerForms.parse(f));rejects(()->FormCodec.decode("x=1&x=2".getBytes(StandardCharsets.UTF_8)));});
        check("strict form percent and UTF8 decoding",()->{for(String v:List.of("x=%FF","x=%C0%AF","x=%GG","x=%","x=%ＦＦ"))rejects(()->FormCodec.decode(v.getBytes(StandardCharsets.UTF_8)));eq(FormCodec.decode("x=a%2Bb+c".getBytes(StandardCharsets.UTF_8)).get("x"),"a+b c");});
        check("form body and field count bounded",()->{rejects(()->FormCodec.decode(new byte[16385]));rejects(()->FormCodec.decode(String.join("&",java.util.stream.IntStream.range(0,41).mapToObj(i->"x"+i+"=1").toList()).getBytes(StandardCharsets.UTF_8)));});
        check("288A ignores paise before rounding to ten",()->{eq(PrimaryEngine.roundTen(new BigDecimal("100004.99")),new BigDecimal("100000"));eq(PrimaryEngine.roundTen(new BigDecimal("100005.01")),new BigDecimal("100010"));});
        check("288B illustration has separate rounding not pre-cess rounding",()->{TaxStages s=calc(Regime.NEW);amount(s,ANNUAL_TAX_BEFORE_CREDITS,"10.40");amount(s,ZERO_CREDIT_ROUNDED_WORKING,"10");eq(PrimaryEngine.roundTen(new BigDecimal("44.99")),new BigDecimal("40"));eq(PrimaryEngine.roundTen(new BigDecimal("45.00")),new BigDecimal("50"));});
        check("agreement never approves rules references or filing",()->{Map<String,Object> r=new DualPlanner().privateReview(input());eq(r.get("independent_reference_approved"),false);eq(r.get("rule_release_approved"),false);eq(r.get("filing_ready"),false);});
        check("all twenty altered stages are detected",()->{PlannerInput in=input();for(TaxStages.Stage stage:TaxStages.Stage.values()){DualPlanner faulty=new DualPlanner((x,r)->{TaxStages s=new PrimaryEngine().calculate(x,r);EnumMap<TaxStages.Stage,BigDecimal> a=new EnumMap<>(s.amounts());a.put(stage,s.value(stage).add(BigDecimal.ONE));return new TaxStages(r,s.ageBand(),a);},new PaiseVerifier()::calculate);eq(faulty.evaluate(in).get(Regime.NEW).status(),DualPlanner.Status.VERIFIER_MISMATCH);mutatedStages++;}});
        check("mismatch review output suppresses numeric workings",()->{DualPlanner faulty=new DualPlanner((x,r)->{TaxStages s=new PrimaryEngine().calculate(x,r);EnumMap<TaxStages.Stage,BigDecimal> a=new EnumMap<>(s.amounts());a.put(CESS,s.value(CESS).add(BigDecimal.ONE));return new TaxStages(r,s.ageBand(),a);},new PaiseVerifier()::calculate);Map<String,Object> row=faulty.evaluate(input()).get(Regime.NEW).reviewProjection();eq(row.containsKey("workings"),false);});
        check("break even first weak paise and strict no win",()->{BreakEven.Analysis a=new BreakEven().analyze(input("salaries","650000.00"),Money.parse("150000"));eq(a.weak().additionalEligible80C().orElseThrow().decimal(),"99995.01");eq(a.strict().status(),"NOT_ATTAINABLE_WITHIN_DECLARED_CAPACITY");});
        check("break even existing tie gives weak zero",()->{BreakEven.Analysis a=new BreakEven().analyze(input("salaries","100000.00"),Money.parse("0"));eq(a.weak().status(),"ALREADY_SATISFIED");eq(a.weak().additionalEligible80C().orElseThrow().decimal(),"0.00");eq(a.strict().status(),"NOT_ATTAINABLE_WITHIN_DECLARED_CAPACITY");});
        check("break even capacity is eligible allowance not invented spending",()->{rejects(()->new BreakEven().analyze(input("eligible_80c","140000.00"),Money.parse("10000.01")));rejects(()->new BreakEven().analyze(input("deduction_inventory","INCOMPLETE"),Money.parse("0")));});
        check("domain strings redact monetary values and DOB",()->{eq(input().toString(),"PlannerInput[redacted]");eq(calc(Regime.NEW).toString(),"TaxStages[redacted]");});
        check("private arithmetic comparison reports an actual tie",()->{Map<String,Object> review=new DualPlanner().privateReview(input("salaries","100000.00"));Object row=review.get("arithmetic_comparison");if(!(row instanceof Map<?,?> map))throw new AssertionError("comparison missing");eq(map.get("lower_annual_tax"),"TIE");eq(map.get("difference_before_credits"),"0.00");eq(map.get("regime_election_eligibility"),"NOT_ESTABLISHED");});
        check("partial or single regime never produces a complete comparison",()->{eq(new DualPlanner().privateReview(input("regimes","NEW")).containsKey("arithmetic_comparison"),false);eq(new DualPlanner().privateReview(input("deduction_inventory","INCOMPLETE")).containsKey("arithmetic_comparison"),false);});
        check("extreme bounded employer inputs fail supported cap without overflow",()->eq(result(Regime.NEW,"salaries",String.join(";",java.util.Collections.nCopies(16,"9999999999999.99"))).status(),DualPlanner.Status.UNSUPPORTED));
        check("age band disagreement is independently detected",()->{DualPlanner faulty=new DualPlanner((x,r)->{TaxStages original=new PrimaryEngine().calculate(x,r);return new TaxStages(r,TaxStages.AgeBand.SUPER_SENIOR,original.amounts());},new PaiseVerifier()::calculate);eq(faulty.evaluate(input()).get(Regime.NEW).status(),DualPlanner.Status.VERIFIER_MISMATCH);});
        check("generated dual implementation comparisons",()->{Random random=new Random(0x5eED2026L);for(int i=0;i<4096;i++){String salary=BigDecimal.valueOf(random.nextInt(450_000_000),2).toPlainString();String age=List.of("2000-10-15","1966-04-01","1966-04-02","1946-04-01","1946-04-02").get(i%5);PlannerInput in=input("salaries",salary,"dob",age,"savings",BigDecimal.valueOf(random.nextInt(5_000_000),2).toPlainString(),"deposits",BigDecimal.valueOf(random.nextInt(5_000_000),2).toPlainString(),"eligible_80c",BigDecimal.valueOf(random.nextInt(20_000_000),2).toPlainString(),"eligible_80d",BigDecimal.valueOf(random.nextInt(10_000_001),2).toPlainString(),"eligible_nps",BigDecimal.valueOf(random.nextInt(7_500_000),2).toPlainString());for(DualPlanner.Assessment a:new DualPlanner().evaluate(in).values())eq(a.status(),DualPlanner.Status.CALCULATED);generated++;}});
        System.out.println(JsonOutput.encode(Map.of("named_groups_passed",passed,"generated_cases",generated,"altered_stage_fields_detected",mutatedStages,"independent_golden_approved",false)));
    }
}
