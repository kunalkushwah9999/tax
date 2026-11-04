#!/usr/bin/env python3
"""Offline reference collection/checking. Never edits golden inputs or approves a public release."""
from pathlib import Path
import argparse,datetime,hashlib,json,re,subprocess,tempfile,urllib.parse

ROOT=Path(__file__).resolve().parent
STAGES=('gross_salary','standard_deduction','taxable_salary','total_interest','gross_total_income',
 'limited_80c_claim','limited_80d_claim','limited_nps_1b_claim','limited_80tta_claim','limited_80ttb_claim',
 'total_deduction_allowed','total_income_before_rounding','rounded_total_income','slab_tax','rebate',
 'marginal_relief','tax_after_relief','cess','annual_tax_before_credits','zero_credit_rounded_working')
INPUT_KEYS=('period','taxpayer_type','residency','dob','regime','salary_by_employer','interest','fact_status',
 'deductions','screening','income_completeness','deduction_inventory_complete','tds_advance_tax_and_filing_interest')
GROUPS={
 'screen_business':('business_income',),
 'screen_special':('capital_gains','vda_income','lottery_or_special_rate_income'),
 'screen_property_foreign':('house_property_income','foreign_income','agricultural_income'),
 'screen_status':('foreign_assets','foreign_signing_authority','company_director','unlisted_equity','deferred_esop_tax','tds_194N'),
 'screen_losses':('brought_forward_losses','carry_forward_losses'),
 'screen_adjustments':('family_pension','salary_exemptions_or_professional_tax','employer_nps_deduction','other_unmodelled_income_or_deduction')}
DECIMAL=re.compile(r'(?:0|[1-9][0-9]{0,15})\.[0-9]{2}\Z')
def canonical(value):return json.dumps(value,sort_keys=True,separators=(',',':'),ensure_ascii=False).encode()
def digest(value):return hashlib.sha256(value).hexdigest()
def pairs(items):
 result={}
 for key,value in items:
  if key in result:raise ValueError('duplicate JSON field')
  result[key]=value
 return result
def read(path):
 if path.stat().st_size>2_000_000:raise ValueError('reference input too large')
 return json.loads(path.read_text(encoding='utf-8'),object_pairs_hook=pairs,parse_constant=lambda _:(_ for _ in ()).throw(ValueError('nonfinite JSON number')))
def corpus(path):
 result=read(path)
 if len(result['cases'])!=30 or {c['case_id'] for c in result['cases']}!={f'G{i:02}' for i in range(1,31)}:raise ValueError('exact thirty registered cases required')
 return result

def input_hash(case):return digest(canonical({key:case[key] for key in INPUT_KEYS}))
def regimes(case):return ['NEW','OLD'] if case['regime']=='BOTH' else [case['regime']]
def form(case):
 p=case['period'];period='ITA1961-FY2025-26-AY2026-27' if p=={'act':'ITA_1961','income_fy':'2025-26','assessment_year':'2026-27','tax_year':None} else 'UNSUPPORTED-PERIOD'
 states=case['fact_status'];salary_state=states['salary'];interest_state=states['interest'];deduction=states['deductions']
 f={'period':period,'residency':case['residency'],'dob':case['dob'],'dob_state':'CONFIRMED',
    'salaries':';'.join(s['gross_salary_before_standard_deduction'] for s in case['salary_by_employer']) if salary_state!='UNKNOWN' else 'UNKNOWN',
    'salary_state':salary_state,'interest_state':interest_state,'deduction_state':deduction,'regimes':case['regime'],
    'income_inventory':'REVIEWED' if case['income_completeness']=='CONFIRMED' else 'UNKNOWN',
    'deduction_inventory':'REVIEWED' if case['deduction_inventory_complete'] else 'INCOMPLETE'}
 for out,key in [('savings','eligible_savings'),('deposits','eligible_bank_fd'),('other_interest','nonqualifying_deposit')]:f[out]='UNKNOWN' if interest_state=='UNKNOWN' else case['interest'][key]
 for out,key in [('eligible_80c','80C'),('eligible_80d','80D'),('eligible_nps','80CCD_1B')]:f[out]='UNKNOWN' if deduction=='UNKNOWN' else case['deductions'][key]
 if set(case['screening'])!=set(sum((list(v) for v in GROUPS.values()),[])):raise ValueError('unmapped screening field')
 for out,keys in GROUPS.items():
  values=[case['screening'].get(k) for k in keys]
  if any(type(v) is not bool for v in values):f[out]='UNKNOWN'
  else:f[out]='YES' if any(values) else 'NO'
 return f

def evaluate_case(case,jar):
 with tempfile.TemporaryDirectory(prefix='tax-private-review-') as folder:
  request=Path(folder)/'request.form';request.write_text(urllib.parse.urlencode(form(case)),encoding='utf-8')
  p=subprocess.run(['java','-jar',str(jar),str(request)],capture_output=True,text=True,timeout=20)
  if p.returncode:raise ValueError('engine execution failed for '+case['case_id'])
  result=json.loads(p.stdout,object_pairs_hook=pairs)
  if result.get('independent_reference_approved') is not False or result.get('rule_release_approved') is not False:raise ValueError('private working cannot approve references')
 return result

def evaluate(source,jar):
 return {'schema':'private-engine-workings.v1','purpose':'ENGINE_OUTPUT_NOT_AN_INDEPENDENT_REFERENCE',
  'engine_jar_sha256':digest(jar.read_bytes()),'corpus_sha256':digest(canonical(source)),
  'cases':[{'case_id':c['case_id'],'input_sha256':input_hash(c),'candidate':evaluate_case(c,jar)} for c in source['cases']],
  'release_approved':False}

def template(source):
 return {'schema':'independent-planner-reference.v1','stage_contract':'planner-stages.v1',
  'instruction':'Fill independently from official utility or qualified workings; do not copy this engine output. This file grants no release authority.',
  'cases':[{'case_id':c['case_id'],'input_sha256':input_hash(c),'expected':{r:{'status':None,'stages':{k:None for k in STAGES} if c['review_kind']=='ARITHMETIC' else {}} for r in regimes(c)},
    'review':{'decision':'PENDING','reviewer':None,'method':None,'evidence_id':None,'evidence_sha256':None,'reference_version':None,'reviewed_on':None,'independent_of_this_engine':None}} for c in source['cases']],
  'release_approved':False}

def check_references(source,references,workings):
 if references.get('schema')!='independent-planner-reference.v1' or references.get('stage_contract')!='planner-stages.v1':raise ValueError('reference schema/contract mismatch')
 if workings.get('schema')!='private-engine-workings.v1' or workings.get('corpus_sha256')!=digest(canonical(source)):raise ValueError('stale or incorrect workings corpus')
 actual_rows=workings.get('cases',[])
 if len(actual_rows)!=30 or {r.get('case_id') for r in actual_rows}!={c['case_id'] for c in source['cases']}:raise ValueError('incomplete workings corpus')
 for case in source['cases']:
  actual=next(r for r in actual_rows if r['case_id']==case['case_id'])
  if actual.get('input_sha256')!=input_hash(case):raise ValueError('workings input mismatch')
 rows=references.get('cases',[])
 if len(rows)!=30 or {r.get('case_id') for r in rows}!={c['case_id'] for c in source['cases']}:raise ValueError('exact registered reference IDs required')
 supplied={r['case_id']:r for r in rows};candidate={r['case_id']:r for r in workings['cases']};results=[]
 for case in source['cases']:
  ref=supplied[case['case_id']];issues=[];review=ref.get('review',{})
  if ref.get('input_sha256')!=input_hash(case):issues.append('INPUT_HASH_MISMATCH')
  if review.get('decision')!='APPROVED' or review.get('independent_of_this_engine') is not True:issues.append('INDEPENDENT_REVIEW_PENDING')
  for key in ['reviewer','method','evidence_id','reference_version']:
   if not isinstance(review.get(key),str) or not 1<=len(review[key].strip())<=256:issues.append('REVIEW_METADATA_'+key.upper())
  evidence=review.get('evidence_sha256','')
  if not isinstance(evidence,str) or not re.fullmatch('[0-9a-f]{64}',evidence) or evidence=='0'*64:issues.append('EVIDENCE_DIGEST_REQUIRED')
  try:
   date=review.get('reviewed_on')
   if not isinstance(date,str) or not re.fullmatch('[0-9]{4}-[0-9]{2}-[0-9]{2}',date):raise ValueError('ISO review date required')
   datetime.date.fromisoformat(date)
  except (ValueError,TypeError):issues.append('REVIEW_DATE_REQUIRED')
  expected=ref.get('expected',{})
  if set(expected)!=set(regimes(case)):issues.append('REGIME_SET_MISMATCH')
  for regime in regimes(case):
   want=expected.get(regime,{});actual=candidate[case['case_id']]['candidate']['regimes'][regime]
   status=want.get('status')
   if status not in ['CALCULATED','INFORMATION_REQUIRED','UNSUPPORTED']:issues.append(regime+':EXPECTED_STATUS_REQUIRED');continue
   if status!=actual['status']:issues.append(regime+':WORKFLOW_MISMATCH');continue
   stages=want.get('stages',{})
   if case['review_kind']=='ARITHMETIC':
    if status!='CALCULATED' or set(stages)!=set(STAGES):issues.append(regime+':FULL_STAGE_REFERENCE_REQUIRED');continue
    for stage in STAGES:
     value=stages[stage]
     if not isinstance(value,str) or not DECIMAL.fullmatch(value):issues.append(regime+':'+stage+':CANONICAL_REFERENCE_REQUIRED')
     elif value!=actual['workings']['stages'][stage]:issues.append(regime+':'+stage+':STAGE_MISMATCH')
   elif stages:issues.append(regime+':CONTROL_CASE_HAS_UNEXPECTED_NUMERICAL_EXPECTATIONS')
  results.append({'case_id':case['case_id'],'status':'MATCHED_REVIEW_ASSERTIONS' if not issues else 'PENDING_OR_FAILED','issues':issues})
 matched=sum(r['status']=='MATCHED_REVIEW_ASSERTIONS' for r in results)
 return {'schema':'reference-check-report.v1','cases_checked':30,'matched_review_assertions':matched,'pending_or_failed':30-matched,
  'cases':results,'ready_for_human_release_review':matched==30,'reviewer_identity_and_independence_authenticated':False,
  'evidence_bytes_independently_validated':False,'release_approved':False,
  'warning':'Matching asserted references does not authenticate a CA, validate evidence bytes or publish rules. Never use this report as release authority.',
  'engine_jar_sha256':workings['engine_jar_sha256'],'corpus_sha256':workings['corpus_sha256']}

def main():
 parser=argparse.ArgumentParser(description=__doc__)
 parser.add_argument('command',choices=['template','evaluate','check'])
 parser.add_argument('--corpus',type=Path,default=ROOT.parent/'golden_cases_v0.json')
 parser.add_argument('--jar',type=Path,default=ROOT/'build/tax-planner-workbench-0.1.0.jar')
 parser.add_argument('--references',type=Path)
 parser.add_argument('--output',required=True,type=Path)
 args=parser.parse_args()
 if args.output.exists():parser.error('choose a new output path; existing evidence is never overwritten')
 source=corpus(args.corpus)
 if args.command=='template':result=template(source)
 else:
  workings=evaluate(source,args.jar.resolve())
  if args.command=='evaluate':result=workings
  else:
   if args.references is None:parser.error('--references is required for check')
   result=check_references(source,read(args.references),workings)
 args.output.parent.mkdir(parents=True,exist_ok=True)
 with args.output.open('x',encoding='utf-8') as stream:stream.write(json.dumps(result,indent=2)+'\n')
 print(json.dumps({'output':str(args.output),'release_approved':False,**({'matched_review_assertions':result['matched_review_assertions']} if 'matched_review_assertions' in result else {})}))
 return 0 if args.command!='check' or result['ready_for_human_release_review'] else 2
if __name__=='__main__':raise SystemExit(main())
