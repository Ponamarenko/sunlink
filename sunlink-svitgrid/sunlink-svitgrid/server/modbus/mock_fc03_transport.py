"""
SunLink — mock FC03 (Read Holding Registers) transport (этап 16).

Полностью программный: никаких TCP-подключений и физических запросов.
Поддерживает отказы, необходимые для тестирования real-data pipeline.
Только FC03; write-методы намеренно отсутствуют.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Dict, List, Optional


class Fc03Error(Exception):
    """Базовая ошибка mock-транспорта."""


class Fc03Timeout(Fc03Error):
    pass


class Fc03ConnectionRefused(Fc03Error):
    pass


class Fc03ModbusException(Fc03Error):
    pass


class Fc03InvalidUnit(Fc03Error):
    pass


@dataclass
class Fc03Result:
    """Ответ FC03; isError() совместим с pymodbus API."""

    registers: List[int]
    error: Optional[str] = None

    def isError(self) -> bool:  # noqa: N802
        return self.error is not None


class MockFc03Transport:
    """Настраиваемый mock-транспорт только для чтения регистров."""

    def __init__(
        self,
        registers: Dict[int, int],
        unit_id: int = 1,
        connect_ok: bool = True,
    ) -> None:
        self.registers = dict(registers)
        self.unit_id = unit_id
        self.connect_ok = connect_ok
        self.connected = False
        self.read_calls: List[tuple] = []
        self.connect_calls = 0
        # Режимы отказов (включаются тестами).
        self.fail_timeout = False
        self.fail_connection = False
        self.fail_modbus_exception = False
        self.wrong_unit_id: Optional[int] = None
        self.invalid_length_at: Optional[int] = None
        self.missing_addresses: set[int] = set()

    def connect(self) -> bool:
        self.connect_calls += 1
        if self.fail_connection or not self.connect_ok:
            self.connected = False
            return False
        self.connected = True
        return True

    def close(self) -> None:
        self.connected = False

    def read_holding_registers(self, address: int, count: int = 1, device_id: int = 1):
        """Только FC03. Возвращает Fc03Result или выбрасывает Fc03Error."""
        self.read_calls.append((address, count, device_id))
        if not self.connected:
            raise Fc03ConnectionRefused("mock connection closed")
        if self.fail_connection:
            self.connected = False
            raise Fc03ConnectionRefused("mock TCP refused")
        if self.fail_timeout:
            raise Fc03Timeout("mock timeout")
        if self.fail_modbus_exception:
            raise Fc03ModbusException("mock Modbus exception")
        if self.wrong_unit_id is not None and device_id != self.wrong_unit_id:
            raise Fc03InvalidUnit(f"unit id {device_id} != {self.wrong_unit_id}")
        if address in self.missing_addresses:
            return Fc03Result([], error=f"register {address} unavailable")
        raw = self.registers.get(address)
        if raw is None:
            return Fc03Result([], error=f"register {address} not present")
        values = [raw]
        if self.invalid_length_at == address:
            values = []  # неверная длина ответа
        if len(values) != count:
            return Fc03Result(values, error="invalid response length")
        return Fc03Result(values)

    # Явная гарантия отсутствия write-пути.
    def __getattr__(self, name: str):
        if name.startswith("write") or name in {"write_register", "write_registers"}:
            raise AttributeError(f"write operation {name!r} is not supported (read-only)")
        raise AttributeError(name)


__all__ = [
    "MockFc03Transport",
    "Fc03Result",
    "Fc03Error",
    "Fc03Timeout",
    "Fc03ConnectionRefused",
    "Fc03ModbusException",
    "Fc03InvalidUnit",
]
