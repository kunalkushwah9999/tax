# Private tax arithmetic workbench 0.1.0

Read ../START_HERE.md first. This module is new implementation, separate from the
unchanged result-safety core and the missing original calculator. It has no third
party dependencies. Java 17+ runs the bundled JAR; rebuilding requires a full JDK.

`python3 build.py` compiles main/test sources with --release 17, UTF-8, -proc:none,
-Xlint:all and -Werror, executes the engine suite and packages only main classes
plus the draft manifest. `python3 check_mutations.py` compiles temporary defective
copies and requires behavioral failure; it never alters original source.
`python3 test_references.py` tests reference collection/comparison with synthetic
assertions in memory, not real approved expectations.

The BigDecimal engine uses ordered brackets. The verifier uses integer paise,
excess-income equations and a separate calendar/rounding implementation. They
share input/output contracts and the same underlying legal assumptions; they are
not independent legal sources, separately reviewed law or separate fault domains.
Every one of 20 stages and the age band must agree. Any disagreement suppresses
review workings and blocks browser delivery. Above-cap computations are withheld.

Supported period: ITA1961-FY2025-26-AY2026-27. ROR individual, adult throughout FY,
up to sixteen declared employer totals, separated eligible savings/deposits/other
ordinary interest, and already eligible distinct 80C/80D/80CCD(1B) claims. Scope
answers and review completeness are mandatory. Neither a missing amount nor an
estimated proposal is zero or confirmed. 80D eligibility is an input dependency;
the 100,000 aggregate upper bound alone cannot establish a claim.

Annual tax before credits = slab tax minus rebate/marginal relief plus cess.
ZERO_CREDIT_ROUNDED_WORKING is a separate 288B illustration, never a settlement.
The private comparison uses exact annual tax before credits. The optional 80C
sensitivity uses the explicitly labelled zero-credit rounded working and searches
only declared remaining eligible capacity, in paise. Weak and strict boundaries
are separate, and the immediately previous paise is checked. The supported old
ordinary-tax function is nonincreasing as an eligible deduction increases;
rounding plateaus and the old rebate threshold preserve that direction.

The manifest is a draft specification/provenance artifact. Formulas are compiled
in two implementations; it is not an approved dynamic rule loader or publication
store. Artifact hashes bind the actual engine JAR and manifest in the HTTP
candidate identity. A signature from the demo process does not approve tax law.

`review_case.py` explicitly prints private synthetic engineering outputs.
`review_workbench.py template` leaves every expected stage/status empty.
`review_workbench.py check` recomputes candidates and checks input hashes, exact
stage sets, canonical reference decimals and asserted review metadata. It always
returns release_approved=false. Real reviewer identity, evidence authenticity,
qualified law review and production publication remain outside this checker.

Original golden workbook and JSON are never edited by these tools. The app's
public gate stays UNAPPROVED/PENDING/UNKNOWN, regardless of private agreement or
reference metadata. No production release or numerical tax validation is claimed.
