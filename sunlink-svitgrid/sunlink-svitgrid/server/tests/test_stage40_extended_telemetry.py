"""Stage 40 — расширенная карта, декодеры и безопасный план чтения.

Все тесты offline; production и физический Modbus не используются.
"""

from __future__ import annotations

import os
import sys
import unittest

SERVER_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, SERVER_DIR)

from domain.extended_decoder import decode_extended  # noqa: E402
from domain.extended_register_map import (  # noqa: E402
    EXTENDED_REGISTERS,
    build_read_plan,
    production_allowed_registers,
)
from domain.telemetry import TelemetrySnapshot  # noqa: E402
from domain.models import DataSource  # noqa: E402


class TestExtendedRegisterMap(unittest.TestCase):
    def test_comprehensive_coverage(self):
        self.assertGreaterEqual(len(EXTENDED_REGISTERS), 45)
        groups = {r.group for r in EXTENDED_REGISTERS.values()}
        self.assertTrue({"battery", "pv", "grid", "load", "inverter"}.issubset(groups))

    def test_only_184_allowed_in_production(self):
        allowed = production_allowed_registers()
        self.assertEqual(set(allowed), {184})
        self.assertTrue(allowed[184].physically_verified)
        self.assertFalse(allowed[184].is_signed)
        self.assertEqual(allowed[184].function_code, 3)

    def test_default_plan_is_single_register(self):
        plan = build_read_plan()
        self.assertEqual(len(plan), 1)
        self.assertEqual(plan[0]["addresses"], [184])
        self.assertEqual(plan[0]["priority"], "critical")

    def test_explicit_addresses_cannot_bypass_production_gate(self):
        plan = build_read_plan(enabled_addresses=[183, 186, 190])
        self.assertEqual(plan, [])

    def test_group_plan_and_chunking(self):
        plan = build_read_plan(enabled_addresses=list(EXTENDED_REGISTERS.keys()), max_block=8)
        self.assertTrue(plan)
        for entry in plan:
            self.assertLessEqual(entry["count"], 8)
            self.assertTrue(all(a in production_allowed_registers() for a in entry["addresses"]))

    def test_metadata_complete(self):
        for address, reg in EXTENDED_REGISTERS.items():
            with self.subTest(address=address):
                self.assertEqual(reg.address, address)
                self.assertIn(reg.data_type, ("U_WORD", "S_WORD", "U_DWORD", "S_DWORD"))
                self.assertIn(reg.function_code, (3, 4))
                self.assertTrue(reg.source)
                self.assertTrue(reg.model)


class TestExtendedDecoder(unittest.TestCase):
    def test_soc_decoding(self):
        result = decode_extended(184, [94])
        self.assertTrue(result.ok)
        self.assertEqual(result.normalized, 94.0)
        self.assertEqual(result.unit, "%")

    def test_signed_current(self):
        result = decode_extended(191, [0xFEA1])
        self.assertAlmostEqual(result.normalized, -3.51)
        self.assertEqual(result.raw, 0xFEA1)

    def test_voltage_scale(self):
        result = decode_extended(183, [5353])
        self.assertAlmostEqual(result.normalized, 53.53)

    def test_u_dword_word_order(self):
        result = decode_extended(96, [6433, 0])
        self.assertAlmostEqual(result.normalized, 643.3)

    def test_negative_dword(self):
        from domain.register_decoder import decode_s_dword
        self.assertEqual(decode_s_dword(0xFFFF, 0xFFFF, scale=0.1), -0.1)

    def test_candidate_scale_not_applied_by_default(self):
        baseline = decode_extended(190, [0xF8C3])
        candidate = decode_extended(190, [0xF8C3], use_candidate=True)
        self.assertEqual(baseline.normalized, -1853.0)
        self.assertAlmostEqual(candidate.normalized, -185.3)

    def test_invalid_input_is_never_zero(self):
        for words in ([], [1, 2], [70000]):
            with self.subTest(words=words):
                result = decode_extended(184, words)
                self.assertIsNone(result.normalized)
                self.assertIsNotNone(result.error)
                self.assertNotEqual(result.normalized, 0.0)

    def test_unknown_register(self):
        result = decode_extended(9999, [1])
        self.assertIsNone(result.normalized)
        self.assertEqual(result.error, "unknown register")


class TestFullModelCoverage(unittest.TestCase):
    def test_all_categories_have_extended_fields(self):
        snapshot = TelemetrySnapshot(
            "deye_sg02lp1_am3", DataSource.DEYE_MODBUS,
            "2026-11-22T10:00:00Z", True, {}
        )
        full = snapshot.to_full_dict()
        expected = {
            "pv": {"pv1_voltage", "pv3_power", "total_energy", "status"},
            "battery": {"voltage", "status", "total_charge_energy", "total_discharge_energy"},
            "grid": {"current_l1", "frequency", "import_power", "connected"},
            "load": {"frequency", "power_factor", "total_energy"},
            "inverter": {"operating_state", "fault_code", "efficiency"},
            "energy": {"daily_grid_import", "total_grid_export"},
        }
        for category, keys in expected.items():
            with self.subTest(category=category):
                self.assertTrue(keys.issubset(full["categories"][category].keys()))
                self.assertTrue(all(v is None for v in full["categories"][category].values()))


if __name__ == "__main__":
    unittest.main()
