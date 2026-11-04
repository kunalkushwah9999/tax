#!/usr/bin/env python3
"""Start the loopback-only synthetic preparation app."""
from pathlib import Path
import argparse
import shutil
import subprocess
import sys

root=Path(__file__).resolve().parent
parser=argparse.ArgumentParser(description='Local synthetic preparation workflow; no tax calculation or saved cases.')
parser.add_argument('--port',type=int,default=8787)
args=parser.parse_args()
if not 0<=args.port<=65535: parser.error('port must be between 0 and 65535')
java=shutil.which('java')
if not java: raise SystemExit('Java 17+ is required.')
jar=root/'build/tax-preparation-local-demo-0.4.0.jar'
if not jar.is_file(): subprocess.run([sys.executable,str(root/'build_app.py')],check=True)
raise SystemExit(subprocess.call([java,'--add-modules','jdk.httpserver','-jar',str(jar),'--port',str(args.port)],cwd=root))
