# Listed-equity calculation progress — review module 0.1

**This is a standalone reviewer/developer module, not a completed public tax
feature.** The main browser application remains 5.1.0; public numerical tax
release and filing remain blocked. Original independent approvals remain **0/30**.
The new, separate equity review corpus is **0/12**, with expectations blank.

## Implemented

`statutory_review/equity.py` calculates candidate matched-lot capital-gains
workings for the pinned FY 2025–26 / AY 2026–27 scope: listed Indian equity,
ROR individual, new regime, checked ordinary taxable income of at least ₹4 lakh,
complete portfolio declarations and no gifts/corporate actions/prior-year losses.

The code includes STT-condition checks, short/long holding candidates, pre-2018
cost-basis treatment, a shared annual 112A rate band and current-year short/long
loss set-off directions. Missing costs/fees/STT/FMV, incomplete declarations,
unsupported status/regime/year, intraday questions and exact holding anniversaries
block all aggregate workings. Original browser FIFO differences are not silently
treated as taxable gains.

Matched lots and allocated costs/FMV/expenses must be independently reconstructed
and checked. This module does not authenticate broker data, match quantities,
deduplicate trades under different IDs or prove the declared legal classification.
It does not compute annual slab tax, rebate/marginal relief, surcharge/cess,
annual rounding, credits/interest, carry-forward eligibility or file an ITR.
The result is **CALCULATED_UNAPPROVED**, never an approved tax reference.

## Run locally

Python 3.10+ standard library; no ChatGPT subscription, model or external API:

```sh
python3 statutory_review/test_equity.py
python3 statutory_review/check_mutations.py
python3 statutory_review/equity.py --input statutory_review/sample_input.json --out /PRIVATE/NEW_WORKINGS.json
```

Use a new output path. The file is private (0600 on POSIX), never overwritten,
and includes input/code/source-manifest hashes for review provenance. Numeric
workings stay in that local review file. Invalid input creates no output; a
blocked case returns `workings:null`. The original golden JSON/XLSX is untouched.

## Validation actually performed

- **20 engineering test groups passed**, covering scoped rates/thresholds, cost
  treatment, loss direction, missing facts, boundaries and strict schema/input.
- **200 generated integer-paise conservation/order checks** passed.
- **10/10 deliberately injected defects detected**, including incorrect rates,
  a duplicated annual band, wrong loss directions, cost-basis errors, unknown-as-zero
  and accidentally enabling filing authority.
- Actual CLI/private-output/non-overwrite behaviour passed. The delivered package
  is checked from clean extraction; the existing 5.1.0 runtime/doctor remains intact.

These are developer-authored engineering checks. They are **not** independent
tax approvals, official utility execution or full return coverage. Reviewed primary
references/access limitations are recorded in `statutory_review/rule_sources.json`.

## Still required

Independent case expectations/rule review; complete statutory ITR/GST/capital-gains
coverage; reviewed integration into the authenticated application and its release
controls; authorized live filing/payments; verified real CA services; remaining
privacy/recovery; production hosting/scale/security; signed/device-tested Android
release. No live service was connected or published. A 0.001% CA rate and growth/
competitor claims remain unproven. Continue to use synthetic data.

For exact input definitions and exclusions, read `statutory_review/README.md`.
