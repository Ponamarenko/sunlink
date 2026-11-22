"""Comprehensive stage: full telemetry model, DWORD decoding, stats API.

Все тесты offline; модbus и production не используются.
"""

from __future__ import annotations

import os
import sys
import tempfile
import unittest
from datetime import datetime, timedelta, timezone
from unittest.mock import patch

SERVER_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, SERVER_DIR)

import deye_gateway  # noqa: E402
from domain.models import (  # noqa: E402
    ConfidenceLevel,
    DataSource,
    Measurement,
    MeasurementQuality,
)
from domain.register_decoder import decode_s_dword, decode_u_dword  # noqa: E402
from domain.telemetry import TelemetrySnapshot, TelemetryValue  # noqa: E402
from services.telemetry_sources import DemoTelemetrySource  # noqa: E402
from storage.sqlite_repository import SQLiteMeasurementRepository  # noqa: E402


class TestDwordDecoders(unittest.TestCase):
    def test_unsigned_lw_hw(self):
        self.assertAlmostEqual(decode_u_dword(6433, 0, scale=0.1), 643.3)
        self.assertEqual(decode_u_dword(0, 1, scale=1.0), 65536)

    def test_signed_negative(self):
        self.assertEqual(decode_s_dword(0xFFFF, 0xFFFF, scale=1.0), -1)
        self.assertEqual(decode_s_dword(0, 0x8000, scale=1.0), -2147483648)

    def test_word_order_matters(self):
        self.assertNotEqual(
            decode_u_dword(1, 2, scale=1.0),
            decode_u_dword(1, 2, scale=1.0, word_order="HW_LW"),
        )

    def test_invalid_words(self):
        with self.assertRaises(ValueError):
            decode_u_dword(70000, 0)
        with self.assertRaises(ValueError):
            decode_s_dword(0, -1)


class TestFullTelemetryModel(unittest.TestCase):
    def test_all_categories_present_and_nulls_are_none(self):
        snapshot = TelemetrySnapshot(
            inverter_id="deye_sg02lp1_am3",
            source=DataSource.DEYE_MODBUS,
            timestamp="2026-11-21T10:00:00+00:00",
            connection_ok=True,
            values={},
        )
        data = snapshot.to_full_dict()
        self.assertTrue(
            {"pv", "battery", "grid", "load", "inverter", "energy"}.issubset(
                set(data["categories"])
            )
        )
        for category in data["categories"].values():
            for item in category.values():
                self.assertIsNone(item)
        self.assertEqual(data["source"], "deye_modbus")
        self.assertFalse(data["stale"])

    def test_pv_total_is_calculated_only_from_present_powers(self):
        ts = "2026-11-21T10:00:00+00:00"
        values = {}
        for name, value in (("pv1_power", 1000.0), ("pv2_power", 500.0)):
            values[name] = TelemetryValue(name, value, "W", ts, "corroborated", "valid", int(value), 186)
        snapshot = TelemetrySnapshot("inv", DataSource.DEYE_MODBUS, ts, True, values)
        data = snapshot.to_full_dict()
        total = data["categories"]["pv"]["total_power"]
        self.assertEqual(total["normalized_value"], 1500.0)
        self.assertEqual(total["confidence"], "calculated")
        self.assertIsNone(total["raw_value"])

        partial = TelemetrySnapshot("inv", DataSource.DEYE_MODBUS, ts, True, {})
        self.assertIsNone(partial.to_full_dict()["categories"]["pv"]["total_power"])


class TestStatistics(unittest.TestCase):
    def test_aggregation_over_history(self):
        tmp = tempfile.NamedTemporaryFile(suffix=".db", delete=False)
        tmp.close()
        try:
            repo = SQLiteMeasurementRepository(tmp.name)
            now = datetime.now(timezone.utc)
            for i, value in enumerate((10.0, 20.0, 30.0)):
                repo.save_measurement(Measurement(
                    inverter_id="inv", register_address=184, parameter_name="battery_soc",
                    raw_value=int(value), normalized_value=value, unit="%",
                    timestamp=now + timedelta(seconds=i),
                    quality=MeasurementQuality.VALID,
                    confidence=ConfidenceLevel.CORROBORATED,
                    source=DataSource.DEYE_MODBUS,
                ))
            stats = repo.get_statistics(
                "inv", "battery_soc",
                now - timedelta(minutes=1), now + timedelta(minutes=1),
            )
            self.assertEqual(stats["count"], 3)
            self.assertEqual(stats["min"], 10.0)
            self.assertEqual(stats["max"], 30.0)
            self.assertEqual(stats["avg"], 20.0)
            self.assertEqual(stats["sum"], 60.0)
            repo.close()
        finally:
            os.unlink(tmp.name)


class TestApiCompatibility(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.NamedTemporaryFile(suffix=".db", delete=False)
        self.tmp.close()
        self.repository = SQLiteMeasurementRepository(self.tmp.name)
        self.gateway = deye_gateway.gateway
        self.old = (
            self.gateway.source,
            self.gateway.physical_modbus_allowed,
            self.gateway.telemetry_source,
            deye_gateway._repository,
        )
        self.gateway.telemetry_source = DemoTelemetrySource()
        self.gateway.source = DataSource.DEMO_SIMULATOR
        self.gateway.physical_modbus_allowed = False
        deye_gateway._repository = self.repository

    def tearDown(self):
        (
            self.gateway.source,
            self.gateway.physical_modbus_allowed,
            self.gateway.telemetry_source,
            deye_gateway._repository,
        ) = self.old
        self.repository.close()
        os.unlink(self.tmp.name)

    def test_new_and_legacy_endpoints(self):
        from fastapi.testclient import TestClient

        with TestClient(deye_gateway.app) as client:
            for path in (
                "/status", "/api/live/raw", "/api/measurements/latest",
                "/api/inverters", "/api/telemetry/full", "/api/health",
                "/api/version",
            ):
                with self.subTest(path=path):
                    self.assertEqual(client.get(path).status_code, 200)
            full = client.get("/api/telemetry/full").json()
            self.assertEqual(full["source"], "demo_simulator")
            self.assertIn("battery", full["categories"])
            self.assertEqual(
                full["categories"]["battery"]["soc"]["normalized_value"], 85.0
            )
            stats = client.get(
                "/api/stats/summary",
                params={
                    "parameter": "battery_soc",
                    "start": "2026-01-01T00:00:00Z",
                    "end": "2027-01-01T00:00:00Z",
                },
            )
            self.assertEqual(stats.status_code, 200)
            self.assertEqual(stats.json()["count"], 0)

    def test_websocket_still_emits_live_data(self):
        from fastapi.testclient import TestClient

        with TestClient(deye_gateway.app) as client:
            with client.websocket_connect("/ws") as ws:
                frame = ws.receive_json()
        self.assertEqual(frame["type"], "live_data")
        self.assertEqual(frame["status"]["source"], "demo_simulator")


if __name__ == "__main__":
    unittest.main()
