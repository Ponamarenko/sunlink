#!/usr/bin/env python3
import json
import time
from http.server import HTTPServer, BaseHTTPRequestHandler
import urllib.parse as urlparse

class SunPortalAPI(BaseHTTPRequestHandler):
    def do_GET(self):
        parsed_path = urlparse.urlparse(self.path)
        path = parsed_path.path
        
        if path == '/api/status':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            response = {
                "status": "ok",
                "timestamp": time.time(),
                "app": "SunPortal",
                "version": "1.0.16",
                "message": "SunPortal API is running"
            }
            self.wfile.write(json.dumps(response).encode())
            
        elif path == '/api/inverter/data':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            response = {
                "status": "ok",
                "timestamp": time.time(),
                "data": {
                    "power": 2.5,
                    "voltage": 230.0,
                    "current": 10.9,
                    "frequency": 50.0,
                    "temperature": 35.2,
                    "efficiency": 95.8,
                    "energy_today": 12.5,
                    "energy_total": 1250.8
                },
                "inverter": "Deye SUN-12K-SG02LP1-EU-AM3",
                "ip": "192.168.25.15:502"
            }
            self.wfile.write(json.dumps(response).encode())
            
        elif path == '/api/test/connection':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            response = {
                "status": "connected",
                "server": "SunPortal Server",
                "response_time": 0.045,
                "modbus_status": "active",
                "last_update": time.time()
            }
            self.wfile.write(json.dumps(response).encode())
            
        elif path == '/':
            self.send_response(200)
            self.send_header('Content-type', 'text/html; charset=utf-8')
            self.end_headers()
            html = """
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SunPortal API Тест</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }
        .container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
        h1 { color: #2c3e50; text-align: center; }
        .api-test { margin: 20px 0; padding: 15px; background: #ecf0f1; border-radius: 5px; }
        button { padding: 10px 20px; margin: 5px; border: none; border-radius: 5px; cursor: pointer; font-size: 14px; }
        .btn-primary { background: #3498db; color: white; }
        .btn-success { background: #27ae60; color: white; }
        .btn-warning { background: #f39c12; color: white; }
        #result { margin-top: 20px; padding: 15px; background: #fff; border-radius: 5px; border-left: 4px solid #3498db; }
        pre { background: #2c3e50; color: #ecf0f1; padding: 15px; border-radius: 5px; overflow-x: auto; }
    </style>
</head>
<body>
    <div class="container">
        <h1>🌞 SunPortal API Тест</h1>
        
        <div class="api-test">
            <h3>API Тестирование</h3>
            <button class="btn-primary" onclick="testStatus()">📊 Статус</button>
            <button class="btn-success" onclick="testInverterData()">🔌 Данные инвертера</button>
            <button class="btn-warning" onclick="testConnection()">🔗 Соединение</button>
        </div>
        
        <div id="result"></div>
    </div>

    <script>
        async function testAPI(endpoint, name) {
            const result = document.getElementById('result');
            result.innerHTML = `<p>🔄 Тестирование ${name}...</p>`;
            
            try {
                const response = await fetch(endpoint);
                const data = await response.json();
                result.innerHTML = `
                    <h4>✅ ${name} - Успешно!</h4>
                    <pre>${JSON.stringify(data, null, 2)}</pre>
                `;
            } catch (error) {
                result.innerHTML = `
                    <h4>❌ Ошибка</h4>
                    <p>${error.message}</p>
                `;
            }
        }
        
        function testStatus() {
            testAPI('/api/status', 'Статуса API');
        }
        
        function testInverterData() {
            testAPI('/api/inverter/data', 'Данных инвертера');
        }
        
        function testConnection() {
            testAPI('/api/test/connection', 'Соединения с сервером');
        }
        
        // Автоматический тест при загрузке
        window.onload = function() {
            testStatus();
        };
    </script>
</body>
</html>
            """
            self.wfile.write(html.encode())
        else:
            self.send_response(404)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            response = {"error": "Endpoint not found"}
            self.wfile.write(json.dumps(response).encode())

if __name__ == '__main__':
    server = HTTPServer(('localhost', 8081), SunPortalAPI)
    print("SunPortal API сервер запущен на http://localhost:8081")
    server.serve_forever()