# 🌐 Production Network Setup for SunLink

## Overview

This guide provides comprehensive instructions for setting up a production-ready network environment for the SunLink system. It covers network architecture, security measures, performance optimization, and monitoring.

## Network Architecture

### Recommended Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Internet                                │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                ┌─────────▼─────────┐
                │   Firewall/Router  │
                │   (UFW/iptables)   │
                └─────────┬─────────┘
                          │
                ┌─────────▼─────────┐
                │   Load Balancer   │
                │   (HAProxy/Nginx) │
                └─────────┬─────────┘
                          │
                ┌─────────▼─────────┐
                │   Reverse Proxy   │
                │      (Caddy)      │
                └─────────┬─────────┘
                          │
            ┌─────────────┼─────────────┐
            │             │             │
    ┌───────▼──────┐ ┌─────▼─────┐ ┌───▼──────┐
    │   Web App   │ │   API     │ │  Admin   │
    │  (React/Vue)│ │  (FastAPI)│ │  Panel   │
    └─────────────┘ └───────────┘ └──────────┘
            │             │             │
            └─────────────┼─────────────┘
                          │
                ┌─────────▼─────────┐
                │   Application     │
                │   Network         │
                └─────────┬─────────┘
                          │
            ┌─────────────┼─────────────┐
            │             │             │
    ┌───────▼──────┐ ┌─────▼─────┐ ┌───▼──────┐
    │   MQTT      │ │  InfluxDB │ │ Prometheus│
    │  Broker     │ │  Database  │ │ Monitoring│
    │ (Mosquitto) │ │           │ │          │
    └─────────────┘ └───────────┘ └──────────┘
```

## Network Configuration

### 1. Basic Network Setup

#### System Requirements
- **Server**: Minimum 2 vCPUs, 4GB RAM, 50GB storage
- **Network**: Public IP with port forwarding
- **Domain**: Registered domain name (e.g., sunlink.pp.ua)

#### Network Interface Configuration
```bash
# Configure network interface
sudo nano /etc/netplan/01-netcfg.yaml

# Example configuration
network:
  version: 2
  ethernets:
    eth0:
      dhcp4: no
      addresses: [192.168.25.20/24]
      gateway4: 192.168.25.1
      nameservers:
          addresses: [8.8.8.8, 8.8.4.4]
```

#### Apply Network Configuration
```bash
sudo netplan apply
sudo systemctl restart networking
```

### 2. Firewall Configuration

#### UFW Setup
```bash
# Install UFW
sudo apt update
sudo apt install ufw -y

# Default policies
sudo ufw default deny incoming
sudo ufw default allow outgoing

# Allow SSH
sudo ufw allow 22/tcp comment "SSH"

# Allow HTTP/HTTPS
sudo ufw allow 80/tcp comment "HTTP"
sudo ufw allow 443/tcp comment "HTTPS"

# Allow MQTT
sudo ufw allow 1883/tcp comment "MQTT"

# Allow InfluxDB
sudo ufw allow 8086/tcp comment "InfluxDB"

# Allow Prometheus
sudo ufw allow 9090/tcp comment "Prometheus"

# Enable firewall
sudo ufw enable
```

#### iptables Rules (Alternative)
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

### 3. DNS Configuration

#### DNS Records
Create the following DNS records:
```
Type: A
Name: @
Value: 192.168.25.20

Type: A
Name: www
Value: 192.168.25.20

Type: A
Name: api
Value: 192.168.25.20

Type: A
Name: mqtt
Value: 192.168.25.20
```

#### Local Hosts File
```bash
# Update local hosts file
sudo nano /etc/hosts

# Add entries
127.0.0.1       localhost
192.168.25.20   sunlink.pp.ua
192.168.25.20   www.sunlink.pp.ua
192.168.25.20   api.sunlink.pp.ua
192.168.25.20   mqtt.sunlink.pp.ua
```

## Security Configuration

### 1. SSL/TLS Setup

#### Let's Encrypt SSL Certificate
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

#### SSL Configuration
```bash
# Update Caddyfile with SSL
sudo nano /etc/caddy/Caddyfile

# Add SSL configuration
sunlink.pp.ua, www.sunlink.pp.ua {
    tls /etc/caddy/ssl/cert.pem /etc/caddy/ssl/key.pem
    
    # Security headers
    header {
        X-Content-Type-Options "nosniff"
        X-Frame-Options "DENY"
        X-XSS-Protection "1; mode=block"
        Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"
        -Server
    }
    
    # Rate limiting
    rate_limit {
        zone static {
            key {remote_host}
            events 100
            window 1m
        }
    }
    
    # Reverse proxy
    reverse_proxy fastapi-app:8000
}
```

### 2. System Security

#### User Management
```bash
# Create system user
sudo useradd -m -s /bin/bash sunlink
sudo usermod -aG sudo sunlink

# Set password
sudo passwd sunlink

# SSH key setup
sudo mkdir -p /home/sunlink/.ssh
sudo cp /root/.ssh/sunlink_key.pub /home/sunlink/.ssh/authorized_keys
sudo chown -R sunlink:sunlink /home/sunlink/.ssh
sudo chmod 600 /home/sunlink/.ssh/authorized_keys
```

#### System Hardening
```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install security tools
sudo apt install fail2ban -y

# Configure fail2ban
sudo nano /etc/fail2ban/jail.local

# Add configuration
[sshd]
enabled = true
port = 22
filter = sshd
logpath = /var/log/auth.log
maxretry = 3
bantime = 3600

# Start fail2ban
sudo systemctl start fail2ban
sudo systemctl enable fail2ban
```

### 3. Application Security

#### Environment Variables
```bash
# Create secure environment file
sudo nano /etc/sunlink/.env

# Set secure permissions
sudo chmod 600 /etc/sunlink/.env
```

#### Security Headers
```bash
# Update Caddyfile with security headers
sudo nano /etc/caddy/Caddyfile

# Add security headers
header {
    X-Content-Type-Options "nosniff"
    X-Frame-Options "DENY"
    X-XSS-Protection "1; mode=block"
    X-Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; connect-src 'self' wss: https:; font-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none';"
    Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"
    -Server
    Referrer-Policy "strict-origin-when-cross-origin"
    Permissions-Policy "camera=(), microphone=(), geolocation=()"
}
```

## Performance Optimization

### 1. System Optimization

#### Kernel Parameters
```bash
# Configure kernel parameters
sudo nano /etc/sysctl.conf

# Add optimizations
net.core.somaxconn = 65535
net.ipv4.tcp_max_syn_backlog = 65535
net.ipv4.tcp_tw_reuse = 1
net.ipv4.tcp_fin_timeout = 30
net.core.netdev_max_backlog = 65535
vm.swappiness = 10
```

#### Apply Kernel Changes
```bash
sudo sysctl -p
```

### 2. Database Optimization

#### InfluxDB Configuration
```bash
# Configure InfluxDB
sudo nano /etc/influxdb2/config.yml

# Add optimizations
[storage]
  cache-max-memory-size = 1073741824
  cache-snapshot-memory-size = 268435456
  compact-full-write-buffer-size = 1073741824
  compact-throughput = 50331648
  compact-throughput-burst = 50331648
  max-concurrent-compactions = 1
```

#### Database Maintenance
```bash
# Create backup script
sudo nano /usr/local/bin/sunlink-backup.sh

#!/bin/bash
BACKUP_DIR="/var/backups/sunlink"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

# Backup InfluxDB
docker exec influxdb influxd backup -portable /tmp/influxdb_backup
tar -czf $BACKUP_DIR/influxdb_$DATE.tar.gz -C /tmp influxdb_backup

# Cleanup old backups
find $BACKUP_DIR -name "*.tar.gz" -mtime +30 -delete

echo "Backup completed: $BACKUP_DIR/influxdb_$DATE.tar.gz"
```

### 3. Network Optimization

#### TCP Optimization
```bash
# Configure TCP settings
sudo nano /etc/sysctl.d/99-tcp.conf

# Add optimizations
net.core.rmem_max = 16777216
net.core.wmem_max = 16777216
net.ipv4.tcp_rmem = 4096 87380 16777216
net.ipv4.tcp_wmem = 4096 65536 16777216
net.ipv4.tcp_congestion_control = bbr
```

#### Apply TCP Settings
```bash
sudo sysctl -p /etc/sysctl.d/99-tcp.conf
```

## Monitoring and Logging

### 1. System Monitoring

#### Prometheus Setup
```bash
# Configure Prometheus
sudo nano /etc/prometheus/prometheus.yml

# Add monitoring targets
scrape_configs:
  - job_name: 'sunlink-backend'
    static_configs:
      - targets: ['fastapi-app:8000']
    metrics_path: '/metrics'
    scrape_interval: 30s
```

#### Grafana Dashboard
```bash
# Install Grafana
sudo apt install grafana -y

# Start Grafana
sudo systemctl start grafana-server
sudo systemctl enable grafana-server

# Configure data source
# Access: http://localhost:3000
# Username: admin
# Password: admin
```

### 2. Logging Configuration

#### Logrotate Setup
```bash
# Configure log rotation
sudo nano /etc/logrotate.d/sunlink

# Add configuration
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

#### Centralized Logging
```bash
# Install Filebeat
sudo apt install filebeat -y

# Configure Filebeat
sudo nano /etc/filebeat/filebeat.yml

# Add configuration
filebeat.inputs:
- type: log
  enabled: true
  paths:
    - /var/log/sunlink/*.log
  fields:
    service: sunlink
    environment: production

output.elasticsearch:
  hosts: ["localhost:9200"]
```

## Backup and Recovery

### 1. Backup Strategy

#### Automated Backup Script
```bash
#!/bin/bash
# /usr/local/bin/sunlink-backup.sh

BACKUP_DIR="/var/backups/sunlink"
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

# Create backup directory
mkdir -p $BACKUP_DIR

# Backup Docker volumes
docker run --rm -v sunlink_influxdb_data:/data -v $BACKUP_DIR:/backup alpine tar czf /backup/influxdb_$DATE.tar.gz -C /data .

# Backup configuration
tar -czf $BACKUP_DIR/config_$DATE.tar.gz -C /etc/sunlink .

# Backup SSL certificates
tar -czf $BACKUP_DIR/ssl_$DATE.tar.gz -C /etc/caddy/ssl .

# Clean old backups
find $BACKUP_DIR -name "*.tar.gz" -mtime +$RETENTION_DAYS -delete

echo "Backup completed: $BACKUP_DIR"
```

#### Backup Schedule
```bash
# Add to crontab
sudo crontab -e

# Add backup schedule
0 2 * * * /usr/local/bin/sunlink-backup.sh
```

### 2. Recovery Procedures

#### System Recovery
```bash
#!/bin/bash
# /usr/local/bin/sunlink-restore.sh

BACKUP_DIR="/var/backups/sunlink"
DATE=$1

if [ -z "$DATE" ]; then
    echo "Usage: $0 <backup-date>"
    exit 1
fi

# Stop services
docker-compose down

# Restore InfluxDB
docker run --rm -v sunlink_influxdb_data:/data -v $BACKUP_DIR:/backup alpine tar xzf /backup/influxdb_$DATE.tar.gz -C /data

# Restore configuration
tar -xzf $BACKUP_DIR/config_$DATE.tar.gz -C /etc/sunlink

# Restore SSL certificates
tar -xzf $BACKUP_DIR/ssl_$DATE.tar.gz -C /etc/caddy/ssl

# Start services
docker-compose up -d

echo "Recovery completed"
```

## Maintenance Procedures

### 1. Regular Maintenance

#### System Updates
```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Update Docker
sudo apt upgrade docker-ce docker-ce-cli containerd.io

# Update Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

#### Service Maintenance
```bash
# Restart services
docker-compose restart

# Clean up Docker
docker system prune -f
docker volume prune -f

# Monitor logs
docker-compose logs -f
```

### 2. Performance Monitoring

#### Health Checks
```bash
#!/bin/bash
# /usr/local/bin/sunlink-health-check.sh

# Check API health
if ! curl -f http://localhost:8000/api/v1/health > /dev/null 2>&1; then
    echo "API health check failed"
    docker-compose restart fastapi-app
fi

# Check MQTT health
if ! docker-compose exec -T mosquitto mosquitto -h localhost > /dev/null 2>&1; then
    echo "MQTT health check failed"
    docker-compose restart mosquitto
fi

# Check InfluxDB health
if ! curl -f http://localhost:8086/ping > /dev/null 2>&1; then
    echo "InfluxDB health check failed"
    docker-compose restart influxdb
fi

echo "Health check completed"
```

## Troubleshooting

### 1. Common Issues

#### Network Connectivity
```bash
# Check network connectivity
ping sunlink.pp.ua
curl -I https://sunlink.pp.ua

# Check firewall status
sudo ufw status

# Check port listening
sudo netstat -tlnp
```

#### Service Issues
```bash
# Check service status
docker-compose ps

# Check service logs
docker-compose logs -f fastapi-app
docker-compose logs -f mosquitto
docker-compose logs -f influxdb

# Check resource usage
htop
df -h
```

### 2. Performance Issues

#### Slow API Response
```bash
# Check API response time
curl -o /dev/null -s -w '%{time_total}\n' https://sunlink.pp.ua/api/v1/health

# Check database performance
docker-compose exec influxdb influx query "SHOW QUERIES"

# Check MQTT performance
docker-compose exec mosquitto mosquitto_sub -h localhost -t "deye/telemetry/+" -v | head -10
```

## Conclusion

This comprehensive network setup provides a secure, performant, and reliable foundation for the SunLink system. Regular maintenance and monitoring are essential to ensure optimal performance and security.

For additional support, please contact:
- **Email**: support@sunlink.pp.ua
- **GitHub**: https://github.com/Ponamarenko/sunlink
- **Documentation**: https://docs.sunlink.pp.ua

---

**SunLink Production Network Setup** - Enterprise-grade solar monitoring 🌞