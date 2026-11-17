"""
Tests for SQLiteMeasurementRepository.
Uses a temporary SQLite database; no production file is touched.
"""

from __future__ import annotations

import os
import sys
import tempfile
import unittest
from datetime import datetime, timezone, timedelta

sys.path.insert(0, __import__("os").path.dirname(__import__("os").path.abspath(__file__)) + "/..")

from domain.models import (  # noqa: E402
    Measurement,
    MeasurementQuality,
    ConfidenceLevel,
    DataSource,
)
from storage.sqlite_repository import (  # noqa: E402
    SQLiteMeasurementRepository,
    SCHEMA_VERSION,
)


def _make_measurement(*, inverter_id="2507245705", register_address=183,
                      parameter_name="battery_voltage", raw_value=5322,
                      normalized_value=53.22, unit="V",
                      quality=MeasurementQuality.VALID,
                      confidence=ConfidenceLevel.CORROBORATED,
                      source=DataSource.MODBUS_REGISTER,
                      offset_seconds=0) -> Measurement:
    ts = datetime(2026, 11, 17, 13, 32, 27, tzinfo=timezone.utc) + timedelta(seconds=offset_seconds)
    return Measurement(
        inverter_id=inverter_id,
        register_address=register_address,
        parameter_name=parameter_name,
        raw_value=raw_value,
        normalized_value=normalized_value,
        unit=unit,
        timestamp=ts,
        quality=quality,
        confidence=confidence,
        source=source,
        description="test",
    )


class TestSchemaVersion(unittest.TestCase):
    def test_schema_version_is_one(self):
        with tempfile.NamedTemporaryFile(suffix=".db", delete=False) as f:
            path = f.name
        try:
            repo = SQLiteMeasurementRepository(path)
            self.assertEqual(repo.schema_version(), SCHEMA_VERSION)
            repo.close()
        finally:
            os.unlink(path)


class TestInsertAndRead(unittest.TestCase):
    def setUp(self):
        self.fd, self.path = tempfile.mkstemp(suffix=".db")
        os.close(self.fd)
        self.repo = SQLiteMeasurementRepository(self.path)

    def tearDown(self):
        self.repo.close()
        os.unlink(self.path)

    def test_save_and_get_latest(self):
        m = _make_measurement()
        self.repo.save_measurement(m)
        latest = self.repo.get_latest("2507245705", "battery_voltage")
        self.assertIsNotNone(latest)
        self.assertEqual(latest.raw_value, 5322)
        self.assertEqual(latest.normalized_value, 53.22)
        self.assertEqual(latest.unit, "V")

    def test_get_latest_missing(self):
        self.assertIsNone(
            self.repo.get_latest("2507245705", "nonexistent")
        )

    def test_batch_insert(self):
        ms = [
            _make_measurement(parameter_name="pv1_power", raw_value=100, normalized_value=100.0, offset_seconds=0),
            _make_measurement(parameter_name="pv2_power", raw_value=50, normalized_value=50.0, offset_seconds=1),
            _make_measurement(parameter_name="pv3_power", raw_value=30, normalized_value=30.0, offset_seconds=2),
        ]
        count = self.repo.save_measurements_batch(ms)
        self.assertEqual(count, 3)
        latest = self.repo.get_latest("2507245705", "pv1_power")
        self.assertIsNotNone(latest)
        self.assertEqual(latest.normalized_value, 100.0)

    def test_get_all_latest(self):
        self.repo.save_measurements_batch([
            _make_measurement(parameter_name="pv1_power", raw_value=100, normalized_value=100.0),
            _make_measurement(parameter_name="battery_soc", raw_value=85, normalized_value=85.0, register_address=184, unit="%"),
        ])
        all_latest = self.repo.get_all_latest("2507245705")
        names = sorted(m.parameter_name for m in all_latest)
        self.assertEqual(names, ["battery_soc", "pv1_power"])

    def test_history_time_window(self):
        base = datetime(2026, 11, 17, 13, 32, 27, tzinfo=timezone.utc)
        self.repo.save_measurements_batch([
            _make_measurement(parameter_name="pv1_power", raw_value=100, normalized_value=100.0,
                              offset_seconds=0),
            _make_measurement(parameter_name="pv1_power", raw_value=200, normalized_value=200.0,
                              offset_seconds=60),
            _make_measurement(parameter_name="pv1_power", raw_value=300, normalized_value=300.0,
                              offset_seconds=120),
        ])
        start = base + timedelta(seconds=30)
        end = base + timedelta(seconds=90)
        hist = self.repo.get_history("2507245705", "pv1_power", start, end)
        self.assertEqual(len(hist), 1)
        self.assertEqual(hist[0].normalized_value, 200.0)

    def test_context_manager(self):
        with tempfile.NamedTemporaryFile(suffix=".db", delete=False) as f:
            path = f.name
        try:
            with SQLiteMeasurementRepository(path) as repo:
                repo.save_measurement(_make_measurement())
                self.assertIsNotNone(repo.get_latest("2507245705", "battery_voltage"))
            # After exit, connection should be closed
            with self.assertRaises(RuntimeError):
                repo.get_latest("2507245705", "battery_voltage")
        finally:
            os.unlink(path)

    def test_close_idempotent(self):
        self.repo.close()
        self.repo.close()  # should not raise


class TestBatchTransaction(unittest.TestCase):
    def setUp(self):
        self.fd, self.path = tempfile.mkstemp(suffix=".db")
        os.close(self.fd)
        self.repo = SQLiteMeasurementRepository(self.path)

    def tearDown(self):
        self.repo.close()
        os.unlink(self.path)

    def test_empty_batch(self):
        count = self.repo.save_measurements_batch([])
        self.assertEqual(count, 0)

    def test_reopen_database(self):
        m = _make_measurement()
        self.repo.save_measurement(m)
        self.repo.close()
        # Reopen and read
        repo2 = SQLiteMeasurementRepository(self.path)
        latest = repo2.get_latest("2507245705", "battery_voltage")
        self.assertIsNotNone(latest)
        self.assertEqual(latest.normalized_value, 53.22)
        repo2.close()


if __name__ == "__main__":
    unittest.main()
