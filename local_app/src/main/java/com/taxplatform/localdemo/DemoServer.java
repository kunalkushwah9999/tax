package com.taxplatform.localdemo;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.taxplatform.safety.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static com.taxplatform.safety.FactEdits.*;
import static com.taxplatform.safety.SafetyInput.*;

/** Loopback-only preparation and arithmetic-readiness workbench; no approved tax release or durable identity. */
public final class DemoServer implements AutoCloseable {
    private static final int MAX_SESSIONS=100;
    private static final Duration LIFETIME=Duration.ofMinutes(30);
    private static final String COOKIE="tax_demo_session";
    private final HttpServer server;
    private final ThreadPoolExecutor workers;
    private final Clock clock=Clock.systemUTC();
    private final SecureRandom random=new SecureRandom();
    private final byte[] bindingKey=new byte[32];
    private final ConcurrentHashMap<String,Session> sessions=new ConcurrentHashMap<>();
    private final Object creationLock=new Object();
    private final EphemeralFactLedger ledger;
    private final DemoSigner signer;
    private final PlannerEndpoint planner;
    private final String authority;
    private final String origin;

    private record Accepted(CalculationKey originalKey,String bodyFingerprint,SourceProposalPolicy.Reference source) { }
    private record LinkedSource(SourceProposalPolicy.Reference reference,String valueTag) { }
    private static final class Session {
        final String id,csrf;
        final Actor actor;
        final ComparisonWorkspace comparison;
        final CalculationKey locator;
        final Instant expires;
        final Map<String,Accepted> accepted=new HashMap<>();
        final Map<AmountField,LinkedSource> sourceLinks=new EnumMap<>(AmountField.class);
        final Set<CalculationKey> planningQuarantine=new java.util.HashSet<>();
        int planningRequests;
        Instant window;
        int requests;
        Session(String id,String csrf,Actor actor,CalculationKey locator,Instant expires,Instant now,ComparisonWorkspace comparison){
            this.id=id;this.csrf=csrf;this.actor=actor;this.comparison=comparison;this.locator=locator;this.expires=expires;window=now;
        }
    }
    private static final class Failure extends Exception {
        private static final long serialVersionUID=1L;
        final int status;
        final String code;
        Failure(int status,String code){super(code);this.status=status;this.code=code;}
    }

    public DemoServer(int port)throws IOException,GeneralSecurityException{
        if(port<0||port>65535)throw new IllegalArgumentException("invalid port");
        random.nextBytes(bindingKey);signer=new DemoSigner(clock);planner=new PlannerEndpoint();
        ledger=new EphemeralFactLedger(clock,new EphemeralFactLedger.Limits(MAX_SESSIONS,100,LIFETIME),(actor,draft)->
                new EphemeralFactLedger.PolicyControls(RuleState.UNAPPROVED,ReferenceState.PENDING,
                        Map.of(Regime.NEW,Availability.UNKNOWN,Regime.OLD,Availability.UNKNOWN)));
        server=HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),port),32);
        authority="127.0.0.1:"+server.getAddress().getPort();origin="http://"+authority;
        workers=new ThreadPoolExecutor(4,4,0L,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(64),new ThreadPoolExecutor.AbortPolicy());
        server.setExecutor(workers);server.createContext("/",this::handle);
    }
    public void start(){server.start();}
    public String url(){return origin;}
    @Override public void close(){server.stop(0);workers.shutdownNow();}

    private void handle(HttpExchange exchange)throws IOException{
        try{
            headers(exchange.getResponseHeaders());
            if(!authority.equals(single(exchange.getRequestHeaders(),"Host")))throw new Failure(421,"HOST_NOT_ALLOWED");
            String path=exchange.getRequestURI().getRawPath();
            if(exchange.getRequestURI().getRawQuery()!=null)throw new Failure(400,"QUERY_NOT_ALLOWED");
            if(!path.startsWith("/api/")){staticFile(exchange,path);return;}
            String fetchSite=single(exchange.getRequestHeaders(),"Sec-Fetch-Site");
            if(fetchSite!=null && !fetchSite.equals("same-origin") && !fetchSite.equals("none"))throw new Failure(403,"CROSS_SITE_REQUEST");
            String suppliedOrigin=single(exchange.getRequestHeaders(),"Origin");
            if(suppliedOrigin!=null && !origin.equals(suppliedOrigin))throw new Failure(403,"ORIGIN_NOT_ALLOWED");
            if(path.equals("/api/session")){
                requireMethod(exchange,"GET");Session session=session(exchange,false);if(session==null)session=open(exchange);
                synchronized(session){alive(session);reply(exchange,200,state(session));}return;
            }
            Session session=session(exchange,true);
            synchronized(session){
                alive(session);rate(session);
                switch(path){
                    case "/api/state"->{requireMethod(exchange,"GET");reply(exchange,200,state(session));}
                    case "/api/history"->{requireMethod(exchange,"GET");reply(exchange,200,ledger.history(session.actor,session.locator).orElseThrow(()->new Failure(401,"SESSION_ENDED")).toResponse());}
                    case "/api/result"->{requireMethod(exchange,"GET");Candidate draft=current(session);reply(exchange,200,new GuidedResultService(ledger,signer.verifier()).deliver(session.actor,signer.sign(draft)).toResponse());}
                    case "/api/edit"->{postProtection(exchange,session);edit(exchange,session);}
                    case "/api/plan"->{postProtection(exchange,session);plan(exchange,session);}
                    case "/api/reconciliation"->{requireMethod(exchange,"GET");reply(exchange,200,session.comparison.projection(current(session).key()));}
                    case "/api/reconciliation/import"->{postProtection(exchange,session);importComparison(exchange,session);}
                    case "/api/reconciliation/review"->{postProtection(exchange,session);reviewComparison(exchange,session);}
                    case "/api/reconciliation/propose"->{postProtection(exchange,session);proposeSource(exchange,session);}
                    case "/api/close"->{postProtection(exchange,session);emptyBody(exchange);ledger.close(session.actor,session.locator);sessions.remove(session.id,session);
                        exchange.getResponseHeaders().add("Set-Cookie",COOKIE+"=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0");reply(exchange,200,Map.of("status","SESSION_CLOSED"));}
                    default->throw new Failure(404,"NOT_FOUND");
                }
            }
        }catch(Failure failure){reply(exchange,failure.status,Map.of("status",failure.code));}
        catch(ComparisonWorkspace.Rejected conflict){reply(exchange,409,Map.of("status",conflict.code));}
        catch(IllegalArgumentException invalid){reply(exchange,400,Map.of("status","INVALID_REQUEST"));}
        catch(GeneralSecurityException|RuntimeException internal){reply(exchange,503,Map.of("status","TEMPORARILY_UNAVAILABLE"));}
        finally{exchange.close();}
    }

    private Session open(HttpExchange exchange)throws Failure,GeneralSecurityException{
        synchronized(creationLock){
            Instant now=clock.instant();
            for(Session old:sessions.values())if(!now.isBefore(old.expires)){
                synchronized(old){ledger.close(old.actor,old.locator);sessions.remove(old.id,old);}
            }
            if(sessions.size()>=MAX_SESSIONS)throw new Failure(503,"SESSION_CAPACITY_REACHED");
            String id=nonce(),csrf=nonce();Actor actor=new Actor("local-demo",UUID.randomUUID().toString());
            ValidationIdentity validation=new ValidationIdentity("DEMO-NOT-A-TAX-YEAR",hash("UNAPPROVED-DEMO-RULES"),hash("NO-TAX-ENGINE"),hash("NO-TAX-VERIFIER"),"facts-v1",hash("LOCAL-SAMPLE-ONLY"));
            CaseFacts facts=new CaseFacts(Scope.SUPPORTED,Fact.unknown(),Fact.unknown(),Fact.unknown(),Fact.unknown(),Fact.unknown(),
                    Map.of(Regime.NEW,Fact.unknown(),Regime.OLD,Fact.unknown()));
            Candidate draft=ledger.open(actor,validation,facts).orElseThrow(()->new Failure(503,"SESSION_CAPACITY_REACHED"));
            Session created=new Session(id,csrf,actor,draft.key(),now.plus(LIFETIME),now,new ComparisonWorkspace(this::nonce,clock));sessions.put(id,created);
            exchange.getResponseHeaders().add("Set-Cookie",COOKIE+"="+id+"; Path=/; HttpOnly; SameSite=Strict; Max-Age=1800");return created;
        }
    }
    private Session session(HttpExchange exchange,boolean required)throws Failure{
        List<String> cookies=exchange.getRequestHeaders().get("Cookie");String id=null;
        if(cookies!=null)for(String header:cookies){
            if(header.length()>4096)throw new Failure(400,"INVALID_COOKIE");
            for(String pair:header.split(";")){String[] bits=pair.strip().split("=",2);if(bits.length==2 && bits[0].equals(COOKIE)){
                if(id!=null)throw new Failure(400,"DUPLICATE_SESSION_COOKIE");id=bits[1];
            }}
        }
        Session session=id!=null && id.matches("[A-Za-z0-9_-]{43}")?sessions.get(id):null;
        if(required && session==null)throw new Failure(401,"SESSION_REQUIRED");return session;
    }
    private void alive(Session session)throws Failure{
        if(sessions.get(session.id)!=session || !clock.instant().isBefore(session.expires)){
            ledger.close(session.actor,session.locator);sessions.remove(session.id,session);throw new Failure(401,"SESSION_ENDED");
        }
    }
    private void rate(Session session)throws Failure{
        Instant now=clock.instant();if(!now.isBefore(session.window.plusSeconds(60))){session.window=now;session.requests=0;}
        if(++session.requests>120)throw new Failure(429,"TRY_AGAIN_LATER");
    }
    private Candidate current(Session session)throws Failure{return ledger.current(session.actor,session.locator).orElseThrow(()->new Failure(401,"SESSION_ENDED"));}
    private Map<String,Object> state(Session session)throws GeneralSecurityException,Failure{
        Candidate draft=current(session);List<Map<String,Object>> amounts=new ArrayList<>(),sourceLinks=new ArrayList<>();
        List<Fact<Money>> facts=List.of(draft.facts().salary(),draft.facts().savingsInterest(),draft.facts().qualifyingDepositInterest(),draft.facts().otherInterest());
        for(int i=0;i<facts.size();i++){
            Fact<Money> fact=facts.get(i);Map<String,Object> row=new TreeMap<>();row.put("field",AmountField.values()[i].name());row.put("state",fact.state().name());
            if(fact instanceof Fact.Confirmed<?> value)row.put("amount",((Money)value.value()).decimal());
            else if(fact instanceof Fact.Estimated<?> value)row.put("amount",((Money)value.value()).decimal());
            amounts.add(Map.copyOf(row));
            AmountField field=AmountField.values()[i];LinkedSource link=session.sourceLinks.get(field);
            Money amount=factAmount(fact);
            if(link!=null && amount!=null && constant(link.valueTag(),sourceValueTag(session,field,amount)))
                sourceLinks.add(link.reference().projection(fact.state().name()));
        }
        Map<String,Object> reviews=new TreeMap<>();reviews.put("INCOME",draft.facts().incomeInventoryComplete().confirmedValue().orElse(false));
        reviews.put("NEW_DEDUCTIONS",draft.facts().deductionInventoryComplete().get(Regime.NEW).confirmedValue().orElse(false));
        reviews.put("OLD_DEDUCTIONS",draft.facts().deductionInventoryComplete().get(Regime.OLD).confirmedValue().orElse(false));
        return Map.of("app_version","0.4.0","mode","SYNTHETIC_PREPARATION_DEMO","csrf_token",session.csrf,"view_token",viewToken(session,draft.key()),
                "expires_at",session.expires.toString(),"saved_for_later",false,"inputs",List.copyOf(amounts),"reviews",Map.copyOf(reviews),
                "source_links",List.copyOf(sourceLinks),
                "intake",new IntakeGuideService(ledger,signer.verifier()).deliver(session.actor,signer.sign(draft)).toResponse());
    }

    private void edit(HttpExchange exchange,Session session)throws Failure,IOException,GeneralSecurityException{
        String type=single(exchange.getRequestHeaders(),"Content-Type");
        if(type==null)throw new Failure(415,"FORM_CONTENT_TYPE_REQUIRED");
        String[] contentType=type.split(";",-1);
        if(!contentType[0].strip().equalsIgnoreCase("application/x-www-form-urlencoded") || contentType.length>2
                || (contentType.length==2 && !contentType[1].strip().equalsIgnoreCase("charset=UTF-8")))throw new Failure(415,"FORM_CONTENT_TYPE_REQUIRED");
        byte[] bytes=exchange.getRequestBody().readNBytes(4097);if(bytes.length>4096)throw new Failure(413,"REQUEST_TOO_LARGE");
        Map<String,String> form=StrictForm.parse(bytes);String op=required(form,"operation"),id=required(form,"request_id"),token=required(form,"view_token");
        if(!id.matches("[A-Za-z0-9._:/-]{1,128}") || !token.matches("[A-Za-z0-9_-]{43}"))throw new Failure(400,"INVALID_REQUEST");
        Command command;
        switch(op){
            case "PROPOSE"->{fields(form,Set.of("operation","request_id","view_token","field","text","unit"));
                String text=required(form,"text");if(text.length()>64)throw new Failure(400,"AMOUNT_TOO_LONG");
                String unit=required(form,"unit");command=new ProposeAmount(AmountField.valueOf(required(form,"field")),text,unit.isEmpty()?null:HumanMoneyParser.Unit.valueOf(unit));}
            case "CONFIRM"->{fields(form,Set.of("operation","request_id","view_token","field","amount"));command=new ConfirmAmount(AmountField.valueOf(required(form,"field")),Money.parse(required(form,"amount")));}
            case "CLEAR"->{fields(form,Set.of("operation","request_id","view_token","field"));command=new ClearAmount(AmountField.valueOf(required(form,"field")));}
            case "REVIEW"->{fields(form,Set.of("operation","request_id","view_token","inventory"));command=new ReviewInventory(Inventory.valueOf(required(form,"inventory")));}
            case "UNREVIEW"->{fields(form,Set.of("operation","request_id","view_token","inventory"));command=new UnreviewInventory(Inventory.valueOf(required(form,"inventory")));}
            default->throw new Failure(400,"UNKNOWN_OPERATION");
        }
        String fingerprint=hash(Json.encode(new TreeMap<>(form)));Accepted accepted=session.accepted.get(id);CalculationKey expected;
        if(accepted!=null){
            if(accepted.source()!=null || !accepted.bodyFingerprint().equals(fingerprint)){reply(exchange,409,Map.of("status","IDEMPOTENCY_CONFLICT"));return;}expected=accepted.originalKey();
        } else {
            Candidate draft=current(session);if(!constant(token,viewToken(session,draft.key()))){reply(exchange,409,Map.of("status","STALE_INPUT"));return;}expected=draft.key();
        }
        FactEditResult result=ledger.apply(session.actor,new Request(id,expected,command));
        boolean ok=Set.of(FactEditResult.Status.APPLIED,FactEditResult.Status.NO_CHANGE,FactEditResult.Status.REPLAYED).contains(result.status());
        if(ok){session.accepted.putIfAbsent(id,new Accepted(expected,fingerprint,null));
            if(result.status()!=FactEditResult.Status.REPLAYED){
                if(command instanceof ProposeAmount proposed)session.sourceLinks.remove(proposed.field());
                else if(command instanceof ClearAmount cleared)session.sourceLinks.remove(cleared.field());
            }
        }
        reply(exchange,ok?200:result.status()==FactEditResult.Status.INVALID_AMOUNT?400:409,result.toResponse());
    }
    private void proposeSource(HttpExchange exchange,Session session)throws Failure,IOException,GeneralSecurityException{
        String type=single(exchange.getRequestHeaders(),"Content-Type");
        if(type==null || !type.matches("(?i)application/x-www-form-urlencoded(?:;\\s*charset=UTF-8)?"))throw new Failure(415,"FORM_CONTENT_TYPE_REQUIRED");
        byte[] body=exchange.getRequestBody().readNBytes(4097);if(body.length>4096)throw new Failure(413,"REQUEST_TOO_LARGE");
        Map<String,String> form=com.taxplatform.planner.FormCodec.decode(body);
        fields(form,Set.of("request_id","view_token","comparison_token","group_id","observation_id","displayed_amount","field","whole_category_confirmed","replace_input_confirmed"));
        String id=form.get("request_id"),view=form.get("view_token");
        if(!id.matches("[A-Za-z0-9._:/-]{1,128}") || !view.matches("[A-Za-z0-9_-]{43}"))throw new Failure(400,"INVALID_REQUEST");
        Money amount=Money.parse(form.get("displayed_amount"));AmountField field=AmountField.valueOf(form.get("field"));
        boolean whole=yesNo(form.get("whole_category_confirmed")),replace=yesNo(form.get("replace_input_confirmed"));
        String fingerprint=hash("source-proposal.v1\n"+Json.encode(new TreeMap<>(form)));
        Accepted accepted=session.accepted.get(id);CalculationKey expected;Command command;SourceProposalPolicy.Reference reference;
        if(accepted!=null){
            if(accepted.source()==null || !accepted.bodyFingerprint().equals(fingerprint))throw new Failure(409,"IDEMPOTENCY_CONFLICT");
            expected=accepted.originalKey();reference=accepted.source();command=SourceProposalPolicy.replayCommand(reference,amount);
        }else{
            Candidate draft=current(session);if(!constant(view,viewToken(session,draft.key())))throw new Failure(409,"STALE_INPUT");
            var snapshot=session.comparison.sourceForProposal(draft.key(),form.get("comparison_token"),form.get("group_id"),form.get("observation_id"));
            var prepared=SourceProposalPolicy.prepare(snapshot,draft.key(),new SourceProposalPolicy.Intent(field,amount,whole,replace));
            expected=draft.key();reference=prepared.reference();command=prepared.command();
        }
        // Compute lineage before the edit. Only a successful new command may publish it.
        LinkedSource link=new LinkedSource(reference,sourceValueTag(session,field,amount));
        FactEditResult result=ledger.apply(session.actor,new Request(id,expected,command));
        boolean ok=Set.of(FactEditResult.Status.APPLIED,FactEditResult.Status.NO_CHANGE,FactEditResult.Status.REPLAYED).contains(result.status());
        if(ok){session.accepted.putIfAbsent(id,new Accepted(expected,fingerprint,reference));
            if(result.status()!=FactEditResult.Status.REPLAYED)session.sourceLinks.put(field,link);
        }
        reply(exchange,ok?200:result.status()==FactEditResult.Status.INVALID_AMOUNT?400:409,result.toResponse());
    }
    private static boolean yesNo(String value){return switch(value){case "YES"->true;case "NO"->false;default->throw new IllegalArgumentException("explicit YES or NO required");};}
    private static Money factAmount(Fact<Money> fact){
        if(fact instanceof Fact.Estimated<?> value)return (Money)value.value();
        if(fact instanceof Fact.Confirmed<?> value)return (Money)value.value();return null;
    }
    private String sourceValueTag(Session session,AmountField field,Money amount)throws GeneralSecurityException{
        try{
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
            for(String part:List.of("local-demo/source-value/v1",session.id,field.name(),amount.decimal())){
                byte[] utf8=part.getBytes(StandardCharsets.UTF_8);out.writeInt(utf8.length);out.write(utf8);
            }
            out.flush();Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(bindingKey,"HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(bytes.toByteArray()));
        }catch(IOException impossible){throw new IllegalStateException("in-memory source binding failed",impossible);}
    }
    private void plan(HttpExchange exchange,Session session)throws Failure,IOException,GeneralSecurityException{
        String type=single(exchange.getRequestHeaders(),"Content-Type");
        if(type==null || !type.matches("(?i)application/x-www-form-urlencoded(?:;\\s*charset=UTF-8)?"))throw new Failure(415,"FORM_CONTENT_TYPE_REQUIRED");
        byte[] bytes=exchange.getRequestBody().readNBytes(4097);if(bytes.length>4096)throw new Failure(413,"REQUEST_TOO_LARGE");
        Map<String,String> profile=com.taxplatform.planner.FormCodec.decode(bytes);
        if(!profile.keySet().equals(PlannerEndpoint.FIELDS))throw new Failure(400,"UNEXPECTED_FIELDS");
        Candidate draft=current(session);
        if(!constant(profile.get("view_token"),viewToken(session,draft.key())))throw new Failure(409,"STALE_INPUT");
        if(session.planningRequests>=100)throw new Failure(429,"PLANNING_LIMIT_REACHED");
        var input=planner.input(draft,profile);session.planningRequests++;
        var rows=planner.evaluate(input);CalculationKey key=planner.key(draft.key(),input,profile);
        if(rows.values().stream().anyMatch(a->a.status()==com.taxplatform.planner.DualPlanner.Status.VERIFIER_MISMATCH))session.planningQuarantine.add(key);
        Candidate candidate=planner.candidate(draft,key,input,rows);
        SafeResultService.ControlPlane controls=new SafeResultService.ControlPlane(){
            @Override public DeliveryContext loadFor(Actor actor,Candidate stored)throws SafeResultService.ControlStateUnavailable{
                if(!actor.equals(session.actor))throw new SafeResultService.ControlStateUnavailable();
                Candidate active;try{alive(session);active=current(session);}catch(Failure ended){throw new SafeResultService.ControlStateUnavailable();}
                if(!active.key().equals(draft.key()))throw new SafeResultService.ControlStateUnavailable();
                return new DeliveryContext(key,true,false,true,true,RuleState.UNAPPROVED,ReferenceState.PENDING,session.planningQuarantine.contains(stored.key()),
                        Map.of(Regime.NEW,Availability.UNKNOWN,Regime.OLD,Availability.UNKNOWN));
            }
            @Override public void quarantine(CalculationKey affected)throws SafeResultService.ControlStateUnavailable{
                if(!affected.equals(key))throw new SafeResultService.ControlStateUnavailable();session.planningQuarantine.add(affected);
            }
        };
        Map<String,Object> guarded=new SignedResultService(controls,signer.verifier()).deliver(session.actor,signer.sign(candidate)).toResponse();
        reply(exchange,200,planner.response(input,rows,guarded));
    }
    private void importComparison(HttpExchange exchange,Session session)throws Failure,IOException,GeneralSecurityException{
        String type=single(exchange.getRequestHeaders(),"Content-Type");
        if(type==null || !type.matches("(?i)text/csv(?:;\\s*charset=UTF-8)?"))throw new Failure(415,"CSV_CONTENT_TYPE_REQUIRED");
        byte[] body=exchange.getRequestBody().readNBytes(32769);if(body.length>32768)throw new Failure(413,"REQUEST_TOO_LARGE");
        String id=single(exchange.getRequestHeaders(),"X-Request-ID"),view=single(exchange.getRequestHeaders(),"X-Input-View");
        if(view==null)throw new Failure(400,"INPUT_VIEW_REQUIRED");
        String fingerprint=ComparisonWorkspace.binding("import",view,body);
        Map<String,Object> prior=session.comparison.replay(id,fingerprint);
        if(prior!=null){reply(exchange,200,prior);return;}
        Candidate draft=current(session);if(!constant(view,viewToken(session,draft.key())))throw new Failure(409,"STALE_INPUT");
        reply(exchange,200,session.comparison.importRows(id,fingerprint,draft.key(),body));
    }
    private void reviewComparison(HttpExchange exchange,Session session)throws Failure,IOException,GeneralSecurityException{
        String type=single(exchange.getRequestHeaders(),"Content-Type");
        if(type==null || !type.matches("(?i)application/x-www-form-urlencoded(?:;\\s*charset=UTF-8)?"))throw new Failure(415,"FORM_CONTENT_TYPE_REQUIRED");
        byte[] body=exchange.getRequestBody().readNBytes(4097);if(body.length>4096)throw new Failure(413,"REQUEST_TOO_LARGE");
        Map<String,String> form=StrictForm.parse(body);String operation=required(form,"operation");boolean reopen=operation.equals("REOPEN");
        if(!reopen && !operation.equals("SELECT"))throw new Failure(400,"UNKNOWN_OPERATION");
        fields(form,reopen?Set.of("operation","request_id","view_token","comparison_token","group_id"):
                Set.of("operation","request_id","view_token","comparison_token","group_id","observation_id","reason"));
        String fingerprint=ComparisonWorkspace.binding("review",form.get("view_token"),body);
        Map<String,Object> prior=session.comparison.replay(form.get("request_id"),fingerprint);
        if(prior!=null){reply(exchange,200,prior);return;}
        Candidate draft=current(session);if(!constant(form.get("view_token"),viewToken(session,draft.key())))throw new Failure(409,"STALE_INPUT");
        ComparisonWorkspace.Reason reason=reopen?null:ComparisonWorkspace.Reason.valueOf(form.get("reason"));
        reply(exchange,200,session.comparison.select(form.get("request_id"),fingerprint,draft.key(),form.get("comparison_token"),form.get("group_id"),form.get("observation_id"),reason,reopen));
    }
    private void postProtection(HttpExchange exchange,Session session)throws Failure{
        requireMethod(exchange,"POST");if(!origin.equals(single(exchange.getRequestHeaders(),"Origin")))throw new Failure(403,"ORIGIN_REQUIRED");
        if(!constant(session.csrf,single(exchange.getRequestHeaders(),"X-CSRF-Token")))throw new Failure(403,"CSRF_REJECTED");
    }
    private void emptyBody(HttpExchange exchange)throws IOException,Failure{if(exchange.getRequestBody().read()!=-1)throw new Failure(400,"EMPTY_BODY_REQUIRED");}
    private static void fields(Map<String,String> form,Set<String> allowed)throws Failure{if(!form.keySet().equals(allowed))throw new Failure(400,"UNEXPECTED_FIELDS");}
    private static String required(Map<String,String> form,String name)throws Failure{if(!form.containsKey(name))throw new Failure(400,"MISSING_FIELD");return form.get(name);}
    private static String single(Headers headers,String name)throws Failure{List<String> values=headers.get(name);if(values==null)return null;if(values.size()!=1)throw new Failure(400,"AMBIGUOUS_HEADER");return values.get(0);}
    private static void requireMethod(HttpExchange exchange,String method)throws Failure{if(!exchange.getRequestMethod().equals(method))throw new Failure(405,"METHOD_NOT_ALLOWED");}
    private static boolean constant(String a,String b){return b!=null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
    private String nonce(){byte[] bytes=new byte[32];random.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    private static String hash(String text)throws GeneralSecurityException{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}
    private String viewToken(Session session,CalculationKey key)throws GeneralSecurityException{
        try{
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
            for(String part:List.of("local-demo/edit-view/v1",session.id,key.tenantId(),key.profileId(),key.caseId(),key.snapshotId(),key.periodKey(),
                    key.ruleBundleHash(),key.engineDigest(),key.verifierDigest(),key.contractVersion(),key.settingsHash())){
                byte[] utf8=part.getBytes(StandardCharsets.UTF_8);out.writeInt(utf8.length);out.write(utf8);
            }
            out.flush();Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(bindingKey,"HmacSHA256"));return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(bytes.toByteArray()));
        }catch(IOException impossible){throw new IllegalStateException("in-memory view binding failed",impossible);}
    }
    private void staticFile(HttpExchange exchange,String path)throws Failure,IOException{
        requireMethod(exchange,"GET");String asset=switch(path){case "/"->"index.html";case "/app.js"->"app.js";case "/styles.css"->"styles.css";default->throw new Failure(404,"NOT_FOUND");};
        try(var input=DemoServer.class.getResourceAsStream("/web/"+asset)){
            if(input==null)throw new Failure(503,"ASSET_UNAVAILABLE");byte[] bytes=input.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type",asset.endsWith(".js")?"text/javascript; charset=utf-8":asset.endsWith(".css")?"text/css; charset=utf-8":"text/html; charset=utf-8");
            exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);
        }
    }
    private static void headers(Headers headers){
        headers.set("Cache-Control","no-store");headers.set("X-Content-Type-Options","nosniff");headers.set("X-Frame-Options","DENY");headers.set("Referrer-Policy","no-referrer");
        headers.set("Content-Security-Policy","default-src 'none'; script-src 'self'; style-src 'self'; connect-src 'self'; img-src 'self'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'");
        headers.set("Permissions-Policy","camera=(), microphone=(), geolocation=(), payment=()");headers.set("Cross-Origin-Resource-Policy","same-origin");
    }
    private static void reply(HttpExchange exchange,int status,Map<String,?> response)throws IOException{
        byte[] bytes=Json.encode(response).getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");exchange.sendResponseHeaders(status,bytes.length);exchange.getResponseBody().write(bytes);
    }
    public static void main(String[] args)throws Exception{
        int port=8787;if(args.length==2 && args[0].equals("--port"))port=Integer.parseInt(args[1]);else if(args.length!=0)throw new IllegalArgumentException("usage: --port NUMBER");
        DemoServer app=new DemoServer(port);Runtime.getRuntime().addShutdownHook(new Thread(app::close));app.start();
        System.out.println("LOCAL_DEMO_URL="+app.url());System.out.println("Synthetic preparation only. Session memory is temporary; tax calculation is unavailable.");System.out.flush();
    }
}
