"""
Tests for SunPortal Data Layer normalizer (pure functions).
No Modbus requests are made.
"""

from __future__ import annotations

import sys
import unittest

sys.path.insert(0, __import__("os").path.dirname(__import__("os").path.abspath(__file__)) + "/..")

from domain.normalizer import (  # noqa: E402
    decode_u_word,
    decode_s_word,
    apply_scale,
    apply_temperature_offset,
    normalize_register,
    NormalizationResult,
)
from domain.register_metadata import REGISTER_MAP
from domain.models import ConfidenceLevel, MeasurementQuality


class TestDecodeUWord(unittest.TestCase):
    def test_bounds(self):
        self.assertEqual(decode_u_word(0), 0)
        self.assertEqual(decode_u_word(65535), 65535)
        self.assertEqual(decode_u_word(32768), 32768)

    def test_out_of_range(self):
        with self.assertRaises(ValueError):
            decode_u_word(-1)
        with self.assertRaises(ValueError):
            decode_u_word(65536)


class TestDecodeSWord(unittest.TestCase):
    def test_positive(self):
        self.assertEqual(decode_s_word(0), 0)
        self.assertEqual(decode_s_word(100), 100)
        self.assertEqual(decode_s_word(32767), 32767)

    def test_negative(self):
        # -1 as S_WORD -> 65535
        self.assertEqual(decode_s_word(65535), -1)
        # -500 W -> 65036
        self.assertEqual(decode_s_word(65036), -500)
        # -32768 -> 32768
        self.assertEqual(decode_s_word(32768), -32768)

    def test_out_of_range(self):
        with self.assertRaises(ValueError):
            decode_s_word(-1)
        with self.assertRaises(ValueError):
            decode_s_word(65536)


class TestApplyScale(unittest.TestCase):
    def test_scale(self):
        self.assertEqual(apply_scale(5322, 0.01), 53.22)
        self.assertEqual(apply_scale(85, 1.0), 85.0)
        self.assertEqual(apply_scale(361, 0.01), 3.61)


class TestApplyTemperatureOffset(unittest.TestCase):
    def test_offset(self):
        self.assertEqual(apply_temperature_offset(136.5, 100.0), 36.5)
        self.assertEqual(apply_temperature_offset(50.0, 100.0), -50.0)
        self.assertEqual(apply_temperature_offset(100.0, 100.0), 0.0)


class TestNormalizeRegister(unittest.TestCase):
    def test_battery_voltage(self):
        r = normalize_register(183, 5322, REGISTER_MAP[183])
        self.assertTrue(r.ok)
        self.assertEqual(r.raw_value, 5322)
        self.assertEqual(r.decoded_value, 5322)
        self.assertEqual(r.normalized_value, 53.22)
        self.assertEqual(r.unit, "V")
        self.assertEqual(r.confidence, ConfidenceLevel.CORROBORATED)
        self.assertEqual(r.quality, MeasurementQuality.VALID)
        self.assertEqual(r.formula, "raw × 0.01")

    def test_battery_soc(self):
        r = normalize_register(184, 85, REGISTER_MAP[184])
        self.assertTrue(r.ok)
        self.assertEqual(r.normalized_value, 85.0)
        self.assertEqual(r.unit, "%")

    def test_pv_power_signed(self):
        r = normalize_register(186, 93, REGISTER_MAP[186])
        self.assertTrue(r.ok)
        self.assertEqual(r.normalized_value, 93.0)
        self.assertEqual(r.confidence, ConfidenceLevel.CORROBORATED)

    def test_battery_current_signed(self):
        r = normalize_register(191, 361, REGISTER_MAP[191])
        self.assertTrue(r.ok)
        self.assertEqual(r.normalized_value, 3.61)
        self.assertEqual(r.unit, "A")

    def test_battery_power_provisional(self):
        r = normalize_register(190, 434, REGISTER_MAP[190])
        self.assertTrue(r.ok)
        self.assertEqual(r.normalized_value, 434.0)
        self.assertEqual(r.confidence, ConfidenceLevel.PROVISIONAL)

    def test_temperature_with_offset(self):
        r = normalize_register(90, 1365, REGISTER_MAP[90])
        self.assertTrue(r.ok)
        self.assertEqual(r.normalized_value, 36.5)
        self.assertEqual(r.confidence, ConfidenceLevel.PROVISIONAL)
        self.assertIn("- 100", r.formula)

    def test_negative_sword_value(self):
        # reg 190 = -500 W as S_WORD -> raw 65036
        r = normalize_register(190, 65036, REGISTER_MAP[190])
        self.assertTrue(r.ok)
        self.assertEqual(r.decoded_value, -500)
        self.assertEqual(r.normalized_value, -500.0)

    def test_error_on_bad_raw(self):
        r = normalize_register(183, -1, REGISTER_MAP[183])
        self.assertFalse(r.ok)
        self.assertIsNotNone(r.error)
        self.assertEqual(r.quality, MeasurementQuality.ERROR)
        self.assertIsNone(r.normalized_value)


if __name__ == "__main__":
    unittest.main()
