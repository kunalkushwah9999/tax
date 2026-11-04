#!/usr/bin/env python3
"""Bounded, offline structural ITR-1 preflight. Never tax approval or return generation."""
from pathlib import Path
import argparse,decimal,hashlib,importlib.metadata,itertools,json,os,re,sys

ROOT=Path(__file__).resolve().parent
SCHEMA_SHA='6a0ef9f64065431879dcc724869d8455a640bcf11279d1818eb19f11448ce763'
SCHEMA_ID='ITR1-AY2026-27-20260630-V1.1'
MAX_BYTES=196608
class Rejected(ValueError):pass
def pairs(items):
 out={}
 for k,v in items:
  if k in out:raise Rejected('DUPLICATE_JSON_KEY')
  out[k]=v
 return out
def integer(value):
 if len(value.lstrip('-'))>18:raise Rejected('JSON_NUMBER_BOUND')
 return int(value)
def number(value):
 if len(value)>40:raise Rejected('JSON_NUMBER_BOUND')
 v=decimal.Decimal(value)
 if not v.is_finite() or abs(v.as_tuple().exponent)>12:raise Rejected('JSON_NUMBER_BOUND')
 return v
def bad_constant(_):raise Rejected('NONFINITE_JSON_NUMBER')
def decode(raw):
 if len(raw)>MAX_BYTES:raise Rejected('NATIVE_DOCUMENT_TOO_LARGE')
 try:result=json.loads(raw.decode('utf-8'),object_pairs_hook=pairs,parse_int=integer,parse_float=number,parse_constant=bad_constant)
 except (UnicodeError,json.JSONDecodeError,RecursionError) as e:raise Rejected('INVALID_NATIVE_JSON') from e
 count=0
 def visit(x,depth):
  nonlocal count
  count+=1
  if depth>24 or count>12000:raise Rejected('NATIVE_STRUCTURE_BOUND')
  if isinstance(x,str):
   if len(x)>4096 or any(0xd800<=ord(c)<=0xdfff for c in x):raise Rejected('NATIVE_STRING_BOUND')
  elif isinstance(x,dict):
   if len(x)>512:raise Rejected('NATIVE_STRUCTURE_BOUND')
   for k,v in x.items():visit(k,depth+1);visit(v,depth+1)
  elif isinstance(x,list):
   if len(x)>500:raise Rejected('NATIVE_STRUCTURE_BOUND')
   for v in x:visit(v,depth+1)
 visit(result,0)
 if not isinstance(result,dict):raise Rejected('NATIVE_ROOT_OBJECT_REQUIRED')
 return result
def offline(*_):raise Rejected('REMOTE_SCHEMA_REFERENCE_DISABLED')
def configuration(directory):
 pins=json.loads((ROOT/'source_pins.json').read_text())
 for package,version in pins['packages'].items():
  if importlib.metadata.version(package)!=version:raise Rejected('NATIVE_DEPENDENCY_VERSION_MISMATCH')
 data=(directory/'itr1_schema.json').read_bytes()
 if len(data)!=148921 or hashlib.sha256(data).hexdigest()!=SCHEMA_SHA:raise Rejected('NATIVE_SCHEMA_PIN_MISMATCH')
 schema=json.loads(data,object_pairs_hook=pairs)
 def references(x):
  if isinstance(x,dict):
   if '$ref' in x and not x['$ref'].startswith('#/'):raise Rejected('REMOTE_SCHEMA_REFERENCE_DISABLED')
   for v in x.values():references(v)
  elif isinstance(x,list):
   for v in x:references(v)
 references(schema)
 from jsonschema import Draft4Validator
 from referencing import Registry
 Draft4Validator.check_schema(schema)
 return schema,Draft4Validator(schema,registry=Registry(retrieve=offline))
def public_fields(schema):
 names=set()
 def visit(x):
  if isinstance(x,dict):
   names.update(x.get('properties',{}))
   for v in x.values():visit(v)
  elif isinstance(x,list):
   for v in x:visit(v)
 visit(schema);return names
def safe_path(parts,names):return '/'+('/'.join(str(x) if isinstance(x,int) and 0<=x<=500 else x if isinstance(x,str) and x in names else 'field' for x in parts))
def base():return {'contract_version':'native-itr-preflight.v1','schema_id':SCHEMA_ID,'schema_sha256':SCHEMA_SHA,'schema_validation':'NOT_RUN','document_format':'UNKNOWN','proposal_pack':'UNAVAILABLE','issues':[],'warnings':[],'proposals':[],'business_rules_validated':False,'tax_arithmetic_checked':False,'source_authenticity':'NOT_VERIFIED','filing_available':False,'raw_document_retained':False}
def validate(raw,schema,validator):
 out=base();data=decode(raw);names=public_fields(schema);issues=[]
 for error in itertools.islice(validator.iter_errors(data),50):
  path=list(error.absolute_path)
  if error.validator=='required' and isinstance(error.instance,dict):
   for name in error.validator_value:
    if name not in error.instance:issues.append({'code':'SCHEMA_REQUIRED','path':safe_path([*path,name],names)})
  else:issues.append({'code':'SCHEMA_'+str(error.validator).upper(),'path':safe_path(path,names)})
 # No error.message, instance, unexpected property name or tax/identity value crosses the process boundary.
 out['issues']=list({(x['code'],x['path']):x for x in issues}.values())[:50]
 if issues:out['schema_validation']='FAIL';return out
 out['schema_validation']='PASS';itr=data.get('ITR',{}).get('ITR1') if isinstance(data.get('ITR'),dict) else None
 form=itr.get('Form_ITR1') if isinstance(itr,dict) else None
 if not isinstance(form,dict) or any(form.get(k)!=v for k,v in {'FormName':'ITR-1','AssessmentYear':'2026','SchemaVer':'Ver1.0','FormVer':'Ver1.0'}.items()):
  out['document_format']='UNSUPPORTED';out['issues']=[{'code':'EXACT_FORM_YEAR_VERSION_REQUIRED','path':'/ITR/ITR1/Form_ITR1'}];return out
 out['document_format']='MATCH';income=itr['ITR1_IncomeDeductions'];path='/ITR/ITR1/ITR1_IncomeDeductions';blocked=[]
 if income.get('PropertyDetails') or income.get('TotalIncomeChargeableUnHP',0)!=0:blocked.append('PROPERTY_INCOME_OUTSIDE_PROPOSAL_PACK')
 if itr.get('LTCG112A'):blocked.append('CAPITAL_GAINS_OUTSIDE_PROPOSAL_PACK')
 if income.get('ExemptIncAgriOthUs10',{}).get('ExemptIncAgriOthUs10Dtls'):blocked.append('EXEMPT_INCOME_REVIEW_OUTSIDE_PROPOSAL_PACK')
 rows=income.get('OthersInc',{}).get('OthersIncDtlsOthSrc',[]);mapping={'SAV':'SAVINGS_INTEREST','IFD':'DEPOSIT_INTEREST','TAX':'OTHER_INTEREST'}
 if any(row['OthSrcNatureDesc'] not in mapping for row in rows):blocked.append('OTHER_INCOME_CATEGORY_OUTSIDE_PROPOSAL_PACK')
 if income.get('DeductionUs57iia',0)!=0:blocked.append('SECTION57_DEDUCTION_OUTSIDE_PROPOSAL_PACK')
 if blocked:
  out['proposal_pack']='EXPANDED_WORKFLOW_REQUIRED';out['warnings']=blocked;return out
 proposals=[]
 def proposal(field,amount,source):
  if isinstance(amount,bool) or not isinstance(amount,int) or amount<0 or amount>999999999:
   out['warnings'].append('AMOUNT_ABOVE_WORKBENCH_BOUND');return
  proposals.append({'field':field,'amount':str(amount)+'.00','source_path':source})
 proposal('SALARY',income['GrossSalary'],path+'/GrossSalary')
 if sum(row['OthSrcOthAmount'] for row in rows)!=income['IncomeOthSrc']:
  out['warnings'].append('INTEREST_BREAKUP_TOTAL_REVIEW_REQUIRED')
 else:
  for category,field in mapping.items():
   entries=[(i,row) for i,row in enumerate(rows) if row['OthSrcNatureDesc']==category]
   if len(entries)==1:
    i,row=entries[0];proposal(field,row['OthSrcOthAmount'],path+'/OthersInc/OthersIncDtlsOthSrc/'+str(i)+'/OthSrcOthAmount')
   elif len(entries)>1:out['warnings'].append('MULTIPLE_'+category+'_OBSERVATIONS_NOT_AGGREGATED')
 out['proposals']=proposals;out['proposal_pack']='AVAILABLE' if proposals else 'UNAVAILABLE';out['warnings']+=['SOURCE_AND_ANNUAL_CATEGORY_COVERAGE_REQUIRE_REVIEW','NO_TAX_CREDIT_DEDUCTION_OR_PROFILE_TRANSFER','BUSINESS_VALIDATION_RULES_NOT_EXECUTED'];return out
def process_limits():
 if sys.platform.startswith('linux'):
  import resource
  resource.setrlimit(resource.RLIMIT_CPU,(3,3));resource.setrlimit(resource.RLIMIT_AS,(268435456,268435456));resource.setrlimit(resource.RLIMIT_FSIZE,(0,0));resource.setrlimit(resource.RLIMIT_CORE,(0,0))
 def audit(event,_):
  if event.startswith('socket.'):raise Rejected('NATIVE_WORKER_NETWORK_DISABLED')
 sys.addaudithook(audit)
def main():
 p=argparse.ArgumentParser();p.add_argument('--schema-dir',type=Path,required=True);p.add_argument('--self-check',action='store_true');args=p.parse_args();process_limits()
 try:
  schema,validator=configuration(args.schema_dir)
  if args.self_check:result={'status':'NATIVE_WORKER_CONFIGURED','schema_sha256':SCHEMA_SHA,'business_rules_validated':False,'filing_available':False}
  else:result=validate(sys.stdin.buffer.read(MAX_BYTES+1),schema,validator)
 except Rejected as e:result=base();result['issues']=[{'code':str(e),'path':'/'}]
 except Exception:result={'status':'NATIVE_WORKER_UNAVAILABLE'}
 sys.stdout.write(json.dumps(result,allow_nan=False,separators=(',',':'))+'\n')
 return 0 if not args.self_check or result.get('status')=='NATIVE_WORKER_CONFIGURED' else 2
if __name__=='__main__':raise SystemExit(main())
