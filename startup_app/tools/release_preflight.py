#!/usr/bin/env python3
"""Read-only engineering integrity and production-gap report. This cannot approve tax or enable live services."""
from pathlib import Path
import argparse,hashlib,json,sys
ROOT=Path(__file__).resolve().parents[1]
GOLDEN_SHA='e5fcaca1c7364e343f19ae52c7bd1577240a67a3446a508a39525814ccd01924'
CHECKS={'unit_test_results.json': ('startup_unit_groups', 27), 'self_service_test_results.json': ('self_service_named_groups', 27), 'workbench_unit_test_results.json': ('workbench_named_groups', 26), 'workbench_mutation_results.json': ('workbench_detected_defects', 8), 'http_test_results.json': ('startup_http_groups', 49), 'https_test_results.json': ('https_proxy_groups', 13), 'native_http_test_results.json': ('native_http_groups', 20), 'operations_http_test_results.json': ('operations_http_groups', 36), 'operations_mysql_http_test_results.json': ('operations_http_groups', 36), 'operations_concurrent_mysql_http_test_results.json': ('operations_http_groups', 36), 'mysql_http_test_results.json': ('startup_http_groups', 49), 'mysql_test_results.json': ('mysql_named_groups', 20), 'mysql_restore_test_results.json': ('mysql_restore_groups', 9), 'concurrent_mysql_http_test_results.json': ('startup_http_groups', 49), 'concurrent_mysql_test_results.json': ('concurrent_mysql_named_groups', 28), 'concurrent_restore_test_results.json': ('concurrent_restore_groups', 10), 'workbench_local_http_test_results.json': ('workbench_http_groups', 24), 'workbench_mysql_http_test_results.json': ('workbench_http_groups', 24), 'workbench_mysql_concurrent_http_test_results.json': ('workbench_http_groups', 24), 'browser_test_results.json': ('browser_checks_passed', 41), 'framed_backup_unit_test_results.json': ('framed_backup_unit_groups', 8), 'security_unit_test_results.json': ('security_named_groups', 20), 'security_mutation_results.json': ('security_detected_defects', 10), 'security_local_http_test_results.json': ('security_http_groups', 25), 'security_mysql_http_test_results.json': ('security_http_groups', 25), 'security_mysql_concurrent_http_test_results.json': ('security_http_groups', 25)}
CHECKS.update({'privacy_unit_test_results.json':('privacy_unit_groups',15), **{'privacy_'+profile+'_http_test_results.json':('privacy_http_groups',13) for profile in ['local','mysql','mysql_concurrent']}})
def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--mode',choices=['engineering','production'],default='engineering');args=p.parse_args();problems=[]
 try:
  build=json.loads((ROOT/'build/build_results.json').read_text());version=build['version'];jar=ROOT/'build'/('tax-platform-startup-'+version+'.jar');expected=build['jar_sha256']
  if digest(jar)!=expected:problems.append('JAR_HASH_MISMATCH')
  for name,sha in build['inputs'].items():
   if digest(ROOT/name)!=sha:problems.append('SOURCE_OR_RESOURCE_CHANGED:'+name)
  components={'tax-result-safety-0.1.4.jar':'java_result_safety','tax-planner-workbench-0.1.0.jar':'tax_engine','tax-preparation-local-demo-0.4.0.jar':'local_app'}
  for filename,sha in build['components'].items():
   if digest(ROOT.parent/components[filename]/'build'/filename)!=sha:problems.append('COMPONENT_CHANGED:'+filename)
  for name,(field,count) in CHECKS.items():
   report=json.loads((ROOT/'build'/name).read_text())
   if report.get(field)!=count or report.get('failures',0) or report.get('errors',0) or report.get('js_errors',[]):problems.append('FAILED_OR_MISSING_CHECK:'+name)
   if 'jar_sha256' in report and report['jar_sha256']!=expected:problems.append('TEST_ARTIFACT_MISMATCH:'+name)
   expected_skip=1 if name in ['mysql_http_test_results.json','concurrent_mysql_http_test_results.json','security_local_http_test_results.json','privacy_local_http_test_results.json'] else 0
   if report.get('skipped',0)!=expected_skip:problems.append('UNEXPECTED_SKIPPED_CHECK:'+name)
   for field in ['inputs','source_inputs','test_sources']:
    for source,sha in report.get(field,{}).items():
     if digest(ROOT/source)!=sha:problems.append('TEST_SOURCE_MISMATCH:'+name+':'+source)
  if digest(ROOT.parent/'golden_cases_v0.json')!=GOLDEN_SHA:problems.append('ORIGINAL_GOLDEN_CORPUS_CHANGED')
  corpus=json.loads((ROOT.parent/'golden_cases_v0.json').read_text());approved=corpus.get('approved_case_count',0)
  if len(corpus['cases'])!=30 or approved!=0:problems.append('GOLDEN_METADATA_REQUIRES_REVIEW')
 except (OSError,KeyError,ValueError) as e:problems.append('MISSING_OR_INVALID_ARTIFACT:'+type(e).__name__);version='unknown';expected='unknown';approved=0
 gates={'independent_tax_rule_release':False,'full_itr_gst_capital_gains_coverage':False,'authorized_live_filing':False,'live_payment_commercial_release':False,'verified_real_ca_service':False,'production_infrastructure_and_security':False,'measured_100000_user_capacity':False,'production_restore_promotion':False}
 output={'version':version,'mode':args.mode,'engineering_checks_ok':not problems,'engineering_issues':problems,'current_jar_sha256':expected,'independent_approved_tax_outputs':approved,'production_gates':gates,'production_ready':False,'tax_or_filing_authority_created':False,'synthetic_data_only':True};print(json.dumps(output,indent=2));return 0 if args.mode=='engineering' and not problems else 2
if __name__=='__main__':sys.exit(main())
