package com.taxplatform.startup;

import java.time.Clock;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Customer active-store erasure; external copies and restore promotion remain reviewed work. */
public final class AccountPrivacy {
    private final DataStore store;
    private final Workspace workspace;
    private final AccountSecurity security;
    private final Clock clock;
    public AccountPrivacy(DataStore store,Workspace workspace,AccountSecurity security,Clock clock){
        this.store=store;this.workspace=workspace;this.security=security;this.clock=clock;
    }
    private static String token(Map<String,Object> body){
        String value=Workspace.text(body,"receipt_token",43,43);
        if(!value.matches("[A-Za-z0-9_-]{43}"))throw new Workspace.Problem(400,"INVALID_RECEIPT_TOKEN");
        // Canonical encoding of 32 random bytes, not an account identifier or a password.
        try{byte[] decoded=java.util.Base64.getUrlDecoder().decode(value);
            if(decoded.length!=32||!java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(decoded).equals(value))throw new IllegalArgumentException();
        }catch(IllegalArgumentException e){throw new Workspace.Problem(400,"INVALID_RECEIPT_TOKEN");}
        return value;
    }
    private static String key(String token){return "privacy:"+Crypto.hash("TAXDESK_ERASURE_RECEIPT_V1\n"+token);}
    static String actorHash(String actor){return Crypto.hash("TAXDESK_ERASED_ACTOR_V1\n"+actor);}
    private static Set<String> ownIds(Map<String,Object> data,String table,String actor){
        Set<String> ids=new HashSet<>();Workspace.table(data,table).forEach((id,raw)->{if(actor.equals(Json.map(raw).get("owner_id")))ids.add(id);});return ids;
    }
    private static boolean hasOrders(Map<String,Object> data,String actor){
        return Workspace.table(data,"operations").values().stream().map(Json::map)
            .anyMatch(row->actor.equals(row.get("owner_id"))&&"ORDER".equals(row.get("kind")));
    }
    public Map<String,Object> view(String actor){return store.read(data->{
        Map<String,Object> user=workspace.user(data,actor);boolean customer="CUSTOMER".equals(user.get("role")),orders=hasOrders(data,actor);
        Map<String,Object> out=new LinkedHashMap<>();out.put("automatic_active_erasure_available",customer&&!orders);
        out.put("reason",!customer?"OPERATOR_ACCOUNT_REVIEW_REQUIRED":orders?"PAYMENT_RECORD_REVIEW_REQUIRED":"FRESH_AUTHENTICATION_REQUIRED");
        out.put("owned_cases",ownIds(data,"cases",actor).size());out.put("owned_tickets",ownIds(data,"tickets",actor).size());
        out.put("retained_control","Pseudonymous account hash, receipt hash, counts and erasure time for restore/privacy review.");
        out.put("external_and_backup_erasure_confirmed",false);out.put("production_privacy_approved",false);return out;
    });}
    private static Map<String,Object> publicReceipt(Map<String,Object> row){
        return Map.of("status",row.get("status"),"completed_at",row.get("completed_at"),"removed_counts",row.get("removed_counts"),
            "active_credentials_removed",true,"external_and_backup_erasure_confirmed",false,
            "follow_up","EXTERNAL_COPIES_AND_BACKUP_REVIEW_REQUIRED","production_privacy_approved",false);
    }
    public Map<String,Object> status(Map<String,Object> body){
        Workspace.fields(body,"receipt_token");String id=key(token(body));
        return store.read(data->{Object raw=Workspace.table(data,"operations").get(id);
            if(raw==null||!"ACCOUNT_ERASURE_CONTROL".equals(Json.map(raw).get("kind")))throw new Workspace.Problem(404,"ERASURE_RECEIPT_NOT_FOUND");
            return publicReceipt(Json.map(raw));});
    }
    public Map<String,Object> erase(String actor,Map<String,Object> body){
        Workspace.fields(body,"expected_security_revision","password","factor_method","factor_code","confirm","receipt_token");
        if(!"DELETE MY ACCOUNT".equals(Workspace.text(body,"confirm",1,32)))throw new Workspace.Problem(400,"ACCOUNT_ERASURE_CONFIRMATION_REQUIRED");
        String controlId=key(token(body));
        return security.protectedMutation(actor,body,data->{
            Map<String,Object> user=workspace.user(data,actor);
            if(!"CUSTOMER".equals(user.get("role")))throw new Workspace.Problem(409,"OPERATOR_ACCOUNT_REVIEW_REQUIRED");
            // No deletion while provider work/financial retention has an unresolved lifecycle.
            if(hasOrders(data,actor))throw new Workspace.Problem(409,"PAYMENT_RECORD_REVIEW_REQUIRED");
            if(Workspace.table(data,"operations").containsKey(controlId))throw new Workspace.Problem(409,"RECEIPT_TOKEN_ALREADY_USED");
            Set<String> resources=new HashSet<>();Map<String,Object> counts=new LinkedHashMap<>();
            for(String name:List.of("cases","tickets","quotes")){
                Set<String> ids=ownIds(data,name,actor);resources.addAll(ids);counts.put(name,ids.size());
                for(String id:ids)Workspace.table(data,name).remove(id);
            }
            for(String name:List.of("grants","operations")){
                Map<String,Object> table=Workspace.table(data,name);Set<String> ids=new HashSet<>();
                table.forEach((id,raw)->{Map<String,Object> row=Json.map(raw);
                    if(actor.equals(row.get("owner_id"))||actor.equals(row.get("preparer_id"))||resources.contains(row.get("case_id")))ids.add(id);});
                resources.addAll(ids);for(String id:ids)table.remove(id);counts.put(name,ids.size());
            }
            // Remove cross-actor idempotency references as well as the owner's receipts.
            Map<String,Object> operations=Workspace.table(data,"operations");Set<String> indirect=new HashSet<>();
            operations.forEach((id,raw)->{if(resources.contains(Json.map(raw).get("resource")))indirect.add(id);});
            for(String id:indirect)operations.remove(id);resources.addAll(indirect);
            counts.put("operations",((Number)counts.get("operations")).intValue()+indirect.size());
            Map<String,Object> receipts=Workspace.table(data,"receipts");int before=receipts.size();
            receipts.entrySet().removeIf(e->e.getKey().startsWith(actor+":")||resources.contains(Json.map(e.getValue()).get("resource")));
            counts.put("receipts",before-receipts.size());List<Object> audit=Workspace.auditRows(data);before=audit.size();
            audit.removeIf(raw->{Map<String,Object> row=Json.map(raw);return actor.equals(row.get("actor_id"))||resources.contains(row.get("resource_id"));});
            counts.put("audit_events",before-audit.size());Workspace.table(data,"users").remove(actor);counts.put("accounts",1);
            Capacity.requireSpace(operations,6000,"OPERATIONS_LIMIT");Map<String,Object> control=new LinkedHashMap<>();
            control.put("id",controlId);control.put("kind","ACCOUNT_ERASURE_CONTROL");control.put("erased_actor_sha256",actorHash(actor));
            control.put("status","ACTIVE_DATA_REMOVED");control.put("completed_at",clock.instant().toString());control.put("removed_counts",counts);
            control.put("restore_promotion_allowed",false);control.put("external_erasure_confirmed",false);operations.put(controlId,control);
            return publicReceipt(control);
        });
    }
}
