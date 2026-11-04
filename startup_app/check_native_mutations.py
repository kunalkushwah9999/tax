#!/usr/bin/env python3
"""Selected compiled native-proposal protocol defects; not schema/tax certification."""
from pathlib import Path
import hashlib,json,os,shutil,subprocess,tempfile
ROOT=Path(__file__).resolve().parent
variants=[
 ('native_proposal_auto_confirms','Workspace.java','inputs.put(field,Map.of("state","ESTIMATED","amount",offered.amount()))','inputs.put(field,Map.of("state","CONFIRMED","amount",offered.amount()))'),
 ('stale_native_preview_revision_ignored','Workspace.java','preview.revision()!=((Number)row.get("revision")).longValue()','false'),
 ('preview_owner_and_case_binding_removed','NativeImports.java','||!preview.actor().equals(actor)||!preview.caseId().equals(caseId)',''),
 ('expired_preview_consulted_before_accepted_replay','Workspace.java','var prior=replay(state,actor,body,"native-propose",caseId);','source.get();var prior=replay(state,actor,body,"native-propose",caseId);'),
 ('native_lineage_leaks_through_prior_preparer_grant','Workspace.java','if(reviewOnly)out.remove("native_import_links");',''),
 ('same_taxpayer_acknowledgement_ignored','Workspace.java','if(!flag(body,"same_taxpayer_confirmed"))','if(false)')]
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();before={str(p.relative_to(ROOT)):sha(p) for p in sorted((ROOT/'src').rglob('*.java'))};results=[]
libs=[ROOT.parent/'java_result_safety/build/tax-result-safety-0.1.4.jar',ROOT.parent/'tax_engine/build/tax-planner-workbench-0.1.0.jar',ROOT.parent/'local_app/build/tax-preparation-local-demo-0.4.0.jar']
for name,file,old,new in variants:
 with tempfile.TemporaryDirectory(prefix='native-protocol-defect-') as temporary:
  folder=Path(temporary);shutil.copytree(ROOT/'src',folder/'src');p=folder/'src/main/java/com/taxplatform/startup'/file;text=p.read_text();assert text.count(old)==1,(name,text.count(old));p.write_text(text.replace(old,new));classes=folder/'classes';classes.mkdir()
  compiled=subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','--add-modules','jdk.httpserver,java.sql','-encoding','UTF-8','-proc:none','-Xlint:all','-Werror','-cp',str(ROOT/'build/dependency-classes'),'-d',str(classes),*map(str,sorted((folder/'src').rglob('*.java')))],capture_output=True,text=True,timeout=45)
  if compiled.returncode:print(compiled.stdout+compiled.stderr);raise SystemExit('Compilation failure is not evidence: '+name)
  run=subprocess.run(['java','--add-modules','jdk.httpserver','-cp',os.pathsep.join(map(str,[classes,folder/'src/main/resources',*libs])),'com.taxplatform.startup.NativeImportTests'],capture_output=True,text=True,timeout=60)
  if run.returncode==0 or not any(word in run.stderr for word in ['AssertionError','Workspace$Problem']):print(run.stdout+run.stderr);raise SystemExit('Expected behavioral failure missing: '+name)
  results.append({'name':name,'compiled':True,'behavioral_failure':True});print('Detected '+name,flush=True)
assert before=={str(p.relative_to(ROOT)):sha(p) for p in sorted((ROOT/'src').rglob('*.java'))}
report={'native_compiled_defects':len(results),'native_detected_defects':len(results),'source_preserved':True,'inputs':before,'variants':results,'independent_tax_approval':False};(ROOT/'build/native_mutation_results.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k!='inputs'},indent=2))
