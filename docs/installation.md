# Installation Guide

This guide provides step-by-step instructions for installing the SunLink system. Choose the installation method that best fits your needs.

## 📋 System Requirements

### Minimum Requirements
- **Operating System**: Linux (Ubuntu 20.04+ / Debian 11+)
- **RAM**: 2GB
- **CPU**: 1 core
- **Storage**: 10GB free space
- **Network**: Internet connection

### Recommended Requirements
- **Operating System**: Ubuntu 22.04 LTS
- **RAM**: 4GB+
- **CPU**: 2+ cores
- **Storage**: 50GB+ SSD
- **Network**: Static IP address

### Network Requirements
- **Port 80**: HTTP (for Let's Encrypt certificate)
- **Port 443**: HTTPS (main service)
- **Port 1883**: MQTT (inverter communication)
- **Port 8086**: InfluxDB (database)
- **Port 9090**: Prometheus (monitoring)

## 🚀 Installation Methods

### 1. Docker Installation (Recommended)

#### Prerequisites
```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Docker
sudo apt install -y docker.io docker-compose

# Add user to docker group
sudo usermod -aG docker $USER

# Enable and start Docker
sudo systemctl enable docker
sudo systemctl start docker
```

#### Installation Steps
```bash
# Clone repository
git clone https://github.com/Ponamarenko/sunlink.git
cd sunlink

# Copy environment file
cp .env.example .env

# Edit environment file
nano .env

# Set permissions
chmod +x scripts/deploy.sh

# Deploy system
sudo ./scripts/deploy.sh
```

#### Environment Configuration
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

#### SSL Certificate Setup
```bash
# Obtain SSL certificate
sudo apt install certbot -y
sudo certbot certonly --standalone -d sunlink.pp.ua -d www.sunlink.pp.ua

# Create certificate directory
sudo mkdir -p /etc/caddy/ssl
sudo cp /etc/letsencrypt/live/sunlink.pp.ua/fullchain.pem /etc/caddy/ssl/cert.pem
sudo cp /etc/letsencrypt/live/sunlink.pp.ua/privkey.pem /etc/caddy/ssl/key.pem
sudo chown -R caddy:caddy /etc/caddy/ssl
```

#### Start Services
```bash
# Start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f
```

### 2. Manual Installation

#### System Preparation
```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install dependencies
sudo apt install -y \
    python3 \
    python3-pip \
    python3-venv \
    docker.io \
    docker-compose \
    nginx \
    certbot \
    ufw

# Enable and start services
sudo systemctl enable docker
sudo systemctl start docker
sudo systemctl enable nginx
sudo systemctl start nginx
```

#### Python Environment Setup
```bash
# Create virtual environment
python3 -m venv venv
source venv/bin/activate

# Install Python dependencies
pip install -r backend/requirements.txt

# Install system dependencies
pip install fastapi uvicorn paho-mqtt influxdb-client python-jose passlib python-multipart redis prometheus-client aiofiles websockets httpx
```

#### Database Setup
```bash
# Install InfluxDB
wget https://dl.influxdata.com/influxdb/releases/influxdb2-2.7.0-linux-amd64.tar.gz
tar -xvzf influxdb2-2.7.0-linux-amd64.tar.gz
sudo cp influxdb2-2.7.0-linux-amd64/usr/bin/* /usr/local/bin/
sudo mkdir -p /etc/influxdb2
sudo cp influxdb2-2.7.0-linux-amd64/etc/influxdb2/config.yml /etc/influxdb2/

# Start InfluxDB
sudo systemctl enable influxdb
sudo systemctl start influxdb
```

#### MQTT Broker Setup
```bash
# Install Mosquitto
sudo apt install -y mosquitto mosquitto-clients

# Create configuration
sudo mkdir -p /etc/mosquitto/conf.d
sudo nano /etc/mosquitto/conf.d/sunlink.conf

# Configuration content
listener 1883
allow_anonymous false
password_file /etc/mosquitto/conf.d/password.txt
acl_file /etc/mosquitto/conf.d/acl.conf

# Create password file
sudo mosquitto_passwd -c /etc/mosquitto/conf.d/password.txt sunlink_user
sudo echo "sunlink_user:SunLinkMQTT2026" | sudo tee /etc/mosquitto/conf.d/password.txt

# Create ACL file
sudo nano /etc/mosquitto/conf.d/acl.conf
cat << EOF | sudo tee /etc/mosquitto/conf.d/acl.conf
user sunlink_user
topic read deye/telemetry/+
topic read deye/status/+
topic read deye/response/+
topic read deye/aggregate/+
topic write deye/control/+
topic write deye/response/+
EOF

# Start Mosquitto
sudo systemctl enable mosquitto
sudo systemctl start mosquitto
```

#### Web Server Setup
```bash
# Install Caddy
sudo apt install -y debian-keyring debian-archive-keyring apt-transport-https
curl -L https://github.com/caddyserver/caddy/releases/download/v2.7.0/caddy_2.7.0_linux_amd64.deb -o caddy.deb
sudo dpkg -i caddy.deb

# Create Caddyfile
sudo nano /etc/caddy/Caddyfile

# Caddyfile content
sunlink.pp.ua, www.sunlink.pp.ua {
    tls /etc/caddy/ssl/cert.pem /etc/caddy/ssl/key.pem
    
    header {
        X-Content-Type-Options "nosniff"
        X-Frame-Options "DENY"
        X-XSS-Protection "1; mode=block"
        Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"
        -Server
    }
    
    reverse_proxy localhost:8000
}

# Create SSL directory
sudo mkdir -p /etc/caddy/ssl
sudo chown -R caddy:caddy /etc/caddy/ssl

# Start Caddy
sudo systemctl enable caddy
sudo systemctl start caddy
```

#### Application Setup
```bash
# Create application directory
sudo mkdir -p /opt/sunlink
sudo chown -R $USER:$USER /opt/sunlink
cd /opt/sunlink

# Copy application files
cp -r /path/to/sunlink/backend/* .
cp -r /path/to/sunlink/mosquitto ./mosquitto
cp -r /path/to/sunlink/influxdb ./influxdb
cp -r /path/to/sunlink/monitoring ./monitoring

# Create environment file
cp .env.example .env
nano .env

# Create systemd service
sudo nano /etc/systemd/system/sunlink.service

# Service content
[Unit]
Description=SunLink Service
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=/opt/sunlink
Environment=PATH=/opt/sunlink/venv/bin
ExecStart=/opt/sunlink/venv/bin/uvicorn main:app --host 0.0.0.0 --port 8000
Restart=always

[Install]
WantedBy=multi-user.target

# Enable and start service
sudo systemctl daemon-reload
sudo systemctl enable sunlink
sudo systemctl start sunlink
```

### 3. Kubernetes Installation (Advanced)

#### Prerequisites
```bash
# Install kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# Install minikube (for testing)
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube

# Start minikube
minikube start --driver=docker
```

#### Kubernetes Configuration
```bash
# Create namespace
kubectl create namespace sunlink

# Apply configurations
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmaps/
kubectl apply -f k8s/secrets/
kubectl apply -f k8s/deployments/
kubectl apply -f k8s/services/
kubectl apply -f k8s/ingress/
```

## 🔧 Post-Installation Setup

### 1. System Health Check
```bash
# Check service status
docker-compose ps

# Check API health
curl -f http://localhost:8000/api/v1/health

# Check MQTT connection
docker-compose exec mosquitto mosquitto_pub -h localhost -t "test" -m "hello"

# Check InfluxDB connection
curl -f http://localhost:8086/ping

# Check Caddy proxy
curl -f http://localhost:80
curl -f http://localhost:443
```

### 2. Initial Configuration
```bash
# Create admin user
curl -X POST "http://localhost:8000/api/v1/auth/admin" \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "your_secure_password"}'

# Test API access
curl -X POST "http://localhost:8000/api/v1/auth/login" \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "your_secure_password"}'
```

### 3. Network Configuration
```bash
# Configure firewall
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw allow 1883/tcp  # MQTT
sudo ufw allow 8086/tcp  # InfluxDB
sudo ufw allow 9090/tcp  # Prometheus
sudo ufw enable

# Configure DNS
# Add A records for your domain pointing to server IP
```

## 🔍 Troubleshooting Installation

### Common Issues

#### Docker Issues
```bash
# Check Docker status
sudo systemctl status docker

# Restart Docker
sudo systemctl restart docker

# Check Docker logs
sudo journalctl -u docker
```

#### Port Conflicts
```bash
# Check port usage
sudo netstat -tlnp | grep -E '80|443|1883|8086|9090'

# Kill conflicting processes
sudo fuser -k 80/tcp
sudo fuser -k 443/tcp
```

#### Permission Issues
```bash
# Fix Docker permissions
sudo usermod -aG docker $USER
newgrp docker

# Fix file permissions
sudo chown -R $USER:$USER /opt/sunlink
chmod +x scripts/deploy.sh
```

#### SSL Certificate Issues
```bash
# Check certificate status
sudo certbot certificates

# Renew certificate
sudo certbot renew

# Test certificate
sudo openssl x509 -in /etc/letsencrypt/live/sunlink.pp.ua/fullchain.pem -text -noout
```

### Debug Commands
```bash
# Check all services
docker-compose ps

# View logs
docker-compose logs -f fastapi-app
docker-compose logs -f mosquitto
docker-compose logs -f influxdb
docker-compose logs -f caddy

# Check resource usage
docker stats
htop
df -h

# Network connectivity
ping sunlink.pp.ua
curl -I https://sunlink.pp.ua
```

## 🚀 Next Steps

After successful installation:

1. **Configure Your Domain**
   - Set up DNS records
   - Configure SSL certificates
   - Test HTTPS access

2. **Connect Your Inverters**
   - Follow the [Universal Connection System](../UNIVERSAL_CONNECTION_SYSTEM.md)
   - Test inverter connections
   - Verify data flow

3. **Set Up Monitoring**
   - Configure Prometheus
   - Set up Grafana dashboards
   - Configure alerts

4. **Deploy Mobile App**
   - Install Flutter app
   - Test API connectivity
   - Configure user accounts

## 📞 Support

If you encounter issues during installation:

- **Check the [Troubleshooting Guide](troubleshooting.md)**
- **Review the [Configuration Guide](configuration.md)**
- **Submit an issue on [GitHub](https://github.com/Ponamarenko/sunlink/issues)**
- **Contact support@sunlink.pp.ua**

---

**Installation Guide** - Get started with SunLink in minutes 🌞