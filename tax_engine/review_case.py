#!/usr/bin/env python3
"""Explicit private engineering review of one synthetic case; never a public tax result."""
from pathlib import Path
import argparse,copy,json,subprocess,tempfile,urllib.parse
import review_workbench as work
ROOT=Path(__file__).resolve().parent
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('case_id');p.add_argument('--both',action='store_true',help='Create a private both-regime variant; not the original registered golden input.')
p.add_argument('--additional-80c-capacity',help='Declared additional eligible amount in INR, for a both-regime private sensitivity check.')
a=p.parse_args();source=work.corpus(ROOT.parent/'golden_cases_v0.json');case=next((copy.deepcopy(c) for c in source['cases'] if c['case_id']==a.case_id),None)
if case is None:p.error('case_id must be G01 through G30')
if a.both:case['regime']='BOTH'
with tempfile.TemporaryDirectory(prefix='private-tax-case-') as folder:
 request=Path(folder)/'request.form';request.write_text(urllib.parse.urlencode(work.form(case)))
 command=['java','-jar',str(ROOT/'build/tax-planner-workbench-0.1.0.jar'),str(request)]
 if a.additional_80c_capacity is not None:command.append(a.additional_80c_capacity)
 result=subprocess.run(command,capture_output=True,text=True)
 if result.returncode:raise SystemExit('Private case could not be evaluated. Sensitivity requires both supported regimes, complete eligible deductions and capacity within the remaining 80C limit.')
 output=json.loads(result.stdout);output['source_case_id']=a.case_id;output['is_modified_variant']=a.both;output['input_sha256']=work.input_hash(case)
 print(json.dumps(output,indent=2))
