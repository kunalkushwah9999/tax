# TaxDesk 5.1.0 — customer erasure and navigation reliability

**Synthetic preparation application; public paid production launch remains blocked.**
Independent tax approvals: **0/30**. FY 2025–26 / AY 2026–27 preparation scope stays
unchanged. Full statutory ITR/GST/capital-gains/TDS and authorized live services
are not supplied by this release.

The current runnable JAR is `startup_app/build/tax-platform-startup-5.1.0.jar`.
SHA-256: `2149851f938c3f3c643208a6403b2d7fa93b2769421642f9490f5c00463de8cf`.
Original result-safety, tax-engine and local-demo components, and the original
30-case golden corpus/workbook, remain byte for byte unchanged. Expected tax
outputs remain blank. Old V5 reports/JAR are historical evidence under
`startup_app/evidence/v5`; they are not current-runtime approval.

## Delivered change

Eligible customers can remove active application data through Account deletion.
The flow requires fresh password/MFA, exact confirmation and a saved secret status
key. Credentials, owned drafts and their related sharing/support/feedback/receipts
are removed atomically. Status recovery handles a lost commit acknowledgement
without replaying the destructive action. Two SQL application processes reject
older sessions after removal. A minimal pseudonymous control remains for restore
and privacy review; external/backup erasure and production privacy approval stay
false. Operator accounts and customers with TEST orders require reviewed handling.
See `startup_app/ACCOUNT_PRIVACY.md` for the exact retained fields and limitations.

The public `/account-deletion` page provides browser instructions and private
receipt lookup without Android installation. Generated deployment pages include
the configured owner support mailbox; monitoring/identity verification is an owner
and engineering responsibility, not an automatically connected service.

Navigation now disables forms until refreshed data renders. The browser matrix
exposed a same-page security refresh race that could discard a typed password
when old controls remained clickable. A controlled delayed-response browser check
verifies that the old form stays disabled through refresh completion.

## Checks on the current artifact

| Check | Result |
| --- | --- |
| New privacy Java units | 15 groups passed, including failure rollback and unknown-commit receipt recovery |
| New privacy HTTP — local | 13 run: 12 passed, one expected shared-SQL/two-node skip |
| New privacy HTTP — legacy MySQL | 13/13 passed, two actual Java application processes |
| New privacy HTTP — concurrent MySQL | 13/13 passed, two actual Java application processes |
| Existing browser regression | 41 flows passed on actual concurrent MySQL; zero JS errors |
| Generated web/TLS focused browser | 14 checks passed, zero JS errors; 390px UI, delayed navigation, deletion and public receipt lookup |
| Release generator/worker contracts | 8 groups passed |
| Existing backend/unit matrices | Rebuilt/passed: workspace 27 + 12 generated; self-service 27 + 60 generated; workbench 26 + 59 generated; security 20; native policy 18; recovery 9; framed backup 8 |
| Existing HTTP regressions | Base 49 on each profile (SQL profiles each have one expected local-vault skip); workbench 24, operations 36, native worker 20 on each profile; security 25 (local has one expected two-node skip); HTTPS fixture 13 |
| Existing SQL/recovery checks | Actual MySQL 8.4: legacy 20 / concurrent 28 named groups, writer rounds and outage/restart; isolated restore legacy 9 / concurrent framed 10 |
| Existing compiled mutation checks | Security 10 and workbench 8 detected; current unchanged backend source bindings verified |
| Offline delivery helper | 14 groups; clean-extraction verification/doctor/start/release generation checked without rebuilding |

Full details and exact bindings: `privacy_release_checks.json`, current
`startup_app/build/*results.json`, `deployment_release_checks.json`. The browser
uses Chromium 156.0.8078.80. The focused HTTPS static proxy is a local test fixture;
Nginx itself, physical Android devices and public cloud infrastructure were not
executed. SQL restores remain isolated and do not prove current erasure-control
reconciliation. Internal test groups are not independent tax expectations.

## Run / deployment

Extract the ZIP; install Java 17+ and Python 3.10+:

```sh
python3 manage.py doctor
python3 manage.py start
```

Open `http://127.0.0.1:8788` and use synthetic accounts/data. No ChatGPT subscription
or paid inference service is needed for this local application. MySQL and optional
native-worker prerequisites remain explicit. Read `release/README.md`,
`release/CLOUD_DEPLOYMENT.md` and `release/ANDROID_RELEASE.md` before generating
owner-origin web/Android/VM files. Android source targets API 36; no compiled/signed
AAB/APK or Play publication is supplied. No hosting account or deployment was used.

## Remaining production work

Implement/independently validate full statutory scope and current rule/year
release; authorized live filing/e-verification/reconciliation; live payments,
invoicing/refunds and real CA services; reviewed operator/payment-account and
external/backup erasure/retention; forgotten-password identity recovery and
notifications; actual monitored hosting/security/offsite recovery/scale; Android
build/signing/device testing and applicable Play review. Existing legacy SQL has
serial writes/16 MiB; concurrent SQL remains bounded and is not proof of 100,000
concurrent users. Credentials alone cannot implement these gaps. Production
preflight intentionally exits 2. A 0.001% CA rate, competitor superiority and
highest growth/profit have no measured evidence.
