# Self-service preparation and isolated recovery — startup_app 1.2.0

1 November 2026. Synthetic data only. This is implemented development/pilot code,
not independent tax approval, full ITR/GST filing or a production launch.

## Delivered changes

| Area | Working behavior | Boundary |
|---|---|---|
| ITR preparation | 14 YES/NO/UNKNOWN declarations, draft-bound scope review and per-input/profile/inventory/document next tasks | FY 2025–26 / AY 2026–27 narrow salary/ordinary-interest preparation; declaration check, not statutory form selection or legal/CA requirement determination |
| GST reconciliation | Exact normalized book/statement comparison; duplicate/missing/unknown/date/tax-component issues; RCM/amendment/availability flags; all source rows visible | Synthetic aliases, not native GSTIN/GSTR-2B/IMS ingestion; no legal credit/rate/return-table eligibility |
| GST review | Explicit current-version/group-bound match or follow-up, coverage declarations, review reset on source replacement, owner JSON export and clear | No discrepancy override; follow-up leaves issues open; no ITC claim amount or nil-return decision |
| Privacy and replay | New tools owner-only; earlier preparation grants do not broaden; amount-free audit; exact command receipt, stale version and no-resurrection behavior | Existing process-local sessions/bounds and pending independent security/privacy review remain |
| Logical backup | Operator CLI takes one consistent business snapshot, encrypts with a separate key/fresh nonce, refuses overwrite, authenticates offline without printing inputs | 24 MiB snapshot limit; local profile must be stopped; not physical MySQL/server/grant/config/PITR/offsite recovery |
| MySQL restore | Actual fresh database and fresh record key; ISOLATED before records load; exact snapshot equality; normal server/init/overwrite denied | No live restore promotion; latest deletion/revocation/control state still requires reconciliation |

See [startup_app/SELF_SERVICE.md](startup_app/SELF_SERVICE.md),
[startup_app/deploy/BACKUP_RESTORE.md](startup_app/deploy/BACKUP_RESTORE.md) and
[startup_app/deploy/MYSQL_SETUP.md](startup_app/deploy/MYSQL_SETUP.md).
The new customer views are English and contain no general-purpose chatbot.
Only latest GST sources/review state are retained. No real taxpayer/provider
credentials, keys, runtime databases, backups or vendor binaries are packaged.

## Observed validation

| Check | Result | Evidence/limit |
|---|---|---|
| Existing workspace | 27 groups plus 12 generated ownership checks passed | Local unit suite, rerun with current source |
| New preparation | 27 groups plus 60 generated component discrepancies passed | Identity/FY/punctuation/case, zero/unknown, duplicate/date/precision, scope staleness, owner/grant separation, replay/reset/clear/export, no ITC/filing authority |
| Archive | 8 groups passed | Consistent logical equality, ciphertext, new nonce, no overwrite, wrong/missing key, tamper/truncation and isolated-only policy |
| Local HTTP | 46 groups passed, zero failures/errors/skips | Real launcher/server/authentication, including ten new preparation route groups |
| SQL contract | 20 groups plus four concurrent distinct-writer rounds passed | MySQL 8.4.13 / pinned Connector-J 26.10.0, verified TLS, atomic receipts, rollback, expected versions, replay and AAD |
| SQL API | 45 passed; one local-vault-byte test skipped; zero failures/errors | Same 46-group HTTP suite against real MySQL; ciphertext checked separately by SQL suite |
| SQL logical recovery | 9 groups passed | Actual CLI backup/verification/restore, consistent business equality, source deletion/revocation after snapshot, fresh isolated target and refused normal startup/init/overwrite |
| SQL crash | mysqld SIGKILL/restart passed | Committed marker/cases persisted, API 503 during outage, no local fallback; power loss remains untested |
| HTTPS API | 12 groups passed | Actual authenticated TLS proxy fixture with explicitly trusted test CA; not browser/Nginx/public deployment |
| Compiled defects | Six business, six new preparation and three SQL defects detected | Independent source copies compiled and failed behavioral assertions; original sources preserved |
| Extracted package | Starts without rebuilding; ITR scope/GST example review/export, source confirmation, actual blocked dual engine, restart and local backup/offline verification passed | Both ZIP manifests verified; no runtime data/keys/archives/vendor binaries packaged |
| Interface | JavaScript syntax and literal HTML-ID checks passed | Existing static landing inspection is prior evidence. New authenticated browser/mobile/keyboard/accessibility QA remains pending; browser download produced an invalid/truncated archive |

The real concurrency suite caught a restore-state read that established an MVCC
snapshot before the database write guard. The write guard now precedes the
consistent recovery-state read, preserving current-state stale-edit/replay
semantics under independent connections. The same actual SQL tests now pass.

The archive test restores a snapshot from before source case deletion/access
revocation. Those old records exist in the isolated target for recovery
inspection, but normal service startup refuses it. There is no automatic or
manual-via-API promotion path; a reviewed current-control reconciliation service
is still missing. The SQL recovery marker is operator-controlled, not an
independent immutable authority or defense against a privileged malicious operator.

Current executable SHA-256: `4280ad0ec6a03ef40e37c5ca520726ca196fc3c17da43d92102270437693acfe`.
Reports are in startup_app/build, including self_service_test_results.json,
recovery_unit_test_results.json, mysql_restore_test_results.json,
self_service_mutation_results.json and the refreshed build/local/SQL/HTTPS/crash/
mutation/interface/package reports. The combined mysql_recovery_results report
identifies backup_restore_kind=LOGICAL_ISOLATED_ONLY and unreconciled production
restore. Counts do not establish tax correctness, legal coverage, security
certification or a capacity percentage.

The guard 0.1.4, private engine 0.1.0 and anonymous app 0.4.0 source/JAR bytes are
unchanged; their earlier suites were not rerun or counted as new tests. Both
original golden files and the separate blank reference template are preserved.
Independent approvals remain **0/30**; no expected value was filled from our engine.

## Production and self-service boundary

A taxpayer can use the new preparation/checking flows without creating a CA or
PREPARER account. This is a product workflow property, not a claim that every
return can legally or safely bypass professional/statutory requirements. Unknown
scope, uncovered schedules, missing evidence, legal certification and failed
validation must remain explicit. Statutory form choice, approved calculation,
reconciliation/eligibility and provider submission/e-verification are separate
release steps. We do not claim a 0.0001% referral rate or superiority over CAs
or any competitor.

SQL still serializes all business writes, scans some bounded collections, caps
encrypted payload at 16 MiB and opens per-operation connections. Sessions/rate
limits/workers remain process-local. The normalized/query-filtered/pool-backed
Spring repository, multi-node control plane and measured production scale are
pending. The global lock was preserved rather than removed without a repository
and authorization/concurrency protocol.

Production hosting/certificates/proxy validation, authenticated browser/mobile/
accessibility and independent security QA, recovery promotion/current-control
reconciliation, physical/offsite/PITR backups, key rotation and RPO/RTO validation
remain pending. Real identity/recovery/MFA, verified CA services, checkout/
invoicing/refunds, native ITR/AIS/26AS/GSTR-2B/IMS ingestion, official schema/
validation packs and filing connections remain unreleased. Full ITR, GST and
capital-gains support is not delivered. No tax figures or provider return file
were released by this change.

## Sources and reproduction

The preparation/gating design preserves the separation between source observation,
legal validation and taxpayer/provider filing. Primary documentation reviewed:

- [ITD offline utility FAQs](https://www.incometax.gov.in/iec/foportal/help/offline-utility-faq)
- [ITD return/form downloads and period-specific schemas](https://www.incometax.gov.in/iec/foportal/downloads/income-tax-returns)
- [GST portal GSTR-2B FAQs](https://tutorial.gst.gov.in/userguide/returns/FAQ_gstr2b.htm) — indexed official content retrieved; direct page fetch returned 404 in this environment, so it is not a pinned native-schema release source
- [GST Returns Offline Tool](https://tutorial.gst.gov.in/downloads/invoiceuploadofflineutility.pdf)

No broad legal rule or native return schema was inferred from the normalized CSV.
Future native adapters must pin actual official period/form/schema/validation
artifacts and independently validate them before release. Start with
`python3 startup_app/run.py`. Rebuild/default checks are in START_HERE.md; real
MySQL/driver/TLS prerequisites and the disposable fixture command are in
MYSQL_SETUP.md. No Docker/Windows/public deployment was exercised.
