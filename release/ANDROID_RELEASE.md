# Android release: source delivered; binary and publication pending

This project opens the hosted HTTPS application through Android Browser Helper's
Trusted Web Activity. It uses the browser's session and web security boundaries;
there is no custom JavaScript bridge, copied customer database or embedded API
credential. Domain validation requires matching Digital Asset Links on the actual
domain. Failed verification can expose browser UI; it must be tested on devices.

The generated source pins Android Gradle Plugin **8.13.2**, Android Browser Helper
**2.7.4**, compile/target SDK **36**, minimum SDK **23**, and Java **17** compatibility.
Use Gradle **8.13** and install Android SDK platform 36. No downloaded Gradle
wrapper or SDK binary is bundled. Import generated `android/` into Android Studio,
or use an installed Gradle 8.13 from that directory:

```sh
gradle --version
gradle --no-daemon :app:lintRelease :app:bundleRelease
```

This command creates an **unsigned** bundle unless the owner configures signing.
The expected output is `app/build/outputs/bundle/release/app-release.aab`. It has
not been built in this environment. Create the owner's upload key and a signed
bundle using Android Studio's Generate Signed Bundle flow; protect/recover the
key separately. Do not commit keys, passwords, local SDK paths or signing config.
Increase `versionCode` on each subsequent Play upload. Test dependency resolution,
merged manifest, launcher/deep links, back navigation, offline behaviour, secure
cookies, MFA, accessibility and app/site updates on actual supported Android devices.

Use Play App Signing's **app-signing** certificate in the web `assetlinks.json`.
Publish that file at `https://YOUR_DOMAIN/.well-known/assetlinks.json` as JSON,
without authentication or redirects. Re-generate after certificate/domain changes.
Test the Play-installed artifact rather than assuming a locally signed APK proves
that Play's signing identity works.

## Gates before Play production submission

Eligible customers now have active-store account erasure through the web/app
navigation and a public `/account-deletion` page with secret receipt lookup. The
generated page includes the configured support mailbox for reviewed requests.
Operator/payment-account removal, processor/backups handling, control retention
and complete fulfillment are still unfinished. See startup_app/ACCOUNT_PRIVACY.md.
Do not declare full account-deletion/privacy compliance from this limited flow.

The owner must supply privacy policy, support contact, accurate Data Safety and
content/financial declarations, consent and retention terms, and review the data
behaviour of the host browser/library. The current TEST payment integration does
not establish compliance for selling digital subscriptions through a Play app.
Review current Play billing rules and the intended commercial offering before
implementing or exposing paid plans. No live payment credentials are included.

As checked on **2026-11-03**, Play's API requirement for ordinary new apps/updates
is Android 16 / API 36. The source targets 36; targeting alone is not Play approval.
For affected personal developer accounts created after 2023-11-13, Google requires
at least 12 opted-in closed testers continuously for 14 days before applying for
production access. Account-specific checks and review still apply.

Primary sources checked on 2026-11-03:

- https://developer.android.com/develop/ui/views/layout/webapps/guide-trusted-web-activities-version2
- https://github.com/GoogleChrome/android-browser-helper
- https://developer.android.com/build/releases/agp-8-13-0-release-notes
- https://support.google.com/googleplay/android-developer/answer/11926878
- https://support.google.com/googleplay/android-developer/answer/14151465
- https://support.google.com/googleplay/android-developer/answer/13327111

These requirements can change. Re-check the official pages and Console messages
when actually building and submitting. Tax approvals remain 0/30; Play review
would not establish statutory tax accuracy or filing authorization.
