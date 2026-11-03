# 🌐 Universal Connection System for Deye Inverters

## Overview

The Universal Connection System provides a comprehensive solution for connecting various types of Deye inverters to the SunLink platform. This system supports multiple connection types and protocols, ensuring seamless integration regardless of the inverter model or communication method.

## Supported Connection Types

### 1. 📡 RS485 to LAN Converters
**Hardware Requirements:**
- RS485 to LAN converter (USB or Ethernet)
- Deye inverter with RS485 port
- Network cable

**Configuration:**
```yaml
connection:
  type: rs485_lan
  host: "192.168.1.100"  # Converter IP
  port: 8899             # Converter port
  baud_rate: 9600
  parity: "N"
  stop_bits: 1
  timeout: 30
```

**Setup Instructions:**
1. Connect RS485 converter to inverter
2. Connect converter to network
3. Configure network settings
4. Test connection using provided tools

### 2. 📶 ESP32 WiFi Connection
**Hardware Requirements:**
- ESP32 development board
- RS485 module
- WiFi connection

**Configuration:**
```yaml
connection:
  type: esp32_wifi
  ssid: "your_wifi_ssid"
  password: "your_wifi_password"
  mqtt_host: "sunlink.pp.ua"
  mqtt_port: 1883
  inverter_id: "INV001"
```

**Setup Instructions:**
1. Flash ESP32 with SunLink firmware
2. Configure WiFi credentials
3. Set up MQTT connection
4. Test with serial monitor

### 3. 🌐 Solarman WiFi Dongle
**Hardware Requirements:**
- Solarman WiFi dongle
- Deye inverter with Solarman support
- WiFi network

**Configuration:**
```yaml
connection:
  type: solarman_wifi
  dongle_ip: "192.168.1.101"
  local_port: 8899
  mqtt_host: "sunlink.pp.ua"
  mqtt_port: 1883
```

**Setup Instructions:**
1. Connect Solarman dongle to inverter
2. Connect dongle to WiFi
3. Configure network settings
4. Test connection

### 4. 🔌 Direct Ethernet Connection
**Hardware Requirements:**
- Deye inverter with Ethernet port
- Network cable
- DHCP server or static IP

**Configuration:**
```yaml
connection:
  type: direct_ethernet
  inverter_ip: "192.168.1.102"
  port: 8899
  use_dhcp: true
```

**Setup Instructions:**
1. Connect inverter to network
2. Configure network settings
3. Test connection
4. Set up monitoring

## Connection Protocol

### Data Format
All connections use a standardized data format:

```json
{
  "inverter_id": "INV001",
  "timestamp": "2026-11-03T10:00:00Z",
  "voltage_dc": 48.5,
  "current_dc": 12.3,
  "power_dc": 597.15,
  "voltage_ac": 230.1,
  "current_ac": 8.5,
  "power_ac": 1955.85,
  "frequency": 50.0,
  "temperature": 35.2,
  "efficiency": 95.8,
  "state": "NORMAL",
  "error_code": 0,
  "battery_soc": 78.5,
  "battery_voltage": 52.3
}
```

### Communication Protocol
- **MQTT**: Primary communication protocol
- **HTTP**: Configuration and status updates
- **WebSocket**: Real-time data streaming
- **Serial**: Direct communication for ESP32

## Configuration Management

### Centralized Configuration
All connection configurations are stored in a centralized database:

```python
class ConnectionConfig:
    def __init__(self, connection_type: str, **kwargs):
        self.connection_type = connection_type
        self.config = kwargs
        self.status = "inactive"
        self.last_seen = None
        self.error_count = 0
```

### Dynamic Configuration Updates
Connections can be updated dynamically without restarting:

```python
def update_connection(connection_id: str, new_config: dict):
    """Update connection configuration dynamically."""
    connection = get_connection(connection_id)
    connection.config.update(new_config)
    connection.restart()
```

## Monitoring and Diagnostics

### Connection Health Monitoring
```python
class ConnectionHealth:
    def __init__(self, connection_id: str):
        self.connection_id = connection_id
        self.last_heartbeat = None
        self.response_time = 0
        self.error_rate = 0
        self.data_quality = 100
```

### Error Handling
```python
def handle_connection_error(connection_id: str, error: str):
    """Handle connection errors."""
    connection = get_connection(connection_id)
    connection.error_count += 1
    
    if connection.error_count > 5:
        connection.status = "error"
        notify_admin(f"Connection {connection_id} has errors")
    
    log_error(f"Connection {connection_id}: {error}")
```

## Security Features

### Authentication
- **Device Authentication**: Each connection has unique credentials
- **Token-based Authentication**: API access requires valid tokens
- **SSL/TLS Encryption**: All communications are encrypted

### Access Control
- **Role-based Access**: Different access levels for different users
- **IP Filtering**: Restrict access by IP address
- **Rate Limiting**: Prevent abuse and DoS attacks

## Performance Optimization

### Data Compression
- **Protocol Buffers**: Efficient data serialization
- **Delta Encoding**: Only send changes in data
- **Batch Processing**: Group multiple readings together

### Caching Strategy
- **Local Caching**: Store recent data locally
- **Cache Invalidation**: Automatic cache updates
- **Cache Optimization**: Smart cache management

## Troubleshooting

### Common Issues

1. **Connection Timeout**
   - Check network connectivity
   - Verify inverter is powered on
   - Check firewall settings

2. **Data Corruption**
   - Check cable connections
   - Verify baud rate settings
   - Test with different converters

3. **Authentication Errors**
   - Verify credentials
   - Check token expiration
   - Review access permissions

### Diagnostic Tools

```bash
# Test connection
python tools/test_connection.py --type rs485_lan --ip 192.168.1.100

# Monitor data flow
python tools/monitor_data.py --connection INV001

# Check system health
python tools/health_check.py
```

## Integration Examples

### Home Assistant Integration
```yaml
# configuration.yaml
sunlink:
  api_url: "https://sunlink.pp.ua/api/v1"
  api_key: "your_api_key"
  inverters:
    - id: "INV001"
      name: "Main Inverter"
      connection_type: "rs485_lan"
```

### Node-RED Integration
```javascript
// SunLink Node-RED node
node-red-contrib-sunlink:
  type: "sunlink-inverter"
  url: "https://sunlink.pp.ua/api/v1"
  inverter_id: "INV001"
  interval: 1000
```

## Future Enhancements

### Planned Features
- **Bluetooth Support**: Direct connection via Bluetooth
- **LoRaWAN**: Remote monitoring for off-grid systems
- **5G Integration**: High-speed mobile connectivity
- **Edge Computing**: Local data processing

### Roadmap
- Q1 2026: Enhanced security features
- Q2 2026: Mobile app integration
- Q3 2026: Advanced analytics
- Q4 2026: AI-powered monitoring

## Support

For technical support, please:
1. Check the troubleshooting guide
2. Review the configuration examples
3. Contact support@sunlink.pp.ua
4. Submit an issue on GitHub

## Contributing

We welcome contributions to the Universal Connection System! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

---

**SunLink Universal Connection System** - Making solar energy monitoring accessible to everyone 🌞