# Customer active-store erasure — 5.1.0

This implements **active application-store removal for eligible customer accounts**.
It is not certification of statutory, Play, processor or backup deletion compliance.
Synthetic data only. Operator accounts and customers with any TEST order require
reviewed ownership/payment/retention handling; their data is not silently erased.

## User flow

Open **Account deletion** in the web workspace. Review the scope, export needed
preparation data, save the random status key, enter `DELETE MY ACCOUNT` and prove
the current password plus an unused authenticator/recovery factor when MFA is
enabled. The security revision must still match. The public `/account-deletion`
page works without installing Android and can check the status key after sign-out.
The generated deployment page additionally exposes the owner's configured support
mailbox for reviewed/lost-access requests. Mailbox operation and identity recovery
are not connected or verified by generating that page.

The browser creates a 256-bit random status key **before** sending the destructive
request. Keep it private. The server stores only a domain-separated SHA-256 digest.
After an interrupted response, bootstrap a session and check the key through
`POST /api/account-deletion/status`. Do not automatically repeat deletion or use a
new key. A missing receipt does not prove failure while a transaction is settling.
No key, password, factor or account identity is included in the status URL.

## Atomic boundary

Fresh password/factor proof and the entire mutation share one DataStore transaction.
Wrong proofs commit existing security throttling; they do not remove business data.
The successful transaction removes the customer's credential/security record,
owned cases (including embedded GST/capital/TDS/native working data), tickets,
quotes, related grants/operations, owner and cross-actor resource receipts and
associated audit rows. Other customer records remain intact. Storage failure
before commit rolls the cascade back. An uncertain acknowledgement maps to
`ERASURE_OUTCOME_UNKNOWN_CHECK_RECEIPT`, with no automatic retry instruction.

Local sessions/challenges and local native previews are discarded after the
acknowledged commit. Other application processes reject old sessions because the
shared user row is gone. Existing SQL point/scan dependencies prevent stale
in-flight writes from committing against a removed authenticated principal.
Other nodes' ephemeral previews/in-flight processing can remain in memory until
their normal bound/expiry; their private endpoints require the now-invalid account.
This is not immediate distributed memory wiping.

## Retained control and unfulfilled scope

One encrypted `ACCOUNT_ERASURE_CONTROL` operation retains a domain-separated hash
of the random account UUID, the receipt digest as its record key, completion time,
counts and `restore_promotion_allowed:false`. It contains no username, display
name, password hash, MFA material, raw receipt key, original UUID or document text.
This is a **pseudonymous control**, not a claim that every remaining byte is anonymous.
The receipt reports `ACTIVE_DATA_REMOVED` and explicitly reports external/backup
erasure as unconfirmed. No API or checkbox can change this into production approval.

The owner still needs a reviewed retention period/expiry process for these controls,
processor fulfillment, offsite backup handling, a staffed external support/identity
flow, operator/shared-content removal and payment record retention/deletion logic.
Downloaded exports, screenshots and browser memory are outside the primary-store
cascade. The existing restore remains **isolated**; no automatic promotion or
reconciliation worker is introduced. Before any future restore promotion, compare
restored account UUID hashes against current deletion controls and reconcile
current grants, security epochs/factors and used recovery codes. Restoring an older
snapshot must never be treated as authorization to resurrect an erased account.

## HTTP surface

| Route | Protection / result |
| --- | --- |
| `GET /account-deletion` | Public instructions and secret receipt form; no account lookup |
| `GET /api/account-deletion` | Authenticated owner inventory and eligibility |
| `POST /api/account-deletion/erase` | Same-origin CSRF, current session, exact confirmation, fresh password/MFA and security revision |
| `POST /api/account-deletion/status` | Same-origin CSRF, secret 32-byte key and bounded network admission; redacted result only |

Receipt admission shares the existing process-local limits and is not a distributed
production rate limiter. TEST payment barriers and operator review are deliberate
unfinished paths, not successful deletion. Production privacy approval stays false.

Primary policy reference checked 2026-11-03:
https://support.google.com/googleplay/android-developer/answer/13327111

## Validation

15 Java unit groups cover cascade isolation, fresh proof/MFA/throttle, stale versions,
token format/collision/secrecy, rollback and lost-acknowledgement receipt recovery.
13 actual HTTP groups run on each profile: local has one expected two-node skip;
both MySQL profiles exercise all 13, including two separate Java processes. The
focused generated-web browser suite covers the mobile deletion and external status
flow; the current 41-flow SQL browser regression also passes. Exact artifact/source
bindings and remaining release gates are in `STARTUP_RELEASE_V5_1.md` at project root.
