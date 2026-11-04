> Current 3.0 addition: see ../CONCURRENT_STORAGE.md (from this deploy directory) and ../../STARTUP_RELEASE_V3.md. The separate mysql-concurrent schema-2 profile uses fresh databases/keys, streaming framed backups and isolated restore. Legacy instructions below retain their original profile/limits. No production restore promotion or 100,000-user capacity is approved.

# Encrypted logical backup and isolated MySQL restore — 1.2.0

The operator-only commands below were exercised with synthetic data and real
MySQL 8.4.13. This is a logical business-data snapshot, not a MySQL physical/server
backup, grant/config backup, point-in-time recovery, failover or production DR
certification. The current 16 MiB SQL pilot and global write guard remain.

## Create and authenticate

Keep a dedicated random backup key separate from the live record key, database,
archive and code. The command creates a missing backup key only for a new backup;
verification never recreates a missing key. Loss of the key loses the archive.
The backup directory must be private. The output path must be new: existing files
are never overwritten. Keys, archives and private JSON configuration are not
part of the deliverable. No backup retention/deletion/offsite policy is automated.

For MySQL, use the restricted runtime configuration with certificate-verified TLS:

```sh
python3 startup_app/run.py --storage mysql --mysql-config /etc/taxdesk/mysql-runtime.json --jdbc-jar vendor/mysql-connector-j-26.10.0.jar --key-file /etc/taxdesk/record.key --backup-to /private/taxdesk-backups/sample.txb --backup-key /private/taxdesk-backup-keys/backup.key
```

For the local profile, stop the server before acquiring its single-writer store:

```sh
python3 startup_app/run.py --data-dir /private/taxdesk-local --backup-to /private/taxdesk-backups/local.txb --backup-key /private/taxdesk-backup-keys/backup.key
```

Authenticate an archive offline, without a MySQL connection or JDBC dependency:

```sh
python3 startup_app/run.py --verify-backup /private/taxdesk-backups/sample.txb --backup-key /private/taxdesk-backup-keys/backup.key
```

Verification reports only format/time/backend/counts and isolated-only policy;
it does not print private inputs, credentials or financial data. A fresh nonce,
AES-GCM authentication and backup-purpose identity bind the payload. Wrong keys,
tampering, truncation, invalid framing/schema and oversized snapshots reject.
Snapshot plaintext is bounded at 24 MiB, separate from the live payload cap.

## Restore to a fresh isolated target

Have an operator provision a completely empty separate MySQL database and private
migration configuration for it. The restore target must have **no existing tables**;
it must use a fresh record-key path. Use 1.2.0 code and the same pinned driver.
Never point this command at the live database or reuse its key path.

```sh
python3 startup_app/run.py --storage mysql --mysql-config /etc/taxdesk/mysql-restore-target.json --jdbc-jar vendor/mysql-connector-j-26.10.0.jar --key-file /private/taxdesk-restore-keys/record.key --restore-backup /private/taxdesk-backups/sample.txb --backup-key /private/taxdesk-backup-keys/backup.key
```

The target's recovery state is ISOLATED before business records are inserted.
The business data and retained request receipts/audit restore transactionally,
re-encrypted under the fresh target key. Failure leaves an isolated target for
operator diagnosis, not an active partial service. Re-running against an existing
or isolated database refuses overwrite. Normal server startup and --init-mysql
reject the isolated target. No promotion/activation flag is provided.

A backup can contain a case deleted or a grant revoked after snapshot time. The
actual test restores that old snapshot and proves it cannot be served normally.
Reconcile current permitted-deletion, access revocation, rule recall, quarantine,
retention/legal-hold and other authoritative control state before designing a
reviewed promotion process. This release does not implement or approve that
process. Do not bypass the isolation marker manually or start older code against
the restored database. The marker is operator-controlled state, not an independent
tamper-proof control plane or protection against a privileged malicious operator.

## Actual evidence and gaps

Eight archive unit groups exercise encryption/equality/no-overwrite/nonce/missing
or wrong key/tamper/truncation/policy. Nine real MySQL/launcher groups authenticate
a snapshot, change source deletion/revocation state, restore exact pre-change
business records into a new database, reject server startup/init/overwrite, and
reject missing-key verification. The full SQL HTTP/concurrency/crash checks pass.
See build/mysql_restore_test_results.json and the combined recovery report.

Public hosting, offsite copies, physical MySQL/config/grant recovery, durable
control-plane reconciliation, RPO/RTO objectives, scheduled backups, key rotation,
production-scale restore, power/storage-controller failure and independent audit
remain pending. A local SIGKILL restart and isolated logical restore do not prove
those properties. No production database was touched and no real data was used.
