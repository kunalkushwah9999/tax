"""Explore the complete finite domain and find deliberately injected failures."""
from __future__ import annotations

from collections import deque
from dataclasses import asdict
from pathlib import Path
import argparse
import json
import platform

from calculation_protocol import (
    ACTIONS, State, assert_state_safety, assert_transition_safety, step,
)

MUTANTS = (
    'read_ignores_snapshot', 'read_ignores_access', 'read_ignores_rule',
    'read_ignores_policy', 'read_ignores_quarantine', 'finish_ignores_snapshot',
    'accept_late_worker', 'reaper_clears_other_attempt',
)


def explore(mutant: str | None = None, max_states: int = 250_000) -> dict:
    initial = State()
    pending = deque([(initial, ())])
    visited = {initial}
    transitions = 0
    maximum_depth = 0
    while pending:
        before, trace = pending.popleft()
        maximum_depth = max(maximum_depth, len(trace))
        for action in ACTIONS:
            after = step(before, action, mutant)
            if after == before:
                continue
            transitions += 1
            try:
                assert_transition_safety(before, after, action)
                assert_state_safety(after, mutant)
            except AssertionError as error:
                return {
                    'model': mutant or 'correct_reference', 'status': 'COUNTEREXAMPLE',
                    'states_discovered': len(visited), 'transitions_checked': transitions,
                    'trace': list(trace + (action,)), 'violation': str(error),
                }
            if after not in visited:
                if len(visited) >= max_states:
                    raise RuntimeError('State budget exhausted: result is INCOMPLETE, never PASS.')
                visited.add(after)
                pending.append((after, trace + (action,)))
    return {
        'model': mutant or 'correct_reference', 'status': 'FINITE_DOMAIN_CHECK_PASSED',
        'states_checked': len(visited), 'transitions_checked': transitions,
        'maximum_shortest_trace_length': maximum_depth,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if not __debug__:
        raise RuntimeError('Run without -O: assertion checks must remain enabled.')
    result = explore()
    if result['status'] != 'FINITE_DOMAIN_CHECK_PASSED':
        raise RuntimeError(json.dumps(result))
    mutations = [explore(name) for name in MUTANTS]
    if any(r['status'] != 'COUNTEREXAMPLE' for r in mutations):
        raise RuntimeError('An injected defect escaped the checker.')
    payload = {
        'scope': 'Finite abstract protocol only; no tax arithmetic or production DB proof.',
        'python_version': platform.python_version(),
        'bounds': {'attempt_ids': ['A', 'B'], 'maximum_fact_edits': 2,
                   'replacement_policy_epochs': 1, 'actions': list(ACTIONS)},
        'correct_model': result,
        'injected_defects_detected': len(mutations),
        'injected_defects': mutations,
        'limitations': [
            'Steps are atomic. Real DB isolation, statement ordering and network boundaries are not modelled.',
            'No proof of unbounded concurrency, timing, liveness or production availability.',
            'Verifier outcomes are inputs. No tax correctness, legal eligibility or CA approval is tested.',
            'Revocation applies at a read decision. Already downloaded bytes cannot be recalled.',
            'A second run can represent an explicit audit, not a reason to remove logical request deduplication.',
            'Deletion is abstract access denial; legal retention and actual storage erasure are outside this model.',
        ],
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, indent=2) + '\n')
    print(json.dumps({'correct_model': result, 'injected_defects_detected': len(mutations)}))


if __name__ == '__main__':
    main()
