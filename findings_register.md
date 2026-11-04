# Findings and acceptance criteria

P0 items block a public tax claim or sensitive-data workflow; P1 items block their affected feature. These are audit findings, not evidence that a patch has been applied.

| ID | Priority | Finding | Required resolution | Acceptance evidence |
|---|---|---|---|---|
| F01 | P0 | IncomeTaxCalculator source subtracts standard deduction from salary plus interest. | Cap deduction at salary, once per year, before aggregating interest. | G14,G15,G23 |
| F02 | P0 | Missing 87A marginal relief creates a false new-regime cliff. | Use rounded total income; compute relief before cess from the applicable rule pack. | G04-G10 |
| F03 | P0 | Old and new rebate behavior has been conflated across chat revisions. | Keep distinct old/new rules. Under-60 old-regime cliff is real; senior variants differ. | G11-G13,G19-G22 |
| F04 | P0 | Code rounds slab tax/cess to rupees and omits statutory ten-rupee stages. | Write stage-by-stage utility-matched rounding rules; distinguish liability from settlement. | G05-G13; extra paise and credit pairs |
| F05 | P0 | Single other-income bucket cannot establish interest deduction eligibility. | Split savings, qualifying deposits and nonqualifying interest; apply 80TTA or 80TTB only if eligible. | G15-G22 |
| F06 | P0 | Income UNKNOWN can be passed as a numeric zero placeholder. | Use typed unknown with no numeric value; require facts or label a user-requested hypothetical. | G29 |
| F07 | P0 | A partial calculation still carries a recommended regime and saving. | Block complete totals/recommendations in unsupported cases; enforce output capabilities on the server. | G25-G30 |
| F08 | P0 | Income completeness and deduction completeness are merged. | Track separately; incomplete deductions keep comparison provisional. | G30 |
| F09 | P0 | ITR-1 form eligibility is confused with product support. | Use two gates. Limited 112A / property income may be legally eligible while unsupported by this starter. | G24-G28 |
| F10 | P0 | Age bucket is user-entered or based on current date. | Use DOB plus relevant FY and applicable age rule. Add April 1/2 and leap-day review cases. | G18-G22 |
| F11 | P0 | 182 days is treated as a complete residency answer. | Do not infer ROR from one threshold; route uncertain history to a reviewed residency determination. | G26; NRI/history backlog |
| F12 | P0 | AY 2026-27 and TY 2026-27 are treated as interchangeable. | Key rules/forms by Act, income period and AY/TY. New-law pack stays disabled until independently reviewed. | Year-mismatch rejection tests |
| F13 | P0 | Golden expectations do not exist; agreement sweep is called correctness. | Obtain independent worked outputs with evidence and human review. No approval from A==B alone. | All 30 draft cases |
| F14 | P0 | Malformed slab/rule data can be consumed by both implementations. | Validate ordering, coverage, bounds, rate domain, applicability and bundle integrity before calculation. | Gap/overlap/duplicate/invalid-rate mutation tests |
| F15 | P0 | Rule version number is used as age/regime variant. | Separate variant dimensions from immutable revision; publish an atomic bundle. | Rule-key and ambiguous-resolution tests |
| F16 | P0 | Rule publication constraints are described but not enforced in DDL. | Transactional publication with approvals, corpus coverage and test-run evidence; no vacuous pass on zero tests. | Direct SQL publish denied; concurrent publication tests |
| F17 | P0 | Rules can change between computing old and new outcomes. | Resolve and freeze one compatible comparison manifest in Tx1; load by immutable ID outside the transaction. | Publication during calculation |
| F18 | P0 | Original orchestrator compares an old version after incrementing it itself. | Verify input snapshot pointer, run ID and policy revision at commit, not an off-by-one version. | Fact edit during compute |
| F19 | P0 | Reaper/retry can allow a late worker to overwrite a newer run. | Lease expiry plus fencing token plus active_run_id compare-and-set. | Crash/retry/late-finisher test |
| F20 | P0 | Idempotency is missing engine/verifier/schema revisions; active-only uniqueness loses completed dedupe. | Separate logical calculation identity from retry attempts and request idempotency. | Same input new engine; retry after timeout; same key changed body |
| F21 | P0 | PAN SHA-256 lookup is vulnerable to enumeration; global uniqueness leaks relationships. | Tenant-scoped HMAC lookup plus separate AEAD encryption; generic error responses; rotation migration. | Cross-tenant lookup, tamper and key-rotation tests |
| F22 | P0 | PAN last4 + DOB is presented as an independent step-up factor. | Use a bound passkey/TOTP or another verified independent channel; static identity facts are insufficient. | Sensitive export on a new device |
| F23 | P0 | UUIDs/roles in JWT can be mistaken for authorization. | Check tenant, profile access, consent scope and expiry on every object action and background task. | Two-user, two-tenant, expired-grant tests |
| F24 | P0 | Delete raw documents in 24-72h is an unconditional product promise. | Lawyer-reviewed retention matrix, holds, processor/backups handling and deletion evidence before promising a time. | Hold vs erase; restore after erasure |
| F25 | P0 | Category-only investment suggestions are assumed safe with a disclaimer. | Keep personalised allocation/ranking disabled pending advice/compliance review. | Boundary tests on recommendation intent |
| F26 | P1 | Tax refund/payable is inferred without credit and interest schedules. | Separate annual liability from settlement and filing readiness. Unknown TDS is not zero. | TDS unknown / overpaid / belated tests |
| F27 | P1 | Gross Form 16 salary and already-deducted salary can be mixed. | Explicit input semantic labels and source fields. Reject/detect double deduction. | Multi-employer gross vs taxable salary fixtures |
| F28 | P1 | Hash proves integrity even when attacker can replace payload and hash. | Authenticate bundles and approved releases with a signing key outside application/DB privileges. | Payload-plus-hash tamper and rollback tests |
| F29 | P1 | Only snapshot/hash/version string is retained for replay. | Retain canonicalization version, code/container digest, signed manifest and permitted source evidence. | Historic replay after upgrade |
| F30 | P1 | Outbox is described as exactly-once delivery. | At-least-once delivery plus consumer/event dedupe, retries and dead-letter handling. | Crash after send before mark-published |
| F31 | P1 | OCR confidence is treated as calibrated tax-field correctness. | Field-level evidence, arithmetic reconciliation, explicit confirmation and holdout evaluation. | Missing minus, decimal, duplicate Form 16, multi-page tables |
| F32 | P1 | Prompt separation alone prevents malicious document instructions. | Tool-less extraction sandbox, restrictive output schema and independent validation; no direct ledger writes. | Document injection and cross-tenant exfiltration corpus |
| F33 | P1 | Millions of scenarios are claimed without bounded search or an execution model. | Approved strategy templates, eligibility pruning, budget, cancellation, Pareto options and execution tracking. | Budget exhaustion, partial execution, low-liquidity tests |
| F34 | P1 | Concurrent users are treated as measured throughput. | Workload model and load/failure tests for DB, uploads, OCR and anonymous calculation separately. | Measured p95/p99 and queue age at a declared load |
| F35 | P1 | Admin control can override tax/approval/security invariants. | Server and DB controls outside commercial admin authority; signed rule release and audited break-glass. | Privilege escalation and maker-checker bypass tests |
| F36 | P1 | Final user filing confirmation is called FILED/VERIFIED. | Keep USER_REPORTED_FILED separate from provider-acknowledged submission and e-verification. | Missing acknowledgement and incomplete e-verification |
| F37 | P1 | Published 100% accuracy / zero mismatches can hide abstentions. | Report eligible denominator, coverage, abstention, per-field error and severity with sampling uncertainty. | Rejected cases remain in denominators |
| F38 | P1 | Stack changes and extra agents are treated as product progress. | Keep Java/MySQL decisions stable unless a measured requirement justifies migration; validate a narrow customer problem. | Reproducible build + reviewer/customer pilot evidence |
| F39 | P0 | The lowest computed tax can belong to an unavailable regime election. | Resolve regime availability from reviewed legal/filing context; unknown cannot yield a definitive recommendation. | Independent election/history/deadline fixtures; not tested by protocol model. |
| F40 | P0 | A committed or cached result can outlive fact/rule/access validity. | Revalidate the current eligibility envelope at every result/export delivery decision. | Model read-after-edit/revoke histories; real API/replica/export tests still required. |
| F41 | P0 | A new mismatch can block only its run while an older matching cached answer stays verified. | Quarantine the calculation identity and reject cached/current reuse; reviewed remediation gets a distinct validation identity. | Model audit mismatch after cached result; production quarantine fanout/recovery tests required. |
| F42 | P0 | Backup restore can resurrect superseded access, recalled answers or erased records. | Isolate restore; reconcile protected current control history and permitted deletion/hold state before serving. | Actual restore drill with revocation, recall, quarantine, erasure and key rotation; not yet executed. |

## Revision 3 component finding

| ID | Priority | Finding | Resolution status | Acceptance evidence |
|---|---|---|---|---|
| F43 | P0 | In standalone 0.1.0, a pending-reference policy response could suppress the service quarantine write for an authentic mismatch, allowing earlier MATCH reuse after approval. | Fixed locally in 0.1.1: verify authority/integrity and producer proof, then capture mismatch before ordinary policy gates. Production durable adapter/fencing still pending. | Regression failed before fix and passes after; selected skip_early_mismatch_quarantine mutation compiles and causes three scenario failures. |

This row describes a bug found in our prior standalone component. It does not
claim that the absent original application had this exact implementation.
