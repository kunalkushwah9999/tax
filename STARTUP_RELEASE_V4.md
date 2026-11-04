# TaxDesk 4.0.0 — working-paper release

3 November 2026. Full source, the runnable startup JAR, its three unchanged
component JARs, tests, deployment examples, original golden workbook and founder
handoff are included in both delivery ZIPs. Read START_HERE.md to run locally.
The archive filenames retain their existing identities; the application inside
is **4.0.0**. No ChatGPT Plus subscription, ChatGPT API key or paid model is
required to run the default application after downloading it.

This release implements three substantial preparation workflows and their
storage/UI/access controls. It does **not** complete an autonomous production
ITR/GST filing startup. Independent tax approvals remain **0/30**. Full statutory
tax implementation and external release/operating work both remain; credentials
alone will not complete them. Use synthetic data only.

## What changed

| Workflow | Implemented behavior | Completion boundary |
| --- | --- | --- |
| Capital working | Strict BUY/SELL CSV; exact micro-unit quantities and paise allocation; account/asset FIFO queues; partial-lot residuals; unmatched sales; historical purchase lineage; separate review and export | Gross entered book difference, with fees separate; taxable gain stays null. No tax classification, demat entry-date evidence, corporate actions, rates, exemptions or return schedules |
| TDS review | Strict certificate/statement observations; grouping by deductor/section/FY/income reference; duplicate/unknown/missing/mismatched fields; explicit per-group review or follow-up; covered/reviewed statement subtotal | No native-source authentication, eligible-credit decision, tax claim or income transfer |
| Notices | Eight private organizers; user-stated dates; India-calendar display; fixed task declarations; user-authored response drafts; edit/delete; unverified user-reported receipt; preparation export | No legal deadline inference, drafted legal argument, government submission or verified official receipt |
| Privacy and consistency | Owner-only endpoints; private field omitted from ordinary/shared case views; exact allowlists, CSRF and case/workbench version checks; transactional receipts; clear/delete replay protection; metadata-only audit | Manual/signed TEST preparation grants do not expand into private working papers. No production privacy/security certification |
| Cross-workflow scope | A recorded capital sale blocks the narrow salary/interest assessment even after a contradictory profile edit; ITR readiness flags capital sales/notices; ledger imports/clears reset relevant review context | Broader statutory engines are not silently substituted with narrow arithmetic |
| User interface | Capital working, TDS review and Notices tabs; examples, separate acknowledgements, review/export controls; draft markup stays text; negative amounts such as -₹0.50 retain the sign | English development interface; synthetic data only |

The application still includes saved cases, explicit income proposal/confirmation,
profile/scope/checklists, normalized source comparison, optional pinned ITR-1
structural intake, private GST observation review, sharing/revocation, preparation
notes, signed TEST expert requests, TEST commerce/entitlements/review bundles,
feedback triage, support and operational activity. There is no general chatbot.
The optional native worker does not make the source authentic or tax-correct.

## Exact arithmetic and review policy

CSV inputs are normalized observations, not arbitrary PDF/spreadsheet uploads.
Missing values use UNKNOWN and never become zero. Quantities have at most six
decimal places; money has at most two. Signs/exponents/invalid precision and
ambiguous event order are rejected. FIFO is entered-date/sequence bookkeeping
within the account/asset; it is not a universal statutory tax policy.

Cumulative floor allocation conserves entered paise across partial disposals and
remaining inventory. Unknown costs/proceeds/fees, unmatched quantities, unresolved
complex events, incomplete coverage and missing review withhold the complete
gross book aggregate. Gross difference never deducts fees automatically.

TDS matching requires unique observations on both sides and matching known gross
and TDS values. Follow-up does not erase original differences. A reviewed source
subtotal is never an eligible tax credit. Notice status is always user-declared;
even SUBMISSION_REPORTED_UNVERIFIED retains official_submission_verified=false.
See startup_app/TAX_WORKBENCH.md for exact contracts, limits and primary references.

## Persistence, access and exports

New data is stored in the existing encrypted case record; no new SQL namespace or
migration is required. The workbench retains normalized records, not duplicate
raw CSV text. Each command binds request ID, case revision and the current
workbench token. Exact retries preserve later edits/reviews and cannot resurrect
cleared or deleted data. Ownership is checked before old receipts.

The module bounds normalized workbench JSON at 128 KiB and rejects a workbench
mutation that would exceed 240,000 UTF-8 bytes for the whole case. Existing local,
legacy SQL and concurrent SQL backend limits remain. The original three
component modules and 30 golden inputs/blank expected outputs are unchanged.

Owner case export and owner TEST review bundles include working papers. Basic
owner export remains free of test-plan credits. Review-bundle credit reuse follows
the existing income input-snapshot contract: nonmaterial preparation reviews or
notice edits can appear as current preparation projections under the same input
snapshot. Such a bundle is not an immutable signed evidence artifact or a filed
return; its workbench version identifies the current private working papers.

Existing `mysql` remains serial with its aggregate 16 MiB limit. Opt-in fresh
schema-2 `mysql-concurrent` uses point/scan dependency validation and short ordered
shard locks, with remaining quota/namespace scan contention. Streaming framed
backup/restore was exercised above 16 MiB. Restore stays isolated; there is no
production promotion or proof of 100,000 concurrent users. See CONCURRENT_STORAGE.md.

## Executed verification

The following results were observed against the current 4.0.0 implementation.
These are engineering checks; **none** count as independent statutory tax
reference approval. Each backend used a separate disposable synthetic fixture.

| Current evidence | Result |
| --- | --- |
| Working-paper unit checks | 26 named groups plus 59 generated allocation-conservation cases |
| Working-paper authenticated API checks | 24/24 on local; 24/24 on legacy MySQL; 24/24 on concurrent MySQL |
| Working-paper compiled defects | 8/8 compiled variants detected through failed behavior assertions |
| Existing unit checks refreshed | Workspace 27 + 12 generated owner-isolation checks; self-service 27 + 60 generated component checks; legacy recovery 9; native policy 18; framed backup 8 |
| Existing authenticated API regression | Local 49/49; each SQL profile 49 run, 48 pass, one intentional local-vault-only skip |
| Native actual Java/Python worker API | 20/20 on each of local, legacy SQL and concurrent SQL |
| Commerce/expert/feedback API | 36/36 on each profile, including the new owner working-paper bundle check; explicit loopback payment fixture and signed TEST roster only |
| HTTPS client/proxy/backend fixture | 12/12; not a public Nginx deployment test |
| Actual MySQL storage | Legacy 20 named groups, concurrent 26; four distinct-writer rounds each; certificate-identity checks; server 8.4.13 and pinned Connector/J 26.10.0 |
| Actual SQL recovery | Crash/kill/restart, saved data and committed marker preserved, outage returns 503 and no local fallback; legacy isolated restore 9 groups; concurrent framed restore 10 including full-state equality and backup above 16 MiB |
| Actual authenticated browser | 31 flows on concurrent SQL, Chrome 156.0.8078.80 / Playwright 1.62.1; 1440×1000 and 390×844; no JS errors; one expected post-logout API 401 |
| Browser visual review | Capital desktop/mobile and notice mobile screenshots inspected; no page overflow; wide tables use their own horizontal scroller |

The browser suite also passed 31 flows on a local diagnostic run. An initial
concurrent-browser attempt timed out waiting for the paid-order display. The
harness now explicitly observes the manual billing refresh response as well as
the backend capture and rendered status; the subsequent complete concurrent run
passed. No automatic payment-create retry was added. This is bounded observed
evidence, not a general concurrency/security proof or certification.

Detected working-paper defects cover cross-account FIFO, lost residual paise,
UNKNOWN becoming zero, private projection leakage, capital sales reaching the
narrow engine, TDS observations granting a claim, a user receipt becoming
verified, and ignored stale workbench tokens. Compilation failures are not
counted as detections. The source tree was preserved during these checks.

Unit cases include quantity/amount precision, strict dates, event/period bounds,
same-day sequence, future purchases, partial/over-sales, unknowns, reviews,
versions, owner roles, stale/replayed/deleted requests, audit redaction, India
calendar boundaries and transactional rollback at the case size guard. API cases
also exercise CSRF, field injection, paid-bundle scope, Unicode draft persistence,
notice limits and private exports. They are not a substitute for broader law,
security, load, physical-mobile, Safari or accessibility evaluation.

Current JSON reports are in startup_app/build. Previous 3.0.0 report bytes are
preserved under startup_app/evidence/v3 with their original bindings. Older
component-module results stay with those unchanged modules; they were not
rebranded as current tax approvals. No new model weights or OCR benchmark were
run. The existing Hugging Face evaluation document remains a candidate plan.

## Reproduce and inspect

Run from the extracted root (Java 17+ and Python 3):

```sh
python3 startup_app/run.py
python3 startup_app/build.py
python3 startup_app/test_workbench.py
python3 startup_app/check_workbench_mutations.py
python3 startup_app/tools/release_preflight.py --mode engineering
python3 startup_app/tools/release_preflight.py --mode production
```

Stop the development server before build/operator/backup operations on its local
vault. Engineering preflight is an offline artifact/source/report integrity
check. Production mode intentionally exits 2 with the unmet gates and does not
enable a live service. Optional native, SQL and browser test commands/prerequisites
are in NATIVE_IMPORT.md, CONCURRENT_STORAGE.md, OPERATIONS.md and their test scripts.
No database, passwords, provider/signing keys, browser binaries, model weights,
runtime stores, schema-cache downloads or screenshots are bundled.

Current startup JAR: `startup_app/build/tax-platform-startup-4.0.0.jar`.
SHA-256: `3c6fe5361f3ba61c9f73d05b130f7cb7bfa0df0b3a861a5f2d538a8f704b3688`.
Package/module manifests pin the delivered files. A final extracted-ZIP smoke
record is described in DELIVERY_VERIFICATION.md.

## Remaining production work

OWNER_HANDOFF.md separates owner-supplied access/evidence from missing engineering.
The major unreleased items are approved year/Act-specific rule publication and
independent expected tax outputs; complete statutory ITR/GST/capital-gains/TDS
schedule and adjustment logic; authorized filing/acknowledgement/e-verification
adapters; live commercial billing, invoices/refunds and operating arrangements;
real CA onboarding/status/revocation and service workflows; actual hosting,
distributed session/admission controls, account recovery/MFA and security review;
measured scale, offsite/PITR drills and current deletion/revocation reconciliation
before any restored system is promoted.

No 0.001% expert-referral rate, competitor superiority, largest growth or highest
profit is proven. The application includes a usable feedback/triage loop, but no
real-customer business outcome was fabricated. This release is a larger runnable
synthetic preparation platform, not a production-complete startup.
