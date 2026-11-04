#!/usr/bin/env python3
"""Compile selected deliberate defects in temporary copies and require behavioral failures."""
from pathlib import Path
import hashlib
import json
import os
import re
import shutil
import subprocess
import tempfile

root = Path(__file__).resolve().parent
sources = root/'src/main/java/com/taxplatform/safety'
build = root/'build'
java = shutil.which('java')
if java is None or not (build/'main').is_dir() or not (build/'test').is_dir():
    raise SystemExit('Run build.py with an installed JDK before mutation checks.')

mutations = [
 ('allow_pending_independent_references', 'ResultSafetyPolicy.java', 'SafetyTests',
  'if (context.referenceState() != ReferenceState.APPROVED) return new Blocked(Reason.INDEPENDENT_REFERENCE_NOT_APPROVED);',
  '// test-only defect: independent references ignored'),
 ('allow_unconfirmed_income', 'ResultSafetyPolicy.java', 'SafetyTests',
  'if (!issues.isEmpty()) return new ActionRequired(List.copyOf(issues));',
  'issues.clear(); // test-only defect: income warnings discarded'),
 ('allow_stale_calculation_identity', 'ResultSafetyPolicy.java', 'SafetyTests',
  'if (!candidate.key().equals(context.currentKey())) return new Blocked(Reason.STALE_RESULT);',
  '// test-only defect: stale cached result accepted'),
 ('allow_unavailable_regime', 'ResultSafetyPolicy.java', 'SafetyTests',
  'if (complete && availability == Availability.ALLOWED && calculation != null)',
  'if (complete && calculation != null)'),
 ('skip_early_mismatch_quarantine', 'SafeResultService.java', 'SafetyTests',
  'if (candidate.calculations().values().stream().anyMatch(c -> c.verification() == Verification.MISMATCH)) {',
  'if (false) { // test-only defect: defer mismatch to policy with no evidence write'),
 ('accept_forged_signature', 'CandidateProofVerifier.java', 'ProofTests',
  'return verification.verify(signed.signature()) ? Status.VALID : Status.INVALID;',
  'return Status.VALID; // test-only defect: cryptographic verification skipped'),
 ('ignore_revoked_key', 'CandidateProofVerifier.java', 'ProofTests',
  'key.status() != KeyStatus.ACTIVE', 'false /* test-only defect: revoked key accepted */'),
 ('ignore_proof_expiry', 'CandidateProofVerifier.java', 'ProofTests',
  '!header.expiresAt().isAfter(now)', 'false /* test-only defect: expiry ignored */'),
 ('auto_confirm_human_amount', 'HumanMoneyParser.java', 'MoneyParserTests',
  'return Fact.estimated(amount);', 'return Fact.confirmed(amount);'),
 ('ignore_conflicting_magnitude', 'HumanMoneyParser.java', 'MoneyParserTests',
  'if (suffixMagnitude != null && selectedMagnitude != null && suffixMagnitude != selectedMagnitude) {',
  'if (false) { // test-only defect: conflicting unit choices ignored'),
 ('reveal_guidance_for_blocked_result', 'GuidedResultService.java', 'GuidanceTests',
  'if (result instanceof Denied || result instanceof Blocked) {',
  'if (false) { // test-only defect: expose private guide despite result gate'),
 ('grant_filing_from_guidance', 'GuidedResultService.java', 'GuidanceTests',
  '"submit_return", false, "export_itr", false));',
  '"submit_return", true, "export_itr", false));'),
 ('ask_reviewed_field_instead_of_missing_field', 'GuidedResultService.java', 'GuidanceTests',
  'private static Map<String, Object> nextAction(PublicResult result, List<Step> steps) {\n        for (Step step : steps) {\n            if (!step.complete())',
  'private static Map<String, Object> nextAction(PublicResult result, List<Step> steps) {\n        for (Step step : steps) {\n            if (step.complete())'),
 ('accept_stale_intake_snapshot', 'IntakeGuideService.java', 'GuidanceTests',
  'if (!draft.key().equals(context.currentKey())) return unavailable(new Blocked(Reason.STALE_RESULT));',
  '// test-only defect: stale input draft accepted'),
 ('accept_numerical_candidate_as_input_draft', 'IntakeGuideService.java', 'GuidanceTests',
  'if (!draft.calculations().isEmpty()) {',
  'if (false) { // test-only defect: result candidate used as draft'),
 ('ignore_input_producer_proof', 'IntakeGuideService.java', 'GuidanceTests',
  'if (proof != CandidateProofVerifier.Status.VALID) return unavailable(new Blocked(Reason.PAYLOAD_INTEGRITY_FAILED));',
  '// test-only defect: untrusted input draft accepted'),
 ('ignore_edit_snapshot_identity', 'EphemeralFactLedger.java', 'FactEditTests',
  'if(!request.expectedKey().equals(before.current().key())) return FactEditResult.rejected(STALE_INPUT);',
  '// test-only defect: stale edit accepted'),
 ('ignore_changed_retry_body', 'EphemeralFactLedger.java', 'FactEditTests',
  'if(!prior.fingerprint().equals(fingerprint)) return FactEditResult.rejected(IDEMPOTENCY_CONFLICT);',
  '// test-only defect: changed request body treated as duplicate'),
 ('accept_wrong_confirmation_value', 'FactEditReducer.java', 'FactEditTests',
  'if(!confirm.displayedAmount().equals(estimate.value())) return rejected(FactEditResult.rejected(CONFIRMATION_VALUE_CHANGED));',
  '// test-only defect: displayed amount not bound to proposal'),
 ('auto_confirm_ledger_proposal', 'FactEditReducer.java', 'FactEditTests',
  '((HumanMoneyParser.Parsed)parsed).asUnconfirmedFact()',
  'Fact.confirmed(((HumanMoneyParser.Parsed)parsed).amount())'),
 ('preserve_reviews_after_amount_change', 'FactEditReducer.java', 'FactEditTests',
  'Fact.unknown(),Map.of(Regime.NEW,Fact.unknown(),Regime.OLD,Fact.unknown()));',
  'facts.incomeInventoryComplete(),facts.deductionInventoryComplete());'),
 ('allow_foreign_owner_edit', 'EphemeralFactLedger.java', 'FactEditTests',
  'if(session.closed || !session.owner.equals(actor)) return FactEditResult.rejected(ACCESS_DENIED);',
  'if(session.closed) return FactEditResult.rejected(ACCESS_DENIED);'),
 ('publish_edit_before_commit_probe', 'EphemeralFactLedger.java', 'FactEditTests',
  'try { publishProbe.beforePublish(); }\n            catch(SafeResultService.ControlStateUnavailable unavailable) { return FactEditResult.rejected(CONTROL_STATE_UNAVAILABLE); }\n            session.data=after;',
  'session.data=after;\n            try { publishProbe.beforePublish(); }\n            catch(SafeResultService.ControlStateUnavailable unavailable) { return FactEditResult.rejected(CONTROL_STATE_UNAVAILABLE); }'),
 ('allow_edit_to_escape_quarantine', 'EphemeralFactLedger.java', 'FactEditTests',
  'if(before.quarantined().contains(before.current().key())) return FactEditResult.rejected(SYSTEM_REVIEW_REQUIRED);',
  '// test-only defect: edited snapshot can escape quarantine'),
 ('ignore_professional_grant_expiry', 'ProfessionalQueueService.java', 'ProfessionalQueueTests',
  '!now.isBefore(access.validUntil())', 'false /* test-only defect: expired grant accepted */'),
 ('ignore_professional_credential_state', 'ProfessionalQueueService.java', 'ProfessionalQueueTests',
  'access.credentialState()!=CredentialState.VERIFIED', 'false /* test-only defect: unverified credential accepted */'),
 ('show_evidence_for_blocked_result', 'ProfessionalQueueService.java', 'ProfessionalQueueTests',
  'if(result instanceof Blocked) return row(index,Route.RESULT_BLOCKED,List.of());',
  '// test-only defect: blocked result exposes evidence readiness'),
 ('treat_submitted_evidence_as_reviewed', 'ProfessionalQueueService.java', 'ProfessionalQueueTests',
  's->s!=EvidenceState.REVIEWED && s!=EvidenceState.NOT_REQUIRED',
  's->s!=EvidenceState.REVIEWED && s!=EvidenceState.NOT_REQUIRED && s!=EvidenceState.SUBMITTED'),
 ('hide_required_professional_action', 'ProfessionalQueueService.java', 'ProfessionalQueueTests',
  'if(access.workKind()==WorkKind.STATUTORY_PROFESSIONAL_ACTION || access.workKind()==WorkKind.SPECIALIST_REVIEW) return Route.PROFESSIONAL_ACTION_REQUIRED;',
  '// test-only defect: required professional workflow suppressed'),
 ('grant_queue_bulk_approval', 'ProfessionalQueue.java', 'ProfessionalQueueTests',
  '"bulk_approve",false', '"bulk_approve",true'),
]
originals = {filename:(sources/filename).read_text() for _,filename,_,_,_ in mutations}
original_hashes = {filename:hashlib.sha256((sources/filename).read_bytes()).hexdigest() for filename in originals}
results = []
for name, filename, suite, before, after in mutations:
    original = originals[filename]
    if original.count(before) != 1:
        raise SystemExit('Mutation anchor is ambiguous: '+name)
    with tempfile.TemporaryDirectory(prefix='tax-safety-mutation-') as temporary:
        temp = Path(temporary)
        changed = temp/filename
        changed.write_text(original.replace(before, after), encoding='utf-8')
        classes = temp/'classes'; classes.mkdir()
        compiled = subprocess.run([java, '-m', 'jdk.compiler/com.sun.tools.javac.Main',
            '--release','17','-encoding','UTF-8','-proc:none','-Xlint:all','-Werror','-cp',str(build/'main'),
            '-d',str(classes),str(changed)], text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=60)
        if compiled.returncode:
            raise SystemExit('Mutation did not compile; cannot count compiler rejection as behavioral detection: '+name)
        tested = subprocess.run([java, '-cp',os.pathsep.join([str(classes),str(build/'main'),str(build/'test')]),
            'com.taxplatform.safety.'+suite], text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=90)
        match = re.search(r'^SCENARIOS_FAILED=(\d+)$', tested.stdout, re.M)
        if tested.returncode == 0 or match is None or int(match.group(1)) == 0:
            raise SystemExit('A deliberately faulty component was not detected: '+name)
        results.append({'mutation':name, 'source_file':filename, 'test_suite':suite, 'compiled':True,
            'behavioral_failure_detected':True, 'named_scenarios_failed':int(match.group(1)),
            'example_failures':[line[5:] for line in tested.stdout.splitlines() if line.startswith('FAIL ')][:6]})
        print('DETECTED '+name+' ('+match.group(1)+' scenario groups)', flush=True)
for filename, expected_hash in original_hashes.items():
    if hashlib.sha256((sources/filename).read_bytes()).hexdigest() != expected_hash:
        raise SystemExit('Original source changed during mutation checks: '+filename)
report = {'scope':'Selected delivery, proof, parser, guidance, edit and professional-review mutations; not exhaustive coverage or tax validation.',
          'mutations_checked':len(results), 'mutations_detected':len(results),
          'original_source_preserved':True, 'results':results}
(build/'mutation_results.json').write_text(json.dumps(report, indent=2)+'\n')
print(json.dumps(report, indent=2))
