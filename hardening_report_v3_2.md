# Hardening report v3.2

31 October 2026. Standalone Java component 0.1.1. Public projection remains
result-safety.v1; signed candidate format is tax-platform/signed-candidate/v1.

## Outcome

Fixed a reproduced evidence-ordering bug in the prior standalone component,
implemented an Ed25519 signed-candidate delivery boundary, and added an exact
English/Hindi amount parser whose output cannot silently become confirmed facts.
The original application was not supplied, integrated, deployed or load-tested.

## F43: mismatch evidence could be lost behind a reference gate

The 0.1.0 service asked the policy for a public result before deciding whether to
write quarantine. With references PENDING, the policy returned a reference block
before the verifier-mismatch branch. The service therefore never recorded the
mismatch. Later approval under the same calculation identity could expose an
older cached MATCH, even though authentic disagreement had already been observed.

This was reproduced with a failing regression before changing service behavior:
50 pre-existing scenario groups passed; the new regression failed. The fixed
service validates authority/integrity/restore binding, then records an authentic
mismatch before reference, rule, freshness and ordinary fact gates. Previously
quarantined identities stay blocked. A failed write blocks the current request.
The selected skip_early_mismatch_quarantine mutation now makes three groups fail.

For cached/untrusted candidates, signature verification runs before any mismatch
write. Bad signatures, unauthorized callers and wrong resources cannot poison
another calculation's quarantine. Production persistence/fencing is still an
adapter obligation, not established by the in-memory regression fixture.

## Signed candidate boundary

- Uses JCA Ed25519, a fixed algorithm and a versioned bounded binary signing
  format. No custom cryptographic algorithm, remote key URL or embedded private key.
- Signs all ten identity fields, all fact states/values, income and deduction
  inventory, scope, both regime amounts/verification states, producer key ID and
  timestamps. UNKNOWN has no numeric slot. Map insertion order is irrelevant.
- Checks fresh key-registry state on every delivery: exact ID, ACTIVE status,
  CALCULATION_PRODUCER purpose, current validity and issue/expiry validity.
  Approval-only keys cannot authorize calculation proofs.
- Enforces a configured bounded lifetime/skew. Expired, future, forged, tampered,
  revoked and missing-key proofs block numerical delivery. Registry outage blocks
  operationally; it never returns a cached amount as a fallback.
- Authenticity grants no rule approval, legal correctness, user access, definitive
  comparison or filing permission. Current policy/control gates still apply.

`SignedResultService` is the entry for cache/untrusted delivery. The raw
`SafeResultService` API remains for already trusted internal candidates and has
no cryptographic check. Integrators must not use that API as a cache shortcut.
The exact producer format and trust obligations are in
java_result_safety/SIGNED_CANDIDATE_V1.md.

## Human amount parser

Supports explicit INR prefixes, rupees/thousand/lakh/crore, selected magnitudes,
English/Hindi suffixes, valid Indian/western grouping, ASCII or Devanagari digits,
and exact decimal scaling. Bare numbers require a unit choice. Conflicting units,
mixed digit scripts, signs, exponent notation, malformed grouping, foreign
currencies and values requiring sub-paise rounding are rejected. Empty is missing;
explicit zero is a value. No failed parse produces a numeric default.

The v1 bound is 64 UTF-16 input characters and nine fractional digits before
scaling, sufficient for paise expressed in crore. Money retains the thirteen
integer-digit/two-decimal transport bound. There is no statutory rounding here.
Every successful parse requires confirmation and supplies Fact.Estimated, so the
result policy still blocks complete tax until the actual confirmation workflow.
No confirmation ledger, HTTP endpoint or persisted fact mutation was implemented.

Opaque IDs now use bounded ASCII, avoiding replacement-prone Unicode in signing
identities. Conflict lists are defensively copied and bounded to 2..8 ordered
values. These deliberately tighten the 0.1.0 input contract; display names are
outside the ID fields. Public JSON response fields/capabilities remain unchanged.

## Observed checks

| Check | Result |
|---|---|
| Compiler/runtime | OpenJDK 17.0.20; --release 17; UTF-8; -proc:none; -Xlint:all; -Werror |
| Policy scenario groups | 52 passed |
| Proof/service scenario groups | 31 passed |
| Parser/confirmation scenario groups | 21 passed |
| Total named groups | 104 passed, 0 failed |
| Combined policy guard cases | 2,048 |
| Generated exact amount cases | 4,096, across four magnitudes and two scripts |
| Signed-field changes | 66 rejected mutations of candidate/header data |
| Selected compiled defect variants | 10/10 detected through behavioral failures |
| Public response examples | Six actual JSON outputs parsed; bytes match across two JVMs |
| Canonical signing fixture | 650 bytes; bytes match across two JVMs |
| Negative compilation checks | Missing unknown accessor, unpermitted result implementation/construction, raw candidate delivery to signed service rejected |
| JAR | 53 library class files; test/ephemeral-key fixtures excluded |
| External Java dependencies/network | None needed for build/tests |

Generated cases are bounded engineering checks, not independently approved tax
scenarios. The 104 named groups include groups which execute generated cases;
these counts should not be combined into a marketing accuracy denominator.

## Mutation evidence

Every variant first compiled successfully. Compiler rejection was not counted as
detection. Original production sources were preserved; only temporary copies changed.

| Deliberate defect | Failing named groups |
|---|---:|
| allow_pending_independent_references | 3 |
| allow_unconfirmed_income | 6 |
| allow_stale_calculation_identity | 3 |
| allow_unavailable_regime | 3 |
| skip_early_mismatch_quarantine | 3 |
| accept_forged_signature | 9 |
| ignore_revoked_key | 2 |
| ignore_proof_expiry | 1 |
| auto_confirm_human_amount | 13 |
| ignore_conflicting_magnitude | 1 |

Selected mutation checks do not establish exhaustive coverage. Proof field
changes and source-code mutations are different experiments; counts are separate.
Machine reports live under java_result_safety/build.

## Remaining release dependencies

1. Authoritative production access/control and key-registry adapters, producer
   signing/key custody/rotation, durable quarantine and cross-node failure fences.
2. Actual Spring/HTTP integration with a strict request decoder and response
   projection, private/no-store policy, authorization and export/action checks.
3. Real MySQL race/crash/recovery tests and a coherent control/key decision point.
   The component does not atomically bind two remote registries or retract bytes
   already delivered; replica lag and mid-request revocation need explicit handling.
4. Independent tax references, reviewed law applicability and a runnable original
   calculator/verifier. The 30 golden inputs remain unchanged with zero approved
   expected outputs. No expected value was generated from this component.
5. Load evidence, staging security review and any affected filing/settlement gates.

The earlier Python protocol checker remains bounded abstract design evidence;
its state model did not include the reference-gate ordering in F43. Its unchanged
results were not rerun or represented as proof of this Java change. No Hugging
Face inference or document benchmark was run in this hardening task. The existing
HF evaluation remains a candidate/evaluation plan, outside the numerical path.

## Primary implementation sources

- [Java 17 Signature API](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/security/Signature.html)
- [Java 17 provider documentation](https://docs.oracle.com/en/java/javase/17/security/oracle-providers.html)
- [OWASP input validation](https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html)
- [OWASP key management](https://cheatsheetseries.owasp.org/cheatsheets/Key_Management_Cheat_Sheet.html)

These sources inform API and boundary choices; local execution evidence comes
from the actual build, regression and mutation runs described above.
