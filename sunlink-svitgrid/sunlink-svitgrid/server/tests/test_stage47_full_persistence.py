"""Stage 47 — полное сохранение реальной телеметрии через существующий poller.

Только offline/mock. Никаких физических Modbus-запросов.
"""

from __future__ import annotations

import math
import os
import sys
import tempfile
import unittest
from datetime import datetime, timedelta, timezone

SERVER_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, SERVER_DIR)

import deye_gateway  # noqa: E402
from domain.models import (  # noqa: E402
    ConfidenceLevel, DataSource, Measurement, MeasurementQuality,
)
from domain.telemetry import TelemetrySnapshot, TelemetryValue  # noqa: E402
from modbus.mock_fc03_transport import MockFc03Transport  # noqa: E402
from modbus.read_only_modbus_client import ReadOnlyModbusClient  # noqa: E402
from services.deye_polling_service import DeyePollingService  # noqa: E402
from services.history_service import KNOWN_PARAMETERS, build_series  # noqa: E402
from storage.sqlite_repository import SQLiteMeasurementRepository  # noqa: E402


TS = "2026-11-22T10:00:00+00:00"


def value(name, number, unit="W", confidence="corroborated", quality="valid", address=1):
    try:
        raw = int(number)
    except (ValueError, OverflowError):
        raw = None
    return TelemetryValue(name, number, unit, TS, confidence, quality, raw, address)


class TestFullPersistence(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.NamedTemporaryFile(suffix=".db", delete=False)
        self.tmp.close()
        self.repo = SQLiteMeasurementRepository(self.tmp.name)

    def tearDown(self):
        self.repo.close()
        os.unlink(self.tmp.name)

    def _poller(self, repo=None):
        transport = MockFc03Transport({184: 94})
        transport.connected = True
        return DeyePollingService(ReadOnlyModbusClient(transport), repository=repo or self.repo)

    def _snapshot(self, values):
        return TelemetrySnapshot("inv", DataSource.DEYE_MODBUS, TS, True, values)

    def test_all_available_parameters_saved_in_one_cycle(self):
        poller = self._poller()
        values = {
            "battery_soc": value("battery_soc", 94.0, "%", address=184),
            "battery_voltage": value("battery_voltage", 53.3, "V", "model_compatible", address=183),
            "battery_current": value("battery_current", 2.44, "A", "model_compatible", address=191),
            "pv1_power": value("pv1_power", 5619.0, "W", "physically_verified", address=186),
            "pv2_power": value("pv2_power", 945.0, "W", "physically_verified", address=187),
            "pv3_power": value("pv3_power", 816.0, "W", "physically_verified", address=188),
            "grid_frequency": value("grid_frequency", 49.96, "Hz", "model_compatible", address=192),
            "load_power": value("load_power", 6885.0, "W", "model_compatible", address=175),
        }
        poller._persist(self._snapshot(values))
        for parameter in list(values) + ["pv_total_power"]:
            history = self.repo.get_history("inv", parameter, datetime(2026, 1, 1, tzinfo=timezone.utc), datetime(2027, 1, 1, tzinfo=timezone.utc))
            self.assertEqual(len(history), 1, parameter)
        total = self.repo.get_latest("inv", "pv_total_power")
        self.assertEqual(total.normalized_value, 7380.0)
        self.assertEqual(total.source, DataSource.CALCULATED)

    def test_two_inverters_are_not_mixed(self):
        poller = self._poller()
        snap_a = TelemetrySnapshot("invA", DataSource.DEYE_MODBUS, TS, True,
                                   {"battery_soc": value("battery_soc", 90.0, "%")})
        snap_b = TelemetrySnapshot("invB", DataSource.DEYE_MODBUS, TS, True,
                                   {"battery_soc": value("battery_soc", 80.0, "%")})
        poller._persist(snap_a)
        poller._persist(snap_b)
        a = self.repo.get_latest("invA", "battery_soc")
        b = self.repo.get_latest("invB", "battery_soc")
        self.assertEqual(a.normalized_value, 90.0)
        self.assertEqual(b.normalized_value, 80.0)

    def test_zero_is_saved_but_none_nan_inf_ignored(self):
        poller = self._poller()
        poller._persist(self._snapshot({
            "zero_ok": value("zero_ok", 0.0),
            "none_skip": TelemetryValue("none_skip", None, "W", TS, "corroborated", "valid", None, 1),
            "nan_skip": value("nan_skip", math.nan),
            "inf_skip": value("inf_skip", math.inf),
        }))
        start = datetime(2026, 1, 1, tzinfo=timezone.utc)
        end = datetime(2027, 1, 1, tzinfo=timezone.utc)
        self.assertEqual(len(self.repo.get_history("inv", "zero_ok", start, end)), 1)
        for name in ("none_skip", "nan_skip", "inf_skip"):
            self.assertEqual(len(self.repo.get_history("inv", name, start, end)), 0)

    def test_duplicate_measurement_not_written_twice(self):
        poller = self._poller()
        snap = self._snapshot({"battery_soc": value("battery_soc", 94.0, "%")})
        poller._persist(snap)
        poller._persist(snap)
        history = self.repo.get_history("inv", "battery_soc", datetime(2026, 1, 1, tzinfo=timezone.utc), datetime(2027, 1, 1, tzinfo=timezone.utc))
        self.assertEqual(len(history), 1)
        self.assertEqual(history[0].timestamp.isoformat(), TS)

    def test_source_quality_and_confidence_preserved(self):
        poller = self._poller()
        poller._persist(self._snapshot({
            "battery_voltage": value("battery_voltage", 53.3, "V", "model_compatible", "valid", 183),
        }))
        row = self.repo.get_latest("inv", "battery_voltage")
        self.assertEqual(row.source, DataSource.DEYE_MODBUS)
        self.assertEqual(row.quality, MeasurementQuality.VALID)
        self.assertEqual(row.confidence, ConfidenceLevel.PROVISIONAL)

    def test_sqlite_error_does_not_stop_poller(self):
        class BrokenRepo:
            def save_measurements_batch(self, rows):
                raise RuntimeError("sqlite locked")
        poller = self._poller(repo=BrokenRepo())
        snap = poller.read()
        self.assertIsNotNone(snap)
        self.assertIn("storage error", str(poller._stats.last_error))

    def test_history_api_all_periods_and_gaps(self):
        now = datetime.now(timezone.utc)
        self.repo.save_measurement(Measurement(
            inverter_id="inv", register_address=184, parameter_name="battery_soc",
            raw_value=94, normalized_value=94.0, unit="%", timestamp=now,
            quality=MeasurementQuality.VALID, confidence=ConfidenceLevel.CORROBORATED,
            source=DataSource.DEYE_MODBUS,
        ))
        for period in ("24h", "7d", "30d"):
            with self.subTest(period=period):
                series = build_series(self.repo, "inv", "battery_soc", period)
                self.assertIn("pv_total_power", KNOWN_PARAMETERS)
                self.assertGreater(series["available_points"], 0)
                self.assertGreater(series["missing_points"], 0)
                self.assertTrue(any(p["value"] is None and p["gap"] for p in series["points"]))

    def test_no_random_or_mock_in_production_modules(self):
        for name in ("deye_polling_service.py", "history_service.py", "sqlite_repository.py"):
            path = os.path.join(SERVER_DIR, "services" if name != "sqlite_repository.py" else "storage", name)
            text = open(path, encoding="utf-8").read()
            self.assertNotIn("random", text.lower())
            self.assertNotIn("uniform(", text)


if __name__ == "__main__":
    unittest.main()
