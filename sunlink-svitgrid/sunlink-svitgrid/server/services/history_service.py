"""
Stage 46 — реальные временные ряды из сохранённых измерений.

Только данные, которые действительно есть в SQLite. Пропуски остаются null
и не соединяются линией в Android-графике.
"""

from __future__ import annotations

from datetime import datetime, timedelta, timezone
from typing import Any, Dict, List, Optional

from domain.register_metadata import REGISTER_MAP
from domain.extended_register_map import EXTENDED_REGISTERS
from storage.repository import MeasurementRepository

PERIODS: Dict[str, Dict[str, Any]] = {
    "24h": {"days": 1, "bucket_seconds": 300},
    "7d": {"days": 7, "bucket_seconds": 3600},
    "30d": {"days": 30, "bucket_seconds": 21600},
}

KNOWN_PARAMETERS = sorted(
    {m.name for m in REGISTER_MAP.values()}
    | {r.name for r in EXTENDED_REGISTERS.values()}
    | {"pv_total_power"}
)


def _parse_iso(value: str) -> datetime:
    dt = datetime.fromisoformat(value.replace("Z", "+00:00"))
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    return dt.astimezone(timezone.utc)


def build_series(
    repository: MeasurementRepository,
    inverter_id: str,
    parameter: str,
    period: str,
    end: Optional[str] = None,
) -> Dict[str, Any]:
    if parameter not in KNOWN_PARAMETERS:
        raise ValueError(f"unknown parameter: {parameter}")
    if period not in PERIODS:
        raise ValueError(f"unknown period: {period}")
    config = PERIODS[period]
    end_dt = _parse_iso(end) if end else datetime.now(timezone.utc)
    start_dt = end_dt - timedelta(days=config["days"])
    bucket = config["bucket_seconds"]

    measurements = repository.get_history(inverter_id, parameter, start_dt, end_dt)
    buckets: Dict[int, List[float]] = {}
    unit = ""
    for measurement in measurements:
        if measurement.normalized_value is None:
            continue
        timestamp = measurement.timestamp.astimezone(timezone.utc)
        index = int(timestamp.timestamp()) // bucket
        buckets.setdefault(index, []).append(float(measurement.normalized_value))
        unit = measurement.unit or unit

    first_index = int(start_dt.timestamp()) // bucket
    last_index = int(end_dt.timestamp()) // bucket
    count = max(1, min(last_index - first_index + 1, 1000))
    points: List[Dict[str, Any]] = []
    missing = 0
    for offset in range(count):
        index = first_index + offset
        ts = datetime.fromtimestamp(index * bucket, tz=timezone.utc).isoformat()
        values = buckets.get(index)
        if not values:
            points.append({"timestamp": ts, "value": None, "gap": True})
            missing += 1
        else:
            points.append({
                "timestamp": ts,
                "value": sum(values) / len(values),
                "gap": False,
                "samples": len(values),
            })

    return {
        "inverter_id": inverter_id,
        "parameter": parameter,
        "period": period,
        "start": start_dt.isoformat(),
        "end": end_dt.isoformat(),
        "interval_seconds": bucket,
        "unit": unit,
        "count": len(points),
        "available_points": len(points) - missing,
        "missing_points": missing,
        "points": points,
    }


__all__ = ["PERIODS", "KNOWN_PARAMETERS", "build_series"]
