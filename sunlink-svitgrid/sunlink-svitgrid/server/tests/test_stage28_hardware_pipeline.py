"""
Stage 28 — read-only hardware pipeline regression tests (MOCK ONLY).

Физическое оборудование в unit-тестах не используется. Тесты проверяют,
что physical pipeline安全工作 и не может перейти на симуляцию/write.
"""

from __future__ import annotations

import os
import sys
import unittest

SERVER_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, SERVER_DIR)
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from data_source_config import DataSourceConfig  # noqa: E402
from domain.models import DataSource  # noqa: E402
from modbus.mock_fc03_transport import (  # noqa: E402
    Fc03InvalidUnit,
    Fc03ModbusException,
    Fc03Result,
    Fc03Timeout,
    MockFc03Transport,
)
from modbus.read_only_modbus_client import ReadOnlyModbusClient  # noqa: E402
from services.deye_polling_service import DeyePollingService  # noqa: E402
from services.production_pipeline import (  # noqa: E402
    ProductionPipelineError,
    build_production_poller,
)


class TestReadOnlyTransport(unittest.TestCase):
    def setUp(self):
        self.t = MockFc03Transport({184: 94})
        self.t.connected = True
        self.client = ReadOnlyModbusClient(self.t)

    def test_fc03_only_and_correct_frame(self):
        val = self.client.read_register(184, 1)
        self.assertEqual(val, 94)
        self.assertEqual(self.client.request_count, 1)

    def test_no_write_methods(self):
        for name in ("write_register", "write_coil", "write_registers"):
            with self.assertRaises(AttributeError):
                getattr(self.client, name)

    def test_wrong_unit_id(self):
        self.t.wrong_unit_id = 2
        with self.assertRaises(Fc03InvalidUnit):
            self.client.read_register(184, 1)

    def test_modbus_exception(self):
        self.t.fail_modbus_exception = True
        with self.assertRaises(Fc03ModbusException):
            self.client.read_register(184, 1)

    def test_timeout_and_close(self):
        self.t.fail_timeout = True
        with self.assertRaises(Fc03Timeout):
            self.client.read_register(184, 1)
        self.client.close()
        self.assertFalse(self.t.connected)


class TestPhysicalPipelineSafety(unittest.TestCase):
    def cfg(self, **extra):
        base = {
            "SUNLINK_DATA_SOURCE": "deye_modbus",
            "SUNLINK_ENABLE_MODBUS": "1",
            "SUNLINK_MODBUS_HOST": "192.168.25.15",
        }
        base.update(extra)
        return DataSourceConfig.from_env(base)

    def test_poller_not_built_without_opt_in(self):
        with self.assertRaises(Exception):
            cfg = DataSourceConfig.from_env({
                "SUNLINK_DATA_SOURCE": "deye_modbus", "SUNLINK_ENABLE_MODBUS": "0"
            })
            build_production_poller(cfg)

    def test_demo_source_never_used_in_production_pipeline(self):
        cfg = DataSourceConfig.from_env({"SUNLINK_DATA_SOURCE": "demo"})
        with self.assertRaises(ProductionPipelineError):
            build_production_poller(cfg)

    def test_error_is_not_masked_as_online_or_demo(self):
        transport = MockFc03Transport({184: 94})
        client = ReadOnlyModbusClient(transport)
        transport.connected = True
        transport.read_holding_registers = lambda a, count=1, device_id=1: Fc03Result([])
        poller = DeyePollingService(client)
        snap = poller.read()
        self.assertEqual(snap.source, DataSource.DEYE_MODBUS)
        self.assertFalse(snap.connection_ok)
        self.assertIsNone(snap.value_of("battery_soc"))
        self.assertEqual(poller.state.value, "offline")


if __name__ == "__main__":
    unittest.main()
