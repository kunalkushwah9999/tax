#!/usr/bin/env python3
"""Compile deliberately broken commerce/consent variants, then require targeted real-HTTP assertions to fail."""
from pathlib import Path
import hashlib,json,os,shutil,subprocess,tempfile
ROOT=Path(__file__).resolve().parent
variants=[
 ('unresolved_order_barrier_removed','StartupOperations.java','if(rows(state,"ORDER").stream().anyMatch(r->actor.equals(r.get("owner_id"))&&plan.get("plan").equals(r.get("plan"))&&Set.of("CREATE_PENDING","OUTCOME_UNKNOWN").contains(r.get("status"))))throw new Problem(409,"EXISTING_ORDER_RECONCILIATION_REQUIRED");','', 'test_06_unknown_create_outcome_is_not_retried_and_reconcile_checks_binding'),
 ('unsigned_callback_accepted','StartupOperations.java','gateway.verifyCallback((String)snapshot.get("provider_order_id"),payment,text(body,"signature",64,64));','', 'test_09_callback_signature_is_bound_to_stored_order'),
 ('captured_amount_not_checked','StartupOperations.java','number(payment,"amount")!=number(row,"amount_paise")','false','test_10_authorized_or_amount_mismatched_payment_does_not_grant_access'),
 ('delayed_capture_erases_refund','StartupOperations.java','Math.max(number(row,"amount_refunded"),number(payment,"amount_refunded"))','number(payment,"amount_refunded")','test_14_partial_refund_suspends_and_stale_capture_cannot_reenable'),
 ('order_replay_posts_again','StartupOperations.java','if(!Boolean.TRUE.equals(reserved.get("new")))return publicOrder(row);','','test_05_identical_parallel_order_requests_create_one_provider_order'),
 ('credential_revocation_ignored_for_existing_grant','Workspace.java','&&(!g.containsKey("professional_subject")||professionalRegistry.valid((String)g.get("professional_subject"),(String)g.get("professional_topic")))','','test_25_signed_credential_revocation_and_tamper_immediately_deny_existing_grant'),
 ('feedback_customer_can_triage','StartupOperations.java','role(workspace.user(state,actor),"ADMIN");Map<String,Object> row=get(state,feedbackId,"FEEDBACK");','workspace.user(state,actor);Map<String,Object> row=get(state,feedbackId,"FEEDBACK");','test_29_feedback_admin_triage_has_revision_and_customer_visible_reply'),
 ('same_snapshot_bundle_charged_twice','StartupOperations.java','if(previous!=null){remember(state,actor,"bundle:"+caseId,body,(String)previous.get("id"));return bundleRecord(state,row,previous);}','', 'test_18_bundle_same_snapshot_and_exact_retry_consume_one_credit')]
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
inputs=[*sorted((ROOT/'src').rglob('*.java')),ROOT/'test_http.py',ROOT/'test_operations.py'];before={str(p.relative_to(ROOT)):sha(p) for p in inputs};results=[]
for name,file,old,new,test in variants:
 with tempfile.TemporaryDirectory(prefix='tax-operations-defect-') as temp:
  folder=Path(temp)/'package';app=folder/'startup_app';app.mkdir(parents=True);shutil.copytree(ROOT/'src/main',app/'src/main')
  for dependency in ['java_result_safety','tax_engine','local_app']:(folder/dependency).symlink_to(ROOT.parent/dependency,target_is_directory=True)
  for source in ['run.py','test_http.py','test_operations.py']:shutil.copy2(ROOT/source,app/source)
  path=app/'src/main/java/com/taxplatform/startup'/file;s=path.read_text();count=s.count(old)
  # The professional predicate also appears in the owner grant projection; both must fail closed.
  if file=='Workspace.java':assert count==2,(name,count)
  else:assert count==1,(name,count)
  path.write_text(s.replace(old,new));build=app/'build';classes=build/'classes';classes.mkdir(parents=True)
  compile=subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','--add-modules','jdk.httpserver,java.sql','-encoding','UTF-8','-proc:none','-Xlint:all','-Werror','-cp',str(ROOT/'build/dependency-classes'),'-d',str(classes),*map(str,sorted((app/'src/main/java').rglob('*.java')))],capture_output=True,text=True,timeout=50)
  if compile.returncode:raise SystemExit('Compilation failure is not a detected behavior: '+name+'\n'+compile.stdout+compile.stderr)
  manifest=build/'manifest';manifest.write_text('Manifest-Version: 1.0\nMain-Class: com.taxplatform.startup.Server\nClass-Path: ../../java_result_safety/build/tax-result-safety-0.1.4.jar ../../tax_engine/build/tax-planner-workbench-0.1.0.jar ../../local_app/build/tax-preparation-local-demo-0.4.0.jar\n\n');jar=build/'tax-platform-startup-5.1.0.jar'
  subprocess.run(['java','-m','jdk.jartool/sun.tools.jar.Main','--create','--file',str(jar),'--manifest',str(manifest),'-C',str(classes),'.','-C',str(app/'src/main/resources'),'.'],check=True,capture_output=True)
  run=subprocess.run(['python',str(app/'test_operations.py')],env=dict(os.environ,TAX_OPERATIONS_SELECTED_GROUP=test),capture_output=True,text=True,timeout=70)
  if run.returncode==0 or 'AssertionError' not in run.stderr:raise SystemExit('Required behavioral failure missing: '+name+'\n'+run.stdout+run.stderr[-3500:])
  results.append({'name':name,'compiled':True,'target_test':test,'behavioral_failure':True});print('Detected '+name,flush=True)
assert before=={str(p.relative_to(ROOT)):sha(p) for p in inputs}
report={'operations_compiled_defects':len(results),'operations_detected_defects':len(results),'source_preserved':True,'inputs':before,'variants':results,'real_charges':False,'independent_tax_approval':False};(ROOT/'build/operations_mutation_results.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k!='inputs'},indent=2))
