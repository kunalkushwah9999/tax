#!/usr/bin/env python3
"""Unapproved FY2025-26 listed-equity review arithmetic. Never a return or public tax result."""
from datetime import date
from decimal import Decimal, ROUND_HALF_UP
import argparse, calendar, hashlib, json, os, re, sys
from pathlib import Path

PERIOD='ITA1961-FY2025-26-AY2026-27'
DECLARATIONS={'synthetic_confirmed','all_accounts_complete','capital_investments_not_business',
 'ordinary_exchange_acquisitions','no_corporate_actions_or_gifts','no_other_capital_transactions_or_exemptions',
 'india_recognised_exchange_listed_equity_only','no_ifsc_or_special_stt_exceptions','no_clubbing_or_special_status',
 'no_prior_year_losses','expenses_exclude_stt_and_are_transfer_only','normalized_lot_values_checked',
 'ordinary_income_complete_after_deductions'}
FIELDS={'period','taxpayer_type','residency','regime','other_taxable_income','declarations','lots'}
LOT_FIELDS={'lot_id','account_alias','asset_alias','asset_type','acquired_on','sold_on','acquisition_cost_excluding_stt',
 'sale_consideration','transfer_expenses_excluding_stt','stt_on_acquisition','stt_on_transfer',
 'fmv_for_lot_2018_01_31','listed_on_2018_01_31'}
START,END=date(2025,4,1),date(2026,3,31)
MAX_BYTES=131072

def require(ok,code):
 if not ok:raise ValueError(code)
def money(value):
 if value=='UNKNOWN':return None
 require(isinstance(value,str) and re.fullmatch(r'(?:0|[1-9][0-9]{0,8})(?:\.[0-9]{1,2})?',value),'EXPLICIT_NONNEGATIVE_RUPEE_STRING_REQUIRED')
 return int(Decimal(value)*100)
def rupees(value):return format(Decimal(value)/100,'.2f')
def day(value):
 require(isinstance(value,str),'ISO_DATE_REQUIRED')
 try:d=date.fromisoformat(value)
 except ValueError:raise ValueError('ISO_DATE_REQUIRED') from None
 require(d.isoformat()==value and 1900<=d.year<=2026,'ISO_DATE_REQUIRED');return d
def alias(value):
 require(isinstance(value,str) and re.fullmatch(r'[A-Z][A-Z0-9_-]{0,31}',value) and not re.fullmatch(r'[A-Z]{5}[0-9]{4}[A-Z]',value),'SYNTHETIC_ALIAS_REQUIRED')
 return value
def strict_json(raw):
 require(len(raw)<=MAX_BYTES,'DOCUMENT_TOO_LARGE')
 def pairs(items):
  result={}
  for k,v in items:
   require(k not in result,'DUPLICATE_JSON_KEY');result[k]=v
  return result
 def invalid(_):raise ValueError('FINITE_JSON_ONLY')
 return json.loads(raw,object_pairs_hook=pairs,parse_constant=invalid)

def calculate(body):
 require(isinstance(body,dict) and set(body)==FIELDS,'EXACT_DOCUMENT_FIELDS_REQUIRED')
 require(len(json.dumps(body).encode())<=MAX_BYTES,'DOCUMENT_TOO_LARGE')
 declarations=body['declarations'];require(isinstance(declarations,dict) and set(declarations)==DECLARATIONS and all(type(v) is bool for v in declarations.values()),'EXPLICIT_DECLARATION_SET_REQUIRED')
 require(declarations['synthetic_confirmed'],'SYNTHETIC_DATA_REQUIRED')
 rows=body['lots'];require(isinstance(rows,list) and 1<=len(rows)<=120,'ONE_TO_120_MATCHED_LOTS_REQUIRED')
 blockers=[];work=[];seen=set()
 for k,v in [('period',PERIOD),('taxpayer_type','INDIVIDUAL'),('residency','ROR'),('regime','NEW')]:
  if body[k]!=v:blockers.append('UNSUPPORTED_'+k.upper())
 for k,v in declarations.items():
  if not v:blockers.append('DECLARATION_REQUIRED:'+k)
 ordinary=money(body['other_taxable_income'])
 if ordinary is None or ordinary<40000000:blockers.append('UNUSED_BASIC_EXEMPTION_REVIEW_REQUIRED')
 short=long=0
 for row in rows:
  require(isinstance(row,dict) and set(row)==LOT_FIELDS,'EXACT_LOT_FIELDS_REQUIRED')
  lot=alias(row['lot_id']);require(lot not in seen,'DUPLICATE_LOT_ID');seen.add(lot)
  alias(row['account_alias']);alias(row['asset_alias']);acquired,sold=day(row['acquired_on']),day(row['sold_on'])
  require(acquired<=sold,'ACQUISITION_AFTER_TRANSFER');require(START<=sold<=END,'SALE_OUTSIDE_FY2025_26')
  require(row['stt_on_acquisition'] in ['YES','NO','UNKNOWN'] and row['stt_on_transfer'] in ['YES','NO','UNKNOWN'],'STT_FACT_REQUIRED')
  require(type(row['listed_on_2018_01_31']) is bool,'EXPLICIT_2018_LISTING_FACT_REQUIRED')
  cost,proceeds,fees,fmv=[money(row[k]) for k in ['acquisition_cost_excluding_stt','sale_consideration','transfer_expenses_excluding_stt','fmv_for_lot_2018_01_31']]
  issues=[]
  if row['asset_type']!='INDIA_LISTED_EQUITY_SHARE':issues.append('UNSUPPORTED_ASSET_TYPE')
  if acquired==sold:issues.append('INTRADAY_OR_BUSINESS_REVIEW_REQUIRED')
  anniversary=date(acquired.year+1,acquired.month,min(acquired.day,calendar.monthrange(acquired.year+1,acquired.month)[1]))
  if sold==anniversary:issues.append('EXACT_HOLDING_ANNIVERSARY_REVIEW_REQUIRED')
  section='112A' if sold>anniversary else '111A'
  if row['stt_on_transfer']!='YES':issues.append('STT_TRANSFER_NOT_CONFIRMED')
  if section=='112A' and row['stt_on_acquisition']!='YES':issues.append('ACQUISITION_STT_EXCEPTION_REVIEW_REQUIRED')
  if cost is None or proceeds is None or fees is None:issues.append('LOT_AMOUNT_UNKNOWN')
  grandfathered=section=='112A' and acquired<date(2018,2,1)
  if grandfathered and (fmv is None or not row['listed_on_2018_01_31']):issues.append('GRANDFATHER_FMV_OR_LISTING_REVIEW_REQUIRED')
  if issues:
   blockers.extend(i+':'+lot for i in issues);continue
  tax_cost=max(cost,min(fmv,proceeds)) if grandfathered else cost
  gain=proceeds-fees-tax_cost
  if section=='111A':short+=gain
  else:long+=gain
  work.append({'lot_id':lot,'account_alias':row['account_alias'],'asset_alias':row['asset_alias'],
   'section_candidate':section,'cost_basis_method':'55_2_ac_CANDIDATE' if grandfathered else 'ACTUAL_COST',
   'cost_basis':rupees(tax_cost),'consideration':rupees(proceeds),'transfer_expenses':rupees(fees),'gain_or_loss':rupees(gain)})
 result={'module':'LISTED_EQUITY_REVIEW_0_1','period':PERIOD,'status':'BLOCKED' if blockers else 'CALCULATED_UNAPPROVED',
  'blockers':sorted(set(blockers)),'workings':None,'tax_rules_approved':False,'independent_approved_cases':0,
  'public_result_release_authorized':False,'filing_ready':False,'reference_expectation':False,
  'loss_carry_forward_eligibility':'NOT_DETERMINED','annual_tax':None,
  'omitted':['ordinary slab tax','annual rebate and marginal relief','surcharge','cess','288A/288B rounding','TDS and advance tax','filing interest and late fees','return eligibility and filing']}
 if blockers:return result
 # Net each category first; residual current-year short-term loss may reduce LTCG.
 st_tax_base=max(short,0);st_loss=max(-short,0);lt_positive=max(long,0);lt_loss=max(-long,0)
 offset=min(st_loss,lt_positive);lt_after=lt_positive-offset
 taxable_lt=max(lt_after-12500000,0)
 def component(paise,rate):return format((Decimal(paise)/100*Decimal(rate)).quantize(Decimal('.01'),rounding=ROUND_HALF_UP),'.2f')
 result['workings']={'lots':sorted(work,key=lambda r:r['lot_id']),'net_111a_before_cross_setoff':rupees(short),
  'net_112a_before_cross_setoff':rupees(long),'st_loss_applied_to_ltcg':rupees(offset),
  'unabsorbed_current_st_loss':rupees(st_loss-offset),'unabsorbed_current_lt_loss':rupees(lt_loss),
  '111a_base_before_annual_rounding':rupees(st_tax_base),'112a_after_current_loss_setoff':rupees(lt_after),
  '112a_annual_rate_band':rupees(min(12500000,lt_after)),'112a_excess_base_before_annual_rounding':rupees(taxable_lt),
  '111a_component_before_surcharge_cess':component(st_tax_base,'.20'),
  '112a_component_before_surcharge_cess':component(taxable_lt,'.125')}
 return result

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--input',type=Path,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
 if a.out.exists():p.error('Output must be a new file')
 try:
  with a.input.open('rb') as f:raw=f.read(MAX_BYTES+1)
  result=calculate(strict_json(raw));result['input_sha256']=hashlib.sha256(raw).hexdigest()
  result['implementation_sha256']=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
  source_manifest=Path(__file__).with_name('rule_sources.json')
  result['rule_sources_sha256']=hashlib.sha256(source_manifest.read_bytes()).hexdigest() if source_manifest.is_file() else None
 except (ValueError,TypeError,OSError) as e:
  print(json.dumps({'status':'INVALID_INPUT','error_type':type(e).__name__,'tax_rules_approved':False}));return 2
 descriptor=os.open(a.out,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
 with os.fdopen(descriptor,'w',encoding='utf-8') as f:json.dump(result,f,indent=2);f.write('\n')
 print(json.dumps({'status':result['status'],'workings_file':str(a.out),'filing_ready':False,'independent_approved_cases':0}));return 0 if result['status']=='CALCULATED_UNAPPROVED' else 2
if __name__=='__main__':sys.exit(main())
