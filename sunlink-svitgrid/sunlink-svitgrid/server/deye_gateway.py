#!/usr/bin/env python3
"""
Гибкий сервер для Deye SUN-12K-SG02LP1-EU-AM3 инвертора
Реализует безопасное управление с учетом ограничений на количество одновременных записей
"""

import asyncio
import logging
import json
import time
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any
from dataclasses import dataclass, asdict
from pymodbus.client import ModbusTcpClient
from pymodbus.exceptions import ModbusIOException
from fastapi import FastAPI, HTTPException, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
from pydantic import BaseModel

# Настройка логирования
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Конфигурация
CONFIG = {
    "modbus": {
        "host": "192.168.25.15",  # Deye inverter (Modbus TCP)
        "port": 502,
        "timeout": 2,
        "slave_id": 1
    },
    "server": {
        "host": "0.0.0.0",
        "port": 8800,  # Изменен порт
        "max_concurrent_writes": 2,  # Безопасное ограничение
        "write_cooldown": 5  # секунд между записями
    },
    "safety": {
        "max_battery_charge_current": 100,  # А
        "max_battery_discharge_current": 100,  # А
        "max_solar_power": 12000,  # Вт
        "emergency_stop": False
    }
}

@dataclass
class RegisterDefinition:
    """Определение регистра"""
    address: int
    name: str
    data_type: str
    scale: float
    unit: str
    writable: bool = False
    description: str = ""

class DeyeRegisterMap:
    """Карта регистров Deye SG02LP1-AM3"""
    
    def __init__(self):
        self.registers = self._load_register_map()
        
    def _load_register_map(self) -> Dict[int, RegisterDefinition]:
        """Загрузка карты регистров (Deye SG02LP1/standard, скориговано)"""
        return {
            # Аккумулятор
            183: RegisterDefinition(183, "battery_voltage", "U_WORD", 0.01, "V", False, "Battery Voltage"),
            184: RegisterDefinition(184, "battery_soc", "U_WORD", 1.0, "%", False, "Battery State of Charge"),
            182: RegisterDefinition(182, "battery_temp", "U_WORD", 0.1, "°C", False, "Battery Temperature"),
            190: RegisterDefinition(190, "battery_power", "S_WORD", 1.0, "W", False, "Battery Power"),
            191: RegisterDefinition(191, "battery_current", "S_WORD", 0.01, "A", False, "Battery Current"),

            # Солнечные панели (3 MPPT)
            186: RegisterDefinition(186, "pv1_power", "U_WORD", 1.0, "W", False, "PV1 Power"),
            187: RegisterDefinition(187, "pv2_power", "U_WORD", 1.0, "W", False, "PV2 Power"),
            188: RegisterDefinition(188, "pv3_power", "U_WORD", 1.0, "W", False, "PV3 Power"),
            109: RegisterDefinition(109, "pv1_voltage", "U_WORD", 0.1, "V", False, "PV1 Voltage"),
            110: RegisterDefinition(110, "pv1_current", "U_WORD", 0.1, "A", False, "PV1 Current"),
            111: RegisterDefinition(111, "pv2_voltage", "U_WORD", 0.1, "V", False, "PV2 Voltage"),
            112: RegisterDefinition(112, "pv2_current", "U_WORD", 0.1, "A", False, "PV2 Current"),
            113: RegisterDefinition(113, "pv3_voltage", "U_WORD", 0.1, "V", False, "PV3 Voltage"),
            114: RegisterDefinition(114, "pv3_current", "U_WORD", 0.1, "A", False, "PV3 Current"),

            # Сеть и навантаження
            150: RegisterDefinition(150, "grid_voltage", "U_WORD", 0.1, "V", False, "Grid Voltage L1"),
            151: RegisterDefinition(151, "grid_frequency", "U_WORD", 0.01, "Hz", False, "Grid Frequency"),
            166: RegisterDefinition(166, "grid_inverter_power", "S_WORD", 1.0, "W", False, "Grid/Inverter Power"),
            167: RegisterDefinition(167, "grid_l1_power", "S_WORD", 1.0, "W", False, "Grid L1 Power"),
            172: RegisterDefinition(172, "grid_ct_power", "S_WORD", 1.0, "W", False, "Grid CT Power"),
            175: RegisterDefinition(175, "total_load_power", "U_WORD", 1.0, "W", False, "Total Load Power"),
            176: RegisterDefinition(176, "load_l1_power", "U_WORD", 1.0, "W", False, "Load L1 Power"),

            # Енергія (32-бітні)
            60: RegisterDefinition(60, "total_pv_production", "U_DWORD", 0.1, "kWh", False, "Total PV Production"),
            63: RegisterDefinition(63, "daily_pv_production", "U_DWORD", 0.1, "kWh", False, "Daily PV Production"),
            74: RegisterDefinition(74, "daily_buy_energy", "U_DWORD", 0.1, "kWh", False, "Daily Buy Energy"),
            76: RegisterDefinition(76, "daily_sell_energy", "U_DWORD", 0.1, "kWh", False, "Daily Sell Energy"),
            78: RegisterDefinition(78, "total_buy_energy", "U_DWORD", 0.1, "kWh", False, "Total Buy Energy"),
            80: RegisterDefinition(80, "total_sell_energy", "U_DWORD", 0.1, "kWh", False, "Total Sell Energy"),
            84: RegisterDefinition(84, "daily_load_energy", "U_DWORD", 0.1, "kWh", False, "Daily Load Energy"),
            85: RegisterDefinition(85, "total_load_energy", "U_DWORD", 0.1, "kWh", False, "Total Load Energy"),

            # Управління (записувані)
            230: RegisterDefinition(230, "max_solar_power_limit", "U_WORD", 1.0, "W", True, "Max Solar Power Limit"),
            232: RegisterDefinition(232, "max_charge_current", "U_WORD", 1.0, "A", True, "Max Charge Current"),
            233: RegisterDefinition(233, "max_discharge_current", "U_WORD", 1.0, "A", True, "Max Discharge Current"),
            248: RegisterDefinition(248, "tou_enable", "U_WORD", 1.0, "", True, "Time of Use Enable"),

            # TOU слоти
            250: RegisterDefinition(250, "tou_time_1", "U_WORD", 1.0, "HHMM", True, "TOU Time Slot 1"),
            256: RegisterDefinition(256, "tou_soc_1", "U_WORD", 1.0, "%", True, "TOU SOC Slot 1"),
            257: RegisterDefinition(257, "tou_power_1", "U_WORD", 1.0, "W", True, "TOU Power Slot 1"),
            258: RegisterDefinition(258, "tou_grid_charge_1", "U_WORD", 1.0, "", True, "TOU Grid Charge Slot 1"),

            # Діагностика
            90: RegisterDefinition(90, "inverter_temp", "U_WORD", 0.1, "°C", False, "Inverter Temperature"),
            103: RegisterDefinition(103, "fault_code_1", "U_WORD", 1.0, "", False, "Fault Code 1"),
            104: RegisterDefinition(104, "fault_code_2", "U_WORD", 1.0, "", False, "Fault Code 2"),
        }

class SafetyManager:
    """Менеджер безопасности для предотвращения повреждения инвертора"""
    
    def __init__(self, config: Dict):
        self.config = config
        self.last_write_time = 0
        self.active_writes = 0
        self.emergency_stop = False
        
    def can_write(self) -> bool:
        """Проверка возможности записи с учетом ограничений"""
        current_time = time.time()
        
        # Проверка emergency stop
        if self.emergency_stop:
            logger.warning("Emergency stop active - writing disabled")
            return False
            
        # Проверка количества активных записей
        if self.active_writes >= self.config["server"]["max_concurrent_writes"]:
            logger.warning(f"Max concurrent writes reached: {self.active_writes}")
            return False
            
        # Проверка cooldown периода
        if current_time - self.last_write_time < self.config["server"]["write_cooldown"]:
            logger.warning("Write cooldown period active")
            return False
            
        return True
        
    def start_write(self) -> bool:
        """Начало операции записи"""
        if not self.can_write():
            return False
            
        self.active_writes += 1
        self.last_write_time = time.time()
        logger.info(f"Starting write operation. Active writes: {self.active_writes}")
        return True
        
    def finish_write(self):
        """Завершение операции записи"""
        self.active_writes = max(0, self.active_writes - 1)
        logger.info(f"Finished write operation. Active writes: {self.active_writes}")
        
    def validate_write_value(self, register_def: RegisterDefinition, value: Any) -> bool:
        """Валидация значения перед записью"""
        if register_def.name == "max_charge_current":
            return 0 <= value <= self.config["safety"]["max_battery_charge_current"]
        elif register_def.name == "max_discharge_current":
            return 0 <= value <= self.config["safety"]["max_battery_discharge_current"]
        elif register_def.name == "max_solar_power_limit":
            return 0 <= value <= self.config["safety"]["max_solar_power"]
        return True

class DeyeGateway:
    """Основной шлюз для работы с Deye инвертором"""
    
    def __init__(self, config: Dict):
        self.config = config
        self.register_map = DeyeRegisterMap()
        self.safety_manager = SafetyManager(config)
        self.client = None
        self.last_data = {}
        self.connection_status = False
        
    async def connect(self):
        """Подключение к инвертору"""
        try:
            self.client = ModbusTcpClient(
                host=self.config["modbus"]["host"],
                port=self.config["modbus"]["port"],
                timeout=self.config["modbus"]["timeout"]
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
            
    async def disconnect(self):
        """Отключение от инвертора"""
        if self.client:
            self.client.close()
            self.connection_status = False
            logger.info("Disconnected from Deye inverter")
            
    async def read_register(self, address: int) -> Optional[float]:
        """Чтение одного регистра"""
        if not self.connection_status:
            ok = await self.connect()
            if not ok:
                return None

        try:
            register_def = self.register_map.registers.get(address)
            if not register_def:
                logger.error(f"Unknown register address: {address}")
                return None

            device_id = self.config["modbus"].get("slave_id", 1)
            if register_def.data_type == "U_DWORD":
                result = self.client.read_holding_registers(address, count=2, device_id=device_id)
                if not result.isError():
                    value = result.registers[0] + (result.registers[1] << 16)
                    return value * register_def.scale
            else:
                result = self.client.read_holding_registers(address, count=1, device_id=device_id)
                if not result.isError():
                    value = result.registers[0]
                    if register_def.data_type == "S_WORD" and value > 32767:
                        value = value - 65536
                    return value * register_def.scale

            logger.error(f"Failed to read register {address}")
            return None

        except ModbusIOException as e:
            logger.error(f"Modbus I/O error reading register {address}: {e}")
            self.connection_status = False
            return None
        except Exception as e:
            logger.error(f"Error reading register {address}: {e}")
            self.connection_status = False
            return None
            
    async def write_register(self, address: int, value: float) -> bool:
        """Безопасная запись регистра с валидацией"""
        if not self.connection_status:
            ok = await self.connect()
            if not ok:
                return False

        register_def = self.register_map.registers.get(address)
        if not register_def or not register_def.writable:
            logger.error(f"Register {address} is not writable")
            return False

        if not self.safety_manager.validate_write_value(register_def, value):
            logger.error(f"Invalid value for register {address}: {value}")
            return False

        if not self.safety_manager.start_write():
            return False

        try:
            raw_value = int(value / register_def.scale)
            if register_def.data_type == "S_WORD" and raw_value < 0:
                raw_value = raw_value + 65536
            device_id = self.config["modbus"].get("slave_id", 1)
            result = self.client.write_register(address, raw_value, device_id=device_id)
            if not result.isError():
                logger.info(f"Successfully wrote register {address}: {value}")
                self.safety_manager.finish_write()
                return True
            else:
                logger.error(f"Failed to write register {address}")
                self.safety_manager.finish_write()
                return False
        except Exception as e:
            logger.error(f"Error writing register {address}: {e}")
            self.connection_status = False
            self.safety_manager.finish_write()
            return False
            
    async def read_all_registers(self) -> Dict[str, Any]:
        """Чтение всех регистров (быстрый выход при недоступности Modbus)"""
        data = {}
        if not self.connection_status:
            ok = await self.connect()
            if not ok:
                return data

        read_groups = [
            [182, 183, 184, 190, 191],  # Battery
            [186, 187, 188, 109, 110, 111, 112, 113, 114],  # PV
            [150, 151, 166, 167, 172, 175, 176],  # Grid & Load
            [60, 63, 74, 76, 78, 80, 84, 85],  # Energy counters
            [90, 103, 104]  # Diagnostics
        ]

        for group in read_groups:
            if not self.connection_status:
                break
            for address in group:
                if address in self.register_map.registers:
                    value = await self.read_register(address)
                    if value is not None:
                        register_def = self.register_map.registers[address]
                        data[register_def.name] = {
                            "value": value,
                            "address": address,
                            "unit": register_def.unit,
                            "timestamp": datetime.now().isoformat()
                        }
        self.last_data = data
        return data
        
    async def get_system_status(self) -> Dict[str, Any]:
        """Получение статуса системы"""
        return {
            "connection_status": self.connection_status,
            "last_update": datetime.now().isoformat(),
            "active_writes": self.safety_manager.active_writes,
            "last_write_time": datetime.fromtimestamp(self.safety_manager.last_write_time).isoformat(),
            "emergency_stop": self.safety_manager.emergency_stop,
            "available_registers": len(self.register_map.registers)
        }

# FastAPI приложение
app = FastAPI(title="Deye Gateway API", version="1.0.0")

# Настройка CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Глобальные переменные
gateway = DeyeGateway(CONFIG)
websocket_connections = []

class ReadRequest(BaseModel):
    addresses: List[int] = None

class WriteRequest(BaseModel):
    address: int
    value: float

@app.on_event("startup")
async def startup_event():
    """Запуск при старте приложения"""
    logger.info("Starting Deye Gateway Server")
    # Не подключаемся автоматически, ждем первого запроса
    logger.info("Server ready - waiting for inverter connection")
    
@app.on_event("shutdown")
async def shutdown_event():
    """Остановка при завершении работы"""
    logger.info("Shutting down Deye Gateway Server")
    await gateway.disconnect()

@app.get("/")
async def root():
    """Базовый endpoint"""
    return {"message": "Deye Gateway API", "version": "1.0.0"}

@app.get("/status")
async def get_status():
    """Получение статуса системы"""
    return await gateway.get_system_status()

@app.get("/registers")
async def get_registers():
    """Получение списка всех регистров"""
    return {
        "registers": [
            {
                "address": addr,
                "name": reg.name,
                "data_type": reg.data_type,
                "scale": reg.scale,
                "unit": reg.unit,
                "writable": reg.writable,
                "description": reg.description
            }
            for addr, reg in gateway.register_map.registers.items()
        ]
    }

@app.post("/read")
async def read_registers(request: ReadRequest):
    """Чтение регистров"""
    if request.addresses:
        # Чтение указанных регистров
        result = {}
        for address in request.addresses:
            value = await gateway.read_register(address)
            if value is not None:
                register_def = gateway.register_map.registers.get(address)
                if register_def:
                    result[register_def.name] = {
                        "value": value,
                        "address": address,
                        "unit": register_def.unit,
                        "timestamp": datetime.now().isoformat()
                    }
    else:
        # Чтение всех регистров
        result = await gateway.read_all_registers()
        
    return {"data": result, "timestamp": datetime.now().isoformat()}

@app.post("/write")
async def write_register(request: WriteRequest):
    """Безопасная запись регистра"""
    success = await gateway.write_register(request.address, request.value)
    return {
        "success": success,
        "address": request.address,
        "value": request.value,
        "timestamp": datetime.now().isoformat()
    }

def _flat_live(data: Dict[str, Any], status: Dict[str, Any]) -> Dict[str, Any]:
    def v(name: str, default=0.0):
        item = data.get(name)
        if isinstance(item, dict):
            return item.get("value", default)
        return item if item is not None else default

    p_string1 = float(v("pv1_power", 0.0))
    p_string2 = float(v("pv2_power", 0.0))
    p_string3 = float(v("pv3_power", 0.0))
    pv_sum = p_string1 + p_string2 + p_string3
    p_sun = float(v("power_dc", pv_sum))
    if p_sun == 0.0 and pv_sum > 0.0:
        p_sun = pv_sum
    p_load = float(v("total_load_power", v("power_ac", 0.0)))
    p_batt = float(v("battery_power", 0.0))
    grid_ct = float(v("grid_ct_power", 0.0))
    if grid_ct != 0.0:
        p_grid = grid_ct
    else:
        # баланс потужності, якщо немає CT/прямого виміру
        p_grid = p_load - p_sun - p_batt

    flat = {
        "p_sun": p_sun,
        "p_load": p_load,
        "p_batt": p_batt,
        "p_grid": p_grid,
        "batt_soc": float(v("battery_soc", 0.0)),
        "battery_voltage": float(v("battery_voltage", 0.0)),
        "battery_current": float(v("battery_current", 0.0)),
        "battery_temp": float(v("battery_temp", 0.0)),
        "pv1_voltage": float(v("pv1_voltage", 0.0)),
        "pv1_current": float(v("pv1_current", 0.0)),
        "pv2_voltage": float(v("pv2_voltage", 0.0)),
        "pv2_current": float(v("pv2_current", 0.0)),
        "pv3_voltage": float(v("pv3_voltage", 0.0)),
        "pv3_current": float(v("pv3_current", 0.0)),
        "p_string1": p_string1,
        "p_string2": p_string2,
        "p_string3": p_string3,
        "grid_voltage_l1": float(v("grid_voltage", 0.0)),
        "grid_voltage": float(v("grid_voltage", 0.0)),
        "timestamp": datetime.now().isoformat(),
        "connection_status": status.get("connection_status", False),
    }
    # також додаємо всі сирі значення плоско
    for name, item in data.items():
        if name not in flat and isinstance(item, dict) and "value" in item:
            flat[name] = item["value"]
    return flat

@app.get("/live")
async def get_live_data():
    """Получение live данных (с швидким fallback, якщо Modbus недоступний)"""
    try:
        data = await gateway.read_all_registers()
        status = await gateway.get_system_status()
        if not data:
            raise RuntimeError("No modbus data")
        flat = _flat_live(data, status)
        return {
            "data": data,
            "status": status,
            "p_sun": flat["p_sun"],
            "p_load": flat["p_load"],
            "p_batt": flat["p_batt"],
            "p_grid": flat["p_grid"],
            "timestamp": flat["timestamp"]
        }
    except Exception:
        return {
            "data": {},
            "status": {"connection_status": False},
            "p_sun": 0.0,
            "p_load": 0.0,
            "p_batt": 0.0,
            "p_grid": 0.0,
            "timestamp": datetime.now().isoformat(),
            "error": "Modbus unavailable"
        }

@app.get("/api/live/raw")
async def get_live_raw():
    """Плоский JSON для Android (сумісність з SvitGrid-style полями)"""
    try:
        data = await gateway.read_all_registers()
        status = await gateway.get_system_status()
        flat = _flat_live(data, status)
        if not data:
            flat["error"] = "Modbus unavailable"
        return flat
    except Exception as e:
        return {"error": str(e), "timestamp": datetime.now().isoformat(), "connection_status": False}

@app.post("/emergency-stop")
async def emergency_stop():
    """Аварийная остановка"""
    gateway.safety_manager.emergency_stop = True
    logger.warning("Emergency stop activated")
    return {"message": "Emergency stop activated", "timestamp": datetime.now().isoformat()}

@app.post("/emergency-resume")
async def emergency_resume():
    """Возобновление работы после аварийной остановки"""
    gateway.safety_manager.emergency_stop = False
    logger.info("Emergency stop deactivated")
    return {"message": "Emergency stop deactivated", "timestamp": datetime.now().isoformat()}

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    """WebSocket для real-time обновлений"""
    await websocket.accept()
    websocket_connections.append(websocket)
    
    try:
        while True:
            # Отправка live данных
            data = await gateway.read_all_registers()
            status = await gateway.get_system_status()
            
            message = {
                "type": "live_data",
                "data": data,
                "status": status,
                "timestamp": datetime.now().isoformat()
            }
            
            await websocket.send_text(json.dumps(message))
            await asyncio.sleep(5)  # Обновление каждые 5 секунд
            
    except WebSocketDisconnect:
        websocket_connections.remove(websocket)

if __name__ == "__main__":
    uvicorn.run(
        "deye_gateway:app",
        host=CONFIG["server"]["host"],
        port=CONFIG["server"]["port"],
        reload=False,
        log_level="info"
    )
# Эндпоинты для погоды и прогноза
import httpx
from typing import Optional

# WMO-коды для иконок
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
    """Поточна погода з Open-Meteo"""
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
    """Прогноз на 3 дні з Open-Meteo"""
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
