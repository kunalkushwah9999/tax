# Tax result safety, Java module 0.1.4

Java 17 source and a compiled dependency-free JAR for deciding which tax-result
fields may be exposed. This implements a standalone part of Task 1. It is not a
tax calculator, full backend, filing system or deployment of the original app.

## Build and run checks

Requirements: Java 17+ with `jdk.compiler` and `jdk.jartool`, and Python 3.10+ for
the portable build runner. No Maven, Gradle, package installation or network is
required. The runner works through Java's installed compiler/tool modules.

```bash
python3 build.py
python3 check_mutations.py
```

On Windows, use `py -3 build.py` / `py -3 check_mutations.py` if that is your
Python launcher. An installed full JDK is needed, even if `java` can start.

Outputs:

- `build/tax-result-safety-0.1.4.jar`: library classes only; 99 class files, no test keys/fixtures.
- `build/test_results.json`: observed scenario/compiler/serialization results.
- `build/example_responses.json`: six synthetic policy examples, not tax-law examples.
- `build/mutation_results.json`: thirty selected test-only defects and observed failing tests.
- `build/canonical_vector.json`: synthetic signing-format digest and cross-JVM check.
- `build/guided_examples.json`: six actual input-preparation/result guidance projections.
- `build/fact_edit_examples.json`: five amount-free edit receipt/history projections.
- `build/professional_examples.json`: four amount-free review readiness projections.

Observed: 205 named groups passed (57 policy, 31 proof, 21 parser, 28 guidance,
42 edits, 26 professional), plus existing guard/parser/guidance/edit and actual-thread
checks. The new professional suite checks 1,296 evidence combinations. All thirty
selected defect variants compiled and were detected through behavioral failures.
These are bounded component checks, not tax golden-case approvals.

The JAR can be placed on an application's classpath. Alternatively, copy the
sources under `com.taxplatform.safety` into the actual backend module and run
its normal integration checks. No dependency version or build-file change to
an unseen application is assumed.

## Implemented behavior

| Condition | Public response |
|---|---|
| Cross-tenant, unauthorized or deleted resource | Generic access denial; no identifiers, facts or amounts |
| Missing control state, bad integrity, stale identity, restore not caught up, quarantine | Blocked; no amounts |
| Rules or independent release references unapproved | Blocked; no amounts |
| Any authentic supplied regime calculation disagrees with verifier | Whole result blocked; service calls quarantine before approval/freshness gates can hide the evidence |
| Verification has not run | Blocked; no amounts |
| Scope partial/unsupported/unknown | More information or supported-workflow action; no total tax |
| Income unknown, estimated, conflicting or inventory incomplete | More information; no zero substitution |
| Old deductions incomplete, new facts/eligibility complete | Only the complete eligible new-regime amount; comparison provisional; no definite saving |
| Regime availability unknown or disallowed | No actionable amount for that regime or definitive two-regime recommendation |
| Both regimes fully supported, confirmed, eligible and verified under approved references | Definitive synthetic-input comparison, including an explicit tie |

All amounts are decimal strings. They represent annual tax **before credits**,
not final payable/refund. Filing/export capabilities are always false in this
module. No paid-plan or admin override is part of the policy input.

## Trusted integration boundary

`Actor`, `Candidate` and `DeliveryContext` are server domain objects. Never bind
them from browser JSON, JWT custom fields or LLM output. The module consumes
authoritative decisions; it does not establish authentication, grant ownership,
legal applicability or CA approval by itself. The signed front door verifies
producer signatures against an injected current public-key registry; it does
not implement a production key registry, signing service or approval workflow.

The real adapter must load the authenticated actor and stored immutable run,
validate tenant/profile/case access, verify the candidate payload's integrity,
resolve current snapshot/validation identity and check independent rule-release
evidence. Missing evidence maps to PENDING/UNAPPROVED/UNKNOWN. This project's
golden references currently have **zero independent approvals**. Positive tests
use explicitly synthetic approved-state fixtures; they do not approve a tax rule.

`SignedResultService` is the required entry point for cached/untrusted candidate
delivery. It reloads control state and verifies an Ed25519 producer proof on every
read, including current revocation/purpose/validity and a bounded signed lifetime.
The proof covers the complete calculation identity, fact states/values, regime
calculations, verifier states, key ID and timestamps. A producer signature cannot
approve tax rules or turn missing references into approved references.

`SafeResultService.deliver(Actor, Candidate)` remains for already trusted internal
inputs. It does not verify cryptographic proofs and must not be used as a cache
or HTTP shortcut. `ControlPlane` and `KeyRegistry` are integration contracts;
in-memory test implementations are not production adapters.

The production quarantine implementation must be durable, idempotent and visible
to every result-delivery instance. A failed quarantine write cannot safely leave
other nodes serving older matching results: the control adapter must fence or
disable affected delivery until consistent state is restored. The module blocks
the failed request but cannot supply distributed persistence by itself.

The decision is valid at the control snapshot's access-decision point. Already
delivered bytes cannot be recalled. Handle replica lag, expiry, signed downloads,
backups and revocations in real database/HTTP integration tests.

## HTTP adapter requirements

Call `signedService.deliver(authenticatedActor, signedStoredCandidate)` and serialize only
the returned `PublicResult`, using `SafeJson.encode` or an equivalent strictly
matched framework projection. Never return `Candidate`, `CaseFacts`, internal
calculation records or arbitrary stored JSON directly.

Map denials to a consistent private-resource HTTP policy, set private/no-store
cache headers, and independently enforce authorization on every export/action.
The UI cannot grant a capability by changing a badge. This package does not
create an HTTP server, login, database, endpoint or download service.

`SafeJson` is a small closed-projection encoder to keep this module dependency
free. It is not a general JSON parser and accepts no client JSON. Framework
serialization during integration must be tested for the same fields and omission
rules. The build runner parses the actual six example responses with Python's
JSON parser and checks blocked-field omission, money strings and filing refusal.
The closed projection uses sorted object keys; the build also compares example
bytes from two independent JVM executions. This is not a general-purpose JSON
canonicalization implementation. The producer proof uses a distinct bounded,
length-prefixed binary format, documented in `SIGNED_CANDIDATE_V1.md`. Never sign
arbitrary JSON or assume framework reserialization preserves signing bytes.

## Types and limitations

Unknown facts have no numeric accessor/value field. Confirmed and estimated
values cannot be null. Conflict candidates and maps are defensively copied.
Money uses BigDecimal with exact two-decimal transport normalization, rejects
scientific notation/noncanonical input, and performs no statutory rounding.
Its thirteen-integer-digit transport bound is not a tax eligibility threshold.
`HumanMoneyParser` accepts supported rupee/unit prefixes, English/Hindi magnitude
suffixes, Indian/western comma groups and ASCII or Devanagari digits. Bare numbers
need an explicit selected magnitude; mixed scripts, signs, exponent notation,
conflicting units and sub-paise rounding are rejected. It handles at most 64
UTF-16 characters and nine fractional digits before exact magnitude scaling.
Every parsed amount requires confirmation and becomes `Fact.Estimated`, never
`Fact.Confirmed`. Currency symbols specify INR; the selected/suffixed magnitude
specifies rupees/thousands/lakh/crore. Reject negative corrections for a separate
reviewed workflow rather than silently taking an absolute value.

Opaque calculation/actor IDs now require 1..128 ASCII characters from
`A-Za-z0-9._:/-`; these are not display names. Conflict sets are bounded to 2..8
ordered values. These are intentional input-contract tightenings from 0.1.0.

Sensitive domain `toString` methods redact values and calculation identities.
That does not make arbitrary logging/telemetry safe; allowlist operational fields
and validate actual application error paths. Public response constructors are
restricted and the result hierarchy is sealed to reduce accidental bypasses.
Trusted application code remains responsible for using the guard correctly.

This module has not been integrated with Spring/MySQL, an existing calculator,
official utility, live identity system, production signing/key-registry service, real customer data,
load environment or penetration test. The test results establish the observed
scope of this local module only.

See `hardening_report_v3_2.md` in the full foundation pack for the quarantine bug, fix and
remaining boundaries. A valid signature establishes producer authenticity and
payload integrity under the configured key trust, not numerical/legal truth.

## Guided customer features, 0.1.2

IntakeGuideService supplies an authorized signed input-only preparation checklist,
focused next step, progress label and amount-free preparation note before a tax
calculation or approved reference exists. It cannot expose numerical results.
GuidedResultService wraps the signed result guard with explanations and regime
status cards; blocked/denied result guides omit private checklist details.

Both require signed inputs and current control state. These are separate response
contracts, not a result-approval bypass. Input review progress never grants filing
or implies legal completeness. The actual PWA, HTTP handlers and confirmation
ledger remain pending. See GUIDED_FEATURES_V1.md and the full pack's
guided_features_v0_1.md for exact behavior and observed tests.

## Confirmation and edits, 0.1.3

EphemeralFactLedger implements explicit proposal/confirmation/conflict resolution,
dependent-review resets, expected-key protection and safe accepted-request retry.
An edit publishes a distinct current snapshot so a prior signed calculation is
stale at a later read. Amount-free receipts/history grant no filing authority.
The ledger reloads independent current policy for result delivery and implements
the existing ControlPlane boundary; commands cannot approve tax or change scope.

This is bounded single-process session memory, not a saved-case database. Process
exit loses it. Idle physical cleanup requires application maintenance; no exact
erase deadline, distributed transaction or durable audit is claimed. The actual
HTTP/PWA/authentication/signing integration remains pending. See FACT_EDITS_V1.md
and the full pack's confirmation_flow_v0_1.md before using these interfaces.

## Professional review readiness, 0.1.4

ProfessionalQueueService produces bounded, amount-free, per-case authorized review
routing with explicit evidence states. Readiness is not approval, evidence receipt
is not verification and professional need/accuracy rates remain unmeasured. Paid
status cannot bypass a result gate. This does not implement a live CA portal,
credential validation, consent capture, review sign-off, billing or filing.
See PROFESSIONAL_QUEUE_V1.md and the full pack's ca_premium_strategy_v0_1.md.
