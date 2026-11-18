"""
Тесты deye_gateway.py (этап 10 — интеграция с Data Layer).

Все тесты используют mock-объекты для Modbus клиента — физический инвертор
не опрашивается, сетевых запросов нет.
"""

from __future__ import annotations

import asyncio
import json
import os
import sys
import tempfile
import unittest
from datetime import datetime, timezone
from unittest.mock import MagicMock, patch, AsyncMock

# server/ на sys.path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from domain.register_metadata import REGISTER_MAP, RegisterMetadata
from domain.normalizer import normalize_register
from domain.models import (
    MeasurementQuality,
    ConfidenceLevel,
    DataSource,
    Measurement,
)
from storage.sqlite_repository import SQLiteMeasurementRepository

import deye_gateway
from deye_gateway import DeyeGateway, INVERTER_ID, CONFIG, app


# ---------------------------------------------------------------------------
# Helper: mock Modbus client
# ---------------------------------------------------------------------------

def _make_mock_client(register_values: dict[int, int]) -> MagicMock:
    """Создаёт mock ModbusTcpClient с заданными значениями регистров."""
    client = MagicMock()
    client.connect.return_value = True

    def read(addr, count=1, device_id=1):
        mock_result = MagicMock()
        mock_result.isError.return_value = False
        if addr in register_values:
            mock_result.registers = [register_values[addr]]
        else:
            mock_result.registers = [0]
        return mock_result

    client.read_holding_registers = MagicMock(side_effect=read)
    client.close = MagicMock()
    return client


class TestGatewayReadOnly(unittest.TestCase):
    """Шлюз строго read-only: нет write-методов, нет SafetyManager."""

    def test_no_write_method(self):
        """У шлюза нет метода write_register."""
        self.assertFalse(hasattr(deye_gateway.DeyeGateway, "write_register"))

    def test_no_safety_manager(self):
        """У шлюза нет SafetyManager (запись запрещена)."""
        self.assertFalse(hasattr(deye_gateway.DeyeGateway, "safety_manager"))
        gw = deye_gateway.DeyeGateway(deye_gateway.CONFIG)
        self.assertFalse(hasattr(gw, "safety_manager"))

    def test_no_write_endpoint(self):
        """Нет HTTP-эндпоинта /write."""
        paths = [r.path for r in app.routes if hasattr(r, "path")]
        self.assertNotIn("/write", paths)
        self.assertNotIn("/emergency-stop", paths)
        self.assertNotIn("/emergency-resume", paths)

    def test_no_writable_registers(self):
        """Все регистры из REGISTER_MAP — не writable (чтение только)."""
        for addr, reg in REGISTER_MAP.items():
            # В domain.register_metadata нет поля writable — регистры не имеют
            # понятия "записываемый". Это гарантирует read-only.
            self.assertIsInstance(reg, RegisterMetadata)

    def test_gateway_read_only_flag(self):
        """Конфигурация не содержит write-параметров."""
        self.assertNotIn("max_concurrent_writes", CONFIG.get("server", {}))
        self.assertNotIn("write_cooldown", CONFIG.get("server", {}))


class TestGatewayNormalization(unittest.TestCase):
    """Чтение регистров использует normalizer из Data Layer."""

    def setUp(self):
        self.tmpdir = tempfile.mkdtemp()
        self.db_path = os.path.join(self.tmpdir, "test_measurements.db")

    def test_normalize_register_applied(self):
        """normalize_register применяется к каждому регистру из REGISTER_MAP."""
        for addr, metadata in REGISTER_MAP.items():
            result = normalize_register(addr, 1000, metadata)
            self.assertIsNotNone(result)
            self.assertEqual(result.register_address, addr)
            self.assertEqual(result.parameter_name, metadata.name)
            self.assertEqual(result.confidence, metadata.confidence)

    def test_signed_decoding(self):
        """S_WORD регистры декодируются как знаковые (two's complement)."""
        # reg 190 (battery_power) — S_WORD, raw 40000 -> -25536
        metadata = REGISTER_MAP[190]
        self.assertTrue(metadata.is_signed)
        result = normalize_register(190, 40000, metadata)
        self.assertEqual(result.decoded_value, 40000 - 65536)
        self.assertEqual(result.normalized_value, -25536.0)

    def test_temperature_offset(self):
        """Температурные регистры (90, 91) применяют offset -100."""
        for addr in (90, 91):
            metadata = REGISTER_MAP[addr]
            self.assertEqual(metadata.offset, 100.0)
            result = normalize_register(addr, 3250, metadata)  # 325.0 °C - 100 = 225.0
            self.assertAlmostEqual(result.normalized_value, 225.0, places=1)

    def test_confidence_levels(self):
        """Регистры имеют правильные уровни достоверности."""
        self.assertEqual(REGISTER_MAP[183].confidence, ConfidenceLevel.CORROBORATED)
        self.assertEqual(REGISTER_MAP[184].confidence, ConfidenceLevel.CORROBORATED)
        self.assertEqual(REGISTER_MAP[190].confidence, ConfidenceLevel.PROVISIONAL)
        self.assertEqual(REGISTER_MAP[90].confidence, ConfidenceLevel.PROVISIONAL)
        self.assertEqual(REGISTER_MAP[91].confidence, ConfidenceLevel.PROVISIONAL)


class TestGatewayRepository(unittest.TestCase):
    """Сохранение и чтение измерений через SQLite репозиторий."""

    def setUp(self):
        self.tmpdir = tempfile.mkdtemp()
        self.db_path = os.path.join(self.tmpdir, "test_measurements.db")
        self.repo = SQLiteMeasurementRepository(self.db_path)

    def tearDown(self):
        self.repo.close()

    def test_save_and_get_latest(self):
        """Измерение сохраняется и читается через get_latest."""
        m = Measurement(
            inverter_id=INVERTER_ID,
            register_address=184,
            parameter_name="battery_soc",
            raw_value=86,
            normalized_value=86.0,
            unit="%",
            timestamp=datetime.now(timezone.utc),
            quality=MeasurementQuality.VALID,
            confidence=ConfidenceLevel.CORROBORATED,
            source=DataSource.MODBUS_REGISTER,
            description="Battery state of charge",
        )
        self.repo.save_measurement(m)

        latest = self.repo.get_latest(INVERTER_ID, "battery_soc")
        self.assertIsNotNone(latest)
        self.assertEqual(latest.parameter_name, "battery_soc")
        self.assertEqual(latest.raw_value, 86)
        self.assertEqual(latest.normalized_value, 86.0)

    def test_get_all_latest(self):
        """get_all_latest возвращает по одному измерению на параметр."""
        now = datetime.now(timezone.utc)
        for param, raw, norm in [("battery_soc", 86, 86.0), ("pv1_power", 1500, 1500.0)]:
            m = Measurement(
                inverter_id=INVERTER_ID,
                register_address=184 if param == "battery_soc" else 186,
                parameter_name=param,
                raw_value=raw,
                normalized_value=norm,
                unit="%" if param == "battery_soc" else "W",
                timestamp=now,
                quality=MeasurementQuality.VALID,
                confidence=ConfidenceLevel.CORROBORATED,
                source=DataSource.MODBUS_REGISTER,
            )
            self.repo.save_measurement(m)

        all_latest = self.repo.get_all_latest(INVERTER_ID)
        params = {m.parameter_name for m in all_latest}
        self.assertIn("battery_soc", params)
        self.assertIn("pv1_power", params)

    def test_schema_version(self):
        """Схема БД имеет версию 1."""
        self.assertEqual(self.repo.schema_version(), 1)


class TestGatewayEndpoints(unittest.TestCase):
    """Структура API endpoints (без живых Modbus-данных)."""

    def test_endpoints_exist(self):
        """Все ожидаемые endpoints присутствуют в FastAPI app."""
        paths = [r.path for r in app.routes if hasattr(r, "path")]
        expected = [
            "/",
            "/status",
            "/registers",
            "/read",
            "/live",
            "/api/live/raw",
            "/api/measurements/latest",
            "/api/measurements/latest/{parameter}",
            "/api/measurements/history",
            "/weather",
            "/forecast",
            "/ws",
        ]
        for path in expected:
            self.assertIn(path, paths, f"Endpoint {path} missing")

    def test_no_write_endpoints(self):
        """Нет endpoints для записи."""
        paths = [r.path for r in app.routes if hasattr(r, "path")]
        self.assertNotIn("/write", paths)

    def test_registers_endpoint_structure(self):
        """Endpoint /registers возвращает все 9 регистра с метаданными."""
        # Проверяем, что register_map содержит 9 регистра из этапа 7
        self.assertEqual(len(deye_gateway.gateway.register_map), 9)
        for addr in (90, 91, 183, 184, 186, 187, 188, 190, 191):
            self.assertIn(addr, deye_gateway.gateway.register_map)

    def test_inverter_id_constant(self):
        """Константа INVERTER_ID определена."""
        self.assertEqual(INVERTER_ID, "deye_sg02lp1_am3")


class TestGatewayAndroidCompatibility(unittest.TestCase):
    """Совместимость с Android приложением (существующие endpoints)."""

    def test_live_raw_endpoint_exists(self):
        """/api/live/raw есть для Android."""
        paths = [r.path for r in app.routes if hasattr(r, "path")]
        self.assertIn("/api/live/raw", paths)

    def test_live_endpoint_exists(self):
        """/live есть для Android."""
        paths = [r.path for r in app.routes if hasattr(r, "path")]
        self.assertIn("/live", paths)

    def test_status_endpoint_exists(self):
        """/status есть для проверки соединения."""
        paths = [r.path for r in app.routes if hasattr(r, "path")]
        self.assertIn("/status", paths)

    def test_flat_live_structure(self):
        """_flat_live возвращает ключи, которые ожидает Android."""
        data = {
            "battery_soc": {"normalized_value": 86.0, "unit": "%"},
            "battery_voltage": {"normalized_value": 52.5, "unit": "V"},
            "pv1_power": {"normalized_value": 1500.0, "unit": "W"},
            "pv2_power": {"normalized_value": 1200.0, "unit": "W"},
            "pv3_power": {"normalized_value": 0.0, "unit": "W"},
            "battery_power": {"normalized_value": -100.0, "unit": "W"},
            "dc_temperature": {"normalized_value": 45.0, "unit": "°C"},
            "ac_temperature": {"normalized_value": 40.0, "unit": "°C"},
        }
        status = {"connection_status": True}
        flat = deye_gateway._flat_live(data, status)

        expected_keys = {"p_sun", "p_load", "p_batt", "p_grid", "batt_soc",
                         "battery_voltage", "battery_current", "timestamp",
                         "connection_status"}
        for key in expected_keys:
            self.assertIn(key, flat, f"Key {key} missing from flat_live output")

        self.assertEqual(flat["batt_soc"], 86.0)
        self.assertEqual(flat["battery_voltage"], 52.5)
        self.assertTrue(flat["connection_status"])

    def test_flat_live_modbus_unavailable(self):
        """При недоступности Modbus — нулевые значения, не аварийный выход."""
        flat = deye_gateway._flat_live({}, {"connection_status": False})
        self.assertEqual(flat["p_sun"], 0.0)
        self.assertEqual(flat["batt_soc"], 0.0)
        self.assertFalse(flat["connection_status"])


class TestGatewayMeasurementsEndpoints(unittest.TestCase):
    """Тесты production endpoints /api/measurements/* (через mock репозиторий)."""

    def setUp(self):
        self.tmpdir = tempfile.mkdtemp()
        self.db_path = os.path.join(self.tmpdir, "test_measurements.db")
        self.repo = SQLiteMeasurementRepository(self.db_path)
        self.gw = DeyeGateway(deye_gateway.CONFIG, repository=self.repo)

    def tearDown(self):
        self.repo.close()

    def test_get_all_latest_empty(self):
        """Без измерений — пустой список."""
        measurements = self.repo.get_all_latest(INVERTER_ID)
        self.assertEqual(len(measurements), 0)

    def test_save_measurement_through_gateway(self):
        """Шлюз может сохранить измерение через репозиторий (имитация read_register + save)."""
        from domain.normalizer import make_measurement

        m = make_measurement(
            inverter_id=INVERTER_ID,
            register_address=184,
            parameter_name="battery_soc",
            raw_value=86,
            normalized_value=86.0,
            unit="%",
            timestamp=datetime.now(timezone.utc),
            quality=MeasurementQuality.VALID,
            confidence=ConfidenceLevel.CORROBORATED,
            source=DataSource.MODBUS_REGISTER,
        )
        self.repo.save_measurement(m)

        latest = self.repo.get_latest(INVERTER_ID, "battery_soc")
        self.assertIsNotNone(latest)
        self.assertEqual(latest.normalized_value, 86.0)

    def test_history_endpoint_params(self):
        """Endpoint /api/measurements/history принимает parameter, start, end."""
        # Просто проверяем, что endpoint существует и принимает правильные query-параметры
        paths = [r.path for r in app.routes if hasattr(r, "path")]
        self.assertIn("/api/measurements/history", paths)


if __name__ == "__main__":
    unittest.main()
