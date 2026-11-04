"""Scenario tests for the reference protocol; uses Python's standard library."""
import unittest

from calculation_protocol import State, Status, get_attempt, replay, step, visible_result


class ProtocolScenarios(unittest.TestCase):
    def test_no_result_before_verification(self):
        self.assertIsNone(visible_result(replay(['start_A'])))

    def test_verified_current_result_is_deliverable(self):
        self.assertIsNotNone(visible_result(replay(['start_A', 'finish_A'])))

    def test_only_one_active_attempt(self):
        state = replay(['start_A', 'start_B'])
        self.assertEqual(state.active, 'A')
        self.assertIsNone(get_attempt(state, 'B'))

    def test_duplicate_finish_is_idempotent(self):
        state = replay(['start_A', 'finish_A'])
        self.assertEqual(step(state, 'finish_A'), state)

    def test_changed_facts_cannot_be_promoted(self):
        state = replay(['start_A', 'edit_facts', 'finish_A'])
        self.assertEqual(get_attempt(state, 'A').status, Status.STALE)
        self.assertIsNone(state.result)

    def test_edit_after_commit_hides_old_result(self):
        self.assertIsNone(visible_result(replay(['start_A', 'finish_A', 'edit_facts'])))

    def test_edit_and_revert_is_another_snapshot(self):
        state = replay(['start_A', 'edit_facts', 'edit_facts', 'finish_A'])
        self.assertEqual(state.snapshot, 2)
        self.assertEqual(get_attempt(state, 'A').status, Status.STALE)

    def test_revoked_rule_blocks_commit(self):
        state = replay(['start_A', 'revoke_rule', 'finish_A'])
        self.assertEqual(get_attempt(state, 'A').status, Status.BLOCKED)
        self.assertIsNone(state.result)

    def test_rule_revoked_after_commit_blocks_delivery(self):
        self.assertIsNone(visible_result(replay(['start_A', 'finish_A', 'revoke_rule'])))

    def test_material_policy_update_blocks_old_attempt(self):
        state = replay(['start_A', 'publish_policy', 'finish_A'])
        self.assertEqual(get_attempt(state, 'A').status, Status.STALE)

    def test_material_policy_update_hides_old_cache(self):
        self.assertIsNone(visible_result(replay(['start_A', 'finish_A', 'publish_policy'])))

    def test_access_revoked_before_finish_cancels_attempt(self):
        state = replay(['start_A', 'revoke_access', 'finish_A'])
        self.assertEqual(get_attempt(state, 'A').status, Status.CANCELLED)

    def test_access_revoked_after_commit_blocks_delivery(self):
        self.assertIsNone(visible_result(replay(['start_A', 'finish_A', 'revoke_access'])))

    def test_late_worker_cannot_overwrite_retry(self):
        state = replay(['start_A', 'expire_A', 'start_B', 'finish_A'])
        self.assertEqual(state.active, 'B')
        self.assertIsNone(state.result)
        self.assertEqual(get_attempt(state, 'A').status, Status.EXPIRED)

    def test_duplicate_reaper_cannot_clear_new_attempt(self):
        state = replay(['start_A', 'expire_A', 'start_B', 'expire_A'])
        self.assertEqual(state.active, 'B')

    def test_retry_can_succeed_after_expiry(self):
        state = replay(['start_A', 'expire_A', 'start_B', 'finish_B'])
        self.assertEqual(visible_result(state).attempt_id, 'B')

    def test_mismatch_quarantines_previously_cached_result(self):
        state = replay(['start_A', 'finish_A', 'start_B', 'mismatch_B'])
        self.assertIsNotNone(state.result)  # historical evidence, not a valid current answer
        self.assertIn((0, 0), state.quarantined)
        self.assertIsNone(visible_result(state))

    def test_mismatch_cannot_be_fixed_by_blind_retry(self):
        state = replay(['start_A', 'mismatch_A', 'start_B'])
        self.assertIsNone(get_attempt(state, 'B'))

    def test_new_reviewed_policy_has_separate_identity(self):
        state = replay(['start_A', 'mismatch_A', 'publish_policy', 'start_B', 'finish_B'])
        self.assertEqual(visible_result(state).policy_epoch, 1)
        self.assertIn((0, 0), state.quarantined)

    def test_failed_verification_never_creates_result(self):
        state = replay(['start_A', 'mismatch_A'])
        self.assertEqual(get_attempt(state, 'A').status, Status.BLOCKED)
        self.assertIsNone(state.result)

    def test_deleted_case_cancels_active_work(self):
        state = replay(['start_A', 'delete_case', 'finish_A'])
        self.assertTrue(state.deleted)
        self.assertIsNone(state.active)
        self.assertEqual(get_attempt(state, 'A').status, Status.CANCELLED)

    def test_deleted_case_cannot_be_reopened_by_new_policy(self):
        state = replay(['start_A', 'finish_A', 'delete_case', 'publish_policy', 'start_B'])
        self.assertTrue(state.deleted)
        self.assertIsNone(visible_result(state))
        self.assertIsNone(get_attempt(state, 'B'))


if __name__ == '__main__':
    unittest.main()
