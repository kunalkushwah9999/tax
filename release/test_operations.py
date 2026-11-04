#!/usr/bin/env python3
"""Real TLS + Java deployment probes; Android doctor contracts without an Android SDK."""
from pathlib import Path
from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer
import hashlib,http.client,importlib.util,json,mimetypes,os,queue,socket,ssl,subprocess,sys,tempfile,threading,unittest
import prepare,verify_site,android_build

class Operations(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.temp=tempfile.TemporaryDirectory(prefix='taxdesk-ops-');cls.folder=Path(cls.temp.name);cls.mode='normal';cls.secrets=[]
  cls.web=cls.folder/'bundle';prepare.render(prepare.configuration('https://pilot.taxdesk.test','in.taxdesk.pilot','support@taxdesk.test',['AB:'*31+'AB']),cls.web)
  with socket.socket() as s:s.bind(('127.0.0.1',0));cls.backend=s.getsockname()[1]
  outer=cls
  class Proxy(BaseHTTPRequestHandler):
   def log_message(self,*args):pass
   def do_GET(self):self.forward()
   def do_POST(self):self.forward()
   def forward(self):
    if self.path.startswith('/api/'):
     if outer.mode=='private-open' and self.path=='/api/cases':self.send_response(200);data=b'{}';self.send_header('Content-Type','application/json')
     elif outer.mode=='bad-version' and self.path=='/api/health':self.send_response(200);data=b'{"status":"OK","version":"old","production_ready":false}';self.send_header('Content-Type','application/json')
     elif outer.mode=='malformed-bootstrap' and self.path=='/api/bootstrap':self.send_response(200);data=b'{"user":null,"csrf_token":{}}';self.send_header('Content-Type','application/json')
     else:
      c=http.client.HTTPConnection('127.0.0.1',outer.backend,timeout=10)
      try:
       c.request(self.command,self.path,body=self.rfile.read(int(self.headers.get('Content-Length','0'))) or None,headers={k:v for k,v in self.headers.items() if k.lower() not in ['connection','transfer-encoding']});r=c.getresponse();data=r.read();self.send_response(r.status)
       for k,v in r.getheaders():
        if k.lower() not in ['connection','content-length','transfer-encoding','server','date']:self.send_header(k,v)
        if k.lower()=='set-cookie':outer.secrets.append(v.split(';',1)[0].split('=',1)[1])
       if self.path=='/api/bootstrap' and r.status==200:outer.secrets.append(json.loads(data)['csrf_token'])
      finally:c.close()
    else:
     name=self.path
     if name=='/':name='/index.html'
     if name=='/account-deletion':name='/account-deletion.html'
     path=outer.web/'web'/name.lstrip('/')
     if not path.is_file():self.send_error(404);return
     data=path.read_bytes();self.send_response(302 if outer.mode=='redirect-root' and self.path=='/' else 200)
     if outer.mode=='malformed-links' and self.path=='/.well-known/assetlinks.json':data=b'[{"relation":null,"target":{"sha256_cert_fingerprints":123}}]'
     if outer.mode=='malformed-manifest' and self.path=='/manifest.webmanifest':data=b'{"start_url":"/","scope":"/","display":"standalone","icons":123}'
     if outer.mode=='redirect-root' and self.path=='/':self.send_header('Location','https://must-not-follow.invalid/')
     kind='application/manifest+json' if name.endswith('.webmanifest') else mimetypes.guess_type(name)[0] or 'application/octet-stream'
     self.send_header('Content-Type',kind);self.send_header('Cache-Control','no-store')
     for k,v in {'X-Frame-Options':'DENY','X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer'}.items():self.send_header(k,v)
     if outer.mode!='missing-csp':self.send_header('Content-Security-Policy',"default-src 'none'; worker-src 'self'; manifest-src 'self'; frame-ancestors 'none'")
    self.send_header('Content-Length',str(len(data)));self.end_headers();self.wfile.write(data)
  cls.proxy=ThreadingHTTPServer(('127.0.0.1',0),Proxy);cls.base='https://localhost:'+str(cls.proxy.server_port)
  cls.cert=cls.folder/'ca.pem';key=cls.folder/'key.pem'
  subprocess.run(['openssl','req','-x509','-newkey','rsa:2048','-nodes','-days','1','-keyout',str(key),'-out',str(cls.cert),'-subj','/CN=localhost','-addext','subjectAltName=DNS:localhost'],check=True,capture_output=True)
  context=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER);context.load_cert_chain(cls.cert,key);cls.proxy.socket=context.wrap_socket(cls.proxy.socket,server_side=True)
  cls.process=subprocess.Popen([sys.executable,str(prepare.ROOT/'startup_app/run.py'),'--port',str(cls.backend),'--data-dir',str(cls.folder/'data'),'--public-origin',cls.base],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
  ready=queue.Queue()
  def reader():
   for line in cls.process.stdout:
    if line.startswith('STARTUP_URL='):ready.put(True)
  threading.Thread(target=reader,daemon=True).start();ready.get(timeout=20);threading.Thread(target=cls.proxy.serve_forever,daemon=True).start()
 @classmethod
 def tearDownClass(cls):
  cls.proxy.shutdown();cls.proxy.server_close();cls.process.terminate()
  try:cls.process.wait(timeout=10)
  except subprocess.TimeoutExpired:cls.process.kill();cls.process.wait(timeout=5)
  cls.temp.cleanup()
 def setUp(self):Operations.mode='normal'
 def verify(self,**extra):return verify_site.verify(self.base,ca_file=self.cert,fixture=True,**extra)
 def test_01_real_java_https_public_contract(self):
  r=self.verify(application_id='in.taxdesk.pilot',fingerprint='AB:'*31+'AB');self.assertTrue(r['infrastructure_smoke_passed'],r['checks']);self.assertEqual(len(r['checks']),16);self.assertFalse(r['cloud_release_validated']);self.assertFalse(r['public_endpoint_smoke_passed']);self.assertTrue(r['peer']['trust_verified'])
 def test_02_untrusted_certificate_is_rejected(self):
  with self.assertRaises(ssl.SSLCertVerificationError):verify_site.verify(self.base,fixture=True)
 def test_03_noncanonical_or_unsafe_origin_is_rejected(self):
  for value in ['http://site.test','https://user:secret@site.test','https://site.test/','https://site.test?q=1','https://Site.test']:
   with self.subTest(value=value),self.assertRaises(ValueError):verify_site.origin(value)
  with self.assertRaises(ValueError):verify_site.origin(self.base)
  with self.assertRaises(ValueError):verify_site.Client('https://site.test',ca_file=self.cert)
 def test_04_private_api_exposure_fails_verification(self):
  Operations.mode='private-open';self.assertFalse(self.verify()['infrastructure_smoke_passed'])
 def test_05_missing_security_header_fails_verification(self):
  Operations.mode='missing-csp';self.assertFalse(self.verify()['infrastructure_smoke_passed'])
 def test_06_old_app_version_fails_verification(self):
  Operations.mode='bad-version';self.assertFalse(self.verify()['infrastructure_smoke_passed'])
 def test_07_redirect_is_never_followed(self):
  Operations.mode='redirect-root';self.assertFalse(self.verify()['infrastructure_smoke_passed'])
 def test_08_play_certificate_mismatch_fails_verification(self):
  self.assertFalse(self.verify(application_id='in.taxdesk.pilot',fingerprint='CD:'*31+'CD')['infrastructure_smoke_passed'])
 def test_09_reports_exclude_cookies_csrf_and_response_bodies(self):
  r=self.verify();text=json.dumps(r)
  for value in self.secrets:self.assertNotIn(value,text)
  self.assertNotIn('set-cookie',text);self.assertNotIn('csrf_token',text)
 def test_10_android_doctor_does_not_claim_missing_sdk_build(self):
  r=android_build.doctor(self.web/'android',self.folder/'missing-sdk','taxdesk-nonexistent-gradle')
  self.assertFalse(r['ready_for_unsigned_build']);self.assertFalse(r['signed_android_build_verified']);self.assertTrue(any('SDK_COMPONENT_MISSING' in x for x in r['issues']))
 def test_11_android_doctor_requires_expected_generated_source(self):
  r=android_build.doctor(self.folder,self.folder/'missing-sdk','taxdesk-nonexistent-gradle');self.assertIn('GENERATED_ANDROID_PROJECT_REQUIRED',r['issues'])
 def test_12_actual_clis_write_honest_reports_and_never_overwrite(self):
  out=self.folder/'site-report.json';command=[sys.executable,str(prepare.HERE/'verify_site.py'),'--origin',self.base,'--fixture','--ca-file',str(self.cert),'--out',str(out)]
  r=subprocess.run(command,capture_output=True,text=True);self.assertEqual(r.returncode,0,r.stderr);self.assertTrue(json.loads(out.read_text())['infrastructure_smoke_passed']);before=out.read_bytes()
  self.assertEqual(subprocess.run(command,capture_output=True).returncode,2);self.assertEqual(out.read_bytes(),before)
  out=self.folder/'android-report.json';r=subprocess.run([sys.executable,str(prepare.HERE/'android_build.py'),'doctor','--project',str(self.web/'android'),'--sdk',str(self.folder/'missing-sdk'),'--gradle','taxdesk-nonexistent-gradle','--out',str(out)],capture_output=True,text=True)
  self.assertEqual(r.returncode,2);report=json.loads(out.read_text());self.assertFalse(report['ready_for_unsigned_build']);self.assertFalse(report['signed_android_build_verified'])
 def test_13_malformed_remote_json_fails_without_approving_or_crashing(self):
  for mode in ['malformed-bootstrap','malformed-links','malformed-manifest']:
   Operations.mode=mode
   with self.subTest(mode=mode):self.assertFalse(self.verify(application_id='in.taxdesk.pilot',fingerprint='AB:'*31+'AB')['infrastructure_smoke_passed'])

if __name__=='__main__':
 result=unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(Operations))
 report={'operations_tool_groups':result.testsRun,'failures':len(result.failures),'errors':len(result.errors),'actual_tls_java_probes':True,'actual_android_sdk_build':False,'actual_cloud_deployment':False,'production_ready':False,'runtime_jar_sha256':hashlib.sha256((prepare.ROOT/'startup_app/build'/prepare.JAR).read_bytes()).hexdigest(),'sources':{name:hashlib.sha256((prepare.HERE/name).read_bytes()).hexdigest() for name in ['verify_site.py','android_build.py','test_operations.py']}}
 (prepare.ROOT/'operations_tool_checks.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2));sys.exit(not result.wasSuccessful())
