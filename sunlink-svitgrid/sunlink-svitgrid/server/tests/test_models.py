"""
Tests for SunPortal Data Layer measurement models.
"""

from __future__ import annotations

import sys
import unittest
from datetime import datetime, timezone

sys.path.insert(0, __import__("os").path.dirname(__import__("os").path.abspath(__file__)) + "/..")

from domain.models import (  # noqa: E402
    Measurement,
    MeasurementQuality,
    ConfidenceLevel,
    DataSource,
    NormalizedReading,
)


class TestEnums(unittest.TestCase):
    def test_quality_values(self):
        self.assertEqual(MeasurementQuality.VALID.value, "valid")
        self.assertEqual(MeasurementQuality.ERROR.value, "error")

    def test_confidence_values(self):
        self.assertEqual(ConfidenceLevel.VERIFIED.value, "verified")
        self.assertEqual(ConfidenceLevel.CORROBORATED.value, "corroborated")
        self.assertEqual(ConfidenceLevel.PROVISIONAL.value, "provisional")
        self.assertEqual(ConfidenceLevel.UNVERIFIED.value, "unverified")

    def test_source_values(self):
        self.assertEqual(DataSource.MODBUS_REGISTER.value, "modbus_register")
        self.assertEqual(DataSource.CALCULATED.value, "calculated")


class TestMeasurement(unittest.TestCase):
    def test_creation_and_fields(self):
        ts = datetime(2026, 11, 17, 13, 32, 27, tzinfo=timezone.utc)
        m = Measurement(
            inverter_id="2507245705",
            register_address=183,
            parameter_name="battery_voltage",
            raw_value=5322,
            normalized_value=53.22,
            unit="V",
            timestamp=ts,
            quality=MeasurementQuality.VALID,
            confidence=ConfidenceLevel.CORROBORATED,
            source=DataSource.MODBUS_REGISTER,
            description="Battery voltage",
        )
        self.assertEqual(m.inverter_id, "2507245705")
        self.assertEqual(m.register_address, 183)
        self.assertEqual(m.raw_value, 5322)
        self.assertEqual(m.normalized_value, 53.22)
        self.assertEqual(m.unit, "V")
        self.assertEqual(m.quality, MeasurementQuality.VALID)
        self.assertEqual(m.confidence, ConfidenceLevel.CORROBORATED)
        self.assertEqual(m.source, DataSource.MODBUS_REGISTER)

    def test_naive_timestamp_becomes_utc(self):
        ts_naive = datetime(2026, 11, 17, 13, 32, 27)
        m = Measurement(
            inverter_id="inv",
            register_address=None,
            parameter_name="test",
            raw_value=None,
            normalized_value=None,
            unit="",
            timestamp=ts_naive,
            quality=MeasurementQuality.UNAVAILABLE,
            confidence=ConfidenceLevel.UNVERIFIED,
            source=DataSource.MANUAL,
        )
        self.assertIsNotNone(m.timestamp.tzinfo)
        self.assertEqual(m.timestamp.tzinfo, timezone.utc)

    def test_to_dict_roundtrip(self):
        ts = datetime(2026, 11, 17, 13, 32, 27, tzinfo=timezone.utc)
        m = Measurement(
            inverter_id="2507245705",
            register_address=184,
            parameter_name="battery_soc",
            raw_value=85,
            normalized_value=85.0,
            unit="%",
            timestamp=ts,
            quality=MeasurementQuality.VALID,
            confidence=ConfidenceLevel.CORROBORATED,
            source=DataSource.MODBUS_REGISTER,
        )
        d = m.to_dict()
        m2 = Measurement.from_dict(d)
        self.assertEqual(m2, m)
        self.assertEqual(m2.timestamp.tzinfo, timezone.utc)

    def test_frozen(self):
        m = Measurement(
            inverter_id="inv",
            register_address=1,
            parameter_name="p",
            raw_value=1,
            normalized_value=1.0,
            unit="W",
            timestamp=datetime.now(timezone.utc),
            quality=MeasurementQuality.VALID,
            confidence=ConfidenceLevel.VERIFIED,
            source=DataSource.MODBUS_REGISTER,
        )
        with self.assertRaises(Exception):
            m.raw_value = 999


class TestNormalizedReading(unittest.TestCase):
    def test_creation(self):
        r = NormalizedReading(
            register_address=183,
            parameter_name="battery_voltage",
            raw_value=5322,
            normalized_value=53.22,
            unit="V",
            formula="raw × 0.01",
            confidence=ConfidenceLevel.CORROBORATED,
        )
        self.assertEqual(r.raw_value, 5322)
        self.assertEqual(r.normalized_value, 53.22)
        self.assertEqual(r.confidence, ConfidenceLevel.CORROBORATED)


if __name__ == "__main__":
    unittest.main()
