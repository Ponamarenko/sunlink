
"""
Tests for MockModbusClient.
"""

from __future__ import annotations

import sys
import unittest

sys.path.insert(0, __import__("os").path.dirname(__import__("os").path.abspath(__file__)) + "/..")

from modbus.mock_client import MockModbusClient  # noqa: E402


class TestMockConnect(unittest.TestCase):
    def test_successful_connect(self):
        client = MockModbusClient(connect_ok=True)
        self.assertTrue(client.connect())
        self.assertTrue(client.connected)
        client.disconnect()
        self.assertFalse(client.connected)

    def test_tcp_refused(self):
        client = MockModbusClient(tcp_refused=True)
        self.assertFalse(client.connect())
        self.assertFalse(client.connected)
        self.assertEqual(client.last_error, "connection refused")

    def test_connect_failed(self):
        client = MockModbusClient(connect_ok=False)
        self.assertFalse(client.connect())
        self.assertFalse(client.connected)


class TestMockReadRegister(unittest.TestCase):
    def setUp(self):
        self.client = MockModbusClient(
            registers={183: 5322, 184: 85, 186: 100},
            connect_ok=True,
        )
        self.client.connect()

    def tearDown(self):
        self.client.disconnect()

    def test_read_existing_register(self):
        val = self.client.read_register(183)
        self.assertEqual(val, 5322)
        self.assertIsNone(self.client.last_error)

    def test_read_unknown_register(self):
        val = self.client.read_register(999)
        self.assertIsNone(val)
        self.assertIsNotNone(self.client.last_error)

    def test_read_when_not_connected(self):
        self.client.disconnect()
        val = self.client.read_register(183)
        self.assertIsNone(val)
        self.assertEqual(self.client.last_error, "not connected")

    def test_set_register(self):
        self.client.set_register(183, 5400)
        self.assertEqual(self.client.read_register(183), 5400)


class TestMockFailureModes(unittest.TestCase):
    def test_read_timeout(self):
        client = MockModbusClient(
            registers={183: 5322}, connect_ok=True, read_timeout=True
        )
        client.connect()
        val = client.read_register(183)
        self.assertIsNone(val)
        self.assertEqual(client.last_error, "modbus timeout")
        self.assertFalse(client.connected)
        client.disconnect()

    def test_modbus_exception(self):
        client = MockModbusClient(
            registers={183: 5322}, connect_ok=True, modbus_exception=True
        )
        client.connect()
        val = client.read_register(183)
        self.assertIsNone(val)
        self.assertEqual(client.last_error, "modbus exception")
        self.assertFalse(client.connected)
        client.disconnect()

    def test_per_register_error(self):
        client = MockModbusClient(
            registers={183: 5322, 184: 85},
            connect_ok=True,
            error_registers={184: "illegal data address"},
        )
        client.connect()
        self.assertEqual(client.read_register(183), 5322)
        self.assertIsNone(client.read_register(184))
        self.assertEqual(client.last_error, "illegal data address")
        client.disconnect()

    def test_tcp_refused_connect(self):
        client = MockModbusClient(tcp_refused=True)
        self.assertFalse(client.connect())
        client.disconnect()

    def test_recovery_after_timeout(self):
        client = MockModbusClient(
            registers={183: 5322}, connect_ok=True, read_timeout=True
        )
        client.connect()
        client.read_register(183)  # fails with timeout, disconnects
        self.assertFalse(client.connected)
        # Recover: clear timeout and reconnect
        client.set_read_timeout(False)
        self.assertTrue(client.connect())
        self.assertEqual(client.read_register(183), 5322)
        client.disconnect()


if __name__ == "__main__":
    unittest.main()
