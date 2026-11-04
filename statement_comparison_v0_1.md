# Normalized statement comparison and local review

1 November 2026. Local app 0.3.0. This implemented workflow compares sample source
observations. It is not a native AIS/26AS/PDF importer, source-authentication
service, official feedback submission or production CA review/sign-off system.

## What changed

The English Compare statements tab accepts a bounded normalized CSV. It groups
explicit matching aliases, category, period, coverage and logical reference,
shows every source observation, and compares gross income and TDS independently.
All simultaneous missing/conflicting/repeated-source flags remain visible.

The user can select one known-gross observation with an explicit reason, reopen
the local selection or import a new batch. Agreement never auto-selects a value.
Selection changes no confirmed income, tax credit, independent reference, release
approval or filing capability. Source differences remain visible after selection.
The existing income ledger, arithmetic engine and signed tax-result guard stay
separate. There are no automatic category totals or multi-source sums.

## Normalized input contract

The exact UTF-8 CSV header is:

```csv
observation_id,source,category,payer_alias,period,coverage,reference,gross_amount,tds_amount,value_kind
```

| Field | Contract |
|---|---|
| observation_id | Unique within the import; bounded ASCII alias |
| source | MANUAL, AIS, FORM26AS, BANK or FORM16, declared by the user; not authenticated |
| category | SALARY, SAVINGS_INTEREST, DEPOSIT_INTEREST, OTHER_INTEREST or OTHER_INCOME |
| payer_alias / reference | Explicit case-sensitive synthetic aliases/logical reference; no guessed identity matching |
| period | FY2025-26 only in this starter |
| coverage | ANNUAL or TRANSACTION; different coverage is never combined |
| gross_amount / tds_amount | Nonnegative canonical INR decimal or literal UNKNOWN; zero must be explicit |
| value_kind | REPORTED; AIS-labelled observations may also be PROCESSED or ACCEPTED; no automatic authority ranking |

One to 200 observations, 32 KiB body, ten columns and 64 characters per cell.
Header order, unique observation IDs, supported codes and UTF-8 are checked.
BOM, CRLF and quoted cells are handled; multiline cells, malformed quotes,
wrong widths, invalid amounts, formulas/HTML aliases and unsupported periods
reject the entire import. Nothing is silently truncated, overwritten or dropped.
Aliases start with an ASCII letter and have at most 48 characters. Payer/reference
normalization and coverage equivalence remain explicit user responsibilities.

The sample in local_app/examples/statement_comparison.csv uses synthetic aliases
and amounts. Do not paste personal documents or identifiers. These bounds and
alias rules are not an automatic personal-data redaction or privacy certification.

## Comparison semantics

Groups use the full tuple category/payer_alias/period/coverage/reference. Equal
amounts alone never create a match. No fuzzy matching, source priority, automatic
deduplication, averaging or assumption of transaction/annual equivalence exists.

Flags are separate for gross and TDS: AGREEMENT, AMOUNT_CONFLICT, VALUE_MISSING,
SINGLE_SOURCE, REPEATED_SOURCE_OBSERVATIONS and SOURCE_VARIANTS. Repeated rows
stay visible; their flag cannot hide missing values or conflicting amounts.
Reported/processed/accepted declarations do not authenticate or supersede a row.
A missing gross in a FORM26AS-labelled row is unknown, not zero income.

A local selection can use only an observation belonging to that exact group
with a known gross amount. It records a declared review reason, not verified
evidence, a source correction, tax eligibility, a CA decision or accepted credit.
TDS discrepancies and original data remain visible. OTHER_INCOME observations
remain in comparison output and do not expand the calculator's supported scope.

## State, provenance and replay

Each session stores only the latest parsed source observations, current local
choices, at most 100 accepted request receipts and amount-free change events.
The raw CSV is not retained server-side. Receipts retain request digests and
opaque outcome versions, not old source snapshots or monetary values. A digest
is private matching metadata, not authentication or anonymization.

The batch binds the complete current ten-field income identity. An income/review
snapshot change makes comparison reads/reviews stale until reimport. Review
versions also change after a local selection. A second writer with an earlier
version cannot overwrite a newer choice. New source data clears old choices.
An exact reimport under a new request ID preserves choices when bytes and the
full income identity are unchanged.

Accepted identical retries return a receipt and request a fresh current read;
they never restore an older source batch or selection. Changed content under the
same ID conflicts. Known rejections do not become accepted receipts. Old accepted
retries remain available after the 100-request capacity is exhausted.

New routes reuse cookie/session, exact Host/Origin, CSRF, request bounds, no-store
and restricted response headers. Source identity/approval cannot be assigned
through review form fields. A session close/expiry revokes access to comparison
as well. State is temporary memory, not saved cases, SQL or a professional audit
trail. Physical memory erasure and production retention are not established.

| Route | Actual behavior |
|---|---|
| GET /api/reconciliation | Current session-owned comparison, or empty/stale status |
| POST /api/reconciliation/import | Strict normalized CSV; current input-view header and request ID |
| POST /api/reconciliation/review | Exact SELECT or REOPEN intent, current input/comparison tokens and request ID |

The response projects source-observation input values separately from tax result
output. It always has filing_ready=false, income_facts_modified=false and
source_authenticity_verified=false. None of these inputs or counts constitutes
approved numerical tax output. No source amounts are added to the tax ledger.

## Observed checks

- 38 new Java comparison groups passed, including strict parsing, unknown/zero,
  grouping dimensions, independent gross/TDS status, observation membership,
  stale identities, replay/no rollback, event redaction and bounded storage.
- 81 generated gross/TDS value-pair combinations checked independently.
- Eight two-writer rounds used actual Java threads; each accepted one review and
  rejected the other stale version. This is not SQL/distributed/load evidence.
- Four selected source defects compiled and caused behavioral failures: unknown
  amount defaulted to zero, category omitted from grouping, stale version ignored
  and selected observation ID ignored. Original Java sources were preserved.
- 60 actual HTTP groups passed: previous 46 plus fourteen new comparison groups.
  They exercise real import/review/reopen, all simultaneous flags, source/snapshot/
  session binding, exact retries, malformed input, request limits/CSRF and an
  unchanged income ledger/history/tax-result block after local selection.
- JavaScript syntax and literal HTML IDs were checked. Browser rendering/click/
  keyboard/accessibility QA remains unverified due to the managed Java preview
  runtime issue. Windows launcher execution is also unverified here.

The tax guard 0.1.4 and arithmetic workbench 0.1.0 are byte-identical to the prior
pack. Their previous 205/64-group baselines were not rerun as new comparison
proof. Both golden files remain unchanged: 30 inputs, zero approved outputs.
There is no new CA-need, tax-accuracy, production capacity or pilot claim.

## Run and next boundary

Run python3 local_app/run.py, open http://127.0.0.1:8787, select Compare statements,
load the sample and compare. Use explicit source selection/reason or reopen it.
Editing the income draft requires comparing the source again. Session data is
not saved for later. Local app build/check instructions are in START_HERE.md.

Native statement schemas, secure document ingestion, source authenticity,
coverage mapping/revisions, category aggregation and proposal-to-income binding
need their own implementation and reference cases. No AIS feedback is posted,
no e-filing portal is accessed and no credit/refund/return is submitted. Qualified
reference/rule approval, MySQL/auth/deployment, real professional workflows and
full filing remain pending.

Primary source checked 1 November 2026: [Income Tax Department AIS FAQ](https://www.incometax.gov.in/iec/foportal/help/all-topics/e-filing-services/ais%20-%20annual%20information%20statement-faqs).
The FAQ distinguishes reported/processed/accepted values and AIS/TIS from TDS/TCS
information in 26AS. It informs these separation boundaries; it does not validate
this normalized schema, authenticate user inputs or provide numerical approvals.
