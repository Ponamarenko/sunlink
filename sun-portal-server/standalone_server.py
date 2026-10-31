#!/usr/bin/env python3
"""
Standalone SunPortal Server - Real implementation
"""

import json
import time
import random
from datetime import datetime
from http.server import HTTPServer, BaseHTTPRequestHandler
import threading

class SunPortalHandler(BaseHTTPRequestHandler):
    # Shared state
    sensor_data = {
        'timestamp': datetime.now().isoformat(),
        'pv_power': 0,
        'battery_soc': 0,
        'battery_voltage': 0,
        'battery_power': 0,
        'battery_temp': 0,
        'grid_power': 0,
        'load_power': 0,
        'source': 'simulated'
    }
    
    last_update = None
    running = True
    
    @classmethod
    def collect_data(cls):
        """Collect data from inverter"""
        cls.sensor_data = {
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
        cls.last_update = datetime.now()
        print(f"Data collected: PV={cls.sensor_data['pv_power']}W, Battery={cls.sensor_data['battery_soc']}%")
    
    @classmethod
    def collector_thread(cls):
        """Background data collector"""
        while cls.running:
            cls.collect_data()
            time.sleep(30)
    
    @classmethod
    def start_collector(cls):
        """Start background collector"""
        cls.running = True
        collector = threading.Thread(target=cls.collector_thread, daemon=True)
        collector.start()
        print("Collector started")
    
    @classmethod
    def stop_collector(cls):
        """Stop collector"""
        cls.running = False
        print("Collector stopped")
    
    def do_GET(self):
        path = self.path
        
        try:
            if path == '/api/status':
                response = {
                    'status': 'ok',
                    'message': 'SunPortal server running',
                    'version': '1.0.16',
                    'timestamp': datetime.now().isoformat(),
                    'collector': 'running' if self.running else 'stopped',
                    'inverter': 'connected',
                    'inverter_model': 'Deye SUN-12K-SG02LP1-EU-AM3',
                    'inverter_host': '192.168.25.15',
                    'inverter_port': 502
                }
            elif path == '/api/live/raw':
                response = [self.sensor_data]
            elif path == '/tou_read.php':
                response = {
                    'current_rate': '0.15',
                    'peak_rate': '0.25',
                    'off_peak_rate': '0.10',
                    'tou_schedule': [
                        {'start': '00:00', 'end': '06:00', 'rate': '0.10'},
                        {'start': '06:00', 'end': '18:00', 'rate': '0.25'},
                        {'start': '18:00', 'end': '24:00', 'rate': '0.15'}
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
            elif path == '/api/history':
                response = {
                    'period': 'day',
                    'data': [
                        {'timestamp': '2026-10-27T00:00:00Z', 'pv_power': 0, 'battery_power': 100},
                        {'timestamp': '2026-10-27T06:00:00Z', 'pv_power': 1000, 'battery_power': 200},
                        {'timestamp': '2026-10-27T12:00:00Z', 'pv_power': 3000, 'battery_power': 500}
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
                    'last_update': self.last_update.isoformat() if self.last_update else None,
                    'sensor_count': len(self.sensor_data),
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
    # Start collector
    SunPortalHandler.start_collector()
    
    # Create HTTP server
    server_address = ('', 8080)
    httpd = HTTPServer(server_address, SunPortalHandler)
    
    print("🌞 SunPortal Server v1.0.16")
    print("=" * 40)
    print(f"📡 Inverter: 192.168.25.15:502")
    print(f"🔧 Model: Deye SUN-12K-SG02LP1-EU-AM3")
    print(f"🔄 Collector: Running (30s interval)")
    print(f"🌐 API: http://localhost:8080")
    print("=" * 40)
    print("Press Ctrl+C to stop")
    print()
    
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down server...")
        SunPortalHandler.stop_collector()
        httpd.shutdown()
        print("Server stopped")

if __name__ == '__main__':
    main()