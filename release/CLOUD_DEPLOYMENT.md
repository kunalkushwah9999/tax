# Google Cloud VM deployment runbook

This is a **single Java service synthetic SQL pilot** deployment path. No cloud
resources have been created or tested. Provision a Google Compute Engine Linux VM
and a separately operated **MySQL 8.4** endpoint with server identity-verifying
TLS. Choose the owner's project, region, budget, private networking, domain, TLS
issuer and operating responsibilities before executing account-specific commands.
No default project, private credentials or cost estimate is fabricated here.

Keep this service on a VM with an explicit database until a separate managed
container architecture is implemented and validated. The Java listener binds to
127.0.0.1; Cloud Run ingress requires 0.0.0.0 and its writable filesystem is
ephemeral. Uploading this JAR with a local vault to Cloud Run is not a durable
deployment. This package does not silently weaken those assumptions.

## Provision and stage

1. Provision a maintained Linux VM with Java 17+, Python 3.10+, Nginx and a
   dedicated non-login `taxdesk` service user. Restrict SSH through the owner's
   normal administration path; only Nginx receives public HTTP/HTTPS traffic.
   Port 8788 and MySQL must not be public. Use an operator-approved persistent
   storage/backup plan; no VM-local key or backup is the sole recovery copy.
2. Verify the extracted code using `python3 manage.py doctor`. Install the verified
   project read-only at `/opt/taxdesk`. Obtain/verify MySQL Connector/J 26.10.0
   separately at `/opt/taxdesk/vendor/mysql-connector-j-26.10.0.jar`. It is not in
   the ZIP. Follow `startup_app/deploy/MYSQL_SETUP.md` and
   `startup_app/CONCURRENT_STORAGE.md` for the actual schema and runtime grants.
3. Create `/etc/taxdesk/mysql-runtime.json` from the provided strict-field example,
   a PKCS12 trust store covering the actual database hostname, and a separate
   32-byte record encryption key at `/etc/taxdesk/record.key`. Use private service
   ownership/modes. Never put passwords or private keys in the generated web root.
   This service uses **mysql-concurrent, schema 2**; initialize a **fresh** target
   with the matching tool and fresh key. Do not point it at legacy schema 1 or
   change a live database mode without a separately reviewed migration.
4. Run `release/prepare.py` with the actual origin/app identity. Install only its
   **generated `web/`** contents at `/opt/taxdesk-site/web`, readable by Nginx.
   Never serve the full source ZIP, generated bundle root, database config or
   `/opt/taxdesk` as the public document root. Private runtime files stay outside.
5. Install the generated `cloud/taxdesk.service` at
   `/etc/systemd/system/taxdesk.service`, `cloud/taxdesk-proxy.conf` at
   `/etc/nginx/taxdesk-proxy.conf`, and `cloud/nginx.conf` as the Nginx site config.
   Ensure normal Nginx MIME types are loaded. Install the actual certificate/key
   at the configured `/etc/taxdesk/tls/` paths and arrange renewal. Remove or
   reconcile default/other conflicting sites under the owner's change process.
6. Before service start, create a synthetic operator through the supplied CLI
   with the exact SQL profile/config/key as the service. Use a private terminal;
   no fixed password is seeded. HTTPS ADMIN/PREPARER access requires MFA enrollment.

The config pins the backend to loopback, uses the current **5.1.0 JAR**, preserves
same-origin CSRF/Secure-cookie behaviour and has no proxy caching. The native ITR
structural-preview route accepts at most **400,000 bytes**; other API bodies are
limited to **65,536 bytes**, matching the Java parser. TLS configuration syntax,
file permissions, database identity/grants and actual renewal must be tested on
the target host. The generated web CSP supports the local fake TEST checkout;
external Razorpay browser checkout is deliberately not enabled by this CSP and
needs a reviewed configuration if used in an authorized TEST deployment.

## Target-host validation and operation

```sh
sudo nginx -t
sudo systemd-analyze verify /etc/systemd/system/taxdesk.service
sudo systemctl daemon-reload
sudo systemctl start taxdesk
sudo systemctl reload nginx
sudo systemctl status taxdesk --no-pager
```

On the **actual configured HTTPS domain**, verify health reports `production_ready:
false`, hostname and Origin rejection, Secure/HttpOnly/SameSite cookies, CSRF,
operator MFA, customer login, private exports, desktop/mobile UI, native preview
size limits, logout, certificate renewal and offline Cache Storage boundaries.
Check Android Digital Asset Links from the eventual Play-installed app. Never
open SQL/8788 to solve a reverse-proxy issue. Configure probes/alerts and review
logs without copying customer payloads, passwords, seeds or recovery codes.

Before charging real users, independently validate the full supported statutory
scope and all release gates in OWNER_HANDOFF.md. Payments, filing connections,
real CA verification, operator/payment-account and external/backup deletion, recovery promotion and measured
production scale remain unfinished engineering/review work. Current processes
have local sessions/admission controls and known bounded namespace scans. Adding
VM replicas is not a completed multi-node public architecture.

Follow `startup_app/deploy/BACKUP_RESTORE.md` and
`startup_app/CONCURRENT_STORAGE.md` for encrypted backups. Prove offsite restore and
reconcile current deletion/revocation/MFA controls **before any promotion**; the
current restore stays isolated. A VM snapshot alone is not that proof. Roll back
only to a retained verified artifact compatible with the existing DB schema; do
not overwrite data or rotate/discard the record key as a rollback shortcut.

Primary platform contracts:
https://docs.cloud.google.com/run/docs/container-contract and
https://docs.cloud.google.com/run/docs/developing (checked 2026-11-03).
