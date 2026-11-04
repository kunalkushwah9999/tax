#!/usr/bin/env python3
"""Build/test the Java 17 library with installed JDK modules; no dependency download."""
from pathlib import Path
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys

root = Path(__file__).resolve().parent
build = root / 'build'
java = shutil.which('java')
if java is None:
    raise SystemExit('Java 17+ with jdk.compiler and jdk.jartool modules is required.')

def execute(arguments):
    result = subprocess.run([java, *arguments], cwd=root, text=True,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, check=False)
    if result.returncode:
        print(result.stdout)
        raise SystemExit(result.returncode)
    return result.stdout

if build.is_symlink():
    raise SystemExit('Refusing to write through a symlinked build directory.')
build.mkdir(exist_ok=True)
for name in ['main', 'test']:
    target = build / name
    if target.is_symlink():
        raise SystemExit('Refusing to replace a symlinked class output directory.')
    if target.exists(): shutil.rmtree(target)
    target.mkdir()

compiler = ['-m', 'jdk.compiler/com.sun.tools.javac.Main', '--release', '17',
            '-encoding', 'UTF-8', '-proc:none', '-Xlint:all', '-Werror']
main = sorted(str(p) for p in (root/'src/main/java').rglob('*.java'))
tests = sorted(str(p) for p in (root/'src/test/java').rglob('*.java'))
if not main or not tests:
    raise SystemExit('Missing source or tests; build is incomplete.')
execute([*compiler, '-d', str(build/'main'), *main])
execute([*compiler, '-cp', str(build/'main'), '-d', str(build/'test'), *tests])
result = execute(['-cp', os.pathsep.join([str(build/'main'), str(build/'test')]),
                  'com.taxplatform.safety.SafetyTests', str(build/'example_responses.json')])
metrics = dict(re.findall(r'^(SCENARIOS_PASSED|SCENARIOS_FAILED|GENERATED_GATE_COMBINATIONS)=(\d+)$', result, re.M))
if set(metrics) != {'SCENARIOS_PASSED','SCENARIOS_FAILED','GENERATED_GATE_COMBINATIONS'}:
    raise SystemExit('Missing test completion metrics; build is incomplete.')
if int(metrics['SCENARIOS_FAILED']) or not int(metrics['SCENARIOS_PASSED']):
    raise SystemExit('Policy checks did not pass.')
suite_reports = {'SafetyTests': {'named_passed':int(metrics['SCENARIOS_PASSED']), 'named_failed':0}}
for suite, generated_metric in [('ProofTests','SIGNED_FIELD_BINDINGS'), ('MoneyParserTests','GENERATED_PARSER_CASES'),
                                 ('GuidanceTests','GENERATED_GUIDANCE_CASES'), ('FactEditTests','GENERATED_EDIT_TRANSITIONS'),
                                 ('ProfessionalQueueTests','GENERATED_REVIEW_COMBINATIONS')]:
    output_file = {'GuidanceTests':'guided_examples.json','FactEditTests':'fact_edit_examples.json',
                   'ProfessionalQueueTests':'professional_examples.json'}.get(suite)
    extra = [str(build/output_file)] if output_file else []
    output = execute(['-cp', os.pathsep.join([str(build/'main'), str(build/'test')]),
                      'com.taxplatform.safety.'+suite, *extra])
    print(output.strip())
    suite_metrics = dict(re.findall(r'^(SCENARIOS_PASSED|SCENARIOS_FAILED|'+generated_metric+r')=(\d+)$', output, re.M))
    if set(suite_metrics) != {'SCENARIOS_PASSED','SCENARIOS_FAILED',generated_metric}:
        raise SystemExit('Missing test completion metrics for '+suite)
    if int(suite_metrics['SCENARIOS_FAILED']) or not int(suite_metrics['SCENARIOS_PASSED']):
        raise SystemExit('Suite did not pass: '+suite)
    suite_reports[suite] = {'named_passed':int(suite_metrics['SCENARIOS_PASSED']), 'named_failed':0,
                            generated_metric.lower():int(suite_metrics[generated_metric])}
    if suite == 'FactEditTests':
        for metric in ['CONCURRENT_LEDGER_ROUNDS','CONCURRENT_LEDGER_REQUESTS']:
            observed=re.search(r'^'+metric+r'=(\d+)$',output,re.M)
            if observed is None: raise SystemExit('Missing concurrent ledger completion metric: '+metric)
            suite_reports[suite][metric.lower()]=int(observed.group(1))
professional_examples=json.loads((build/'professional_examples.json').read_text())
professional_replica=build/'professional_examples_second_jvm.json'
execute(['-cp',os.pathsep.join([str(build/'main'),str(build/'test')]),
         'com.taxplatform.safety.ProfessionalQueueTests','--examples-only',str(professional_replica)])
if professional_replica.read_bytes()!=(build/'professional_examples.json').read_bytes():
    raise SystemExit('Professional queue examples changed across JVMs.')
professional_replica.unlink()
for example in professional_examples:
    response=example['response']
    if any(response['capabilities'].values()): raise SystemExit('Review queue upgraded approval/filing capabilities.')
    if response['professional_need_rate']!='NOT_MEASURED' or response['tax_correctness_rate']!='NOT_ESTABLISHED':
        raise SystemExit('Unmeasured professional-need or accuracy claim appeared.')
    counts=sum(int(v) for v in response['counts_by_route'].values())
    if counts!=int(response['requested_case_count']): raise SystemExit('Queue denominator omitted a requested case.')
    for row in response['cases']:
        if row['route'] in ['ACCESS_DENIED','FEATURE_DISABLED','UNAVAILABLE','STALE_CASE','RESULT_BLOCKED'] and 'evidence_checklist' in row:
            raise SystemExit('Ineligible queue row leaked private evidence metadata.')
    if any(v in json.dumps(response) for v in ['98765.43','420.00','700.00','reviewerA','tenantA','annual_taxes']):
        raise SystemExit('Professional queue leaked values, IDs or tax payload.')
edit_examples = json.loads((build/'fact_edit_examples.json').read_text())
edit_replica = build/'fact_edit_examples_second_jvm.json'
execute(['-cp',os.pathsep.join([str(build/'main'),str(build/'test')]),
         'com.taxplatform.safety.FactEditTests','--examples-only',str(edit_replica)])
if edit_replica.read_bytes()!=(build/'fact_edit_examples.json').read_bytes():
    raise SystemExit('Edit receipt/history projections changed across JVMs.')
edit_replica.unlink()
for example in edit_examples:
    response=example['response']
    serialized=json.dumps(response)
    if any(x in serialized for x in ['98765.43','100000.00','200000.00','ownerA','tenantA','annual_taxes']):
        raise SystemExit('Edit projection exposed a private value or numerical tax payload.')
    if response['contract_version']=='fact-edit.v1':
        if response['capabilities']['submit_return'] or response['capabilities']['export_itr']:
            raise SystemExit('Edit receipt granted filing authority.')
        if response['status'] in ['APPLIED','NO_CHANGE','REPLAYED'] and not response.get('refresh_current_draft'):
            raise SystemExit('Historical command receipt was presented without refresh requirement.')
guided_examples = json.loads((build/'guided_examples.json').read_text())
guided_replica = build/'guided_examples_second_jvm.json'
execute(['-cp', os.pathsep.join([str(build/'main'),str(build/'test')]),
         'com.taxplatform.safety.GuidanceTests','--examples-only',str(guided_replica)])
if guided_replica.read_bytes() != (build/'guided_examples.json').read_bytes():
    raise SystemExit('Guidance output changed across separate JVMs.')
guided_replica.unlink()
for example in guided_examples:
    response = example['response']
    guide = response['guide']
    for capability in ['submit_return','export_itr']:
        if guide['capabilities'][capability]: raise SystemExit('Guidance upgraded filing/export authority.')
    if 'result' in response:
        inner = response['result']
        if inner['status'] in ['BLOCKED','ACCESS_DENIED']:
            if any(k in guide for k in ['checklist','progress_label','regime_cards','preparation_note']):
                raise SystemExit('Blocked result leaked private guidance.')
    elif 'annual_taxes' in json.dumps(response) or 'difference_in_annual_tax' in json.dumps(response):
        raise SystemExit('Intake preparation leaked a numerical result.')
    if any(v in guide.get('preparation_note','') for v in ['98765.43','420.00','700.00','tenantA','profileA','caseA']):
        raise SystemExit('Preparation note contains values or personal IDs.')
canonical = build/'canonical_vector.bin'
replica_canonical = build/'canonical_vector_second_jvm.bin'
for target in [canonical, replica_canonical]:
    execute(['-cp', os.pathsep.join([str(build/'main'), str(build/'test')]),
             'com.taxplatform.safety.ProofTests', '--canonical-only', str(target)])
if canonical.read_bytes() != replica_canonical.read_bytes():
    raise SystemExit('Signing bytes changed across separate JVM runs.')
canonical_report = {'format':'tax-platform/signed-candidate/v1', 'synthetic_fixture_only':True,
                    'byte_length':len(canonical.read_bytes()), 'sha256':hashlib.sha256(canonical.read_bytes()).hexdigest(),
                    'canonical_bytes_match_across_two_jvms':True}
(build/'canonical_vector.json').write_text(json.dumps(canonical_report, indent=2)+'\n')
canonical.unlink(); replica_canonical.unlink()
examples = json.loads((build/'example_responses.json').read_text())
replica_example=build/'example_responses_second_jvm.json'
execute(['-cp',os.pathsep.join([str(build/'main'),str(build/'test')]),
         'com.taxplatform.safety.SafetyTests','--examples-only',str(replica_example)])
if replica_example.read_bytes()!=(build/'example_responses.json').read_bytes():
    raise SystemExit('Closed projection output changed across separate JVM runs.')
replica_example.unlink()
for example in examples:
    response = example['policy_response']
    if response['status'] != 'CALCULATION_AVAILABLE':
        if 'annual_taxes' in response or 'comparison' in response:
            raise SystemExit('Blocked projection contains numerical output.')
    for tax in response.get('annual_taxes', []):
        if not isinstance(tax['annual_tax_before_credits'], str):
            raise SystemExit('Money was encoded as a JSON floating point number.')
    if response['capabilities']['submit_return'] or response['capabilities']['export_itr']:
        raise SystemExit('Calculation-only module exposed filing authority.')
artifact = build/'tax-result-safety-0.1.4.jar'
execute(['-m', 'jdk.jartool/sun.tools.jar.Main', '--create', '--file', str(artifact),
         '-C', str(build/'main'), '.'])
report = {'module':'tax-result-safety','version':'0.1.4',
          'java_runtime':execute(['-version']).strip(),
          'named_scenarios_passed':sum(s['named_passed'] for s in suite_reports.values()),
          'named_scenarios_failed':0,
          'generated_gate_combinations':int(metrics['GENERATED_GATE_COMBINATIONS']),
          'generated_parser_cases':suite_reports['MoneyParserTests']['generated_parser_cases'],
          'signed_field_bindings':suite_reports['ProofTests']['signed_field_bindings'],
          'generated_guidance_cases':suite_reports['GuidanceTests']['generated_guidance_cases'],
          'generated_edit_transitions':suite_reports['FactEditTests']['generated_edit_transitions'],
          'concurrent_ledger_rounds':suite_reports['FactEditTests']['concurrent_ledger_rounds'],
          'concurrent_ledger_requests':suite_reports['FactEditTests']['concurrent_ledger_requests'],
          'generated_professional_review_combinations':suite_reports['ProfessionalQueueTests']['generated_review_combinations'],
          'suites':suite_reports,
          'compiler_flags':['--release 17','-encoding UTF-8','-proc:none','-Xlint:all','-Werror'],
          'example_json_parsed':len(examples),
          'guided_example_json_parsed':len(guided_examples),
          'edit_example_json_parsed':len(edit_examples),
          'professional_example_json_parsed':len(professional_examples),
          'professional_projection_bytes_match_across_two_jvms':True,
          'edit_projection_bytes_match_across_two_jvms':True,
          'guided_projection_bytes_match_across_two_jvms':True,
          'projection_bytes_match_across_two_jvms':True,
          'canonical_signing_bytes_match_across_two_jvms':True,
          'jar_sha256':hashlib.sha256(artifact.read_bytes()).hexdigest(),
          'external_dependencies':[],
          'tax_correctness_validated':False,'production_integration_validated':False,
          'sql_integration_validated':False,'durable_session_storage':False}
(build/'test_results.json').write_text(json.dumps(report,indent=2)+'\n')
print(result.strip())
print(json.dumps(report,indent=2))
