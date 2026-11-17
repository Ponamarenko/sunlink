"""
Mock-based tests for DeyeReadonlyAdapter (read-only).

NO real Modbus connection is ever made: pymodbus ModbusTcpClient is mocked.
The tests assert:
  * U_WORD / S_WORD decoding and scaling,
  * negative S_WORD values,
  * DWORD (LW,HW) assembly,
  * PV1/PV2/PV3 presence and calculated p_sun,
  * Grid Power ×10 and CT L1,
  * Battery SOC,
  * timeout / unavailable-device handling,
  * NO Modbus Write functions exist and are never called.
"""

from __future__ import annotations

import sys
import unittest
from datetime import datetime, timezone
from unittest.mock import MagicMock, patch

# Make `server` importable when run as `python -m tests.test_...` or directly.
sys.path.insert(0, __import__("os").path.dirname(__import__("os").path.abspath(__file__)) + "/..")

from deye_readonly_adapter import (  # noqa: E402
    DeyeReadonlyAdapter,
    RegisterValue,
    SOURCE_CALCULATED,
    SOURCE_MEASURED,
    SOURCE_UNCONFIRMED,
)


class _MockResult:
    """Stand-in for a pymodbus read_holding_registers response."""

    def __init__(self, registers, is_error=False):
        self.registers = list(registers)
        self._is_error = is_error

    def isError(self):
        return self._is_error


def _build_client(read_map, connect_ok=True, read_side_effect=None):
    """Build a mocked ModbusTcpClient.

    read_map: dict address -> raw int value returned for FC 03 count=1.
    """
    client = MagicMock()
    client.connect.return_value = connect_ok

    def _read_holding_registers(address, count=1, device_id=None, **kwargs):
        if read_side_effect is not None:
            raise read_side_effect(address, count, device_id, **kwargs)
        if address not in read_map:
            return _MockResult([], is_error=True)
        if count == 1:
            return _MockResult([read_map[address]], is_error=False)
        # count == 2 (DWORD): return two consecutive addresses
        return _MockResult(
            [read_map.get(address, 0), read_map.get(address + 1, 0)],
            is_error=False,
        )

    client.read_holding_registers.side_effect = _read_holding_registers
    return client


class TestAdapterBasics(unittest.TestCase):
    def setUp(self):
        self.adapter = DeyeReadonlyAdapter(
            host="127.0.0.1", port=502, slave_id=1, timeout=2.0
        )

    def test_no_write_methods_exist(self):
        """The adapter class must not expose any Modbus write method."""
        for forbidden in (
            "write_register",
            "write_registers",
            "write_coil",
            "write_coils",
            "write_to",
        ):
            self.assertFalse(
                hasattr(DeyeReadonlyAdapter, forbidden),
                f"forbidden write method {forbidden} must not exist",
            )

    def test_defaults(self):
        self.assertFalse(self.adapter.connected)
        self.assertIsNone(self.adapter.last_successful_read)
        self.assertIsNone(self.adapter.last_error)


class TestReadSingleWord(unittest.TestCase):
    READ_MAP = {
        186: 2500,      # pv1_power 2500 W
        187: 1200,      # pv2_power
        188: 800,       # pv3_power
        109: 3500,      # pv1_voltage 350.0 V
        110: 55,        # pv1_current 5.5 A
        183: 5350,      # battery_voltage 53.50 V
        184: 78,        # battery_soc 78 %
        190: 1500,      # battery_power 1500 W
        191: 3000,      # battery_current 30.00 A
        167: -200,      # grid_ct_l1_power -200 W
        169: 450,       # grid_total_power 4500 W (scale x10)
        90: 420,        # dc_temperature 42.0 C
        91: 380,        # ac_temperature 38.0 C
        108: 125,       # daily_pv_production 12.5 kWh
    }

    def setUp(self):
        self.client = _build_client(self.READ_MAP)
        self.adapter = DeyeReadonlyAdapter(host="127.0.0.1", port=502)
        self.patcher = patch(
            "deye_readonly_adapter.ModbusTcpClient", return_value=self.client
        )
        self.patcher.start()
        self.adapter.connect()

    def tearDown(self):
        self.patcher.stop()
        self.adapter.disconnect()

    def test_u_word_voltage_scale(self):
        rv = self.adapter.read_register(109)
        self.assertTrue(rv.ok)
        self.assertEqual(rv.value, 350.0)
        self.assertEqual(rv.raw, 3500)
        self.assertEqual(rv.unit, "V")
        self.assertEqual(rv.source, SOURCE_MEASURED)
        self.assertTrue(rv.confirmed)

    def test_u_word_current_scale(self):
        rv = self.adapter.read_register(110)
        self.assertTrue(rv.ok)
        self.assertEqual(rv.value, 5.5)

    def test_battery_voltage_scale(self):
        rv = self.adapter.read_register(183)
        self.assertEqual(rv.value, 53.5)

    def test_battery_soc(self):
        rv = self.adapter.read_register(184)
        self.assertEqual(rv.value, 78)

    def test_s_word_positive(self):
        rv = self.adapter.read_register(190)
        self.assertEqual(rv.value, 1500)

    def test_grid_power_x10(self):
        rv = self.adapter.read_register(169)
        self.assertEqual(rv.value, 4500.0)

    def test_ct_l1_power(self):
        rv = self.adapter.read_register(167)
        self.assertEqual(rv.value, -200)


class TestReadNegativeSWord(unittest.TestCase):
    """S_WORD values above 32767 must decode as negative."""

    def setUp(self):
        # -500 W as S_WORD -> 65536 - 500 = 65036
        self.client = _build_client({169: 65036})
        self.adapter = DeyeReadonlyAdapter(host="127.0.0.1", port=502)
        self.patcher = patch(
            "deye_readonly_adapter.ModbusTcpClient", return_value=self.client
        )
        self.patcher.start()
        self.adapter.connect()

    def tearDown(self):
        self.patcher.stop()
        self.adapter.disconnect()

    def test_negative_grid_power(self):
        rv = self.adapter.read_register(169)
        self.assertTrue(rv.ok)
        self.assertEqual(rv.value, -5000.0)  # scale x10


class TestReadDWord(unittest.TestCase):
    """DWORD assembly: value = low + (high << 16), scale applied."""

    def setUp(self):
        # total_pv_production = 0x0002_0001 = 131073 raw -> *0.1 = 13107.3 kWh
        self.client = _build_client({96: 1, 97: 2})
        self.adapter = DeyeReadonlyAdapter(host="127.0.0.1", port=502)
        self.patcher = patch(
            "deye_readonly_adapter.ModbusTcpClient", return_value=self.client
        )
        self.patcher.start()
        self.adapter.connect()

    def tearDown(self):
        self.patcher.stop()
        self.adapter.disconnect()

    def test_dword_lw_hw_order(self):
        rv = self.adapter.read_dword_pair(96, 97)
        self.assertTrue(rv.ok)
        self.assertEqual(rv.raw, 1 + (2 << 16))  # 131073
        self.assertAlmostEqual(rv.value, 13107.3)


class TestReadAllSnapshot(unittest.TestCase):
    READ_MAP = {
        186: 2500, 187: 1200, 188: 800,
        109: 3500, 110: 55, 111: 3400, 112: 40,
        113: 0, 114: 0,
        183: 5350, 184: 78, 190: 1500, 191: 3000,
        167: -200, 169: 450,
        90: 420, 91: 380,
        96: 1, 97: 2,           # total_pv_production DWORD
        72: 0, 73: 10,          # total_battery_charge DWORD
        74: 5, 75: 0,           # total_battery_discharge DWORD
        108: 125,
    }

    def setUp(self):
        self.client = _build_client(self.READ_MAP)
        self.adapter = DeyeReadonlyAdapter(host="127.0.0.1", port=502)
        self.patcher = patch(
            "deye_readonly_adapter.ModbusTcpClient", return_value=self.client
        )
        self.patcher.start()
        self.adapter.connect()

    def tearDown(self):
        self.patcher.stop()
        self.adapter.disconnect()

    def test_snapshot_structure(self):
        snap = self.adapter.read_all()
        self.assertIsInstance(snap.timestamp, str)
        self.assertTrue(snap.connection_ok)
        self.assertIsNone(snap.error)
        self.assertIsInstance(snap.registers, dict)
        self.assertIsInstance(snap.summary, dict)

    def test_pv_present_and_calculated_p_sun(self):
        snap = self.adapter.read_all()
        regs = snap.registers
        self.assertTrue(regs["pv1_power"].ok)
        self.assertEqual(regs["pv1_power"].value, 2500)
        self.assertTrue(regs["pv2_power"].ok)
        self.assertTrue(regs["pv3_power"].ok)
        # p_sun calculated from PV1+PV2+PV3 = 4500 W
        self.assertEqual(snap.summary["p_sun"], 4500)
        self.assertEqual(snap.summary["p_sun_source"], SOURCE_CALCULATED)

    def test_battery_soc_present(self):
        snap = self.adapter.read_all()
        self.assertEqual(snap.summary["battery_soc"], 78)

    def test_grid_power_measured_from_169(self):
        snap = self.adapter.read_all()
        self.assertEqual(snap.summary["p_grid_measured"], 4500.0)
        self.assertEqual(snap.summary["p_grid_source"], "measured_register_169")
        # p_grid_estimated must NOT exist (no total_load_power in map)
        self.assertIsNone(snap.summary.get("p_grid_estimated"))

    def test_grid_power_falls_to_ct_when_169_absent(self):
        map_wo_169 = {k: v for k, v in self.READ_MAP.items() if k != 169}
        self.client = _build_client(map_wo_169)
        adapter = DeyeReadonlyAdapter(host="127.0.0.1", port=502)
        p = patch(
            "deye_readonly_adapter.ModbusTcpClient", return_value=self.client
        )
        p.start()
        adapter.connect()
        try:
            snap = adapter.read_all()
            self.assertEqual(snap.summary["p_grid_measured"], -200)
            self.assertEqual(
                snap.summary["p_grid_source"], "measured_register_167_ct"
            )
        finally:
            p.stop()
            adapter.disconnect()

    def test_no_estimated_grid_without_load(self):
        snap = self.adapter.read_all()
        self.assertIsNone(snap.summary.get("p_grid_estimated"))

    def test_last_successful_read_set(self):
        before = datetime.now(timezone.utc).isoformat()
        self.adapter.read_all()
        after = datetime.now(timezone.utc).isoformat()
        self.assertIsNotNone(self.adapter.last_successful_read)
        self.assertTrue(before <= self.adapter.last_successful_read <= after)

    def test_unconfirmed_register_not_in_map(self):
        # 182 battery_temp is deliberately absent from REGISTER_MAP
        self.assertNotIn(182, self.adapter.REGISTER_MAP)
        rv = self.adapter.read_register(182)
        self.assertFalse(rv.ok)
        self.assertIsNotNone(rv.error)
        self.assertEqual(rv.source, SOURCE_UNCONFIRMED)
        self.assertFalse(rv.confirmed)


class TestErrorAndUnavailable(unittest.TestCase):
    def test_modbus_error_marks_connection_down(self):
        client = _build_client({184: 50})
        client.read_holding_registers.side_effect = lambda *a, **k: (
            _MockResult([], is_error=True)
        )
        adapter = DeyeReadonlyAdapter(host="127.0.0.1", port=502)
        p = patch(
            "deye_readonly_adapter.ModbusTcpClient", return_value=client
        )
        p.start()
        adapter.connect()
        try:
            rv = adapter.read_register(184)
            self.assertFalse(rv.ok)
            self.assertIsNotNone(rv.error)
            self.assertFalse(adapter.connected)
        finally:
            p.stop()
            adapter.disconnect()

    def test_unavailable_device_read_all(self):
        client = _build_client({}, connect_ok=False)
        adapter = DeyeReadonlyAdapter(host="127.0.0.1", port=502)
        p = patch(
            "deye_readonly_adapter.ModbusTcpClient", return_value=client
        )
        p.start()
        ok = adapter.connect()
        self.assertFalse(ok)
        try:
            snap = adapter.read_all()
            self.assertFalse(snap.connection_ok)
            self.assertIsNotNone(snap.error)
        finally:
            p.stop()
            adapter.disconnect()


class TestNoWriteCalls(unittest.TestCase):
    """The adapter must never invoke any Modbus write function on the client."""

    def test_read_all_only_calls_read(self):
        client = _build_client({184: 50, 186: 100})
        adapter = DeyeReadonlyAdapter(host="127.0.0.1", port=502)
        p = patch(
            "deye_readonly_adapter.ModbusTcpClient", return_value=client
        )
        p.start()
        adapter.connect()
        try:
            adapter.read_all()
            adapter.read_register(184)
        finally:
            p.stop()
            adapter.disconnect()

        # Only connect + read_holding_registers should have been invoked.
        called = [c for c in client.method_calls]
        method_names = [c[0] for c in called]
        self.assertIn("connect", method_names)
        self.assertIn("read_holding_registers", method_names)
        for forbidden in ("write_register", "write_registers", "write_coil", "write_coils"):
            self.assertNotIn(forbidden, method_names)


if __name__ == "__main__":
    unittest.main()
