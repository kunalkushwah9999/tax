> Historical 1.3 checkpoint. The current startup binary is 2.0.0; see [STARTUP_RELEASE_V2.md](STARTUP_RELEASE_V2.md) for its tests, hashes and readiness.

# Native ITR-1 structural preflight — startup_app 1.3.0

1 November 2026. Synthetic data only. The new flow is optional and disabled by
default; enable it using startup_app/NATIVE_IMPORT.md. This is not full ITR/GST
filing, legal eligibility validation or independent tax approval.

## Implemented flow

| Stage | Working behavior | Authority boundary |
|---|---|---|
| Source setup | Explicit official-schema/PDF download with exact hash/size; pinned validator dependencies; startup self-check | PDF business rules are a reference, not executed/approved rules |
| Preview | Owner-only real local Python Draft-04 validator; exact form/year/version; bounded/masked diagnostics | Structural match does not verify taxpayer/source identity, legal scope or tax arithmetic |
| Extraction | Explicit gross salary and single classified interest observations, source-total/duplicate/unsupported-pack checks | No automatic category sum/zero, profile, deduction, tax/credit/refund or filing transfer |
| User review | Same taxpayer outside preview, complete annual category/classification and exact displayed replacement | User declarations are not authenticated government/professional evidence |
| Atomic proposal | Selected fields, minimal unverified lineage and receipt commit together as ESTIMATED; new snapshot/review reset | Ordinary confirmation is a separate later action; signed tax release gates unchanged |
| Retry/privacy | Expiring owner/case/revision-bound sanitized cache; accepted replay before cache lookup; no raw-file/PAN digest retention | No new private lineage through earlier PREPARER grants; audit excludes source/amounts |
| Failure control | Real bounded subprocess, concurrent pipe handling, timeout, worker cap, fixed output projection and no remote refs | Process limits are engineering controls, not security/OS-sandbox certification |

The English UI adds ITR file check, a synthetic structural example, redacted
preview, per-category review/replacement and preview discard. It contains no
chatbot. Only accepted income facts and minimal source-schema/path metadata persist.
Raw return JSON and identity/bank/contact/tax fields are not stored or echoed.
No real identity, government login, payment, filing or professional service was used.

## Observed evidence

| Check | Result | Limit |
|---|---|---|
| Native Java policy | 18 groups passed | Binding/TTL/caps, atomicity, acknowledgements, exact replay, separate confirmation, lineage/privacy and source-snapshot staleness; unit previews are controlled fixtures |
| Real validator | 21 groups plus 28 generated rejections passed | Exact official schema with jsonschema 4.26.0; root/pattern gaps, malformed/duplicate/bounded input, unknown/zero, no summing, unsupported scope, redacted paths/values and offline validation |
| Native local API | 20 groups passed, zero failures/errors | Actual launcher -> Java -> Python -> pinned schema -> preview -> SQL/file-backed proposal workflow and separate confirmation; local encrypted profile |
| Native MySQL API | 20 groups passed, zero failures/errors | Same real worker against MySQL 8.4.13; cross-account/grant isolation, concurrent distinct-session replay, process restart and no raw source persistence |
| Process faults | Four groups passed | Actual controlled child processes: wrong self-check pin, malformed/oversized/bad-shape output, unread stdin timeout, concurrent cap and slot recovery; not schema-correctness tests |
| Selected native defects | Six compiled Java and six targeted Python defects detected | Auto-confirmation, stale/binding/replay/lineage/ack defects; ignored schema/exact-header/pin checks, multiple-row selection, raw echo and false tax-check flag; original source preserved |
| Existing HTTP | 49 local groups passed; 48 MySQL groups passed with one local-file-only skip | Three new default-disabled-route checks; zero failures/errors |
| Existing SQL/recovery | 20 SQL groups/four writer rounds, real SIGKILL restart and nine actual isolated-restore groups passed | No local fallback; restored target cannot serve old deletion/revocation state; production promotion remains unimplemented |
| Existing unit/transport/defects | 27 workspace groups/12 generated isolation checks, 27 preparation groups/60 generated component changes, eight archive groups, 12 HTTPS proxy groups, six business/six preparation/three SQL defects rerun | Original guard/engine/anonymous-app component suites were not rerun |
| Interface | JavaScript syntax and literal ID checks passed | Authenticated browser/mobile/keyboard/accessibility and independent security QA remain pending; no new visual pass claimed |

The native concurrency test uses two independently authenticated sessions, not
one serialized session. One request applies, the identical request replays, and
the saved revision advances once. Restart drops unaccepted previews while an
already committed exact command can still replay and preserve later edits.

An intentionally contradictory positive structural specimen supplies fictional
identity/bank data and zero/arbitrary tax figures. Those values are not an
independent tax reference or a fileable return. A structurally permitted supplied
tax figure is never returned or accepted as a computed result. All 30 original
golden expectations and the separate reference template remain blank/unapproved.

Executable SHA-256: `2b96a62955de9e61bf024444df3cc48dcfa504f50f74d60dcd8e200b9c7e46bf`.
Worker SHA-256: `30f22b417eaa2bcc5b5fae62b0bab0ef01c49e8d5c5c16b302cd4a8bb7a61844`.
Reports are under startup_app/build, including native_policy_test_results.json,
native_worker_test_results.json, native_http_test_results.json,
native_mysql_http_test_results.json, native_process_test_results.json,
native_mutation_results.json and native_worker_mutation_results.json. Current
build/default HTTP/SQL/HTTPS/crash/isolated-restore/defect reports are refreshed.
Counts are engineering evidence, not a tax accuracy, coverage or referral percentage.

## Primary artifacts and scope

The official ITD downloads page linked the ITR-1 AY 2026–27 main schema published
30 June 2026 and the separate validation PDF. Their bytes were fetched and the
packaged fetch helper independently reproduced the pinned hashes. The schema's
internal form/schema headers remain Ver1.0 although the source artifact is V1.1.
Exact adapter checks supplement permissive schema roots/patterns. The broader
legal form scope was not silently added to the existing narrow arithmetic engine.

| Artifact | Exact pin | Use |
|---|---|---|
| ITR-1 main schema | 148,921 bytes; SHA-256 6a0ef9f64065431879dcc724869d8455a640bcf11279d1818eb19f11448ce763 | Real Draft-04 structural validation only |
| ITD validation PDF | 555,796 bytes; SHA-256 fd0e447af6c0a6ed43c29697722c4d469f42d5e7a0bad54e21bd4600803a100a | Human reference; full business-rule execution/approval pending |

- [Official ITD downloads](https://www.incometax.gov.in/iec/foportal/downloads/income-tax-returns)
- [Official ITR-1 schema artifact](https://www.incometax.gov.in/iec/foportal/sites/default/files/2026-06/ITR-1_2026_Main_V1.1.json)
- [Official validation document](https://www.incometax.gov.in/iec/foportal/sites/default/files/2026-05/CBDT_e-Filing_ITR%201_Validation%20Rules_AY%202026-27.pdf)
- [Validator reference-resolution documentation](https://python-jsonschema.readthedocs.io/en/stable/referencing/)

The official GST download/developer endpoints returned HTTP 403. No third-party
sample schema was promoted as official. Native GSTR-2B/IMS/GSTR-1/3B support stays
unreleased; the existing normalized observation matcher still does not determine
ITC eligibility or a nil return. This source gap is explicit in source_pins.json.

## Remaining production work

Independent numerical approvals remain **0/30**. Full ITR/GST/capital-gains
support, statutory form/eligibility/business-rule packs, verified source/CA
services, government/provider filing and payments remain pending. User scope/
coverage acknowledgements cannot replace those authorities or guarantee that all
cases can bypass a professional or statutory certification.

SQL writes remain globally serialized with the 16 MiB payload cap and bounded
aggregate iteration. The query-filtered/normalized/pool-backed Spring backend,
durable sessions/control plane, measured scale, production hosting/TLS operations,
reconciled restore promotion, offsite/physical/PITR recovery and key rotation are
still pending. This release did not perform public deployment, real-data testing,
Windows/ARM/Docker testing, browser/mobile/accessibility QA or independent security
certification. The worker is limited to two processes and sanitized ephemeral
previews; no 100,000-user throughput claim is supported.

See startup_app/NATIVE_IMPORT.md for exact setup, source/version checks, limits
and reproduction. The original guard 0.1.4, engine 0.1.0 and anonymous app 0.4.0
source/JAR bytes are preserved. New official reference files, venv/wheels,
runtime keys, customer records and backups are excluded from the code archives.
