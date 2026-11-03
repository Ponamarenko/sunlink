#!/usr/bin/env python3
"""
Deye Gateway Backend API
Основной файл FastAPI приложения для работы с MQTT и InfluxDB
"""

import asyncio
import json
import logging
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any
from fastapi import FastAPI, HTTPException, WebSocket, WebSocketDisconnect, Depends, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel
import paho.mqtt.client as mqtt
import uvicorn
from influxdb_client import InfluxDBClient
from influxdb_client.client.write_api import SYNCHRONOUS
import jwt
from dataclasses import dataclass, asdict

# Настройка логирования
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Конфигурация
CONFIG = {
    "mqtt": {
        "host": "mosquitto",
        "port": 1883,
        "username": "deye_user",
        "password": "deye_pass",
        "topics": {
            "telemetry": "deye/telemetry/+",
            "status": "deye/status/+",
            "control": "deye/control/+",
            "response": "deye/response/+",
            "error": "deye/error/+",
            "aggregate": "deye/aggregate/+"
        }
    },
    "influxdb": {
        "url": "http://influxdb:8086",
        "token": "your-super-secret-token",
        "org": "deye",
        "bucket": "deye_data"
    },
    "jwt": {
        "secret": "your-jwt-secret-key",
        "algorithm": "HS256",
        "expiration": 3600  # 1 час
    },
    "server": {
        "host": "0.0.0.0",
        "port": 8000
    }
}

# Модели данных
class TelemetryData(BaseModel):
    inverter_id: str
    timestamp: str
    data: Dict[str, Any]

class ControlCommand(BaseModel):
    inverter_id: str
    register: str
    value: float

class User(BaseModel):
    id: str
    username: str
    email: str
    role: str

@dataclass
class InverterInfo:
    id: str
    name: str
    type: str
    model: str
    status: str
    location: Dict[str, str]
    last_update: str

# FastAPI приложение
app = FastAPI(
    title="Deye Gateway API",
    description="API для мониторинга и управления инверторами Deye",
    version="1.0.0"
)

# Настройка CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Глобальные переменные
mqtt_client = None
influx_client = None
influx_write_api = None
websocket_connections = []
security = HTTPBearer()

class MQTTManager:
    """Менеджер MQTT соединений"""
    
    def __init__(self, config: Dict):
        self.config = config
        self.client = None
        self.connected = False
        self.telemetry_data = {}
        
    def connect(self):
        """Подключение к MQTT брокеру"""
        try:
            self.client = mqtt.Client()
            self.client.username_pw_set(
                self.config["username"],
                self.config["password"]
            )
            
            # Callback функции
            self.client.on_connect = self._on_connect
            self.client.on_message = self._on_message
            self.client.on_disconnect = self._on_disconnect
            
            # Подключение
            self.client.connect(self.config["host"], self.config["port"], 60)
            self.client.loop_start()
            
            logger.info("MQTT client connected")
            
        except Exception as e:
            logger.error(f"Failed to connect to MQTT: {e}")
            
    def disconnect(self):
        """Отключение от MQTT брокера"""
        if self.client:
            self.client.loop_stop()
            self.client.disconnect()
            self.connected = False
            logger.info("MQTT client disconnected")
            
    def _on_connect(self, client, userdata, flags, rc):
        """Callback при подключении"""
        if rc == 0:
            self.connected = True
            logger.info("Connected to MQTT broker")
            
            # Подписка на топики
            topics = [
                self.config["topics"]["telemetry"],
                self.config["topics"]["status"],
                self.config["topics"]["response"],
                self.config["topics"]["error"]
            ]
            
            for topic in topics:
                client.subscribe(topic)
                logger.info(f"Subscribed to: {topic}")
        else:
            logger.error(f"Failed to connect to MQTT, return code {rc}")
            
    def _on_message(self, client, userdata, msg):
        """Callback при получении сообщения"""
        try:
            topic = msg.topic
            payload = json.loads(msg.payload.decode())
            
            # Обработка разных типов сообщений
            if topic.startswith(self.config["topics"]["telemetry"]):
                inverter_id = topic.split("/")[-1]
                self.telemetry_data[inverter_id] = payload
                logger.debug(f"Received telemetry from {inverter_id}")
                
                # Рассылка через WebSocket
                asyncio.create_task(self._broadcast_websocket({
                    "type": "telemetry",
                    "inverter_id": inverter_id,
                    "data": payload,
                    "timestamp": datetime.now().isoformat()
                }))
                
            elif topic.startswith(self.config["topics"]["status"]):
                inverter_id = topic.split("/")[-1]
                logger.info(f"Status update from {inverter_id}: {payload}")
                
            elif topic.startswith(self.config["topics"]["response"]):
                inverter_id = topic.split("/")[-1]
                logger.info(f"Command response from {inverter_id}: {payload}")
                
            elif topic.startswith(self.config["topics"]["error"]):
                inverter_id = topic.split("/")[-1]
                logger.error(f"Error from {inverter_id}: {payload}")
                
        except Exception as e:
            logger.error(f"Error processing MQTT message: {e}")
            
    def _on_disconnect(self, client, userdata, rc):
        """Callback при отключении"""
        self.connected = False
        logger.warning(f"Disconnected from MQTT broker, return code {rc}")
        
    def publish_control_command(self, inverter_id: str, register: str, value: float):
        """Публикация команды управления"""
        if not self.connected:
            raise HTTPException(status_code=503, detail="MQTT client not connected")
            
        command = {
            "register": register,
            "value": value,
            "timestamp": datetime.now().isoformat()
        }
        
        topic = self.config["topics"]["control"].replace("+", inverter_id)
        result = self.client.publish(topic, json.dumps(command))
        
        logger.info(f"Published control command to {topic}: {command}")
        return result
        
    async def _broadcast_websocket(self, message: Dict[str, Any]):
        """Рассылка сообщения через WebSocket"""
        if not websocket_connections:
            return
            
        message_str = json.dumps(message)
        disconnected = []
        
        for websocket in websocket_connections:
            try:
                await websocket.send_text(message_str)
            except:
                disconnected.append(websocket)
                
        # Удаление отключенных клиентов
        for websocket in disconnected:
            websocket_connections.remove(websocket)

class InfluxDBManager:
    """Менеджер InfluxDB соединений"""
    
    def __init__(self, config: Dict):
        self.config = config
        self.client = None
        self.write_api = None
        
    def connect(self):
        """Подключение к InfluxDB"""
        try:
            self.client = InfluxDBClient(
                url=self.config["url"],
                token=self.config["token"],
                org=self.config["org"]
            )
            self.write_api = self.client.write_api(write_options=SYNCHRONOUS)
            logger.info("Connected to InfluxDB")
            return True
        except Exception as e:
            logger.error(f"Failed to connect to InfluxDB: {e}")
            return False
            
    def disconnect(self):
        """Отключение от InfluxDB"""
        if self.client:
            self.client.close()
            logger.info("Disconnected from InfluxDB")
            
    def write_telemetry(self, inverter_id: str, data: Dict[str, Any]):
        """Запись телеметрии в InfluxDB"""
        if not self.write_api:
            return False
            
        try:
            # Создание точек данных
            points = []
            
            for key, value in data.items():
                if isinstance(value, (int, float)):
                    point = {
                        "measurement": key,
                        "tags": {
                            "inverter_id": inverter_id,
                            "unit": value.get("unit", "")
                        },
                        "time": datetime.utcnow(),
                        "fields": {
                            "value": value["value"]
                        }
                    }
                    points.append(point)
                    
            if points:
                self.write_api.write(
                    bucket=self.config["bucket"],
                    record=points
                )
                logger.debug(f"Written {len(points)} data points for {inverter_id}")
                
            return True
            
        except Exception as e:
            logger.error(f"Error writing to InfluxDB: {e}")
            return False
            
    def get_inverter_data(self, inverter_id: str, start_time: datetime, end_time: datetime) -> Dict[str, Any]:
        """Получение исторических данных инвертора"""
        if not self.client:
            return {}
            
        try:
            query = f'''
            from(bucket: "{self.config["bucket"]}")
            |> range(start: {start_time.isoformat()}, stop: {end_time.isoformat()})
            |> filter(fn: (r) => r.inverter_id == "{inverter_id}")
            |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
            '''
            
            result = self.client.query_api().query(query, org=self.config["org"])
            
            # Форматирование результата
            data = {}
            for table in result:
                for record in table.records:
                    measurement = record.get_measurement()
                    if measurement not in data:
                        data[measurement] = []
                    data[measurement].append({
                        "time": record.get_time().isoformat(),
                        "value": record.get_value()
                    })
                    
            return data
            
        except Exception as e:
            logger.error(f"Error querying InfluxDB: {e}")
            return {}

# Инициализация менеджеров
mqtt_manager = MQTTManager(CONFIG["mqtt"])
influx_manager = InfluxDBManager(CONFIG["influxdb"])

# Startup и Shutdown события
@app.on_event("startup")
async def startup_event():
    """Запуск при старте приложения"""
    logger.info("Starting Deye Gateway API")
    
    # Подключение к MQTT
    mqtt_manager.connect()
    
    # Подключение к InfluxDB
    influx_manager.connect()
    
    logger.info("Deye Gateway API started")

@app.on_event("shutdown")
async def shutdown_event():
    """Остановка при завершении работы"""
    logger.info("Shutting down Deye Gateway API")
    
    # Отключение от MQTT
    mqtt_manager.disconnect()
    
    # Отключение от InfluxDB
    influx_manager.disconnect()
    
    logger.info("Deye Gateway API stopped")

# JWT утилиты
def create_jwt_token(user: User) -> str:
    """Создание JWT токена"""
    payload = {
        "user_id": user.id,
        "username": user.username,
        "role": user.role,
        "exp": datetime.utcnow() + timedelta(seconds=CONFIG["jwt"]["expiration"]),
        "iat": datetime.utcnow()
    }
    
    return jwt.encode(payload, CONFIG["jwt"]["secret"], algorithm=CONFIG["jwt"]["algorithm"])

def verify_jwt_token(token: str) -> Optional[User]:
    """Проверка JWT токена"""
    try:
        payload = jwt.decode(token, CONFIG["jwt"]["secret"], algorithms=[CONFIG["jwt"]["algorithm"]])
        return User(
            id=payload["user_id"],
            username=payload["username"],
            email="user@example.com",
            role=payload["role"]
        )
    except jwt.PyJWTError:
        return None

# API endpoints
@app.get("/")
async def root():
    """Базовый endpoint"""
    return {"message": "Deye Gateway API", "version": "1.0.0"}

@app.get("/health")
async def health_check():
    """Проверка здоровья системы"""
    return {
        "status": "healthy",
        "mqtt_connected": mqtt_manager.connected,
        "influxdb_connected": influx_manager.client is not None,
        "timestamp": datetime.now().isoformat()
    }

@app.get("/inverters")
async def get_inverters():
    """Получение списка всех инверторов"""
    inverters = []
    
    # Получаем данные из MQTT
    for inverter_id, data in mqtt_manager.telemetry_data.items():
        inverters.append({
            "id": inverter_id,
            "name": f"Inverter {inverter_id}",
            "type": "deye",
            "model": "SUN-12K-SG02LP1-EU-AM3",
            "status": "online" if data else "offline",
            "last_update": data.get("timestamp", datetime.now().isoformat()) if data else None,
            "location": {
                "latitude": 55.7558,
                "longitude": 37.6173
            }
        })
    
    return {"inverters": inverters}

@app.get("/inverters/{inverter_id}")
async def get_inverter(inverter_id: str):
    """Получение информации об инверторе"""
    if inverter_id not in mqtt_manager.telemetry_data:
        raise HTTPException(status_code=404, detail="Inverter not found")
    
    data = mqtt_manager.telemetry_data[inverter_id]
    
    return {
        "id": inverter_id,
        "name": f"Inverter {inverter_id}",
        "type": "deye",
        "model": "SUN-12K-SG02LP1-EU-AM3",
        "status": "online",
        "data": data,
        "timestamp": datetime.now().isoformat()
    }

@app.get("/inverters/{inverter_id}/history")
async def get_inverter_history(
    inverter_id: str,
    hours: int = 24,
    resolution: str = "1m"
):
    """Получение исторических данных инвертора"""
    if inverter_id not in mqtt_manager.telemetry_data:
        raise HTTPException(status_code=404, detail="Inverter not found")
    
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=hours)
    
    data = influx_manager.get_inverter_data(inverter_id, start_time, end_time)
    
    return {
        "inverter_id": inverter_id,
        "data": data,
        "period": f"{hours}h",
        "resolution": resolution,
        "timestamp": datetime.now().isoformat()
    }

@app.post("/inverters/{inverter_id}/control")
async def control_inverter(inverter_id: str, command: ControlCommand):
    """Управление инвертором"""
    if inverter_id not in mqtt_manager.telemetry_data:
        raise HTTPException(status_code=404, detail="Inverter not found")
    
    try:
        result = mqtt_manager.publish_control_command(
            inverter_id,
            command.register,
            command.value
        )
        
        return {
            "success": True,
            "inverter_id": inverter_id,
            "register": command.register,
            "value": command.value,
            "timestamp": datetime.now().isoformat()
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/aggregate")
async def get_aggregated_data():
    """Получение агрегированных данных по всем инверторам"""
    aggregated = {
        "timestamp": datetime.now().isoformat(),
        "total_inverters": len(mqtt_manager.telemetry_data),
        "online_inverters": len(mqtt_manager.telemetry_data),
        "total_power": 0,
        "inverters": {}
    }
    
    for inverter_id, data in mqtt_manager.telemetry_data.items():
        # Суммируем мощность
        total_power = 0
        for key, value in data.items():
            if "power" in key.lower() and isinstance(value, dict):
                total_power += value.get("value", 0)
                
        aggregated["total_power"] += total_power
        aggregated["inverters"][inverter_id] = {
            "id": inverter_id,
            "name": f"Inverter {inverter_id}",
            "power": total_power,
            "data": data
        }
    
    return aggregated

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    """WebSocket для real-time обновлений"""
    await websocket.accept()
    websocket_connections.append(websocket)
    
    try:
        while True:
            # Отправка агрегированных данных
            aggregated_data = await get_aggregated_data()
            message = {
                "type": "aggregated_data",
                "data": aggregated_data,
                "timestamp": datetime.now().isoformat()
            }
            
            await websocket.send_text(json.dumps(message))
            await asyncio.sleep(5)  # Обновление каждые 5 секунд
            
    except WebSocketDisconnect:
        websocket_connections.remove(websocket)

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host=CONFIG["server"]["host"],
        port=CONFIG["server"]["port"],
        reload=False,
        log_level="info"
    )