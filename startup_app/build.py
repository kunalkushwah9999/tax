#!/usr/bin/env python3
from pathlib import Path
import hashlib,json,os,shutil,subprocess,zipfile
root=Path(__file__).resolve().parent
build=root/'build';build.mkdir(exist_ok=True)
libs=[root.parent/'java_result_safety/build/tax-result-safety-0.1.4.jar',root.parent/'tax_engine/build/tax-planner-workbench-0.1.0.jar',root.parent/'local_app/build/tax-preparation-local-demo-0.4.0.jar']
if not all(p.is_file() for p in libs):raise SystemExit('The three existing verified component JARs are required.')
classes=build/'classes'
if classes.exists():shutil.rmtree(classes)
classes.mkdir()
sources=sorted((root/'src/main/java').rglob('*.java'))
dependency_classes=build/'dependency-classes'
if dependency_classes.exists():shutil.rmtree(dependency_classes)
dependency_classes.mkdir()
for library in libs:
 with zipfile.ZipFile(library) as archive:
  for name in archive.namelist():
   if name.endswith('.class'):
    target=dependency_classes/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(archive.read(name))
cp=str(dependency_classes)
def run(args):
 p=subprocess.run(['java',*args],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 if p.returncode:print(p.stdout);raise SystemExit(p.returncode)
 return p.stdout
run(['-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','--add-modules','jdk.httpserver','-encoding','UTF-8','-proc:none','-Xlint:all','-Werror','-cp',cp,'-d',str(classes),*[str(p) for p in sources]])

test_classes=build/'test-classes'
if test_classes.exists():shutil.rmtree(test_classes)
test_classes.mkdir()
tests=sorted((root/'src/test/java').rglob('*.java'))
if tests:
 run(['-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-proc:none','-Xlint:all','-Werror','-cp',os.pathsep.join([str(classes),str(dependency_classes)]),'-d',str(test_classes),*[str(p) for p in tests]])
 test_log=run(['--add-modules','jdk.httpserver','-cp',os.pathsep.join([str(test_classes),str(classes),*[str(p) for p in libs]]),'com.taxplatform.startup.WorkspaceTests'])
 test_report=json.loads(test_log.strip().splitlines()[-1])
 (build/'unit_test_results.json').write_text(json.dumps(test_report,indent=2)+'\n')
 print(json.dumps(test_report))
 self_log=run(['--add-modules','jdk.httpserver','-cp',os.pathsep.join([str(test_classes),str(classes),*[str(p) for p in libs]]),'com.taxplatform.startup.SelfServiceTests'])
 self_report=json.loads(self_log.strip().splitlines()[-1])
 (build/'self_service_test_results.json').write_text(json.dumps(self_report,indent=2)+'\n')
 print(json.dumps(self_report))
 recovery_log=run(['--add-modules','jdk.httpserver','-cp',os.pathsep.join([str(test_classes),str(classes),*[str(p) for p in libs]]),'com.taxplatform.startup.RecoveryTests'])
 recovery_report=json.loads(recovery_log.strip().splitlines()[-1])
 (build/'recovery_unit_test_results.json').write_text(json.dumps(recovery_report,indent=2)+'\n')
 print(json.dumps(recovery_report))
 native_log=run(['--add-modules','jdk.httpserver','-cp',os.pathsep.join([str(test_classes),str(classes),str(root/'src/main/resources'),*[str(p) for p in libs]]),'com.taxplatform.startup.NativeImportTests'])
 native_report=json.loads(native_log.strip().splitlines()[-1])
 (build/'native_policy_test_results.json').write_text(json.dumps(native_report,indent=2)+'\n')
 print(json.dumps(native_report))

workbench_log=run(['--add-modules','jdk.httpserver','-cp',os.pathsep.join([str(test_classes),str(classes),str(root/'src/main/resources'),*[str(p) for p in libs]]),'com.taxplatform.startup.TaxWorkbenchTests'])
workbench_report=json.loads(workbench_log.strip().splitlines()[-1]);(build/'workbench_unit_test_results.json').write_text(json.dumps(workbench_report,indent=2)+'\n');print(json.dumps(workbench_report))

security_log=run(['--add-modules','jdk.httpserver','-cp',os.pathsep.join([str(test_classes),str(classes),str(root/'src/main/resources'),*[str(p) for p in libs]]),'com.taxplatform.startup.AccountSecurityTests'])
security_report=json.loads(security_log.strip().splitlines()[-1]);(build/'security_unit_test_results.json').write_text(json.dumps(security_report,indent=2)+'\n');print(json.dumps(security_report))

privacy_log=run(['--add-modules','jdk.httpserver','-cp',os.pathsep.join([str(test_classes),str(classes),str(root/'src/main/resources'),*[str(p) for p in libs]]),'com.taxplatform.startup.AccountPrivacyTests'])
privacy_report=json.loads(privacy_log.strip().splitlines()[-1]);(build/'privacy_unit_test_results.json').write_text(json.dumps(privacy_report,indent=2)+'\n');print(json.dumps(privacy_report))

framed_log=run(['--add-modules','jdk.httpserver','-cp',os.pathsep.join([str(test_classes),str(classes),str(root/'src/main/resources'),*[str(p) for p in libs]]),'com.taxplatform.startup.FramedBackupTests'])
framed_report=json.loads(framed_log.strip().splitlines()[-1]);(build/'framed_backup_unit_test_results.json').write_text(json.dumps(framed_report,indent=2)+'\n');print(json.dumps(framed_report))

manifest=build/'MANIFEST.MF';manifest.write_text('Manifest-Version: 1.0\nMain-Class: com.taxplatform.startup.Server\nClass-Path: '+' '.join('../../'+str(p.relative_to(root.parent)) for p in libs)+'\n\n')
jar=build/'tax-platform-startup-5.1.0.jar'
run(['-m','jdk.jartool/sun.tools.jar.Main','--create','--file',str(jar),'--manifest',str(manifest),'-C',str(classes),'.','-C',str(root/'src/main/resources'),'.'])
report={'version':'5.1.0','jar_sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'inputs':{str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in [*sources,*sorted((root/'src/main/resources').rglob('*'))] if p.is_file()},'components':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in libs},'production_ready':False}
(build/'build_results.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k!='inputs'},indent=2))
