"""
Stage 22 — тесты декодированияregister-типов для будущего расширения телеметрии.

Никаких физических запросов. Тесты проверяют ПРАВИЛА декодирования,
а не включают новые регистры в активный polling map.
"""

from __future__ import annotations

import os
import sys
import unittest

SERVER_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, SERVER_DIR)

from domain.models import ConfidenceLevel  # noqa: E402
from domain.normalizer import (  # noqa: E402
    decode_s_word,
    decode_u_word,
    normalize_register,
)
from domain.register_metadata import (  # noqa: E402
    ACTIVE_POLL_REGISTERS,
    REGISTER_MAP,
)


def decode_u_dword(low: int, high: int, scale: float = 1.0) -> float:
    """LW,GW: value = low + (high << 16), затем scale."""
    return (low + (high << 16)) * scale


def decode_s_dword(low: int, high: int, scale: float = 1.0) -> float:
    raw = low + (high << 16)
    if not 0 <= raw <= 0xFFFFFFFF:
        raise ValueError("DWORD out of range")
    signed = raw - (1 << 32) if raw > 0x7FFFFFFF else raw
    return signed * scale


class TestWordDecoding(unittest.TestCase):
    def test_u_word(self):
        self.assertEqual(decode_u_word(0), 0)
        self.assertEqual(decode_u_word(5322), 5322)
        self.assertEqual(decode_u_word(0xFFFF), 65535)

    def test_s_word_negative_power(self):
        # -1000 W в two's complement
        self.assertEqual(decode_s_word(0xFC18), -1000)
        self.assertEqual(decode_s_word(0x0000), 0)
        self.assertEqual(decode_s_word(0x7FFF), 32767)
        self.assertEqual(decode_s_word(0x8000), -32768)

    def test_invalid_raw_rejected(self):
        for value in (-1, 70000):
            with self.subTest(value=value):
                with self.assertRaises(ValueError):
                    decode_u_word(value)
                with self.assertRaises(ValueError):
                    decode_s_word(value)

    def test_scaled_voltage_and_current(self):
        meta = REGISTER_MAP[183]   # battery_voltage, U_WORD, 0.01
        self.assertEqual(normalize_register(183, 5322, meta).normalized_value, 53.22)
        meta = REGISTER_MAP[191]   # battery_current, S_WORD, 0.01
        self.assertEqual(normalize_register(191, 361, meta).normalized_value, 3.61)
        self.assertEqual(normalize_register(191, 0xFC18, meta).normalized_value, -10.0)

    def test_temperature_offset_is_provisional(self):
        meta = REGISTER_MAP[90]
        self.assertEqual(normalize_register(90, 1365, meta).normalized_value, 36.5)
        self.assertEqual(meta.confidence, ConfidenceLevel.PROVISIONAL)
        self.assertIn("offset", meta.provisional_reason.lower())

    def test_soc_physically_read_value(self):
        meta = ACTIVE_POLL_REGISTERS[184]
        self.assertEqual(normalize_register(184, 0x005F, meta).normalized_value, 95.0)
        self.assertEqual(normalize_register(184, 0x0063, meta).normalized_value, 99.0)


class TestDwordDecoding(unittest.TestCase):
    """Правила для будущих energy-счётчиков (пока НЕ в активном map)."""

    def test_lw_hw_order(self):
        # Stage 3: total_pv_production raw 6433 -> 643.3 kWh (scale 0.1)
        self.assertAlmostEqual(decode_u_dword(6433, 0, 0.1), 643.3)

    def test_high_word_contribution(self):
        self.assertEqual(decode_u_dword(0, 1, 1.0), 65536)

    def test_signed_dword(self):
        self.assertEqual(decode_s_dword(0xFFFF, 0xFFFF, 1.0), -1)

    def test_dword_out_of_range(self):
        with self.assertRaises(ValueError):
            decode_s_dword(0, 0x10000)


class TestRegisterMapDiscipline(unittest.TestCase):
    def test_active_map_has_only_physically_read_register(self):
        self.assertEqual(set(ACTIVE_POLL_REGISTERS), {184})

    def test_unconfirmed_registers_stay_out_of_active_map(self):
        for address in (109, 110, 150, 151, 167, 169, 175, 178, 182, 192, 194):
            if address in REGISTER_MAP:
                self.assertNotIn(address, ACTIVE_POLL_REGISTERS)

    def test_missing_parameter_is_none_not_zero(self):
        # Неизвестный адрес не должен превращаться в нулевое измерение.
        self.assertNotIn(9999, REGISTER_MAP)
        self.assertIsNone(ACTIVE_POLL_REGISTERS.get(9999))

    def test_confidence_levels_not_upgraded(self):
        self.assertEqual(REGISTER_MAP[190].confidence, ConfidenceLevel.PROVISIONAL)
        self.assertEqual(REGISTER_MAP[90].confidence, ConfidenceLevel.PROVISIONAL)
        self.assertEqual(REGISTER_MAP[184].confidence, ConfidenceLevel.CORROBORATED)


if __name__ == "__main__":
    unittest.main()
