#!/usr/bin/env python3

import json
import os
import sys
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs

class SunPortalAPIHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        path = urlparse(self.path).path
        
        if path == '/api/status':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            response = {
                'status': 'ok',
                'message': 'SunPortal API server running',
                'version': '1.0.16'
            }
            self.wfile.write(json.dumps(response).encode())
            
        elif path == '/api/data':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            response = {
                'pv_power': 2500,
                'battery_soc': 75,
                'battery_voltage': 48.2,
                'battery_power': 500,
                'battery_temp': 25.5,
                'grid_power': 0,
                'load_power': 2000,
                'source': 'simulated'
            }
            self.wfile.write(json.dumps(response).encode())
            
        elif path == '/api/collector/status':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            response = {
                'status': 'running',
                'last_update': '2026-10-27T10:30:00Z',
                'sensor_count': 25,
                'source': 'modbus',
                'version': 'v2'
            }
            self.wfile.write(json.dumps(response).encode())
            
        else:
            self.send_response(404)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            response = {'error': 'Unknown endpoint'}
            self.wfile.write(json.dumps(response).encode())
    
    def do_POST(self):
        path = urlparse(self.path).path
        
        if path == '/api/tou_write':
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)
            data = json.loads(post_data.decode())
            
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            
            response = {
                'status': 'success',
                'action': data.get('action', 'unknown'),
                'rate': data.get('rate', '0.00'),
                'message': 'TOU operation completed successfully'
            }
            self.wfile.write(json.dumps(response).encode())
            
        else:
            self.send_response(404)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            response = {'error': 'Unknown endpoint'}
            self.wfile.write(json.dumps(response).encode())

if __name__ == '__main__':
    server_address = ('', 8080)
    httpd = HTTPServer(server_address, SunPortalAPIHandler)
    print("SunPortal API server running on port 8080")
    httpd.serve_forever()
