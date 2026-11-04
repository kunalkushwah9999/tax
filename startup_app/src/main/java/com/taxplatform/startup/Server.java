package com.taxplatform.startup;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.taxplatform.localdemo.StartupAdapter;
import com.taxplatform.safety.SafetyInput.Candidate;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Loopback startup development application. No live payment, filing or professional authority. */
public final class Server implements AutoCloseable {
    private final HttpServer http;private final ThreadPoolExecutor pool;private final DataStore vault;private final Workspace workspace;private final StartupAdapter calculation;
    private final AccountSecurity security;private final AccountPrivacy privacy;private final StartupOperations operations;private final PaymentGateway paymentGateway;private final NativeImports nativeImports;private final String origin,authority;private final boolean secureCookie;private final ConcurrentHashMap<String,Session> sessions=new ConcurrentHashMap<>();private final Clock clock=Clock.systemUTC();
    private final Map<String,Window> loginLimits=new LinkedHashMap<>();private final Map<String,Window> generalLimits=new LinkedHashMap<>();
    private static final String COOKIE="tax_startup_session";
    private static final class Session {final String token=Crypto.nonce(),csrf=Crypto.nonce();final Instant expires;final String userId;final long epoch;String pendingUser;Instant challengeExpires;Session(String userId,long epoch,Instant now){this.userId=userId;this.epoch=epoch;expires=now.plus(Duration.ofMinutes(30));}}
    private static final class Window {Instant until;int attempts;Window(Instant now,Duration length){until=now.plus(length);}}
    public Server(int port,Path directory,Path keyFile)throws IOException,GeneralSecurityException{this(port,new VaultStore(directory,keyFile));}
    public Server(int port,DataStore store)throws IOException,GeneralSecurityException{this(port,store,null);}
    public Server(int port,DataStore store,String publicOrigin)throws IOException,GeneralSecurityException{this(port,store,publicOrigin,NativeImports.disabled());}
    public Server(int port,DataStore store,String publicOrigin,NativeImports nativeImports)throws IOException,GeneralSecurityException{
        this(port,store,publicOrigin,nativeImports,PaymentGateway.disabled(),ProfessionalRegistry.disabled());
    }
    public Server(int port,DataStore store,String publicOrigin,NativeImports nativeImports,PaymentGateway payments,ProfessionalRegistry professionals)throws IOException,GeneralSecurityException{
        paymentGateway=payments;this.nativeImports=nativeImports;vault=store;workspace=new Workspace(vault,clock,professionals);security=new AccountSecurity(vault,workspace,clock);privacy=new AccountPrivacy(vault,workspace,security,clock);operations=new StartupOperations(vault,workspace,payments,professionals,clock);calculation=new StartupAdapter();
        http=HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),port),32);if(publicOrigin==null){authority="127.0.0.1:"+http.getAddress().getPort();origin="http://"+authority;secureCookie=false;}else{PublicEndpoint endpoint=PublicEndpoint.https(publicOrigin);authority=endpoint.authority();origin=endpoint.origin();secureCookie=true;}
        pool=new ThreadPoolExecutor(4,4,0L,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(64),new ThreadPoolExecutor.AbortPolicy());http.setExecutor(pool);http.createContext("/",this::handle);
    }
    public void start(){http.start();}
    public String url(){return origin;}
    @Override public void close()throws IOException{http.stop(0);pool.shutdownNow();nativeImports.close();vault.close();}
    private synchronized void limit(Map<String,Window> map,String identity,int max,Duration length){Instant now=clock.instant();map.entrySet().removeIf(e->!now.isBefore(e.getValue().until));if(!map.containsKey(identity)&&map.size()>=2048)throw new Workspace.Problem(429,"REQUEST_LIMIT");Window w=map.computeIfAbsent(identity,k->new Window(now,length));if(++w.attempts>max)throw new Workspace.Problem(429,"REQUEST_LIMIT");}
    private void handle(HttpExchange exchange)throws IOException{
        try{
            headers(exchange.getResponseHeaders());if(!authority.equals(header(exchange,"Host")))throw new Workspace.Problem(421,"HOST_NOT_ALLOWED");
            if(exchange.getRequestURI().getRawQuery()!=null)throw new Workspace.Problem(400,"QUERY_NOT_ALLOWED");String path=exchange.getRequestURI().getRawPath();
            if(!path.startsWith("/api/")){asset(exchange,path);return;}
            String supplied=header(exchange,"Origin"),fetch=header(exchange,"Sec-Fetch-Site");if(supplied!=null&&!origin.equals(supplied))throw new Workspace.Problem(403,"ORIGIN_REJECTED");if(fetch!=null&&!List.of("same-origin","none").contains(fetch))throw new Workspace.Problem(403,"CROSS_SITE_REQUEST");
            if(path.equals("/api/payments/webhook")){method(exchange,"POST");byte[] raw=exchange.getRequestBody().readNBytes(65537);if(raw.length>65536)throw new Workspace.Problem(413,"REQUEST_TOO_LARGE");reply(exchange,200,operations.webhook(header(exchange,"X-Razorpay-Event-Id"),header(exchange,"X-Razorpay-Signature"),raw));return;}
            Session session=session(exchange);String remote=exchange.getRemoteAddress().getAddress().getHostAddress();limit(generalLimits,session==null?"anonymous:"+remote:Crypto.hash(session.token),180,Duration.ofMinutes(1));
            if(path.equals("/api/bootstrap")){method(exchange,"GET");if(session==null)session=issue(exchange,null);Map<String,Object> response=new LinkedHashMap<>();response.put("csrf_token",session.csrf);response.put("user",session.userId==null?null:workspace.account(session.userId));response.put("expires_at",session.expires.toString());response.put("readiness",workspace.readiness());response.put("mfa_required",session.pendingUser!=null&&clock.instant().isBefore(session.challengeExpires));response.put("operator_mfa_enrollment_required",session.userId!=null&&secureCookie&&!workspace.account(session.userId).get("role").equals("CUSTOMER")&&!Boolean.TRUE.equals(workspace.account(session.userId).get("mfa_enabled")));reply(exchange,200,response);return;}
            if(path.equals("/api/health")){method(exchange,"GET");reply(exchange,200,Map.of("status","OK","version","5.1.0","production_ready",false));return;}
            if(path.equals("/api/prices")){method(exchange,"GET");reply(exchange,200,Map.of("prices",workspace.prices(),"checkout_available",false));return;}
            boolean write=!exchange.getRequestMethod().equals("GET");if(write)protection(exchange,session);
            if(path.equals("/api/account-deletion/status")){method(exchange,"POST");limit(loginLimits,"privacy-receipts:"+remote,30,Duration.ofMinutes(15));reply(exchange,200,privacy.status(body(exchange,4096)));return;}
            if(path.equals("/api/register")||path.equals("/api/login")){
                method(exchange,"POST");Map<String,Object> body=body(exchange);limit(loginLimits,"network:"+remote,30,Duration.ofMinutes(15));String login=body.get("username") instanceof String name?name.toLowerCase(java.util.Locale.ROOT):"invalid";limit(loginLimits,"account:"+Crypto.hash(login),10,Duration.ofMinutes(15));
                Workspace.LoginProof proof;if(path.endsWith("register")){Map<String,Object> user=workspace.register(body);proof=new Workspace.LoginProof((String)user.get("id"),0L,false,user);}else proof=workspace.passwordLogin(body);
                sessions.remove(Crypto.hash(session.token));if(proof.mfaRequired()){Session challenge=issue(exchange,null,proof.epoch());challenge.pendingUser=proof.userId();challenge.challengeExpires=clock.instant().plus(Duration.ofMinutes(5));Map<String,Object> response=new LinkedHashMap<>();response.put("user",null);response.put("csrf_token",challenge.csrf);response.put("mfa_required",true);response.put("expires_at",challenge.challengeExpires.toString());reply(exchange,200,response);}else{Session signed=issue(exchange,proof.userId(),proof.epoch());reply(exchange,200,loginResponse(signed,proof.user()));}return;
            }
            if(path.equals("/api/login-cancel")){method(exchange,"POST");empty(exchange);sessions.remove(Crypto.hash(session.token));Session fresh=issue(exchange,null);Map<String,Object> response=new LinkedHashMap<>();response.put("user",null);response.put("csrf_token",fresh.csrf);response.put("mfa_required",false);reply(exchange,200,response);return;}
            if(path.equals("/api/login-factor")){method(exchange,"POST");if(session==null||session.pendingUser==null)throw new Workspace.Problem(401,"MFA_CHALLENGE_REQUIRED");synchronized(session){if(sessions.get(Crypto.hash(session.token))!=session||!clock.instant().isBefore(session.challengeExpires))throw new Workspace.Problem(401,"MFA_CHALLENGE_EXPIRED");Workspace.LoginProof proof;try{proof=security.finishLogin(session.pendingUser,session.epoch,body(exchange,4096));}catch(Workspace.Problem problem){if(problem.code.equals("STORAGE_OUTCOME_UNKNOWN"))throw new Workspace.Problem(503,"SECURITY_OUTCOME_UNKNOWN");throw problem;}sessions.remove(Crypto.hash(session.token));Session signed=issue(exchange,proof.userId(),proof.epoch());reply(exchange,200,loginResponse(signed,proof.user()));return;}}
            if(session==null||session.userId==null)throw new Workspace.Problem(401,"LOGIN_REQUIRED");synchronized(session){if(sessions.get(Crypto.hash(session.token))!=session||!clock.instant().isBefore(session.expires))throw new Workspace.Problem(401,"SESSION_ENDED");String actor=session.userId;workspace.bindSession(actor,session.epoch);Map<String,Object> account=workspace.account(actor);boolean operatorMfaRequired=secureCookie&&!account.get("role").equals("CUSTOMER");if(operatorMfaRequired&&!Boolean.TRUE.equals(account.get("mfa_enabled"))&&!List.of("/api/security","/api/security/update","/api/logout").contains(path))throw new Workspace.Problem(403,"OPERATOR_MFA_REQUIRED");
            switch(path){
                case "/api/logout"->{method(exchange,"POST");empty(exchange);sessions.remove(Crypto.hash(session.token));exchange.getResponseHeaders().add("Set-Cookie",cookieName()+"=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0"+(secureCookie?"; Secure":""));reply(exchange,200,Map.of("status","SIGNED_OUT"));}
                case "/api/security"->{method(exchange,"GET");Map<String,Object> view=new LinkedHashMap<>(security.view(actor));view.put("operator_mfa_required",operatorMfaRequired);reply(exchange,200,view);}
                case "/api/account-deletion"->{method(exchange,"GET");reply(exchange,200,privacy.view(actor));}
                case "/api/account-deletion/erase"->{
                    method(exchange,"POST");Map<String,Object> result;
                    try{result=privacy.erase(actor,body(exchange,4096));}
                    catch(Workspace.Problem problem){if(problem.code.equals("STORAGE_OUTCOME_UNKNOWN"))throw new Workspace.Problem(503,"ERASURE_OUTCOME_UNKNOWN_CHECK_RECEIPT");throw problem;}
                    sessions.entrySet().removeIf(e->actor.equals(e.getValue().userId)||actor.equals(e.getValue().pendingUser));nativeImports.discardActor(actor);
                    exchange.getResponseHeaders().add("Set-Cookie",cookieName()+"=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0"+(secureCookie?"; Secure":""));reply(exchange,200,result);
                }
                case "/api/security/update"->{method(exchange,"POST");AccountSecurity.Change change;try{change=security.command(actor,body(exchange,4096),operatorMfaRequired);}catch(Workspace.Problem problem){if(problem.code.equals("STORAGE_OUTCOME_UNKNOWN"))throw new Workspace.Problem(503,"SECURITY_OUTCOME_UNKNOWN");throw problem;}sessions.remove(Crypto.hash(session.token));Session fresh=issue(exchange,actor,change.epoch());Map<String,Object> response=new LinkedHashMap<>(change.response());response.putAll(loginResponse(fresh,change.user()));reply(exchange,200,response);}
                case "/api/operations-capabilities"->{method(exchange,"GET");reply(exchange,200,operations.capabilities());}
                case "/api/catalog"->{method(exchange,"GET");reply(exchange,200,Map.of("plans",operations.catalog()));}
                case "/api/billing"->{method(exchange,"GET");reply(exchange,200,operations.billing(actor));}
                case "/api/orders"->{method(exchange,"POST");reply(exchange,200,operations.createOrder(actor,body(exchange)));}
                case "/api/firm-queue"->{method(exchange,"GET");reply(exchange,200,operations.firmQueue(actor));}
                case "/api/experts"->{method(exchange,"GET");reply(exchange,200,Map.of("experts",operations.directory(actor),"real_ca_verified",false));}
                case "/api/expert-requests"->{if(exchange.getRequestMethod().equals("GET"))reply(exchange,200,Map.of("requests",operations.expertRequests(actor)));else{method(exchange,"POST");reply(exchange,200,operations.requestExpert(actor,body(exchange)));}}
                case "/api/feedback"->{if(exchange.getRequestMethod().equals("GET"))reply(exchange,200,Map.of("feedback",operations.feedbackList(actor)));else{method(exchange,"POST");reply(exchange,200,operations.feedback(actor,body(exchange)));}}
                case "/api/admin/catalog"->{method(exchange,"POST");reply(exchange,200,operations.publishPlan(actor,body(exchange)));}
                case "/api/admin/operations"->{method(exchange,"GET");reply(exchange,200,operations.metrics(actor));}
                case "/api/cases"->{if(exchange.getRequestMethod().equals("GET"))reply(exchange,200,Map.of("cases",workspace.cases(actor)));else{method(exchange,"POST");reply(exchange,200,workspace.createCase(actor,body(exchange)));}}
                case "/api/native-sample"->{method(exchange,"GET");try(var in=Server.class.getResourceAsStream("/native/synthetic_itr1.json")){if(in==null)throw new Workspace.Problem(503,"SAMPLE_UNAVAILABLE");reply(exchange,200,Map.of("json_text",new String(in.readAllBytes(),StandardCharsets.UTF_8),"synthetic_only",true,"fileable",false));}}
                case "/api/native-capabilities"->{method(exchange,"GET");reply(exchange,200,nativeImports.capabilities());}
                case "/api/preparers"->{method(exchange,"GET");reply(exchange,200,Map.of("preparers",workspace.preparers(actor)));}
                case "/api/quotes"->{if(exchange.getRequestMethod().equals("GET"))reply(exchange,200,Map.of("quotes",workspace.quotes(actor)));else{method(exchange,"POST");reply(exchange,200,workspace.quote(actor,body(exchange)));}}
                case "/api/tickets"->{if(exchange.getRequestMethod().equals("GET"))reply(exchange,200,Map.of("tickets",workspace.tickets(actor)));else{method(exchange,"POST");reply(exchange,200,workspace.openTicket(actor,body(exchange)));}}
                case "/api/audit"->{method(exchange,"GET");reply(exchange,200,Map.of("events",workspace.audit(actor)));}
                case "/api/admin"->{method(exchange,"GET");reply(exchange,200,workspace.admin(actor));}
                case "/api/admin/prices"->{method(exchange,"POST");reply(exchange,200,workspace.publishPrice(actor,body(exchange)));}
                default->{String[] parts=path.split("/");if(parts.length>=4&&parts[2].equals("cases")){String caseId=parts[3];if(!caseId.matches("[a-f0-9-]{36}"))throw new Workspace.Problem(404,"CASE_NOT_FOUND");String action=parts.length==4?"read":parts.length==5?parts[4]:"invalid";caseAction(exchange,actor,caseId,action);}
                    else if(parts.length==5&&parts[2].equals("orders")){switch(parts[4]){case "checkout"->{method(exchange,"GET");reply(exchange,200,operations.checkout(actor,parts[3]));}case "reconcile"->{method(exchange,"POST");reply(exchange,200,operations.reconcileOrder(actor,parts[3],body(exchange)));}case "callback"->{method(exchange,"POST");reply(exchange,200,operations.callback(actor,parts[3],body(exchange)));}default->throw new Workspace.Problem(404,"NOT_FOUND");}}
                    else if(parts.length==5&&parts[2].equals("expert-requests")&&parts[4].equals("update")){method(exchange,"POST");reply(exchange,200,operations.updateExpert(actor,parts[3],body(exchange)));}
                    else if(parts.length==6&&parts[2].equals("admin")&&parts[3].equals("feedback")&&parts[5].equals("triage")){method(exchange,"POST");reply(exchange,200,operations.triageFeedback(actor,parts[4],body(exchange)));}
                    else if(parts.length==5&&parts[2].equals("tickets")&&parts[4].equals("update")){method(exchange,"POST");reply(exchange,200,workspace.updateTicket(actor,parts[3],body(exchange)));}
                    else throw new Workspace.Problem(404,"NOT_FOUND");}
            }
            }
        }catch(Workspace.Problem problem){reply(exchange,problem.status,java.util.Set.of("STORAGE_OUTCOME_UNKNOWN","PAYMENT_ORDER_OUTCOME_UNKNOWN").contains(problem.code)?Map.of("status",problem.code,"retry_same_request",true):Map.of("status",problem.code));}
        catch(IllegalArgumentException problem){reply(exchange,400,Map.of("status","INVALID_REQUEST"));}
        catch(GeneralSecurityException|RuntimeException problem){reply(exchange,503,Map.of("status","TEMPORARILY_UNAVAILABLE"));}
        finally{workspace.clearSession();exchange.close();}
    }
    private void caseAction(HttpExchange exchange,String actor,String caseId,String action)throws IOException,GeneralSecurityException{
        switch(action){
            case "preparation-bundle"->{method(exchange,"POST");reply(exchange,200,operations.prepareBundle(actor,caseId,body(exchange)));}
            case "read"->{method(exchange,"GET");reply(exchange,200,workspace.readCase(actor,caseId));}
            case "edit"->{method(exchange,"POST");reply(exchange,200,workspace.edit(actor,caseId,body(exchange)));}
            case "itr-native-discard"->{method(exchange,"POST");Map<String,Object> input=body(exchange);Workspace.fields(input,"expected_revision","preview_id");workspace.nativeRevision(actor,caseId,Workspace.number(input,"expected_revision"));nativeImports.discard(actor,caseId,Workspace.text(input,"preview_id",43,43));reply(exchange,200,Map.of("status","PREVIEW_DISCARDED"));}
            case "itr-native-preview"->{method(exchange,"POST");Map<String,Object> input=body(exchange,400000);Workspace.fields(input,"expected_revision","synthetic_confirmed","json_text");if(!Workspace.flag(input,"synthetic_confirmed"))throw new Workspace.Problem(400,"SYNTHETIC_DOCUMENT_CONFIRMATION_REQUIRED");long version=workspace.nativeRevision(actor,caseId,Workspace.number(input,"expected_revision"));Object document=input.get("json_text");if(!(document instanceof String raw)||raw.isEmpty()||raw.length()>196608)throw new Workspace.Problem(400,"INVALID_NATIVE_DOCUMENT");Map<String,Object> preview=nativeImports.preview(actor,caseId,version,raw);workspace.nativeRevision(actor,caseId,version);reply(exchange,200,preview);}
            case "itr-native-propose"->{method(exchange,"POST");Map<String,Object> input=body(exchange);Map<String,Object> receipt=workspace.proposeNative(actor,caseId,input,()->nativeImports.find(actor,caseId,Workspace.text(input,"preview_id",43,43)));nativeImports.discard(actor,caseId,Workspace.text(input,"preview_id",43,43));reply(exchange,200,receipt);}
            case "itr-readiness"->{method(exchange,"GET");reply(exchange,200,workspace.itrReadiness(actor,caseId));}
            case "itr-scope"->{method(exchange,"POST");reply(exchange,200,workspace.saveItrScope(actor,caseId,body(exchange)));}
            case "gst"->{if(exchange.getRequestMethod().equals("GET"))reply(exchange,200,workspace.gst(actor,caseId));else{method(exchange,"POST");reply(exchange,200,workspace.gstCommand(actor,caseId,body(exchange)));}}
            case "gst-export"->{method(exchange,"GET");exchange.getResponseHeaders().set("Content-Disposition","attachment; filename=gst-preparation-review.json");reply(exchange,200,workspace.gstExport(actor,caseId));}
            case "workbench"->{if(exchange.getRequestMethod().equals("GET"))reply(exchange,200,workspace.workbench(actor,caseId));else{method(exchange,"POST");reply(exchange,200,workspace.workbenchCommand(actor,caseId,body(exchange,65000)));}}
            case "workbench-export"->{method(exchange,"GET");exchange.getResponseHeaders().set("Content-Disposition","attachment; filename=tax-preparation-workbench.json");reply(exchange,200,workspace.workbenchExport(actor,caseId));}
            case "sources"->{method(exchange,"GET");reply(exchange,200,workspace.comparison(actor,caseId));}
            case "export"->{method(exchange,"GET");exchange.getResponseHeaders().set("Content-Disposition","attachment; filename=preparation-data.json");reply(exchange,200,workspace.exportCase(actor,caseId));}
            case "delete"->{method(exchange,"POST");reply(exchange,200,workspace.deleteCase(actor,caseId,body(exchange)));}
            case "grants"->{if(exchange.getRequestMethod().equals("GET"))reply(exchange,200,Map.of("grants",workspace.grants(actor,caseId)));else{method(exchange,"POST");reply(exchange,200,workspace.grant(actor,caseId,body(exchange)));}}
            case "revoke"->{method(exchange,"POST");reply(exchange,200,workspace.revoke(actor,caseId,body(exchange)));}
            case "note"->{method(exchange,"POST");reply(exchange,200,workspace.note(actor,caseId,body(exchange)));}
            case "assess"->{method(exchange,"POST");empty(exchange);Candidate source=workspace.calculationSource(actor,caseId);Map<String,Object> row=workspace.readCase(actor,caseId);Map<String,String> profile=new LinkedHashMap<>();Json.map(row.get("profile")).forEach((k,v)->profile.put(k,(String)v));
                Map<String,Object> result=calculation.assess(source,profile,new StartupAdapter.Current(){
                    @Override public boolean stillCurrent(){return workspace.current(actor,caseId,source.key());}
                    @Override public void quarantine(){workspace.quarantine(actor,caseId);}
                    @Override public boolean quarantined(){return workspace.quarantined(actor,caseId);}
                });if(!workspace.current(actor,caseId,source.key()))throw new Workspace.Problem(409,"STALE_CASE");reply(exchange,200,result);}
            default->throw new Workspace.Problem(404,"NOT_FOUND");
        }
    }
    private Session session(HttpExchange exchange){String cookie=header(exchange,"Cookie");if(cookie==null)return null;if(cookie.length()>4096)throw new Workspace.Problem(400,"INVALID_COOKIE");String token=null;for(String part:cookie.split(";")){String[] pair=part.strip().split("=",2);if(pair.length==2&&pair[0].equals(cookieName())){if(token!=null)throw new Workspace.Problem(400,"DUPLICATE_COOKIE");token=pair[1];}}if(token==null||!token.matches("[A-Za-z0-9_-]{43}"))return null;Session s=sessions.get(Crypto.hash(token));if(s!=null&&!clock.instant().isBefore(s.expires)){sessions.remove(Crypto.hash(token));return null;}if(s!=null&&s.userId!=null&&workspace.authenticationEpoch(s.userId)!=s.epoch){sessions.remove(Crypto.hash(token));return null;}return s;}
    private Session issue(HttpExchange exchange,String userId){return issue(exchange,userId,0L);}
    private synchronized Session issue(HttpExchange exchange,String userId,long epoch){Instant now=clock.instant();sessions.entrySet().removeIf(e->!now.isBefore(e.getValue().expires));if(sessions.size()>=1000)throw new Workspace.Problem(503,"SESSION_CAPACITY");Session session=new Session(userId,epoch,now);sessions.put(Crypto.hash(session.token),session);exchange.getResponseHeaders().add("Set-Cookie",cookieName()+"="+session.token+"; Path=/; HttpOnly; SameSite=Strict; Max-Age=1800"+(secureCookie?"; Secure":""));return session;}
    private Map<String,Object> loginResponse(Session session,Map<String,Object> user){return Map.of("user",user,"csrf_token",session.csrf,"expires_at",session.expires.toString(),"mfa_required",false,"operator_mfa_enrollment_required",secureCookie&&!user.get("role").equals("CUSTOMER")&&!Boolean.TRUE.equals(user.get("mfa_enabled")));}
    private String cookieName(){return secureCookie?"__Host-"+COOKIE:COOKIE;}
    private void protection(HttpExchange exchange,Session session){method(exchange,"POST");if(session==null)throw new Workspace.Problem(401,"SESSION_REQUIRED");if(!origin.equals(header(exchange,"Origin")))throw new Workspace.Problem(403,"ORIGIN_REQUIRED");if(!Crypto.equal(session.csrf,header(exchange,"X-CSRF-Token")))throw new Workspace.Problem(403,"CSRF_REJECTED");}
    private static String header(HttpExchange exchange,String name){List<String> values=exchange.getRequestHeaders().get(name);if(values==null)return null;if(values.size()!=1)throw new Workspace.Problem(400,"AMBIGUOUS_HEADER");return values.get(0);}
    private static void method(HttpExchange exchange,String required){if(!exchange.getRequestMethod().equals(required))throw new Workspace.Problem(405,"METHOD_NOT_ALLOWED");}
    private static void empty(HttpExchange exchange)throws IOException{if(exchange.getRequestBody().read()!=-1)throw new Workspace.Problem(400,"EMPTY_BODY_REQUIRED");}
    private static Map<String,Object> body(HttpExchange exchange)throws IOException{return body(exchange,65536);}
    private static Map<String,Object> body(HttpExchange exchange,int maximum)throws IOException{String type=header(exchange,"Content-Type");if(type==null||!type.matches("(?i)application/json(?:;\\s*charset=UTF-8)?"))throw new Workspace.Problem(415,"JSON_REQUIRED");byte[] bytes=exchange.getRequestBody().readNBytes(maximum+1);if(bytes.length>maximum)throw new Workspace.Problem(413,"REQUEST_TOO_LARGE");return Json.object(bytes);}
    private void headers(Headers h){if(secureCookie)h.set("Strict-Transport-Security","max-age=86400");h.set("Cache-Control","no-store");h.set("X-Content-Type-Options","nosniff");h.set("X-Frame-Options","DENY");h.set("Referrer-Policy","no-referrer");h.set("Permissions-Policy","camera=(), microphone=(), geolocation=()");h.set("Content-Security-Policy","default-src 'none'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'; object-src 'none'");if(paymentGateway.browserCheckout())h.set("Content-Security-Policy","default-src 'none'; script-src 'self' https://checkout.razorpay.com; style-src 'self'; img-src 'self' data:; connect-src 'self' https://api.razorpay.com; frame-src https://api.razorpay.com https://checkout.razorpay.com; base-uri 'none'; form-action 'self'; frame-ancestors 'none'; object-src 'none'");}
    private static void reply(HttpExchange exchange,int code,Object result)throws IOException{byte[] bytes=Json.bytes(result);exchange.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");exchange.sendResponseHeaders(code,bytes.length);exchange.getResponseBody().write(bytes);}
    private static void asset(HttpExchange exchange,String path)throws IOException{method(exchange,"GET");String name=switch(path){case "/"->"index.html";case "/app.js"->"app.js";case "/workbench.js"->"workbench.js";case "/security.js"->"security.js";case "/privacy.js"->"privacy.js";case "/account-deletion"->"deletion.html";case "/deletion-status.js"->"deletion-status.js";case "/styles.css"->"styles.css";default->throw new Workspace.Problem(404,"NOT_FOUND");};try(var stream=Server.class.getResourceAsStream("/web/"+name)){if(stream==null)throw new Workspace.Problem(503,"ASSET_UNAVAILABLE");byte[] bytes=stream.readAllBytes();exchange.getResponseHeaders().set("Content-Type",name.endsWith("html")?"text/html; charset=utf-8":name.endsWith("js")?"text/javascript; charset=utf-8":"text/css; charset=utf-8");exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);}}
    public static void main(String[] args)throws Exception{
        int port=8788;Path data=Path.of("runtime-data","records"),key=Path.of("runtime-data","keys","vault.key");String operation="serve",storage="local",publicOrigin=null;Path sqlConfig=null,backup=null,backupKey=null,nativePython=null,nativeWorker=null,nativeSchema=null,paymentConfig=null,professionalRegistry=null;
        for(int i=0;i<args.length;i++){switch(args[i]){case "--port"->port=Integer.parseInt(args[++i]);case "--data"->data=Path.of(args[++i]);case "--key"->key=Path.of(args[++i]);case "--operator"->operation="operator";case "--storage"->storage=args[++i];case "--mysql-config"->sqlConfig=Path.of(args[++i]);case "--init-mysql"->operation="init-mysql";case "--public-origin"->publicOrigin=args[++i];case "--backup-to"->{operation="backup";backup=Path.of(args[++i]);}case "--verify-backup"->{operation="verify-backup";backup=Path.of(args[++i]);}case "--restore-backup"->{operation="restore-backup";backup=Path.of(args[++i]);}case "--backup-key"->backupKey=Path.of(args[++i]);case "--payment-config"->paymentConfig=Path.of(args[++i]);case "--professional-registry"->professionalRegistry=Path.of(args[++i]);case "--native-python"->nativePython=Path.of(args[++i]);case "--native-worker"->nativeWorker=Path.of(args[++i]);case "--native-schema-dir"->nativeSchema=Path.of(args[++i]);default->throw new IllegalArgumentException("unsupported option");}}
        if(!java.util.Set.of("local","mysql","mysql-concurrent").contains(storage))throw new IllegalArgumentException("storage profile");
        if(operation.equals("init-mysql")){if(storage.equals("local")||sqlConfig==null)throw new IllegalArgumentException("explicit MySQL configuration required");if(storage.equals("mysql-concurrent"))ConcurrentMysqlStore.initialize(SqlConfig.load(sqlConfig),key);else MysqlStore.initialize(SqlConfig.load(sqlConfig),key);System.out.println("MYSQL_SCHEMA_INITIALIZED");return;}
        if(operation.equals("verify-backup")||operation.equals("restore-backup")){
            if(backupKey==null||backup==null)throw new IllegalArgumentException("explicit backup and separate backup key required");if(FramedBackup.framed(backup)){if(operation.equals("verify-backup")){System.out.println(Json.encode(FramedBackup.verify(backup,backupKey)));return;}if(!storage.equals("mysql-concurrent")||sqlConfig==null)throw new IllegalArgumentException("framed restore requires a fresh concurrent MySQL target");System.out.println(Json.encode(ConcurrentMysqlStore.restoreFramed(SqlConfig.load(sqlConfig),key,backup,backupKey)));return;}Map<String,Object> payload=RecoveryArchive.load(backup,backupKey);
            if(operation.equals("verify-backup")){System.out.println(Json.encode(RecoveryArchive.summary(payload)));return;}
            if(!storage.equals("mysql")||sqlConfig==null||key.toAbsolutePath().normalize().equals(backupKey.toAbsolutePath().normalize()))throw new IllegalArgumentException("restore requires MySQL and a separate new record key");
            System.out.println(Json.encode(MysqlStore.restoreIsolated(SqlConfig.load(sqlConfig),key,payload)));return;
        }
        DataStore selected=storage.equals("mysql-concurrent")?new ConcurrentMysqlStore(SqlConfig.load(java.util.Objects.requireNonNull(sqlConfig)),key):storage.equals("mysql")?new MysqlStore(SqlConfig.load(java.util.Objects.requireNonNull(sqlConfig)),key):new VaultStore(data,key);
        if(operation.equals("backup")){try(DataStore store=selected){if(backupKey==null||backup==null||key.toAbsolutePath().normalize().equals(backupKey.toAbsolutePath().normalize()))throw new IllegalArgumentException("separate backup key required");System.out.println(Json.encode(storage.equals("mysql-concurrent")?FramedBackup.write(store,backup,backupKey):RecoveryArchive.write(store,backup,backupKey)));}return;}
        if(operation.equals("operator")){try(DataStore store=selected){Map<String,Object> input=Json.object(System.in.readNBytes(4097));Workspace.fields(input,"username","display_name","password","role");new Workspace(store,Clock.systemUTC()).createOperator(Workspace.text(input,"username",3,32),Workspace.text(input,"display_name",1,64),Workspace.text(input,"password",12,128),Workspace.text(input,"role",1,16));System.out.println("OPERATOR_CREATED");}return;}
        if(port<0||port>65535)throw new IllegalArgumentException("port");NativeImports imports=nativePython==null?NativeImports.disabled():NativeImports.configured(nativePython,java.util.Objects.requireNonNull(nativeWorker),java.util.Objects.requireNonNull(nativeSchema));Server server=new Server(port,selected,publicOrigin,imports,paymentConfig==null?PaymentGateway.disabled():PaymentGateway.load(paymentConfig),professionalRegistry==null?ProfessionalRegistry.disabled():ProfessionalRegistry.load(professionalRegistry,Clock.systemUTC()));Runtime.getRuntime().addShutdownHook(new Thread(()->{try{server.close();}catch(IOException e){/* Process is stopping; no input or key is logged. */}}));server.start();System.out.println("STARTUP_URL="+server.url());System.out.println("Development release. Synthetic data only. Tax release, live payments and filing are unavailable.");
    }
}
