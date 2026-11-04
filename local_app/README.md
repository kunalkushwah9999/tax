# Local preparation and calculation-readiness app 0.4.0

Start from ../START_HERE.md. The English interface now connects the 0.1.4 guard
core and the new private arithmetic workbench to a real loopback HTTP API.
It remains a synthetic local application with temporary sessions and no approved
numerical public tax result, saved cases, billing or filing.

## Run

With Java 17+ and Python 3 installed, from the extracted package root:

```sh
python3 local_app/run.py
```

On Windows use `py -3 local_app/run.py` or the included start.bat. Open the exact
http://127.0.0.1:8787 address; localhost is not an accepted Host. Stop with Ctrl+C.
A different port is `--port 8788`. Compiled JARs are included; no build, Maven,
npm, database, model, API key or network service is required to run.

Java can launch directly without Python:

```sh
java --add-modules jdk.httpserver -jar local_app/build/tax-preparation-local-demo-0.4.0.jar --port 8787
```

Keep both core/engine JARs in local_app/build/lib as packaged. Changing Java/web
sources requires an explicit rebuild; the launcher does not check timestamps.

## Customer flow

Propose gross salary before standard deduction and separated gross interest,
check the displayed INR values and explicitly confirm each, including zeros.
Complete income and both deduction review checkpoints. A material amount edit
clears those dependent reviews; stale edits and changed retry bodies are rejected.
The income checklist can reach 8/8, while profile screening and tax validation
remain separate.

Calculation check uses the current server-owned income snapshot. Enter sample
DOB/residency, six scope answers and already eligible 80C/80D/80CCD(1B) amounts.
An edited profile/deduction input clears its UI confirmation and earlier check
output. The server binds all submitted profile facts plus current income snapshot,
manifest and engine/verifier identities. A stale view token is rejected.

The server performs both arithmetic implementations and exposes only readiness,
issue codes and their agreement status. The actual signed result guard stays
UNAPPROVED/PENDING with regime availability UNKNOWN. No tax stages, amounts,
comparison or private operator workings are returned over this route. Agreement
is not independently verified tax correctness, deduction eligibility or legal
regime-election availability. These gates cannot be set through client fields.

History excludes monetary values. Restart revokes the case. Sessions expire at
30 minutes, are lost on process exit and have at most 100 accepted edit IDs and
100 planning requests. At most 100 sessions, four request workers, a 64-item
worker queue and 120 API requests/minute/session are configured; bootstrap is
excluded from that rate counter. These are bounds, not measured capacity.
Expiry denies access immediately; physical cleanup is opportunistic on opening
another session, not a formal retention/deletion service.

## Normalized statement comparison

The Compare statements tab accepts normalized synthetic CSV observations, with
separate gross/TDS differences, duplicate/missing/variant flags and explicit
source selection/reopen reasons. It has no native AIS/26AS/PDF reader or official
feedback/provider integration. Selecting a source changes no confirmed income,
tax credit or filing permission. It creates no category totals or independent
source authentication. Source rows remain visible; unknown is never zero.

A current input snapshot and opaque comparison version bind each review. New
source data clears previous choices; exact imports/retries preserve newer work.
Receipts/history are bounded to 512 accepted actions; only the latest parsed rows
are retained server-side, at most 200 observations/32 KiB CSV per session. Raw CSV
is not stored. Income-source proposals share the separate existing 100-command income edit
budget. State and local review are lost on restart/expiry; no durable case
or professional sign-off is implied. See ../statement_comparison_v0_1.md.

## Selected source to income proposal

After recording a local choice, an eligible whole annual group offers a separate
Create income proposal button. Check full-category/classification and replacement
acknowledgements. It replaces the category with the selected gross as ESTIMATED;
use Confirm amount separately. No values are added together and no TDS is moved.
Transactions, other income, multiple same-category groups and missing/conflicting/
repeated/variant gross observations cannot transfer. Unseen payers and correct
classification still require user review; this is no source/coverage authentication.

Current state shows a minimal historical unverified source link separately from
confirmation. New manual proposals/clears remove it; exact retries preserve later
state and links. Income changes make old comparisons stale. Reimport/review before
another transfer. See ../source_proposal_v0_1.md for exact policy, endpoint and
retry limits. The sample salary group works; sample savings/deposit groups block.

## Build and check

Keep tax_engine, java_result_safety and local_app as siblings. A full Java 17+
JDK with jdk.compiler, jdk.jartool and jdk.httpserver is required:

```sh
python3 tax_engine/build.py
python3 local_app/build_app.py
python3 local_app/test_http.py
python3 local_app/check_source_proposal_mutations.py
```

Compilation uses --release 17, UTF-8, -proc:none, -Xlint:all and -Werror.
79 actual HTTP groups passed: prior 60 plus nineteen source-proposal groups.
Source policy adds 28 named groups, 36 generated cases and six selected compiled
defects detected, including two real-HTTP mutations. Java comparison tests cover 38 groups, 81 generated amount pairs and
eight actual-thread review rounds; four selected compiled defects are detected. They cover real arithmetic readiness that stays blocked, profile/scope/
deduction unknowns, safe client errors, stale/isolation/CSRF/Origin/Host/form
controls, query bounds and an unchanged ledger/history after a planning read.
Actual app/core/engine artifact hashes are recorded in build reports.

JavaScript syntax was checked. Browser rendering/click/keyboard/accessibility QA
remains unverified: the managed preview runtime could not load Java libjli.so.
The HTTP client is not a browser. No VAPT, production load or security certificate
is implied.

## Boundaries

Sample data only; no PAN, Aadhaar, uploads, accounts or documents. Server inputs
and private operator workings are distinct from the restricted result projection.
Fresh in-process Ed25519 keys sign demo snapshots and calculated candidates;
none is an authoritative legal/rule release or professional signature. No private
key is persisted or exported, and no demo signer is inside the unchanged core JAR.

Host/Origin/CSRF checks, opaque HttpOnly SameSite=Strict host-only cookies,
full-identity edit binding, strict bounded UTF-8 form decoding, sanitized errors,
no-store and CSP/response headers remain implemented. Cookies are not Secure on
loopback HTTP. The new planning route caps bodies at 4,096 bytes and accepts an
exact field set; no approval/price/tenant/tax mass assignment is accepted.

Independent approved golden outputs remain zero. Qualified rule/reference
approval, authoritative production controls/signing, real Spring/MySQL/durable
case integration, professional credentials/consent, deployment, document
reconciliation, payments and filing remain separate work. See the engine report.
