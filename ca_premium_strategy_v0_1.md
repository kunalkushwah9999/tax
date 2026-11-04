# CA premium direction and professional review component

31 October 2026. Component 0.1.4. This report separates a proposed product
direction from implemented, locally tested read-only queue logic. The actual
PWA/Spring/MySQL product, original calculator and live CA workspace are absent.

## Product direction

Give individuals a single place to prepare supported routine cases and give
professionals a reliable workspace for larger client workloads. Where qualified
professional work is required, it may be coordinated within a properly authorized
platform workflow; it is not removed by a model, subscription or UI label.

0.0001% means one case in one million. That professional-need rate is not established
by this project and must not be marketed as a result or achieved by hiding complex,
unsupported, blocked or abandoned cases. It is distinct from tax correctness and
from how many cases can be prepared without manual data entry. The current thirty
golden inputs still have zero independently approved expected outputs.

For the applicable Form 3CA-3CD workflow, the official manual specifies an assigned
registered CA and CA upload using DSC. My CA authorization can be withdrawn or
deactivated and is distinct from credential standing. The ERI workflow describes
registration and taxpayer-consent prerequisites. Those sources justify explicit
role/consent/submission gates, not a general conclusion that every tax task needs
a CA or that this platform is authorized to perform those services today.

Sources: [Form 3CA-3CD manual](https://www.incometax.gov.in/iec/foportal/help/statutory-forms/popular-form/form3ca-3cd-um),
[My CA manual](https://www.incometax.gov.in/iec/foportal/help/how-to-work-with-ca),
[ERI client manual](https://www.incometax.gov.in/iec/foportal/help/addclient).
Actual applicability, law period and provider rights require the reviewed release
workflow; these pages are not a new-law calculation pack or a blanket filing permission.

## Make premium valuable through useful work

| Audience | Useful workflow | Current state |
|---|---|---|
| Individual | Guided supported intake, explicit amount confirmation, missing-information checklist, clear permitted result explanation | Standalone Java logic tested; actual UI/API and calculator integration pending |
| CA / reviewer | Triage assigned cases by missing information, evidence status, eligibility and required professional action | Read-only queue logic now implemented and locally tested |
| CA firm | Client directory, staff assignment, snapshot-bound maker/checker decisions and durable review history | Planned; real permissions/storage and onboarding not implemented |
| Individual + CA | Versioned evidence/working papers, reviewed document reconciliation and coordinated follow-up | Planned; evidence engine, durable client documents and recipient workflows absent |
| Authorized filer | Appropriate provider submission, taxpayer acceptance/e-verification and acknowledgement tracking | Planned; no filing capability or provider integration implemented |

Price should be justified by measured reductions in repeated work, clearer review
and reliable records. Use transparent plan/usage rules and data portability; a
paid plan cannot bypass evidence, tax-reference, access or professional-role gates.
No payment, premium entitlement, pricing change or customer message was sent here.
The proposed paid features are not represented as a deployed or sellable portal.

## Traceability to the existing requirements

| Requirement retained | Current implementation or dependency |
|---|---|
| Golden sheet first | 30 draft inputs remain unchanged; independent outputs/evidence/reviewer approval still missing |
| Distinct old/new rebate relief and statutory rounding | Documented findings and boundary inputs; original tax-engine fixes remain unimplemented/unverified |
| 80TTA/80TTB and separated interest | Separated monetary facts and intake logic implemented; tax deduction math still needs original engine and approved references |
| DOB/FY and 1 April age boundary | Reviewed source/case requirements retained; actual age/tax integration not implemented |
| PAN HMAC with protected pepper plus separate encryption | Target design retained; anonymous starter collects no PAN; sensitive profile store not built |
| No general chatbot; English product text | Deterministic scoped workflows and English projection text; no general chatbot added |
| Central versioned pricing/admin audit | Target retained; commercial admin cannot be tax/role override; billing/admin implementation absent |
| Useful-data retention and MySQL/Oracle handling | Session memory bounds implemented; durable retention/sync/restore/deletion workflow still pending |
| One lakh concurrent users | Target only; no capacity claim follows from local thread tests |
| ITR, AIS/26AS, capital gains, GST, TDS, notices | Broad roadmap retained; starter remains narrow salary/interest planning, not full service implementation |
| Hugging Face help | Five candidates researched; no inference or Indian-tax document benchmark run |
| Strong result controls | Typed uncertainty, signed/current delivery, scope/approval gates, stale-tab/retry protection locally tested |
| Premium professional workflow | Current read-only readiness/routing component; real credentials, assignments, evidence validation and sign-off still pending |

This matrix records the requirements actually available in this chat and current
project artifacts. It is not a claim that inaccessible original repositories,
unseen prior messages or unrelated uploads were read. The original runnable tax
backend is still a concrete missing dependency.

## Implemented professional review queue

- Accepts a bounded batch of 1..50 unique case resources, with fresh per-case
  source/assignment, credential, tax-period, validity/revocation and release checks.
- Uses current signed result delivery gates. Paid status is not an authority input;
  result-level denial, bad proof, rule/reference blocks and stale snapshots are
  not overridden by a professional assignment.
- Distinguishes UNKNOWN, MISSING, SUBMITTED, REVIEWED, NOT_REQUIRED and CONFLICT
  for four required evidence categories. Submitted does not mean reviewed.
- Routes statutory/specialist work to the applicable professional workflow and
  distinguishes client information, evidence, scope, eligibility and calculation
  needs. Complete preparation is READY_FOR_REVIEW, never approval or no-CA-needed.
- Returns amount-free, identifier-free indexed rows and route counts. Blocked,
  denied, stale, unavailable and unreleased cases expose no private evidence list.
- Keeps every requested row in the batch denominator. Professional need and tax
  correctness rates are explicitly unmeasured/not established; no 99.9999% claim.
- Grants no approval, bulk approval, submission or ITR-export capability. No review
  decision is recorded; durable professional review history remains unimplemented.

These are readiness metadata and policy checks. Real membership validation,
taxpayer-consent capture, evidence authenticity/OCR, AIS/26AS reconciliation,
case assignment, durable review decisions and professional portal UI are not
implemented by an enum or a synthetic provider fixture. The live feature should
remain unreleased until its actual adapters and release requirements are satisfied.

## Observed component checks

| Check | Observed |
|---|---|
| Runtime/compiler | OpenJDK 17.0.20; --release 17; UTF-8; -proc:none; -Xlint:all; -Werror |
| Policy / proof / parser / guidance / edits / professional groups | 57 / 31 / 21 / 28 / 42 / 26 passed |
| Total named scenario groups | 205 passed, 0 failed |
| Professional evidence combinations | 1,296 across four fields with six states |
| Prior generated guard/parser/guidance/edit checks | 2,048 / 4,096 / 256 / 64 |
| Prior actual-thread session rounds/requests, rerun in current build | 48 / 192, one process |
| Signed-field changes | 66 rejected |
| Selected compiled defects | 30/30 detected through behavioral failures |
| JSON examples parsed | Six core, six guidance, five edit, four professional queue |
| Cross-JVM consistency | All example families and canonical signing bytes matched |
| JAR | 99 library class files, test/ephemeral-key fixtures excluded |
| Real CA / customer pilot, SQL/HTTP portal, credential or filing integration | Not run / not implemented |

The counts are separate engineering dimensions. They do not establish tax accuracy,
professional-need percentages, paid conversion, throughput or a security certificate.
Positive credentials/grants/APPROVED states are synthetic fixtures only.

## New selected defect evidence

Each variant compiled first. Compilation errors were not counted as detection;
only temporary source copies changed. Original production sources were preserved.

| Deliberate defect | Failing named groups |
|---|---:|
| ignore_professional_grant_expiry | 1 |
| ignore_professional_credential_state | 1 |
| show_evidence_for_blocked_result | 2 |
| treat_submitted_evidence_as_reviewed | 2 |
| hide_required_professional_action | 1 |
| grant_queue_bulk_approval | 1 |

The prior twenty-four selected defects were also checked. This is selected
mutation evidence, not exhaustive coverage. Machine reports are included in build.

## Next product dependencies and pilot measurements

1. Recover the original runnable backend and independently approve the tax golden
   references. Validate core interactions and supported scope before marketing
   tax results, full filing or rare professional-referral claims.
2. Integrate actual MySQL/HTTP/PWA, protected storage, current firm/client grants,
   professional onboarding and durable version-bound review history. Keep source
   and result authorization coherent under revocation, expiry and replica races.
3. Build document/evidence ingestion and reconciliation with field provenance,
   explicit confirmation, safe retention and independent document holdout cases.
4. Add maker/checker decisions and working-paper exports with current permission
   checks. Add appropriate consent/provider submission and acknowledgement flows
   only after their own release gates; never label an unacknowledged action filed.
5. Run a declared-cohort pilot with individuals and qualified professionals. Measure
   preparation/review time, correction/error severity, supported coverage,
   abstention/block rate, professional referrals and willingness to pay separately.

Use unique cases and explicit observation windows. Include incomplete/unsupported
cases in declared coverage reporting; adjudicate correctness independently. A queue
route is not an observed final outcome. Do not force a desired percentage by
ignoring referrals, disabling scope gates or calling agreement between engines law.

The existing golden files remain byte-for-byte unchanged with zero approvals.
Original PWA/backend/SQL, load, VAPT, HF inference and professional/customer pilots
remain pending. Temporary session storage remains memory-only, with no durable
saved-case or multi-instance recovery claim. See PROFESSIONAL_QUEUE_V1.md for the
exact read-only contract and decision-point limits.
