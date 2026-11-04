package com.taxplatform.startup;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/** Optional bounded worker and ephemeral owner/revision-bound previews. No raw native file retention. */
public final class NativeImports implements AutoCloseable {
    public static final String SCHEMA_SHA="6a0ef9f64065431879dcc724869d8455a640bcf11279d1818eb19f11448ce763";
    public static final String SCHEMA_ID="ITR1-AY2026-27-20260630-V1.1";
    private static final String INCOME="/ITR/ITR1/ITR1_IncomeDeductions";
    public record Proposal(String field,String amount,String sourcePath){ }
    public record Preview(String actor,String caseId,long revision,Instant expires,Map<String,Proposal> proposals){public Preview{proposals=Map.copyOf(proposals);}}
    @FunctionalInterface interface Worker {Map<String,Object> run(byte[] input);}
    private Worker worker;private final Clock clock;private final boolean enabled;private final Map<String,Preview> previews=new LinkedHashMap<>();private final Semaphore slots=new Semaphore(2);private final Set<Process> children=ConcurrentHashMap.newKeySet();private volatile boolean closed;
    private NativeImports(Worker worker,Clock clock,boolean enabled){this.worker=worker;this.clock=clock;this.enabled=enabled;}
    public static NativeImports disabled(){return new NativeImports(input->{throw new Workspace.Problem(503,"NATIVE_IMPORT_NOT_CONFIGURED");},Clock.systemUTC(),false);}
    static NativeImports testCache(Clock clock){return new NativeImports(input->{throw new UnsupportedOperationException();},clock,true);}
    public static NativeImports configured(Path python,Path script,Path schemaDirectory)throws IOException{
        NativeImports result=new NativeImports(null,Clock.systemUTC(),true);
        List<String> command=List.of(python.toAbsolutePath().normalize().toString(),"-B",script.toAbsolutePath().normalize().toString(),"--schema-dir",schemaDirectory.toAbsolutePath().normalize().toString());
        Map<String,Object> check=result.process(command,new byte[0],true);if(!"NATIVE_WORKER_CONFIGURED".equals(check.get("status"))||!SCHEMA_SHA.equals(check.get("schema_sha256")))throw new IOException("native worker configuration unavailable");
        result.worker=input->result.process(command,input,false);return result;
    }
    private Map<String,Object> process(List<String> command,byte[] input,boolean selfCheck){
        if(closed||!slots.tryAcquire())throw new Workspace.Problem(503,"NATIVE_IMPORT_BUSY");Process process=null;var streams=Executors.newFixedThreadPool(2);
        try{
            List<String> args=new ArrayList<>(command);if(selfCheck)args.add("--self-check");process=new ProcessBuilder(args).redirectError(ProcessBuilder.Redirect.DISCARD).start();children.add(process);if(closed)throw new Workspace.Problem(503,"NATIVE_IMPORT_NOT_CONFIGURED");Process child=process;
            var writing=streams.submit(()->{try(var out=child.getOutputStream()){out.write(input);}return true;});var reading=streams.submit(()->{try(var in=child.getInputStream()){byte[] bytes=in.readNBytes(65537);if(bytes.length>65536){child.destroyForcibly();throw new IOException("native worker output bound");}return bytes;}});
            if(!process.waitFor(5,TimeUnit.SECONDS)){process.destroyForcibly();throw new Workspace.Problem(503,"NATIVE_WORKER_TIMEOUT");}writing.get(1,TimeUnit.SECONDS);byte[] bytes=reading.get(1,TimeUnit.SECONDS);if(process.exitValue()!=0)throw new Workspace.Problem(503,"NATIVE_WORKER_UNAVAILABLE");return Json.object(bytes);
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new Workspace.Problem(503,"NATIVE_WORKER_INTERRUPTED");}
        catch(Workspace.Problem e){throw e;}
        catch(Exception e){throw new Workspace.Problem(503,"NATIVE_WORKER_UNAVAILABLE");}
        finally{if(process!=null){process.destroyForcibly();children.remove(process);}streams.shutdownNow();slots.release();}
    }
    public Map<String,Object> capabilities(){return Map.of("native_itr_configured",enabled&&!closed,"schema_id",SCHEMA_ID,"schema_sha256",SCHEMA_SHA,"business_rules_validated",false,"native_gst_available",false,"filing_available",false,"raw_document_retained",false);}
    public Map<String,Object> preview(String actor,String caseId,long revision,String raw){
        if(!enabled||closed)throw new Workspace.Problem(503,"NATIVE_IMPORT_NOT_CONFIGURED");byte[] bytes=raw.getBytes(java.nio.charset.StandardCharsets.UTF_8);if(bytes.length>196608)throw new Workspace.Problem(413,"NATIVE_DOCUMENT_TOO_LARGE");
        Map<String,Object> result;try{result=projection(worker.run(bytes));}catch(Workspace.Problem problem){if(problem.status==400)throw new Workspace.Problem(503,"NATIVE_WORKER_CONTRACT_REJECTED");throw problem;}catch(IllegalArgumentException problem){throw new Workspace.Problem(503,"NATIVE_WORKER_CONTRACT_REJECTED");}finally{java.util.Arrays.fill(bytes,(byte)0);}
        List<Object> offered=Json.list(result.get("proposals"));Map<String,Proposal> fields=new LinkedHashMap<>();for(Object item:offered){Map<String,Object> p=Json.map(item);String field=(String)p.get("field");if(fields.put(field,new Proposal(field,(String)p.get("amount"),(String)p.get("source_path")))!=null)throw new Workspace.Problem(503,"NATIVE_WORKER_CONTRACT_REJECTED");}
        if(!fields.isEmpty()){String token=save(actor,caseId,revision,fields);result.put("preview_id",token);result.put("expires_in_seconds",600L);}else result.put("preview_id",null);return result;
    }
    synchronized String save(String actor,String caseId,long revision,Map<String,Proposal> fields){
        if(closed)throw new Workspace.Problem(503,"NATIVE_IMPORT_NOT_CONFIGURED");previews.entrySet().removeIf(e->!clock.instant().isBefore(e.getValue().expires()));previews.entrySet().removeIf(e->e.getValue().actor().equals(actor)&&e.getValue().caseId().equals(caseId));if(previews.values().stream().filter(p->p.actor().equals(actor)).count()>=4)throw new Workspace.Problem(503,"NATIVE_PREVIEW_ACCOUNT_LIMIT");if(previews.size()>=64)throw new Workspace.Problem(503,"NATIVE_PREVIEW_CAPACITY");String token=Crypto.nonce();previews.put(Crypto.hash(token),new Preview(actor,caseId,revision,clock.instant().plus(Duration.ofMinutes(10)),fields));return token;
    }
    public synchronized Preview find(String actor,String caseId,String token){
        if(closed||!token.matches("[A-Za-z0-9_-]{43}"))throw new Workspace.Problem(409,"NATIVE_PREVIEW_EXPIRED_REIMPORT");Preview preview=previews.get(Crypto.hash(token));if(preview==null||!clock.instant().isBefore(preview.expires())||!preview.actor().equals(actor)||!preview.caseId().equals(caseId))throw new Workspace.Problem(409,"NATIVE_PREVIEW_EXPIRED_REIMPORT");return preview;
    }
    public synchronized void discard(String actor,String caseId,String token){Preview p=previews.get(Crypto.hash(token));if(p!=null&&p.actor().equals(actor)&&p.caseId().equals(caseId))previews.remove(Crypto.hash(token));}
    public synchronized void discardActor(String actor){previews.entrySet().removeIf(e->e.getValue().actor().equals(actor));}
    private static final Set<String> CODES=Set.of("SCHEMA_TYPE","SCHEMA_REQUIRED","SCHEMA_ENUM","SCHEMA_PATTERN","SCHEMA_MINIMUM","SCHEMA_MAXIMUM","SCHEMA_MINLENGTH","SCHEMA_MAXLENGTH","SCHEMA_MINITEMS","SCHEMA_MAXITEMS","SCHEMA_ADDITIONALPROPERTIES","SCHEMA_ALLOF","DUPLICATE_JSON_KEY","INVALID_NATIVE_JSON","NONFINITE_JSON_NUMBER","JSON_NUMBER_BOUND","NATIVE_DOCUMENT_TOO_LARGE","NATIVE_STRUCTURE_BOUND","NATIVE_STRING_BOUND","NATIVE_ROOT_OBJECT_REQUIRED","EXACT_FORM_YEAR_VERSION_REQUIRED","REMOTE_SCHEMA_REFERENCE_DISABLED","NATIVE_SCHEMA_PIN_MISMATCH");
    private static final Set<String> WARNINGS=Set.of("PROPERTY_INCOME_OUTSIDE_PROPOSAL_PACK","CAPITAL_GAINS_OUTSIDE_PROPOSAL_PACK","EXEMPT_INCOME_REVIEW_OUTSIDE_PROPOSAL_PACK","OTHER_INCOME_CATEGORY_OUTSIDE_PROPOSAL_PACK","SECTION57_DEDUCTION_OUTSIDE_PROPOSAL_PACK","AMOUNT_ABOVE_WORKBENCH_BOUND","INTEREST_BREAKUP_TOTAL_REVIEW_REQUIRED","MULTIPLE_SAV_OBSERVATIONS_NOT_AGGREGATED","MULTIPLE_IFD_OBSERVATIONS_NOT_AGGREGATED","MULTIPLE_TAX_OBSERVATIONS_NOT_AGGREGATED","SOURCE_AND_ANNUAL_CATEGORY_COVERAGE_REQUIRE_REVIEW","NO_TAX_CREDIT_DEDUCTION_OR_PROFILE_TRANSFER","BUSINESS_VALIDATION_RULES_NOT_EXECUTED");
    private static String path(String raw){
        Set<String> names;try(var in=NativeImports.class.getResourceAsStream("/native/public_fields.json")){if(in==null)throw new IOException("missing public fields");names=new java.util.HashSet<>(Json.list(Json.object(in.readAllBytes()).get("fields")).stream().map(String.class::cast).toList());}catch(IOException e){throw new Workspace.Problem(503,"NATIVE_WORKER_UNAVAILABLE");}
        List<String> safe=new ArrayList<>();for(String part:raw.split("/")){if(!part.isEmpty())safe.add(names.contains(part)||part.matches("[0-9]{1,3}")?part:"field");}if(safe.size()>24)return "/field";return "/"+String.join("/",safe);
    }
    static Map<String,Object> projection(Map<String,Object> raw){
        if(!"native-itr-preflight.v1".equals(raw.get("contract_version"))||!SCHEMA_ID.equals(raw.get("schema_id"))||!SCHEMA_SHA.equals(raw.get("schema_sha256")))throw new Workspace.Problem(503,"NATIVE_WORKER_CONTRACT_REJECTED");
        String schema=Workspace.text(raw,"schema_validation",1,20),identity=Workspace.text(raw,"document_format",1,20),pack=Workspace.text(raw,"proposal_pack",1,40);if(!Set.of("PASS","FAIL","NOT_RUN").contains(schema)||!Set.of("MATCH","UNSUPPORTED","UNKNOWN").contains(identity)||!Set.of("AVAILABLE","UNAVAILABLE","EXPANDED_WORKFLOW_REQUIRED").contains(pack))throw new Workspace.Problem(503,"NATIVE_WORKER_CONTRACT_REJECTED");
        List<Object> errors=Json.list(raw.get("issues")),warnings=Json.list(raw.get("warnings")),proposals=Json.list(raw.get("proposals"));if(errors.size()>50||warnings.size()>25||proposals.size()>4)throw new Workspace.Problem(503,"NATIVE_WORKER_CONTRACT_REJECTED");List<Object> safeErrors=new ArrayList<>(),safeProposals=new ArrayList<>();List<String> safeWarnings=new ArrayList<>();
        for(Object value:errors){Map<String,Object> error=Json.map(value);String code=Workspace.text(error,"code",1,100);safeErrors.add(Map.of("code",CODES.contains(code)?code:"NATIVE_VALIDATION_ISSUE","path",path(Workspace.text(error,"path",1,1024))));}
        for(Object value:warnings)if(value instanceof String text)safeWarnings.add(WARNINGS.contains(text)?text:"NATIVE_REVIEW_REQUIRED");
        if(!proposals.isEmpty()&&!(schema.equals("PASS")&&identity.equals("MATCH")&&pack.equals("AVAILABLE")&&errors.isEmpty()))throw new Workspace.Problem(503,"NATIVE_WORKER_CONTRACT_REJECTED");
        for(Object value:proposals){Map<String,Object> p=Json.map(value);Workspace.fields(p,"field","amount","source_path");String field=Workspace.text(p,"field",1,32),amount=Workspace.text(p,"amount",1,16),source=Workspace.text(p,"source_path",1,256);boolean salary=field.equals("SALARY")&&source.equals(INCOME+"/GrossSalary"),interest=Set.of("SAVINGS_INTEREST","DEPOSIT_INTEREST","OTHER_INTEREST").contains(field)&&source.matches(INCOME+"/OthersInc/OthersIncDtlsOthSrc/[0-9]{1,3}/OthSrcOthAmount");if(!(salary||interest)||!amount.matches("(?:0|[1-9][0-9]{0,8})\\.00"))throw new Workspace.Problem(503,"NATIVE_WORKER_CONTRACT_REJECTED");safeProposals.add(Map.of("field",field,"amount",amount,"source_path",source));}
        Map<String,Object> out=new LinkedHashMap<>();out.put("schema_id",SCHEMA_ID);out.put("schema_sha256",SCHEMA_SHA);out.put("schema_validation",schema);out.put("document_format",identity);out.put("proposal_pack",pack);out.put("issues",safeErrors);out.put("warnings",safeWarnings);out.put("proposals",safeProposals);out.put("business_rules_validated",false);out.put("tax_arithmetic_checked",false);out.put("filing_available",false);out.put("source_authenticity","NOT_VERIFIED");out.put("taxpayer_identity_verified",false);out.put("raw_document_retained",false);return out;
    }
    @Override public synchronized void close(){closed=true;previews.clear();for(Process child:children)child.destroyForcibly();}
}
