> 3.0: routes and permissions remain compatible. Uncertain payment-create responses now carry retry_same_request=true. POST /api/orders rejects a fresh request ID with EXISTING_ORDER_RECONCILIATION_REQUIRED while the same owner/plan has CREATE_PENDING or OUTCOME_UNKNOWN; exact replay still returns the existing record. Storage selection is operator-only, never an HTTP tax/plan approval field.

# Startup development API

Same-origin JSON from a loopback listener. With explicit --public-origin,
external Host and Origin must match that configured HTTPS authority and session
cookies are Secure __Host- cookies. X-Forwarded-* does not grant authority.
The default development profile uses exact loopback HTTP authority. All responses are no-store. Mutating routes require
POST, exact Origin, a current opaque session cookie and X-CSRF-Token. Bodies are
at most 64 KiB; duplicate keys, malformed UTF-8/surrogates, decimals/exponents as
JSON number tokens and extra authority fields reject. Money inputs are strings;
versions and price paise are integer tokens. Errors contain codes, not raw inputs.

| Route | Method | Authority / behavior |
|---|---|---|
| /api/bootstrap | GET | Open/reuse anonymous session; return CSRF, current public account or null, fixed release readiness |
| /api/register | POST | Exact username/display_name/password; CUSTOMER only, no supplied role |
| /api/login | POST | Username/password; salted credential check and session/CSRF rotation |
| /api/logout | POST, empty | Current session revocation |
| /api/health | GET | Process readiness only, never tax release readiness |
| /api/prices | GET | Immutable scheduled planning-price catalog; checkout_available=false |
| /api/cases | GET / POST | Owner's cases or actively shared PREPARER queue / CUSTOMER creates bounded sample draft |
| /api/cases/{id} | GET | Owner or active specific PREPARER grant; ADMIN has no private-case override |
| /api/cases/{id}/edit | POST | Owner-only exact operation; expected_revision and request_id required |
| /api/cases/{id}/sources | GET | Owner's current normalized source projection; stale input snapshot rejects |
| /api/cases/{id}/assess | POST, empty | Owner-only existing dual arithmetic and signed blocked result; current identity rechecked |
| /api/cases/{id}/export | GET | Owner-only preparation JSON attachment, explicitly not ITR |
| /api/cases/{id}/delete | POST | Owner, expected_revision and explicit confirm=DELETE; accepted create receipts retained |
| /api/preparers | GET | Authenticated directory of local preparation aliases; credentials unverified |
| /api/cases/{id}/grants | GET / POST | Owner lists grants / explicit purpose-limited consent for 1–30 days |
| /api/cases/{id}/revoke | POST | Owner with grant/case binding and current version |
| /api/cases/{id}/note | POST | Active PREPARER grant; snapshot-bound preparation comment, no sign-off |
| /api/quotes | GET / POST | Own immutable planning quotes / quote from currently effective price, no charge or entitlement |
| /api/admin | GET | ADMIN aggregate counts and pending release capabilities; no financial case payloads |
| /api/admin/prices | POST | ADMIN future immutable price version; no tax approval fields |
| /api/tickets | GET / POST | Own threads or ADMIN support queue / new scoped in-app request |
| /api/tickets/{id}/update | POST | Thread owner or ADMIN; expected version, bounded reply/status |
| /api/audit | GET | Own metadata events; ADMIN operational metadata; no amounts/source/password/message bodies |

Edit operations: PROPOSE, CONFIRM, CLEAR, REVIEW, UNREVIEW, PROFILE, RENAME,
DOCUMENT, ARCHIVE, RESTORE, SOURCE_IMPORT, SOURCE_SELECT, SOURCE_REOPEN,
SOURCE_PROPOSE. PROFILE has the existing planner's exact field set. Each action
has an exact allowlist in Workspace.edit; actor, owner, scope/approval, current
snapshot, professional authority, entitlement and result values are not writable.

Accepted create/edit/grant/revoke/note/price/quote/support actions share a
user/request-ID namespace with a canonical command fingerprint. Identical retries
return an amount-free receipt and require a current authorized read. They do not
restore old state. Changed/cross-route reuse conflicts. Rejected attempts do not
reserve IDs. Authorization is rechecked before receipt delivery. Sign-out and
other authenticated operations on the same session are serialized.

The default local aggregate uses a process file lock. The optional MySQL pilot
uses a singleton InnoDB control-row lock across independent SQL connections;
case, accepted receipt and audit commit in one SQL transaction. Record revision
and identity are bound by authenticated encryption. Runtime account privileges
are limited to DML; schema initialization is explicit and separate.

An exception at SQL commit/acknowledgement yields 503 STORAGE_OUTCOME_UNKNOWN with
retry_same_request=true. Preserve the same request ID, command and expected
revision, then retry that exact action. The outcome may already be committed;
never generate a new request ID to guess a resolution. Authorized exact retries
return the retained receipt and do not overwrite later state. There is no
automatic write retry, session replication or fallback to the local store.
SQL unavailable/read failures are not tax zeroes or successful responses.

All SQL business writes currently serialize, some namespace operations scan
bounded collections and encrypted payload totals are capped at 16 MiB. This is
not the final normalized/indexed Spring repository or distributed-session API.
See deploy/MYSQL_SETUP.md and ../mysql_persistence_v1_1.md for measured evidence.

## Optional native ITR intake (1.3.0)

| Route | Authority / behavior |
|---|---|
| GET /api/native-capabilities | Authenticated configuration/status; no filing or native GST authority |
| GET /api/native-sample | Authenticated explicitly synthetic structural specimen, not a fileable/tax-correct return |
| POST /api/cases/{id}/itr-native-preview | Owner only, exact expected_revision/synthetic_confirmed/json_text; no durable write; local pinned-schema worker; owner/case/revision-bound sanitized preview |
| POST /api/cases/{id}/itr-native-propose | Owner only, exact request_id/expected_revision/preview_id/same_taxpayer_confirmed/selections; atomic selected fields as ESTIMATED with ordinary separate confirmation |
| POST /api/cases/{id}/itr-native-discard | Owner only, expected_revision/preview_id; discard ephemeral preview; no case mutation |

Each selection has exactly field, amount, whole_category_confirmed and
replace_input_confirmed. Amount/field must match the server preview. Accepted
replay is authorized and matched before preview lookup, so restart/expiry does
not overwrite later state or prevent confirming an already accepted outcome.
The preview route alone allows a 400,000-byte JSON wrapper for a 196,608-byte raw
file. Other routes keep 64 KiB. UTF-8/native structure/number/string/node bounds
remain enforced. Two worker processes maximum, five-second parent deadline,
64 previews total/four per account/one per case, ten-minute TTL. No client schema
URL, worker path, result authority or dynamic reference is accepted. GST native
schema and all filing remain unreleased. See NATIVE_IMPORT.md.


## Consolidated 2.0 operational routes

All routes below require a signed-in enabled account except the raw signed webhook.
All authenticated POSTs retain exact Origin and session CSRF checks. Roles and
accepted request IDs do not grant tax authority. See OPERATIONS.md for payload
semantics and the real-HTTP tests for executable examples.

| Route | Method | Authorization / result |
| --- | --- | --- |
| `/api/operations-capabilities` | GET | Test/live/registry capability truth |
| `/api/catalog` | GET | Immutable future/effective TEST plans |
| `/api/admin/catalog` | POST | ADMIN; request_id, plan, label, amount_paise, valid_days, credits, effective_at, terms_version |
| `/api/billing` | GET | Own orders/entitlements only |
| `/api/orders` | POST | Exact request_id, plan_version_id, terms_version, test_payment_consent; frozen server price |
| `/api/orders/{id}/checkout` | GET | Owner; public TEST checkout parameters |
| `/api/orders/{id}/reconcile` | POST | Owner; existing provider_order_id, backend binding verification |
| `/api/orders/{id}/callback` | POST | Owner; payment_id/signature, stored-order HMAC and provider fetch |
| `/api/payments/webhook` | POST | No browser CSRF; raw-body provider HMAC and X-Razorpay-Event-Id required |
| `/api/cases/{id}/preparation-bundle` | POST | Owner; request_id/expected_revision, atomic personal TEST credit; never approves tax |
| `/api/firm-queue` | GET | PREPARER plus active firm TEST access; current case grants only |
| `/api/experts` | GET | Current signed TEST directory, real_ca_verified=false |
| `/api/expert-requests` | GET/POST | Own/current-consented request list; explicit customer case/revision/topic/purpose/reviewer/days/consent |
| `/api/expert-requests/{id}/update` | POST | Owner cancellation or current signed TEST preparer accept/decline; expected revision/request_id |
| `/api/feedback` | GET/POST | Own history; ADMIN action queue; feature/rating/message and exact request_id |
| `/api/admin/feedback/{id}/triage` | POST | ADMIN; request_id/expected_revision/status/reply |
| `/api/admin/operations` | GET | ADMIN; aggregates, excludes test captures from live revenue |

Legacy `/api/prices`, planning quotes and support tickets retain their earlier
quote-only semantics. They do not create the new test entitlements. No real
CA verification, live payment, tax release or filing endpoint is enabled.
