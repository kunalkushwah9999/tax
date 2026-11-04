#!/usr/bin/env python3
"""Real password/MFA/session APIs with an independent Python RFC6238 generator."""
from pathlib import Path
import base64,concurrent.futures,hashlib,hmac,importlib.util,json,os,struct,subprocess,sys,tempfile,time,unittest,uuid
ROOT=Path(__file__).resolve().parent
H=None
def otp(secret,offset=0):
 key=base64.b32decode(secret);digest=hmac.new(key,struct.pack('>Q',int(time.time())//30+offset),hashlib.sha1).digest();i=digest[-1]&15;return str((struct.unpack('>I',digest[i:i+4])[0]&0x7fffffff)%1000000).zfill(6)
def password_login(client,name,password):
 client.bootstrap();data,headers=client.call('/api/login','POST',dict(username=name,password=password));client.csrf=data['csrf_token'];client.user=data['user'];return data,headers
def factor_login(client,code,method='RECOVERY_CODE',status=200):
 data,headers=client.call('/api/login-factor','POST',dict(method=method,code=code),status=status)
 if status==200:client.csrf=data['csrf_token'];client.user=data['user']
 return data,headers
def raw_factor(client,code,method='TOTP'):
 import urllib.request,urllib.error
 request=urllib.request.Request(H.BASE+'/api/login-factor',data=json.dumps(dict(method=method,code=code)).encode(),headers={'Origin':H.BASE,'X-CSRF-Token':client.csrf,'Content-Type':'application/json'},method='POST')
 try:r=client.opener.open(request,timeout=15)
 except urllib.error.HTTPError as e:r=e
 with r:return r.status
class Security(unittest.TestCase):
 seq=0
 def setUp(self):
  # Bound fixtures avoid weakening the application's password-login admission limit.
  if Security.seq%3==0:H.stop();H.start()
  Security.seq+=1;self.username='security_'+str(Security.seq);self.password=H.PASSWORD;self.a=H.Client();self.a.register(self.username)
 def view(self):return self.a.call('/api/security')[0]
 def payload(self,op,method='',code='',**values):return dict(operation=op,expected_security_revision=self.view()['security_revision'],password=self.password,factor_method=method,factor_code=code,**values)
 def command(self,op,method='',code='',status=200,**values):
  body=self.payload(op,method,code,**values);return self.send(body,status)
 def send(self,body,status=200):
  result,headers=self.a.call('/api/security/update','POST',body,status=status)
  if status==200:self.a.csrf=result['csrf_token'];self.a.user=result['user']
  return result,headers
 def enroll(self):
  setup,_=self.command('START_ENROLLMENT');result,_=self.command('CONFIRM_ENROLLMENT','TOTP',otp(setup['secret']),setup_id=setup['setup_id']);return setup['secret'],result['recovery_codes']
 def challenge(self,client=None):
  client=client or H.Client();result,headers=password_login(client,self.username,self.password);self.assertTrue(result['mfa_required']);self.assertIsNone(result['user']);return client,result,headers
 def test_01_defaults_and_password_sign_in_remain_compatible(self):
  state=self.view();self.assertFalse(state['mfa_enabled']);self.assertEqual(state['security_revision'],0);self.assertFalse(state['password_recovery_available']);self.assertFalse(self.a.user['mfa_enabled'])
 def test_02_start_setup_is_pending_and_key_is_not_readable_again(self):
  setup,_=self.command('START_ENROLLMENT');self.assertEqual(len(setup['secret']),32);self.assertIn('otpauth://totp/TaxDesk:',setup['otpauth_uri']);state=self.view();self.assertTrue(state['pending_enrollment']);self.assertFalse(state['mfa_enabled']);self.assertNotIn(setup['secret'],json.dumps(state));self.assertNotIn('setup_id',state)
 def test_03_setup_confirmation_needs_password_and_valid_code(self):
  setup,_=self.command('START_ENROLLMENT');self.command('CONFIRM_ENROLLMENT','TOTP','bad',setup_id=setup['setup_id'],status=401);p=self.payload('CONFIRM_ENROLLMENT','TOTP',otp(setup['secret']),setup_id=setup['setup_id']);p['password']='wrong-password';self.send(p,401);self.assertFalse(self.view()['mfa_enabled'])
 def test_04_confirmation_rotates_session_and_only_then_enables(self):
  setup,_=self.command('START_ENROLLMENT');old=self.a.csrf;result,h=self.command('CONFIRM_ENROLLMENT','TOTP',otp(setup['secret']),setup_id=setup['setup_id']);self.assertTrue(self.view()['mfa_enabled']);self.assertNotEqual(old,self.a.csrf);self.assertIn('HttpOnly',h['Set-Cookie']);self.assertEqual(len(set(result['recovery_codes'])),10);self.assertTrue(result['other_sessions_revoked']);self.assertNotIn('secret',result)
 def test_05_password_challenge_has_no_read_or_write_authority(self):
  self.enroll();pending,r,_=self.challenge();pending.call('/api/cases',status=401);pending.call('/api/security',status=401);pending.call('/api/cases','POST',dict(title='denied',request_id=str(uuid.uuid4())),status=401);self.assertIsNone(pending.bootstrap()[0]['user']);self.assertTrue(pending.bootstrap()[0]['mfa_required'])
 def test_06_factor_failures_and_injected_identity_cannot_authenticate(self):
  secret,_=self.enroll();client,_,_=self.challenge();factor_login(client,'bad',method='TOTP',status=401);client.call('/api/login-factor','POST',dict(method='TOTP',code=otp(secret,1),actor_id=self.a.user['id']),status=400);client.call('/api/cases',status=401)
 def test_07_independent_fresh_totp_finishes_sign_in_and_rotates_cookie(self):
  secret,_=self.enroll();client,r,h=self.challenge();csrf=r['csrf_token'];cookie=h['Set-Cookie'];result,h=factor_login(client,otp(secret,1),'TOTP');self.assertEqual(result['user']['username'],self.username);self.assertFalse(result['mfa_required']);self.assertNotEqual(result['csrf_token'],csrf);self.assertNotEqual(h['Set-Cookie'],cookie);client.call('/api/cases')
 def test_08_one_recovery_code_authenticates_only_once(self):
  _,codes=self.enroll();one,_,_=self.challenge();factor_login(one,codes[0]);two,_,_=self.challenge();factor_login(two,codes[0],status=401);self.assertEqual(self.view()['recovery_codes_remaining'],9);self.assertTrue(self.view()['mfa_enabled'])
 def test_09_same_totp_is_consumed_globally_across_challenges(self):
  secret,_=self.enroll();clients=[self.challenge()[0] for _ in range(3)];code=otp(secret,1)
  with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:results=list(pool.map(lambda c:raw_factor(c,code),clients))
  self.assertEqual(sorted(results),[200,401,401])
 def test_10_recovery_code_race_creates_only_one_authenticated_session(self):
  _,codes=self.enroll();clients=[self.challenge()[0] for _ in range(3)]
  with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:statuses=list(pool.map(lambda c:raw_factor(c,codes[0],'RECOVERY_CODE'),clients))
  self.assertEqual(sorted(statuses),[200,401,401]);self.assertEqual(self.view()['recovery_codes_remaining'],9)
 def test_11_stale_settings_and_injection_preserve_codes(self):
  _,codes=self.enroll();p=self.payload('REVOKE_OTHER_SESSIONS','RECOVERY_CODE',codes[0]);p['expected_security_revision']=0;self.send(p,409);p=self.payload('DISABLE_MFA','RECOVERY_CODE',codes[0]);p['mfa_enabled']=False;self.send(p,400);self.assertEqual(self.view()['recovery_codes_remaining'],10)
 def test_12_revocation_denies_old_sessions_and_pending_password_proofs(self):
  _,codes=self.enroll();old,_,_=self.challenge();factor_login(old,codes[0]);pending,_,_=self.challenge();self.command('REVOKE_OTHER_SESSIONS','RECOVERY_CODE',codes[1]);self.a.call('/api/cases');old.call('/api/cases',status=401);factor_login(pending,codes[2],status=401);fresh,_,_=self.challenge();factor_login(fresh,codes[2])
 def test_13_password_change_revokes_sessions_and_rejects_old_password(self):
  _,codes=self.enroll();old,_,_=self.challenge();factor_login(old,codes[0]);new='Synthetic-changed-'+str(uuid.uuid4());self.command('CHANGE_PASSWORD','RECOVERY_CODE',codes[1],new_password=new);old.call('/api/cases',status=401);c=H.Client();c.bootstrap();c.call('/api/login','POST',dict(username=self.username,password=self.password),status=401);self.password=new;c,_,_=self.challenge();factor_login(c,codes[2])
 def test_14_regeneration_invalidates_all_old_recovery_codes(self):
  _,old=self.enroll();result,_=self.command('REGENERATE_CODES','RECOVERY_CODE',old[0]);new=result['recovery_codes'];self.assertEqual(len(set(new)),10);client,_,_=self.challenge();factor_login(client,old[1],status=401);factor_login(client,new[0]);self.assertEqual(self.view()['recovery_codes_remaining'],9)
 def test_15_disable_requires_an_unused_factor_and_releases_password_sign_in(self):
  _,codes=self.enroll();self.command('DISABLE_MFA',status=401);self.command('DISABLE_MFA','RECOVERY_CODE',codes[0]);self.assertFalse(self.view()['mfa_enabled']);c=H.Client();r,_=password_login(c,self.username,self.password);self.assertFalse(r['mfa_required']);self.assertEqual(r['user']['username'],self.username)
 def test_16_replacing_authenticator_preserves_old_until_confirmed(self):
  secret,codes=self.enroll();setup,_=self.command('START_ENROLLMENT','RECOVERY_CODE',codes[0]);client,_,_=self.challenge();factor_login(client,otp(secret,1),'TOTP');result,_=self.command('CONFIRM_ENROLLMENT','TOTP',otp(setup['secret']),setup_id=setup['setup_id']);client.call('/api/cases',status=401);pending,_,_=self.challenge();factor_login(pending,codes[1],status=401);factor_login(pending,result['recovery_codes'][0])
 def test_17_cancellation_invalidates_pending_setup_id(self):
  setup,_=self.command('START_ENROLLMENT');self.command('CANCEL_ENROLLMENT');self.command('CONFIRM_ENROLLMENT','TOTP',otp(setup['secret']),setup_id=setup['setup_id'],status=409);self.assertFalse(self.view()['mfa_enabled'])
 def test_18_factor_throttle_survives_process_restart(self):
  _,codes=self.enroll();pending,_,_=self.challenge()
  for i in range(6):factor_login(pending,'invalid',status=429 if i==5 else 401)
  H.stop();H.start();pending,_,_=self.challenge();factor_login(pending,codes[0],status=429);pending.call('/api/cases',status=401)
 def test_19_current_session_csrf_and_method_are_required(self):
  p=self.payload('REVOKE_OTHER_SESSIONS');self.a.call('/api/security/update','POST',p,headers={'X-CSRF-Token':'wrong'},status=403);self.a.call('/api/security/update',status=405);self.a.call('/api/security','POST',{},status=405);self.assertEqual(self.view()['security_revision'],0)
 def test_20_export_audit_and_security_reads_do_not_return_secrets(self):
  secret,codes=self.enroll();row=self.a.create();visible=json.dumps(self.a.call('/api/cases/'+row['id']+'/export')[0])+json.dumps(self.view())+json.dumps(self.a.call('/api/audit')[0])+json.dumps(self.a.bootstrap()[0]);self.assertNotIn(secret,visible)
  for code in codes:self.assertNotIn(code,visible)
  self.assertNotIn('totp_secret',visible);self.assertNotIn('recovery_hashes',visible)
 def test_21_revoke_makes_old_bootstrap_anonymous(self):
  old=H.Client().login(self.username);self.command('REVOKE_OTHER_SESSIONS');result,_=old.bootstrap();self.assertIsNone(result['user']);self.assertFalse(result['mfa_required']);old.call('/api/cases',status=401)
 def test_22_cancel_password_challenge_rotates_csrf_and_denies_old_factor(self):
  _,codes=self.enroll();pending,r,_=self.challenge();data,_=pending.call('/api/login-cancel','POST');pending.csrf=data['csrf_token'];self.assertNotEqual(pending.csrf,r['csrf_token']);factor_login(pending,codes[0],status=401);pending.call('/api/cases',status=401)
 def test_23_replaced_enrollment_and_password_policy_are_explicit(self):
  old,_=self.command('START_ENROLLMENT');self.command('START_ENROLLMENT');self.command('CONFIRM_ENROLLMENT','TOTP',otp(old['secret']),setup_id=old['setup_id'],status=409);self.command('CHANGE_PASSWORD',new_password=self.password,status=400);self.command('CHANGE_PASSWORD',new_password='short',status=400)
 def test_24_security_asset_has_self_only_csp_and_current_version(self):
  script,h=self.a.call('/security.js');self.assertIn('completeFactor',script);self.assertIn("script-src 'self'",h['Content-Security-Policy']);self.assertEqual(self.a.call('/api/health')[0]['version'],'5.1.0')
 @unittest.skipUnless(os.environ.get('TAX_TEST_STORAGE')=='mysql','Two-node SQL fixture only; local vault is a single writer.')
 def test_25_two_application_processes_enforce_shared_epoch_and_used_codes(self):
  _,codes=self.enroll();spec=importlib.util.spec_from_file_location('security_second_node',ROOT/'test_http.py');other=importlib.util.module_from_spec(spec);spec.loader.exec_module(other);other.PASSWORD=H.PASSWORD;other.PROFILE_ARGS=list(H.PROFILE_ARGS);other.SERVER_ARGS=list(H.SERVER_ARGS)
  with tempfile.TemporaryDirectory(prefix='security-second-node-') as name:
   other.DATA=Path(name);other.start()
   try:
    client=other.Client();r,_=password_login(client,self.username,self.password);self.assertTrue(r['mfa_required']);factor_login(client,codes[0]);client.call('/api/cases');pending=other.Client();password_login(pending,self.username,self.password);self.command('REVOKE_OTHER_SESSIONS','RECOVERY_CODE',codes[1]);client.call('/api/cases',status=401);factor_login(pending,codes[2],status=401);self.assertEqual(self.view()['recovery_codes_remaining'],8)
   finally:other.stop()

def run_existing(http):
 global H
 H=http;Security.seq=0;result=unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(Security));profile=http.PROFILE_ARGS[http.PROFILE_ARGS.index('--storage')+1] if http.PROFILE_ARGS else 'local';report={'security_http_groups':result.testsRun,'failures':len(result.failures),'errors':len(result.errors),'skipped':len(result.skipped),'storage_profile':profile,'independent_python_totp':True,'two_process_sql_tested':bool(http.PROFILE_ARGS),'jar_sha256':hashlib.sha256((ROOT/'build/tax-platform-startup-5.1.0.jar').read_bytes()).hexdigest(),'source_inputs':{'test_security.py':hashlib.sha256(Path(__file__).read_bytes()).hexdigest()},'conformance_certification':False,'production_ready':False};(ROOT/'build'/('security_'+profile.replace('-','_')+'_http_test_results.json')).write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report));return result.wasSuccessful()
if __name__=='__main__':
 spec=importlib.util.spec_from_file_location('security_http_base',ROOT/'test_http.py');http=importlib.util.module_from_spec(spec);spec.loader.exec_module(http)
 with tempfile.TemporaryDirectory(prefix='tax-security-http-') as name:
  http.DATA=Path(name);http.start()
  try:ok=run_existing(http)
  finally:http.stop()
 sys.exit(0 if ok else 1)
