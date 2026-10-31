#!/usr/bin/env python3

import json
import os
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs

class SunPortalAPIHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        path = urlparse(self.path).path
        
        if path == '/api/live/raw':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            
            with open('api/live/raw', 'r') as f:
                self.wfile.write(f.read().encode())
                
        elif path == '/tou_read.php':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            
            with open('tou_read.php', 'r') as f:
                # Remove PHP tags and just return JSON
                content = f.read()
                json_start = content.find('[')
                json_end = content.rfind(']') + 1
                json_content = content[json_start:json_end]
                self.wfile.write(json_content.encode())
                
        elif path == '/weather_read.php':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            
            with open('weather_read.php', 'r') as f:
                content = f.read()
                json_start = content.find('{')
                json_end = content.rfind('}') + 1
                json_content = content[json_start:json_end]
                self.wfile.write(json_content.encode())
                
        elif path == '/forecast_read.php':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            
            with open('forecast_read.php', 'r') as f:
                content = f.read()
                json_start = content.find('{')
                json_end = content.rfind('}') + 1
                json_content = content[json_start:json_end]
                self.wfile.write(json_content.encode())
                
        elif path == '/api/history.json':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            
            with open('api/history.json', 'r') as f:
                self.wfile.write(f.read().encode())
                
        elif path == '/api/blackout.json':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            
            with open('api/blackout.json', 'r') as f:
                self.wfile.write(f.read().encode())
                
        else:
            self.send_response(404)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            response = {'error': 'Unknown endpoint'}
            self.wfile.write(json.dumps(response).encode())

if __name__ == '__main__':
    server_address = ('', 8086)
    httpd = HTTPServer(server_address, SunPortalAPIHandler)
    print("SunPortal API server running on port 8086")
    httpd.serve_forever()