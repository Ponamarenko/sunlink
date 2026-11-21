"""
Stage 34 — offline validation of Deye register decoding and data provenance.

НЕТ физических Modbus-запросов, нет TCP к 192.168.25.15, нет записей регистров.
Тесты используют только чистые функции и MockFc03Transport.
Искусственные fixtures живут исключительно внутри этого файла.
"""

from __future__ import annotations

import os
import sys
import unittest
from datetime import datetime, timezone

SERVER_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, SERVER_DIR)

from data_source_config import DataSourceConfig, DataSourceConfigError  # noqa: E402
from domain.models import ConfidenceLevel, DataSource, MeasurementQuality  # noqa: E402
from domain.normalizer import (  # noqa: E402
    apply_scale,
    apply_temperature_offset,
    decode_s_word,
    decode_u_word,
    normalize_register,
)
from domain.register_decoder import decode_register  # noqa: E402
from domain.register_metadata import (  # noqa: E402
    ACTIVE_POLL_REGISTERS,
    CANDIDATE_REGISTERS,
    REGISTER_MAP,
)
from domain.telemetry import (  # noqa: E402
    TelemetrySnapshot,
    TelemetryValue,
    utc_now_iso,
)
from modbus.mock_fc03_transport import (  # noqa: E402
    Fc03ModbusException,
    Fc03Timeout,
    MockFc03Transport,
)
from modbus.read_only_modbus_client import ReadOnlyModbusClient  # noqa: E402
from services.deye_polling_service import DeyePollingService  # noqa: E402
from services.production_pipeline import (  # noqa: E402
    ProductionPipelineError,
    build_production_poller,
)


# Локальные правила для 32-битных счётчиков (пока НЕ в production decoder).
def decode_u_dword(low: int, high: int, scale: float = 1.0) -> float:
    if not (0 <= low <= 0xFFFF and 0 <= high <= 0xFFFF):
        raise ValueError("DWORD word out of 16-bit range")
    return (low + (high << 16)) * scale


def decode_s_dword(low: int, high: int, scale: float = 1.0) -> float:
    raw = low + (high << 16)
    if not 0 <= raw <= 0xFFFFFFFF:
        raise ValueError("DWORD out of range")
    signed = raw - (1 << 32) if raw > 0x7FFFFFFF else raw
    return signed * scale


class TestPrimitiveDecoders(unittest.TestCase):
    def test_u_word(self):
        self.assertEqual(decode_u_word(0), 0)
        self.assertEqual(decode_u_word(0xFFFF), 65535)
        with self.assertRaises(ValueError):
            decode_u_word(-1)
        with self.assertRaises(ValueError):
            decode_u_word(0x10000)

    def test_s_word_two_complement(self):
        self.assertEqual(decode_s_word(0xFC18), -1000)
        self.assertEqual(decode_s_word(0x8000), -32768)
        self.assertEqual(decode_s_word(0x7FFF), 32767)
        self.assertEqual(decode_s_word(0xFFFF), -1)

    def test_u_dword_lw_hw_and_scale(self):
        self.assertAlmostEqual(decode_u_dword(6433, 0, 0.1), 643.3)
        self.assertEqual(decode_u_dword(0, 1, 1.0), 65536)
        self.assertAlmostEqual(decode_u_dword(0xFFFF, 0x0001, 1.0), 131071.0)

    def test_s_dword_negative(self):
        self.assertEqual(decode_s_dword(0xFFFF, 0xFFFF, 1.0), -1)
        self.assertEqual(decode_s_dword(0x0000, 0x8000, 1.0), -2147483648)
        with self.assertRaises(ValueError):
            decode_s_dword(0, 0x10000)

    def test_scaling_and_temperature_offset(self):
        self.assertEqual(apply_scale(5322, 0.01), 53.22)
        self.assertEqual(apply_temperature_offset(141.0, 100.0), 41.0)
        self.assertEqual(apply_temperature_offset(48.2, 100.0), -51.8)


class TestRegisterDecoderContract(unittest.TestCase):
    def test_battery_soc_decoding(self):
        meta = ACTIVE_POLL_REGISTERS[184]
        self.assertEqual(set(ACTIVE_POLL_REGISTERS), {184})
        self.assertEqual(normalize_register(184, 94, meta).normalized_value, 94.0)
        self.assertEqual(decode_register(94, meta).normalized_value, 94.0)
        d = decode_register(94, meta)
        self.assertEqual(d.quality, MeasurementQuality.VALID.value)
        self.assertEqual(d.confidence, ConfidenceLevel.CORROBORATED.value)
        self.assertEqual(d.data_type, "U_WORD")
        self.assertEqual(d.scale, 1.0)

    def test_battery_voltage_scale(self):
        d = decode_register(5353, REGISTER_MAP[183])
        self.assertAlmostEqual(d.normalized_value, 53.53)
        self.assertEqual(d.unit, "V")

    def test_signed_current_negative(self):
        d = decode_register(0xFEA1, REGISTER_MAP[191])
        self.assertAlmostEqual(d.normalized_value, -3.51)
        self.assertEqual(d.decoded_value, -351)

    def test_temperature_offset_is_candidate_only(self):
        meta = REGISTER_MAP[90]
        baseline = decode_register(1410, meta)
        candidate = decode_register(1410, meta, use_candidate=True)
        # baseline уже включает offset -100; кандидат даёт тот же результат,
        # но явно помечен как PROVISIONAL и не повышает достоверность.
        self.assertAlmostEqual(baseline.normalized_value, 41.0)
        self.assertAlmostEqual(candidate.normalized_value, 41.0)
        self.assertTrue(candidate.used_candidate)
        self.assertEqual(candidate.confidence, ConfidenceLevel.PROVISIONAL.value)

    def test_battery_power_candidate_conflict_is_not_silently_applied(self):
        meta = REGISTER_MAP[190]
        baseline = decode_register(0xF8C3, meta)
        candidate = decode_register(0xF8C3, meta, use_candidate=True)
        self.assertEqual(baseline.normalized_value, -1853.0)
        self.assertAlmostEqual(candidate.normalized_value, -185.3)
        self.assertEqual(meta.confidence, ConfidenceLevel.PROVISIONAL)
        self.assertEqual(meta.candidate_scale, 0.1)
        # Обе версии остаются кандидатами: neither is promoted without display check.
        self.assertNotEqual(meta.confidence, ConfidenceLevel.VERIFIED)

    def test_null_invalid_and_out_of_range_are_never_zero(self):
        meta = REGISTER_MAP[184]
        for raw in (None, -1, 0x10000):
            with self.subTest(raw=raw):
                d = decode_register(raw, meta)
                self.assertIsNone(d.normalized_value)
                self.assertFalse(d.ok)
                self.assertNotEqual(d.normalized_value, 0.0)
        self.assertIsNone(decode_register(200, meta).normalized_value)  # SOC > 100

    def test_word_order_for_provisional_dword_rule(self):
        self.assertNotEqual(decode_u_dword(1, 2), decode_u_dword(2, 1))
        self.assertEqual(decode_u_dword(1, 2), 131073)


class TestOfflineFailureHandling(unittest.TestCase):
    def _client(self, flags=None, **kwargs):
        transport = MockFc03Transport({184: 94})
        for key, value in kwargs.items():
            setattr(transport, key, value)
        for flag in (flags or ()):
            setattr(transport, flag, True)
        transport.connected = True
        return ReadOnlyModbusClient(transport), transport

    def test_timeout_and_modbus_exception_do_not_fabricate_value(self):
        for key in ("fail_timeout", "fail_modbus_exception"):
            with self.subTest(key=key):
                client, transport = self._client(flags=(key,))
                poller = DeyePollingService(client)
                snap = poller.read()
                self.assertFalse(snap.connection_ok)
                self.assertFalse(snap.values)
                self.assertIsNone(snap.value_of("battery_soc"))

    def test_missing_and_invalid_length_registers(self):
        for key in ("missing_addresses", "invalid_length_at"):
            with self.subTest(key=key):
                value = {184} if key == "missing_addresses" else 184
                client, transport = self._client(**{key: value})
                poller = DeyePollingService(client)
                snap = poller.read()
                self.assertFalse(snap.connection_ok)
                self.assertIsNone(snap.value_of("battery_soc"))

    def test_stale_snapshot_keeps_last_good_but_marks_stale(self):
        client, transport = self._client()
        poller = DeyePollingService(client)
        ts = utc_now_iso()
        value = TelemetryValue(
            "battery_soc", 94.0, "%", ts, "corroborated", "valid", 94, 184
        )
        poller._last_good = TelemetrySnapshot(
            "deye_sg02lp1_am3", DataSource.DEYE_MODBUS, ts, True, {"battery_soc": value}
        )
        transport.fail_timeout = True
        snap = poller.read()
        self.assertTrue(snap.stale)
        self.assertFalse(snap.connection_ok)
        self.assertEqual(snap.value_of("battery_soc"), 94.0)

    def test_timestamp_is_written_for_each_measurement(self):
        client, transport = self._client()
        poller = DeyePollingService(client)
        snap = poller.read()
        parsed = datetime.fromisoformat(snap.timestamp)
        self.assertEqual(parsed.tzinfo, timezone.utc)
        self.assertEqual(snap.values["battery_soc"].timestamp, snap.timestamp)

    def test_no_write_methods_on_client(self):
        client, transport = self._client()
        for name in ("write_register", "write_coil", "write_registers"):
            with self.assertRaises(AttributeError):
                getattr(client, name)


class TestSourceIsolation(unittest.TestCase):
    def test_invalid_source_rejected(self):
        with self.assertRaises(DataSourceConfigError):
            DataSourceConfig.from_env({"SUNLINK_DATA_SOURCE": "bogus"})

    def test_production_pipeline_refuses_demo_and_unconfirmed_physical(self):
        demo = DataSourceConfig.from_env({"SUNLINK_DATA_SOURCE": "demo"})
        with self.assertRaises(ProductionPipelineError):
            build_production_poller(demo)
        # Явный opt-in обязателен: без него from_env уже выбрасывает ошибку.
        with self.assertRaises(DataSourceConfigError):
            DataSourceConfig.from_env({
                "SUNLINK_DATA_SOURCE": "deye_modbus",
                "SUNLINK_ENABLE_MODBUS": "0",
            })

    def test_demo_source_is_labeled_and_not_deye_modbus(self):
        from services.telemetry_sources import DemoTelemetrySource

        source = DemoTelemetrySource()
        self.assertEqual(source.source, DataSource.DEMO_SIMULATOR)
        snap = source.read()
        self.assertNotEqual(snap.source, DataSource.DEYE_MODBUS)
        self.assertEqual(snap.value_of("battery_soc"), 85.0)  # только в тесте

    def test_production_entrypoint_does_not_import_demo_modules(self):
        with open(os.path.join(SERVER_DIR, "deye_gateway.py"), encoding="utf-8") as f:
            code = f.read()
        for module in ("demo_backend", "demo_stage7", "demo_stage8", "modbus.mock_client"):
            self.assertNotIn(f"import {module}", code)
            self.assertNotIn(f"from {module}", code)
        self.assertIn("production_pipeline", code)

    def test_no_random_in_production_sources(self):
        root = os.path.join(SERVER_DIR)
        bad = []
        for dirpath, dirnames, filenames in os.walk(root):
            if "tests" in dirpath or "__pycache__" in dirpath:
                continue
            for name in filenames:
                if not name.endswith(".py"):
                    continue
                path = os.path.join(dirpath, name)
                with open(path, encoding="utf-8") as f:
                    text = f.read()
                if "random." in text or "uniform(" in text or "from random" in text:
                    bad.append(name)
        self.assertEqual(bad, [], f"random in production: {bad}")

    def test_candidate_registers_are_not_in_active_map(self):
        self.assertTrue(set(CANDIDATE_REGISTERS).issubset(set(REGISTER_MAP)))
        self.assertEqual(set(ACTIVE_POLL_REGISTERS), {184})
        for address in (183, 186, 187, 188, 190, 191, 90, 91):
            self.assertNotIn(address, ACTIVE_POLL_REGISTERS)


if __name__ == "__main__":
    unittest.main()
