# Private tax working papers — 4.0.0

Open a sample draft, then **Capital working**, **TDS review** or **Notices**.
These modules save actual working papers in the existing encrypted case record.
They do not create statutory tax approval, a return payload or a filing receipt.
Use synthetic records only. No AI/model subscription is required.

## Capital transaction working

The exact normalized CSV header is:

```csv
record_id,account_alias,asset_alias,event_type,trade_date,sequence,quantity,gross_amount,fees
BUY_A,ACCOUNT_A,ASSET_A,BUY,2024-07-01,1,3,1000.00,3.00
SALE_A,ACCOUNT_A,ASSET_A,SELL,2025-07-15,1,1,500.00,1.00
SALE_B,ACCOUNT_A,ASSET_A,SELL,2025-08-10,1,1,550.00,1.00
```

At most 120 rows and 20,000 UTF-8 bytes. Use LF-normalized plain CSV without
quotes, embedded commas, blank records or formulas. Record IDs are unique.
Account/asset aliases use uppercase letters, digits, underscore or hyphen, start
with a letter and contain at most 32 characters. Some PAN/TAN-shaped aliases are
rejected; this is not a complete personal-data detector or identity verification.
Dates use ISO YYYY-MM-DD. SELL dates must be in FY2025-26; historical BUY dates
from 1900 are accepted. The triple account/asset/date plus sequence must be unique.
Sequence is 1–999999. Only BUY and SELL are supported.

Quantities are positive, at most 999999999.999999. Enter explicit gross INR
amounts and fees with at most two decimals, up to 999999999.99, or UNKNOWN.
Blank is never zero. Sale amounts describe total gross proceeds for the whole
entered quantity, not per-unit prices. Purchase amounts describe total gross
acquisition value for the entered lot. Enter fees separately.

Records are sorted by entered date/sequence; FIFO queues are isolated by account
and asset. A later purchase cannot fill an earlier short sale. Each allocation
keeps buy/sale record IDs and entered dates. For total value V paise, quantity Q
micro-units, already allocated B and next quantity M, the allocated value is:

`floor(V*(B+M)/Q) - floor(V*B/Q)`

Integer multiplication uses BigInteger. This conserves entered paise across
partial sales and the remaining lot. Gross book difference is gross allocated
proceeds minus gross allocated acquisition value. **Fees are displayed separately
and never deducted automatically.** Negative sub-rupee amounts retain their sign.
Unmatched quantities retain unallocated proceeds/fees and block the complete
aggregate. Unknown matched costs/proceeds/fees, incomplete coverage, unresolved
complex events, no records or missing separate review also withhold the aggregate.

Importing replaces the ledger and clears its review. Review acknowledges the
entered bookkeeping working; it cannot determine taxable gain. The aggregate is
not a statutory capital-gains computation. There is no asset/tax classification,
demat credit-date lineage, corporate-action/transfer handling, ST/LT treatment,
FX, STT, grandfathering, exemptions, indexation, loss adjustment or ITR schedule.
Do not apply entered trade-date ordering as a universal tax FIFO rule.

A recorded capital sale blocks the narrow salary/interest assessment even when
a subsequent profile declaration says special-rate income is absent. Capital
import/clear resets the input snapshot, review checkpoints and profile intent;
it never fills income facts. Broader statutory working is still required.

## TDS observation review

Both normalized CSV files use this exact header:

```csv
record_id,deductor_alias,section_code,financial_year,income_ref,gross_amount,tds_amount
CERT_A,EMPLOYER_A,192,FY2025-26,SALARY_TOTAL,800000.00,40000.00
```

At most 80 rows/20,000 UTF-8 bytes per side, using the same decimal rules.
The supported year is FY2025-26. Certificate and statement sides are manually
normalized observations, not authenticated native Form16/26AS/AIS imports.
Deductor and income-reference aliases must agree exactly. Section codes are
user labels, not verified legal classifications.

Grouping uses deductor + section + financial year + income reference. A match
requires exactly one known gross/TDS observation on each side and equality of
both amounts. Missing sides, unknown amounts, duplicate keys and gross/TDS
differences remain explicit. Duplicates are never silently summed or discarded.
Only matching groups allow REVIEWED_OBSERVATIONS; other groups allow follow-up,
document/timing check or NO_CLAIM_REVIEW. Recording follow-up does not erase a
discrepancy. Reimport creates new group identities and drops prior decisions.

Only complete declared coverage, nonempty groups, no issues and explicit review
of every group release a **statement observation subtotal**. Claim amount stays
null and tax-credit eligibility stays NOT_DETERMINED. Nothing is transferred to
income or an ITR. This is not the government's tax-credit mismatch service.

## Notice-response organizer

Up to eight private notices per case. Each has a synthetic alias, user-stated
category/section label, received date, stated due date or UNKNOWN, four fixed
preparation tasks, a user-authored draft of at most 3,000 Java string characters,
and optional user-reported receipt/date. The current calendar date uses
Asia/Kolkata. Days to the entered due date do not determine a legal deadline.
An entered due date earlier than receipt is shown for checking, not rejected or
silently extended. No notices or messages are sent to any person or authority.

Statuses are OPEN, DOCUMENTS_PREPARING, RESPONSE_DRAFTED,
SUBMISSION_REPORTED_UNVERIFIED and CLOSED_BY_USER. A drafted status needs text.
Reported submission requires an alias/date and explicit user source-check
declaration. Every status retains official_submission_verified=false. Checklist
items are user declarations. Draft markup is rendered as text, never HTML.
The platform does not generate legal arguments or determine service, limitation,
extensions, acceptance, disposal or the correctness of a proposed response.

## API, concurrency and privacy

| Route | Scope |
| --- | --- |
| GET /api/cases/{id}/workbench | Owner-private current projection |
| POST /api/cases/{id}/workbench | Owner, same-origin CSRF, exact operation fields |
| GET /api/cases/{id}/workbench-export | Owner preparation JSON attachment |

Each command includes operation, request_id, expected_revision and
workbench_version (empty before the first import). Every accepted operation
changes the workbench token and case revision. Invalid/stale commands roll back.
Exact accepted retries return a receipt without rewriting later state. Changed
bodies using an old request ID conflict. Ownership is checked before receipts,
so retained retries cannot resurrect a deleted case. Clear/delete remove current
records; old receipts retain body digests, not CSV/draft contents.

Operations: CAPITAL_IMPORT, CAPITAL_REVIEW, CAPITAL_CLEAR; TDS_IMPORT, TDS_REVIEW,
TDS_CLEAR; NOTICE_SAVE, NOTICE_DELETE. Inspect the bundled UI for exact payloads
or TaxWorkbench.validateFields for the full allowlists. No client approval,
claim, role or filing field is accepted.

Ordinary case projections omit the workbench entirely. Existing manual and signed
TEST expert grants do not include it. Other customers, PREPARER and ADMIN cannot
read/write/export these private records. Owner case export and owner test review
bundles include the preparation projection. Case deletion also removes current
working papers. Audit contains operation/resource metadata, never draft text,
aliases, financial values or CSV. Private download files need owner custody.

Persisted workbench JSON is bounded at 128 KiB; a workbench mutation rejects a
resulting case above 240,000 UTF-8 bytes before saving/receipting. Raw CSV copies
are not retained in this module. Existing storage/backend limits also apply.
No new database namespace or schema migration is needed. Restored backups remain
isolated under the existing policy; this feature does not authorize promotion.

## Verification and references

26 named unit groups plus 59 generated conservation cases; 24 authenticated API
groups on each of local, legacy MySQL and concurrent MySQL; eight compiled
behavioral defects detected. The final browser suite exercises real UI/API flows
on concurrent MySQL at desktop and mobile viewport sizes. See the current release
report for exact artifacts. None of these are independent tax reference approvals.

Primary references checked 3 November 2026 explain the separation from official
tax/notice procedures; they are not implemented statutory rule packs:

- https://www.incometax.gov.in/iec/foportal/help/all-topics/e-filing-services/tax-credit-mismatch
- https://www.incometax.gov.in/iec/foportal/help/how-to-manage-tax-credit-mismatch
- https://www.incometax.gov.in/iec/foportal/help/respond-to-e-proceedings
- https://www.incometaxindia.gov.in/en/sale-of-shares
