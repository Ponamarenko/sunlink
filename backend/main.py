#!/usr/bin/env python3
"""
SunLink API Server - Оптимизированная версия
"""

import asyncio
import json
import logging
import time
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any
from fastapi import FastAPI, WebSocket, WebSocketDisconnect, HTTPException, Depends, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
import paho.mqtt.client as mqtt
from pydantic import BaseModel
import uvicorn
from influxdb_client import InfluxDBClient
from influxdb_client.client.write_api import SYNCHRONOUS
import jwt
from dataclasses import dataclass, asdict
import prometheus_client
from prometheus_client import Counter, Gauge, Histogram

# Настройка логирования
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('/app/logs/app.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

# Prometheus метрики
REQUEST_COUNT = Counter('http_requests_total', 'Total HTTP requests', ['method', 'endpoint'])
ERROR_COUNT = Counter('http_errors_total', 'Total HTTP errors', ['method', 'endpoint', 'status_code'])
ACTIVE_CONNECTIONS = Gauge('active_connections', 'Active WebSocket connections')
SYSTEM_UPTIME = Gauge('system_uptime', 'System uptime in seconds')
MQTT_CONNECTION_STATUS = Gauge('mqtt_connection_status', 'MQTT connection status (0=disconnected, 1=connected)')

# FastAPI приложение
app = FastAPI(
    title="SunLink API Server",
    description="Система интеграции инвертора Deye с мобильным приложением sunlink.pp.ua",
    version="1.0.0",
    debug=False
)

# Middleware для Prometheus
@app.middleware("http")
async def metrics_middleware(request: Request, call_next):
    """Middleware для сбора метрик Prometheus"""
    start_time = time.time()
    
    # Логирование запроса
    REQUEST_COUNT.labels(
        method=request.method,
        endpoint=request.url.path
    ).inc()
    
    try:
        response = await call_next(request)
        return response
    except Exception as e:
        ERROR_COUNT.labels(
            method=request.method,
            endpoint=request.url.path,
            status_code="500"
        ).inc()
        raise e

# Настройка CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["https://sunlink.pp.ua", "https://api.sunlink.pp.ua"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Глобальные переменные
mqtt_client = None
influx_client = None
influx_write_api = None
websocket_connections = {}
security = HTTPBearer()

# Хранение состояния инверторов
current_inverter_states: Dict[str, InverterState] = {}

# Конфигурация
CONFIG = {
    "mqtt": {
        "host": "mosquitto",
        "port": 1883,
        "username": "sunlink_user",
        "password": "SunLinkMQTT2026",
        "keepalive": 60,
        "retry_attempts": 3,
        "retry_delay": 5
    },
    "influxdb": {
        "url": "http://influxdb:8086",
        "token": "SunLinkInfluxDBToken2026",
        "org": "sunlink_org",
        "bucket": "deye_metrics",
        "timeout": 30
    },
    "jwt": {
        "secret": "SunLinkJWTSecret2026",
        "algorithm": "HS256",
        "expiration": 2592000  # 30 дней
    },
    "server": {
        "host": "0.0.0.0",
        "port": 8000
    }
}

# Модели данных
class InverterState(BaseModel):
    inverter_id: str
    timestamp: datetime
    voltage_dc: float
    current_dc: float
    power_dc: float
    voltage_ac: float
    current_ac: float
    power_ac: float
    frequency: float
    temperature: float
    efficiency: float
    state: str
    error_code: int
    battery_soc: float
    battery_voltage: float

class LoginRequest(BaseModel):
    username: str
    password: str

class LoginResponse(BaseModel):
    access_token: str
    token_type: str
    expires_in: int

class TelemetryData(BaseModel):
    inverter_id: str
    timestamp: datetime
    voltage_dc: float
    current_dc: float
    power_dc: float
    voltage_ac: float
    current_ac: float
    power_ac: float
    frequency: float
    temperature: float
    efficiency: float
    state: str
    error_code: int
    battery_soc: float
    battery_voltage: float

# MQTT менеджер
class MQTTManager:
    def __init__(self):
        self.client = None
        self.connected = False
        self.reconnect_attempts = 0
        self.max_reconnect_attempts = CONFIG["mqtt"]["retry_attempts"]
    
    def connect(self):
        """Подключение к MQTT брокеру"""
        try:
            self.client = mqtt.Client()
            self.client.username_pw_set(
                CONFIG["mqtt"]["username"],
                CONFIG["mqtt"]["password"]
            )
            
            # Callback функции
            self.client.on_connect = self.on_connect
            self.client.on_disconnect = self.on_disconnect
            self.client.on_message = self.on_message
            self.client.on_log = self.on_log
            
            # Подключение
            self.client.connect(
                CONFIG["mqtt"]["host"],
                CONFIG["mqtt"]["port"],
                CONFIG["mqtt"]["keepalive"]
            )
            
            # Запуск цикла
            self.client.loop_start()
            
            logger.info("MQTT client started")
            return True
            
        except Exception as e:
            logger.error(f"Failed to connect to MQTT: {e}")
            return False
    
    def on_connect(self, client, userdata, flags, rc):
        """Callback при подключении"""
        if rc == 0:
            self.connected = True
            self.reconnect_attempts = 0
            MQTT_CONNECTION_STATUS.set(1)
            logger.info("Connected to MQTT broker")
            
            # Подписка на топики
            topics = [
                "deye/telemetry/+",
                "deye/status/+",
                "deye/control/+",
                "deye/response/+",
                "deye/error/+",
                "deye/aggregate/+"
            ]
            
            for topic in topics:
                client.subscribe(topic)
                logger.info(f"Subscribed to: {topic}")
                
        else:
            logger.error(f"Failed to connect to MQTT, return code: {rc}")
    
    def on_disconnect(self, client, userdata, rc):
        """Callback при отключении"""
        self.connected = False
        MQTT_CONNECTION_STATUS.set(0)
        logger.warning(f"Disconnected from MQTT broker, return code: {rc}")
        
        # Автоматическое переподключение
        if self.reconnect_attempts < self.max_reconnect_attempts:
            self.reconnect_attempts += 1
            logger.info(f"Attempting to reconnect ({self.reconnect_attempts}/{self.max_reconnect_attempts})")
            time.sleep(CONFIG["mqtt"]["retry_delay"])
            self.connect()
    
    def on_message(self, client, userdata, msg):
        """Callback при получении сообщения"""
        try:
            topic = msg.topic
            payload = msg.payload.decode('utf-8')
            
            logger.info(f"Received message on {topic}: {payload}")
            
            # Обработка сообщения
            self.process_message(topic, payload)
            
        except Exception as e:
            logger.error(f"Error processing MQTT message: {e}")
    
    def on_log(self, client, userdata, level, buf):
        """Логирование MQTT событий"""
        logger.debug(f"MQTT Log: {buf}")
    
    def process_message(self, topic: str, payload: str):
        """Обработка MQTT сообщений"""
        try:
            data = json.loads(payload)
            
            # Определение типа сообщения
            if "telemetry" in topic:
                self.handle_telemetry(data)
            elif "status" in topic:
                self.handle_status(data)
            elif "error" in topic:
                self.handle_error(data)
                
        except json.JSONDecodeError:
            logger.error(f"Invalid JSON payload: {payload}")
        except Exception as e:
            logger.error(f"Error processing message: {e}")
    
    def handle_telemetry(self, data: dict):
        """Обработка телеметрии"""
        try:
            inverter_id = data.get("inverter_id")
            if not inverter_id:
                return
            
            # Сохранение в InfluxDB
            self.save_to_influxdb(inverter_id, data)
            
            # Обновление состояния
            state = InverterState(**data)
            current_inverter_states[inverter_id] = state
            
            # Рассылка по WebSocket
            self.broadcast_to_websockets(inverter_id, data)
            
        except Exception as e:
            logger.error(f"Error handling telemetry: {e}")
    
    def handle_status(self, data: dict):
        """Обработка статуса"""
        try:
            inverter_id = data.get("inverter_id")
            if not inverter_id:
                return
            
            # Обновление состояния
            if inverter_id in current_inverter_states:
                current_inverter_states[inverter_id].state = data.get("state", "UNKNOWN")
                current_inverter_states[inverter_id].error_code = data.get("error_code", 0)
            
            logger.info(f"Status update for {inverter_id}: {data}")
            
        except Exception as e:
            logger.error(f"Error handling status: {e}")
    
    def handle_error(self, data: dict):
        """Обработка ошибок"""
        logger.error(f"Error from inverter {data.get('inverter_id', 'UNKNOWN')}: {data}")
    
    def save_to_influxdb(self, inverter_id: str, data: dict):
        """Сохранение данных в InfluxDB"""
        try:
            if not influx_client:
                return
            
            # Формирование данных для InfluxDB
            point = {
                "measurement": "telemetry",
                "tags": {
                    "inverter_id": inverter_id
                },
                "fields": {
                    "voltage_dc": data.get("voltage_dc", 0),
                    "current_dc": data.get("current_dc", 0),
                    "power_dc": data.get("power_dc", 0),
                    "voltage_ac": data.get("voltage_ac", 0),
                    "current_ac": data.get("current_ac", 0),
                    "power_ac": data.get("power_ac", 0),
                    "frequency": data.get("frequency", 0),
                    "temperature": data.get("temperature", 0),
                    "efficiency": data.get("efficiency", 0),
                    "state": data.get("state", "UNKNOWN"),
                    "error_code": data.get("error_code", 0),
                    "battery_soc": data.get("battery_soc", 0),
                    "battery_voltage": data.get("battery_voltage", 0)
                }
            }
            
            influx_write_api.write(
                bucket=CONFIG["influxdb"]["bucket"],
                record=point
            )
            
        except Exception as e:
            logger.error(f"Error saving to InfluxDB: {e}")
    
    def broadcast_to_websockets(self, inverter_id: str, data: dict):
        """Рассылка данных по WebSocket соединениям"""
        if not websocket_connections:
            return
        
        message = {
            "type": "telemetry",
            "inverter_id": inverter_id,
            "data": data,
            "timestamp": datetime.utcnow().isoformat()
        }
        
        # Отправка всем подключенным клиентам
        for websocket_id, websocket in websocket_connections.items():
            try:
                asyncio.create_task(websocket.send_text(json.dumps(message)))
            except Exception as e:
                logger.error(f"Error sending WebSocket message: {e}")
                # Удаление мертвого соединения
                del websocket_connections[websocket_id]

# Инициализация менеджеров
mqtt_manager = MQTTManager()

# Эндпоинты для Prometheus
@app.get("/metrics")
async def metrics():
    """Экспорт метрик Prometheus"""
    return prometheus_client.generate_latest()

# Health check
@app.get("/api/v1/health")
async def health_check():
    """Проверка здоровья системы"""
    return {
        "status": "healthy",
        "timestamp": datetime.utcnow().isoformat(),
        "mqtt_connected": mqtt_manager.connected,
        "influxdb_connected": influx_client is not None,
        "active_connections": len(websocket_connections),
        "uptime": time.time()
    }

# Аутентификация
def verify_token(credentials: HTTPAuthorizationCredentials = Depends(security)):
    """Верификация JWT токена"""
    try:
        payload = jwt.decode(
            credentials.credentials,
            CONFIG["jwt"]["secret"],
            algorithms=[CONFIG["jwt"]["algorithm"]]
        )
        return payload
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Token expired")
    except jwt.JWTError:
        raise HTTPException(status_code=401, detail="Invalid token")

# Логин
@app.post("/api/v1/auth/login", response_model=LoginResponse)
async def login(request: LoginRequest):
    """Аутентификация пользователя"""
    # Здесь должна быть проверка пользователя из базы данных
    # Для примера используем hardcoded значения
    if request.username == "admin" and request.password == "admin":
        access_token = jwt.create_access_token(
            data={"sub": request.username},
            expires_delta=timedelta(seconds=CONFIG["jwt"]["expiration"])
        )
        return LoginResponse(
            access_token=access_token,
            token_type="bearer",
            expires_in=CONFIG["jwt"]["expiration"]
        )
    else:
        raise HTTPException(status_code=401, detail="Invalid credentials")

# WebSocket для реального времени
@app.websocket("/api/v1/live")
async def websocket_endpoint(websocket: WebSocket):
    """WebSocket endpoint для реального времени"""
    await websocket.accept()
    websocket_id = id(websocket)
    websocket_connections[websocket_id] = websocket
    ACTIVE_CONNECTIONS.inc()
    
    try:
        while True:
            # Отправка текущего состояния
            if current_inverter_states:
                message = {
                    "type": "status_update",
                    "inverters": {k: v.dict() for k, v in current_inverter_states.items()},
                    "timestamp": datetime.utcnow().isoformat()
                }
                await websocket.send_text(json.dumps(message))
            
            # Ожидание 1 секунды
            await asyncio.sleep(1)
            
    except WebSocketDisconnect:
        del websocket_connections[websocket_id]
        ACTIVE_CONNECTIONS.dec()
        logger.info(f"WebSocket disconnected: {websocket_id}")

# API эндпоинты
@app.get("/api/v1/inverters")
async def get_inverters():
    """Получение списка инверторов"""
    return {
        "inverters": list(current_inverter_states.keys()),
        "count": len(current_inverter_states)
    }

@app.get("/api/v1/inverters/{inverter_id}")
async def get_inverter(inverter_id: str):
    """Получение состояния конкретного инвертора"""
    if inverter_id not in current_inverter_states:
        raise HTTPException(status_code=404, detail="Inverter not found")
    
    return current_inverter_states[inverter_id].dict()

@app.get("/api/v1/telemetry/{inverter_id}")
async def get_telemetry(inverter_id: str, hours: int = 24):
    """Получение телеметрии для инвертора"""
    # Здесь должна быть реализация запроса к InfluxDB
    return {
        "inverter_id": inverter_id,
        "hours": hours,
        "message": "Telemetry data would be fetched from InfluxDB"
    }

# Startup и Shutdown события
@app.on_event("startup")
async def startup_event():
    logger.info("Starting SunLink API Server")
    
    # Запуск метрик uptime
    SYSTEM_UPTIME.set(time.time())
    
    # Подключение к MQTT
    if not mqtt_manager.connect():
        logger.error("Failed to connect to MQTT, application will start with limited functionality")
        MQTT_CONNECTION_STATUS.set(0)
    else:
        MQTT_CONNECTION_STATUS.set(1)
    
    # Подключение к InfluxDB
    try:
        influx_client = InfluxDBClient(
            url=CONFIG["influxdb"]["url"],
            token=CONFIG["influxdb"]["token"],
            org=CONFIG["influxdb"]["org"]
        )
        influx_write_api = influx_client.write_api(write_options=SYNCHRONOUS)
        logger.info("Connected to InfluxDB")
    except Exception as e:
        logger.error(f"Failed to connect to InfluxDB: {e}")
    
    logger.info("SunLink API Server started successfully")

@app.on_event("shutdown")
async def shutdown_event():
    logger.info("Shutting down SunLink API Server")
    
    # Отключение от MQTT
    if mqtt_manager.client:
        mqtt_manager.client.loop_stop()
        mqtt_manager.client.disconnect()
        MQTT_CONNECTION_STATUS.set(0)
    
    # Отключение от InfluxDB
    if influx_client:
        influx_client.close()
    
    logger.info("SunLink API Server shutdown complete")

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host=CONFIG["server"]["host"],
        port=CONFIG["server"]["port"],
        reload=False
    )