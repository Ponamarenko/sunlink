#!/usr/bin/env python3
"""
Реализация WebSocket для real-time обновлений данных инверторов
"""

import asyncio
import json
import logging
from datetime import datetime
from typing import Dict, List, Set
from fastapi import WebSocket, WebSocketDisconnect
from dataclasses import dataclass, asdict

logger = logging.getLogger(__name__)

@dataclass
class WebSocketClient:
    """Класс для управления WebSocket клиентом"""
    websocket: WebSocket
    client_id: str
    subscriptions: Set[str]  # Подписки на конкретные инверторы
    connected_at: datetime
    last_activity: datetime
    
    def update_activity(self):
        """Обновление времени последней активности"""
        self.last_activity = datetime.now()
        
    def add_subscription(self, inverter_id: str):
        """Добавление подписки на инвертор"""
        self.subscriptions.add(inverter_id)
        self.update_activity()
        
    def remove_subscription(self, inverter_id: str):
        """Удаление подписки на инвертор"""
        self.subscriptions.discard(inverter_id)
        self.update_activity()
        
    def get_subscriptions(self) -> Set[str]:
        """Получение списка подписок"""
        return self.subscriptions.copy()

class WebSocketManager:
    """Менеджер WebSocket соединений"""
    
    def __init__(self):
        self.active_connections: Dict[str, WebSocketClient] = {}
        self.broadcast_task = None
        self.broadcast_interval = 2  # секунды
        self.client_timeout = 300  # 5 минут без активности
        
    async def connect(self, websocket: WebSocket) -> str:
        """Подключение нового WebSocket клиента"""
        await websocket.accept()
        
        # Генерируем уникальный ID клиента
        client_id = f"client_{len(self.active_connections) + 1}_{int(datetime.now().timestamp())}"
        
        # Создаем клиента
        client = WebSocketClient(
            websocket=websocket,
            client_id=client_id,
            subscriptions=set(),
            connected_at=datetime.now(),
            last_activity=datetime.now()
        )
        
        self.active_connections[client_id] = client
        logger.info(f"WebSocket client {client_id} connected")
        
        # Отправляем приветственное сообщение
        await self._send_to_client(client_id, {
            "type": "connection_established",
            "client_id": client_id,
            "message": "Connected to Deye Gateway",
            "timestamp": datetime.now().isoformat()
        })
        
        return client_id
        
    async def disconnect(self, client_id: str):
        """Отключение WebSocket клиента"""
        if client_id in self.active_connections:
            client = self.active_connections[client_id]
            try:
                await client.websocket.close()
            except:
                pass
            del self.active_connections[client_id]
            logger.info(f"WebSocket client {client_id} disconnected")
            
    async def subscribe_to_inverter(self, client_id: str, inverter_id: str):
        """Подписка клиента на данные инвертора"""
        if client_id in self.active_connections:
            client = self.active_connections[client_id]
            client.add_subscription(inverter_id)
            logger.info(f"Client {client_id} subscribed to inverter {inverter_id}")
            
            # Отправляем подтверждение подписки
            await self._send_to_client(client_id, {
                "type": "subscription_confirmed",
                "inverter_id": inverter_id,
                "message": f"Subscribed to {inverter_id}",
                "timestamp": datetime.now().isoformat()
            })
            
    async def unsubscribe_from_inverter(self, client_id: str, inverter_id: str):
        """Отписка клиента от данных инвертора"""
        if client_id in self.active_connections:
            client = self.active_connections[client_id]
            client.remove_subscription(inverter_id)
            logger.info(f"Client {client_id} unsubscribed from inverter {inverter_id}")
            
            # Отправляем подтверждение отписки
            await self._send_to_client(client_id, {
                "type": "subscription_removed",
                "inverter_id": inverter_id,
                "message": f"Unsubscribed from {inverter_id}",
                "timestamp": datetime.now().isoformat()
            })
            
    async def broadcast_polling_data(self, polling_results: Dict[str, Dict]):
        """Рассылка данных опроса всем подписанным клиентам"""
        if not self.active_connections:
            return
            
        # Создаем сообщение с данными опроса
        message = {
            "type": "polling_data",
            "data": polling_results,
            "timestamp": datetime.now().isoformat()
        }
        
        # Отправляем сообщение всем клиентам
        disconnected_clients = []
        for client_id, client in self.active_connections.items():
            try:
                # Проверяем, есть ли подписки у клиента
                if client.get_subscriptions():
                    # Отправляем только данные для подписанных инверторов
                    filtered_data = {
                        inverter_id: data 
                        for inverter_id, data in polling_results.items() 
                        if inverter_id in client.get_subscriptions()
                    }
                    
                    if filtered_data:  # Отправляем только если есть данные
                        message["data"] = filtered_data
                        await client.websocket.send_text(json.dumps(message))
                        
                client.update_activity()
                
            except Exception as e:
                logger.error(f"Error sending data to client {client_id}: {e}")
                disconnected_clients.append(client_id)
                
        # Удаляем отключенных клиентов
        for client_id in disconnected_clients:
            await self.disconnect(client_id)
            
    async def broadcast_aggregated_data(self, aggregated_data: Dict[str, Any]):
        """Рассылка агрегированных данных всем клиентам"""
        if not self.active_connections:
            return
            
        message = {
            "type": "aggregated_data",
            "data": aggregated_data,
            "timestamp": datetime.now().isoformat()
        }
        
        # Отправляем всем клиентам (без фильтрации)
        disconnected_clients = []
        for client_id, client in self.active_connections.items():
            try:
                await client.websocket.send_text(json.dumps(message))
                client.update_activity()
            except Exception as e:
                logger.error(f"Error sending aggregated data to client {client_id}: {e}")
                disconnected_clients.append(client_id)
                
        # Удаляем отключенных клиентов
        for client_id in disconnected_clients:
            await self.disconnect(client_id)
            
    async def send_system_status(self, system_status: Dict[str, Any]):
        """Отправка статуса системы всем клиентам"""
        if not self.active_connections:
            return
            
        message = {
            "type": "system_status",
            "data": system_status,
            "timestamp": datetime.now().isoformat()
        }
        
        # Отправляем всем клиентам
        disconnected_clients = []
        for client_id, client in self.active_connections.items():
            try:
                await client.websocket.send_text(json.dumps(message))
                client.update_activity()
            except Exception as e:
                logger.error(f"Error sending system status to client {client_id}: {e}")
                disconnected_clients.append(client_id)
                
        # Удаляем отключенных клиентов
        for client_id in disconnected_clients:
            await self.disconnect(client_id)
            
    async def _send_to_client(self, client_id: str, message: Dict[str, Any]):
        """Отправка сообщения конкретному клиенту"""
        if client_id in self.active_connections:
            client = self.active_connections[client_id]
            try:
                await client.websocket.send_text(json.dumps(message))
                client.update_activity()
            except Exception as e:
                logger.error(f"Error sending message to client {client_id}: {e}")
                await self.disconnect(client_id)
                
    async def cleanup_inactive_clients(self):
        """Очистка неактивных клиентов"""
        if not self.active_connections:
            return
            
        current_time = datetime.now()
        inactive_clients = []
        
        for client_id, client in self.active_connections.items():
            # Проверяем время последней активности
            time_diff = (current_time - client.last_activity).total_seconds()
            if time_diff > self.client_timeout:
                inactive_clients.append(client_id)
                
        # Удаляем неактивных клиентов
        for client_id in inactive_clients:
            logger.info(f"Removing inactive client {client_id} (inactive for {time_diff:.0f}s)")
            await self.disconnect(client_id)
            
    def get_client_stats(self) -> Dict[str, Any]:
        """Получение статистики по клиентам"""
        total_clients = len(self.active_connections)
        total_subscriptions = sum(len(client.subscriptions) for client in self.active_connections.values())
        
        return {
            "total_clients": total_clients,
            "total_subscriptions": total_subscriptions,
            "clients": [
                {
                    "client_id": client_id,
                    "connected_at": client.connected_at.isoformat(),
                    "last_activity": client.last_activity.isoformat(),
                    "subscriptions": list(client.subscriptions)
                }
                for client_id, client in self.active_connections.items()
            ]
        }

# Интеграция с основным шлюзом
class WebSocketIntegration:
    """Интеграция WebSocket с MultiInverterGateway"""
    
    def __init__(self, gateway):
        self.gateway = gateway
        self.websocket_manager = WebSocketManager()
        self.cleanup_task = None
        
    async def start_periodic_tasks(self):
        """Запуск периодических задач"""
        # Запускаем задачу очистки неактивных клиентов
        self.cleanup_task = asyncio.create_task(self._cleanup_periodic())
        
    async def stop_periodic_tasks(self):
        """Остановка периодических задач"""
        if self.cleanup_task:
            self.cleanup_task.cancel()
            
    async def _cleanup_periodic(self):
        """Периодическая очистка неактивных клиентов"""
        while True:
            try:
                await asyncio.sleep(60)  # Проверяем каждую минуту
                await self.websocket_manager.cleanup_inactive_clients()
            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"Error in cleanup task: {e}")
                
    async def handle_new_connection(self, websocket: WebSocket) -> str:
        """Обработка нового WebSocket соединения"""
        return await self.websocket_manager.connect(websocket)
        
    async def handle_client_disconnect(self, client_id: str):
        """Об отключении клиента"""
        await self.websocket_manager.disconnect(client_id)
        
    async def handle_client_subscription(self, client_id: str, inverter_id: str):
        """Обработка подписки клиента на инвертор"""
        await self.websocket_manager.subscribe_to_inverter(client_id, inverter_id)
        
    async def handle_client_unsubscription(self, client_id: str, inverter_id: str):
        """Обработка отписки клиента от инвертора"""
        await self.websocket_manager.unsubscribe_from_inverter(client_id, inverter_id)
        
    async def broadcast_polling_results(self, polling_results: Dict[str, Dict]):
        """Рассылка результатов опроса"""
        await self.websocket_manager.broadcast_polling_data(polling_results)
        
    async def broadcast_aggregated_results(self, aggregated_data: Dict[str, Any]):
        """Рассылка агрегированных данных"""
        await self.websocket_manager.broadcast_aggregated_data(aggregated_data)
        
    async def send_system_status(self, system_status: Dict[str, Any]):
        """Отправка статуса системы"""
        await self.websocket_manager.send_system_status(system_status)
        
    def get_websocket_stats(self) -> Dict[str, Any]:
        """Получение статистики WebSocket соединений"""
        return self.websocket_manager.get_client_stats()