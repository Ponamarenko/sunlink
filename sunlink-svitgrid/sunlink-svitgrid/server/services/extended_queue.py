"""
Stage 42 — очередь дополнительных FC03-заданий для существующего poller.

Нет второго transport'а, второго poller'а и второго потока. Задания
выполняются последовательно внутри уже существующего цикла чтения.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import TYPE_CHECKING, Dict, List, Optional

from data_source_config import DataSourceConfig
from domain.extended_decoder import decode_extended
from domain.extended_register_map import EXTENDED_REGISTERS, production_allowed_registers
from domain.telemetry import TelemetryValue

if TYPE_CHECKING:
    from services.extended_polling import ExtendedPollingConfig


@dataclass(frozen=True)
class ReadJob:
    address: int
    count: int
    group: str
    priority: int
    data_type: str
    confidence: str
    production_eligible: bool
    max_retries: int = 1
    timeout_seconds: float = 3.0

    @property
    def function_code(self) -> int:
        return 3  # FC03 — единственная разрешённая функция чтения


@dataclass
class JobStats:
    attempts: int = 0
    successes: int = 0
    failures: int = 0
    last_error: Optional[str] = None


PRIORITY = {
    "battery": 10,
    "pv": 20,
    "grid": 30,
    "load": 40,
    "inverter": 50,
    "energy": 60,
    "status": 70,
    "warnings": 80,
    "errors": 90,
    "system": 100,
}


def build_extended_jobs(config: "ExtendedPollingConfig") -> List[ReadJob]:
    """Строит allowlist только из production_allowed регистров.

    Произвольные адреса из env или кода не попадают в очередь автоматически —
    для нового регистра требуется явное изменение `production_allowed`.
    """
    allowed = production_allowed_registers()
    if not config.enabled:
        return []
    addresses = config.resolve_addresses()
    jobs: List[ReadJob] = []
    for address in addresses:
        reg = allowed.get(address)
        if reg is None:
            continue
        jobs.append(ReadJob(
            address=address,
            count=reg.register_count,
            group=reg.group,
            priority=PRIORITY.get(reg.group, 999),
            data_type=reg.data_type,
            confidence=reg.confidence,
            production_eligible=True,
            timeout_seconds=3.0,
        ))
    jobs.sort(key=lambda job: (job.priority, job.address))
    return jobs


@dataclass
class ExtendedCycleResult:
    values: Dict[str, TelemetryValue] = field(default_factory=dict)
    errors: List[str] = field(default_factory=list)
    executed: List[int] = field(default_factory=list)


class ExtendedJobQueue:
    """Round-robin очередь, безопасная для одного transport'а."""

    def __init__(self, jobs: List[ReadJob], max_block: int = 8, jobs_per_cycle: int = 2) -> None:
        if max_block < 1 or max_block > 8:
            raise ValueError("max block must be 1..8")
        self.jobs = jobs
        self.max_block = max_block
        self.jobs_per_cycle = max(1, jobs_per_cycle)
        self.cursor = 0
        self.stats: Dict[int, JobStats] = {job.address: JobStats() for job in jobs}

    def next_jobs(self) -> List[ReadJob]:
        if not self.jobs:
            return []
        selected: List[ReadJob] = []
        for _ in range(min(self.jobs_per_cycle, len(self.jobs))):
            selected.append(self.jobs[self.cursor % len(self.jobs)])
            self.cursor = (self.cursor + 1) % len(self.jobs)
        return selected

    def validate_job(self, job: ReadJob) -> Optional[str]:
        if job.function_code != 3:
            return "only FC03 is allowed"
        if not job.production_eligible:
            return "job is not production eligible"
        if job.count < 1 or job.count > self.max_block:
            return f"invalid register count {job.count}"
        if job.address not in production_allowed_registers():
            return "address is not in production allowlist"
        return None

    def decode(self, job: ReadJob, registers: List[int], timestamp: str) -> tuple[Dict[str, TelemetryValue], List[str]]:
        result = decode_extended(job.address, registers)
        if not result.ok:
            return {}, [f"reg {job.address}: {result.error}"]
        return {
            result.parameter: TelemetryValue(
                parameter_name=result.parameter,
                normalized_value=result.normalized,
                unit=result.unit,
                timestamp=timestamp,
                confidence=result.confidence.lower(),
                quality="valid",
                raw_value=result.raw,
                register_address=job.address,
            )
        }, []


__all__ = ["ReadJob", "JobStats", "ExtendedJobQueue", "build_extended_jobs", "PRIORITY"]
