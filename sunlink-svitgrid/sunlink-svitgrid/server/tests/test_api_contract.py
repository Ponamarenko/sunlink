"""
API contract tests for the Data Layer.

Verifies that Measurement and NormalizedReading expose a stable serializable
shape suitable for a future API, and that register metadata matches the
stage 7 register set.
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
from domain.register_metadata import REGISTER_MAP
from domain.normalizer import normalize_register


class TestMeasurementDictShape(unittest.TestCase):
    """Measurement must expose a stable dict shape (for future API JSON)."""

    def test_dict_contains_all_required_fields(self):
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
        )
        d = m.to_dict()
        required = {
            "inverter_id", "register_address", "parameter_name", "raw_value",
            "normalized_value", "unit", "timestamp", "quality", "confidence",
            "source", "description",
        }
        self.assertEqual(set(d.keys()), required)
        self.assertEqual(d["quality"], "valid")
        self.assertEqual(d["confidence"], "corroborated")
        self.assertEqual(d["source"], "modbus_register")

    def test_from_dict_roundtrip_preserves_all_fields(self):
        ts = datetime(2026, 11, 17, 13, 32, 27, tzinfo=timezone.utc)
        m = Measurement(
            inverter_id="inv",
            register_address=190,
            parameter_name="battery_power",
            raw_value=434,
            normalized_value=434.0,
            unit="W",
            timestamp=ts,
            quality=MeasurementQuality.VALID,
            confidence=ConfidenceLevel.PROVISIONAL,
            source=DataSource.MODBUS_REGISTER,
            description="provisional sign semantics",
        )
        m2 = Measurement.from_dict(m.to_dict())
        self.assertEqual(m2, m)


class TestRegisterMetadataStage7(unittest.TestCase):
    """Register map must match the stage 7 initial set exactly."""

    def test_expected_registers_present(self):
        expected = {90, 91, 183, 184, 186, 187, 188, 190, 191}
        self.assertEqual(set(REGISTER_MAP.keys()), expected)

    def test_no_excluded_registers(self):
        excluded = {166, 167, 169, 172, 175, 176, 177, 178, 182, 192, 194}
        for addr in excluded:
            self.assertNotIn(addr, REGISTER_MAP)

    def test_confidence_levels(self):
        self.assertEqual(REGISTER_MAP[183].confidence, ConfidenceLevel.CORROBORATED)
        self.assertEqual(REGISTER_MAP[184].confidence, ConfidenceLevel.CORROBORATED)
        self.assertEqual(REGISTER_MAP[190].confidence, ConfidenceLevel.PROVISIONAL)
        self.assertEqual(REGISTER_MAP[90].confidence, ConfidenceLevel.PROVISIONAL)
        self.assertEqual(REGISTER_MAP[91].confidence, ConfidenceLevel.PROVISIONAL)

    def test_temperature_offset_documented(self):
        for addr in (90, 91):
            meta = REGISTER_MAP[addr]
            self.assertEqual(meta.offset, 100.0)
            self.assertTrue(meta.provisional_reason)

    def test_battery_power_provisional_reason(self):
        self.assertTrue(REGISTER_MAP[190].provisional_reason)
        self.assertIn("sign", REGISTER_MAP[190].provisional_reason.lower())


class TestNormalizerContract(unittest.TestCase):
    """Normalizer output must be predictable for API consumers."""

    def test_stage4_battery_voltage(self):
        r = normalize_register(183, 5322, REGISTER_MAP[183])
        self.assertTrue(r.ok)
        self.assertAlmostEqual(r.normalized_value, 53.22)

    def test_stage4_temperature_with_offset(self):
        r = normalize_register(90, 1365, REGISTER_MAP[90])
        self.assertTrue(r.ok)
        self.assertAlmostEqual(r.normalized_value, 36.5)

    def test_provisional_fields_preserve_raw(self):
        r = normalize_register(190, 434, REGISTER_MAP[190])
        self.assertTrue(r.ok)
        self.assertEqual(r.raw_value, 434)
        self.assertEqual(r.normalized_value, 434.0)
        self.assertEqual(r.confidence, ConfidenceLevel.PROVISIONAL)


class TestNormalizedReadingShape(unittest.TestCase):
    def test_fields(self):
        r = NormalizedReading(
            register_address=184,
            parameter_name="battery_soc",
            raw_value=85,
            normalized_value=85.0,
            unit="%",
            formula="raw × 1",
            confidence=ConfidenceLevel.CORROBORATED,
        )
        self.assertEqual(r.register_address, 184)
        self.assertEqual(r.parameter_name, "battery_soc")


if __name__ == "__main__":
    unittest.main()
