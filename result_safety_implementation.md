# Result safety implementation

31 October 2026. Component version 0.1.4; public projection contract result-safety.v1.

## Delivered

A standalone Java 17 result-safety library with source, compiled JAR, portable
build runner, scenario tests, selected mutation checks and synthetic response
examples. This is the implemented core of Task 1, pending application integration.
The original backend project, calculator and database were not supplied or patched.

The module runs without Maven, Gradle, network or external Java dependencies.
`java_result_safety/README.md` gives the full build and integration contract.

## Files and responsibilities

| File | Responsibility |
|---|---|
| Fact.java | Sealed confirmed, unknown, estimated and conflicting fact states; unknown has no value slot |
| Money.java | Exact nonnegative decimal transport values; no statutory rounding or tax math |
| CalculationKey.java | Tenant/profile/case/snapshot/period and validation-artifact identity |
| SafetyInput.java | Immutable trusted server inputs and explicit availability/approval states |
| ResultSafetyPolicy.java | Pure result-exposure policy; no DB, HTTP, LLM or tax calculation |
| PublicResult.java | Restricted sealed response hierarchy and explicit capabilities |
| SafeJson.java | Closed-projection encoding; blocked responses have no tax/saving payload |
| SafeResultService.java | Reload control state for each delivery and call quarantine adapter on eligible mismatch |
| SafetyTests.java | Scenario tests, generated guard combinations and negative compilation checks |
| build.py | Portable compiler/test/JAR runner using installed JDK modules |
| SignedCandidate.java / CandidateCanonicalizer.java | Defensive signed wrapper and bounded versioned signing bytes |
| CandidateProofVerifier.java / SignedResultService.java | Ed25519 proof, current key trust and guarded cached delivery |
| HumanMoneyParser.java | Exact English/Hindi amount proposals; all results require confirmation |
| ProofTests.java / MoneyParserTests.java | Actual signatures, service failures, exact amounts and confirmation boundary |
| GuidedResultService.java / GuidedResult.java | Post-gate customer explanations, checklist, status cards and preparation note |
| IntakeGuideService.java / IntakeGuide.java | Current signed input-only preparation before numerical calculation/approval |
| GuidanceTests.java | Guided action, privacy, progress, note and entry-point boundaries |
| FactEdits.java / FactEditReducer.java | Explicit user-intent commands, exact confirmation and dependency resets |
| EphemeralFactLedger.java | Bounded single-process atomic session edits, retry binding, history and current control identity |
| FactEditResult.java / FactEditHistory.java | Restricted amount-free receipt/history projections |
| FactEditTests.java | Session/action boundaries, injected publication failure and actual-thread races |
| ProfessionalReview.java / ProfessionalQueueService.java | Trusted professional assignment/evidence metadata and read-only per-case routing |
| ProfessionalQueue.java / ProfessionalQueueTests.java | Restricted readiness projection and evidence/permission/denominator checks |
| check_mutations.py | Thirty selected defects injected into temporary compiled source copies |

## Behavior enforced by the module

- Unknown, estimated, conflicting or incomplete income cannot produce a complete
  tax amount. Confirmed zero remains a real amount; unknown never becomes zero.
- Partial, unsupported or unknown scope suppresses combined tax and comparison.
- Any supplied verifier mismatch blocks the whole result. Unrun verification also
  suppresses amounts. No averaging or choosing a convenient implementation exists.
- Old-regime deductions can remain unknown while a complete eligible new-regime
  calculation is exposed. The comparison then remains PROVISIONAL and has no
  definite recommendation or numerical saving.
- An unavailable or uncertain regime is excluded from actionable annual amounts
  and definitive two-regime comparison. The module consumes trusted eligibility;
  it does not decide legal elections or deadlines.
- Stale snapshot/validation identity, quarantine, unreconciled restore state,
  bad payload integrity and unapproved references block delivery.
- Cross-tenant, unauthorized and deleted resources return generic denial without
  identifiers, facts, rule details or amounts. Fresh authorization/control state
  is loaded even when the numerical candidate came from a cache.
- Annual tax is named annual_tax_before_credits. Refund/payable, ITR export and
  filing authority are not inferred; filing/export capabilities are always false.
- Public fields have fixed types and sorted object keys. Financial amounts are
  decimal strings, and sensitive domain toString methods redact values/identities.

## Observed verification

| Check | Observed result |
|---|---|
| Runtime | OpenJDK 17.0.20 in this environment |
| Compiler | Java 17 target, annotation processing disabled, all warnings enabled, warnings treated as errors |
| Named scenario groups | 205 passed, 0 failed: 57 policy, 31 proof/service, 21 parser, 28 guidance, 42 edits, 26 professional |
| Generated combined guard cases | 2,048 checked |
| Generated parser cases | 4,096 exact magnitude/script cases |
| Signed-field changes | 66 changes rejected |
| Generated guidance cases | 256 monetary fact-state combinations |
| Generated edit transitions | 64 field/state/magnitude combinations |
| Actual-thread rounds/requests | 48 / 192, single process |
| Generated professional evidence cases | 1,296 combinations |
| Selected compiled mutations | All 30 detected by behavioral test failures |
| Projection examples | Six core, six guidance, five edit and four professional JSON outputs parsed and checked |
| Independent JVM executions | All four projection families and canonical signing bytes matched |
| Negative compilation checks | Unknown.value(), unpermitted response implementation, external response construction and raw delivery to signed service rejected |
| JAR packaging | Valid archive, 99 library class files; test fixtures excluded |
| External Java dependencies | None |

These are engineering policy fixtures. Amounts such as 420.00 and 700.00 are
arbitrary synthetic values; they are not official expected tax calculations.
Positive approval states in tests are synthetic. The project's independent tax
golden approvals remain **zero**, and the golden workbook/JSON are unchanged.

## Mutation evidence

Each faulty variant compiled successfully. A compilation error was not counted
as detection. Tests then failed when a required guard was removed:

| Injected defect | Named scenario groups failing |
|---|---:|
| allow_pending_independent_references | 3 |
| allow_unconfirmed_income | 6 |
| allow_stale_calculation_identity | 3 |
| allow_unavailable_regime | 3 |
| skip_early_mismatch_quarantine | 3 |
| accept_forged_signature | 9 |
| ignore_revoked_key | 2 |
| ignore_proof_expiry | 1 |
| auto_confirm_human_amount | 13 |
| ignore_conflicting_magnitude | 1 |
| reveal_guidance_for_blocked_result | 4 |
| grant_filing_from_guidance | 2 |
| ask_reviewed_field_instead_of_missing_field | 9 |
| accept_stale_intake_snapshot | 1 |
| accept_numerical_candidate_as_input_draft | 1 |
| ignore_input_producer_proof | 2 |
| ignore_edit_snapshot_identity | 3 |
| ignore_changed_retry_body | 1 |
| accept_wrong_confirmation_value | 1 |
| auto_confirm_ledger_proposal | 4 |
| preserve_reviews_after_amount_change | 7 |
| allow_foreign_owner_edit | 1 |
| publish_edit_before_commit_probe | 1 |
| allow_edit_to_escape_quarantine | 1 |
| ignore_professional_grant_expiry | 1 |
| ignore_professional_credential_state | 1 |
| show_evidence_for_blocked_result | 2 |
| treat_submitted_evidence_as_reviewed | 2 |
| hide_required_professional_action | 1 |
| grant_queue_bulk_approval | 1 |

These are thirty selected mutation checks, not exhaustive coverage. Original
sources were preserved; only temporary compiled copies changed.

## Actual response behavior

Unknown bank interest produces INFORMATION_REQUIRED with reason UNKNOWN_INCOME
and no annual_taxes/comparison fields. Old deductions incomplete produces a
PROVISIONAL comparison, exposes only a complete eligible regime amount and omits
the comparison object. A mismatch or stale snapshot produces BLOCKED with no
tax payload. Unapproved independent references also block amounts.

The full six-example file is `java_result_safety/build/example_responses.json`.
The closed encoder and Python JSON-parser checks were exercised. No independent
JSON Schema validator or framework HTTP serializer was exercised here.

## Integration boundary and remaining work

The guard consumes trusted server state. Authentication, object permissions,
production key-registry/producer custody, legal rule applicability, real CA/release approval
and durable quarantine must be established by authoritative adapters. Client
JSON, LLM output and editable UI flags must never construct that trusted state.
Java types reduce accidental misuse; they are not a security sandbox against
malicious trusted application code.

The production control adapter must durably and idempotently quarantine an
affected calculation identity and prevent other nodes from serving old matching
results. If that write fails, affected delivery must remain fenced/disabled
until consistency is restored. The standalone service blocks the failed request;
it does not implement distributed database persistence or repair other nodes.

The model is valid at the control snapshot's access-decision point. Real SQL
isolation, atomic publication, replica lag, grant expiry, signed downloads,
crash/retry, quarantine fanout and isolated recovery require integration tests.
Already delivered data cannot be recalled by changing an API capability.

For cached/untrusted candidates call SignedResultService with a signed candidate.
The raw SafeResultService path is restricted to already trusted internal inputs.
The signing format and key checks do not approve law or replace current controls.
HumanMoneyParser yields estimated facts. An in-process session confirmation ledger is implemented; durable storage and actual HTTP user-action binding remain pending.

Return only the guarded PublicResult projection at the HTTP boundary, set the
appropriate private/no-store policy, and test framework serialization. Never
return Candidate, CaseFacts or stored calculation JSON directly. No HTTP server,
login, database or tax calculator was added by this component.

Current genuine reference evidence must map to PENDING/UNAPPROVED until approved.
Do not copy synthetic APPROVED test fixtures into a production control adapter.

Next: connect the module to the actual backend, verify its provider adapters and
HTTP/export paths, then apply tax-interaction fixes against independently approved
reference cases. Tax correctness, live DB behavior, load capacity and VAPT remain
pending; this local module does not establish them.

## Primary implementation references

- [Java 17 sealed classes](https://docs.oracle.com/en/java/javase/17/language/sealed-classes-and-interfaces.html)
- [Java 17 BigDecimal](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/math/BigDecimal.html)
- [OWASP Authorization Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html)

The observed local code/test results are separate from these documentation sources.

## Revision 3 hardening evidence

See hardening_report_v3_2.md for F43 red/green evidence, signed-payload design,
parser contracts, exact observed counts and remaining production dependencies.

## Revision 4 customer guidance

Component 0.1.2 adds focused input actions, an eight-field review checklist,
permitted regime status/explanation and an amount-free preparation note. Intake
preparation is authorized separately from tax-result publication and accepts only
signed current input-only snapshots. Its progress grants no calculation/filing
authority. Detailed result guidance remains behind the signed result guard.
See guided_features_v0_1.md and java_result_safety/GUIDED_FEATURES_V1.md.

## Revision 5 session confirmation and edit flow

Component 0.1.3 implements explicit amount proposals/confirmation/conflict
resolution, review resets and safe expected-key/idempotency handling in bounded
session memory. Receipt/history projections contain no amounts or principal IDs.
Fresh result reads use the new snapshot key after a material edit. Current
quarantine cannot be bypassed by editing. See confirmation_flow_v0_1.md and
java_result_safety/FACT_EDITS_V1.md. This is not durable storage or SQL validation.

## Revision 6 professional readiness

Component 0.1.4 adds read-only professional queue routing and evidence-status
projection with current per-case grant/credential/release and existing signed
result gates. READY_FOR_REVIEW grants no approval. Professional need/accuracy
rates are not measured. The actual CA portal, verification/consent providers,
durable sign-off/history, reconciliation, billing and filing remain absent.
See ca_premium_strategy_v0_1.md and PROFESSIONAL_QUEUE_V1.md for exact scope.
