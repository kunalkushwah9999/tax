#!/usr/bin/env python3
"""Actual authenticated working-paper APIs. These tests cannot approve statutory tax outputs."""
from pathlib import Path
import hashlib,importlib.util,json,subprocess,sys,tempfile,unittest,uuid
ROOT=Path(__file__).resolve().parent
H=None
CAPITAL_HEADER='record_id,account_alias,asset_alias,event_type,trade_date,sequence,quantity,gross_amount,fees'
CAPITAL=CAPITAL_HEADER+'\nB1,ACC_A,ASSET_A,BUY,2024-04-01,1,3,1.00,0.01\nS1,ACC_A,ASSET_A,SELL,2025-04-01,1,1,1.00,0.00\nS2,ACC_A,ASSET_A,SELL,2025-04-02,1,1,1.00,0.00\nS3,ACC_A,ASSET_A,SELL,2025-04-03,1,1,1.00,0.00\n'
TDS_HEADER='record_id,deductor_alias,section_code,financial_year,income_ref,gross_amount,tds_amount'
CERTIFICATE=TDS_HEADER+'\nCERT_A,EMP_A,192,FY2025-26,SALARY_TOTAL,800000.00,40000.00\n'
STATEMENT=CERTIFICATE.replace('CERT_A','STMT_A')
class Workbench(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.a=H.Client();cls.a.register('workbench_customer');cls.b=H.Client();cls.b.register('workbench_other');cls.staff=H.Client().login('team_staff');cls.admin=H.Client().login('ops_admin');cls.owners=[cls.a]+[H.Client().login('workbench_customer') for _ in range(4)];cls.seq=0
 def setUp(self):
  self.a=self.owners[Workbench.seq%len(self.owners)];Workbench.seq+=1;self.row=self.a.create('Synthetic workbench test')
 def get(self,row=None):return self.a.call('/api/cases/'+(row or self.row)['id']+'/workbench')[0]
 def payload(self,op,**values):return dict(operation=op,request_id=str(uuid.uuid4()),expected_revision=self.a.case(self.row['id'])['revision'],workbench_version=self.get()['version'],**values)
 def send(self,p,status=200,client=None):return (client or self.a).call('/api/cases/'+self.row['id']+'/workbench','POST',p,status=status)[0]
 def cap(self,**values):return self.payload('CAPITAL_IMPORT',csv=CAPITAL,synthetic_confirmed=True,same_entity_confirmed=True,coverage_checked=True,complex_events='NO')|values
 def tds(self,**values):return self.payload('TDS_IMPORT',certificate_csv=CERTIFICATE,statement_csv=STATEMENT,synthetic_confirmed=True,same_entity_confirmed=True,certificate_complete=True,statement_complete=True)|values
 def notice(self,**values):return self.payload('NOTICE_SAVE',notice_id='',notice_alias='NOTICE_A',category='INCOME_TAX',section_label='USER_LABEL',received_on='2026-11-01',stated_due_on='UNKNOWN',status='OPEN',checklist={k:'TODO' for k in self.get()['notice_tasks']},draft='',receipt_alias='',reported_on='',source_checked=False,synthetic_confirmed=True)|values
 def grant(self):
  self.a.call('/api/cases/'+self.row['id']+'/grants','POST',dict(request_id=str(uuid.uuid4()),expected_revision=self.a.case(self.row['id'])['revision'],preparer_username='team_staff',expires_days=7,consent=True))
 def test_01_initial_unknown_and_authority_flags(self):
  s=self.get();self.assertEqual(s['version'],'');self.assertEqual(s['capital']['status'],'NO_CAPITAL_WORKSHEET');self.assertFalse(s['tax_values_released'] or s['filing_available'] or s['ca_approval'])
 def test_02_capital_fifo_paise_review_and_export(self):
  self.send(self.cap());s=self.get()['capital'];self.assertIsNone(s['gross_book_difference_total']);self.assertEqual([x['acquisition_gross'] for x in s['matches']],['0.33','0.33','0.34']);self.send(self.payload('CAPITAL_REVIEW',observations_reviewed=True));s=self.get()['capital'];self.assertEqual(s['gross_book_difference_total'],'2.00');self.assertIsNone(s['taxable_gain']);self.assertFalse(s['fees_deducted_from_difference']);export,h=self.a.call('/api/cases/'+self.row['id']+'/workbench-export');self.assertIn('attachment',h['Content-Disposition']);self.assertEqual(export['format'],'TAX_PREPARATION_WORKBENCH_NOT_A_RETURN');self.assertEqual(export['worksheet']['capital']['gross_book_difference_total'],'2.00')
 def test_03_cross_account_and_asset_do_not_share_lots(self):
  self.send(self.cap(csv=CAPITAL.replace('S1,ACC_A','S1,ACC_B')));s=self.get()['capital'];self.assertEqual(len(s['unmatched_sales']),1);self.assertIsNone(s['gross_book_difference_total']);self.assertEqual(s['unmatched_sales'][0]['quantity'],'1')
 def test_04_unknown_cost_and_fee_do_not_become_zero(self):
  for replace in [('1.00,0.01','UNKNOWN,0.01'),('1.00,0.01','1.00,UNKNOWN')]:
   self.send(self.cap(csv=CAPITAL.replace(*replace)));self.send(self.payload('CAPITAL_REVIEW',observations_reviewed=True));s=self.get()['capital'];self.assertIsNone(s['gross_book_difference_total']);self.assertIn('UNKNOWN_MATCHED_AMOUNT',s['issues'])
 def test_05_duplicate_identity_ambiguous_order_invalid_input_atomic(self):
  before=self.get()
  for csv in [CAPITAL+CAPITAL.splitlines()[1]+'\n',CAPITAL+CAPITAL.splitlines()[1].replace('B1,','B2,')+'\n',CAPITAL.replace('0.01','0.001'),CAPITAL.replace('2024-04-01','2025-02-29'),CAPITAL.replace(',BUY,',',BONUS,'),CAPITAL.replace('ACC_A','ABCDE1234F')]:self.send(self.cap(csv=csv),400);self.assertEqual(self.get(),before)
 def test_06_coverage_complex_and_review_are_separate(self):
  self.send(self.cap(complex_events='UNKNOWN',coverage_checked=False));self.send(self.payload('CAPITAL_REVIEW',observations_reviewed=True));s=self.get()['capital'];self.assertIsNone(s['gross_book_difference_total']);self.assertIn('COVERAGE_REVIEW_REQUIRED',s['issues']);self.assertIn('COMPLEX_EVENTS_NOT_RESOLVED',s['issues'])
 def test_07_capital_scope_blocks_even_if_profile_overwrites_screen(self):
  self.send(self.cap());row=self.a.case(self.row['id']);self.assertNotEqual(self.row['snapshot'],row['snapshot']);self.assertEqual(row['profile']['profile_confirmed'],'NO');self.assertFalse(any(row['reviews'].values()));profile=dict(row['profile'],screen_special='NO');self.a.edit(row,'PROFILE',profile=profile);r,_=self.a.call('/api/cases/'+self.row['id']+'/assess','POST',status=409);self.assertEqual(r['status'],'CAPITAL_GAINS_REQUIRE_EXPANDED_TAX_ENGINE');self.assertEqual(self.a.call('/api/cases/'+self.row['id']+'/itr-readiness')[0]['status'],'EXPANDED_WORKFLOW_REQUIRED')
 def test_08_exact_replay_preserves_later_work_and_clear(self):
  p=self.cap();self.send(p);self.send(self.payload('CAPITAL_REVIEW',observations_reviewed=True));current=self.get();self.assertEqual(self.send(p)['status'],'REPLAYED');self.assertEqual(self.get(),current);self.send(self.payload('CAPITAL_CLEAR'));cleared=self.get();self.assertEqual(self.send(p)['status'],'REPLAYED');self.assertEqual(self.get(),cleared);self.assertNotIn('ACC_A',json.dumps(cleared))
 def test_09_stale_revision_and_version_and_changed_request_id_conflict(self):
  p=self.cap();self.send(p);self.send(dict(p,csv=CAPITAL.replace('1.00','2.00')),409);self.send(dict(p,request_id=str(uuid.uuid4())),409);p=self.payload('CAPITAL_CLEAR');p['workbench_version']='old';self.assertEqual(self.send(p,409)['status'],'STALE_TAX_WORKBENCH')
 def test_10_owner_only_reads_writes_exports_and_grants(self):
  p=self.cap();self.send(p);self.grant();self.assertNotIn('tax_workbench',self.staff.case(self.row['id']));self.assertNotIn('tax_workbench',self.a.case(self.row['id']))
  for client in [self.b,self.staff,self.admin]:
   for suffix in ['/workbench','/workbench-export','/export']:client.call('/api/cases/'+self.row['id']+suffix,status=404)
   self.send(p,404,client)
 def test_11_no_csrf_and_mass_assignment_cannot_write(self):
  for extra in ['owner_id','tax_approved','filing_available','claim_amount']:self.send(self.cap()|{extra:True},400)
  self.a.call('/api/cases/'+self.row['id']+'/workbench','POST',self.cap(),headers={'X-CSRF-Token':'wrong'},status=403);self.assertEqual(self.get()['version'],'')
 def test_12_tds_matching_observations_need_review_and_no_tax_credit(self):
  self.send(self.tds());s=self.get()['tds'];self.assertEqual(s['groups'][0]['status'],'MATCHED_OBSERVATIONS');self.assertIsNone(s['statement_tds_observation_total']);self.send(self.payload('TDS_REVIEW',group_id=s['groups'][0]['id'],action='REVIEWED_OBSERVATIONS',observations_reviewed=True));s=self.get()['tds'];self.assertEqual(s['statement_tds_observation_total'],'40000.00');self.assertIsNone(s['claim_amount']);self.assertEqual(s['tax_credit_eligibility'],'NOT_DETERMINED');self.assertTrue(all(x['state']=='UNKNOWN' for x in self.a.case(self.row['id'])['inputs'].values()))
 def test_13_tds_mismatch_followup_does_not_resolve_original_issue(self):
  self.send(self.tds(statement_csv=STATEMENT.replace('40000.00','39999.00')));s=self.get()['tds'];g=s['groups'][0];self.send(self.payload('TDS_REVIEW',group_id=g['id'],action='REVIEWED_OBSERVATIONS',observations_reviewed=True),409);self.send(self.payload('TDS_REVIEW',group_id=g['id'],action='DEDUCTOR_FOLLOW_UP',observations_reviewed=True));s=self.get()['tds'];self.assertEqual(s['status'],'PREPARATION_INCOMPLETE');self.assertEqual(s['groups'][0]['review']['status'],'DEDUCTOR_FOLLOW_UP');self.assertIn('DIFFERENT_TDS_AMOUNT',s['groups'][0]['issues']);self.assertIsNone(s['statement_tds_observation_total'])
 def test_14_tds_duplicates_missing_unknown_and_reimport_drop_reviews(self):
  for statement in [TDS_HEADER+'\n',STATEMENT.replace('40000.00','UNKNOWN'),STATEMENT+STATEMENT.splitlines()[1].replace('STMT_A','STMT_B')+'\n']:
   self.send(self.tds(statement_csv=statement));self.assertEqual(self.get()['tds']['groups'][0]['status'],'REVIEW_REQUIRED');self.assertIsNone(self.get()['tds']['statement_tds_observation_total'])
  self.send(self.tds());g=self.get()['tds']['groups'][0];self.send(self.payload('TDS_REVIEW',group_id=g['id'],action='REVIEWED_OBSERVATIONS',observations_reviewed=True));self.send(self.tds());self.assertEqual(self.get()['tds']['reviewed_groups'],0);self.send(self.payload('TDS_REVIEW',group_id=g['id'],action='REVIEWED_OBSERVATIONS',observations_reviewed=True),404)
 def test_15_tds_invalid_year_section_alias_and_blank_reject(self):
  for csv in [CERTIFICATE.replace('FY2025-26','FY2026-27'),CERTIFICATE.replace('EMP_A','ABCD12345E'),CERTIFICATE.replace('40000.00',''),CERTIFICATE.replace(',192,',',=192,')]:self.send(self.tds(certificate_csv=csv),400)
 def test_16_notice_stated_dates_unknown_draft_and_html_are_safe_data(self):
  self.send(self.notice(draft='<script>throw 1</script>'));n=self.get()['notices'][0];self.assertIsNone(n['days_to_user_stated_date']);self.assertFalse(n['legal_deadline_determined'] or n['official_submission_verified']);self.assertEqual(n['draft'],'<script>throw 1</script>');self.assertEqual(self.a.call('/api/cases/'+self.row['id']+'/itr-readiness')[0]['status'],'EXPANDED_WORKFLOW_REQUIRED')
 def test_17_notice_invalid_date_and_reported_receipt_requirement(self):
  self.send(self.notice(stated_due_on='2026-02-30'),400);self.send(self.notice(status='RESPONSE_DRAFTED'),400);self.send(self.notice(status='SUBMISSION_REPORTED_UNVERIFIED'),400);self.send(self.notice(status='SUBMISSION_REPORTED_UNVERIFIED',source_checked=True,receipt_alias='RECEIPT_A',reported_on='2026-11-02'));n=self.get()['notices'][0];self.assertEqual(n['status'],'SUBMISSION_REPORTED_UNVERIFIED');self.assertFalse(n['official_submission_verified'])
 def test_18_notice_update_keeps_id_and_delete_retry_does_not_resurrect(self):
  self.send(self.notice());n=self.get()['notices'][0];p=self.notice(notice_id=n['id'],status='RESPONSE_DRAFTED',draft='Synthetic response for document review.');self.send(p);self.assertEqual(len(self.get()['notices']),1);self.send(self.payload('NOTICE_DELETE',notice_id=n['id']));self.assertEqual(self.send(p)['status'],'REPLAYED');self.assertEqual(self.get()['notices'],[])
 def test_19_notice_count_and_draft_limits_atomic(self):
  self.send(self.notice(draft='a'*3001),400)
  for i in range(8):self.send(self.notice(notice_alias='NOTICE_'+str(i)))
  version=self.get()['version'];self.send(self.notice(),409);self.assertEqual(self.get()['version'],version)
 def test_20_archived_mutations_blocked_but_owner_export_stays(self):
  self.send(self.cap());row=self.a.case(self.row['id']);self.a.edit(row,'ARCHIVE');self.send(self.payload('CAPITAL_CLEAR'),409);self.assertEqual(self.a.call('/api/cases/'+self.row['id']+'/workbench-export')[0]['format'],'TAX_PREPARATION_WORKBENCH_NOT_A_RETURN')
 def test_21_case_export_includes_private_data_audit_does_not(self):
  self.send(self.cap());self.send(self.tds());self.send(self.notice(draft='PRIVATE_WORKBENCH_DRAFT'));export=self.a.call('/api/cases/'+self.row['id']+'/export')[0];self.assertIn('PRIVATE_WORKBENCH_DRAFT',json.dumps(export));self.assertIn('tax_workbench',export['case']);audit=json.dumps(self.admin.call('/api/audit')[0]);self.assertNotIn('PRIVATE_WORKBENCH_DRAFT',audit);self.assertNotIn('ACC_A',audit);self.assertNotIn('800000.00',audit)
 def test_22_case_delete_denies_retry_and_all_private_exports(self):
  p=self.cap();self.send(p);self.a.call('/api/cases/'+self.row['id']+'/delete','POST',dict(expected_revision=self.a.case(self.row['id'])['revision'],confirm='DELETE'));self.send(p,404)
  for suffix in ['/workbench','/workbench-export','/export']:self.a.call('/api/cases/'+self.row['id']+suffix,status=404)
 def test_23_unicode_drafts_preserved_without_truncation(self):
  for i in range(8):self.send(self.notice(notice_alias='LONG_'+str(i),draft='क'*3000))
  # The valid Unicode drafts survive the encrypted store; neither byte nor character size is silently truncated.
  self.assertTrue(all(len(n['draft'])==3000 for n in self.get()['notices']))
 def test_24_served_workbench_asset_and_unchanged_tax_release(self):
  asset,h=self.a.call('/workbench.js');self.assertIn('renderCapitalWorking',asset);self.assertIn('text/javascript',h['Content-Type']);self.assertEqual(self.a.call('/api/bootstrap')[0]['readiness']['independent_approved_outputs'],0)

def run_existing(http):
 global H
 H=http;result=unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(Workbench));profile=http.PROFILE_ARGS[http.PROFILE_ARGS.index('--storage')+1] if http.PROFILE_ARGS else 'local';report={'workbench_http_groups':result.testsRun,'failures':len(result.failures),'errors':len(result.errors),'skipped':len(result.skipped),'jar_sha256':hashlib.sha256((ROOT/'build/tax-platform-startup-5.1.0.jar').read_bytes()).hexdigest(),'storage_profile':profile,'real_http':True,'synthetic_data_only':True,'independent_tax_approval':False,'source_inputs':{'test_workbench.py':hashlib.sha256(Path(__file__).read_bytes()).hexdigest()}};(ROOT/'build'/('workbench_'+profile.replace('-','_')+'_http_test_results.json')).write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report));return result.wasSuccessful()
if __name__=='__main__':
 spec=importlib.util.spec_from_file_location('workbench_base_http',ROOT/'test_http.py');http=importlib.util.module_from_spec(spec);spec.loader.exec_module(http)
 with tempfile.TemporaryDirectory(prefix='tax-workbench-http-') as name:
  http.DATA=Path(name)/'data'
  for username,role in [('ops_admin','ADMIN'),('team_staff','PREPARER')]:subprocess.run([sys.executable,str(ROOT/'run.py'),'--data-dir',str(http.DATA),'--operator-json'],input=json.dumps({'username':username,'display_name':'Synthetic '+role,'password':http.PASSWORD,'role':role}).encode(),check=True,stdout=subprocess.PIPE)
  http.start()
  try:ok=run_existing(http)
  finally:http.stop()
  sys.exit(0 if ok else 1)
