package com.taxplatform.startup;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Normalized synthetic observations only. Matching never grants ITC or filing authority. */
public final class GstReconciliation {
    public static final String HEADER="supplier_ref,document_type,document_no,document_date,taxable,igst,cgst,sgst,cess,reverse_charge,statement_itc";
    private static final List<String> MONEY=List.of("taxable","igst","cgst","sgst","cess");
    private GstReconciliation(){ }
    private static void require(boolean ok,String code){if(!ok)throw new Workspace.Problem(400,code);}
    public static String period(String value){require(value.matches("20[0-9]{2}-(?:0[1-9]|1[0-2])"),"GST_PERIOD_USE_YYYY_MM");return value;}
    public static List<Map<String,Object>> parse(String csv,boolean statement){
        require(csv.getBytes(StandardCharsets.UTF_8).length<=20000,"GST_CSV_TOO_LARGE");
        require(csv.indexOf('"')<0&&csv.indexOf('\t')<0&&csv.indexOf('\0')<0,"GST_NORMALIZED_CSV_REQUIRED");
        String normalized=csv.replace("\r\n","\n");require(normalized.indexOf('\r')<0,"GST_NORMALIZED_CSV_REQUIRED");
        if(normalized.endsWith("\n"))normalized=normalized.substring(0,normalized.length()-1);
        String[] lines=normalized.split("\n",-1);require(lines.length>=1&&lines.length<=101,"GST_USE_0_TO_100_ROWS");require(lines[0].equals(HEADER),"GST_HEADER_MISMATCH");
        List<Map<String,Object>> result=new ArrayList<>();
        for(int i=1;i<lines.length;i++){
            String[] c=lines[i].split(",",-1);require(c.length==11,"GST_COLUMN_COUNT");
            require(c[0].matches("[A-Z][A-Z0-9_]{1,31}"),"GST_SUPPLIER_ALIAS_REQUIRED");
            require(Set.of("INVOICE","CREDIT_NOTE","DEBIT_NOTE","AMENDMENT").contains(c[1]),"GST_DOCUMENT_TYPE");
            require(c[2].matches("[A-Za-z0-9][A-Za-z0-9/-]{0,31}"),"GST_DOCUMENT_NUMBER");
            LocalDate date;try{date=LocalDate.parse(c[3]);}catch(DateTimeParseException e){throw new Workspace.Problem(400,"GST_DOCUMENT_DATE");}
            require(date.toString().equals(c[3])&&date.getYear()>=2000&&date.getYear()<=2099,"GST_DOCUMENT_DATE");
            Map<String,Object> row=new LinkedHashMap<>();row.put("row",(long)i);row.put("side",statement?"STATEMENT":"BOOKS");row.put("supplier_ref",c[0]);row.put("document_type",c[1]);row.put("document_no",c[2]);row.put("document_date",c[3]);
            int start=date.getMonthValue()>=4?date.getYear():date.getYear()-1;row.put("financial_year",start+"-"+(start+1));
            for(int j=0;j<MONEY.size();j++){String value=c[j+4];require(value.equals("UNKNOWN")||value.matches("(?:0|[1-9][0-9]{0,8})(?:\\.[0-9]{1,2})?"),"GST_EXPLICIT_AMOUNT_REQUIRED");row.put(MONEY.get(j),value.equals("UNKNOWN")?value:new BigDecimal(value).setScale(2).toPlainString());}
            require(Set.of("YES","NO","UNKNOWN").contains(c[9]),"GST_REVERSE_CHARGE_REQUIRED");row.put("reverse_charge",c[9]);
            require(statement?Set.of("AVAILABLE","NOT_AVAILABLE","UNKNOWN").contains(c[10]):c[10].equals("NOT_APPLICABLE"),"GST_ITC_SOURCE_LABEL");row.put("statement_itc",c[10]);result.add(row);
        }
        return result;
    }
    private static String key(Map<String,Object> row){return String.join("|",(String)row.get("supplier_ref"),(String)row.get("financial_year"),(String)row.get("document_type"),(String)row.get("document_no"));}
    public static List<Map<String,Object>> groups(String books,String statement,String version){
        Map<String,List<Map<String,Object>>> left=new TreeMap<>(),right=new TreeMap<>();
        for(var row:parse(books,false))left.computeIfAbsent(key(row),k->new ArrayList<>()).add(row);
        for(var row:parse(statement,true))right.computeIfAbsent(key(row),k->new ArrayList<>()).add(row);
        Set<String> keys=new java.util.TreeSet<>(left.keySet());keys.addAll(right.keySet());List<Map<String,Object>> out=new ArrayList<>();
        for(String key:keys){
            var a=left.getOrDefault(key,List.of());var b=right.getOrDefault(key,List.of());List<String> issues=new ArrayList<>();
            if(a.size()>1||b.size()>1)issues.add("DUPLICATE_DOCUMENT_KEY");if(a.isEmpty())issues.add("MISSING_IN_BOOKS");if(b.isEmpty())issues.add("MISSING_IN_STATEMENT");
            if(key.contains("|AMENDMENT|"))issues.add("AMENDMENT_LINKAGE_UNSUPPORTED");
            for(var rows:List.of(a,b))for(var row:rows){
                if(MONEY.stream().anyMatch(k->row.get(k).equals("UNKNOWN"))||row.get("reverse_charge").equals("UNKNOWN"))issues.add("INCOMPLETE_DATA");
                if(row.get("reverse_charge").equals("YES"))issues.add("REVERSE_CHARGE_REVIEW");
                if(!row.get("igst").equals("UNKNOWN")&&!row.get("cgst").equals("UNKNOWN")&&!row.get("sgst").equals("UNKNOWN")&&new BigDecimal((String)row.get("igst")).signum()>0&&(new BigDecimal((String)row.get("cgst")).signum()>0||new BigDecimal((String)row.get("sgst")).signum()>0))issues.add("MIXED_TAX_COMPONENTS_REVIEW");
            }
            if(a.size()==1&&b.size()==1){for(String field:List.of("document_date","taxable","igst","cgst","sgst","cess","reverse_charge"))if(!a.get(0).get(field).equals(b.get(0).get(field)))issues.add("DIFFERENT_"+field.toUpperCase(java.util.Locale.ROOT));}
            for(var row:b)if(!row.get("statement_itc").equals("AVAILABLE"))issues.add("STATEMENT_ITC_"+row.get("statement_itc"));
            List<String> unique=issues.stream().distinct().toList();Map<String,Object> group=new LinkedHashMap<>();group.put("id",Crypto.hash("GST_NORMALIZED_V1\n"+version+"\n"+key));group.put("document_key",key);group.put("books",a);group.put("statement",b);group.put("status",unique.isEmpty()?"MATCHED_OBSERVATIONS":"REVIEW_REQUIRED");group.put("issues",unique);group.put("credit_eligibility","NOT_DETERMINED");out.add(group);
        }
        return out;
    }
    public static Map<String,Object> projection(Map<String,Object> sheet){
        if(sheet==null)return Map.of("status","NO_GST_WORKSHEET","format","GST_NORMALIZED_OBSERVATIONS_V1","header",HEADER,"filing_available",false,"credit_eligibility","NOT_DETERMINED");
        List<Map<String,Object>> groups=groups((String)sheet.get("books_csv"),(String)sheet.get("statement_csv"),(String)sheet.get("version"));Map<String,Object> decisions=Json.map(sheet.get("decisions"));int reviewed=0,issues=0;
        for(var group:groups){Object decision=decisions.get(group.get("id"));group.put("review",decision==null?Map.of("status","NOT_REVIEWED"):decision);if(decision!=null)reviewed++;if(!Json.list(group.get("issues")).isEmpty())issues++;}
        boolean coverage=List.of("same_entity_confirmed","books_complete","statement_complete").stream().allMatch(k->Boolean.TRUE.equals(sheet.get(k)));
        Map<String,Object> result=new LinkedHashMap<>();result.put("status",coverage&&issues==0&&reviewed==groups.size()?"OBSERVATION_REVIEW_COMPLETE":"PREPARATION_INCOMPLETE");result.put("format","GST_NORMALIZED_OBSERVATIONS_V1");result.put("period",sheet.get("period"));result.put("version",sheet.get("version"));result.put("groups",groups);result.put("reviewed_groups",reviewed);result.put("open_issue_groups",issues);result.put("coverage_declarations_complete",coverage);for(String k:List.of("same_entity_confirmed","books_complete","statement_complete"))result.put(k,sheet.get(k));result.put("credit_eligibility","NOT_DETERMINED");result.put("itc_claim_amount",null);result.put("nil_return_determined",false);result.put("filing_available",false);result.put("source_authenticity","NOT_VERIFIED");return result;
    }
    public static void review(Map<String,Object> sheet,Map<String,Object> command){
        if(sheet==null)throw new Workspace.Problem(409,"GST_IMPORT_REQUIRED");
        if(!sheet.get("version").equals(Workspace.text(command,"worksheet_version",1,64)))throw new Workspace.Problem(409,"STALE_GST_WORKSHEET");
        String id=Workspace.text(command,"group_id",1,64),action=Workspace.text(command,"action",1,40);
        var group=groups((String)sheet.get("books_csv"),(String)sheet.get("statement_csv"),(String)sheet.get("version")).stream().filter(g->g.get("id").equals(id)).findFirst().orElseThrow(()->new Workspace.Problem(404,"GST_GROUP_NOT_FOUND"));
        boolean matched=group.get("status").equals("MATCHED_OBSERVATIONS");
        if(!(matched?Set.of("REVIEWED_MATCH","REOPEN"):Set.of("SUPPLIER_FOLLOW_UP","BOOKS_CHECK_PENDING","TIMING_CHECK_PENDING","NO_CLAIM_REVIEW","REOPEN")).contains(action))throw new Workspace.Problem(409,"GST_ACTION_NOT_ALLOWED_FOR_GROUP");
        if(!Workspace.flag(command,"observations_reviewed"))throw new Workspace.Problem(400,"GST_EXPLICIT_REVIEW_REQUIRED");
        Map<String,Object> decisions=Json.map(sheet.get("decisions"));if(action.equals("REOPEN"))decisions.remove(id);else decisions.put(id,Map.of("status",action,"worksheet_version",sheet.get("version"),"credit_eligibility","NOT_DETERMINED"));sheet.put("decisions",decisions);
    }
}
