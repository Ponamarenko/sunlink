#!/usr/bin/env python3
"""
Интеграция InfluxDB для хранения исторических данных инверторов
"""

import asyncio
import logging
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any
from influxdb_client import InfluxDBClient
from influxdb_client.client.write_api import SYNCHRONOUS
from dataclasses import dataclass

logger = logging.getLogger(__name__)

@dataclass
class InfluxDBConfig:
    """Конфигурация InfluxDB"""
    url: str = "http://localhost:8086"
    token: str = "your-token"
    org: str = "your-org"
    bucket: str = "deye_data"
    retention_policy: str = "30d"

class InfluxDBManager:
    """Менеджер для работы с InfluxDB"""
    
    def __init__(self, config: InfluxDBConfig):
        self.config = config
        self.client = None
        self.write_api = None
        
    def connect(self):
        """Подключение к InfluxDB"""
        try:
            self.client = InfluxDBClient(
                url=self.config.url,
                token=self.config.token,
                org=self.config.org
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
            
    def write_inverter_data(self, inverter_id: str, data: Dict[str, Any]):
        """Запись данных инвертора в InfluxDB"""
        if not self.write_api:
            return False
            
        try:
            # Создаем точки данных для каждого параметра
            points = []
            
            # Батарея
            if "battery_soc" in data:
                points.append(self._create_point(
                    inverter_id, "battery_soc", data["battery_soc"]["value"],
                    data["battery_soc"]["unit"]
                ))
            if "battery_voltage" in data:
                points.append(self._create_point(
                    inverter_id, "battery_voltage", data["battery_voltage"]["value"],
                    data["battery_voltage"]["unit"]
                ))
            if "battery_current" in data:
                points.append(self._create_point(
                    inverter_id, "battery_current", data["battery_current"]["value"],
                    data["battery_current"]["unit"]
                ))
            if "battery_power" in data:
                points.append(self._create_point(
                    inverter_id, "battery_power", data["battery_power"]["value"],
                    data["battery_power"]["unit"]
                ))
                
            # Солнечные панели
            if "pv1_power" in data:
                points.append(self._create_point(
                    inverter_id, "pv1_power", data["pv1_power"]["value"],
                    data["pv1_power"]["unit"]
                ))
            if "pv2_power" in data:
                points.append(self._create_point(
                    inverter_id, "pv2_power", data["pv2_power"]["value"],
                    data["pv2_power"]["unit"]
                ))
            if "pv3_power" in data:
                points.append(self._create_point(
                    inverter_id, "pv3_power", data["pv3_power"]["value"],
                    data["pv3_power"]["unit"]
                ))
                
            # Сеть и нагрузка
            if "grid_voltage" in data:
                points.append(self._create_point(
                    inverter_id, "grid_voltage", data["grid_voltage"]["value"],
                    data["grid_voltage"]["unit"]
                ))
            if "grid_frequency" in data:
                points.append(self._create_point(
                    inverter_id, "grid_frequency", data["grid_frequency"]["value"],
                    data["grid_frequency"]["unit"]
                ))
            if "grid_inverter_power" in data:
                points.append(self._create_point(
                    inverter_id, "grid_inverter_power", data["grid_inverter_power"]["value"],
                    data["grid_inverter_power"]["unit"]
                ))
            if "total_load_power" in data:
                points.append(self._create_point(
                    inverter_id, "total_load_power", data["total_load_power"]["value"],
                    data["total_load_power"]["unit"]
                ))
                
            # Температура
            if "inverter_temp" in data:
                points.append(self._create_point(
                    inverter_id, "inverter_temp", data["inverter_temp"]["value"],
                    data["inverter_temp"]["unit"]
                ))
                
            # Записываем все точки
            if points:
                self.write_api.write(bucket=self.config.bucket, record=points)
                logger.debug(f"Written {len(points)} data points for inverter {inverter_id}")
                
            return True
            
        except Exception as e:
            logger.error(f"Error writing data to InfluxDB for inverter {inverter_id}: {e}")
            return False
            
    def _create_point(self, inverter_id: str, measurement: str, value: float, unit: str):
        """Создание точки данных для InfluxDB"""
        return {
            "measurement": measurement,
            "tags": {
                "inverter_id": inverter_id,
                "unit": unit
            },
            "time": datetime.utcnow(),
            "fields": {
                "value": value
            }
        }
        
    def get_inverter_data(self, inverter_id: str, start_time: datetime, end_time: datetime) -> Dict[str, List]:
        """Получение исторических данных инвертора"""
        if not self.client:
            return {}
            
        try:
            query = f'''
            from(bucket: "{self.config.bucket}")
            |> range(start: {start_time.isoformat()}, stop: {end_time.isoformat()})
            |> filter(fn: (r) => r.inverter_id == "{inverter_id}")
            |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
            '''
            
            result = self.client.query_api().query(query, org=self.config.org)
            
            # Форматируем результат
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
            logger.error(f"Error querying data from InfluxDB for inverter {inverter_id}: {e}")
            return {}
            
    def get_aggregated_data(self, start_time: datetime, end_time: datetime) -> Dict[str, Any]:
        """Получение агрегированных данных по всем инверторам"""
        if not self.client:
            return {}
            
        try:
            query = f'''
            from(bucket: "{self.config.bucket}")
            |> range(start: {start_time.isoformat()}, stop: {end_time.isoformat()})
            |> filter(fn: (r) => r._measurement == "pv1_power" or r._measurement == "pv2_power" or r._measurement == "pv3_power")
            |> group(columns: ["inverter_id"])
            |> sum()
            '''
            
            result = self.client.query_api().query(query, org=self.config.org)
            
            # Форматируем результат
            aggregated = {}
            for table in result:
                for record in table.records:
                    inverter_id = record.get_tag("inverter_id")
                    if inverter_id not in aggregated:
                        aggregated[inverter_id] = {}
                    aggregated[inverter_id][record.get_measurement()] = record.get_value()
                    
            return aggregated
            
        except Exception as e:
            logger.error(f"Error querying aggregated data from InfluxDB: {e}")
            return {}

# Интеграция с основным шлюзом
class InfluxDBIntegration:
    """Интеграция InfluxDB с MultiInverterGateway"""
    
    def __init__(self, gateway, influxdb_config: InfluxDBConfig):
        self.gateway = gateway
        self.influxdb_manager = InfluxDBManager(influxdb_config)
        self.influxdb_manager.connect()
        
    async def write_polling_data(self, results: Dict[str, Any]):
        """Запись результатов опроса в InfluxDB"""
        for inverter_id, result in results.items():
            if result["status"] == "success" and result["data"]:
                self.influxdb_manager.write_inverter_data(inverter_id, result["data"])
                
    async def get_history(self, inverter_id: str, hours: int = 24) -> Dict[str, Any]:
        """Получение истории данных за период"""
        end_time = datetime.utcnow()
        start_time = end_time - timedelta(hours=hours)
        
        return self.influxdb_manager.get_inverter_data(inverter_id, start_time, end_time)
        
    async def get_aggregated_history(self, hours: int = 24) -> Dict[str, Any]:
        """Получение агрегированной истории"""
        end_time = datetime.utcnow()
        start_time = end_time - timedelta(hours=hours)
        
        return self.influxdb_manager.get_aggregated_data(start_time, end_time)