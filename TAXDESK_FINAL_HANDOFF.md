# TaxDesk application handoff — 5.1.0

The ZIP includes the runnable Java/web application, complete current source,
original tax components and golden workbook, tests, installable web layer,
Android source and cloud VM configuration. **It is a synthetic preparation pilot,
not a production-complete paid tax/CA platform. Independent approvals: 0/30.**

## Run locally

Extract the ZIP. Install Java 17+ and Python 3.10+, then run:

```sh
python3 manage.py doctor
python3 manage.py start
```

Open `http://127.0.0.1:8788`. Use `python` instead of `python3` where appropriate.
No ChatGPT subscription, API key or paid AI model is needed for the local app.
Hosting/provider/professional operating costs are separate. Keep a local ZIP copy.

## Current release

5.1.0 adds fresh-proof customer active-store erasure, a public deletion/status
page, lost-response receipt recovery and a navigation refresh fix. Privacy has
15 unit groups and 13 HTTP groups on each storage profile (local: one expected
two-node skip; both SQL: all pass). Current browser regressions pass 41 flows;
focused generated web checks pass 14, both with zero JS errors.

Customers with TEST payment records and operator accounts still require reviewed
removal. External copies/backups are not confirmed erased. A pseudonymous control
is retained for privacy/restore review. This does not complete production privacy
compliance. Exact scope: `startup_app/ACCOUNT_PRIVACY.md` and `STARTUP_RELEASE_V5_1.md`.

## Still unfinished before paid public launch

- Independent statutory expectations remain 0/30; full ITR/GST/capital-gains/TDS,
  reviewed rules, authorized live filing and commercial payments/CA services.
- Operator/payment-account and external/backup deletion, retention/control expiry,
  staffed support/identity recovery, external security notifications and review.
- Actual cloud/Nginx/TLS deployment, monitoring, offsite recovery/control
  reconciliation, production security and measured scale.
- Android SDK build/lint/signing, device testing and Play account/policy review.
  Only Android source is supplied; no signed AAB/APK or publication is claimed.

The original components and golden inputs/blank expectations are unchanged. Working
papers do not determine taxable capital gains/TDS claims/ITC, and native structural
checks do not approve eligibility, source authenticity or filing. A 0.001% CA rate,
competitor superiority and exceptional business growth/profit remain unproven.

Read `OWNER_HANDOFF.md`, `STARTUP_RELEASE_V5_1.md` and `release/README.md` for the
remaining implementation/ownership work. Run the engineering/production preflight
through `startup_app/tools/release_preflight.py`; production intentionally fails.
File hashes detect changed listed bytes, not publisher identity or tax accuracy.
The default app remains usable offline from the downloaded code with synthetic
data; its installable web layer intentionally caches only generic public offline
resources and does not make private account data available offline.


## Release commands outside ChatGPT

See `RELEASE_OPERATIONS.md`. New `release/verify_site.py` checks the deployed
HTTPS public contract without real customer/payment/filing actions;
`release/android_build.py` checks SDK/Gradle prerequisites and runs an unsigned
lint/bundle build on the owner's machine. The tools passed 13 local TLS/Java/CLI
contract groups. No actual public deployment, SDK build, signing or external
approval was executed. Application runtime stays 5.1.0 and approvals stay 0/30.
The downloaded application/tools do not require a ChatGPT subscription to run.


## Listed-equity review arithmetic (standalone, unapproved)

See `STATUTORY_REVIEW_RELEASE.md` and `statutory_review/README.md`. The new
Python review module implements bounded matched-lot equity workings, with 20
engineering groups, 200 generated checks and 10 injected defects detected.
It is not connected to the browser application's result/filing flow. Original
approvals remain 0/30; the separate equity review cases are 0/12 with blank
independent expectations. Main application source/JAR stays 5.1.0 unchanged.
