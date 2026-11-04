package com.taxplatform.localdemo;

import com.taxplatform.safety.CalculationKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

final class ComparisonTests {
    private static int passed,generated,rounds;
    private static final Clock CLOCK=Clock.fixed(Instant.parse("2026-11-01T01:00:00Z"),ZoneOffset.UTC);
    private ComparisonTests(){ }
    @FunctionalInterface private interface Test {void run()throws Exception;}
    private static void check(String name,Test test)throws Exception{test.run();passed++;System.out.println("PASS "+name);}
    private static void eq(Object a,Object b){if(!java.util.Objects.equals(a,b))throw new AssertionError("unexpected comparison state");}
    private static void bad(Runnable op){try{op.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError("invalid input accepted");}
    private static void rejected(String code,Runnable op){try{op.run();}catch(ComparisonWorkspace.Rejected expected){eq(expected.code,code);return;}throw new AssertionError("rejection missing: "+code);}
    private static CalculationKey key(String snapshot,String settings){String h="a".repeat(64);return new CalculationKey("t","p","c",snapshot,"FY2025-26",h,h,h,"facts-v1",settings);}
    private static CalculationKey key(){return key("s1","b".repeat(64));}
    private static byte[] csv(String... rows){return (String.join(",",StatementComparison.HEADER)+"\n"+String.join("\n",rows)+"\n").getBytes(StandardCharsets.UTF_8);}
    private static String row(String id,String source,String gross,String tds){return id+","+source+",SAVINGS_INTEREST,BANK_A,FY2025-26,ANNUAL,REF_A,"+gross+","+tds+",REPORTED";}
    private static byte[] pair(){return csv(row("a","BANK","5000.00","0.00"),row("b","AIS","6500.00","0.00"));}
    private static List<StatementComparison.Group> groups(byte[] data){return StatementComparison.group(StatementComparison.parse(data));}
    private static ComparisonWorkspace workspace(){AtomicInteger n=new AtomicInteger();return new ComparisonWorkspace(()->"token_"+n.incrementAndGet(),CLOCK);}
    private static String version(ComparisonWorkspace w){return (String)w.projection(key()).get("comparison_token");}
    private static String group(){return groups(pair()).get(0).id();}
    private static void init(ComparisonWorkspace w){w.importRows("import_1","fp_1",key(),pair());}
    private static Map<String,Object> select(ComparisonWorkspace w,String id,String v,String obs){return w.select(id,id,key(),v,group(),obs,ComparisonWorkspace.Reason.CHECKED_SOURCE_RECORD,false);}
    public static void main(String[] args)throws Exception {
        check("strict header and normalized columns",()->{eq(StatementComparison.parse(pair()).size(),2);bad(()->StatementComparison.parse("source,id\nAIS,a\n".getBytes(StandardCharsets.UTF_8)));});
        check("malformed UTF8 and overlarge bodies reject",()->{bad(()->StatementComparison.parse(new byte[]{(byte)0xff}));bad(()->StatementComparison.parse(new byte[32769]));});
        check("BOM quoted fields and CRLF accepted",()->{String value="\uFEFF"+new String(pair(),StandardCharsets.UTF_8).replace("\n","\r\n").replace("BANK_A","\"BANK_A\"");eq(StatementComparison.parse(value.getBytes(StandardCharsets.UTF_8)).size(),2);});
        check("malformed quote and multiline cells reject",()->{bad(()->StatementComparison.parse(csv(row("a","BANK","0","0")+"\"")));bad(()->StatementComparison.parse((String.join(",",StatementComparison.HEADER)+"\n\"a\nb\",BANK,x\n").getBytes(StandardCharsets.UTF_8)));});
        check("duplicate observation IDs reject rather than overwrite",()->bad(()->StatementComparison.parse(csv(row("a","BANK","0","0"),row("a","AIS","0","0")))));
        check("aliases cannot be spreadsheet formulas or HTML",()->{for(String id:List.of("=SUM(A1)","<script>","+cmd"))bad(()->StatementComparison.parse(csv(row(id,"BANK","0","0"))));});
        check("unknown and explicit zero remain distinct",()->{var a=StatementComparison.parse(csv(row("a","BANK","UNKNOWN","0.00"))).get(0);eq(a.gross().isEmpty(),true);eq(a.tds().orElseThrow().decimal(),"0.00");});
        check("canonical INR amounts only",()->{for(String x:List.of("-1","1e3","1,000","1 lakh","","0.001"))bad(()->StatementComparison.parse(csv(row("a","BANK",x,"0"))));});
        check("wrong source value kind and period reject",()->{bad(()->StatementComparison.parse(csv(row("a","BANK","0","0").replace("REPORTED","ACCEPTED"))));bad(()->StatementComparison.parse(csv(row("a","AIS","0","0").replace("FY2025-26","TY2026-27"))));});
        check("200 rows accepted and 201 rejected",()->{List<String> r=new ArrayList<>();for(int i=0;i<200;i++)r.add(row("r"+i,"BANK","0","0"));eq(StatementComparison.parse(csv(r.toArray(String[]::new))).size(),200);r.add(row("r200","BANK","0","0"));bad(()->StatementComparison.parse(csv(r.toArray(String[]::new))));});
        check("group dimensions include category payer coverage and reference",()->{String base=row("a","BANK","100","0");byte[] data=csv(base,base.replace("a,BANK","b,AIS").replace("SAVINGS_INTEREST","OTHER_INTEREST"),base.replace("a,BANK","c,AIS").replace("BANK_A","BANK_B"),base.replace("a,BANK","d,AIS").replace("ANNUAL","TRANSACTION"),base.replace("a,BANK","e,AIS").replace("REF_A","REF_B"));eq(groups(data).size(),5);});
        check("same amount and different reference never fuzzy match",()->eq(groups(csv(row("a","BANK","100","0"),row("b","AIS","100","0").replace("REF_A","REF_B"))).size(),2));
        check("gross conflict and TDS agreement independent",()->{var g=groups(pair()).get(0);eq(g.incomeStatus(),StatementComparison.Status.AMOUNT_CONFLICT);eq(g.tdsStatus(),StatementComparison.Status.AGREEMENT);});
        check("gross agreement can coexist with TDS conflict",()->{var g=groups(csv(row("a","BANK","100","10"),row("b","AIS","100","11"))).get(0);eq(g.incomeStatus(),StatementComparison.Status.AGREEMENT);eq(g.tdsStatus(),StatementComparison.Status.AMOUNT_CONFLICT);});
        check("26AS unknown gross never becomes zero income",()->{var g=groups(csv(row("a","BANK","100","10"),row("b","FORM26AS","UNKNOWN","10"))).get(0);eq(g.incomeStatus(),StatementComparison.Status.VALUE_MISSING);eq(g.tdsStatus(),StatementComparison.Status.AGREEMENT);});
        check("repeated source rows preserved not silently deduplicated",()->{var g=groups(csv(row("a","AIS","100","10"),row("b","AIS","100","10"))).get(0);eq(g.observations().size(),2);eq(g.incomeStatus(),StatementComparison.Status.REPEATED_SOURCE_OBSERVATIONS);});
        check("AIS reported processed accepted are not one trusted value",()->{var g=groups(csv(row("a","AIS","100","10"),row("b","AIS","100","10").replace("REPORTED","ACCEPTED"))).get(0);eq(g.incomeStatus(),StatementComparison.Status.SOURCE_VARIANTS);});
        check("single source is not independent agreement",()->eq(groups(csv(row("a","BANK","100","10"))).get(0).incomeStatus(),StatementComparison.Status.SINGLE_SOURCE));
        check("group identity and observation order stable under row reorder",()->eq(groups(pair()),groups(csv(row("b","AIS","6500.00","0.00"),row("a","BANK","5000.00","0.00")))));
        check("observations are immutable",()->{try{StatementComparison.parse(pair()).clear();}catch(UnsupportedOperationException expected){return;}throw new AssertionError("mutable observations");});
        check("agreement still requires explicit review",()->{ComparisonWorkspace w=workspace();w.importRows("i","f",key(),csv(row("a","BANK","100","0"),row("b","AIS","100","0")));eq(w.projection(key()).get("groups_reviewed"),0);});
        check("selection records local choice and retains source conflict",()->{ComparisonWorkspace w=workspace();init(w);select(w,"r1",version(w),"a");eq(w.projection(key()).get("groups_reviewed"),1);eq(w.projection(key()).get("filing_ready"),false);eq(w.projection(key()).get("income_facts_modified"),false);eq(w.projection(key()).get("source_authenticity_verified"),false);});
        check("foreign group observation cannot be selected",()->{ComparisonWorkspace w=workspace();byte[] data=csv(row("a","BANK","100","0"),row("b","AIS","100","0").replace("REF_A","OTHER_REF"));w.importRows("i","f",key(),data);String target=groups(data).stream().filter(g->g.observations().get(0).id().equals("a")).findFirst().orElseThrow().id();rejected("OBSERVATION_NOT_FOUND",()->w.select("r","r",key(),version(w),target,"b",ComparisonWorkspace.Reason.CHECKED_SOURCE_RECORD,false));});
        check("unknown gross cannot become selected income",()->{ComparisonWorkspace w=workspace();byte[] d=csv(row("a","FORM26AS","UNKNOWN","10"));w.importRows("i","f",key(),d);rejected("GROSS_VALUE_MISSING",()->w.select("r","r",key(),version(w),groups(d).get(0).id(),"a",ComparisonWorkspace.Reason.CHECKED_SOURCE_RECORD,false));});
        check("stale review version rejects a second writer",()->{ComparisonWorkspace w=workspace();init(w);String v=version(w);select(w,"r1",v,"a");rejected("STALE_RECONCILIATION",()->select(w,"r2",v,"b"));});
        check("full input identity invalidates source review",()->{ComparisonWorkspace w=workspace();init(w);rejected("STALE_RECONCILIATION",()->w.projection(key("s2","b".repeat(64))));rejected("STALE_RECONCILIATION",()->w.projection(key("s1","c".repeat(64))));});
        check("old import retry never erases later review",()->{ComparisonWorkspace w=workspace();init(w);select(w,"r1",version(w),"a");String latest=version(w);eq(w.importRows("import_1","fp_1",key(),pair()).get("status"),"REPLAYED");eq(version(w),latest);eq(w.projection(key()).get("groups_reviewed"),1);});
        check("old import retry never restores previous batch",()->{ComparisonWorkspace w=workspace();init(w);w.importRows("i2","f2",key(),csv(row("different","BANK","100","0")));String latest=version(w);w.importRows("import_1","fp_1",key(),pair());eq(version(w),latest);});
        check("same request ID changed body is conflict",()->{ComparisonWorkspace w=workspace();init(w);rejected("IDEMPOTENCY_CONFLICT",()->w.importRows("import_1","changed",key(),pair()));});
        check("identical explicit reimport preserves reviews",()->{ComparisonWorkspace w=workspace();init(w);select(w,"r1",version(w),"a");eq(w.importRows("new_id","new_fp",key(),pair()).get("status"),"NO_CHANGE");eq(w.projection(key()).get("groups_reviewed"),1);});
        check("new source batch clears old selections",()->{ComparisonWorkspace w=workspace();init(w);select(w,"r1",version(w),"a");w.importRows("new","new",key(),csv(row("x","BANK","50","0")));eq(w.projection(key()).get("groups_reviewed"),0);});
        check("reopening does not choose another value",()->{ComparisonWorkspace w=workspace();init(w);select(w,"r1",version(w),"a");w.select("r2","r2",key(),version(w),group(),null,null,true);eq(w.projection(key()).get("groups_reviewed"),0);});
        check("review retry cannot roll back later selection",()->{ComparisonWorkspace w=workspace();init(w);String old=version(w);select(w,"r1",old,"a");select(w,"r2",version(w),"b");String latest=version(w);eq(select(w,"r1",old,"a").get("status"),"REPLAYED");eq(version(w),latest);});
        check("change events omit raw amounts aliases and CSV",()->{ComparisonWorkspace w=workspace();init(w);select(w,"r1",version(w),"a");String text=Json.encode(w.projection(key()).get("events"));for(String s:List.of("5000.00","6500.00","BANK_A","gross_amount","REF_A"))eq(text.contains(s),false);});
        check("bounded requests retain old retries after capacity",()->{ComparisonWorkspace w=workspace();for(int i=0;i<512;i++)w.importRows("i"+i,"f"+i,key(),pair());rejected("COMPARISON_LIMIT_REACHED",()->w.importRows("extra","extra",key(),pair()));eq(w.importRows("i0","f0",key(),pair()).get("status"),"REPLAYED");});
        check("duplicate rows cannot hide other missing or conflicting values",()->{var rows=StatementComparison.parse(csv(row("a","AIS","100","10"),row("b","AIS","UNKNOWN","11"),row("c","BANK","200","10")));var flags=StatementComparison.flags(rows,true);eq(flags.containsAll(List.of(StatementComparison.Status.REPEATED_SOURCE_OBSERVATIONS,StatementComparison.Status.VALUE_MISSING,StatementComparison.Status.AMOUNT_CONFLICT)),true);});
        check("generated gross and TDS combinations remain independent",()->{String[] values={"UNKNOWN","0.00","100.00"};for(String a:values)for(String b:values)for(String x:values)for(String y:values){var g=groups(csv(row("a","BANK",a,x),row("b","AIS",b,y))).get(0);eq(g.incomeStatus(),a.equals("UNKNOWN")||b.equals("UNKNOWN")?StatementComparison.Status.VALUE_MISSING:a.equals(b)?StatementComparison.Status.AGREEMENT:StatementComparison.Status.AMOUNT_CONFLICT);eq(g.tdsStatus(),x.equals("UNKNOWN")||y.equals("UNKNOWN")?StatementComparison.Status.VALUE_MISSING:x.equals(y)?StatementComparison.Status.AGREEMENT:StatementComparison.Status.AMOUNT_CONFLICT);generated++;}});
        check("actual concurrent review writers commit one version",()->{var pool=Executors.newFixedThreadPool(2);try{for(int i=0;i<8;i++){ComparisonWorkspace w=workspace();init(w);String v=version(w);Callable<String> one=()->{try{return (String)select(w,"r1",v,"a").get("status");}catch(ComparisonWorkspace.Rejected e){return e.code;}};Callable<String> two=()->{try{return (String)select(w,"r2",v,"b").get("status");}catch(ComparisonWorkspace.Rejected e){return e.code;}};List<String> statuses=pool.invokeAll(List.of(one,two)).stream().map(f->{try{return f.get();}catch(Exception e){throw new AssertionError(e);}}).sorted().toList();eq(statuses,List.of("APPLIED","STALE_RECONCILIATION"));rounds++;}}finally{pool.shutdownNow();}});
        System.out.println(Json.encode(Map.of("comparison_named_groups",passed,"generated_amount_pairs",generated,"concurrent_review_rounds",rounds,"filing_approval_granted",false)));
    }
}
