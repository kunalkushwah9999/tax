> 3.0 hardening: an unresolved CREATE_PENDING/OUTCOME_UNKNOWN order now blocks a new request ID for the same owner/plan. Exact replay still returns the recorded order without a second POST. Uncertain provider-create responses carry retry_same_request=true, so the existing UI retains its exact retry action. Reconcile the existing order before another purchase.

# Commerce, expert requests and feedback — 2.0.0

This is executable **synthetic/test-mode** functionality. No live charge, real CA
verification, statutory sign-off or government filing has been performed or
released. The UI is English. Basic drafts, consent controls and the owner's
ordinary data export do not require a paid entitlement.

## Test billing

An ADMIN publishes an immutable **future** plan version in Administration. Plans
are `CUSTOMER_PREPARATION` and `FIRM_REVIEW`. The price is a positive integer in
paise, INR only; fixed duration 1–366 days, up to 1,000 personal review credits.
The latest effective version must be explicitly accepted with its terms version.
There is no automatic renewal, recurring debit or live subscription mandate.
Firm access uses the validity period for a consented queue; its personal bundle
credit counter is not consumed by queue reads. Do not advertise unimplemented
firm entitlements or a statutory/tax service as included in a plan.

The default catalog is empty and payments are disabled. No default real selling
price or paid tax claim is invented. Create a **test** Razorpay account/key through
the provider's own onboarding, then write a private operator file outside the
source package (POSIX mode 0600):

```json
{
  "mode": "TEST",
  "api_base": "https://api.razorpay.com/v1/",
  "key_id": "rzp_test_REPLACE",
  "key_secret": "REPLACE_WITH_YOUR_TEST_SECRET",
  "webhook_secret": "REPLACE_WITH_YOUR_TEST_WEBHOOK_SECRET"
}
```

```sh
python3 startup_app/run.py --payment-config /absolute/private/payment.json
```

Only the fixed provider origin or an explicit `http://127.0.0.1:PORT/v1/` test
fixture is accepted. No configurable remote host or redirect is followed. Live
keys/mode are refused. Real provider account activation, tax invoicing, consumer
terms/refunds/support and commercial release are **pending**. The real account
and its checkout iframe have not been exercised; the delivered integration tests
use a deliberately fake provider. Browser checkout is offered only for an
explicit real-provider **test** configuration, not the loopback fixture.

The backend stores a durable local order before calling the provider, freezes the
price/terms/credits and binds the provider receipt and opaque note to it. A new
request ID is not the recovery procedure for an uncertain payment. Exact replay
returns the recorded order without another provider POST. `CREATE_PENDING` and
`OUTCOME_UNKNOWN` require reconciliation against the existing provider order ID;
receipt, note, currency and amount must agree. There is deliberately no automatic
POST retry after a lost response. A reserved order with no discoverable provider
record needs operator investigation; there is no automatic cancellation/recharge.

Callbacks authenticate the **stored** provider order ID and payment ID using
HMAC-SHA256, then fetch payment and order from the configured provider. Only an
exact captured/paid amount, currency and binding can create one test entitlement.
Uncaptured, foreign-order, wrong-amount or invalid-signature responses grant none.
Webhook signatures authenticate the raw bytes before parsing. Provider event IDs
plus body hashes are durable and idempotent; changed bodies under the same ID are
rejected. An unbound order or unsettled refund requests retry rather than
acknowledging an event that was not applied. No raw webhook payload is stored.

Refund totals are monotonic locally: partial refunds suspend access for review;
full refunds revoke it. A delayed capture or replay cannot clear a known refund.
Refund verification never initiates a refund or sends money. The synchronous
provider reconciliation has bounded individual requests and is not a certified
webhook-latency/SLA deployment; provider retries, dead-letter operation and outage
monitoring require live integration review.

A personal test entitlement creates a preparation review bundle. The first
request for one case input snapshot consumes one credit atomically. Exact retries
and new requests for that same snapshot reuse it. An old accepted request after a
material edit returns a historical-snapshot marker, not the new private data.
Refunded/expired entitlements cannot download their paid bundle. Ordinary owner
export remains available. A bundle cannot confirm inputs, approve tax or file.

Primary provider contracts used for implementation:

- [Create order](https://razorpay.com/docs/api/orders/create/)
- [Payment fetch](https://razorpay.com/docs/api/payments/fetch-with-id/)
- [Checkout verification](https://razorpay.com/docs/payments/payment-gateway/web-integration/standard/integration-steps/)
- [Webhook validation](https://razorpay.com/docs/webhooks/validate-test/)
- [Refund fetch](https://razorpay.com/docs/api/refunds/fetch-with-id/)

## Expert preparation requests

CUSTOMER requests specify a draft, current revision, ITR/GST topic, purpose,
reviewer and 1–30 day sharing consent. With no verified live roster the request
waits explicitly; it is not a fabricated booking or CA connection. No external
message is sent. An assigned reviewer can accept/decline a **test preparation**
request and use the existing snapshot-bound comment flow. The customer can cancel
and revoke the request, or revoke its grant in Share & notes.

Optional `--professional-registry /absolute/private/professionals.json` pins an
Ed25519 public key with signed **TEST_PREPARATION_ONLY** attestations. The operator
file is bounded/private; no private signing key is bundled. Each access reloads
and verifies the attestations against the pinned key, subject, skills, status,
not-before and expiry (at most 30 days). Missing, altered, replaced-key, revoked,
expired or wrong-skill attestations deny professional grants immediately. The
public directory says `SIGNED_TEST_ATTESTATION_ONLY`, `real_ca_verified=false`.
Account roles, an ADMIN checkbox or a subscription cannot self-assert CA status.

The registry format is exercised in `test_operations.py`; the test harness makes
fresh keys outside the application. A real credential authority/API, ICAI status,
professional contract and its current revocation checks remain unimplemented.
No test signature is acceptable evidence for a real professional. Native raw
files/previews and private ITR/GST worksheets are excluded from preparer grants.
Existing explicitly consented manual grants remain labelled unverified.

## Feedback that can drive a product decision

Signed-in users submit a feature, rating 1–5 and bounded message; exact retry does
not duplicate it. Each customer sees their own feedback. ADMIN sees a triage queue
with expected revisions, states OPEN/ACKNOWLEDGED/PLANNED/RESOLVED/DECLINED and an
in-app reply. The customer sees the updated state and reply after signing back in.
Simple PAN/long-number redaction is tested, **not** a complete PII detector. Do not
paste identities, account numbers or documents into feedback. Rendered feedback
uses text nodes; the browser smoke includes an HTML-injection string.

Operations reports aggregate counts, feature ratings and outstanding feedback.
Test captures are separated from live revenue (`0`); automatic tax approvals and
verified real CAs remain `0`. This is an in-product collection/triage loop, not an
unattended external customer-monitoring service or automatic legal-rule updater.
There are no real customer feedback or market-growth data in this delivery.

## Reproduce the operations checks

The Java app has no Python commerce dependency. The optional Python **test**
harness uses `cryptography==50.0.2` to issue synthetic expert signatures:

```sh
python3 -m venv /absolute/test-env
/absolute/test-env/bin/python -m pip install -r startup_app/requirements-test.txt
/absolute/test-env/bin/python startup_app/test_operations.py
/absolute/test-env/bin/python startup_app/check_operations_mutations.py
```

The real MySQL harness also runs these tests before its isolated backup/restore.
Read [deploy/MYSQL_SETUP.md](deploy/MYSQL_SETUP.md). New encrypted `operations`
records share the existing serial pilot transaction guard and 16 MiB SQL limit;
they do not establish production scale. Local vault limit remains 8 MiB. Upgrade
SQL explicitly with V003 through the normal migration launcher and original key.
Older vaults/backups obtain an empty operations namespace; old tax state is not
changed. Deletes remove linked expert requests, bundle references and their
request receipts. Billing/feedback retain their own bounded records and have no
stored case income payload; a production retention/erasure policy is pending.
