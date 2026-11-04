#!/usr/bin/env python3
"""Reference checker tests use synthetic assertions in memory; they never approve or alter goldens."""
from pathlib import Path
import copy,hashlib,json,tempfile,unittest
import review_workbench as work
ROOT=Path(__file__).resolve().parent
class ReferenceTests(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.corpus_path=ROOT.parent/'golden_cases_v0.json';cls.before=cls.corpus_path.read_bytes()
  cls.source=work.corpus(cls.corpus_path);cls.workings=work.evaluate(cls.source,ROOT/'build/tax-planner-workbench-0.1.0.jar')
  cls.blank=work.template(cls.source)
 def asserted_fixture(self):
  result=copy.deepcopy(self.blank)
  cases={c['case_id']:c for c in self.source['cases']}
  actual={r['case_id']:r for r in self.workings['cases']}
  for row in result['cases']:
   row['review']={'decision':'APPROVED','reviewer':'FIXTURE ONLY','method':'SYNTHETIC SELF-TEST, NOT EXTERNAL REVIEW',
    'evidence_id':'FIXTURE','evidence_sha256':work.digest(b'not actual review evidence'),'reference_version':'TEST_ONLY',
    'reviewed_on':'2026-11-01','independent_of_this_engine':True}
   for regime,item in row['expected'].items():
    found=actual[row['case_id']]['candidate']['regimes'][regime];item['status']=found['status']
    item['stages']=copy.deepcopy(found['workings']['stages']) if cases[row['case_id']]['review_kind']=='ARITHMETIC' else {}
  return result
 def check_pack(self,pack):return work.check_references(self.source,pack,self.workings)
 def test_template_has_no_numerical_expectations_or_approved_reviews(self):
  self.assertEqual(len(self.blank['cases']),30)
  for row in self.blank['cases']:
   self.assertEqual(row['review']['decision'],'PENDING')
   for value in row['expected'].values():self.assertIsNone(value['status']);self.assertTrue(all(v is None for v in value['stages'].values()))
 def test_blank_template_is_not_release_readiness(self):
  r=self.check_pack(self.blank);self.assertEqual(r['matched_review_assertions'],0);self.assertFalse(r['ready_for_human_release_review']);self.assertFalse(r['release_approved'])
 def test_synthetic_matched_assertions_cannot_authenticate_reviewers_or_release(self):
  r=self.check_pack(self.asserted_fixture());self.assertEqual(r['matched_review_assertions'],30)
  self.assertFalse(r['release_approved']);self.assertFalse(r['reviewer_identity_and_independence_authenticated']);self.assertFalse(r['evidence_bytes_independently_validated'])
 def test_changed_input_hash_blocks_that_reference(self):
  p=self.asserted_fixture();p['cases'][0]['input_sha256']='a'*64;self.assertEqual(self.check_pack(p)['matched_review_assertions'],29)
 def test_one_wrong_stage_is_detected(self):
  p=self.asserted_fixture();p['cases'][0]['expected']['NEW']['stages']['slab_tax']='999.00';r=self.check_pack(p)
  self.assertEqual(r['matched_review_assertions'],29);self.assertIn('NEW:slab_tax:STAGE_MISMATCH',r['cases'][0]['issues'])
 def test_numeric_json_values_are_not_decimal_references(self):
  p=self.asserted_fixture();p['cases'][0]['expected']['NEW']['stages']['slab_tax']=0.0;self.assertEqual(self.check_pack(p)['matched_review_assertions'],29)
 def test_missing_or_additional_stage_is_rejected(self):
  for operation in ['remove','add']:
   p=self.asserted_fixture();stages=p['cases'][0]['expected']['NEW']['stages']
   if operation=='remove':stages.pop('cess')
   else:stages['invented_tax']='0.00'
   self.assertEqual(self.check_pack(p)['matched_review_assertions'],29)
 def test_wrong_workflow_status_does_not_pass_on_zero_tax(self):
  p=self.asserted_fixture();p['cases'][24]['expected']['NEW']['status']='CALCULATED';self.assertEqual(self.check_pack(p)['matched_review_assertions'],29)
 def test_duplicate_missing_or_extra_case_ids_rejected(self):
  for mode in ['missing','duplicate','extra']:
   p=self.asserted_fixture()
   if mode=='missing':p['cases'].pop()
   elif mode=='duplicate':p['cases'][-1]['case_id']='G01'
   else:p['cases'].append(copy.deepcopy(p['cases'][0]))
   with self.assertRaises(ValueError):self.check_pack(p)
 def test_missing_independence_and_evidence_metadata_never_pass(self):
  for key,value in [('independent_of_this_engine',False),('evidence_sha256','0'*64),('reviewer',''),('reviewed_on','2026-02-30'),('method',None)]:
   p=self.asserted_fixture();p['cases'][0]['review'][key]=value;self.assertEqual(self.check_pack(p)['matched_review_assertions'],29)
 def test_control_cases_do_not_sneak_in_numerical_reference_fields(self):
  p=self.asserted_fixture();p['cases'][24]['expected']['NEW']['stages']={'slab_tax':'0.00'};self.assertEqual(self.check_pack(p)['matched_review_assertions'],29)
 def test_wrong_stage_contract_is_rejected(self):
  p=self.asserted_fixture();p['stage_contract']='anything-goes'
  with self.assertRaises(ValueError):self.check_pack(p)
 def test_stale_or_rebound_workings_are_rejected(self):
  for mode in ['corpus','input']:
   changed=copy.deepcopy(self.workings)
   if mode=='corpus':changed['corpus_sha256']='a'*64
   else:changed['cases'][0]['input_sha256']='a'*64
   with self.assertRaises(ValueError):work.check_references(self.source,self.asserted_fixture(),changed)
 def test_strict_json_rejects_duplicate_fields_and_nonfinite_numbers(self):
  for text in ['{"cases":[],"cases":[]}','{"tax":NaN}']:
   with tempfile.TemporaryDirectory() as folder:
    p=Path(folder)/'data.json';p.write_text(text)
    with self.assertRaises(ValueError):work.read(p)
 def test_extra_authority_claims_do_not_publish_rules(self):
  p=self.asserted_fixture();p['release_approved']=True;p['paid_plan']='CA_PREMIUM';self.assertFalse(self.check_pack(p)['release_approved'])
 def test_original_golden_expectations_unchanged_and_unapproved(self):
  self.assertEqual(self.corpus_path.read_bytes(),self.before);self.assertEqual(self.source['approved_case_count'],0)
  self.assertTrue(all(all(v is None for v in c['expected'].values()) for c in self.source['cases']))
if __name__=='__main__':
 result=unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(ReferenceTests))
 report={'reference_checker_groups':result.testsRun,'failures':len(result.failures),'errors':len(result.errors),
  'synthetic_assertions_only':True,'independent_approved_outputs':0,'release_approved':False,
  'engine_jar_sha256':hashlib.sha256((ROOT/'build/tax-planner-workbench-0.1.0.jar').read_bytes()).hexdigest(),
  'golden_corpus_sha256':hashlib.sha256((ROOT.parent/'golden_cases_v0.json').read_bytes()).hexdigest()}
 (ROOT/'build/reference_test_results.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
 raise SystemExit(0 if result.wasSuccessful() else 1)
