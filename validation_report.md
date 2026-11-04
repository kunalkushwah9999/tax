# Validation performed

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

The final extracted startup launcher was checked without rebuilding, including
saved-case/source confirmation, the actual existing dual arithmetic/blocked guard,
and restart persistence. Archive manifests passed. A wrapper lifetime defect found
in this packaging check was corrected by process replacement.

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

Both archives passed checksum verification. The extracted packaged launcher
started without rebuilding, and six selected real HTTP groups passed against its
JAR, including proposal/confirmation, retries, zero handling and concurrent retries.
Windows launcher execution remains unverified.

Earlier chronological records follow; their version-specific limitations do not
replace this current checkpoint.

31 October 2026. These checks concern the delivered review pack, not tax correctness or application security.

| Item | Observed result |
|---|---|
| Structured corpus | 30 unique synthetic cases: 24 arithmetic and 6 workflow controls |
| Numerical expected outputs | All null in JSON and blank in workbook |
| Independent approval | 0 approved; all rows pending |
| Worksheet formulas | 32 formulas; artifact-tool recalculation and error scan completed with no formula-error matches |
| Readiness behavior | Confirmed zeros accepted only with required evidence; missing evidence, rejection and a fake numeric control output correctly prevented completion |
| Restoration after authoring checks | All temporary values removed; exported XML checked |
| Unknown interest | Blank values preserved rather than converted to zero |
| Visual inspection | All three worksheets rendered and inspected; changed input/source areas checked after update |
| Original application integration tests | Not run; original runnable project not supplied |
| CA / official utility | Not run; expected values still need independent work and approval |
| Model inference or benchmark | Not run; candidate metrics and artifact revisions are null |
| Penetration / load test | Not run; own application/staging environment not supplied |
| Native Excel execution | Not operated; XLSX export and artifact-tool formulas were checked |

The findings register contains 42 audit findings. Some concern source defects visible in snippets and some concern design/release controls. It is not a claim of 38 exploited vulnerabilities or 38 applied fixes.

## Revision 1 observed checks

- Python 3.12.14 standard-library scenario suite: 22 tests passed.
- Complete reachable-state exploration inside the documented finite domain: 10,016 states and 22,558 non-no-op transitions; no invariant violation in the correct reference model.
- All eight deliberately faulty variants produced explicit counterexamples recorded in protocol_check_results.json.
- No claim of unbounded/liveness proof, real SQL isolation, network-failure handling, tax/legal correctness, AI-model performance or application VAPT is made by these results.
- Golden workbook and its unapproved expected values were preserved unchanged.

## Revision 2 Java module checks

- Java 17 target compiled with -Xlint:all, -Werror and annotation processing off.
- 50 named scenario groups passed; 2,048 combined guard inputs checked.
- Four deliberately removed guards compiled and produced behavioral test failures.
- Six public JSON examples parsed; output bytes matched across independent JVMs.
- Negative compilation tests reject unknown-value access and unpermitted public
  response implementation/construction. Sensitive domain default strings redact.
- Valid JAR has 36 library class files and excludes test fixtures.
- Original tax application/SQL/HTTP integration, load, CA/official utility and VAPT
  checks remain unrun. Golden workbook/JSON preserved unchanged, approvals zero.

## Revision 3 hardening checks

Actual red/green regression evidence for mismatch hidden by PENDING reference
state. Current Java suite passed 104 named groups (52/31/21), 2,048 generated guard
cases, 4,096 exact parser cases, and 66 signed-field changes. Ten selected source
mutations compiled and were caught by behavioral failures. Source remained intact.
JAR has 53 library classes with test fixtures excluded. Signing bytes and six
public JSON examples matched across two JVMs. UTF-8, --release 17, -proc:none,
-Xlint:all and -Werror were used. No external Java dependency/download was needed.

Golden workbook/JSON preserved byte-for-byte; expected outputs stay empty and
independent approvals zero. No new tax arithmetic, official utility, HF inference,
production SQL/HTTP/key-registry/load/VAPT checks were performed. Prior bounded
protocol evidence was not rerun or extended to cover F43 reference ordering.

## Revision 4 guided feature checks

OpenJDK 17.0.20 compiled component 0.1.2 with Java 17 target, UTF-8,
annotation processing disabled, -Xlint:all and -Werror. 135 named groups passed:
55 policy, 31 proof, 21 parser, 28 guidance. Generated checks: 2,048 guards,
4,096 parser inputs, 256 guidance fact-state combinations and 66 signed-field
changes. Sixteen selected defects compiled and caused behavioral failures.
Six core and six guidance JSON examples parsed and matched across JVMs; canonical
signing bytes also matched. The JAR has 59 library classes, excluding tests/keys.

The inline preview uses actual generated synthetic JSON. JavaScript syntax,
selector existence and fragment shape were checked; browser rendering/visual QA
was not run. No production UI/HTTP/MySQL/confirmation/load/security integration,
HF model inference or numerical tax approval is implied. Golden files unchanged.

## Revision 5 confirmation/session checks

Java 17 target compiled with UTF-8, -proc:none, -Xlint:all and -Werror.
178 named groups passed: 56 policy, 31 proof, 21 parser, 28 guidance, 42 session
edits. Existing generated checks remain 2,048 guards, 4,096 parser cases,
256 guidance combinations and 66 signed-field changes. New checks: 64 edit
transitions, 32 two-writer rounds and 16 eight-retry rounds, totaling 48 rounds
and 192 actual concurrent requests. Controlled edit/close during external policy
lookup and handled pre-publication failure were exercised. Twenty-four selected
defects compiled and caused behavioral failures. Original sources were preserved.
Six core, six guidance and five edit JSON examples passed parsing and cross-JVM
byte checks. The JAR has 87 library classes, excluding test/key fixtures.

This is in-process concurrency evidence. MySQL/MariaDB/Docker were unavailable;
no SQL, durable crash recovery, multi-node, load, original PWA/HTTP, HF inference,
VAPT or tax-law validation was performed. Golden workbook/JSON unchanged, approvals zero.
Earlier Python protocol evidence was not rerun or upgraded by this step.

## Revision 6 professional queue checks

Java 17 target compiled under OpenJDK 17.0.20 with UTF-8, -proc:none,
-Xlint:all and -Werror. 205 named groups passed: 57 policy, 31 proof, 21 parser,
28 guidance, 42 edits and 26 professional. The professional suite checked all
1,296 four-field/six-state evidence combinations. Thirty selected defect variants
compiled and caused behavioral failures; original source was preserved. Four
new professional JSON examples parsed and matched across JVMs alongside all prior
families. JAR contains 99 library classes; tests/ephemeral credentials excluded.

Positive professional credentials, grants and APPROVED references are synthetic
fixtures. No actual CA/customer pilot, membership service, consent capture,
evidence verification, SQL/HTTP portal, billing, filing, load or tax-law validation
ran. Golden workbook/JSON unchanged, independent approvals zero. Queue counts are
request-batch routing observations and not professional-need/accuracy statistics.

## Revision 7 local HTTP application checks

App 0.1.0 compiled against unchanged core 0.1.4 using Java 17 target, UTF-8,
-proc:none, -Xlint:all and -Werror. 31 real loopback HTTP groups passed, zero
failures/errors, including one accepted transition for four simultaneous retries,
full preparation that remains blocked, exact confirmation, stale input/session
isolation and HTTP decoding/CSRF/Origin/Host restrictions. JavaScript syntax passed.
Actual app and core JAR hashes are recorded in local_app/build reports.

Core source/JAR and its previous 205 named groups/30 mutation results are
unchanged; those suites were not repeated as new evidence. The managed browser
preview failed to load Java libjli.so, so visual/click/keyboard/accessibility QA
is not claimed. No production hosting, original backend/SQL, durable recovery,
load, model inference, official tax utility/CA result validation or VAPT ran.
Both golden files are verified against the preceding foundation archive and
remain byte-identical: 30 inputs, all numerical expectations null, approvals zero.

## Revision 8 arithmetic and integrated workbench checks

New private arithmetic workbench compiled with strict Java 17 flags and passed
64 named groups plus 4,096 generated cases. Decimal/bracket and integer-paise/
excess-income implementations compare all twenty stages and age. Tests detected
all twenty deliberately altered stage fields; eight source variants compiled and
caused behavioral failures, preserving original sources. Reference checker passed
16 groups with synthetic assertions only, including that complete matching
metadata still grants no release/CA authentication/evidence validation.

App 0.2.0 passed 46 actual HTTP groups (previous 31 plus 15 planning groups).
Successful dual arithmetic still exposes no private numbers; unknown profile/
deductions, excluded scope, per-regime cap, stale views, CSRF/Origin, exact fields,
request bounds and unchanged ledger/history are exercised. Invalid calendar-date
error handling and new count serialization defects were found and fixed. JS syntax
passed. Actual source/JAR hashes appear in machine reports. Original core 0.1.4
JAR and existing 205-group/30-mutation baseline are unchanged, not rerun as new work.

Original workbook/JSON remain byte-identical: thirty cases, expected outputs
null, approvals zero. The additional collection template is entirely blank.
Its pending check reports zero matched review assertions and no release readiness.
Browser UI/accessibility, production SQL/load/recovery, credential/provider/filing,
real utility/CA numerical validation, HF inference and VAPT remain unverified.

## Revision 9 comparison and HTTP evidence

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

The new defect experiments defaulted UNKNOWN to zero, omitted category from the
matching key, ignored stale review versions and ignored selected observation IDs.
Every variant compiled and failed behaviorally; original Java sources remained
intact. A compiler classpath fixture issue in the experiment runner was repaired;
compiler warnings/errors were not counted as detected behavioral defects.
