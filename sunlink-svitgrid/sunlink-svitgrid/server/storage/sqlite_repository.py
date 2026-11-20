"""
SunPortal Data Layer — SQLite repository implementation.

Stores measurement history in a versioned SQLite database. Independent of
Home Assistant; Redis is not a required dependency. The DB path is passed
via configuration (constructor argument), never hardcoded.
"""

from __future__ import annotations

import os
import sqlite3
import threading
from datetime import datetime, timezone
from typing import List, Optional

from domain.models import (
    Measurement,
    MeasurementQuality,
    ConfidenceLevel,
    DataSource,
)
from storage.repository import MeasurementRepository

SCHEMA_VERSION = 1


def _thread_safe(fn):
    """Сериализует доступ к общему SQLite-соединению между потоками."""
    from functools import wraps

    @wraps(fn)
    def wrapper(self, *args, **kwargs):
        with self._lock:
            return fn(self, *args, **kwargs)

    return wrapper

_SCHEMA = f"""
CREATE TABLE IF NOT EXISTS measurements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    inverter_id TEXT NOT NULL,
    register_address INTEGER,
    parameter_name TEXT NOT NULL,
    raw_value INTEGER,
    normalized_value REAL,
    unit TEXT NOT NULL,
    timestamp TEXT NOT NULL,   -- ISO-8601 UTC
    quality TEXT NOT NULL,
    confidence TEXT NOT NULL,
    source TEXT NOT NULL,
    description TEXT DEFAULT ""
);

CREATE INDEX IF NOT EXISTS idx_measurements_inverter_param
    ON measurements(inverter_id, parameter_name);
CREATE INDEX IF NOT EXISTS idx_measurements_timestamp
    ON measurements(timestamp);
CREATE INDEX IF NOT EXISTS idx_measurements_inverter_param_ts
    ON measurements(inverter_id, parameter_name, timestamp);
"""


def _row_to_measurement(row: tuple) -> Measurement:
    return Measurement(
        inverter_id=row[0],
        register_address=row[1],
        parameter_name=row[2],
        raw_value=row[3],
        normalized_value=row[4],
        unit=row[5],
        timestamp=datetime.fromisoformat(row[6]),
        quality=MeasurementQuality(row[7]),
        confidence=ConfidenceLevel(row[8]),
        source=DataSource(row[9]),
        description=row[10] or "",
    )


class SQLiteMeasurementRepository(MeasurementRepository):
    """SQLite-backed measurement repository."""

    def __init__(self, db_path: str) -> None:
        self.db_path = db_path
        parent = os.path.dirname(os.path.abspath(db_path))
        if parent and not os.path.exists(parent):
            os.makedirs(parent, exist_ok=True)
        self._conn: Optional[sqlite3.Connection] = None
        # FastAPI/TestClient может вызывать репозиторий из другого потока.
        # Одно соединение + блокировка делают репозиторий потокобезопасным.
        self._lock = threading.RLock()
        self._connect()
        self._init_schema()

    def _connect(self) -> None:
        if self._conn is not None:
            return
        self._conn = sqlite3.connect(self.db_path, check_same_thread=False)
        self._conn.row_factory = sqlite3.Row
        self._conn.execute("PRAGMA foreign_keys = ON")

    def _init_schema(self) -> None:
        assert self._conn is not None
        version = int(self._conn.execute("PRAGMA user_version").fetchone()[0])
        if version == 0:
            # Новая БД: создаём схему и фиксируем версию.
            self._conn.executescript(_SCHEMA)
            self._conn.execute(f"PRAGMA user_version = {SCHEMA_VERSION}")
            self._conn.commit()
        elif version < SCHEMA_VERSION:
            # Аддитивная миграция с backup; данные не удаляются.
            from storage.migrations import migrate_database
            self._conn.close()
            self._conn = None
            migrate_database(self.db_path, SCHEMA_VERSION)
            self._connect()

    @_thread_safe
    def close(self) -> None:
        if self._conn is not None:
            self._conn.close()
            self._conn = None

    def __enter__(self) -> "SQLiteMeasurementRepository":
        return self

    def __exit__(self, exc_type, exc_val, exc_tb) -> None:
        self.close()

    def _ensure_open(self) -> sqlite3.Connection:
        if self._conn is None:
            raise RuntimeError("repository is closed")
        return self._conn

    @_thread_safe
    def save_measurement(self, measurement: Measurement) -> None:
        self.save_measurements_batch([measurement])

    @_thread_safe
    def save_measurements_batch(self, measurements: List[Measurement]) -> int:
        conn = self._ensure_open()
        if not measurements:
            return 0
        rows = [
            (
                m.inverter_id,
                m.register_address,
                m.parameter_name,
                m.raw_value,
                m.normalized_value,
                m.unit,
                m.timestamp.isoformat(),
                m.quality.value,
                m.confidence.value,
                m.source.value,
                m.description,
            )
            for m in measurements
        ]
        try:
            with conn:
                conn.executemany(
                    """
                    INSERT INTO measurements
                    (inverter_id, register_address, parameter_name, raw_value,
                     normalized_value, unit, timestamp, quality, confidence,
                     source, description)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    rows,
                )
        except sqlite3.Error as exc:
            raise RuntimeError(f"failed to persist measurements: {exc}") from exc
        return len(measurements)

    @_thread_safe
    def get_latest(
        self, inverter_id: str, parameter_name: str
    ) -> Optional[Measurement]:
        conn = self._ensure_open()
        try:
            row = conn.execute(
                """
                SELECT inverter_id, register_address, parameter_name, raw_value,
                       normalized_value, unit, timestamp, quality, confidence,
                       source, description
                FROM measurements
                WHERE inverter_id = ? AND parameter_name = ?
                ORDER BY timestamp DESC, id DESC
                LIMIT 1
                """,
                (inverter_id, parameter_name),
            ).fetchone()
        except sqlite3.Error as exc:
            raise RuntimeError(f"failed to read latest measurement: {exc}") from exc
        return _row_to_measurement(tuple(row)) if row else None

    @_thread_safe
    def get_history(
        self,
        inverter_id: str,
        parameter_name: str,
        start: datetime,
        end: datetime,
    ) -> List[Measurement]:
        conn = self._ensure_open()
        try:
            rows = conn.execute(
                """
                SELECT inverter_id, register_address, parameter_name, raw_value,
                       normalized_value, unit, timestamp, quality, confidence,
                       source, description
                FROM measurements
                WHERE inverter_id = ? AND parameter_name = ?
                  AND timestamp >= ? AND timestamp <= ?
                ORDER BY timestamp ASC, id ASC
                """,
                (
                    inverter_id,
                    parameter_name,
                    start.isoformat(),
                    end.isoformat(),
                ),
            ).fetchall()
        except sqlite3.Error as exc:
            raise RuntimeError(f"failed to read history: {exc}") from exc
        return [_row_to_measurement(tuple(r)) for r in rows]

    @_thread_safe
    def get_all_latest(self, inverter_id: str) -> List[Measurement]:
        conn = self._ensure_open()
        try:
            rows = conn.execute(
                """
                SELECT m.inverter_id, m.register_address, m.parameter_name,
                       m.raw_value, m.normalized_value, m.unit, m.timestamp,
                       m.quality, m.confidence, m.source, m.description
                FROM measurements m
                INNER JOIN (
                    SELECT parameter_name, MAX(timestamp) AS max_ts
                    FROM measurements
                    WHERE inverter_id = ?
                    GROUP BY parameter_name
                ) latest ON m.parameter_name = latest.parameter_name
                           AND m.timestamp = latest.max_ts
                           AND m.inverter_id = ?
                ORDER BY m.parameter_name
                """,
                (inverter_id, inverter_id),
            ).fetchall()
        except sqlite3.Error as exc:
            raise RuntimeError(f"failed to read all latest: {exc}") from exc
        return [_row_to_measurement(tuple(r)) for r in rows]

    def schema_version(self) -> int:
        conn = self._ensure_open()
        row = conn.execute("PRAGMA user_version").fetchone()
        return int(row[0]) if row else 0
