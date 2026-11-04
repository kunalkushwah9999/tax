# Current application: TaxDesk 5.1.0

Run `python3 startup_app/run.py` from the extracted package root. Java 17+ and
Python 3 are required. The default app needs no ChatGPT Plus/API subscription.
Use synthetic accounts and data. See ../STARTUP_RELEASE_V5.md and ACCOUNT_SECURITY.md
for the newly implemented MFA, recovery-code and revocation workflows and limits.
Full production statutory tax/filing services remain unreleased; approvals are 0/30.
Current evidence is in build; V4 evidence is preserved under evidence/v4.

---

# Historical application: TaxDesk 4.0.0

Start with `python3 startup_app/run.py` from the extracted package root, or
`python3 run.py` from this module. Java 17+ and Python 3 are required. No ChatGPT
API key or Plus subscription is needed. Use synthetic information only.

The current release adds owner-private capital FIFO working, TDS observation
review and notice-response preparation. See TAX_WORKBENCH.md and
../STARTUP_RELEASE_V4.md for contracts, observed tests and limitations. These are
working papers; full statutory ITR/GST/capital-gains computation and live filing
remain unreleased. Independent tax approvals remain 0/30.

This module's current engineering reports are in build. V3 reports copied from
the prior delivery are preserved under evidence/v3 and retain their original
artifact/source bindings. Earlier notes below are historical.

---

# Startup module 3.0.0

Start with ../START_HERE.md and ../STARTUP_RELEASE_V3.md. New opt-in SQL/recovery
setup: CONCURRENT_STORAGE.md. Current implementation is a synthetic preparation
platform; no numerical tax approval, live charge, verified real CA or filing is
released. OWNER_HANDOFF.md distinguishes remaining code from external inputs.

Earlier module notes follow as history.

---

# TaxDesk startup module 2.0.0

See [../START_HERE.md](../START_HERE.md) for the default runnable setup and
[../STARTUP_RELEASE_V2.md](../STARTUP_RELEASE_V2.md) for the consolidated scope,
passed checks and production gates. [OPERATIONS.md](OPERATIONS.md) documents new
test billing, expert requests and feedback. [NATIVE_IMPORT.md](NATIVE_IMPORT.md)
documents optional native ITR-1 structural checks. There is no live money,
verified real CA status, numerical tax release or filing in this pilot.

Earlier module implementation notes follow as history.

---

# TaxDesk startup development/pilot application 1.3.0

This is the integrated **local development release** for the India-focused
preparation product. It contains customer accounts, persisted sample cases,
explicit confirmation, source review, preparation-team sharing, administration,
planning quotes, support and activity history. The existing Java arithmetic and
result-safety components are reused unchanged.

It is **not an approved production tax service**. Use synthetic information only.
Numerical tax release, payment collection, full ITR/filing, credential verification,
real document ingestion and public production hosting are unavailable. The optional
MySQL pilot and strict HTTPS proxy mode are implemented and tested; setup is in
[deploy/MYSQL_SETUP.md](deploy/MYSQL_SETUP.md).
See ../startup_implementation_v1.md and ../INDIA_LAUNCH_PLAN.md.

## Optional native ITR-1 intake

An owner-only native file preview validates synthetic AY 2026–27 ITR-1 return
JSON against a pinned official schema, masks private fields and can create
separately confirmed estimated income proposals. It is disabled by default and
needs an explicitly configured local worker/reference cache. No schema download
occurs during document handling. See [NATIVE_IMPORT.md](NATIVE_IMPORT.md).
This does not approve business rules, tax arithmetic, statutory form eligibility,
source authenticity or filing. Native GST remains unreleased after primary schema
endpoints returned 403; normalized GST observation checking is still available.

## New self-service and recovery workflows

Owner-only ITR scope/task preparation and normalized GST invoice-observation
reconciliation are integrated into the saved-case UI. See [SELF_SERVICE.md](SELF_SERVICE.md).
These do not determine statutory form/ITC eligibility, release tax or file returns.
Existing preparation-sharing consent does not expose these new private datasets.

Dedicated-key encrypted logical backup, offline authentication and fresh-MySQL
isolated restore are implemented. See [deploy/BACKUP_RESTORE.md](deploy/BACKUP_RESTORE.md).
The restored database refuses normal app startup; production restore promotion,
current-control reconciliation, offsite/physical recovery and key rotation remain pending.
Existing 1.1.0 SQL databases require an explicit --init-mysql migration with their
original key before 1.2.0 starts; V002 adds the recovery-state control table.

## Run the complete development application

Java 17+ and Python 3 are required. No Maven, npm, pip package, model, API key,
database or external network service is required for the packaged local version.
Keep startup_app, local_app, java_result_safety and tax_engine as siblings.
From the extracted root, optionally create the two operator roles first:

```sh
python3 startup_app/run.py --create-operator ADMIN
python3 startup_app/run.py --create-operator PREPARER
```

Each command prompts for a username, display alias and password without putting
the password into command arguments. There are **no seeded users or passwords**.
Stop the server before these setup commands: a second store writer is refused.
Operators are created locally, never through the public registration API.

Then start:

```sh
python3 startup_app/run.py
```

Open **http://127.0.0.1:8788** using that exact address. `localhost` is not an
accepted Host. Windows: use `py -3` in place of `python3`, or start.bat from the
package root. Another port is `--port 8789`; stop with Ctrl+C.

Register a CUSTOMER from the UI, or sign in with an operator created above.
PREPARER is an unverified preparation role, **not a credentialed CA identity**.
ADMIN sees aggregate operations, price configuration, support and amount-free
activity. ADMIN cannot inspect another customer's private financial case.

## Try the implemented flows

1. Create a draft and enter gross salary and separated interest. Save proposals,
   inspect exact values and confirm each separately, including intended zeros.
2. Fill sample profile/scope and eligible-deduction declarations. Profile changes
   reset checkpoints. Complete the income and deduction reviews.
3. Check the saved scenario. Both existing arithmetic implementations run, but
   only readiness and the signed blocked result are returned.
4. Compare normalized sample observations. Record a known-gross source choice,
   check complete annual coverage/classification and replacement, then create an
   estimated income proposal. Sample salary can transfer; conflicting savings
   cannot. TDS is never transferred as income or credit.
5. Declare document availability. This is a checklist, not authenticated evidence.
6. Share one draft with a specific PREPARER for 1–30 days with explicit consent.
   That account can read the draft and add snapshot-bound preparation comments.
   It cannot edit income, export the owner's data or grant CA/filing approval.
   Revocation/expiry deny later reads and old comment retries.
7. ADMIN can create an immutable future price version. After it becomes active,
   customers can generate immutable planning quotes. No charge, tax invoice,
   subscription or premium capability is created.
8. Open an in-app support request; the owner and ADMIN can update that thread.
   No external email, SMS or notification is sent.
9. Export owner-only preparation JSON, archive/restore or delete a sample case.
   The export is not ITR JSON. Old create retries cannot resurrect deleted cases.

## Persistence and bounds

The default profile below is local. The optional explicit MySQL profile stores
AES-GCM encrypted records transactionally, binds ciphertext to record identity
and revision, uses certificate/hostname-verified database TLS and has no local
fallback on SQL failure. Schema initialization uses a separate operator account.
Its single control-row lock serializes all writes and its payload limit is 16 MiB.
Some namespace operations still scan bounded collections; sessions remain local.
This is a pilot, not the final normalized/indexed/pooled Spring repository or a
large-scale deployment. See [../mysql_persistence_v1_1.md](../mysql_persistence_v1_1.md).


The development store is a single encrypted aggregate, at most 8 MiB, protected
by AES-256-GCM with a fresh nonce per replacement. The random key is a separate
file under runtime-data/keys; the encrypted records are under runtime-data/records.
Both are **runtime secrets/data and excluded from all delivery archives**.
Losing the key loses access to the data. Do not copy runtime-data into Git or share
it. This local profile is not managed KMS or a production credential vault.

A process file lock prevents concurrent writers. Transactions work on a private
copy, flush the replacement and atomically rename it on the same filesystem.
Directory syncing is platform-dependent. Actual close/reopen and process-restart
flows are tested; power-loss, storage-controller failure, multi-node concurrency,
backup restore and disaster recovery are not established. The whole aggregate is
rewritten per transaction; this deliberate local profile does not support a
large production workload. The production target remains Java/Spring + MySQL.

Passwords have unique random salts and PBKDF2-HMAC-SHA256 at 600,000 iterations.
HTTP cookies are opaque, HttpOnly, host-only and SameSite=Strict. The loopback
HTTP profile does not set Secure. Explicit `--public-origin https://YOUR_HOSTNAME`
selects exact external Host/Origin checks, Secure `__Host-` cookies and HSTS behind
a trusted TLS proxy; it does not trust X-Forwarded-* authority. Sessions expire
after 30 minutes and disappear
on server restart; accounts/cases persist. There is no email verification,
password recovery, MFA, SSO or production onboarding. No real identity is verified.

Configured local limits: 1,000 accounts, 50 cases per customer, 1,000 sessions,
2,000 sharing grants, 2,000 quotes, 2,000 support threads, 200 price versions,
50 preparation comments/case, 30 support messages/thread, 10,000 amount-free
request receipts and 5,000 amount-free operational events. Requests: 64 KiB JSON,
180 API requests/minute per session (anonymous requests grouped by loopback IP),
30 login/register attempts per IP and 10 per username per 15 minutes; four workers
and a 64-item queue. The normalized source parser keeps its own 32 KiB/200-row
limit. These are bounds, **not capacity benchmarks**.

Deleting a case removes current inputs, profile, source text, notes and grants
from the active aggregate. Operational events and amount-free idempotency receipts
remain bounded to prevent resurrection and preserve accepted outcomes. This does
not prove physical erasure of old filesystem blocks, snapshots or backups. A
reviewed production retention/deletion policy is still required. No real PAN,
Aadhaar, bank document, credentials, customer database or test account is packaged.

## Rebuild and verify

A full Java 17+ JDK with jdk.compiler, jdk.jartool and jdk.httpserver is needed to
build; a Java runtime with jdk.httpserver is sufficient to run the packaged JAR.

```sh
python3 startup_app/build.py
python3 startup_app/test_http.py
python3 startup_app/test_https.py
python3 startup_app/check_mutations.py
python3 startup_app/check_self_service_mutations.py
```

Observed on 1.3.0: 18 native policy groups; 21 actual validator groups plus 28
generated rejections; 20 native real-worker HTTP groups on local and 20 on MySQL;
four actual controlled-child-process fault groups. Six compiled native protocol
and six targeted worker defects were detected. Refreshed existing suites include
27 workspace groups/12 generated ownership checks, 27 preparation groups/60
component changes, eight archive groups, 49 local API groups, 20 SQL groups/four
writer rounds, 48 SQL API groups passed with one local-file-only skip, nine real
isolated-restore groups and 12 HTTPS proxy groups. Existing six business/six
preparation/three SQL compiled defects were rerun. No accuracy percentage follows.

JavaScript syntax and static ID references pass. Desktop landing layout and
signup navigation were inspected in the supported browser preview, which served
**static assets only**. Authenticated browser flows, mobile viewport, keyboard/
accessibility audit and a production browser environment remain unverified.
Actual Java API behavior was tested separately through local HTTP, MySQL-backed
HTTP and a TLS proxy fixture. An attempted headless browser installation returned
an invalid/truncated download; no new browser/mobile/keyboard pass is claimed.
