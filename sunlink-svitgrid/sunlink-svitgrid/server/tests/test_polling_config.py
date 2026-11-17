
"""
Tests for PollingConfig.
"""

from __future__ import annotations

import sys
import unittest

sys.path.insert(0, __import__("os").path.dirname(__import__("os").path.abspath(__file__)) + "/..")

from services.polling_config import PollingConfig  # noqa: E402


class TestPollingConfigDefaults(unittest.TestCase):
    def test_defaults(self):
        cfg = PollingConfig()
        self.assertEqual(cfg.inverter_id, "2507245705")
        self.assertEqual(cfg.polling_interval, 5.0)
        self.assertEqual(cfg.connect_timeout, 2.0)
        self.assertEqual(cfg.read_timeout, 2.0)
        self.assertEqual(cfg.max_register_retries, 1)
        self.assertEqual(cfg.retry_backoff_base, 0.5)
        self.assertEqual(cfg.retry_backoff_max, 2.0)
        self.assertEqual(cfg.max_consecutive_failures, 3)
        self.assertEqual(cfg.data_freshness_seconds, 30.0)

    def test_custom_values(self):
        cfg = PollingConfig(
            inverter_id="test-inv",
            polling_interval=10.0,
            connect_timeout=3.0,
            read_timeout=1.0,
            max_register_retries=2,
            max_consecutive_failures=5,
            data_freshness_seconds=60.0,
        )
        self.assertEqual(cfg.inverter_id, "test-inv")
        self.assertEqual(cfg.polling_interval, 10.0)


class TestPollingConfigValidation(unittest.TestCase):
    def test_negative_interval(self):
        with self.assertRaises(ValueError):
            PollingConfig(polling_interval=-1)

    def test_zero_connect_timeout(self):
        with self.assertRaises(ValueError):
            PollingConfig(connect_timeout=0)

    def test_negative_read_timeout(self):
        with self.assertRaises(ValueError):
            PollingConfig(read_timeout=-0.5)

    def test_negative_retries(self):
        with self.assertRaises(ValueError):
            PollingConfig(max_register_retries=-1)

    def test_zero_consecutive_failures(self):
        with self.assertRaises(ValueError):
            PollingConfig(max_consecutive_failures=0)

    def test_zero_freshness(self):
        with self.assertRaises(ValueError):
            PollingConfig(data_freshness_seconds=0)


if __name__ == "__main__":
    unittest.main()
