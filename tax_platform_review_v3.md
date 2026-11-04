## Current checkpoint — 5.0.0 (3 November 2026)

Current source/executable delivery is described in [STARTUP_RELEASE_V5.md](STARTUP_RELEASE_V5.md),
with [START_HERE.md](START_HERE.md) and [OWNER_HANDOFF.md](OWNER_HANDOFF.md).
Added authenticator MFA, one-use recovery codes, protected password/device changes,
shared SQL session revocation, request/transaction epoch checks and HTTPS operator
MFA enforcement. See startup_app/ACCOUNT_SECURITY.md for contracts and limits.
This is an implemented account-security workflow, not full account/identity recovery.

Observed: 20 security unit groups, 12 checks based on six published RFC vectors, 25 API groups per
backend (one intentional local-only skip), 10 compiled security defects and 41
browser flows on concurrent SQL. Two actual Java app processes enforce SQL
revocation. An in-flight revoked edit and lost MFA-activation acknowledgement were
also tested. Existing preparation/native/workbench/commerce/recovery suites passed.
The three original component modules and original 30-case golden corpus are unchanged.

Production is still not complete. Independent tax approvals remain **0/30**.
Full statutory ITR/GST/capital-gains/TDS implementation and approved rule release,
live filing/commercial payments, real CA verification, actual hosting/scale/security
review and restore promotion remain work. Forgotten-password recovery, passkeys and
external security notifications are also not connected. Credentials alone cannot
complete these software gaps. Synthetic data only; no proven 0.001% referral,
competitor-superiority or growth/profit claim. Older records below are historical.

---

## Historical checkpoint — 4.0.0 (3 November 2026)

The current executable release is documented in [STARTUP_RELEASE_V4.md](STARTUP_RELEASE_V4.md).
Start with [START_HERE.md](START_HERE.md) and [OWNER_HANDOFF.md](OWNER_HANDOFF.md).
Added owner-private capital BUY/SELL FIFO working, TDS observation reconciliation,
and notice-response organizing, with real persistence, reviews, exports and UI.
Capital sales cannot enter the narrow salary/interest assessment. TDS matching
never grants a credit and user-reported notice receipts never become verified filing.
These are working papers, not full statutory capital-gains/ITR/GST implementation.

New evidence: 26 named workbench groups plus 59 generated conservation cases,
24 workbench API groups on each of three storage profiles, eight compiled defects
detected, and 31 desktop/mobile browser flows on real concurrent MySQL. Existing
HTTP/native/commerce/HTTPS, SQL crash/recovery and isolated restore checks passed.
The previous three component JARs and original golden corpus remain unchanged.

Independent tax approvals remain **0/30**. Full statutory schedules, approved
rules/output release, live filing/payments, real CA verification, production
hosting/security/scale and restore promotion still require work. Credentials alone
do not implement them. The default runnable app needs no ChatGPT Plus/API key.
All older checkpoints below are historical; current evidence/readiness is V4.
Synthetic information only. No proven 0.001% referral or superiority/growth claim.

---

## Historical checkpoint — 3.0.0 (2 November 2026)

See [STARTUP_RELEASE_V3.md](STARTUP_RELEASE_V3.md), [START_HERE.md](START_HERE.md)
and [OWNER_HANDOFF.md](OWNER_HANDOFF.md). Added opt-in schema-2 concurrent MySQL,
validated read/scan dependencies, short shard publication locks, streaming framed
backup/isolated restore above 16 MiB, and payment recovery protection against new
request IDs while an order outcome is unresolved. Exact retry now remains visible
in the UI. The legacy mysql profile still has its original guard/16 MiB limit.

New evidence: 26 concurrent storage groups, 10 framed SQL restore groups, 8 framed
unit groups, 4 compiled concurrent defects and 22 browser checks on the concurrent
backend. Existing local/legacy SQL/native/operations/HTTPS checks were refreshed.
Test operations remain fake-provider/signed-TEST-credential checks, not real sales
or CA verification. Short quota/scan contention and bounded application/session/
worker limits remain; no 100,000-user scale evidence is claimed.

Independent tax approvals are **0/30**. Full statutory coverage, live filing,
commercial billing, real CA verification and production hosting/security/recovery
are still unreleased. This is a runnable engineering platform, not a complete
production startup. All earlier checkpoints below are historical and superseded
for current readiness by STARTUP_RELEASE_V3.md. Synthetic inputs only.

---

## Current consolidated checkpoint — 2.0.0 (1 November 2026)

Current delivery is described in [STARTUP_RELEASE_V2.md](STARTUP_RELEASE_V2.md)
and launched through [START_HERE.md](START_HERE.md). Added test-mode billing and
fixed-duration access/credits, snapshot review bundles, signed-test expert
requests/revocation and structured feedback triage. New operations checks passed
35 local + 35 actual-MySQL groups; authenticated Chrome passed 22 checks including
a 390px mobile viewport. Earlier pending browser-install statements below are
historical. Physical-device/Safari/full accessibility QA remain outside that smoke.

Independent tax approvals remain **0/30**. Full ITR/GST/capital gains, filing,
verified real CA services, live money collection and production hosting/scale are
unreleased. SQL writes still serialize and cap encrypted data at 16 MiB. All
examples are synthetic. Earlier reports below are preserved implementation history
and do not override this checkpoint or establish tax/commercial approval.

---

# AI Tax Platform: implementation review v3

## Current startup checkpoint: 1.3.0 — optional native ITR-1 structural intake

An owner-only **ITR file check** now runs a bounded local Python worker against a
hash-pinned official ITR-1 AY 2026–27 Draft-04 JSON schema. Exact form/year/version
checks supplement structural validation. The official business-validation PDF is
pinned as a reference, not implemented as approved business rules. Prefill JSON,
other forms/years, statutory eligibility and tax correctness are not inferred.

A preview offers only supported income observations: explicit gross salary and
single classified interest observations, subject to source-total/pack checks.
Missing categories never become zero; multiple observations are not silently
summed. Unsupported income schedules require a broader workflow. The user must
confirm same taxpayer, annual category coverage/classification and exact input
replacement. Selected fields commit atomically as **ESTIMATED**; confirmation is
still separate. No source tax/credit/deduction/profile fields are transferred.

Raw JSON, PAN, bank/contact details and supplied tax figures are not saved in
cases, receipts, previews or audit. Previews hold sanitized income only, expire
after ten minutes, bind owner/case/revision, and disappear on discard, accepted
use or process restart. Accepted command replay needs no surviving preview and
preserves later edits. New proposals change material identity and stale earlier
scope review. Existing preparation grants do not expose native-import lineage.

The feature is **optional and disabled by default**. The packaged app still runs
without third-party Python packages. To enable native checks, install the exact
worker dependencies in a separate venv, explicitly fetch the pinned reference
files and pass --native-python / --native-schema-dir. No reference file is fetched
while handling a customer's document. See startup_app/NATIVE_IMPORT.md.

Observed: 18 native policy groups; 21 real validator groups plus 28 generated
rejections; 20 native real-worker API groups on local storage and 20 on real
MySQL; four controlled actual-child-process fault groups; six compiled Java
native-protocol defects and six targeted Python-worker defects detected.
Refreshed regressions: 49 local API groups; 48 SQL API groups passed with one
local-file-only group skipped; 20 SQL groups/four concurrent-writer rounds;
9 isolated logical-restore groups; 12 authenticated HTTPS proxy groups; existing
workspace/preparation/archive suites and business/preparation/SQL defect checks.
These are engineering checks, not tax accuracy or independent reference approval.

Native GST schema endpoints returned HTTP 403. No vendor example was substituted
for a primary schema, and native GSTR-2B/IMS/GSTR-1/3B intake/filing stays unreleased.
The existing normalized GST observation workflow remains available; it does not
determine ITC eligibility. Synthetic data only. Independent tax approvals remain
**0/30**, and the original golden values/reference template remain blank.

SQL global write serialization/16 MiB cap, production hosting/scale/control-plane
recovery, authenticated browser/mobile/accessibility QA, verified CA services,
payments and filing connections remain pending. Earlier browser-runtime failure
was not treated as a UI pass. Guard 0.1.4, engine 0.1.0 and anonymous app 0.4.0
source/JAR bytes are unchanged; those component suites were not rerun.

## Historical checkpoint: startup 1.2.0 — self-service preparation and isolated recovery

The customer workspace now includes **ITR preparation** and **GST reconciliation**
tabs. ITR adds 14 explicit yes/no/unknown scope declarations, current-draft-bound
review and a task list for inputs/profile/inventories/document declarations. It
covers preparation for FY 2025–26 / AY 2026–27 salary and ordinary separated
interest within the declared narrow scope. It does not select a statutory ITR
form or determine whether a CA is legally required.

GST compares two normalized synthetic CSVs, using supplier aliases, document
kind, exact document number and financial year as identity. It preserves all
rows, detects duplicates/missing/unknown/date/tax-component discrepancies, marks
RCM/amendment/statement-availability issues, and records version-bound review or
follow-up. Source replacement resets reviews; exact retries preserve later work.
Matching never determines eligible ITC or a nil return, and no claim amount is
calculated. Native GSTR-2B/IMS JSON and GSTR-1/3B filing remain unavailable.

The operator can create/authenticate a dedicated-key encrypted logical backup
and restore it into a completely empty MySQL database with a fresh record key.
Restore creates **ISOLATED** state before loading records. Normal application
startup and repeated initialization refuse that target. No activation/promotion
command is supplied: current deletion, revocation, rule/control/quarantine and
retention state must be reconciled through a future reviewed recovery process.

Observed on 1.2.0: 27 existing workspace unit groups plus 12 generated ownership
checks; 27 new self-service groups plus 60 generated component discrepancies;
8 archive unit groups; 46 local HTTP groups; 20 real MySQL groups and four
concurrent-writer rounds; 45 SQL HTTP groups passed with one local-file-only test
skipped; 9 real SQL/launcher backup-restore groups; 12 authenticated HTTPS proxy
groups; six business, six preparation and three SQL compiled defects detected.
The actual MySQL concurrency suite caught an added guard-read ordering defect;
write locking now precedes the consistent read, and the same concurrency/replay
checks pass. Counts are engineering evidence, not legal or accuracy percentages.

Use `python3 startup_app/run.py` and `http://127.0.0.1:8788`. Read
`self_service_and_recovery_v1_2.md`, `startup_app/SELF_SERVICE.md` and
`startup_app/deploy/BACKUP_RESTORE.md`. Existing SQL users must explicitly run
`--init-mysql` with the migration configuration and original key to add V002.
The key/record format and original data are retained; an isolated target cannot
be activated by this command. See `startup_app/deploy/MYSQL_SETUP.md`.

Global SQL write serialization, bounded namespace iteration, the 16 MiB payload
cap and process-local sessions remain. A normalized/pool-backed Spring repository,
production scale/hosting/operations and reconciled production disaster recovery
are pending. The browser runtime download failed; no authenticated browser,
mobile, keyboard or accessibility pass is claimed for these new views.
Independent tax approvals remain **0/30** and original golden values stay blank.
Full ITR/GST/capital gains, verified CA services, checkout and filing connections
remain unreleased. Synthetic data only. Guard 0.1.4, engine 0.1.0 and anonymous
local_app 0.4.0 source/JAR bytes are unchanged; those suites were not rerun.

## Historical checkpoint: startup 1.1.0 — MySQL and HTTPS pilot

`startup_app` now has an explicit MySQL 8.4 persistence profile and strict HTTPS
reverse-proxy mode. The default encrypted local profile is still available.
Customer/team/admin workflows, consent, source proposals, planning quotes and
support use the same business rules with either store. No real taxpayer data or
provider credentials were used.

Observed on the current 1.1.0 JAR: 27 unit groups and 12 generated ownership checks;
36 local HTTP groups; 20 actual MySQL groups with four concurrent-writer rounds;
35 MySQL-backed HTTP groups passed and one local-file-only group skipped;
12 actual authenticated HTTPS proxy groups; six business defects and three SQL
defects independently compiled and detected. Real MySQL SIGKILL/restart preserved
committed data; the API failed during outage without a local-file fallback.
The HTTPS test used a disposable trusted certificate and a local proxy fixture,
not a browser or Nginx/public deployment.

The SQL adapter is deliberately bounded: a singleton control-row lock serializes
all business writes, some namespace operations scan bounded collections, and
encrypted payload totals are limited to 16 MiB. It is not the final normalized,
indexed, pooled Spring repository or a capacity benchmark. Sessions remain local
to one process. Backup restore, key rotation and production operations are pending.

Run `python3 startup_app/run.py` at `http://127.0.0.1:8788` for the default profile.
See `startup_app/deploy/MYSQL_SETUP.md` for explicit driver/config/migration/HTTPS
steps and `mysql_persistence_v1_1.md` for evidence and limits. No runtime data,
passwords, record keys, MySQL distribution or JDBC binary is packaged.

Independent tax approvals remain **0 of 30**; all original golden expectations
stay blank. Numerical tax release, full ITR/GST/capital gains, verified CA services,
checkout and filing connections remain unavailable. Authenticated browser/mobile/
accessibility QA and independent security review remain pending. The original
guard 0.1.4, engine 0.1.0 and anonymous local_app 0.4.0 source/JARs are unchanged;
their earlier tests were not rerun this turn. Synthetic data only.

## Historical checkpoint: startup 1.0.0

A new **startup_app 1.0.0 local development application** now brings customer
registration/login, encrypted saved sample cases, the existing income/profile/
source workflow, consent/expiry/revocation, a preparation-review queue and
snapshot-bound comments, future immutable price versions, frozen planning quotes,
in-app support, owner export/archive/delete and amount-free operational activity
into one runnable Java application. ADMIN cannot inspect private customer case
inputs; PREPARER is an unverified preparation role, not a credentialed CA identity.

Observed: **25 new unit groups**, **12 generated ownership-denial checks**,
**36 new actual loopback HTTP groups**, and **six selected compiled defects
detected**. Real process restart preserves saved sample data and invalidates old
sessions. The existing core 0.1.4, engine 0.1.0 and local_app 0.4.0 source/JARs
remain unchanged; their earlier test counts are prior evidence, not new tests.

Start `python3 startup_app/run.py` and open `http://127.0.0.1:8788`.
Optional operator setup commands, limits and complete flow are in
startup_app/README.md. The anonymous 0.4.0 workbench remains available separately
with `python3 local_app/run.py` at port 8787. No test accounts/passwords or runtime
data/keys are included in the archive.

This is **not a complete production startup**. Independently approved tax outputs
remain zero; public tax numbers, payment collection, full ITR/filing, verified
professional sign-off and native document parsing remain unavailable. The local
encrypted single-writer aggregate is a development profile, not tested MySQL or
large-scale infrastructure. The production target remains Java/Spring + MySQL.
Only static desktop landing/signup navigation were inspected in the browser;
authenticated browser, mobile/accessibility and independent security QA remain
pending. See startup_implementation_v1.md and INDIA_LAUNCH_PLAN.md for evidence,
product pilot experiments and the exact live-launch gates.

Earlier version-specific records follow; this checkpoint describes the new
module and does not retroactively change those historical tests.

## Current checkpoint: 0.4.0 (1 November 2026)

Local app **0.4.0** adds an explicit selected-source-to-income proposal. An
eligible known annual gross replaces one whole category input as **ESTIMATED**;
the ordinary separate displayed-value confirmation is still required. Multiple
groups in the same category, transactions, gross conflicts/missing/repeated/value
variants and unknown gross block transfer. Two user acknowledgements cover
whole-category/classification review and replacement; no source amount is summed
or treated as TDS credit. A minimal historical source link is shown separately
from confirmation and remains unverified. Accepted retries preserve later edits
and lineage; new manual proposals/clears remove that field's source link.

Observed: **79 actual HTTP groups**, zero failures/errors; **28 new source-policy
groups**, **36 generated gross/category cases**, and **six selected compiled
defects detected** (four policy, two actual-HTTP). Comparison regression evidence
remains 38 groups, 81 generated pairs, eight actual-thread rounds and four compiled
defects. Core 0.1.4 and private engine 0.1.0 are unchanged. The original 30 golden
inputs and all blank expectations stay unchanged: independently approved outputs
remain **zero**. See source_proposal_v0_1.md and START_HERE.md.

Browser visual/click/keyboard/accessibility QA, native document/provider adapters,
source authenticity, durable SQL/identity and professional/filing integration
remain pending. No public tax figure, export, filing or CA approval is enabled.

Earlier chronological records follow; their version-specific limitations do not
replace this current checkpoint.

Review date: 31 October 2026, India time. Owner: Kunal. Status: foundation specifications and draft test inputs, pending implementation evidence and independent tax review.

## 1. Decision

Keep the product ambition, narrow the release contract. Build a deterministic, explainable tax-planning core first. It should accept a clearly defined set of facts, apply a reviewed rule bundle, independently recompute the result and state exactly what is supported and what still needs confirmation. Add document extraction and professional workflows after that core is proven. A larger model or more agents does not repair missing facts, an incorrect legal specification or a broken transaction boundary.

The current attachments are overlapping chat exports. They contain useful design, incomplete source snippets, repeated recommendations and contradictory historical answers. They are not a runnable repository. Claims such as “10,002 PASS”, “built” and “memory updated” are historical assertions, not verification performed in this review. The original application has not been built or integration-tested. No penetration test, official-utility run or AI-model benchmark was performed. Revision 2 adds a standalone Java safety module and its local engineering tests.

The concrete deliverables in this pack are a 30-case golden-input draft, a review workbook, a findings register, updated system overview, build tasks, Hugging Face evaluation plan and a continuation checkpoint. All numerical golden expectations remain empty. There are zero independently approved cases.

## 2. Decisions retained from the conversation

| Area | Decision |
|---|---|
| Product | India tax preparation and lawful tax optimization; self-service where supported; professional review for uncertain/regulated work |
| AI authority | Extraction, finding relevant evidence and drafting explanations only. No tax arithmetic, rule publication, price, payment or filing authority |
| Verification | Deterministic calculator and independently implemented verifier; disagreement blocks the affected output |
| Missing facts | Unknown, absent, zero, conflict and not applicable are different states |
| Rules | Reviewed, versioned and historically replayable; government material ingested offline rather than queried inside a tax request |
| Product UI | Simple English/Hindi wording, one decision per screen; free-text assistance stays inside a named workflow; no general chatbot |
| Commercial | Frozen accepted quotes and audited future pricing versions; no retrospective price rewrite |
| Expansion | CA Assist remains a later escalation workflow. GST, accounting, business tax and investment planning are separately gated packs |
| Storage | Minimize what is collected. Retention/deletion follows a reviewed purpose and legal basis, not blanket “keep everything” or “delete everything” |
| Initial stack | Java core, Spring Boot boundary, MySQL, TypeScript PWA; Python isolated for document processing and offline evaluation |

The older suggestion to add a generic “AI se pucho” screen is superseded by the scoped-workflow requirement. The suggestion to switch databases without inspecting a working repository is not adopted. MySQL can support the required constraints with careful transactions and schema design; do not add a migration project merely to make the architecture appear stronger.

## 3. The four immediate corrections

### New-regime threshold and old-regime cliff

For AY 2026-27, the new-regime ordinary-income rebate/relief must be applied to the appropriate rounded total income. The pre-cess tax remaining in the relief zone is limited by the excess over the rebate threshold. Cess is then considered separately. The limit does not mean final tax including cess can never exceed that excess. [T01,T02,T03,T05,T06]

Illustrative manual calculations, not approved golden outputs:

| Situation | Working | Rounded annual tax illustration |
|---|---|---:|
| New, ordinary taxable income before rounding 12,00,004 | Total income rounds to 12,00,000; rebate applies | 0 |
| New, rounded total income 12,00,010 | Slab tax 60,001.50; relief 59,991.50; remaining tax 10; cess 0.40 | about 10 |
| New, rounded total income 12,10,000 | Remaining pre-cess tax 10,000 after relief; cess 400 | about 10,400 |
| Old, resident under 60, total income 5,00,000 | Ordinary tax 12,500 covered by eligible rebate | 0 |
| Old, resident under 60, total income 5,00,010 | Ordinary tax 12,502; no threshold rebate; cess 500.08 | about 13,000 |

These examples assume only ordinary-rate income and no other adjustments. The old-regime example is age-specific: a senior's basic exemption differs, and a very senior's boundary behaves differently. Do not apply a universal “old regime jumps to 13,000” rule. The official utility must settle the exact schedule rounding and filing settlement stages before these become approved reference cases.

### Interest deductions belong in the first interest-capable release

Capture gross interest by qualifying account/deposit type and institution. For old-regime eligibility, 80TTA covers eligible savings interest up to its limit, while 80TTB covers qualifying deposit interest for a resident senior citizen up to its aggregate limit. A company FD is not automatically a qualifying bank deposit. Do not double-claim both sections or infer that all “other income” qualifies. These deductions are not available under the ordinary new-regime claim path. [T07,T08,T01]

This matters before a regime recommendation is made. Deferring these deductions while advertising a reliable interest-based old/new comparison would overstate what the product knows.

### April 1 DOB is supported by an official circular

CBDT Circular 28/2016 expressly addresses the preceding-day age rule. For the old-law FY 2025-26 age tests, the pairs are DOB 1966-04-01 versus 1966-04-02, and 1946-04-01 versus 1946-04-02. The first of each pair reaches the relevant prescribed age on the preceding 31 March. Record the source and confirm all affected benefit mappings in the review sheet. Do not simply calculate today's age. [T04]

The circular is an old-Act source. A new-Act rule pack must separately document the transition/applicability of existing instructions. This review does not automatically approve that carry-forward.

### PAN lookup must use a secret key, separately from encryption

Use HMAC-SHA-256 over a normalized, purpose-separated, tenant-scoped identifier, with a key held outside the database. Encrypt the PAN separately with authenticated encryption and managed keys when retaining the original is necessary. Store key IDs, random nonce, ciphertext and integrity tag; bind tenant/profile/field identity as authenticated additional data. Lookup key and encryption key are different secrets.

The specific “all PANs in GPU minutes” claim is not established by the attachments. The security concern is valid: the structured PAN search space is small enough that fast unkeyed hashing is a poor confidentiality mechanism. Hardware speed is unnecessary to justify the fix. A database leak plus application/KMS-key compromise remains a serious threat even after HMAC.

## 4. Additional errors that materially change the design

1. **A zero placeholder is not safe for unknown interest.** The source explicitly permits it because zero is plausible. A large omitted FD interest amount can change tax and eligibility. The final result must require confirmation; a hypothetical can be shown only as a hypothetical with its assumptions.
2. **One “verified” grade hides different questions.** Arithmetic agreement, full income inventory, deduction completeness, fresh facts, legal scope and filing readiness need separate fields. A provisional comparison must not show a definite “you save” claim.
3. **A public summary can be stale.** The inspected salaried-taxpayer summary still says one property. Notification 45/2026 and the current ITR-1 FAQ permit two for AY 2026-27. Use the notified form and maintain an explicit source-conflict record. [T10,T11]
4. **Form eligibility is not product support.** The official ITR-1 can accept certain limited 112A gains; the starter calculator can conservatively decline all capital gains. Tell the user the software does not support that case, rather than declaring the form legally unavailable. [T10,T11]
5. **50 lakh is not a gross-salary cutoff.** Apply the legally specified income/form conditions, separately from a clearly named product cap. G24/G25 expose this distinction.
6. **182 days does not prove ROR.** Residency involves the applicable tests, exceptions and past history. This starter consumes a reviewed ROR determination or abstains; it does not infer residency from a trip question.
7. **PAN last4 and DOB are not a strong second factor.** They can be known to others. Sensitive exports need a bound authenticator or an independently verified channel and recovery controls.
8. **A hash stored next to editable data is not tamper proof.** The attacker may replace both. Approved bundles need signatures checked against a trust root outside DB-writer control. Snapshot hashes still help detect accidental changes.
9. **A correct annual tax number does not establish refund/payable.** Credits, filing dates, interest and fees have their own completeness and rounding requirements.
10. **A category label does not make personalised investment advice unregulated.** Recommending how a particular person should allocate money may cross the advice boundary even without naming a fund. Keep personalized allocation disabled until the legal/service model is reviewed. [L04,L05]

The complete issue-to-fix-to-test mapping is in findings_register.md. Finding an issue is not the same as implementing its fix.

## 5. Correctness contract and result model

Use an immutable fact record with value when known, provenance, confirmation state, source period and revision. Money is a decimal string over APIs and BigDecimal internally. Reject floats, NaN, infinity, invalid grouping, excessive scale and out-of-domain values. Support Indian commas and Devanagari digits through a deterministic parser; ambiguous phrases such as “12” need an explicit unit. A blank answer is never parsed as zero.

Gross salary, taxable salary already reduced in Form 16, family pension and business receipts are different input concepts. Each salary field must state whether exemptions and standard deduction have already been applied. Sum multiple employers at the legally correct stage; apply the annual standard deduction once and cap it at relevant salary.

Keep these independent result axes:

| Field | Example values | Meaning |
|---|---|---|
| scope | SUPPORTED / PARTIAL / UNSUPPORTED | Whether all relevant income and rules are handled |
| facts | CONFIRMED / ESTIMATED / MISSING / CONFLICT | What is known about the inputs |
| arithmetic | MATCH / MISMATCH / NOT_RUN | What the second implementation checked |
| comparison | DEFINITIVE / PROVISIONAL / NOT_AVAILABLE | Whether old/new ranking is supported |
| freshness | CURRENT / STALE / REVOKED_RULE | Whether it remains valid for the current snapshot/policy |
| filing | NOT_PREPARED / PREPARATION_READY / USER_REPORTED_FILED / PROVIDER_ACKNOWLEDGED / EVERIFIED | Filing workflow, separately evidenced |

Expose server-generated capabilities such as show_complete_tax, show_definitive_recommendation, export_preparation and submit_return. The UI cannot enable them by replacing a badge. This starter has no submit capability.

Use explicit blocked/partial response types rather than a single response with nullable warnings around an otherwise convincing total. For unsupported income, preserve the case and the supported component facts; do not invent a combined final liability or a tax-saving recommendation. If an estimate is requested, use a separate estimate contract with visible exclusions.

## 6. Tax computation and independent verification

The starter domain is deliberately small: adult resident ordinarily resident individuals, gross salary before standard deduction, separated bank interest, no special-rate income, no losses, no other salary adjustments, and a reviewed ordinary-income range. This is a calculator pack, not complete ITR-1 coverage.

Pipeline:

1. Validate input semantics, completeness and period identity.
2. Validate the frozen signed rule manifest and supported variants.
3. Compute income under each relevant head, including the salary-specific cap.
4. Apply eligible head adjustments and Chapter VI-A deductions in the approved order.
5. Round total income at the specified legal stage.
6. Compute ordinary slab tax and any separately supported special-rate amounts.
7. Apply the relevant rebate/relief rules to the correct components.
8. Apply surcharge/cess only where the rule pack supports them; otherwise reject the affected range.
9. Follow the approved tax/credit/fee/interest settlement order. Do not call annual liability “refund”.
10. Independently recompute all material stages and compare exact required outputs.

The primary implementation can walk slabs. The verifier should use a separately authored form-order specification and checkpoint formulas. Do not share salary-deduction, rebate, relief or rounding helper functions between them. Sharing raw approved rates is acceptable but creates common-mode rule-data risk, which the external references and rule review must address. Different programming languages do not by themselves make a legal specification independent.

Reject slab gaps, overlaps, unsorted brackets, duplicate variants, ambiguous publications and invalid rates before arithmetic. A null top bracket is permitted only where the schema defines it. No arbitrary code, expression language or LLM-generated formula is executed from a rule row.

Properties must be scoped. Tax nonnegativity and standard deduction no greater than salary are useful. “Tax is globally continuous” is false at some rebate boundaries; “take-home always increases” can fail around cliffs and cess interactions. Equality between two implementations proves neither correct law nor correct inputs.

## 7. Golden reference workflow

There are exactly 30 initial cases in golden_cases_v0.json and the workbook. Inputs are synthetic and fully identify the old-Act period. All expected numerical values, reference evidence, reviewers and review dates are blank. G25-G30 are workflow controls rather than supported numerical cases. Engineering expectations are hypotheses and do not count as tax approval.

The independent reviewer should receive input facts before seeing the implementation's answer. Use official utility screenshots/exports or signed worked calculations, record exact utility/schema version and settings, and compare intermediate stages. Do not populate expected output by running the same calculator being tested. Do not label manually derived examples in this report as approved golden values.

After review, keep independent evidence outside the public repository if it contains personal or copyrighted material; use synthetic/shareable workings for public fixtures. An approved case binds input hash, expectation hash, legal pack identity, reference version, reviewer and review date. An input edit invalidates that approval. CI must reject missing expectations for mandatory cases; silently skipping them must fail the release gate. No required case set or zero tests is not a pass.

Thirty cases are a starting review batch. Before each added tax feature, expand to cover all supported rules and their interactions. Add paise boundaries, new-Act rejection, NRI/history, leap-day DOB, 80C/80D/NPS/HRA eligibility, invalid formats, salary already net of deduction, missing TDS and losses as separate backlog cases. The workbook is not a comprehensive tax test suite.

## 8. Break-even deductions and honest optimization

Keep a baseline for “do nothing”. Rank only lawful, eligible, evidenced alternatives. Separate tax change, contribution/cash outlay, liquidity restriction, lock-in, fees, risk and execution requirements. An investment contribution remains an asset; do not subtract its full principal as if it were a permanent cost. Compare future after-tax cash flows or scenario outcomes with explicit return assumptions, rather than inventing a single money value for “risk” or “lock-in”.

Break-even needs a precise question: the smallest additional eligible deduction at which old is no worse, or the smallest at which old is strictly cheaper. Those are different answers. Freeze all other facts. Use the already-known 80TTA/80TTB and relevant regime-specific deductions. Limit search to genuinely attainable eligible deductions, not an arbitrary 10 lakh input.

Binary search is acceptable only over a documented monotone subproblem. Rounding creates plateaus; validate the selected point and adjacent permitted steps, as well as eligibility. If old cannot win inside the eligible limit, return that result. Never translate a mathematical threshold directly into “invest this much”. HRA, head-specific adjustments and employer NPS are not interchangeable rupees in one deduction bucket.

Later scenario search uses approved templates and eligibility pruning, exact deterministic tax evaluation, a per-request time/cost budget and a small Pareto set. A timeout returns incomplete search coverage, not “best among all possible strategies”. Track plan status as suggested, user accepted, evidence of execution, partly executed and completed. Never assume a March plan was executed in March merely because a recommendation existed.

## 9. Rules, versions and replay

Rule identity includes Act, income-period start/end, AY or TY label, taxpayer type, residency, regime, age conditions and supported income scope. Legal applicability time and publication/knowledge time are separate. Revision is separate from variant. A forecast-announced change may be simulated as a labelled scenario; it never becomes the enacted tax default.

Publish an atomic bundle for a supported calculation/comparison, not a random collection of the newest individual rows. Keep immutable content separate from lifecycle state/publication pointer. A critical release needs a named maker, separate qualified checker(s), source mapping, complete mandatory test evidence and a signature. Two reviewers is a proposed control for critical changes, not a claim about a statutory review-count requirement.

The app DB identity cannot edit approved content or approve its own rule release. MySQL CHECK constraints cannot validate a test suite across other tables; perform publication in a restricted transactional service/procedure and verify its concurrency behavior. Prevent direct privileged bypass operationally. Document legal interval overlaps and require an explicit reviewed resolution instead of choosing the maximum version number.

Replay requires immutable facts, canonicalization version, approved bundle ID and hash/signature, engine/verifier artifact digests, settings and structured result. Version strings alone are insufficient. Do not put PAN or entire financial snapshots in event payloads or application logs. Source bytes and evidence are retained only under the permitted retention policy.

Reproduction is different from current eligibility. A historically completed result can be reproduced with its old bundle; if that bundle is recalled, the current user-facing result is marked revoked/stale and cannot be used for filing without recalculation. Affected-case indexing and recall notices belong to the release plan.

## 10. Transaction and retry design

Use one canonical model: tax_case, fact_snapshot, calculation_identity, calculation_attempt, verification_record, outbox_event. Do not retain parallel tax_years/tax_calculations concepts that mean the same thing. Immutable snapshots have distinct IDs even if facts change and later revert.

**Tx1:** authorize tenant/profile/case access; lock or compare-and-set the case; validate its current snapshot; resolve/freeze the whole compatible rule manifest; create or locate a logical calculation identity; create an attempt with lease/fencing token; set active_run_id. Commit. No external inference or slow document work is inside this transaction.

**Compute:** load the exact frozen manifest by ID and run both implementations outside the transaction. Do not query “latest effective” independently for each regime.

**Tx2:** atomically check STARTED attempt, current lease/fencing token, active_run_id and current_snapshot_id. A newer snapshot/run makes the result stale. A revoked bundle blocks promotion. Commit the result, verification and outbox record together, with a conditional update whose affected-row count must be one.

After a crash, the reaper expires an attempt using the same conditions and clears the case pointer only if it still points to that attempt. A late worker with an old token cannot promote a result. Retrying uses a new attempt under the same logical identity when appropriate. Completed valid results remain deduplicated even after an active lease disappears.

Logical calculation identity includes tenant/case, immutable input ID/hash, full rule-manifest hash, engine/verifier artifact revisions, canonicalization/schema version and relevant settings. Client request idempotency is separate: the same request key with a different body is rejected. Authorization is checked again before returning a reused result.

Outbox delivery is at least once. Use event ID, consumer dedupe, bounded backoff, dead-letter handling and metrics. Notifications never determine financial state. Revoke a professional grant before a background export starts, and check again before a download capability is issued.

## 11. Data model and security controls

Accounts represent actors; taxpayer profiles represent people; an account can access multiple profiles through explicit scoped grants. A CA firm is a tenant. Family/member/CA roles do not imply global visibility. No cross-tenant PAN uniqueness rule may reveal whether someone is a client elsewhere.

Use a tenant-scoped HMAC lookup index and separate encryption of sensitive originals. Key rotation requires an explicit dual-read/dual-write migration epoch: writers use the same required key set until backfill completes; only then retire the old lookup. Adding key_version to a unique key alone permits duplicate logical taxpayers under different key versions. Test recovery and restores during rotation.

PAN syntax is not PAN ownership verification. Avoid collecting PAN at all in the anonymous planner. In saved-case workflows, explain the purpose and restrict plaintext access. Use generic duplicate/conflict responses; do not expose an enrollment oracle. Separate customer, reviewer, operations and rule-publishing privileges. Hardware-backed admin MFA, maker-checker controls and break-glass audit protect high-impact actions.

Browser sessions should use secure, HttpOnly, appropriately SameSite cookies with CSRF protection where applicable. If JWTs are used, bind their purpose/audience and revoke sessions; a JWT is not a substitute for object authorization. Do not store sensitive long-lived tokens in browser localStorage. Limit login/OTP attempts atomically across all instances and implement account recovery against SIM recycling and swap.

Uploads use private objects, size/page/dimension limits, magic-byte/type checks, quarantine, malware scanning and parsing in an isolated non-root worker with CPU/RAM/time/temp-disk limits and no network. Detection/scanning does not make an upload harmless. Prevent signed-URL reuse after a grant is revoked using short expiry and controlled download mediation where required.

Logs, analytics, error trackers and traces must be tested for leakage; regex scrubbing alone is not enough. Use explicit allowlisted event fields. A financial snapshot hash can itself leak low-entropy values or enable linking; keep it access-controlled and consider keyed audit identifiers. Do not put raw facts in metrics labels. Backups need isolated credentials, encryption, restore tests and a deletion/hold policy.

Security tests are limited to the system's own authorized staging environment. No live third-party penetration testing was performed in this task. Independent VAPT, remediation and retesting are release evidence for the sensitive workflows, not an “unhackable” guarantee.

## 12. Privacy, retention and regulated boundaries

Review the DPDP Act commencement notifications, Rules and corrigendum together. The Rules phase in obligations; a public statement about May 2027 is not a substitute for a per-obligation applicability register. The reviewed text includes specific retention/security provisions, including one-year requirements, and a 48-hour erasure notice tied to its particular rule context. Do not promise deletion in 24-72 hours for all records or assume that notice applies identically to every deletion. [L01,L02,L06]

Create a retention matrix by data class and purpose: transient upload, confirmed tax fact, approval evidence, account/session data, security log, invoice, professional matter and backup. Record retention trigger, minimum/maximum where applicable, legal hold, erase mechanism, processor action and evidence. Separate model-improvement consent from the core service; default to no training on customer financial data. Legal review must resolve the exact treatment of raw documents and retained processing evidence.

Incident handling needs its own legal timers and owner. CERT-In's covered-incident reporting and ICT-log directions are separate from applicable DPDP breach duties. “72 hours to the Board” must not be mistaken for permission to ignore the earlier without-delay/initial notification requirements. [L01,L03]

Professional and filing features stay disabled until the relevant entity, authorization, contracts, payment/tax treatment and service responsibilities are settled. A CA review is not automatically SEBI investment-adviser authorization. A disclaimer does not expand legal authority. This pack makes no claim that a particular ERI application, ICAI fee-sharing structure, domain name or trademark is approved or available. Those old chat claims were not revalidated here.

## 13. Hugging Face: useful roles, limited authority

Hugging Face is useful for sourcing open model candidates, inspecting model cards and running controlled evaluations. It does not supply an approved Indian tax engine or an independent legal oracle. The five inspected publisher cards and candidate roles are detailed in hf_evaluation.md. License fields were inspected; model weights and local performance were not tested.

Start with deterministic parsing of digital PDFs and structured files, followed by isolated OCR where necessary. Evaluate the English and Devanagari Paddle recognizers only as text-line components of a complete document pipeline. Add retrieval/reranking over reviewed official sources for reviewer assistance and explanations after the tax core is stable. An optional small language model may produce strict-schema proposals but cannot commit confirmed facts, publish rules or calculate tax.

Your 16 GB U-series laptop is suitable for developing the Java core, fixtures and small model experiments with measured limits. A quantized small LLM may be feasible as an experiment; context/KV cache and other processes matter. There is no measured latency or concurrency result here. Run one model experiment at a time before considering a dedicated inference worker. “Free weights” does not mean free hosting or unlimited production calls.

## 14. Stack, performance and operations

Start with a modular monolith and isolated document worker. Use one well-managed MySQL deployment with verified backup/restore for a small pilot. Add a cache, replica, broker or independent service when measurements demonstrate the need. Do not run customer PAN data on a development laptop or rely on a disposable free VM as the only durable copy.

Keep calculate/verify pure and dependency-free. Use typed API validation at the boundary, Flyway-style versioned migrations, CI build and relevant tests, secret scanning, dependency/container scanning and a release artifact manifest. Pin a supported JDK/framework/dependency set at implementation time after checking current support; the choice of LTS number is not the security architecture.

Define workloads separately. For illustration, 100,000 active users making one call every 10 seconds means about 10,000 requests/second; 1,000,000 under the same assumption means 100,000 requests/second. These are workload assumptions, not supported capacity. Upload/OCR, saved-case DB writes and anonymous arithmetic have different bottlenecks.

Proposed initial performance targets, to be measured rather than advertised: pure ordinary-tax computation under 50 ms p95 on the declared test host; planner API under 800 ms p95 at a declared pilot load; uploaded work asynchronous with a visible queue/status. Record p99, error/abstention rates, database connections, queue age and cost as well. A target that fails does not justify hiding missing verification.

Use a waiting room, bounded queues, backpressure and disable nonessential inference during deadline bursts. Immutable rule bundles can be cached by hash. Private results need tenant-aware cache keys and invalidation by snapshot/policy; shared caches must never reuse another user's facts. PWA service workers should not cache private API responses or documents by default.

Changes follow expand/contract migrations, compatibility checks and reversible feature flags. Rule rollback is an explicit controlled new publication or recall, not overwriting history. Maintain an incident owner, current runbooks, off-host backups and periodic restore exercises. Never claim “tested against multiple attacks” without logs showing the actual scope, tools, findings, fixes and retest.

## 15. Product and commercial scope

First useful product: an anonymous scoped regime planner with a calculation breakdown and honest “more information needed” outcomes. PAN and login are unnecessary for that step. Add saved profiles, document review and preparation only when the corresponding security and tax evidence exist.

Validate the distribution hypothesis with salaried users and CAs/preparers. Ask about actual prior returns, time lost, mismatches, existing tools and willingness to provide a consented pilot case. Do not treat an invented interview count or price threshold as proof of demand. Reuse the same core for a CA exception/review tool if evidence supports that route.

CA Assist later uses a matter-based workflow with explicit grant, quote, service window, reconnect/reassign/refund behavior and named professional responsibility. Pricing is outside the tax core; neither a credit nor a paid plan can unlock an incorrect result. Commercial admin cannot alter calculations or override a mismatch. Operational support sees masked information unless a justified, audited access grant is issued.

When paid features are introduced, treat hosted payments, webhook verification/dedupe, double-entry credit ledger, refunds and quote acceptance races as their own release slice. A gateway does not remove all platform fraud or compliance responsibilities. Customer money is not held without a reviewed lawful settlement model.

The defensible advantage is measurable: fewer preparation errors, clear sources, lower reviewer time, protected data and correct abstention. “Better than one million CAs” or “1000 times ClearTax” cannot be established by this architecture.

## 16. Build sequence and release gates

| Gate | Concrete output | Exit evidence |
|---|---|---|
| 0 | This draft golden workbook and legal/source questions | 30 inputs checked for scope/semantics; no fabricated expectations |
| 1 | Independently approved expected stages | Qualified review + exact utility/workings evidence; mandatory cases approved |
| 2 | Original source repository recovered and baseline run | Reproducible build, test report, migrations and known-failure list |
| 3 | Result safety plus interaction-aware tax fixes | Golden stage comparisons, negative/gate tests, scoped properties and mutation checks |
| 4 | Canonical schema/rule publication plus orchestrator | Real-MySQL constraints, concurrent publication, crash/retry/stale/fencing tests |
| 5 | Thin anonymous API and accessible PWA planner | Contract/UI tests, declared-load test, error/abstention metrics and reviewer pilot |
| 6 | Saved-case/document preparation workflow | Consent, object authorization, encryption/retention mapping, extraction evaluation, VAPT and remediation |
| 7 | Filing/paid/professional features | Current forms/schema/channel approvals, settlement controls and feature-specific review |

For a stateless planner, persistent orchestration can follow the pure engine/API; it should not delay reviewing the tax logic. If saved cases are introduced, their transaction and authorization gates are mandatory first. Schedule by completed evidence, not a guessed “two weeks” containing an unbooked CA/legal review.

Tasks and acceptance tests are in implementation_tasks.md. The next session must begin from NEXT_SESSION.md and the actual code zip. This pack does not pretend to patch source that was not provided.

## 17. Failure review, revision 1

The follow-up user instruction permits any language while prioritizing quality. Keep technology choices tied to measured need and preserve the agreed core until evidence justifies a change.

Four additional controls/refinements are recorded as F39-F42: legal availability of a regime choice, live eligibility checks before delivering cached results, identity-wide quarantine after contradictory verification, and recovery of current control/deletion state before a restored system serves traffic.

See failure_review_v3_1.md for details, sources and acceptance boundaries. A standard-library Python reference protocol is now included under verification_models. Its 22 scenario tests passed; complete exploration of its finite domain checked 10,016 states and 22,558 non-no-op transitions. Eight deliberately faulty variants each produced a counterexample. This is abstract protocol evidence, not tax validation or proof of a Spring/MySQL implementation.

The findings register now contains 42 findings. The 30-case golden workbook is unchanged and still has zero independently approved expectations. No application/SQL/load/VAPT or AI inference result is newly claimed.

## 18. Implemented result-safety module, revision 2

The core of Task 1 is now implemented as an independently buildable Java 17
library. It consumes trusted case/calculation/control state and projects only
permitted output fields. Unknown/partial income, mismatches, stale or quarantined
results and unapproved independent references cannot expose a complete tax amount.
Incomplete deductions/eligibility suppress definitive comparisons.

The module passed 50 named scenario groups and 2,048 combined guard cases. Four
selected guard-removal mutations compiled and were detected by failing tests.
Actual JSON examples were parsed and matched across two JVM runs. Source, JAR,
tests and build reports are included under java_result_safety; see
result_safety_implementation.md for the integration contract and limitations.

This is component-level implementation evidence. It does not establish tax-law
correctness, production authentication/signature checks, SQL concurrency, durable
distributed quarantine, HTTP integration, load capacity or VAPT. The independent
golden corpus remains unchanged with zero approvals.

## 19. Hardening revision 3: evidence order, producer proof and input proposals

The next code revision fixes a reproduced mismatch-quarantine ordering bug,
adds Ed25519 signed-candidate delivery with current key status/purpose/expiry
checks, and an exact Hindi/English amount parser that requires confirmation.
Local execution passed 104 named scenario groups, 2,048 guard states, 4,096
parser cases and 66 signed-field changes; all ten selected compiled defects
were detected. See hardening_report_v3_2.md and component version 0.1.1.

No production database/key-registry/HTTP integration or tax-law validation is
implied. The signing proof authenticates producer bytes; the independent golden
corpus remains unchanged with zero approvals. HF candidates remain unbenchmarked.

## 20. Guided customer features, revision 4

Component 0.1.2 implements a focused next input action, review checklist,
permitted regime status explanation and preparation note. A separate signed,
current input-only entry allows preparation before tax approval/calculation;
it cannot publish numerical results. Result guidance wraps the existing gate.

Observed: 135 named groups, 256 new generated guidance combinations and sixteen
selected compiled defects detected, alongside existing guard/parser/proof checks.
Six new JSON examples were parsed and matched across JVMs. The English inline
preview uses these synthetic examples and is not a deployed application.
Actual PWA/HTTP/database integration and tax-law correctness remain pending;
the 30 golden inputs and zero independent approvals are unchanged.

## 21. Session edit/confirmation flow, revision 5

Component 0.1.3 adds explicit amount proposal/confirmation and conflict
resolution with expected-key checks, exact accepted-request retry and dependency
resets. Bounded in-process session state atomically publishes current facts,
receipts and amount-free history; a later cached-result read uses the new key.

178 named groups pass. New checks include 64 edit transitions, 48 actual-thread
rounds with 192 requests, edit/close during policy lookup, and eight additional
selected compiled defects detected (24 total). Five new JSON examples parsed and
matched across JVMs. No SQL, durable recovery, load, original-backend integration
or tax-law correctness is established. Session-only data is lost on process exit;
the real UI must make that clear. Golden files and zero approvals are unchanged.

## 22. Professional readiness and premium strategy, revision 6

Read-only professional queue routing now checks current assignment/credential
validity, release, snapshot-bound evidence and existing signed result gates. It
keeps every requested case in its denominator and emits no invented professional-
need or accuracy rate. Ready means ready for review, not approved or filed.

205 named groups pass, including 26 professional groups and 1,296 evidence-state
combinations. Thirty selected compiled defects are detected. Four new projection
examples parse and match across JVMs. This is not a live CA portal, real credential
lookup, consent capture, evidence verification, durable review trail or filing.
ca_premium_strategy_v0_1.md maps current requirements and staged paid value.
Original-backend integration and independent tax references remain missing.

## 23. Runnable local preparation application, revision 7

The separate local_app 0.1.0 now connects actual Java components to a real
loopback HTTP API and English browser interface. It implements amount proposals,
explicit confirmation, review resets/prerequisites, signed input guidance,
amount-free change history, bounded retry and blocked result status. Server-only
controls stay UNAPPROVED/PENDING/UNKNOWN; no tax amount is calculated or exposed.

31 real HTTP groups pass, including stale/retry/session/decoding/CSRF/Host/Origin
checks and complete preparation that stays blocked. The existing 0.1.4 core JAR
and prior 205-group evidence are unchanged; these were not rerun as new tests.
JavaScript syntax passed. Browser visual/click/keyboard verification is unrun:
managed preview could not load Java libjli.so. See local_app_implementation_v0_1.md.

Run python3 local_app/run.py with Java 17+ and Python 3, then open the exact
http://127.0.0.1:8787 address. Sessions are temporary memory, not saved cases.
Original Spring/MySQL/calculator, live CA workflow, production signing/identity,
filing, payments and load/security validation remain pending. Golden workbook
and JSON remain byte-identical with thirty inputs and zero approved expectations.

## 24. Scoped arithmetic and local workbench integration, revision 8

New tax_engine 0.1.0 fills the arithmetic gap with a BigDecimal implementation,
a separately coded integer-paise verifier, twenty private stages, exact arithmetic
comparison/ties and declared eligible-80C sensitivity. The period and adult-ROR
salary/ordinary-interest scope are explicit. Standard deduction, separated old
interest deductions, age/April1, rebate, relief-before-cess and rounding are tested.
80D is an already eligible input, not an implemented premium/eligibility workflow.

Local app 0.2.0 adds profile/screening/deduction confirmation and real planning-
readiness HTTP integration. It binds current server-owned income and all profile/
artifact identities, signs candidates and uses the existing result guard. The
browser receives no tax amount/stage/comparison; rules/references/availability
remain UNAPPROVED/PENDING/UNKNOWN. No approval switch or synthetic production
reference was introduced.

Observed: 64 engine groups, 4,096 generated cases, twenty altered stage fields,
eight compiled defects detected, sixteen reference-checker groups and forty-six
real HTTP groups. Core 0.1.4 and prior evidence remain unchanged. A separate blank
reference template and checker preserve all thirty original golden inputs and
zero approvals. Matching asserted reference metadata never grants release.

START_HERE.md provides launchers and private review commands. This completes the
implemented local preparation/private-arithmetic/reference-checking path. It does
not complete a production Spring/MySQL tax/CA platform, actual professional
credential/consent/evidence workflows, billing, broader schedules/GST/full ITR or
filing. Browser rendering/click/keyboard, load, SQL/recovery, utility/CA numerical
validation, HF inference and VAPT remain unrun. See tax_engine_implementation_v0_1.md.

## 25. Normalized source comparison and local review, revision 9

Local app 0.3.0 adds normalized sample statement comparison and explicit local
source selection/reopen. Gross income and TDS are independent; missing values,
conflicts, repeated observations and declared value kinds stay visible. No rows
are silently deduplicated, no category totals are made and no tax facts/credits
or filing permissions are changed. This is not native AIS/26AS/PDF parsing,
source authentication, official feedback or real CA review/sign-off.

Observed: 38 comparison Java groups, 81 generated amount-pair combinations,
eight actual-thread review rounds, four compiled defects detected and 60 real
HTTP groups (prior 46 plus fourteen new). Exact accepted retries never restore
older batches/choices. Full income identity and per-review version binding reject
stale work; sources remain in bounded temporary session memory. See
statement_comparison_v0_1.md, local_app/examples/statement_comparison.csv and
START_HERE.md. Browser QA and production SQL/identity/CA/filing remain pending.

Guard core 0.1.4 and private engine 0.1.0 are unchanged. Both original golden files
remain byte-identical, with thirty input cases and zero approved expectations.
