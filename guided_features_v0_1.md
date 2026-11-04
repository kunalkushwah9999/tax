# Guided customer features v0.1

31 October 2026. Java component 0.1.2. This update implements customer-facing
backend guidance logic and response contracts. The actual application/PWA,
production database and original tax engine remain absent from this workspace.

## Implemented features and intended value

| Feature | Behavior implemented | Intended customer benefit |
|---|---|---|
| Focused next step | Deterministic first unfinished scope/amount/inventory review action; distinguishes missing, estimate and conflict | Less guessing about what to do next |
| Field-by-field checklist | Eight supported input-review items and completion label; confirmed zero stays distinct from missing | Clear preparation progress without invented confidence/accuracy |
| Regime status cards and explanation | Only guarded regimes are shown as calculation available; incomplete/uncertain comparison remains explicit; ties claim no saving | Clear distinction between a permitted calculation and an unfinished comparison |
| Preparation note | Fixed labels/statuses and pending items; omits amounts and personal IDs; narrow local-copy capability | Easier review/handoff of outstanding preparation work |

These are product hypotheses, not measured conversion, retention or accuracy
improvements. Customer appeal still needs usability testing with the actual UI.
No chatbot, automatic investment recommendation, payment plan, filing or live
document/OCR workflow was added by this update.

## Intake must work before a tax calculation exists

The previous result guard intentionally blocks missing calculations/unapproved
references. Reusing that gate as the only entry to input preparation would hide
the checklist precisely when users need to complete their inputs.

The new IntakeGuideService therefore accepts a current signed input-only snapshot
with an empty calculations map. After current access, exact snapshot, integrity,
restore/quarantine and proof checks, it can show input preparation even while
tax references/rules remain pending. It contains no numerical result, comparison
or recommendation. A numerical candidate is refused with DRAFT_REQUIRED.

All reviewed inputs wait for validation when references/rules are pending.
REQUEST_CALCULATION is only a next-step label after the preparation checks; it
does not authorize or assert success of the actual calculation. Filing/export
capabilities remain false. Missing independent tax approval is never manufactured.

GuidedResultService separately wraps SignedResultService. Its nested result is
the sole numerical authority; blocked/denied results get no private checklist,
completion label, preparation note or regime cards. The contracts are explicitly
different: intake-guide.v1 and guided-result.v1; result-safety.v1 is unchanged.

## User flow implemented by the contracts

1. Open an authorized current input-only snapshot.
2. Review the first unfinished input. Unknown asks for an amount; estimates ask
   for confirmation; conflicts ask for resolution. No default zero or silent choice.
3. Review the checklist and preparation note. Completion measures review of eight
   supported fields, not legal completeness, tax correctness or filing readiness.
4. Request a separately checked calculation once the actual release/command gates
   permit it. Inspect only guarded result values and the corresponding explanation.
5. If access, proof, snapshot or result validation fails, detailed data is omitted
   and an appropriate generic next step is shown.

The English inline preview displays actual Java-generated synthetic examples:
missing savings interest, unfinished old-regime deduction review, pending result
validation and denied access. It is an illustrative local interaction, not a
deployed PWA or a live tax calculator. Its JavaScript syntax and selectors were
checked; browser rendering and visual QA were not run. No live APIs or customer
data are used. The preview is response content, not part of the backend archive.

## Actual verification

| Check | Observed |
|---|---|
| Runtime/compiler | OpenJDK 17.0.20; --release 17; UTF-8; -proc:none; -Xlint:all; -Werror |
| Policy scenario groups | 55 passed |
| Proof/service scenario groups | 31 passed |
| Parser/confirmation scenario groups | 21 passed |
| Guidance scenario groups | 28 passed |
| Total named scenario groups | 135 passed, 0 failed |
| Combined guard cases | 2,048 |
| Exact parser cases | 4,096 |
| Generated guidance fact combinations | 256 |
| Signed-field changes | 66 rejected |
| Selected compiled defects | 16/16 detected by behavioral failures |
| JSON examples parsed | Six core plus six new guidance responses |
| Independent JVM consistency | Core projection, signing bytes and new guidance example bytes matched |
| JAR | 59 library class files; test/ephemeral-key fixtures excluded |
| External Java dependencies/downloads | None |

The 135 named groups include generated-case groups; these numbers are separate
engineering dimensions, not a tax-accuracy or conversion denominator.
Original source is preserved by mutation checks; only temporary copies are altered.

## New selected mutation evidence

| Injected defect | Failing named groups |
|---|---:|
| reveal_guidance_for_blocked_result | 4 |
| grant_filing_from_guidance | 2 |
| ask_reviewed_field_instead_of_missing_field | 9 |
| accept_stale_intake_snapshot | 1 |
| accept_numerical_candidate_as_input_draft | 1 |
| ignore_input_producer_proof | 2 |

The ten prior selected delivery/proof/parser defects were also checked. Every
variant first compiled; compilation errors are not counted as behavioral detection.
Selected checks are not exhaustive mutation coverage or a security certification.

## Integration contract and limits

- Real HTTP handlers must obtain actor, current case access, snapshots and trust
  from authoritative adapters, never browser flags. Both guidance services recheck
  current controls. Every command or actual note-export action still authorizes
  independently; copied/delivered bytes cannot be recalled.
- A producer signs input snapshots only after constructing them from the trusted
  fact/confirmation path. No production signer, key store or confirmation ledger
  is implemented here. Scope, residency/DOB and tax-year semantics still need the
  reviewed real intake schema; these eight fields are not full ITR readiness.
- The note is private preparation metadata. There is no recipient lookup, automatic
  sharing, email, provider handoff or official ITR/PDF export operation.
- The PWA must render guide actions as instructions and use the nested guarded
  result for all numerical permissions. Completion labels cannot unlock filing.
- Actual Spring/MySQL/HTTP integration, private cache/export behavior, load tests,
  recovery, production quarantine/key controls and security review remain pending.
- Golden workbook and JSON are byte-for-byte unchanged: 30 draft inputs, zero
  independently approved expected outputs. No tax arithmetic or HF inference ran.

Read java_result_safety/GUIDED_FEATURES_V1.md for exact fields/order, authority
boundaries and service inputs. Build and mutation reports are included in build.

## Revision 5 integration update

Component 0.1.3 now supplies the missing in-process session edit/confirmation
logic through EphemeralFactLedger: explicit proposals, exact displayed-value
confirmation, conflict choices, dependent-review resets and safe stale-tab/retry
handling. It connects to the existing current result-control boundary. The ledger
is temporary session memory, not durable saved cases or a production MySQL adapter.
The actual PWA/HTTP/authentication/confirmation-action binding still needs integration.
Read confirmation_flow_v0_1.md and FACT_EDITS_V1.md for current evidence and limits.
