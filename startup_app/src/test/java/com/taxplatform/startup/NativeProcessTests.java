package com.taxplatform.startup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Controlled fake worker processes exercise transport failures, not schema/tax correctness. */
final class NativeProcessTests {
    private static int passed;
    private NativeProcessTests(){ }
    @FunctionalInterface private interface Test{void run()throws Exception;}
    private static void check(String name,Test test)throws Exception{test.run();passed++;System.out.println("PASS "+name);}
    private static void eq(Object a,Object b){if(!java.util.Objects.equals(a,b))throw new AssertionError("native process assertion failed");}
    private static void fail(String code,Runnable work){try{work.run();}catch(Workspace.Problem e){eq(e.status,503);eq(e.code,code);return;}throw new AssertionError("expected native process rejection: "+code);}
    private static NativeImports configured(Path python,Path script,String mode)throws Exception{return NativeImports.configured(python,script,Path.of(mode));}
    public static void main(String[] args)throws Exception{
        Path directory=Files.createTempDirectory("native-process-faults-");Path script=directory.resolve("fault.py"),python=Path.of(args[0]);
        String code="""
                import json,sys,time
                mode=sys.argv[sys.argv.index('--schema-dir')+1].split('/')[-1]
                if '--self-check' in sys.argv:
                    print(json.dumps({'status':'NATIVE_WORKER_CONFIGURED','schema_sha256':'BAD' if mode=='badpin' else '6a0ef9f64065431879dcc724869d8455a640bcf11279d1818eb19f11448ce763'}));raise SystemExit()
                if mode=='noread':time.sleep(12);raise SystemExit()
                raw=sys.stdin.buffer.read()
                if mode=='malformed':print('{broken');raise SystemExit()
                if mode=='oversized':print('X'*70000);raise SystemExit()
                if mode=='badshape':print('{}');raise SystemExit()
                if mode=='slow' and raw!=b'FAST':
                    marker=__file__+'.started.'+str(__import__('os').getpid())
                    open(marker,'w').close();time.sleep(12)
                print(json.dumps({'contract_version':'native-itr-preflight.v1','schema_id':'ITR1-AY2026-27-20260630-V1.1','schema_sha256':'6a0ef9f64065431879dcc724869d8455a640bcf11279d1818eb19f11448ce763','schema_validation':'PASS','document_format':'MATCH','proposal_pack':'AVAILABLE','issues':[],'warnings':[],'proposals':[{'field':'SALARY','amount':'1.00','source_path':'/ITR/ITR1/ITR1_IncomeDeductions/GrossSalary'}]}))
                """;
        Files.writeString(script,code);try{
            check("wrong self-check pin cannot configure a worker",()->{try(NativeImports ignored=configured(python,script,"badpin")){ignored.capabilities();throw new AssertionError("bad pin accepted");}catch(java.io.IOException expected){/* explicit configuration refusal */}});
            check("malformed oversized and bad-shape output never become client validation errors",()->{for(String mode:List.of("malformed","oversized","badshape"))try(NativeImports imports=configured(python,script,mode)){fail(mode.equals("badshape")?"NATIVE_WORKER_CONTRACT_REJECTED":"NATIVE_WORKER_UNAVAILABLE",()->imports.preview("a","c",1,"{}"));}});
            check("a worker that never consumes stdin cannot block an HTTP worker indefinitely",()->{try(NativeImports imports=configured(python,script,"noread")){long start=System.nanoTime();fail("NATIVE_WORKER_TIMEOUT",()->imports.preview("a","c",1,"x".repeat(196608)));eq(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()-start)<8,true);}});
            check("two real child processes enforce the shared cap and timeout releases slots",()->{try(NativeImports imports=configured(python,script,"slow")){var pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);try{var first=pool.submit(()->{start.await();try{imports.preview("a","one",1,"SLOW");return "unexpected";}catch(Workspace.Problem e){return e.code;}});var second=pool.submit(()->{start.await();try{imports.preview("a","two",1,"SLOW");return "unexpected";}catch(Workspace.Problem e){return e.code;}});start.countDown();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);boolean ready=false;while(System.nanoTime()<deadline){try(var files=Files.list(directory)){if(files.filter(p->p.getFileName().toString().startsWith("fault.py.started.")).count()==2){ready=true;break;}}Thread.sleep(10);}eq(ready,true);fail("NATIVE_IMPORT_BUSY",()->imports.preview("a","three",1,"FAST"));eq(first.get(8,TimeUnit.SECONDS),"NATIVE_WORKER_TIMEOUT");eq(second.get(8,TimeUnit.SECONDS),"NATIVE_WORKER_TIMEOUT");eq(imports.preview("a","fresh",1,"FAST").get("schema_validation"),"PASS");}finally{pool.shutdownNow();}}});
        }finally{try(var files=Files.walk(directory)){for(Path p:files.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
        Map<String,Object> report=Map.of("native_process_fault_groups",passed,"actual_child_processes",true,"controlled_worker_fixtures",true,"schema_correctness_tested_by_this_suite",false,"production_ready",false);Files.writeString(Path.of(args[1]),Json.encode(report)+"\n");System.out.println(Json.encode(report));
    }
}
