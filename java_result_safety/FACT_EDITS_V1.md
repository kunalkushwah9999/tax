# Session input edits and confirmation v1

Component 0.1.3 adds a bounded, single-process EphemeralFactLedger for anonymous
preparation sessions. It is not durable saved-case storage or a MySQL substitute.
The HTTP/PWA/authentication integration and production signer remain pending.
Do not describe these edits as saved for later or recovered after a restart.

## Commands and transitions

Every FactEdits.Request contains a requestId, complete expected CalculationKey
and a sealed command. The authenticated actor is a separate trusted server input.
Scope, period, release identity, regime availability, rule approval, verification
and calculated tax are not editable through these commands.

| Command | Required state | Result |
|---|---|---|
| ProposeAmount(field, text, unit) | Supported parser syntax/unit; no default zero | Exact parsed amount becomes ESTIMATED, including an explicit zero |
| ConfirmAmount(field, displayedAmount) | Current amount is ESTIMATED and exactly matches the displayed value | CONFIRMED amount |
| ResolveConflict(field, selectedAmount) | Current field is CONFLICT and selected value is one of its candidates | Explicit selected value becomes CONFIRMED; no averaging or automatic choice |
| ClearAmount(field) | Current field is present or already unknown | UNKNOWN with no numeric fallback |
| ReviewInventory(INCOME) | Supported scope and all four monetary fields confirmed | Income inventory confirmed true |
| ReviewInventory(NEW/OLD_DEDUCTIONS) | Supported scope, confirmed amounts and current income inventory confirmed true | Selected regime review confirmed true |
| UnreviewInventory(INCOME) | No numerical prerequisites | Income and both deduction reviews become unknown |
| UnreviewInventory(NEW/OLD_DEDUCTIONS) | No numerical prerequisites | Selected regime review becomes unknown |

A material amount/state change resets income and both regime deduction reviews.
Every material change generates a distinct snapshot identity and an empty
calculations map. The original facts/candidate remain immutable. An operation
which changes no facts keeps the snapshot and returns NO_CHANGE; it still binds
the accepted request ID for safe retry. No-op requests add no change-history row.

These review markers are user attestations within the existing eight-field
workflow. They are not an independent tax review, evidence verification, complete
filing inventory or a legal regime election. No deduction/tax arithmetic is added.

## Stale tabs and retries

The full expected CalculationKey must match current state, including snapshot,
period, rules, engine/verifier, settings and schema identity. Two distinct requests
using the same old key cannot both write a new state. A stale request returns
STALE_INPUT; the UI must refresh and ask the user to review the current proposal.

Request IDs are scoped to the owned case. A bounded length-prefixed digest binds
the exact expected key and command body, including raw input spelling and selected
unit. The raw string is not retained in the ledger. Changing spelling, field,
unit, operation, value or expected identity with a reused accepted ID returns
IDEMPOTENCY_CONFLICT. The digest is private request metadata, not authentication,
encryption or anonymization; low-entropy input hashes still require protection.

Authorization, expiry and current quarantine are checked before looking up a
receipt. Identical accepted retries return REPLAYED without another transition
or history event. This remains true after later edits, but the receipt contains
only its historical outcome_snapshot_id. It always requests refresh_current_draft;
it does not return historical facts, tax values or label that snapshot current.
Accepted receipts are not evicted within a live session. Once its request cap is
reached, new requests fail while an already accepted exact retry remains usable.

## Single-process publication and policy reads

Within one session monitor, the ledger checks owner/key/replay state and builds
the next immutable state containing the candidate, receipt map, history and known
identities. A single state-pointer assignment publishes that bundle. A handled
failure before publication leaves facts, receipts and history unchanged. This
does not make an acknowledgement durable against process loss.

The ledger implements ControlPlane. New reads observe the new current key, so an
earlier signed calculation becomes stale at a later delivery decision. Calculations
and rule approval are never created by edit receipts. PolicyResolver must supply
fresh, independently authoritative rule/reference/availability state. Missing
policy data fails closed; synthetic APPROVED fixtures are not production evidence.

External policy lookup runs outside the edit/close monitor. On return, ownership,
expiry and current identity are rechecked. An edit during lookup causes a control
block; a closed session causes access denial. Other edits/close calls need not
wait for that remote lookup. The access-decision point and already-delivered-byte
limits of the existing result guard still apply.

Known calculation identities can be quarantined. Current quarantine blocks
editing/replay/current-draft reuse, so a new snapshot cannot silently escape an
incident. Quarantining an older known identity does not poison a distinct current
snapshot. Unknown identities cannot grow the quarantine registry. The quarantine
callback is a trusted internal operation, not an exposed browser command.

## Lifetime, capacity and history

Limits explicitly bound sessions (1..10,000), accepted request records per session
(1..1,000) and absolute session lifetime (>0, at most four hours). These are
configuration ceilings, not demonstrated throughput or supported user counts.
UUID-based profile/case/snapshot IDs are generated on the server; IDs are not
authorization. PolicyControls require explicit per-regime availability states.

Expiry blocks access at the boundary without requiring a cleanup sweep. Close and
observed expiry detach the session from the ledger. purgeExpired can be scheduled
by the real application for idle cleanup and also runs before opening a session.
There is no background cleanup scheduler in this library and no exact physical
erasure deadline or zeroization guarantee. Objects already delivered to trusted
callers may still exist. Clock is a trusted server dependency, never a client time.

Only the current fact set is retained, plus bounded receipts, identity/quarantine
sets and amount-free history. No complete historical fact snapshots or raw input
text are duplicated. History contains operation, field, sequence, timestamp and
dependent-review reset status; it omits values and principal identifiers. It is
private session metadata, not a durable/compliance audit log or automatic sharing.
An owner may read history even when a result needs system review; history grants
no numerical access or edit/remediation authority.

All state disappears when the process stops. No SQL, disk journal, backup,
cross-process lock, cluster routing or recovery path is implemented. A new process
cannot replay old receipts. A real saved-case workflow still requires MySQL
transactions, grants/consent, durable audit/idempotency, storage protection and
crash/recovery/multi-instance integration tests. Do not deploy several independent
memory ledgers and imply that their cases are shared or durable.

## Safe integration

`open`, `current`, ValidationIdentity and initial CaseFacts are server-domain
interfaces. Never bind initial CONFIRMED/scope/release flags from browser JSON.
Use open/current only with trusted state; sign the returned input-only candidate
through the real producer before passing it to IntakeGuideService. Never serialize
Candidate directly. The module does not implement production private-key custody.

Apply commands only after establishing a current authenticated/anonymous-session
actor and explicit user action in the real handler. Add CSRF/session protection,
rate/body limits and strict decoding. An OCR/LLM proposal is not a user confirmation;
never call ConfirmAmount or ReviewInventory automatically from an extraction worker.
No HTTP identity or role system is created by a Java command type.

FactEditResult uses fact-edit.v1; FactEditHistory uses fact-history.v1. Constructors
are restricted, response maps/lists immutable and SafeJson has closed overloads.
Receipts/history omit amounts; filing/export capabilities remain false. Show a
clear session-only notice in the real UI, then refresh the signed current draft
after every accepted receipt. The actual browser/HTTP edit flow is not integrated.

Build/test: python3 build.py; python3 check_mutations.py. The examples are in
build/fact_edit_examples.json. Read the full pack's confirmation_flow_v0_1.md for
observed checks and explicit limits. No test here approves tax golden expectations.
