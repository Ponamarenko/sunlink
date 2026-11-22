"""
Stage 41 — конфигурация расширенного опроса.

Включение expand polling по умолчанию ВЫКЛЮЧЕНО. Даже при явном включении
опрашиваются только регистры с `production_allowed=True` (сейчас — 184).
Это НЕ меняет работающий production poller: новый poller создаётся только
явным вызовом build_extended_poller() вне работающего процесса.
"""

from __future__ import annotations

import os
from dataclasses import dataclass
from typing import Dict, Mapping, Optional, Tuple

from data_source_config import DataSourceConfig
from domain.extended_register_map import (
    EXTENDED_REGISTERS,
    build_read_plan,
    production_allowed_registers,
)
from domain.models import ConfidenceLevel
from domain.register_metadata import RegisterMetadata
from modbus.modbus_tcp_transport import ModbusTcpTransport
from modbus.read_only_modbus_client import ReadOnlyModbusClient
from services.deye_polling_service import DeyePollingService
from services.extended_queue import ExtendedJobQueue, build_extended_jobs

ENV_EXTENDED = "SUNLINK_EXTENDED_POLLING"
ENV_GROUPS = "SUNLINK_EXTENDED_GROUPS"
ENV_ADDRESSES = "SUNLINK_EXTENDED_ADDRESSES"
ENV_MAX_BLOCK = "SUNLINK_EXTENDED_MAX_BLOCK"
ENV_INTERVAL = "SUNLINK_EXTENDED_INTERVAL"
ENV_DRY_RUN = "SUNLINK_EXTENDED_DRY_RUN"
ENV_JOBS_PER_CYCLE = "SUNLINK_EXTENDED_JOBS_PER_CYCLE"


@dataclass(frozen=True)
class ExtendedPollingConfig:
    enabled: bool = False
    groups: Tuple[str, ...] = ()
    addresses: Tuple[int, ...] = ()
    max_block: int = 8
    interval_seconds: float = 30.0
    dry_run: bool = False
    jobs_per_cycle: int = 4

    @classmethod
    def from_env(cls, environ: Optional[Mapping[str, str]] = None) -> "ExtendedPollingConfig":
        env = environ if environ is not None else os.environ
        enabled = env.get(ENV_EXTENDED, "0").strip().lower() in ("1", "true", "yes")
        groups = tuple(
            g.strip() for g in env.get(ENV_GROUPS, "").split(",") if g.strip()
        )
        raw_addresses = env.get(ENV_ADDRESSES, "").strip()
        addresses: Tuple[int, ...] = ()
        if raw_addresses:
            try:
                addresses = tuple(int(a.strip()) for a in raw_addresses.split(",") if a.strip())
            except ValueError as exc:
                raise ValueError(f"invalid {ENV_ADDRESSES}: {exc}") from exc
        try:
            max_block = int(env.get(ENV_MAX_BLOCK, "8"))
            interval = float(env.get(ENV_INTERVAL, "30"))
        except ValueError as exc:
            raise ValueError(f"invalid extended polling numeric value: {exc}") from exc
        if not 1 <= max_block <= 16:
            raise ValueError("max block must be 1..16")
        if interval <= 0:
            raise ValueError("interval must be positive")
        dry_run = env.get(ENV_DRY_RUN, "0").strip().lower() in ("1", "true", "yes")
        jobs_per_cycle = int(env.get(ENV_JOBS_PER_CYCLE, "4"))
        if not 1 <= jobs_per_cycle <= 8:
            raise ValueError("jobs per cycle must be 1..8")
        return cls(enabled, groups, addresses, max_block, interval, dry_run, jobs_per_cycle)

    def resolve_addresses(self) -> Tuple[int, ...]:
        """Адресы плана; каждый адрес обязан иметь production_allowed=True."""
        allowed = production_allowed_registers()
        if self.addresses:
            requested = [a for a in self.addresses if a in allowed]
        elif self.groups:
            requested = [
                a for a, r in EXTENDED_REGISTERS.items()
                if r.group in self.groups and a in allowed
            ]
        else:
            requested = list(allowed.keys())
        return tuple(sorted(set(requested)))

    def plan(self):
        return build_read_plan(
            enabled_addresses=list(self.resolve_addresses()),
            max_block=self.max_block,
        )


def _to_register_metadata(address: int) -> RegisterMetadata:
    reg = EXTENDED_REGISTERS[address]
    return RegisterMetadata(
        address=reg.address,
        name=reg.name,
        data_type=reg.data_type,
        scale=reg.scale,
        unit=reg.unit,
        confidence=ConfidenceLevel.PROVISIONAL if reg.confidence != "PHYSICALLY_VERIFIED" else ConfidenceLevel.CORROBORATED,
        formula=f"{reg.data_type} × {reg.scale}" + (f" − {reg.offset}" if reg.offset else ""),
        offset=reg.offset,
        is_signed=reg.is_signed,
        description=reg.description,
        physical_status=reg.confidence,
        candidate_scale=reg.candidate_scale,
        production_allowed=reg.production_allowed,
    )


def build_extended_poller(
    config: DataSourceConfig,
    extended: ExtendedPollingConfig,
    repository=None,
    existing_poller: Optional[DeyePollingService] = None,
) -> Optional[DeyePollingService]:
    """Подключает расширенные FC03-задания к существующему poller'у.

    При `extended.enabled=False` ничего не меняет. При включении требует
    существующий poller: создание второго transport'а/мастера запрещено.
    """
    if not extended.enabled:
        return None
    if existing_poller is None:
        raise RuntimeError(
            "extended polling requires the existing production poller; "
            "creating a second Modbus master is forbidden"
        )
    if config.source not in ("deye_modbus", "modbus") or not config.enable_modbus:
        raise RuntimeError("extended polling requires explicitly enabled physical Modbus source")
    jobs = build_extended_jobs(extended)
    if not jobs:
        raise RuntimeError("no production_allowed addresses resolved")
    existing_poller.attach_extended_queue(
        ExtendedJobQueue(jobs, max_block=extended.max_block, jobs_per_cycle=extended.jobs_per_cycle),
        dry_run=extended.dry_run,
    )
    return existing_poller


__all__ = ["ExtendedPollingConfig", "build_extended_poller", "ENV_EXTENDED"]
