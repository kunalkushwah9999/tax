#!/usr/bin/env python3
"""Build the separate local demonstration JAR against the existing guard library."""
from pathlib import Path
import hashlib
import os
import json
import shutil
import subprocess

root=Path(__file__).resolve().parent
core=root.parent/'java_result_safety/build/tax-result-safety-0.1.4.jar'
if not core.is_file(): raise SystemExit('Build java_result_safety/build.py first; its 0.1.4 JAR is required.')
engine=root.parent/'tax_engine/build/tax-planner-workbench-0.1.0.jar'
if not engine.is_file():raise SystemExit('Build tax_engine/build.py first; its workbench JAR is required.')
java=shutil.which('java')
if not java: raise SystemExit('Java 17+ with jdk.compiler, jdk.httpserver and jdk.jartool is required.')
build=root/'build'
if build.is_symlink(): raise SystemExit('Refusing a symlinked build directory.')
build.mkdir(exist_ok=True)
classes=build/'classes'
if classes.is_symlink(): raise SystemExit('Refusing symlinked class output.')
if classes.exists(): shutil.rmtree(classes)
classes.mkdir()
sources=sorted((root/'src/main/java').rglob('*.java'))
def execute(arguments):
    result=subprocess.run([java,*arguments],cwd=root,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    if result.returncode: print(result.stdout);raise SystemExit(result.returncode)
    return result.stdout
execute(['-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','--add-modules','jdk.httpserver',
         '-encoding','UTF-8','-proc:none','-Xlint:all','-Werror','-cp',os.pathsep.join([str(core),str(engine)]),'-d',str(classes),*[str(p) for p in sources]])
test_classes=build/'test-classes'
if test_classes.exists():shutil.rmtree(test_classes)
test_classes.mkdir()
test_sources=sorted((root/'src/test/java').rglob('*.java'))
execute(['-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','--add-modules','jdk.httpserver','-encoding','UTF-8','-proc:none','-Xlint:all','-Werror','-cp',os.pathsep.join([str(classes),str(core),str(engine)]),'-d',str(test_classes),*[str(p) for p in test_sources]])
log=execute(['--add-modules','jdk.httpserver','-cp',os.pathsep.join([str(test_classes),str(classes),str(core),str(engine)]),'com.taxplatform.localdemo.ComparisonTests'])
comparison=json.loads(log.strip().splitlines()[-1])
source_log=execute(['--add-modules','jdk.httpserver','-cp',os.pathsep.join([str(test_classes),str(classes),str(core),str(engine)]),'com.taxplatform.localdemo.SourceProposalTests'])
source_proposals=json.loads(source_log.strip().splitlines()[-1])
lib=build/'lib'
if lib.is_symlink(): raise SystemExit('Refusing a symlinked library directory.')
lib.mkdir(exist_ok=True)
shutil.copyfile(core,lib/core.name)
shutil.copyfile(engine,lib/engine.name)
manifest=build/'MANIFEST.MF'
manifest.write_text('Manifest-Version: 1.0\nMain-Class: com.taxplatform.localdemo.DemoServer\nClass-Path: lib/'+core.name+' lib/'+engine.name+'\n\n')
jar=build/'tax-preparation-local-demo-0.4.0.jar'
execute(['-m','jdk.jartool/sun.tools.jar.Main','--create','--file',str(jar),'--manifest',str(manifest),
         '-C',str(classes),'.','-C',str(root/'src/main/resources'),'.'])
inputs=[*sources,*sorted((root/'src/main/resources').rglob('*'))]
report={'app':'tax-preparation-local-demo','version':'0.4.0','core_version':'0.1.4',
        'jar_sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'core_jar_sha256':hashlib.sha256(core.read_bytes()).hexdigest(),'engine_jar_sha256':hashlib.sha256(engine.read_bytes()).hexdigest(),
        'inputs':{str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs if p.is_file()},
        'external_dependencies':[],'private_arithmetic_enabled':True,'public_tax_values_enabled':False,'tax_calculation_enabled':False,'durable_storage':False,'production_ready':False}
comparison['app_jar_sha256']=report['jar_sha256']
source_proposals['app_jar_sha256']=report['jar_sha256']
(build/'source_proposal_test_results.json').write_text(json.dumps(source_proposals,indent=2)+'\n')
(build/'comparison_test_results.json').write_text(json.dumps(comparison,indent=2)+'\n')
(build/'build_results.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps({k:v for k,v in report.items() if k!='inputs'},indent=2))
