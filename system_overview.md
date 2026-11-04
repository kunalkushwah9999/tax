# TaxDesk system overview — 5.1.0

**Purpose:** workflow-based synthetic tax preparation for customers and preparation
teams. No general chatbot. English interface. Not a production-complete tax/CA
replacement; independent expectations remain 0/30. Original 30-case golden inputs,
blank outputs and three tax component modules stay unchanged.

## Runtime and responsibilities

The current executable is Java 17, JDK HttpServer, with vanilla browser JS/CSS.
It is a modular application, not a claimed production microservices architecture.
The optional HTTPS web deployment uses Nginx in front of the loopback Java listener.
Storage profiles are encrypted local vault, legacy MySQL and opt-in concurrent
MySQL; no implicit profile migration/fallback. The app can run locally without a
ChatGPT subscription or paid AI inference service.

| Module | Implemented boundary |
| --- | --- |
| Accounts | Salted PBKDF2 passwords, same-origin/CSRF sessions, authenticator MFA, one-use recovery codes, account epochs and SQL cross-node revocation |
| Preparation | Saved owner cases, explicit proposals/confirmation, profile/scope/checklists, normalized source comparison and narrow guarded tax preparation |
| Private workbench | Entered-lot capital FIFO observations, TDS review and notice organization; taxable gain/claim/filing authority not granted |
| Native/GST | Optional pinned ITR-1 structural worker and private normalized GST matching; no document authenticity, form eligibility or ITC approval |
| Coordination | Expiring/revocable preparation grants, comments, signed TEST expert requests, support and feedback |
| Commerce | Versioned TEST plans, test payment reconciliation/credits/bundles; no live billing/filing or verified real CA service |
| Privacy 5.1 | Eligible customer active-store cascade with fresh password/MFA, secret status receipt and retained pseudonymous restore control; operator/payment/external/backup review unfinished |
| Web/Android/cloud | Installable web generator, public deletion page, Android TWA source and VM/Nginx/systemd configs; no actual Android binary or public deployment |

## Security and storage

Record payloads use authenticated encryption with keys kept outside database
records; usernames use keyed database lookup. No PAN is collected in normal
preparation. Native import is synthetic-only and avoids raw document persistence.
Private workbench/native fields stay owner-only; preparation grants do not release
statutory/professional authority. An interrupted payment/write is not blindly retried.

Legacy SQL still has serial writes and a 16 MiB aggregate bound. Concurrent SQL
uses optimistic point/scan validation and short shard/quota publication locks; it
still has bounded scans/session/admission limits and does not prove 100,000 users.
Backups are encrypted; framed archives support up to 1 GiB / 100,000 records.
Restore remains isolated. Current deletion controls, grants, epochs/MFA and used
codes must be reconciled before a separately implemented production promotion.

Customer deletion removes credentials and owned case/support/feedback/sharing
records atomically. Current-node sessions/previews are discarded; other SQL nodes
reject absent users. A minimal pseudonymous erasure control stays in encrypted
storage. Backup/external-copy deletion is unconfirmed, and privacy control expiry,
payment/operator removal, support identity recovery and processor handling remain
review/engineering work. Public receipts expose only redacted result/counts/time.

## Verification and release

The 5.1.0 artifact has 15 privacy unit groups, privacy HTTP 13 per profile (local
one expected two-node skip; SQL all pass), current 41-flow SQL browser regression,
14 focused generated-web browser checks and the existing rebuilt API/storage/MFA/
workbench/native/recovery checks. Source/artifact bindings are in build reports
and STARTUP_RELEASE_V5_1.md. Internal tests are not independent tax approval.

Statutory ITR/GST/capital-gains/TDS implementation and independent rule approval,
live filing/payments, real CA verification, public production operations/scale,
external/backup privacy fulfillment and signed/device-tested Android release are
unfinished. No 0.001% CA rate, competitor superiority or growth/profit claim is
proven. See OWNER_HANDOFF.md and TAXDESK_FINAL_HANDOFF.md for the owner handoff.
