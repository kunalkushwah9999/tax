# TaxDesk 3.0.0 — code delivery, verification and release boundary

2 November 2026. This release improves the runnable preparation platform with an
opt-in concurrent MySQL store, streaming authenticated backups and safer payment
recovery. It is **not a production-complete autonomous CA replacement**. Source,
compiled application/components, migrations, fixtures, tests, setup and founder
handoff are delivered together. No claim of 0.001% expert involvement, competitor
superiority, 100,000-user capacity, growth or profit is established.

## Concrete changes

| Problem in the previous release | Implemented behavior in 3.0 |
| --- | --- |
| MySQL held one global write guard throughout each business transaction | New `mysql-concurrent` profile does optimistic work, validates point/scan dependencies and acquires short ordered shard locks for publication |
| Stored encrypted state was capped at an aggregate 16 MiB | Schema-2 store removes that aggregate check; actual stored payload above 16 MiB was exercised |
| Logical JSON backups needed the whole aggregate in memory and had a small aggregate bound | New framed format streams a consistent JDBC snapshot, authenticates each archive/sequence-bound frame and checks a final digest/count/EOF |
| Recovery of a larger snapshot needed a safe target contract | Fully authenticate, import into a fresh target/key, commit records plus shard accounting together, remain ISOLATED; normal app/init refuse the target |
| A payment-create response could be uncertain while a different request ID created another order | New owner/plan barrier blocks fresh IDs while CREATE_PENDING/OUTCOME_UNKNOWN exists; exact accepted replay still works without another provider POST |
| The browser needed to preserve an uncertain payment-create action | The response now marks retry_same_request=true; the existing UI retains its exact retry action |

The original `mysql` profile keeps its compatible global guard/16 MiB contract.
Local vault mode keeps its 8 MiB bound. The new profile must be selected explicitly
on a fresh database/key. It cannot silently convert or share a legacy database.
Full namespace scans, quota checks, connection-per-transaction behavior and bounded
process-local sessions/workers remain. Short quota/scan locks can still serialize
publication or cause retries. This change is not a normalized repository,
connection pool, distributed identity system or production load certification.

## Included application scope

The English interface has accounts, encrypted saved cases, separate amount
proposal/confirmation, profile/scope, source comparison, ITR preparation tasks,
optional pinned ITR-1 JSON structural checks, normalized synthetic GST observation
review, owner exports/archive/delete, consented preparer comments, test plans and
credits, test payment verification/refunds, expert preparation requests, structured
feedback triage and operational administration. It has no general-purpose chatbot.

The tax pack remains narrow FY 2025–26 / AY 2026–27 salary and separated ordinary
interest. Numerical public tax results remain blocked. ITR schema conformance is
not legal eligibility, business-rule validation or tax approval. Invoice matching
is not ITC eligibility. Full ITR/GST/capital-gains, real CA services, live billing
and government filing are not released. **Independent tax approvals remain 0/30.**
The original golden inputs and blank expected values are unchanged.

## Run without a ChatGPT dependency

Java 17+ and Python 3, from the extracted top-level folder:

```sh
python3 startup_app/run.py
```

Open `http://127.0.0.1:8788`. Register a sample CUSTOMER. No account/password or
private signing key is seeded. Optional ADMIN/PREPARER setup before starting the
local server uses `--create-operator ADMIN` / `PREPARER`. Running this application
does not require a ChatGPT API key or Plus subscription. Keep the downloaded ZIP.

[START_HERE.md](START_HERE.md) is the entry point. Read
[OWNER_HANDOFF.md](OWNER_HANDOFF.md) for an honest separation of delivered software,
remaining implementation and external owner approvals/access. Optional setup:

- [Concurrent storage and framed recovery](startup_app/CONCURRENT_STORAGE.md)
- [ITR native-file checks](startup_app/NATIVE_IMPORT.md)
- [Test commerce, expert requests and feedback](startup_app/OPERATIONS.md)
- [MySQL TLS/accounts](startup_app/deploy/MYSQL_SETUP.md)
- [Legacy isolated backup/restore](startup_app/deploy/BACKUP_RESTORE.md)

## Executed engineering evidence

| Suite | Result | Qualification |
| --- | --- | --- |
| Startup unit | 27 groups + 12 generated owner-isolation checks | Business/auth/version invariants |
| ITR/GST self-service | 27 groups + 60 generated mismatches | Preparation, not statutory validation |
| Legacy backup unit | 9 groups | Existing framing/encryption/upgrade contract |
| New framed backup unit | 8 groups | Real local export/reconstruction, tamper/footer/EOF/length/key checks |
| Local authenticated HTTP | 49/49 | Actual app, including permission/replay/deletion/preparation routes |
| Legacy MySQL storage | 20 groups + 4 writer rounds | Actual MySQL 8.4.13 and pinned Connector/J 26.10.0 with TLS identity checks |
| New concurrent MySQL storage | 26 groups + 4 writer rounds | Independent-record progress while another callback waits; stale/replay, read dependencies, scan phantoms, write skew, quota, lost commit acknowledgement, in-flight case-grant revocation and >16 MiB persistence |
| Base HTTP on SQL | 48 passed + 1 intentional local-vault-only skip on each SQL profile | 49 run per profile |
| New framed SQL restore | 10 groups | Actual >16 MiB archive, fresh isolated import, complete state equality, no overwrite/promotion |
| Legacy SQL restore | 9 groups | Existing isolated logical recovery still passes |
| Test operations | 35 local + 35 legacy SQL + 35 concurrent SQL | Explicit loopback fake provider and fresh signed TEST experts; includes new-ID barrier and exact-retry signal |
| Native ITR Java policy | 18 groups | Controlled proposal protocol fixtures |
| Native actual validator | 21 groups + 28 generated rejections | Pinned official schema and jsonschema 4.26.0; not independent tax approval |
| Native actual Java/Python HTTP | 20 local + 20 legacy SQL + 20 concurrent SQL | Real worker, owner/preview/replay/confirmation/privacy checks |
| Native child-process faults | 4 groups | Actual bounded subprocesses, invalid/oversize output, hung stdin, cap and slot recovery |
| HTTPS API | 12 groups | Disposable actual TLS proxy/CA, Secure cookie and origin checks; public Nginx deployment not tested |
| Authenticated browser on concurrent SQL | 22 passed; no JavaScript errors | Actual Chrome/Playwright, desktop and 390px viewport; expected final logged-out 401 observed |
| Deliberately broken Java variants | 6 business + 6 self-service + 3 legacy SQL + 6 native + 8 operations + 4 concurrent SQL detected | Each compiled and failed behavioral assertions; original sources preserved |

The new SQL defect experiments disable record read validation, namespace scan
validation, lost-ack reporting and capacity rechecks in separate compiled copies.
The added payment defect removes the unresolved-order barrier; its targeted HTTP
assertions fail. These selected experiments are not exhaustive proofs or a
security/tax certification. Internal test groups, generated cases and fault
variants must not be added into an accuracy denominator.

The browser exercised registration, case creation, amount confirmation, profile,
source observations, ITR declarations/native preview, estimated native replacement,
GST preparation, safely rendered feedback, test expert consent, verified test
webhook/access, review-bundle download, preparer review, admin feedback/price
publication, re-login, mobile navigation and logout. Desktop and mobile screenshots
were inspected internally. Physical devices, Safari and a full accessibility audit
remain untested. Provider collection and CA verification use explicit TEST fixtures.

SQL SIGKILL/restart, committed-state recovery, API failure during DB outage and no
local fallback were exercised on both profiles. Neither power-loss/PITR/offsite
recovery nor reconciled production restore promotion was certified. Framed format
bounds remain **1 MiB/frame, 256 KiB/record, 100,000 records and 1 GiB/archive**.
These are resource limits, not user/load capacity. Server account/case/receipt/
operations/session/worker limits are still bounded and documented.

The original guard 0.1.4, decimal engine 0.1.0 and anonymous app 0.4.0 source/JARs
are unchanged byte-for-byte. Their historical suites were not counted as new
independent tax validation. The extracted final package is smoke-tested separately;
its manifests and shipped launcher/workflows are checked without a rebuild.

## Current artifact binding

Startup JAR SHA-256:

`1913ca1bef605df9483c13e7aada6edc77833bafaf84c2139222fe783f631e77`

All current report bindings and source/resource inputs are checked before
packaging. The archive carries SHA-256 manifests and the original blank golden
sheet. Runtime customer data, database/vendor/browser binaries, official source
caches, account credentials, record/backup keys and test signing keys are excluded.
The stable archive filenames retain their existing identities; startup module
version inside them is 3.0.0. Earlier release documents are implementation history.

## Reproduce or inspect

```sh
python3 startup_app/build.py
python3 startup_app/test_http.py
python3 startup_app/test_https.py
python3 startup_app/tools/release_preflight.py --mode engineering
python3 startup_app/tools/release_preflight.py --mode production
```

Production preflight **intentionally exits 2** with unmet gates. It cannot approve
rules or switch on live services. Optional signed-test operations, pinned native
worker, actual MySQL and browser prerequisites/commands are documented in the
linked module guides. The new real-SQL harness is `test_concurrent_mysql.py`;
`--with-concurrent-mutations` uses fresh destructive test schemas, never an
existing database. No real provider credentials, tax returns or people were used.

## Remaining production work

There is still substantial software work: complete statutory schedules and tax
validation; authorized filing/live commercial adapters; real professional identity;
normalized/query-filtered repositories and pooling; distributed sessions/admission;
account recovery/MFA; production key/control/revocation/quarantine; Oracle sync,
retention/PITR/offsite recovery and reconciled restore promotion. External inputs
include independent approved tax references, provider/CA onboarding/access,
consent/e-verification arrangements and real hosting/operations ownership.

Credentials alone do not complete that software or prove market demand. Real
customer cohorts, retention, support/referral rates, paid conversion and verified
outcomes are needed for growth/superiority claims. No public launch, real charge,
government submission or verified live CA booking occurred. Use synthetic data.
