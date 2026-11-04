# Confirmation and session edit flow v0.1

31 October 2026. Java component 0.1.3. This update implements a bounded
single-process anonymous-session edit ledger, not durable saved-case storage.
Original Spring/MySQL/PWA integration and the original tax engine remain absent.

## Implemented behavior

| Customer action | Observed behavior |
|---|---|
| Enter an amount such as 1 lakh | Strict parser creates an estimate; no automatic confirmation |
| Confirm a proposed amount | Exact displayed value must match the current estimate and full expected snapshot identity |
| Resolve conflicting values | Only an explicitly selected offered candidate is accepted |
| Clear a value | Unknown state; no zero substitution |
| Edit a material amount/state | New snapshot; income and both deduction reviews reset; earlier result becomes stale at a later read |
| Mark an inventory reviewed | Supported scope and current confirmed-income prerequisites required; no tax/filing approval |
| Submit from two stale tabs | One expected identity can accept only one distinct material edit |
| Retry an accepted request | One transition; later calls replay its receipt without another history row |
| Reuse a request ID with a changed body | Conflict; no silent overwrite or unrelated receipt |
| Read a receipt after later edits | Historical outcome ID only, explicit refresh requirement; no claim that old state is current |
| Close or expire the session | Current/history/replay and cached result access cease |

Input edits never set scope, release approval, regime availability, verifier
outcomes or calculated taxes. Review markers are explicit user attestations in
the existing eight-field workflow, not independent evidence verification or a
complete ITR inventory. No tax arithmetic or official utility was operated.

## Atomic within one process

The session monitor covers ownership, expiry/quarantine, replay binding,
expected-key checks and publication of a new immutable bundle. Candidate,
receipt, history and known identity sets are published with one pointer update.
An injected handled failure before that publication leaves all four unchanged.
This is an in-process property, not a durable transaction or process-crash proof.

Policy lookup is outside the edit/close monitor. A controlled concurrent edit can
finish while a policy lookup is waiting; the later identity recheck blocks the
old context. Closing during lookup similarly causes access denial. New result
reads use the ledger's current key and retain the existing signed result gates.
Already-delivered bytes and the cross-registry decision-point limits remain.

## Bounded session storage and privacy

Only current facts are retained, with bounded retry receipts, amount-free change
history and known/quarantined identities. Full historical fact snapshots and raw
input strings are not duplicated. Exact request digests remain private metadata;
they are not authentication, encryption or anonymization. Receipt/history outputs
omit monetary values and principal IDs. Filing/export capabilities remain false.

Configuration bounds sessions, accepted requests and an absolute lifetime of at
most four hours. Configuration ceilings do not establish load capacity. Accepted
retry receipts are retained until session end/capacity; they are not evicted in a
way that could apply the same accepted request again. When capacity is exhausted,
new requests block. Close detaches state; observed expiry blocks access at its
boundary. Idle physical cleanup needs the application's purgeExpired maintenance
call; no exact erasure/zeroization deadline or background scheduler is claimed.

Session state is memory-only and disappears on process exit. There is no disk,
SQL, backup or multi-instance sharing. A new ledger cannot replay prior process
receipts. Show a session-only notice in the real UI; do not market saved/resumable
cases or deploy multiple independent ledgers as if they shared this state.

## Observed verification

| Check | Observed result |
|---|---|
| Runtime/compiler | OpenJDK 17.0.20; --release 17; UTF-8; -proc:none; -Xlint:all; -Werror |
| Policy scenario groups | 56 passed |
| Proof/service scenario groups | 31 passed |
| Parser scenario groups | 21 passed |
| Guidance scenario groups | 28 passed |
| Fact-edit/session scenario groups | 42 passed |
| Total named scenario groups | 178 passed, 0 failed |
| Generated guard/parser/guidance cases | 2,048 / 4,096 / 256 |
| Generated edit transitions | 64 field/state/magnitude combinations |
| Actual competing edit rounds | 32 rounds, two threads each; exactly one distinct writer accepted |
| Actual duplicate retry rounds | 16 rounds, eight threads each; one edit and seven replay receipts |
| Total actual-thread rounds/requests | 48 rounds / 192 submitted requests |
| Additional controlled races | Edit and session close during an external policy lookup |
| Selected compiled defects | 24/24 detected through behavioral failures |
| JSON examples parsed | Six core, six guidance and five new receipt/history responses |
| Cross-JVM consistency | Core/guided/edit projection bytes and canonical signing bytes matched |
| JAR | 87 library class files; tests and ephemeral test credentials excluded |
| Java dependencies/download | None needed |
| MySQL/Docker/SQL integration | Unavailable in this runtime; not run |

The 178 named groups include groups which execute generated/race cases. Counts
must not be combined into a tax-accuracy, throughput or security-certification
claim. These are actual JVM threads in a single process, not MySQL isolation,
distributed races, a load benchmark or exhaustive scheduling exploration.

## New selected defect variants

Each variant compiled; compiler rejection was not counted as detection. Only
temporary source copies were changed; production sources were preserved.

| Injected defect | Failing named groups |
|---|---:|
| ignore_edit_snapshot_identity | 3 |
| ignore_changed_retry_body | 1 |
| accept_wrong_confirmation_value | 1 |
| auto_confirm_ledger_proposal | 4 |
| preserve_reviews_after_amount_change | 7 |
| allow_foreign_owner_edit | 1 |
| publish_edit_before_commit_probe | 1 |
| allow_edit_to_escape_quarantine | 1 |

The prior sixteen selected defects were also checked against the current build.
This remains selected mutation evidence, not exhaustive coverage. Machine reports
and synthetic examples are included under java_result_safety/build.

## Remaining work

1. Actual HTTP/PWA edit flow: trusted actor resolution, explicit user-action/CSRF
   binding, strict decoding, rate limits, session-only messaging and fresh signed
   draft retrieval. Never confirm or mark an inventory from OCR/LLM output alone.
2. Production producer signing/key custody and independently authoritative current
   release/reference/availability state; zero approvals still means no tax release.
3. MySQL saved-case implementation with atomic grants/current snapshot/receipt/
   audit changes, protected storage, durable idempotency and distributed quarantine.
   Exercise crash/retry, recovery, consent changes and multiple serving instances
   against the real database. This in-memory ledger is not a substitute.
4. Recover the original calculator project and obtain independent numerical
   references before tax-interaction patches, settlement or filing work.

The golden workbook/JSON remain byte-for-byte unchanged: 30 draft inputs and zero
independently approved expected outputs. No HF model inference, penetration test,
production deployment or new tax-law validation was performed. Earlier bounded
Python protocol evidence was not rerun or represented as proof of these commands.

See java_result_safety/FACT_EDITS_V1.md for exact transitions, replay semantics,
storage limits, trusted interfaces and action boundaries.

## Primary implementation references

- [Java 17 ConcurrentHashMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html)
- [Java 17 Clock](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Clock.html)
- [OWASP Authorization](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html)

These references inform API use and authority boundaries; local execution reports
establish only the observed component checks described above.
