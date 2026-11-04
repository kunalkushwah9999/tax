#!/usr/bin/env python3
"""Compiled SQL defect experiments invoked only by the disposable real-MySQL harness."""
from pathlib import Path
import hashlib,json,os,shutil,subprocess,tempfile
ROOT=Path(__file__).resolve().parent
variants=[
 ('database_write_guard_removed','src/main/java/com/taxplatform/startup/MysqlStore.java','+(write?" FOR UPDATE":"")','+""'),
 ('accepted_receipt_not_committed','src/main/java/com/taxplatform/startup/MysqlStore.java','for(SqlTable table:tables.values())bytes+=table.flush();','for(SqlTable table:tables.values())if(!table.namespace.equals("receipts"))bytes+=table.flush();'),
 ('record_identity_missing_from_aad','src/main/java/com/taxplatform/startup/RecordCrypto.java','java.util.List.of("mysql-record.v1",namespace,id,revision)','java.util.List.of("mysql-record.v1",namespace,namespace.equals("meta")?id:"UNBOUND",revision)')]
def run_variants(fixtures,driver):
 sources=sorted((ROOT/'src').rglob('*.java'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();before={str(p.relative_to(ROOT)):sha(p) for p in sources};results=[]
 libs=[ROOT/'build/tax-platform-startup-5.1.0.jar',driver,ROOT.parent/'java_result_safety/build/tax-result-safety-0.1.4.jar',ROOT.parent/'tax_engine/build/tax-planner-workbench-0.1.0.jar',ROOT.parent/'local_app/build/tax-preparation-local-demo-0.4.0.jar']
 for variant,fixture in zip(variants,fixtures,strict=True):
  name,relative,old,new=variant;app,admin,key=fixture
  with tempfile.TemporaryDirectory(prefix='mysql-compiled-defect-') as folder:
   copy=Path(folder);shutil.copytree(ROOT/'src',copy/'src');p=copy/relative;text=p.read_text();assert text.count(old)==1,(name,text.count(old));p.write_text(text.replace(old,new));classes=copy/'classes';classes.mkdir()
   compiled=subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','--add-modules','jdk.httpserver,java.sql','-encoding','UTF-8','-proc:none','-Xlint:all','-Werror','-cp',str(ROOT/'build/dependency-classes'),'-d',str(classes),*[str(p) for p in sorted((copy/'src').rglob('*.java'))]],capture_output=True,text=True,timeout=45)
   if compiled.returncode:raise RuntimeError('Compilation failure is not counted: '+name+' '+compiled.stderr)
   tested=subprocess.run(['java','--add-modules','jdk.httpserver,java.sql','-cp',os.pathsep.join([str(classes),*map(str,libs)]),'com.taxplatform.startup.MysqlTests',str(app),str(admin),str(key),str(copy/'unused-report.json')],capture_output=True,text=True,timeout=90)
   if tested.returncode==0 or 'AssertionError' not in tested.stderr:raise RuntimeError('Expected actual SQL assertion missing: '+name+' '+tested.stderr[-1200:])
   results.append({'name':name,'compiled':True,'actual_mysql_behavioral_failure':True});print('Detected SQL defect '+name,flush=True)
 assert before=={str(p.relative_to(ROOT)):sha(p) for p in sources}
 report={'mysql_compiled_defects':len(results),'mysql_detected_defects':len(results),'source_preserved':True,'source_inputs':before,'driver_sha256':sha(driver),'variants':results,'security_certification':False};(ROOT/'build/mysql_mutation_results.json').write_text(json.dumps(report,indent=2)+'\n');return report
