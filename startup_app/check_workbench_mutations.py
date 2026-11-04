#!/usr/bin/env python3
"""Compiled defects must fail behavior assertions. No statutory-tax or security certification."""
from pathlib import Path
import hashlib,json,os,shutil,subprocess,tempfile
ROOT=Path(__file__).resolve().parent
variants=[
 ('fifo_crosses_account_boundary','TaxWorkbench.java','row.get("account_alias")+"|"+row.get("asset_alias")','(String)row.get("asset_alias")'),
 ('partial_cost_loses_residual_paise','TaxWorkbench.java','v.multiply(BigInteger.valueOf(before+matched)).divide(t).subtract(v.multiply(BigInteger.valueOf(before)).divide(t))','v.multiply(BigInteger.valueOf(matched)).divide(t)'),
 ('unknown_amount_becomes_zero','TaxWorkbench.java','if(value.equals("UNKNOWN"))return null;','if(value.equals("UNKNOWN"))return 0L;'),
 ('private_working_papers_leak','Workspace.java','out.remove("tax_workbench");',''),
 ('capital_sale_enters_narrow_engine','Workspace.java','if(TaxWorkbench.hasCapitalSales(row))throw new Problem(409,"CAPITAL_GAINS_REQUIRE_EXPANDED_TAX_ENGINE");',''),
 ('tds_observations_grant_claim','TaxWorkbench.java','out.put("claim_amount",null);','out.put("claim_amount","40000.00");'),
 ('user_receipt_becomes_verified','TaxWorkbench.java','n.put("official_submission_verified",false);','n.put("official_submission_verified",true);'),
 ('stale_working_paper_version_ignored','TaxWorkbench.java','if(!expected.equals(work.getOrDefault("version","")))','if(false)')]
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
before={str(p.relative_to(ROOT)):sha(p) for p in sorted((ROOT/'src').rglob('*.java'))};results=[]
libs=[ROOT.parent/'java_result_safety/build/tax-result-safety-0.1.4.jar',ROOT.parent/'tax_engine/build/tax-planner-workbench-0.1.0.jar',ROOT.parent/'local_app/build/tax-preparation-local-demo-0.4.0.jar']
for name,file,old,new in variants:
 with tempfile.TemporaryDirectory(prefix='workbench-defect-') as temporary:
  folder=Path(temporary);shutil.copytree(ROOT/'src',folder/'src');target=folder/'src/main/java/com/taxplatform/startup'/file;s=target.read_text();assert s.count(old)==1,(name,s.count(old));target.write_text(s.replace(old,new));classes=folder/'classes';classes.mkdir()
  compile=subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','--add-modules','jdk.httpserver','-encoding','UTF-8','-proc:none','-Xlint:all','-Werror','-cp',str(ROOT/'build/dependency-classes'),'-d',str(classes),*map(str,sorted((folder/'src').rglob('*.java')))],capture_output=True,text=True,timeout=45)
  if compile.returncode:raise RuntimeError('Compile failure is not behavioral evidence: '+name+'\n'+compile.stdout+compile.stderr)
  run=subprocess.run(['java','--add-modules','jdk.httpserver','-cp',os.pathsep.join(map(str,[classes,*libs])),'com.taxplatform.startup.TaxWorkbenchTests'],capture_output=True,text=True,timeout=60)
  if run.returncode==0 or 'AssertionError' not in run.stderr:raise RuntimeError('Expected assertion failure missing: '+name+'\n'+run.stdout+run.stderr)
  results.append({'name':name,'compiled':True,'behavioral_assertion_failed':True});print('Detected '+name,flush=True)
assert before=={str(p.relative_to(ROOT)):sha(p) for p in sorted((ROOT/'src').rglob('*.java'))}
report={'workbench_compiled_defects':len(results),'workbench_detected_defects':len(results),'source_preserved':True,'inputs':before,'variants':results,'tax_law_approved':False,'security_certification':False}
(ROOT/'build/workbench_mutation_results.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k!='inputs'},indent=2))
