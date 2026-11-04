#!/usr/bin/env python3
"""Repeat engineering checks; optional MySQL tests use a fresh disposable database."""
from pathlib import Path
import argparse,subprocess,sys
root=Path(__file__).resolve().parent
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--include-unchanged-core',action='store_true');p.add_argument('--skip-selected-mutations',action='store_true');p.add_argument('--mysql-basedir',type=Path);p.add_argument('--jdbc-jar',type=Path);p.add_argument('--mysql-library-path',default='');p.add_argument('--with-operations',action='store_true',help='Requires the optional cryptography test dependency.');args=p.parse_args()
if bool(args.mysql_basedir)!=bool(args.jdbc_jar):p.error('--mysql-basedir and --jdbc-jar must be supplied together.')
if args.mysql_library_path and not args.mysql_basedir:p.error('--mysql-library-path requires the optional MySQL profile.')
commands=[]
if args.include_unchanged_core:commands.extend(['java_result_safety/build.py','java_result_safety/check_mutations.py'])
commands.append('tax_engine/build.py')
if not args.skip_selected_mutations:commands.append('tax_engine/check_mutations.py')
commands.extend(['tax_engine/test_references.py','local_app/build_app.py'])
if not args.skip_selected_mutations:commands.extend(['local_app/check_comparison_mutations.py','local_app/check_source_proposal_mutations.py'])
commands.extend(['local_app/test_http.py','startup_app/build.py','startup_app/test_http.py','startup_app/test_https.py'])
if args.with_operations:
 commands.append('startup_app/test_operations.py')
 if not args.skip_selected_mutations:commands.append('startup_app/check_operations_mutations.py')
if not args.skip_selected_mutations:commands.extend(['startup_app/check_mutations.py','startup_app/check_self_service_mutations.py','startup_app/check_native_mutations.py'])
for relative in commands:
 print('Running '+relative,flush=True);subprocess.run([sys.executable,str(root/relative)],cwd=root,check=True)
if args.mysql_basedir:
 command=[sys.executable,str(root/'startup_app/test_mysql.py'),'--mysql-basedir',str(args.mysql_basedir.resolve()),'--jdbc-jar',str(args.jdbc_jar.resolve())]
 if args.mysql_library_path:command+=['--mysql-library-path',args.mysql_library_path]
 if args.skip_selected_mutations:command.append('--skip-selected-mutations')
 print('Running disposable real MySQL checks',flush=True);subprocess.run(command,cwd=root,check=True)
print('Engineering checks passed. Independent tax approval and production release remain pending. Run optional native and browser harnesses with the documented pinned prerequisites.')
