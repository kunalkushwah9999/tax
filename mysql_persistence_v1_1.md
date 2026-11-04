# MySQL persistence and HTTPS — startup_app 1.1.0

1 November 2026. Implemented and tested using synthetic data only. This records a
bounded development/pilot profile, not tax approval or a production startup.

## Delivered behavior

The existing customer, sample case, consent, preparation comment, planning quote,
support and metadata-audit operations now run against either the original local
encrypted vault or an explicitly selected MySQL store. SQL stores one encrypted
record per entity, lazily reads records and flushes changed records with the
accepted receipt and audit in one InnoDB transaction. A singleton control row
locks business writes; expected versions and retained canonical request receipts
preserve stale-edit and exact-retry behavior across separate connections.
No failed SQL operation falls back to a local file.

AES-256-GCM uses fresh 12-byte nonces and authenticates format, namespace, record
identity and storage revision. Separate HMAC-derived keys serve encryption and
username lookup. The random 32-byte record key is outside MySQL; a key-binding
probe rejects wrong/missing keys on an initialized database. This app does not
collect PAN; no production PAN identity/encryption provider is claimed.

Connector/J uses VERIFY_IDENTITY with an explicit PKCS12 CA trust store. It
disables system-trust fallback, public-key retrieval, local-file loading,
multi-statements and autoreconnect. Credentials are private operator-file
properties, not URL/query/command-line arguments. Schema initialization is explicit
with a separate migration configuration. The test runtime account has DML only;
its attempted DDL is denied by actual MySQL.

The listener stays at 127.0.0.1. An explicit HTTPS public-origin selects exact
external Host/Origin authority, Secure/HttpOnly/SameSite=Strict __Host- cookies and
HSTS. No incoming X-Forwarded header grants authority. The operator must provide
the actual trusted proxy/certificate/network setup. Nginx and systemd examples
are supplied; they were not deployed or executed here.

## Observed evidence on the delivered version

| Check | Actual result | Scope/limit |
|---|---|---|
| Startup unit | 27 named groups and 12 generated ownership-denial checks passed | Unit evidence, not a SQL engine |
| Local API | 36 groups passed, 0 failures/errors/skips | Real Java HTTP process; default encrypted-vault profile |
| SQL contract | 20 groups passed; four concurrent distinct-writer rounds | Real MySQL 8.4.13, Connector/J 26.10.0 and certificate-authenticated TLS |
| SQL-backed API | 35 groups passed; 1 local-vault-byte group intentionally skipped; 0 failures/errors | Same 36-group API suite through the actual Python launcher/JDBC profile; SQL suite checks ciphertext separately |
| Write failure | A SQL trigger rejects an intermediate receipt insert after a case write; neither case nor receipt survives | Demonstrates rollback across actual SQL statements |
| Commit ambiguity | Commit succeeds, an injected acknowledgement exception follows, exact retry returns the retained receipt | Actual SQL commit with a post-commit fault hook, not a real network-partition experiment |
| Process crash | Actual mysqld SIGKILL and restart preserve a committed marker and saved API cases | API returns 503 during outage; no local fallback; power loss/backup restore not tested |
| SQL authority | Separate runtime account denied DDL; wrong key and certificate hostname mismatch rejected; payload transplant rejected | Tested fixture grants/certificate/key, not deployment credential audit |
| HTTPS API | 12 authenticated TLS proxy groups passed, 0 failures/errors | Real HTTPS client and local TLS fixture with explicitly trusted disposable CA; not browser, Nginx or public hosting |
| Selected SQL defects | Three independent copies compiled and failed behavioral SQL assertions | Removed write guard, dropped accepted receipt, omitted record identity from payload AAD; original source preserved |
| Selected business defects | Six independent copies compiled and were detected | Ownership, revoked access, stale edits, proposal confirmation, deleted-create receipt and frozen prices |
| Interface | JavaScript syntax and literal HTML-ID checks pass | Prior static desktop landing/signup inspection retained; authenticated browser/mobile/accessibility still unverified |
| Extracted package | Starts without rebuilding; saved case/source confirmation, actual dual engine/blocked guard and restart pass; both archive manifests verified | Default local profile; SQL and HTTPS tests bind the exact same packaged JAR |

Raw machine-readable reports are under startup_app/build: build_results.json,
unit_test_results.json, http_test_results.json, mysql_test_results.json,
mysql_http_test_results.json, mysql_recovery_results.json,
mysql_mutation_results.json, https_test_results.json, mutation_results.json,
interface_checks.json and package_smoke_results.json. Report counts are not a
security, tax-accuracy or coverage percentage. No 100,000-user benchmark was run.

The same 1.1.0 executable was used for local API, SQL API and HTTPS checks.
Executable SHA-256: `2678a76ab57ab1993816c75d3428a1c2c3b49c387bdb8379f0cbe148fa1846b3`.

The original guard 0.1.4, private engine 0.1.0 and anonymous workbench 0.4.0
source/JAR bytes are preserved. Their earlier suites were not rerun or counted
as new MySQL evidence. The original 30 golden inputs and XLSX are unchanged,
all expected values remain blank and independently approved outputs remain zero.

## Deliberate limits and remaining work

All SQL business writes serialize on one control row. Namespace/audit iteration
may load bounded collections; encrypted payload totals are capped at 16 MiB.
Existing account/case/receipt limits remain. Sessions, rate limits and the small
worker pool are process-local. There is no pool, distributed-session system,
normalized indexed case repository or implemented Spring service. These choices
preserve the aggregate contract for a bounded pilot; they do not establish final
production throughput or availability.

There is no automatic import from the local vault, silent record-key replacement,
production key rotation, backup/restore proof, point-in-time recovery, real
network-partition test, power-loss test, multi-node failover or independent
security certification. MySQL process recovery does not establish those claims.
Nginx/systemd configuration syntax and runtime behavior still require operator
validation on the intended host.

Authenticated browser/mobile/keyboard/accessibility QA remains pending. API
sessions tested over TLS do not substitute for those user-interface checks.
Public hosting, domain/certificates, monitoring/on-call, real identity/recovery,
professional credential services, checkout/invoicing/refunds and filing provider
connections remain pending. Numerical tax release, full ITR, GST, capital gains
and verified CA sign-off remain unavailable. Use synthetic data only.

## Reproduce

Default packaged profile: `python3 startup_app/run.py`.
Build/check: `python3 startup_app/build.py`, then `test_http.py`, `test_https.py`
and `check_mutations.py` from startup_app. See
[startup_app/deploy/MYSQL_SETUP.md](startup_app/deploy/MYSQL_SETUP.md) for the exact
SQL setup/test commands and prerequisites. The disposable harness creates its own
private database/users/passwords/TLS CA and removes them at completion. It does
not connect to an existing user database. No vendor server/JDBC binary, real
credentials or record key is bundled. The supplied fetch helper verifies the
pinned official Connector/J artifact before writing it.

MySQL 8.4.13 minimal Linux and Connector/J 26.10.0 were used, with Java 17 and
Python 3.12. No Docker, Windows or public cloud deployment was exercised.
Driver SHA-256: `cef70caf532833c9ab8f41f74773ce22c08bf503fad215c9c2ee1e9db87e759e`.
Primary vendor references reviewed for the implementation:

- [Connector/J security properties](https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-security.html)
- [MySQL 8.4 release notes](https://dev.mysql.com/doc/relnotes/mysql/8.4/en/)
- [Generic binary installation](https://dev.mysql.com/doc/refman/8.4/en/binary-installation.html)
- [Pinned official Maven artifact](https://repo.maven.apache.org/maven2/com/mysql/mysql-connector-j/26.10.0/mysql-connector-j-26.10.0.jar)
