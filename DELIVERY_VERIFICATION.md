# Delivery verification — 5.0.0

3 November 2026. Both ZIP manifests were verified. The current code/resource/JAR
payload was extracted into a separate directory and started with the packaged
launcher, without compilation or rebuilding. Bundled engineering preflight passed.

Observed against that extracted payload:

- Core API: 49/49 groups passed.
- Account-security API: 25 run, 24 passed, one intentional two-node-SQL-only skip.
- Private workbench API: 24/24 passed.
- Actual Java/Python native-worker API: 20/20 passed.
- TEST commerce/consent/expert/feedback/bundle API: 36/36 passed.
- A private capital ledger persisted across application restart unchanged.
- MFA remained enabled after restart, with consumed recovery codes still removed;
  two distinct recovery-code sign-ins changed the remaining count from 10 to 9
  and then 8 across the restart.
- Encrypted local backup creation and verification passed.
- The startup JAR remained byte-identical; no classes directory or rebuild was used.

Startup JAR SHA-256:
`1548c778417a554434b1ea467c6cea162787f398e75ca2d7742838391e841feb`.

After this smoke, current-release documentation clarified that 12 RFC checks use
six published time points at two digit widths; this verification document and
checksum manifests were also added/updated. Entry-by-entry comparison confirms
that tested code, resources, fixtures, executable and test-report bytes are unchanged.
Both final ZIPs carry the same file payload under their existing top-level names.

These are delivery/engineering checks, not statutory tax or production approval.
Independent tax approvals remain 0/30. Read STARTUP_RELEASE_V5.md and OWNER_HANDOFF.md
for implemented scope and remaining software/external work. Synthetic data only.
