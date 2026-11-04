> Current 3.0 addition: see ../CONCURRENT_STORAGE.md (from this deploy directory) and ../../STARTUP_RELEASE_V3.md. The separate mysql-concurrent schema-2 profile uses fresh databases/keys, streaming framed backups and isolated restore. Legacy instructions below retain their original profile/limits. No production restore promotion or 100,000-user capacity is approved.

# Current handoff — consolidated 2.0.0

Read [../../STARTUP_RELEASE_V2.md](../../STARTUP_RELEASE_V2.md) for current gates
and [../OPERATIONS.md](../OPERATIONS.md) for test-only billing, expert attestation
and feedback. V003 explicitly adds encrypted operations records. Runtime DML
accounts do not perform migrations; the original data key remains required.

Authenticated Chrome desktop and 390px viewport smoke QA passed 22 checks. This
does not certify physical devices/Safari/accessibility or a public deployment.
35 operations groups passed on each storage backend with fake payment providers
and signed test professionals. No live sales, verified CA roster or filing
connection has been certified. All other scale/recovery/tax gates below remain.

Earlier 1.2 handoff is retained for its architecture limitations:

---

# Production handoff — current 1.2.0 boundary

The optional MySQL pilot, explicit migration and strict HTTPS-origin mode are now
implemented. Read [MYSQL_SETUP.md](MYSQL_SETUP.md) for runnable setup and
[../../mysql_persistence_v1_1.md](../../mysql_persistence_v1_1.md) for actual tests.
The service binds only 127.0.0.1. No public deployment was performed. Owner-only ITR scope/task preparation, normalized
GST observations and encrypted logical backup/isolated MySQL restore are now
implemented. Read ../SELF_SERVICE.md and BACKUP_RESTORE.md for exact boundaries.

## Implemented and exercised

Real MySQL 8.4.13/Connector-J 26.10.0 verified TLS; explicit initialization;
restricted DML runtime account; encrypted identity/revision-bound records and
HMAC username lookup; atomic case/receipt/audit writes; rollback after an
intermediate SQL failure; concurrent stale/replay isolation; post-commit lost-ack
recovery by exact retry; real SIGKILL restart and API failure during DB outage.
Strict external Host/Origin, Secure __Host- cookies, HSTS and CSRF were checked
through a local authenticated TLS proxy fixture. Nginx and systemd examples are
operator templates, not executed deployment evidence.

## Required before a production service

The SQL unit-of-work still has a singleton control-row lock, serializes all
business writes, scans some bounded namespaces and caps payload at 16 MiB.
Replace this aggregate contract with normalized/query-filtered repositories,
reviewed constraints/lock scopes, connection pooling and measured concurrency.
Retain Java/Spring + MySQL as the production target. Distributed sessions, real
identity/recovery/MFA, production authorization/control/quarantine and the
calculation-attempt/outbox protocol need their own implementation and tests.

Owner/grant rechecks, expected versions, exact request fingerprints and atomic
accepted receipts must survive that change. Deleted-create tombstones must
prevent resurrection. Preparation comments must stay input-snapshot-bound and
revoked/expired access must deny old receipts. ADMIN prices and paid entitlements
must never authorize a tax result. Keep expensive calculation outside write
transactions and recheck source identity/authorization at delivery.

Production domain/certificates/proxy validation, key custody/rotation, reconciled production
restore promotion and physical/offsite/PITR recovery, access recovery, monitoring, incident/rollback ownership,
retention/deletion, independent security and authenticated browser/mobile QA
remain pending. No public tax amount, verified CA status, charge or filing is
released. All 30 independent reference expectations remain blank/unapproved.
Provider integrations and broader tax schedules are separate unreleased scope.

The delivered archives contain code/templates/reports and synthetic fixtures,
without runtime customer data, credentials, private keys or vendor binaries.
