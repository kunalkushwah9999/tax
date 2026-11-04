# Next session checkpoint

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

## Next concrete work

Recover authenticated browser/mobile/keyboard QA when a usable browser is
available, and implement normalized/query-filtered SQL repositories/pooling/
sessions before a scale claim. Preserve atomic accepted receipts, owner/grant
checks, restore isolation and the tested MVCC read ordering.

For native expansion, acquire official GST schema/revision evidence through an
authorized primary channel; current public endpoints returned 403. Do not use a
vendor sample as an official-schema release. Add ITR business-rule/eligibility
packs and independent numerical references separately from the now-tested
ITR-1 structural adapter. Keep exact source/version pins, per-category coverage,
no automatic confirmation/zero/aggregation, masked diagnostics and no raw-file
retention. All 30 original independent expectations remain blank.

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

## Next concrete work

Prioritize authenticated browser/mobile/keyboard QA when a runnable browser is
available, then the normalized/query-filtered SQL repository and session/pool
work. Preserve atomic receipt/authority checks and the now-tested MVCC read
ordering. No more read should establish a write transaction's snapshot before
its serialization/row-lock protocol is acquired.

For native ITR/GST expansion, pin official period/form/schema/validation packs;
validate synthetic imports and independently produced cases. Do not relabel the
normalized CSV matcher as GSTR-2B/IMS ingestion, form eligibility or ITC approval.
Current approved golden expectations remain 0/30. Review recovery promotion
against current deletion/revocation/quarantine state before enabling any restored
service. External hosting, professional credentials and provider/payment/filing
connections require real provisioned integrations.

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

Project: Kunal's India tax planning/preparation platform. Current date of review: 31 October 2026.

## What was actually completed

- Compared two attached chat exports and reviewed key source fragments and evolving design decisions.
- Researched primary tax/notification sources and publisher Hugging Face cards.
- Created a 30-case synthetic golden-input draft with ALL numerical expected values and reviewer evidence empty.
- Created updated system overview, an initial 38 findings, implementation tasks and an HF evaluation plan.
- No original application code was supplied or patched. No application, official utility, model inference or penetration test was run.

## Locked decisions

Deterministic tax core; independent verifier plus external reference tests; mismatch blocks. Unknown is not zero. Product scope and form eligibility are separate. No general chatbot. No automatic law/model/rule publication. Versioned future pricing. CA Assist later. MySQL/Java retained absent a measured reason to change. No PAN/login in anonymous planner.

## Most important corrections

New-regime marginal relief removes the false 62,400 jump near 12 lakh; cess is separate. Old under-60 5 lakh cliff differs. 80TTA/80TTB needed with interest support. April 1 age circular exists. PAN: tenant-scoped HMAC lookup and separate AEAD encryption; static PAN/DOB is not step-up authentication. FY 2025-26 / AY 2026-27 old Act is NOT TY 2026-27 new Act. Notified AY 2026-27 ITR-1 supports two properties; starter need not calculate them. Personalized category allocation is not automatically unregulated.

## Missing dependencies

Original runnable code zip with pom/build files and migrations; independent CA/official-utility expectations and evidence; new-Act legal pack; runtime/integration/security evidence. Existing historical “10,002 PASS” statements are unverified here.

## Resume command for the next chat

“Read system_overview.md, NEXT_SESSION.md and findings_register.md from this foundation pack. I am attaching the actual runnable project zip. Run the baseline, report observed failures, then implement Task 1 output safety and Task 2 interaction-aware fixes using independently approved golden references. Preserve the agreed scope; no broad rewrite or added agents.”

If no approved reference outputs exist yet, work on engineering controls and mark tax correctness pending; do not fill expected values with the same engine under test or claim release approval.

This file is a durable handoff artifact. It is not a claim that automatic cross-chat memory was changed.

## Revision 1 progress

User permits any suitable language; quality remains the priority. Failure review adds F39-F42 for regime availability, live result delivery, cached-result quarantine and isolated recovery. The standard-library Python reference protocol passed 22 scenario tests and checked 10,016 finite states / 22,558 transitions; all eight injected defects produced counterexamples. See verification_models/protocol_check_results.json.

These are protocol-design results, not tax/application/SQL/VAPT results. The golden corpus is unchanged and unapproved. Use the reference histories when implementing real concurrency and delivery tests after the source project is supplied. Protect restored control/quarantine/deletion state before allowing traffic; that drill has not been executed.

## Revision 2 progress and next execution

Standalone Java result-safety core implemented under java_result_safety. Run
python3 build.py and python3 check_mutations.py in that folder. Observed: 50 named
scenario groups, 2,048 guard combinations, all four selected compiled mutations
detected. Source, JAR, six response examples and reports are included. Positive
APPROVED states in those examples are synthetic policy fixtures only.

Original tax application still absent; no existing backend integration was made.
Approved tax golden expectations remain zero. Next connect authoritative control
and calculator adapters to the module, then validate real HTTP/SQL/export behavior.
Do not recreate this module as another paper reference model, disable the approval
guard, infer tax correctness from its tests or put synthetic approval fixtures in
production.

## Revision 3 checkpoint

Current component: java_result_safety 0.1.1. F43 ordering bug reproduced then
fixed. Added signed-candidate delivery and exact English/Hindi amount proposals.
Observed: 104 named groups, 2,048 guard combinations, 4,096 parser cases, 66 signed
field changes, ten selected compiled defects detected. Signing bytes and six
projection examples match across JVMs. Read hardening_report_v3_2.md and
java_result_safety/SIGNED_CANDIDATE_V1.md before integration.

Use SignedResultService for cache/untrusted data. Its registry/control fixtures
are not production adapters. The raw trusted API is not a cache-proof shortcut.
Production signer/custody, current registry, durable quarantine, actual Spring/
MySQL/HTTP tests, confirmation ledger and original calculator are still missing.
All 30 golden expected-output drafts remain independently unapproved and unchanged.
Do not restart completed component work or infer numerical tax correctness from it.

## Revision 4 checkpoint

Current component is java_result_safety 0.1.2. Four backend customer features
implemented: focused next action, review checklist, guarded regime explanation/
status cards and amount-free preparation note. IntakeGuideService works on signed
current input-only snapshots before calculation/approval; GuidedResultService
wraps the original result gate. Contracts and obligations are documented in
GUIDED_FEATURES_V1.md and guided_features_v0_1.md. Actual PWA/HTTP/SQL/confirmation
integration remains missing; the English inline example is illustrative only.

Observed: 135 named groups, 2,048 guard cases, 4,096 parser cases, 256 generated
guidance combinations, 66 signed-field changes; sixteen compiled defects caught.
Six core and six new guidance JSON examples passed checks and cross-JVM matching.
Original calculator project still absent; all 30 golden expectations remain
unapproved/unchanged. Keep input preparation separate from numerical authority.

## Revision 5 checkpoint

Current component is java_result_safety 0.1.3. Explicit amount proposals,
displayed-value confirmation, offered conflict resolution, review resets,
stale-tab/idempotency handling and amount-free history are implemented in the
bounded single-process EphemeralFactLedger. It implements ControlPlane so later
reads reject a prior calculation key after a material edit. Policy lookup runs
outside the edit/close monitor and rechecks current state afterward.

Observed: 178 named groups (56/31/21/28/42), 64 generated edit transitions,
48 actual-thread rounds/192 requests, twenty-four compiled defect variants caught.
Five new receipt/history JSON examples matched across JVMs. The module JAR has
87 library classes and no test fixtures. Read confirmation_flow_v0_1.md and
java_result_safety/FACT_EDITS_V1.md before integration.

Session state is memory-only and lost on restart. MySQL/Docker are unavailable
here; SQL, durable recovery, shared multi-instance state and load are not tested.
Real HTTP/PWA/authentication/user-action/signer integration remains missing.
All 30 golden expected-output drafts stay unchanged and independently unapproved.
Do not turn session receipts into saved-case or tax-accuracy claims.

## Revision 6 checkpoint

Current Java component is 0.1.4. Added read-only ProfessionalQueueService,
current assignment/credential/validity/release checks, snapshot-bound evidence
states and guarded routing. Detailed evidence is hidden when access/result gates
fail. Ready is not approved. No professional-need/accuracy percentage is invented.

Observed: 205 named groups (57/31/21/28/42/26), 1,296 professional evidence
combinations, thirty compiled defects caught and four new JSON examples matched
across JVMs. Existing session concurrency cases reran. JAR has 99 library classes.
Read ca_premium_strategy_v0_1.md and PROFESSIONAL_QUEUE_V1.md before integration.

Live CA portal, real credentials/consent, durable review history, actual evidence
validation/reconciliation, billing and provider filing are not implemented. Do
not advertise 0.0001% CA need or a paid plan that overrides tax/access gates.
The original backend/calculator and approved golden outputs are still missing;
the 30 draft inputs remain unchanged and unapproved. Temporary sessions remain
single-process memory. Full PWA/MySQL/HTTP/load/security integration is pending.

## Revision 7 checkpoint — run the connected sample

Core remains 0.1.4 unchanged. New local_app 0.1.0 provides a runnable Java HTTP
application and English interface, using actual parser/ledger/signed guidance/
result components. Run python3 local_app/run.py and open http://127.0.0.1:8787.
Bundled JARs need Java 17+; Python 3 launches/tests. Sample data only; no PAN/upload.

31 new real HTTP groups pass. Core's existing 205 named groups and 30 compiled
mutation results are retained, not rerun. JavaScript syntax passed. Managed
browser preview was unavailable because Java libjli.so could not load; browser
visual/click/keyboard/accessibility checks remain unverified. State, session
receipts and demo keys are memory-only. No live CA or production tax result exists.

Read local_app_implementation_v0_1.md and local_app/README.md before continuing.
Do not rebuild the already connected sample as another mock or weaken its guards.
Next obtain original runnable backend + independent approved expected stages and
run browser QA where Java/browser preview is supported. All 30 expected-output
sets remain null and independently unapproved. Full Spring/MySQL, durable saved
cases, authoritative signer/control/credential adapters and release gates remain
pending. Do not infer tax accuracy or rare professional-need rates from this demo.

## Revision 8 checkpoint — actual arithmetic, guarded local integration

New tax_engine 0.1.0 implements private scoped arithmetic, independent integer-
paise verification of twenty stages, arithmetic comparison/ties and declared
eligible-80C sensitivity. Local app is 0.2.0. Guard core stays 0.1.4 unchanged.
Start with START_HERE.md; run python3 local_app/run.py. Private operator examples
and reference collection/checking commands are included. No Maven/npm/DB/model
or API key is needed to run the bundled local JARs; Java 17+ and Python 3 are needed.

Observed: 64 engine groups, 4,096 generated cases, twenty altered fields, eight
compiled defects caught, sixteen reference-checker groups and forty-six real
HTTP groups. App returns readiness only, never private arithmetic workings.
UNAPPROVED/PENDING/UNKNOWN controls remain fixed. The template has no expected
values; all original thirty golden inputs remain unchanged and unapproved.
Even matched asserted review metadata is not authoritative release/CA proof.

Read tax_engine_implementation_v0_1.md before the next change. Do not rebuild this
as another mock or use engine output as independent expectations. The original
calculator/Spring backend remains absent; the new module is fresh implementation.
Next obtain qualified independent expected stages/evidence and authoritative
release controls, then integrate actual production identity/MySQL/persistence
and verify browser interaction where Java preview is supported. Real CA/consent/
evidence, reconciliation, billing, full schedules/GST/ITR and filing remain separate
incomplete workflows. No million-case accuracy/professional-need claim exists.

## Revision 9 checkpoint — source comparison

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

Run python3 local_app/run.py and open Compare statements. Load the synthetic
example, compare entries and explicitly select a source/reason or reopen a local
choice. Keep it separate from income confirmation. Do not import native PDFs/JSON
or label this workflow AIS integration. No feedback was sent to any external site.
Next add independently validated native schema/coverage/provenance adapters and
safe proposal binding only when their own data/security cases are available;
qualified numerical tax references and production controls remain blocking inputs.
