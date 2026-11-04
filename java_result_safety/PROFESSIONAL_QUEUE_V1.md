# Professional review queue v1

Component 0.1.4 adds read-only professional review routing and evidence-status
projection. It does not implement a live CA portal, credential registry lookup,
client-consent UI, professional sign-off, durable review history, billing or filing.
Keep the live professional feature unreleased until those dependencies are met.

## Inputs and authority

ProfessionalQueueService requires a trusted ProfessionalReview.Source, existing
ControlPlane, CandidateProofVerifier and server Clock. deliver accepts an actor
and 1..50 unique requested case resources. Empty, oversized or duplicate-resource
batches fail before source access. Different periods/snapshots of the same resource
cannot be used to inflate a single batch's case count.

Source.currentFor must resolve current authoritative assignment, professional
credential status, grant validity/revocation, release flag, work classification,
snapshot-bound evidence metadata and signed candidate for each request. Never
bind ProfessionalReview.Access from browser flags, a paid plan or LLM output.
CredentialState.VERIFIED is an adapter assertion, not a verification integration
implemented by this enum. Credential standing and client authorization validity
are separately represented and must not be conflated.

The assigned actor, tenant/profile/case and tax period must match. Grants must be
active and valid at the server-clock decision point. Unverified/pending credentials,
revoked/expired/future grants and assignment mismatches deny access. Cross-tenant
requests do not query either source or controls. Missing source denies; outage or
null response yields an unavailable row. No default approval/credential exists.

Evidence metadata must bind to the exact requested signed snapshot. A changed
candidate or evidence snapshot requires refresh. Existing SignedResultService
then rechecks result-level access, proof/current key, restore/quarantine, rules,
independent references and numerical-policy eligibility. A professional assignment
cannot override those gates. Blocked/denied/unavailable/stale/unreleased rows show
no private evidence checklist. There are no tax values in any queue row.

These are separate source/control lookups, not a distributed transaction. Real
adapters must document the authorization decision point and test revocation,
snapshot/evidence changes, credential expiry and replica lag under concurrency.
Each new delivery reloads the source; already delivered metadata cannot be recalled.

## Explicit evidence states

Every Access supplies INCOME_SOURCES, INTEREST_BREAKDOWN, DEDUCTION_SUPPORT and
RECONCILIATION with one explicit state: UNKNOWN, MISSING, SUBMITTED, REVIEWED,
NOT_REQUIRED or CONFLICT. Missing map entries are not treated as complete.
Only REVIEWED/NOT_REQUIRED satisfy readiness. SUBMITTED does not mean checked.
CONFLICT takes precedence over ordinary missing/submitted evidence.

These are trusted metadata fields, not implemented document verification, OCR,
AIS/26AS reconciliation or evidence authenticity checks. Adapters must justify
REVIEWED/NOT_REQUIRED applicability; this class does not establish them.

## Routing

Access/release/snapshot/result gates precede detailed routes. For an authorized,
signed and policy-accepted case, route in this order:

| Condition | Route |
|---|---|
| Classified statutory professional action or specialist work | PROFESSIONAL_ACTION_REQUIRED |
| Unknown work classification or unsupported/unknown product scope | SCOPE_REVIEW_REQUIRED |
| Income unknown, estimated, conflicting or inventory incomplete | CLIENT_INFORMATION_REQUIRED |
| Evidence conflict | EVIDENCE_CONFLICT |
| Required evidence unknown/missing/submitted | EVIDENCE_REQUIRED |
| Regime availability unknown/disallowed | ELIGIBILITY_REVIEW_REQUIRED |
| Deduction review incomplete | DEDUCTION_REVIEW_REQUIRED |
| Required comparison calculation missing | CALCULATION_PENDING |
| Complete permitted calculation and readiness metadata | READY_FOR_REVIEW |

READY_FOR_REVIEW is neither approval nor an assertion that no CA is needed.
All rows say professional_review_status=NOT_RECORDED. Statutory classification is
supplied by the qualified current work-assessment adapter; this service does not
determine audit applicability or legal obligations. Under a result block it shows
the system block, rather than approving or exposing the underlying case.

The self-service individual path remains separate. This queue is for cases
explicitly submitted to an authorized professional workflow; it does not require
every individual planner user to hire a professional.

## Output and measurement

professional-queue.v1 returns request-index-based rows, route counts and narrow
readiness metadata. It omits names, PAN, profile IDs, income, tax amounts and raw
case payloads. The frontend must obtain client labels only through its own current
authorized client-list path; this module does not implement that directory.

All requested unique rows remain in the batch denominator, including denial and
unavailability. Repeated batches are not unique production-user statistics.
professional_need_rate is NOT_MEASURED and tax_correctness_rate NOT_ESTABLISHED.
No fabricated 99.9999% automation/accuracy, 0.0001% professional need or conversion
metric is generated. Real measurements require a declared cohort, independent
outcome assessment and coverage/abstention reporting.

bulk_approve, approve_cases, submit_return and export_itr are always false.
There is no approval command, bypass, subscription checkout or bulk filing action.
The service does not persist a review decision or professional audit trail.
Constructors are restricted and nested projection maps/lists immutable. Serialize
only ProfessionalQueue through SafeJson or an equivalently constrained adapter.

## Validation and remaining work

ProfessionalQueueTests checks all 1,296 combinations of four evidence fields with
six states, plus assignment/credential/expiry, source outage, snapshot, result-gate
and denominator boundaries. Positive credential/grant/reference fixtures are
synthetic, not real registrations or legal/tax approvals. The build parses four
new JSON examples and compares their bytes across JVMs. Selected compiled defects
must cause behavioral failures. No real CA/customer pilot or HTTP/SQL portal ran.

Real premium CA functionality still needs an authorized client directory, verified
professional onboarding, scoped/expiring consent, durable evidence/provenance,
snapshot-bound maker/checker review decisions, working papers, reviewed deadline
sources, compliant submission integration, recovery and independent validation.
Source and ControlPlane must enforce professional access per case; do not treat
the temporary anonymous owner-only ledger as a shared CA firm database.

Read the full pack's ca_premium_strategy_v0_1.md for product scope, requirement
traceability, current evidence and the distinction between automation and legally
required professional work. Original tax engine and approved numerical outputs
remain prerequisites; these routing tests certify neither.
