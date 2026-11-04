"""Finite reference model of result promotion and delivery safety.

This is an executable specification, not the tax application or a database
implementation. No tax amount, PAN, network, clock or persistent store exists
in this model. The finite domain is two attempts, two fact edits and one
replacement policy epoch. Expiry is an explicit event, not a simulated clock.
"""
from __future__ import annotations

from dataclasses import dataclass, replace
from enum import Enum


class Status(str, Enum):
    STARTED = 'STARTED'
    VERIFIED = 'VERIFIED'
    EXPIRED = 'EXPIRED'
    STALE = 'STALE'
    BLOCKED = 'BLOCKED'
    CANCELLED = 'CANCELLED'


@dataclass(frozen=True)
class Attempt:
    attempt_id: str
    snapshot: int
    policy_epoch: int
    fence: int
    status: Status = Status.STARTED


@dataclass(frozen=True)
class Result:
    attempt_id: str
    snapshot: int
    policy_epoch: int


@dataclass(frozen=True)
class State:
    snapshot: int = 0
    policy_epoch: int = 0
    rule_valid: bool = True
    authorized: bool = True
    deleted: bool = False
    fence: int = 0
    active: str | None = None
    attempts: tuple[Attempt, ...] = ()
    result: Result | None = None
    quarantined: frozenset[tuple[int, int]] = frozenset()


ACTIONS = (
    'start_A', 'start_B', 'finish_A', 'finish_B',
    'mismatch_A', 'mismatch_B', 'expire_A', 'expire_B',
    'edit_facts', 'publish_policy', 'revoke_rule', 'revoke_access', 'delete_case',
)


def get_attempt(state: State, attempt_id: str) -> Attempt | None:
    return next((a for a in state.attempts if a.attempt_id == attempt_id), None)


def put_attempt(state: State, attempt: Attempt) -> State:
    records = {a.attempt_id: a for a in state.attempts}
    records[attempt.attempt_id] = attempt
    return replace(state, attempts=tuple(records[k] for k in sorted(records)))


def visible_result(state: State, mutant: str | None = None) -> Result | None:
    """Deliver a current verified result only after checking live control state."""
    result = state.result
    if result is None or state.deleted:
        return None
    if not state.authorized and mutant != 'read_ignores_access':
        return None
    if not state.rule_valid and mutant != 'read_ignores_rule':
        return None
    if result.snapshot != state.snapshot and mutant != 'read_ignores_snapshot':
        return None
    if result.policy_epoch != state.policy_epoch and mutant != 'read_ignores_policy':
        return None
    identity = (result.snapshot, result.policy_epoch)
    if identity in state.quarantined and mutant != 'read_ignores_quarantine':
        return None
    return result


def start(state: State, attempt_id: str) -> State:
    identity = (state.snapshot, state.policy_epoch)
    if (state.active is not None or get_attempt(state, attempt_id) is not None
            or not state.authorized or state.deleted or not state.rule_valid
            or identity in state.quarantined):
        return state
    fence = state.fence + 1
    attempt = Attempt(attempt_id, state.snapshot, state.policy_epoch, fence)
    return put_attempt(replace(state, active=attempt_id, fence=fence), attempt)


def finish(state: State, attempt_id: str, matched: bool,
           mutant: str | None = None) -> State:
    attempt = get_attempt(state, attempt_id)
    if attempt is None:
        return state
    owns_lease = (attempt.status == Status.STARTED
                  and state.active == attempt_id and attempt.fence == state.fence)
    if not owns_lease:
        if mutant != 'accept_late_worker' or attempt.status != Status.EXPIRED:
            return state
    if not state.authorized or state.deleted:
        return put_attempt(replace(state, active=None),
                           replace(attempt, status=Status.CANCELLED))
    if not state.rule_valid:
        return put_attempt(replace(state, active=None),
                           replace(attempt, status=Status.BLOCKED))
    wrong_snapshot = attempt.snapshot != state.snapshot
    if ((wrong_snapshot and mutant != 'finish_ignores_snapshot')
            or attempt.policy_epoch != state.policy_epoch):
        return put_attempt(replace(state, active=None),
                           replace(attempt, status=Status.STALE))
    identity = (attempt.snapshot, attempt.policy_epoch)
    if not matched or identity in state.quarantined:
        new_state = replace(state, active=None,
                            quarantined=state.quarantined | {identity})
        return put_attempt(new_state, replace(attempt, status=Status.BLOCKED))
    result = Result(attempt_id, attempt.snapshot, attempt.policy_epoch)
    return put_attempt(replace(state, active=None, result=result),
                       replace(attempt, status=Status.VERIFIED))


def expire(state: State, attempt_id: str, mutant: str | None = None) -> State:
    attempt = get_attempt(state, attempt_id)
    if attempt is None:
        return state
    if mutant == 'reaper_clears_other_attempt':
        if attempt.status not in (Status.STARTED, Status.EXPIRED):
            return state
    elif attempt.status != Status.STARTED or state.active != attempt_id:
        return state
    return put_attempt(replace(state, active=None),
                       replace(attempt, status=Status.EXPIRED))


def step(state: State, action: str, mutant: str | None = None) -> State:
    if action.startswith('start_'):
        return start(state, action[-1])
    if action.startswith('finish_') or action.startswith('mismatch_'):
        return finish(state, action[-1], action.startswith('finish_'), mutant)
    if action.startswith('expire_'):
        return expire(state, action[-1], mutant)
    if action == 'edit_facts' and not state.deleted and state.snapshot < 2:
        # Reverting a monetary value is another immutable snapshot, not old ID 0.
        return replace(state, snapshot=state.snapshot + 1)
    if action == 'publish_policy' and not state.deleted and state.policy_epoch == 0:
        # A material validation-policy change, not every unrelated software deploy.
        return replace(state, policy_epoch=1, rule_valid=True)
    if action == 'revoke_rule' and state.rule_valid:
        return replace(state, rule_valid=False)
    if action == 'revoke_access' and state.authorized:
        return replace(state, authorized=False)
    if action == 'delete_case' and not state.deleted:
        cancelled = tuple(replace(a, status=Status.CANCELLED)
                          if a.status == Status.STARTED else a for a in state.attempts)
        return replace(state, deleted=True, authorized=False,
                       active=None, attempts=cancelled)
    return state


def assert_state_safety(state: State, mutant: str | None = None) -> None:
    started = [a for a in state.attempts if a.status == Status.STARTED]
    assert len(started) <= 1, 'multiple live attempts'
    assert ((state.active is None and not started)
            or (len(started) == 1 and started[0].attempt_id == state.active)), \
        'active attempt pointer lost or assigned incorrectly'
    if state.result is not None:
        record = get_attempt(state, state.result.attempt_id)
        assert record is not None and record.status == Status.VERIFIED, \
            'result is not backed by a verified attempt'
    delivered = visible_result(state, mutant)
    if delivered is not None:
        assert state.authorized and not state.deleted, 'result delivered after access revoked'
        assert state.rule_valid, 'result delivered under revoked rule'
        assert delivered.snapshot == state.snapshot, 'stale facts delivered as current'
        assert delivered.policy_epoch == state.policy_epoch, 'old validation policy delivered as current'
        assert (delivered.snapshot, delivered.policy_epoch) not in state.quarantined, \
            'quarantined calculation delivered from cache'


def assert_transition_safety(before: State, after: State, action: str) -> None:
    assert after.snapshot >= before.snapshot, 'snapshot identity went backwards'
    assert after.fence >= before.fence, 'fencing token went backwards'
    assert before.quarantined <= after.quarantined, 'quarantine silently cleared'
    assert not before.deleted or after.deleted, 'deleted case silently resurrected'
    if after.result != before.result:
        result = after.result
        assert result is not None, 'verified result disappeared without explicit archival policy'
        worker = get_attempt(before, result.attempt_id)
        assert worker is not None and worker.status == Status.STARTED, 'terminal worker promoted a result'
        assert before.active == worker.attempt_id, 'late worker overwrote another active attempt'
        assert worker.fence == before.fence, 'worker promoted without current fencing token'
        assert result.snapshot == after.snapshot, 'stale facts promoted'
        assert result.policy_epoch == after.policy_epoch, 'stale validation policy promoted'
        assert after.authorized and not after.deleted and after.rule_valid, 'invalid context promoted'
        assert (result.snapshot, result.policy_epoch) not in after.quarantined, 'quarantined identity promoted'
    if action.startswith('expire_') and before.active not in (None, action[-1]):
        assert after.active == before.active, 'duplicate reaper cleared a newer active attempt'


def replay(actions: list[str] | tuple[str, ...], mutant: str | None = None) -> State:
    state = State()
    for action in actions:
        state = step(state, action, mutant)
    return state
