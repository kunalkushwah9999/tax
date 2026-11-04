#!/usr/bin/env python3
"""Disposable schema-2 concurrent MySQL, actual API, outage and framed recovery checks."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,secrets,shutil,signal,socket,subprocess,sys,tempfile,time,unittest
ROOT=Path(__file__).resolve().parent
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--mysql-basedir',type=Path,required=True);p.add_argument('--jdbc-jar',type=Path,required=True);p.add_argument('--mysql-library-path',default='');p.add_argument('--keep-private-fixture',action='store_true');p.add_argument('--skip-selected-mutations',action='store_true',default=True);p.add_argument('--native-python',type=Path);p.add_argument('--native-schema-dir',type=Path);p.add_argument('--with-concurrent-mutations',action='store_true');p.add_argument('--browser-chromium',type=Path);p.add_argument('--browser-playwright',type=Path);p.add_argument('--browser-node',type=Path);p.add_argument('--browser-output',type=Path);args=p.parse_args()
if bool(args.native_python)!=bool(args.native_schema_dir):p.error('Both optional native worker paths are required.')
mysql=args.mysql_basedir.resolve();driver=args.jdbc_jar.resolve();server=mysql/'bin/mysqld'
if not server.is_file() or not driver.is_file():p.error('An existing MySQL 8.4 distribution and official Connector/J JAR are required.')
if hashlib.sha256(driver.read_bytes()).hexdigest()!='cef70caf532833c9ab8f41f74773ce22c08bf503fad215c9c2ee1e9db87e759e':p.error('Use the pinned official Connector/J 26.10.0 JAR; review and update the pin before using another artifact.')
folder=Path(tempfile.mkdtemp(prefix='tax-mysql-test-'));os.chmod(folder,0o700)
mysql_env=os.environ.copy()
if args.mysql_library_path:mysql_env['LD_LIBRARY_PATH']=args.mysql_library_path
passwords={name:secrets.token_hex(24) for name in ['root','migrate','app','trust']};port=0;process=None;http=None
log=folder/'mysqld.log';(folder/'data').mkdir();certs=folder/'certs';certs.mkdir();database='tax_test_'+secrets.token_hex(6)
def run(command,**kwargs):
 result=subprocess.run(command,capture_output=True,text=True,**kwargs)
 if result.returncode:
  text=result.stdout+result.stderr
  for value in passwords.values():text=text.replace(value,'[redacted]')
  raise RuntimeError('Test setup/verification failed: '+text[-4000:])
 return result.stdout
def private_json(path,data):
 path.write_text(json.dumps(data));os.chmod(path,0o600)
def certificate_setup():
 run(['openssl','req','-x509','-newkey','rsa:2048','-nodes','-days','2','-keyout',str(certs/'ca.key'),'-out',str(certs/'ca.pem'),'-subj','/CN=TaxIntegrationCA'])
 run(['openssl','req','-newkey','rsa:2048','-nodes','-keyout',str(certs/'server.key'),'-out',str(certs/'server.csr'),'-subj','/CN=localhost'])
 (certs/'server.ext').write_text('subjectAltName=DNS:localhost\nextendedKeyUsage=serverAuth\n')
 run(['openssl','x509','-req','-in',str(certs/'server.csr'),'-CA',str(certs/'ca.pem'),'-CAkey',str(certs/'ca.key'),'-CAcreateserial','-days','2','-out',str(certs/'server.pem'),'-extfile',str(certs/'server.ext')])
 env=os.environ.copy();env['TAX_TEST_TRUST_PASSWORD']=passwords['trust']
 run(['keytool','-importcert','-noprompt','-alias','mysql-test-ca','-file',str(certs/'ca.pem'),'-keystore',str(certs/'trust.p12'),'-storetype','PKCS12','-storepass:env','TAX_TEST_TRUST_PASSWORD'],env=env)
 for key in certs.glob('*.key'):os.chmod(key,0o600)
def start_db(first=False):
 global process
 options=[str(server),'--no-defaults','--basedir='+str(mysql),'--datadir='+str(folder/'data'),'--bind-address=127.0.0.1','--port='+str(port),'--socket=','--pid-file='+str(folder/'mysql.pid'),'--log-error='+str(log),'--mysqlx=0','--skip-log-bin','--skip-name-resolve','--require-secure-transport=ON','--ssl-ca='+str(certs/'ca.pem'),'--ssl-cert='+str(certs/'server.pem'),'--ssl-key='+str(certs/'server.key'),'--innodb-buffer-pool-size=67108864','--innodb-redo-log-capacity=33554432','--innodb-flush-log-at-trx-commit=1','--max-connections=40','--local-infile=OFF','--secure-file-priv=NULL','--default-time-zone=+00:00']
 if hasattr(os,'geteuid') and os.geteuid()==0:options.append('--user=root')
 if first:options.append('--init-file='+str(folder/'initialize.sql'))
 stream=(folder/'server-console.log').open('a');process=subprocess.Popen(options,stdout=stream,stderr=subprocess.STDOUT,env=mysql_env);stream.close()
 until=time.monotonic()+30
 while time.monotonic()<until:
  if process.poll() is not None:
   error=log.read_text() if log.exists() else (folder/'server-console.log').read_text()
   for value in passwords.values():error=error.replace(value,'[redacted]')
   raise RuntimeError('Isolated MySQL startup failed: '+error[-3500:])
  try:
   with socket.create_connection(('127.0.0.1',port),timeout=.3):return
  except OSError:time.sleep(.1)
 raise RuntimeError('Isolated MySQL startup timed out.')
def stop_db(crash=False):
 if process and process.poll() is None:
  process.send_signal(signal.SIGKILL if crash else signal.SIGTERM)
  try:process.wait(timeout=15)
  except subprocess.TimeoutExpired:process.kill();process.wait(timeout=5)
def java_class(name,*values):
 libs=[ROOT/'build/test-classes',ROOT/'build/classes',ROOT/'build/tax-platform-startup-5.1.0.jar',driver,ROOT.parent/'java_result_safety/build/tax-result-safety-0.1.4.jar',ROOT.parent/'tax_engine/build/tax-planner-workbench-0.1.0.jar',ROOT.parent/'local_app/build/tax-preparation-local-demo-0.4.0.jar']
 return ['java','--add-modules','jdk.httpserver,java.sql','-cp',os.pathsep.join(map(str,libs)),name,*map(str,values)]
try:
 certificate_setup()
 init=[str(server),'--no-defaults','--initialize-insecure','--basedir='+str(mysql),'--datadir='+str(folder/'data')]
 if hasattr(os,'geteuid') and os.geteuid()==0:init.append('--user=root')
 run(init,env=mysql_env,timeout=40)
 with socket.socket() as sock:sock.bind(('127.0.0.1',0));port=sock.getsockname()[1]
 script=f"ALTER USER 'root'@'localhost' IDENTIFIED BY '{passwords['root']}';\nCREATE DATABASE {database} CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;\nCREATE USER 'tax_migrate'@'127.0.0.1' IDENTIFIED BY '{passwords['migrate']}' REQUIRE SSL;\nGRANT ALL ON {database}.* TO 'tax_migrate'@'127.0.0.1';\nCREATE USER 'tax_app'@'127.0.0.1' IDENTIFIED BY '{passwords['app']}' REQUIRE SSL;\nGRANT SELECT,INSERT,UPDATE,DELETE ON {database}.* TO 'tax_app'@'127.0.0.1';\n"
 
 if not args.skip_selected_mutations:
  for i in range(3):
   db=database+'_defect'+str(i);script+=f"CREATE DATABASE {db} CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;\nGRANT ALL ON {db}.* TO 'tax_migrate'@'127.0.0.1';\nGRANT SELECT,INSERT,UPDATE,DELETE ON {db}.* TO 'tax_app'@'127.0.0.1';\n"
 if args.with_concurrent_mutations:
  for at in range(4):
   variant_db=database+'_concurrent_defect'+str(at);script+=f"CREATE DATABASE {variant_db} CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;\nGRANT ALL ON {variant_db}.* TO 'tax_migrate'@'127.0.0.1';\nGRANT SELECT,INSERT,UPDATE,DELETE ON {variant_db}.* TO 'tax_app'@'127.0.0.1';\n"
 script+=f"CREATE DATABASE {database}_restore CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;\nGRANT ALL ON {database}_restore.* TO 'tax_migrate'@'127.0.0.1';\n"
 (folder/'initialize.sql').write_text(script);os.chmod(folder/'initialize.sql',0o600);start_db(True)
 common={'host':'localhost','port':port,'database':database,'trust_store':str(certs/'trust.p12'),'trust_store_password':passwords['trust']}
 admin_cfg=folder/'migration.json';app_cfg=folder/'application.json';key=folder/'keys/record.key'
 private_json(admin_cfg,dict(common,user='tax_migrate',password=passwords['migrate']));private_json(app_cfg,dict(common,user='tax_app',password=passwords['app']))
 run([sys.executable,str(ROOT/'run.py'),'--storage','mysql-concurrent','--mysql-config',str(admin_cfg),'--jdbc-jar',str(driver),'--key-file',str(key),'--init-mysql'],timeout=30)
 print('Isolated MySQL initialized; running actual SQL tests.',flush=True)
 sql_output=run(java_class('com.taxplatform.startup.ConcurrentMysqlTests',app_cfg,admin_cfg,key,ROOT/'build/concurrent_mysql_test_results.json'),timeout=120)
 print(sql_output,flush=True)
 restore_cfg=folder/'restore-target.json';restore_key=folder/'restore-target-keys/record.key';private_json(restore_cfg,dict(common,database=database+'_restore',user='tax_migrate',password=passwords['migrate']))
 output=run(java_class('com.taxplatform.startup.ConcurrentRecoveryTests',app_cfg,key,restore_cfg,restore_key,folder/'restore-check',ROOT/'build/concurrent_restore_test_results.json'),timeout=180)
 print(output,flush=True)
 os.environ.update(TAX_TEST_STORAGE='mysql',TAX_MYSQL_CONFIG=str(app_cfg),TAX_MYSQL_JDBC=str(driver),TAX_MYSQL_KEY=str(key))
 spec=importlib.util.spec_from_file_location('mysql_http',ROOT/'test_http.py');http=importlib.util.module_from_spec(spec);spec.loader.exec_module(http);http.DATA=folder/'application-local-placeholder';http.DATA.mkdir();http.PROFILE_ARGS=['--storage','mysql-concurrent','--mysql-config',str(app_cfg),'--jdbc-jar',str(driver),'--key-file',str(key)]
 for name,role in [('ops_admin','ADMIN'),('team_staff','PREPARER')]:
  run([sys.executable,str(ROOT/'run.py'),'--operator-json','--storage','mysql-concurrent','--mysql-config',str(app_cfg),'--jdbc-jar',str(driver),'--key-file',str(key)],input=json.dumps({'username':name,'display_name':'Sample '+role,'password':http.PASSWORD,'role':role}),timeout=20)
 http.start()
 if args.browser_chromium:
  if not all([args.browser_playwright,args.browser_node,args.browser_output,args.native_python,args.native_schema_dir]):raise RuntimeError('All browser/native prerequisites are required together.')
  browser_folder=folder/'browser-fixture';browser_folder.mkdir();spec=importlib.util.spec_from_file_location('concurrent_browser',ROOT/'test_browser.py');browser_module=importlib.util.module_from_spec(spec);spec.loader.exec_module(browser_module);browser_module.run_existing(http,browser_folder,chromium=args.browser_chromium.resolve(),playwright=args.browser_playwright.resolve(),node=args.browser_node.resolve(),native_python=args.native_python.absolute(),native_schema_dir=args.native_schema_dir.resolve(),output_dir=args.browser_output.resolve())
 result=unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(http.Integration))
 report={'startup_http_groups':result.testsRun,'failures':len(result.failures),'errors':len(result.errors),'skipped':len(result.skipped),'jar_sha256':hashlib.sha256((ROOT/'build/tax-platform-startup-5.1.0.jar').read_bytes()).hexdigest(),'real_http':True,'mysql_profile_exercised':True,'restart_recovery_exercised':True,'production_ready':False};(ROOT/'build/concurrent_mysql_http_test_results.json').write_text(json.dumps(report,indent=2)+'\n')
 if not result.wasSuccessful():raise RuntimeError('Actual MySQL HTTP integration failed.')
 client=http.Client().login('customer_a');before=client.call('/api/cases')[0]
 stop_db(crash=True);client.call('/api/cases',status=503);start_db(False);after=client.call('/api/cases')[0];assert before==after
 run(java_class('com.taxplatform.startup.ConcurrentMysqlRecoveryProbe',app_cfg,key),timeout=20)
 software={'mysql_server_version':run([str(server),'--version'],env=mysql_env).strip().split(' Ver ',1)[-1],'jdbc_sha256':hashlib.sha256(driver.read_bytes()).hexdigest()}
 recovery={'mysqld_sigkill_and_restart':True,'committed_marker_preserved':True,'saved_cases_preserved':True,'http_unavailable_during_database_outage':True,'no_local_storage_fallback':True,'software':software,'power_loss_tested':False,'backup_restore_tested':False,'production_ready':False};(ROOT/'build/concurrent_mysql_recovery_results.json').write_text(json.dumps(recovery,indent=2)+'\n');print(json.dumps({'sql_http':report,'recovery':recovery},indent=2))
 if args.native_python:
  http.stop();http.SERVER_ARGS=['--native-python',str(args.native_python.absolute()),'--native-schema-dir',str(args.native_schema_dir.resolve())];http.start()
  spec=importlib.util.spec_from_file_location('native_sql_http',ROOT/'test_native_import.py');native_module=importlib.util.module_from_spec(spec);spec.loader.exec_module(native_module)
  if not native_module.run_existing(http):raise RuntimeError('Native real-worker MySQL API checks failed.')
 spec=importlib.util.spec_from_file_location('workbench_sql_http',ROOT/'test_workbench.py');workbench_module=importlib.util.module_from_spec(spec);spec.loader.exec_module(workbench_module)
 if not workbench_module.run_existing(http):raise RuntimeError('Workbench MySQL API checks failed.')
 spec=importlib.util.spec_from_file_location('security_sql_http',ROOT/'test_security.py');security_module=importlib.util.module_from_spec(spec);spec.loader.exec_module(security_module)
 if not security_module.run_existing(http):raise RuntimeError('Account security MySQL API checks failed.')
 spec=importlib.util.spec_from_file_location('operations_sql_http',ROOT/'test_operations.py');operations_module=importlib.util.module_from_spec(spec);spec.loader.exec_module(operations_module)
 if not operations_module.run_existing(http):raise RuntimeError('Operations MySQL API checks failed.')
 spec=importlib.util.spec_from_file_location('privacy_sql',ROOT/'test_privacy.py');privacy_module=importlib.util.module_from_spec(spec);spec.loader.exec_module(privacy_module)
 if not privacy_module.run_existing(http):raise RuntimeError('Account privacy MySQL API checks failed.')
 recovery.update(backup_restore_tested=True,backup_restore_kind='FRAMED_ISOLATED_ONLY',production_restore_reconciled=False);(ROOT/'build/concurrent_mysql_recovery_results.json').write_text(json.dumps(recovery,indent=2)+'\n')
 for source,destination in [('operations_mysql_http_test_results.json','operations_concurrent_mysql_http_test_results.json'),('native_mysql_http_test_results.json','native_concurrent_mysql_http_test_results.json')]:
  if (ROOT/'build'/source).exists() and (source.startswith('operations') or args.native_python):shutil.copy2(ROOT/'build'/source,ROOT/'build'/destination)

 if args.with_concurrent_mutations:
  fixtures=[]
  for at in range(4):
   cfg=dict(common,database=database+'_concurrent_defect'+str(at));runtime_cfg=folder/('defect-app-'+str(at)+'.json');migrate_cfg=folder/('defect-admin-'+str(at)+'.json');record_key=folder/('defect-key-'+str(at)+'/key');private_json(runtime_cfg,dict(cfg,user='tax_app',password=passwords['app']));private_json(migrate_cfg,dict(cfg,user='tax_migrate',password=passwords['migrate']));run([sys.executable,str(ROOT/'run.py'),'--storage','mysql-concurrent','--mysql-config',str(migrate_cfg),'--jdbc-jar',str(driver),'--key-file',str(record_key),'--init-mysql'],timeout=30);fixtures.append((runtime_cfg,migrate_cfg,record_key))
  spec=importlib.util.spec_from_file_location('concurrent_defects',ROOT/'check_concurrent_variants.py');mutations=importlib.util.module_from_spec(spec);spec.loader.exec_module(mutations);mutations.run_variants(fixtures,driver)

finally:
 if http and http.PROCESS and http.PROCESS.poll() is None:http.stop()
 stop_db()
 if args.keep_private_fixture:print('Private disposable fixture retained for local diagnosis; no credentials are included in reports.')
 else:shutil.rmtree(folder,ignore_errors=True)
