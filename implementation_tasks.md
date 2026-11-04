## Current checkpoint — 3.0.0 (2 November 2026)

See [STARTUP_RELEASE_V3.md](STARTUP_RELEASE_V3.md), [START_HERE.md](START_HERE.md)
and [OWNER_HANDOFF.md](OWNER_HANDOFF.md). Added opt-in schema-2 concurrent MySQL,
validated read/scan dependencies, short shard publication locks, streaming framed
backup/isolated restore above 16 MiB, and payment recovery protection against new
request IDs while an order outcome is unresolved. Exact retry now remains visible
in the UI. The legacy mysql profile still has its original guard/16 MiB limit.

New evidence: 26 concurrent storage groups, 10 framed SQL restore groups, 8 framed
unit groups, 4 compiled concurrent defects and 22 browser checks on the concurrent
backend. Existing local/legacy SQL/native/operations/HTTPS checks were refreshed.
Test operations remain fake-provider/signed-TEST-credential checks, not real sales
or CA verification. Short quota/scan contention and bounded application/session/
worker limits remain; no 100,000-user scale evidence is claimed.

Independent tax approvals are **0/30**. Full statutory coverage, live filing,
commercial billing, real CA verification and production hosting/security/recovery
are still unreleased. This is a runnable engineering platform, not a complete
production startup. All earlier checkpoints below are historical and superseded
for current readiness by STARTUP_RELEASE_V3.md. Synthetic inputs only.

---

## Current consolidated checkpoint — 2.0.0 (1 November 2026)

Current delivery is described in [STARTUP_RELEASE_V2.md](STARTUP_RELEASE_V2.md)
and launched through [START_HERE.md](START_HERE.md). Added test-mode billing and
fixed-duration access/credits, snapshot review bundles, signed-test expert
requests/revocation and structured feedback triage. New operations checks passed
35 local + 35 actual-MySQL groups; authenticated Chrome passed 22 checks including
a 390px mobile viewport. Earlier pending browser-install statements below are
historical. Physical-device/Safari/full accessibility QA remain outside that smoke.

Independent tax approvals remain **0/30**. Full ITR/GST/capital gains, filing,
verified real CA services, live money collection and production hosting/scale are
unreleased. SQL writes still serialize and cap encrypted data at 16 MiB. All
examples are synthetic. Earlier reports below are preserved implementation history
and do not override this checkpoint or establish tax/commercial approval.

---

# Implementation tasks and acceptance evidence

## Current startup checkpoint: 1.3.0 — optional native ITR-1 structural intake

An owner-only **ITR file check** now runs a bounded local Python worker against a
hash-pinned official ITR-1 AY 2026–27 Draft-04 JSON schema. Exact form/year/version
checks supplement structural validation. The official business-validation PDF is
pinned as a reference, not implemented as approved business rules. Prefill JSON,
other forms/years, statutory eligibility and tax correctness are not inferred.

A preview offers only supported income observations: explicit gross salary and
single classified interest observations, subject to source-total/pack checks.
Missing categories never become zero; multiple observations are not silently
summed. Unsupported income schedules require a broader workflow. The user must
confirm same taxpayer, annual category coverage/classification and exact input
replacement. Selected fields commit atomically as **ESTIMATED**; confirmation is
still separate. No source tax/credit/deduction/profile fields are transferred.

Raw JSON, PAN, bank/contact details and supplied tax figures are not saved in
cases, receipts, previews or audit. Previews hold sanitized income only, expire
after ten minutes, bind owner/case/revision, and disappear on discard, accepted
use or process restart. Accepted command replay needs no surviving preview and
preserves later edits. New proposals change material identity and stale earlier
scope review. Existing preparation grants do not expose native-import lineage.

The feature is **optional and disabled by default**. The packaged app still runs
without third-party Python packages. To enable native checks, install the exact
worker dependencies in a separate venv, explicitly fetch the pinned reference
files and pass --native-python / --native-schema-dir. No reference file is fetched
while handling a customer's document. See startup_app/NATIVE_IMPORT.md.

Observed: 18 native policy groups; 21 real validator groups plus 28 generated
rejections; 20 native real-worker API groups on local storage and 20 on real
MySQL; four controlled actual-child-process fault groups; six compiled Java
native-protocol defects and six targeted Python-worker defects detected.
Refreshed regressions: 49 local API groups; 48 SQL API groups passed with one
local-file-only group skipped; 20 SQL groups/four concurrent-writer rounds;
9 isolated logical-restore groups; 12 authenticated HTTPS proxy groups; existing
workspace/preparation/archive suites and business/preparation/SQL defect checks.
These are engineering checks, not tax accuracy or independent reference approval.

Native GST schema endpoints returned HTTP 403. No vendor example was substituted
for a primary schema, and native GSTR-2B/IMS/GSTR-1/3B intake/filing stays unreleased.
The existing normalized GST observation workflow remains available; it does not
determine ITC eligibility. Synthetic data only. Independent tax approvals remain
**0/30**, and the original golden values/reference template remain blank.

SQL global write serialization/16 MiB cap, production hosting/scale/control-plane
recovery, authenticated browser/mobile/accessibility QA, verified CA services,
payments and filing connections remain pending. Earlier browser-runtime failure
was not treated as a UI pass. Guard 0.1.4, engine 0.1.0 and anonymous app 0.4.0
source/JAR bytes are unchanged; those component suites were not rerun.

## Historical checkpoint: startup 1.2.0 — self-service preparation and isolated recovery

The customer workspace now includes **ITR preparation** and **GST reconciliation**
tabs. ITR adds 14 explicit yes/no/unknown scope declarations, current-draft-bound
review and a task list for inputs/profile/inventories/document declarations. It
covers preparation for FY 2025–26 / AY 2026–27 salary and ordinary separated
interest within the declared narrow scope. It does not select a statutory ITR
form or determine whether a CA is legally required.

GST compares two normalized synthetic CSVs, using supplier aliases, document
kind, exact document number and financial year as identity. It preserves all
rows, detects duplicates/missing/unknown/date/tax-component discrepancies, marks
RCM/amendment/statement-availability issues, and records version-bound review or
follow-up. Source replacement resets reviews; exact retries preserve later work.
Matching never determines eligible ITC or a nil return, and no claim amount is
calculated. Native GSTR-2B/IMS JSON and GSTR-1/3B filing remain unavailable.

The operator can create/authenticate a dedicated-key encrypted logical backup
and restore it into a completely empty MySQL database with a fresh record key.
Restore creates **ISOLATED** state before loading records. Normal application
startup and repeated initialization refuse that target. No activation/promotion
command is supplied: current deletion, revocation, rule/control/quarantine and
retention state must be reconciled through a future reviewed recovery process.

Observed on 1.2.0: 27 existing workspace unit groups plus 12 generated ownership
checks; 27 new self-service groups plus 60 generated component discrepancies;
8 archive unit groups; 46 local HTTP groups; 20 real MySQL groups and four
concurrent-writer rounds; 45 SQL HTTP groups passed with one local-file-only test
skipped; 9 real SQL/launcher backup-restore groups; 12 authenticated HTTPS proxy
groups; six business, six preparation and three SQL compiled defects detected.
The actual MySQL concurrency suite caught an added guard-read ordering defect;
write locking now precedes the consistent read, and the same concurrency/replay
checks pass. Counts are engineering evidence, not legal or accuracy percentages.

Use `python3 startup_app/run.py` and `http://127.0.0.1:8788`. Read
`self_service_and_recovery_v1_2.md`, `startup_app/SELF_SERVICE.md` and
`startup_app/deploy/BACKUP_RESTORE.md`. Existing SQL users must explicitly run
`--init-mysql` with the migration configuration and original key to add V002.
The key/record format and original data are retained; an isolated target cannot
be activated by this command. See `startup_app/deploy/MYSQL_SETUP.md`.

Global SQL write serialization, bounded namespace iteration, the 16 MiB payload
cap and process-local sessions remain. A normalized/pool-backed Spring repository,
production scale/hosting/operations and reconciled production disaster recovery
are pending. The browser runtime download failed; no authenticated browser,
mobile, keyboard or accessibility pass is claimed for these new views.
Independent tax approvals remain **0/30** and original golden values stay blank.
Full ITR/GST/capital gains, verified CA services, checkout and filing connections
remain unreleased. Synthetic data only. Guard 0.1.4, engine 0.1.0 and anonymous
local_app 0.4.0 source/JAR bytes are unchanged; those suites were not rerun.

## Historical checkpoint: startup 1.1.0 — MySQL and HTTPS pilot

`startup_app` now has an explicit MySQL 8.4 persistence profile and strict HTTPS
reverse-proxy mode. The default encrypted local profile is still available.
Customer/team/admin workflows, consent, source proposals, planning quotes and
support use the same business rules with either store. No real taxpayer data or
provider credentials were used.

Observed on the current 1.1.0 JAR: 27 unit groups and 12 generated ownership checks;
36 local HTTP groups; 20 actual MySQL groups with four concurrent-writer rounds;
35 MySQL-backed HTTP groups passed and one local-file-only group skipped;
12 actual authenticated HTTPS proxy groups; six business defects and three SQL
defects independently compiled and detected. Real MySQL SIGKILL/restart preserved
committed data; the API failed during outage without a local-file fallback.
The HTTPS test used a disposable trusted certificate and a local proxy fixture,
not a browser or Nginx/public deployment.

The SQL adapter is deliberately bounded: a singleton control-row lock serializes
all business writes, some namespace operations scan bounded collections, and
encrypted payload totals are limited to 16 MiB. It is not the final normalized,
indexed, pooled Spring repository or a capacity benchmark. Sessions remain local
to one process. Backup restore, key rotation and production operations are pending.

Run `python3 startup_app/run.py` at `http://127.0.0.1:8788` for the default profile.
See `startup_app/deploy/MYSQL_SETUP.md` for explicit driver/config/migration/HTTPS
steps and `mysql_persistence_v1_1.md` for evidence and limits. No runtime data,
passwords, record keys, MySQL distribution or JDBC binary is packaged.

Independent tax approvals remain **0 of 30**; all original golden expectations
stay blank. Numerical tax release, full ITR/GST/capital gains, verified CA services,
checkout and filing connections remain unavailable. Authenticated browser/mobile/
accessibility QA and independent security review remain pending. The original
guard 0.1.4, engine 0.1.0 and anonymous local_app 0.4.0 source/JARs are unchanged;
their earlier tests were not rerun this turn. Synthetic data only.

## Historical checkpoint: startup 1.0.0

A new **startup_app 1.0.0 local development application** now brings customer
registration/login, encrypted saved sample cases, the existing income/profile/
source workflow, consent/expiry/revocation, a preparation-review queue and
snapshot-bound comments, future immutable price versions, frozen planning quotes,
in-app support, owner export/archive/delete and amount-free operational activity
into one runnable Java application. ADMIN cannot inspect private customer case
inputs; PREPARER is an unverified preparation role, not a credentialed CA identity.

Observed: **25 new unit groups**, **12 generated ownership-denial checks**,
**36 new actual loopback HTTP groups**, and **six selected compiled defects
detected**. Real process restart preserves saved sample data and invalidates old
sessions. The existing core 0.1.4, engine 0.1.0 and local_app 0.4.0 source/JARs
remain unchanged; their earlier test counts are prior evidence, not new tests.

Start `python3 startup_app/run.py` and open `http://127.0.0.1:8788`.
Optional operator setup commands, limits and complete flow are in
startup_app/README.md. The anonymous 0.4.0 workbench remains available separately
with `python3 local_app/run.py` at port 8787. No test accounts/passwords or runtime
data/keys are included in the archive.

This is **not a complete production startup**. Independently approved tax outputs
remain zero; public tax numbers, payment collection, full ITR/filing, verified
professional sign-off and native document parsing remain unavailable. The local
encrypted single-writer aggregate is a development profile, not tested MySQL or
large-scale infrastructure. The production target remains Java/Spring + MySQL.
Only static desktop landing/signup navigation were inspected in the browser;
authenticated browser, mobile/accessibility and independent security QA remain
pending. See startup_implementation_v1.md and INDIA_LAUNCH_PLAN.md for evidence,
product pilot experiments and the exact live-launch gates.

Earlier version-specific records follow; this checkpoint describes the new
module and does not retroactively change those historical tests.

## Current checkpoint: 0.4.0 (1 November 2026)

Local app **0.4.0** adds an explicit selected-source-to-income proposal. An
eligible known annual gross replaces one whole category input as **ESTIMATED**;
the ordinary separate displayed-value confirmation is still required. Multiple
groups in the same category, transactions, gross conflicts/missing/repeated/value
variants and unknown gross block transfer. Two user acknowledgements cover
whole-category/classification review and replacement; no source amount is summed
or treated as TDS credit. A minimal historical source link is shown separately
from confirmation and remains unverified. Accepted retries preserve later edits
and lineage; new manual proposals/clears remove that field's source link.

Observed: **79 actual HTTP groups**, zero failures/errors; **28 new source-policy
groups**, **36 generated gross/category cases**, and **six selected compiled
defects detected** (four policy, two actual-HTTP). Comparison regression evidence
remains 38 groups, 81 generated pairs, eight actual-thread rounds and four compiled
defects. Core 0.1.4 and private engine 0.1.0 are unchanged. The original 30 golden
inputs and all blank expectations stay unchanged: independently approved outputs
remain **zero**. See source_proposal_v0_1.md and START_HERE.md.

Browser visual/click/keyboard/accessibility QA, native document/provider adapters,
source authenticity, durable SQL/identity and professional/filing integration
remain pending. No public tax figure, export, filing or CA approval is enabled.

Earlier chronological records follow; their version-specific limitations do not
replace this current checkpoint.

Order is dependency-based. The original source project was not supplied. Task 1 now has a locally tested standalone Java guard component; integration with the existing application remains pending. Build a focused adapter/patch against the recovered repository instead of reconstructing the entire application from chat snippets.

## Task 0: independent references

Give the 30 input fixtures to a qualified reviewer; collect stage outputs independently of engine output. Record method, source/evidence ID, utility/workings version, reviewer/date and decision. Required cases must have approved references; missing values fail release, not silently skip. Tie approvals to input/expectation hashes. Retain all unresolved law/rounding questions explicitly.

## Task 1: output safety

Status: STANDALONE_CORE_IMPLEMENTED; LOCAL_WORKBENCH_HTTP_IMPLEMENTED; PRIVATE_ARITHMETIC_IMPLEMENTED; ORIGINAL_APPLICATION_INTEGRATION_PENDING. See java_result_safety, local_app and their reports. Core 0.1.4 evidence: 205 named groups and thirty selected compiled defects detected; unchanged this revision. Local app 0.2.0 has 46 real HTTP groups and new private engine 0.1.0 has 64 named groups. Independent tax validation and browser UI QA remain pending.

Implement typed scope/fact/verification/freshness/comparison capabilities. Unknown interest is not zero. Unsupported incomes cannot produce a complete tax or definitive recommendation. Incomplete deductions keep comparison provisional. Annual tax is not payable/refund and not filing readiness.

Tests first: G25-G30, null/unknown vs confirmed zero, contradictory facts, stale/revoked rules, UI serialization and export refusal. A mismatch in either required regime blocks the comparison; it cannot be suppressed by the UI or a paid feature.

## Task 2: tax interactions

Specify the full stage order before patching. Salary cap and multiple employers, separated 80TTA/80TTB, DOB/FY age, rounded total income, old/new rebate distinction, marginal relief before cess, and final rounding interact. Do not implement three isolated helpers with no combined golden test.

Tests first: approved G01-G24 stage comparisons; salary 0/low/multiple; new threshold -1/at/+1 and statutory rounding steps; old age-specific thresholds; relief upper-edge pairs; paise; arbitrary unsorted/gapped/overlapping slabs rejected. Missing new-Act or surcharge support returns unsupported. Add shared-error mutation checks: remove salary cap, deduct twice, include FD in 80TTA, omit relief, round prematurely, use current age, default UNKNOWN to zero.

## Task 3: comparison/break-even

Return tie as tie. Require complete eligible deductions for a definitive choice. Separate weak/strict break-even. Search only attainable deductions over a proven monotone domain; handle rounding plateaus and no-win cases. Show cash outlay/lock-in separately, without turning a threshold into an investment instruction.

Tests: first point and prior allowed point, zero additional deduction, eligible cap exhausted, tie plateau, senior interest benefit and a changed fact invalidating the earlier recommendation.

## Task 4: canonical rules and persistence

Reconcile duplicate tax-year/calculation models. Separate Act/period/variant/revision; enforce immutable approved content and atomic manifest publication. Test source and bundle integrity, maker/checker separation, no empty test-set publication and direct application-user denial. Avoid “EFFECTIVE today” queries as a replay API.

Use real MySQL integration tests for constraints, transaction isolation and locking; an in-memory substitute is insufficient evidence. Record DB version. Do not claim Testcontainers passed unless Docker tests actually ran.

## Task 5: orchestrator for saved cases

Freeze full comparison manifest in Tx1. Compute outside transaction. Tx2 checks immutable snapshot ID, active attempt, fencing token, lease and bundle recall status. Logical calculation identity, attempts and request idempotency are separate records. Outbox consumers deduplicate event IDs.

Tests: simultaneous clicks/devices; same key different body; fact edit and revert during run; new rule publication during run; crash after Tx1; reaper then retry; old worker finishes late; crash after result commit; send then crash before outbox acknowledgement; grant revoked during export. Verify row counts and terminal states, not only happy-path HTTP responses.

## Task 6: thin anonymous planner API/PWA

No PAN/login/AI dependency. Stateless request scope and input semantics are explicit. Template explanations link to result stages and reviewed sources. Keep personal values out of analytics and public cache keys. Measure declared-load behavior and publish only demonstrated scope/limits.

Tests: request schema, number formats, bad dates, incomplete screening, cross-field contradictions, result capability presentation, keyboard/accessibility and expected error messages. Load test ordinary arithmetic separately from future OCR.

## Task 7: saved profiles and documents

Add explicit tenant/profile grants, secure sessions/step-up, HMAC lookup plus AEAD where identifiers are needed, reviewed retention matrix, private uploads, sandboxed parsing, confirmation and reconciliation. Native PDF parser before OCR; no HF model until its evaluation gate passes.

Tests: cross-user/tenant object access, expired/revoked consent, field tamper/AAD mismatch, key rotation/restore, OTP races and abuse controls, data leaks in logs/errors/Sentry, signed URL expiry, hostile files/timeouts, duplicate/mis-signed amounts, retention hold and deletion propagation. Independent staging VAPT with remediation/retest evidence before the affected sensitive workflow is released.

## Later packs

TDS/advance tax/interest/fees settlement, complete filing facts and current form schema, guided submission/authorized filing, CA Assist, paid commercial ledger, house property, capital gains, business tax, GST and personalized financial planning each require their own law map, independent references, data/security contract, release flag and owner. Their presence in a roadmap is not implementation.

## Definition of done per patch

Source diff; requirement/issue IDs; relevant passing tests with exact environment; explicitly unrun checks; new/changed golden references; API/schema compatibility; release/rollback plan where applicable. Never infer legal correctness solely from matching two engines or passing a large random sweep.

## Revision 1 additions

Before definitive regime comparison, add reviewed regime availability with explicit ALLOWED/NOT_ALLOWED/UNKNOWN outcomes. Add live result-delivery eligibility, a calculation quarantine registry and an isolated-restore catch-up gate. See F39-F42.

The executable histories in verification_models are a reference specification. Translate them into actual Spring/MySQL integration tests, including the duplicate reaper after a newer attempt starts, late completion, access/rule revocation after commit, and a mismatch after a previously cached result. Keep normal client deduplication separate from an explicit audit run. Do not copy the Python checker into the production service as an implementation shortcut.

Do not treat protocol model results as approved golden tax values, DB isolation proof, VAPT or capacity evidence.

## Revision 2 next step

Use the tested Java component as the public-result guard. Implement its trusted
ControlPlane adapter from current authenticated/authorized server state, approved
release evidence, integrity verification and durable quarantine. Test actual
Spring serialization, HTTP/no-store handling, exports and MySQL concurrency.
Never map missing approval or legal evidence to synthetic APPROVED fixture values.
Task 1 is complete for the standalone policy boundary only, not the integrated app.

## Revision 3 integration requirements

Use SignedResultService for cached/untrusted candidates, with the complete
versioned producer signing format and an authoritative current KeyRegistry.
No caller-provided key URLs or default trust keys. Never use the raw trusted
service as a proof bypass. Keep rule approval independent of producer signatures.

Persist authentic mismatch quarantine before ordinary reference/rule gates can
hide evidence; test the F43 history against real SQL and multiple serving nodes.
Test registry/control changes at the chosen delivery decision point and fence
affected delivery after write failures. The Java callback alone is not persistence.

Connect HumanMoneyParser to explicit unit selection and a real confirmation
ledger. Parsed proposals stay ESTIMATED. Supply strict HTTP decoding and preserve
the public projection; no endpoint or confirmation persistence was built here.

## Revision 4 guided feature integration

Wire IntakeGuideService to authorized current signed input-only snapshots and
GuidedResultService to guarded signed calculation candidates. Preserve separate
intake-guide.v1 and guided-result.v1 contracts; use the nested result as the sole
numerical authority. Bind checklist actions to real authorized confirmation/edit
commands, updating snapshot identity. The eight-item checklist is not full ITR
or legal readiness; complete progress cannot unlock filing or a regime election.

Add actual UI/HTTP tests for missing/estimated/conflicting amounts, unsupported
scope, unfinished deductions, unavailable regimes, equal tax, approval pending,
revoked access and stale snapshots. Support user-initiated local preparation-note
copy without implying an official return or automatically sharing private metadata.
These adapters and the persistent confirmation ledger are still unimplemented.

## Revision 5 session edit integration

Use the explicit FactEdits commands for user-driven session preparation. Bind
the actor and confirmation intent on the server, preserve the full expected key,
and refresh the signed current draft after an accepted receipt. Never treat an
old replay receipt as current facts. Commands do not accept scope/release/tax flags.

EphemeralFactLedger is implemented and locally tested only for bounded temporary
single-process sessions. Do not label it saved/resumable. Schedule idle cleanup
in the actual app if required; expiry authorization is immediate. For saved cases,
port and test atomic grants/snapshot/receipt/history behavior against real MySQL,
including durable retry, process crash/recovery, quarantine and multiple instances.
No real SQL or PWA/HTTP implementation was supplied or tested in this step.

## Revision 6 professional integration

Integrate ProfessionalReview.Source from independently authoritative current
professional credentials and scoped case/period grants; never from plan/LLM/UI
flags. Snapshot-bind evidence metadata and preserve submitted versus reviewed
states. Keep the live feature unreleased until actual adapters and release tests
exist. Test revocation/expiry/evidence changes across source/control lookups.

The current queue is read-only: no approval, bulk approval, filing, credential
lookup, client directory, consent UI or durable professional audit trail. Build
those through separate permissioned, version-bound workflows. Keep individual
self-service scope separate from legally/professionally required work and measure
actual coverage/referrals after independent validation rather than forcing a rate.

## Revision 7 local application integration

Task 6 now has a runnable loopback sample implementation under local_app, with
strict form decoding, session-owned input projections, CSRF/Origin/Host checks,
full-key view binding and existing Java confirmation/guidance/result guards.
31 real HTTP groups pass. Production PWA/authentication/MySQL, original calculator,
load/security review and browser rendering/accessibility remain pending. The
preview environment could not load Java libjli.so; HTTP tests are not browser QA.

Keep this sample app separate from production signing/control adapters. No tax
candidate exists and every result remains blocked. Do not replace the 30 missing
independent golden expectations with app output. Next recover the original repo,
collect qualified independent reference approvals, and run a supported browser
pass before integrating applicable production features. Refer to the latest
local_app_implementation_v0_1.md rather than replaying completed policy work.

## Revision 8 private arithmetic and workbench completion

Task 2 now has new scoped private implementation under tax_engine: capped salary
standard deduction once across employers, separated old interest deductions,
FY-bound age, income rounding, old/new rebate distinction, new marginal relief
before cess and a separate zero-credit rounded illustration. A separately coded
integer-paise verifier compares all twenty stages. This is not an approved tax
fix against the absent original backend. Independent expectations remain missing.

Task 3 has private exact annual-tax comparison/ties and weak/strict sensitivity
over declared remaining eligible 80C capacity in paise. It does not support an
arbitrary deduction domain, personal investment advice or inferred regime-election
availability. Every sensitivity evaluation must pass both arithmetic implementations.

Task 6 now connects that engine to the existing local HTTP/session app 0.2.0,
with current income binding, explicit profile/deduction/screening intent and
nonnumerical readiness. The public signed result remains blocked. 64 engine
named groups, 4,096 generated cases, eight compiled defects, 16 reference-checker
groups and 46 real HTTP groups pass. START_HERE.md describes running/reviewing it.

The blank reference template and checker assist Task 0; no actual reference was
approved. Even all asserted matches cannot publish a rule or authenticate a CA.
Production Spring/MySQL/identity/release adapters, saved cases, real CA workflows,
complete schedules/filing and browser/load/security evidence remain incomplete.

## Revision 9 document/reconciliation boundary

Local app 0.3.0 adds normalized sample statement comparison and explicit local
source selection/reopen. Gross income and TDS are independent; missing values,
conflicts, repeated observations and declared value kinds stay visible. No rows
are silently deduplicated, no category totals are made and no tax facts/credits
or filing permissions are changed. This is not native AIS/26AS/PDF parsing,
source authentication, official feedback or real CA review/sign-off.

Observed: 38 comparison Java groups, 81 generated amount-pair combinations,
eight actual-thread review rounds, four compiled defects detected and 60 real
HTTP groups (prior 46 plus fourteen new). Exact accepted retries never restore
older batches/choices. Full income identity and per-review version binding reject
stale work; sources remain in bounded temporary session memory. See
statement_comparison_v0_1.md, local_app/examples/statement_comparison.csv and
START_HERE.md. Browser QA and production SQL/identity/CA/filing remain pending.

Guard core 0.1.4 and private engine 0.1.0 are unchanged. Both original golden files
remain byte-identical, with thirty input cases and zero approved expectations.

This advances source-review tooling without claiming Task 7 complete. Native
statement schemas, protected real document ingestion, source/revision/coverage
validation, actual reconciliation to tax facts, durable MySQL history and real
professional evidence/authentication still need implementation and independent
cases. A local source choice is not a qualified approval or a tax credit.
