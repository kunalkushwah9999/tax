package com.taxplatform.startup;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Owner-private synthetic working papers. No tax rate, credit, legal deadline or filing inference. */
public final class TaxWorkbench {
    public static final String CAPITAL_HEADER="record_id,account_alias,asset_alias,event_type,trade_date,sequence,quantity,gross_amount,fees";
    public static final String TDS_HEADER="record_id,deductor_alias,section_code,financial_year,income_ref,gross_amount,tds_amount";
    public static final List<String> NOTICE_TASKS=List.of("READ_ORIGINAL_NOTICE","CHECK_PERIOD_AND_IDENTITY","GATHER_SUPPORTING_RECORDS","REVIEW_RESPONSE_AND_STATED_DATE");
    private static final LocalDate START=LocalDate.of(2025,4,1),END=LocalDate.of(2026,3,31);
    private static final Set<String> MATERIAL=Set.of("CAPITAL_IMPORT","CAPITAL_CLEAR","TDS_IMPORT","TDS_CLEAR");
    private TaxWorkbench(){ }
    private static void require(boolean ok,String code){if(!ok)throw new Workspace.Problem(400,code);}
    static Map<String,Object> state(Map<String,Object> row){return row.get("tax_workbench")==null?new LinkedHashMap<>():Json.map(row.get("tax_workbench"));}
    private static Map<String,Object> module(Map<String,Object> work,String name){return work.get(name)==null?null:Json.map(work.get(name));}
    private static String alias(String value){require(value.matches("[A-Z][A-Z0-9_-]{0,31}")&&!value.matches("[A-Z]{5}[0-9]{4}[A-Z]")&&!value.matches("[A-Z]{4}[0-9]{5}[A-Z]"),"WORKBENCH_SYNTHETIC_ALIAS_REQUIRED");return value;}
    private static String label(String value){require(value.matches("[A-Za-z0-9][A-Za-z0-9_()./-]{0,31}"),"WORKBENCH_LABEL_REQUIRED");return value;}
    private static LocalDate date(String value){try{LocalDate d=LocalDate.parse(value);require(d.toString().equals(value)&&d.getYear()>=1900&&d.getYear()<=2099,"WORKBENCH_DATE_REQUIRED");return d;}catch(java.time.format.DateTimeParseException e){throw new Workspace.Problem(400,"WORKBENCH_DATE_REQUIRED");}}
    private static Long money(String value){if(value.equals("UNKNOWN"))return null;require(value.matches("(?:0|[1-9][0-9]{0,8})(?:\\.[0-9]{1,2})?"),"WORKBENCH_EXPLICIT_AMOUNT_REQUIRED");return new BigDecimal(value).movePointRight(2).longValueExact();}
    private static String amount(Long value){return value==null?"UNKNOWN":BigDecimal.valueOf(value,2).toPlainString();}
    private static long units(String value){require(value.matches("(?:0|[1-9][0-9]{0,8})(?:\\.[0-9]{1,6})?"),"CAPITAL_QUANTITY_REQUIRED");long n=new BigDecimal(value).movePointRight(6).longValueExact();require(n>0,"CAPITAL_POSITIVE_QUANTITY_REQUIRED");return n;}
    private static String quantity(long n){return BigDecimal.valueOf(n,6).stripTrailingZeros().toPlainString();}
    private static List<String[]> csv(String value,String header,int columns,int maxRows){
        require(value.getBytes(StandardCharsets.UTF_8).length<=20000,"WORKBENCH_CSV_TOO_LARGE");require(value.indexOf('"')<0&&value.indexOf('\t')<0&&value.indexOf('\0')<0,"WORKBENCH_NORMALIZED_CSV_REQUIRED");
        String normalized=value.replace("\r\n","\n");require(normalized.indexOf('\r')<0,"WORKBENCH_NORMALIZED_CSV_REQUIRED");if(normalized.endsWith("\n"))normalized=normalized.substring(0,normalized.length()-1);
        String[] lines=normalized.split("\n",-1);require(lines.length>=1&&lines.length<=maxRows+1,"WORKBENCH_ROW_LIMIT");require(lines[0].equals(header),"WORKBENCH_HEADER_MISMATCH");
        List<String[]> rows=new ArrayList<>();Set<String> ids=new HashSet<>();for(int i=1;i<lines.length;i++){String[] c=lines[i].split(",",-1);require(c.length==columns,"WORKBENCH_COLUMN_COUNT");label(c[0]);require(ids.add(c[0]),"WORKBENCH_DUPLICATE_RECORD_ID");rows.add(c);}return rows;
    }
    public static List<Map<String,Object>> parseCapital(String value){
        List<Map<String,Object>> rows=new ArrayList<>();Set<String> order=new HashSet<>();
        for(String[] c:csv(value,CAPITAL_HEADER,9,120)){
            alias(c[1]);alias(c[2]);require(Set.of("BUY","SELL").contains(c[3]),"CAPITAL_BUY_SELL_ONLY");LocalDate d=date(c[4]);
            require(!d.isAfter(END)&&(!c[3].equals("SELL")||!d.isBefore(START)),"CAPITAL_PERIOD_UNSUPPORTED");
            require(c[5].matches("[1-9][0-9]{0,5}"),"CAPITAL_SEQUENCE_REQUIRED");long sequence=Long.parseLong(c[5]);require(order.add(c[1]+"|"+c[2]+"|"+c[4]+"|"+sequence),"CAPITAL_AMBIGUOUS_ORDER");
            Map<String,Object> row=new LinkedHashMap<>();row.put("record_id",c[0]);row.put("account_alias",c[1]);row.put("asset_alias",c[2]);row.put("event_type",c[3]);row.put("trade_date",c[4]);row.put("sequence",sequence);row.put("quantity",quantity(units(c[6])));row.put("gross_amount",amount(money(c[7])));row.put("fees",amount(money(c[8])));rows.add(row);
        }
        rows.sort(Comparator.<Map<String,Object>,String>comparing(r->(String)r.get("trade_date")).thenComparingLong(r->((Number)r.get("sequence")).longValue()).thenComparing(r->(String)r.get("record_id")));return rows;
    }
    private static final class Lot {
        final Map<String,Object> row;final long total;long consumed;
        Lot(Map<String,Object> row){this.row=row;total=units((String)row.get("quantity"));}
    }
    /** Cumulative integer allocation conserves each entered paise across all partial disposals. */
    private static Long allocation(Long value,long before,long matched,long total){if(value==null)return null;BigInteger v=BigInteger.valueOf(value),t=BigInteger.valueOf(total);return v.multiply(BigInteger.valueOf(before+matched)).divide(t).subtract(v.multiply(BigInteger.valueOf(before)).divide(t)).longValueExact();}
    public static Map<String,Object> capital(Map<String,Object> sheet){
        if(sheet==null)return Map.of("status","NO_CAPITAL_WORKSHEET","header",CAPITAL_HEADER,"taxable_gain_determined",false);
        Map<String,ArrayDeque<Lot>> lots=new TreeMap<>();List<Map<String,Object>> matches=new ArrayList<>(),unmatched=new ArrayList<>(),remaining=new ArrayList<>();List<String> issues=new ArrayList<>();long totalDifference=0;int sales=0;
        List<Object> records=Json.list(sheet.get("records"));
        for(Object raw:records){Map<String,Object> row=Json.map(raw);String key=row.get("account_alias")+"|"+row.get("asset_alias");ArrayDeque<Lot> queue=lots.computeIfAbsent(key,k->new ArrayDeque<>());
            if(row.get("event_type").equals("BUY")){queue.add(new Lot(row));continue;}
            sales++;long total=units((String)row.get("quantity")),consumed=0;Long gross=money((String)row.get("gross_amount")),fees=money((String)row.get("fees"));
            while(consumed<total&&!queue.isEmpty()){
                Lot buy=queue.getFirst();long matched=Math.min(total-consumed,buy.total-buy.consumed);Long cost=allocation(money((String)buy.row.get("gross_amount")),buy.consumed,matched,buy.total),proceeds=allocation(gross,consumed,matched,total),buyFee=allocation(money((String)buy.row.get("fees")),buy.consumed,matched,buy.total),sellFee=allocation(fees,consumed,matched,total);
                Map<String,Object> line=new LinkedHashMap<>();for(String k:List.of("account_alias","asset_alias"))line.put(k,row.get(k));line.put("buy_record_id",buy.row.get("record_id"));line.put("sell_record_id",row.get("record_id"));line.put("buy_trade_date",buy.row.get("trade_date"));line.put("sell_trade_date",row.get("trade_date"));line.put("quantity",quantity(matched));line.put("acquisition_gross",amount(cost));line.put("disposal_gross",amount(proceeds));line.put("buy_fees",amount(buyFee));line.put("sell_fees",amount(sellFee));line.put("gross_book_difference",cost==null||proceeds==null?"UNKNOWN":amount(proceeds-cost));line.put("taxable_gain",null);line.put("tax_treatment","NOT_DETERMINED");matches.add(line);
                if(cost==null||proceeds==null||buyFee==null||sellFee==null)issues.add("UNKNOWN_MATCHED_AMOUNT");if(cost!=null&&proceeds!=null)totalDifference=Math.addExact(totalDifference,proceeds-cost);
                consumed+=matched;buy.consumed+=matched;if(buy.consumed==buy.total)queue.removeFirst();
            }
            if(consumed<total){issues.add("UNMATCHED_SALE_QUANTITY");unmatched.add(Map.of("record_id",row.get("record_id"),"account_alias",row.get("account_alias"),"asset_alias",row.get("asset_alias"),"quantity",quantity(total-consumed),"unallocated_disposal_gross",amount(allocation(gross,consumed,total-consumed,total)),"unallocated_fees",amount(allocation(fees,consumed,total-consumed,total))));}
        }
        for(var queue:lots.values())for(Lot lot:queue){remaining.add(Map.of("record_id",lot.row.get("record_id"),"account_alias",lot.row.get("account_alias"),"asset_alias",lot.row.get("asset_alias"),"quantity",quantity(lot.total-lot.consumed),"remaining_acquisition_gross",amount(allocation(money((String)lot.row.get("gross_amount")),lot.consumed,lot.total-lot.consumed,lot.total)),"remaining_fees",amount(allocation(money((String)lot.row.get("fees")),lot.consumed,lot.total-lot.consumed,lot.total))));}
        if(!Boolean.TRUE.equals(sheet.get("coverage_checked"))||!Boolean.TRUE.equals(sheet.get("same_entity_confirmed")))issues.add("COVERAGE_REVIEW_REQUIRED");if(!sheet.get("complex_events").equals("NO"))issues.add("COMPLEX_EVENTS_NOT_RESOLVED");if(!Boolean.TRUE.equals(sheet.get("reviewed")))issues.add("WORKING_NOT_REVIEWED");if(records.isEmpty())issues.add("NO_RECORDS");
        Map<String,Object> out=new LinkedHashMap<>();out.put("status",issues.isEmpty()?"BOOKKEEPING_REVIEW_COMPLETE":"PREPARATION_INCOMPLETE");out.put("version",sheet.get("version"));out.put("records",records);out.put("matches",matches);out.put("unmatched_sales",unmatched);out.put("remaining_lots",remaining);out.put("issues",issues.stream().distinct().toList());out.put("sale_count",sales);out.put("gross_book_difference_total",issues.isEmpty()?amount(totalDifference):null);out.put("reviewed",sheet.get("reviewed"));for(String k:List.of("coverage_checked","same_entity_confirmed","complex_events"))out.put(k,sheet.get(k));out.put("method","FIFO_ENTERED_ORDER_WITHIN_ACCOUNT_AND_ASSET");out.put("fees_deducted_from_difference",false);out.put("taxable_gain",null);out.put("taxable_gain_determined",false);out.put("source_authenticity","NOT_VERIFIED");return out;
    }
    public static List<Map<String,Object>> parseTds(String value){
        List<Map<String,Object>> records=new ArrayList<>();for(String[] c:csv(value,TDS_HEADER,7,80)){alias(c[1]);label(c[2]);require(c[3].equals("FY2025-26"),"TDS_PERIOD_UNSUPPORTED");alias(c[4]);Map<String,Object> row=new LinkedHashMap<>();row.put("record_id",c[0]);row.put("deductor_alias",c[1]);row.put("section_code",c[2]);row.put("financial_year",c[3]);row.put("income_ref",c[4]);row.put("gross_amount",amount(money(c[5])));row.put("tds_amount",amount(money(c[6])));records.add(row);}return records;
    }
    private static String tdsKey(Map<String,Object> row){return String.join("|",(String)row.get("deductor_alias"),(String)row.get("section_code"),(String)row.get("financial_year"),(String)row.get("income_ref"));}
    public static Map<String,Object> tds(Map<String,Object> sheet){
        if(sheet==null)return Map.of("status","NO_TDS_WORKSHEET","header",TDS_HEADER,"tax_credit_eligibility","NOT_DETERMINED");
        Map<String,List<Map<String,Object>>> left=new TreeMap<>(),right=new TreeMap<>();for(String side:List.of("certificate","statement"))for(Object raw:Json.list(sheet.get(side))){Map<String,Object> r=Json.map(raw);(side.equals("certificate")?left:right).computeIfAbsent(tdsKey(r),k->new ArrayList<>()).add(r);}
        Set<String> keys=new java.util.TreeSet<>(left.keySet());keys.addAll(right.keySet());List<Map<String,Object>> groups=new ArrayList<>();Map<String,Object> decisions=Json.map(sheet.get("decisions"));int reviewed=0,issueGroups=0;long statementTds=0;boolean knownUnique=true;
        for(String key:keys){List<Map<String,Object>> a=left.getOrDefault(key,List.of()),b=right.getOrDefault(key,List.of());List<String> issues=new ArrayList<>();if(a.size()>1||b.size()>1)issues.add("DUPLICATE_OBSERVATIONS");if(a.isEmpty())issues.add("MISSING_CERTIFICATE_OBSERVATION");if(b.isEmpty())issues.add("MISSING_STATEMENT_OBSERVATION");
            for(var records:List.of(a,b))for(var r:records)if(r.get("gross_amount").equals("UNKNOWN")||r.get("tds_amount").equals("UNKNOWN"))issues.add("UNKNOWN_AMOUNT");
            if(a.size()==1&&b.size()==1)for(String k:List.of("gross_amount","tds_amount"))if(!a.get(0).get(k).equals(b.get(0).get(k)))issues.add("DIFFERENT_"+k.toUpperCase(java.util.Locale.ROOT));
            String groupId=Crypto.hash("TDS_OBSERVATIONS_V1\n"+sheet.get("version")+"\n"+key);Object review=decisions.get(groupId);Map<String,Object> g=new LinkedHashMap<>();g.put("id",groupId);g.put("observation_key",key);g.put("certificate",a);g.put("statement",b);g.put("issues",issues.stream().distinct().toList());g.put("status",issues.isEmpty()?"MATCHED_OBSERVATIONS":"REVIEW_REQUIRED");g.put("review",review==null?Map.of("status","NOT_REVIEWED"):review);g.put("tax_credit_eligibility","NOT_DETERMINED");groups.add(g);if(review!=null)reviewed++;if(!issues.isEmpty())issueGroups++;
            if(b.size()!=1||b.get(0).get("tds_amount").equals("UNKNOWN"))knownUnique=false;else statementTds=Math.addExact(statementTds,money((String)b.get(0).get("tds_amount")));
        }
        boolean coverage=List.of("same_entity_confirmed","certificate_complete","statement_complete").stream().allMatch(k->Boolean.TRUE.equals(sheet.get(k)));boolean complete=coverage&&!keys.isEmpty()&&issueGroups==0&&reviewed==groups.size();
        Map<String,Object> out=new LinkedHashMap<>();out.put("status",complete?"OBSERVATION_REVIEW_COMPLETE":"PREPARATION_INCOMPLETE");out.put("version",sheet.get("version"));out.put("groups",groups);out.put("reviewed_groups",reviewed);out.put("open_issue_groups",issueGroups);out.put("coverage_declarations_complete",coverage);out.put("statement_tds_observation_total",complete&&knownUnique?amount(statementTds):null);out.put("tax_credit_eligibility","NOT_DETERMINED");out.put("claim_amount",null);out.put("source_authenticity","NOT_VERIFIED");for(String k:List.of("same_entity_confirmed","certificate_complete","statement_complete"))out.put(k,sheet.get(k));return out;
    }
    public static void validateFields(Map<String,Object> body){
        String op=Workspace.text(body,"operation",1,32);Set<String> extra=switch(op){
            case "CAPITAL_IMPORT"->Set.of("csv","synthetic_confirmed","same_entity_confirmed","coverage_checked","complex_events");
            case "CAPITAL_REVIEW"->Set.of("observations_reviewed");
            case "CAPITAL_CLEAR","TDS_CLEAR"->Set.of();
            case "TDS_IMPORT"->Set.of("certificate_csv","statement_csv","synthetic_confirmed","same_entity_confirmed","certificate_complete","statement_complete");
            case "TDS_REVIEW"->Set.of("group_id","action","observations_reviewed");
            case "NOTICE_SAVE"->Set.of("notice_id","notice_alias","category","section_label","received_on","stated_due_on","status","checklist","draft","receipt_alias","reported_on","source_checked","synthetic_confirmed");
            case "NOTICE_DELETE"->Set.of("notice_id");default->throw new Workspace.Problem(400,"WORKBENCH_UNKNOWN_OPERATION");};
        Set<String> fields=new HashSet<>(Set.of("operation","request_id","expected_revision","workbench_version"));fields.addAll(extra);require(body.keySet().equals(fields),"UNEXPECTED_FIELDS");
    }
    private static void synthetic(Map<String,Object> b){require(Workspace.flag(b,"synthetic_confirmed"),"SYNTHETIC_DATA_CONFIRMATION_REQUIRED");}
    public static boolean apply(Map<String,Object> row,Map<String,Object> body,Clock clock){
        Map<String,Object> work=state(row);String expected=Workspace.text(body,"workbench_version",0,64);if(!expected.equals(work.getOrDefault("version","")))throw new Workspace.Problem(409,"STALE_TAX_WORKBENCH");String op=(String)body.get("operation");
        switch(op){
            case "CAPITAL_IMPORT"->{synthetic(body);Map<String,Object> s=new LinkedHashMap<>();s.put("records",parseCapital(Workspace.text(body,"csv",1,20000)));for(String k:List.of("same_entity_confirmed","coverage_checked"))s.put(k,Workspace.flag(body,k));String complex=Workspace.text(body,"complex_events",1,7);require(Set.of("YES","NO","UNKNOWN").contains(complex),"CAPITAL_COMPLEX_EVENTS_DECLARATION_REQUIRED");s.put("complex_events",complex);s.put("reviewed",false);s.put("version",Workspace.id());work.put("capital",s);}
            case "CAPITAL_REVIEW"->{Map<String,Object> s=module(work,"capital");if(s==null)throw new Workspace.Problem(409,"CAPITAL_IMPORT_REQUIRED");require(Workspace.flag(body,"observations_reviewed"),"WORKBENCH_EXPLICIT_REVIEW_REQUIRED");s.put("reviewed",true);work.put("capital",s);}
            case "CAPITAL_CLEAR"->work.remove("capital");
            case "TDS_IMPORT"->{synthetic(body);Map<String,Object> s=new LinkedHashMap<>();s.put("certificate",parseTds(Workspace.text(body,"certificate_csv",1,20000)));s.put("statement",parseTds(Workspace.text(body,"statement_csv",1,20000)));for(String k:List.of("same_entity_confirmed","certificate_complete","statement_complete"))s.put(k,Workspace.flag(body,k));s.put("decisions",new LinkedHashMap<>());s.put("version",Workspace.id());work.put("tds",s);}
            case "TDS_REVIEW"->{Map<String,Object> s=module(work,"tds");if(s==null)throw new Workspace.Problem(409,"TDS_IMPORT_REQUIRED");require(Workspace.flag(body,"observations_reviewed"),"WORKBENCH_EXPLICIT_REVIEW_REQUIRED");String group=Workspace.text(body,"group_id",64,64),action=Workspace.text(body,"action",1,40);Map<String,Object> g=Json.list(tds(s).get("groups")).stream().map(Json::map).filter(r->r.get("id").equals(group)).findFirst().orElseThrow(()->new Workspace.Problem(404,"TDS_GROUP_NOT_FOUND"));Set<String> allowed=g.get("status").equals("MATCHED_OBSERVATIONS")?Set.of("REVIEWED_OBSERVATIONS","REOPEN"):Set.of("DEDUCTOR_FOLLOW_UP","DOCUMENT_CHECK_PENDING","TIMING_CHECK_PENDING","NO_CLAIM_REVIEW","REOPEN");if(!allowed.contains(action))throw new Workspace.Problem(409,"TDS_ACTION_NOT_ALLOWED_FOR_GROUP");Map<String,Object> decisions=Json.map(s.get("decisions"));if(action.equals("REOPEN"))decisions.remove(group);else decisions.put(group,Map.of("status",action,"tax_credit_eligibility","NOT_DETERMINED"));s.put("decisions",decisions);work.put("tds",s);}
            case "TDS_CLEAR"->work.remove("tds");
            case "NOTICE_SAVE"->saveNotice(work,body,clock);
            case "NOTICE_DELETE"->{Map<String,Object> notices=work.get("notices")==null?new LinkedHashMap<>():Json.map(work.get("notices"));if(notices.remove(Workspace.text(body,"notice_id",1,64))==null)throw new Workspace.Problem(404,"NOTICE_NOT_FOUND");work.put("notices",notices);}
            default->throw new IllegalStateException("unhandled workbench operation");
        }
        work.put("version",Workspace.id());require(Json.bytes(work).length<=131072,"WORKBENCH_STORAGE_LIMIT");row.put("tax_workbench",work);return MATERIAL.contains(op);
    }
    private static void saveNotice(Map<String,Object> work,Map<String,Object> b,Clock clock){
        synthetic(b);Map<String,Object> notices=work.get("notices")==null?new LinkedHashMap<>():Json.map(work.get("notices"));String id=Workspace.text(b,"notice_id",0,64);if(id.isEmpty()){if(notices.size()>=8)throw new Workspace.Problem(409,"NOTICE_LIMIT");id=Workspace.id();}else if(!notices.containsKey(id))throw new Workspace.Problem(404,"NOTICE_NOT_FOUND");
        String name=alias(Workspace.text(b,"notice_alias",1,32)),category=Workspace.text(b,"category",1,32),section=label(Workspace.text(b,"section_label",1,32)),received=date(Workspace.text(b,"received_on",10,10)).toString(),due=Workspace.text(b,"stated_due_on",7,10);
        require(Set.of("INCOME_TAX","GST","OTHER_TAX").contains(category),"NOTICE_CATEGORY_REQUIRED");if(!due.equals("UNKNOWN"))date(due);
        String status=Workspace.text(b,"status",1,40),draft=Workspace.text(b,"draft",0,3000),receipt=Workspace.text(b,"receipt_alias",0,32),reported=Workspace.text(b,"reported_on",0,10);boolean checked=Workspace.flag(b,"source_checked");
        require(Set.of("OPEN","DOCUMENTS_PREPARING","RESPONSE_DRAFTED","SUBMISSION_REPORTED_UNVERIFIED","CLOSED_BY_USER").contains(status),"NOTICE_STATUS_REQUIRED");
        Map<String,Object> checklist=Json.map(b.get("checklist"));require(checklist.keySet().equals(Set.copyOf(NOTICE_TASKS))&&checklist.values().stream().allMatch(v->v instanceof String&&Set.of("TODO","DONE","NOT_APPLICABLE_USER").contains(v)),"NOTICE_CHECKLIST_REQUIRED");
        if(status.equals("RESPONSE_DRAFTED"))require(!draft.isBlank(),"NOTICE_DRAFT_REQUIRED");
        if(!receipt.isEmpty())alias(receipt);if(!reported.isEmpty())date(reported);
        if(status.equals("SUBMISSION_REPORTED_UNVERIFIED"))require(checked&&!receipt.isEmpty()&&!reported.isEmpty(),"NOTICE_USER_REPORTED_RECEIPT_REQUIRED");
        Map<String,Object> n=new LinkedHashMap<>();n.put("id",id);n.put("notice_alias",name);n.put("category",category);n.put("section_label",section);n.put("received_on",received);n.put("stated_due_on",due);n.put("status",status);n.put("checklist",checklist);n.put("draft",draft);n.put("receipt_alias",receipt);n.put("reported_on",reported);n.put("source_checked",checked);n.put("updated_at",clock.instant().toString());n.put("official_submission_verified",false);notices.put(id,n);work.put("notices",notices);
    }
    public static boolean hasCapitalSales(Map<String,Object> row){Map<String,Object> s=module(state(row),"capital");return s!=null&&Json.list(s.get("records")).stream().map(Json::map).anyMatch(r->r.get("event_type").equals("SELL"));}
    public static boolean hasNotices(Map<String,Object> row){Object notices=state(row).get("notices");return notices!=null&&!Json.map(notices).isEmpty();}
    public static Map<String,Object> projection(Map<String,Object> row,Clock clock){
        Map<String,Object> work=state(row);List<Map<String,Object>> notices=new ArrayList<>();LocalDate today=LocalDate.now(clock.withZone(ZoneId.of("Asia/Kolkata")));
        if(work.get("notices")!=null)for(Object raw:Json.map(work.get("notices")).values()){
            Map<String,Object> n=new LinkedHashMap<>(Json.map(raw));String due=(String)n.get("stated_due_on");Long days=due.equals("UNKNOWN")?null:ChronoUnit.DAYS.between(today,date(due));n.put("days_to_user_stated_date",days);n.put("stated_date_status",days==null?"UNKNOWN":days<0?"PAST_USER_STATED_DATE":days<=7?"WITHIN_7_DAYS_OF_USER_STATED_DATE":"FUTURE_USER_STATED_DATE");n.put("received_after_stated_date",!due.equals("UNKNOWN")&&date((String)n.get("received_on")).isAfter(date(due)));n.put("legal_deadline_determined",false);notices.add(n);
        }
        notices.sort(Comparator.comparing(n->(String)n.get("updated_at")));Map<String,Object> out=new LinkedHashMap<>();out.put("format","TAX_PREPARATION_WORKBENCH_NOT_A_RETURN");out.put("version",work.getOrDefault("version",""));out.put("case_revision",row.get("revision"));out.put("period","FY2025-26");out.put("capital",capital(module(work,"capital")));out.put("tds",tds(module(work,"tds")));out.put("notices",notices);out.put("notice_tasks",NOTICE_TASKS);out.put("today_ist",today.toString());out.put("source_authenticity","NOT_VERIFIED");out.put("tax_values_released",false);out.put("filing_available",false);out.put("ca_approval",false);return out;
    }
}
