# Web, Android and Google Cloud release preparation

**Status: synthetic pilot. Public paid production launch remains blocked.**
Application runtime is the previously tested Java **5.1.0** binary. This layer
adds an origin-specific installable web front end, Android Trusted Web Activity
source, Digital Asset Links generation and VM reverse-proxy/service configuration.
It does not implement the outstanding statutory tax or commercial services.

From the extracted project root, run the offline delivery checks first:

```sh
python3 manage.py doctor
python3 release/test_release.py
python3 release/prepare.py --origin https://YOUR_OWN_DOMAIN --application-id in.YOURCOMPANY.taxdesk --support-email support@YOUR_OWN_DOMAIN --out /ABSOLUTE/NEW_BUNDLE_DIRECTORY
```

Replace the example arguments with your own lowercase DNS name, lowercase Android
application ID and real support mailbox. The output parent must exist; the output
directory must be new. Uppercase placeholders are intentionally rejected. There
is no upload, billing, DNS change, provider enrollment or publication in this command.
No ChatGPT subscription is needed to run the delivered application locally.

Optionally add `--app-signing-sha256 AA:BB:...` with the **full 32-byte Play app-signing
certificate fingerprint**. Repeat for an approved signing-key rotation, maximum
four distinct certificates. Never use an invented fingerprint. The upload-key
fingerprint can differ from Play's app-signing certificate. Without this argument,
`assetlinks.json` is an empty array and domain verification is explicitly false.
The generator verifies the archive checksums and requires static web sources to
match the tested JAR before producing files. Checksums detect changed bytes;
they are not publisher signatures or independent approvals.

The generated bundle contains:

| Path | Purpose |
| --- | --- |
| `web/` | Full existing UI plus manifest, install controls, icons and public offline page |
| `web/.well-known/assetlinks.json` | Configured app identity and public signing-certificate fingerprints |
| `android/` | Gradle Android TWA source, API 36, Internet permission only |
| `cloud/` | Nginx and systemd configuration for the existing Java/MySQL service |
| `release-status.json` | Machine-readable remaining implementation/release blockers |
| `checksums.sha256` | All generated public/config file hashes |

Only the generic offline HTML/CSS and two icons enter Service Worker Cache Storage.
Account pages, API requests, scripts, tax documents, payment responses and writes
are not cached or replayed by this worker. An interrupted action still uses the
existing backend's explicit status/reconciliation workflow. Users must sign out
and protect browser/device access; this is not a claim to erase browser memory,
downloads, screenshots or other applications' caches. No analytics SDK was added.

Read [ANDROID_RELEASE.md](ANDROID_RELEASE.md) and
[CLOUD_DEPLOYMENT.md](CLOUD_DEPLOYMENT.md) before using the generated artifacts.
The illustrative TaxDesk name needs the owner's normal brand/domain review.

## Evidence and limits

`release/test_release.py` executes eight groups, including invalid origins and
configuration injection, certificate parsing, byte binding, preservation of an
existing output, Android/web identity agreement and actual worker execution in
Node. `release/test_browser.py` additionally launches the real Java server and
Chromium through a disposable TLS static proxy, including authenticated mobile
use, Cache Storage contents, CSRF, logout, offline handling and no mutation replay.
The fixture is not the actual generated Nginx process or an Android device.

```sh
python3 release/test_browser.py --chromium /PATH/TO/CHROMIUM --playwright /PATH/TO/playwright/index.mjs --output-dir /PRIVATE/QA_DIRECTORY
```

Android SDK/Gradle, Nginx, Google Cloud and Play Console are not present in the
delivery test environment. No signed AAB/APK, emulator/device result, cloud load
test, actual public TLS result or provider certification is supplied. Existing
backend test evidence remains tied to the unchanged 5.1.0 runtime. See
`DEPLOYMENT_RELEASE_REPORT.md` at the project root for the checks actually run.

Current application 5.1.0 adds eligible customer active-store deletion and public
secret receipt lookup. Focused web checks now total 14, including delayed-navigation
locking and the mobile deletion/status path. Operator/payment-account and external/
backup privacy fulfillment remain pending. See STARTUP_RELEASE_V5_1.md.
