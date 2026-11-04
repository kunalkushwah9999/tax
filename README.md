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

# Tax platform foundation v3

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

Prepared on 31 October 2026.

This pack includes reviews, draft tax references, an abstract verification model, a compiled/tested Java result-safety component and a runnable local Java/HTTP preparation application. It is not the full tax application or a patch of the missing original project.

Start with START_HERE.md for the runnable workbench; read tax_platform_review_v3.md for the review history. system_overview.md is the concise current design. findings_register.md maps 43 findings to fixes/tests. implementation_tasks.md orders the work. hf_evaluation.md and hf_candidates.json record model candidates with no benchmark/production approval. source_register.md lists evidence and limits. NEXT_SESSION.md preserves the continuation point.

golden_cases_v0.xlsx is the reviewer workbook. golden_cases_v0.json is the same synthetic input corpus in structured form. All numerical expectations are blank/null; approved case count is zero. The workbook's readiness fields are workflow checks, not tax-engine test results. Do not use this pack to file a return.

Input period: Income-tax Act 1961, FY 2025-26 / AY 2026-27. It is not automatically a Tax Year 2026-27 pack under the new Act.

No real PAN, Aadhaar, bank document, model weights, hidden credentials or original chat export is included in the delivery archive. Source snippets and historical passing claims were not converted into a fabricated working application.

## Revision 1

failure_review_v3_1.md adds delivery/quarantine, regime-availability and isolated-recovery controls. verification_models contains executable reference code and measured checker output: 22 scenario tests, 10,016 finite states, 22,558 transitions, eight deliberately faulty variants detected. Read the model's abstraction limits before using its results. The actual tax application and real DB still require implementation/testing. No golden expectation has been approved.

## Revision 2

java_result_safety contains the implemented standalone Java 17 guard, compiled
library JAR, tests, portable build runner and actual reports. See
result_safety_implementation.md. Local checks: 50 named scenarios, 2,048 combined
guards and four selected compiled mutations detected. Application integration,
production adapters, tax correctness, database/load/security validation remain
pending. Golden expectations are unchanged and independently unapproved.

## Revision 3

Java component 0.1.1 adds signed delivery and exact Hindi/English amount
proposals, and fixes a reproduced quarantine ordering bug (F43). Current observed
checks: 104 named groups, 2,048 guard cases, 4,096 parser cases, 66 signed-field
changes and ten selected compiled defects detected. See hardening_report_v3_2.md.
This pack includes source/JAR/tests, not the original full tax application.
Independent golden approvals remain zero; deployment/integration are pending.

## Revision 4

Component 0.1.2 adds signed current input preparation, focused review actions,
an eight-field checklist, guarded regime explanations and a preparation note.
135 named groups pass; sixteen selected compiled defects are caught. Six new
guidance JSON examples and full contracts are included. See guided_features_v0_1.md.
The actual PWA/HTTP/database application and approved tax expectations remain pending.

## Revision 5

Component 0.1.3 adds bounded single-process session input confirmation and
edit handling, with stale-tab/retry protection and prior-result invalidation at
later reads. 178 named groups pass; 48 actual-thread rounds/192 requests and
twenty-four selected compiled defects were checked. See confirmation_flow_v0_1.md.
The ledger is temporary memory, not durable saved cases or a MySQL replacement.
Original-backend/PWA/SQL integration and independent tax references remain pending.

## Revision 6

Component 0.1.4 adds read-only professional readiness routing and evidence
statuses, with 205 named groups and thirty selected compiled defects detected.
See ca_premium_strategy_v0_1.md for the requirement matrix, paid-value direction
and actual limits. Live CA functionality and tax references are still pending;
no rare-professional-need, accuracy, conversion or production claim is established.

## Revision 7: run the connected local application

From this extracted directory run python3 local_app/run.py with Java 17+ and
Python 3 installed, then open http://127.0.0.1:8787. The JARs are bundled; no
Maven, npm, database or external service is needed to run. See local_app/README.md
for direct Java/Windows commands and local_app_implementation_v0_1.md for details.

The English interface connects sample amount proposals, confirmation, checklist,
review resets, change history and guarded result status to the actual Java core.
31 real HTTP groups pass. Browser visual/click QA remains unverified because the
managed preview's Java runtime could not load libjli.so. No tax is calculated:
the 30 independent golden expectations remain unapproved. Sample session data is
memory-only; original backend/MySQL, live CA and production release remain pending.

## Revision 8: actual private arithmetic and reference workbench

Start with START_HERE.md. Run python3 local_app/run.py, then open the exact
http://127.0.0.1:8787 address. Java 17+ and Python 3 are required; compiled JARs,
Windows/Linux launchers and the local verification runner are included.

New tax_engine supplies actual scoped BigDecimal arithmetic, separate integer-
paise verification, 20-stage private workings, arithmetic comparison, eligible-
80C sensitivity and independent-reference collection/checking. Local app 0.2.0
uses this engine behind profile/screening confirmation and the unchanged signed
result guard. Browser results remain nonnumerical and blocked pending approval.

64 engine groups, 4,096 generated cases, eight compiled defects, 16 reference-
checker groups and 46 real HTTP groups pass. The core JAR is unchanged. Original
30-case golden files and zero independent approvals are preserved. Browser QA,
production persistence/deployment, authoritative release/CA controls, documents,
payments, broader tax schedules and filing remain pending; this is not the full
production platform. See tax_engine_implementation_v0_1.md for exact evidence.

## Revision 9: normalized statement comparison

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
