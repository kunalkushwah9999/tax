#!/usr/bin/env python3
"""Four compiled schema-2 defects, behaviorally checked against independent disposable real-MySQL databases."""
from pathlib import Path
import hashlib,json,os,shutil,subprocess,tempfile
ROOT=Path(__file__).resolve().parent
VARIANTS=[
 ('read_dependencies_not_validated','if(!table.ns.equals("audit"))table.validate();','if(false)table.validate();','read dependency validation prevents two-record write skew'),
 ('scan_epoch_check_disabled','if(current.get(controlKey(ns,i)).epoch()!=snapshot(ns,i).epoch())throw new Conflict();','if(false)throw new Conflict();','scan phantoms cause a fresh callback before publishing dependent state'),
 ('lost_commit_ack_misreported','write&&commitStarted?"STORAGE_OUTCOME_UNKNOWN":"MYSQL_STORAGE_UNAVAILABLE"','false?"STORAGE_OUTCOME_UNKNOWN":"MYSQL_STORAGE_UNAVAILABLE"','lost acknowledgement after a real commit safely replays once'),
 ('capacity_predicate_not_checked','if(count+delta>table.capacity)throw new Workspace.Problem(409,table.capacityCode);','if(false)throw new Workspace.Problem(409,table.capacityCode);','capacity predicate rejects a concurrent oversubscription atomically')]
def run_variants(fixtures,driver):
 sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();sources=sorted((ROOT/'src').rglob('*.java'));before={str(p.relative_to(ROOT)):sha(p) for p in sources};results=[]
 libs=[ROOT.parent/'java_result_safety/build/tax-result-safety-0.1.4.jar',ROOT.parent/'tax_engine/build/tax-planner-workbench-0.1.0.jar',ROOT.parent/'local_app/build/tax-preparation-local-demo-0.4.0.jar']
 for (name,old,new,group),(runtime_cfg,admin_cfg,key) in zip(VARIANTS,fixtures,strict=True):
  with tempfile.TemporaryDirectory(prefix='concurrent-sql-defect-') as temp:
   folder=Path(temp);shutil.copytree(ROOT/'src',folder/'src');p=folder/'src/main/java/com/taxplatform/startup/ConcurrentMysqlStore.java';source=p.read_text();expected=2 if name=='lost_commit_ack_misreported' else 1;assert source.count(old)==expected,(name,source.count(old));p.write_text(source.replace(old,new));classes=folder/'classes';classes.mkdir()
   compile=subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','--add-modules','jdk.httpserver,java.sql','-encoding','UTF-8','-proc:none','-Xlint:all','-Werror','-cp',str(ROOT/'build/dependency-classes'),'-d',str(classes),*map(str,sorted((folder/'src').rglob('*.java')))],capture_output=True,text=True,timeout=60)
   if compile.returncode:raise RuntimeError('Compile failure is not behavioral detection: '+name+'\n'+compile.stderr)
   execute=subprocess.run(['java','--add-modules','jdk.httpserver,java.sql','-cp',os.pathsep.join(map(str,[classes,folder/'src/main/resources',driver,*libs])),'com.taxplatform.startup.ConcurrentMysqlTests',str(runtime_cfg),str(admin_cfg),str(key),str(folder/'result.json')],env=dict(os.environ,TAX_CONCURRENT_SELECTED_GROUP=group),capture_output=True,text=True,timeout=55)
   if execute.returncode==0 or 'AssertionError' not in execute.stderr:raise RuntimeError('Required targeted behavioral failure missing: '+name+'\n'+execute.stdout[-1800:]+execute.stderr[-2400:])
   results.append({'name':name,'compiled':True,'behavioral_failure':True,'group':group});print('Detected concurrent defect '+name,flush=True)
 assert before=={str(p.relative_to(ROOT)):sha(p) for p in sources}
 report={'concurrent_sql_compiled_defects':len(results),'concurrent_sql_detected_defects':len(results),'source_preserved':True,'actual_mysql':True,'inputs':before,'variants':results,'production_certification':False};(ROOT/'build/concurrent_mysql_mutation_results.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':raise SystemExit('Run through test_concurrent_mysql.py --with-concurrent-mutations; never point these destructive fixtures at an existing database.')
