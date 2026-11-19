"""
SunLink — конфигурация выбора источника телеметрии (этап 16).

Безопасность по умолчанию:
- SUNLINK_DATA_SOURCE не задан  -> demo (без сети);
- demo / mock запускаются без дополнительных условий;
- modbus требует ЯВНОГО SUNLINK_ENABLE_MODBUS=1;
- неизвестное значение -> понятная ошибка;
- production .env не читается и не изменяется.
"""

from __future__ import annotations

import os
from dataclasses import dataclass
from typing import Mapping, Optional

VALID_SOURCES = ("demo", "mock", "modbus")
ENV_SOURCE = "SUNLINK_DATA_SOURCE"
ENV_ENABLE_MODBUS = "SUNLINK_ENABLE_MODBUS"


class DataSourceConfigError(ValueError):
    """Понятная ошибка конфигурации источника."""


@dataclass(frozen=True)
class DataSourceConfig:
    source: str
    enable_modbus: bool = False
    modbus_host: Optional[str] = None
    modbus_port: int = 502
    unit_id: int = 1
    timeout_seconds: float = 2.0
    poll_interval_seconds: float = 5.0
    register_profile: str = "stage7"

    @classmethod
    def from_env(cls, environ: Optional[Mapping[str, str]] = None) -> "DataSourceConfig":
        env = environ if environ is not None else os.environ
        source = env.get(ENV_SOURCE, "demo").strip().lower()

        if source not in VALID_SOURCES:
            raise DataSourceConfigError(
                f"Неизвестное значение {ENV_SOURCE}={source!r}. "
                f"Допустимые значения: {', '.join(VALID_SOURCES)}"
            )

        enable = env.get(ENV_ENABLE_MODBUS, "").strip().lower() in ("1", "true", "yes")
        if source == "modbus" and not enable:
            raise DataSourceConfigError(
                "Физический Modbus-источник требует явного включения: "
                f"установите {ENV_ENABLE_MODBUS}=1. "
                "Автоматическое подключение к оборудованию запрещено."
            )

        try:
            port = int(env.get("SUNLINK_MODBUS_PORT", "502"))
            unit_id = int(env.get("SUNLINK_MODBUS_UNIT_ID", "1"))
            timeout = float(env.get("SUNLINK_MODBUS_TIMEOUT", "2.0"))
            interval = float(env.get("SUNLINK_POLL_INTERVAL", "5.0"))
        except (TypeError, ValueError) as exc:
            raise DataSourceConfigError(f"Некорректное числовое значение конфигурации: {exc}") from exc

        if not (1 <= port <= 65535):
            raise DataSourceConfigError("SUNLINK_MODBUS_PORT должен быть 1..65535")
        if not (1 <= unit_id <= 247):
            raise DataSourceConfigError("SUNLINK_MODBUS_UNIT_ID должен быть 1..247")
        if timeout <= 0 or interval <= 0:
            raise DataSourceConfigError("timeout и poll interval должны быть положительными")

        return cls(
            source=source,
            enable_modbus=enable,
            modbus_host=env.get("SUNLINK_MODBUS_HOST"),
            modbus_port=port,
            unit_id=unit_id,
            timeout_seconds=timeout,
            poll_interval_seconds=interval,
            register_profile=env.get("SUNLINK_REGISTER_PROFILE", "stage7"),
        )


__all__ = ["DataSourceConfig", "DataSourceConfigError", "VALID_SOURCES"]
