package com.taxplatform.startup;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Authenticator lifecycle in the encrypted user record. No plaintext recovery-code receipts. */
public final class AccountSecurity {
    public record Change(Map<String,Object> response,String userId,long epoch,Map<String,Object> user){ }
    private final DataStore store;private final Workspace workspace;private final Clock clock;
    public AccountSecurity(DataStore store,Workspace workspace,Clock clock){this.store=store;this.workspace=workspace;this.clock=clock;}
    static Map<String,Object> state(Map<String,Object> user){return user.get("security")==null?new LinkedHashMap<>():Json.map(user.get("security"));}
    static long value(Map<String,Object> s,String name){return ((Number)s.getOrDefault(name,0L)).longValue();}
    static long epoch(Map<String,Object> user){return value(state(user),"epoch");}
    static boolean enabled(Map<String,Object> user){return state(user).get("totp_secret") instanceof String;}
    private static Map<String,Object> projection(Map<String,Object> user,Instant now){Map<String,Object> s=state(user),out=new LinkedHashMap<>();boolean pending=s.get("pending")!=null&&now.isBefore(Instant.parse((String)Json.map(s.get("pending")).get("expires_at")));out.put("security_revision",value(s,"revision"));out.put("mfa_enabled",enabled(user));out.put("pending_enrollment",pending);out.put("recovery_codes_remaining",s.get("recovery_hashes")==null?0:Json.list(s.get("recovery_hashes")).size());out.put("factor_locked_until",value(s,"locked_until")>now.getEpochSecond()?Instant.ofEpochSecond(value(s,"locked_until")).toString():"");out.put("password_recovery_available",false);out.put("phishing_resistant",false);return out;}
    public Map<String,Object> view(String actor){return store.read(state->projection(workspace.user(state,actor),clock.instant()));}
    private void audit(Map<String,Object> state,String actor,String action){List<Object> rows=Workspace.auditRows(state);rows.add(Map.of("id",Workspace.id(),"actor_id",actor,"action",action,"resource_id",actor,"at",clock.instant().toString()));if(rows.size()>5000)rows.remove(0);}
    private static void save(Map<String,Object> state,Map<String,Object> user,Map<String,Object> s){user.put("security",s);Workspace.table(state,"users").put((String)user.get("id"),user);}
    private boolean locked(Map<String,Object> s){return value(s,"locked_until")>clock.instant().getEpochSecond();}
    private Map<String,Object> denied(Map<String,Object> state,Map<String,Object> user,Map<String,Object> s){long now=clock.instant().getEpochSecond();if(now-value(s,"failure_window_start")>=600){s.put("failure_window_start",now);s.put("failures",0L);}long count=value(s,"failures")+1;s.put("failures",count);if(count>=6)s.put("locked_until",now+600);save(state,user,s);audit(state,(String)user.get("id"),"AUTHENTICATION_PROOF_REJECTED");return Map.of("error",count>=6?"AUTHENTICATION_RATE_LIMIT":"AUTHENTICATION_PROOF_FAILED");}
    private static void clearFailures(Map<String,Object> s){s.remove("failures");s.remove("failure_window_start");s.remove("locked_until");}
    private static String recoveryHash(String actor,String code){return Crypto.hash("TAXDESK_RECOVERY_V1\n"+actor+"\n"+code);}
    private boolean factor(Map<String,Object> user,Map<String,Object> s,String method,String code){
        if(method.equals("TOTP")){long step=Totp.verify((String)s.get("totp_secret"),code,clock.instant(),((Number)s.getOrDefault("last_totp_step",-1L)).longValue());if(step<0)return false;s.put("last_totp_step",step);return true;}
        if(method.equals("RECOVERY_CODE")){if(!code.matches("[A-Za-z0-9_-]{22}"))return false;String hash=recoveryHash((String)user.get("id"),code);List<Object> hashes=Json.list(s.getOrDefault("recovery_hashes",List.of()));int found=-1;for(int i=0;i<hashes.size();i++)if(Crypto.equal((String)hashes.get(i),hash))found=i;if(found<0)return false;hashes.remove(found);s.put("recovery_hashes",hashes);return true;}return false;
    }
    private static List<String> freshCodes(String actor,Map<String,Object> s){List<String> codes=new ArrayList<>();for(int i=0;i<10;i++)codes.add(java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(Crypto.random(16)));s.put("recovery_hashes",codes.stream().map(c->recoveryHash(actor,c)).toList());return codes;}
    private static void failure(Map<String,Object> result){if(result.get("error") instanceof String error)throw new Workspace.Problem(error.equals("AUTHENTICATION_RATE_LIMIT")?429:401,error);}
    public Workspace.LoginProof finishLogin(String actor,long expectedEpoch,Map<String,Object> body){
        Workspace.fields(body,"method","code");String method=Workspace.text(body,"method",1,20),code=Workspace.text(body,"code",1,64);if(!Set.of("TOTP","RECOVERY_CODE").contains(method))throw new Workspace.Problem(400,"INVALID_FACTOR_METHOD");
        Map<String,Object> result=store.transaction(state->{Map<String,Object> user=workspace.user(state,actor),s=state(user);if(epoch(user)!=expectedEpoch||!enabled(user))throw new Workspace.Problem(401,"MFA_CHALLENGE_EXPIRED");if(locked(s))return Map.of("error","AUTHENTICATION_RATE_LIMIT");if(!factor(user,s,method,code))return denied(state,user,s);clearFailures(s);save(state,user,s);audit(state,actor,method.equals("RECOVERY_CODE")?"RECOVERY_CODE_USED":"MFA_LOGIN_CONFIRMED");return Map.of("user",Workspace.publicUser(user),"epoch",epoch(user));});failure(result);return new Workspace.LoginProof(actor,((Number)result.get("epoch")).longValue(),false,Json.map(result.get("user")));
    }
    /** Fresh proof and the protected mutation share one storage commit/dependency set. */
    Map<String,Object> protectedMutation(String actor,Map<String,Object> body,java.util.function.Function<Map<String,Object>,Map<String,Object>> mutation){
        String password=Workspace.text(body,"password",1,128),method=Workspace.text(body,"factor_method",0,20),code=Workspace.text(body,"factor_code",0,64);
        if(!Set.of("","TOTP","RECOVERY_CODE").contains(method))throw new Workspace.Problem(400,"INVALID_FACTOR_METHOD");
        long revision=Workspace.number(body,"expected_security_revision");
        Map<String,Object> result=store.transaction(data->{
            Map<String,Object> user=workspace.user(data,actor),s=state(user);
            if(value(s,"revision")!=revision)throw new Workspace.Problem(409,"STALE_SECURITY_SETTINGS");
            if(locked(s))return Map.of("error","AUTHENTICATION_RATE_LIMIT");
            if(!Crypto.verifies(password,Json.map(user.get("credential")))||enabled(user)&&!factor(user,s,method,code))return denied(data,user,s);
            return mutation.apply(data);
        });
        failure(result);return result;
    }
    public Change command(String actor,Map<String,Object> body,boolean operatorMfaRequired){
        String operation=Workspace.text(body,"operation",1,32);Set<String> extras=switch(operation){case "CONFIRM_ENROLLMENT"->Set.of("setup_id");case "CHANGE_PASSWORD"->Set.of("new_password");case "START_ENROLLMENT","CANCEL_ENROLLMENT","REGENERATE_CODES","DISABLE_MFA","REVOKE_OTHER_SESSIONS"->Set.of();default->throw new Workspace.Problem(400,"UNKNOWN_SECURITY_OPERATION");};Set<String> fields=new java.util.HashSet<>(Set.of("operation","expected_security_revision","password","factor_method","factor_code"));fields.addAll(extras);if(!body.keySet().equals(fields))throw new Workspace.Problem(400,"UNEXPECTED_FIELDS");
        String password=Workspace.text(body,"password",1,128),method=Workspace.text(body,"factor_method",0,20),code=Workspace.text(body,"factor_code",0,64);if(!Set.of("","TOTP","RECOVERY_CODE").contains(method))throw new Workspace.Problem(400,"INVALID_FACTOR_METHOD");long revision=Workspace.number(body,"expected_security_revision");
        Map<String,Object> result=store.transaction(state->{Map<String,Object> user=workspace.user(state,actor),s=state(user);if(value(s,"revision")!=revision)throw new Workspace.Problem(409,"STALE_SECURITY_SETTINGS");if(operatorMfaRequired&&operation.equals("DISABLE_MFA"))throw new Workspace.Problem(403,"OPERATOR_MFA_REQUIRED");if(locked(s))return Map.of("error","AUTHENTICATION_RATE_LIMIT");if(!Crypto.verifies(password,Json.map(user.get("credential"))))return denied(state,user,s);
            if(enabled(user)&&!operation.equals("CONFIRM_ENROLLMENT")&&!factor(user,s,method,code))return denied(state,user,s);
            Map<String,Object> response=new LinkedHashMap<>();response.put("status","SECURITY_UPDATED");boolean revoke=false;
            switch(operation){
                case "START_ENROLLMENT"->{String secret=Totp.secret(),setup=Workspace.id();s.put("pending",Map.of("id",setup,"secret",secret,"expires_at",clock.instant().plus(Duration.ofMinutes(10)).toString()));response.put("setup_id",setup);response.put("secret",secret);response.put("otpauth_uri","otpauth://totp/TaxDesk:"+user.get("username")+"?secret="+secret+"&issuer=TaxDesk&algorithm=SHA1&digits=6&period=30");response.put("status","ENROLLMENT_PROOF_REQUIRED");}
                case "CANCEL_ENROLLMENT"->s.remove("pending");
                case "CONFIRM_ENROLLMENT"->{Map<String,Object> pending=s.get("pending")==null?null:Json.map(s.get("pending"));if(pending==null||!pending.get("id").equals(Workspace.text(body,"setup_id",1,64))||!clock.instant().isBefore(Instant.parse((String)pending.get("expires_at"))))throw new Workspace.Problem(409,"ENROLLMENT_EXPIRED_OR_REPLACED");long step=method.equals("TOTP")?Totp.verify((String)pending.get("secret"),code,clock.instant(),-1):-1;if(step<0)return denied(state,user,s);s.put("totp_secret",pending.get("secret"));s.put("last_totp_step",step);s.remove("pending");response.put("recovery_codes",freshCodes(actor,s));revoke=true;}
                case "REGENERATE_CODES"->{if(!enabled(user))throw new Workspace.Problem(409,"MFA_NOT_ENABLED");response.put("recovery_codes",freshCodes(actor,s));revoke=true;}
                case "DISABLE_MFA"->{if(!enabled(user))throw new Workspace.Problem(409,"MFA_NOT_ENABLED");for(String field:List.of("totp_secret","last_totp_step","recovery_hashes","pending"))s.remove(field);revoke=true;}
                case "CHANGE_PASSWORD"->{String next=Workspace.text(body,"new_password",12,128);Crypto.validatePassword(next);if(Crypto.equal(next,password))throw new Workspace.Problem(400,"NEW_PASSWORD_MUST_DIFFER");user.put("credential",Crypto.password(next));s.remove("pending");revoke=true;}
                case "REVOKE_OTHER_SESSIONS"->revoke=true;
                default->throw new IllegalStateException("unhandled security operation");
            }
            clearFailures(s);s.put("revision",revision+1);if(revoke)s.put("epoch",value(s,"epoch")+1);save(state,user,s);audit(state,actor,"SECURITY_"+operation);response.put("security",projection(user,clock.instant()));response.put("other_sessions_revoked",revoke);return Map.of("response",response,"epoch",epoch(user),"user",Workspace.publicUser(user));});failure(result);return new Change(Json.map(result.get("response")),actor,((Number)result.get("epoch")).longValue(),Json.map(result.get("user")));
    }
}
