"""
Stage 35A — offline safety hardening tests.

НИКАКИХ физических подключений. ModbusTcpClient подменяется на объект, который
немедленно падает при попытке создать/открыть соединение — тест провалится,
если DEMO или отсутствующая конфигурация попытаются обратиться к Ebyte.
"""

from __future__ import annotations

import asyncio
import os
import sys
import tempfile
import unittest
from typing import Any, Dict, List
from unittest.mock import patch

SERVER_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, SERVER_DIR)

import deye_gateway  # noqa: E402
from data_source_config import DataSourceConfig, DataSourceConfigError  # noqa: E402
from domain.models import DataSource  # noqa: E402
from domain.register_metadata import (  # noqa: E402
    ACTIVE_POLL_REGISTERS,
    REGISTER_MAP,
    RegisterStatus,
    normalize_physical_status,
)
from modbus.read_only_modbus_client import (  # noqa: E402
    ModbusReadError,
    ReadOnlyModbusClient,
)
from services.deye_polling_service import DeyePollingService  # noqa: E402
from services.production_pipeline import (  # noqa: E402
    ProductionPipelineError,
    build_production_poller,
)
from services.telemetry_sources import DemoTelemetrySource  # noqa: E402
from storage.sqlite_repository import SQLiteMeasurementRepository  # noqa: E402


class ForbiddenPhysicalClient:
    """Маркер: любое обращение означает попытку физического подключения."""

    def __init__(self, *args: Any, **kwargs: Any) -> None:
        raise AssertionError("physical Modbus client must NOT be created in offline tests")


class StubTransport:
    def __init__(self, response: Any = None, exc: Exception | None = None) -> None:
        self.response = response
        self.exc = exc
        self.request_count = 0

    def read_holding_registers(self, address: int, count: int = 1, unit_id: int = 1):
        self.request_count += 1
        if self.exc is not None:
            raise self.exc
        return self.response

    def close(self) -> None:
        pass


class OkResponse:
    def __init__(self, registers: List[int]) -> None:
        self.registers = registers

    def isError(self) -> bool:
        return False


class ErrorResponse:
    def __init__(self) -> None:
        self.registers = []

    def isError(self) -> bool:
        return True

    message = "modbus exception 0x02"


class TestDemoProductionIsolation(unittest.TestCase):
    def test_demo_config_does_not_create_physical_transport(self):
        cfg = DataSourceConfig.from_env({})
        self.assertEqual(cfg.source, "demo")
        with self.assertRaises(ProductionPipelineError):
            build_production_poller(cfg)

    def test_missing_configuration_is_not_implicit_physical(self):
        cfg = DataSourceConfig.from_env({})
        self.assertNotIn(cfg.source, ("modbus", "deye_modbus"))
        self.assertFalse(cfg.enable_modbus)

    def test_production_source_is_explicit_and_registers_are_active_only(self):
        cfg = DataSourceConfig.from_env({
            "SUNLINK_DATA_SOURCE": "deye_modbus",
            "SUNLINK_ENABLE_MODBUS": "1",
            "SUNLINK_MODBUS_HOST": "192.168.25.15",
        })
        poller, client, transport = build_production_poller(cfg)
        self.assertEqual(poller.source, DataSource.DEYE_MODBUS)
        self.assertEqual(set(poller._active), {184})
        self.assertEqual(transport.host, "192.168.25.15")
        # Transport object created but no socket opened.
        self.assertEqual(transport.request_count, 0)
        client.close()

    def test_demo_gateway_never_constructs_modbus_client(self):
        gateway = deye_gateway.DeyeGateway(
            deye_gateway.CONFIG,
            telemetry_source=DemoTelemetrySource(),
            physical_modbus_allowed=False,
        )
        with patch.object(deye_gateway, "ModbusTcpClient", ForbiddenPhysicalClient):
            self.assertFalse(asyncio.run(gateway.connect()))
            data = asyncio.run(gateway.read_all_registers())
        self.assertEqual(data["battery_soc"]["normalized_value"], 85.0)
        self.assertIsNone(gateway.client)
        self.assertEqual(gateway.source, DataSource.DEMO_SIMULATOR)

    def test_gateway_without_source_and_disabled_modbus_does_not_fallback(self):
        gateway = deye_gateway.DeyeGateway(
            deye_gateway.CONFIG, physical_modbus_allowed=False
        )
        with patch.object(deye_gateway, "ModbusTcpClient", ForbiddenPhysicalClient):
            self.assertEqual(asyncio.run(gateway.read_all_registers()), {})
            self.assertFalse(gateway.connection_status)
        self.assertIsNone(gateway.client)

    def test_simulator_error_does_not_enable_physical_modbus(self):
        source = DemoTelemetrySource({"battery_soc": None})
        gateway = deye_gateway.DeyeGateway(
            deye_gateway.CONFIG,
            telemetry_source=source,
            physical_modbus_allowed=False,
        )
        with patch.object(deye_gateway, "ModbusTcpClient", ForbiddenPhysicalClient):
            data = asyncio.run(gateway.read_all_registers())
        self.assertEqual(data["battery_soc"]["normalized_value"], None)
        self.assertIsNone(gateway.client)

    def test_physical_failure_does_not_enable_simulator(self):
        transport = StubTransport(exc=TimeoutError("offline"))
        poller = DeyePollingService(ReadOnlyModbusClient(transport))
        snap = poller.read()
        self.assertFalse(snap.connection_ok)
        self.assertFalse(snap.values)
        self.assertEqual(snap.source, DataSource.DEYE_MODBUS)
        self.assertNotEqual(snap.source, DataSource.DEMO_SIMULATOR)


class TestModbusErrorHandling(unittest.TestCase):
    def _client(self, response=None, exc=None) -> ReadOnlyModbusClient:
        return ReadOnlyModbusClient(StubTransport(response, exc))

    def test_success(self):
        self.assertEqual(self._client(OkResponse([94])).read_register(184), 94)
        self.assertEqual(self._client(OkResponse([1, 2])).read_holding_registers(184, 2), [1, 2])

    def test_none_response(self):
        with self.assertRaises(ModbusReadError):
            self._client(None).read_register(184)

    def test_modbus_exception(self):
        with self.assertRaises(ModbusReadError):
            self._client(ErrorResponse()).read_register(184)

    def test_missing_or_empty_registers(self):
        class Missing:
            pass

        for response in (Missing(), OkResponse([])):
            with self.subTest(response=type(response).__name__):
                with self.assertRaises(ModbusReadError):
                    self._client(response).read_register(184)

    def test_wrong_length_and_invalid_type(self):
        for response in (OkResponse([1, 2]), OkResponse(["94"]), OkResponse([70000])):
            with self.subTest(value=response.registers):
                with self.assertRaises(ModbusReadError):
                    self._client(response).read_register(184)

    def test_timeout_and_transport_error(self):
        for exc in (TimeoutError("timeout"), OSError("disconnected")):
            with self.subTest(exc=type(exc).__name__):
                with self.assertRaises(Exception):
                    self._client(exc=exc).read_register(184)

    def test_poller_does_not_persist_invalid_value(self):
        for response in (None, ErrorResponse(), OkResponse([]), OkResponse([1, 2])):
            with self.subTest(response=response):
                poller = DeyePollingService(self._client(response))
                snap = poller.read()
                self.assertFalse(snap.connection_ok)
                self.assertIsNone(snap.value_of("battery_soc"))
                self.assertFalse(snap.values)


class TestRegisterStatusSemantics(unittest.TestCase):
    def test_legacy_status_alias(self):
        self.assertEqual(normalize_physical_status("PHYSICALLY_READ"), "fc03_read")
        self.assertEqual(
            normalize_physical_status("PHYSICALLY_CONFIRMED"),
            RegisterStatus.PHYSICALLY_CONFIRMED.value,
        )

    def test_only_184_is_physically_confirmed_and_production_allowed(self):
        confirmed = [a for a, m in REGISTER_MAP.items()
                     if m.physical_status == RegisterStatus.PHYSICALLY_CONFIRMED.value]
        allowed = [a for a, m in REGISTER_MAP.items() if m.production_allowed]
        self.assertEqual(confirmed, [184])
        self.assertEqual(allowed, [184])
        self.assertEqual(set(ACTIVE_POLL_REGISTERS), {184})

    def test_fc03_read_registers_are_not_promoted(self):
        for address in (183, 186, 187, 188, 191):
            meta = REGISTER_MAP[address]
            self.assertEqual(meta.physical_status, RegisterStatus.FC03_READ.value)
            self.assertFalse(meta.display_verified)
            self.assertFalse(meta.production_allowed)
            self.assertNotIn(address, ACTIVE_POLL_REGISTERS)

    def test_no_register_is_display_verified(self):
        self.assertFalse(any(m.display_verified for m in REGISTER_MAP.values()))


class TestStatusAndApiCompatibility(unittest.TestCase):
    def test_status_distinguishes_known_active_and_confirmed(self):
        poller = DeyePollingService(
            ReadOnlyModbusClient(StubTransport(OkResponse([94])))
        )
        gateway = deye_gateway.DeyeGateway(
            deye_gateway.CONFIG,
            telemetry_source=poller,
            physical_modbus_allowed=True,
        )
        status = asyncio.run(gateway.get_system_status())
        self.assertEqual(status["available_registers"], 9)  # legacy
        self.assertEqual(status["known_registers_count"], 9)
        self.assertEqual(status["active_registers_count"], 1)
        self.assertEqual(status["active_register_addresses"], [184])
        self.assertEqual(status["physically_confirmed_registers_count"], 1)
        self.assertEqual(status["source"], "deye_modbus")

    def test_demo_status_has_no_active_physical_registers(self):
        gateway = deye_gateway.DeyeGateway(
            deye_gateway.CONFIG,
            telemetry_source=DemoTelemetrySource(),
            physical_modbus_allowed=False,
        )
        status = asyncio.run(gateway.get_system_status())
        self.assertEqual(status["source"], "demo_simulator")
        self.assertEqual(status["active_registers_count"], 0)
        self.assertEqual(status["active_register_addresses"], [])
        self.assertEqual(status["physically_confirmed_registers_count"], 1)

    def test_rest_compatibility_with_offline_source(self):
        from fastapi.testclient import TestClient

        gateway = deye_gateway.gateway
        old_source, old_allowed, old_telemetry = (
            gateway.source, gateway.physical_modbus_allowed, gateway.telemetry_source
        )
        old_repository = deye_gateway._repository
        tmp = tempfile.NamedTemporaryFile(suffix=".db", delete=False)
        tmp.close()
        try:
            deye_gateway._repository = SQLiteMeasurementRepository(tmp.name)
            gateway.telemetry_source = DemoTelemetrySource()
            gateway.source = DataSource.DEMO_SIMULATOR
            gateway.physical_modbus_allowed = False
            with patch.object(deye_gateway, "ModbusTcpClient", ForbiddenPhysicalClient):
                client = TestClient(deye_gateway.app)
                status = client.get("/status")
                raw = client.get("/api/live/raw")
                latest = client.get("/api/measurements/latest")
                inverters = client.get("/api/inverters")
            self.assertEqual(status.status_code, 200)
            self.assertEqual(raw.status_code, 200)
            self.assertEqual(latest.status_code, 200)
            self.assertEqual(inverters.status_code, 200)
            self.assertEqual(raw.json()["batt_soc"], 85.0)
            self.assertEqual(raw.json()["source"], "demo_simulator")
            self.assertIn("active_registers_count", status.json())
        finally:
            gateway.source = old_source
            gateway.physical_modbus_allowed = old_allowed
            gateway.telemetry_source = old_telemetry
            deye_gateway._repository = old_repository
            os.unlink(tmp.name)

    def test_websocket_route_and_frame_format(self):
        from fastapi.testclient import TestClient

        gateway = deye_gateway.gateway
        old_source, old_allowed, old_telemetry = (
            gateway.source, gateway.physical_modbus_allowed, gateway.telemetry_source
        )
        try:
            gateway.telemetry_source = DemoTelemetrySource()
            gateway.source = DataSource.DEMO_SIMULATOR
            gateway.physical_modbus_allowed = False
            with patch.object(deye_gateway, "ModbusTcpClient", ForbiddenPhysicalClient):
                with TestClient(deye_gateway.app) as client:
                    with client.websocket_connect("/ws") as ws:
                        frame = ws.receive_json()
            self.assertEqual(frame["type"], "live_data")
            self.assertIn("data", frame)
            self.assertIn("status", frame)
            self.assertEqual(frame["status"]["source"], "demo_simulator")
        finally:
            gateway.source = old_source
            gateway.physical_modbus_allowed = old_allowed
            gateway.telemetry_source = old_telemetry


if __name__ == "__main__":
    unittest.main()
