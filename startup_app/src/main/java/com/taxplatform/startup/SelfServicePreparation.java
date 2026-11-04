package com.taxplatform.startup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Preparation scope and tasks, explicitly separate from statutory form eligibility and tax approval. */
public final class SelfServicePreparation {
    public static final Map<String,String> QUESTIONS;
    private static final Set<String> REQUIRED_YES=Set.of("individual","ordinary_resident","adult_under_60_throughout_fy","salary_and_ordinary_interest_only");
    static{Map<String,String> q=new LinkedHashMap<>();q.put("individual","This draft is for an individual.");q.put("ordinary_resident","Resident and ordinarily resident throughout the relevant period, after checking applicable residency rules.");q.put("adult_under_60_throughout_fy","Age is at least 18 and below 60 throughout the financial year.");q.put("salary_and_ordinary_interest_only","All income is salary and ordinary separated interest covered by this preparation pack.");q.put("business_or_profession","Business or professional income is present.");q.put("capital_gains","Capital gains, including shares, funds, property or virtual digital assets, are present.");q.put("house_property","House-property income or loss is present.");q.put("foreign_income_or_assets","Foreign income, assets, accounts or foreign reporting questions are present.");q.put("agricultural_income","Agricultural income is present.");q.put("special_rate_or_other_income","Special-rate or any other uncovered income is present.");q.put("loss_adjustment","Current or carried-forward losses require adjustment.");q.put("directorship_or_unlisted_shares","Directorship or unlisted equity reporting questions are present.");q.put("audit_or_certification","An audit or statutory certification requirement has been identified.");q.put("notice_or_dispute","A tax notice, dispute, revised/updated return or earlier-year correction is involved.");QUESTIONS=java.util.Collections.unmodifiableMap(q);}
    private SelfServicePreparation(){ }
    private static Object canonical(Object value){if(value instanceof Map<?,?> m){Map<String,Object> out=new java.util.TreeMap<>();m.forEach((k,v)->out.put((String)k,canonical(v)));return out;}if(value instanceof List<?> l)return l.stream().map(SelfServicePreparation::canonical).toList();return value;}
    public static String fingerprint(Map<String,Object> row){Map<String,Object> relevant=new LinkedHashMap<>();for(String k:List.of("period","snapshot","inputs","profile","reviews","documents","source_version","source_snapshot","tax_workbench"))relevant.put(k,row.get(k));return Crypto.hash(Json.encode(canonical(relevant)));}
    public static Map<String,Object> scope(Map<String,Object> body,Map<String,Object> row){
        if(!Workspace.text(body,"period",1,32).equals("FY2025-26-AY2026-27"))throw new Workspace.Problem(400,"ITR_PREPARATION_PERIOD_UNSUPPORTED");
        Map<String,Object> answers=Json.map(body.get("answers"));if(!answers.keySet().equals(QUESTIONS.keySet())||answers.values().stream().anyMatch(v->!(v instanceof String)||!Set.of("YES","NO","UNKNOWN").contains(v)))throw new Workspace.Problem(400,"ITR_SCOPE_ANSWERS_REQUIRED");
        if(!Workspace.flag(body,"answers_reviewed"))throw new Workspace.Problem(400,"ITR_EXPLICIT_SCOPE_REVIEW_REQUIRED");
        return new LinkedHashMap<>(Map.of("period","FY2025-26-AY2026-27","answers",new LinkedHashMap<>(answers),"reviewed_fingerprint",fingerprint(row),"declaration_only",true));
    }
    public static Map<String,Object> itr(Map<String,Object> row,Map<String,Object> saved){
        Map<String,Object> answers=saved==null?new LinkedHashMap<>():new LinkedHashMap<>(Json.map(saved.get("answers")));if(saved==null)QUESTIONS.keySet().forEach(k->answers.put(k,"UNKNOWN"));
        List<String> unknown=new ArrayList<>(),outside=new ArrayList<>();for(String key:QUESTIONS.keySet()){Object value=answers.get(key);if(value.equals("UNKNOWN"))unknown.add(key);else if(!value.equals(REQUIRED_YES.contains(key)?"YES":"NO"))outside.add(key);}
        if(TaxWorkbench.hasCapitalSales(row)&&!outside.contains("capital_gains"))outside.add("capital_sales_in_workbench");
        if(TaxWorkbench.hasNotices(row)&&!outside.contains("notice_or_dispute"))outside.add("notice_in_workbench");
        boolean current=saved!=null&&fingerprint(row).equals(saved.get("reviewed_fingerprint"));
        Map<String,Object> profile=Json.map(row.get("profile"));List<Map<String,Object>> tasks=new ArrayList<>();
        add(tasks,"scope",current&&unknown.isEmpty()&&outside.isEmpty(),"Review every scope answer against the current draft.");
        for(var field:Json.map(row.get("inputs")).entrySet())add(tasks,"confirm_"+field.getKey().toLowerCase(java.util.Locale.ROOT),Json.map(field.getValue()).get("state").equals("CONFIRMED"),"Save and separately confirm "+field.getKey().replace('_',' ')+", including an intended zero.");
        add(tasks,"profile",profile.get("profile_confirmed").equals("YES")&&profile.get("eligible_confirmed").equals("YES"),"Complete the supported period, age/residency/scope and eligible-deduction profile.");
        for(var review:Json.map(row.get("reviews")).entrySet())add(tasks,"review_"+review.getKey().toLowerCase(java.util.Locale.ROOT),Boolean.TRUE.equals(review.getValue()),"Complete the "+review.getKey().replace('_',' ')+" inventory checkpoint.");
        for(var document:Json.map(row.get("documents")).entrySet())add(tasks,"document_"+document.getKey().toLowerCase(java.util.Locale.ROOT),!document.getValue().equals("UNKNOWN"),"Declare availability/applicability of "+document.getKey()+". A declaration does not verify the document.");
        boolean prepared=tasks.stream().allMatch(t->t.get("status").equals("DECLARED_COMPLETE"));
        List<String> releaseBlocks=new ArrayList<>(List.of("INDEPENDENT_TAX_REFERENCE_APPROVAL_PENDING","REVIEWED_RULE_RELEASE_PENDING","STATUTORY_FORM_ELIGIBILITY_NOT_DETERMINED","FILING_PROVIDER_NOT_CONNECTED"));
        Map<String,Object> out=new LinkedHashMap<>();out.put("status",!outside.isEmpty()?"EXPANDED_WORKFLOW_REQUIRED":prepared?"SUPPORTED_PREPARATION_DECLARED_COMPLETE":"INFORMATION_OR_REVIEW_NEEDED");out.put("period","FY2025-26-AY2026-27");out.put("questions",QUESTIONS);out.put("answers",answers);out.put("unknown_answers",unknown);out.put("outside_preparation_scope",outside);out.put("scope_review_current",current);out.put("tasks",tasks);out.put("release_blocks",releaseBlocks);out.put("scope_is_statutory_form_eligibility",false);out.put("ca_required_determined",false);out.put("tax_values_released",false);out.put("filing_available",false);return out;
    }
    private static void add(List<Map<String,Object>> tasks,String code,boolean done,String message){tasks.add(Map.of("code",code,"status",done?"DECLARED_COMPLETE":"ACTION_NEEDED","message",message));}
}
