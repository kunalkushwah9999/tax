#!/usr/bin/env python3
"""Local Android prerequisites and unsigned build. Never signs, publishes or uploads."""
from pathlib import Path
import argparse, hashlib, json, os, re, shutil, subprocess, sys, zipfile

def doctor(project,sdk,gradle):
    project=Path(project).absolute();sdk=Path(sdk).absolute();issues=[]
    if not (project/'app/build.gradle').is_file() or not (project/'settings.gradle').is_file():issues.append('GENERATED_ANDROID_PROJECT_REQUIRED')
    else:
        text=(project/'app/build.gradle').read_text()
        if not all(s in text for s in ['compileSdk 36','targetSdk 36','androidbrowserhelper:2.7.4']):issues.append('PROJECT_VERSION_REVIEW_REQUIRED')
    for path in ['platforms/android-36/android.jar','build-tools/35.0.0/aapt2']:
        actual=sdk/path
        if os.name=='nt' and actual.name=='aapt2':actual=actual.with_suffix('.exe')
        if not actual.is_file():issues.append('SDK_COMPONENT_MISSING:'+path)
    java=shutil.which('java');version=None
    if java:
        r=subprocess.run([java,'-version'],capture_output=True,text=True,timeout=15)
        match=re.search(r'version "([0-9]+)',r.stdout+r.stderr);version=int(match[1]) if match else None
    if version is None or version<17:issues.append('JDK_17_OR_NEWER_REQUIRED')
    executable=shutil.which(str(gradle)) if gradle else None
    if not executable:issues.append('GRADLE_8_13_REQUIRED')
    else:
        r=subprocess.run([executable,'--version'],capture_output=True,text=True,timeout=30)
        if r.returncode or not re.search(r'^Gradle 8\.13\s*$',r.stdout,re.M):issues.append('GRADLE_8_13_REQUIRED')
    return {'ready_for_unsigned_build':not issues,'issues':issues,'java_major':version,'project':str(project),'sdk':str(sdk),'gradle':executable,'signed_android_build_verified':False,'production_ready':False}

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('command',choices=['doctor','build']);p.add_argument('--project',type=Path,required=True);p.add_argument('--sdk',type=Path,required=True);p.add_argument('--gradle',default='gradle');p.add_argument('--allow-dependency-download',action='store_true');p.add_argument('--out',type=Path,required=True);a=p.parse_args()
    if a.out.exists():p.error('Output report must be a new file')
    try:
        report=doctor(a.project,a.sdk,a.gradle)
        if a.command=='build' and report['ready_for_unsigned_build']:
            artifact=a.project/'app/build/outputs/bundle/release/app-release.aab'
            if artifact.exists():raise ValueError('Move the previous AAB aside or generate a fresh project before building')
            command=[report['gradle'],'--no-daemon','--console=plain']
            if not a.allow_dependency_download:command.append('--offline')
            command+=[':app:lintRelease',':app:bundleRelease'];environment=os.environ.copy();environment['ANDROID_HOME']=str(a.sdk.absolute());environment['ANDROID_SDK_ROOT']=str(a.sdk.absolute())
            result=subprocess.run(command,cwd=a.project,env=environment,timeout=1800)
            report['gradle_exit_code']=result.returncode;report['unsigned_build_succeeded']=False
            if result.returncode==0 and artifact.is_file():
                with zipfile.ZipFile(artifact) as z:
                    if 'base/manifest/AndroidManifest.xml' not in z.namelist() or z.testzip() is not None:raise ValueError('Invalid Android App Bundle')
                report.update(unsigned_build_succeeded=True,artifact=str(artifact.absolute()),artifact_sha256=hashlib.sha256(artifact.read_bytes()).hexdigest())
            else:report['issues'].append('BUILD_FAILED_INSPECT_LOCAL_GRADLE_OUTPUT')
            # Do not put raw Gradle output, paths to secrets or credentials in the report.
        passed=report['ready_for_unsigned_build'] and (a.command=='doctor' or report.get('unsigned_build_succeeded',False))
    except (OSError,ValueError,subprocess.SubprocessError,zipfile.BadZipFile) as error:
        report={'ready_for_unsigned_build':False,'issues':['BUILD_TOOL_ERROR:'+type(error).__name__],'production_ready':False,'signed_android_build_verified':False};passed=False
    with a.out.open('x',encoding='utf-8') as f:json.dump(report,f,indent=2);f.write('\n')
    print(json.dumps({'report':str(a.out),'passed':passed,'signed_android_build_verified':False,'production_ready':False}));return 0 if passed else 2
if __name__=='__main__':sys.exit(main())
