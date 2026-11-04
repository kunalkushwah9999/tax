> Current 3.0 addition: see ../CONCURRENT_STORAGE.md (from this deploy directory) and ../../STARTUP_RELEASE_V3.md. The separate mysql-concurrent schema-2 profile uses fresh databases/keys, streaming framed backups and isolated restore. Legacy instructions below retain their original profile/limits. No production restore promotion or 100,000-user capacity is approved.

> 2.0 upgrade: the explicit initializer now applies V001, V002 and **V003__startup_operations.sql**. Stop the app and use the original record key and migration account. V003 extends the namespace constraint, without changing existing ciphertext. Runtime opening checks the migrated table definition; it cannot silently upgrade through a DML-only account. Operations remain within the serial-write/16 MiB pilot contract.

# MySQL 8.4 and HTTPS pilot setup

The optional MySQL profile is implemented and exercised against a disposable real
MySQL 8.4.13 server with Connector/J 26.10.0. It is a transactional **pilot**
profile, not the final high-volume relational repository or a public deployment.
The default local encrypted-vault profile remains available without a driver.
No MySQL server, JDBC binary, runtime credentials, CA private key or record key
is distributed in the code archive.

## Upgrade an existing 1.1.0 database explicitly

Keep its original record key and run --init-mysql with the migration configuration
before starting 1.2.0. The normal service account cannot create the new V002
workspace_recovery control table. An absent table fails startup until this explicit
upgrade. Existing ciphertext/key format is retained. This command never promotes
an ISOLATED restored database to ACTIVE. Review a backup before a real upgrade;
only synthetic fresh/isolated fixtures were exercised here.

## Provision explicitly

Have an operator provision MySQL 8.4 LTS with InnoDB and certificate-authenticated
TLS. Create the intended database, a separate migration account and a restricted
runtime account. Runtime needs SELECT, INSERT, UPDATE and DELETE on this database;
it must not have CREATE, ALTER, DROP, TRIGGER or global administration rights.
The supplied integration harness proves DDL denial for its runtime account.
The deployment's actual grants still need independent verification.

Create a PKCS12 trust store for the database CA. Its leaf certificate must match
the configured database hostname. SqlConfig always uses VERIFY_IDENTITY, disables
system-trust fallback, public-key retrieval, local-file loading, multi-statements
and reconnect. There is no insecure-mode API/flag or local-vault fallback when
MySQL is unavailable.

Copy the two example JSON files to private operator paths, replace every
placeholder and restrict access to the service account (0600 on tested Linux).
The migration and runtime files must have different database accounts. Do not
place actual configuration in Git, the code ZIP, web assets or command arguments.
The examples are templates and cannot connect as supplied.

Fetch the pinned official driver without bundling or installing it globally:

```sh
python3 startup_app/tools/fetch_jdbc.py --output vendor/mysql-connector-j-26.10.0.jar
```

The helper verifies the reviewed artifact SHA-256. Review the vendor's license
before redistribution. Upgrades require a new reviewed pin and repeat tests;
this pin does not itself certify the artifact or promise future compatibility.

## Initialize with the migration account

Use a fresh explicit SQL record-key path outside the database. Initialization
creates the two InnoDB tables and a key-binding probe. It refuses to replace a
missing/wrong key for an existing initialized database.

```sh
python3 startup_app/run.py --storage mysql --mysql-config /etc/taxdesk/mysql-migration.json --jdbc-jar vendor/mysql-connector-j-26.10.0.jar --key-file /etc/taxdesk/record.key --init-mysql
```

Run the service with the restricted runtime account:

```sh
python3 startup_app/run.py --storage mysql --mysql-config /etc/taxdesk/mysql-runtime.json --jdbc-jar vendor/mysql-connector-j-26.10.0.jar --key-file /etc/taxdesk/record.key
```

Existing local-vault accounts/cases are **not silently imported**. This profile
starts with the explicitly initialized SQL database. An offline, reviewed import
and backup/restore/rotation protocol is separate work. Do not copy a vault file
into the schema or create a replacement key to recover missing encrypted data.

The existing `--create-operator ADMIN` / `--create-operator PREPARER` setup commands
accept these same explicit SQL flags. They do not verify professional credentials.

## HTTPS reverse proxy

The application keeps its inner listener on 127.0.0.1. An explicit
`--public-origin https://YOUR_HOSTNAME` selects the exact external authority,
Secure/HttpOnly/SameSite=Strict `__Host-` cookies and HSTS. HTTP origins, credentials,
query/fragment/path-bearing origins and invalid ports are rejected. Incoming
X-Forwarded-* headers do not grant scheme/host authority.

Terminate TLS at an operator-managed reverse proxy and preserve the original
Host and Origin. Replace and validate deploy/nginx.conf.example and
taxdesk.service.example on the target machine. The service unit contains no
password argument. The application/proxy/database accounts, domain, certificate
renewal, monitoring and incident ownership must actually exist before deployment.
No host or domain was provisioned here. Nginx/systemd templates were not executed.

The tested HTTPS path used a real local TLS reverse-proxy fixture and an explicitly
trusted disposable test certificate. It exercised authenticated requests and the
Secure cookie/Host/Origin/CSRF/result guard boundaries. It was not a browser,
Nginx deployment, public certificate or production security assessment.

## Deliberate pilot limits

One encrypted record is stored per user/case/grant/quote/receipt/event. Payload
AEAD binds namespace, record ID and storage revision; usernames use a separate
purpose-derived HMAC lookup tag. Keys stay outside MySQL. Reads load records
lazily; changes and receipts are flushed in one transaction. A missing/invalid
record does not become zero income or an approved tax result.

A singleton InnoDB control row serializes **all business write transactions**.
This prevents cross-process lost updates while the application still exposes its
aggregate unit-of-work contract. Namespace iteration and audit maintenance can
read bounded collections. Total encrypted payload is capped at 16 MiB. Existing
account/case/receipt limits still apply. This is not the final normalized/indexed
SQL service and cannot substantiate 100,000 concurrent users.

Replace this guard/aggregate contract with measured, query-filtered repositories,
appropriate constraints and locking scopes, durable session/control/quarantine
services and the production calculation-attempt/outbox protocol before scale.
Spring integration, pooling, distributed sessions, migration/import, production
restore promotion/current-control reconciliation, physical/offsite/PITR backups,
key rotation and load/recovery certification remain pending. Logical encrypted
backup and a real fresh-MySQL isolated restore are now tested; see BACKUP_RESTORE.md.

## Repeat the disposable tests

A Linux MySQL 8.4 distribution, its system libraries, OpenSSL, keytool, Java 17+
JDK and the pinned driver are required. The harness creates its own private data
directory, random database/users/passwords, test TLS CA and record key. It never
connects to an existing user database. The server binds loopback, requires TLS
and disables the unused Unix/X sockets. Fixture secrets are removed at completion.

```sh
python3 startup_app/build.py
python3 startup_app/test_mysql.py --mysql-basedir /path/to/mysql-8.4 --jdbc-jar /path/to/mysql-connector-j-26.10.0.jar
python3 startup_app/test_https.py
```

Use `--mysql-library-path` only when that portable MySQL distribution needs its
normal private shared-library path. `--skip-selected-mutations` omits only the
compiled SQL defect experiments. `--keep-private-fixture` is for local diagnosis;
never publish that fixture. This test harness was not run on Windows or Docker.
