#!/usr/bin/env python3
"""Fetch one pinned official JDBC artifact; never installs it or starts a server."""
from pathlib import Path
import argparse,hashlib,os,tempfile,urllib.request
VERSION='26.10.0';SHA256='cef70caf532833c9ab8f41f74773ce22c08bf503fad215c9c2ee1e9db87e759e'
URL='https://repo.maven.apache.org/maven2/com/mysql/mysql-connector-j/'+VERSION+'/mysql-connector-j-'+VERSION+'.jar'
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,required=True);args=p.parse_args();target=args.output.resolve()
if target.exists():
 if hashlib.sha256(target.read_bytes()).hexdigest()!=SHA256:raise SystemExit('Existing artifact differs; it was not overwritten.')
 print('Pinned official JDBC artifact already verified.');raise SystemExit(0)
target.parent.mkdir(parents=True,exist_ok=True)
with urllib.request.urlopen(URL,timeout=30) as response:data=response.read(4_194_305)
if len(data)>4_194_304 or hashlib.sha256(data).hexdigest()!=SHA256:raise SystemExit('Artifact verification failed; nothing was installed.')
fd,name=tempfile.mkstemp(prefix='jdbc-',suffix='.pending',dir=target.parent)
try:
 with os.fdopen(fd,'wb') as out:out.write(data);out.flush();os.fsync(out.fileno())
 os.link(name,target)
finally:os.unlink(name)
print('Official Connector/J '+VERSION+' downloaded and SHA-256 verified. Review its vendor license before redistribution.')
