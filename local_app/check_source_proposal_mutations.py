#!/usr/bin/env python3
"""Selected compiled source-copy defects; engineering evidence, not tax validation."""
from pathlib import Path
import hashlib,importlib.util,io,json,os,queue,shutil,subprocess,tempfile,threading,unittest
ROOT=Path(__file__).resolve().parent
CORE=ROOT.parent/'java_result_safety/build/tax-result-safety-0.1.4.jar'
ENGINE=ROOT.parent/'tax_engine/build/tax-planner-workbench-0.1.0.jar'
POLICY='src/main/java/com/taxplatform/localdemo/SourceProposalPolicy.java'
SERVER='src/main/java/com/taxplatform/localdemo/DemoServer.java'
variants=[
 ('transaction_allowed_as_annual',POLICY,'if(group.coverage()!=StatementComparison.Coverage.ANNUAL)','if(false)',None),
 ('second_category_group_ignored',POLICY,'if(all.stream().filter(g->g.category()==group.category()).count()!=1)','if(false)',None),
 ('tds_transferred_instead_of_gross',POLICY,'Money gross=source.selected().gross().orElseThrow','Money gross=source.selected().tds().orElseThrow',None),
 ('source_automatically_confirms',POLICY,'new FactEdits.ProposeAmount(reference.field(),amount.decimal(),HumanMoneyParser.Unit.RUPEES)','new FactEdits.ConfirmAmount(reference.field(),amount)',None),
 ('source_retry_restores_lineage',SERVER,'if(result.status()!=FactEditResult.Status.REPLAYED)session.sourceLinks.put(field,link);','if(true)session.sourceLinks.put(field,link);','test_source_link_is_cleared_by_manual_same_value_and_clear'),
 ('manual_retry_erases_new_lineage',SERVER,'if(result.status()!=FactEditResult.Status.REPLAYED){','if(true){','test_old_manual_retry_does_not_clear_new_source_link')]
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
sources=sorted((ROOT/'src').rglob('*.java'));before={str(p.relative_to(ROOT)):sha(p) for p in sources}
spec=importlib.util.spec_from_file_location('source_http',ROOT/'test_http.py');http=importlib.util.module_from_spec(spec);spec.loader.exec_module(http)
results=[]
for name,relative,old,new,http_test in variants:
 with tempfile.TemporaryDirectory(prefix='source-proposal-defect-') as folder:
  copy=Path(folder);shutil.copytree(ROOT/'src',copy/'src');p=copy/relative;text=p.read_text();assert text.count(old)==1,name;p.write_text(text.replace(old,new))
  classes=copy/'classes';classes.mkdir();classpath=os.pathsep.join([str(classes),str(CORE),str(ENGINE)])
  built=subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','--add-modules','jdk.httpserver','-encoding','UTF-8','-proc:none','-Xlint:all','-Werror','-cp',os.pathsep.join([str(CORE),str(ENGINE)]),'-d',str(classes),*[str(p) for p in sorted((copy/'src').rglob('*.java'))]],capture_output=True,text=True,timeout=45)
  if built.returncode:print(built.stdout+built.stderr);raise SystemExit('Compilation failure is not a detected defect: '+name)
  if http_test:
   process=subprocess.Popen(['java','--add-modules','jdk.httpserver','-cp',classpath,'com.taxplatform.localdemo.DemoServer','--port','0'],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
   startup=queue.Queue()
   def read_start():
    for line in process.stdout:
     if line.startswith('LOCAL_DEMO_URL='):startup.put(line.strip().split('=',1)[1]);return
   threading.Thread(target=read_start,daemon=True).start()
   try:
    http.BASE=startup.get(timeout=10)
    log=io.StringIO();tested=unittest.TextTestRunner(stream=log,verbosity=2).run(unittest.TestSuite([http.HTTPTests(http_test)]))
    if tested.errors or len(tested.failures)!=1:raise SystemExit('Expected assertion failure missing: '+name+' '+log.getvalue())
   finally:
    process.terminate()
    try:process.wait(timeout=5)
    except subprocess.TimeoutExpired:process.kill();process.wait(timeout=5)
  else:
   tested=subprocess.run(['java','--add-modules','jdk.httpserver','-cp',classpath,'com.taxplatform.localdemo.SourceProposalTests'],capture_output=True,text=True,timeout=30)
   if tested.returncode==0 or not any(x in tested.stderr for x in ['AssertionError','ComparisonWorkspace$Rejected']):raise SystemExit('No behavioral rejection: '+name)
  results.append({'name':name,'compiled':True,'behavioral_failure':True,'test_layer':'actual_loopback_http' if http_test else 'policy_unit'})
  print('Detected '+name,flush=True)
assert before=={str(p.relative_to(ROOT)):sha(p) for p in sources}
report={'source_proposal_variants_compiled':len(results),'source_proposal_variants_detected':len(results),'source_preserved':True,'inputs':before,'http_suite_sha256':sha(ROOT/'test_http.py'),'variants':results,'tax_validation':False,'production_release':False}
(ROOT/'build/source_proposal_mutation_results.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k!='inputs'},indent=2))
