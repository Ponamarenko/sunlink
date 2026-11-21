"""
Stage 24 — offline тесты расширенной телеметрии (от регистра до API).

Физического подключения нет: mock FC03 transport + DEMO-источники + TestClient.
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
from domain.models import DataSource, Measurement, MeasurementQuality, ConfidenceLevel  # noqa: E402
from domain.register_decoder import decode_register  # noqa: E402
from domain.register_metadata import (  # noqa: E402
    ACTIVE_POLL_REGISTERS,
    CANDIDATE_REGISTERS,
    REGISTER_MAP,
)
from domain.telemetry import TelemetrySnapshot, TelemetryValue  # noqa: E402
from services.telemetry_sources import (  # noqa: E402
    DEMO_SCENARIOS,
    DemoTelemetrySource,
    demo_source_for,
)
from storage.sqlite_repository import SQLiteMeasurementRepository  # noqa: E402


class TestExpandedDecoder(unittest.TestCase):
    """Stage 23 physical raw values decode correctly, negatives kept."""

    def test_soc_physically_read(self):
        result = decode_register(0x005F, REGISTER_MAP[184])
        self.assertTrue(result.ok)
        self.assertEqual(result.normalized_value, 95.0)
        self.assertEqual(result.physical_status, "physically_confirmed")
        self.assertEqual(result.unit, "%")

    def test_pv_power_positive_and_signed_equivalence(self):
        for address, raw in ((186, 1519), (187, 796), (188, 123)):
            with self.subTest(address=address):
                result = decode_register(raw, REGISTER_MAP[address])
                self.assertEqual(result.normalized_value, float(raw))
                self.assertEqual(result.physical_status, "fc03_read")

    def test_battery_voltage(self):
        result = decode_register(0x14E9, REGISTER_MAP[183])
        self.assertEqual(result.normalized_value, 53.53)

    def test_negative_current_preserved(self):
        result = decode_register(0xFEA1, REGISTER_MAP[191])
        self.assertEqual(result.decoded_value, -351)
        self.assertAlmostEqual(result.normalized_value, -3.51)

    def test_battery_power_baseline_and_candidate(self):
        baseline = decode_register(0xF8C3, REGISTER_MAP[190])
        self.assertEqual(baseline.normalized_value, -1853.0)
        candidate = decode_register(0xF8C3, REGISTER_MAP[190], use_candidate=True)
        self.assertAlmostEqual(candidate.normalized_value, -185.3)
        self.assertTrue(candidate.used_candidate)
        self.assertEqual(candidate.confidence, "provisional")

    def test_temperature_candidates_preserve_raw(self):
        for address, raw, expected in ((90, 1410, 41.0), (91, 1482, 48.2)):
            with self.subTest(address=address):
                result = decode_register(raw, REGISTER_MAP[address], use_candidate=True)
                self.assertEqual(result.raw_value, raw)
                self.assertAlmostEqual(result.normalized_value, expected, places=1)
                self.assertEqual(result.confidence, "provisional")

    def test_missing_and_invalid_values_are_not_zero(self):
        missing = decode_register(None, REGISTER_MAP[183])
        self.assertIsNone(missing.normalized_value)
        self.assertFalse(missing.ok)
        invalid = decode_register(70000, REGISTER_MAP[183])
        self.assertIsNone(invalid.normalized_value)
        self.assertIsNotNone(invalid.error)

    def test_out_of_range_rejected(self):
        self.assertFalse(decode_register(200, REGISTER_MAP[184]).ok)
        self.assertTrue(decode_register(100, REGISTER_MAP[184]).ok)

    def test_candidate_profile_is_separate_from_active(self):
        self.assertEqual(set(ACTIVE_POLL_REGISTERS), {184})
        self.assertEqual(
            set(CANDIDATE_REGISTERS),
            {183, 186, 187, 188, 190, 191, 90, 91},
        )
        self.assertNotIn(190, ACTIVE_POLL_REGISTERS)


class TestTelemetrySnapshotAccessors(unittest.TestCase):
    def test_named_accessors_and_provisional_markers(self):
        values = {
            "pv1_power": TelemetryValue("pv1_power", 1519.0, "W", "2026-01-01T00:00:00+00:00", "corroborated", "valid"),
            "battery_power": TelemetryValue("battery_power", -185.3, "W", "2026-01-01T00:00:00+00:00", "provisional", "valid"),
            "dc_temperature": TelemetryValue("dc_temperature", 41.0, "°C", "2026-01-01T00:00:00+00:00", "provisional", "valid"),
        }
        snap = TelemetrySnapshot("inv", DataSource.DEMO_SIMULATOR,
                                 "2026-01-01T00:00:00+00:00", True, values)
        self.assertEqual(snap.pv1_power, 1519.0)
        self.assertEqual(snap.battery_power, -185.3)
        self.assertIsNone(snap.pv2_power)  # отсутствует — не 0
        self.assertEqual(snap.provisional_parameters, ["battery_power", "dc_temperature"])


class TestDemoScenarios(unittest.TestCase):
    def test_scenarios_present(self):
        self.assertEqual(set(DEMO_SCENARIOS), {"charging", "discharging", "no_pv", "partial"})

    def test_discharging_scenario_has_negative_values(self):
        snap = demo_source_for("discharging").read()
        self.assertEqual(snap.source, DataSource.DEMO_SIMULATOR)
        self.assertLess(snap.value_of("battery_current"), 0)
        self.assertLess(snap.value_of("battery_power"), 0)

    def test_partial_scenario_has_nulls_not_zeros(self):
        snap = demo_source_for("partial").read()
        self.assertIsNone(snap.value_of("pv1_power"))
        self.assertIsNone(snap.value_of("battery_current"))
        self.assertEqual(snap.value_of("battery_soc"), 70.0)

    def test_unknown_scenario_rejected(self):
        with self.assertRaises(ValueError):
            demo_source_for("does-not-exist")


class TestSqliteNewParameters(unittest.TestCase):
    def setUp(self):
        self.tmpdir = tempfile.mkdtemp()
        self.db = os.path.join(self.tmpdir, "new.db")
        self.repo = SQLiteMeasurementRepository(self.db)

    def tearDown(self):
        self.repo.close()

    def test_all_new_parameters_saved_and_read(self):
        now = datetime.now(timezone.utc)
        for name, unit, value in (
            ("pv1_power", "W", 1519.0), ("pv2_power", "W", 796.0),
            ("pv3_power", "W", 123.0), ("battery_voltage", "V", 53.53),
            ("battery_current", "A", -3.51), ("battery_power", "W", -185.3),
            ("dc_temperature", "°C", 41.0), ("ac_temperature", "°C", 48.2),
        ):
            self.repo.save_measurement(Measurement(
                inverter_id="inv", register_address=184, parameter_name=name,
                raw_value=0, normalized_value=value, unit=unit, timestamp=now,
                quality=MeasurementQuality.VALID,
                confidence=ConfidenceLevel.PROVISIONAL if "power" in name or "temperature" in name
                else ConfidenceLevel.CORROBORATED,
                source=DataSource.DEMO_SIMULATOR,
            ))
        latest = self.repo.get_all_latest("inv")
        self.assertEqual(len(latest), 8)
        self.assertEqual(
            {m.parameter_name for m in latest},
            {"pv1_power", "pv2_power", "pv3_power", "battery_voltage",
             "battery_current", "battery_power", "dc_temperature", "ac_temperature"},
        )

    def test_reopen_old_database_keeps_history(self):
        self.repo.save_measurement(Measurement(
            inverter_id="inv", register_address=184, parameter_name="battery_soc",
            raw_value=95, normalized_value=95.0, unit="%",
            timestamp=datetime.now(timezone.utc), quality=MeasurementQuality.VALID,
            confidence=ConfidenceLevel.CORROBORATED, source=DataSource.MODBUS_REGISTER,
        ))
        self.repo.close()
        reopened = SQLiteMeasurementRepository(self.db)
        try:
            self.assertEqual(reopened.schema_version(), 1)
            self.assertEqual(reopened.get_latest("inv", "battery_soc").normalized_value, 95.0)
        finally:
            reopened.close()


class TestApiExpansion(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(deye_gateway.app)
        self.old_gateway = deye_gateway.gateway
        self.old_repo = deye_gateway._repository

    def tearDown(self):
        self.client.close()
        deye_gateway.gateway = self.old_gateway
        deye_gateway._repository = self.old_repo

    def _use(self, scenario):
        repo = SQLiteMeasurementRepository(os.path.join(tempfile.mkdtemp(), "api.db"))
        deye_gateway.gateway = deye_gateway.DeyeGateway(
            deye_gateway.CONFIG, repo, demo_source_for(scenario)
        )
        deye_gateway._repository = repo
        return repo

    def test_live_raw_includes_new_fields_and_provisional_markers(self):
        self._use("charging")
        data = self.client.get("/api/live/raw").json()
        for key in ("pv1_power", "pv2_power", "pv3_power", "battery_voltage",
                    "battery_current", "battery_power", "dc_temperature", "ac_temperature"):
            self.assertIn(key, data)
        self.assertIn("battery_power", data["provisional_parameters"])
        self.assertIn("dc_temperature", data["provisional_parameters"])
        self.assertEqual(data["source"], DataSource.DEMO_SIMULATOR.value)

    def test_discharge_scenario_exposes_negative_values(self):
        self._use("discharging")
        data = self.client.get("/api/live/raw").json()
        self.assertLess(data["battery_current"], 0)
        self.assertLess(data["battery_power"], 0)

    def test_missing_values_are_null_not_zero(self):
        self._use("partial")
        data = self.client.get("/api/live/raw").json()
        self.assertIsNone(data["pv1_power"])
        self.assertIsNone(data["battery_current"])

    def test_no_write_endpoints(self):
        paths = [r.path for r in deye_gateway.app.routes if hasattr(r, "path")]
        for forbidden in ("/write", "/set-register", "/control"):
            self.assertNotIn(forbidden, paths)


if __name__ == "__main__":
    unittest.main()
