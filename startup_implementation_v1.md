## Current checkpoint — 5.0.0 (3 November 2026)

Current source/executable delivery is described in [STARTUP_RELEASE_V5.md](STARTUP_RELEASE_V5.md),
with [START_HERE.md](START_HERE.md) and [OWNER_HANDOFF.md](OWNER_HANDOFF.md).
Added authenticator MFA, one-use recovery codes, protected password/device changes,
shared SQL session revocation, request/transaction epoch checks and HTTPS operator
MFA enforcement. See startup_app/ACCOUNT_SECURITY.md for contracts and limits.
This is an implemented account-security workflow, not full account/identity recovery.

Observed: 20 security unit groups, 12 checks based on six published RFC vectors, 25 API groups per
backend (one intentional local-only skip), 10 compiled security defects and 41
browser flows on concurrent SQL. Two actual Java app processes enforce SQL
revocation. An in-flight revoked edit and lost MFA-activation acknowledgement were
also tested. Existing preparation/native/workbench/commerce/recovery suites passed.
The three original component modules and original 30-case golden corpus are unchanged.

Production is still not complete. Independent tax approvals remain **0/30**.
Full statutory ITR/GST/capital-gains/TDS implementation and approved rule release,
live filing/commercial payments, real CA verification, actual hosting/scale/security
review and restore promotion remain work. Forgotten-password recovery, passkeys and
external security notifications are also not connected. Credentials alone cannot
complete these software gaps. Synthetic data only; no proven 0.001% referral,
competitor-superiority or growth/profit claim. Older records below are historical.

---

## Historical checkpoint — 4.0.0 (3 November 2026)

The current executable release is documented in [STARTUP_RELEASE_V4.md](STARTUP_RELEASE_V4.md).
Start with [START_HERE.md](START_HERE.md) and [OWNER_HANDOFF.md](OWNER_HANDOFF.md).
Added owner-private capital BUY/SELL FIFO working, TDS observation reconciliation,
and notice-response organizing, with real persistence, reviews, exports and UI.
Capital sales cannot enter the narrow salary/interest assessment. TDS matching
never grants a credit and user-reported notice receipts never become verified filing.
These are working papers, not full statutory capital-gains/ITR/GST implementation.

New evidence: 26 named workbench groups plus 59 generated conservation cases,
24 workbench API groups on each of three storage profiles, eight compiled defects
detected, and 31 desktop/mobile browser flows on real concurrent MySQL. Existing
HTTP/native/commerce/HTTPS, SQL crash/recovery and isolated restore checks passed.
The previous three component JARs and original golden corpus remain unchanged.

Independent tax approvals remain **0/30**. Full statutory schedules, approved
rules/output release, live filing/payments, real CA verification, production
hosting/security/scale and restore promotion still require work. Credentials alone
do not implement them. The default runnable app needs no ChatGPT Plus/API key.
All older checkpoints below are historical; current evidence/readiness is V4.
Synthetic information only. No proven 0.001% referral or superiority/growth claim.

---

## Historical checkpoint — 3.0.0 (2 November 2026)

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

# Integrated startup application 1.3.0

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

## Historical implementation: 1.2.0


## Current startup checkpoint: 1.2.0 — self-service preparation and isolated recovery

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

## Historical implementation: 1.1.0


## Current startup checkpoint: 1.1.0 — MySQL and HTTPS pilot

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

## Historical implementation: 1.0.0


1 November 2026. This records actual new implementation, not production approval,
a registered business, a public deployment or a growth forecast.

## Delivered application

| Area | Implemented behavior | Practical boundary |
|---|---|---|
| Product front door | English landing, customer enrollment, sign-in and persistent workspace | No public service deployed; synthetic local aliases only |
| Identity | Unique customer usernames, salted password verifiers, opaque rotated sessions/CSRF, bounded attempts | No email verification, recovery, MFA, SSO or real identity proof |
| Saved drafts | Owner-bound case creation/list/open, revision CAS, unknown/zero/estimated/confirmed facts | Single-process development store, not a production SQL adapter |
| Preparation | Existing exact parser, separate confirmation, profile/scope/deduction intent and reset checkpoints | Narrow FY 2025–26 adult-ROR salary/ordinary-interest starter |
| Source workflow | Current normalized source comparison, explicit selections, conservative annual gross replacement proposals | Declared labels; no native parser, TDS credit, inferred coverage or authenticity |
| Arithmetic | Existing decimal engine and integer-paise verifier, existing signed result guard | Only readiness is exposed; independent numerical approval remains zero |
| Coordination | Specific case consent, 1–30 day grants, expiry/revocation and PREPARER queue | Unverified preparation role; no CA credentials, sign-off or financial input editing |
| Working comments | Snapshot-bound preparation notes; stale comments remain visibly historical | No professional approval or evidence authentication |
| Administration | Aggregate counts, price configuration, support queue and amount-free activity | No private-case, tax-approval, role-upgrade or paid-bypass API |
| Commercial | Immutable future price versions and price-frozen expiring planning quotes | No checkout, payment, tax invoice, entitlement or refund integration |
| Support | Owner/ADMIN in-app threads with versioned updates | No external message, SMS or email sent |
| Portability | Owner-only preparation JSON attachment | Not an ITR export, payable/refund, filing acknowledgement or professional report |
| Lifecycle | Archive/restore; case deletion removes active case/source/comment/grant data | Bounded operational metadata/receipts retained; physical/backup erasure unverified |

This code is additive. The previous 0.4.0 anonymous application is preserved and
still runs independently. The new saved-case application does not replace or
weaken its tax-result guard, original golden corpus or scoped arithmetic contract.

## Implementation and security boundaries

The new module is Java 17 with JDK HttpServer and bundled native HTML/CSS/JS.
No new external runtime dependency is required. It links the existing verified
core/engine/0.4.0 JARs; it does not rewrite tax arithmetic into JavaScript.
StartupAdapter is a trusted bridge in the existing adapter package, with current
case authorization established by the new server before invocation. Calculation
reads use the existing profile form contract and signed blocked result projection.
Current case identity is rechecked during and after evaluation. Payment/price,
local role, prepared checklist and matching arithmetic cannot approve tax release.

The local VaultStore uses AES-256-GCM, a fresh nonce for each encrypted replacement
and fixed format-associated data. A separate random key file is created privately
outside the records directory. Passwords use independent salts and
PBKDF2-HMAC-SHA256 at 600,000 iterations. These are implemented engineering choices;
they do not establish a security certification or production key-management system.

Local transactions copy the current aggregate, apply bounded validation, write
and flush a private temporary file, then atomically replace on the same filesystem.
A second writer is refused. The committed in-memory snapshot changes only after
successful replacement. Directory syncing is platform-dependent; no universal
power-loss guarantee, SQL isolation, distributed durability, online backup or
restore/recovery guarantee is claimed. Actual close/reopen and a real server
restart were exercised. The whole aggregate is rewritten per transaction and
capped at 8 MiB; this is deliberately a local development profile. Production
Java/Spring + MySQL integration remains a concrete implementation dependency.

HTTP requires exact loopback Host/Origin, same-origin metadata where supplied,
POST+current CSRF for writes, opaque host-only HttpOnly SameSite=Strict cookies,
strict bounded JSON and exact operation field sets. No CORS approval, arbitrary
file serving, tax/owner/role mass assignment or private candidate serialization
exists. Secure cookies/TLS/proxy deployment are not implemented in this loopback
profile. Authenticated operations and logout on the same session are serialized.
Server restarts revoke sessions but retain saved accounts/cases.

Ownership and current purpose-specific grants are checked before reads, writes
and accepted-receipt delivery. ADMIN has operational counts and support access,
not a global financial-case override. PREPARER can read specifically shared drafts
and add comments but cannot edit income, export owner data, authenticate source
records, change grants/prices or grant CA/filing approval. Revocation/expiry also
deny old note retries. Registration always creates CUSTOMER; operator setup is
an explicit local CLI action requiring exclusive access to the development store.
There are no default or pre-seeded credentials in the delivery.

Accepted operations have a user/request-ID namespace and canonical request
fingerprint, with amount-free receipt and original outcome revision. They commit
with their mutation in the same local transaction. Exact retries preserve later
state. Changed/cross-route reuse conflicts. Deleting a case retains the bounded
receipt metadata needed to prevent old create retries from resurrecting it. This
is not an anonymous hash scheme or evidence of physical deletion compliance.
Detailed quotas, retention behavior and API contracts are in startup_app/README.md
and startup_app/API.md.

The saved source workflow retains only the latest normalized source text and
selection state with its bound input snapshot; it does not append raw financial
snapshots to history. This is purposeful source evidence separate from the
current proposed/confirmed fact, not a second category total. Source transfer
uses the existing 0.4.0 conservative policy. Source references are explicitly
historical/unverified. Manual new proposals/clears remove a field's link; old
accepted retries cannot relink it. Confirmation remains a separate exact-value
user action. TDS is never transferred into income or credits.

## Executed evidence

| Check | Observed result |
|---|---|
| Java compilation | Java 17 target, UTF-8, -proc:none, -Xlint:all, -Werror passed |
| New unit suite | 25 named groups passed; 12 generated cross-owner denial checks |
| New real loopback suite | 36 groups passed, zero failures/errors |
| Persistence | Real close/reopen and process restart retained saved inputs/accounts; old session denied |
| Corruption and secrets | Tampered AES-GCM record rejected; missing key could not silently recreate data; second writer refused; ciphertext checked for known test values/password |
| Narrow concurrency | Two identical HTTP commands made one edit and a replay; two distinct commands from one version made one edit and a stale rejection |
| Access | Cross-customer/admin/preparer private-case denial, scoped grants, revocation/expiry, old note retry denial |
| Tax guard integration | Complete prepared scenario ran both existing implementations; MATCH readiness with BLOCKED result and false filing/export capabilities |
| Commercial | Future price immutability, old quote preservation, replay and no payment/entitlement upgrade |
| Lifecycle | Owner export, archive/restore and deletion; old create receipt could not recreate deleted data |
| Selected defects | Six separately compiled source copies produced behavioral test failures; original sources unchanged |
| Extracted package | Launcher started without rebuilding; owner case/source confirmation, the existing dual engine/guard, real restart and both archive manifests verified |
| Interface | Node syntax, literal IDs and duplicate-ID checks passed; desktop static landing and signup navigation inspected |

Selected injected defects: remove private owner check, allow a revoked grant,
ignore expected case revision, auto-confirm an amount proposal, forget deleted
create receipts, and rewrite existing quotes when a new price is published.
Compilation errors are not counted as detected defects. This is not exhaustive
fault coverage or an independent penetration test.

Browser preview served static assets only. Its expected API-unavailable notice
was visible; no mock API result was substituted and no authenticated transaction
was presented as browser-verified. Actual backend transactions were checked via
real loopback HTTP. Authenticated browser flows, mobile viewport, keyboard/
accessibility audit, Windows execution and production UX remain unverified.

The previous core/engine/0.4.0 regression evidence remains retained with identical
source/JAR bytes; those suites were not rerun or counted as new startup tests.
Both original golden files are preserved byte-for-byte: 30 inputs, all expected
numerical outputs blank/null, zero independently approved outputs. No CA/official
utility, MySQL/JDBC/Spring test, payment or filing connection, model inference,
production load test, public deployment or independent security audit ran.

## Run, deployment and founder handoff

Extract the complete pack, install Java 17+ and Python 3, optionally create local
ADMIN/PREPARER operators with the README setup commands, then run:

```sh
python3 startup_app/run.py
```

Open http://127.0.0.1:8788. The root start.sh/start.bat launch this application. A packaged restart check
found and fixed a wrapper/child-process lifetime issue: the Python launcher now
replaces itself with Java, so stopping it releases the development store lock.
The actual HTTP suite also runs through this launcher.
The old anonymous app remains `python3 local_app/run.py` at port 8787. The full
pack includes source, current compiled JARs, tests, reports, exact golden inputs,
prior source/Hugging Face evaluation and the new India pilot/launch plan.

INDIA_LAUNCH_PLAN.md supplies proposed interviews, success metrics and explicit
gates for a real India launch. It does not assert customer demand, growth,
commercial superiority or a price recommendation. A startup is not complete
merely because local workflows run: independently approved tax rules/references,
real SQL/authentication/hosting operations, appropriate credential/consent/data
handling and live provider workflows remain before offering affected services.
The app has no admin or paid bypass for those missing approvals.

Implementation references (engineering guidance, not certification):
- https://docs.oracle.com/en/java/javase/17/security/java-cryptography-architecture-jca-reference-guide.html
- https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
- https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/file/StandardCopyOption.html

Startup JAR SHA-256: `66e9fb9382263c3b6f017b46cdc51c9f0a49d7a31b10e5cbff8ec0d478aeccd9`.
