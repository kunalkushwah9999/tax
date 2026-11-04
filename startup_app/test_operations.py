#!/usr/bin/env python3
"""Real app HTTP tests with an explicitly fake loopback payment provider and signed TEST experts.
Optional test dependency: cryptography==50.0.2. No real charge, CA identity or filing is tested.
"""
from pathlib import Path
from http import server as http_server
import base64,concurrent.futures,hashlib,hmac,importlib.util,json,os,secrets,subprocess,sys,tempfile,threading,time,unittest,uuid
ROOT=Path(__file__).resolve().parent

def private_json(path,data):
 path.write_text(json.dumps(data));path.chmod(0o600)
class Provider:
 def __init__(self,folder):
  self.lock=threading.Lock();self.orders={};self.payments={};self.refunds={};self.creates=0;self.next_fault='';self.secret=secrets.token_hex(20);self.webhook=secrets.token_hex(20);self.key='rzp_test_fixture'+secrets.token_hex(4);provider=self
  class Handler(http_server.BaseHTTPRequestHandler):
   def log_message(self,*args):pass
   def do_GET(self):self.dispatch(False)
   def do_POST(self):self.dispatch(True)
   def dispatch(self,post):
    expected='Basic '+base64.b64encode((provider.key+':'+provider.secret).encode()).decode()
    if self.headers.get('Authorization')!=expected:self.send_error(401);return
    with provider.lock:
     if post and self.path=='/v1/orders':
      raw=json.loads(self.rfile.read(int(self.headers.get('Content-Length','0'))));provider.creates+=1;pid='order_'+secrets.token_hex(10);row=dict(raw,id=pid,status='created',amount_paid=0,amount_due=raw['amount']);provider.orders[pid]=row;fault=provider.next_fault;provider.next_fault='';out=dict(row);code=200
      if fault=='unknown':code=500
      if fault=='bad_amount':out['amount']+=1
     elif not post:
      table=provider.orders if self.path.startswith('/v1/orders/') else provider.payments if self.path.startswith('/v1/payments/') else provider.refunds if self.path.startswith('/v1/refunds/') else {}
      row=table.get(self.path.rsplit('/',1)[-1]);out=dict(row) if row else {};code=200 if row else 404
     else:out={};code=404
    body=json.dumps(out).encode();self.send_response(code);self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(body)));self.end_headers();self.wfile.write(body)
  self.server=http_server.ThreadingHTTPServer(('127.0.0.1',0),Handler);self.thread=threading.Thread(target=self.server.serve_forever,daemon=True);self.thread.start();self.config=folder/'payment.json';private_json(self.config,{'mode':'TEST','api_base':f'http://127.0.0.1:{self.server.server_port}/v1/','key_id':self.key,'key_secret':self.secret,'webhook_secret':self.webhook})
 def close(self):self.server.shutdown();self.server.server_close();self.thread.join(timeout=3)
 def capture(self,order):
  with self.lock:
   row=self.orders[order['provider_order_id']];row.update(status='paid',amount_paid=row['amount'],amount_due=0);pid='pay_'+secrets.token_hex(10);self.payments[pid]={'id':pid,'order_id':row['id'],'amount':row['amount'],'currency':'INR','captured':True,'status':'captured','amount_refunded':0};return pid
 def refund(self,payment,total):
  with self.lock:
   row=self.payments[payment];row['amount_refunded']=total;row['status']='refunded' if total==row['amount'] else 'captured';rid='rfnd_'+secrets.token_hex(8);self.refunds[rid]={'id':rid,'payment_id':payment,'amount':total,'status':'processed'};return rid
 def signature(self,order,payment):return hmac.new(self.secret.encode(),(order['provider_order_id']+'|'+payment).encode(),hashlib.sha256).hexdigest()
 def event(self,payment,event='payment.captured',refund=None):
  payload={'refund':{'entity':{'id':refund}}} if refund else {'payment':{'entity':{'id':payment}}};return json.dumps({'event':event,'payload':payload},separators=(',',':')).encode()
 def headers(self,raw,event_id=None):return {'Origin':None,'X-CSRF-Token':None,'Content-Type':'application/json','X-Razorpay-Signature':hmac.new(self.webhook.encode(),raw,hashlib.sha256).hexdigest(),'X-Razorpay-Event-Id':event_id or 'evt_'+secrets.token_hex(10)}
class Registry:
 def __init__(self,folder):
  from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
  from cryptography.hazmat.primitives import serialization
  self.key=Ed25519PrivateKey.generate();self.public=base64.b64encode(self.key.public_key().public_bytes(serialization.Encoding.DER,serialization.PublicFormat.SubjectPublicKeyInfo)).decode();self.path=folder/'professionals.json';self.claims={};self.write()
 def write(self):private_json(self.path,{'mode':'TEST','public_key':self.public,'attestations':[{'payload_b64':base64.b64encode(raw).decode(),'signature_b64':base64.b64encode(self.key.sign(raw)).decode()} for raw in self.claims.values()]})
 def set(self,subject,status='ACTIVE',skills=None,expired=False):
  from datetime import datetime,timedelta,timezone
  now=datetime.now(timezone.utc);start=now-timedelta(days=2) if expired else now-timedelta(seconds=2);end=now-timedelta(days=1) if expired else now+timedelta(days=1)
  claim={'subject_id':subject,'credential_ref':'TEST-NOT-ICAI-'+subject[:8],'skills':skills or ['ITR','GST'],'not_before':start.isoformat().replace('+00:00','Z'),'expires_at':end.isoformat().replace('+00:00','Z'),'status':status,'purpose':'TEST_PREPARATION_ONLY'};self.claims[subject]=json.dumps(claim,separators=(',',':')).encode();self.write()

class Operations(unittest.TestCase):
 http=None;provider=None;registry=None
 @classmethod
 def setUpClass(cls):
  h=cls.http;cls.admin=h.Client().login('ops_admin');cls.staff=h.Client().login('team_staff');cls.a=h.Client();cls.a.register('ops_customer');cls.b=h.Client();cls.b.register('ops_other');cls.owners=[cls.a]+[h.Client().login('ops_customer') for _ in range(3)];cls.seq=0;cls.registry.set(cls.staff.user['id'])
  cls.personal=cls.publish('CUSTOMER_PREPARATION',100,5);cls.firm=cls.publish('FIRM_REVIEW',200,3);time.sleep(1.1)
 @classmethod
 def publish(cls,plan,amount=100,credits=5):
  from datetime import datetime,timedelta,timezone
  body={'request_id':str(uuid.uuid4()),'plan':plan,'label':'Synthetic test plan','amount_paise':amount,'credits':credits,'valid_days':30,'effective_at':(datetime.now(timezone.utc)+timedelta(seconds=1)).isoformat().replace('+00:00','Z'),'terms_version':'test-v1'}
  return cls.admin.call('/api/admin/catalog','POST',body)[0]
 def setUp(self):self.a=self.owners[Operations.seq%len(self.owners)];Operations.seq+=1;self.registry.set(self.staff.user['id'])
 def order_body(self,plan=None):return {'request_id':str(uuid.uuid4()),'plan_version_id':(plan or self.personal)['id'],'terms_version':'test-v1','test_payment_consent':True}
 def order(self,client=None,plan=None):return (client or self.a).call('/api/orders','POST',self.order_body(plan))[0]
 def callback(self,order,payment,client=None,status=200,signature=None):return (client or self.a).call('/api/orders/'+order['id']+'/callback','POST',{'payment_id':payment,'signature':signature or self.provider.signature(order,payment)},status=status)[0]
 def paid(self,client=None,plan=None):
  o=self.order(client,plan);pid=self.provider.capture(o);self.callback(o,pid,client);return o,pid
 def webhook(self,raw,event_id=None,status=200,signature=None):
  headers=self.provider.headers(raw,event_id)
  if signature:headers['X-Razorpay-Signature']=signature
  return self.a.call('/api/payments/webhook','POST',raw,status=status,headers=headers)[0]
 def requests(self,case,preparer=None):return {'request_id':str(uuid.uuid4()),'case_id':case['id'],'expected_revision':case['revision'],'topic':'ITR','purpose':'Synthetic unresolved preparation check','preparer_id':self.staff.user['id'] if preparer is None else preparer,'expires_days':1,'consent':True}
 def billing(self,client=None):return (client or self.a).call('/api/billing')[0]
 def test_01_capabilities_cannot_claim_live_tax_authority(self):
  c=self.a.call('/api/operations-capabilities')[0];self.assertEqual(c['payment_mode'],'TEST_FIXTURE');self.assertFalse(c['live_money_collection']);self.assertFalse(c['real_ca_verified']);self.assertFalse(c['tax_authority_changed'])
 def test_02_only_admin_publishes_prices_and_replay_precedes_time_check(self):
  from datetime import datetime,timedelta,timezone
  b=dict(request_id=str(uuid.uuid4()),plan='CUSTOMER_PREPARATION',label='Future',amount_paise=111,valid_days=1,credits=1,effective_at=(datetime.now(timezone.utc)+timedelta(hours=1)).isoformat(),terms_version='test-v2');self.a.call('/api/admin/catalog','POST',b,status=403);one=self.admin.call('/api/admin/catalog','POST',b)[0];two=self.admin.call('/api/admin/catalog','POST',b)[0];self.assertEqual(one,two);b['amount_paise']=112;self.admin.call('/api/admin/catalog','POST',b,status=409)
 def test_03_order_uses_frozen_price_and_rejects_injected_amount_or_role(self):
  b=self.order_body();b['amount_paise']=1;self.a.call('/api/orders','POST',b,status=400);self.staff.call('/api/orders','POST',self.order_body(),status=403);o=self.order();self.assertEqual(o['amount_paise'],100);self.assertNotIn('binding',o);self.assertNotIn('receipt',o);self.assertFalse(o['tax_approval_included'])
 def test_04_unaccepted_terms_or_no_consent_cannot_create_order(self):
  b=self.order_body();b['terms_version']='forged';self.a.call('/api/orders','POST',b,status=409);b['terms_version']='test-v1';b['test_payment_consent']=False;self.a.call('/api/orders','POST',b,status=400)
 def test_05_identical_parallel_order_requests_create_one_provider_order(self):
  b=self.order_body();before=self.provider.creates
  with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:results=list(pool.map(lambda c:c.call('/api/orders','POST',b)[0],self.owners))
  self.assertEqual(len({r['id'] for r in results}),1);self.assertEqual(self.provider.creates-before,1)
 def test_06_unknown_create_outcome_is_not_retried_and_reconcile_checks_binding(self):
  wrong=self.order();b=self.order_body();self.provider.next_fault='unknown';before=self.provider.creates;uncertain=self.a.call('/api/orders','POST',b,status=503)[0];self.assertTrue(uncertain['retry_same_request']);provider=list(self.provider.orders)[-1];o=self.a.call('/api/orders','POST',b)[0];self.assertEqual(o['status'],'OUTCOME_UNKNOWN');self.assertEqual(self.provider.creates-before,1)
  blocked=self.a.call('/api/orders','POST',self.order_body(),status=409)[0];self.assertEqual(blocked['status'],'EXISTING_ORDER_RECONCILIATION_REQUIRED');self.assertEqual(self.provider.creates-before,1);self.a.call('/api/orders/'+o['id']+'/reconcile','POST',{'provider_order_id':wrong['provider_order_id']},status=409);fixed=self.a.call('/api/orders/'+o['id']+'/reconcile','POST',{'provider_order_id':provider})[0];self.assertEqual(fixed['status'],'AWAITING_PAYMENT')
 def test_07_malformed_provider_order_cannot_be_bound(self):
  b=self.order_body();self.provider.next_fault='bad_amount';self.a.call('/api/orders','POST',b,status=503);o=self.a.call('/api/orders','POST',b)[0];self.assertEqual(o['provider_order_id'],'');self.assertEqual(o['status'],'OUTCOME_UNKNOWN');self.a.call('/api/orders/'+o['id']+'/reconcile','POST',{'provider_order_id':list(self.provider.orders)[-1]})
 def test_08_foreign_owner_cannot_read_checkout_or_submit_callback(self):
  o=self.order();pid=self.provider.capture(o);self.b.call('/api/orders/'+o['id']+'/checkout',status=404);self.callback(o,pid,self.b,status=404);self.assertEqual(self.billing(self.b)['orders'],[])
 def test_09_callback_signature_is_bound_to_stored_order(self):
  o=self.order();pid=self.provider.capture(o);self.callback(o,pid,status=403,signature='0'*64);self.assertFalse(any(e['order_id']==o['id'] for e in self.billing()['entitlements']))
 def test_10_authorized_or_amount_mismatched_payment_does_not_grant_access(self):
  o=self.order();pid=self.provider.capture(o);self.provider.payments[pid]['captured']=False;self.callback(o,pid,status=409);self.provider.payments[pid]['captured']=True;self.provider.payments[pid]['amount']+=1;self.callback(o,pid,status=409)
 def test_11_verified_capture_and_replayed_callback_grant_one_test_entitlement(self):
  o,pid=self.paid();self.callback(o,pid);ent=[e for e in self.billing()['entitlements'] if e['order_id']==o['id']];self.assertEqual(len(ent),1);self.assertEqual(ent[0]['credits'],5);self.assertFalse(ent[0]['tax_authority']);self.assertEqual(ent[0]['mode'],'TEST_FIXTURE')
 def test_12_webhook_raw_signature_and_exact_event_replay_are_checked(self):
  o=self.order();pid=self.provider.capture(o);raw=self.provider.event(pid);eid='evt_'+secrets.token_hex(8);self.webhook(raw,eid,status=403,signature='0'*64);self.assertEqual(self.webhook(raw,eid)['status'],'PROCESSED');self.assertEqual(self.webhook(raw,eid)['status'],'REPLAYED');self.webhook(raw+b' ',eid,status=409)
 def test_13_duplicate_capture_under_another_event_does_not_duplicate_credits(self):
  o,pid=self.paid();self.webhook(self.provider.event(pid));self.webhook(self.provider.event(pid,'order.paid'));self.assertEqual(len([e for e in self.billing()['entitlements'] if e['order_id']==o['id']]),1)
 def test_14_partial_refund_suspends_and_stale_capture_cannot_reenable(self):
  o,pid=self.paid();rid=self.provider.refund(pid,10);self.webhook(self.provider.event(pid,'refund.processed',rid));ent=next(e for e in self.billing()['entitlements'] if e['order_id']==o['id']);self.assertEqual(ent['status'],'SUSPENDED_REFUND_REVIEW');self.assertFalse(ent['currently_active']);self.provider.payments[pid]['amount_refunded']=0;self.callback(o,pid);ent=next(e for e in self.billing()['entitlements'] if e['order_id']==o['id']);self.assertEqual(ent['status'],'SUSPENDED_REFUND_REVIEW')
 def test_15_full_refund_revokes_even_when_seen_before_capture_event(self):
  o=self.order();pid=self.provider.capture(o);rid=self.provider.refund(pid,100);self.webhook(self.provider.event(pid,'refund.processed',rid));self.webhook(self.provider.event(pid));ent=next(e for e in self.billing()['entitlements'] if e['order_id']==o['id']);self.assertEqual(ent['status'],'REVOKED_REFUNDED');self.assertFalse(ent['currently_active'])
 def test_16_unbound_webhook_requests_retry_instead_of_losing_credit(self):
  self.provider.next_fault='unknown';b=self.order_body();self.a.call('/api/orders','POST',b,status=503);o=self.a.call('/api/orders','POST',b)[0];o['provider_order_id']=list(self.provider.orders)[-1];pid=self.provider.capture(o);raw=self.provider.event(pid);self.webhook(raw,status=409);self.a.call('/api/orders/'+o['id']+'/reconcile','POST',{'provider_order_id':o['provider_order_id']});self.webhook(raw)
 def test_17_bundle_requires_credit_and_never_confirms_or_approves_inputs(self):
  case=self.b.create();body={'request_id':str(uuid.uuid4()),'expected_revision':case['revision']};self.b.call('/api/cases/'+case['id']+'/preparation-bundle','POST',body,status=402);self.paid(self.b);out=self.b.call('/api/cases/'+case['id']+'/preparation-bundle','POST',body)[0];self.assertFalse(out['tax_approved']);self.assertFalse(out['filing_available']);self.assertTrue(all(v['state']=='UNKNOWN' for v in out['income'].values()));self.assertEqual(self.b.case(case['id'])['revision'],case['revision'])
 def test_18_bundle_same_snapshot_and_exact_retry_consume_one_credit(self):
  self.paid();case=self.a.create();before=sum(e['used'] for e in self.billing()['entitlements']);body={'request_id':str(uuid.uuid4()),'expected_revision':case['revision']};one=self.a.call('/api/cases/'+case['id']+'/preparation-bundle','POST',body)[0];self.a.call('/api/cases/'+case['id']+'/preparation-bundle','POST',body);body['request_id']=str(uuid.uuid4());two=self.a.call('/api/cases/'+case['id']+'/preparation-bundle','POST',body)[0];self.assertEqual(one['id'],two['id']);self.assertEqual(sum(e['used'] for e in self.billing()['entitlements'])-before,1)
 def test_19_old_bundle_retry_does_not_export_current_changed_snapshot(self):
  self.paid();case=self.a.create();body={'request_id':str(uuid.uuid4()),'expected_revision':case['revision']};self.a.call('/api/cases/'+case['id']+'/preparation-bundle','POST',body);self.a.edit(case,'PROPOSE',field='SALARY',text='2 lakh',unit='');result=self.a.call('/api/cases/'+case['id']+'/preparation-bundle','POST',body)[0];self.assertEqual(result['status'],'HISTORICAL_SNAPSHOT_CHANGED');self.assertNotIn('income',result)
 def test_20_firm_subscription_does_not_reveal_unshared_cases(self):
  self.paid(self.staff,self.firm);private=self.a.create();queue=self.staff.call('/api/firm-queue')[0];self.assertNotIn(private['id'],[c['id'] for c in queue['cases']]);self.assertFalse(queue['bulk_approval_available']);self.a.call('/api/firm-queue',status=403)
 def test_21_only_signed_test_professionals_appear_in_directory(self):
  experts=self.a.call('/api/experts')[0]['experts'];entry=next(e for e in experts if e['id']==self.staff.user['id']);self.assertEqual(entry['verification'],'SIGNED_TEST_ATTESTATION_ONLY');self.assertFalse(entry['real_ca_verified']);self.registry.set(self.staff.user['id'],status='REVOKED');self.assertEqual(self.a.call('/api/experts')[0]['experts'],[])
 def test_22_expert_request_explicit_consent_and_current_revision_are_required(self):
  case=self.a.create();b=self.requests(case);b['consent']=False;self.a.call('/api/expert-requests','POST',b,status=400);b['consent']=True;b['expected_revision']=0;self.a.call('/api/expert-requests','POST',b,status=409);self.b.call('/api/expert-requests','POST',self.requests(case),status=404)
 def test_23_no_live_roster_request_waits_without_access_or_fake_signoff(self):
  case=self.a.create();r=self.a.call('/api/expert-requests','POST',self.requests(case,''))[0];self.assertEqual(r['status'],'WAITING_NO_VERIFIED_LIVE_ROSTER');self.assertEqual(r['grant_id'],'');self.assertFalse(r['professional_signoff']);self.staff.call('/api/cases/'+case['id'],status=404)
 def test_24_expert_can_accept_comment_but_cannot_edit_or_access_private_worksheets(self):
  case=self.a.create();r=self.a.call('/api/expert-requests','POST',self.requests(case))[0];seen=self.staff.case(case['id']);self.assertTrue(seen['review_only']);self.staff.call('/api/expert-requests/'+r['id']+'/update','POST',{'request_id':str(uuid.uuid4()),'expected_revision':1,'status':'ACCEPTED_TEST_REVIEW'});self.staff.call('/api/cases/'+case['id']+'/gst',status=404);self.staff.call('/api/cases/'+case['id']+'/edit','POST',self.staff.payload(seen,'PROPOSE',field='SALARY',text='1 lakh',unit=''),status=404)
 def test_25_signed_credential_revocation_and_tamper_immediately_deny_existing_grant(self):
  case=self.a.create();self.a.call('/api/expert-requests','POST',self.requests(case));self.staff.case(case['id']);self.registry.set(self.staff.user['id'],status='REVOKED');self.staff.call('/api/cases/'+case['id'],status=404);self.registry.set(self.staff.user['id']);cfg=json.loads(self.registry.path.read_text());cfg['attestations'][0]['signature_b64']=base64.b64encode(b'0'*64).decode();private_json(self.registry.path,cfg);self.staff.call('/api/cases/'+case['id'],status=404)
 def test_26_expired_or_wrong_skill_attestation_cannot_get_new_request(self):
  case=self.a.create();self.registry.set(self.staff.user['id'],expired=True);self.a.call('/api/expert-requests','POST',self.requests(case),status=409);self.registry.set(self.staff.user['id'],skills=['GST']);self.a.call('/api/expert-requests','POST',self.requests(case),status=409)
 def test_27_request_cancel_revokes_and_replayed_accept_does_not_restore_access(self):
  case=self.a.create();r=self.a.call('/api/expert-requests','POST',self.requests(case))[0];accept={'request_id':str(uuid.uuid4()),'expected_revision':1,'status':'ACCEPTED_TEST_REVIEW'};self.staff.call('/api/expert-requests/'+r['id']+'/update','POST',accept);self.a.call('/api/expert-requests/'+r['id']+'/update','POST',{'request_id':str(uuid.uuid4()),'expected_revision':2,'status':'CANCELLED'});self.staff.call('/api/cases/'+case['id'],status=404);self.staff.call('/api/expert-requests/'+r['id']+'/update','POST',accept,status=404)
 def test_28_feedback_is_isolated_redacted_and_idempotent(self):
  b={'request_id':str(uuid.uuid4()),'topic':'USABILITY','rating':2,'message':'Synthetic ABCDE1234F and 123456789012 must not persist here.'};one=self.a.call('/api/feedback','POST',b)[0];two=self.a.call('/api/feedback','POST',b)[0];self.assertEqual(one['id'],two['id']);self.assertNotIn('ABCDE1234F',one['message']);self.assertNotIn('123456789012',one['message']);self.assertNotIn(one['id'],[f['id'] for f in self.b.call('/api/feedback')[0]['feedback']])
 def test_29_feedback_admin_triage_has_revision_and_customer_visible_reply(self):
  f=self.a.call('/api/feedback','POST',{'request_id':str(uuid.uuid4()),'topic':'GST_PREPARATION','rating':3,'message':'Make period selection clearer.'})[0];b={'request_id':str(uuid.uuid4()),'expected_revision':1,'status':'PLANNED','reply':'Period selector improvement recorded.'};self.a.call('/api/admin/feedback/'+f['id']+'/triage','POST',b,status=403);self.admin.call('/api/admin/feedback/'+f['id']+'/triage','POST',b);b['request_id']=str(uuid.uuid4());self.admin.call('/api/admin/feedback/'+f['id']+'/triage','POST',b,status=409);saved=next(v for v in self.a.call('/api/feedback')[0]['feedback'] if v['id']==f['id']);self.assertEqual(saved['status'],'PLANNED');self.assertEqual(saved['reply'],b['reply'])
 def test_30_operations_metrics_exclude_test_captures_from_live_revenue(self):
  stats=self.admin.call('/api/admin/operations')[0];self.assertGreater(stats['test_captured_paise'],0);self.assertEqual(stats['live_revenue_paise'],0);self.assertEqual(stats['automatic_tax_approvals'],0);self.assertFalse(stats['growth_claim_verified']);self.a.call('/api/admin/operations',status=403)
 def test_31_deleting_case_removes_assistance_and_bundle_links_without_resurrection(self):
  self.paid();case=self.a.create();bundle={'request_id':str(uuid.uuid4()),'expected_revision':case['revision']};self.a.call('/api/cases/'+case['id']+'/preparation-bundle','POST',bundle);request=self.requests(case);self.a.call('/api/expert-requests','POST',request);case=self.a.case(case['id']);self.a.call('/api/cases/'+case['id']+'/delete','POST',{'expected_revision':case['revision'],'confirm':'DELETE'});self.a.call('/api/cases/'+case['id']+'/preparation-bundle','POST',bundle,status=404);self.a.call('/api/expert-requests','POST',request,status=404);self.assertNotIn(case['id'],[r['case_id'] for r in self.a.call('/api/expert-requests')[0]['requests']]);self.staff.call('/api/cases/'+case['id'],status=404)

 def test_32_restart_preserves_orders_entitlements_and_request_receipts(self):
  body=self.order_body();order=self.a.call('/api/orders','POST',body)[0];pid=self.provider.capture(order);self.callback(order,pid);feedback=self.a.call('/api/feedback','POST',{'request_id':str(uuid.uuid4()),'topic':'OTHER','rating':4,'message':'Synthetic restart marker.'})[0];before=self.provider.creates
  self.http.stop();self.http.start();client=self.http.Client().login('ops_customer');replay=client.call('/api/orders','POST',body)[0];self.assertEqual(replay['id'],order['id']);self.assertEqual(replay['status'],'PAID_TEST');self.assertEqual(self.provider.creates,before);self.assertTrue(any(e['order_id']==order['id'] for e in self.billing(client)['entitlements']));self.assertIn(feedback['id'],[f['id'] for f in client.call('/api/feedback')[0]['feedback']]);Operations.owners=[client]+[self.http.Client().login('ops_customer') for _ in range(3)];Operations.admin=self.http.Client().login('ops_admin');Operations.staff=self.http.Client().login('team_staff')
 def test_33_signed_callback_for_another_order_payment_is_rejected(self):
  one=self.order();two=self.order();pid=self.provider.capture(two);self.callback(one,pid,status=409);self.assertFalse(any(e['order_id']==one['id'] for e in self.billing()['entitlements']))
 def test_34_parallel_bundle_requests_for_one_snapshot_charge_once(self):
  self.paid();case=self.a.create();before=sum(e['used'] for e in self.billing()['entitlements']);bodies=[{'request_id':str(uuid.uuid4()),'expected_revision':case['revision']} for _ in self.owners]
  with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:values=list(pool.map(lambda pair:pair[0].call('/api/cases/'+case['id']+'/preparation-bundle','POST',pair[1])[0],zip(self.owners,bodies)))
  self.assertEqual(len({v['id'] for v in values}),1);self.assertEqual(sum(e['used'] for e in self.billing()['entitlements'])-before,1)
 def test_35_refund_blocks_paid_bundle_replay_but_keeps_owner_data_export(self):
  client=self.http.Client();client.register('ops_refund_user');order,pid=self.paid(client);case=client.create();body={'request_id':str(uuid.uuid4()),'expected_revision':case['revision']};client.call('/api/cases/'+case['id']+'/preparation-bundle','POST',body);rid=self.provider.refund(pid,100);self.webhook(self.provider.event(pid,'refund.processed',rid));client.call('/api/cases/'+case['id']+'/preparation-bundle','POST',body,status=402);export=client.call('/api/cases/'+case['id']+'/export')[0];self.assertFalse(export['filing_available']);self.assertEqual(export['case']['id'],case['id'])

 def test_36_paid_bundle_includes_owner_working_papers_without_tax_release(self):
  self.paid();case=self.a.create('Synthetic capital bundle');path='/api/cases/'+case['id'];csv='record_id,account_alias,asset_alias,event_type,trade_date,sequence,quantity,gross_amount,fees\nB1,ACC_A,ASSET_A,BUY,2024-01-01,1,1,1.00,0.00\nS1,ACC_A,ASSET_A,SELL,2025-04-01,1,1,0.50,0.00\n'
  self.a.call(path+'/workbench','POST',dict(operation='CAPITAL_IMPORT',request_id=str(uuid.uuid4()),expected_revision=case['revision'],workbench_version='',csv=csv,synthetic_confirmed=True,same_entity_confirmed=True,coverage_checked=True,complex_events='NO'));case=self.a.case(case['id']);body=dict(request_id=str(uuid.uuid4()),expected_revision=case['revision']);bundle=self.a.call(path+'/preparation-bundle','POST',body)[0];self.assertIn('tax_workbench',bundle);self.assertIsNone(bundle['tax_workbench']['capital']['taxable_gain']);self.assertFalse(bundle['tax_approved'] or bundle['filing_available']);sheet=self.a.call(path+'/workbench')[0];self.a.call(path+'/workbench','POST',dict(operation='CAPITAL_CLEAR',request_id=str(uuid.uuid4()),expected_revision=case['revision'],workbench_version=sheet['version']));self.assertEqual(self.a.call(path+'/preparation-bundle','POST',body)[0]['status'],'HISTORICAL_SNAPSHOT_CHANGED');self.assertEqual(self.a.call(path+'/export')[0]['case']['tax_workbench']['capital']['status'],'NO_CAPITAL_WORKSHEET')

def run_existing(http_module,folder=None):
 owned=folder is None;folder=folder or Path(tempfile.mkdtemp(prefix='tax-operations-'));provider=Provider(folder);registry=Registry(folder);old_args=list(http_module.SERVER_ARGS)
 try:
  http_module.stop();http_module.SERVER_ARGS=old_args+['--payment-config',str(provider.config),'--professional-registry',str(registry.path)];http_module.start();Operations.http=http_module;Operations.provider=provider;Operations.registry=registry
  selected=os.environ.get("TAX_OPERATIONS_SELECTED_GROUP");suite=unittest.TestSuite([Operations(selected)]) if selected else unittest.defaultTestLoader.loadTestsFromTestCase(Operations);result=unittest.TextTestRunner(verbosity=2).run(suite);report={'operations_http_groups':result.testsRun,'failures':len(result.failures),'errors':len(result.errors),'skipped':len(result.skipped),'real_app_http':True,'payment_provider':'EXPLICIT_LOOPBACK_TEST_FIXTURE','real_charges_tested':False,'professional_credentials':'SIGNED_TEST_ATTESTATIONS_NOT_ICAI','mysql_profile_exercised':bool(http_module.PROFILE_ARGS),'production_ready':False,'jar_sha256':hashlib.sha256((ROOT/'build/tax-platform-startup-5.1.0.jar').read_bytes()).hexdigest()};name='operations_mysql_http_test_results.json' if http_module.PROFILE_ARGS else 'operations_http_test_results.json';(ROOT/'build'/name).write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report));return result.wasSuccessful()
 finally:
  provider.close();http_module.stop();http_module.SERVER_ARGS=old_args;http_module.start()
  if owned:
   import shutil;shutil.rmtree(folder,ignore_errors=True)
if __name__=='__main__':
 spec=importlib.util.spec_from_file_location('operations_base_http',ROOT/'test_http.py');http=importlib.util.module_from_spec(spec);spec.loader.exec_module(http)
 with tempfile.TemporaryDirectory(prefix='tax-operations-http-') as name:
  folder=Path(name);http.DATA=folder/'data'
  for username,role in [('ops_admin','ADMIN'),('team_staff','PREPARER')]:subprocess.run([sys.executable,str(ROOT/'run.py'),'--data-dir',str(http.DATA),'--operator-json'],input=json.dumps({'username':username,'display_name':'Synthetic '+role,'password':http.PASSWORD,'role':role}).encode(),check=True,stdout=subprocess.PIPE)
  http.start()
  try:ok=run_existing(http,folder)
  finally:http.stop()
 sys.exit(0 if ok else 1)
