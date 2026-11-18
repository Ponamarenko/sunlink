"""
Этап 12 — контрактные тесты Backend ↔ Android.

Проверяют, что реальные JSON-ответы backend соответствуют ожиданиям
Android DTO (LiveRawDto, MeasurementDto, StatusDto и т.д.).

Нет физических подключений: используется DEMO-окружение demo_backend.py
и in-process HTTP (fastapi TestClient).
"""

from __future__ import annotations

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
from deye_gateway import app, INVERTER_ID, _flat_live  # noqa: E402
import demo_backend  # noqa: E402
from domain.models import DataSource  # noqa: E402
from domain.register_metadata import REGISTER_MAP  # noqa: E402
from storage.sqlite_repository import SQLiteMeasurementRepository  # noqa: E402

# Поля, которые Android LiveRawDto ожидает в /api/live/raw.
ANDROID_LIVE_RAW_FIELDS = {
    "p_sun", "p_load", "p_batt", "p_grid",
    "batt_soc", "battery_voltage", "battery_current",
    "dc_temperature", "ac_temperature",
    "pv1_power", "pv2_power", "pv3_power",
    "timestamp", "connection_status", "error",
    "inverter_id", "source",
}

# Поля, которые Android MeasurementDto ожидает в /api/measurements/*.
ANDROID_MEASUREMENT_FIELDS = {
    "inverter_id", "register_address", "parameter_name", "raw_value",
    "normalized_value", "unit", "timestamp", "quality", "confidence",
    "source", "description",
}


class Stage12DemoFixture(unittest.TestCase):
    """Изолированное DEMO-окружение для контрактных тестов."""

    def setUp(self) -> None:
        self.tmpdir = tempfile.mkdtemp()
        self.db_path = os.path.join(self.tmpdir, "contract_test.db")
        self.repo = demo_backend.configure(self.db_path)
        demo_backend.seed_demo_history(self.repo)
        self.client = TestClient(app)

    def tearDown(self) -> None:
        self.client.close()
        self.repo.close()

    @staticmethod
    def _iso_z(dt: datetime) -> str:
        """ISO-8601 UTC с Z-суффиксом (плюс в +00:00 ломается в query string)."""
        return dt.isoformat().replace("+00:00", "Z")

    def _history_url(self, parameter: str = "battery_soc") -> str:
        start = self._iso_z(datetime.now(timezone.utc) - timedelta(hours=2))
        end = self._iso_z(datetime.now(timezone.utc) + timedelta(hours=1))
        return (
            f"/api/measurements/history?parameter={parameter}"
            f"&start={start}&end={end}"
        )


class TestBackendEndpointContracts(Stage12DemoFixture):
    """HTTP-статусы и базовая структура всех используемых endpoint."""

    def test_health(self):
        r = self.client.get("/")
        self.assertEqual(r.status_code, 200)
        self.assertTrue(r.json()["read_only"])

    def test_status_contract(self):
        r = self.client.get("/status")
        self.assertEqual(r.status_code, 200)
        d = r.json()
        for key in ("connection_status", "last_update", "inverter_id",
                    "available_registers", "registers", "source"):
            self.assertIn(key, d, f"/status missing {key}")
        self.assertEqual(d["source"], DataSource.DEMO_SIMULATOR.value)
        self.assertEqual(d["available_registers"], 9)

    def test_inverters_contract(self):
        r = self.client.get("/api/inverters")
        self.assertEqual(r.status_code, 200)
        d = r.json()
        self.assertEqual(d["count"], 1)
        self.assertEqual(d["inverters"][0]["inverter_id"], INVERTER_ID)
        self.assertEqual(d["source"], DataSource.DEMO_SIMULATOR.value)

    def test_live_raw_contract(self):
        r = self.client.get("/api/live/raw")
        self.assertEqual(r.status_code, 200)
        d = r.json()
        for key in ANDROID_LIVE_RAW_FIELDS - {"error"}:
            self.assertIn(key, d, f"/api/live/raw missing {key}")
        self.assertEqual(d["source"], DataSource.DEMO_SIMULATOR.value)
        self.assertEqual(d["inverter_id"], INVERTER_ID)
        self.assertTrue(d["connection_status"])

    def test_live_contract(self):
        r = self.client.get("/live")
        self.assertEqual(r.status_code, 200)
        d = r.json()
        self.assertIn("data", d)
        self.assertIn("status", d)
        self.assertIn("source", d)
        self.assertEqual(d["source"], DataSource.DEMO_SIMULATOR.value)

    def test_registers_contract(self):
        r = self.client.get("/registers")
        self.assertEqual(r.status_code, 200)
        d = r.json()
        self.assertEqual(d["count"], 9)
        self.assertEqual(len(d["registers"]), 9)
        for reg in d["registers"]:
            for key in ("address", "name", "data_type", "scale", "unit",
                        "confidence", "description"):
                self.assertIn(key, reg)

    def test_latest_measurements_contract(self):
        r = self.client.get("/api/measurements/latest")
        self.assertEqual(r.status_code, 200)
        d = r.json()
        self.assertEqual(d["count"], 9)
        self.assertEqual(d["inverter_id"], INVERTER_ID)
        for m in d["measurements"]:
            self.assertEqual(set(m.keys()), ANDROID_MEASUREMENT_FIELDS)
            self.assertEqual(m["source"], DataSource.DEMO_SIMULATOR.value)
            self.assertIn(m["parameter_name"], {reg.name for reg in REGISTER_MAP.values()})

    def test_latest_measurement_by_parameter_contract(self):
        r = self.client.get("/api/measurements/latest/battery_soc")
        self.assertEqual(r.status_code, 200)
        d = r.json()
        self.assertEqual(set(d.keys()), ANDROID_MEASUREMENT_FIELDS)
        self.assertEqual(d["parameter_name"], "battery_soc")
        self.assertEqual(d["unit"], "%")
        self.assertEqual(d["source"], DataSource.DEMO_SIMULATOR.value)

    def test_latest_measurement_bad_parameter_404(self):
        r = self.client.get("/api/measurements/latest/does_not_exist")
        self.assertEqual(r.status_code, 404)
        self.assertIn("detail", r.json())

    def test_history_contract(self):
        r = self.client.get(self._history_url("battery_soc"))
        self.assertEqual(r.status_code, 200)
        d = r.json()
        for key in ("inverter_id", "parameter", "start", "end", "count", "measurements"):
            self.assertIn(key, d)
        self.assertGreater(d["count"], 0)
        self.assertEqual(d["parameter"], "battery_soc")
        for m in d["measurements"]:
            self.assertEqual(set(m.keys()), ANDROID_MEASUREMENT_FIELDS)
            # Все точки отсортированы по времени (Android строит график по порядку)
            ts = [m["timestamp"] for m in d["measurements"]]
        self.assertEqual(ts, sorted(ts))

    def test_empty_history_returns_count_zero(self):
        r = self.client.get(self._history_url("does_not_exist"))
        self.assertEqual(r.status_code, 200)
        d = r.json()
        self.assertEqual(d["count"], 0)
        self.assertEqual(d["measurements"], [])

    def test_invalid_history_dates_400(self):
        r = self.client.get(
            "/api/measurements/history?parameter=battery_soc"
            "&start=not-a-date&end=also-bad"
        )
        self.assertEqual(r.status_code, 400)

    def test_history_all_android_parameters(self):
        """Все параметры History экрана Android должны отдавать данные."""
        for param in ("battery_soc", "battery_power", "dc_temperature",
                      "pv1_power", "pv2_power", "pv3_power",
                      "battery_voltage", "battery_current"):
            r = self.client.get(self._history_url(param))
            self.assertEqual(r.status_code, 200, param)
            self.assertGreater(r.json()["count"], 0, f"empty history for {param}")

    def test_history_persists_after_repo_reopen(self):
        """История доступна после перезапуска (novoe соединение с той же БД)."""
        self.repo.close()
        reopened = SQLiteMeasurementRepository(self.db_path)
        try:
            measurements = reopened.get_history(
                INVERTER_ID,
                "battery_soc",
                datetime.now(timezone.utc) - timedelta(hours=2),
                datetime.now(timezone.utc) + timedelta(hours=1),
            )
            self.assertGreater(len(measurements), 0)
        finally:
            reopened.close()
            # Восстанавливаем репозиторий для tearDown
            self.repo = SQLiteMeasurementRepository(self.db_path)
            deye_gateway._repository = self.repo
            deye_gateway.gateway.repository = self.repo


class TestFlatLiveContract(unittest.TestCase):
    """Отсутствующие данные не подменяются нулями."""

    def test_missing_fields_are_null(self):
        flat = _flat_live({}, {"connection_status": False})
        for key in ("p_sun", "p_load", "p_batt", "p_grid", "batt_soc",
                    "battery_voltage", "battery_current"):
            self.assertIsNone(flat[key], key)

    def test_pv_sum_and_provenance(self):
        data = {
            "pv1_power": {"normalized_value": 100.0},
            "pv2_power": {"normalized_value": 200.0},
        }
        flat = _flat_live(data, {
            "connection_status": True,
            "source": DataSource.DEMO_SIMULATOR.value,
        })
        self.assertEqual(flat["p_sun"], 300.0)
        self.assertEqual(flat["source"], DataSource.DEMO_SIMULATOR.value)
        self.assertIsNone(flat["p_grid"])


if __name__ == "__main__":
    unittest.main()
