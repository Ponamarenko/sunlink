#!/usr/bin/env python3
"""
Локальный веб-интерфейс для мониторинга и управления инверторами Deye
"""

import json
import logging
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any
from fastapi import FastAPI, HTTPException, Request, WebSocket, WebSocketDisconnect
from fastapi.responses import HTMLResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
from dataclasses import dataclass, asdict

logger = logging.getLogger(__name__)

@dataclass
class InverterCard:
    """Карточка инвертора для интерфейса"""
    id: str
    name: str
    type: str
    model: str
    status: str  # online, offline, error
    location: Dict[str, str]
    last_update: str
    metrics: Dict[str, Any]
    
    def to_dict(self) -> Dict:
        """Преобразование в словарь"""
        return asdict(self)

class WebInterfaceManager:
    """Менеджер веб-интерфейса"""
    
    def __init__(self, gateway):
        self.gateway = gateway
        self.app = FastAPI(title="Deye Gateway Web Interface", version="1.0.0")
        self.templates = None
        self.websocket_connections = []
        
        # Настройка middleware
        self.app.add_middleware(
            CORSMiddleware,
            allow_origins=["*"],
            allow_credentials=True,
            allow_methods=["*"],
            allow_headers=["*"],
        )
        
        # Регистрация роутов
        self._register_routes()
        
    def _register_routes(self):
        """Регистрация роутов"""
        
        @self.app.get("/", response_class=HTMLResponse)
        async def dashboard(request: Request):
            """Главная страница - дашборд"""
            return self.templates.TemplateResponse("dashboard.html", {
                "request": request,
                "title": "Deye Gateway Dashboard"
            })
            
        @self.app.get("/inverters", response_class=HTMLResponse)
        async def inverters_page(request: Request):
            """Страница инверторов"""
            inverters = await self._get_inverter_cards()
            return self.templates.TemplateResponse("inverters.html", {
                "request": request,
                "title": "Inverters",
                "inverters": inverters
            })
            
        @self.app.get("/inverter/{inverter_id}", response_class=HTMLResponse)
        async def inverter_detail(request: Request, inverter_id: str):
            """Детальная страница инвертора"""
            inverter = await self._get_inverter_card(inverter_id)
            if not inverter:
                raise HTTPException(status_code=404, detail="Inverter not found")
                
            return self.templates.TemplateResponse("inverter_detail.html", {
                "request": request,
                "title": f"Inverter {inverter.name}",
                "inverter": inverter
            })
            
        @self.app.get("/settings", response_class=HTMLResponse)
        async def settings_page(request: Request):
            """Страница настроек"""
            return self.templates.TemplateResponse("settings.html", {
                "request": request,
                "title": "Settings"
            })
            
        @self.app.get("/api/inverters")
        async def get_inverters_api():
            """API для получения списка инверторов"""
            inverters = await self._get_inverter_cards()
            return {"inverters": [inv.to_dict() for inv in inverters]}
            
        @self.app.get("/api/inverter/{inverter_id}")
        async def get_inverter_api(inverter_id: str):
            """API для получения данных инвертора"""
            inverter = await self._get_inverter_card(inverter_id)
            if not inverter:
                raise HTTPException(status_code=404, detail="Inverter not found")
                
            return inverter.to_dict()
            
        @self.app.get("/api/system/status")
        async def get_system_status_api():
            """API для получения статуса системы"""
            status = await self.gateway.get_system_status()
            return status
            
        @self.app.websocket("/ws")
        async def websocket_endpoint(websocket: WebSocket):
            """WebSocket для real-time обновлений"""
            await websocket.accept()
            self.websocket_connections.append(websocket)
            
            try:
                while True:
                    # Отправка обновлений каждую секунду
                    await websocket.send_text(json.dumps({
                        "type": "update",
                        "timestamp": datetime.now().isoformat(),
                        "data": await self._get_realtime_data()
                    }))
                    await asyncio.sleep(1)
                    
            except WebSocketDisconnect:
                self.websocket_connections.remove(websocket)
                
        @self.app.get("/map")
        async def map_page(request: Request):
            """Страница с картой инверторов"""
            inverters = await self._get_inverter_cards()
            return self.templates.TemplateResponse("map.html", {
                "request": request,
                "title": "Inverter Map",
                "inverters": inverters
            })
            
    async def _get_inverter_cards(self) -> List[InverterCard]:
        """Получение карточек инверторов"""
        inverters_list = []
        
        for inverter_id, inverter in self.gateway.inverters.items():
            status = await inverter.get_system_status()
            
            # Определение статуса
            if status["connection_status"]:
                status_str = "online"
            elif status["error_count"] > 0:
                status_str = "error"
            else:
                status_str = "offline"
                
            # Создание карточки
            card = InverterCard(
                id=inverter_id,
                name=inverter.name,
                type=inverter.config.type,
                model=inverter.config.model,
                status=status_str,
                location=inverter.config.location,
                last_update=status["last_update"],
                metrics=inverter.last_data
            )
            
            inverters_list.append(card)
            
        return inverters_list
        
    async def _get_inverter_card(self, inverter_id: str) -> Optional[InverterCard]:
        """Получение карточки конкретного инвертора"""
        if inverter_id not in self.gateway.inverters:
            return None
            
        inverter = self.gateway.inverters[inverter_id]
        status = await inverter.get_system_status()
        
        # Определение статуса
        if status["connection_status"]:
            status_str = "online"
        elif status["error_count"] > 0:
            status_str = "error"
        else:
            status_str = "offline"
            
        card = InverterCard(
            id=inverter_id,
            name=inverter.name,
            type=inverter.config.type,
            model=inverter.config.model,
            status=status_str,
            location=inverter.config.location,
            last_update=status["last_update"],
            metrics=inverter.last_data
        )
        
        return card
        
    async def _get_realtime_data(self) -> Dict[str, Any]:
        """Получение real-time данных"""
        aggregated_data = await self.gateway.get_aggregated_data()
        
        # Добавляем информацию о каждом инверторе
        for inverter_id, inverter in self.gateway.inverters.items():
            status = await inverter.get_system_status()
            aggregated_data["inverters"][inverter_id]["status"] = status
            
        return aggregated_data
        
    def setup_templates(self, templates_dir: str):
        """Настройка шаблонов Jinja2"""
        self.templates = Jinja2Templates(directory=templates_dir)

class TemplateGenerator:
    """Генератор HTML шаблонов"""
    
    @staticmethod
    def generate_dashboard_template() -> str:
        """Генерация шаблона дашборда"""
        return '''
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{ title }}</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
        .header { background: #2c3e50; color: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; }
        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; margin-bottom: 20px; }
        .stat-card { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .stat-value { font-size: 2em; font-weight: bold; color: #3498db; }
        .stat-label { color: #7f8c8d; margin-top: 5px; }
        .inverters-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 15px; }
        .inverter-card { background: white; padding: 15px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .status-online { color: #27ae60; }
        .status-offline { color: #e74c3c; }
        .status-error { color: #f39c12; }
        .chart-container { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); margin-top: 20px; }
    </style>
</head>
<body>
    <div class="header">
        <h1>Deye Gateway Dashboard</h1>
        <p>Real-time monitoring of multiple Deye inverters</p>
    </div>
    
    <div class="stats-grid">
        <div class="stat-card">
            <div class="stat-value" id="total-inverters">-</div>
            <div class="stat-label">Total Inverters</div>
        </div>
        <div class="stat-card">
            <div class="stat-value" id="online-inverters">-</div>
            <div class="stat-label">Online Inverters</div>
        </div>
        <div class="stat-card">
            <div class="stat-value" id="total-power">-</div>
            <div class="stat-label">Total Power (W)</div>
        </div>
        <div class="stat-card">
            <div class="stat-value" id="last-update">-</div>
            <div class="stat-label">Last Update</div>
        </div>
    </div>
    
    <div class="chart-container">
        <h2>Power Generation</h2>
        <canvas id="powerChart" width="400" height="200"></canvas>
    </div>
    
    <div class="inverters-grid" id="inverters-grid">
        <!-- Inverter cards will be inserted here -->
    </div>
    
    <script>
        let powerChart;
        let updateInterval;
        
        // Initialize chart
        function initChart() {
            const ctx = document.getElementById('powerChart').getContext('2d');
            powerChart = new Chart(ctx, {
                type: 'line',
                data: {
                    labels: [],
                    datasets: []
                },
                options: {
                    responsive: true,
                    scales: {
                        y: {
                            beginAtZero: true
                        }
                    }
                }
            });
        }
        
        // Update dashboard data
        async function updateDashboard() {
            try {
                const response = await fetch('/api/inverters');
                const data = await response.json();
                
                // Update stats
                document.getElementById('total-inverters').textContent = data.inverters.length;
                document.getElementById('online-inverters').textContent = 
                    data.inverters.filter(inv => inv.status === 'online').length;
                document.getElementById('total-power').textContent = 
                    data.inverters.reduce((sum, inv) => sum + (inv.metrics?.pv1_power?.value || 0), 0);
                document.getElementById('last-update').textContent = new Date().toLocaleTimeString();
                
                // Update inverter cards
                const grid = document.getElementById('inverters-grid');
                grid.innerHTML = '';
                
                data.inverters.forEach(inverter => {
                    const card = document.createElement('div');
                    card.className = 'inverter-card';
                    card.innerHTML = `
                        <h3>${inverter.name}</h3>
                        <p><strong>Type:</strong> ${inverter.type}</p>
                        <p><strong>Model:</strong> ${inverter.model}</p>
                        <p><strong>Status:</strong> <span class="status-${inverter.status}">${inverter.status}</span></p>
                        <p><strong>Last Update:</strong> ${new Date(inverter.last_update).toLocaleTimeString()}</p>
                        ${inverter.metrics ? `
                            <p><strong>PV1 Power:</strong> ${inverter.metrics.pv1_power?.value || 0} W</p>
                            <p><strong>Battery SOC:</strong> ${inverter.metrics.battery_soc?.value || 0}%</p>
                        ` : ''}
                    `;
                    grid.appendChild(card);
                });
                
            } catch (error) {
                console.error('Error updating dashboard:', error);
            }
        }
        
        // Initialize on page load
        document.addEventListener('DOMContentLoaded', function() {
            initChart();
            updateDashboard();
            
            // Update every 5 seconds
            updateInterval = setInterval(updateDashboard, 5000);
        });
        
        // Cleanup on page unload
        window.addEventListener('beforeunload', function() {
            if (updateInterval) {
                clearInterval(updateInterval);
            }
        });
    </script>
</body>
</html>
        '''
        
    @staticmethod
    def generate_inverters_template() -> str:
        """Генерация шаблона страницы инверторов"""
        return '''
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{ title }}</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
        .header { background: #2c3e50; color: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; }
        .inverter-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; }
        .inverter-card { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .status-online { color: #27ae60; }
        .status-offline { color: #e74c3c; }
        .status-error { color: #f39c12; }
        .controls { margin-top: 15px; }
        .btn { padding: 8px 16px; margin: 5px; border: none; border-radius: 4px; cursor: pointer; }
        .btn-primary { background: #3498db; color: white; }
        .btn-danger { background: #e74c3c; color: white; }
    </style>
</head>
<body>
    <div class="header">
        <h1>{{ title }}</h1>
        <p>Manage and monitor all Deye inverters</p>
    </div>
    
    <div class="inverter-grid" id="inverter-grid">
        <!-- Inverter cards will be inserted here -->
    </div>
    
    <script>
        async function loadInverters() {
            try {
                const response = await fetch('/api/inverters');
                const data = await response.json();
                
                const grid = document.getElementById('inverter-grid');
                grid.innerHTML = '';
                
                data.inverters.forEach(inverter => {
                    const card = document.createElement('div');
                    card.className = 'inverter-card';
                    card.innerHTML = `
                        <h3>${inverter.name}</h3>
                        <p><strong>ID:</strong> ${inverter.id}</p>
                        <p><strong>Type:</strong> ${inverter.type}</p>
                        <p><strong>Model:</strong> ${inverter.model}</p>
                        <p><strong>Status:</strong> <span class="status-${inverter.status}">${inverter.status}</span></p>
                        <p><strong>Location:</strong> ${inverter.location.latitude}, ${inverter.location.longitude}</p>
                        <p><strong>Last Update:</strong> ${new Date(inverter.last_update).toLocaleTimeString()}</p>
                        <div class="controls">
                            <a href="/inverter/${inverter.id}" class="btn btn-primary">View Details</a>
                            <button class="btn btn-danger" onclick="emergencyStop('${inverter.id}')">Emergency Stop</button>
                        </div>
                    `;
                    grid.appendChild(card);
                });
                
            } catch (error) {
                console.error('Error loading inverters:', error);
            }
        }
        
        async function emergencyStop(inverterId) {
            if (confirm('Are you sure you want to emergency stop this inverter?')) {
                try {
                    const response = await fetch(`/api/inverters/${inverterId}/emergency-stop`, {
                        method: 'POST'
                    });
                    const result = await response.json();
                    alert(result.message);
                    loadInverters();
                } catch (error) {
                    console.error('Error stopping inverter:', error);
                }
            }
        }
        
        document.addEventListener('DOMContentLoaded', loadInverters);
    </script>
</body>
</html>
        '''
        
    @staticmethod
    def generate_map_template() -> str:
        """Генерация шаблона с картой"""
        return '''
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{ title }}</title>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.7.1/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js"></script>
    <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 0; }
        #map { height: 100vh; width: 100%; }
        .inverter-popup { padding: 10px; }
        .status-online { color: #27ae60; }
        .status-offline { color: #e74c3c; }
        .status-error { color: #f39c12; }
    </style>
</head>
<body>
    <div id="map"></div>
    
    <script>
        let map;
        let inverters = [];
        
        function initMap() {
            map = L.map('map').setView([55.7558, 37.6173], 10);
            
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '© OpenStreetMap contributors'
            }).addTo(map);
            
            loadInverters();
        }
        
        async function loadInverters() {
            try {
                const response = await fetch('/api/inverters');
                const data = await response.json();
                
                data.inverters.forEach(inverter => {
                    const marker = L.marker([inverter.location.latitude, inverter.location.longitude])
                        .addTo(map);
                    
                    const popupContent = `
                        <div class="inverter-popup">
                            <h3>${inverter.name}</h3>
                            <p><strong>Type:</strong> ${inverter.type}</p>
                            <p><strong>Model:</strong> ${inverter.model}</p>
                            <p><strong>Status:</strong> <span class="status-${inverter.status}">${inverter.status}</span></p>
                            <p><strong>Last Update:</strong> ${new Date(inverter.last_update).toLocaleTimeString()}</p>
                            <a href="/inverter/${inverter.id}" class="btn">View Details</a>
                        </div>
                    `;
                    
                    marker.bindPopup(popupContent);
                    inverters.push(marker);
                });
                
            } catch (error) {
                console.error('Error loading inverters:', error);
            }
        }
        
        document.addEventListener('DOMContentLoaded', initMap);
    </script>
</body>
</html>
        '''

# Создание шаблонов
def create_templates():
    """Создание HTML шаблонов"""
    generator = TemplateGenerator()
    
    # Создаем директорию для шаблонов
    import os
    os.makedirs('/tmp/templates', exist_ok=True)
    
    # Генерируем шаблоны
    with open('/tmp/templates/dashboard.html', 'w') as f:
        f.write(generator.generate_dashboard_template())
        
    with open('/tmp/templates/inverters.html', 'w') as f:
        f.write(generator.generate_inverters_template())
        
    with open('/tmp/templates/map.html', 'w') as f:
        f.write(generator.generate_map_template())
        
    return '/tmp/templates'

# Пример использования
if __name__ == "__main__":
    # Создаем шаблоны
    templates_dir = create_templates()
    
    # Инициализируем веб-интерфейс
    web_interface = WebInterfaceManager(gateway)
    web_interface.setup_templates(templates_dir)
    
    # Запускаем сервер
    uvicorn.run(web_interface.app, host="0.0.0.0", port=8080)