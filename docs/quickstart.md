# Quick Start Guide

Get started with SunLink in minutes! This quick start guide will help you install and configure the SunLink system for monitoring your Deye inverters.

## 🚀 What You'll Need

### Requirements
- **Server**: Linux (Ubuntu 20.04+ / Debian 11+)
- **RAM**: 2GB minimum, 4GB recommended
- **CPU**: 1 core minimum, 2+ recommended
- **Storage**: 10GB+ free space
- **Network**: Internet connection
- **Domain**: Registered domain name (e.g., sunlink.pp.ua)

### Network Ports
- **Port 80**: HTTP (for SSL certificate)
- **Port 443**: HTTPS (main service)
- **Port 1883**: MQTT (inverter communication)
- **Port 8086**: InfluxDB (database)
- **Port 9090**: Prometheus (monitoring)

## 📋 Step 1: Server Setup

### Update System
```bash
# Update system packages
sudo apt update && sudo apt upgrade -y

# Install required packages
sudo apt install -y docker.io docker-compose git
```

### Install Docker
```bash
# Add user to docker group
sudo usermod -aG docker $USER
newgrp docker

# Enable and start Docker
sudo systemctl enable docker
sudo systemctl start docker
```

## 📦 Step 2: Install SunLink

### Clone Repository
```bash
# Clone SunLink repository
git clone https://github.com/Ponamarenko/sunlink.git
cd sunlink

# Check directory structure
ls -la
```

### Configure Environment
```bash
# Copy environment file
cp .env.example .env

# Edit environment file
nano .env
```

Edit the `.env` file with your settings:
```env
# Basic settings
DOMAIN=sunlink.pp.ua
ENVIRONMENT=production
LOG_LEVEL=INFO

# MQTT settings
MQTT_HOST=mosquitto
MQTT_PORT=1883
MQTT_USERNAME=sunlink_user
MQTT_PASSWORD=SunLinkMQTT2026

# InfluxDB settings
INFLUXDB_URL=http://influxdb:8086
INFLUXDB_TOKEN=SunLinkInfluxDBToken2026
INFLUXDB_ORG=sunlink_org
INFLUXDB_BUCKET=deye_metrics

# JWT settings
JWT_SECRET=SunLinkJWTSecret2026
JWT_EXPIRATION=2592000
```

## 🔧 Step 3: Deploy System

### Run Deployment Script
```bash
# Make deploy script executable
chmod +x scripts/deploy.sh

# Run deployment
sudo ./scripts/deploy.sh
```

The deployment script will:
- Create necessary directories
- Generate SSL certificates
- Copy configuration files
- Build Docker images
- Start all services
- Set up monitoring

### Check Installation
```bash
# Check service status
docker-compose ps

# Check API health
curl -f http://localhost:8000/api/v1/health

# Check MQTT connection
docker-compose exec mosquitto mosquitto_pub -h localhost -t "test" -m "hello"

# Check InfluxDB connection
curl -f http://localhost:8086/ping
```

## 🌐 Step 4: Configure Domain

### Set Up DNS
Create DNS records for your domain:
```
Type: A
Name: @
Value: your_server_ip

Type: A
Name: www
Value: your_server_ip

Type: A
Name: api
Value: your_server_ip
```

### Configure SSL Certificate
```bash
# Install Certbot
sudo apt install certbot -y

# Obtain SSL certificate
sudo certbot certonly --standalone -d sunlink.pp.ua -d www.sunlink.pp.ua

# Create certificate directory
sudo mkdir -p /etc/caddy/ssl
sudo cp /etc/letsencrypt/live/sunlink.pp.ua/fullchain.pem /etc/caddy/ssl/cert.pem
sudo cp /etc/letsencrypt/live/sunlink.pp.ua/privkey.pem /etc/caddy/ssl/key.pem
sudo chown -R caddy:caddy /etc/caddy/ssl
```

### Restart Services
```bash
# Restart Caddy with SSL
docker-compose restart caddy

# Test HTTPS access
curl -I https://sunlink.pp.ua
```

## 🔌 Step 5: Connect Your Inverter

### Choose Connection Type
Select the appropriate connection method for your inverter:

#### Option 1: RS485 to LAN Converter
```yaml
connection:
  type: rs485_lan
  host: "192.168.1.100"  # Converter IP
  port: 8899
  baud_rate: 9600
```

#### Option 2: ESP32 WiFi Connection
```yaml
connection:
  type: esp32_wifi
  ssid: "your_wifi_ssid"
  password: "your_wifi_password"
  mqtt_host: "sunlink.pp.ua"
  mqtt_port: 1883
  inverter_id: "INV001"
```

#### Option 3: Direct Ethernet
```yaml
connection:
  type: direct_ethernet
  inverter_ip: "192.168.1.102"
  port: 8899
```

### Test Connection
```bash
# Test MQTT connection
docker-compose exec mosquitto mosquitto_sub -h localhost -t "deye/telemetry/+" -v | head -5

# Check inverter data
curl -X GET "http://localhost:8000/api/v1/inverters" \
     -H "Authorization: Bearer your_token"
```

## 🔐 Step 6: Set Up Authentication

### Create Admin User
```bash
# Create admin user (replace with your credentials)
curl -X POST "http://localhost:8000/api/v1/auth/admin" \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "your_secure_password"}'
```

### Test Login
```bash
# Login to get JWT token
curl -X POST "http://localhost:8000/api/v1/auth/login" \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "your_secure_password"}'
```

### Test API Access
```bash
# Test API with JWT token
curl -X GET "http://localhost:8000/api/v1/inverters" \
     -H "Authorization: Bearer your_jwt_token"
```

## 📱 Step 7: Mobile App Setup

### Install Flutter App
Download the SunLink mobile app from:
- **Google Play**: [SunLink Mobile](https://play.google.com/store/apps/details?id=com.sunlink.mobile)
- **Apple App Store**: [SunLink Mobile](https://apps.apple.com/app/sunlink-mobile)

### Configure App
1. Open the app
2. Enter your domain: `sunlink.pp.ua`
3. Use your admin credentials to login
4. Add your inverters using the connection type you selected

## 🔍 Step 8: Verify System

### Check System Health
```bash
# Check overall system health
curl -X GET "http://localhost:8000/api/v1/health"

# Check individual services
curl -X GET "http://localhost:8000/api/v1/health/services"
```

### Monitor Data Flow
```bash
# Check MQTT messages
docker-compose exec mosquitto mosquitto_sub -h localhost -t "deye/telemetry/+" -v | head -10

# Check InfluxDB data
docker-compose exec influxdb influx query "SELECT * FROM telemetry LIMIT 5"
```

### Test WebSocket Connection
```javascript
// Test WebSocket connection in browser console
const ws = new WebSocket('wss://sunlink.pp.ua/api/v1/live');

ws.onopen = () => {
  console.log('WebSocket connected');
  ws.send(JSON.stringify({
    type: 'auth',
    token: 'your_jwt_token'
  }));
};

ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log('Received data:', data);
};

ws.onerror = (error) => {
  console.error('WebSocket error:', error);
};
```

## 🎯 Next Steps

### 1. Configure Multiple Inverters
```bash
# Add additional inverters
curl -X POST "http://localhost:8000/api/v1/inverters" \
     -H "Authorization: Bearer your_jwt_token" \
     -H "Content-Type: application/json" \
     -d '{
       "id": "INV002",
       "name": "Backup Inverter",
       "type": "deye_5kw",
       "connection_type": "wifi"
     }'
```

### 2. Set Up Monitoring
```bash
# Access Prometheus
open http://localhost:9090

# Access Grafana
open http://localhost:3000
# Username: admin
# Password: admin
```

### 3. Configure Alerts
Set up email notifications for:
- Inverter offline
- High temperature
- Low battery
- System errors

### 4. Backup Configuration
```bash
# Create backup
sudo ./scripts/backup.sh

# Schedule daily backup
echo "0 2 * * * /usr/local/bin/sunlink-backup.sh" | sudo crontab -
```

## 🚀 Troubleshooting

### Common Issues

#### Service Not Starting
```bash
# Check service status
docker-compose ps

# View logs
docker-compose logs -f fastapi-app
docker-compose logs -f mosquitto
docker-compose logs -f influxdb
```

#### SSL Certificate Issues
```bash
# Check certificate
sudo openssl x509 -in /etc/caddy/ssl/cert.pem -text -noout

# Renew certificate
sudo certbot renew
```

#### MQTT Connection Issues
```bash
# Test MQTT
docker-compose exec mosquitto mosquitto_pub -h localhost -t "test" -m "hello"
docker-compose exec mosquitto mosquitto_sub -h localhost -t "test"

# Check MQTT logs
docker-compose logs -f mosquitto
```

#### API Access Issues
```bash
# Test API health
curl -f http://localhost:8000/api/v1/health

# Check API logs
docker-compose logs -f fastapi-app
```

## 📞 Support

If you encounter issues:

1. **Check the logs**: `docker-compose logs -f`
2. **Review this guide**: Ensure all steps are completed
3. **Check GitHub Issues**: [sunlink/issues](https://github.com/Ponamarenko/sunlink/issues)
4. **Contact support**: support@sunlink.pp.ua

## 🎉 Congratulations!

You've successfully installed and configured the SunLink system! Your solar inverters are now being monitored and managed through the SunLink platform.

### What's Next?
- **Explore the API**: Check out the [API Documentation](api.md)
- **Set up monitoring**: Configure Prometheus and Grafana
- **Customize settings**: Adjust configuration for your needs
- **Add integrations**: Connect with smart home systems

---

**Quick Start Guide** - Get started with SunLink in minutes 🌞