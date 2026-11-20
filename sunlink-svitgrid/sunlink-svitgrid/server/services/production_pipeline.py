"""
SunLink — единый production data pipeline (Stage 27).

Deye -> RS485/LAN adapter -> Modbus TCP (FC03, read-only) -> decoder/Data Layer
     -> SQLite -> API/WebSocket -> Android.

НИКОГДА не импортирует и не запускает симулятор. Физический источник создаётся
только при явном включении в конфигурации; реальный опрос запускается отдельно
и только с разрешения владельца.
"""

from __future__ import annotations

from typing import Optional, Tuple

from data_source_config import DataSourceConfig, DataSourceConfigError, PHYSICAL_SOURCES
from modbus.modbus_tcp_transport import ModbusTcpTransport
from modbus.read_only_modbus_client import ReadOnlyModbusClient
from services.deye_polling_service import (
    DEFAULT_PHYSICAL_POLL_INTERVAL,
    DeyePollingService,
)
from storage.repository import MeasurementRepository

SIMULATION_MODULES = {"demo_backend", "demo_stage7", "demo_stage8", "modbus.mock_client"}


class ProductionPipelineError(RuntimeError):
    """Ошибка конфигурации production pipeline."""


def build_production_poller(
    config: DataSourceConfig,
    repository: Optional[MeasurementRepository] = None,
) -> Tuple[DeyePollingService, ReadOnlyModbusClient, ModbusTcpTransport]:
    """Создаёт единственный read-only poller для физического источника."""
    if config.source not in PHYSICAL_SOURCES:
        raise ProductionPipelineError(
            "production pipeline requires source=deye_modbus (or legacy modbus)"
        )
    if not config.enable_modbus:
        raise ProductionPipelineError(
            "physical Modbus must be explicitly enabled (SUNLINK_ENABLE_MODBUS=1)"
        )
    if not config.modbus_host:
        raise ProductionPipelineError("SUNLINK_MODBUS_HOST is required")
    if config.register_profile != "stage7":
        raise ProductionPipelineError(
            f"unsupported register profile: {config.register_profile}"
        )

    transport = ModbusTcpTransport(
        host=config.modbus_host,
        port=config.modbus_port,
        timeout=config.timeout_seconds,
    )
    client = ReadOnlyModbusClient(transport)
    poller = DeyePollingService(
        client=client,
        repository=repository,
        unit_id=config.unit_id,
        poll_interval=config.physical_poll_interval_seconds or DEFAULT_PHYSICAL_POLL_INTERVAL,
        reconnect_backoff=5.0,
        max_backoff=60.0,
    )
    return poller, client, transport


def attach_to_gateway(gateway, poller: DeyePollingService) -> None:
    """Подключает poller к существующему FastAPI gateway без второго poller."""
    if getattr(gateway, "telemetry_source", None) is not None:
        raise ProductionPipelineError("gateway already has a telemetry source")
    gateway.telemetry_source = poller
    gateway.source = poller.source


__all__ = [
    "ProductionPipelineError",
    "SIMULATION_MODULES",
    "build_production_poller",
    "attach_to_gateway",
]
