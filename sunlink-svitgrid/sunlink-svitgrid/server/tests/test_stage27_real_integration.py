"""
Stage 27 — offline тесты реального pipeline (без Modbus/оборудования).

Проверяют: единственный poller, backoff/reconnect, аддитивные миграции SQLite,
production-config pipeline без симуляции, API + WebSocket.
"""

from __future__ import annotations

import os
import sqlite3
import sys
import tempfile
import unittest

SERVER_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, SERVER_DIR)
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import deye_gateway  # noqa: E402
from data_source_config import DataSourceConfig  # noqa: E402
from domain.models import DataSource  # noqa: E402
from modbus.mock_fc03_transport import Fc03Result, MockFc03Transport  # noqa: E402
from modbus.read_only_modbus_client import ReadOnlyModbusClient  # noqa: E402
from services.deye_polling_service import (  # noqa: E402
    DeyePollingService,
    PollerAlreadyRunning,
    PollingState,
)
from services.production_pipeline import (  # noqa: E402
    ProductionPipelineError,
    attach_to_gateway,
    build_production_poller,
)
from storage.migrations import backup_database, current_version, migrate_database  # noqa: E402
from storage.sqlite_repository import SQLiteMeasurementRepository  # noqa: E402

from fastapi.testclient import TestClient  # noqa: E402


def ok_transport(raw=95):
    t = MockFc03Transport({184: raw})
    t.connected = True
    t.read_holding_registers = lambda a, count=1, device_id=1: Fc03Result([raw])
    return t


class TestSinglePoller(unittest.TestCase):
    def test_second_poller_for_same_inverter_rejected(self):
        p1 = DeyePollingService(ReadOnlyModbusClient(ok_transport()))
        p2 = DeyePollingService(ReadOnlyModbusClient(ok_transport()))
        p1.start()
        try:
            with self.assertRaises(PollerAlreadyRunning):
                p2.start()
        finally:
            p1.stop()
        # после stop можно запустить снова
        p2.start()
        try:
            self.assertTrue(p2.is_running())
        finally:
            p2.stop()


class TestReconnectBackoff(unittest.TestCase):
    def test_backoff_is_bounded_and_reconnect_counted(self):
        t = ok_transport()
        client = ReadOnlyModbusClient(t)
        poller = DeyePollingService(
            client, poll_interval=0.01, reconnect_backoff=0.5, max_backoff=1.0
        )
        # первый цикл успешен
        self.assertTrue(poller.read().connection_ok)
        # отказ
        t.read_holding_registers = lambda a, count=1, device_id=1: (_ for _ in ()).throw(
            __import__('modbus.mock_fc03_transport', fromlist=['Fc03Timeout']).Fc03Timeout('x')
        )
        fail = poller.read()
        self.assertFalse(fail.connection_ok)
        self.assertEqual(poller.state, PollingState.OFFLINE)
        # восстановление
        t.read_holding_registers = lambda a, count=1, device_id=1: Fc03Result([95])
        good = poller.read()
        self.assertTrue(good.connection_ok)
        self.assertAlmostEqual(good.value_of("battery_soc"), 95.0)
        self.assertLessEqual(poller._current_backoff, poller._max_backoff)


class TestSqliteMigrations(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp()
        self.db = os.path.join(self.tmp, "old.db")

    def _seed_old_db(self, version=0):
        conn = sqlite3.connect(self.db)
        conn.execute(
            "CREATE TABLE IF NOT EXISTS measurements ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT, inverter_id TEXT NOT NULL, "
            "register_address INTEGER, parameter_name TEXT NOT NULL, "
            "raw_value INTEGER, normalized_value REAL, unit TEXT NOT NULL, "
            "timestamp TEXT NOT NULL, quality TEXT NOT NULL, confidence TEXT NOT NULL, "
            "source TEXT NOT NULL, description TEXT DEFAULT '')"
        )
        conn.execute(
            "INSERT INTO measurements (inverter_id, parameter_name, normalized_value, "
            "unit, timestamp, quality, confidence, source) VALUES (?,?,?,?,?,?,?,?)",
            ("inv", "battery_soc", 95.0, "%", "2026-01-01T00:00:00+00:00",
             "valid", "corroborated", "deye_modbus"),
        )
        conn.execute(f"PRAGMA user_version = {version}")
        conn.commit()
        conn.close()

    def test_migration_preserves_rows_and_creates_backup(self):
        self._seed_old_db(0)
        result = migrate_database(self.db)
        self.assertEqual(result, "ok")
        self.assertEqual(current_version(self.db), 1)
        conn = sqlite3.connect(self.db)
        rows = conn.execute("SELECT normalized_value FROM measurements").fetchall()
        conn.close()
        self.assertEqual(rows, [(95.0,)])
        self.assertTrue(any(f.startswith("old.db.bak.") for f in os.listdir(self.tmp)))

    def test_version_never_downgraded(self):
        self._seed_old_db(5)
        self.assertEqual(migrate_database(self.db), "noop")
        self.assertEqual(current_version(self.db), 5)

    def test_repository_open_does_not_downgrade(self):
        self._seed_old_db(5)
        repo = SQLiteMeasurementRepository(self.db)
        self.assertEqual(repo.schema_version(), 5)
        self.assertEqual(repo.get_latest("inv", "battery_soc").normalized_value, 95.0)
        repo.close()


class TestProductionPipeline(unittest.TestCase):
    def _cfg(self, **extra):
        base = {
            "SUNLINK_DATA_SOURCE": "deye_modbus",
            "SUNLINK_ENABLE_MODBUS": "1",
            "SUNLINK_MODBUS_HOST": "127.0.0.1",
        }
        base.update(extra)
        return DataSourceConfig.from_env(base)

    def test_demo_source_refused_for_production(self):
        cfg = DataSourceConfig.from_env({"SUNLINK_DATA_SOURCE": "demo"})
        with self.assertRaises(ProductionPipelineError):
            build_production_poller(cfg)

    def test_modbus_without_explicit_flag_refused(self):
        with self.assertRaises(Exception):
            DataSourceConfig.from_env({
                "SUNLINK_DATA_SOURCE": "deye_modbus",
                "SUNLINK_ENABLE_MODBUS": "0",
            })

    def test_valid_config_builds_single_poller(self):
        poller, client, transport = build_production_poller(self._cfg())
        self.assertEqual(poller.source, DataSource.DEYE_MODBUS)
        self.assertEqual(client.request_count, 0)   # физического запроса нет
        self.assertEqual(poller.get_status()["active_registers"], [184])

    def test_attach_to_gateway_only_once(self):
        poller, _, _ = build_production_poller(self._cfg())
        gw = deye_gateway.DeyeGateway(deye_gateway.CONFIG)
        attach_to_gateway(gw, poller)
        self.assertIs(gw.telemetry_source, poller)
        p2, _, _ = build_production_poller(self._cfg())
        with self.assertRaises(ProductionPipelineError):
            attach_to_gateway(gw, p2)


class TestApiAndWebSocket(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(deye_gateway.app)
        self.old_gateway = deye_gateway.gateway
        self.old_repo = deye_gateway._repository

    def tearDown(self):
        self.client.close()
        deye_gateway.gateway = self.old_gateway
        deye_gateway._repository = self.old_repo

    def test_api_and_websocket_with_telemetry_source(self):
        source = demo_source = None
        from services.telemetry_sources import DemoTelemetrySource
        source = DemoTelemetrySource()
        repo = SQLiteMeasurementRepository(os.path.join(tempfile.mkdtemp(), "api.db"))
        deye_gateway.gateway = deye_gateway.DeyeGateway(deye_gateway.CONFIG, repo, source)
        deye_gateway._repository = repo

        for path in ("/status", "/api/inverters", "/api/live/raw",
                     "/api/measurements/latest"):
            r = self.client.get(path)
            self.assertEqual(r.status_code, 200, path)
        self.assertEqual(
            self.client.get("/api/live/raw").json()["batt_soc"], 85.0
        )

        with self.client.websocket_connect("/ws") as ws:
            msg = ws.receive_json()
            self.assertEqual(msg["type"], "live_data")
            self.assertIn("status", msg)


if __name__ == "__main__":
    unittest.main()
