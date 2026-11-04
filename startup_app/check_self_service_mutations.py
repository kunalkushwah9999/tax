#!/usr/bin/env python3
"""Independent compiled preparation-policy defects; never law or security certification."""
from pathlib import Path
import hashlib,json,os,shutil,subprocess,tempfile
ROOT=Path(__file__).resolve().parent
variants=[
 ('fiscal_year_missing_from_invoice_identity','GstReconciliation.java','(String)row.get("financial_year"),',''),
 ('unknown_amount_treated_as_explicit_zero','GstReconciliation.java','value.equals("UNKNOWN")?value:new BigDecimal(value)','value.equals("UNKNOWN")?"0.00":new BigDecimal(value)'),
 ('mismatch_can_be_marked_as_reviewed_match','GstReconciliation.java','Set.of("SUPPLIER_FOLLOW_UP","BOOKS_CHECK_PENDING"','Set.of("REVIEWED_MATCH","SUPPLIER_FOLLOW_UP","BOOKS_CHECK_PENDING"'),
 ('stale_worksheet_review_version_ignored','GstReconciliation.java','if(!sheet.get("version").equals(Workspace.text(command,"worksheet_version",1,64)))','if(false)'),
 ('private_self_service_data_leaks_to_shared_case','Workspace.java','out.remove("self_service");',''),
 ('old_scope_declaration_survives_material_edit','SelfServicePreparation.java','saved!=null&&fingerprint(row).equals(saved.get("reviewed_fingerprint"))','saved!=null')]
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
before={str(p.relative_to(ROOT)):sha(p) for p in sorted((ROOT/'src').rglob('*.java'))};results=[]
libs=[ROOT.parent/'java_result_safety/build/tax-result-safety-0.1.4.jar',ROOT.parent/'tax_engine/build/tax-planner-workbench-0.1.0.jar',ROOT.parent/'local_app/build/tax-preparation-local-demo-0.4.0.jar']
for name,file,old,new in variants:
 with tempfile.TemporaryDirectory(prefix='self-service-defect-') as temporary:
  folder=Path(temporary);shutil.copytree(ROOT/'src',folder/'src');target=folder/'src/main/java/com/taxplatform/startup'/file;s=target.read_text();assert s.count(old)==1,(name,s.count(old));target.write_text(s.replace(old,new));classes=folder/'classes';classes.mkdir()
  compile=subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','--add-modules','jdk.httpserver','-encoding','UTF-8','-proc:none','-Xlint:all','-Werror','-cp',str(ROOT/'build/dependency-classes'),'-d',str(classes),*map(str,sorted((folder/'src').rglob('*.java')))],capture_output=True,text=True,timeout=45)
  if compile.returncode:print(compile.stdout+compile.stderr);raise SystemExit('Compile failure is not behavioral evidence: '+name)
  run=subprocess.run(['java','--add-modules','jdk.httpserver','-cp',os.pathsep.join(map(str,[classes,*libs])),'com.taxplatform.startup.SelfServiceTests'],capture_output=True,text=True,timeout=60)
  if run.returncode==0 or 'AssertionError' not in run.stderr:print(run.stdout+run.stderr);raise SystemExit('Expected assertion failure missing: '+name)
  results.append({'name':name,'compiled':True,'behavioral_assertion_failed':True});print('Detected '+name,flush=True)
assert before=={str(p.relative_to(ROOT)):sha(p) for p in sorted((ROOT/'src').rglob('*.java'))}
report={'self_service_compiled_defects':len(results),'self_service_detected_defects':len(results),'source_preserved':True,'inputs':before,'variants':results,'tax_law_approved':False,'security_certification':False}
(ROOT/'build/self_service_mutation_results.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k!='inputs'},indent=2))
