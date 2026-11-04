#!/usr/bin/env python3
"""Real pinned-schema worker checks. Synthetic values are not approved tax references."""
from pathlib import Path
import argparse,copy,hashlib,importlib.util,json,socket,subprocess,sys,tempfile,unittest
ROOT=Path(__file__).resolve().parent;P=argparse.ArgumentParser();P.add_argument('--schema-dir',type=Path,required=True);P.add_argument('--case',action='append');args=P.parse_args()
spec=importlib.util.spec_from_file_location('native_worker',ROOT/'worker.py');w=importlib.util.module_from_spec(spec);spec.loader.exec_module(w)
SCHEMA,VALIDATOR=w.configuration(args.schema_dir);SAMPLE=json.loads((ROOT.parent/'src/main/resources/native/synthetic_itr1.json').read_text());GENERATED=0
def check(data):return w.validate(json.dumps(data,separators=(',',':')).encode(),SCHEMA,VALIDATOR)
def itr(data):return data['ITR']['ITR1']
def income(data):return itr(data)['ITR1_IncomeDeductions']
class Tests(unittest.TestCase):
 def test_01_pinned_official_schema_validates_structural_fixture(self):
  d=check(SAMPLE);self.assertEqual(d['schema_validation'],'PASS');self.assertEqual(d['document_format'],'MATCH');self.assertEqual({x['field'] for x in d['proposals']},{'SALARY','SAVINGS_INTEREST','DEPOSIT_INTEREST'});self.assertEqual(d['schema_sha256'],w.SCHEMA_SHA)
 def test_02_official_root_gap_does_not_create_proposals(self):
  d=check({});self.assertEqual(d['schema_validation'],'PASS');self.assertEqual(d['document_format'],'UNSUPPORTED');self.assertEqual(d['proposals'],[])
 def test_03_schema_missing_required_field_is_visible_without_values(self):
  d=copy.deepcopy(SAMPLE);del income(d)['GrossSalary'];r=check(d);self.assertEqual(r['schema_validation'],'FAIL');self.assertIn('/ITR/ITR1/ITR1_IncomeDeductions/GrossSalary',[e['path'] for e in r['issues']]);self.assertFalse(r['proposals'])
 def test_04_pattern_substring_acceptance_does_not_expand_form_identity(self):
  d=copy.deepcopy(SAMPLE);itr(d)['Form_ITR1']['FormName']='PREFIX-ITR-1-SUFFIX';r=check(d);self.assertEqual(r['schema_validation'],'PASS');self.assertEqual(r['document_format'],'UNSUPPORTED');self.assertFalse(r['proposals'])
 def test_05_wrong_year_and_unknown_fields_reject(self):
  d=copy.deepcopy(SAMPLE);itr(d)['Form_ITR1']['AssessmentYear']='2027';self.assertEqual(check(d)['schema_validation'],'FAIL');d=copy.deepcopy(SAMPLE);income(d)['filing_available']=True;self.assertEqual(check(d)['schema_validation'],'FAIL')
 def test_06_duplicate_keys_invalid_utf8_surrogates_and_nonfinite_reject(self):
  for raw in [b'{"ITR":{},"ITR":{}}',b'\xff',b'{"x":"\\ud800"}',b'{"x":NaN}',b'{"x":Infinity}']:
   with self.assertRaises(w.Rejected):w.validate(raw,SCHEMA,VALIDATOR)
 def test_07_native_bounds_limit_strings_numbers_depth_and_rows(self):
  for d in [{'x':'a'*4097},{'x':10**25},{'x':[0]*501}]:
   with self.assertRaises(w.Rejected):check(d)
  d={};x=d
  for _ in range(30):x['x']={};x=x['x']
  with self.assertRaises(w.Rejected):check(d)
 def test_08_crlf_tabs_and_reordered_keys_are_valid_json_not_an_api_extension(self):
  raw=json.dumps(SAMPLE,indent='\t',sort_keys=True).replace('\n','\r\n').encode();self.assertEqual(w.validate(raw,SCHEMA,VALIDATOR)['schema_validation'],'PASS')
 def test_09_explicit_zero_is_offered_but_absence_is_not_inferred_as_zero(self):
  d=copy.deepcopy(SAMPLE);income(d)['GrossSalary']=0;r=check(d);self.assertEqual(next(x['amount'] for x in r['proposals'] if x['field']=='SALARY'),'0.00');del income(d)['OthersInc'];income(d)['IncomeOthSrc']=0;r=check(d);self.assertEqual([x['field'] for x in r['proposals']],['SALARY'])
 def test_10_multiple_category_observations_are_never_summed_into_proposal(self):
  d=copy.deepcopy(SAMPLE);rows=income(d)['OthersInc']['OthersIncDtlsOthSrc'];rows.append({'OthSrcNatureDesc':'SAV','OthSrcOthAmount':5000});income(d)['IncomeOthSrc']=17000;r=check(d);self.assertNotIn('SAVINGS_INTEREST',[p['field'] for p in r['proposals']]);self.assertIn('MULTIPLE_SAV_OBSERVATIONS_NOT_AGGREGATED',r['warnings'])
 def test_11_inconsistent_source_total_blocks_interest_transfer(self):
  d=copy.deepcopy(SAMPLE);income(d)['IncomeOthSrc']=12001;r=check(d);self.assertEqual([x['field'] for x in r['proposals']],['SALARY']);self.assertIn('INTEREST_BREAKUP_TOTAL_REVIEW_REQUIRED',r['warnings'])
 def test_12_dividends_other_income_and_family_pension_are_outside_pack(self):
  for kind in ['DIV','OTH','FAP','10(11)(iP)']:
   d=copy.deepcopy(SAMPLE);income(d)['OthersInc']['OthersIncDtlsOthSrc'][0]['OthSrcNatureDesc']=kind;r=check(d);self.assertEqual(r['proposal_pack'],'EXPANDED_WORKFLOW_REQUIRED');self.assertFalse(r['proposals'])
 def test_13_property_presence_does_not_pass_as_salary_only(self):
  d=copy.deepcopy(SAMPLE);income(d)['TotalIncomeChargeableUnHP']=1;r=check(d);self.assertEqual(r['proposal_pack'],'EXPANDED_WORKFLOW_REQUIRED');self.assertFalse(r['proposals'])
 def test_14_tax_figures_are_neither_trusted_returned_nor_used_as_expectations(self):
  d=copy.deepcopy(SAMPLE);itr(d)['ITR1_TaxComputation']['TotalTaxPayable']=987654321;r=check(d);self.assertEqual(r['schema_validation'],'PASS');self.assertNotIn('987654321',json.dumps(r));self.assertFalse(r['tax_arithmetic_checked']);self.assertFalse(r['business_rules_validated']);self.assertFalse(r['filing_available'])
 def test_15_identity_bank_contact_and_unexpected_field_names_never_escape(self):
  secrets=['ZZZZZ0000Z','TESTACCOUNT1','TEST0000000','synthetic@example.invalid','SYNTHETIC ONLY BANK'];r=check(SAMPLE)
  for secret in secrets:self.assertNotIn(secret,json.dumps(r))
  d=copy.deepcopy(SAMPLE);d['ZZZZZ0000Z']={'secret':'TESTACCOUNT1'};r=check(d)
  for secret in secrets:self.assertNotIn(secret,json.dumps(r))
 def test_16_schema_data_urls_are_not_resolved_or_executed(self):
  d=copy.deepcopy(SAMPLE);d['$ref']='http://127.0.0.1:9/should-never-fetch';self.assertEqual(check(d)['schema_validation'],'FAIL')
 def test_17_validation_runs_with_socket_creation_prohibited(self):
  original=socket.socket
  def blocked(*_a,**_k):raise AssertionError('network attempted')
  socket.socket=blocked
  try:self.assertEqual(check(SAMPLE)['schema_validation'],'PASS')
  finally:socket.socket=original
 def test_18_corrupt_schema_pin_fails_before_document_validation(self):
  with tempfile.TemporaryDirectory(prefix='native-pin-negative-') as temporary:
   folder=Path(temporary);(folder/'itr1_schema.json').write_text('{}')
   with self.assertRaisesRegex(w.Rejected,'NATIVE_SCHEMA_PIN_MISMATCH'):w.configuration(folder)
 def test_19_real_worker_process_has_only_redacted_structural_output(self):
  p=subprocess.run([sys.executable,'-B',str(ROOT/'worker.py'),'--schema-dir',str(args.schema_dir)],input=json.dumps(SAMPLE).encode(),capture_output=True,timeout=6,check=True);r=json.loads(p.stdout);self.assertEqual(r['schema_validation'],'PASS');self.assertEqual(p.stderr,b'');self.assertNotIn(b'ZZZZZ0000Z',p.stdout);self.assertFalse(r['raw_document_retained'])
 def test_20_generated_precision_type_and_boundary_rejections(self):
  global GENERATED
  for key in ['GrossSalary','IncomeOthSrc','TotalIncome','DeductionUs16']:
   for value in [-1,True,'800000',1.5,None,[],{}]:
    d=copy.deepcopy(SAMPLE);income(d)[key]=value;r=check(d);self.assertEqual(r['schema_validation'],'FAIL');self.assertFalse(r['proposals']);GENERATED+=1
 def test_21_invalid_unused_tax_field_still_blocks_all_proposals(self):
  d=copy.deepcopy(SAMPLE);itr(d)['ITR1_TaxComputation']['TotalTaxPayable']=-1;r=check(d);self.assertEqual(r['schema_validation'],'FAIL');self.assertFalse(r['proposals'])
if __name__=='__main__':
 suite=unittest.TestSuite(Tests(name) for name in args.case) if args.case else unittest.defaultTestLoader.loadTestsFromTestCase(Tests);result=unittest.TextTestRunner(verbosity=2).run(suite);report={'native_worker_groups':result.testsRun,'generated_rejections':GENERATED,'failures':len(result.failures),'errors':len(result.errors),'schema_sha256':w.SCHEMA_SHA,'worker_sha256':hashlib.sha256((ROOT/'worker.py').read_bytes()).hexdigest(),'actual_jsonschema_library':'4.26.0','independent_tax_approval':False,'gst_native_released':False,'inputs':{str(p.relative_to(ROOT.parent)):hashlib.sha256(p.read_bytes()).hexdigest() for p in [ROOT/'worker.py',ROOT/'test_worker.py',ROOT/'source_pins.json',ROOT/'requirements.txt',ROOT.parent/'src/main/resources/native/synthetic_itr1.json']}};(ROOT.parent/'build/native_worker_test_results.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2));raise SystemExit(0 if result.wasSuccessful() else 1)
