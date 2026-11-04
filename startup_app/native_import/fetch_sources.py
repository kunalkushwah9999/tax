#!/usr/bin/env python3
"""Explicitly fetch hash-pinned public reference files; never runs return data or changes pins."""
from pathlib import Path
import argparse,hashlib,json,os,tempfile,urllib.request
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,required=True);args=p.parse_args();root=args.output.resolve();root.mkdir(parents=True,exist_ok=True)
pins=json.loads((Path(__file__).parent/'source_pins.json').read_text())
for key in ['schema','business_validation_reference']:
 item=pins[key];target=root/item['name']
 if target.exists():
  if target.is_symlink() or hashlib.sha256(target.read_bytes()).hexdigest()!=item['sha256']:raise SystemExit('Existing reference differs; nothing was overwritten.')
  print(item['name']+' already verified.');continue
 with urllib.request.urlopen(item['url'],timeout=30) as response:data=response.read(item['bytes']+1)
 if len(data)!=item['bytes'] or hashlib.sha256(data).hexdigest()!=item['sha256']:raise SystemExit('Reference hash/size differs; review a new source release before updating pins.')
 fd,name=tempfile.mkstemp(prefix='reference-',suffix='.pending',dir=root)
 try:
  with os.fdopen(fd,'wb') as f:f.write(data);f.flush();os.fsync(f.fileno())
  os.link(name,target)
 finally:os.unlink(name)
 print(item['name']+' downloaded and SHA-256 verified.')
