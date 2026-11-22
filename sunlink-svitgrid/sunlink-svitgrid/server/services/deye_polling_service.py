"""
SunLink — управляемый read-only polling engine (этап 20).

Один владелец соединения, один цикл за раз, только FC03 через
ReadOnlyModbusClient, только подтверждённый регистр 184 (ACTIVE_POLL_REGISTERS).
Нет DEMO fallback: при ошибке физического источника выдаётся stale-статус,
а не симуляция. Физический запуск на этом этапе не выполняется.
"""

from __future__ import annotations

import math
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
from services.extended_queue import ExtendedCycleResult, ExtendedJobQueue

INVERTER_ID = "deye_sg02lp1_am3"
# Консервативный интервал для физического опроса: 30 с (настраивается).
DEFAULT_PHYSICAL_POLL_INTERVAL = 30.0
# Ограниченный backoff при потере связи; без бесконечных retry.
DEFAULT_RECONNECT_BACKOFF = 5.0
DEFAULT_MAX_BACKOFF = 60.0

# Реестр запущенных poller — только один на инвертор на процесс.
_ACTIVE_POLLERS: set[str] = set()
_POLLERS_LOCK = threading.Lock()


class PollerAlreadyRunning(RuntimeError):
    """Второй poller для того же инвертора запрещён."""


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
    reconnect_count: int = 0
    last_success_utc: Optional[str] = None
    last_error_utc: Optional[str] = None
    last_error: Optional[str] = None


_CONFIDENCE_MAP = {
    "physically_verified": "corroborated",
    "documented": "corroborated",
    "corroborated": "corroborated",
    "verified": "verified",
    "model_compatible": "provisional",
    "provisional": "provisional",
    "calculated": "provisional",
    "unverified": "unverified",
}


def _telemetry_confidence(value: str) -> str:
    """Приводит статус регистра к корректному ConfidenceLevel."""
    return _CONFIDENCE_MAP.get(str(value).lower(), "unverified")


def _measurement_confidence(value: str) -> ConfidenceLevel:
    return ConfidenceLevel(_telemetry_confidence(value))


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
        reconnect_backoff: float = DEFAULT_RECONNECT_BACKOFF,
        max_backoff: float = DEFAULT_MAX_BACKOFF,
        extended_queue: Optional[ExtendedJobQueue] = None,
        extended_dry_run: bool = False,
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
        self._backoff = max(0.1, reconnect_backoff)
        self._max_backoff = max(self._backoff, max_backoff)
        self._current_backoff = self._backoff
        self._registered = False
        # Stage 42: очередь дополнительных FC03-заданий в том же цикле/transport'е.
        self._extended_queue = extended_queue
        self._extended_enabled = extended_queue is not None
        self._extended_values: Dict[str, TelemetryValue] = {}
        self._extended_dry_run = extended_dry_run
        self._extended_last_cycle: Dict[str, object] = {
            "enabled": self._extended_enabled,
            "jobs": len(extended_queue.jobs) if extended_queue else 0,
            "executed": [],
            "errors": [],
            "dry_run": extended_dry_run,
        }

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

    def attach_extended_queue(
        self, queue: ExtendedJobQueue, dry_run: bool = False
    ) -> None:
        """Подключает очередь к ЭТОМУ poller'у — новый transport не создаётся."""
        if self._extended_queue is not None:
            raise RuntimeError("extended queue is already attached")
        self._extended_queue = queue
        self._extended_enabled = True
        self._extended_dry_run = dry_run
        self._extended_last_cycle = {
            "enabled": True,
            "jobs": len(queue.jobs),
            "executed": [],
            "errors": [],
            "dry_run": dry_run,
        }

    def start(self) -> None:
        """Запуск фонового polling. Не делает инвертор online автоматически."""
        with _POLLERS_LOCK:
            if self._inverter_id in _ACTIVE_POLLERS:
                raise PollerAlreadyRunning(
                    f"poller for inverter {self._inverter_id} already running"
                )
            _ACTIVE_POLLERS.add(self._inverter_id)
            self._registered = True
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
        with _POLLERS_LOCK:
            if self._registered:
                _ACTIVE_POLLERS.discard(self._inverter_id)
                self._registered = False
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
            "reconnect_count": stats.reconnect_count,
            "active_registers": sorted(self._active.keys()),
            "poll_interval_seconds": self._poll_interval,
            "stale": self._last_good is not None and not self._connected,
            "extended_polling": dict(self._extended_last_cycle),
        }

    # -- internal -----------------------------------------------------------

    def _poll_loop(self) -> None:
        while not self._stop_event.is_set():
            was_connected = self._connected
            self.read()
            if self._connected:
                if not was_connected:
                    self._stats.reconnect_count += 1
                self._current_backoff = self._backoff
                delay = self._poll_interval
            else:
                with self._state_lock:
                    self._state = PollingState.CONNECTING
                delay = self._current_backoff
                self._current_backoff = min(self._current_backoff * 2, self._max_backoff)
            self._stop_event.wait(delay)

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
        # Последние корректные расширенные значения сохраняются между циклами.
        values: Dict[str, TelemetryValue] = dict(self._extended_values)
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

        if self._extended_enabled and self._extended_queue is not None:
            extended = self._run_extended_jobs(ts)
            values.update(extended.values)
            errors.extend(extended.errors)
            self._extended_last_cycle = {
                "enabled": True,
                "jobs": len(self._extended_queue.jobs),
                "executed": extended.executed,
                "errors": extended.errors,
                "dry_run": False,
            }

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

    def _run_extended_jobs(self, timestamp: str) -> ExtendedCycleResult:
        """Выполняет несколько FC03-заданий через ТОТ ЖЕ client/transport.

        Называется только из `_run_cycle`, который уже держит `_cycle_lock`.
       Никакого параллельного чтения и второго соединения нет.
        """
        result = ExtendedCycleResult()
        queue = self._extended_queue
        if queue is None or self._extended_dry_run:
            return result
        for job in queue.next_jobs():
            if self._stop_event.is_set():
                break
            stats = queue.stats[job.address]
            stats.attempts += 1
            reason = queue.validate_job(job)
            if reason is not None:
                stats.failures += 1
                stats.last_error = reason
                result.errors.append(f"reg {job.address}: {reason}")
                continue
            registers = None
            last_error: Optional[str] = None
            for attempt in range(job.max_retries + 1):
                try:
                    registers = self._client.read_holding_registers(
                        job.address, job.count, self._unit_id
                    )
                    break
                except ModbusTransportError as exc:
                    last_error = str(exc)
                except Exception as exc:  # defensive: never fabricate values
                    last_error = type(exc).__name__
                if self._stop_event.is_set():
                    break
            if registers is None:
                stats.failures += 1
                stats.last_error = last_error
                result.errors.append(f"reg {job.address}: {last_error}")
                continue
            values, decode_errors = queue.decode(job, registers, timestamp)
            result.values.update(values)
            self._extended_values.update(values)
            result.errors.extend(decode_errors)
            if decode_errors:
                stats.failures += 1
                stats.last_error = "; ".join(decode_errors)
            else:
                stats.successes += 1
                stats.last_error = None
                result.executed.append(job.address)
        return result

    def _persist(self, snapshot: TelemetrySnapshot) -> None:
        if self._repository is None or not snapshot.values:
            return
        try:
            rows: list[Measurement] = []
            for name, value in snapshot.values.items():
                numeric = value.normalized_value
                if numeric is None or not math.isfinite(float(numeric)):
                    continue
                if value.quality not in ("valid", "stale"):
                    continue
                rows.append(Measurement(
                    inverter_id=snapshot.inverter_id,
                    register_address=value.register_address,
                    parameter_name=name,
                    raw_value=value.raw_value,
                    normalized_value=numeric,
                    unit=value.unit,
                    timestamp=datetime.fromisoformat(value.timestamp),
                    quality=MeasurementQuality(value.quality),
                    confidence=_measurement_confidence(value.confidence),
                    source=self._source,
                    description=value.verification or "read-only FC03 polling",
                ))
            # Расчётная сумма PV — только из реально прочитанных трёх входов.
            pv_values = [
                snapshot.values[n].normalized_value
                for n in ("pv1_power", "pv2_power", "pv3_power")
                if n in snapshot.values and snapshot.values[n].normalized_value is not None
            ]
            if len(pv_values) == 3 and all(math.isfinite(float(v)) for v in pv_values):
                from domain.models import DataSource
                rows.append(Measurement(
                    inverter_id=snapshot.inverter_id,
                    register_address=None,
                    parameter_name="pv_total_power",
                    raw_value=None,
                    normalized_value=float(sum(pv_values)),
                    unit="W",
                    timestamp=datetime.fromisoformat(snapshot.timestamp),
                    quality=MeasurementQuality.VALID,
                    confidence=ConfidenceLevel.PROVISIONAL,
                    source=DataSource.CALCULATED,
                    description="SUM(pv1_power,pv2_power,pv3_power)",
                ))
            self._repository.save_measurements_batch(rows)
        except Exception as exc:
            # Ошибка хранения не останавливает poller и не подменяет телеметрию.
            self._stats.last_error = f"storage error: {type(exc).__name__}"


__all__ = [
    "DeyePollingService",
    "PollingState",
    "PollingStats",
    "PollerAlreadyRunning",
    "DEFAULT_PHYSICAL_POLL_INTERVAL",
]
