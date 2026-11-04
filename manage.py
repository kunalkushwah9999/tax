#!/usr/bin/env python3
"""Offline TaxDesk delivery verification and launch. Standard library only."""
from pathlib import Path, PurePosixPath
import argparse,hashlib,json,re,shutil,subprocess,sys

ROOT=Path(__file__).resolve().parent
REQUIRED={'manage.py','startup_app/run.py','startup_app/build/build_results.json','golden_cases_v0.json','startup_app/tools/release_preflight.py'}
class IntegrityError(ValueError):pass

def verify(root,required=()):
 root=Path(root).resolve();manifest=root/'checksums.sha256'
 if manifest.is_symlink() or not manifest.is_file():raise IntegrityError('Missing regular checksum manifest')
 if manifest.stat().st_size>2_000_000:raise IntegrityError('Manifest exceeds delivery limit')
 lines=manifest.read_text(encoding='utf-8').splitlines()
 if not lines or len(lines)>10000:raise IntegrityError('Invalid manifest size')
 seen=set()
 for line in lines:
  if '  ' not in line:raise IntegrityError('Invalid manifest line')
  digest,name=line.split('  ',1);relative=PurePosixPath(name)
  if not re.fullmatch('[0-9a-f]{64}',digest):raise IntegrityError('Invalid SHA-256 digest')
  if not name or '\\' in name or ':' in name or any(ord(c)<32 for c in name) or relative.is_absolute() or any(p in {'.','..'} for p in relative.parts) or relative.as_posix()!=name:raise IntegrityError('Unsafe manifest path')
  if name in seen or name=='checksums.sha256':raise IntegrityError('Duplicate or self-referencing manifest path')
  seen.add(name);target=root
  for part in relative.parts:
   target=target/part
   if target.is_symlink():raise IntegrityError('Symlink in delivery path: '+name)
  if not target.is_file():raise IntegrityError('Missing file: '+name)
  actual=hashlib.sha256()
  with target.open('rb') as stream:
   for chunk in iter(lambda:stream.read(65536),b''):actual.update(chunk)
  if actual.hexdigest()!=digest:raise IntegrityError('Changed file: '+name)
 if not set(required)<=seen:raise IntegrityError('Required delivery files are not covered')
 return {'files_verified':len(seen),'integrity_ok':True,'publisher_signature_verified':False}

def doctor(root=ROOT):
 result=verify(root,REQUIRED);result['python_supported']=sys.version_info>=(3,10);result['python_version']='.'.join(map(str,sys.version_info[:3]));result['java_supported']=False
 java=shutil.which('java')
 if java:
  try:
   process=subprocess.run([java,'-version'],capture_output=True,text=True,timeout=10)
   match=re.search(r'version "([0-9]+)(?:\.([0-9]+))?',process.stderr+process.stdout)
   if match:
    major=int(match[1]);major=int(match[2] or 0) if major==1 else major;result['java_major']=major;result['java_supported']=process.returncode==0 and major>=17
  except (OSError,subprocess.TimeoutExpired):pass
 if result['python_supported']:
  check=subprocess.run([sys.executable,str(root/'startup_app/tools/release_preflight.py'),'--mode','engineering'],capture_output=True,text=True,timeout=30)
  try:evidence=json.loads(check.stdout)
  except ValueError:evidence={}
  result['engineering_integrity_ok']=check.returncode==0 and evidence.get('engineering_checks_ok') is True
  result['application_version']=evidence.get('version','unknown');result['independent_approved_tax_outputs']=evidence.get('independent_approved_tax_outputs',0)
 else:result['engineering_integrity_ok']=False
 result['ready_to_run_synthetic_app']=all(result.get(k) is True for k in ['integrity_ok','python_supported','java_supported','engineering_integrity_ok']);result['production_ready']=False;result['synthetic_data_only']=True;result['chatgpt_subscription_required']=False
 return result

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('command',choices=['verify','doctor','start'],nargs='?',default='doctor');p.add_argument('--port',type=int,default=8788);p.add_argument('--data-dir',type=Path,default=ROOT/'startup_app/runtime-data');args=p.parse_args()
 if not 0<=args.port<=65535:p.error('Port must be between 0 and 65535')
 try:
  if args.command=='verify':print(json.dumps(verify(ROOT,REQUIRED),indent=2));return 0
  state=doctor()
  if args.command=='doctor':print(json.dumps(state,indent=2));return 0 if state['ready_to_run_synthetic_app'] else 2
  if not state['ready_to_run_synthetic_app']:print(json.dumps(state,indent=2));return 2
  return subprocess.call([sys.executable,str(ROOT/'startup_app/run.py'),'--port',str(args.port),'--data-dir',str(args.data_dir.absolute())])
 except (IntegrityError,OSError,ValueError,subprocess.TimeoutExpired) as error:
  print(json.dumps({'status':'DELIVERY_CHECK_FAILED','reason':str(error),'production_ready':False}));return 2
if __name__=='__main__':sys.exit(main())
