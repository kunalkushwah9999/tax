#!/usr/bin/env python3
"""Compile selected deliberately broken source copies and require behavioral test failure."""
from pathlib import Path
import hashlib,json,os,shutil,subprocess,tempfile
ROOT=Path(__file__).resolve().parent
CORE=ROOT.parent/'java_result_safety/build/tax-result-safety-0.1.4.jar'
P='src/main/java/com/taxplatform/planner/PrimaryEngine.java'
D='src/main/java/com/taxplatform/planner/DualPlanner.java'
MUTATIONS=[
 ('remove_salary_cap',P,'BigDecimal standard=salary.min(new BigDecimal(regime==Regime.NEW?"75000":"50000"));','BigDecimal standard=new BigDecimal(regime==Regime.NEW?"75000":"50000");'),
 ('deduct_standard_per_employer',P,'BigDecimal standard=salary.min(new BigDecimal(regime==Regime.NEW?"75000":"50000"));','BigDecimal standard=salary.min(new BigDecimal(regime==Regime.NEW?"75000":"50000")).multiply(new BigDecimal(input.salaries().confirmedValue().orElseThrow().size()));'),
 ('include_fd_in_80tta',P,'tta=input.savings().confirmedValue().orElseThrow().value().min(new BigDecimal("10000"));','tta=input.savings().confirmedValue().orElseThrow().value().add(input.deposits().confirmedValue().orElseThrow().value()).min(new BigDecimal("10000"));'),
 ('omit_new_marginal_relief',P,'else relief=slab.subtract(total.subtract(new BigDecimal("1200000"))).max(ZERO);','else relief=ZERO;'),
 ('invent_old_marginal_relief',P,'else if(total.compareTo(new BigDecimal("500000"))<=0)rebate=slab.min(new BigDecimal("12500"));','else if(total.compareTo(new BigDecimal("500000"))<=0)rebate=slab.min(new BigDecimal("12500")); else relief=slab.subtract(total.subtract(new BigDecimal("500000"))).max(ZERO);'),
 ('round_paise_instead_of_ignoring',P,'amount.setScale(0,RoundingMode.DOWN)','amount.setScale(0,RoundingMode.HALF_UP)'),
 ('use_review_date_age',P,'LocalDate end=LocalDate.of(2026,3,31);','LocalDate end=LocalDate.of(2026,11,1);'),
 ('compare_final_tax_only',D,'for(TaxStages.Stage stage:TaxStages.Stage.values())if(first.value(stage).compareTo(second.value(stage))!=0)mismatches.add(stage.name());','if(first.value(TaxStages.Stage.ANNUAL_TAX_BEFORE_CREDITS).compareTo(second.value(TaxStages.Stage.ANNUAL_TAX_BEFORE_CREDITS))!=0)mismatches.add("ANNUAL_TAX_BEFORE_CREDITS");')]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
sources=sorted((ROOT/'src').rglob('*.java'));before={str(p.relative_to(ROOT)):sha(p) for p in sources};rows=[]
for name,relative,old,new in MUTATIONS:
 with tempfile.TemporaryDirectory(prefix='tax-engine-mutation-') as folder:
  copy=Path(folder);shutil.copytree(ROOT/'src',copy/'src');path=copy/relative;text=path.read_text();assert text.count(old)==1,name;path.write_text(text.replace(old,new))
  classes=copy/'classes';classes.mkdir();flags=['-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-proc:none','-Xlint:all','-Werror','-cp',str(CORE),'-d',str(classes)]
  compiled=subprocess.run(['java',*flags,*[str(p) for p in sorted((copy/'src').rglob('*.java'))]],capture_output=True,text=True,timeout=45)
  if compiled.returncode:raise SystemExit('Mutation did not compile; not counted: '+name)
  tested=subprocess.run(['java','-cp',os.pathsep.join([str(classes),str(CORE)]),'com.taxplatform.planner.EngineTests'],capture_output=True,text=True,timeout=45)
  if tested.returncode==0:raise SystemExit('Mutation survived: '+name)
  if 'AssertionError' not in tested.stderr and 'Exception' not in tested.stderr:raise SystemExit('No behavioral failure evidence: '+name)
  rows.append({'mutation':name,'compiled':True,'behavioral_failure':True});print('Detected '+name,flush=True)
assert before=={str(p.relative_to(ROOT)):sha(p) for p in sources}
report={'compiled_variants':len(rows),'behaviorally_detected':len(rows),'source_preserved':True,'variants':rows,
 'meaning':'selected engineering defects only, not tax-law approval or exhaustive mutation coverage'}
(ROOT/'build/mutation_results.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
