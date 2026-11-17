"""
SunPortal Data Layer — mock Modbus client.

Implements ModbusClientInterface for testing without a physical inverter.
Supports configurable register values, delays, timeouts, TCP failures,
Modbus exceptions, per-register errors, and recovery after failure.
"""

from __future__ import annotations

import time
from typing import Dict, Optional

from modbus.client_interface import ModbusClientInterface


class MockModbusClient(ModbusClientInterface):
    """Mock Modbus TCP client for offline testing."""

    def __init__(
        self,
        registers: Optional[Dict[int, int]] = None,
        connect_ok: bool = True,
        read_delay: float = 0.0,
        read_timeout: bool = False,
        tcp_refused: bool = False,
        modbus_exception: bool = False,
        error_registers: Optional[Dict[int, str]] = None,
    ) -> None:
        self._registers = dict(registers or {})
        self._connect_ok = connect_ok
        self._read_delay = read_delay
        self._read_timeout = read_timeout
        self._tcp_refused = tcp_refused
        self._modbus_exception = modbus_exception
        self._error_registers = dict(error_registers or {})
        self._connected = False
        self._last_error: Optional[str] = None

    @property
    def connected(self) -> bool:
        return self._connected

    def connect(self) -> bool:
        self.disconnect()
        self._last_error = None
        if self._tcp_refused:
            self._last_error = "connection refused"
            self._connected = False
            return False
        if not self._connect_ok:
            self._last_error = "connect failed"
            self._connected = False
            return False
        self._connected = True
        return True

    def disconnect(self) -> None:
        self._connected = False

    def read_register(self, address: int) -> Optional[int]:
        if not self._connected:
            self._last_error = "not connected"
            return None

        if self._read_delay > 0:
            time.sleep(self._read_delay)

        if self._read_timeout:
            self._last_error = "modbus timeout"
            self._connected = False
            return None

        if self._modbus_exception:
            self._last_error = "modbus exception"
            self._connected = False
            return None

        if address in self._error_registers:
            self._last_error = self._error_registers[address]
            return None

        if address not in self._registers:
            self._last_error = f"unknown register {address}"
            return None

        self._last_error = None
        return self._registers[address]

    @property
    def last_error(self) -> Optional[str]:
        return self._last_error

    def set_register(self, address: int, value: int) -> None:
        self._registers[address] = value

    def set_tcp_refused(self, value: bool) -> None:
        self._tcp_refused = value

    def set_read_timeout(self, value: bool) -> None:
        self._read_timeout = value

    def set_modbus_exception(self, value: bool) -> None:
        self._modbus_exception = value

    def set_connect_ok(self, value: bool) -> None:
        self._connect_ok = value

    def clear_error_registers(self) -> None:
        self._error_registers.clear()
