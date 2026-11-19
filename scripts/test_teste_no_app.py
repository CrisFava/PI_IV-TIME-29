"""Regressões locais: nenhuma chamada ao Firebase ou Mongo real."""
import json
import tempfile
import unittest
from datetime import date
from pathlib import Path
from unittest.mock import MagicMock, patch

import teste_no_app as app
from simulation.api_response import ApiResponse
from simulation.simulation_failure import SimulationFailure
from simulation.simulation_world import SimulationWorld


class PreparationTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.state = Path(self.directory.name) / 'state.json'
        self.enterContext(patch.object(app, 'STATE_FILE', self.state))
        self.mongo = self.enterContext(patch.object(app, 'MongoClient'))

    def test_failure_retains_cleanup_emails_before_first_account(self):
        api = MagicMock()
        api.call.return_value = ApiResponse(503, {})
        with self.assertRaises(SimulationFailure):
            app.prepare(api, 'key', 'local', 'test')
        state = json.loads(self.state.read_text())
        self.assertEqual(len(state['emails']), 3)
        self.assertEqual(api.call.call_args.kwargs['body']['email'], state['emails'][0])
        previous = self.state.read_bytes()
        with self.assertRaises(SimulationFailure):
            app.prepare(api, 'key', 'local', 'test')
        self.assertEqual(api.call.call_count, 1)
        self.assertEqual(self.state.read_bytes(), previous)

    def test_registration_and_approval_failures_retain_cleanup(self):
        for stage in ('registration', 'approval', 'vehicle'):
            with self.subTest(stage=stage):
                self.state.unlink(missing_ok=True)
                api = MagicMock()
                api.call.return_value = ApiResponse(200, {'localId': 'uid', 'idToken': 'token'})
                api.post.return_value = ApiResponse(500, {}) if stage == 'registration' else ApiResponse(200, {'data': {'id': 'id'}})
                with patch.object(SimulationWorld, '_approve_documents') as approve:
                    if stage == 'approval':
                        approve.side_effect = SimulationFailure('approval failed')
                    if stage == 'vehicle':
                        api.post.side_effect = [ApiResponse(200, {'data': {'id': 'id'}}), ApiResponse(500, {})]
                    with self.assertRaises(SimulationFailure):
                        app.prepare(api, 'key', 'local', 'test')
                self.assertIn(api.call.call_args.kwargs['body']['email'], json.loads(self.state.read_text())['emails'])

    def test_success_emails_match_accounts(self):
        api = MagicMock()
        api.call.return_value = ApiResponse(200, {'localId': 'uid', 'idToken': 'token'})
        api.post.return_value = ApiResponse(200, {'data': {'id': 'id'}})
        with patch.object(SimulationWorld, '_approve_documents'), patch('builtins.print'):
            app.prepare(api, 'key', 'local', 'test')
        state = json.loads(self.state.read_text())
        self.assertEqual(state['emails'], [state[role]['email'] for role in ('app', 'motorista', 'passageiro')])

    def test_clean_keeps_state_on_failure_and_removes_on_success(self):
        self.state.write_text(json.dumps({'runId': 'run', 'emails': ['unregistered@test']}))
        with patch.object(app, 'SimulationWorld') as world, patch('builtins.print'):
            world.return_value.delete_accounts_of_run.return_value = ([], ['unregistered@test'])
            with self.assertRaises(SimulationFailure):
                app.clean(MagicMock(), 'key', 'local', 'test')
            self.assertTrue(self.state.exists())
            world.return_value.delete_accounts_of_run.assert_called_with('run', ['unregistered@test'])
            world.return_value.delete_accounts_of_run.return_value = ([], [])
            app.clean(MagicMock(), 'key', 'local', 'test')
            self.assertFalse(self.state.exists())


class CleanupTest(unittest.TestCase):
    def test_only_explicit_absence_or_successful_deletion_resolves_account(self):
        cases = [
            (ApiResponse(400, {'error': {'message': 'EMAIL_NOT_FOUND'}}), None, []),
            (ApiResponse(400, {'error': {'message': 'INVALID_LOGIN_CREDENTIALS'}}), None, ['orphan@test']),
            (ApiResponse(503, {}), None, ['orphan@test']),
            (ApiResponse(200, {}), None, ['orphan@test']),
            (ApiResponse(500, {'idToken': 'token'}), None, ['orphan@test']),
            (ApiResponse(200, {'idToken': 'token'}), ApiResponse(200, {}), []),
            (ApiResponse(200, {'idToken': 'token'}), ApiResponse(500, {}), ['orphan@test']),
            (SimulationFailure('network'), None, ['orphan@test']),
        ]
        for login, deletion, expected in cases:
            with self.subTest(login=login, deletion=deletion):
                api, users = MagicMock(), MagicMock()
                users.find.return_value = []
                api.call.side_effect = [login, deletion]
                world = SimulationWorld(api, 'key', users, 'run', date.today())
                deleted, leftovers = world.delete_accounts_of_run('run', ['orphan@test'])
                self.assertEqual(leftovers, expected)
                self.assertEqual(deleted, ['orphan@test'] if deletion and deletion.ok else [])


if __name__ == '__main__':
    unittest.main()
