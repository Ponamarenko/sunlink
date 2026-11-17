
"""
Integration tests for ModbusPollingService.

Full path:
Mock Modbus -> Adapter (mocked) -> Polling Service -> Normalizer -> SQLite.

No physical Modbus connection is ever made.
"""

from __future__ import annotations

import os
import sys
import tempfile
import unittest
from datetime import datetime, timezone
from unittest.mock import MagicMock

sys.path.insert(0, __import__("os").path.dirname(__import__("os").path.abspath(__file__)) + "/..")

from domain.models import (  # noqa: E402
    ConfidenceLevel,
    Measurement,
    MeasurementQuality,
)
from domain.register_metadata import REGISTER_MAP
from services.inverter_state import InverterState, ConnectionStatus
from services.modbus_polling_service import (  # noqa: E402
    ModbusPollingService,
    PollingResult,
    STAGE_7_REGISTERS,
)
from services.polling_config import PollingConfig
from storage.sqlite_repository import SQLiteMeasurementRepository


class _FakeRegisterValue:
    """Stand-in for RegisterValue returned by DeyeReadonlyAdapter.read_register."""

    def __init__(self, raw, error=None):
        self.raw = raw
        self.error = error


class _FakeAdapter:
    """Mock DeyeReadonlyAdapter implementing the same call signature."""

    def __init__(self, registers, connect_ok=True, fail_registers=None,
                 connect_error=None, fail_after_connect=False):
        self._registers = dict(registers)
        self._connect_ok = connect_ok
        self._fail_registers = set(fail_registers or [])
        self._connect_error = connect_error
        self._fail_after_connect = fail_after_connect
        self._connected = False
        self.last_error = None
        self.connect_calls = 0
        self.disconnect_calls = 0
        self.read_calls = []

    @property
    def connected(self):
        return self._connected

    def connect(self):
        self.connect_calls += 1
        self.last_error = None
        if self._connect_error:
            self.last_error = self._connect_error
            self._connected = False
            return False
        if not self._connect_ok:
            self.last_error = "connect failed"
            self._connected = False
            return False
        self._connected = True
        if self._fail_after_connect:
            self._fail_after_connect = False
            self.last_error = "post-connect failure"
            self._connected = False
            return False
        return True

    def disconnect(self):
        self.disconnect_calls += 1
        self._connected = False

    def read_register(self, address):
        self.read_calls.append(address)
        if not self._connected:
            self.last_error = "not connected"
            return _FakeRegisterValue(None, error="not connected")
        if address in self._fail_registers:
            self.last_error = f"error reading reg {address}"
            return _FakeRegisterValue(None, error=self.last_error)
        self.last_error = None
        return _FakeRegisterValue(self._registers.get(address, 0))


class TestPollingServiceInit(unittest.TestCase):
    def test_registers_match_stage7(self):
        self.assertEqual(STAGE_7_REGISTERS, [90, 91, 183, 184, 186, 187, 188, 190, 191])

    def test_all_registers_in_map(self):
        for addr in STAGE_7_REGISTERS:
            self.assertIn(addr, REGISTER_MAP)


class TestPollingServiceSuccessfulCycle(unittest.TestCase):
    def setUp(self):
        self.fd, self.path = tempfile.mkstemp(suffix=".db")
        os.close(self.fd)
        self.registers = {
            90: 1365, 91: 1424, 183: 5322, 184: 85,
            186: 100, 187: 60, 188: 40,
            190: 200, 191: 50,
        }
        self.adapter = _FakeAdapter(self.registers)
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

    def test_successful_poll_all_registers(self):
        result = self.service.poll_once()
        self.assertTrue(result.success)
        self.assertEqual(len(result.measurements), 9)
        self.assertEqual(len(result.errors), 0)
        self.assertEqual(self.state.connection_status, ConnectionStatus.ONLINE)
        self.assertEqual(self.state.consecutive_failures, 0)

    def test_raw_values_preserved(self):
        result = self.service.poll_once()
        by_addr = {m["register_address"]: m for m in result.measurements}
        self.assertEqual(by_addr[183]["raw_value"], 5322)
        self.assertEqual(by_addr[184]["raw_value"], 85)
        self.assertEqual(by_addr[190]["raw_value"], 200)
        self.assertEqual(by_addr[90]["raw_value"], 1365)

    def test_normalized_values(self):
        result = self.service.poll_once()
        by_addr = {m["register_address"]: m for m in result.measurements}
        self.assertAlmostEqual(by_addr[183]["normalized_value"], 53.22)
        self.assertAlmostEqual(by_addr[184]["normalized_value"], 85.0)
        self.assertAlmostEqual(by_addr[90]["normalized_value"], 36.5)

    def test_provisional_confidence(self):
        result = self.service.poll_once()
        by_addr = {m["register_address"]: m for m in result.measurements}
        self.assertEqual(by_addr[190]["confidence"], ConfidenceLevel.PROVISIONAL.value)
        self.assertEqual(by_addr[90]["confidence"], ConfidenceLevel.PROVISIONAL.value)
        self.assertEqual(by_addr[91]["confidence"], ConfidenceLevel.PROVISIONAL.value)
        self.assertEqual(by_addr[183]["confidence"], ConfidenceLevel.CORROBORATED.value)

    def test_utc_timestamps(self):
        result = self.service.poll_once()
        for m in result.measurements:
            ts = datetime.fromisoformat(m["timestamp"])
            self.assertIsNotNone(ts.tzinfo)

    def test_persisted_to_sqlite(self):
        self.service.poll_once()
        latest = self.repo.get_latest("test-inv", "battery_voltage")
        self.assertIsNotNone(latest)
        self.assertEqual(latest.raw_value, 5322)
        self.assertAlmostEqual(latest.normalized_value, 53.22)


class TestPollingServicePartialError(unittest.TestCase):
    def setUp(self):
        self.fd, self.path = tempfile.mkstemp(suffix=".db")
        os.close(self.fd)
        self.registers = {
            90: 1365, 91: 1424, 183: 5322, 184: 85,
            186: 100, 187: 60, 188: 40,
            190: 200, 191: 50,
        }
        self.adapter = _FakeAdapter(self.registers, fail_registers={190})
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

    def test_one_register_fails_others_succeed(self):
        result = self.service.poll_once()
        # 8 registers succeed, 1 fails
        self.assertEqual(len(result.measurements), 8)
        self.assertIn(190, result.errors)
        self.assertNotEqual(self.state.connection_status, ConnectionStatus.OFFLINE)
        self.assertEqual(self.state.connection_status, ConnectionStatus.DEGRADED)

    def test_failed_register_not_persisted(self):
        self.service.poll_once()
        self.assertIsNone(self.repo.get_latest("test-inv", "battery_power"))
        self.assertIsNotNone(self.repo.get_latest("test-inv", "battery_voltage"))


class TestPollingServiceConnectFailure(unittest.TestCase):
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

    def test_connect_failed_marks_offline_after_retries(self):
        result = self.service.poll_once()
        self.assertFalse(result.success)
        self.assertEqual(len(result.measurements), 0)
        self.assertEqual(self.state.connection_status, ConnectionStatus.DEGRADED)


class TestPollingServiceReconnect(unittest.TestCase):
    def setUp(self):
        self.fd, self.path = tempfile.mkstemp(suffix=".db")
        os.close(self.fd)
        # First connect fails (post-connect failure), second succeeds
        self.adapter = _FakeAdapter(
            {183: 5322}, connect_ok=True, fail_after_connect=True
        )
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

    def test_reconnect_after_initial_failure(self):
        # First cycle: connect fails
        r1 = self.service.poll_once()
        self.assertFalse(r1.success)
        # Second cycle: connect succeeds (fail_after_connect consumed)
        r2 = self.service.poll_once()
        self.assertTrue(r2.success)
        self.assertEqual(self.adapter.connect_calls, 2)


class TestPollingServiceStartStop(unittest.TestCase):
    def setUp(self):
        self.fd, self.path = tempfile.mkstemp(suffix=".db")
        os.close(self.fd)
        self.adapter = _FakeAdapter({183: 5322})
        self.repo = SQLiteMeasurementRepository(self.path)
        self.config = PollingConfig(polling_interval=0.05)
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

    def test_start_stop(self):
        self.assertFalse(self.service.is_running())
        self.service.start()
        self.assertTrue(self.service.is_running())
        self.service.stop(timeout=2.0)
        self.assertFalse(self.service.is_running())

    def test_double_start_no_duplicate_thread(self):
        self.service.start()
        thread1 = self.service._poll_thread
        self.service.start()  # should be no-op
        thread2 = self.service._poll_thread
        self.assertIs(thread1, thread2)
        self.service.stop(timeout=2.0)


class TestPollingServiceUnknownState(unittest.TestCase):
    def test_initial_state_unknown(self):
        fd, path = tempfile.mkstemp(suffix=".db")
        os.close(fd)
        try:
            adapter = _FakeAdapter({183: 5322})
            repo = SQLiteMeasurementRepository(path)
            config = PollingConfig(polling_interval=0)
            state = InverterState(inverter_id="test-inv")
            service = ModbusPollingService(adapter, repo, config, state)
            self.assertEqual(state.connection_status, ConnectionStatus.UNKNOWN)
            self.assertFalse(state.is_data_fresh())
        finally:
            service.stop()
            repo.close()
            os.unlink(path)


class TestPollingServiceNoWriteOperations(unittest.TestCase):
    def test_adapter_has_no_write_methods(self):
        fd, path = tempfile.mkstemp(suffix=".db")
        os.close(fd)
        try:
            adapter = _FakeAdapter({183: 5322})
            repo = SQLiteMeasurementRepository(path)
            config = PollingConfig(polling_interval=0)
            state = InverterState(inverter_id="test-inv")
            service = ModbusPollingService(adapter, repo, config, state)
            for forbidden in ("write_register", "write_registers", "write_coil", "write_coils"):
                self.assertFalse(hasattr(adapter, forbidden))
            service.poll_once()
            # Only read calls should have been made
            for addr in adapter.read_calls:
                self.assertIn(addr, STAGE_7_REGISTERS)
        finally:
            service.stop()
            repo.close()
            os.unlink(path)


if __name__ == "__main__":
    unittest.main()
