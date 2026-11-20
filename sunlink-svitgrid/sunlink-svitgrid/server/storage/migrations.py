"""
SunLink — ADDITIVE SQLite migrations (Stage 27).

Правила:
- никогда не удаляем таблицы/столбцы/строки;
- перед любой миграцией создаётся файловая копия БД;
- user_version никогда не понижается;
- схема версии 1 уже содержит все поля, необходимые Data Layer.
"""

from __future__ import annotations

import os
import shutil
import sqlite3
from datetime import datetime, timezone
from typing import Dict, Callable

SCHEMA_VERSION = 1

# Номер версии -> функция миграции (пока нет изменений схемы > 1).
MIGRATIONS: Dict[int, Callable[[sqlite3.Connection], None]] = {}


def backup_database(db_path: str) -> str:
    """Создаёт копию БД перед миграцией; возвращает путь к копии."""
    if not os.path.isfile(db_path):
        raise FileNotFoundError(db_path)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")
    backup = f"{db_path}.bak.{stamp}"
    shutil.copy2(db_path, backup)
    return backup


def current_version(db_path: str) -> int:
    if not os.path.isfile(db_path):
        return 0
    conn = sqlite3.connect(db_path)
    try:
        return int(conn.execute("PRAGMA user_version").fetchone()[0])
    finally:
        conn.close()


def migrate_database(db_path: str, target: int = SCHEMA_VERSION) -> str:
    """Безопасная аддитивная миграция. Возвращает 'noop'/'ok'/'backed-up'."""
    if not os.path.isfile(db_path):
        return "noop"
    version = current_version(db_path)
    if version >= target:
        return "noop"          # никогда не понижаем версию
    backup_database(db_path)

    conn = sqlite3.connect(db_path)
    try:
        for step in range(version, target):
            fn = MIGRATIONS.get(step)
            if fn is not None:
                fn(conn)
        conn.execute(f"PRAGMA user_version = {target}")
        conn.commit()
    finally:
        conn.close()
    return "ok"


__all__ = ["SCHEMA_VERSION", "MIGRATIONS", "backup_database", "current_version", "migrate_database"]
