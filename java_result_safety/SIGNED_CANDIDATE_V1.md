# Signed candidate format v1

Implemented by `CandidateCanonicalizer`, `SignedCandidate`,
`CandidateProofVerifier` and `SignedResultService` in component 0.1.1.
The domain string is `tax-platform/signed-candidate/v1`.

## Scope and trust

This signature authenticates a producer's complete calculation candidate. It is
not encryption, a tax-rule approval signature, user consent, or proof that the
producer/verifier computed the law correctly. Do not expose a signing oracle
which signs client or LLM candidates. The trusted producer must construct facts,
validation identity and verifier outcomes from authoritative immutable inputs.

Production private keys belong outside DB/application read privileges. This
module deliberately supplies no production signer, private key, default public
key, key-fetch URL, remote JWKS parser or automatic key discovery. Test fixtures
generate ephemeral keys in memory; test classes are excluded from the JAR.

## Binary primitives

All integers use Java DataOutputStream big-endian encoding. There are no BOM,
padding, whitespace, separators, trailing data or platform line endings.

| Symbol | Encoding |
|---|---|
| text(s) | Four-byte signed nonnegative UTF-8 byte length, followed by exactly those bytes; field limit 256 bytes |
| boolean(v) | One byte: false=0, true=1 |
| instant(t) | Signed 64-bit epoch seconds, then signed 32-bit nanoseconds in 0..999,999,999 |
| money(m) | text of the exact nonnegative canonical decimal, with exactly two fractional digits |
| moneyFact(f) | text(state); CONFIRMED/ESTIMATED then money(value); UNKNOWN then nothing; CONFLICT then int32 count followed by money(value) for each ordered candidate |
| booleanFact(f) | text(state); CONFIRMED/ESTIMATED then boolean(value); UNKNOWN then nothing; CONFLICT then int32 count followed by boolean(value) for each ordered candidate |

The four state strings are `CONFIRMED`, `UNKNOWN`, `ESTIMATED`, `CONFLICT`.
Conflict counts are 2..8. Money has at most 13 integer digits. Identity text is
validated ASCII; hashes are exactly 64 lowercase hexadecimal characters. Any
future encoding change needs a new domain-format version. Do not reorder a
conflict's candidates or substitute zero for UNKNOWN.

## Ordered signing payload

1. text(domain string), text(producerKeyId), instant(issuedAt), instant(expiresAt).
2. Ten text fields in this exact order: tenantId, profileId, caseId, snapshotId,
   periodKey, ruleBundleHash, engineDigest, verifierDigest, contractVersion,
   settingsHash.
3. text(scope): SUPPORTED, PARTIAL, UNSUPPORTED or UNKNOWN.
4. moneyFact(salary), moneyFact(savingsInterest),
   moneyFact(qualifyingDepositInterest), moneyFact(otherInterest).
5. booleanFact(incomeInventoryComplete).
6. For NEW then OLD: text(regime), booleanFact(deductionInventoryComplete[regime]).
7. For NEW then OLD: text(regime), boolean(calculation present). When present,
   append text(calculation.regime), money(annualTaxBeforeCredits),
   text(verification). Verification is MATCH, MISMATCH or NOT_RUN.

The signing payload is at most 8,192 bytes. The signature is an external
64-byte Ed25519 signature over exactly this payload. The signature itself is
not part of the payload. JCA performs signing/verification; the format does not
define a new cryptographic algorithm. There is no caller-chosen algorithm field.

`build/canonical_vector.json` records the digest and length of a fixed synthetic
test fixture. The build compares the canonical bytes in two separate JVMs. This
is a local consistency check; cross-language interoperability has not been run.

## Verification and delivery order

The service denies cross-tenant requests before reading private control state.
It then obtains a current immutable control snapshot and checks authorization,
deletion, resource binding, restore readiness and adapter integrity state.
It checks the signed proof before any mismatch can create a quarantine write.

Every proof verification resolves the exact key ID through the current registry.
Only ACTIVE Ed25519 CALCULATION_PRODUCER keys are accepted. RULE_APPROVAL keys
cannot sign calculation delivery. Missing, revoked, wrong-purpose, wrong-ID,
wrong-key or invalid proofs fail closed. Registry outage/null reply yields an
operational block, without a cached numerical fallback.

Require issuedAt < expiresAt, now < expiresAt, issuedAt <= now+configured skew,
and lifetime <= configured maximum. Issue and expiry must lie inside key
validity; now must also lie inside key validity. Maximum lifetime is explicitly
injected and restricted to (0,24h]; skew is [0,60s]. Tests use a ten-minute maximum
and five-second skew. There is no production default clock or registry.

After authenticity succeeds, an existing quarantine blocks. An authentic
MISMATCH is recorded through the quarantine adapter before reference, rule,
freshness or ordinary fact gates. This prevents pending references from hiding
mismatch evidence. Invalid/unauthorized payloads must never poison quarantine.
All other current safety gates still apply; a signature grants no filing/export
capability and does not replace independent references.

## Production adapter obligations

Do not use a stale replica/cache as an authoritative key-revocation or access
registry. Control and registry snapshots need a documented authorization decision
point and coherent fail-closed behavior under races. This standalone component
does not create a distributed transaction spanning access, revocation and delivery.
Already delivered bytes cannot be recalled.

Quarantine must be durable, idempotent and visible to every serving instance.
A write outage must fence affected delivery across nodes; blocking only the
request does not repair global state. Restore/revocation/rollback and key rotation
need real database and HTTP integration tests. The provided fixtures prove none
of those production properties. A compromised producer key also requires incident
handling and reviewed revalidation; do not re-sign old data merely to bypass a block.
