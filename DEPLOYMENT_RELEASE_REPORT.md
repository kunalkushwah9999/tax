# TaxDesk deployment status — application 5.1.0

**Synthetic pilot; paid production launch remains blocked.** Full current source
is in the updated ZIP. Runtime is tax-platform-startup-5.1.0.jar, SHA-256
`2149851f938c3f3c643208a6403b2d7fa93b2769421642f9490f5c00463de8cf`.

The owner-origin generator ships an installable web layer, Android Trusted Web
Activity source (target/compile API 36), Digital Asset Links configuration and
Nginx/systemd files with a Google Compute Engine VM runbook. The current layer
also serves the account-deletion page and a configured owner support mailbox.
Only four public offline resources enter Service Worker Cache Storage; private
API/document responses and mutations are not cached or replayed.

Release generator/worker tests: 8 groups passed. The focused real-Java/disposable
TLS-proxy browser suite: 14 checks passed, zero JS errors, including authenticated
390px layout, navigation locking, active customer deletion and public secret
receipt lookup. The full 41-flow browser regression also passed on concurrent SQL.
Exact current application evidence: STARTUP_RELEASE_V5_1.md and
privacy_release_checks.json. Prior 5.0.0 deployment evidence is historical.

Nginx itself, real cloud infrastructure and Android SDK/device execution were not
available. No compiled/signed AAB/APK, Play upload, public deployment or live
provider certification is supplied. Account deletion now removes eligible
customers' active records; operator/TEST-payment accounts and external/backup
copies still need reviewed implementation/fulfillment. Privacy approval is false.

Before paid launch: complete/independently approve statutory ITR/GST/capital-gains
and TDS; authorized filing; live commercial payments and real CA services;
privacy/retention/recovery; actual monitored hosting/scale/offsite restore/control
reconciliation; Android build/signing/device QA and Play review. Tax approvals
remain 0/30. Credentials alone do not implement this scope. See OWNER_HANDOFF.md,
release/ANDROID_RELEASE.md and release/CLOUD_DEPLOYMENT.md for instructions.


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
