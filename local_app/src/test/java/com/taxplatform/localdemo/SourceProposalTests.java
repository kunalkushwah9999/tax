package com.taxplatform.localdemo;

import com.taxplatform.safety.*;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static com.taxplatform.safety.FactEdits.*;

final class SourceProposalTests {
    private static int passed,generated;
    private SourceProposalTests(){ }
    @FunctionalInterface private interface Test {void run()throws Exception;}
    private static void check(String name,Test test)throws Exception{test.run();passed++;System.out.println("PASS "+name);}
    private static void eq(Object a,Object b){if(!java.util.Objects.equals(a,b))throw new AssertionError("source proposal assertion failed");}
    private static void rejected(String code,Runnable task){try{task.run();}catch(ComparisonWorkspace.Rejected e){eq(e.code,code);return;}throw new AssertionError("missing rejection: "+code);}
    private static CalculationKey key(){String h="a".repeat(64);return new CalculationKey("tenant","profile","case","snapshot","FY2025-26",h,h,h,"v1",h);}
    private static String row(String id,String source,String category,String coverage,String reference,String gross,String tds){return String.join(",",id,source,category,"PAYER_A","FY2025-26",coverage,reference,gross,tds,"REPORTED");}
    private static String row(String id,String source,String gross,String tds){return row(id,source,"SALARY","ANNUAL","TOTAL",gross,tds);}
    private static byte[] csv(String... rows){return (String.join(",",StatementComparison.HEADER)+"\n"+String.join("\n",rows)+"\n").getBytes(StandardCharsets.UTF_8);}
    private static List<StatementComparison.Group> groups(String... rows){return StatementComparison.group(StatementComparison.parse(csv(rows)));}
    private static SourceProposalPolicy.Snapshot snapshot(List<StatementComparison.Group> all){var group=all.get(0);return new SourceProposalPolicy.Snapshot(key(),"v1",group,group.observations().get(0),all);}
    private static SourceProposalPolicy.Snapshot source(String... rows){return snapshot(groups(rows));}
    private static SourceProposalPolicy.Intent intent(String amount){return new SourceProposalPolicy.Intent(AmountField.SALARY,Money.parse(amount),true,true);}
    private static SourceProposalPolicy.Prepared prepare(SourceProposalPolicy.Snapshot source,String amount){return SourceProposalPolicy.prepare(source,key(),intent(amount));}
    private static ComparisonWorkspace workspace(){AtomicInteger n=new AtomicInteger();return new ComparisonWorkspace(()->"t"+n.incrementAndGet(),Clock.systemUTC());}
    private static String version(ComparisonWorkspace w){return (String)w.projection(key()).get("comparison_token");}
    private static void select(ComparisonWorkspace w,StatementComparison.Group g){w.select("review","f2",key(),version(w),g.id(),g.observations().get(0).id(),ComparisonWorkspace.Reason.CHECKED_SOURCE_RECORD,false);}
    public static void main(String[] args)throws Exception{
        check("known annual gross creates only an unconfirmed proposal",()->{var prepared=prepare(source(row("a","BANK","800000.00","40000.00")),"800000.00");if(!(prepared.command() instanceof ProposeAmount p))throw new AssertionError("source auto-confirmed");eq(p.field(),AmountField.SALARY);eq(((HumanMoneyParser.Parsed)new HumanMoneyParser().parse(p.text(),p.unit())).amount(),Money.parse("800000.00"));});
        check("several agreeing sources transfer one gross rather than a sum",()->{var p=prepare(source(row("a","BANK","800000","10"),row("b","AIS","800000","20")),"800000");eq(p.amount(),Money.parse("800000"));});
        check("TDS cannot replace or reduce gross",()->{var s=source(row("a","FORM16","800000","40000"));rejected("SOURCE_AMOUNT_CHANGED",()->prepare(s,"40000"));rejected("SOURCE_AMOUNT_CHANGED",()->prepare(s,"760000"));eq(prepare(s,"800000").amount(),Money.parse("800000"));});
        check("known zero remains a real unconfirmed gross",()->eq(prepare(source(row("a","BANK","0","UNKNOWN")),"0").amount(),Money.parse("0")));
        check("unknown gross is not zero",()->rejected("GROSS_SOURCE_REVIEW_REQUIRED",()->prepare(source(row("a","FORM26AS","UNKNOWN","0")),"0")));
        check("gross conflicts block even after a local selection",()->rejected("GROSS_SOURCE_REVIEW_REQUIRED",()->prepare(source(row("a","BANK","100","0"),row("b","AIS","200","0")),"100")));
        check("known chosen amount does not hide another missing gross",()->rejected("GROSS_SOURCE_REVIEW_REQUIRED",()->prepare(source(row("a","BANK","100","0"),row("b","FORM26AS","UNKNOWN","0")),"100")));
        check("repeated observations block even identical gross",()->rejected("GROSS_SOURCE_REVIEW_REQUIRED",()->prepare(source(row("a","AIS","100","0"),row("b","AIS","100","0")),"100")));
        check("AIS value variants block transfer",()->rejected("GROSS_SOURCE_REVIEW_REQUIRED",()->prepare(source(row("a","AIS","100","0"),row("b","AIS","100","0").replace("REPORTED","ACCEPTED")),"100")));
        check("a transaction never becomes a whole annual replacement",()->rejected("ANNUAL_SOURCE_REQUIRED",()->prepare(source(row("a","BANK","SALARY","TRANSACTION","TX1","100","0")),"100")));
        check("annual and transaction overlap blocks category",()->rejected("CATEGORY_COVERAGE_AMBIGUOUS",()->prepare(source(row("a","BANK","100","0"),row("b","AIS","SALARY","TRANSACTION","TX1","100","0")),"100")));
        check("other payer in same category cannot be silently omitted",()->rejected("CATEGORY_COVERAGE_AMBIGUOUS",()->prepare(source(row("a","BANK","100","0"),row("b","AIS","100","0").replace("PAYER_A","PAYER_B")),"100")));
        check("other annual reference in same category is ambiguous",()->rejected("CATEGORY_COVERAGE_AMBIGUOUS",()->prepare(source(row("a","BANK","100","0"),row("b","AIS","SALARY","ANNUAL","ANOTHER","100","0")),"100")));
        check("distinct income categories do not count as overlaps",()->{var all=groups(row("a","BANK","100","0"),row("b","AIS","OTHER_INTEREST","ANNUAL","TOTAL","300","0"));var g=all.stream().filter(x->x.category()==StatementComparison.Category.SALARY).findFirst().orElseThrow();eq(prepare(new SourceProposalPolicy.Snapshot(key(),"v",g,g.observations().get(0),all),"100").amount(),Money.parse("100"));});
        check("all four categories map to their own destination",()->{for(AmountField f:AmountField.values()){var s=source(row("a","BANK",f.name(),"ANNUAL","TOTAL","10","0"));eq(SourceProposalPolicy.prepare(s,key(),new SourceProposalPolicy.Intent(f,Money.parse("10"),true,true)).reference().field(),f);}});
        check("other income is outside planner inputs",()->rejected("CATEGORY_NOT_PLANNER_INPUT",()->prepare(source(row("a","BANK","OTHER_INCOME","ANNUAL","TOTAL","10","0")),"10")));
        check("client destination cannot redirect a source category",()->{var s=source(row("a","BANK","100","0"));rejected("SOURCE_FIELD_MISMATCH",()->SourceProposalPolicy.prepare(s,key(),new SourceProposalPolicy.Intent(AmountField.OTHER_INTEREST,Money.parse("100"),true,true)));});
        check("whole category acknowledgement is required",()->{var s=source(row("a","BANK","100","0"));rejected("WHOLE_CATEGORY_REVIEW_REQUIRED",()->SourceProposalPolicy.prepare(s,key(),new SourceProposalPolicy.Intent(AmountField.SALARY,Money.parse("100"),false,true)));});
        check("replacement acknowledgement is required",()->{var s=source(row("a","BANK","100","0"));rejected("REPLACEMENT_REVIEW_REQUIRED",()->SourceProposalPolicy.prepare(s,key(),new SourceProposalPolicy.Intent(AmountField.SALARY,Money.parse("100"),true,false)));});
        check("full input identity binds the proposal",()->{var s=source(row("a","BANK","100","0"));CalculationKey k=key();String[] fields={k.tenantId(),k.profileId(),k.caseId(),k.snapshotId(),k.periodKey(),k.ruleBundleHash(),k.engineDigest(),k.verifierDigest(),k.contractVersion(),k.settingsHash()};for(int i=0;i<fields.length;i++){String[] v=fields.clone();v[i]=i>=5&&i<=7||i==9?"b".repeat(64):"different";var changed=new CalculationKey(v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7],v[8],v[9]);rejected("STALE_INPUT",()->SourceProposalPolicy.prepare(s,changed,intent("100")));}});
        check("selected observation must belong to the exact group",()->{var a=source(row("a","BANK","100","0"));var b=source(row("b","BANK","100","0"));rejected("SOURCE_BINDING_INVALID",()->prepare(new SourceProposalPolicy.Snapshot(key(),"v",a.group(),b.selected(),a.allGroups()),"100"));rejected("SOURCE_BINDING_INVALID",()->prepare(new SourceProposalPolicy.Snapshot(key(),"v",a.group(),a.selected(),b.allGroups()),"100"));});
        check("source retrieval requires a recorded matching choice",()->{var w=workspace();byte[] data=csv(row("a","BANK","100","0"),row("b","AIS","100","0"));w.importRows("i","f",key(),data);var g=groups(row("a","BANK","100","0"),row("b","AIS","100","0")).get(0);rejected("SOURCE_SELECTION_REQUIRED",()->w.sourceForProposal(key(),version(w),g.id(),"a"));select(w,g);rejected("SOURCE_SELECTION_REQUIRED",()->w.sourceForProposal(key(),version(w),g.id(),"b"));eq(w.sourceForProposal(key(),version(w),g.id(),"a").selected().id(),"a");});
        check("source retrieval rejects stale comparison version",()->{var w=workspace();byte[] data=csv(row("a","BANK","100","0"));w.importRows("i","f",key(),data);String old=version(w);var g=groups(row("a","BANK","100","0")).get(0);select(w,g);rejected("STALE_RECONCILIATION",()->w.sourceForProposal(key(),old,g.id(),"a"));});
        check("reopening withdraws source retrieval permission",()->{var w=workspace();w.importRows("i","f",key(),csv(row("a","BANK","100","0")));var g=groups(row("a","BANK","100","0")).get(0);select(w,g);w.select("open","f3",key(),version(w),g.id(),null,null,true);rejected("SOURCE_SELECTION_REQUIRED",()->w.sourceForProposal(key(),version(w),g.id(),"a"));});
        check("source reference has no amount or approval assertion",()->{var p=prepare(source(row("a","BANK","800000","40000")),"800000");String text=Json.encode(p.reference().projection("ESTIMATED"));for(String secret:List.of("800000","40000","payer","valueTag","APPROVED"))eq(text.contains(secret),false);eq(p.reference().projection("ESTIMATED").get("source_authenticity_verified"),false);for(Object redacted:List.of(p,p.reference(),p.command()))eq(redacted.toString().contains("800000"),false);});
        check("retry reconstructs unconfirmed command without retained amount",()->{var p=prepare(source(row("a","BANK","100","0")),"100");eq(SourceProposalPolicy.replayCommand(p.reference(),Money.parse("100")),p.command());});
        check("all gross pairs with four categories apply the conservative policy",()->{String[] values={"UNKNOWN","0","100"};for(AmountField field:AmountField.values())for(String a:values)for(String b:values){var g=groups(row("a","BANK",field.name(),"ANNUAL","TOTAL",a,"0"),row("b","AIS",field.name(),"ANNUAL","TOTAL",b,"999"));eq(SourceProposalPolicy.barrier(g.get(0),g),!a.equals("UNKNOWN")&&a.equals(b)?"READY_FOR_USER_ATTESTATION":"GROSS_SOURCE_REVIEW_REQUIRED");generated++;}});
        check("200 groups retain category ambiguity rather than truncating coverage",()->{List<String> rows=new ArrayList<>();for(int i=0;i<200;i++)rows.add(row("a"+i,"BANK","SALARY","ANNUAL","R"+i,"100","0"));var all=groups(rows.toArray(String[]::new));eq(all.size(),200);for(var g:all)eq(SourceProposalPolicy.barrier(g,all),"CATEGORY_COVERAGE_AMBIGUOUS");});
        System.out.println(Json.encode(Map.of("source_proposal_named_groups",passed,"generated_gross_category_cases",generated,"automatic_confirmation",false,"tax_approval_granted",false)));
    }
}
