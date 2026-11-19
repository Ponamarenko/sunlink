"""
Stage 16 — тесты единого pipeline источников телеметрии.

НЕТ физических подключений: только mock FC03-транспорт, DEMO-источник
и in-process HTTP (TestClient). Проверяется read-only, маркировка источников,
обработка отказов и совместимость Android API.
"""

from __future__ import annotations

import asyncio
import os
import sys
import tempfile
import unittest
from datetime import datetime, timedelta, timezone

SERVER_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, SERVER_DIR)
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from fastapi.testclient import TestClient  # noqa: E402

import deye_gateway  # noqa: E402
import demo_backend  # noqa: E402
from data_source_config import DataSourceConfig, DataSourceConfigError  # noqa: E402
from domain.models import DataSource, MeasurementQuality  # noqa: E402
from domain.register_metadata import REGISTER_MAP  # noqa: E402
from domain.telemetry import TelemetrySource  # noqa: E402
from storage.sqlite_repository import SQLiteMeasurementRepository  # noqa: E402
from modbus.mock_fc03_transport import (  # noqa: E402
    Fc03ConnectionRefused,
    Fc03InvalidUnit,
    Fc03ModbusException,
    Fc03Timeout,
    MockFc03Transport,
)
from services.telemetry_sources import (  # noqa: E402
    DEFAULT_DEMO_REGISTERS,
    DemoTelemetrySource,
    MockTelemetrySource,
    ModbusTelemetrySource,
)


class TestDataSourceConfig(unittest.TestCase):
    """Безопасный выбор источника: demo по умолчанию, modbus только явно."""

    def test_default_is_demo_without_network(self):
        cfg = DataSourceConfig.from_env({})
        self.assertEqual(cfg.source, "demo")
        self.assertFalse(cfg.enable_modbus)

    def test_explicit_demo_and_mock(self):
        self.assertEqual(DataSourceConfig.from_env({"SUNLINK_DATA_SOURCE": "demo"}).source, "demo")
        self.assertEqual(DataSourceConfig.from_env({"SUNLINK_DATA_SOURCE": "mock"}).source, "mock")

    def test_modbus_requires_explicit_opt_in(self):
        with self.assertRaises(DataSourceConfigError) as ctx:
            DataSourceConfig.from_env({"SUNLINK_DATA_SOURCE": "modbus"})
        self.assertIn("SUNLINK_ENABLE_MODBUS", str(ctx.exception))

        cfg = DataSourceConfig.from_env({
            "SUNLINK_DATA_SOURCE": "modbus",
            "SUNLINK_ENABLE_MODBUS": "1",
        })
        self.assertEqual(cfg.source, "modbus")
        self.assertTrue(cfg.enable_modbus)

    def test_unknown_source_gives_clear_error(self):
        with self.assertRaises(DataSourceConfigError) as ctx:
            DataSourceConfig.from_env({"SUNLINK_DATA_SOURCE": "production"})
        self.assertIn("SUNLINK_DATA_SOURCE", str(ctx.exception))

    def test_invalid_numbers_rejected(self):
        with self.assertRaises(DataSourceConfigError):
            DataSourceConfig.from_env({"SUNLINK_MODBUS_UNIT_ID": "0"})
        with self.assertRaises(DataSourceConfigError):
            DataSourceConfig.from_env({"SUNLINK_MODBUS_PORT": "70000"})


class TestUnifiedTelemetrySources(unittest.TestCase):
    """Все источники возвращают TelemetrySnapshot одного формата."""

    def test_demo_source_is_labelled_and_normalized(self):
        source = DemoTelemetrySource()
        self.assertEqual(source.source, DataSource.DEMO_SIMULATOR)
        snap = source.read()

        self.assertEqual(snap.source, DataSource.DEMO_SIMULATOR)
        self.assertTrue(snap.connection_ok)
        self.assertTrue(snap.error is None)
        self.assertEqual(set(snap.values), set(meta.name for meta in REGISTER_MAP.values()))
        self.assertAlmostEqual(snap.values["battery_soc"].normalized_value, 85.0)
        self.assertAlmostEqual(snap.values["battery_voltage"].normalized_value, 53.22)
        self.assertAlmostEqual(snap.values["dc_temperature"].normalized_value, 36.5)
        for value in snap.values.values():
            self.assertIn("T", value.timestamp)  # ISO UTC
            self.assertIn(value.confidence, {"corroborated", "provisional"})

    def test_demo_missing_register_is_none_not_zero(self):
        source = DemoTelemetrySource({184: 85})  # только SOC
        snap = source.read()
        self.assertEqual(snap.values["battery_soc"].normalized_value, 85.0)
        self.assertIsNone(snap.values["pv1_power"].normalized_value)
        self.assertEqual(
            snap.values["pv1_power"].quality, MeasurementQuality.UNAVAILABLE.value
        )

    def test_mock_source_normal_read(self):
        transport = MockFc03Transport(DEFAULT_DEMO_REGISTERS, unit_id=1)
        source = MockTelemetrySource(transport, unit_id=1)
        snap = source.read()

        self.assertEqual(source.source, DataSource.MOCK)
        self.assertEqual(snap.source, DataSource.MOCK)
        self.assertTrue(snap.connection_ok)
        self.assertAlmostEqual(snap.values["battery_soc"].normalized_value, 85.0)
        self.assertEqual(len(transport.read_calls), len(REGISTER_MAP))

    def test_modbus_source_labelled_but_no_network(self):
        transport = MockFc03Transport(DEFAULT_DEMO_REGISTERS)
        source = ModbusTelemetrySource(transport)
        self.assertEqual(source.source, DataSource.DEYE_MODBUS)
        snap = source.read()
        self.assertEqual(snap.source, DataSource.DEYE_MODBUS)
        self.assertTrue(snap.connection_ok)

    def test_failure_modes_never_fabricate_values(self):
        cases = [
            ("fail_timeout", True, Fc03Timeout),
            ("fail_connection", True, Fc03ConnectionRefused),
            ("fail_modbus_exception", True, Fc03ModbusException),
            ("wrong_unit_id", 7, Fc03InvalidUnit),
        ]
        for attr, value, _exc in cases:
            with self.subTest(mode=attr):
                transport = MockFc03Transport(DEFAULT_DEMO_REGISTERS)
                setattr(transport, attr, value)
                source = MockTelemetrySource(transport)
                snap = source.read()
                self.assertFalse(snap.connection_ok)
                self.assertIsNotNone(snap.error)
                self.assertTrue(
                    all(v.normalized_value is None for v in snap.values.values()),
                    "отказ источника не должен превращаться в нули",
                )

    def test_invalid_length_and_missing_register(self):
        transport = MockFc03Transport(DEFAULT_DEMO_REGISTERS)
        transport.invalid_length_at = 184
        transport.missing_addresses.add(186)
        snap = MockTelemetrySource(transport).read()

        self.assertIsNone(snap.values["battery_soc"].normalized_value)
        self.assertIsNone(snap.values["pv1_power"].normalized_value)
        self.assertAlmostEqual(snap.values["battery_voltage"].normalized_value, 53.22)

    def test_reconnect_after_failure(self):
        transport = MockFc03Transport(DEFAULT_DEMO_REGISTERS)
        transport.fail_connection = True
        source = MockTelemetrySource(transport)
        self.assertFalse(source.read().connection_ok)

        transport.fail_connection = False
        snap = source.read()
        self.assertTrue(snap.connection_ok)
        self.assertAlmostEqual(snap.values["battery_soc"].normalized_value, 85.0)

    def test_timestamp_is_fresh_per_snapshot(self):
        source = DemoTelemetrySource()
        first = source.read()
        second = source.read()
        parsed_first = datetime.fromisoformat(first.timestamp)
        parsed_second = datetime.fromisoformat(second.timestamp)
        self.assertGreaterEqual(parsed_second, parsed_first)
        self.assertIsNotNone(parsed_first.tzinfo)

    def test_sources_have_no_write_interface(self):
        transport = MockFc03Transport(DEFAULT_DEMO_REGISTERS)
        for obj in (transport, MockTelemetrySource(transport), DemoTelemetrySource()):
            for name in ("write_register", "write_registers", "write_coil", "set_register"):
                with self.assertRaises(AttributeError):
                    getattr(obj, name)
        self.assertFalse(hasattr(TelemetrySource, "write"))


class TestSourceToSqlitePipeline(unittest.TestCase):
    """Mock/DEMO ответ -> normalizer -> модель -> SQLite -> source-метка."""

    def setUp(self):
        self.tmpdir = tempfile.mkdtemp()
        self.db_path = os.path.join(self.tmpdir, "pipeline.db")
        self.repo = SQLiteMeasurementRepository(self.db_path)

    def tearDown(self):
        self.repo.close()

    def test_demo_snapshot_is_persisted_with_source_label(self):
        source = DemoTelemetrySource({184: 85})  # только SOC доступен
        gateway = deye_gateway.DeyeGateway(deye_gateway.CONFIG, self.repo, source)
        data = asyncio.run(gateway.read_all_registers())

        self.assertEqual(set(data), set(meta.name for meta in REGISTER_MAP.values()))
        self.assertEqual([k for k, v in data.items() if v["normalized_value"] is not None], ["battery_soc"])
        latest = self.repo.get_all_latest("deye_sg02lp1_am3")
        self.assertEqual(len(latest), 1)
        m = latest[0]
        self.assertEqual(m.parameter_name, "battery_soc")
        self.assertEqual(m.normalized_value, 85.0)
        self.assertEqual(m.source, DataSource.DEMO_SIMULATOR)
        self.assertIsNotNone(m.timestamp.tzinfo)
        # Отсутствующие регистры не записаны нулями
        self.assertIsNone(self.repo.get_latest("deye_sg02lp1_am3", "pv1_power"))

    def test_mock_snapshot_persists_all_nine_registers(self):
        transport = MockFc03Transport(DEFAULT_DEMO_REGISTERS)
        source = MockTelemetrySource(transport)
        gateway = deye_gateway.DeyeGateway(deye_gateway.CONFIG, self.repo, source)
        asyncio.run(gateway.read_all_registers())

        latest = self.repo.get_all_latest("deye_sg02lp1_am3")
        self.assertEqual(len(latest), len(REGISTER_MAP))
        self.assertTrue(all(m.source == DataSource.MOCK for m in latest))
        self.assertTrue(all(m.normalized_value is not None for m in latest))


class TestStage16ApiCompatibility(unittest.TestCase):
    """Android API остаётся совместимым; демо явно помечается."""

    def setUp(self):
        self.tmpdir = tempfile.mkdtemp()
        self.db_path = os.path.join(self.tmpdir, "stage16.db")
        self.repo = demo_backend.configure(self.db_path)
        demo_backend.seed_demo_history(self.repo)
        self.client = TestClient(deye_gateway.app)

    def tearDown(self):
        self.client.close()
        self.repo.close()

    def test_live_raw_has_source_and_no_fabricated_grid(self):
        data = self.client.get("/api/live/raw").json()
        self.assertEqual(data["source"], DataSource.DEMO_SIMULATOR.value)
        self.assertEqual(data["batt_soc"], 85.0)
        self.assertIsNone(data["p_grid"])
        self.assertIsNone(data["p_load"])
        self.assertEqual(data["inverter_id"], "deye_sg02lp1_am3")

    def test_status_and_inverters_are_labelled(self):
        status = self.client.get("/status").json()
        self.assertEqual(status["source"], DataSource.DEMO_SIMULATOR.value)
        self.assertTrue(status["connection_status"])
        inverters = self.client.get("/api/inverters").json()
        self.assertEqual(inverters["source"], DataSource.DEMO_SIMULATOR.value)
        self.assertEqual(inverters["inverters"][0]["source"], DataSource.DEMO_SIMULATOR.value)

    def test_measurements_and_history_keep_source(self):
        latest = self.client.get("/api/measurements/latest").json()
        self.assertEqual(latest["count"], len(REGISTER_MAP))
        for m in latest["measurements"]:
            self.assertEqual(m["source"], DataSource.DEMO_SIMULATOR.value)
            self.assertIsNotNone(m["timestamp"])

        start = (datetime.now(timezone.utc) - timedelta(hours=2)).isoformat().replace("+00:00", "Z")
        end = (datetime.now(timezone.utc) + timedelta(hours=1)).isoformat().replace("+00:00", "Z")
        history = self.client.get(
            f"/api/measurements/history?parameter=battery_soc&start={start}&end={end}"
        ).json()
        self.assertGreater(history["count"], 0)
        for m in history["measurements"]:
            self.assertEqual(m["source"], DataSource.DEMO_SIMULATOR.value)

    def test_no_write_endpoints(self):
        paths = [r.path for r in deye_gateway.app.routes if hasattr(r, "path")]
        for forbidden in ("/write", "/set-register", "/control", "/emergency-stop"):
            self.assertNotIn(forbidden, paths)


if __name__ == "__main__":
    unittest.main()
