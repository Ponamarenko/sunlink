# SunLink Documentation

Welcome to the SunLink documentation! This comprehensive guide provides all the information you need to understand, install, and use the SunLink system for monitoring and managing Deye inverters.

## 📚 Documentation Structure

### 🚀 Getting Started
- [Installation Guide](installation.md) - Step-by-step installation instructions
- [Configuration Guide](configuration.md) - System configuration and customization
- [Quick Start](quickstart.md) - Quick setup and first steps

### 🏗️ Architecture
- [System Architecture](architecture.md) - Overall system architecture
- [Universal Connection System](../UNIVERSAL_CONNECTION_SYSTEM.md) - Multiple connection types
- [Production Network Setup](../PRODUCTION_NETWORK_SETUP.md) - Network configuration

### 🔧 API Documentation
- [API Reference](api.md) - Complete API documentation
- [WebSocket API](websocket.md) - Real-time data streaming
- [Authentication](authentication.md) - JWT authentication

### 📱 Mobile Application
- [Flutter App Guide](mobile/flutter.md) - Mobile app development
- [App Features](mobile/features.md) - App features and capabilities

### 🔍 Monitoring & Maintenance
- [Monitoring Guide](monitoring.md) - System monitoring
- [Troubleshooting](troubleshooting.md) - Common issues and solutions
- [Backup & Recovery](backup.md) - Data backup and recovery

### 🛠️ Development
- [Development Guide](development.md) - Setting up development environment
- [Contributing](../CONTRIBUTING.md) - Contributing to the project
- [Code Standards](code-standards.md) - Coding standards and guidelines

## 🎯 Quick Links

- **Main Website**: [sunlink.pp.ua](https://sunlink.pp.ua)
- **GitHub Repository**: [Ponamarenko/sunlink](https://github.com/Ponamarenko/sunlink)
- **API Documentation**: [sunlink.pp.ua/docs](https://sunlink.pp.ua/docs)
- **Support**: [GitHub Issues](https://github.com/Ponamarenko/sunlink/issues)

## 📖 How to Use This Documentation

### For New Users
1. Start with the [Installation Guide](installation.md)
2. Follow the [Quick Start](quickstart.md) for basic setup
3. Read the [Configuration Guide](configuration.md) to customize your system

### For Developers
1. Follow the [Development Guide](development.md) to set up your environment
2. Review the [API Reference](api.md) for integration
3. Check the [Code Standards](code-standards.md) for contribution guidelines

### For System Administrators
1. Review the [Production Network Setup](../PRODUCTION_NETWORK_SETUP.md)
2. Follow the [Monitoring Guide](monitoring.md) for system health
3. Read the [Troubleshooting Guide](troubleshooting.md) for common issues

## 🚀 Installation Overview

The SunLink system consists of several components:

1. **Backend API** - FastAPI-based server for data processing
2. **MQTT Broker** - Message broker for real-time communication
3. **InfluxDB** - Time-series database for data storage
4. **Caddy Proxy** - Reverse proxy with SSL termination
5. **Mobile App** - Flutter-based mobile application

### System Requirements
- **OS**: Linux (Ubuntu 20.04+ / Debian 11+)
- **RAM**: 2GB minimum, 4GB recommended
- **CPU**: 1 core minimum, 2+ recommended
- **Storage**: 10GB+ free space
- **Network**: Internet connection for SSL certificates

## 🔧 Configuration Overview

The system is configured through environment variables and configuration files:

### Environment Variables
- `DOMAIN` - Your domain name
- `MQTT_HOST` - MQTT broker host
- `INFLUXDB_URL` - InfluxDB connection URL
- `JWT_SECRET` - JWT secret key

### Configuration Files
- `docker-compose.yml` - Docker services configuration
- `Caddyfile` - Reverse proxy configuration
- `mosquitto/config/` - MQTT broker configuration

## 📊 Data Flow

The system processes data through the following flow:

1. **Inverters** → **Universal Connection System** → **MQTT Broker**
2. **MQTT Broker** → **FastAPI Backend** → **InfluxDB**
3. **FastAPI Backend** → **WebSocket** → **Mobile App**
4. **Caddy Proxy** → **HTTPS** → **Users**

## 🔐 Security Features

- **JWT Authentication** - Secure API access
- **SSL/TLS Encryption** - All communications encrypted
- **Rate Limiting** - Prevent abuse and DoS attacks
- **Access Control** - Role-based access control
- **Data Encryption** - Sensitive data encrypted at rest

## 📈 Monitoring and Analytics

- **Prometheus** - System metrics collection
- **Grafana** - Data visualization
- **Logrotate** - Log rotation and management
- **Health Checks** - Automated health monitoring

## 🔄 Backup and Recovery

- **Automated Backups** - Daily backups of configuration and data
- **Disaster Recovery** - Complete system recovery procedures
- **Data Migration** - Migration between servers

## 🛠️ Troubleshooting

Common issues and their solutions:

- **Connection Issues** - Network connectivity problems
- **Performance Issues** - Slow response times
- **Data Loss** - Recovery procedures
- **Security Issues** - Security breach response

## 📞 Support and Community

- **GitHub Issues** - Bug reports and feature requests
- **GitHub Discussions** - Community discussions
- **Email Support** - Direct support for enterprise customers
- **Documentation** - Comprehensive guides and tutorials

## 🚀 Next Steps

1. **Choose Your Installation Method**:
   - [Docker Installation](installation.md#docker-installation)
   - [Manual Installation](installation.md#manual-installation)

2. **Configure Your System**:
   - [Basic Configuration](configuration.md#basic-configuration)
   - [Advanced Configuration](configuration.md#advanced-configuration)

3. **Connect Your Inverters**:
   - [RS485 Connection](../UNIVERSAL_CONNECTION_SYSTEM.md#rs485-to-lan-converters)
   - [WiFi Connection](../UNIVERSAL_CONNECTION_SYSTEM.md#esp32-wifi-connection)
   - [Ethernet Connection](../UNIVERSAL_CONNECTION_SYSTEM.md#direct-ethernet-connection)

4. **Deploy Your System**:
   - [Production Deployment](../PRODUCTION_NETWORK_SETUP.md)
   - [Monitoring Setup](monitoring.md)

---

**SunLink Documentation** - Your complete guide to solar monitoring 🌞