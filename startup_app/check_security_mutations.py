#!/usr/bin/env python3
"""Compiled account-security defects must fail behavior assertions, not compilation."""
from pathlib import Path
import hashlib,json,os,shutil,subprocess,tempfile
ROOT=Path(__file__).resolve().parent
variants=[
 ('totp_reuse_accepted','Totp.java','step>lastStep','step>=lastStep'),
 ('recovery_code_not_consumed','AccountSecurity.java','hashes.remove(found);',''),
 ('plaintext_recovery_codes_stored','AccountSecurity.java','return Crypto.hash("TAXDESK_RECOVERY_V1\\n"+actor+"\\n"+code);','return code;'),
 ('request_principal_epoch_ignored','Workspace.java','if(principal!=null&&principal.actor().equals(userId)&&principal.epoch()!=AccountSecurity.epoch(row))','if(false)'),
 ('stale_password_challenge_epoch_ignored','AccountSecurity.java','if(epoch(user)!=expectedEpoch||!enabled(user))','if(!enabled(user))'),
 ('current_password_not_required','AccountSecurity.java','if(!Crypto.verifies(password,Json.map(user.get("credential"))))','if(false)'),
 ('pending_setup_enables_mfa','AccountSecurity.java','String secret=Totp.secret(),setup=Workspace.id();','String secret=Totp.secret(),setup=Workspace.id();s.put("totp_secret",secret);'),
 ('password_only_service_bypasses_mfa','Workspace.java','if(proof.mfaRequired())throw new Problem(401,"MFA_REQUIRED");',''),
 ('secret_leaks_in_settings_projection','AccountSecurity.java','out.put("phishing_resistant",false);','out.put("phishing_resistant",false);out.put("secret",s.get("totp_secret"));'),
 ('failed_proof_counters_not_persisted','AccountSecurity.java','save(state,user,s);audit(state,(String)user.get("id"),"AUTHENTICATION_PROOF_REJECTED");','audit(state,(String)user.get("id"),"AUTHENTICATION_PROOF_REJECTED");')]
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
before={str(p.relative_to(ROOT)):sha(p) for p in sorted((ROOT/'src').rglob('*.java'))};results=[]
libs=[ROOT.parent/'java_result_safety/build/tax-result-safety-0.1.4.jar',ROOT.parent/'tax_engine/build/tax-planner-workbench-0.1.0.jar',ROOT.parent/'local_app/build/tax-preparation-local-demo-0.4.0.jar']
for name,file,old,new in variants:
 with tempfile.TemporaryDirectory(prefix='security-defect-') as temp:
  folder=Path(temp);shutil.copytree(ROOT/'src',folder/'src');target=folder/'src/main/java/com/taxplatform/startup'/file;s=target.read_text();assert s.count(old)==1,(name,s.count(old));target.write_text(s.replace(old,new));classes=folder/'classes';classes.mkdir()
  compile=subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','--add-modules','jdk.httpserver','-encoding','UTF-8','-proc:none','-Xlint:all','-Werror','-cp',str(ROOT/'build/dependency-classes'),'-d',str(classes),*map(str,sorted((folder/'src').rglob('*.java')))],capture_output=True,text=True,timeout=45)
  if compile.returncode:raise RuntimeError('Compile failure is not behavioral evidence: '+name+'\n'+compile.stdout+compile.stderr)
  run=subprocess.run(['java','--add-modules','jdk.httpserver','-cp',os.pathsep.join(map(str,[classes,*libs])),'com.taxplatform.startup.AccountSecurityTests'],capture_output=True,text=True,timeout=60)
  if run.returncode==0 or 'AssertionError' not in run.stderr:raise RuntimeError('Expected behavioral failure missing: '+name+'\n'+run.stdout+run.stderr)
  results.append({'name':name,'compiled':True,'behavioral_assertion_failed':True});print('Detected '+name,flush=True)
assert before=={str(p.relative_to(ROOT)):sha(p) for p in sorted((ROOT/'src').rglob('*.java'))}
report={'security_compiled_defects':len(results),'security_detected_defects':len(results),'source_preserved':True,'inputs':before,'variants':results,'conformance_certification':False,'tax_approval':False};(ROOT/'build/security_mutation_results.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k!='inputs'},indent=2))
