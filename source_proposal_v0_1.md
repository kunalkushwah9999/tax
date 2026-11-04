# Selected source to income proposal v0.1

1 November 2026. Implemented in local app 0.4.0. This is a local synthetic-data
workflow, not independent tax validation, statement authentication or a saved case.

## Resulting flow

1. Load or paste normalized sample CSV and compare it with the current income draft.
2. Explicitly select a known-gross source observation and record a local reason.
3. For an eligible annual group, review its displayed gross and destination. Check
   both acknowledgements: complete annual category/classification and replacement
   of the existing category amount. Unseen payers or incorrect classifications
   cannot be detected from this CSV; the acknowledgements do not prove coverage.
4. Choose **Create income proposal**. The source gross replaces the current field;
   it is never added to it. The resulting amount is ESTIMATED, including an explicit
   zero or an amount equal to a previously confirmed input. Review resets follow
   the existing ledger. No TDS credit or other category is changed.
5. Inspect the current value in Prepare inputs, then separately choose **Confirm
   amount**. A minimal historical source link remains marked unverified.
6. An income snapshot change makes the comparison stale. Reimport/review sources
   before another transfer; this implementation does not silently rebase reviews.

The sample salary group can exercise this flow. Its agreeing observations produce
one 800000.00 proposal, never their sum or the 40000.00 TDS observation. The sample
savings/deposit groups intentionally contain unresolved gross issues and cannot
transfer just because a user selected one row.

## Server-owned acceptance policy

| Check | Required behavior |
|---|---|
| Actor and request | Current temporary cookie session, exact Host/Origin, POST, CSRF; no client actor/approval binding |
| Input binding | Current HMAC view token; full current CalculationKey inside the workspace |
| Source binding | Current comparison token, exact group and exact recorded selected observation |
| Supported category | Salary, savings interest, qualifying deposit interest or other interest; OTHER_INCOME rejected |
| Period | Normalized sample FY2025-26 only; local case identity remains DEMO-NOT-A-TAX-YEAR, so no real cross-year case eligibility is claimed |
| Coverage | ANNUAL and exactly one group for that category in the entire imported batch, even across payers/references/annual/transaction rows |
| Gross clarity | Only single-source or cross-source gross agreement; every simultaneous gross flag checked rather than trusting a primary label |
| Displayed value | Exact selected known gross; client destination must match the server category mapping |
| User intent | Explicit YES for whole-category/classification review and replacement; no implicit consent |
| Confirmation | Existing ProposeAmount command only; never ConfirmAmount or automatic review |

TDS missing/conflicts are retained independently. They neither block an otherwise
eligible gross proposal nor become income, credits, refund or settlement amounts.
No aggregation or native-schema coverage inference is implemented. When there are
multiple annual groups, manually prepare a separately checked annual total; this
route does not guess how they overlap. A user declaration of a source label/value
kind, including AIS ACCEPTED, does not authenticate it or establish an approval.

## Endpoint, retries and historical provenance

POST `/api/reconciliation/propose` accepts exactly nine flat UTF-8 form fields:
`request_id`, `view_token`, `comparison_token`, `group_id`, `observation_id`,
`displayed_amount`, `field`, `whole_category_confirmed`, `replace_input_confirmed`.
The body is at most 4096 bytes; duplicate/missing/extra fields, invalid money,
malformed encoding and unsupported boolean spellings reject without fact writes.
The route uses the existing bounded generic FormCodec with its own exact field
allowlist; the older eight-field edit/review routes are not loosened.

Accepted source and manual income commands share the ledger's 100-request budget
and HTTP retry namespace. A domain-separated fingerprint binds the exact accepted
form, with its original full key and amount-free source reference. Identical
accepted retries go directly to the authoritative existing ledger receipt, even
if income or the comparison changed later. They cannot restore an old amount,
demote later confirmation, relink an old source or erase a newer source link.
Changed or cross-route reuse of an accepted request ID is a conflict. Rejected
attempts do not reserve IDs. After a lost response the UI blocks other writes
and keeps the exact action for retry; it refreshes current state, never paints the
historical request amount over the current value.

At most four current source links hold an amount-free reference plus an internal
process-keyed, session/field/domain-bound HMAC value tag. No historical Money or
raw request body is retained in this HTTP metadata. The projection checks the
current fact amount against the tag and never exposes the tag or private keys.
A source reference contains field, declared source/value kind, observation ID and
period; no payer alias/reference is copied. It is explicitly historical and
unverified. Source reimports do not turn it into a current reconciliation approval.
Manual new PROPOSE/CLEAR actions remove that field's link, including NO_CHANGE;
manual REPLAYED commands leave current links alone. Exact confirmation keeps a
matching link while changing the separately shown current fact state.

All operations share the existing Session monitor in one process. This is not a
durable transaction, cross-node ledger, tamper-proof audit, crash-recovery or
professional evidence chain. Process exit loses facts, keys, receipts and links.

Comparison-only receipts/events now allow **512** accepted import/review actions
(previously 100), enough to review up to 200 observation groups and reopen them
within one bounded session. Income edits still allow 100 IDs, planning 100 requests,
session lifetime 30 minutes, at most 100 sessions, four workers/64 queued requests,
and 120 API requests per minute per session. These are configured bounds, not load
measurements. Retry receipts remain available at their respective capacity.

## Observed engineering evidence

| Layer | Executed result |
|---|---|
| Strict Java 17 build | UTF-8, -proc:none, -Xlint:all, -Werror passed |
| Source proposal policy | 28 named groups, 36 generated category/gross combinations passed |
| Source defect experiments | Six independently compiled copies detected: transaction admitted, overlapping category ignored, TDS for gross, auto-confirmation, source retry relinking, manual retry unlinking |
| Comparison regressions | 38 named groups, 81 generated gross/TDS combinations, eight actual-thread review rounds passed |
| Comparison defect experiments | All four selected compiled variants detected; original sources preserved |
| Real loopback HTTP | 79 groups, zero failures/errors; includes prior 60 plus nineteen source-proposal groups |
| Narrow concurrency | Identical source retries make one edit; distinct new source writes from the same view cannot both apply |
| Request budget | Source proposals share the actual existing 100-command income ledger limit; accepted retry works at capacity |
| Extracted package | Packaged Python launcher started without rebuilding; six selected actual HTTP groups passed, and both archive manifests verified |
| Public result | Source proposal, confirmation and completed preparation do not unlock tax amounts, export or filing |

Source experiments include two actual-HTTP behavioral failures; compilation
failures are not counted. This selected set is not exhaustive fault coverage.
JavaScript syntax and literal HTML IDs/sample parity are checked separately.
Browser rendering/click/keyboard/accessibility remains **unverified** because the
managed preview Java runtime could not load libjli.so. No alternate browser
infrastructure or security relaxation was introduced. Packaged extraction/launcher
and checksum checks are recorded in the release packaging evidence.

The unchanged core/engine suites were not rerun as new work. Their prior evidence
is retained, not counted as new source tests or independent reference validation.
No official utility, CA sign-off, MySQL/recovery, model inference, load test or
security audit ran for this change. Both original golden files remain byte-identical
with 30 inputs and zero independently approved numerical expectations.

## Artifacts and next dependency

Run `python3 local_app/run.py` and open `http://127.0.0.1:8787`; Java 17+ and Python
3 are required. Source, compiled JAR, tests, exact CSV sample and launchers are
included in both updated archives. Full JDK modules are needed only to rebuild.

Recover the original backend for Spring/MySQL integration; independently validate
native schema/period/revision/coverage adapters before real documents. Add durable,
version-bound provenance and trusted identity/credential/consent adapters as their
own work. Obtain qualified independent golden stage expectations and authoritative
rule/control/signing release before exposing numerical tax results. Do not infer
CA-need rates, commercial superiority or tax accuracy from these engineering tests.

Built app JAR SHA-256: `3d1e18e306cac75647a1e3df8475354f02fe13ce393fd32186f982cafa1d8d4a`.
