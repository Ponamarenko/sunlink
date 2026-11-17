#!/usr/bin/bin/env python3
"""
Диагностический скрипт для одного (READ-ONLY) опроса инвертора
Deye SUN-12K-SG02LP1-EU-AM3 через Ebyte NA111-E.

БЕЗОПАСНОСТЬ:
  * Только Modbus Function Code 03 (Read Holding Registers).
  * Никаких функций записи (05/06/15/16) — в этом скрипте их нет.
  * Один цикл чтения, без polling и фоновых потоков.
  * После опроса соединение закрывается, скрипт завершается.

Использование (после подтверждения остановки другого Modbus-мастера,
например клиента Home Assistant, и определения Slave ID):

    python3 readonly_diagnostic.py --host 192.168.25.15 --port 502 \
        --slave-id 1 --timeout 2

Параметры можно изменить через аргументы командной строки.
"""

from __future__ import annotations

import argparse
import json
import sys
import time
from datetime import datetime, timezone
from typing import Any, Dict

from deye_readonly_adapter import DeyeReadonlyAdapter, SOURCE_MEASURED


def format_registry(rv) -> Dict[str, Any]:
    """Представить RegisterValue в виде словаря для вывода/лога."""
    return {
        "address": rv.address,
        "name": rv.name,
        "data_type": rv.data_type,
        "scale": rv.scale,
        "unit": rv.unit,
        "source": rv.source,
        "confirmed": rv.confirmed,
        "raw": rv.raw,
        "value": rv.value,
        "error": rv.error,
        "timestamp": rv.timestamp,
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Read-only diagnostic poll of Deye SG02LP1-AM3 via Ebyte NA111-E"
    )
    parser.add_argument("--host", default="192.168.25.15", help="Ebyte NA111-E IP")
    parser.add_argument("--port", type=int, default=502, help="Modbus TCP port")
    parser.add_argument("--slave-id", type=int, default=1, help="Modbus slave/unit ID")
    parser.add_argument("--timeout", type=float, default=2.0, help="Socket timeout (s)")
    args = parser.parse_args()

    print("=" * 72)
    print("DEYE SUN-12K-SG02LP1-EU-AM3 — READ-ONLY DIAGNOSTIC POLL")
    print("=" * 72)
    print(f"Host        : {args.host}")
    print(f"Port        : {args.port}")
    print(f"Slave/Unit  : {args.slave_id}")
    print(f"Timeout     : {args.timeout} s")
    print(f"Started at  : {datetime.now(timezone.utc).isoformat()}")
    print("Function    : FC 03 Read Holding Registers only")
    print("-" * 72)

    adapter = DeyeReadonlyAdapter(
        host=args.host,
        port=args.port,
        slave_id=args.slave_id,
        timeout=args.timeout,
    )

    # 1. Подключение (read-only)
    t_connect_start = time.perf_counter()
    connected = adapter.connect()
    t_connect_ms = (time.perf_counter() - t_connect_start) * 1000.0

    print(f"Connection  : {'OK' if connected else 'FAILED'} ({t_connect_ms:.1f} ms)")
    if not connected:
        print(f"Error       : {adapter.last_error}")
        print("-" * 72)
        print("ABORTED: cannot read inverter registers without a connection.")
        adapter.disconnect()
        return 1

    # 2. Один цикл чтения всех зарегистрированных регистров
    t_read_start = time.perf_counter()
    snapshot = adapter.read_all()
    t_read_ms = (time.perf_counter() - t_read_start) * 1000.0

    print(f"Read cycle  : OK ({t_read_ms:.1f} ms)")
    print(f"Last error  : {snapshot.error or 'none'}")
    print("-" * 72)

    # 3. Вывод регистров
    print(f"{'Addr':>6}  {'Name':<32} {'Type':<9} {'Raw':>10}  {'Value':>14}  {'Unit':<6} {'Src':<12} {'Conf'}")
    print("-" * 72)
    # Порядок вывода: по возрастанию адреса; DWORD-пары — под именем старшего (low).
    ordered = sorted(snapshot.registers.values(), key=lambda r: r.address)
    for rv in ordered:
        raw = rv.raw if rv.raw is not None else "-"
        value = rv.value if rv.value is not None else "-"
        print(
            f"{rv.address:>6}  {rv.name:<32} {rv.data_type:<9} {str(raw):>10}  "
            f"{str(value):>14}  {rv.unit:<6} {rv.source:<12} {str(rv.confirmed):<5}"
        )

    print("-" * 72)
    # 4. Сводка (измеренное vs расчётное)
    print("SUMMARY:")
    for key in (
        "p_sun", "p_sun_source",
        "p_batt", "p_batt_source",
        "battery_soc", "battery_voltage",
        "p_grid_measured", "p_grid_source",
        "p_grid_estimated", "p_grid_estimated_source",
        "dc_temperature", "ac_temperature",
        "total_pv_production", "daily_pv_production",
        "total_battery_charge", "total_battery_discharge",
    ):
        print(f"  {key:<32} = {snapshot.summary.get(key)}")

    print("-" * 72)
    print(f"Connection OK : {snapshot.connection_ok}")
    print(f"Last success  : {snapshot.last_successful_read or 'none'}")
    print(f"Finished at   : {datetime.now(timezone.utc).isoformat()}")

    # 5. Закрыть соединение — один раз, без фоновых процессов
    adapter.disconnect()
    print(f"Connection closed: {not adapter.connected}")
    print("=" * 72)
    print("DIAGNOSTIC COMPLETE — single read-only cycle, no polling left running.")
    print("=" * 72)

    # Код возврата: 0 если хоть один регистр прочитан успешно, иначе 1
    any_ok = any(rv.ok for rv in snapshot.registers.values())
    return 0 if (connected and any_ok) else 1


if __name__ == "__main__":
    sys.exit(main())
