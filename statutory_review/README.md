# Listed-equity statutory review module 0.1

**Standalone engineering/reviewer CLI. It is not connected to the main browser
application or its public result/filing flow. Rules remain unapproved.** Main
application stays 5.1.0 with 0/30 independent salary/interest approvals. This
additional equity corpus is separately 0/12. Synthetic data only.

This implements a bounded part of the previously missing capital-gains arithmetic;
it does not release full capital-gains, ITR or GST support. Running more internal
tests does not replace qualified independent review or official utility evidence.

## Run

Python 3.10+ standard library only; no ChatGPT subscription or paid API is used.
From the extracted project root, choose a **new private output path**:

```sh
python3 statutory_review/test_equity.py
python3 statutory_review/check_mutations.py
python3 statutory_review/equity.py --input statutory_review/sample_input.json --out /PRIVATE/NEW_EQUITY_WORKINGS.json
```

The CLI emits a status on the console and places numeric working stages only in
the requested reviewer file. On POSIX the output is created exclusively with mode
0600; existing files are never overwritten. Protect the folder/ACL on your own
computer. Exit 0 means **CALCULATED_UNAPPROVED**, not filing-ready. Blocked scope
returns exit 2 and `workings:null`; invalid input returns exit 2 without an output.
The original golden sheet/JSON is neither edited nor filled from this engine.

## Supported candidate scope

FY 2025–26 / AY 2026–27 under the Income-tax Act 1961; ROR individual; new regime;
normal listed equity on a recognised Indian exchange, held as capital investment.
All account lots and ordinary taxable income after deductions must be checked.
Ordinary taxable income must be at least ₹4,00,000 so unused basic-exemption
allocation is outside this module. Each lot must explicitly be
`INDIA_LISTED_EQUITY_SHARE`; bonds, mutual funds and VDA are rejected. IFSC/STT
exceptions, clubbing/special status, exemptions/reinvestment, gifts/corporate
actions, other capital transactions and brought-forward losses are excluded.
Every scope declaration
must be explicit; false or unknown material facts suppress all computed totals.

Inputs are **already matched lot allocations**, not an unprocessed broker trade
file. Use the complete account-specific FIFO source reconstruction, then review
each allocation. This CLI does not itself verify broker authenticity, quantities,
duplicate trades under different IDs, matching, listing status or source coverage.
The existing browser FIFO book difference must not be copied in as taxable gain.

Each lot uses allocated cost/consideration/transfer expenses for that matched
portion. Purchase cost includes appropriate acquisition costs, excludes STT, and
must already be checked. Transfer expenses exclude STT and must be legally
allowable transfer expenses. The module trusts these **declared facts**; it does
not infer them from an undifferentiated broker-fees total. For older lots the FMV
input is the value for the **entire matched lot**, not a per-share price.

## Implemented workings

- Holding-period candidates use a calendar anniversary. Exact twelve-month
  anniversaries, leap-date boundary conventions and intraday/business questions
  require independent date/treatment review rather than an automatic result.
- Section 111A candidate component uses 20% for in-scope FY transfers, with
  transfer STT confirmed. Source: `S111A_2025` in rule_sources.json.
- Section 112A candidate component uses 12.5% on the post-set-off aggregate above
  ₹1,25,000. The annual band is shared across lots/accounts; equity acquisition
  and transfer STT must be confirmed. Exceptions are not inferred. Source: `S112A_2025`.
- Pre-February-2018 cost candidates use the greater of actual cost and the lesser
  of the checked January-2018 FMV and sale consideration. Special listing/FMV
  circumstances require review. Sources: `S55_2025`, `S55_MEMO`.
- Current-year ST losses are netted within ST first; a residual can reduce net
  positive LT. LT losses reduce LT, never ST. Unabsorbed losses are reported with
  **carry-forward eligibility undetermined**. Source: `S70_2025`.

These are components **before annual rounding, surcharge and cess**, not annual
tax. The model omits ordinary slab tax, total-income integration, 87A rebate and
marginal relief, surcharge, cess, 288A/288B, TDS/advance tax, interest/late fees,
return eligibility/schedules and filing. A fractional component is not a payable
tax amount. Money uses integer paise/Decimal; no binary floating-point arithmetic.

## Review and tests

`review_cases_v0.json` has twelve extra synthetic inputs with **all independent
expectations blank**. A reviewer must use their own authoritative reference or
official utility, record version/evidence/date, and resolve the blocked cases.
Do not approve a case by copying this module's outputs. The source manifest
records retrieved primary references and access limitations; it is not a signed
legal rule release or proof that all applicable law has been modeled.

Engineering evidence: 20 groups, including 200 generated paise-conservation/order
checks; ten deliberately injected defects detected in isolated temporary copies.
Tests cover rates, a single annual band, loss direction, pre-2018 costs, unknown
facts, STT, unsupported scope, strict input/duplicate keys, private file creation
and non-overwrite behaviour. They are developer-authored checks, **not 20/200/10
independent approvals**. See engineering_test_results.json and mutation_results.json.

The main application's current source/JAR and existing test evidence are preserved.
Public result/filing remain blocked. Full statutory implementation, independent
approval, real CA/provider connections, remaining privacy fulfillment, production
cloud validation and signed Android release are still unfinished.
