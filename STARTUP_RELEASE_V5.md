# TaxDesk 5.0.0 — account-security release

3 November 2026. Both delivery ZIPs contain the full current source, runnable
startup JAR, unchanged original component JARs, tests, original golden workbook,
deployment examples and founder handoff. Their existing archive filenames retain
their identity; the application inside is **5.0.0**. Run with Java 17+ and Python 3:

```sh
python3 startup_app/run.py
```

The default downloaded application needs no ChatGPT Plus subscription, ChatGPT
API key or paid model. Use synthetic accounts and data. This release closes a
substantial account-security engineering gap; it does **not** complete the entire
production tax startup. Independent tax approvals are still **0/30**.

## Implemented in this release

| Area | Current behavior |
| --- | --- |
| Authenticator setup | Current password proof; existing MFA factor when applicable; random 160-bit setup key; ten-minute pending setup; separate new TOTP confirmation before activation |
| MFA sign-in | Password proof creates a five-minute anonymous challenge bound to its cookie/CSRF. A correct unused factor is required before authenticated access; cookie/CSRF rotate again |
| Replay protection | Six-digit SHA-1 TOTP with 30-second steps and adjacent-step tolerance; persisted consumed-step marker. Recovery-code consumption is atomic across app processes |
| Recovery codes | Ten random 128-bit codes, displayed once. Only account/domain-bound hashes persist inside the encrypted user record. A code plus the password can recover access after losing the authenticator |
| Protected changes | Current password and applicable unused factor protect password change, authenticator replacement, code regeneration, MFA disable and device-session revocation. Stale settings revisions are rejected |
| Session revocation | Credential changes increment the shared account epoch. Older sessions and pending challenges are denied by other application processes using the same SQL store |
| Transaction authorization | The server binds account/epoch to the request; workspace user access checks it within business transactions. Concurrent SQL validates that user-record read at commit, blocking a write revoked during the transaction |
| Failed proofs | Persisted account proof-attempt counters; six failures cause a ten-minute pause, including across process restart. Success clears failures |
| HTTPS operators | ADMIN/PREPARER accounts must enroll MFA before business/admin access when configured with an HTTPS public origin. Security setup/logout remain accessible; disabling operator MFA is denied |
| UI and uncertainty | Account security and verification screens, one-time recovery display and safe password handling. Incorrect current-password feedback keeps the signed-in screen. Interrupted secret-bearing security operations are not automatically replayed |

Existing saved-case, income confirmation, profile/checklist, normalized source
comparison, optional pinned native ITR-1 structural intake, GST observations,
capital FIFO working, TDS review, notice organizer, scoped preparation sharing,
TEST commerce/bundles, TEST expert requests, feedback and support remain present.
There is no general chatbot. Payment, role, MFA or preparation completion cannot
grant tax approval or filing authority.

## Important boundaries

Lost-authenticator recovery still requires the password. Forgotten-password
recovery, external security notifications, identity proofing, passkeys and a
production KMS/HSM rollout are not supplied. TOTP is not phishing-resistant; no
NIST assurance-level or independent security certification is claimed. Password
login admission and session storage retain process-local limits. Shared epoch
revocation is not a distributed session inventory or 100,000-user scale proof.

All security state is within the existing encrypted user record; no new SQL
schema/namespace migration is needed. Settings GET, case exports and audit omit
passwords, keys and proof values. Security changes do not write plaintext recovery
codes or password-bearing body fingerprints into idempotency receipts. Exact
settings revision checks prevent an old command from issuing another secret set.
If confirmation committed but its reply was lost, reauthenticate and authorize a
fresh recovery-code regeneration; an old response cannot be retrieved as a secret
export. See startup_app/ACCOUNT_SECURITY.md for API contracts and procedures.

An external provider call already authorized and sent before revocation cannot
be undone by revoking the session. Provider reconciliation still follows the
existing order/binding/idempotency protocol. No automatic payment-create retry
was introduced.

All restores remain isolated. Before any future promotion, current credential
epochs, replaced/compromised factors and used-code/revocation/deletion state must
be reconciled. No production restore-promotion command exists. The optional
concurrent SQL profile still has bounded records/application resources and
quota/scan contention; legacy mysql remains serial/16 MiB. Default local storage
is an 8 MiB single-writer vault. SQL/public hosting must be explicitly provisioned.

## Observed verification against 5.0.0

These are engineering results, not independent statutory tax approvals.

| Check | Observed result |
| --- | --- |
| Security unit checks | 20 named groups; 12 RFC6238 checks: six published eight-digit SHA-1 time vectors and their six-digit counterparts, including post-2038 times |
| Security real API | Local: 25 run, 24 pass, one intentional two-process-SQL-only skip. Legacy MySQL: 25/25. Concurrent MySQL: 25/25 |
| Two application processes | Both SQL profiles tested shared revocation and stale MFA challenges against two actual Java server processes |
| Concurrent transaction race | A case edit paused after authorization; another store revoked the epoch; the pending edit was denied and case/revision stayed unchanged |
| Uncertain SQL commit | Actual MFA activation committed, simulated acknowledgement loss raised an unknown outcome, and old confirmation retry could not reissue plaintext recovery codes |
| Security compiled defects | 10/10 compiled behavior defects detected; unchanged source verified |
| Existing core/unit regression | Workspace 27 + 12 generated isolation checks; self-service 27 + 60 generated checks; workbench 26 + 59 conservation cases; recovery 9; native policy 18; framed backup 8 |
| Existing authenticated API | Local 49/49; each SQL profile 49 run with one intentional local-vault-only skip |
| Workbench API | 24/24 on each of local, legacy SQL and concurrent SQL |
| Native worker API | 20/20 on each profile using the actual Java/Python worker and pinned schema |
| Commerce/expert/feedback API | 36/36 on each profile; loopback payment fixture and signed TEST credentials only |
| HTTPS | 13/13 client/proxy/backend fixture groups, including operator MFA and Secure-cookie enforcement; not a public deployment test |
| Actual MySQL storage | Legacy 20 groups; concurrent 28, including the two new security race/unknown-commit checks; four distinct-writer rounds each |
| SQL recovery | Crash/kill/restart preserved saved data/committed marker; outage gave 503 and no local fallback. Legacy isolated restore 9 groups; concurrent framed restore 10 with full snapshot equality and archive above 16 MiB |
| Compiled workbench regression defects | 8/8 detected against the current source |
| Browser | 41 real UI/API flows on local and concurrent SQL; Chrome 156.0.8078.80 / Playwright 1.62.1; desktop 1440×1000 and mobile 390×844 |
| Browser expected errors | One rejected current-password proof and one post-logout cases request, both HTTP 401; no JS errors or other observed API errors |

Security browser flows cover wrong-password handling, pending setup, new-code
proof, one-time code display/hide, password-only pending state, recovery sign-in,
code regeneration, device revocation, password change/new-password sign-in and
MFA disable. Desktop/mobile security screenshots were inspected after keys/codes
were hidden. They are internal QA and not bundled. There was no physical-device,
Safari/WebKit, full accessibility, independent penetration or capacity audit.

The 10 security defects cover TOTP reuse, unconsumed recovery codes, plaintext
code storage, ignored request epochs, ignored challenge epochs, missing current
password checks, activation before proof, password-only service MFA bypass,
secret leakage and missing persisted failure counters. Compilation failures do
not count as detections. Current source/report hashes are checked in the delivery.

Current reports are under startup_app/build. Byte-identical V4 report copies are
under startup_app/evidence/v4; earlier history retains its original bindings.
The original three component modules and both golden files remain byte-identical.
The original expected tax values stay blank. No law approval, real CA status,
model benchmark or paid business outcome was fabricated.

The concurrent framed restore matrix runs on its explicit storage fixture before
the HTTP/browser matrix; it includes the new committed MFA fixture/epoch state.
It is not a restore-promotion drill against current external revocation controls.
Legacy restore also checks its populated HTTP fixture. See DELIVERY_VERIFICATION.md
for the separate extracted-package run and encrypted-backup check.

## Reproduce

```sh
python3 startup_app/build.py
python3 startup_app/test_security.py
python3 startup_app/check_security_mutations.py
python3 startup_app/test_https.py
python3 startup_app/tools/release_preflight.py --mode engineering
python3 startup_app/tools/release_preflight.py --mode production
```

Stop the local server before build/operator/backup operations on its vault.
Optional SQL/native/browser prerequisites and flags are in the corresponding
module documents and test scripts. Engineering preflight checks current
artifact/source/report bindings; production mode intentionally exits 2 with the
unmet gates. It cannot approve rules or activate filing/live services.

Startup JAR: `startup_app/build/tax-platform-startup-5.0.0.jar`.
SHA-256: `1548c778417a554434b1ea467c6cea162787f398e75ca2d7742838391e841feb`.
No runtime databases, credentials, signing keys, browser binaries, schema caches,
private helper scripts or model weights are included in either ZIP.

## Remaining production work

Independent expectations/qualified review and authoritative tax-rule publication
are still needed. Full statutory ITR/GST/capital-gains/TDS schedules,
classification, eligibility, settlement, amendments and adjustment logic remain
engineering work. Working-paper arithmetic or document schema validation does
not implement that scope.

Authorized live filing/e-verification/acknowledgement adapters, live commercial
payments/invoices/refunds, verified real CA onboarding/status/service workflows,
actual hosting and production security/monitoring/rollback, forgotten-password
recovery/notifications, normalized query repositories/pooling, distributed
admission/session controls, measured capacity and restore-promotion reconciliation
are also unfinished. OWNER_HANDOFF.md separates engineering work from
owner-supplied access, independent evidence and operating responsibilities.

This is a larger tested synthetic platform. It is not a production-complete
startup, a verified CA replacement, or evidence for 0.001% expert involvement,
competitor superiority, highest growth or highest profit.
