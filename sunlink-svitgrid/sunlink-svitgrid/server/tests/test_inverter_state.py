
"""
Tests for InverterState.
"""

from __future__ import annotations

import sys
import unittest
from datetime import datetime, timezone, timedelta

sys.path.insert(0, __import__("os").path.dirname(__import__("os").path.abspath(__file__)) + "/..")

from services.inverter_state import (  # noqa: E402
    InverterState,
    ConnectionStatus,
)


class TestInverterStateDefaults(unittest.TestCase):
    def test_defaults(self):
        state = InverterState(inverter_id="inv-1")
        self.assertEqual(state.inverter_id, "inv-1")
        self.assertEqual(state.connection_status, ConnectionStatus.UNKNOWN)
        self.assertIsNone(state.last_successful_poll)
        self.assertIsNone(state.last_failed_poll)
        self.assertIsNone(state.last_error)
        self.assertEqual(state.consecutive_failures, 0)
        self.assertFalse(state.is_data_fresh())

    def test_connection_status_constants(self):
        self.assertEqual(ConnectionStatus.UNKNOWN, "unknown")
        self.assertEqual(ConnectionStatus.ONLINE, "online")
        self.assertEqual(ConnectionStatus.OFFLINE, "offline")
        self.assertEqual(ConnectionStatus.DEGRADED, "degraded")


class TestInverterStateTransitions(unittest.TestCase):
    def setUp(self):
        self.state = InverterState(
            inverter_id="inv-1",
            data_freshness_seconds=300,
        )

    def test_mark_successful_poll(self):
        self.state.mark_successful_poll(duration_ms=100.0)
        self.assertEqual(self.state.connection_status, ConnectionStatus.ONLINE)
        self.assertIsNotNone(self.state.last_successful_poll)
        self.assertIsNone(self.state.last_error)
        self.assertEqual(self.state.consecutive_failures, 0)
        self.assertTrue(self.state.is_data_fresh())

    def test_mark_failed_poll(self):
        self.state.mark_failed_poll("timeout")
        self.assertEqual(self.state.connection_status, ConnectionStatus.DEGRADED)
        self.assertIsNotNone(self.state.last_failed_poll)
        self.assertEqual(self.state.last_error, "timeout")
        self.assertEqual(self.state.consecutive_failures, 1)

    def test_consecutive_failures_go_offline(self):
        self.state.mark_failed_poll("err1")
        self.state.mark_failed_poll("err2")
        self.state.mark_failed_poll("err3")
        self.assertEqual(self.state.connection_status, ConnectionStatus.OFFLINE)
        self.assertEqual(self.state.consecutive_failures, 3)

    def test_success_after_failures_resets(self):
        self.state.mark_failed_poll("err1")
        self.state.mark_failed_poll("err2")
        self.assertEqual(self.state.connection_status, ConnectionStatus.DEGRADED)
        self.state.mark_successful_poll(duration_ms=50.0)
        self.assertEqual(self.state.connection_status, ConnectionStatus.ONLINE)
        self.assertEqual(self.state.consecutive_failures, 0)
        self.assertIsNone(self.state.last_error)

    def test_data_stale_after_threshold(self):
        self.state.mark_successful_poll(duration_ms=50.0)
        self.assertTrue(self.state.is_data_fresh())
        # Simulate old timestamp
        self.state.last_successful_poll = datetime.now(timezone.utc) - timedelta(seconds=600)
        self.state.data_freshness_seconds = 300
        self.assertFalse(self.state.is_data_fresh())

    def test_mark_degraded(self):
        self.state.mark_successful_poll(duration_ms=50.0)
        self.state.mark_degraded("register 190 error")
        self.assertEqual(self.state.connection_status, ConnectionStatus.DEGRADED)
        self.assertEqual(self.state.last_error, "register 190 error")

    def test_to_dict(self):
        self.state.mark_successful_poll(duration_ms=120.5)
        d = self.state.to_dict()
        self.assertEqual(d["inverter_id"], "inv-1")
        self.assertEqual(d["connection_status"], "online")
        self.assertEqual(d["consecutive_failures"], 0)
        self.assertEqual(d["last_poll_duration_ms"], 120.5)
        self.assertTrue(d["data_fresh"])


if __name__ == "__main__":
    unittest.main()
