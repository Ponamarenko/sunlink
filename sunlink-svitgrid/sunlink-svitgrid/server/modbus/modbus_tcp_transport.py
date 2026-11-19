"""
SunLink — read-only Modbus TCP transport (этап 20).

Реализует MBAP-кадрирование и СТРОГО одну функцию FC03. Каждый запрос
использует отдельное TCP-соединение: это гарантирует предсказуемое закрытие
сокета, отсутствие «зависших» соединений после timeout/disconnect и простую
проверку полного ответа по MBAP Length. Нет persistent connection, нет retry.

Сокет вводится через socket_factory — тесты подменяют его фиктивным транспортом
и НЕ обращаются к физическому оборудованию.
"""

from __future__ import annotations

import binascii
import socket
import struct
from dataclasses import dataclass
from typing import Callable, List, Optional, Tuple

FC_READ_HOLDING_REGISTERS = 0x03
MAX_RESPONSE_BYTES = 512
MAX_QUANTITY = 125


class ModbusTransportError(Exception):
    """Базовая ошибка Modbus TCP transport."""


class ModbusTimeout(ModbusTransportError):
    """Ответ не получен в течение timeout."""


class ModbusDisconnect(ModbusTransportError):
    """Соединение закрыто до завершения ответа."""


class ModbusInvalidResponse(ModbusTransportError):
    """Ответ не прошёл проверку MBAP/PDU."""


class ModbusExceptionResponse(ModbusTransportError):
    """Получен Modbus exception response (FC | 0x80)."""

    def __init__(self, exception_code: int, function_code: int) -> None:
        self.exception_code = exception_code
        self.function_code = function_code
        super().__init__(
            f"Modbus exception fc=0x{function_code:02X} code=0x{exception_code:02X}"
        )


@dataclass(frozen=True)
class Fc03Response:
    transaction_id: int
    unit_id: int
    address: int
    quantity: int
    registers: Tuple[int, ...]
    raw_request_hex: str
    raw_response_hex: str


def _recv_exact(sock: socket.socket, count: int, timeout: float) -> bytes:
    """Читает ровно count байт; один recv() может вернуть меньше."""
    chunks: List[bytes] = []
    remaining = count
    while remaining > 0:
        try:
            chunk = sock.recv(remaining)
        except socket.timeout as exc:
            if not chunks:
                raise ModbusTimeout("no response before timeout") from exc
            raise ModbusInvalidResponse("incomplete response (timeout)") from exc
        if not chunk:
            if not chunks:
                raise ModbusDisconnect("connection closed before response")
            raise ModbusDisconnect("connection closed during response")
        chunks.append(chunk)
        remaining -= len(chunk)
    return b"".join(chunks)


class ModbusTcpTransport:
    """Только FC03, один запрос = одно соединение, без retry."""

    def __init__(
        self,
        host: str,
        port: int = 502,
        timeout: float = 2.0,
        socket_factory: Optional[Callable] = None,
        max_response_bytes: int = MAX_RESPONSE_BYTES,
    ) -> None:
        self.host = host
        self.port = port
        self.timeout = timeout
        self._socket_factory = socket_factory or socket.create_connection
        self.max_response_bytes = max_response_bytes
        self._next_tid = 1
        self.request_count = 0

    def _transaction_id(self) -> int:
        tid = self._next_tid
        self._next_tid = (self._next_tid % 0xFFFF) + 1
        return tid

    def read_holding_registers(
        self, address: int, count: int = 1, unit_id: int = 1
    ) -> Fc03Response:
        """Один FC03-запрос. Никаких повторов при ошибке."""
        if not 0 <= address <= 0xFFFF:
            raise ModbusInvalidResponse(f"address {address} out of range")
        if not 1 <= count <= MAX_QUANTITY:
            raise ModbusInvalidResponse(f"quantity {count} out of range")
        if not 1 <= unit_id <= 247:
            raise ModbusInvalidResponse(f"unit id {unit_id} out of range")

        tid = self._transaction_id()
        pdu = struct.pack(">BHH", FC_READ_HOLDING_REGISTERS, address, count)
        mbap = struct.pack(">HHHB", tid, 0x0000, len(pdu) + 1, unit_id)
        request = mbap + pdu
        request_hex = binascii.hexlify(request).decode().upper()
        self.request_count += 1

        sock = None
        try:
            sock = self._socket_factory((self.host, self.port), timeout=self.timeout)
            sock.settimeout(self.timeout)
            sock.sendall(request)  # ровно одна отправка

            header = _recv_exact(sock, 7, self.timeout)
            r_tid, proto_id, length, r_unit = struct.unpack(">HHHB", header)
            if proto_id != 0:
                raise ModbusInvalidResponse(f"protocol id {proto_id} != 0")
            if length < 2 or length > self.max_response_bytes:
                raise ModbusInvalidResponse(f"MBAP length {length} invalid")
            body = _recv_exact(sock, length - 1, self.timeout)
            response = header + body
            response_hex = binascii.hexlify(response).decode().upper()

            if r_tid != tid:
                raise ModbusInvalidResponse(f"transaction id {r_tid} != {tid}")
            if r_unit != unit_id:
                raise ModbusInvalidResponse(f"unit id {r_unit} != {unit_id}")
            if not body:
                raise ModbusInvalidResponse("empty PDU")
            fc = body[0]
            if fc & 0x80:
                code = body[1] if len(body) > 1 else 0
                raise ModbusExceptionResponse(code, fc & 0x7F)
            if fc != FC_READ_HOLDING_REGISTERS:
                raise ModbusInvalidResponse(f"function code 0x{fc:02X} != 0x03")
            if len(body) < 2:
                raise ModbusInvalidResponse("missing byte count")
            byte_count = body[1]
            if byte_count != 2 * count:
                raise ModbusInvalidResponse(
                    f"byte count {byte_count} != {2 * count}"
                )
            if len(body) != 2 + byte_count:
                raise ModbusInvalidResponse("PDU length mismatch")
            registers = tuple(
                struct.unpack(">H", body[2 + i * 2 : 4 + i * 2])[0]
                for i in range(count)
            )
            return Fc03Response(
                transaction_id=r_tid,
                unit_id=r_unit,
                address=address,
                quantity=count,
                registers=registers,
                raw_request_hex=request_hex,
                raw_response_hex=response_hex,
            )
        finally:
            if sock is not None:
                try:
                    sock.close()
                except OSError:
                    pass

    def close(self) -> None:
        """Нет постоянного соединения — закрывать нечего."""
        return None


__all__ = [
    "ModbusTcpTransport",
    "Fc03Response",
    "ModbusTransportError",
    "ModbusTimeout",
    "ModbusDisconnect",
    "ModbusInvalidResponse",
    "ModbusExceptionResponse",
    "FC_READ_HOLDING_REGISTERS",
]
