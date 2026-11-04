#!/usr/bin/env python3
"""Launch the integrated development/pilot app. MySQL is an explicit optional profile."""
from pathlib import Path
import argparse,getpass,json,os,shutil,subprocess,sys
root=Path(__file__).resolve().parent
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--port',type=int,default=8788);p.add_argument('--data-dir',type=Path,default=root/'runtime-data');p.add_argument('--create-operator',choices=['ADMIN','PREPARER']);p.add_argument('--operator-json',action='store_true',help=argparse.SUPPRESS);p.add_argument('--storage',choices=['local','mysql','mysql-concurrent'],default='local');p.add_argument('--mysql-config',type=Path);p.add_argument('--jdbc-jar',type=Path);p.add_argument('--key-file',type=Path);p.add_argument('--init-mysql',action='store_true');p.add_argument('--public-origin',help='Exact HTTPS origin of a trusted TLS reverse proxy; inner listener remains loopback.');p.add_argument('--backup-to',type=Path);p.add_argument('--verify-backup',type=Path);p.add_argument('--restore-backup',type=Path);p.add_argument('--backup-key',type=Path);p.add_argument('--native-python',type=Path);p.add_argument('--native-schema-dir',type=Path);p.add_argument('--payment-config',type=Path);p.add_argument('--professional-registry',type=Path);args=p.parse_args()
operations=[bool(args.create_operator or args.operator_json),args.init_mysql,bool(args.backup_to),bool(args.verify_backup),bool(args.restore_backup)]
if sum(operations)>1:p.error('Choose exactly one operator action per invocation.')
if any([args.backup_to,args.verify_backup,args.restore_backup]) and not args.backup_key:p.error('Backup operations require a separate --backup-key.')
if args.restore_backup and args.storage=='local':p.error('Restore requires an explicitly provisioned fresh MySQL target; it stays isolated.')
if bool(args.native_python)!=bool(args.native_schema_dir):p.error('--native-python and --native-schema-dir are required together.')
java=shutil.which('java')
if not java:raise SystemExit('Install Java 17+ first.')
if not 0<=args.port<=65535:p.error('Port must be from 0 to 65535.')
jar=root/'build/tax-platform-startup-5.1.0.jar'
if not jar.is_file():subprocess.run([sys.executable,str(root/'build.py')],check=True)
key=(args.key_file or args.data_dir/'keys/vault.key').resolve()
if args.storage in ['mysql','mysql-concurrent']:
 if not args.mysql_config or not args.jdbc_jar or not args.jdbc_jar.is_file():p.error('MySQL requires --mysql-config and --jdbc-jar. No local fallback is performed.')
 base=[java,'--add-modules','jdk.httpserver,java.sql','-cp',os.pathsep.join([str(jar),str(args.jdbc_jar.resolve())]),'com.taxplatform.startup.Server','--storage',args.storage,'--mysql-config',str(args.mysql_config.resolve()),'--key',str(key)]
else:
 if args.init_mysql:p.error('--init-mysql requires --storage mysql.')
 base=[java,'--add-modules','jdk.httpserver,java.sql','-jar',str(jar),'--data',str(args.data_dir.resolve()/'records'),'--key',str(key)]
if args.payment_config:base+=['--payment-config',str(args.payment_config.resolve())]
if args.professional_registry:base+=['--professional-registry',str(args.professional_registry.resolve())]
if args.public_origin:base+=['--public-origin',args.public_origin]
if args.native_python:base+=['--native-python',str(args.native_python.absolute()),'--native-worker',str(root/'native_import/worker.py'),'--native-schema-dir',str(args.native_schema_dir.resolve())]
for action,path in [('--backup-to',args.backup_to),('--verify-backup',args.verify_backup),('--restore-backup',args.restore_backup)]:
 if path:raise SystemExit(subprocess.call(base+[action,str(path.resolve()),'--backup-key',str(args.backup_key.resolve())]))
if args.init_mysql:raise SystemExit(subprocess.call(base+['--init-mysql']))
if args.operator_json:
 payload=sys.stdin.buffer.read(4097)
 if len(payload)>4096:raise SystemExit('Operator request is too large.')
 raise SystemExit(subprocess.run(base+['--operator'],input=payload).returncode)
if args.create_operator:
 username=input('Operator username: ').strip();display=input('Display name (use a sample alias): ').strip();password=getpass.getpass('Password (12–128 characters): ')
 if password!=getpass.getpass('Repeat password: '):raise SystemExit('Passwords do not match.')
 payload={'username':username,'display_name':display,'password':password,'role':args.create_operator}
 raise SystemExit(subprocess.run(base+['--operator'],input=json.dumps(payload).encode()).returncode)
os.execv(java,base+['--port',str(args.port)])
