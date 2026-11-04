# Account security — 5.1.0

Use a synthetic account. Open **Account security** after sign-in. No email/SMS,
paid authentication service or model is required. This implements authenticator
lifecycle and lost-authenticator recovery while the password is still known.
It is not forgotten-password recovery, identity proofing or certification.

## Authenticator and recovery flow

1. Enter the current password and start setup. Existing MFA also requires an
   unused current factor. A random 160-bit Base32 key is shown for manual entry
   into an authenticator: SHA-1, six digits, 30 seconds. Setup expires after ten
   minutes. Starting another setup replaces the pending key; it does not silently
   replace an active authenticator.
2. Separately enter the password and a valid code from the new authenticator.
   Only this confirmation enables/replaces MFA, consumes that code, invalidates
   older authentication epochs and displays ten recovery codes once.
3. Save the recovery codes separately. Each is a random 128-bit secret and may
   replace the authenticator once in combination with the account password.
   Codes are not included in case exports, public settings reads or audit rows.
   The encrypted user record stores account/domain-bound SHA-256 hashes only.
   This differs from fast hashing of a low-entropy structured identifier such as
   PAN; no PAN index or ownership verification is added by this feature.

MFA-enabled password sign-in returns an anonymous, cookie/CSRF-bound pending
challenge, not an authenticated user. The challenge expires after five minutes.
Completing a factor rotates cookie/CSRF again and then creates authenticated
access. An enrollment code is already consumed: immediate sign-in needs a fresh
time-step code or an unused recovery code. The server accepts only the adjacent
past/current/future time steps and strictly advances the stored consumed step.

The account's persisted proof-attempt window allows six failed password/factor
proofs before a ten-minute pause. The account-level factor pause survives process
restart and applies across application processes sharing the SQL database.
Successful proof clears failures. Password-login admission still has the earlier
process-local network/account limits; distributed password admission remains work.

## Settings and sessions

Changing the password, confirming a new authenticator, regenerating recovery
codes, disabling MFA or signing out other devices increments the account's
authentication epoch. The current browser receives a new session. Older sessions
and outstanding password/MFA challenges are rejected on other application
processes too. Recovery-code use and TOTP replay state update atomically.

Requests bind their server-authenticated account/epoch into the workspace. The
workspace checks that epoch again when reading the user within the business
transaction. Concurrent SQL validates that user-record dependency at commit.
An epoch change between initial authorization and commit forces a retry, which
then denies the revoked session before publishing the write. This does not undo
an external provider request that was already authorized and sent earlier.

Settings commands require an exact `expected_security_revision`, current password
and, where enabled, an unused factor. Stale settings cannot consume a factor or
apply a second change. Settings revisions describe configuration changes; login
factor usage and failure counters can change without incrementing that revision.
Pending setup can be canceled; replacing a device leaves the old authenticator
active until new proof succeeds. Regenerating codes invalidates the entire old
set. There is no automatic disabling or administrative HTTP reset shortcut.

When configured with an HTTPS public origin, ADMIN and PREPARER accounts must
enable MFA before business/admin access; only security setup and logout remain
available beforehand. Those HTTPS operator accounts cannot disable MFA. Customer
enrollment is optional. Local HTTP remains a synthetic loopback development mode.
Actual deployment, trusted TLS termination and operating controls are still needed.

## API and interrupted responses

| Route | Purpose |
| --- | --- |
| POST /api/login | Password proof; may return `mfa_required=true`, `user=null` |
| POST /api/login-factor | Pending challenge + CSRF; `{method, code}` only |
| POST /api/login-cancel | End challenge and rotate to a fresh anonymous session |
| GET /api/security | Current account's non-secret settings and remaining count |
| POST /api/security/update | Exact operation-specific password/factor proof |

Operations: START_ENROLLMENT, CONFIRM_ENROLLMENT, CANCEL_ENROLLMENT,
REGENERATE_CODES, CHANGE_PASSWORD, DISABLE_MFA, REVOKE_OTHER_SESSIONS.
Every settings command includes operation, expected_security_revision, password,
factor_method and factor_code. Confirmation adds setup_id; password change adds
new_password. A disabled-MFA operation uses empty factor fields except confirmation,
which always needs a TOTP from the pending new key. Actor, epoch, role, MFA-enabled,
approval and arbitrary extra fields are never accepted from clients.

Setup keys and plaintext recovery codes are not saved in command receipts or
returned on an accepted-request replay. This prevents a receipt/export from
becoming a secret-retrieval endpoint. On an uncertain security commit or network
response, the UI does not automatically replay the secret-bearing operation.
Sign in again and inspect current settings. A replaced setup needs a newly issued
key; lost recovery-code display needs a freshly authorized regeneration. A lost
password-change response requires checking sign-in with the intended new password.
Current business-edit/payment idempotency behavior remains separately implemented.

The MFA secret, used-step marker, pending key, recovery hashes, failed-attempt
state and epochs are fields of the existing encrypted user record; no new SQL
namespace/migration is required. There is no plaintext recovery-code record.
Normal reads and metadata audit omit keys, passwords and proof values. Runtime
sessions remain in process memory; this is shared revocation, not a distributed
session store or a 100,000-user capacity result.

Backups include encrypted account-security state. All restores still remain
isolated. Reconcile current credential epochs, compromised/replaced factors,
used codes, revocations and deletion controls before any future promotion. Old
backup state must not be treated as current authentication authority. There is
still no production restore-promotion command.

## Observed checks and limits

20 unit groups and 12 checks using six published RFC time vectors passed. The legacy JSON field
`published_rfc_vectors` counts those 12 format checks, not 12 distinct published
time points. The real API suite ran
25 groups on each backend: local has one intentional two-process-SQL-only skip;
legacy and concurrent MySQL passed all 25. Both SQL profiles tested two Java
application processes against the same database. Concurrent SQL also tested an
epoch change during an in-flight business write and a lost MFA-activation commit
acknowledgement. Ten compiled security defects were detected by assertions.

The full 41-flow browser suite passed on local and concurrent SQL at desktop and
390px mobile sizes. Wrong current-password feedback stays on the signed-in screen.
Setup, proof, one-time code display, password-only pending state, recovery login,
code regeneration, device revocation, password change and MFA removal ran through
the actual UI/API. HTTPS fixture checks enforced operator MFA and Secure cookies.
This is bounded engineering evidence, not an independent security audit.

No phishing-resistant passkeys, forgotten-password recovery, external recovery
notifications, identity proofing, HSM/KMS rollout, distributed password admission,
device inventory or independent security certification is supplied. Do not claim
NIST assurance-level conformance. MFA never grants tax correctness, verified CA
status or filing authority. Independent tax approvals remain 0/30.

Primary specifications consulted 3 November 2026:

- https://www.rfc-editor.org/rfc/rfc6238.html
- https://pages.nist.gov/800-63-4/sp800-63b/authenticators/
- https://pages.nist.gov/800-63-4/sp800-63b/events/
