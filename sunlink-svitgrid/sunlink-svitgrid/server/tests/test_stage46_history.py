"""Stage 46 — реальная история, агрегация и пропуски."""

from __future__ import annotations

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
from services.history_service import KNOWN_PARAMETERS, PERIODS, build_series  # noqa: E402
from storage.sqlite_repository import SQLiteMeasurementRepository  # noqa: E402


def make_measurement(ts: datetime, value: float, parameter: str = "battery_soc") -> Measurement:
    return Measurement(
        inverter_id="inv", register_address=184, parameter_name=parameter,
        raw_value=int(value), normalized_value=value, unit="%", timestamp=ts,
        quality=MeasurementQuality.VALID,
        confidence=ConfidenceLevel.CORROBORATED,
        source=DataSource.DEYE_MODBUS,
    )


class TestHistoryStorage(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.NamedTemporaryFile(suffix=".db", delete=False)
        self.tmp.close()
        self.repo = SQLiteMeasurementRepository(self.tmp.name)

    def tearDown(self):
        self.repo.close()
        os.unlink(self.tmp.name)

    def test_real_measurements_survive_restart(self):
        now = datetime.now(timezone.utc)
        self.repo.save_measurement(make_measurement(now, 94.0))
        self.repo.close()
        self.repo = SQLiteMeasurementRepository(self.tmp.name)
        history = self.repo.get_history("inv", "battery_soc", now - timedelta(minutes=1), now + timedelta(minutes=1))
        self.assertEqual(len(history), 1)
        self.assertEqual(history[0].normalized_value, 94.0)

    def test_duplicates_and_pruning(self):
        now = datetime.now(timezone.utc)
        old = now - timedelta(days=40)
        self.repo.save_measurement(make_measurement(old, 80.0))
        self.repo.save_measurement(make_measurement(now, 94.0))
        self.repo.save_measurement(make_measurement(now, 94.0))
        removed = self.repo.prune_before("inv", now - timedelta(days=30))
        self.assertEqual(removed, 1)
        history = self.repo.get_history("inv", "battery_soc", old - timedelta(days=1), now + timedelta(days=1))
        self.assertEqual(len(history), 2)


class TestHistorySeries(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.NamedTemporaryFile(suffix=".db", delete=False)
        self.tmp.close()
        self.repo = SQLiteMeasurementRepository(self.tmp.name)

    def tearDown(self):
        self.repo.close()
        os.unlink(self.tmp.name)

    def test_all_periods_and_aggregation(self):
        now = datetime.now(timezone.utc)
        for i in range(4):
            self.repo.save_measurement(make_measurement(now - timedelta(seconds=i * 10), 90.0 + i))
        for period in PERIODS:
            with self.subTest(period=period):
                series = build_series(self.repo, "inv", "battery_soc", period)
                self.assertEqual(series["period"], period)
                self.assertEqual(len(series["points"]), series["count"])
                self.assertLessEqual(series["count"], 1000)
                self.assertGreater(series["available_points"], 0)

    def test_gaps_are_null_not_zero(self):
        now = datetime.now(timezone.utc)
        self.repo.save_measurement(make_measurement(now, 95.0))
        series = build_series(self.repo, "inv", "battery_soc", "24h")
        gaps = [p for p in series["points"] if p["gap"]]
        self.assertGreater(len(gaps), 0)
        self.assertTrue(all(p["value"] is None for p in gaps))

    def test_multiple_parameters(self):
        now = datetime.now(timezone.utc)
        self.repo.save_measurement(make_measurement(now, 94.0, "battery_soc"))
        self.repo.save_measurement(make_measurement(now, 53.3, "battery_voltage"))
        soc = build_series(self.repo, "inv", "battery_soc", "24h")
        voltage = build_series(self.repo, "inv", "battery_voltage", "24h")
        self.assertEqual(soc["parameter"], "battery_soc")
        self.assertEqual(voltage["unit"], "%")

    def test_invalid_inputs_rejected(self):
        for parameter, period in (("unknown_param", "24h"), ("battery_soc", "99d")):
            with self.subTest(parameter=parameter, period=period):
                with self.assertRaises(ValueError):
                    build_series(self.repo, "inv", parameter, period)

    def test_known_parameters_include_real_telemetry(self):
        self.assertIn("battery_soc", KNOWN_PARAMETERS)
        self.assertIn("pv1_power", KNOWN_PARAMETERS)
        self.assertIn("grid_frequency", KNOWN_PARAMETERS)


class TestApi(unittest.TestCase):
    def test_series_endpoint_and_validation(self):
        tmp = tempfile.NamedTemporaryFile(suffix=".db", delete=False)
        tmp.close()
        repo = SQLiteMeasurementRepository(tmp.name)
        now = datetime.now(timezone.utc)
        repo.save_measurement(make_measurement(now, 96.0))
        old_repo = deye_gateway._repository
        try:
            deye_gateway._repository = repo
            from fastapi.testclient import TestClient
            with TestClient(deye_gateway.app) as client:
                ok = client.get("/api/history/series", params={"parameter": "battery_soc", "period": "24h"})
                bad = client.get("/api/history/series", params={"parameter": "battery_soc", "period": "99d"})
            self.assertEqual(ok.status_code, 200)
            self.assertEqual(ok.json()["parameter"], "battery_soc")
            self.assertEqual(bad.status_code, 400)
        finally:
            deye_gateway._repository = old_repo
            repo.close()
            os.unlink(tmp.name)


if __name__ == "__main__":
    unittest.main()
