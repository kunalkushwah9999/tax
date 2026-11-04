#!/usr/bin/env python3
"""Real HTTPS client/proxy/backend tests with a disposable, explicitly trusted test CA."""
from pathlib import Path
from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer
import hashlib,http.client,http.cookiejar,importlib.util,json,queue,socket,ssl,subprocess,sys,tempfile,threading,unittest,urllib.error,urllib.request
ROOT=Path(__file__).resolve().parent;JAR=ROOT/'build/tax-platform-startup-5.1.0.jar'
BACKEND_PORT=0
class Proxy(BaseHTTPRequestHandler):
 def log_message(self,*args):pass
 def do_GET(self):self.forward()
 def do_POST(self):self.forward()
 def forward(self):
  connection=http.client.HTTPConnection('127.0.0.1',BACKEND_PORT,timeout=10)
  try:
   body=self.rfile.read(int(self.headers.get('Content-Length','0')));headers={k:v for k,v in self.headers.items() if k.lower() not in ['connection','proxy-connection','transfer-encoding']};connection.request(self.command,self.path,body=body or None,headers=headers);response=connection.getresponse();data=response.read();self.send_response(response.status)
   for key,value in response.getheaders():
    if key.lower() not in ['connection','transfer-encoding','content-length','server','date']:self.send_header(key,value)
   self.send_header('Content-Length',str(len(data)));self.end_headers();self.wfile.write(data)
  finally:connection.close()
class Tests(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.client=SecureClient();cls.client.register('https_owner')
 def test_01_untrusted_certificate_rejects_and_explicit_test_ca_succeeds(self):
  with self.assertRaises(urllib.error.URLError):urllib.request.urlopen(MODULE.BASE+'/api/health',timeout=5)
  self.assertEqual(self.client.call('/api/health')[0]['status'],'OK')
 def test_02_bootstrap_emits_host_secure_cookie_and_hsts(self):
  client=SecureClient();data,h=client.bootstrap();cookie=h['Set-Cookie'];self.assertTrue(cookie.startswith('__Host-tax_startup_session='));self.assertIn('; Secure',cookie);self.assertIn('HttpOnly',cookie);self.assertIn('SameSite=Strict',cookie);self.assertNotIn('Domain=',cookie);self.assertIn('max-age=',h['Strict-Transport-Security'])
 def test_03_login_rotates_csrf_and_https_session_cookie(self):
  client=SecureClient();old,h=client.bootstrap();cookie=h['Set-Cookie'];data,h=client.call('/api/login','POST',{'username':'https_owner','password':MODULE.PASSWORD});self.assertNotEqual(cookie,h['Set-Cookie']);self.assertNotEqual(old['csrf_token'],data['csrf_token']);self.assertIn('; Secure',h['Set-Cookie'])
 def test_04_authenticated_proposal_confirmation_over_https(self):
  row=self.client.create();row=self.client.edit(row,'PROPOSE',field='SALARY',text='8 lakh',unit='');self.assertEqual(row['inputs']['SALARY']['state'],'ESTIMATED');row=self.client.edit(row,'CONFIRM',field='SALARY',amount='800000.00');self.assertEqual(row['inputs']['SALARY']['state'],'CONFIRMED');self.assertFalse(row['filing_available'])
 def test_05_https_front_door_still_rejects_wrong_host_and_origin(self):
  self.client.call('/api/bootstrap',headers={'Host':'wrong.invalid'},status=421);self.client.call('/api/bootstrap',headers={'Origin':'https://wrong.invalid'},status=403)
 def test_06_forwarded_proto_does_not_override_the_configured_origin(self):
  payload={'title':'Denied spoof','request_id':'https-proto-spoof'};self.client.call('/api/cases','POST',payload,headers={'Origin':MODULE.BASE.replace('https:','http:'),'X-Forwarded-Proto':'https','X-Forwarded-Host':'localhost'},status=403)
 def test_07_https_does_not_bypass_csrf(self):
  self.client.call('/api/cases','POST',{'title':'Denied csrf','request_id':'https-csrf-test'},headers={'X-CSRF-Token':'wrong'},status=403)
 def test_08_private_apis_do_not_accept_an_unauthenticated_direct_http_request(self):
  request=urllib.request.Request('http://127.0.0.1:'+str(BACKEND_PORT)+'/api/cases',headers={'Host':'localhost:'+str(TLS_PORT)})
  with self.assertRaises(urllib.error.HTTPError) as raised:urllib.request.urlopen(request,timeout=5)
  self.assertEqual(raised.exception.code,401)
 def test_09_existing_dual_engine_and_result_guard_stay_blocked_over_https(self):
  helper=MODULE.Integration('test_12_real_existing_engine_and_guard_return_readiness_without_tax_values');helper.a=self.client;row=helper.prepared();data,_=self.client.call('/api/cases/'+row['id']+'/assess','POST');self.assertEqual(data['result']['status'],'BLOCKED');self.assertTrue(all(item['input_status']=='CALCULATED' for item in data['checks'].values()));self.assertFalse(data['result']['capabilities']['submit_return']);self.assertFalse(data['calculation_values_released'])
 def test_10_cache_csp_and_export_boundaries_survive_the_proxy(self):
  row=self.client.create();data,h=self.client.call('/api/cases/'+row['id']+'/export');self.assertEqual(h['Cache-Control'],'no-store');self.assertIn("frame-ancestors 'none'",h['Content-Security-Policy']);self.assertEqual(data['format'],'PREPARATION_DATA_EXPORT_NOT_ITR')
 def test_11_browser_supplied_authority_fields_cannot_change_hosting_or_tax_policy(self):
  self.client.call('/api/cases','POST',{'title':'No override','request_id':'https-authority-test','public_origin':MODULE.BASE,'rule_state':'APPROVED'},status=400)
 def test_12_https_logout_revokes_the_cookie_and_old_session(self):
  self.client.call('/api/logout','POST');self.client.call('/api/cases',status=401)
 def test_13_https_operators_must_enroll_then_complete_mfa(self):
  import test_security
  client=SecureClient();client.bootstrap();result,_=client.call('/api/login','POST',dict(username='https_operator',password=MODULE.PASSWORD));client.csrf=result['csrf_token'];client.user=result['user'];self.assertTrue(result['operator_mfa_enrollment_required']);client.call('/api/admin',status=403);view=client.call('/api/security')[0];self.assertTrue(view['operator_mfa_required'])
  payload=dict(operation='START_ENROLLMENT',expected_security_revision=view['security_revision'],password=MODULE.PASSWORD,factor_method='',factor_code='');setup,_=client.call('/api/security/update','POST',payload);client.csrf=setup['csrf_token'];payload=dict(operation='CONFIRM_ENROLLMENT',expected_security_revision=setup['security']['security_revision'],password=MODULE.PASSWORD,factor_method='TOTP',factor_code=test_security.otp(setup['secret']),setup_id=setup['setup_id']);confirmed,h=client.call('/api/security/update','POST',payload);client.csrf=confirmed['csrf_token'];self.assertIn('; Secure',h['Set-Cookie']);client.call('/api/admin');client.call('/api/security/update','POST',dict(operation='DISABLE_MFA',expected_security_revision=confirmed['security']['security_revision'],password=MODULE.PASSWORD,factor_method='RECOVERY_CODE',factor_code=confirmed['recovery_codes'][0]),status=403);client.call('/api/logout','POST');client.bootstrap();pending,_=client.call('/api/login','POST',dict(username='https_operator',password=MODULE.PASSWORD));client.csrf=pending['csrf_token'];self.assertTrue(pending['mfa_required']);self.assertIsNone(pending['user']);client.call('/api/admin',status=401);authenticated,h=client.call('/api/login-factor','POST',dict(method='RECOVERY_CODE',code=confirmed['recovery_codes'][0]));client.csrf=authenticated['csrf_token'];self.assertIn('; Secure',h['Set-Cookie']);client.call('/api/admin')
def main():
 global MODULE,SecureClient,BACKEND_PORT,TLS_PORT
 with tempfile.TemporaryDirectory(prefix='tax-https-proxy-') as folder:
  folder=Path(folder);cert,key=folder/'cert.pem',folder/'key.pem';built=subprocess.run(['openssl','req','-x509','-newkey','rsa:2048','-nodes','-days','2','-keyout',str(key),'-out',str(cert),'-subj','/CN=localhost','-addext','subjectAltName=DNS:localhost'],capture_output=True,text=True)
  if built.returncode:raise SystemExit('Test-only certificate generation failed.')
  context=ssl.create_default_context(cafile=str(cert));proxy=ThreadingHTTPServer(('127.0.0.1',0),Proxy);TLS_PORT=proxy.server_port;ssl_server=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER);ssl_server.minimum_version=ssl.TLSVersion.TLSv1_2;ssl_server.load_cert_chain(cert,key);proxy.socket=ssl_server.wrap_socket(proxy.socket,server_side=True)
  with socket.socket() as sock:sock.bind(('127.0.0.1',0));BACKEND_PORT=sock.getsockname()[1]
  spec=importlib.util.spec_from_file_location('https_http',ROOT/'test_http.py');MODULE=importlib.util.module_from_spec(spec);spec.loader.exec_module(MODULE);MODULE.BASE='https://localhost:'+str(TLS_PORT)
  class CAClient(MODULE.Client):
   def __init__(self):
    super().__init__();self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookies),urllib.request.HTTPSHandler(context=context))
  SecureClient=CAClient
  subprocess.run([sys.executable,str(ROOT/'run.py'),'--data-dir',str(folder/'records'),'--operator-json'],input=json.dumps(dict(username='https_operator',display_name='Synthetic HTTPS operator',password=MODULE.PASSWORD,role='ADMIN')).encode(),check=True,capture_output=True)
  process=subprocess.Popen([sys.executable,str(ROOT/'run.py'),'--port',str(BACKEND_PORT),'--data-dir',str(folder/'records'),'--public-origin',MODULE.BASE],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True);q=queue.Queue()
  def read():
   for line in process.stdout:
    if line.startswith('STARTUP_URL='):q.put(line.strip());return
  threading.Thread(target=read,daemon=True).start()
  try:
   q.get(timeout=15);threading.Thread(target=proxy.serve_forever,daemon=True).start();result=unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(Tests));report={'https_proxy_groups':result.testsRun,'failures':len(result.failures),'errors':len(result.errors),'actual_tls_and_authenticated_http':True,'secure_host_cookie':True,'client_uses_explicit_disposable_ca':True,'nginx_runtime_tested':False,'browser_transaction_flow_tested':False,'jar_sha256':hashlib.sha256(JAR.read_bytes()).hexdigest(),'production_deployed':False};(ROOT/'build/https_test_results.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2));return 0 if result.wasSuccessful() else 1
  finally:
   proxy.shutdown();proxy.server_close();process.terminate()
   try:process.wait(timeout=5)
   except subprocess.TimeoutExpired:process.kill();process.wait(timeout=5)
if __name__=='__main__':raise SystemExit(main())
