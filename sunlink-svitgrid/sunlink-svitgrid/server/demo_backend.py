#!/usr/bin/env python3
"""
SunLink — изолированный DEMO backend для интеграции Backend ↔ Android (этап 12).

НЕ является production-сервером:
- отдельный dev-порт (по умолчанию 8801);
- отдельная тестовая SQLite БД (по умолчанию /tmp/sunlink-stage12-demo.db);
- источник данных явно помечен как DEMO (source=demo_simulator);
- НЕ изменяет production .env, systemd, Redis;
- НЕ подключается к Ebyte, инвертору, HA или production URL.

Использует тот же FastAPI app и Data Layer, что и deye_gateway.py, но с
DEMO-клиентом вместо ModbusTcpClient. Все показания — демонстрационные.
"""

from __future__ import annotations

import argparse
import os
import sys
from datetime import datetime, timedelta, timezone
from typing import Dict

SERVER_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, SERVER_DIR)

import uvicorn  # noqa: E402

import deye_gateway  # noqa: E402
from deye_gateway import DeyeGateway, CONFIG, INVERTER_ID  # noqa: E402
from domain.models import DataSource, MeasurementQuality  # noqa: E402
from domain.normalizer import make_measurement  # noqa: E402
from domain.register_metadata import REGISTER_MAP  # noqa: E402
from storage.sqlite_repository import SQLiteMeasurementRepository  # noqa: E402

# Демонстрационные RAW-значения тех же 9 регистров этапа 7.
# Это НЕ реальные показания инвертора.
DEMO_REGISTERS: Dict[int, int] = {
    90: 1365,    # dc_temperature  -> 36.5 °C (offset -100)
    91: 1424,    # ac_temperature  -> 42.4 °C
    183: 5322,   # battery_voltage -> 53.22 V
    184: 85,     # battery_soc     -> 85 %
    186: 211,    # pv1_power       -> 211 W
    187: 131,    # pv2_power       -> 131 W
    188: 107,    # pv3_power       -> 107 W
    190: 1915,   # battery_power   -> 1915 W (provisional sign)
    191: 361,    # battery_current -> 3.61 A
}

# Несколько точек во времени, чтобы history была непустой сразу после старта.
DEMO_SAMPLES = 4
DEMO_SAMPLE_INTERVAL_MINUTES = 15


def seed_demo_history(repo: SQLiteMeasurementRepository) -> int:
    """Записывает DEMO-измерения в тестовую SQLite (несколько точек)."""
    now = datetime.now(timezone.utc)
    written = 0
    for sample in range(DEMO_SAMPLES):
        ts = now - timedelta(minutes=(DEMO_SAMPLES - 1 - sample) * DEMO_SAMPLE_INTERVAL_MINUTES)
        for address, raw in DEMO_REGISTERS.items():
            metadata = REGISTER_MAP[address]
            m = make_measurement(
                inverter_id=INVERTER_ID,
                register_address=address,
                parameter_name=metadata.name,
                raw_value=raw,
                # Небольшая вариативность во времени, чтобы график не был прямой линией.
                normalized_value=round(raw * metadata.scale + metadata.offset + sample * 0.01, 4),
                unit=metadata.unit,
                timestamp=ts,
                quality=MeasurementQuality.VALID,
                confidence=metadata.confidence,
                source=DataSource.DEMO_SIMULATOR,
                description=f"DEMO sample {sample + 1}/{DEMO_SAMPLES} - not live data",
            )
            repo.save_measurement(m)
            written += 1
    return written


def configure(db_path: str) -> SQLiteMeasurementRepository:
    """Устанавливает ЯВНОЕ DEMO-окружение в глобальные переменные FastAPI app.

    DEMO-режим выбирается только здесь — по умолчанию gateway остаётся
    read-only, но без физического подключения, пока не выбран режим modbus.
    """
    from services.telemetry_sources import DEFAULT_DEMO_REGISTERS, DemoTelemetrySource

    repo = SQLiteMeasurementRepository(db_path)
    demo_source = DemoTelemetrySource(DEFAULT_DEMO_REGISTERS)
    gateway = DeyeGateway(CONFIG, repository=repo, telemetry_source=demo_source)
    deye_gateway.gateway = gateway
    deye_gateway._repository = repo
    return repo


def main() -> None:
    parser = argparse.ArgumentParser(description="SunLink isolated DEMO backend")
    parser.add_argument("--port", type=int, default=8801, help="dev port (not production 8800)")
    parser.add_argument(
        "--db",
        default=os.environ.get("SUNLINK_DEMO_DB", "/tmp/sunlink-stage12-demo.db"),
        help="test-only SQLite path",
    )
    parser.add_argument("--host", default="127.0.0.1", help="bind host")
    parser.add_argument("--no-seed", action="store_true", help="do not seed DEMO history")
    args = parser.parse_args()

    repo = configure(args.db)
    seeded = 0 if args.no_seed else seed_demo_history(repo)

    print("=" * 70)
    print("SunLink ISOLATED DEMO BACKEND (stage 12) - NOT PRODUCTION")
    print("=" * 70)
    print(f"  URL:      http://{args.host}:{args.port}")
    print(f"  DB:       {args.db}")
    print(f"  Source:   {DataSource.DEMO_SIMULATOR.value}")
    print(f"  Seeded:   {seeded} DEMO measurements")
    print("  No inverter / Ebyte / HA / Redis / production .env is touched.")
    print("=" * 70)

    try:
        uvicorn.run(deye_gateway.app, host=args.host, port=args.port, log_level="info")
    finally:
        repo.close()


if __name__ == "__main__":
    main()
