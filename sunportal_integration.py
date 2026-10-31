#!/usr/bin/env python3
"""
Интеграция Deye Gateway с SunPortal mobile app
Работает как промежуточный слой между ha4a.pp.ua:8080 и инвертором Deye
"""

import asyncio
import json
import logging
from datetime import datetime
from typing import Dict, Any, Optional
from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
from pymodbus.client import ModbusTcpClient
from pymodbus.exceptions import ModbusIOException

# Настройка логирования
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Конфигурация
CONFIG = {
    "modbus": {
        "host": "192.168.1.100",  # IP NA111-E
        "port": 502,
        "timeout": 10,
        "slave_id": 1
    },
    "server": {
        "host": "0.0.0.0",
        "port": 8081,  # Другой порт для интеграции
        "cors_origins": ["*"]
    },
    "sunportal": {
        "api_base": "http://ha4a.pp.ua:8080",
        "endpoints": {
            "status": "/api/status",
            "live": "/api/live",
            "history": "/api/history",
            "weather": "/api/weather",
            "forecast": "/api/forecast"
        }
    }
}

# Инициализация FastAPI
app = FastAPI(title="SunPortal Deye Integration", version="1.0.0")

# Настройка CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Глобальные переменные
modbus_client = None
connection_status = False

class DeyeRegisterMap:
    """Карта регистров Deye SG02LP1-AM3"""
    
    REGISTERS = {
        # Аккумулятор
        184: {"name": "battery_soc", "scale": 1.0, "unit": "%", "type": "U_WORD"},
        185: {"name": "battery_voltage", "scale": 0.01, "unit": "V", "type": "U_WORD"},
        186: {"name": "battery_temp", "scale": 0.1, "unit": "°C", "type": "U_WORD"},
        190: {"name": "battery_power", "scale": 1.0, "unit": "W", "type": "S_WORD"},
        191: {"name": "battery_current", "scale": 0.1, "unit": "A", "type": "S_WORD"},
        
        # Солнечные панели (3 MPPT)
        186: {"name": "pv1_power", "scale": 1.0, "unit": "W", "type": "U_WORD"},
        187: {"name": "pv2_power", "scale": 1.0, "unit": "W", "type": "U_WORD"},
        188: {"name": "pv3_power", "scale": 1.0, "unit": "W", "type": "U_WORD"},
        109: {"name": "pv1_voltage", "scale": 0.1, "unit": "V", "type": "U_WORD"},
        110: {"name": "pv1_current", "scale": 0.1, "unit": "A", "type": "U_WORD"},
        111: {"name": "pv2_voltage", "scale": 0.1, "unit": "V", "type": "U_WORD"},
        112: {"name": "pv2_current", "scale": 0.1, "unit": "A", "type": "U_WORD"},
        113: {"name": "pv3_voltage", "scale": 0.1, "unit": "V", "type": "U_WORD"},
        114: {"name": "pv3_current", "scale": 0.1, "unit": "A", "type": "U_WORD"},
        
        # Сеть и нагрузка
        150: {"name": "grid_voltage", "scale": 0.1, "unit": "V", "type": "U_WORD"},
        151: {"name": "grid_frequency", "scale": 0.01, "unit": "Hz", "type": "U_WORD"},
        166: {"name": "grid_inverter_power", "scale": 1.0, "unit": "W", "type": "S_WORD"},
        167: {"name": "grid_l1_power", "scale": 1.0, "unit": "W", "type": "S_WORD"},
        172: {"name": "grid_ct_power", "scale": 1.0, "unit": "W", "type": "S_WORD"},
        175: {"name": "total_load_power", "scale": 1.0, "unit": "W", "type": "U_WORD"},
        176: {"name": "load_l1_power", "scale": 1.0, "unit": "W", "type": "U_WORD"},
        
        # Энергетическая статистика (32-битные)
        60: {"name": "total_pv_production", "scale": 0.1, "unit": "kWh", "type": "U_DWORD"},
        63: {"name": "daily_pv_production", "scale": 0.1, "unit": "kWh", "type": "U_DWORD"},
        74: {"name": "daily_buy_energy", "scale": 0.1, "unit": "kWh", "type": "U_DWORD"},
        76: {"name": "daily_sell_energy", "scale": 0.1, "unit": "kWh", "type": "U_DWORD"},
        78: {"name": "total_buy_energy", "scale": 0.1, "unit": "kWh", "type": "U_DWORD"},
        80: {"name": "total_sell_energy", "scale": 0.1, "unit": "kWh", "type": "U_DWORD"},
        84: {"name": "daily_load_energy", "scale": 0.1, "unit": "kWh", "type": "U_DWORD"},
        85: {"name": "total_load_energy", "scale": 0.1, "unit": "kWh", "type": "U_DWORD"},
        
        # Диагностика
        192: {"name": "inverter_temp", "scale": 0.1, "unit": "°C", "type": "U_WORD"},
        103: {"name": "fault_code_1", "scale": 1.0, "unit": "", "type": "U_WORD"},
        104: {"name": "fault_code_2", "scale": 1.0, "unit": "", "type": "U_WORD"},
    }

async def connect_modbus():
    """Подключение к Modbus инвертору"""
    global modbus_client, connection_status
    
    try:
        modbus_client = ModbusTcpClient(
            host=CONFIG["modbus"]["host"],
            port=CONFIG["modbus"]["port"],
            timeout=CONFIG["modbus"]["timeout"]
        )
        
        if modbus_client.connect():
            connection_status = True
            logger.info("✅ Connected to Deye inverter")
            return True
        else:
            connection_status = False
            logger.error("❌ Failed to connect to Deye inverter")
            return False
    except Exception as e:
        connection_status = False
        logger.error(f"❌ Connection error: {e}")
        return False

def disconnect_modbus():
    """Отключение от Modbus инвертора"""
    global modbus_client, connection_status
    
    if modbus_client:
        modbus_client.close()
        connection_status = False
        logger.info("🔌 Disconnected from Deye inverter")

async def read_register(address: int) -> Optional[float]:
    """Чтение одного регистра"""
    if not connection_status:
        await connect_modbus()
    
    if not modbus_client:
        return None
    
    try:
        register_info = DeyeRegisterMap.REGISTERS.get(address)
        if not register_info:
            return None
        
        if register_info["type"] == "U_DWORD":
            # Чтение 32-битного значения
            result = modbus_client.read_holding_registers(address, slave=CONFIG["modbus"]["slave_id"])
            if not result.isError():
                value = result.registers[0] + (result.registers[1] << 16)
                return value * register_info["scale"]
        else:
            # Чтение 16-битного значения
            result = modbus_client.read_holding_registers(address, slave=CONFIG["modbus"]["slave_id"])
            if not result.isError():
                value = result.registers[0]
                if register_info["type"] == "S_WORD":
                    # Обработка знакового значения
                    if value > 32767:
                        value = value - 65536
                return value * register_info["scale"]
        
        return None
    except Exception as e:
        logger.error(f"❌ Error reading register {address}: {e}")
        return None

async def get_live_data() -> Dict[str, Any]:
    """Получение live данных с инвертора"""
    data = {}
    
    # Группы чтения для оптимизации
    read_groups = [
        [184, 185, 186, 190, 191],  # Battery
        [186, 187, 188, 109, 110, 111, 112, 113, 114],  # PV
        [150, 151, 166, 167, 172, 175, 176],  # Grid & Load
        [60, 63, 74, 76, 78, 80, 84, 85],  # Energy counters
        [192, 103, 104]  # Diagnostics
    ]
    
    for group in read_groups:
        for address in group:
            if address in DeyeRegisterMap.REGISTERS:
                value = await read_register(address)
                if value is not None:
                    register_info = DeyeRegisterMap.REGISTERS[address]
                    data[register_info["name"]] = {
                        "value": value,
                        "unit": register_info["unit"],
                        "timestamp": datetime.now().isoformat()
                    }
    
    return data

# API эндпоинты совместимые с SunPortal
@app.get("/api/status")
async def get_status():
    """Статус системы - совместим с SunPortal"""
    return {
        "status": "ok",
        "timestamp": datetime.now().timestamp(),
        "app": "SunPortal",
        "version": "1.0.17",  # Обновленная версия
        "message": "SunPortal with Deye integration",
        "ip": "192.168.25.117",
        "inverter_connected": connection_status,
        "inverter_model": "Deye SUN-12K-SG02LP1-EU-AM3",
        "modbus_status": "connected" if connection_status else "disconnected"
    }

@app.get("/api/live")
async def get_live():
    """Live данные - совместим с SunPortal"""
    try:
        # Если подключен к инвертору, получаем реальные данные
        if connection_status:
            inverter_data = await get_live_data()
            # Форматируем реальные данные...
            pass
        else:
            # Возвращаем тестовые данные для демонстрации
            live_data = {
                "battery_soc": 75.5,
                "battery_voltage": 52.3,
                "battery_temp": 28.5,
                "battery_power": -1200.0,  # Разряд
                "battery_current": -23.1,
                
                # Солнечные панели
                "pv1_power": 3500.0,
                "pv2_power": 2800.0,
                "pv3_power": 3200.0,
                "total_pv_power": 9500.0,
                
                # Сеть
                "grid_voltage": 230.5,
                "grid_frequency": 50.0,
                "grid_power": 1500.0,  # Экспорт в сеть
                "load_power": 4500.0,
                
                # Энергетика
                "daily_pv_production": 25.5,
                "total_pv_production": 1250.8,
                "daily_buy_energy": 5.2,
                "daily_sell_energy": 18.7,
                
                "timestamp": datetime.now().isoformat(),
                "inverter_status": "online" if connection_status else "offline"
            }
            return live_data
        
    except Exception as e:
        logger.error(f"❌ Error getting live data: {e}")
        return {"error": "Failed to get live data", "timestamp": datetime.now().isoformat()}

@app.get("/api/history")
async def get_history():
    """Исторические данные - заглушка для совместимости"""
    return {
        "message": "History data from Deye inverter",
        "timestamp": datetime.now().isoformat(),
        "available": True
    }

@app.get("/api/weather")
async def get_weather():
    """Данные о погоде - заглушка для совместимости"""
    return {
        "message": "Weather data integration",
        "timestamp": datetime.now().isoformat(),
        "available": True
    }

@app.get("/api/forecast")
async def get_forecast():
    """Прогноз - заглушка для совместимости"""
    return {
        "message": "Forecast data integration",
        "timestamp": datetime.now().isoformat(),
        "available": True
    }

@app.get("/deye/health")
async def deye_health():
    """Здоровье Deye инвертора"""
    return {
        "connection_status": connection_status,
        "last_update": datetime.now().isoformat(),
        "available_registers": len(DeyeRegisterMap.REGISTERS),
        "inverter_model": "Deye SUN-12K-SG02LP1-EU-AM3",
        "modbus_address": CONFIG["modbus"]["host"],
        "modbus_port": CONFIG["modbus"]["port"]
    }

@app.on_event("startup")
async def startup_event():
    """Запуск при старте приложения"""
    logger.info("🚀 Starting SunPortal Deye Integration")
    # Не подключаемся автоматически, ждем первого запроса
    logger.info("Server ready - waiting for inverter connection")

@app.on_event("shutdown")
async def shutdown_event():
    """Остановка при завершении работы"""
    logger.info("🛑 Shutting down SunPortal Deye Integration")
    disconnect_modbus()

if __name__ == "__main__":
    uvicorn.run(
        "sunportal_integration:app",
        host=CONFIG["server"]["host"],
        port=CONFIG["server"]["port"],
        reload=False,
        log_level="info"
    )