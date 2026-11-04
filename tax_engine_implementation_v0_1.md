# Arithmetic engine, verifier and workbench integration

1 November 2026. New arithmetic workbench 0.1.0; local application 0.2.0;
result-safety core remains byte-identical at 0.1.4. This is implemented local
engineering code, not completion or release of the full production tax/CA platform.

The earlier preparation-only app now has actual arithmetic behind a scoped
planning-readiness API, a separate private operator CLI, differential stage
verification, comparison/sensitivity and an independent-reference collection tool.
No original calculator repository was patched: this is new implementation.

## Implemented arithmetic and scope

| Concern | Implemented behavior |
|---|---|
| Period | Explicit ITA 1961 / FY 2025–26 / AY 2026–27; new-Act tax year is rejected |
| Eligibility | Individual ROR, adult throughout FY; confirmed DOB, income and screening required; product scope is separate from ITR eligibility |
| Salary | Sum up to sixteen declared employer totals; standard deduction once, capped at salary, with different old/new caps |
| Interest | Separate eligible savings, qualifying deposit and other ordinary interest; no salary deduction against interest |
| Old interest deductions | 80TTA applies only to qualifying savings; resident senior 80TTB includes qualifying savings/deposits, excluding other interest; no double TTA/TTB |
| Other deductions | Already eligible, distinct 80C, 80D and 80CCD(1B) facts; section caps and aggregate GTI limit; 80D premium/eligibility calculation is not implemented |
| Age | Separate under-60/senior/super-senior slabs; next 1 April 60/80 birthday counted for preceding FY; no current-date age substitution |
| Tax stages | 20 exact decimal stages, separate old/new rebate, new marginal relief before cess; total-income rounding ignores paise first |
| Supported cap | Rounded total income after relevant deductions, per regime, not gross salary; above 50 lakh is unsupported |
| Comparison | Private exact annual-tax arithmetic, actual ties and unavailable/partial cases; legal election availability not established |
| Sensitivity | Private weak/strict boundaries in paise over declared additional eligible 80C capacity, never arbitrary deductions or a spending instruction |

The grouped scope screening covers business/professional income, gains/VDA/
special-rate income, property/agriculture/foreign income, restricted statuses,
losses, family pension, salary adjustments, employer NPS and other unmodelled
income/deductions. Every answer must be known; a selected exclusion rejects the
starter. Product exclusions do not assert that the income is legally uncomputable
or that a particular ITR form is universally unavailable.

80D is a precomputed eligible amount with an aggregate maximum check, not a
premium/expense input. Family/parent limits, payment conditions, no double
claiming and actual evidence remain review dependencies. 80CCC/80CCD(1), employer
NPS, other deductions and the broader schedules are outside this implementation.

## Two implementations and exact contracts

PrimaryEngine uses BigDecimal and ordered bracket integration. PaiseVerifier
uses integer-paise excess-income equations, a separate calendar calculation and
independent rounding logic. They share the fact/output schema and underlying
legal assumptions. They are compiled in one module and are not independent law
sources or isolated fault domains. Agreement does not replace external references.

All 20 stages, regime binding and age band must agree. Mismatch suppresses private
review projection and quarantines the affected local planning identity before
ordinary release gates. Above-cap and incomplete cases expose no private workings.
Income and eligible-deduction fact state remain distinct; missing/estimated is
never silently zero/confirmed. Monetary arithmetic uses no binary floating point.

Annual tax before credits is exact tax after rebate/marginal relief plus cess.
A separate ZERO_CREDIT_ROUNDED_WORKING applies the specified 288B rounding as an
illustration. Neither is a payable/refund, and final settlement order still needs
utility/reference validation. The private comparison uses the exact annual amount;
sensitivity explicitly uses the zero-credit rounded working. Its weak and strict
boundaries are separate; the preceding paise is checked. No eligible capacity or
regime-election permission is invented.

The bundled draft manifest records rules, scope, assumptions and official links.
Compiled formulas are not dynamically loaded from that file. Actual JAR/manifest
digests are included in the planning identity; this is not an approved rule store
or signed publication/release process.

## Real HTTP and English UI integration

POST /api/plan reuses the existing cookie, exact Origin/CSRF controls and current
income view token. Actor and confirmed income come from the server's ledger.
The exact profile field set contains only taxpayer facts and confirmation intent;
client approval, payment, tenant or calculated-tax fields are rejected.

The backend performs both calculations. The signed candidate binds current
income snapshot, all profile inputs, rule manifest and actual engine/verifier
artifact identities. The normal signed result guard remains in the path.
UNAPPROVED rules, PENDING references and UNKNOWN regime availability are hardcoded
for this local workbench. The browser receives only nonnumerical readiness/issue
metadata and blocked result capabilities; no private stage/amount/comparison
projection is serialized. The planning read leaves income ledger and history
unchanged. Its per-session request limit is separate from accepted edit IDs.

The Calculation check tab adds explicit period, DOB, residency, screening and
eligible-deduction confirmation. Edited profile/deduction controls clear their
confirmation/earlier check. The existing 8/8 checklist describes income review;
profile and validation are separate. Screens remain English with no general
chatbot, external model, analytics or personal-document upload.

## Independent-reference tools

The original golden workbook and JSON remain unchanged: thirty inputs and zero
approved expectations. A separate blank template has 20 stage fields for each of
24 arithmetic cases and statuses for six control cases. Every expected value is
null; reviewer/evidence fields remain pending.

The offline checker recomputes private candidate workings, verifies exact IDs,
input/corpus hashes, regime/stage sets, canonical string decimals, expected
statuses and asserted review/evidence metadata. It rejects stale/rebound cases,
missing/wrong stages, duplicate JSON keys and nonfinite values. It never edits the
original corpus or overwrites an existing output file.

Matching metadata does not authenticate a CA/reviewer, establish independence or
validate evidence bytes. Even every match only prepares human release review;
release_approved is always false. The web app does not consume this report as
approval. Positive metadata inside tests is synthetic and held in memory only.
Actual qualified review, evidence validation and authoritative publication remain
pending, rather than being represented by an enum or an unchecked checkbox.

## Observed checks

| Evidence | Observed |
|---|---|
| Engine named groups | 64 passed |
| Generated supported cases | 4,096, both implementations compared |
| Deliberately altered stage fields | All 20 detected; age/regime handling also tested |
| Selected compiled source defects | 8/8 produced behavioral failures; original sources preserved |
| Reference-checker groups | 16 passed; synthetic assertions only |
| Actual loopback HTTP groups | 46 passed: previous 31 plus 15 planning integration groups |
| Original guard core | Unchanged JAR; existing 205-group/30-mutation baseline retained, not rerun as new evidence |
| JavaScript | Syntax checked |
| Golden expectations | Original 30 inputs unchanged; zero independently approved outputs |

Selected defects removed the salary cap, multiplied standard deduction by
employer count, admitted FD interest to 80TTA, omitted new relief, invented old
relief, rounded paise prematurely, used review-date age and compared only final
tax. Compilation errors were not counted as mutation detection.

Unit tests found an invalid-date exception that would have mapped to a server
error; it is now normalized to a safe client error. Actual HTTP tests found and
verified the fix for a metadata count rejected by the response encoder.
Tests now exercise successful arithmetic that stays blocked, per-regime cap,
unknown/estimated profile/deductions, unsafe bodies/authority fields, stale views,
CSRF/Origin and a planning read that changes neither income nor history.

Browser rendering/click/keyboard QA remains unverified because the supported
managed preview could not load Java libjli.so. No alternate browser infrastructure
or weakened Host/Origin policy was introduced. Accessibility, production load,
SQL/recovery, professional pilots, HF inference, official utility/CA numerical
validation and independent security audit were not run.

## Run and completeness boundary

START_HERE.md contains the one-command launcher and private review commands.
Compiled app/core/engine JARs are bundled. A full JDK is needed only to rebuild;
no Maven/npm/DB/model/API key is required to try this local package. verify_all.py
repeats the appropriate checks; optional --include-unchanged-core reruns prior work.

The runnable local workbench completes the implemented preparation/private-
arithmetic/reference-checking path. It does not complete production Spring/MySQL
persistence, secure multi-user deployment, authoritative release controls/signing,
real CA credentials/consent/review history, AIS/26AS/document reconciliation,
payments/admin, GST/capital-gains schedules, full ITR or authorized filing and
acknowledgement. These remain concrete work/dependencies. No professional-need
percentage, best-in-world claim, customer appeal, measured capacity or tax-law
accuracy is established by the local test counts.

Official sources rechecked 1 November 2026: [AY 2026–27 salaried guidance](https://www.incometax.gov.in/iec/foportal/help/individual/return-applicable-1?mobile-app=1),
[288A](https://www.incometaxindia.gov.in/w/section-288a-62), [288B](https://www.incometaxindia.gov.in/w/section-288b-58),
[80TTA](https://www.incometaxindia.gov.in/w/section-80tta-12), [80TTB](https://wmstatic-prd.incometaxindia.gov.in/web/guest/w/section-80ttb-5),
[87A amendment](https://wmstatic-prd.incometaxindia.gov.in/hi/web/guest/w/section-20-111),
[CBDT Circular 28/2016](https://incometaxindia.gov.in/communications/circular/cirularno282016.pdf).
Some primary pages were available through official search text while direct fetch
returned 403/redirect errors. This records engineering source review, not CA
approval, retained official-utility evidence or a new-Act law pack.
