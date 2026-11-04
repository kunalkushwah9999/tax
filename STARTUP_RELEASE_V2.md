# TaxDesk 2.0.0 — consolidated system and release evidence

Release date: 1 November 2026. Deliverable: **runnable full-source synthetic pilot**
with consolidated setup, test evidence and commercial workflow foundations. A
production startup and an approved autonomous ITR/GST filing service have not been
completed. All claims below distinguish implemented software from external
approvals, real provider access and market outcomes.

## What the delivered system does

| Area | Implemented behavior | Boundary |
| --- | --- | --- |
| Customer workspace | Accounts, CSRF sessions, encrypted saved drafts, explicit proposals/confirmations, profile/scope and document declarations | Sample aliases and synthetic inputs; no real PAN collection |
| Arithmetic | Existing decimal engine plus separate integer-paise verifier, stale-source checks and result guard | Numerical customer tax results blocked; independent reference approvals 0/30 |
| Source comparison | Normalized sample AIS/26AS-style observations, gross/TDS separation, local selection, annual-category replacement with explicit coverage/replacement review | No native AIS/26AS/PDF authentication or government feedback submission |
| ITR self-service | Fourteen explicit scope declarations, version-bound preparation tasks and exports | Narrow salary/ordinary-interest preparation, not statutory form selection or full ITR support |
| Native ITR input | Optional offline pinned AY 2026–27 ITR-1 JSON schema worker; masked structural preview and selected estimated category proposals | Schema validation does not establish source authenticity, legal eligibility, business-rule/tax correctness or filing readiness |
| GST self-service | Normalized synthetic books/statement CSV matching, duplicates/missing/unknown/date/component/RCM/amendment issues, version-bound review and export | Supplier aliases, not native GSTR-2B/IMS; matching does not approve ITC or create GSTR-1/3B/nil returns |
| Plans and billing | Future immutable test catalog, accepted terms, durable order reservation, provider binding, callback/webhook HMAC checks, captured-payment verification, idempotent entitlement, expiry/refund suspension/revocation | Explicit test mode only; no real money, recurring debit, tax invoice or live account certification |
| Personal premium workflow | Preparation review bundle per input snapshot with atomic credit usage, free exact retry and same-snapshot reuse | No paid approval override; ordinary owner data export remains available |
| Professional workspace | Time-limited consented case queue, snapshot comments; user-requested preparation help, accept/decline/cancel, request-specific grants | Signed test attestations only; no verified live CA roster or statutory sign-off |
| Feedback loop | Feature/rating/message, duplicate-safe submission, owner history, admin revisioned triage/reply, aggregate feature signals | No live customer dataset, external unattended monitoring or automatic legal-rule changes |
| Persistence/recovery | Local encrypted vault; optional actual MySQL with TLS identity checks, per-record encryption/HMAC lookup, atomic business receipts, encrypted logical backup and isolated fresh-MySQL restore | Local 8 MiB/SQL 16 MiB bounds; SQL serial writes, process-local sessions/rate limits; restore promotion disabled |
| UI | English task screens, desktop and mobile viewport navigation, no generic chatbot | Tested Chrome smoke scope; not every browser/device or an accessibility certification |

## Start and use the code

Extract the standalone archive and run from its top-level folder:

```sh
python3 startup_app/run.py
```

Use `http://127.0.0.1:8788`. No account/password is seeded. Register a synthetic
CUSTOMER. ADMIN/PREPARER accounts are created by the interactive operator command
before starting the local server. [START_HERE.md](START_HERE.md) is the single entry
point. Optional setup is in [OPERATIONS.md](startup_app/OPERATIONS.md),
[NATIVE_IMPORT.md](startup_app/NATIVE_IMPORT.md),
[MYSQL_SETUP.md](startup_app/deploy/MYSQL_SETUP.md) and
[BACKUP_RESTORE.md](startup_app/deploy/BACKUP_RESTORE.md).

The standalone archive is named `tax_platform_startup_v1.zip` to preserve the
existing downloadable artifact identity; its current startup module is **2.0.0**.
The foundation ZIP also contains the broader design/source register/Hugging Face
evaluation and implementation history. Both have an archive-wide SHA-256 manifest
and module manifests. The three previous core components and the original 30
blank-expected golden inputs are preserved byte-for-byte.

## Evidence from executable checks

All listed current startup engineering suites passed. These counts are distinct
suite groups, generated checks or selected defects; they are **not** a summed tax
accuracy rate or independent legal validation.

| Suite | Observed result | Qualification |
| --- | --- | --- |
| Startup unit | 27 groups + 12 generated owner-isolation checks | Local business/auth/version invariants |
| ITR/GST self-service | 27 groups + 60 generated component mismatches | Preparation rules, not statutory correctness |
| Backup unit | 9 groups | Includes legacy empty-operations upgrade; cryptographic framing/tamper checks |
| Native Java policy | 18 groups | Protocol tests use controlled previews |
| Native real worker | 21 groups + 28 generated rejections | Actual jsonschema 4.26.0 and pinned official schema; no approved tax expectations |
| Native child faults | 4 groups | Actual bounded subprocesses, invalid output/size, stdin hang, cap and slot recovery |
| Local HTTP | 49/49 | Actual app, session/owner/CSRF/replay/stale/deletion and preparation routes |
| MySQL base HTTP | 48 passed, 1 intentional local-vault-only skip | 49 run against actual MySQL 8.4.13 and Connector/J 26.10.0 |
| Native HTTP | 20 local + 20 actual-MySQL | Actual Java → Python worker, proposal/confirm/restart/raw-data boundaries |
| New operations HTTP | 35 local + 35 actual-MySQL | Explicit fake provider and fresh signed TEST experts; no real charges/ICAI claim |
| MySQL storage | 20 groups + 4 distinct-writer rounds | Real TLS, rollback, lost commit acknowledgement, write guard, ciphertext binding and outage/restart |
| MySQL logical restore | 9 groups | Full snapshot equality including operations; deleted/revoked historical data not served; isolation enforced |
| HTTPS authenticated API | 12 groups | Real disposable TLS proxy/CA; Nginx runtime/public deployment not tested |
| Authenticated browser | 22 passed, no JavaScript errors | Chrome for Testing 156.0.8078.80, Playwright 1.62.1; 1440×1000 and 390×844 viewports |
| Deliberately broken Java variants | 6 business + 6 self-service + 3 SQL + 6 native + 7 operations detected | Each compiled and failed behavioral assertions; original sources preserved |
| Deliberately broken worker variants | 6/6 detected | Targeted real-validator assertions; sources preserved |

Browser flows include customer registration, create/reopen, proposal/confirmation,
profile, source comparison, ITR declarations, masked native preview and estimated
replacement, GST comparison, feedback with an HTML-injection string, test expert
consent, test order and verified webhook, bundle download, preparer review, admin
feedback reply/plan publication, re-login, mobile viewport navigation and logout.
Desktop review-bundle and mobile GST screenshots were visually inspected. The
payment webhook in browser QA is delivered by the explicit provider fixture.
Physical Android/iOS, Safari/WebKit, assistive-technology testing and a full
accessibility audit have **not** been completed.

Current engineering reports are under `startup_app/build/`. Historical core,
engine and anonymous-app reports remain attached to their unchanged binaries;
their suites were not rerun as new 2.0 tax validation. The final extracted-package
smoke report checks startup without rebuild, both manifests, preserved workflows,
optional native worker and new operations, then encrypted backup verification.

Current startup JAR SHA-256:

`e876ca96ac813bf1223f90969c8c4edd6c0e48f76e95dd97788a09e6e80c9da2`

Native worker SHA-256:

`30f22b417eaa2bcc5b5fae62b0bab0ef01c49e8d5c5c16b302cd4a8bb7a61844`

Official schema pin (148,921 bytes):

`6a0ef9f64065431879dcc724869d8455a640bcf11279d1818eb19f11448ce763`

The official schema is downloaded explicitly through the pinned source helper;
it and the reference PDF/browser/JDBC/MySQL/vendor runtimes are excluded from the
code archive. See [native source pins](startup_app/native_import/source_pins.json)
and the source links in the native/operations documentation.

## Repeat the checks

```sh
python3 startup_app/build.py
python3 startup_app/test_http.py
python3 startup_app/test_https.py
python3 startup_app/check_mutations.py
python3 startup_app/check_self_service_mutations.py
python3 startup_app/check_native_mutations.py
```

Optional operations harness: install `startup_app/requirements-test.txt` in a test
venv and run its `test_operations.py` and `check_operations_mutations.py`. The
server itself requires no Python commerce package. For native testing use the
separate pinned worker environment, source fetch and commands in NATIVE_IMPORT.md.
The MySQL harness runs storage/base HTTP/native (when configured)/operations and
isolated-restore suites in fresh disposable databases. It does not touch an
existing production database.

Authenticated browser harness (supply installed tool paths, never download an
unknown runtime automatically):

```sh
python3 startup_app/test_browser.py \
  --chromium /absolute/chrome-headless-shell \
  --playwright /absolute/node_modules/playwright/index.mjs \
  --node /absolute/node \
  --native-python /absolute/native-venv/bin/python \
  --native-schema-dir /absolute/pinned-native-sources \
  --output-dir /absolute/private-browser-results
```

## Gates that still prevent a production launch

| Gate | Current evidence | Required external/engineering completion |
| --- | --- | --- |
| Independent tax approval | **0/30**, original expected columns blank | Independent utility/qualified reviewer results tied to exact Act/year/rule manifest, approval and regression evidence |
| Statutory coverage | Narrow salary/ordinary-interest pack and preparation tools | Full ITR schedules, capital gains/business/property/foreign/losses and GST eligibility/returns, current legal/utility validation |
| Filing | Not connected or released | Authorized ITD/GST access/provider arrangements, current schemas, consent/e-verification, acknowledgements/retry/amendment/reconciliation and sandbox certification |
| Live billing | Test-only implementation, fake-provider tests | Provider account, reviewed live adapter/checkout/webhook operation, tax invoices, terms/refunds/support and production reconciliation |
| Real professionals | Test-signed preparation roster only | Genuine CA onboarding/status/revocation verification and service agreements; statutory certifications cannot be supplied by a software role |
| Production hosting/scale | Loopback app; bounded SQL pilot | Actual infrastructure/domain/TLS, normalized concurrent repositories/pooling, distributed sessions/rate limits, load/availability measurement and operation ownership |
| Recovery/security/privacy | Logical isolated restore tested; no promotion | Current deletion/revocation/control reconciliation, offsite/PITR/restore drills, secret custody/rotation, account recovery/MFA, retention/erasure, independent security review |
| Broad QA | Chrome desktop/mobile viewport smoke passed | Required browser/device/assistive-tech matrix and operating procedures |
| Market/growth | No real revenue/customer cohorts or comparative study | Paid cohort evidence, retention/support/referral/outcome measurement; no invented competitor comparison |

A paid plan, feedback item, invoice match, account role or signed **test** expert
can never unlock numerical tax release or filing. Software tests cannot approve
their own golden expected values. The proposed 0.001% expert-referral rate is an
unvalidated goal, not a measured capability or a guarantee. “Better than the top
100,000 CAs/ClearTax”, highest growth and highest profit have no benchmark or
market evidence here and are not asserted.

## Startup operating package

[INDIA_LAUNCH_PLAN.md](INDIA_LAUNCH_PLAN.md) contains the launch hypotheses and
pilot economics. Use actual customer feedback and operational counts to decide
what improves completion; test captures are excluded from live revenue. The new
in-product triage loop records a response and decision for feedback, without
silently changing tax rules. No public launch, real customer data ingestion,
external monitoring schedule, provider charge or government submission was made.
