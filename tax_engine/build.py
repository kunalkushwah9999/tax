#!/usr/bin/env python3
"""Build private arithmetic workbench, compare two implementations, and package without test fixtures."""
from pathlib import Path
import hashlib,json,os,shutil,subprocess
root=Path(__file__).resolve().parent
core=root.parent/'java_result_safety/build/tax-result-safety-0.1.4.jar'
java=shutil.which('java')
if not java or not core.is_file():raise SystemExit('Java 17+ and sibling core 0.1.4 JAR are required.')
build=root/'build';build.mkdir(exist_ok=True)
def compile_to(target,sources,classpath):
 if target.is_symlink():raise SystemExit('Refusing a symlinked class output.')
 if target.exists():shutil.rmtree(target)
 target.mkdir()
 return run(['-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-proc:none','-Xlint:all','-Werror','-cp',classpath,'-d',str(target),*[str(p) for p in sources]])
def run(args):
 r=subprocess.run([java,*args],cwd=root,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 if r.returncode:print(r.stdout);raise SystemExit(r.returncode)
 return r.stdout
sources=sorted((root/'src/main/java').rglob('*.java'))
compile_to(build/'classes',sources,str(core))
compile_to(build/'test-classes',sorted((root/'src/test/java').rglob('*.java')),os.pathsep.join([str(build/'classes'),str(core)]))
log=run(['-cp',os.pathsep.join([str(build/'test-classes'),str(build/'classes'),str(core)]),'com.taxplatform.planner.EngineTests'])
summary=json.loads(log.strip().splitlines()[-1]);(build/'test_log.txt').write_text(log)
lib=build/'lib';lib.mkdir(exist_ok=True);shutil.copyfile(core,lib/core.name)
manifest=build/'MANIFEST.MF';manifest.write_text('Manifest-Version: 1.0\nMain-Class: com.taxplatform.planner.PlannerCli\nClass-Path: lib/'+core.name+'\n\n')
jar=build/'tax-planner-workbench-0.1.0.jar'
run(['-m','jdk.jartool/sun.tools.jar.Main','--create','--file',str(jar),'--manifest',str(manifest),'-C',str(build/'classes'),'.','-C',str(root/'src/main/resources'),'.'])
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
report={'module':'tax-planner-workbench','version':'0.1.0',**summary,'jar_sha256':sha(jar),'core_jar_sha256':sha(core),'inputs':{str(p.relative_to(root)):sha(p) for p in [*sources,*sorted((root/'src/main/resources').rglob('*'))] if p.is_file()},'external_dependencies':[], 'tax_law_independently_validated':False,'production_release_approved':False}
(build/'test_results.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps({k:v for k,v in report.items() if k!='inputs'},indent=2))
