"""
SunLink — read-only FC03 client (этап 20, hardened в Stage 35A).

Тонкая обёртка над ModbusTcpTransport с register-level API. Намеренно НЕ имеет
write-методов. Stage 35A: любой ошибочный/неполный ответ превращается в
ModbusReadError, но никогда не в 0 и не в "успешную" телеметрию.
"""

from __future__ import annotations

from typing import Any, List

from modbus.modbus_tcp_transport import (
    Fc03Response,
    ModbusTcpTransport,
)


class ModbusReadError(RuntimeError):
    """Получен некорректный ответ FC03 — значение не может быть использовано."""


class ReadOnlyModbusClient:
    """Только чтение holding-регистров через FC03."""

    def __init__(self, transport: ModbusTcpTransport) -> None:
        self._transport = transport

    def _validate(self, response: Any, count: int, address: int) -> List[int]:
        if response is None:
            raise ModbusReadError(f"reg {address}: empty response")
        is_error = getattr(response, "isError", None)
        if callable(is_error):
            try:
                if is_error():
                    detail = getattr(response, "message", None) or getattr(
                        response, "error", None
                    ) or "modbus exception"
                    raise ModbusReadError(f"reg {address}: {detail}")
            except ModbusReadError:
                raise
            except Exception as exc:  # defensive: isError() itself must not mask data
                raise ModbusReadError(
                    f"reg {address}: isError() failed: {type(exc).__name__}"
                ) from exc
        registers = getattr(response, "registers", None)
        if registers is None:
            raise ModbusReadError(f"reg {address}: response has no registers")
        if not isinstance(registers, (list, tuple)):
            raise ModbusReadError(
                f"reg {address}: registers has invalid type {type(registers).__name__}"
            )
        if len(registers) == 0:
            raise ModbusReadError(f"reg {address}: empty registers list")
        if len(registers) != count:
            raise ModbusReadError(
                f"reg {address}: got {len(registers)} registers, expected {count}"
            )
        for value in registers:
            if not isinstance(value, int) or isinstance(value, bool) or not 0 <= value <= 0xFFFF:
                raise ModbusReadError(
                    f"reg {address}: invalid register value {value!r}"
                )
        return list(registers)

    def read_holding_registers(
        self, address: int, count: int = 1, unit_id: int = 1
    ) -> List[int]:
        response: Fc03Response = self._transport.read_holding_registers(
            address, count, unit_id
        )
        return self._validate(response, count, address)

    def read_register(self, address: int, unit_id: int = 1) -> int:
        values = self.read_holding_registers(address, 1, unit_id)
        return values[0]

    def read_with_details(
        self, address: int, count: int = 1, unit_id: int = 1
    ) -> Fc03Response:
        response = self._transport.read_holding_registers(address, count, unit_id)
        self._validate(response, count, address)
        return response

    def close(self) -> None:
        self._transport.close()

    @property
    def request_count(self) -> int:
        return self._transport.request_count


__all__ = ["ModbusReadError", "ReadOnlyModbusClient"]
