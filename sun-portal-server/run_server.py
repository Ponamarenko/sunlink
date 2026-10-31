#!/usr/bin/env python3

import sys
import os
import subprocess
import time

# Change to server directory
os.chdir('/root/opt/sun-portal-server')

# Start the server
print("🚀 Starting SunPortal Server...")
subprocess.run([sys.executable, 'working_server.py'])