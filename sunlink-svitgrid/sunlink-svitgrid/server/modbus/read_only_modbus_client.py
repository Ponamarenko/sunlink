"""
SunLink — read-only FC03 client (этап 20).

Тонкая обёртка над ModbusTcpTransport сregister-level API. Намеренно НЕ имеет
write-методов: это зафиксировано тестами.
"""

from __future__ import annotations

from typing import List

from modbus.modbus_tcp_transport import (
    Fc03Response,
    ModbusTcpTransport,
)


class ReadOnlyModbusClient:
    """Только чтение holding-регистров через FC03."""

    def __init__(self, transport: ModbusTcpTransport) -> None:
        self._transport = transport

    def read_holding_registers(
        self, address: int, count: int = 1, unit_id: int = 1
    ) -> List[int]:
        response: Fc03Response = self._transport.read_holding_registers(
            address, count, unit_id
        )
        return list(response.registers)

    def read_register(self, address: int, unit_id: int = 1) -> int:
        values = self.read_holding_registers(address, 1, unit_id)
        return values[0]

    def read_with_details(
        self, address: int, count: int = 1, unit_id: int = 1
    ) -> Fc03Response:
        return self._transport.read_holding_registers(address, count, unit_id)

    def close(self) -> None:
        self._transport.close()

    @property
    def request_count(self) -> int:
        return self._transport.request_count


__all__ = ["ReadOnlyModbusClient"]
