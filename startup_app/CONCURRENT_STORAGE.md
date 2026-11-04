# Concurrent MySQL and streaming recovery — 3.0.0

The opt-in `mysql-concurrent` profile replaces the **transaction-wide global
write guard** with optimistic business work, record dependency validation and
short shard locks at publication. The original `mysql` profile remains compatible
and keeps its global guard/16 MiB cap. Local mode remains an 8 MiB encrypted vault.
The new profile is an engineering release with measured tests, not a production
scale or 100,000-user certification. Use synthetic inputs.

## Isolation contract

- The SQL metadata schema is **2**; encrypted logical business state remains
  schema 1. Legacy and concurrent adapters refuse each other's database schema.
  Initialization requires a fresh database/key or an already initialized active
  schema-2 database with its original key. There is no silent in-place conversion.
- Business work reads a repeatable snapshot. At commit, observed record versions
  and missing keys are checked again under locking reads. The identity/version
  AES-GCM binding and unique HMAC username index remain in force.
- Full namespace scans record all 32 shard epochs and validate them at commit.
  This detects changes and new matching rows, including grant/entitlement scans.
  Namespace capacity checks are explicit predicates, rechecked against locked
  current counters and the transaction's net additions.
- Shard locks are acquired in a stable order; record writes retain revision CAS.
  Conflicts and known pre-commit deadlock/timeout/unique-key failures are rolled
  back and retried at most six times. The callback must contain **only retry-safe
  local business work**. External payment POSTs remain outside these callbacks.
- Once commit begins, failures return an uncertain outcome and are **never
  automatically retried**. Exact accepted request replay reconciles the result.
- Appending audit records merges only the transaction's changes; it never replaces
  another writer's appended event. Retention trimming can still contend/retry at
  the existing audit bound. Counts and audit history are not tax authority.
- The new store has no 16 MiB aggregate payload check. Per-record 256 KiB limits,
  existing application account/case/receipt/operations bounds and admission limits
  remain. Quota checks and broad scans can still serialize a short commit or cause
  retries. This is not a normalized owner-filtered repository or a connection pool.

The external signed TEST registry is checked by the workflow; its file changes are
not atomically bound to a SQL commit. Real credential/control integration remains
unreleased. The in-flight revocation test concerns a database case grant.

Do not deploy multiple copies as a complete distributed service: sessions/rate
limits are still process-local, workers/queue are bounded, and all production
identity, recovery, security, infrastructure and tax-release gates remain.

## Setup

Provision **MySQL 8.4** and use the pinned Connector/J 26.10.0 artifact. Keep
certificate/hostname verification enabled. The connection format and separate
DDL migration / restricted DML runtime accounts are described in
[deploy/MYSQL_SETUP.md](deploy/MYSQL_SETUP.md). Use a fresh database and new key:

```sh
python3 startup_app/run.py --storage mysql-concurrent \
  --mysql-config /absolute/private/migration.json \
  --jdbc-jar /absolute/mysql-connector-j-26.10.0.jar \
  --key-file /absolute/private/new-record-keys/key --init-mysql

python3 startup_app/run.py --storage mysql-concurrent \
  --mysql-config /absolute/private/runtime.json \
  --jdbc-jar /absolute/mysql-connector-j-26.10.0.jar \
  --key-file /absolute/private/new-record-keys/key
```

Initialization applies the existing tables plus `V004__concurrent_records.sql`
with 32 version/count/byte controls per namespace. `workspace_meta.payload_bytes`
is the legacy field and is not the schema-2 total. Query
`SUM(payload_bytes)` from `workspace_concurrency` for the new encrypted byte count.
Only the migration operator creates schema; application startup never migrates or
falls back to local storage. The same `--storage`, config, driver and original key
must be passed to operator-account setup and all subsequent commands.

## Framed backup

For `mysql-concurrent`, `--backup-to` selects the new format automatically:

```sh
python3 startup_app/run.py --storage mysql-concurrent \
  --mysql-config /absolute/private/runtime.json \
  --jdbc-jar /absolute/mysql-connector-j-26.10.0.jar \
  --key-file /absolute/private/new-record-keys/key \
  --backup-to /absolute/private/backups/snapshot.txb \
  --backup-key /absolute/private/backup-keys/key

python3 startup_app/run.py \
  --verify-backup /absolute/private/backups/snapshot.txb \
  --backup-key /absolute/private/backup-keys/key
```

`TAXFRM02` archives stream one consistent JDBC snapshot instead of constructing a
whole-state JSON payload. Each bounded frame uses authenticated encryption bound
to a fresh archive ID and its exact sequence. An authenticated footer checks
record counts, all prior encrypted frames' digest and exact EOF. Missing footer,
reordering/splicing, ciphertext/binding changes, duplicate identities, invalid
lengths, truncation and trailing bytes reject. Verification never creates a missing
key. Files are private; publication never overwrites an existing target.

Format limits: 1 MiB per encoded frame, 256 KiB logical record, 100,000 records and
1 GiB archive file. These are resource bounds, **not** load-test results. Larger
sharded/offsite/PITR recovery remains unimplemented. A snapshot contains business
state at backup time; it does not prove current revocations/deletions or law.

## Restore always stays isolated

```sh
python3 startup_app/run.py --storage mysql-concurrent \
  --mysql-config /absolute/private/fresh-target-migration.json \
  --jdbc-jar /absolute/mysql-connector-j-26.10.0.jar \
  --key-file /absolute/private/fresh-target-keys/key \
  --restore-backup /absolute/private/backups/snapshot.txb \
  --backup-key /absolute/private/backup-keys/key
```

The complete archive is authenticated before target mutation and authenticated
again while importing. A fresh target/key are mandatory. Record inserts and
recomputed shard accounting commit together, under an **ISOLATED** target. Normal
server and normal initializer refuse it. There is no promotion/unlock command.
Current deletion, revocation, quarantine, signing/control and permitted data state
must be reconciled before a separately implemented/reviewed production promotion.
Source and existing target databases are never overwritten by this operation.

Legacy `TAXBKP01` verification/isolated legacy restore stays supported with the
legacy profile. Framed restoration requires `mysql-concurrent`; changing a flag
does not convert an existing legacy database. Keep old and new test databases and
keys separate. The Java framed exporter also supports a bounded legacy DataStore;
no automatic migration/cutover service is claimed.

## Reproduce the actual checks

```sh
python3 startup_app/test_concurrent_mysql.py \
  --mysql-basedir /absolute/mysql-8.4.13-linux-glibc2.28-x86_64-minimal \
  --jdbc-jar /absolute/mysql-connector-j-26.10.0.jar \
  --native-python /absolute/native-venv/bin/python \
  --native-schema-dir /absolute/pinned-native-sources \
  --with-concurrent-mutations
```

The harness creates disposable TLS databases with random credentials and cleans
them up; it never targets an existing database. Optional `--browser-chromium`,
`--browser-playwright`, `--browser-node`, `--browser-output` run the authenticated
browser suite on the new SQL backend. Install `requirements-test.txt` for fresh
signed TEST-professional fixtures; those signatures are not real CA credentials.
Detailed observed outcomes and current source/JAR bindings are in build reports.

Primary design references:

- [MySQL 8.4 locking reads](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking-reads.html)
- [Deadlocks and retries](https://dev.mysql.com/doc/refman/8.4/en/innodb-deadlocks.html)
- [Error handling](https://dev.mysql.com/doc/refman/8.4/en/innodb-error-handling.html)

The algorithm's read-set and scan-epoch checks are this implementation's controls;
the manuals do not certify this code. Independent review and deployment/load tests
are still necessary. No tax rule, golden expectation, paid tax permission or filing
connection is changed by selecting the new storage profile.
