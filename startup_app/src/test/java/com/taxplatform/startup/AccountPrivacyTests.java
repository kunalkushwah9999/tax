package com.taxplatform.startup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class AccountPrivacyTests {
    private static final String PASSWORD="Synthetic-privacy-fixture-2026";
    private static int passed;
    @FunctionalInterface interface Test {void run()throws Exception;}
    static void check(String name,Test test)throws Exception{test.run();passed++;System.out.println("PASS "+name);}
    static void eq(Object a,Object b){if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
    static void reject(String code,Runnable work){try{work.run();throw new AssertionError("accepted "+code);}catch(Workspace.Problem e){eq(e.code,code);}}
    static Map<String,Object> map(Object... pairs){Map<String,Object> out=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)out.put((String)pairs[i],pairs[i+1]);return out;}
    static final class Fixture implements AutoCloseable {
        final Path path=Files.createTempDirectory("taxdesk-privacy-unit-");
        final VaultStore store=new VaultStore(path.resolve("records"),path.resolve("key"));
        final Workspace workspace=new Workspace(store,Clock.systemUTC());
        final AccountSecurity security=new AccountSecurity(store,workspace,Clock.systemUTC());
        final AccountPrivacy privacy=new AccountPrivacy(store,workspace,security,Clock.systemUTC());
        final String actor=(String)workspace.register(map("username","privacy_owner","display_name","Private alias canary","password",PASSWORD)).get("id");
        Fixture()throws Exception{ }
        Map<String,Object> body(){return map("expected_security_revision",security.view(actor).get("security_revision"),"password",PASSWORD,"factor_method","","factor_code","","confirm","DELETE MY ACCOUNT","receipt_token",Crypto.nonce());}
        String create(String owner){return (String)workspace.createCase(owner,map("title","Synthetic private draft","request_id",Workspace.id())).get("id");}
        @Override public void close()throws java.io.IOException{store.close();}
    }
    static DataStore fault(DataStore delegate,boolean afterCommit){return new DataStore(){
        public <T>T read(Function<Map<String,Object>,T> f){return delegate.read(f);}
        public <T>T transaction(Function<Map<String,Object>,T> f){
            if(afterCommit){delegate.transaction(f);throw new Workspace.Problem(503,"STORAGE_OUTCOME_UNKNOWN");}
            return delegate.transaction(data->{f.apply(data);throw new Workspace.Problem(503,"STORAGE_WRITE_FAILED");});
        }
        public String backend(){return delegate.backend();}
        public void close(){ }
    };}
    public static void main(String[] args)throws Exception{
        check("eligible inventory does not expose proof or receipt secrets",()->{try(Fixture f=new Fixture()){f.create(f.actor);var view=f.privacy.view(f.actor);eq(view.get("owned_cases"),1);eq(view.get("automatic_active_erasure_available"),true);eq(view.get("external_and_backup_erasure_confirmed"),false);}});
        check("exact confirmation and request field set required",()->{try(Fixture f=new Fixture()){var body=f.body();body.put("confirm","DELETE");reject("ACCOUNT_ERASURE_CONFIRMATION_REQUIRED",()->f.privacy.erase(f.actor,body));var extra=f.body();extra.put("other_actor",Workspace.id());reject("UNEXPECTED_FIELDS",()->f.privacy.erase(f.actor,extra));eq(f.workspace.account(f.actor).get("id"),f.actor);}});
        check("status token requires canonical 32-byte base64url",()->{try(Fixture f=new Fixture()){for(String value:List.of("short","/".repeat(43),"A".repeat(42)+"B"))reject(value.length()==43?"INVALID_RECEIPT_TOKEN":"INVALID_RECEIPT_TOKEN",()->f.privacy.status(map("receipt_token",value)));}});
        check("wrong password preserves all customer data",()->{try(Fixture f=new Fixture()){String caseId=f.create(f.actor);var body=f.body();body.put("password","wrong-password");reject("AUTHENTICATION_PROOF_FAILED",()->f.privacy.erase(f.actor,body));eq(f.workspace.readCase(f.actor,caseId).get("id"),caseId);}});
        check("fresh proof throttling persists across requests",()->{try(Fixture f=new Fixture()){var body=f.body();body.put("password","wrong-password");for(int i=0;i<5;i++)reject("AUTHENTICATION_PROOF_FAILED",()->f.privacy.erase(f.actor,body));reject("AUTHENTICATION_RATE_LIMIT",()->f.privacy.erase(f.actor,body));reject("AUTHENTICATION_RATE_LIMIT",()->f.privacy.erase(f.actor,f.body()));}});
        check("stale security revision cannot erase account",()->{try(Fixture f=new Fixture()){var body=f.body();body.put("expected_security_revision",99L);reject("STALE_SECURITY_SETTINGS",()->f.privacy.erase(f.actor,body));}});
        check("MFA-enabled account needs unused fresh factor",()->{try(Fixture f=new Fixture()){
            var setup=f.security.command(f.actor,map("operation","START_ENROLLMENT","expected_security_revision",0L,"password",PASSWORD,"factor_method","","factor_code",""),false);
            var enabled=f.security.command(f.actor,map("operation","CONFIRM_ENROLLMENT","expected_security_revision",1L,"password",PASSWORD,"factor_method","TOTP","factor_code",Totp.at((String)setup.response().get("secret"),Clock.systemUTC().instant().getEpochSecond()/30,6),"setup_id",setup.response().get("setup_id")),false);
            var body=f.body();reject("AUTHENTICATION_PROOF_FAILED",()->f.privacy.erase(f.actor,body));body.put("factor_method","RECOVERY_CODE");body.put("factor_code",Json.list(enabled.response().get("recovery_codes")).get(0));eq(f.privacy.erase(f.actor,body).get("status"),"ACTIVE_DATA_REMOVED");
        }});
        check("cascade removes grants tickets quotes feedback and cross-actor receipts",()->{try(Fixture f=new Fixture()){
            String caseId=f.create(f.actor);f.workspace.openTicket(f.actor,map("request_id","support-fixture","topic","DATA_REQUEST","subject","Synthetic request","message","Synthetic message"));
            f.store.transaction(data->{Workspace.table(data,"grants").put("grant-fixture",map("id","grant-fixture","case_id",caseId,"preparer_id","other-reviewer"));Workspace.table(data,"quotes").put("quote-fixture",map("id","quote-fixture","owner_id",f.actor));Workspace.table(data,"operations").put("feedback-fixture",map("id","feedback-fixture","kind","FEEDBACK","owner_id",f.actor,"message","Private canary"));Workspace.table(data,"operations").put("request-fixture",map("id","request-fixture","kind","REQUEST_RECEIPT","owner_id","other-reviewer","resource","feedback-fixture"));Workspace.table(data,"receipts").put("other-reviewer:receipt",map("resource",caseId));return null;});
            f.privacy.erase(f.actor,f.body());f.store.read(data->{for(String name:List.of("users","cases","tickets","quotes","grants","receipts"))eq(Workspace.table(data,name).size(),0);eq(Workspace.table(data,"operations").size(),1);return null;});
        }});
        check("unrelated customer data and receipts survive",()->{try(Fixture f=new Fixture()){
            String other=(String)f.workspace.register(map("username","other_customer","display_name","Other sample","password",PASSWORD)).get("id");String caseId=f.create(other);byte[] before=Json.bytes(f.workspace.readCase(other,caseId));f.create(f.actor);f.privacy.erase(f.actor,f.body());eq(java.util.Arrays.equals(before,Json.bytes(f.workspace.readCase(other,caseId))),true);eq(f.workspace.cases(other).size(),1);
        }});
        check("deleted credentials sessions and private resources cannot revive",()->{try(Fixture f=new Fixture()){String caseId=f.create(f.actor);f.privacy.erase(f.actor,f.body());eq(f.workspace.authenticationEpoch(f.actor),-1L);reject("LOGIN_FAILED",()->f.workspace.login(map("username","privacy_owner","password",PASSWORD)));reject("SESSION_ENDED",()->f.workspace.readCase(f.actor,caseId));}});
        check("receipt is durable minimal and token is never stored plaintext",()->{try(Fixture f=new Fixture()){var body=f.body();String token=(String)body.get("receipt_token");f.create(f.actor);f.privacy.erase(f.actor,body);String state=f.store.read(Json::encode);eq(state.contains(token),false);eq(state.contains(f.actor),false);eq(state.contains("Private alias canary"),false);eq(f.privacy.status(map("receipt_token",token)).get("status"),"ACTIVE_DATA_REMOVED");reject("ERASURE_RECEIPT_NOT_FOUND",()->f.privacy.status(map("receipt_token",Crypto.nonce())));}});
        check("receipt token reuse cannot delete a second account",()->{try(Fixture f=new Fixture()){var body=f.body();f.privacy.erase(f.actor,body);String other=(String)f.workspace.register(map("username","second_owner","display_name","Other sample","password",PASSWORD)).get("id");body.put("expected_security_revision",0L);reject("RECEIPT_TOKEN_ALREADY_USED",()->f.privacy.erase(other,body));eq(f.workspace.account(other).get("id"),other);}});
        check("payment lifecycle and operator accounts require reviewed removal",()->{try(Fixture f=new Fixture()){f.store.transaction(data->{Workspace.table(data,"operations").put("order-fixture",map("id","order-fixture","kind","ORDER","owner_id",f.actor));return null;});reject("PAYMENT_RECORD_REVIEW_REQUIRED",()->f.privacy.erase(f.actor,f.body()));String operator=(String)f.workspace.createOperator("privacy_operator","Operator",PASSWORD,"PREPARER").get("id");reject("OPERATOR_ACCOUNT_REVIEW_REQUIRED",()->f.privacy.erase(operator,f.body()));}});
        check("pre-commit failure rolls back the complete cascade",()->{try(Fixture f=new Fixture()){String caseId=f.create(f.actor);DataStore failing=fault(f.store,false);var security=new AccountSecurity(failing,f.workspace,Clock.systemUTC());var privacy=new AccountPrivacy(failing,f.workspace,security,Clock.systemUTC());reject("STORAGE_WRITE_FAILED",()->privacy.erase(f.actor,f.body()));eq(f.workspace.readCase(f.actor,caseId).get("id"),caseId);}});
        check("lost commit acknowledgement recovers through existing secret receipt",()->{try(Fixture f=new Fixture()){var body=f.body();DataStore failing=fault(f.store,true);var security=new AccountSecurity(failing,f.workspace,Clock.systemUTC());var privacy=new AccountPrivacy(failing,f.workspace,security,Clock.systemUTC());reject("STORAGE_OUTCOME_UNKNOWN",()->privacy.erase(f.actor,body));eq(f.privacy.status(map("receipt_token",body.get("receipt_token"))).get("status"),"ACTIVE_DATA_REMOVED");eq(f.workspace.authenticationEpoch(f.actor),-1L);}});
        System.out.println(Json.encode(Map.of("privacy_unit_groups",passed,"failures",0,"production_privacy_approved",false)));
    }
}
