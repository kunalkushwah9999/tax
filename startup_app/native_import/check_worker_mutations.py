#!/usr/bin/env python3
"""Actual pinned-validator defects checked in separate source copies with targeted assertions."""
from pathlib import Path
import argparse,ast,hashlib,json,shutil,subprocess,sys,tempfile
ROOT=Path(__file__).resolve().parent;p=argparse.ArgumentParser();p.add_argument('--schema-dir',type=Path,required=True);args=p.parse_args()
variants=[
 ('official_schema_errors_ignored','for error in itertools.islice(validator.iter_errors(data),50):','for error in []:','test_21_invalid_unused_tax_field_still_blocks_all_proposals'),
 ('exact_form_version_check_ignored','form.get(k)!=v','False','test_04_pattern_substring_acceptance_does_not_expand_form_identity'),
 ('multiple_category_rows_silently_select_first','if len(entries)==1:','if entries:','test_10_multiple_category_observations_are_never_summed_into_proposal'),
 ('raw_identity_document_echoed','out[\'proposals\']=proposals;','out[\'raw_document\']=data;out[\'proposals\']=proposals;','test_15_identity_bank_contact_and_unexpected_field_names_never_escape'),
 ('schema_integrity_pin_disabled','if len(data)!=148921 or hashlib.sha256(data).hexdigest()!=SCHEMA_SHA:','if False:','test_18_corrupt_schema_pin_fails_before_document_validation'),
 ('tax_arithmetic_marked_as_checked','\'tax_arithmetic_checked\':False','\'tax_arithmetic_checked\':True','test_14_tax_figures_are_neither_trusted_returned_nor_used_as_expectations')]
sha=lambda f:hashlib.sha256(f.read_bytes()).hexdigest();before={str(f.relative_to(ROOT.parent)):sha(f) for f in [*sorted(ROOT.glob('*.py')),ROOT/'source_pins.json',ROOT/'requirements.txt',ROOT.parent/'src/main/resources/native/synthetic_itr1.json']};results=[]
for name,old,new,case in variants:
 with tempfile.TemporaryDirectory(prefix='native-worker-defect-') as temporary:
  folder=Path(temporary);shutil.copytree(ROOT,folder/'native_import',ignore=shutil.ignore_patterns('__pycache__'));resources=folder/'src/main/resources/native';resources.mkdir(parents=True);shutil.copy2(ROOT.parent/'src/main/resources/native/synthetic_itr1.json',resources/'synthetic_itr1.json');(folder/'build').mkdir();worker=folder/'native_import/worker.py';text=worker.read_text();assert text.count(old)==1,(name,text.count(old));worker.write_text(text.replace(old,new));ast.parse(worker.read_text())
  run=subprocess.run([sys.executable,'-B',str(folder/'native_import/test_worker.py'),'--schema-dir',str(args.schema_dir.resolve()),'--case',case],capture_output=True,text=True,timeout=12)
  if run.returncode==0 or 'AssertionError' not in run.stderr:print(run.stdout+run.stderr);raise SystemExit('Expected targeted assertion missing: '+name)
  results.append({'name':name,'syntax_valid':True,'targeted_assertion_failed':True,'test':case});print('Detected '+name,flush=True)
assert before=={str(f.relative_to(ROOT.parent)):sha(f) for f in [*sorted(ROOT.glob('*.py')),ROOT/'source_pins.json',ROOT/'requirements.txt',ROOT.parent/'src/main/resources/native/synthetic_itr1.json']}
report={'native_worker_defects':len(results),'native_worker_detected_defects':len(results),'source_preserved':True,'inputs':before,'schema_sha256':'6a0ef9f64065431879dcc724869d8455a640bcf11279d1818eb19f11448ce763','variants':results,'independent_tax_approval':False};(ROOT.parent/'build/native_worker_mutation_results.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k!='inputs'},indent=2))
