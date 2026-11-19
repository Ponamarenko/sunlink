#!/usr/bin/env python3
"""
Гибкий сервер для Deye SUN-12K-SG02LP1-EU-AM3 инвертора.
Read-only production API, интегрированный с Data Layer (этап 10).

Только Function Code 03 (Read Holding Registers). Никаких функций записи
(05/06/15/16) — в этом сервере их нет. Регистры, нормализация и хранение
отвечают компонентам Data Layer (domain.register_metadata, domain.normalizer,
storage.sqlite_repository).
"""

import asyncio
import logging
import json
import time
from datetime import datetime, timezone
from typing import Dict, List, Optional, Any
from dataclasses import dataclass, asdict
from pymodbus.client import ModbusTcpClient
from pymodbus.exceptions import ModbusIOException
from fastapi import FastAPI, HTTPException, WebSocket, WebSocketDisconnect, Query
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
from pydantic import BaseModel

# Data Layer (этапы 7–8)
from domain.register_metadata import REGISTER_MAP, RegisterMetadata
from domain.normalizer import (
    normalize_register,
    normalize_to_reading,
    make_measurement,
    NormalizationResult,
)
from domain.models import (
    MeasurementQuality,
    ConfidenceLevel,
    DataSource,
    Measurement,
    NormalizedReading,
)
from storage.sqlite_repository import SQLiteMeasurementRepository

# Настройка логирования
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Инвертор
INVERTER_ID = "deye_sg02lp1_am3"

# Конфигурация (упрощённый — только read-only, нет write-параметров)
CONFIG = {
    "modbus": {
        "host": "192.168.25.15",  # Deye inverter (Modbus TCP через Ebyte NA111-E)
        "port": 502,
        "timeout": 2,
        "slave_id": 1
    },
    "server": {
        "host": "0.0.0.0",
        "port": 8800
    },
    "storage": {
        "db_path": "storage/measurements.db"
    }
}


class DeyeGateway:
    """Основной шлюз для работы с Deye инвертором (строго read-only).

    Читает регистры из domain.register_metadata.REGISTER_MAP (9 регистров этапа 7),
    нормализует через domain.normalizer и сохраняет через storage.repository.
    """

    def __init__(
        self,
        config: Dict,
        repository: Optional[SQLiteMeasurementRepository] = None,
        telemetry_source=None,  # Optional[TelemetrySource] — этап 16
    ) -> None:
        self.config = config
        self.register_map = REGISTER_MAP  # из Data Layer, не инлайновый
        self.client = None
        self.last_data: Dict[str, Any] = {}
        self.connection_status = False
        self.repository = repository
        # Единый источник телеметрии (этап 16). Если задан, read_all_registers()
        # и get_system_status() используют его вместо прямого Modbus-клиента.
        self.telemetry_source = telemetry_source
        self._last_snapshot = None
        self.source: DataSource = (
            telemetry_source.source if telemetry_source is not None
            else DataSource.MODBUS_REGISTER
        )

    # ------------------------------------------------------------------
    # Connection
    # ------------------------------------------------------------------

    async def connect(self) -> bool:
        """Подключение к инвертору (Modbus TCP client, read-only)."""
        try:
            self.client = ModbusTcpClient(
                host=self.config["modbus"]["host"],
                port=self.config["modbus"]["port"],
                timeout=self.config["modbus"]["timeout"],
            )
            connected = self.client.connect()
            if connected:
                self.connection_status = True
                logger.info("Connected to Deye inverter")
                return True
            else:
                self.connection_status = False
                logger.error("Failed to connect to Deye inverter")
                return False
        except Exception as e:
            self.connection_status = False
            logger.error(f"Connection error: {e}")
            return False

    async def disconnect(self) -> None:
        """Отключение от инвертора."""
        if self.client:
            self.client.close()
            self.connection_status = False
            logger.info("Disconnected from Deye inverter")

    # ------------------------------------------------------------------
    # Read-only register access
    # ------------------------------------------------------------------

    async def read_register(self, address: int) -> Optional[NormalizationResult]:
        """Чтение одного регистра с нормализацией через Data Layer.

        Возвращает NormalizationResult (raw + decoded + normalized + confidence).
        При успешном чтении и наличии репозитория — сохраняет измерение в SQLite.
        """

        if not self.connection_status:
            ok = await self.connect()
            if not ok:
                return None

        metadata: Optional[RegisterMetadata] = self.register_map.get(address)
        if not metadata:
            logger.error(f"Unknown register address: {address}")
            return None

        try:
            device_id = self.config["modbus"].get("slave_id", 1)
            result = self.client.read_holding_registers(
                address, count=1, device_id=device_id
            )
            if result.isError():
                logger.error(f"Failed to read register {address}")
                return None

            raw_value = result.registers[0]
            # Для S_WORD — дополнительно декодируем знаковое (two's complement),
            # но normalize_register тоже сделает это. Передаём raw как есть,
            # normalize_register расправится со signedness по metadata.is_signed.
            norm_result = normalize_register(address, raw_value, metadata)

            # Сохраняем в репозиторий (если нормализация успешна)
            if self.repository and norm_result.ok:
                measurement = make_measurement(
                    inverter_id=INVERTER_ID,
                    register_address=norm_result.register_address,
                    parameter_name=norm_result.parameter_name,
                    raw_value=norm_result.raw_value,
                    normalized_value=norm_result.normalized_value,
                    unit=norm_result.unit,
                    timestamp=datetime.now(timezone.utc),
                    quality=norm_result.quality,
                    confidence=norm_result.confidence,
                    source=self.source,
                    description=metadata.description,
                )
                try:
                    self.repository.save_measurement(measurement)
                except Exception as exc:
                    logger.warning(f"Failed to persist measurement {address}: {exc}")

            return norm_result

        except ModbusIOException as e:
            logger.error(f"Modbus I/O error reading register {address}: {e}")
            self.connection_status = False
            return None
        except Exception as e:
            logger.error(f"Error reading register {address}: {e}")
            self.connection_status = False
            return None

    async def read_all_registers(self) -> Dict[str, Any]:
        """Чтение всех регистров этапа 7 (FC 03, count=1, последовательно).

        Возвращает словарь: имя параметра → нормализованные данные.
        При недоступности Modbus — пустой словарь (быстрый выход).
        """

        data: Dict[str, Any] = {}
        if self.telemetry_source is not None:
            # Единый pipeline: источник сам нормализует и помечает данные.
            snapshot = self.telemetry_source.read()
            self.connection_status = snapshot.connection_ok
            data = snapshot.to_read_all_dict()
            self.last_data = data
            self._last_snapshot = snapshot

            # Сохранение в Data Layer с меткой ИСТОЧНИКА (без смешивания).
            if (
                self.repository is not None
                and snapshot.present_values()
                and not getattr(self.telemetry_source, "persists_itself", False)
            ):
                try:
                    rows = [
                        Measurement(
                            inverter_id=snapshot.inverter_id,
                            register_address=value.register_address,
                            parameter_name=name,
                            raw_value=value.raw_value,
                            normalized_value=value.normalized_value,
                            unit=value.unit,
                            timestamp=datetime.fromisoformat(value.timestamp),
                            quality=MeasurementQuality(value.quality),
                            confidence=ConfidenceLevel(value.confidence),
                            source=self.source,
                            description=f"{self.source.value} telemetry (stage 16)",
                        )
                        for name, value in snapshot.present_values().items()
                    ]
                    self.repository.save_measurements_batch(rows)
                except Exception as exc:
                    logger.warning(f"Failed to persist telemetry snapshot: {exc}")
            return data

        if not self.connection_status:
            ok = await self.connect()
            if not ok:
                return data

        for address in sorted(self.register_map.keys()):
            if not self.connection_status:
                break
            norm_result = await self.read_register(address)
            if norm_result and norm_result.ok:
                metadata = self.register_map[address]
                data[metadata.name] = {
                    "raw_value": norm_result.raw_value,
                    "normalized_value": norm_result.normalized_value,
                    "unit": norm_result.unit,
                    "formula": norm_result.formula,
                    "confidence": norm_result.confidence.value,
                    "quality": norm_result.quality.value,
                    "address": address,
                    "timestamp": datetime.now(timezone.utc).isoformat(),
                }

        self.last_data = data
        return data

    async def get_system_status(self) -> Dict[str, Any]:
        """Получение статуса системы."""
        snapshot = self._last_snapshot
        last_update = snapshot.timestamp if snapshot is not None else datetime.now(timezone.utc).isoformat()
        inverter_id = snapshot.inverter_id if snapshot is not None else INVERTER_ID
        if snapshot is not None:
            connection_status = snapshot.connection_ok
        elif self.telemetry_source is not None:
            connection_status = self.telemetry_source.connection_ok
        else:
            connection_status = self.connection_status
        return {
            "connection_status": connection_status,
            "last_update": last_update,
            "inverter_id": inverter_id,
            "available_registers": len(self.register_map),
            "registers": sorted(self.register_map.keys()),
            "source": self.source.value,
        }


# ------------------------------------------------------------------
# FastAPI приложение
# ------------------------------------------------------------------

app = FastAPI(title="Deye Gateway API", version="2.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Глобальные переменные
_repository = SQLiteMeasurementRepository(CONFIG["storage"]["db_path"])
gateway = DeyeGateway(CONFIG, repository=_repository)
websocket_connections: List[WebSocket] = []


class ReadRequest(BaseModel):
    addresses: Optional[List[int]] = None


# ------------------------------------------------------------------
# Endpoints (совместимость с Android — сохранены)
# ------------------------------------------------------------------

@app.get("/")
async def root():
    """Базовый endpoint."""
    return {"message": "Deye Gateway API", "version": "2.0.0", "read_only": True}


@app.get("/status")
async def get_status():
    """Получение статуса системы."""
    return await gateway.get_system_status()


@app.get("/api/inverters")
async def get_inverters():
    """Список инверторов, которые обслуживает этот gateway.

    Gateway — одноканальный: он читает один сконфигурированный инвертор.
    Возвращаем реальный inverter_id и статус, без выдуманных устройств.
    """
    status = await gateway.get_system_status()
    return {
        "inverters": [
            {
                "inverter_id": status["inverter_id"],
                "online": status["connection_status"],
                "available_registers": status["available_registers"],
                "source": status["source"],
            }
        ],
        "count": 1,
        "source": status["source"],
    }


@app.get("/registers")
async def get_registers():
    """Получение списка регистров (из Data Layer register_metadata)."""
    return {
        "registers": [
            {
                "address": addr,
                "name": reg.name,
                "data_type": reg.data_type,
                "scale": reg.scale,
                "unit": reg.unit,
                "confidence": reg.confidence.value,
                "provisional_reason": reg.provisional_reason,
                "description": reg.description,
            }
            for addr, reg in gateway.register_map.items()
        ],
        "count": len(gateway.register_map),
        "source": "domain.register_metadata (stage 7)",
    }


@app.post("/read")
async def read_registers(request: ReadRequest):
    """Чтение регистров (read-only, FC 03)."""
    if request.addresses:
        result: Dict[str, Any] = {}
        for address in request.addresses:
            norm_result = await gateway.read_register(address)
            if norm_result and norm_result.ok:
                metadata = gateway.register_map.get(address)
                if metadata:
                    result[metadata.name] = {
                        "raw_value": norm_result.raw_value,
                        "normalized_value": norm_result.normalized_value,
                        "unit": norm_result.unit,
                        "formula": norm_result.formula,
                        "confidence": norm_result.confidence.value,
                        "quality": norm_result.quality.value,
                        "address": address,
                        "timestamp": datetime.now(timezone.utc).isoformat(),
                    }
    else:
        result = await gateway.read_all_registers()

    return {"data": result, "timestamp": datetime.now(timezone.utc).isoformat()}


# Удалён: POST /write, POST /emergency-stop, POST /emergency-resume
# (запись Modbus запрещена — строго read-only)


def _flat_live(data: Dict[str, Any], status: Dict[str, Any]) -> Dict[str, Any]:
    """Плоский JSON для Android (сумісність з SvitGrid-style полями).

    Берет normalized_value из нормализованных данных Data Layer.

    ВАЖНО (этап 12): отсутствующий регистр НЕ подменяется нулём. Если
    register_map не содержит параметра (например, p_grid/p_load), поле
    возвращается как null — Android отображает «—». Ранее здесь были
    заглушки 0.0 и p_load = battery_power, что меняло смысл показателя.
    """

    def v(name: str) -> Optional[float]:
        """Нормализованное значение или None, если регистра нет."""
        item = data.get(name)
        if isinstance(item, dict):
            value = item.get("normalized_value")
            return float(value) if value is not None else None
        if isinstance(item, (int, float)) and not isinstance(item, bool):
            return float(item)
        return None

    p_string1 = v("pv1_power")
    p_string2 = v("pv2_power")
    p_string3 = v("pv3_power")
    pv_values = [p for p in (p_string1, p_string2, p_string3) if p is not None]
    # p_sun = суммарная мощность PV; если нет ни одного PV-регистра — null.
    p_sun = float(sum(pv_values)) if pv_values else None
    # p_grid и p_load отсутствуют в register_map этапа 7 — честно null.
    p_grid = None
    p_load = None

    flat = {
        "p_sun": p_sun,
        "p_load": p_load,
        "p_batt": v("battery_power"),
        "p_grid": p_grid,
        "batt_soc": v("battery_soc"),
        "battery_voltage": v("battery_voltage"),
        "battery_current": v("battery_current"),
        "dc_temperature": v("dc_temperature"),
        "ac_temperature": v("ac_temperature"),
        "pv1_power": p_string1,
        "pv2_power": p_string2,
        "pv3_power": p_string3,
        "p_string1": p_string1,
        "p_string2": p_string2,
        "p_string3": p_string3,
        # Этап 24: явная маркировка PROVISIONAL-параметров для Android UI.
        "provisional_parameters": sorted(
            name for name, item in data.items()
            if isinstance(item, dict) and item.get("confidence") == "provisional"
        ),
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "connection_status": status.get("connection_status", False),
        "inverter_id": status.get("inverter_id", INVERTER_ID),
        "available_registers": status.get("available_registers", len(REGISTER_MAP)),
        # Источник данных: Android должен явно помечать DEMO.
        "source": status.get("source", DataSource.MODBUS_REGISTER.value),
    }
    # Добавляем все нормализованные значения плоско
    for name, item in data.items():
        if name not in flat and isinstance(item, dict) and item.get("normalized_value") is not None:
            flat[name] = item["normalized_value"]
    return flat


@app.get("/live")
async def get_live_data():
    """Получение live данных (с fallback, если Modbus недоступен)."""
    try:
        data = await gateway.read_all_registers()
        status = await gateway.get_system_status()
        flat = _flat_live(data, status)
        if not data:
            flat["error"] = "Modbus unavailable"
        return {
            "data": data,
            "status": status,
            "p_sun": flat["p_sun"],
            "p_load": flat["p_load"],
            "p_batt": flat["p_batt"],
            "p_grid": flat["p_grid"],
            "inverter_id": flat["inverter_id"],
            "source": flat["source"],
            "timestamp": flat["timestamp"],
        }
    except Exception as exc:
        # Нет данных — честно null + error, никаких выдуманных нулей.
        return {
            "data": {},
            "status": {"connection_status": False, "source": gateway.source.value},
            "p_sun": None,
            "p_load": None,
            "p_batt": None,
            "p_grid": None,
            "inverter_id": INVERTER_ID,
            "source": gateway.source.value,
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "error": f"Modbus unavailable: {exc}",
        }


@app.get("/api/live/raw")
async def get_live_raw():
    """Плоский JSON для Android (сумісність з SvitGrid-style полями)."""
    try:
        data = await gateway.read_all_registers()
        status = await gateway.get_system_status()
        flat = _flat_live(data, status)
        if not data:
            flat["error"] = "Modbus unavailable"
        return flat
    except Exception as e:
        return {"error": str(e), "timestamp": datetime.now(timezone.utc).isoformat(), "connection_status": False}


# ------------------------------------------------------------------
# Production endpoints (этап 10) — Data Layer measurements
# ------------------------------------------------------------------

@app.get("/api/measurements/latest")
async def get_latest_measurements():
    """Все последние измерения инвертора (по одному на параметр)."""
    try:
        measurements = _repository.get_all_latest(INVERTER_ID)
        return {
            "inverter_id": INVERTER_ID,
            "count": len(measurements),
            "measurements": [m.to_dict() for m in measurements],
        }
    except Exception as e:
        logger.error(f"Failed to get latest measurements: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/measurements/latest/{parameter}")
async def get_latest_measurement(parameter: str):
    """Последнее измерение конкретного параметра."""
    try:
        m = _repository.get_latest(INVERTER_ID, parameter)
        if m is None:
            raise HTTPException(status_code=404, detail=f"Parameter '{parameter}' not found")
        return m.to_dict()
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to get latest measurement {parameter}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/measurements/history")
async def get_measurement_history(
    parameter: str = Query(..., description="Имя параметра"),
    start: str = Query(..., description="Начало периода (ISO-8601 UTC)"),
    end: str = Query(..., description="Конец периода (ISO-8601 UTC)"),
):
    """История измерений параметра за период."""
    try:
        start_dt = datetime.fromisoformat(start)
        end_dt = datetime.fromisoformat(end)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=f"Invalid date format: {e}")

    try:
        measurements = _repository.get_history(INVERTER_ID, parameter, start_dt, end_dt)
        return {
            "inverter_id": INVERTER_ID,
            "parameter": parameter,
            "start": start,
            "end": end,
            "count": len(measurements),
            "measurements": [m.to_dict() for m in measurements],
        }
    except Exception as e:
        logger.error(f"Failed to get history for {parameter}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# ------------------------------------------------------------------
# Weather (без изменений — для прогноза на экране Android)
# ------------------------------------------------------------------

import httpx  # noqa: E402

WMO_ICONS = {
    0: "☀️", 1: "🌤", 2: "⛅", 3: "☁️",
    45: "🌫", 48: "🌫", 51: "🌦", 53: "🌦",
    55: "🌦", 61: "🌧", 63: "🌧", 65: "🌧",
    71: "🌨", 73: "🌨", 75: "🌨", 77: "🌨",
    80: "🌦", 81: "🌧", 82: "🌧", 85: "🌨",
    86: "🌨", 95: "⛈", 96: "⛈", 99: "⛈"
}

WMO_DESC = {
    0: "Ясно", 1: "Переважно ясно", 2: "Частково хмарно",
    3: "Хмарно", 45: "Туман", 48: "Туман з інеєм",
    51: "Легка мряка", 53: "Помірна мряка", 55: "Густа мряка",
    61: "Легкий дощ", 63: "Помірний дощ", 65: "Сильний дощ",
    71: "Легкий сніг", 73: "Помірний сніг", 75: "Сильний сніг",
    77: "Снігові зерна", 80: "Легкі зливи", 81: "Помірні зливи",
    82: "Сильні зливи", 85: "Легкий сніг", 86: "Сильний сніг",
    95: "Гроза", 96: "Гроза з градом", 99: "Сильна гроза"
}


@app.get("/weather")
async def get_weather():
    """Поточна погода з Open-Meteo."""
    try:
        lat = 50.35  # Київ
        lon = 30.95

        url = "https://api.open-meteo.com/v1/forecast"
        params = {
            "latitude": lat,
            "longitude": lon,
            "current": "temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code,apparent_temperature",
            "timezone": "Europe/Kiev"
        }

        async with httpx.AsyncClient() as client:
            response = await client.get(url, params=params, timeout=10.0)
            data = response.json()

        current = data.get("current", {})
        code = current.get("weather_code", 0)

        return {
            "temp": current.get("temperature_2m", 0),
            "feels": current.get("apparent_temperature", 0),
            "humidity": current.get("relative_humidity_2m", 0),
            "wind": current.get("wind_speed_10m", 0),
            "code": code,
            "icon": WMO_ICONS.get(code, "❓"),
            "desc": WMO_DESC.get(code, "Невідомо")
        }
    except Exception as e:
        logger.error(f"Помилка отримання погоди: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/forecast")
async def get_forecast():
    """Прогноз на 3 дні з Open-Meteo."""
    try:
        lat = 50.35  # Київ
        lon = 30.95

        url = "https://api.open-meteo.com/v1/forecast"
        params = {
            "latitude": lat,
            "longitude": lon,
            "daily": "temperature_2m_max,temperature_2m_min,precipitation_probability_max,weather_code",
            "timezone": "Europe/Kiev",
            "forecast_days": 3
        }

        async with httpx.AsyncClient() as client:
            response = await client.get(url, params=params, timeout=10.0)
            data = response.json()

        daily = data.get("daily", {})
        days = []

        for i in range(len(daily.get("time", []))):
            code = daily.get("weather_code", [])[i] if i < len(daily.get("weather_code", [])) else 0
            days.append({
                "date": daily.get("time", [])[i],
                "temp_max": daily.get("temperature_2m_max", [])[i],
                "temp_min": daily.get("temperature_2m_min", [])[i],
                "precip": daily.get("precipitation_probability_max", [])[i],
                "icon": WMO_ICONS.get(code, "❓"),
                "desc": WMO_DESC.get(code, "Невідомо")
            })

        return {"days": days}
    except Exception as e:
        logger.error(f"Помилка отримання прогнозу: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# ------------------------------------------------------------------
# WebSocket для real-time обновлений
# ------------------------------------------------------------------

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    """WebSocket для real-time обновлений (read-only)."""
    await websocket.accept()
    websocket_connections.append(websocket)

    try:
        while True:
            data = await gateway.read_all_registers()
            status = await gateway.get_system_status()

            message = {
                "type": "live_data",
                "data": data,
                "status": status,
                "timestamp": datetime.now(timezone.utc).isoformat(),
            }

            await websocket.send_text(json.dumps(message))
            await asyncio.sleep(5)  # Обновление каждые 5 секунд

    except WebSocketDisconnect:
        if websocket in websocket_connections:
            websocket_connections.remove(websocket)


@app.on_event("shutdown")
async def shutdown_event():
    """Остановка при завершении работы."""
    logger.info("Shutting down Deye Gateway Server")
    await gateway.disconnect()
    _repository.close()


if __name__ == "__main__":
    uvicorn.run(
        "deye_gateway:app",
        host=CONFIG["server"]["host"],
        port=CONFIG["server"]["port"],
        reload=False,
        log_level="info"
    )
