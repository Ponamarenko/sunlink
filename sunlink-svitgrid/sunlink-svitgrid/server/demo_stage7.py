#!/usr/bin/env python3
"""
SunPortal Data Layer — stage 7 demonstration.

Uses ONLY test data from stage 4 (physical read-only FC03 poll via Ebyte)
and stage 6 (Home Assistant log). No connection to Ebyte, Solarman, or the
physical inverter is made. Data is saved to a temporary SQLite database and
deleted when the script ends.

Output is labeled as HISTORICAL DIAGNOSTIC EXAMPLES — not live readings.
"""

from __future__ import annotations

import os
import sys
import tempfile
from datetime import datetime, timezone

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from domain.models import (  # noqa: E402
    Measurement,
    MeasurementQuality,
    ConfidenceLevel,
    DataSource,
)
from domain.register_metadata import REGISTER_MAP
from domain.normalizer import normalize_register
from storage.sqlite_repository import SQLiteMeasurementRepository

INVERTER_ID = "2507245705"  # from HA log (serial), diagnostic example only


def _build_measurements(source_label: str, samples: dict[int, int],
                        base_ts: datetime) -> list[Measurement]:
    """Normalize raw stage-4/HA samples into Measurements."""
    measurements: list[Measurement] = []
    for i, (addr, raw) in enumerate(sorted(samples.items())):
        meta = REGISTER_MAP[addr]
        result = normalize_register(addr, raw, meta)
        ts = datetime.fromisoformat(base_ts.isoformat())  # distinct instant
        ts = ts.replace(microsecond=ts.microsecond + i)
        measurements.append(
            Measurement(
                inverter_id=INVERTER_ID,
                register_address=addr,
                parameter_name=meta.name,
                raw_value=raw,
                normalized_value=result.normalized_value,
                unit=meta.unit,
                timestamp=ts,
                quality=MeasurementQuality.VALID if result.ok else MeasurementQuality.ERROR,
                confidence=meta.confidence,
                source=DataSource.MODBUS_REGISTER,
                description=f"{source_label} — historical diagnostic example",
            )
        )
    return measurements


def main() -> None:
    print("=" * 70)
    print("SunPortal Data Layer — stage 7 demonstration")
    print("=" * 70)
    print()
    print("SOURCE DATA (historical diagnostic examples — NOT live readings):")
    print("  Stage 4: physical FC03 poll via Ebyte 192.168.25.15:502")
    print("           2026-11-17T13:32:27 UTC")
    print("  Stage 6: Home Assistant log (Solarman 192.168.25.14:8899)")
    print("           single snapshot, time not synchronized with stage 4")
    print()

    stage4_raw = {
        183: 5322,   # battery_voltage -> 53.22 V
        184: 85,     # battery_soc     -> 85.0 %
        186: 93,     # pv1_power       -> 93.0 W
        187: 57,     # pv2_power       -> 57.0 W
        188: 49,     # pv3_power       -> 49.0 W
        190: 434,    # battery_power   -> 434.0 W (provisional sign)
        191: 81,     # battery_current -> 0.81 A
        90: 1365,    # dc_temperature  -> 36.5 °C (offset −100, provisional)
        91: 1424,    # ac_temperature  -> 42.4 °C (offset −100, provisional)
    }

    ha_raw = {
        183: 5302,   # battery_voltage -> 53.02 V
        184: 85,     # battery_soc     -> 85.0 %
        186: 211,    # pv1_power       -> 211.0 W
        187: 131,    # pv2_power       -> 131.0 W
        188: 107,    # pv3_power       -> 107.0 W
        190: 1915,   # battery_power   -> 1915.0 W (provisional sign)
        191: 361,    # battery_current -> 3.61 A
    }

    stage4_ts = datetime(2026, 11, 17, 13, 32, 27, tzinfo=timezone.utc)
    ha_ts = datetime(2026, 11, 17, 14, 0, 0, tzinfo=timezone.utc)

    stage4_ms = _build_measurements("stage-4 Ebyte", stage4_raw, stage4_ts)
    ha_ms = _build_measurements("stage-6 HA", ha_raw, ha_ts)
    all_ms = stage4_ms + ha_ms

    print("NORMALIZED MEASUREMENTS:")
    print(f"  total: {len(all_ms)} measurements")
    print()

    with tempfile.NamedTemporaryFile(suffix=".db", delete=False) as f:
        db_path = f.name

    try:
        repo = SQLiteMeasurementRepository(db_path)
        saved = repo.save_measurements_batch(all_ms)
        print(f"PERSISTED TO SQLITE: {saved} measurements (temporary DB: {db_path})")
        print(f"  schema version: {repo.schema_version()}")
        print()

        print("LATEST VALUES PER PARAMETER:")
        latest = repo.get_all_latest(INVERTER_ID)
        for m in latest:
            src = m.description.split(" — ")[0]
            conf = m.confidence.value.upper()
            print(
                f"  {m.parameter_name:<20} {m.normalized_value:>8} {m.unit:<4} "
                f"raw={m.raw_value:>6} [{conf}] src={src}"
            )
        print()

        print("HISTORY FOR battery_voltage (stage 4 + HA):")
        start = datetime(2026, 11, 17, 13, 0, 0, tzinfo=timezone.utc)
        end = datetime(2026, 11, 17, 15, 0, 0, tzinfo=timezone.utc)
        hist = repo.get_history(INVERTER_ID, "battery_voltage", start, end)
        for m in hist:
            print(f"  {m.timestamp.isoformat()}  {m.normalized_value} {m.unit}  raw={m.raw_value}")
        print()

        repo.close()
    finally:
        os.unlink(db_path)

    print("NOTE: All values are historical diagnostic examples from stage 4 and")
    print("stage 6. They are NOT live readings from the physical inverter.")
    print("Temporary SQLite database has been deleted.")
    print("=" * 70)


if __name__ == "__main__":
    main()
