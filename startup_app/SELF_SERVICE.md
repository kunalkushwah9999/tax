# ITR preparation and GST observation review — 1.2.0

Use synthetic information only. These flows prepare and organize declared data;
they do not require a PREPARER account. They do not determine all circumstances
in which professional or statutory certification is needed. Full ITR/GST filing,
CA verification, independent numerical release and native document ingestion
remain unavailable.

## ITR preparation

Create/open a draft, prepare and confirm each income field, complete the profile
and inventory/document checkpoints, then open ITR preparation. Four positive
scope questions and ten exclusion questions each keep YES/NO/UNKNOWN distinct.
Review and save declarations for the current draft. Material inputs/profile,
document or source-review changes make that scope review stale. Exact old retries
cannot mark a later draft reviewed. Uncovered scope routes to an expanded workflow,
not automatic approval or a claim that every such case requires a CA.

The scope here is preparation for FY 2025–26 / AY 2026–27, individual/adult-under-60
ROR salary/ordinary interest within the declared pack. The task checklist is
based on declarations; it does not authenticate evidence or validate statutory
ITR-1 eligibility, legal deduction eligibility, return type or tax correctness.
The fixed release blockers show independent reference/rule approval, statutory
form eligibility and the missing filing provider. No paid/admin switch clears them.

## GST normalized observations

Open GST reconciliation and load the synthetic example, or use examples/gst_books.csv
and examples/gst_statement.csv. These are **not government CSV/JSON schemas**.
They contain supplier aliases, not GSTIN/PAN. Map only synthetic aliases. Do not
paste real GSTINs, bank records, PAN, government credentials or private tax documents.
Header (exact order):

```csv
supplier_ref,document_type,document_no,document_date,taxable,igst,cgst,sgst,cess,reverse_charge,statement_itc
```

| Field | Accepted values / semantics |
|---|---|
| supplier_ref | Uppercase synthetic alias, 2–32 letters/digits/underscores, first a letter; no identity or registration verification |
| document_type | INVOICE, CREDIT_NOTE, DEBIT_NOTE, AMENDMENT; amendments remain unsupported linkage/review issues |
| document_no | Exact case-sensitive identifier of 1–32 ASCII letters/digits/slashes/hyphens, first alphanumeric; no fuzzy punctuation removal |
| document_date | Exact valid ISO date, 2000–2099; the April–March financial-year key separates repeated numbers |
| taxable/igst/cgst/sgst/cess | Nonnegative exact rupees with at most 2 decimals and 9 whole digits, or explicit UNKNOWN; no blank/exponent/sign/rounding |
| reverse_charge | YES, NO or UNKNOWN; YES/UNKNOWN require review, not automated liability/credit calculation |
| statement_itc | Books: NOT_APPLICABLE. Statement: AVAILABLE, NOT_AVAILABLE or UNKNOWN, preserving the declared source flag only |

At most 100 rows and 20,000 UTF-8 bytes per file; exact header-only files explicitly
represent zero observations. Empty/malformed headers do not silently become zero
records. This does not determine a nil return. LF and CRLF are accepted; quotes,
embedded commas/newlines/tabs in fields are outside this normalized format.

Invoice identity combines alias, derived financial year, type and exact number.
Date and every amount/tax component remain separately compared. Identical duplicate
rows are preserved and flagged; they are never silently deduplicated or summed.
Credit/debit notes are not netted or assigned return tables. Missing-side entries,
unknowns, RCM, mixed components, amendments and unavailable/unknown statement flags
remain visible issues. Matching does not determine legal ITC or source authenticity.
The period is a declared comparison period, not verified return-period eligibility.

Import replaces the whole current worksheet and clears earlier reviews. It accepts
separate same-entity/books-coverage/statement-coverage declarations. Every review
binds to the current worksheet version and exact group ID, with explicit human
acknowledgement. Mismatches cannot use REVIEWED_MATCH. Follow-up/no-claim notes
leave issues open; fix and reimport the source instead of overriding a discrepancy.
Even all matched/reviewed observations return credit_eligibility=NOT_DETERMINED,
itc_claim_amount=null and filing_available=false. No statutory/rate/credit eligibility
rule pack is shipped as approved.

## Ownership, retention and export

New ITR scope and GST worksheets are owner-only. Existing case-sharing consent
for preparation comments does not extend to these datasets; ADMIN has no override.
Ordinary case projections hide the raw preparation container. Owner preparation
export includes it; GST export is explicitly GST_PREPARATION_REVIEW_NOT_A_RETURN.
Only the latest worksheet sources/review map are retained, not duplicate historical
raw files. Clear removes the latest GST sources; reimport resets decisions; case
deletion removes its active preparation data. Amount-free receipts/audit remain
bounded, and old accepted retries cannot resurrect cleared data. Backups have
separate retention/recovery requirements; physical erasure is not established.

The browser views are implemented and JavaScript/static ID checks pass. Actual
owner-only API/SQL behavior is tested. Authenticated browser/mobile/keyboard and
accessibility QA remain pending because a usable headless browser download failed.
