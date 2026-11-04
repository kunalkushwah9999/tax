# Native ITR-1 structural intake — 1.3.0

Use synthetic data only. This optional owner-only adapter checks a pinned official
AY 2026–27 ITR-1 **return JSON**, then offers narrow income proposals. It does not
approve tax calculations, legal eligibility, business validations, source identity,
CA requirements or filing. Native GST is not released. The default app remains
usable without Python third-party packages and reports this feature unconfigured.

## Enable explicitly

The tested environment is Linux, Java 17 and Python 3.12 with the exact versions
in native_import/requirements.txt. From the extracted package root:

```sh
python3 -m venv .venv-native
.venv-native/bin/python -m pip install -r startup_app/native_import/requirements.txt
python3 startup_app/native_import/fetch_sources.py --output runtime-native
python3 startup_app/run.py --native-python .venv-native/bin/python --native-schema-dir runtime-native
```

Use the venv interpreter path without resolving away its symlink: that preserves
its package environment. Windows/native-worker behavior was not exercised; the
default Java app still has its earlier Windows launcher instructions. No venv,
Python wheel, official schema/PDF cache, runtime key/database or private file is
bundled. No paid API or government account is used by this local adapter.

The explicit fetch helper verifies exact byte lengths and SHA-256 for both public
reference files and refuses mismatched existing files. A changed source requires
reviewed new pins and repeat tests. Validation itself never downloads schemas.
Startup self-check rejects missing/mismatched dependencies or schema bytes.
The official PDF is a pinned human reference; its full business rules have not
been implemented or approved. See native_import/source_pins.json for URLs/pins.

Open the app, sign in as a CUSTOMER, create/open a draft and choose **ITR file
check**. Load the synthetic structural example or select a synthetic JSON file
up to 192 KiB. The example has intentionally fictional identity/bank details and
is not a tax-correct/fileable return or an independent numerical expectation.

## Review and propose

Preview requires explicit synthetic-data acknowledgement and current draft
revision. Schema validation and exact form/year/version are separate statuses.
The published artifact is version V1.1 while its internal FormVer/SchemaVer
fields are Ver1.0; these are not interchangeable. Even the official schema's
permissive empty-root/sub-string-pattern cases do not bypass the exact adapter
envelope check. Prefill JSON, different forms/years and changed schema versions
require separate release packs.

The proposal pack accepts an explicit gross-salary field and single ordinary
SAV/IFD/TAX interest observations. It does not sum multiple same-category rows,
turn absent categories into zero, transfer net salary as gross, or infer that
interest qualifies for a deduction. It checks source interest-detail totals and
flags unsupported schedules/categories. The supported proposal pack is narrower
than the official form's full legal scope. Do not remove real income to fit it;
use an expanded workflow when genuine uncovered data is present.

For each selected category, check complete annual coverage/classification and
acknowledge replacement of the exact displayed amount. Also confirm outside this
preview that the source belongs to the same synthetic taxpayer: identities are
not compared or stored here. The whole selected action commits as ESTIMATED in
one transaction and resets affected material identity/review checkpoints. Confirm
each displayed amount separately in the ordinary Income tab. Missing/ambiguous
fields require their own checked input, not a generated zero.

## Data and authority boundaries

Raw native JSON, PAN/Aadhaar, names, bank/contact details, filenames, supplied
TDS/credit/deduction/tax-computation/refund fields are not durably retained or
returned in previews. Native tax figures are ignored; no supplied calculation
becomes an independent expected result. The worker returns only permitted income,
fixed status codes and schema-defined paths. The Java projection independently
allowlists fields/paths and forces filing/business/tax authority false.

Previews are sanitized memory only: owner/case/revision bound, ten-minute TTL,
one per case, four per account, 64 total. Discard/replacement/accepted use/restart
remove access to them. No raw-file digest or PAN lookup hash is stored. Accepted
proposal receipts fingerprint only the sanitized command; they replay without a
live preview and preserve later edits. Only minimal schema/path/unverified lineage
is saved with accepted income. Manual/source replacements clear that field's
native lineage. Earlier PREPARER grants do not expose this new private lineage.
Operational audit remains amount/source/identity-free.

Input necessarily passes through transient browser/JVM/worker memory; no claim
of physical RAM/swap/backup erasure or independent privacy certification is made.
The raw file is not written to a temporary input file or sent in command arguments.

## Process and format limits

The parent caps two concurrent workers, bounds stdout, drains input/output on
separate streams and enforces a five-second process deadline. A worker that does
not consume stdin cannot block indefinitely. Linux workers set CPU/address-space/
core/file-size limits and deny socket audit events. The validator registry denies
remote references; the pinned schema is checked to contain local references only.
This is bounded process isolation, not an independently certified OS sandbox.

Native payloads are at most 196,608 UTF-8 bytes; the dedicated HTTP wrapper is
at most 400,000 bytes. Other API routes remain at 64 KiB. Native depth is 24,
node count 12,000, arrays 500, strings 4,096 characters and numeric token sizes
are bounded. Duplicate keys, invalid UTF-8/surrogates and non-finite values reject.
Raw source failures cannot become zero income, approved tax or successful filing.

## Repeat checks

```sh
.venv-native/bin/python -B startup_app/native_import/test_worker.py --schema-dir runtime-native
.venv-native/bin/python -B startup_app/native_import/check_worker_mutations.py --schema-dir runtime-native
python3 startup_app/test_native_import.py --native-python .venv-native/bin/python --native-schema-dir runtime-native
python3 startup_app/check_native_mutations.py
```

For the disposable MySQL harness, add the same --native-python and
--native-schema-dir flags to the documented test_mysql.py command. It runs the
existing default API suite, enables the worker for a separate native API suite,
and exercises crash/isolated restore/selected SQL defects with synthetic data.
The default verify_all.py does not silently install packages or fetch references.
Authenticated browser/mobile/keyboard/accessibility QA remains pending.

## GST boundary

The official GST downloads/developer schema endpoints returned HTTP 403 here.
No vendor sample was treated as an official native schema. The existing normalized
GST observation workflow remains; it does not approve ITC or determine a nil return.
Native GSTR-2B/IMS/GSTR-1/3B schemas, business rules and provider filing need their
own authoritative version pins and independently validated release packs.
