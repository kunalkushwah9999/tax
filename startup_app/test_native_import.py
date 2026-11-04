#!/usr/bin/env python3
"""Actual authenticated Java -> bounded Python -> pinned official-schema integration."""
from pathlib import Path
import argparse,concurrent.futures,copy,hashlib,importlib.util,json,subprocess,sys,tempfile,unittest,uuid
ROOT=Path(__file__).resolve().parent;H=None
SAMPLE=json.loads((ROOT/'src/main/resources/native/synthetic_itr1.json').read_text())
class Tests(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.a=H.Client();cls.a.register('native_import_a');cls.b=H.Client();cls.b.register('native_import_b');cls.admin=H.Client().login('ops_admin');cls.preparer=H.Client().login('team_staff');cls.accepted=None
 def setUp(self):self.tokens=[]
 def tearDown(self):
  for case,token in self.tokens:
   try:row=self.a.case(case);self.a.call('/api/cases/'+case+'/itr-native-discard','POST',{'expected_revision':row['revision'],'preview_id':token})
   except (AssertionError,KeyError):pass
 def preview(self,row,data=None,raw=None):
  p={'expected_revision':row['revision'],'synthetic_confirmed':True,'json_text':raw if raw is not None else json.dumps(SAMPLE if data is None else data)};r,_=self.a.call('/api/cases/'+row['id']+'/itr-native-preview','POST',p)
  if r['preview_id']:self.tokens.append((row['id'],r['preview_id']))
  return r
 def command(self,row,preview):return {'request_id':str(uuid.uuid4()),'expected_revision':row['revision'],'preview_id':preview['preview_id'],'same_taxpayer_confirmed':True,'selections':[{'field':p['field'],'amount':p['amount'],'whole_category_confirmed':True,'replace_input_confirmed':True} for p in preview['proposals']]}
 def test_01_real_worker_config_and_pinned_schema_are_visible_without_authority(self):
  d,_=self.a.call('/api/native-capabilities');self.assertTrue(d['native_itr_configured']);self.assertFalse(d['native_gst_available']);self.assertFalse(d['filing_available']);self.assertFalse(d['business_rules_validated'])
 def test_02_preview_changes_no_saved_state_and_returns_only_allowed_income(self):
  row=self.a.create();r=self.preview(row);self.assertEqual(r['schema_validation'],'PASS');self.assertEqual(r['document_format'],'MATCH');self.assertEqual(self.a.case(row['id']),row);self.assertEqual([x['amount'] for x in r['proposals']],['800000.00','5000.00','7000.00']);self.assertFalse(r['taxpayer_identity_verified']);self.assertFalse(r['filing_available'])
 def test_03_pii_bank_contact_and_supplied_tax_never_cross_preview_or_export(self):
  row=self.a.create();data=copy.deepcopy(SAMPLE);data['ITR']['ITR1']['ITR1_TaxComputation']['TotalTaxPayable']=987654321;r=self.preview(row,data);self.assertEqual(r['schema_validation'],'PASS');combined=json.dumps(r)+json.dumps(self.a.call('/api/cases/'+row['id']+'/export')[0])
  for value in ['ZZZZZ0000Z','TESTACCOUNT1','TEST0000000','synthetic@example.invalid','987654321']:self.assertNotIn(value,combined)
  self.assertFalse(r['tax_arithmetic_checked'])
 def test_04_other_accounts_admin_and_granted_preparer_cannot_run_private_preview(self):
  row=self.a.create();base='/api/cases/'+row['id'];self.a.call(base+'/grants','POST',{'request_id':str(uuid.uuid4()),'expected_revision':row['revision'],'preparer_username':'team_staff','expires_days':1,'consent':True});row=self.a.case(row['id']);p={'expected_revision':row['revision'],'synthetic_confirmed':True,'json_text':json.dumps(SAMPLE)}
  for c in [self.b,self.admin,self.preparer]:c.call(base+'/itr-native-preview','POST',p,status=404)
  r=self.preview(row);other=self.b.create();cmd=self.command(other,r);self.b.call('/api/cases/'+other['id']+'/itr-native-propose','POST',cmd,status=409)
 def test_05_multi_field_proposal_is_estimated_and_needs_separate_confirmation(self):
  row=self.a.create();r=self.preview(row);p=self.command(row,r);base='/api/cases/'+row['id'];self.a.call(base+'/itr-native-propose','POST',p);after=self.a.case(row['id']);self.assertEqual(after['revision'],row['revision']+1);self.assertTrue(all(after['inputs'][x]['state']=='ESTIMATED' for x in ['SALARY','SAVINGS_INTEREST','DEPOSIT_INTEREST']));self.assertEqual(after['inputs']['OTHER_INTEREST'],{'state':'UNKNOWN'});self.assertFalse(any(after['reviews'].values()));self.assertFalse(after['tax_values_released']);self.assertEqual(len(after['native_import_links']),3);after=self.a.edit(after,'CONFIRM',field='SALARY',amount='800000.00');self.assertEqual(after['inputs']['SALARY']['state'],'CONFIRMED')
 def test_06_one_changed_selection_rejects_all_and_rejected_id_is_not_reserved(self):
  row=self.a.create();r=self.preview(row);p=self.command(row,r);p['selections'][1]['amount']='5001.00';base='/api/cases/'+row['id'];self.a.call(base+'/itr-native-propose','POST',p,status=409);self.assertEqual(self.a.case(row['id']),row);p['selections'][1]['amount']='5000.00';self.assertEqual(self.a.call(base+'/itr-native-propose','POST',p)[0]['status'],'APPLIED')
 def test_07_coverage_replacement_and_same_taxpayer_declarations_are_separate(self):
  row=self.a.create();r=self.preview(row);base='/api/cases/'+row['id'];p=self.command(row,r);p['same_taxpayer_confirmed']=False;self.a.call(base+'/itr-native-propose','POST',p,status=409)
  for flag in ['whole_category_confirmed','replace_input_confirmed']:
   p=self.command(row,r);p['selections'][0][flag]=False;self.a.call(base+'/itr-native-propose','POST',p,status=409)
  self.assertEqual(self.a.case(row['id']),row)
 def test_08_stale_preview_does_not_accept_refreshed_case_revision(self):
  row=self.a.create();r=self.preview(row);after=self.a.edit(row,'RENAME',title='Changed after preview');p=self.command(after,r);self.assertEqual(self.a.call('/api/cases/'+row['id']+'/itr-native-propose','POST',p,status=409)[0]['status'],'STALE_NATIVE_PREVIEW');self.assertEqual(self.a.case(row['id']),after)
 def test_09_exact_replay_after_later_edit_needs_no_preview_and_preserves_state(self):
  row=self.a.create();r=self.preview(row);p=self.command(row,r);base='/api/cases/'+row['id'];self.a.call(base+'/itr-native-propose','POST',p);after=self.a.edit(self.a.case(row['id']),'PROPOSE',field='SALARY',text='4 lakh',unit='');self.assertEqual(self.a.call(base+'/itr-native-propose','POST',p)[0]['status'],'REPLAYED');self.assertEqual(self.a.case(row['id']),after);type(self).accepted=(row['id'],copy.deepcopy(p),after)
 def test_10_form_year_unknown_field_duplicate_json_and_empty_root_fail_closed(self):
  row=self.a.create()
  for raw in ['{}','{"ITR":{},"ITR":{}}','{"$ref":"http://127.0.0.1:9/no-request"}']:
   r=self.preview(row,raw=raw);self.assertIsNone(r['preview_id']);self.assertFalse(r['proposals']);self.assertFalse(r['filing_available'])
  data=copy.deepcopy(SAMPLE);data['ITR']['ITR1']['Form_ITR1']['AssessmentYear']='2027';r=self.preview(row,data);self.assertEqual(r['schema_validation'],'FAIL');self.assertFalse(r['proposals'])
 def test_11_multiple_observations_never_sum_or_create_an_absent_category_zero(self):
  row=self.a.create();d=copy.deepcopy(SAMPLE);inc=d['ITR']['ITR1']['ITR1_IncomeDeductions'];inc['OthersInc']['OthersIncDtlsOthSrc'].append({'OthSrcNatureDesc':'SAV','OthSrcOthAmount':2000});inc['IncomeOthSrc']=14000;r=self.preview(row,d);fields=[p['field'] for p in r['proposals']];self.assertNotIn('SAVINGS_INTEREST',fields);self.assertNotIn('OTHER_INTEREST',fields)
 def test_12_csrf_mass_assignment_and_tax_authority_injection_are_rejected(self):
  row=self.a.create();base='/api/cases/'+row['id'];data={'expected_revision':row['revision'],'synthetic_confirmed':True,'json_text':json.dumps(SAMPLE)};self.a.call(base+'/itr-native-preview','POST',data,status=403,headers={'X-CSRF-Token':'wrong'});data['schema_url']='http://127.0.0.1:9/';self.a.call(base+'/itr-native-preview','POST',data,status=400);r=self.preview(row);p=self.command(row,r);p['filing_available']=True;self.a.call(base+'/itr-native-propose','POST',p,status=400);self.assertEqual(self.a.case(row['id']),row)
 def test_13_discarded_preview_cannot_be_applied_and_does_not_mutate_case(self):
  row=self.a.create();r=self.preview(row);base='/api/cases/'+row['id'];self.a.call(base+'/itr-native-discard','POST',{'expected_revision':row['revision'],'preview_id':r['preview_id']});self.a.call(base+'/itr-native-propose','POST',self.command(row,r),status=409);self.assertEqual(self.a.case(row['id']),row)
 def test_14_windows_whitespace_and_small_bounded_file_work_through_real_worker(self):
  row=self.a.create();r=self.preview(row,raw=json.dumps(SAMPLE,indent='\t',sort_keys=True).replace('\n','\r\n'));self.assertEqual(r['schema_validation'],'PASS');self.assertTrue(r['proposals'])
 def test_15_distinct_sessions_concurrent_same_request_commit_once(self):
  row=self.a.create();r=self.preview(row);p=self.command(row,r);one=H.Client().login('native_import_a');two=H.Client().login('native_import_a');base='/api/cases/'+row['id'];
  with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:out=list(pool.map(lambda c:c.call(base+'/itr-native-propose','POST',p)[0]['status'],[one,two]))
  self.assertEqual(sorted(out),['APPLIED','REPLAYED']);self.assertEqual(self.a.case(row['id'])['revision'],row['revision']+1)
 def test_16_active_preparer_does_not_inherit_native_lineage_after_income_transfer(self):
  row=self.a.create();r=self.preview(row);base='/api/cases/'+row['id'];self.a.call(base+'/itr-native-propose','POST',self.command(row,r));row=self.a.case(row['id']);self.a.call(base+'/grants','POST',{'request_id':str(uuid.uuid4()),'expected_revision':row['revision'],'preparer_username':'team_staff','expires_days':1,'consent':True});shared=self.preparer.case(row['id']);self.assertNotIn('native_import_links',shared);self.assertEqual(shared['inputs']['SALARY']['amount'],'800000.00')
 def test_17_no_raw_source_or_identity_is_persisted_in_owner_export_or_local_disk(self):
  row=self.a.create();r=self.preview(row);base='/api/cases/'+row['id'];self.a.call(base+'/itr-native-propose','POST',self.command(row,r));out=json.dumps(self.a.call(base+'/export')[0]);
  for value in ['ZZZZZ0000Z','TESTACCOUNT1','synthetic@example.invalid','ITR1_TaxComputation','json_text']:self.assertNotIn(value,out)
  if not H.PROFILE_ARGS:
   raw=(H.DATA/'records/workspace.vault').read_bytes()
   for value in [b'ZZZZZ0000Z',b'TESTACCOUNT1',b'800000.00']:self.assertNotIn(value,raw)
 def test_18_native_body_and_document_bounds_do_not_create_saved_state(self):
  row=self.a.create();base='/api/cases/'+row['id'];self.a.call(base+'/itr-native-preview','POST',{'expected_revision':row['revision'],'synthetic_confirmed':True,'json_text':'x'*196609},status=400);self.a.call(base+'/itr-native-preview','POST',b' '*400001,headers={'Content-Type':'application/json'},status=413);self.assertEqual(self.a.case(row['id']),row)
 def test_19_explicit_synthetic_acknowledgement_is_required_before_worker_use(self):
  row=self.a.create();self.a.call('/api/cases/'+row['id']+'/itr-native-preview','POST',{'expected_revision':row['revision'],'synthetic_confirmed':False,'json_text':json.dumps(SAMPLE)},status=400);self.assertEqual(self.a.case(row['id']),row)
 def test_zz_restart_drops_preview_cache_but_replays_already_committed_action(self):
  saved,p,expected=type(self).accepted;pending=self.a.create();preview=self.preview(pending);H.stop();H.start();self.a.call('/api/cases',status=401);type(self).a=H.Client().login('native_import_a');self.assertEqual(self.a.call('/api/cases/'+saved+'/itr-native-propose','POST',p)[0]['status'],'REPLAYED');self.assertEqual(self.a.case(saved),expected);self.a.call('/api/cases/'+pending['id']+'/itr-native-propose','POST',self.command(pending,preview),status=409)
def run_existing(http):
 global H
 H=http;result=unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(Tests));report={'native_http_groups':result.testsRun,'failures':len(result.failures),'errors':len(result.errors),'actual_java_python_worker':True,'mysql_profile_exercised':bool(H.PROFILE_ARGS),'jar_sha256':hashlib.sha256((ROOT/'build/tax-platform-startup-5.1.0.jar').read_bytes()).hexdigest(),'worker_sha256':hashlib.sha256((ROOT/'native_import/worker.py').read_bytes()).hexdigest(),'schema_sha256':'6a0ef9f64065431879dcc724869d8455a640bcf11279d1818eb19f11448ce763','browser_ui_tested':False,'independent_tax_approval':False};(ROOT/'build'/('native_mysql_http_test_results.json' if H.PROFILE_ARGS else 'native_http_test_results.json')).write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2));return result.wasSuccessful()
def main():
 p=argparse.ArgumentParser();p.add_argument('--native-python',type=Path,required=True);p.add_argument('--native-schema-dir',type=Path,required=True);args=p.parse_args();spec=importlib.util.spec_from_file_location('native_http_base',ROOT/'test_http.py');http=importlib.util.module_from_spec(spec);spec.loader.exec_module(http)
 with tempfile.TemporaryDirectory(prefix='native-http-private-') as temp:
  http.DATA=Path(temp);http.SERVER_ARGS=['--native-python',str(args.native_python.absolute()),'--native-schema-dir',str(args.native_schema_dir.resolve())]
  for name,role in [('ops_admin','ADMIN'),('team_staff','PREPARER')]:
   subprocess.run([sys.executable,str(ROOT/'run.py'),'--operator-json','--data-dir',str(http.DATA),*http.PROFILE_ARGS],input=json.dumps({'username':name,'display_name':'Synthetic '+role,'password':http.PASSWORD,'role':role}),text=True,check=True,capture_output=True)
  http.start()
  try:return 0 if run_existing(http) else 1
  finally:http.stop()
if __name__=='__main__':raise SystemExit(main())
