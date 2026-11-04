#!/usr/bin/env python3
"""Authenticated browser checks against real Java APIs, optionally on an existing disposable SQL fixture."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,subprocess,sys,tempfile,threading,time,urllib.request
ROOT=Path(__file__).resolve().parent

def module(name,file):
 spec=importlib.util.spec_from_file_location(name,file);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m

def run_existing(http,folder,*,chromium,playwright,node,native_python,native_schema_dir,output_dir):
 ops=module('browser_operations',ROOT/'test_operations.py');provider=ops.Provider(folder);registry=ops.Registry(folder);old_args=list(http.SERVER_ARGS);output_dir.mkdir(parents=True,exist_ok=True)
 http.stop();http.SERVER_ARGS=old_args+['--payment-config',str(provider.config),'--professional-registry',str(registry.path),'--native-python',str(native_python.absolute()),'--native-schema-dir',str(native_schema_dir.resolve())];http.start();failures=[]
 try:
  admin=http.Client().login('ops_admin');staff=http.Client().login('team_staff');registry.set(staff.user['id']);ops.Operations.admin=admin;ops.Operations.personal=ops.Operations.publish('CUSTOMER_PREPARATION');time.sleep(1.1)
  config=folder/'browser.json';order_file=folder/'order.json';result=output_dir/'browser_result.json';ops.private_json(config,{'base':http.BASE,'password':http.PASSWORD,'staff_id':staff.user['id'],'chromium':str(chromium.resolve()),'playwright':str(playwright.resolve()),'screenshot_dir':str(output_dir.resolve()),'result':str(result.resolve()),'order_file':str(order_file)})
  stop=threading.Event()
  def webhook():
   while not stop.wait(.1):
    if not order_file.exists():continue
    try:
     order=json.loads(order_file.read_text());pid=provider.capture(order);raw=provider.event(pid);headers=provider.headers(raw);headers={k:v for k,v in headers.items() if v is not None};request=urllib.request.Request(http.BASE+'/api/payments/webhook',data=raw,headers=headers,method='POST')
     with urllib.request.urlopen(request,timeout=10) as response:assert response.status==200
    except Exception as e:failures.append(type(e).__name__)
    return
  watcher=threading.Thread(target=webhook,daemon=True);watcher.start();run=subprocess.run([str(node.resolve()),str(ROOT/'browser_checks.mjs'),str(config)],timeout=240);stop.set();watcher.join(timeout=12)
  if run.returncode or failures:raise SystemExit('Browser checks failed; inspect the last PASS and local report.')
  report=json.loads(result.read_text());report['jar_sha256']=hashlib.sha256((ROOT/'build/tax-platform-startup-5.1.0.jar').read_bytes()).hexdigest();report['browser_binary_sha256']=hashlib.sha256(chromium.read_bytes()).hexdigest();report['playwright_version']=json.loads((playwright.parent/'package.json').read_text())['version'];report['storage_profile']=http.PROFILE_ARGS[http.PROFILE_ARGS.index('--storage')+1] if http.PROFILE_ARGS else 'local';report['test_sources']={n:hashlib.sha256((ROOT/n).read_bytes()).hexdigest() for n in ['test_browser.py','browser_checks.mjs','test_operations.py','test_security.py']};(ROOT/'build/browser_test_results.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))

 finally:
  http.stop();provider.close();http.SERVER_ARGS=old_args;http.start()

if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--chromium',type=Path,required=True);p.add_argument('--playwright',type=Path,required=True);p.add_argument('--node',type=Path,required=True);p.add_argument('--native-python',type=Path,required=True);p.add_argument('--native-schema-dir',type=Path,required=True);p.add_argument('--output-dir',type=Path,required=True);a=p.parse_args();http=module('browser_base_http',ROOT/'test_http.py')
 with tempfile.TemporaryDirectory(prefix='tax-browser-check-') as name:
  folder=Path(name);http.DATA=folder/'data'
  for username,role in [('ops_admin','ADMIN'),('team_staff','PREPARER')]:subprocess.run([sys.executable,str(ROOT/'run.py'),'--data-dir',str(http.DATA),'--operator-json'],input=json.dumps({'username':username,'display_name':'Synthetic '+role,'password':http.PASSWORD,'role':role}).encode(),check=True,stdout=subprocess.PIPE)
  http.start()
  try:run_existing(http,folder,chromium=a.chromium,playwright=a.playwright,node=a.node,native_python=a.native_python,native_schema_dir=a.native_schema_dir,output_dir=a.output_dir)
  finally:http.stop()
