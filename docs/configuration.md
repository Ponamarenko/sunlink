# Configuration Guide

This comprehensive guide covers all configuration options for the SunLink system. Learn how to customize your installation for optimal performance and security.

## 📋 Configuration Overview

The SunLink system uses a combination of environment variables, configuration files, and database settings to control system behavior. This guide covers all configuration aspects.

## 🔧 Environment Configuration

### Environment Variables

The primary configuration is stored in the `.env` file. Copy `.env.example` to `.env` and customize:

```bash
cp .env.example .env
nano .env
```

#### Basic Settings
```env
# System identification
DOMAIN=sunlink.pp.ua
ENVIRONMENT=production
DEBUG=false
LOG_LEVEL=INFO

# Server configuration
SERVER_HOST=0.0.0.0
SERVER_PORT=8000
SERVER_RELOAD=false
```

#### MQTT Configuration
```env
# MQTT Broker settings
MQTT_HOST=mosquitto
MQTT_PORT=1883
MQTT_USERNAME=sunlink_user
MQTT_PASSWORD=SunLinkMQTT2026
MQTT_KEEPALIVE=60
MQTT_RETRY_ATTEMPTS=3
MQTT_RETRY_DELAY=5

# MQTT Topics
MQTT_TELEMETRY_TOPIC=deye/telemetry/+
MQTT_STATUS_TOPIC=deye/status/+
MQTT_CONTROL_TOPIC=deye/control/+
MQTT_RESPONSE_TOPIC=deye/response/+
MQTT_ERROR_TOPIC=deye/error/+
MQTT_AGGREGATE_TOPIC=deye/aggregate/+
```

#### InfluxDB Configuration
```env
# InfluxDB connection
INFLUXDB_URL=http://influxdb:8086
INFLUXDB_TOKEN=SunLinkInfluxDBToken2026
INFLUXDB_ORG=sunlink_org
INFLUXDB_BUCKET=deye_metrics
INFLUXDB_TIMEOUT=30

# InfluxDB optimization
INFLUXDB_CACHE_MAX_MEMORY_SIZE=1073741824
INFLUXDB_CACHE_SNAPSHOT_MEMORY_SIZE=268435456
INFLUXDB_COMPACT_FULL_WRITE_BUFFER_SIZE=1073741824
```

#### JWT Configuration
```env
# JWT settings
JWT_SECRET=SunLinkJWTSecret2026
JWT_ALGORITHM=HS256
JWT_EXPIRATION=2592000  # 30 days in seconds
JWT_REFRESH_EXPIRATION=7776000  # 90 days in seconds
```

#### Security Configuration
```env
# Authentication settings
MAX_LOGIN_ATTEMPTS=5
PASSWORD_MIN_LENGTH=8
SESSION_TIMEOUT=3600  # 1 hour
PASSWORD_EXPIRATION_DAYS=90

# Rate limiting
RATE_LIMIT_REQUESTS=100
RATE_LIMIT_WINDOW=60  # seconds
```

#### Application Configuration
```env
# Application settings
APP_NAME=SunLink
APP_VERSION=1.0.0
APP_DESCRIPTION=Deye Inverter Integration System

# Data processing
DATA_RETENTION_DAYS=30
DATA_COMPRESSION=true
DATA_BATCH_SIZE=1000
DATA_FLUSH_INTERVAL=10  # seconds
```

### Environment File Structure

```env
# ===========================================
# SunLink System Configuration
# ===========================================

# Basic Settings
DOMAIN=sunlink.pp.ua
ENVIRONMENT=production
DEBUG=false
LOG_LEVEL=INFO

# MQTT Configuration
MQTT_HOST=mosquitto
MQTT_PORT=1883
MQTT_USERNAME=sunlink_user
MQTT_PASSWORD=SunLinkMQTT2026
MQTT_KEEPALIVE=60
MQTT_RETRY_ATTEMPTS=3
MQTT_RETRY_DELAY=5

# InfluxDB Configuration
INFLUXDB_URL=http://influxdb:8086
INFLUXDB_TOKEN=SunLinkInfluxDBToken2026
INFLUXDB_ORG=sunlink_org
INFLUXDB_BUCKET=deye_metrics
INFLUXDB_TIMEOUT=30

# JWT Configuration
JWT_SECRET=SunLinkJWTSecret2026
JWT_ALGORITHM=HS256
JWT_EXPIRATION=2592000

# Security Configuration
MAX_LOGIN_ATTEMPTS=5
PASSWORD_MIN_LENGTH=8
SESSION_TIMEOUT=3600

# Server Configuration
SERVER_HOST=0.0.0.0
SERVER_PORT=8000
SERVER_RELOAD=false

# SSL Configuration
SSL_CERT_PATH=/etc/caddy/ssl/cert.pem
SSL_KEY_PATH=/etc/caddy/ssl/key.pem

# Database Configuration
DATABASE_URL=sqlite:///./sunlink.db

# Redis Configuration
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_DB=0

# Logging Configuration
LOG_FILE_PATH=/app/logs/app.log
LOG_MAX_SIZE=10MB
LOG_BACKUP_COUNT=5

# Monitoring Configuration
PROMETHEUS_ENABLED=true
PROMETHEUS_PORT=9090

# WebSocket Configuration
WEBSOCKET_MAX_CONNECTIONS=100
WEBSOCKET_PING_INTERVAL=30

# Inverter Configuration
INVERTER_TIMEOUT=300
INVERTER_RETRY_INTERVAL=60

# Battery Configuration
BATTERY_SOC_MIN=10
BATTERY_SOC_MAX=90
BATTERY_VOLTAGE_MIN=44
BATTERY_VOLTAGE_MAX=58

# Solar Configuration
SOLAR_POWER_MAX=12000
SOLAR_VOLTAGE_MAX=100
SOLAR_CURRENT_MAX=100

# Network Configuration
NETWORK_TIMEOUT=30
NETWORK_RETRY_COUNT=3
NETWORK_RETRY_DELAY=5
```

## 📁 Configuration Files

### Docker Compose Configuration

The `docker-compose.yml` file defines all services and their configurations:

```yaml
version: '3.8'

# Logging configuration
logging:
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "3"

networks:
  sunlink_net:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16

services:
  # MQTT Broker
  mosquitto:
    image: eclipse-mosquitto:2.0.15
    container_name: sunlink-mqtt
    restart: unless-stopped
    volumes:
      - ./mosquitto/config:/mosquitto/config
      - ./mosquitto/data:/mosquitto/data
      - ./mosquitto/log:/mosquitto/log
    networks:
      - sunlink_net
    environment:
      - TZ=Europe/Kiev
    healthcheck:
      test: ["CMD", "mosquitto", "-h", "localhost"]
      interval: 30s
      timeout: 10s
      retries: 3

  # InfluxDB
  influxdb:
    image: influxdb:2.7
    container_name: sunlink-db
    restart: unless-stopped
    environment:
      - DOCKER_INFLUXDB_INIT_MODE=setup
      - DOCKER_INFLUXDB_INIT_USERNAME=sunlink_admin
      - DOCKER_INFLUXDB_INIT_PASSWORD=SunLinkSecurePassword2026
      - DOCKER_INFLUXDB_INIT_ORG=sunlink_org
      - DOCKER_INFLUXDB_INIT_BUCKET=deye_metrics
      - DOCKER_INFLUXDB_INIT_RETENTION=30d
      - DOCKER_INFLUXDB_INIT_ADMIN_TOKEN=SunLinkInfluxDBToken2026
    volumes:
      - ./influxdb/config:/etc/influxdb2
      - influxdb_data:/var/lib/influxdb2
    networks:
      - sunlink_net
    ports:
      - "8086:8086"
    healthcheck:
      test: ["CMD", "influx", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Caddy Proxy
  caddy:
    image: caddy:2.7
    container_name: sunlink-proxy
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile
      - ./ssl:/etc/caddy/ssl
      - caddy_data:/data
      - caddy_config:/config
      - ./logs:/var/log/caddy
    depends_on:
      - fastapi-app
    networks:
      - sunlink_net
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:2019/config/"]
      interval: 30s
      timeout: 10s
      retries: 3

  # FastAPI Backend
  fastapi-app:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: sunlink-backend
    restart: unless-stopped
    working_dir: /app
    volumes:
      - ./backend:/app
      - ./logs:/app/logs
    environment:
      - MQTT_HOST=mosquitto
      - MQTT_PORT=1883
      - MQTT_USERNAME=sunlink_user
      - MQTT_PASSWORD=SunLinkMQTT2026
      - INFLUXDB_URL=http://influxdb:8086
      - INFLUXDB_TOKEN=SunLinkInfluxDBToken2026
      - INFLUXDB_ORG=sunlink_org
      - INFLUXDB_BUCKET=deye_metrics
      - JWT_SECRET=SunLinkJWTSecret2026
      - JWT_ALGORITHM=HS256
      - JWT_EXPIRATION=2592000
      - LOG_LEVEL=INFO
      - ENVIRONMENT=production
    depends_on:
      mosquitto:
        condition: service_healthy
      influxdb:
        condition: service_healthy
    networks:
      - sunlink_net
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8000/api/v1/health"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  influxdb_data:
  caddy_data:
  caddy_config:
```

### Caddy Configuration

The `Caddyfile` configures the reverse proxy and SSL termination:

```caddy
# Main domain
sunlink.pp.ua, www.sunlink.pp.ua {
    
    # Enhanced logging
    log {
        output file /var/log/caddy/access.log {
            roll_size 100mb
            roll_keep 5
            roll_for 720h
        }
        format json
        level INFO
    }
    
    # Maximum security
    header {
        # Attack protection
        X-Content-Type-Options "nosniff"
        X-Frame-Options "DENY"
        X-XSS-Protection "1; mode=block"
        X-Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; connect-src 'self' wss: https:; font-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none';"
        Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"
        -Server
        Referrer-Policy "strict-origin-when-cross-origin"
        Permissions-Policy "camera=(), microphone=(), geolocation=()"
    }
    
    # Rate limiting for API
    rate_limit {
        zone static {
            key {remote_host}
            events 100
            window 1m
        }
    }
    
    # Request routing
    reverse_proxy fastapi-app:8000 {
        header_up Host {upstream_hostport}
        header_up X-Real-IP {remote_host}
        header_up X-Forwarded-For {remote_host}
        header_up X-Forwarded-Proto {scheme}
        header_up X-Forwarded-Host {host}
        header_up X-Forwarded-Port {port}
        
        # Health check
        health_uri /api/v1/health
        health_interval 30s
        health_timeout 5s
    }
    
    # WebSocket proxy
    handle /api/v1/live {
        reverse_proxy fastapi-app:8000 {
            header_up Upgrade {>Upgrade}
            header_up Connection {>Connection}
        }
    }
    
    # Static files
    root * /var/www/html
    file_server
    
    # Compression
    encode zstd gzip
}

# API subdomain with additional protection
api.sunlink.pp.ua {
    # Rate limiting for API
    rate_limit {
        zone api {
            key {remote_host}
            events 1000
            window 1h
        }
    }
    
    # API request routing
    reverse_proxy fastapi-app:8000 {
        header_up Host {upstream_hostport}
        header_up X-Real-IP {remote_host}
        header_up X-Forwarded-For {remote_host}
        header_up X-Forwarded-Proto {scheme}
    }
    
    # API request logging
    log {
        output file /var/log/caddy/api_access.log {
            roll_size 100mb
            roll_keep 5
            roll_for 720h
        }
        format json
    }
}
```

### MQTT Configuration

The `mosquitto/config/mosquitto.conf` file configures the MQTT broker:

```ini
# Mosquitto configuration for SunLink

# Network settings
listener 1883
protocol mqtt
allow_anonymous false
password_file /mosquitto/config/password.txt
acl_file /mosquitto/config/acl.conf

# Logging
log_dest file /mosquitto/log/mosquitto.log
log_type error
log_type warning
log_type notice
log_type information
log_timestamp true
log_timestamp_format %Y-%m-%dT%H:%M:%S

# Persistence
persistence true
persistence_location /mosquitto/data/
autosave_interval 1800

# Connection settings
max_connections 100
max_inflight_messages 20
max_queued_messages 1000
message_size_limit 268435456

# Security
allow_zero_length_clientid false
use_username_as_clientid true
connection_messages true
log_timestamp true

# Custom settings for SunLink
sys_interval 30
upgrade_outgoing_qos false

# WebSocket support
listener 8080
protocol websockets
allow_anonymous false

# TLS support (optional)
# cafile /mosquitto/config/ca.crt
# certfile /mosquitto/config/server.crt
# keyfile /mosquitto/config/server.key
# require_certificate false
# use_identity_as_username true
```

### InfluxDB Configuration

The `influxdb/config/config.yml` file configures InfluxDB:

```yaml
# InfluxDB configuration for SunLink

[meta]
  dir = "/var/lib/influxdb2/meta"
  retention-duration = "0s"

[storage]
  dir = "/var/lib/influxdb2/engine"

[bolt]
  dir = "/var/lib/influxdb2/bolt"

[http]
  bind-address = ":8086"
  auth-enabled = true
  log-enabled = true
  https-enabled = false
  https-certificate = ""
  https-private-key = ""

[logging]
  format = "auto"
  level = "info"
  timestamp-format = "2006-01-02T15:04:05Z07:00"

[auth]
  enabled = true
  session-length = "0s"
  session-inactivity = "0s"

[task]
  log-enabled = true
  tracing-enabled = false

[storage-engine]
  cache-max-memory-size = 1073741824
  cache-snapshot-memory-size = 268435456
  compact-full-write-buffer-size = 1073741824
  compact-throughput = 50331648
  compact-throughput-burst = 50331648
  max-concurrent-compactions = 1
  max-concurrent-compactions-bytenorm = 1048576
  max-index-log-file-size = 1048576
  max-series-per-database = 1000000
  max-values-per-tag = 100000
  max-wal-size = 1073741824
  snapshot-memory-size = 268435456
  wal-dir = "/var/lib/influxdb2/wal"
  wal-partition-size = 536870912

[query]
  max-concurrent-queries = 0
  query-timeout = "0s"
  max-select-point = 0
  max-select-series = 0
  max-select-buckets = 0
  http-chunked-encoding = true
  http-read-timeout = "0s"
  http-write-timeout = "0s"
  http-idle-timeout = "0s"
  http-max-concurrent-requests = 0
  http-ping-interval = "30s"
  http-ping-timeout = "5s"

[continuous]
  enabled = true
  query-concurrency = 0
  query-timeout = "0s"

[monitor]
  store-enabled = true
  store-interval = "10s"

[subscriber]
  enabled = true
  http-timeout = "0s"
  http-idle-timeout = "0s"
  http-max-concurrent-requests = 0

[export]
  enabled = false
  http-timeout = "0s"

[retention]
  enabled = true
  check-interval = "30m"

[shard-precreation]
  enabled = true
  check-interval = "10m"
  advance-period = 30m

[data]
  trace-enabled = false
  dir = "/var/lib/influxdb2/data"

[cluster]
  meta-service-enabled = false

[enterprise]
  enabled = false

[telemetry]
  enabled = true
  interval = "15m"

[hinted-handoff]
  enabled = true
  dir = "/var/lib/influxdb2/hh"
  max-size = 1073741824
  max-age = "168h"
  retry-interval = "1s"
  retry-max-interval = "30s"
  retry-limit = 0
  retry-max-jitter = "100ms"
  purge-interval = "1m"

[tsm1]
  cache-max-memory-size = 1073741824
  cache-snapshot-memory-size = 268435456
  compact-full-write-buffer-size = 1073741824
  compact-throughput = 50331648
  compact-throughput-burst = 50331648
  max-concurrent-compactions = 1
  max-index-log-file-size = 1048576
  max-series-per-database = 1000000
  max-values-per-tag = 100000
  max-wal-size = 1073741824
  snapshot-memory-size = 268435456
  wal-dir = "/var/lib/influxdb2/wal"
  wal-partition-size = 536870912

[coordination]
  enabled = true
  dir = "/var/lib/influxdb2/coordination"
  timeout = "5s"

[consul]
  enabled = false
  host = "localhost"
  port = 8500
  key = "influxdb"
  meta-service-enabled = false
```

## 🔐 Security Configuration

### Authentication Configuration

#### JWT Configuration
```python
# backend/config.py
class AuthConfig:
    JWT_SECRET = "SunLinkJWTSecret2026"
    JWT_ALGORITHM = "HS256"
    JWT_EXPIRATION = 2592000  # 30 days
    REFRESH_TOKEN_EXPIRATION = 7776000  # 90 days
    TOKEN_BLACKLIST_REDIS_KEY = "jwt_blacklist"
```

#### Password Policy
```python
# backend/config.py
class PasswordPolicy:
    MIN_LENGTH = 8
    MAX_LENGTH = 64
    REQUIRE_UPPERCASE = True
    REQUIRE_LOWERCASE = True
    REQUIRE_NUMBER = True
    REQUIRE_SPECIAL = True
    EXPIRATION_DAYS = 90
    HISTORY_COUNT = 5
```

#### Rate Limiting
```python
# backend/config.py
class RateLimitConfig:
    LOGIN_ATTEMPTS = 5
    LOGIN_WINDOW = 300  # 5 minutes
    API_REQUESTS = 100
    API_WINDOW = 60  # 1 minute
    WS_CONNECTIONS = 100
```

### SSL/TLS Configuration

#### SSL Certificate Setup
```bash
# Obtain Let's Encrypt certificate
sudo certbot certonly --standalone -d sunlink.pp.ua -d www.sunlink.pp.ua

# Create certificate directory
sudo mkdir -p /etc/caddy/ssl
sudo cp /etc/letsencrypt/live/sunlink.pp.ua/fullchain.pem /etc/caddy/ssl/cert.pem
sudo cp /etc/letsencrypt/live/sunlink.pp.ua/privkey.pem /etc/caddy/ssl/key.pem
sudo chown -R caddy:caddy /etc/caddy/ssl
```

#### SSL Configuration
```caddy
# Caddyfile SSL configuration
sunlink.pp.ua, www.sunlink.pp.ua {
    tls /etc/caddy/ssl/cert.pem /etc/caddy/ssl/key.pem
    
    # SSL security headers
    header {
        Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"
        X-Content-Type-Options "nosniff"
        X-Frame-Options "DENY"
        X-XSS-Protection "1; mode=block"
    }
}
```

### Firewall Configuration

#### UFW Configuration
```bash
# Basic firewall setup
sudo ufw default deny incoming
sudo ufw default allow outgoing

# Allow essential services
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP (for Let's Encrypt)
sudo ufw allow 443/tcp   # HTTPS
sudo ufw allow 1883/tcp  # MQTT
sudo ufw allow 8086/tcp  # InfluxDB
sudo ufw allow 9090/tcp  # Prometheus

# Enable firewall
sudo ufw enable
```

#### iptables Configuration
```bash
# Create iptables rules
sudo iptables -A INPUT -p tcp --dport 22 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 80 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 443 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 1883 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 8086 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 9090 -j ACCEPT

# Save rules
sudo iptables-save > /etc/iptables/rules.v4
```

## 📊 Monitoring Configuration

### Prometheus Configuration

The `monitoring/prometheus.yml` file configures Prometheus monitoring:

```yaml
# Prometheus configuration for SunLink

global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  # - "first_rules.yml"
  # - "second_rules.yml"

scrape_configs:
  # Prometheus itself
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # SunLink FastAPI backend
  - job_name: 'sunlink-backend'
    static_configs:
      - targets: ['fastapi-app:8000']
    metrics_path: '/metrics'
    scrape_interval: 30s
    scrape_timeout: 10s

  # SunLink MQTT broker
  - job_name: 'sunlink-mqtt'
    static_configs:
      - targets: ['mosquitto:1883']
    metrics_path: '/metrics'
    scrape_interval: 30s
    scrape_timeout: 10s

  # SunLink InfluxDB
  - job_name: 'sunlink-influxdb'
    static_configs:
      - targets: ['influxdb:8086']
    metrics_path: '/metrics'
    scrape_interval: 30s
    scrape_timeout: 10s

  # SunLink Caddy proxy
  - job_name: 'sunlink-caddy'
    static_configs:
      - targets: ['caddy:2019']
    metrics_path: '/metrics'
    scrape_interval: 30s
    scrape_timeout: 10s

  # Node exporter (if available)
  - job_name: 'node-exporter'
    static_configs:
      - targets: ['node-exporter:9100']
    scrape_interval: 30s
    scrape_timeout: 10s
```

### Grafana Configuration

```bash
# Install Grafana
sudo apt install grafana -y

# Start Grafana
sudo systemctl start grafana-server
sudo systemctl enable grafana-server

# Access Grafana
# URL: http://localhost:3000
# Username: admin
# Password: admin
```

### Logrotate Configuration

```bash
# Configure log rotation
sudo nano /etc/logrotate.d/sunlink

# Configuration
/var/log/sunlink/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 644 sunlink sunlink
    postrotate
        docker-compose restart fastapi-app
    endscript
}
```

## 🔧 Advanced Configuration

### Database Optimization

#### InfluxDB Optimization
```yaml
# influxdb/config/config.yml
[storage-engine]
  cache-max-memory-size = 1073741824  # 1GB
  cache-snapshot-memory-size = 268435456  # 256MB
  compact-full-write-buffer-size = 1073741824  # 1GB
  compact-throughput = 50331648  # 48MB/s
  compact-throughput-burst = 50331648  # 48MB/s
  max-concurrent-compactions = 1
  max-index-log-file-size = 1048576  # 1MB
  max-series-per-database = 1000000
  max-values-per-tag = 100000
  max-wal-size = 1073741824  # 1GB
  snapshot-memory-size = 268435456  # 256MB
  wal-dir = "/var/lib/influxdb2/wal"
  wal-partition-size = 536870912  # 512MB
```

#### Database Indexing
```python
# backend/database.py
class DatabaseConfig:
    # Time series optimization
    TIME_SERIES_RESOLUTION = "1s"  # 1 second resolution
    DATA_RETENTION = "30d"  # 30 days retention
    COMPRESSION_ENABLED = True
    BUCKET_NAME = "deye_metrics"
    
    # Indexing strategy
    INDEX_FIELDS = ["inverter_id", "timestamp", "state"]
    COMPOSITE_INDEX = ["inverter_id", "timestamp"]
```

### Network Optimization

#### TCP Optimization
```bash
# Configure TCP settings
sudo nano /etc/sysctl.d/99-tcp.conf

# Add optimizations
net.core.somaxconn = 65535
net.core.netdev_max_backlog = 65535
net.ipv4.tcp_max_syn_backlog = 65535
net.ipv4.tcp_tw_reuse = 1
net.ipv4.tcp_fin_timeout = 30
net.core.rmem_max = 16777216
net.core.wmem_max = 16777216
net.ipv4.tcp_rmem = 4096 87380 16777216
net.ipv4.tcp_wmem = 4096 65536 16777216
net.ipv4.tcp_congestion_control = bbr
vm.swappiness = 10

# Apply changes
sudo sysctl -p
```

#### Network Security
```bash
# Configure network security
sudo nano /etc/sysctl.d/99-security.conf

# Add security settings
net.ipv4.conf.all.send_redirects = 0
net.ipv4.conf.default.send_redirects = 0
net.ipv4.conf.all.accept_source_route = 0
net.ipv4.conf.default.accept_source_route = 0
net.ipv4.conf.all.accept_redirects = 0
net.ipv4.conf.default.accept_redirects = 0
net.ipv4.conf.all.secure_redirects = 0
net.ipv4.conf.default.secure_redirects = 0
net.ipv4.conf.all.log_martians = 1
net.ipv4.conf.default.log_martians = 1

# Apply changes
sudo sysctl -p
```

### Performance Tuning

#### Application Performance
```python
# backend/config.py
class PerformanceConfig:
    # Concurrency settings
    MAX_WORKERS = 4
    WORKER_TIMEOUT = 30
    WORKER_CONNECTIONS = 1000
    
    # Data processing
    BATCH_SIZE = 1000
    FLUSH_INTERVAL = 10  # seconds
    COMPRESSION_ENABLED = True
    CACHE_SIZE = 1000  # items
    
    # Database optimization
    DB_POOL_SIZE = 10
    DB_POOL_TIMEOUT = 30
    DB_MAX_OVERFLOW = 20
```

#### Caching Strategy
```python
# backend/cache.py
class CacheConfig:
    # Redis configuration
    REDIS_HOST = "redis"
    REDIS_PORT = 6379
    REDIS_DB = 0
    REDIS_PASSWORD = None
    
    # Cache settings
    CACHE_TTL = 300  # 5 minutes
    CACHE_PREFIX = "sunlink:"
    CACHE_MAX_SIZE = 10000  # items
    
    # Cache invalidation
    CACHE_INVALIDATION_INTERVAL = 60  # seconds
```

## 🔄 Configuration Management

### Configuration Validation

```bash
# Validate Docker Compose configuration
docker-compose config -q

# Validate Python configuration
python -c "import config; print('Configuration valid')"

# Validate SSL certificate
openssl x509 -in /etc/caddy/ssl/cert.pem -text -noout
```

### Configuration Backup

```bash
# Create configuration backup
sudo tar -czf /var/backups/sunlink-config-$(date +%Y%m%d).tar.gz \
    /etc/sunlink \
    /etc/caddy/Caddyfile \
    /etc/mosquitto/conf.d \
    /etc/influxdb2/config.yml \
    .env

# Set backup retention
sudo find /var/backups/sunlink-config-*.tar.gz -mtime +30 -delete
```

### Configuration Updates

```bash
# Update configuration
nano .env
docker-compose down
docker-compose up -d

# Validate configuration
docker-compose ps
curl -f http://localhost:8000/api/v1/health
```

## 🚀 Deployment Configuration

### Production Deployment

```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  fastapi-app:
    build:
      context: ./backend
      dockerfile: Dockerfile.prod
    environment:
      - ENVIRONMENT=production
      - LOG_LEVEL=WARNING
      - DEBUG=false
    restart: unless-stopped
    deploy:
      replicas: 2
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

### Development Deployment

```yaml
# docker-compose.dev.yml
version: '3.8'

services:
  fastapi-app:
    build:
      context: ./backend
      dockerfile: Dockerfile.dev
    environment:
      - ENVIRONMENT=development
      - LOG_LEVEL=DEBUG
      - DEBUG=true
    volumes:
      - ./backend:/app
      - /app/__pycache__
    ports:
      - "8000:8000"
```

## 📞 Support

For configuration issues:

- **Check the [Installation Guide](installation.md)**
- **Review the [Troubleshooting Guide](troubleshooting.md)**
- **Submit an issue on [GitHub](https://github.com/Ponamarenko/sunlink/issues)**
- **Contact support@sunlink.pp.ua**

---

**Configuration Guide** - Customize your SunLink system for optimal performance 🌞