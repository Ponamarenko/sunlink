"""
SunPortal Data Layer - Modbus client interface.

Defines the contract for reading holding registers. The real
DeyeReadonlyAdapter and the mock client both satisfy this interface.
"""

from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Optional


class ModbusClientInterface(ABC):
    """Read-only Modbus client contract (FC 03 only)."""

    @property
    @abstractmethod
    def connected(self) -> bool:
        """Return True if the Modbus connection is currently open."""

    @abstractmethod
    def connect(self) -> bool:
        """Open the Modbus TCP connection. Return True on success."""

    @abstractmethod
    def disconnect(self) -> None:
        """Close the Modbus connection."""

    @abstractmethod
    def read_register(self, address: int) -> Optional[int]:
        """
        Read one holding register (FC 03, count=1).

        Returns the raw 16-bit value, or None on error.
        """

    @property
    @abstractmethod
    def last_error(self) -> Optional[str]:
        """Return the last error message, or None."""
