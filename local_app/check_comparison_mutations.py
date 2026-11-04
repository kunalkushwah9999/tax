#!/usr/bin/env python3
"""Selected source-copy defect experiments for local normalized statement comparison."""
from pathlib import Path
import hashlib,json,os,shutil,subprocess,tempfile
ROOT=Path(__file__).resolve().parent
CORE=ROOT.parent/'java_result_safety/build/tax-result-safety-0.1.4.jar';ENGINE=ROOT.parent/'tax_engine/build/tax-planner-workbench-0.1.0.jar'
C='src/main/java/com/taxplatform/localdemo/StatementComparison.java';W='src/main/java/com/taxplatform/localdemo/ComparisonWorkspace.java'
variants=[
 ('unknown_amount_becomes_zero',C,'value.equals("UNKNOWN")?Optional.empty():Optional.of(Money.parse(value))','value.equals("UNKNOWN")?Optional.of(Money.parse("0")):Optional.of(Money.parse(value))'),
 ('ignore_income_category_when_grouping',C,'row.category().name(),row.payer(),row.period(),row.coverage().name(),row.reference()','row.payer(),row.period(),row.coverage().name(),row.reference()'),
 ('ignore_stale_review_token',W,'capacity();fresh(current);if(!Objects.equals(version,expectedVersion))throw new Rejected("STALE_RECONCILIATION");','capacity();fresh(current);if(false)throw new Rejected("STALE_RECONCILIATION");'),
 ('ignore_selected_observation_id',W,'.filter(o->o.id().equals(observation))','.filter(o->true)')]
sources=sorted((ROOT/'src').rglob('*.java'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();before={str(p.relative_to(ROOT)):sha(p) for p in sources};results=[]
for name,relative,old,new in variants:
 with tempfile.TemporaryDirectory(prefix='statement-comparison-defect-') as folder:
  copy=Path(folder);shutil.copytree(ROOT/'src',copy/'src');p=copy/relative;s=p.read_text();assert s.count(old)==1,name;p.write_text(s.replace(old,new))
  classes=copy/'classes';classes.mkdir();classpath=os.pathsep.join([str(CORE),str(ENGINE)])
  args=['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','--add-modules','jdk.httpserver','-encoding','UTF-8','-proc:none','-Xlint:all','-Werror','-cp',classpath,'-d',str(classes),*[str(p) for p in sorted((copy/'src').rglob('*.java'))]]
  built=subprocess.run(args,capture_output=True,text=True,timeout=45)
  if built.returncode:print(built.stdout+built.stderr);raise SystemExit('Variant did not compile; not counted: '+name)
  tested=subprocess.run(['java','--add-modules','jdk.httpserver','-cp',os.pathsep.join([str(classes),classpath]),'com.taxplatform.localdemo.ComparisonTests'],capture_output=True,text=True,timeout=45)
  if tested.returncode==0 or not any(s in tested.stderr for s in ['AssertionError','Exception']):raise SystemExit('No behavioral failure: '+name)
  results.append({'name':name,'compiled':True,'behavioral_failure':True});print('Detected '+name,flush=True)
assert before=={str(p.relative_to(ROOT)):sha(p) for p in sources}
report={'comparison_variants_compiled':len(results),'comparison_variants_detected':len(results),'source_preserved':True,'variants':results,'production_release':False}
(ROOT/'build/comparison_mutation_results.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
