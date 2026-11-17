
"""
SunPortal Data Layer — Modbus polling service.

Manages the polling cycle:
Modbus TCP -> DeyeReadonlyAdapter -> Polling Service -> Normalizer -> SQLite Repository.

Responsibilities:
  * Sequential polling of stage-7 registers (FC 03 only, via DeyeReadonlyAdapter).
  * Managed polling interval and timeouts.
  * Normalization of RAW values via domain normalizer.
  * Persistence via MeasurementRepository.
  * Inverter state tracking (online/offline/degraded/unknown).
  * Reconnect with bounded backoff after failures.
  * Explicit start/stop — no background polling on import or test startup.

No Modbus write operations (FC 05/06/15/16) are ever issued.
"""

from __future__ import annotations

import threading
import time
from datetime import datetime, timezone
from typing import Callable, Dict, List, Optional

from deye_readonly_adapter import DeyeReadonlyAdapter, RegisterValue
from domain.models import (
    ConfidenceLevel,
    DataSource,
    Measurement,
    MeasurementQuality,
)
from domain.normalizer import normalize_register, NormalizationResult
from domain.register_metadata import REGISTER_MAP
from services.inverter_state import InverterState, ConnectionStatus
from services.polling_config import PollingConfig
from storage.repository import MeasurementRepository

# Stage-7 register set (sequential read order).
STAGE_7_REGISTERS: List[int] = sorted(REGISTER_MAP.keys())


class PollingResult:
    """Outcome of a single polling cycle."""

    def __init__(
        self,
        success: bool,
        measurements: List[dict],
        errors: Dict[int, str],
        duration_ms: float,
        inverter_state: InverterState,
    ) -> None:
        self.success = success
        self.measurements = measurements
        self.errors = errors
        self.duration_ms = duration_ms
        self.inverter_state = inverter_state


class ModbusPollingService:
    """
    Sequential Modbus polling service.

    The adapter is responsible for reading (FC 03 only). This service owns
    the schedule, sequencing, normalization, persistence and state tracking.
    """

    def __init__(
        self,
        adapter: DeyeReadonlyAdapter,
        repository: MeasurementRepository,
        config: Optional[PollingConfig] = None,
        inverter_state: Optional[InverterState] = None,
    ) -> None:
        self._adapter = adapter
        self._repository = repository
        self._config = config or PollingConfig()
        self._state = inverter_state or InverterState(
            inverter_id=self._config.inverter_id,
            data_freshness_seconds=self._config.data_freshness_seconds,
        )

        self._stop_event = threading.Event()
        self._poll_thread: Optional[threading.Thread] = None
        self._lock = threading.Lock()
        self._last_result: Optional[PollingResult] = None

    # -- lifecycle ----------------------------------------------------------

    def start(self) -> None:
        """Start the polling loop in a background thread.

        Safe to call multiple times — subsequent calls are no-ops while
        already running. No polling starts on import.
        """
        with self._lock:
            if self._poll_thread is not None and self._poll_thread.is_alive():
                return
            self._stop_event.clear()
            self._poll_thread = threading.Thread(
                target=self._poll_loop, name="modbus-polling", daemon=True
            )
            self._poll_thread.start()

    def stop(self, timeout: float = 5.0) -> None:
        """Stop the polling loop and wait for the thread to finish."""
        self._stop_event.set()
        with self._lock:
            thread = self._poll_thread
        if thread is not None:
            thread.join(timeout=timeout)

    def is_running(self) -> bool:
        """Return True if the polling loop is currently active."""
        with self._lock:
            thread = self._poll_thread
        return thread is not None and thread.is_alive()

    def poll_once(self) -> PollingResult:
        """Execute a single sequential polling cycle (blocking)."""
        return self._run_cycle()

    def get_state(self) -> InverterState:
        """Return the current inverter state."""
        return self._state

    def get_last_result(self) -> Optional[PollingResult]:
        """Return the result of the last polling cycle."""
        return self._last_result

    # -- internal -----------------------------------------------------------

    def _poll_loop(self) -> None:
        """Background polling loop. Runs until stop() is called."""
        while not self._stop_event.is_set():
            self._run_cycle()
            if self._config.polling_interval > 0:
                self._stop_event.wait(self._config.polling_interval)

    def _run_cycle(self) -> PollingResult:
        """Run one sequential poll cycle across all stage-7 registers."""
        start = time.perf_counter()
        measurements: List[dict] = []
        errors: Dict[int, str] = {}

        # Connect (reconnect if needed).
        if not self._adapter.connected:
            ok = self._adapter.connect()
            if not ok:
                err = self._adapter.last_error or "connect failed"
                self._state.mark_failed_poll(err)
                duration_ms = (time.perf_counter() - start) * 1000.0
                result = PollingResult(False, [], errors, duration_ms, self._state)
                self._last_result = result
                return result

        # Sequential register reads.
        for addr in STAGE_7_REGISTERS:
            meta = REGISTER_MAP[addr]
            ts = datetime.now(timezone.utc)

            rv: RegisterValue = self._adapter.read_register(addr)
            if rv.error is not None or rv.raw is None:
                errors[addr] = rv.error or "read error"
                continue

            # Normalize raw value into engineering value.
            norm: NormalizationResult = normalize_register(
                addr, rv.raw, meta, quality=MeasurementQuality.VALID
            )

            measurements.append(
                {
                    "inverter_id": self._state.inverter_id,
                    "register_address": addr,
                    "parameter_name": meta.name,
                    "raw_value": rv.raw,
                    "normalized_value": norm.normalized_value,
                    "unit": meta.unit,
                    "timestamp": ts.isoformat(),
                    "quality": "valid" if norm.ok else "error",
                    "confidence": meta.confidence.value,
                    "source": "modbus_register",
                    "description": meta.description,
                    "formula": norm.formula,
                }
            )

        # Persist all successful measurements in one transaction.
        storage_error: Optional[str] = None
        if measurements:
            try:
                db_measurements = [
                    Measurement(
                        inverter_id=m["inverter_id"],
                        register_address=m["register_address"],
                        parameter_name=m["parameter_name"],
                        raw_value=m["raw_value"],
                        normalized_value=m["normalized_value"],
                        unit=m["unit"],
                        timestamp=datetime.fromisoformat(m["timestamp"]),
                        quality=MeasurementQuality(m["quality"]),
                        confidence=ConfidenceLevel(m["confidence"]),
                        source=DataSource(m["source"]),
                        description=m["description"],
                    )
                    for m in measurements
                ]
                self._repository.save_measurements_batch(db_measurements)
            except Exception as exc:  # noqa: BLE001
                storage_error = str(exc)
                errors["_storage"] = storage_error

        # Guaranteed connection close after every cycle (read-only diagnostic safety).
        self._adapter.disconnect()

        duration_ms = (time.perf_counter() - start) * 1000.0

        has_register_errors = len(errors) > 0 and "_storage" not in errors
        has_storage_error = "_storage" in errors

        if has_storage_error and not has_register_errors:
            self._state.mark_degraded(storage_error or "storage error")
        elif has_register_errors:
            self._state.mark_degraded(
                "register errors: " + ", ".join(str(k) for k in errors if k != "_storage")
            )
        else:
            self._state.mark_successful_poll(
                duration_ms,
                measurement=measurements[-1].copy() if measurements else None,
            )

        result = PollingResult(
            success=(not has_register_errors and not has_storage_error),
            measurements=measurements,
            errors=errors,
            duration_ms=duration_ms,
            inverter_state=self._state,
        )
        self._last_result = result
        return result
