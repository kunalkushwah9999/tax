#!/usr/bin/env python3
"""Real HTTP customer erasure, proof/receipt isolation and SQL peer revocation."""
from pathlib import Path
import base64, hashlib, importlib.util, json, secrets, sys, tempfile, unittest
ROOT=Path(__file__).resolve().parent
H=None
class Privacy(unittest.TestCase):
 seq=0
 def customer(self):
  Privacy.seq+=1; name='privacy_http_'+str(Privacy.seq);c=H.Client();c.register(name);return c,name
 def proof(self,c,**updates):
  token=base64.urlsafe_b64encode(secrets.token_bytes(32)).decode().rstrip('=')
  body=dict(expected_security_revision=c.call('/api/security')[0]['security_revision'],password=H.PASSWORD,factor_method='',factor_code='',confirm='DELETE MY ACCOUNT',receipt_token=token);body.update(updates);return body
 def receipt(self,token):
  c=H.Client();c.bootstrap();return c.call('/api/account-deletion/status','POST',dict(receipt_token=token))[0]
 def test_01_public_web_path_requires_no_login_or_app_install(self):
  c=H.Client();page,h=c.call('/account-deletion');self.assertIn('Account deletion',page);self.assertIn('without reinstalling',page);self.assertEqual(h['Cache-Control'],'no-store');c.call('/deletion-status.js')
 def test_02_private_plan_requires_authentication(self):
  c=H.Client();c.bootstrap();c.call('/api/account-deletion',status=401)
 def test_03_confirmation_and_extra_actor_fields_cannot_target_another_account(self):
  c,_=self.customer();body=self.proof(c,confirm='DELETE');c.call('/api/account-deletion/erase','POST',body,status=400);body=self.proof(c,other_actor='another');c.call('/api/account-deletion/erase','POST',body,status=400);self.assertTrue(c.call('/api/bootstrap')[0]['user'])
 def test_04_wrong_password_preserves_owned_draft(self):
  c,_=self.customer();row=c.create();c.call('/api/account-deletion/erase','POST',self.proof(c,password='wrong-password'),status=401);self.assertEqual(c.case(row['id'])['id'],row['id'])
 def test_05_erase_does_not_bypass_csrf_or_origin(self):
  c,_=self.customer();body=self.proof(c);c.call('/api/account-deletion/erase','POST',body,status=403,headers={'X-CSRF-Token':'wrong'});c.call('/api/account-deletion/erase','POST',body,status=403,headers={'Origin':'https://wrong.invalid'})
 def test_06_erasure_ends_older_sessions_and_removes_owned_resources(self):
  c,name=self.customer();row=c.create();older=H.Client().login(name);body=self.proof(c);result,headers=c.call('/api/account-deletion/erase','POST',body);self.assertEqual(result['status'],'ACTIVE_DATA_REMOVED');self.assertIn('Max-Age=0',headers['Set-Cookie']);older.call('/api/cases',status=401);c.call('/api/cases',status=401);self.assertEqual(self.receipt(body['receipt_token'])['removed_counts']['cases'],1);new=H.Client();new.bootstrap();new.call('/api/login','POST',dict(username=name,password=H.PASSWORD),status=401)
 def test_07_unrelated_customer_data_survives(self):
  one,_=self.customer();two,_=self.customer();first=one.create();second=two.create();one.call('/api/account-deletion/erase','POST',self.proof(one));self.assertEqual(two.case(second['id'])['id'],second['id']);two.call('/api/cases/'+first['id'],status=404)
 def test_08_receipt_is_secret_scoped_and_contains_no_identity(self):
  c,name=self.customer();actor=c.user['id'];body=self.proof(c);c.call('/api/account-deletion/erase','POST',body);result=self.receipt(body['receipt_token']);encoded=json.dumps(result);self.assertNotIn(name,encoded);self.assertNotIn(actor,encoded);self.assertFalse(result['external_and_backup_erasure_confirmed']);self.assertNotIn(body['receipt_token'],encoded);anon=H.Client();anon.bootstrap();anon.call('/api/account-deletion/status','POST',dict(receipt_token=self.proof_for_token()),status=404)
 def proof_for_token(self):return base64.urlsafe_b64encode(secrets.token_bytes(32)).decode().rstrip('=')
 def test_09_unknown_or_malformed_receipt_never_returns_success(self):
  c=H.Client();c.bootstrap();c.call('/api/account-deletion/status','POST',dict(receipt_token='short'),status=400);c.call('/api/account-deletion/status','POST',dict(receipt_token=self.proof_for_token()),status=404);c.call('/api/account-deletion/status','POST',dict(receipt_token=self.proof_for_token()),status=403,headers={'X-CSRF-Token':'wrong'})
 def test_10_mfa_is_required_for_erasure(self):
  import test_security
  c,_=self.customer();setup,_=c.call('/api/security/update','POST',dict(operation='START_ENROLLMENT',expected_security_revision=0,password=H.PASSWORD,factor_method='',factor_code=''));c.csrf=setup['csrf_token'];enabled,_=c.call('/api/security/update','POST',dict(operation='CONFIRM_ENROLLMENT',expected_security_revision=1,password=H.PASSWORD,factor_method='TOTP',factor_code=test_security.otp(setup['secret']),setup_id=setup['setup_id']));c.csrf=enabled['csrf_token'];body=self.proof(c);c.call('/api/account-deletion/erase','POST',body,status=401);body.update(factor_method='RECOVERY_CODE',factor_code=enabled['recovery_codes'][0]);result,_=c.call('/api/account-deletion/erase','POST',body);self.assertEqual(result['status'],'ACTIVE_DATA_REMOVED')
 def test_11_stale_revision_keeps_account_active(self):
  c,_=self.customer();c.call('/api/account-deletion/erase','POST',self.proof(c,expected_security_revision=999),status=409);self.assertTrue(c.call('/api/bootstrap')[0]['user'])
 def test_12_receipt_and_deletion_survive_restart(self):
  c,name=self.customer();body=self.proof(c);c.call('/api/account-deletion/erase','POST',body);H.stop();H.start();self.assertEqual(self.receipt(body['receipt_token'])['status'],'ACTIVE_DATA_REMOVED');c=H.Client();c.bootstrap();c.call('/api/login','POST',dict(username=name,password=H.PASSWORD),status=401)
 def test_13_sql_peer_rejects_session_after_erasure_on_another_node(self):
  if not H.PROFILE_ARGS:self.skipTest('Two nodes require a shared SQL profile')
  c,name=self.customer();spec=importlib.util.spec_from_file_location('privacy_peer',ROOT/'test_http.py');peer=importlib.util.module_from_spec(spec);spec.loader.exec_module(peer);peer.PASSWORD=H.PASSWORD;peer.PROFILE_ARGS=list(H.PROFILE_ARGS)
  with tempfile.TemporaryDirectory(prefix='privacy-peer-') as temp:
   peer.DATA=Path(temp);peer.start()
   try:
    other=peer.Client().login(name);c.call('/api/account-deletion/erase','POST',self.proof(c));other.call('/api/cases',status=401);other.call('/api/security',status=401)
   finally:peer.stop()
def run_existing(http):
 global H
 H=http;Privacy.seq=0;result=unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(Privacy));profile=http.PROFILE_ARGS[http.PROFILE_ARGS.index('--storage')+1] if http.PROFILE_ARGS else 'local'
 report={'privacy_http_groups':result.testsRun,'failures':len(result.failures),'errors':len(result.errors),'skipped':len(result.skipped),'storage_profile':profile,'actual_http':True,'two_process_sql_tested':bool(http.PROFILE_ARGS),'jar_sha256':hashlib.sha256((ROOT/'build/tax-platform-startup-5.1.0.jar').read_bytes()).hexdigest(),'test_sources':{'test_privacy.py':hashlib.sha256(Path(__file__).read_bytes()).hexdigest()},'production_privacy_approved':False,'production_ready':False}
 (ROOT/'build'/('privacy_'+profile.replace('-','_')+'_http_test_results.json')).write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report));return result.wasSuccessful()
def main():
 spec=importlib.util.spec_from_file_location('privacy_http',ROOT/'test_http.py');http=importlib.util.module_from_spec(spec);spec.loader.exec_module(http)
 with tempfile.TemporaryDirectory(prefix='taxdesk-privacy-http-') as temp:
  http.DATA=Path(temp);http.start()
  try:return 0 if run_existing(http) else 1
  finally:http.stop()
if __name__=='__main__':sys.exit(main())
