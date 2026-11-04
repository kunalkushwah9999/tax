# Executable protocol model

This models result promotion, current-result delivery, expiry, retries and
calculation quarantine. It contains no tax calculator or production service.
Python 3.10+ and the standard library are sufficient. Do not run with `-O`.

From this directory:

```bash
python3 -m unittest -v test_protocol.py
python3 check_protocol.py --output protocol_check_results.json
```

The checker explores the complete reachable state space within a finite domain:
two unique attempt IDs, two immutable fact edits, and one material replacement
validation-policy epoch. Rule revocation, grant revocation, deletion, verification
success/mismatch and duplicate expiry/finish events can interleave. It rejects an
exhausted search budget as incomplete. It also injects eight deliberate defects
and records a counterexample for each.

An explicit audit can create a second attempt for an existing calculation
identity. Normal repeated client requests should still reuse an authorized,
current, nonquarantined logical result. The policy epoch represents a reviewed
change requiring invalidation, not every application deployment.

The model treats each step atomically. Real SQL locking, isolation, replicas,
message deliveries, clocks, distributed control-plane freshness and actual data
erasure require integration and operational tests. It checks safety within its
abstraction, not liveness, unbounded concurrency, tax law or application security.

A positive model result is useful design evidence. It cannot certify a Java
implementation that has not been supplied, connected or exercised.
