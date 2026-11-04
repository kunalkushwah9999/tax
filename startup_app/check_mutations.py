#!/usr/bin/env python3
"""Selected compiled startup workflow defects, never a tax/security certification."""
from pathlib import Path
import hashlib,json,os,shutil,subprocess,tempfile
ROOT=Path(__file__).resolve().parent;TARGET='src/main/java/com/taxplatform/startup/Workspace.java'
libs=[ROOT.parent/'java_result_safety/build/tax-result-safety-0.1.4.jar',ROOT.parent/'tax_engine/build/tax-planner-workbench-0.1.0.jar',ROOT.parent/'local_app/build/tax-preparation-local-demo-0.4.0.jar']
variants=[
 ('private_case_owner_check_removed','raw==null||!Json.map(raw).get("owner_id").equals(actor)','raw==null'),
 ('revoked_preparation_access_still_allowed','g.get("preparer_id").equals(actor)&&Boolean.TRUE.equals(g.get("active"))','g.get("preparer_id").equals(actor)'),
 ('stale_case_version_ignored','if(((Number)row.get("revision")).longValue()!=expected)','if(false)'),
 ('manual_proposal_auto_confirms','Map<String,Object> replacement=Map.of("state","ESTIMATED","amount",money.decimal())','Map<String,Object> replacement=Map.of("state","CONFIRMED","amount",money.decimal())'),
 ('deleted_create_receipt_forgotten','table(state,"cases").remove(caseId);','table(state,"cases").remove(caseId);table(state,"receipts").entrySet().removeIf(e->Json.map(e.getValue()).get("resource").equals(caseId));'),
 ('new_price_rewrites_old_quotes','audit(state,actor,"FUTURE_PRICE_VERSION_CREATED",priceId);','for(Object raw:table(state,"quotes").values()){Json.map(raw);Map<String,Object> q=table(state,"quotes").containsValue(raw)?Json.map(raw):new LinkedHashMap<>();q.put("amount_paise",price);table(state,"quotes").put((String)q.get("id"),q);}audit(state,actor,"FUTURE_PRICE_VERSION_CREATED",priceId);')]
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();before={str(p.relative_to(ROOT)):sha(p) for p in sorted((ROOT/'src').rglob('*.java'))};results=[]
for name,old,new in variants:
 with tempfile.TemporaryDirectory(prefix='startup-workflow-defect-') as folder:
  copy=Path(folder);shutil.copytree(ROOT/'src',copy/'src');p=copy/TARGET;text=p.read_text();assert text.count(old)==1,(name,text.count(old));p.write_text(text.replace(old,new));classes=copy/'classes';classes.mkdir()
  compile_cp=str(ROOT/'build/dependency-classes');compiled=subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','--add-modules','jdk.httpserver','-encoding','UTF-8','-proc:none','-Xlint:all','-Werror','-cp',compile_cp,'-d',str(classes),*[str(p) for p in sorted((copy/'src').rglob('*.java'))]],capture_output=True,text=True,timeout=45)
  if compiled.returncode:print(compiled.stdout+compiled.stderr);raise SystemExit('Compilation failure is not counted: '+name)
  run=subprocess.run(['java','--add-modules','jdk.httpserver','-cp',os.pathsep.join([str(classes),*map(str,libs)]),'com.taxplatform.startup.WorkspaceTests'],capture_output=True,text=True,timeout=45)
  if run.returncode==0 or not any(t in run.stderr for t in ['AssertionError','Workspace$Problem']):print(run.stdout+run.stderr);raise SystemExit('Expected behavioral failure missing: '+name)
  results.append({'name':name,'compiled':True,'behavioral_failure':True});print('Detected '+name,flush=True)
assert before=={str(p.relative_to(ROOT)):sha(p) for p in sorted((ROOT/'src').rglob('*.java'))}
report={'startup_compiled_mutants':len(results),'startup_detected_mutants':len(results),'source_preserved':True,'inputs':before,'variants':results,'tax_validation':False,'security_audit':False}
(ROOT/'build/mutation_results.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k!='inputs'},indent=2))
