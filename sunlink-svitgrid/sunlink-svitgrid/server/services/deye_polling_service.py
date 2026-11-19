"""
SunLink — управляемый read-only polling engine (этап 20).

Один владелец соединения, один цикл за раз, только FC03 через
ReadOnlyModbusClient, только подтверждённый регистр 184 (ACTIVE_POLL_REGISTERS).
Нет DEMO fallback: при ошибке физического источника выдаётся stale-статус,
а не симуляция. Физический запуск на этом этапе не выполняется.
"""

from __future__ import annotations

import threading
from dataclasses import dataclass, replace
from datetime import datetime, timezone
from enum import Enum
from typing import Dict, List, Optional

from domain.models import (
    ConfidenceLevel,
    DataSource,
    Measurement,
    MeasurementQuality,
)
from domain.normalizer import normalize_register
from domain.register_metadata import ACTIVE_POLL_REGISTERS
from domain.telemetry import (
    TelemetrySnapshot,
    TelemetrySource,
    TelemetryValue,
    utc_now_iso,
)
from modbus.modbus_tcp_transport import ModbusTransportError
from modbus.read_only_modbus_client import ReadOnlyModbusClient
from storage.repository import MeasurementRepository

INVERTER_ID = "deye_sg02lp1_am3"
# Консервативный интервал для физического опроса: 30 с (настраивается).
DEFAULT_PHYSICAL_POLL_INTERVAL = 30.0


class PollingState(str, Enum):
    DISABLED = "disabled"
    CONNECTING = "connecting"
    ONLINE = "online"
    DEGRADED = "degraded"
    OFFLINE = "offline"
    STOPPED = "stopped"


@dataclass
class PollingStats:
    success_count: int = 0
    error_count: int = 0
    last_success_utc: Optional[str] = None
    last_error_utc: Optional[str] = None
    last_error: Optional[str] = None


class DeyePollingService(TelemetrySource):
    """Read-only polling service; реализует контракт TelemetrySource.read()."""

    def __init__(
        self,
        client: ReadOnlyModbusClient,
        repository: Optional[MeasurementRepository] = None,
        inverter_id: str = INVERTER_ID,
        unit_id: int = 1,
        poll_interval: float = DEFAULT_PHYSICAL_POLL_INTERVAL,
        source: DataSource = DataSource.DEYE_MODBUS,
        active_registers: Optional[Dict[int, object]] = None,
    ) -> None:
        if poll_interval <= 0:
            raise ValueError("poll_interval must be positive")
        self._client = client
        self._repository = repository
        self._inverter_id = inverter_id
        self._unit_id = unit_id
        self._poll_interval = poll_interval
        self._source = source
        self._active = dict(active_registers or ACTIVE_POLL_REGISTERS)
        self._state_lock = threading.Lock()
        self._cycle_lock = threading.Lock()
        self._stop_event = threading.Event()
        self._thread: Optional[threading.Thread] = None
        self._state = PollingState.DISABLED
        self._stats = PollingStats()
        self._last_good: Optional[TelemetrySnapshot] = None
        self._connected = False

    # -- TelemetrySource ----------------------------------------------------

    # Data Layer наполняется самим сервисом — gateway не дублирует запись.
    persists_itself = True

    @property
    def source(self) -> DataSource:
        return self._source

    @property
    def connection_ok(self) -> bool:
        return self._connected

    def read(self) -> TelemetrySnapshot:
        """Один цикл опроса. Не parallel: если цикл уже идёт — stale/last good."""
        if not self._cycle_lock.acquire(blocking=False):
            snap = self._last_good
            if snap is not None:
                return replace(
                    snap, stale=True, connection_ok=False,
                    error="poll cycle already running"
                )
            return self._empty_snapshot("poll cycle already running")

        try:
            return self._run_cycle()
        finally:
            self._cycle_lock.release()

    def close(self) -> None:
        self.stop()
        self._client.close()

    # -- lifecycle ----------------------------------------------------------

    def start(self) -> None:
        """Запуск фонового polling. Не делает инвертор online автоматически."""
        with self._state_lock:
            if self._thread is not None and self._thread.is_alive():
                return
            self._stop_event.clear()
            self._state = PollingState.CONNECTING
            self._thread = threading.Thread(
                target=self._poll_loop, name="deye-polling", daemon=True
            )
            self._thread.start()

    def stop(self, timeout: float = 5.0) -> None:
        self._stop_event.set()
        with self._state_lock:
            thread = self._thread
        if thread is not None:
            thread.join(timeout=timeout)
        with self._state_lock:
            self._state = PollingState.STOPPED

    def is_running(self) -> bool:
        thread = self._thread
        return thread is not None and thread.is_alive()

    @property
    def state(self) -> PollingState:
        return self._state

    def get_status(self) -> Dict[str, object]:
        """Минимальный статус источника (без секретов)."""
        with self._state_lock:
            state = self._state.value
            stats = PollingStats(**self._stats.__dict__)
        return {
            "source": self._source.value,
            "state": state,
            "connection_status": self._connected,
            "last_success": stats.last_success_utc,
            "last_error": stats.last_error,
            "error_count": stats.error_count,
            "success_count": stats.success_count,
            "active_registers": sorted(self._active.keys()),
            "poll_interval_seconds": self._poll_interval,
            "stale": self._last_good is not None and not self._connected,
        }

    # -- internal -----------------------------------------------------------

    def _poll_loop(self) -> None:
        while not self._stop_event.is_set():
            self.read()
            self._stop_event.wait(self._poll_interval)

    def _empty_snapshot(self, error: str) -> TelemetrySnapshot:
        return TelemetrySnapshot(
            inverter_id=self._inverter_id,
            source=self._source,
            timestamp=utc_now_iso(),
            connection_ok=False,
            values={},
            error=error,
            stale=self._last_good is not None,
        )

    def _run_cycle(self) -> TelemetrySnapshot:
        ts = utc_now_iso()
        values: Dict[str, TelemetryValue] = {}
        errors: List[str] = []

        for address, meta in sorted(self._active.items()):
            try:
                raw = self._client.read_register(address, self._unit_id)
            except ModbusTransportError as exc:
                errors.append(f"reg {address}: {exc}")
                continue
            except Exception as exc:  # defensive: never fabricate values
                errors.append(f"reg {address}: {type(exc).__name__}")
                continue

            norm = normalize_register(address, raw, meta)
            if not norm.ok:
                errors.append(f"reg {address}: normalization failed")
                continue
            values[meta.name] = TelemetryValue(
                parameter_name=meta.name,
                normalized_value=norm.normalized_value,
                unit=meta.unit,
                timestamp=ts,
                confidence=meta.confidence.value,
                quality=norm.quality.value,
                raw_value=raw,
                register_address=address,
            )

        if errors:
            self._connected = False
            self._stats.error_count += 1
            self._stats.last_error_utc = utc_now_iso()
            self._stats.last_error = "; ".join(errors)

        if values:
            self._connected = True
            self._stats.success_count += 1
            self._stats.last_success_utc = ts
            with self._state_lock:
                self._state = (
                    PollingState.DEGRADED if errors else PollingState.ONLINE
                )
            snapshot = TelemetrySnapshot(
                inverter_id=self._inverter_id,
                source=self._source,
                timestamp=ts,
                connection_ok=True,
                values=values,
                error="; ".join(errors) if errors else None,
                stale=False,
            )
            self._last_good = snapshot
            self._persist(snapshot)
            return snapshot

        with self._state_lock:
            self._state = PollingState.OFFLINE
        if self._last_good is not None:
            # Сохраняем последнее достоверное измерение, но честно помечаем stale.
            return replace(
                self._last_good,
                connection_ok=False,
                stale=True,
                error="; ".join(errors) or "no values",
            )
        return self._empty_snapshot("; ".join(errors) or "no values")

    def _persist(self, snapshot: TelemetrySnapshot) -> None:
        if self._repository is None or not snapshot.values:
            return
        try:
            rows = [
                Measurement(
                    inverter_id=snapshot.inverter_id,
                    register_address=value.register_address,
                    parameter_name=name,
                    raw_value=value.raw_value,
                    normalized_value=value.normalized_value,
                    unit=value.unit,
                    timestamp=datetime.fromisoformat(value.timestamp),
                    quality=MeasurementQuality(value.quality),
                    confidence=ConfidenceLevel(value.confidence),
                    source=self._source,
                    description="read-only FC03 polling (stage 20)",
                )
                for name, value in snapshot.values.items()
            ]
            self._repository.save_measurements_batch(rows)
        except Exception as exc:
            # Ошибка хранения не превращается в фиктивную телеметрию.
            self._stats.last_error = f"storage error: {type(exc).__name__}"


__all__ = [
    "DeyePollingService",
    "PollingState",
    "PollingStats",
    "DEFAULT_PHYSICAL_POLL_INTERVAL",
]
