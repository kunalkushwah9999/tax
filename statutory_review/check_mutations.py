#!/usr/bin/env python3
"""Inject known engineering defects into private copies; does not approve tax law."""
from pathlib import Path
import hashlib,json,subprocess,sys,tempfile
ROOT=Path(__file__).resolve().parent
original=(ROOT/'equity.py').read_text();test=(ROOT/'test_equity.py').read_bytes()
variants=[
 ('st_rate_15_percent',"component(st_tax_base,'.20')","component(st_tax_base,'.15')"),
 ('lt_rate_10_percent',"component(taxable_lt,'.125')","component(taxable_lt,'.10')"),
 ('double_annual_band','lt_after-12500000','lt_after-25000000'),
 ('skip_st_loss_cross_setoff','offset=min(st_loss,lt_positive)','offset=0'),
 ('lt_loss_reduces_st','st_tax_base=max(short,0)','st_tax_base=max(short+min(long,0),0)'),
 ('grandfather_lowers_actual_cost','max(cost,min(fmv,proceeds))','min(cost,min(fmv,proceeds))'),
 ('ignore_holding_anniversary','if sold==anniversary:issues.append','if False:issues.append'),
 ('unknown_amount_becomes_zero',"if value=='UNKNOWN':return None","if value=='UNKNOWN':return 0"),
 ('skip_pan_alias_rejection',"and not re.fullmatch(r'[A-Z]{5}[0-9]{4}[A-Z]',value)",' '),
 ('grant_filing_authority',"'public_result_release_authorized':False,'filing_ready':False","'public_result_release_authorized':False,'filing_ready':True")]
results=[]
for name,old,new in variants:
 if original.count(old)!=1:raise SystemExit('Mutation target drift: '+name)
 with tempfile.TemporaryDirectory(prefix='equity-defect-') as temp:
  temp=Path(temp);(temp/'equity.py').write_text(original.replace(old,new));(temp/'test_equity.py').write_bytes(test)
  run=subprocess.run([sys.executable,str(temp/'test_equity.py')],capture_output=True,text=True,timeout=20)
  report=json.loads((temp/'engineering_test_results.json').read_text());detected=run.returncode!=0 and report['equity_engineering_test_groups']==20 and report['failures']>0
  results.append({'defect':name,'detected':detected})
assert (ROOT/'equity.py').read_text()==original and (ROOT/'test_equity.py').read_bytes()==test
report={'injected_defects':len(results),'detected_defects':sum(r['detected'] for r in results),'variants':results,'original_sources_preserved':True,'tax_law_approved':False,'independent_approved_cases':0,'sources':{name:hashlib.sha256((ROOT/name).read_bytes()).hexdigest() for name in ['equity.py','test_equity.py','check_mutations.py']}}
(ROOT/'mutation_results.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2));sys.exit(0 if all(r['detected'] for r in results) else 1)
