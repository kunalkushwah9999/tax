#!/usr/bin/env python3
"""Actual loopback integration checks for the integrated startup development application."""
from pathlib import Path
import concurrent.futures,os,hashlib,http.cookiejar,json,queue,subprocess,sys,tempfile,threading,time,unittest,urllib.error,urllib.request,uuid
ROOT=Path(__file__).resolve().parent;BASE='';PROCESS=None;DATA=None
JAR=ROOT/'build/tax-platform-startup-5.1.0.jar'
PROFILE_ARGS=[]
SERVER_ARGS=[]
if os.environ.get('TAX_TEST_STORAGE')=='mysql':PROFILE_ARGS=['--storage','mysql','--mysql-config',os.environ['TAX_MYSQL_CONFIG'],'--jdbc-jar',os.environ['TAX_MYSQL_JDBC'],'--key-file',os.environ['TAX_MYSQL_KEY']]
PASSWORD='Development-'+str(uuid.uuid4())
class Client:
 def __init__(self):
  self.cookies=http.cookiejar.CookieJar();self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookies));self.csrf='';self.user=None
 def call(self,path,method='GET',body=None,status=200,headers=None):
  h={}
  if method=='POST':h={'Origin':BASE,'X-CSRF-Token':self.csrf}
  if isinstance(body,dict):body=json.dumps(body).encode();h['Content-Type']='application/json'
  for key,value in (headers or {}).items():
   if value is None:h.pop(key,None)
   else:h[key]=value
  request=urllib.request.Request(BASE+path,data=body,headers=h,method=method)
  try:r=self.opener.open(request,timeout=10)
  except urllib.error.HTTPError as e:r=e
  with r:
   raw=r.read();result=json.loads(raw) if 'application/json' in r.headers.get('Content-Type','') else raw.decode()
   if r.status!=status:raise AssertionError(f'{path}: HTTP {r.status} expected {status}; code '+str(result.get('status') if isinstance(result,dict) else 'asset'))
   return result,r.headers
 def bootstrap(self):
  data,h=self.call('/api/bootstrap');self.csrf=data['csrf_token'];self.user=data['user'];return data,h
 def login(self,name):
  self.bootstrap();data,_=self.call('/api/login','POST',{'username':name,'password':PASSWORD});self.csrf=data['csrf_token'];self.user=data['user'];return self
 def register(self,name):
  self.bootstrap();data,h=self.call('/api/register','POST',{'username':name,'display_name':'Sample '+name,'password':PASSWORD});self.csrf=data['csrf_token'];self.user=data['user'];return data,h
 def create(self,title='Sample preparation'):
  data,_=self.call('/api/cases','POST',{'title':title,'request_id':str(uuid.uuid4())});return self.case(data['id'])
 def case(self,case):return self.call('/api/cases/'+case)[0]
 def payload(self,row,op,**values):return {'operation':op,'request_id':str(uuid.uuid4()),'expected_revision':row['revision'],**values}
 def edit(self,row,op,status=200,**values):
  result,_=self.call('/api/cases/'+row['id']+'/edit','POST',self.payload(row,op,**values),status=status)
  return self.case(row['id']) if status==200 else result

def start():
 global PROCESS,BASE
 PROCESS=subprocess.Popen([sys.executable,str(ROOT/'run.py'),'--port','0','--data-dir',str(DATA),*PROFILE_ARGS,*SERVER_ARGS],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,cwd=ROOT)
 q=queue.Queue()
 def read():
  for line in PROCESS.stdout:
   if line.startswith('STARTUP_URL='):q.put(line.strip().split('=',1)[1]);return
 threading.Thread(target=read,daemon=True).start();BASE=q.get(timeout=15)
def stop():
 PROCESS.terminate()
 try:PROCESS.wait(timeout=5)
 except subprocess.TimeoutExpired:PROCESS.kill();PROCESS.wait(timeout=5)
class Integration(unittest.TestCase):
 seq=0
 @classmethod
 def setUpClass(cls):
  cls.first=Client();cls.first.register('customer_a');cls.second=Client();cls.second.register('customer_b')
  cls.owners=[cls.first]+[Client().login('customer_a') for _ in range(4)]
  cls.admin=Client().login('ops_admin');cls.preparer=Client().login('team_staff')
 def setUp(self):self.a=self.owners[Integration.seq%len(self.owners)];Integration.seq+=1;self.b=self.second
 def test_01_customer_registration_is_persisted_and_cannot_assign_roles(self):
  self.assertEqual(self.a.user['role'],'CUSTOMER');self.assertNotIn('credential',self.a.user)
  anon=Client();anon.bootstrap();self.assertEqual(anon.call('/api/register','POST',{'username':'injected','display_name':'Bad','password':PASSWORD,'role':'ADMIN'},status=400)[0]['status'],'UNEXPECTED_FIELDS')
 def test_02_cookie_rotation_and_csrf_are_session_bound(self):
  anon=Client();before,h=anon.bootstrap();oldcookie=h['Set-Cookie'];data,h=anon.call('/api/login','POST',{'username':'customer_a','password':PASSWORD});self.assertNotEqual(before['csrf_token'],data['csrf_token']);self.assertNotEqual(oldcookie,h['Set-Cookie'])
  self.assertIn('HttpOnly',h['Set-Cookie']);self.assertIn('SameSite=Strict',h['Set-Cookie']);self.assertNotIn('Domain=',h['Set-Cookie'])
 def test_03_unauthenticated_accounts_cannot_read_or_create_cases(self):
  anon=Client();anon.call('/api/cases',status=401);anon.bootstrap();anon.call('/api/cases','POST',{'title':'Denied','request_id':str(uuid.uuid4())},status=401)
 def test_04_exact_host_origin_and_fetch_metadata_are_enforced(self):
  anon=Client();anon.call('/api/bootstrap',headers={'Host':'malicious.invalid'},status=421);anon.call('/api/bootstrap',headers={'Origin':'https://malicious.invalid'},status=403);anon.call('/api/bootstrap',headers={'Sec-Fetch-Site':'cross-site'},status=403)
 def test_05_csrf_missing_origin_and_foreign_session_csrf_cannot_write(self):
  row=self.a.create();p=self.a.payload(row,'CLEAR',field='SALARY')
  for headers in [{'X-CSRF-Token':'wrong'},{'Origin':None},{'X-CSRF-Token':self.b.csrf}]:self.a.call('/api/cases/'+row['id']+'/edit','POST',p,headers=headers,status=403)
  self.assertEqual(self.a.case(row['id']),row)
 def test_06_saved_case_starts_unknown_and_cross_account_access_is_denied(self):
  row=self.a.create();self.assertTrue(all(v=={'state':'UNKNOWN'} for v in row['inputs'].values()))
  for client in [self.b,self.admin,self.preparer]:
   for suffix in ['', '/export','/sources','/grants']:client.call('/api/cases/'+row['id']+suffix,status=404)
 def test_07_exact_amount_parser_and_separate_confirmation(self):
  row=self.a.create();row=self.a.edit(row,'PROPOSE',field='SALARY',text='8 lakh',unit='');self.assertEqual(row['inputs']['SALARY'],{'state':'ESTIMATED','amount':'800000.00'})
  self.assertEqual(self.a.edit(row,'CONFIRM',status=409,field='SALARY',amount='800001.00')['status'],'DISPLAYED_AMOUNT_CHANGED')
  row=self.a.edit(row,'CONFIRM',field='SALARY',amount='800000.00');self.assertEqual(row['inputs']['SALARY']['state'],'CONFIRMED')
 def test_08_blank_is_not_zero_and_clear_is_not_a_zero_fact(self):
  row=self.a.create();self.a.edit(row,'PROPOSE',status=400,field='SALARY',text='',unit='');row=self.a.edit(row,'PROPOSE',field='SALARY',text='0 rupees',unit='');row=self.a.edit(row,'CONFIRM',field='SALARY',amount='0.00');row=self.a.edit(row,'CLEAR',field='SALARY');self.assertEqual(row['inputs']['SALARY'],{'state':'UNKNOWN'})
 def test_09_stale_edit_is_rejected_and_identical_retry_keeps_later_changes(self):
  row=self.a.create();p=self.a.payload(row,'PROPOSE',field='SALARY',text='1 lakh',unit='');first,_=self.a.call('/api/cases/'+row['id']+'/edit','POST',p);row=self.a.case(row['id']);row=self.a.edit(row,'PROPOSE',field='SALARY',text='2 lakh',unit='')
  replay,_=self.a.call('/api/cases/'+row['id']+'/edit','POST',p);self.assertEqual(replay['status'],'REPLAYED');self.assertEqual(self.a.case(row['id']),row)
  p['request_id']=str(uuid.uuid4());self.a.call('/api/cases/'+row['id']+'/edit','POST',p,status=409)
 def test_10_same_id_changed_body_conflicts_and_mass_assignment_is_rejected(self):
  row=self.a.create();p=self.a.payload(row,'PROPOSE',field='SALARY',text='1 lakh',unit='');self.a.call('/api/cases/'+row['id']+'/edit','POST',p)
  self.a.call('/api/cases/'+row['id']+'/edit','POST',dict(p,text='2 lakh'),status=409)
  for name in ['owner_id','rule_state','reference_state','filing_available','tax_values_released']:self.a.call('/api/cases/'+row['id']+'/edit','POST',dict(p,**{name:'APPROVED'}),status=400)
 def test_11_full_income_review_resets_after_material_amount_edit(self):
  row=self.prepared();self.assertTrue(all(row['reviews'].values()));row=self.a.edit(row,'PROPOSE',field='SALARY',text='900000.00',unit='RUPEES');self.assertFalse(any(row['reviews'].values()))
 def prepared(self):
  row=self.a.create();profile=dict(row['profile'],dob='2000-10-15',residency='ROR',profile_confirmed='YES',eligible_confirmed='YES',eligible_80c='0.00',eligible_80d='0.00',eligible_nps='0.00');profile.update({key:'NO' for key in profile if key.startswith('screen_')});row=self.a.edit(row,'PROFILE',profile=profile)
  for field,value in [('SALARY','800000.00'),('SAVINGS_INTEREST','5000.00'),('DEPOSIT_INTEREST','0.00'),('OTHER_INTEREST','0.00')]:row=self.a.edit(row,'PROPOSE',field=field,text=value,unit='RUPEES');row=self.a.edit(row,'CONFIRM',field=field,amount=value)
  for inv in ['INCOME','NEW_DEDUCTIONS','OLD_DEDUCTIONS']:row=self.a.edit(row,'REVIEW',inventory=inv)
  return row
 def test_12_real_existing_engine_and_guard_return_readiness_without_tax_values(self):
  row=self.prepared();data,_=self.a.call('/api/cases/'+row['id']+'/assess','POST');self.assertEqual(data['result']['status'],'BLOCKED');self.assertEqual(data['independent_approved_cases'],0)
  self.assertTrue(all(v['input_status']=='CALCULATED' and v['arithmetic_cross_check']=='MATCH' for v in data['checks'].values()));text=json.dumps(data)
  for value in ['annual_taxes','slab_tax','800000.00','workings']:self.assertNotIn(value,text)
  self.assertFalse(data['result']['capabilities']['submit_return']);self.assertFalse(data['result']['capabilities']['export_itr'])
 def test_13_incomplete_and_unsupported_planning_remain_explicit(self):
  row=self.a.create();data,_=self.a.call('/api/cases/'+row['id']+'/assess','POST');self.assertTrue(all(v['input_status']=='INFORMATION_REQUIRED' for v in data['checks'].values()))
  p=dict(row['profile'],screen_business='YES');row=self.a.edit(row,'PROFILE',profile=p);data,_=self.a.call('/api/cases/'+row['id']+'/assess','POST');self.assertFalse(data['calculation_values_released'])
 @staticmethod
 def csv(*rows):return 'observation_id,source,category,payer_alias,period,coverage,reference,gross_amount,tds_amount,value_kind\n'+'\n'.join(rows)+'\n'
 @staticmethod
 def source_row(identifier='salary_form16',source='FORM16',gross='800000.00',coverage='ANNUAL',reference='TOTAL'):return ','.join([identifier,source,'SALARY','EMP_A','FY2025-26',coverage,reference,gross,'40000.00','REPORTED'])
 def source_ready(self,csv=None):
  row=self.a.create();row=self.a.edit(row,'SOURCE_IMPORT',csv=csv or self.csv(self.source_row()));data,_=self.a.call('/api/cases/'+row['id']+'/sources');group=data['groups'][0];row=self.a.edit(row,'SOURCE_SELECT',source_version=data['comparison_token'],group_id=group['group_id'],observation_id=group['observations'][0]['observation_id'],reason='CHECKED_SOURCE_RECORD');data,_=self.a.call('/api/cases/'+row['id']+'/sources')
  p=self.a.payload(row,'SOURCE_PROPOSE',source_version=data['comparison_token'],group_id=group['group_id'],observation_id=group['observations'][0]['observation_id'],field='SALARY',amount=group['observations'][0]['gross_amount'],whole_category_confirmed=True,replace_input_confirmed=True);return row,p
 def test_14_persisted_source_proposal_stays_estimated_and_confirmation_is_separate(self):
  row,p=self.source_ready();self.a.call('/api/cases/'+row['id']+'/edit','POST',p);row=self.a.case(row['id']);self.assertEqual(row['inputs']['SALARY'],{'state':'ESTIMATED','amount':'800000.00'});self.assertFalse(row['source_links']['SALARY']['source_authenticity_verified']);self.a.call('/api/cases/'+row['id']+'/sources',status=409)
  row=self.a.edit(row,'CONFIRM',field='SALARY',amount='800000.00');self.assertEqual(row['source_links']['SALARY']['current_fact_state'],'CONFIRMED');self.assertFalse(row['ca_approval'])
 def test_15_source_retry_cannot_restore_removed_lineage_or_old_value(self):
  row,p=self.source_ready();self.a.call('/api/cases/'+row['id']+'/edit','POST',p);row=self.a.case(row['id']);row=self.a.edit(row,'PROPOSE',field='SALARY',text='800000.00',unit='RUPEES');self.assertEqual(row['source_links'],{});self.a.call('/api/cases/'+row['id']+'/edit','POST',p);self.assertEqual(self.a.case(row['id']),row)
 def test_16_conflicting_annual_and_overlapping_transaction_sources_are_blocked(self):
  for csv,code in [(self.csv(self.source_row(),self.source_row('salary_ais','AIS','900000.00')),'GROSS_SOURCE_REVIEW_REQUIRED'),(self.csv(self.source_row(),self.source_row('tx','AIS',coverage='TRANSACTION',reference='TX1')),'CATEGORY_COVERAGE_AMBIGUOUS')]:
   row,p=self.source_ready(csv);self.assertEqual(self.a.call('/api/cases/'+row['id']+'/edit','POST',p,status=409)[0]['status'],code);self.assertEqual(self.a.case(row['id'])['source_links'],{})
 def test_17_source_cannot_transfer_tds_or_skip_acknowledgements(self):
  row,p=self.source_ready()
  for values,code in [({'amount':'40000.00'},'SOURCE_AMOUNT_CHANGED'),({'whole_category_confirmed':False},'WHOLE_CATEGORY_REVIEW_REQUIRED'),({'replace_input_confirmed':False},'REPLACEMENT_REVIEW_REQUIRED')]:self.assertEqual(self.a.call('/api/cases/'+row['id']+'/edit','POST',dict(p,**values),status=409)[0]['status'],code)
 def test_18_native_documents_and_bad_normalized_csv_cannot_be_uploaded(self):
  row=self.a.create()
  for csv in ['%PDF-1.7','wrong,header\n','\ufffd'] :self.a.edit(row,'SOURCE_IMPORT',status=400,csv=csv)
  self.a.call('/api/cases/'+row['id']+'/upload','POST',{'file':'data'},status=404)
 def grant_case(self):
  row=self.a.create();p={'request_id':str(uuid.uuid4()),'expected_revision':row['revision'],'preparer_username':'team_staff','expires_days':7,'consent':True};self.a.call('/api/cases/'+row['id']+'/grants','POST',p);return self.a.case(row['id'])
 def test_19_consent_is_explicit_and_shared_role_cannot_edit_income(self):
  row=self.grant_case();shared=self.preparer.case(row['id']);self.assertTrue(shared['review_only']);self.assertFalse(shared['ca_approval']);self.preparer.call('/api/cases/'+row['id']+'/edit','POST',self.preparer.payload(shared,'CLEAR',field='SALARY'),status=404)
 def test_20_revocation_denies_new_reads_and_identical_old_note_retries(self):
  row=self.grant_case();p={'request_id':str(uuid.uuid4()),'expected_revision':row['revision'],'text':'Check the synthetic savings interest.'};self.preparer.call('/api/cases/'+row['id']+'/note','POST',p);row=self.a.case(row['id']);grants,_=self.a.call('/api/cases/'+row['id']+'/grants');self.a.call('/api/cases/'+row['id']+'/revoke','POST',{'request_id':str(uuid.uuid4()),'expected_revision':row['revision'],'grant_id':grants['grants'][0]['id']});self.preparer.call('/api/cases/'+row['id'],status=404);self.preparer.call('/api/cases/'+row['id']+'/note','POST',p,status=404)
 def test_21_preparation_notes_are_snapshot_bound_and_not_ca_approval(self):
  row=self.grant_case();self.preparer.call('/api/cases/'+row['id']+'/note','POST',{'request_id':str(uuid.uuid4()),'expected_revision':row['revision'],'text':'Sample working-paper request.'});row=self.a.case(row['id']);note=row['preparation_notes'][0];self.assertEqual(note['scope'],'PREPARATION_COMMENT_NOT_CA_SIGNOFF');row=self.a.edit(row,'PROPOSE',field='SALARY',text='1 lakh',unit='');self.assertNotEqual(note['input_snapshot'],row['snapshot'])
 def test_22_document_checklist_declarations_do_not_authenticate_evidence(self):
  row=self.a.create();row=self.a.edit(row,'DOCUMENT',document='FORM16',state='DECLARED_AVAILABLE');self.assertEqual(row['documents']['FORM16'],'DECLARED_AVAILABLE');self.assertFalse(row['tax_values_released']);self.assertFalse(row['ca_approval'])
 def test_23_admin_sees_counts_without_private_financial_case_values(self):
  data,_=self.admin.call('/api/admin');self.assertGreater(data['cases_count'],0);self.assertEqual(data['mysql_backend_active'],os.environ.get('TAX_TEST_STORAGE')=='mysql');self.assertNotIn('inputs',json.dumps(data));self.a.call('/api/admin',status=403)
 def test_24_issued_quotes_freeze_price_and_never_create_payments_or_tax_permissions(self):
  now=time.time();p={'request_id':str(uuid.uuid4()),'plan':'GUIDED_PREPARATION','amount_paise':9900,'effective_at':time.strftime('%Y-%m-%dT%H:%M:%SZ',time.gmtime(now+1.1)),'label':'Synthetic pilot planning price'};self.admin.call('/api/admin/prices','POST',p);self.assertEqual(self.admin.call('/api/admin/prices','POST',p)[0]['status'],'REPLAYED');time.sleep(1.2)
  request={'request_id':str(uuid.uuid4()),'plan':'GUIDED_PREPARATION'};self.a.call('/api/quotes','POST',request);quotes,_=self.a.call('/api/quotes');quote=quotes['quotes'][0];self.assertEqual(quote['amount_paise'],9900);self.assertFalse(quote['payment_available']);self.assertFalse(quote['tax_capability_changed']);self.assertEqual(self.a.call('/api/quotes','POST',request)[0]['status'],'REPLAYED')
  future=dict(p,request_id=str(uuid.uuid4()),amount_paise=19900,effective_at=time.strftime('%Y-%m-%dT%H:%M:%SZ',time.gmtime(time.time()+60)));self.admin.call('/api/admin/prices','POST',future);self.assertEqual(self.a.call('/api/quotes')[0]['quotes'][0],quote);self.assertEqual(self.b.call('/api/quotes')[0]['quotes'],[])
 def test_25_only_admin_can_set_future_prices_and_prices_do_not_enable_checkout(self):
  p={'request_id':str(uuid.uuid4()),'plan':'FIRM_WORKSPACE','amount_paise':0,'effective_at':'2099-01-01T00:00:00Z','label':'Synthetic future'};self.a.call('/api/admin/prices','POST',p,status=403);self.admin.call('/api/admin/prices','POST',dict(p,effective_at='2020-01-01T00:00:00Z'),status=400);self.assertFalse(self.a.call('/api/prices')[0]['checkout_available']);self.a.call('/api/pay','POST',{},status=404)
 def test_26_support_threads_isolate_customers_and_allow_scoped_admin_responses(self):
  p={'request_id':str(uuid.uuid4()),'topic':'PRODUCT_HELP','subject':'Synthetic support','message':'How to confirm a proposal?'};receipt,_=self.a.call('/api/tickets','POST',p);self.assertEqual(self.a.call('/api/tickets','POST',p)[0]['status'],'REPLAYED');self.assertEqual(self.b.call('/api/tickets')[0]['tickets'],[])
  self.admin.call('/api/tickets/'+receipt['id']+'/update','POST',{'request_id':str(uuid.uuid4()),'expected_revision':1,'status':'RESOLVED','message':'Use the separate Confirm amount action.'});self.assertEqual(self.a.call('/api/tickets')[0]['tickets'][0]['status'],'RESOLVED')
 def test_27_export_is_owner_only_preparation_data_not_itr(self):
  row=self.a.create();row=self.a.edit(row,'PROPOSE',field='SALARY',text='1 lakh',unit='');data,headers=self.a.call('/api/cases/'+row['id']+'/export');self.assertEqual(data['format'],'PREPARATION_DATA_EXPORT_NOT_ITR');self.assertEqual(data['case']['inputs']['SALARY']['amount'],'100000.00');self.assertFalse(data['filing_available']);self.assertIn('attachment;',headers['Content-Disposition'])
 def test_28_archiving_blocks_changes_and_deleted_create_retry_does_not_resurrect(self):
  p={'title':'Delete me','request_id':str(uuid.uuid4())};first,_=self.a.call('/api/cases','POST',p);row=self.a.case(first['id']);row=self.a.edit(row,'ARCHIVE');self.a.edit(row,'CLEAR',status=409,field='SALARY');row=self.a.edit(row,'RESTORE');self.a.call('/api/cases/'+row['id']+'/delete','POST',{'expected_revision':row['revision'],'confirm':'DELETE'});self.assertEqual(self.a.call('/api/cases','POST',p)[0]['status'],'REPLAYED');self.a.call('/api/cases/'+row['id'],status=404)
 def test_29_audit_excludes_financial_values_raw_source_text_and_passwords(self):
  events,_=self.a.call('/api/audit');text=json.dumps(events)
  for secret in ['800000.00','1 lakh',PASSWORD,'salary_form16','How to confirm a proposal?','gross_amount','Sample working-paper request.']:self.assertNotIn(secret,text)
 def test_30_static_assets_and_routes_have_security_headers(self):
  for path in ['/','/app.js','/styles.css']:
   body,h=self.a.call(path);self.assertTrue(body);self.assertEqual(h['Cache-Control'],'no-store');self.assertEqual(h['X-Frame-Options'],'DENY');self.assertIn("frame-ancestors 'none'",h['Content-Security-Policy'])
  for path in ['/../runtime-data/keys/vault.key','/runtime-data/records/workspace.vault','/api/approve','/api/file'] :self.a.call(path,status=404)
  self.a.call('/api/cases?owner=other',status=400)
 def test_31_json_decoding_rejects_duplicate_fields_invalid_utf8_size_and_floats(self):
  row=self.a.create();route='/api/cases/'+row['id']+'/edit'
  for payload in [b'{"operation":"CLEAR","operation":"CONFIRM"}',b'{"x":0.5}',b'\xff',b'{"x":"\\uD800"}']:self.a.call(route,'POST',payload,headers={'Content-Type':'application/json'},status=400)
  self.a.call(route,'POST',b'x'*65537,headers={'Content-Type':'application/json'},status=413);self.a.call(route,'POST',b'{}',headers={'Content-Type':'text/plain'},status=415)
 def test_32_concurrent_identical_commands_commit_once(self):
  row=self.a.create();p=self.a.payload(row,'PROPOSE',field='SALARY',text='1 lakh',unit='');cookie='; '.join(c.name+'='+c.value for c in self.a.cookies)
  def send(_):return Client().call('/api/cases/'+row['id']+'/edit','POST',p,headers={'Cookie':cookie,'X-CSRF-Token':self.a.csrf})[0]['status']
  with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:self.assertEqual(sorted(pool.map(send,range(2))),['APPLIED','REPLAYED'])
  self.assertEqual(self.a.case(row['id'])['revision'],2)
 def test_33_concurrent_new_writers_cannot_both_apply_stale_view(self):
  row=self.a.create();payloads=[self.a.payload(row,'PROPOSE',field='SALARY',text=value,unit='') for value in ['1 lakh','2 lakh']];cookie='; '.join(c.name+'='+c.value for c in self.a.cookies)
  def send(p):
   request=urllib.request.Request(BASE+'/api/cases/'+row['id']+'/edit',data=json.dumps(p).encode(),headers={'Cookie':cookie,'Origin':BASE,'X-CSRF-Token':self.a.csrf,'Content-Type':'application/json'},method='POST')
   try:r=urllib.request.urlopen(request,timeout=10)
   except urllib.error.HTTPError as e:r=e
   with r:return json.loads(r.read())['status']
  with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:self.assertEqual(sorted(pool.map(send,payloads)),['APPLIED','STALE_CASE'])
 def test_34_local_disk_contains_ciphertext_without_inputs_or_passwords(self):
  
  if PROFILE_ARGS:self.skipTest('MySQL at-rest ciphertext checks are in the actual SQL suite.')
  raw=(DATA/'records/workspace.vault').read_bytes();self.assertNotIn(PASSWORD.encode(),raw);self.assertNotIn(b'800000.00',raw);self.assertNotIn(b'persist_user',raw);self.assertEqual(len((DATA/'keys/vault.key').read_bytes()),32)
 def test_35_authentication_abuse_is_bounded_without_echoing_credentials(self):
  anon=Client();anon.bootstrap()
  for _ in range(10):self.assertEqual(anon.call('/api/login','POST',{'username':'unknown_actor','password':'not-the-password'},status=401)[0]['status'],'LOGIN_FAILED')
  self.assertEqual(anon.call('/api/login','POST',{'username':'unknown_actor','password':'not-the-password'},status=429)[0]['status'],'REQUEST_LIMIT')
 def gst_import(self,row,**changes):
  header='supplier_ref,document_type,document_no,document_date,taxable,igst,cgst,sgst,cess,reverse_charge,statement_itc'
  p={'operation':'IMPORT','request_id':str(uuid.uuid4()),'expected_revision':row['revision'],'period':'2025-07','books_csv':header+'\nSUPPLIER_A,INVOICE,INV/01,2025-07-03,1000.00,180.00,0.00,0.00,0.00,NO,NOT_APPLICABLE','statement_csv':header+'\nSUPPLIER_A,INVOICE,INV/01,2025-07-03,1000.00,180.00,0.00,0.00,0.00,NO,AVAILABLE','same_entity_confirmed':True,'books_complete':True,'statement_complete':True};p.update(changes);return p
 def gst_review(self,row,sheet,action='REVIEWED_MATCH'):
  return {'operation':'REVIEW','request_id':str(uuid.uuid4()),'expected_revision':row['revision'],'worksheet_version':sheet['version'],'group_id':sheet['groups'][0]['id'],'action':action,'observations_reviewed':True}
 def scope_body(self,row,**changes):
  data=self.a.call('/api/cases/'+row['id']+'/itr-readiness')[0];positive={'individual','ordinary_resident','adult_under_60_throughout_fy','salary_and_ordinary_interest_only'}
  p={'request_id':str(uuid.uuid4()),'expected_revision':row['revision'],'period':data['period'],'answers':{k:'YES' if k in positive else 'NO' for k in data['questions']},'answers_reviewed':True};p.update(changes);return p
 def test_36_itr_unknown_scope_and_new_owner_only_routes(self):
  row=self.a.create();base='/api/cases/'+row['id'];data=self.a.call(base+'/itr-readiness')[0];self.assertEqual(len(data['unknown_answers']),14);self.assertFalse(data['filing_available']);self.assertFalse(data['scope_is_statutory_form_eligibility'])
  for client in [self.b,self.admin,self.preparer]:
   for suffix in ['/itr-readiness','/gst','/gst-export']:client.call(base+suffix,status=404)
   client.call(base+'/itr-scope','POST',self.scope_body(row),status=404);client.call(base+'/gst','POST',self.gst_import(row),status=404)
 def test_37_itr_declaration_stales_after_material_edit_and_exact_retry_preserves_staleness(self):
  row=self.a.create();base='/api/cases/'+row['id'];p=self.scope_body(row);self.a.call(base+'/itr-scope','POST',p);self.assertTrue(self.a.call(base+'/itr-readiness')[0]['scope_review_current']);row=self.a.case(row['id']);self.a.edit(row,'PROPOSE',field='SALARY',text='1234 rupees',unit='');self.assertFalse(self.a.call(base+'/itr-readiness')[0]['scope_review_current']);self.assertEqual(self.a.call(base+'/itr-scope','POST',p)[0]['status'],'REPLAYED');self.assertFalse(self.a.call(base+'/itr-readiness')[0]['scope_review_current'])
 def test_38_itr_outside_scope_is_not_approved_by_a_declaration(self):
  row=self.a.create();base='/api/cases/'+row['id'];p=self.scope_body(row);p['answers']['capital_gains']='YES';self.a.call(base+'/itr-scope','POST',p);d=self.a.call(base+'/itr-readiness')[0];self.assertEqual(d['status'],'EXPANDED_WORKFLOW_REQUIRED');self.assertFalse(d['ca_required_determined']);self.assertIn('STATUTORY_FORM_ELIGIBILITY_NOT_DETERMINED',d['release_blocks'])
 def test_39_gst_real_saved_match_requires_review_and_has_no_credit_claim(self):
  row=self.a.create();base='/api/cases/'+row['id'];self.a.call(base+'/gst','POST',self.gst_import(row));sheet=self.a.call(base+'/gst')[0];self.assertEqual(sheet['status'],'PREPARATION_INCOMPLETE');self.a.call(base+'/gst','POST',self.gst_review(self.a.case(row['id']),sheet));after=self.a.call(base+'/gst')[0];self.assertEqual(after['status'],'OBSERVATION_REVIEW_COMPLETE');self.assertEqual(after['reviewed_groups'],1);self.assertIsNone(after['itc_claim_amount']);self.assertEqual(after['credit_eligibility'],'NOT_DETERMINED');self.assertFalse(after['filing_available']);self.assertNotIn('self_service',self.a.case(row['id']))
 def test_40_gst_duplicate_unknown_and_mismatch_cannot_be_promoted_to_a_match(self):
  row=self.a.create();base='/api/cases/'+row['id'];p=self.gst_import(row);p['statement_csv']=p['statement_csv'].replace('1000.00','UNKNOWN');p['books_csv']+='\n'+p['books_csv'].split('\n')[1];self.a.call(base+'/gst','POST',p);sheet=self.a.call(base+'/gst')[0];self.assertIn('DUPLICATE_DOCUMENT_KEY',sheet['groups'][0]['issues']);self.assertIn('INCOMPLETE_DATA',sheet['groups'][0]['issues']);self.a.call(base+'/gst','POST',self.gst_review(self.a.case(row['id']),sheet),status=409);self.a.call(base+'/gst','POST',self.gst_review(self.a.case(row['id']),sheet,'NO_CLAIM_REVIEW'));self.assertEqual(self.a.call(base+'/gst')[0]['open_issue_groups'],1)
 def test_41_gst_reimport_resets_reviews_and_rejects_stale_worksheet_version(self):
  row=self.a.create();base='/api/cases/'+row['id'];p=self.gst_import(row);self.a.call(base+'/gst','POST',p);sheet=self.a.call(base+'/gst')[0];old=self.gst_review(self.a.case(row['id']),sheet);self.a.call(base+'/gst','POST',old);self.a.call(base+'/gst','POST',self.gst_import(self.a.case(row['id'])));after=self.a.call(base+'/gst')[0];self.assertNotEqual(after['version'],sheet['version']);self.assertEqual(after['reviewed_groups'],0);old['request_id']=str(uuid.uuid4());old['expected_revision']=self.a.case(row['id'])['revision'];old['group_id']=after['groups'][0]['id'];self.a.call(base+'/gst','POST',old,status=409);self.a.call(base+'/gst','POST',p);self.assertEqual(self.a.call(base+'/gst')[0],after)
 def test_42_gst_no_native_upload_or_filing_authority_and_coverage_remains_distinct(self):
  row=self.a.create();base='/api/cases/'+row['id'];p=self.gst_import(row,books_complete=False);self.a.call(base+'/gst','POST',p);sheet=self.a.call(base+'/gst')[0];self.a.call(base+'/gst','POST',self.gst_review(self.a.case(row['id']),sheet));after=self.a.call(base+'/gst')[0];self.assertFalse(after['coverage_declarations_complete']);self.assertEqual(after['status'],'PREPARATION_INCOMPLETE');p=self.gst_import(self.a.case(row['id']),filing_available=True);self.a.call(base+'/gst','POST',p,status=400);p=self.gst_import(self.a.case(row['id']),books_csv='{"gstin":"synthetic"}');self.a.call(base+'/gst','POST',p,status=400)
 def test_43_gst_export_clear_and_old_retry_do_not_resurrect_sources(self):
  row=self.a.create();base='/api/cases/'+row['id'];p=self.gst_import(row);self.a.call(base+'/gst','POST',p);out,h=self.a.call(base+'/gst-export');self.assertEqual(out['format'],'GST_PREPARATION_REVIEW_NOT_A_RETURN');self.assertFalse(out['itc_eligibility_determined']);self.assertIn('attachment',h['Content-Disposition']);self.a.call(base+'/gst','POST',{'operation':'CLEAR','request_id':str(uuid.uuid4()),'expected_revision':self.a.case(row['id'])['revision']});self.a.call(base+'/gst','POST',p);self.assertEqual(self.a.call(base+'/gst')[0]['status'],'NO_GST_WORKSHEET');self.assertNotIn('INV/01',json.dumps(self.a.call(base+'/export')[0]))
 def test_44_granted_preparer_does_not_inherit_new_private_gst_or_itr_data(self):
  row=self.a.create();base='/api/cases/'+row['id'];self.a.call(base+'/gst','POST',self.gst_import(row));row=self.a.case(row['id']);self.a.call(base+'/grants','POST',{'request_id':str(uuid.uuid4()),'expected_revision':row['revision'],'preparer_username':'team_staff','expires_days':1,'consent':True});self.assertNotIn('self_service',self.preparer.case(row['id']));self.assertNotIn('SUPPLIER_A',json.dumps(self.preparer.case(row['id'])));self.preparer.call(base+'/gst',status=404);self.preparer.call(base+'/itr-readiness',status=404)
 def test_45_gst_new_routes_keep_csrf_and_reject_stale_case_writes(self):
  row=self.a.create();base='/api/cases/'+row['id'];p=self.gst_import(row);self.a.call(base+'/gst','POST',p,headers={'X-CSRF-Token':'wrong'},status=403);self.a.edit(row,'RENAME',title='Changed');self.a.call(base+'/gst','POST',p,status=409);self.assertEqual(self.a.call(base+'/gst')[0]['status'],'NO_GST_WORKSHEET')
 def test_46_optional_native_worker_is_disabled_by_default(self):
  data,_=self.a.call('/api/native-capabilities');self.assertFalse(data['native_itr_configured']);self.assertFalse(data['native_gst_available']);self.assertFalse(data['filing_available'])
 def test_47_disabled_native_preview_keeps_owner_csrf_and_configuration_gates(self):
  row=self.a.create();base='/api/cases/'+row['id'];p={'expected_revision':row['revision'],'synthetic_confirmed':True,'json_text':'{}'};self.a.call(base+'/itr-native-preview','POST',p,status=503);self.b.call(base+'/itr-native-preview','POST',p,status=404);self.a.call(base+'/itr-native-preview','POST',p,status=403,headers={'X-CSRF-Token':'wrong'})
 def test_48_forged_native_preview_cannot_create_or_confirm_income(self):
  row=self.a.create();p={'request_id':str(uuid.uuid4()),'expected_revision':row['revision'],'preview_id':'A'*43,'same_taxpayer_confirmed':True,'selections':[{'field':'SALARY','amount':'800000.00','whole_category_confirmed':True,'replace_input_confirmed':True}]};self.a.call('/api/cases/'+row['id']+'/itr-native-propose','POST',p,status=409);self.assertEqual(self.a.case(row['id']),row)
 def test_zz_restart_preserves_saved_data_and_invalidates_old_sessions(self):
  row=self.a.create('Persists across restart');row=self.a.edit(row,'PROPOSE',field='SALARY',text='123456.78',unit='RUPEES');stop();start();self.a.call('/api/cases',status=401);reopened=Client().login('customer_a');saved=reopened.case(row['id']);self.assertEqual(saved['inputs']['SALARY']['amount'],'123456.78');self.assertFalse(saved['tax_values_released'])
def main():
 global DATA
 if not JAR.exists():raise SystemExit('Build the startup application first.')
 with tempfile.TemporaryDirectory(prefix='tax-startup-http-') as folder:
  DATA=Path(folder)
  for username,role in [('ops_admin','ADMIN'),('team_staff','PREPARER')]:
   payload={'username':username,'display_name':'Sample '+role,'password':PASSWORD,'role':role};p=subprocess.run([sys.executable,str(ROOT/'run.py'),'--operator-json','--data-dir',str(DATA),*PROFILE_ARGS],input=json.dumps(payload),capture_output=True,text=True)
   if p.returncode:raise SystemExit('Test operator setup failed: '+p.stderr)
  start()
  try:
   result=unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(Integration));report={'startup_http_groups':result.testsRun,'failures':len(result.failures),'errors':len(result.errors),'jar_sha256':hashlib.sha256(JAR.read_bytes()).hexdigest(),'real_http':True,'restart_recovery_exercised':True,'browser_transaction_flow_tested':False,'mysql_profile_exercised':bool(PROFILE_ARGS),'skipped':len(result.skipped),'production_ready':False};(ROOT/'build'/('mysql_http_test_results.json' if PROFILE_ARGS else 'http_test_results.json')).write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2));return 0 if result.wasSuccessful() else 1
  finally:stop()
if __name__=='__main__':raise SystemExit(main())
