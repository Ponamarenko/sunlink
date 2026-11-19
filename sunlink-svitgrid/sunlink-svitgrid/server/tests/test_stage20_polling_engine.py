"""
Stage 20 — offline тесты read-only Modbus polling engine.

НИКАКИХ физических подключений: все TCP-ответы синтетические через FakeSocket.
"""

from __future__ import annotations

import os
import socket
import struct
import sys
import tempfile
import threading
import unittest
from datetime import datetime, timedelta, timezone

SERVER_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, SERVER_DIR)
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from data_source_config import DataSourceConfig, DataSourceConfigError  # noqa: E402
from domain.models import DataSource  # noqa: E402
from domain.register_metadata import ACTIVE_POLL_REGISTERS, REGISTER_MAP  # noqa: E402
from modbus.modbus_tcp_transport import (  # noqa: E402
    ModbusDisconnect,
    ModbusTransportError,
    ModbusExceptionResponse,
    ModbusInvalidResponse,
    ModbusTimeout,
    ModbusTcpTransport,
)
from modbus.read_only_modbus_client import ReadOnlyModbusClient  # noqa: E402
from services.deye_polling_service import (  # noqa: E402
    DEFAULT_PHYSICAL_POLL_INTERVAL,
    DeyePollingService,
    PollingState,
)
from storage.sqlite_repository import SQLiteMeasurementRepository  # noqa: E402


def fc03_response(tid=1, unit=1, fc=3, registers=(99,), proto=0, length=None):
    body = bytes([fc, 2 * len(registers)])
    for reg in registers:
        body += struct.pack(">H", reg)
    mbap = struct.pack(">HHHB", tid, proto, length if length is not None else len(body) + 1, unit)
    return mbap + body


class FakeSocket:
    """Программный сокет: отдаёт ответ кусками, фиксирует send/close."""

    def __init__(self, chunks, timeout=False, disconnect=False):
        self.chunks = list(chunks)
        self.sent = []
        self.closed = False
        self.fail_timeout = timeout
        self.fail_disconnect = disconnect

    def settimeout(self, value):
        pass

    def sendall(self, data):
        self.sent.append(data)

    def recv(self, n):
        if self.fail_timeout:
            raise socket.timeout("fake timeout")
        if self.fail_disconnect:
            return b""
        if not self.chunks:
            return b""
        chunk = self.chunks[0]
        if len(chunk) <= n:
            self.chunks.pop(0)
            return chunk
        self.chunks[0] = chunk[n:]
        return chunk[:n]

    def close(self):
        self.closed = True


def make_transport(*fakes, address=184, unit=1, timeout=2.0):
    """Transport с очередью фиктивных сокетов."""
    queue = list(fakes)
    factory = lambda addr, timeout=timeout: queue.pop(0)
    transport = ModbusTcpTransport("127.0.0.1", 502, timeout, socket_factory=factory)
    return transport, queue


class TestModbusTcpTransport(unittest.TestCase):
    """Byte-level проверки MBAP/FC03."""

    def _read(self, response_bytes, **kwargs):
        fake = FakeSocket([response_bytes])
        transport, _ = make_transport(fake)
        result = transport.read_holding_registers(
            kwargs.get("address", 184), kwargs.get("count", 1), kwargs.get("unit", 1)
        )
        return result, fake

    def test_correct_fc03_response(self):
        result, fake = self._read(fc03_response(registers=(99,)))
        self.assertEqual(result.registers, (99,))
        self.assertEqual(result.transaction_id, 1)
        self.assertEqual(result.unit_id, 1)
        self.assertEqual(result.raw_request_hex, "000100000006010300B80001")
        self.assertEqual(result.raw_response_hex, "0001000000050103020063")
        self.assertEqual(len(fake.sent), 1)  # ровно одна отправка
        self.assertTrue(fake.closed)

    def test_full_response_from_partial_tcp_chunks(self):
        response = fc03_response(registers=(99,))
        fake = FakeSocket([response[:3], response[3:8], response[8:]])
        transport, _ = make_transport(fake)
        self.assertEqual(
            transport.read_holding_registers(184, 1, 1).registers, (99,)
        )

    def test_wrong_transaction_id(self):
        with self.assertRaises(ModbusInvalidResponse):
            self._read(fc03_response(tid=99))

    def test_wrong_unit_id(self):
        with self.assertRaises(ModbusInvalidResponse):
            self._read(fc03_response(unit=2))

    def test_wrong_protocol_id(self):
        with self.assertRaises(ModbusInvalidResponse):
            self._read(fc03_response(proto=1))

    def test_wrong_mbap_length(self):
        with self.assertRaises(ModbusInvalidResponse):
            self._read(fc03_response(length=2))
        with self.assertRaises(ModbusInvalidResponse):
            self._read(fc03_response(length=9999))

    def test_wrong_function_code(self):
        with self.assertRaises(ModbusInvalidResponse):
            self._read(fc03_response(fc=4))

    def test_modbus_exception_response(self):
        with self.assertRaises(ModbusExceptionResponse) as ctx:
            self._read(fc03_response(fc=0x83, registers=(2,)))
        self.assertEqual(ctx.exception.exception_code, 2)

    def test_wrong_byte_count(self):
        response = bytearray(fc03_response(registers=(99,)))
        response[8] = 4  # byte count != 2
        with self.assertRaises(ModbusInvalidResponse):
            self._read(bytes(response))

    def test_wrong_register_count(self):
        response = bytearray(fc03_response(registers=(99,)))
        response[8] = 4  # заявляет 2 регистра, но данных на 1
        with self.assertRaises(ModbusInvalidResponse):
            self._read(bytes(response))

    def test_timeout(self):
        fake = FakeSocket([], timeout=True)
        transport, _ = make_transport(fake)
        with self.assertRaises(ModbusTimeout):
            transport.read_holding_registers(184)
        self.assertTrue(fake.closed)

    def test_disconnect(self):
        fake = FakeSocket([], disconnect=True)
        transport, _ = make_transport(fake)
        with self.assertRaises(ModbusDisconnect):
            transport.read_holding_registers(184)
        self.assertTrue(fake.closed)

    def test_malformed_short_response(self):
        # Короткий заголовок + EOF: это обрыв соединения в ответе.
        with self.assertRaises(ModbusTransportError):
            self._read(b"\x00\x01\x00")

    def test_close_called_on_success_and_error(self):
        for response in (fc03_response(), b""):
            with self.subTest(response=response):
                fake = FakeSocket([response])
                transport, _ = make_transport(fake)
                try:
                    transport.read_holding_registers(184)
                except ModbusTransportErrorAlias:
                    pass
                self.assertTrue(fake.closed)

    def test_recovery_after_failure_uses_new_socket(self):
        bad = FakeSocket([], disconnect=True)
        good = FakeSocket([fc03_response(tid=2, registers=(99,))])
        transport, _ = make_transport(bad, good)
        with self.assertRaises(ModbusDisconnect):
            transport.read_holding_registers(184)
        self.assertEqual(transport.read_holding_registers(184).registers, (99,))
        self.assertEqual(transport.request_count, 2)

    def test_no_write_methods(self):
        transport, _ = make_transport()
        client = ReadOnlyModbusClient(transport)
        for obj in (transport, client):
            for name in ("write_register", "write_registers", "write_coil"):
                with self.assertRaises(AttributeError):
                    getattr(obj, name)


# Алиас для удобства перехвата в тесте закрытия сокета
ModbusTransportErrorAlias = Exception


class TestSocDecoder(unittest.TestCase):
    """Декодирование только подтверждённого регистра 184."""

    def test_0x63_to_99_percent(self):
        from domain.normalizer import normalize_register
        meta = ACTIVE_POLL_REGISTERS[184]
        norm = normalize_register(184, 0x0063, meta)
        self.assertTrue(norm.ok)
        self.assertEqual(norm.normalized_value, 99.0)
        self.assertEqual(meta.unit, "%")

    def test_boundary_values(self):
        from domain.normalizer import normalize_register
        meta = ACTIVE_POLL_REGISTERS[184]
        self.assertEqual(normalize_register(184, 0, meta).normalized_value, 0.0)
        self.assertEqual(normalize_register(184, 100, meta).normalized_value, 100.0)

    def test_invalid_raw_is_not_zero(self):
        from domain.normalizer import normalize_register
        meta = ACTIVE_POLL_REGISTERS[184]
        self.assertFalse(normalize_register(184, -1, meta).ok)
        self.assertFalse(normalize_register(184, 70000, meta).ok)

    def test_only_reg_184_is_active(self):
        self.assertEqual(set(ACTIVE_POLL_REGISTERS), {184})
        self.assertEqual(ACTIVE_POLL_REGISTERS[184].name, "battery_soc")
        self.assertIn(190, REGISTER_MAP)  # остальные остаются в полной карте
        self.assertNotIn(190, ACTIVE_POLL_REGISTERS)


class TestDeyePollingService(unittest.TestCase):
    """Lifecycle, ошибки, stale, SQLite, отсутствие DEMO fallback."""

    def setUp(self):
        self.tmpdir = tempfile.mkdtemp()
        self.db_path = os.path.join(self.tmpdir, "stage20.db")
        self.repo = SQLiteMeasurementRepository(self.db_path)

    def tearDown(self):
        self.repo.close()

    def _service(self, *fakes, interval=0.05):
        transport, queue = make_transport(*fakes)
        client = ReadOnlyModbusClient(transport)
        service = DeyePollingService(
            client, self.repo, unit_id=1, poll_interval=interval
        )
        return service, queue

    def test_successful_cycle(self):
        service, _ = self._service(FakeSocket([fc03_response(registers=(99,))]))
        snap = service.read()
        self.assertEqual(service.state, PollingState.ONLINE)
        self.assertEqual(snap.source, DataSource.DEYE_MODBUS)
        self.assertEqual(snap.values["battery_soc"].normalized_value, 99.0)
        self.assertEqual(snap.values["battery_soc"].raw_value, 99)
        self.assertTrue(snap.connection_ok)
        self.assertFalse(snap.stale)

    def test_multiple_sequential_cycles(self):
        service, _ = self._service(
            FakeSocket([fc03_response(tid=1, registers=(99,))]),
            FakeSocket([fc03_response(tid=2, registers=(98,))]),
        )
        self.assertEqual(service.read().values["battery_soc"].normalized_value, 99.0)
        self.assertEqual(service.read().values["battery_soc"].normalized_value, 98.0)
        self.assertEqual(service.get_status()["success_count"], 2)

    def test_start_stop_and_no_parallel_poller(self):
        service, _ = self._service(FakeSocket([fc03_response()]))
        service.start()
        self.assertTrue(service.is_running())
        service.stop()
        self.assertFalse(service.is_running())
        self.assertEqual(service.state, PollingState.STOPPED)

        # Имитируем занятый цикл — возвращается последний dobr данные как stale
        with service._cycle_lock:
            snap = service.read()
        self.assertTrue(snap.stale)
        self.assertEqual(snap.values["battery_soc"].normalized_value, 99.0)

    def test_timeout_marks_offline_without_fabricated_values(self):
        service, _ = self._service(FakeSocket([], timeout=True))
        snap = service.read()
        self.assertEqual(service.state, PollingState.OFFLINE)
        self.assertEqual(snap.values, {})
        self.assertFalse(snap.connection_ok)
        self.assertIsNotNone(snap.error)
        self.assertEqual(service.get_status()["error_count"], 1)

    def test_stale_data_preserved_on_failure(self):
        service, _ = self._service(
            FakeSocket([fc03_response(registers=(99,))]),
            FakeSocket([], disconnect=True),
        )
        good = service.read()
        failed = service.read()
        self.assertTrue(failed.stale)
        self.assertEqual(failed.values["battery_soc"].normalized_value, 99.0)
        self.assertEqual(failed.values["battery_soc"].timestamp,
                         good.values["battery_soc"].timestamp)
        self.assertFalse(failed.connection_ok)

    def test_recovery_via_mock(self):
        service, _ = self._service(
            FakeSocket([], timeout=True),
            FakeSocket([fc03_response(tid=2, registers=(99,))]),
        )
        self.assertFalse(service.read().connection_ok)
        recovered = service.read()
        self.assertTrue(recovered.connection_ok)
        self.assertFalse(recovered.stale)
        self.assertEqual(service.state, PollingState.ONLINE)

    def test_no_demo_fallback(self):
        service, _ = self._service(FakeSocket([], disconnect=True))
        snap = service.read()
        self.assertEqual(snap.source, DataSource.DEYE_MODBUS)
        self.assertNotEqual(snap.source, DataSource.DEMO_SIMULATOR)

    def test_sqlite_persistence_and_history(self):
        service, _ = self._service(FakeSocket([fc03_response(registers=(99,))]))
        service.read()
        latest = self.repo.get_latest("deye_sg02lp1_am3", "battery_soc")
        self.assertIsNotNone(latest)
        self.assertEqual(latest.normalized_value, 99.0)
        self.assertEqual(latest.source, DataSource.DEYE_MODBUS)
        start = datetime.now(timezone.utc) - timedelta(minutes=1)
        end = datetime.now(timezone.utc) + timedelta(minutes=1)
        history = self.repo.get_history("deye_sg02lp1_am3", "battery_soc", start, end)
        self.assertEqual(len(history), 1)

    def test_config_default_demo_and_explicit_modbus(self):
        self.assertEqual(DataSourceConfig.from_env({}).source, "demo")
        with self.assertRaises(DataSourceConfigError):
            DataSourceConfig.from_env({"SUNLINK_DATA_SOURCE": "deye_modbus"})
        cfg = DataSourceConfig.from_env({
            "SUNLINK_DATA_SOURCE": "deye_modbus",
            "SUNLINK_ENABLE_MODBUS": "1",
        })
        self.assertEqual(cfg.source, "deye_modbus")
        self.assertEqual(cfg.physical_poll_interval_seconds, DEFAULT_PHYSICAL_POLL_INTERVAL)

    def test_conservative_interval_configurable(self):
        cfg = DataSourceConfig.from_env({
            "SUNLINK_DATA_SOURCE": "demo",
            "SUNLINK_PHYSICAL_POLL_INTERVAL": "60",
        })
        self.assertEqual(cfg.physical_poll_interval_seconds, 60.0)


class TestPollingSourceApiIntegration(unittest.TestCase):
    """Существующий API продолжает работать с новым источником."""

    def setUp(self):
        from fastapi.testclient import TestClient
        import deye_gateway

        self.tmpdir = tempfile.mkdtemp()
        self.db_path = os.path.join(self.tmpdir, "api.db")
        self.repo = SQLiteMeasurementRepository(self.db_path)
        transport, _ = make_transport(FakeSocket([fc03_response(registers=(99,))]))
        client = ReadOnlyModbusClient(transport)
        self.service = DeyePollingService(client, self.repo)
        self.gateway = deye_gateway.DeyeGateway(
            deye_gateway.CONFIG, self.repo, self.service
        )
        self.old_gateway = deye_gateway.gateway
        self.old_repo = deye_gateway._repository
        deye_gateway.gateway = self.gateway
        deye_gateway._repository = self.repo
        self.http = TestClient(deye_gateway.app)

    def tearDown(self):
        import deye_gateway
        self.http.close()
        deye_gateway.gateway = self.old_gateway
        deye_gateway._repository = self.old_repo
        self.repo.close()

    def test_live_raw_and_status_use_modbus_source(self):
        live = self.http.get("/api/live/raw").json()
        self.assertEqual(live["source"], DataSource.DEYE_MODBUS.value)
        self.assertEqual(live["batt_soc"], 99.0)
        status = self.http.get("/status").json()
        self.assertEqual(status["source"], DataSource.DEYE_MODBUS.value)
        self.assertTrue(status["connection_status"])

    def test_measurements_and_history(self):
        self.http.get("/api/live/raw")  # один цикл read -> сохранение в SQLite
        latest = self.http.get("/api/measurements/latest").json()
        self.assertEqual(latest["count"], 1)
        self.assertEqual(latest["measurements"][0]["source"], DataSource.DEYE_MODBUS.value)
        start = (datetime.now(timezone.utc) - timedelta(minutes=1)).isoformat().replace("+00:00", "Z")
        end = (datetime.now(timezone.utc) + timedelta(minutes=1)).isoformat().replace("+00:00", "Z")
        history = self.http.get(
            f"/api/measurements/history?parameter=battery_soc&start={start}&end={end}"
        ).json()
        self.assertGreater(history["count"], 0)

    def test_no_write_endpoints(self):
        import deye_gateway
        paths = [r.path for r in deye_gateway.app.routes if hasattr(r, "path")]
        for forbidden in ("/write", "/set-register", "/control"):
            self.assertNotIn(forbidden, paths)


if __name__ == "__main__":
    unittest.main()
