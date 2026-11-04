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

# India launch and growth plan — working draft

1 November 2026. This is an experiment plan for Kunal, not evidence of demand,
paid conversion, tax correctness or a growth forecast. TaxDesk is a working
product name; no company, trademark, domain or service registration was created.
The code is development/pilot release 1.3.0 with tested optional MySQL and local HTTPS proxy profiles. It is not cleared for real taxpayers. See native_itr_preflight_v1_3.md for current evidence and deliberate limits.

## First product to validate

Start with an English workflow for salary and ordinary separated-interest
preparation, plus consent-based coordination with preparation teams. The current
arithmetic pack is specifically FY 2025–26 / AY 2026–27, adult throughout FY,
individual ROR. It cannot be relabelled as another tax year or a complete filing
product. Do not market GST, capital gains, notices, full ITR filing, settlement,
refunds, guaranteed savings or verified CA services as released features.

The proposed advantage is practical: clearer missing-information requests,
fewer duplicate entries, visible source differences, explicit confirmation,
less repeated client follow-up and a traceable draft handoff. These are hypotheses
to measure. Do not claim superiority over ClearTax or a percentage of cases that
never need a professional. A paid plan must not suppress referrals or uncertainty.

## Founder pilot sequence

| Stage | Concrete work | Exit evidence |
|---|---|---|
| Synthetic walkthrough | Founder demonstrates customer, PREPARER and ADMIN roles using the supplied synthetic flow | All role flows understood; observed failures logged and fixed |
| Discovery | Proposed first sample: interview 15 salaried taxpayers and 3 small preparation/CA firms without collecting their tax files | Document their present workflow, repeated errors, willingness to try and reasons to decline |
| Independent reference review | Give qualified reviewers the unchanged golden inputs and separate blank stage template | Independently produced expectations/evidence; unresolved cases and rule approval recorded |
| Production engineering | Build and test the real SQL/authentication/control/hosting and provider adapters | Required release matrix below has evidence and accountable owners |
| Limited authorized pilot | Recruit a small, consented cohort only after the affected data and tax workflows are released | Measured completion, corrections, support effort and retention; no guessed market metrics |
| Paid experiment | Founder chooses transparent service/plan and tests willingness to pay after live checkout/invoicing is implemented | Actual paid conversion, refunds, support costs and unit economics recorded |
| Expansion | Add another tax period/income pack or workflow only after independent cases and operational evidence | Approved scope map, benchmark and rollback for each new pack |

The interview counts above are proposed samples, not completed interviews or a
statistical market study. No customer was contacted and no paid order was placed.
No growth rate, acquisition channel return or price recommendation is established.

## What to measure

Use cohort-level aggregates with explicit denominators. No personal amounts,
PAN, source CSV, account identifiers or support bodies belong in analytics.

| Metric | Definition for the pilot |
|---|---|
| Draft activation | Users who create a draft and confirm the required income inputs / consented users who start |
| Preparation completion | Drafts reaching the explicit supported preparation checkpoint / drafts started |
| Correction rate | Drafts requiring correction after independent review / independently reviewed drafts |
| Time saved | Within-participant measured preparation/review time versus their declared previous workflow; report the sample and method |
| Repeat usage | Activated users returning in the declared observation window / activated users with a full window |
| Support burden | Time and request count per completed supported case |
| Scope coverage | Supported, unsupported, incomplete, referred and abandoned cases, all kept in the denominator |
| Paid conversion | Actual paid orders / eligible, consenting users shown a valid offer; measure only after payment release |

None of these metrics is measured by the delivered tests. The current admin
counts and audit are operational development tools, not a growth analytics system.

## Pricing and professional value

The app implements future price versions and immutable **planning quotes**.
There is no default price, hidden charge or live checkout. Founder-selected
prices must state service scope, applicable tax/invoice handling, renewal,
cancellation/refund and actual support responsibilities before real collection.
Get suitable commercial/legal review; the app does not generate those obligations.

Professional value should come from less repetitive preparation work, clear
source classification, consent, review context and version history. Statutory
sign-off, verified credential identity, taxpayer/provider authorization and
professional responsibility are separate capabilities. PREPARER in this app is
an unverified local role and must not be advertised as a verified CA.

## Production release matrix

| Gate | Delivered state | Required launch evidence |
|---|---|---|
| Independent numerical expectations | 30 inputs, 0 independently approved outputs | Qualified independent values, evidence, input hashes, stage matches and reviewed rule release |
| Real identity and account recovery | Local aliases/passwords only | Production enrollment/recovery/MFA/abuse controls and user testing |
| Durable operational backend | Local profile plus SQL encrypted-record pilot; atomicity/concurrency/crash checks and dedicated-key logical backup/fresh-MySQL isolated restore exercised; isolated target refuses app startup | Final normalized/query-filtered Spring repositories, scoped concurrency, pooling, durable sessions, reconciled/authorized restore promotion, physical/offsite/PITR recovery, key rotation and operating procedures |
| Deployment | Loopback application; strict HTTPS-origin mode and Secure cookies tested with a local TLS proxy fixture; Nginx/systemd templates supplied | Actual proxy/config/certificate/domain/hosting validation, monitoring, incident and rollback ownership; no public deployment completed |
| Privacy and retention | Local bounds and owner case deletion; no compliance certificate | Reviewed notices/terms, purpose/data inventory, retention/holds, access/export/deletion including backups |
| Professional services | Preparation sharing/comments only | Appropriate credential/grant/consent and sign-off adapters with independent validation |
| Source documents | Optional pinned official ITR-1 structural intake with sanitized, separately reviewed estimated proposals; existing normalized income/GST observations; native GST unapproved/unavailable | Remaining official form/year/business-validation/source-authenticity packs, protected native GST/document adapters, reviewed extraction/coverage, independent tax and adversarial cases |
| Payments and invoicing | Planning quotes only | Authorized provider, independently verified webhooks, idempotent entitlement/refund handling and appropriate invoices |
| Filing and settlement | Unavailable | Authorized provider, current schemas, taxpayer confirmation and acknowledgements; validated credits/fees/interest separately |
| UX and security | Unit/local HTTP/MySQL/HTTPS/isolated-restore/selected-defect tests; browser download failed and no new UI pass claimed | Authenticated desktop/mobile/keyboard/accessibility testing and independent security remediation/retest |
| Scale | Four local request workers, no benchmark | Declared workload, real infrastructure/DB load tests, p95/p99, saturation and recovery evidence |
| Current-year and broader schedules | Not covered by this starter | Separate legal/scope packs and approved independent cases |

No admin button, premium entitlement, signature fixture or passing engineering
test is allowed to turn a pending gate into an approved gate. Keep this matrix
with actual evidence and dates as the startup moves toward launch.
