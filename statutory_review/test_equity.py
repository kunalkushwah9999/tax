#!/usr/bin/env python3
"""Engineering checks only; expected constants are not independently approved tax references."""
from pathlib import Path
from copy import deepcopy
import hashlib,json,os,random,stat,subprocess,sys,tempfile,unittest
import equity

def lot(identifier='LOT_A',*,long=False,cost='100000.00',sale='200000.00',fees='0.00',**extra):
 r={'lot_id':identifier,'account_alias':'ACCOUNT_A','asset_alias':'ASSET_A','asset_type':'INDIA_LISTED_EQUITY_SHARE','acquired_on':'2024-04-01' if long else '2025-04-01','sold_on':'2025-06-02','acquisition_cost_excluding_stt':cost,'sale_consideration':sale,'transfer_expenses_excluding_stt':fees,'stt_on_acquisition':'YES','stt_on_transfer':'YES','fmv_for_lot_2018_01_31':'UNKNOWN','listed_on_2018_01_31':False};r.update(extra);return r
def document(*lots):
 return {'period':equity.PERIOD,'taxpayer_type':'INDIVIDUAL','residency':'ROR','regime':'NEW','other_taxable_income':'800000.00','declarations':{k:True for k in sorted(equity.DECLARATIONS)},'lots':list(lots or [lot()])}
def workings(body):
 result=equity.calculate(body)
 if result['status']!='CALCULATED_UNAPPROVED':raise AssertionError(result['blockers'])
 return result['workings']

class Equity(unittest.TestCase):
 def test_01_short_term_rate_and_transfer_expense(self):
  r=workings(document(lot(fees='1000.00')));self.assertEqual(r['111a_base_before_annual_rounding'],'99000.00');self.assertEqual(r['111a_component_before_surcharge_cess'],'19800.00')
 def test_02_long_term_threshold_and_rate(self):
  r=workings(document(lot(long=True,sale='600000.00')));self.assertEqual(r['112a_excess_base_before_annual_rounding'],'375000.00');self.assertEqual(r['112a_component_before_surcharge_cess'],'46875.00')
 def test_03_annual_threshold_is_shared_across_accounts_and_lots(self):
  b=document(lot('LOT_A',long=True),lot('LOT_B',long=True,account_alias='ACCOUNT_B'));r=workings(b);self.assertEqual(r['112a_excess_base_before_annual_rounding'],'75000.00');self.assertEqual(r['112a_component_before_surcharge_cess'],'9375.00')
 def test_04_threshold_below_at_and_one_rupee_above(self):
  for gain,expected in [('124999.00','0.00'),('125000.00','0.00'),('125001.00','0.13')]:
   with self.subTest(gain=gain):self.assertEqual(workings(document(lot(long=True,cost='0.00',sale=gain)))['112a_component_before_surcharge_cess'],expected)
 def test_05_current_st_loss_offsets_st_first_then_lt(self):
  b=document(lot('ST_GAIN',sale='140000.00'),lot('ST_LOSS',cost='200000.00',sale='100000.00'),lot('LT_GAIN',long=True,sale='300000.00'))
  r=workings(b);self.assertEqual(r['111a_base_before_annual_rounding'],'0.00');self.assertEqual(r['st_loss_applied_to_ltcg'],'60000.00');self.assertEqual(r['112a_after_current_loss_setoff'],'140000.00');self.assertEqual(r['112a_component_before_surcharge_cess'],'1875.00')
 def test_06_long_loss_never_reduces_short_gains(self):
  r=workings(document(lot('ST_GAIN'),lot('LT_LOSS',long=True,cost='200000.00',sale='100000.00')));self.assertEqual(r['111a_base_before_annual_rounding'],'100000.00');self.assertEqual(r['unabsorbed_current_lt_loss'],'100000.00')
 def test_07_unabsorbed_losses_do_not_grant_carry_forward(self):
  r=equity.calculate(document(lot(sale='50000.00')));self.assertEqual(r['workings']['unabsorbed_current_st_loss'],'50000.00');self.assertEqual(r['loss_carry_forward_eligibility'],'NOT_DETERMINED');self.assertIsNone(r['annual_tax'])
 def test_08_pre2018_cost_formula_never_forces_actual_cost_down(self):
  for actual,fmv,sale,cost in [('100.00','200.00','300.00','200.00'),('100.00','400.00','300.00','300.00'),('400.00','200.00','300.00','400.00')]:
   b=document(lot(long=True,acquired_on='2017-03-01',cost=actual,sale=sale,fmv_for_lot_2018_01_31=fmv,listed_on_2018_01_31=True))
   with self.subTest(actual=actual,fmv=fmv,sale=sale):self.assertEqual(workings(b)['lots'][0]['cost_basis'],cost)
 def test_09_missing_old_fmv_or_listing_blocks_every_total(self):
  for value in ['UNKNOWN','100000.00']:
   b=document(lot('OLD_LOT',long=True,acquired_on='2018-01-31',fmv_for_lot_2018_01_31=value),lot('GOOD_LOT'))
   r=equity.calculate(b);self.assertEqual(r['status'],'BLOCKED');self.assertIsNone(r['workings'])
 def test_10_post2018_uses_actual_cost_without_fmv(self):
  r=workings(document(lot(long=True,acquired_on='2018-02-01')));self.assertEqual(r['lots'][0]['cost_basis_method'],'ACTUAL_COST')
 def test_11_stt_conditions_and_exceptions_are_not_guessed(self):
  for long,key in [(False,'stt_on_transfer'),(True,'stt_on_transfer'),(True,'stt_on_acquisition')]:
   b=document(lot(long=long,**{key:'UNKNOWN'}));self.assertIsNone(equity.calculate(b)['workings'])
  b=document(lot(stt_on_acquisition='NO'));self.assertEqual(equity.calculate(b)['status'],'CALCULATED_UNAPPROVED')
  for asset in ['LISTED_BOND','EQUITY_MUTUAL_FUND','VDA','UNKNOWN']:self.assertIsNone(equity.calculate(document(lot(asset_type=asset)))['workings'])
 def test_12_holding_anniversary_and_intraday_require_review(self):
  for acquired,sold in [('2024-06-02','2025-06-02'),('2025-02-28','2026-02-28'),('2025-06-02','2025-06-02')]:
   r=equity.calculate(document(lot(acquired_on=acquired,sold_on=sold)));self.assertEqual(r['status'],'BLOCKED');self.assertIsNone(r['workings'])
 def test_13_other_regimes_status_year_and_unused_exemption_block(self):
  for key,value in [('regime','OLD'),('residency','NR'),('taxpayer_type','HUF'),('period','2026-27'),('other_taxable_income','399999.99'),('other_taxable_income','UNKNOWN')]:
   b=document();b[key]=value
   with self.subTest(key=key,value=value):self.assertIsNone(equity.calculate(b)['workings'])
 def test_14_each_missing_declaration_blocks_no_false_completion(self):
  for name in equity.DECLARATIONS-{'synthetic_confirmed'}:
   b=document();b['declarations'][name]=False;self.assertEqual(equity.calculate(b)['status'],'BLOCKED')
  b=document();b['declarations']['synthetic_confirmed']=False
  with self.assertRaises(ValueError):equity.calculate(b)
 def test_15_unknown_amount_is_not_zero(self):
  for k in ['acquisition_cost_excluding_stt','sale_consideration','transfer_expenses_excluding_stt']:
   self.assertIsNone(equity.calculate(document(lot(**{k:'UNKNOWN'})))['workings'])
 def test_16_strict_schema_numbers_aliases_and_dates(self):
  for value in [1.0,'1e5','1,000','-1.00','1.001','NaN']:
   with self.subTest(value=value),self.assertRaises(ValueError):equity.calculate(document(lot(sale=value)))
  for changed in [dict(account_alias='ABCDE1234F'),dict(acquired_on='2025-07-01'),dict(sold_on='2026-04-01'),dict(sold_on='2025-2-1')]:
   with self.subTest(changed=changed),self.assertRaises(ValueError):equity.calculate(document(lot(**changed)))
  b=document();b['PAN']='ABCDE1234F'
  with self.assertRaises(ValueError):equity.calculate(b)
 def test_17_duplicate_ids_json_keys_and_size_limits(self):
  with self.assertRaises(ValueError):equity.calculate(document(lot(),lot()))
  for raw in [b'{"a":1,"a":2}',b'{"a":NaN}',b' '*131073]:
   with self.assertRaises(ValueError):equity.strict_json(raw)
  with self.assertRaises(ValueError):equity.calculate(document(*[lot('LOT_'+str(i)) for i in range(121)]))
 def test_18_engineering_result_has_no_public_or_filing_authority(self):
  r=equity.calculate(document());self.assertFalse(r['tax_rules_approved']);self.assertFalse(r['public_result_release_authorized']);self.assertFalse(r['filing_ready']);self.assertFalse(r['reference_expectation']);self.assertEqual(r['independent_approved_cases'],0);self.assertIsNone(r['annual_tax'])
 def test_19_deterministic_200_generated_integer_paise_setoff_cases(self):
  randomizer=random.Random(20261103)
  for i in range(200):
   st,lt=[randomizer.randrange(-10000000,30000000) for _ in range(2)]
   make=lambda n,g,long:lot(n,long=long,cost=equity.rupees(max(-g,0)),sale=equity.rupees(max(g,0)))
   b=document(make('ST_LOT',st,False),make('LT_LOT',lt,True));r=workings(b);applied=min(max(-st,0),max(lt,0))
   self.assertEqual(equity.money(r['st_loss_applied_to_ltcg']),applied)
   self.assertEqual(equity.money(r['112a_after_current_loss_setoff'])+applied,max(lt,0))
   self.assertEqual(equity.money(r['unabsorbed_current_st_loss'])+applied,max(-st,0))
   b['lots'].reverse();self.assertEqual(r,workings(b))
 def test_20_actual_cli_private_output_and_no_overwrite(self):
  with tempfile.TemporaryDirectory() as temp:
   temp=Path(temp);source=temp/'input.json';out=temp/'workings.json';source.write_text(json.dumps(document()));command=[sys.executable,str(Path(equity.__file__)),'--input',str(source),'--out',str(out)]
   r=subprocess.run(command,capture_output=True,text=True);self.assertEqual(r.returncode,0,r.stderr);result=json.loads(out.read_text());self.assertEqual(result['status'],'CALCULATED_UNAPPROVED');self.assertEqual(result['implementation_sha256'],hashlib.sha256(Path(equity.__file__).read_bytes()).hexdigest());self.assertEqual(result['input_sha256'],hashlib.sha256(source.read_bytes()).hexdigest());self.assertNotIn('19800',r.stdout)
   if os.name!='nt':self.assertEqual(stat.S_IMODE(out.stat().st_mode),0o600)
   before=out.read_bytes();self.assertEqual(subprocess.run(command,capture_output=True).returncode,2);self.assertEqual(out.read_bytes(),before)
   source.write_text('{"a":1,"a":2}');other=temp/'invalid.json';command[-1]=str(other);self.assertEqual(subprocess.run(command,capture_output=True).returncode,2);self.assertFalse(other.exists())

if __name__=='__main__':
 result=unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(Equity))
 report={'equity_engineering_test_groups':result.testsRun,'generated_paise_conservation_cases':200,'failures':len(result.failures),'errors':len(result.errors),'independent_approved_cases':0,'tax_rules_approved':False,'public_result_release_authorized':False,'filing_ready':False,'sources':{name:hashlib.sha256((Path(__file__).parent/name).read_bytes()).hexdigest() for name in ['equity.py','test_equity.py']}}
 (Path(__file__).parent/'engineering_test_results.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2));sys.exit(not result.wasSuccessful())
