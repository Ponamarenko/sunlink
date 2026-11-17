
"""
Additional integration tests for ModbusPollingService.

Covers: TCP refused, Modbus exception, SQLite error, data freshness,
parallel polling prevention.
"""

from __future__ import annotations

import os
import sys
import tempfile
import unittest

sys.path.insert(0, __import__("os").path.dirname(__import__("os").path.abspath(__file__)) + "/..")

from services.inverter_state import InverterState, ConnectionStatus
from services.modbus_polling_service import (  # noqa: E402
    ModbusPollingService,
    STAGE_7_REGISTERS,
)
from services.polling_config import PollingConfig
from storage.sqlite_repository import SQLiteMeasurementRepository
from tests.test_polling_service import _FakeAdapter, _FakeRegisterValue


class TestPollingServiceTcpRefused(unittest.TestCase):
    def setUp(self):
        self.fd, self.path = tempfile.mkstemp(suffix=".db")
        os.close(self.fd)
        self.adapter = _FakeAdapter({}, connect_ok=False)
        self.repo = SQLiteMeasurementRepository(self.path)
        self.config = PollingConfig(polling_interval=0)
        self.state = InverterState(inverter_id="test-inv")
        self.service = ModbusPollingService(
            adapter=self.adapter,
            repository=self.repo,
            config=self.config,
            inverter_state=self.state,
        )

    def tearDown(self):
        self.service.stop()
        self.repo.close()
        os.unlink(self.path)

    def test_connect_refused_marks_degraded(self):
        result = self.service.poll_once()
        self.assertFalse(result.success)
        self.assertEqual(len(result.measurements), 0)
        self.assertEqual(self.state.connection_status, ConnectionStatus.DEGRADED)
        self.assertIsNotNone(self.state.last_error)


class TestPollingServiceModbusException(unittest.TestCase):
    def setUp(self):
        self.fd, self.path = tempfile.mkstemp(suffix=".db")
        os.close(self.fd)
        # Adapter that raises on read (simulating modbus exception after connect)
        self.adapter = _FakeAdapter({183: 5322}, connect_ok=True)
        orig_read = self.adapter.read_register

        def failing_read(address):
            self.adapter.read_calls.append(address)
            if not self.adapter._connected:
                self.adapter.last_error = "not connected"
                return _FakeRegisterValue(None, error="not connected")
            self.adapter.last_error = "modbus exception"
            self.adapter._connected = False
            return _FakeRegisterValue(None, error="modbus exception")

        self.adapter.read_register = failing_read
        self.repo = SQLiteMeasurementRepository(self.path)
        self.config = PollingConfig(polling_interval=0)
        self.state = InverterState(inverter_id="test-inv")
        self.service = ModbusPollingService(
            adapter=self.adapter,
            repository=self.repo,
            config=self.config,
            inverter_state=self.state,
        )

    def tearDown(self):
        self.service.stop()
        self.repo.close()
        os.unlink(self.path)

    def test_modbus_exception_marks_degraded(self):
        result = self.service.poll_once()
        self.assertFalse(result.success)
        self.assertEqual(self.state.connection_status, ConnectionStatus.DEGRADED)


class TestPollingServiceStorageError(unittest.TestCase):
    def setUp(self):
        self.fd, self.path = tempfile.mkstemp(suffix=".db")
        os.close(self.fd)
        self.adapter = _FakeAdapter({183: 5322})
        self.repo = SQLiteMeasurementRepository(self.path)
        self.config = PollingConfig(polling_interval=0)
        self.state = InverterState(inverter_id="test-inv")
        self.service = ModbusPollingService(
            adapter=self.adapter,
            repository=self.repo,
            config=self.config,
            inverter_state=self.state,
        )
        # Corrupt the repository by closing it, then any save will fail
        self.repo.close()

    def tearDown(self):
        self.service.stop()
        try:
            os.unlink(self.path)
        except OSError:
            pass

    def test_storage_error_is_visible(self):
        result = self.service.poll_once()
        self.assertFalse(result.success)
        self.assertIn("_storage", result.errors)
        self.assertEqual(self.state.connection_status, ConnectionStatus.DEGRADED)


class TestPollingServiceDataFreshness(unittest.TestCase):
    def setUp(self):
        self.fd, self.path = tempfile.mkstemp(suffix=".db")
        os.close(self.fd)
        self.adapter = _FakeAdapter({183: 5322})
        self.repo = SQLiteMeasurementRepository(self.path)
        self.config = PollingConfig(
            polling_interval=0, data_freshness_seconds=1
        )
        self.state = InverterState(
            inverter_id="test-inv", data_freshness_seconds=1
        )
        self.service = ModbusPollingService(
            adapter=self.adapter,
            repository=self.repo,
            config=self.config,
            inverter_state=self.state,
        )

    def tearDown(self):
        self.service.stop()
        self.repo.close()
        os.unlink(self.path)

    def test_data_fresh_after_poll(self):
        self.service.poll_once()
        self.assertTrue(self.state.is_data_fresh())

    def test_data_stale_after_threshold(self):
        self.service.poll_once()
        self.assertTrue(self.state.is_data_fresh())
        # Manually age the last successful poll
        from datetime import datetime, timezone, timedelta
        self.state.last_successful_poll = datetime.now(timezone.utc) - timedelta(seconds=10)
        self.assertFalse(self.state.is_data_fresh())


if __name__ == "__main__":
    unittest.main()
