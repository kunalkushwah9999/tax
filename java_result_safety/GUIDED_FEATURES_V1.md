# Guided preparation and result contracts

Component 0.1.2. This is runnable Java product-feature logic and closed response
projections, pending integration with the actual PWA/HTTP/database application.
The product text is English. There is no general chatbot or external AI call.

## Two entry points

| Entry | Required input | Purpose |
|---|---|---|
| IntakeGuideService.deliver(actor, signedDraft) | Current signed Candidate with an empty calculations map | Prepare/review case inputs before any numerical calculation exists |
| GuidedResultService.deliver(actor, signedCandidate) | Signed result candidate | Explain only the result already permitted by SignedResultService |

Actor, current controls and proof/key trust must come from authoritative server
adapters. Never bind these domain objects from browser JSON or trust a client
CONFIRMED/APPROVED flag. Both services reload current controls on every call;
proof verification uses the current key registry. Anonymous users can have
ephemeral case-scoped server identities; these interfaces do not require PAN or
create a login/identity system.

The intake draft uses the existing signed-candidate/v1 wire format with no regime
calculations. A trusted producer may sign an immutable input-only snapshot with
the CALCULATION_PRODUCER key purpose. That proof authenticates the snapshot;
it does not certify its legal completeness or approve a calculation. No new
private keys, default trust or production signing implementation are provided.

## Input preparation

After current authorization, resource binding, restore/integrity, exact snapshot,
quarantine and producer-proof checks succeed, an input-only draft may show a
checklist even while rules/references are pending. The response contains no
numerical result, raw input amounts, recommendation, comparison or regime cards.
It is not a calculation-authority path. A candidate containing calculations is
refused with DRAFT_REQUIRED. This separation avoids making intake depend on an
already completed tax calculation.

The outer contract is `intake-guide.v1`. Status is INPUT_PREPARATION_AVAILABLE,
DRAFT_REQUIRED, ACCESS_DENIED or BLOCKED. Its `guide` contains fixed English text,
an input-review checklist, a focused next action, completion label, validation
notice, preparation note and narrow guidance capabilities when permitted.
Blocked/denied/draft-required responses omit the private checklist, progress,
regime cards and note. No quarantine writes are made by this preparation service;
it rejects numerical candidates without processing their calculation outcomes.

## Checklist and focused next action

| Order / field ID | Review item |
|---|---|
| 1 / SUPPORTED_SCOPE | Supported income types |
| 2 / SALARY | Annual salary |
| 3 / SAVINGS_INTEREST | Savings account interest |
| 4 / DEPOSIT_INTEREST | Qualifying deposit interest |
| 5 / OTHER_INTEREST | Other interest |
| 6 / INCOME_INVENTORY | All income reviewed |
| 7 / NEW_DEDUCTIONS | New regime deduction review |
| 8 / OLD_DEDUCTIONS | Old regime deduction review |

This is a checklist for the eight fields already present in CaseFacts, not a full
filing-fact or legal-eligibility checklist. DOB, residency history, form eligibility
and future scope need their own reviewed intake schema/workflows.

Each item has id, label, status, action and help. Unknown monetary facts are
MISSING/ENTER_AMOUNT; estimates NEEDS_CONFIRMATION/CONFIRM_AMOUNT; conflicting
values CONFLICT/RESOLVE_CONFLICT; confirmed money REVIEWED/NONE. Confirmed zero
is reviewed, while unknown never substitutes zero. An inventory is reviewed
only when its Boolean fact is confirmed true; estimated true and confirmed false
remain unfinished. Scope is reviewed only when supported.

The next input action selects the first unfinished item in the fixed order.
When all draft items are reviewed, the next action is WAIT_FOR_VALIDATION if
rules/references are pending, otherwise REQUEST_CALCULATION. This action is a
UI instruction, never authorization for a command. A real request/confirmation
endpoint must independently authorize, validate and persist current state.

Progress text says `N of 8 input reviews complete`. Eight reviewed items do not
imply tax accuracy, available regime elections, final payable/refund or filing
readiness. These concepts are never merged into a percentage or a trust score.

## Result explanation

The outer contract is `guided-result.v1`, containing `result` (the unchanged
result-safety.v1 projection) and `guide` (product guidance). The nested result is
the sole source of permitted numerical values, comparison and result capabilities.
Do not derive numerical permissions from checklist completion or guide mode.

Detailed fact-derived guidance is built only after the signed result service
returns ActionRequired or Calculation. Denied and Blocked outputs get a generic
next step without private checklist, note, completion label or regime cards.
Unknown income, unfinished deductions, scope review, absent comparison calculation
and uncertain regime availability have distinct next actions.

Regime cards only reflect regimes in the guarded annual_taxes projection; they
never inspect raw numerical candidates or duplicate hidden tax values. A complete
eligible regime can remain visible while another needs review. A definitive tie
says equal annual tax and claims no tax saving. All text identifies annual tax
before credits, not final payable/refund. No new tax math is implemented.

## Preparation note and capabilities

The note contains fixed checklist labels/statuses and relevant pending-review
items. It receives no Money value or case identity. It omits income/tax amounts,
PAN and profile IDs, and explicitly disclaims filing/return approval. The note is
private case-preparation metadata, not a public sharing or messaging operation.
The UI can offer a user-initiated local copy action; no recipient, email or upload
operation is implemented. The inline preview only views this generated text.

Guidance capabilities are show_input_checklist and copy_preparation_note.
submit_return and export_itr are always false, including a fully reviewed draft
or definitive synthetic comparison. These capabilities do not replace action
authorization in the real backend. Already delivered/copied bytes cannot be revoked.

## Projection and verification

Only the services construct GuidedResult/IntakeGuide; constructors are restricted.
Nested maps/lists are immutable. SafeJson has closed overloads for these types;
it remains an encoder, not a general JSON parser. Domain toString methods redact.
Serialize the returned projections only; never return Candidate/CaseFacts directly.

GuidanceTests exercises actual signed service calls and 256 generated monetary
fact-state combinations. The build parses six new example JSON responses and
compares bytes from separate JVMs. Selected mutations remove privacy, proof,
snapshot, draft-only, next-step and filing guards and must cause behavioral failures.
Current examples are synthetic engineering fixtures; their positive APPROVED
states and 420.00/700.00 amounts are not independent tax references.

Run `python3 build.py` and `python3 check_mutations.py`. See machine reports under
build and the full pack's guided_features_v0_1.md for observed results/boundaries.
Actual HTTP/UI integration, persistent confirmation, database concurrency,
producer custody, external reference approval and production load remain pending.
