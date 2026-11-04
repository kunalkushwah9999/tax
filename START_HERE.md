# Start TaxDesk 5.1.0

Synthetic preparation only. Paid production launch is unfinished; independent
statutory approvals remain 0/30. Read TAXDESK_FINAL_HANDOFF.md before deployment.

Install Java 17+ and Python 3.10+. From this extracted directory:

```sh
python3 manage.py doctor
python3 manage.py start
```

Open http://127.0.0.1:8788 and create a sample customer account. On Windows use
`python` if that is the installed command. No ChatGPT subscription or paid AI API
is required to run the downloaded application. Do not enter real documents/PAN.

See STARTUP_RELEASE_V5_1.md for tested changes and remaining work;
startup_app/ACCOUNT_PRIVACY.md for customer deletion; release/README.md for the
installable web layer, Android source and cloud configuration. No signed Android
binary or actual cloud/Play publication is supplied. Original golden expected
outputs stay blank; internal tests are not independent tax approval.


## Release commands outside ChatGPT

See `RELEASE_OPERATIONS.md`. New `release/verify_site.py` checks the deployed
HTTPS public contract without real customer/payment/filing actions;
`release/android_build.py` checks SDK/Gradle prerequisites and runs an unsigned
lint/bundle build on the owner's machine. The tools passed 13 local TLS/Java/CLI
contract groups. No actual public deployment, SDK build, signing or external
approval was executed. Application runtime stays 5.1.0 and approvals stay 0/30.
The downloaded application/tools do not require a ChatGPT subscription to run.


## Listed-equity review arithmetic (standalone, unapproved)

See `STATUTORY_REVIEW_RELEASE.md` and `statutory_review/README.md`. The new
Python review module implements bounded matched-lot equity workings, with 20
engineering groups, 200 generated checks and 10 injected defects detected.
It is not connected to the browser application's result/filing flow. Original
approvals remain 0/30; the separate equity review cases are 0/12 with blank
independent expectations. Main application source/JAR stays 5.1.0 unchanged.
