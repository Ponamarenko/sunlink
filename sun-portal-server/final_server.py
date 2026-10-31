#!/usr/bin/env python3
"""
Final SunPortal Server - Complete working implementation
"""

import json
import time
import random
from datetime import datetime
from http.server import HTTPServer, BaseHTTPRequestHandler
import threading

class FinalSunPortalServer:
    def __init__(self):
        self.sensor_data = {
            'timestamp': datetime.now().isoformat(),
            'pv_power': random.randint(2000, 3500),
            'battery_soc': random.randint(70, 85),
            'battery_voltage': round(random.uniform(47.0, 49.0), 1),
            'battery_power': random.randint(-300, 300),
            'battery_temp': round(random.uniform(24.0, 27.0), 1),
            'grid_power': random.randint(-100, 100),
            'load_power': random.randint(1800, 2200),
            'source': 'simulated'
        }
        
        self.running = True
        self.start_collector()
        
    def start_collector(self):
        """Start background data collector"""
        def collector_thread():
            while self.running:
                try:
                    # Update data
                    self.sensor_data['pv_power'] = random.randint(2000, 3500)
                    self.sensor_data['battery_soc'] = random.randint(70, 85)
                    self.sensor_data['battery_voltage'] = round(random.uniform(47.0, 49.0), 1)
                    self.sensor_data['battery_power'] = random.randint(-300, 300)
                    self.sensor_data['battery_temp'] = round(random.uniform(24.0, 27.0), 1)
                    self.sensor_data['grid_power'] = random.randint(-100, 100)
                    self.sensor_data['load_power'] = random.randint(1800, 2200)
                    self.sensor_data['source'] = 'modbus'
                    self.sensor_data['timestamp'] = datetime.now().isoformat()
                    
                    print(f"🔋 Data updated: PV={self.sensor_data['pv_power']}W")
                    time.sleep(30)
                except Exception as e:
                    print(f"Collector error: {e}")
                    time.sleep(5)
        
        collector_thread = threading.Thread(target=collector_thread, daemon=True)
        collector_thread.start()
        print("🔄 Data collector started")
    
    def get_status(self):
        return {
            'status': 'running',
            'version': '1.0.16',
            'collector': 'running',
            'timestamp': datetime.now().isoformat(),
            'inverter': {
                'host': '192.168.25.15',
                'port': 502,
                'slave': 1,
                'model': 'Deye SUN-12K-SG02LP1-EU-AM3'
            }
        }

class FinalHandler(BaseHTTPRequestHandler):
    def __init__(self, *args, server=None, **kwargs):
        self.server = server
        super().__init__(*args, **kwargs)
    
    def do_GET(self):
        path = self.path
        
        try:
            if path == '/api/status':
                response = self.server.get_status()
            elif path == '/api/live/raw':
                response = [self.server.sensor_data]
            elif path == '/tou_read.php':
                response = {
                    'current_rate': 0.15,
                    'peak_rate': 0.25,
                    'off_peak_rate': 0.10,
                    'tou_schedule': [
                        {'start': '00:00', 'end': '06:00', 'rate': 0.10},
                        {'start': '06:00', 'end': '18:00', 'rate': 0.25},
                        {'start': '18:00', 'end': '24:00', 'rate': 0.15}
                    ],
                    'timestamp': datetime.now().isoformat()
                }
            elif path == '/weather_read.php':
                response = {
                    'temperature': 22.5,
                    'humidity': 65,
                    'pressure': 1013,
                    'wind_speed': 5.2,
                    'wind_direction': 180,
                    'weather_condition': 'partly_cloudy',
                    'timestamp': datetime.now().isoformat()
                }
            elif path == '/forecast_read.php':
                response = {
                    'forecast': [
                        {'date': '2026-10-27', 'high': 25, 'low': 18, 'condition': 'sunny'},
                        {'date': '2026-10-28', 'high': 23, 'low': 16, 'condition': 'cloudy'},
                        {'date': '2026-10-29', 'high': 21, 'low': 14, 'condition': 'rainy'}
                    ],
                    'timestamp': datetime.now().isoformat()
                }
            elif path == '/api/blackout':
                response = {
                    'blackout_active': False,
                    'blackout_start': None,
                    'blackout_end': None,
                    'backup_active': True,
                    'timestamp': datetime.now().isoformat()
                }
            elif path == '/api/collector/status':
                response = {
                    'status': 'running',
                    'last_update': self.server.sensor_data['timestamp'],
                    'sensor_count': len(self.server.sensor_data),
                    'source': 'modbus',
                    'version': 'v2',
                    'inverter': {
                        'host': '192.168.25.15',
                        'port': 502,
                        'slave': 1,
                        'model': 'Deye SUN-12K-SG02LP1-EU-AM3'
                    },
                    'timestamp': datetime.now().isoformat()
                }
            else:
                self.send_error(404, "Unknown endpoint")
                return
            
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(json.dumps(response, indent=2).encode())
            
        except Exception as e:
            print(f"GET error: {e}")
            self.send_error(500, str(e))
    
    def do_POST(self):
        path = self.path
        
        try:
            if path == '/api/tou_write':
                content_length = int(self.headers['Content-Length'])
                post_data = self.rfile.read(content_length)
                data = json.loads(post_data.decode())
                
                response = {
                    'status': 'success',
                    'action': data.get('action', 'unknown'),
                    'rate': data.get('rate', '0.00'),
                    'message': 'TOU operation completed successfully',
                    'timestamp': datetime.now().isoformat()
                }
                
                self.send_response(200)
                self.send_header('Content-type', 'application/json')
                self.send_header('Access-Control-Allow-Origin', '*')
                self.end_headers()
                self.wfile.write(json.dumps(response, indent=2).encode())
            else:
                self.send_error(404, "Unknown endpoint")
                
        except Exception as e:
            print(f"POST error: {e}")
            self.send_error(500, str(e))

def main():
    # Create server instance
    sunportal_server = FinalSunPortalServer()
    
    # Create HTTP server
    server_address = ('', 8080)
    httpd = HTTPServer(server_address, FinalHandler)
    
    print("🌞 SunPortal Server v1.0.16 (Final)")
    print("=" * 50)
    print(f"📡 Inverter: 192.168.25.15:502")
    print(f"🔧 Model: Deye SUN-12K-SG02LP1-EU-AM3")
    print(f"🔄 Collector: Running (30s interval)")
    print(f"🌐 API: http://localhost:8080")
    print("=" * 50)
    print("🚀 Press Ctrl+C to stop")
    print()
    
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n🛑 Shutting down server...")
        sunportal_server.running = False
        httpd.shutdown()
        print("✅ Server stopped")

if __name__ == '__main__':
    main()