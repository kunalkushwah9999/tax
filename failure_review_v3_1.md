# Failure review, revision 1

31 October 2026. This extends the foundation pack with four additional controls and an executable protocol model. It does not certify the tax calculations or a production application.

## What changed

The current user instruction permits any implementation language and puts quality first. Technology choice remains evidence-based: retain the agreed Java/MySQL core, Python document/evaluation tooling and TypeScript UI unless a measured requirement justifies a specific change. This revision uses Python's standard library to make concurrency/delivery assumptions executable; it does not replace the application stack.

The design needs to protect the entire lifetime of an answer: eligibility, input confirmation, calculation, commit, cache delivery, export, recall and recovery. A successful calculation at one point in time is not perpetual permission to show that answer as current.

## F39: cheapest regime may not be an available election

The earlier comparison contract asks which regime has the lower computed amount. It must also ask whether that taxpayer can legally use it in the relevant filing context. The official old/new FAQ ties non-business regime election to timely filing, while business cases have separate option/history rules. [T17]

Do not infer the answer solely from today's date or generalize a rule across original, revised, belated and future-planning cases. The legal pack must determine availability from Act/period, taxpayer/business status, filing context, applicable notified deadline, prior elections and supporting evidence. This revision does not calculate those legal conditions.

Add regime_availability = ALLOWED / NOT_ALLOWED / UNKNOWN, reason and evidence to the comparison contract. A labelled hypothetical can show a calculation under an unavailable option; a definitive actionable recommendation cannot select it. Unknown availability prevents a definitive recommendation. Add independent legal fixtures before exposing that feature.

Acceptance cases: lower old-regime arithmetic but unavailable election; availability unknown; prior valid election with a revised return; missing history; and future planner clearly distinguished from an actual return. Expected legal outcomes require professional review. They are not populated by this revision.

## F40: recheck at delivery, after successful commit

Existing Tx2 checks protect promotion. They cannot alone protect a cached result requested after facts change, a rule is recalled or a professional's grant is revoked.

Every result endpoint, cached-result response, export and download decision needs current server-side authorization and freshness/quarantine checks. OWASP requires permissions to be validated on every request; this design extends that rule with calculation freshness. [S01]

Use an immutable result payload plus a current eligibility envelope: tenant/profile/case grant, snapshot identity, applicable validation-policy epoch, rule recall status and quarantine status. Cached payloads are not cached permission. If authoritative eligibility cannot be established, do not silently return the payload as a current verified result.

The guarantee has a precise boundary: the access-decision point. Already delivered bytes cannot be recalled. Long-lived signed URLs and replicas with stale control state can introduce revocation windows; document and test those windows. A private artifact gateway and short-lived capabilities may be needed for sensitive downloads. Do not claim instantaneous global revocation from this model.

## F41: mismatch must quarantine matching cached answers

Consider this sequence:

1. Run A verifies input snapshot S under approved validation identity V; its answer is cached.
2. An explicit audit/run B recomputes that same identity and reports a mismatch.
3. B is blocked, but A's cached answer is still served as verified.

Blocking B alone does not resolve the contradictory evidence. At minimum, quarantine the exact calculation identity and prevent its current-result delivery or reuse, including cached answers and exports. Retain restricted historical evidence according to policy, with an explicit invalid status.

The identity includes canonical input/snapshot identity, rule manifest, engine and verifier artifact revisions, schema/canonicalization and calculation settings. Triage may require widening the scope to all results using an affected rule or engine release. A blind retry must not remove the quarantine. Recovery needs a reviewed fix and distinct approved validation identity; old quarantine records are not silently overwritten.

The reference model represents this identity as (snapshot, policy_epoch). It models a second attempt as an audit or controlled recomputation, not an excuse to drop normal request idempotency.

## F42: restore must recover current control state before serving

A backup may contain a formerly valid grant, an old verified result or a record subsequently erased. Restoring business tables while losing newer recalls, consent revocations, quarantine records or deletion instructions can resurrect access or misleading results.

Run restores in isolation with result delivery, exports, background sending and external side effects disabled. Reconcile the latest permitted control records and key/policy state, replay applicable revocations and deletion instructions, validate the restored data, then explicitly permit service. If the required control history cannot be recovered or its freshness established, fail closed for affected records.

Control records need their own protected recovery plan. This does not authorize permanent retention of personal information or tombstones: define minimal records, retention, erasure and legal holds with the privacy/legal policy. Rehearse an actual restore with a revoked grant, recalled bundle, quarantined identity, erased record and rotated key before releasing the affected workflow.

The executable protocol treats deletion as irreversible access denial inside its finite abstraction. It does not simulate backup recovery, storage erasure, key management or legal retention. F42 remains an implementation/operational acceptance task.

## Executable design evidence

Files: verification_models/calculation_protocol.py, test_protocol.py, check_protocol.py and protocol_check_results.json. The README gives reproducible commands. Only the Python standard library is used.

| Check | Observed result |
|---|---|
| Scenario tests | 22 passed |
| Reachable states checked | 10,016 |
| Non-no-op state transitions checked | 22,558 |
| Finite-domain closure | Exploration completed without an invariant violation |
| Deliberately injected faulty variants | All 8 produced counterexamples |
| Approved tax golden cases | 0; unchanged |
| Application/SQL/VAPT evidence | Not supplied or established by this model |

Finite bounds: two unique attempts, up to two immutable fact edits, one material replacement validation-policy epoch, and explicit success/mismatch, expiry, access/rule revocation and deletion events. Maximum shortest reachable trace length was 10. Completion within this finite domain is not a proof for arbitrary concurrency or a deployed system.

Safety assertions cover ownership of the active attempt, late-worker rejection, immutable snapshot identity, current policy at promotion, revoked access/rules at delivery and quarantine of cached calculations. Liveness, clocks, actual database isolation, network partitions, source authenticity, numerical tax correctness and security of an implementation remain outside the abstraction.

## Counterexamples from deliberate defects

These are injected mistakes in the reference checker, not discovered exploits in an existing application.

| Faulty variant | Shortest discovered event sequence | Violated condition |
|---|---|---|
| read_ignores_snapshot | start_A -> finish_A -> edit_facts | stale facts delivered as current |
| read_ignores_access | start_A -> finish_A -> revoke_access | result delivered after access revoked |
| read_ignores_rule | start_A -> finish_A -> revoke_rule | result delivered under revoked rule |
| read_ignores_policy | start_A -> finish_A -> publish_policy | old validation policy delivered as current |
| read_ignores_quarantine | start_A -> finish_A -> start_B -> mismatch_B | quarantined calculation delivered from cache |
| finish_ignores_snapshot | start_A -> edit_facts -> finish_A | stale facts promoted |
| accept_late_worker | start_A -> expire_A -> finish_A | terminal worker promoted a result |
| reaper_clears_other_attempt | start_A -> expire_A -> start_B -> expire_A | duplicate reaper cleared a newer active attempt |

## Production acceptance work

Translate these histories into tests against the actual Spring/MySQL implementation. Assert committed database state, affected row counts, API results and export behavior, not merely a mocked service response. Exercise crashes between statements, duplicate events, concurrent readers, replica lag and stale workers. Scope the tests to the system's own authorized staging environment.

Before a release, compare the candidate with the control using version-attributed metrics. Canarying can reduce the population exposed to a bad release, but an HTTP 200 rate is insufficient for a tax product: also track verifier disagreement, quarantine/recall state, incorrect freshness, unsupported-case handling and unexplained stage differences. [S02]

A release cannot be rescued by suppressing mismatches or routing users to an unverified engine. If the candidate's correctness cannot be established, pause its affected path. Diagnose and validate the fix with independent references before promotion.

## Sources added in this revision

- T17: [Income Tax Department, old/new regime FAQ](https://www.incometax.gov.in/iec/foportal/help/new-tax-vs-old-tax-regime-faqs?mobile-app=1). Supports the need for a context-dependent regime-election gate. Exact current deadlines/history outcomes remain unapproved inputs here.
- S01: [OWASP Authorization Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html). Permission validation on every request; static/export resources need protection too.
- S02: [Google SRE Workbook, Canarying Releases](https://sre.google/workbook/canarying-releases/). Candidate/control rollout and attributable evaluation. The tax-specific metrics above are this review's proposed controls.

## What this permits us to claim

The abstract protocol has exercised the listed finite event domain and rejected eight injected defects. The golden workbook remains an independent-reference collection draft. No tax application, real database, official utility, model inference, deployment load or penetration test has been validated by these results. The next implementation dependency is still the runnable project and approved tax references.
