# Local application implementation history

## Current 0.4.0 checkpoint

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

## Earlier 0.3.0 checkpoint


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


## Earlier 0.2.0 checkpoint

1 November 2026. Local app 0.2.0 now includes profile/screening/deduction
confirmation and the new actual arithmetic workbench behind a nonnumerical
planning-readiness API. 46 real HTTP groups pass. Engine: 64 named groups,
4,096 generated cases, eight compiled defects and sixteen reference-checker groups.
The existing core remains 0.1.4 unchanged. START_HERE.md and
tax_engine_implementation_v0_1.md describe the latest code, commands and boundaries.
No independent numerical golden output or production tax release is approved.

## Original 0.1.0 baseline

The following records the earlier preparation-only app and its original evidence.

The previous Java components now run together behind a real loopback HTTP API
and an English browser interface. This is an implemented sample preparation
application, separate from the missing original tax-calculator repository.
It is not a production service, a full PWA or a validated tax calculator.

## Implemented customer flow

| Action | Actual behavior |
|---|---|
| Open sample | Create a bounded, temporary server-owned session; all four monetary inputs start Unknown |
| Enter salary and separated interest | Strict form decoding feeds the existing exact Hindi/English parser; successful proposals remain Estimated |
| Confirm displayed input | Existing ledger checks the current snapshot and the exact displayed amount before confirmation |
| Review checkpoints | Income review requires confirmed inputs; regime checkpoints require income review; an amount edit clears all three reviews |
| Follow checklist | Existing signed input guidance supplies next action, eight-item review status and an amount-free preparation note |
| Check result | Existing signed result guard returns blocked; input progress cannot approve rules/references or enable filing |
| Inspect changes | Actual ledger events, without raw amount strings or monetary values; not a durable professional audit record |
| Retry an interrupted edit | The same request body/ID reuses the original full key, even after later edits; conflicting reuse is rejected |
| Refresh or restart | Refresh reads the current session; restart revokes it and opens a new sample; process exit loses all state |

The sample button only proposes synthetic amounts. It never confirms them or
marks a review complete. A complete checklist is preparation progress, not an
accuracy score, eligibility decision or CA approval. The professional portal
remains unavailable; the premium roadmap and read-only queue contracts in
ca_premium_strategy_v0_1.md are retained.

## Implementation and API

The separate JDK HTTP application uses the existing library JAR rather than
copying its policy/ledger into JavaScript. No third-party runtime dependencies
or network services are required. The bundled web assets use native DOM APIs,
textContent and exact decimal strings/BigInt formatting, without analytics,
external fonts, uploads or a general chatbot.

| Route | Purpose | Boundary |
|---|---|---|
| GET /, /app.js, /styles.css | Bundled application assets | Exact whitelist; no arbitrary paths or query strings |
| GET /api/session | Open/reuse temporary session | Exact loopback Host and optional Origin checks; no client actor/approval binding |
| GET /api/state | Read current sample inputs and guide | Current session cookie; explicit input projection, separate from result |
| GET /api/history | Read session events | Current session; amounts/raw inputs omitted |
| GET /api/result | Read guarded result status | Current signed draft and actual result guard; no numerical candidate exists |
| POST /api/edit | PROPOSE / CONFIRM / CLEAR / REVIEW / UNREVIEW | Current session, exact Origin, CSRF, operation-specific form fields and identity-bound view token |
| POST /api/close | Revoke temporary session | Same Origin/CSRF controls; empty body |

The server binds only 127.0.0.1. Requests must use its exact local authority;
Host aliases, arbitrary origins and cross-site API fetches are rejected. Opaque
host-only cookies are HttpOnly and SameSite=Strict. They are deliberately not
Secure on local HTTP. Every response is no-store with CSP, no framing, nosniff,
referrer and permissions restrictions. These are implemented controls, not a
VAPT or a claim of production security.

The request view HMAC includes the session and all ten CalculationKey fields.
The browser cannot supply tenant/profile identities, scope, tax output, plan
overrides, reference approvals or regime availability. Accepted retry metadata
keeps only the original key and body digest; both HTTP and ledger retries are
bounded. Form bodies are capped at 4,096 bytes, eight fields, strict UTF-8,
single percent decoding and exact field allowlists. Errors do not echo inputs.

This application generates a separate process-local Ed25519 key and signs only
the current input-only demo candidate. That signer is not included in the
unchanged core library JAR. Rule state is fixed UNAPPROVED, references PENDING
and regime availability UNKNOWN. The synthetic period is DEMO-NOT-A-TAX-YEAR.
No engine, external reference, tax figure or release approval is fabricated.

## Observed verification

- App compiled against the unchanged core with Java 17 target, UTF-8,
  annotation processing disabled, -Xlint:all and -Werror.
- **31 real loopback HTTP scenario groups passed**, zero failures/errors.
  They exercise complete preparation, unknown/zero, explicit confirmation,
  stale snapshots, isolation, same/changed retry IDs, four concurrent retries,
  origin/CSRF/Host/cookie rejection, strict decoding, request bounds, field
  injection, static traversal, history redaction, close and blocked results.
- Completing all four confirmations and three reviews reaches 8/8 while the
  result remains BLOCKED, without annual_taxes/comparison, export or submission.
  A subsequent amount edit clears the dependent review flags.
- Four simultaneous identical HTTP edits produce one APPLIED and three
  REPLAYED outcomes with one history event. This is narrow concurrency evidence,
  not a load test or distributed correctness claim.
- The standalone ZIP was extracted to a separate directory; its packaged launcher
  started successfully and served the page and a new session without rebuilding.
- JavaScript syntax passed Node's --check. App/core artifact digests are recorded
  in build/build_results.json and build/http_test_results.json.
- The core JAR is byte-identical to the previous 0.1.4 artifact. Its existing
  evidence remains 205 named groups and 30 selected compiled defects detected;
  these unchanged suites were not rerun or counted as new HTTP/browser tests.
- Both golden files are preserved against the previous foundation archive:
  30 inputs, every expected numerical field still null, zero approved outputs.

Browser visual/click/keyboard verification **was not completed**. The supported
managed preview failed because its Java runtime could not load libjli.so, after
repairing a preview launcher path. No alternate browser infrastructure or
application security relaxation was introduced. Responsive CSS, labels and focus
styles are implemented, but accessibility/rendering require a real browser pass.

No official utility/CA tax validation, original Spring backend integration,
MySQL/crash recovery, multi-node or load test, model inference, credential check,
filing, payment, deployment or independent security audit was performed.

## Run and next dependency

The standalone ZIP keeps local_app and java_result_safety as sibling folders.
Start with `python3 local_app/run.py`, then open http://127.0.0.1:8787. Bundled
JARs require Java 17+; Python 3 is used by the launcher/tests. A direct Java
launch and Windows instructions are in local_app/README.md. No build is needed
to try the packaged version. Rebuild explicitly after changing sources/assets.

Use synthetic amounts only. Sessions last at most 30 minutes, are bounded to
100 accepted request IDs and are not saved cases. The server caps sessions at
100 and workers at four; those limits are not benchmarked throughput. Expiry
denies access immediately, but physical memory cleanup is opportunistic on
session creation rather than a formal retention/deletion implementation.

Next obtain the original runnable calculator and independently approved golden
stage outputs, then integrate authoritative control/signing and real persistence
where needed. Validate the UI in a supported Java/browser environment. Keep tax
calculation and professional release gated until their own evidence is complete.

Implementation references: [JDK HttpServer](https://docs.oracle.com/en/java/javase/17/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html),
[OWASP CSRF prevention](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html),
[OWASP HTTP headers](https://cheatsheetseries.owasp.org/cheatsheets/HTTP_Headers_Cheat_Sheet.html).
These references inform engineering controls; they do not certify this build.
