# Founder handoff — runnable code and remaining ownership

> Current application: **5.1.0**. See STARTUP_RELEASE_V5_1.md and
> startup_app/ACCOUNT_PRIVACY.md. Eligible customer active-store deletion is now
> implemented/tested; operator/payment-account and external/backup removal,
> retention, identity recovery and production privacy approval remain pending.
> Independent tax approvals: **0/30**. Android/cloud publication remains pending.




For the final offline commands and completion matrix, read TAXDESK_FINAL_HANDOFF.md.
`python manage.py doctor` checks the delivery/runtime; `python manage.py start`
launches the existing synthetic app after integrity checks.

Download and keep the ZIP locally. Running the default Java/Python application
does not use a ChatGPT API key or a ChatGPT Plus subscription. Optional local
native checks need the pinned Python dependencies; MySQL needs its own explicitly
provisioned server/driver. No paid AI model or inference service is required.

Start with Java 17+ and Python 3, from the extracted top-level folder:

```sh
python3 startup_app/run.py
```

Open `http://127.0.0.1:8788`, register a **sample** customer and use the preparation
workflows. Before starting, optional ADMIN/PREPARER accounts are created through
`python3 startup_app/run.py --create-operator ADMIN` / `PREPARER`. No passwords or
private signing keys are seeded. STOP the local server before operator setup on
the same local vault. The application, source, tests, build and deployment examples
are delivered together. See START_HERE.md and STARTUP_RELEASE_V5.md.

## Honest completion boundary

This is an executable **synthetic preparation platform**, with test commerce,
professional-request consent, feedback, account MFA/revocation, concurrent SQL/recovery and private capital/TDS/notice working papers.
It is not a production-complete autonomous CA replacement. There are both
**remaining software scope** and **external approvals/access**; credentials alone
will not implement missing tax schedules or establish production scale.

| Area | Owner must provide | Engineering/review still required before release |
| --- | --- | --- |
| Tax correctness | Independent qualified review/official utility evidence for the 30 inputs and rule manifest | Approved stage expectations, current Act/year applicability, authoritative release/control/signing integration; the app still blocks numerical release |
| Broader ITR/GST/capital gains | Defined forms, years, taxpayer scope, source samples and reviewed acceptance corpus | Statutory ITR schedules, capital-gains classification/adjustments/exemptions, TDS credit allocation, GST outward/inward/settlement/amendment logic and full validation; current working papers/CSV matches do not approve tax/ITC |
| Government filing | Authorized ITD/GST/provider account/access and required user consent/e-verification arrangements | Live adapters, sandbox certification, acknowledgements/retries/amendments/reconciliation; no browser credential/OTP bypass |
| Payments | Provider onboarding and reviewed commercial terms/invoicing/refunds/support | Live commercial adapter/checkout/webhook review; current mode is explicitly TEST only |
| Professionals | Real CA onboarding/status/revocation checks and service agreements | Live trusted credential adapter and reviewed service workflow; signed TEST attestations are not ICAI verification |
| Hosting/operations | Infrastructure/domain/TLS, operating ownership and budgets | Actual deployment, monitoring, rollback, secrets/rotation, forgotten-password/identity recovery, external security notifications, phishing-resistant authenticators, normalized/query-filtered repositories/pooling and distributed session/admission controls |
| Recovery/privacy | Retention/deletion requirements and offsite custody | Current control/deletion/revocation and credential-epoch/used-code/factor reconciliation before restore promotion, PITR/offsite drills, Oracle sync/retention implementation and independent security review |
| Product/growth | Real consenting customers and outcome/retention/support data | Measure completion, error/referral/support rates and paid conversion; test captures are not revenue and do not prove superiority or profit |

The 0.001% expert-referral goal has no measured evidence. No plan, role, JSON file,
feedback action or storage setting approves tax or filing. The original 30 golden
expected outputs remain blank. Do not replace missing values with the engine's
own results or count internal tests as independent tax approval.

## Verify the delivered engineering evidence

```sh
python3 startup_app/tools/release_preflight.py --mode engineering
python3 startup_app/tools/release_preflight.py --mode production
```

Engineering mode checks the bundled current artifact/source/report bindings. It
does not repeat the whole test matrix or contact a provider. Production mode
**intentionally exits 2** with the unmet gates. It cannot publish rules, turn on
real payments/filing or certify infrastructure. Exact test commands and optional
prerequisites are in the release report and module documentation.

The new `mysql-concurrent` profile removes the old transaction-wide lock and
aggregate 16 MiB check. It still has short shard/quota/scan publication locks,
bounded application/session/worker limits and namespace scans. Streaming framed
backups were tested above 16 MiB; they retain 1 GiB / 100,000-record format limits.
This does not establish 100,000 concurrent users. Use a fresh database/key and
follow startup_app/CONCURRENT_STORAGE.md. Legacy `mysql` remains available with
its original limits. All restored data remains isolated; no promotion is shipped.


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
