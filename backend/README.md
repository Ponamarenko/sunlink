# SunLink Backend API

FastAPI backend for SunLink - Deye inverter integration system.

## Features

- 🚀 **FastAPI** - High-performance web framework
- 📡 **MQTT Integration** - Real-time data from inverters
- 💾 **InfluxDB** - Time-series data storage
- 🔐 **JWT Authentication** - Secure API access
- 📱 **WebSocket** - Real-time data streaming
- 📊 **Prometheus** - System monitoring
- 🐳 **Docker** - Containerized deployment

## Quick Start

### Development

```bash
# Install dependencies
pip install -r requirements.txt

# Run development server
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

### Docker

```bash
# Build image
docker build -t sunlink-backend .

# Run container
docker run -d -p 8000:8000 sunlink-backend
```

## API Endpoints

### Authentication
- `POST /api/v1/auth/login` - User login
- `GET /api/v1/health` - Health check

### Inverters
- `GET /api/v1/inverters` - List all inverters
- `GET /api/v1/inverters/{id}` - Get inverter details
- `GET /api/v1/telemetry/{id}` - Get telemetry data

### WebSocket
- `WS /api/v1/live` - Real-time data streaming

## Configuration

Environment variables:

```env
MQTT_HOST=mosquitto
MQTT_PORT=1883
MQTT_USERNAME=sunlink_user
MQTT_PASSWORD=SunLinkMQTT2026

INFLUXDB_URL=http://influxdb:8086
INFLUXDB_TOKEN=SunLinkInfluxDBToken2026
INFLUXDB_ORG=sunlink_org
INFLUXDB_BUCKET=deye_metrics

JWT_SECRET=SunLinkJWTSecret2026
JWT_EXPIRATION=2592000
```

## MQTT Topics

- `deye/telemetry/{id}` - Telemetry data
- `deye/status/{id}` - Status updates
- `deye/control/{id}` - Control commands
- `deye/error/{id}` - Error messages

## Development

### Testing

```bash
pytest tests/
```

### Code Quality

```bash
black .
isort .
flake8 .
mypy .
```

## Monitoring

### Prometheus Metrics

- `http_requests_total` - HTTP request count
- `http_errors_total` - HTTP error count
- `active_connections` - WebSocket connections
- `mqtt_connection_status` - MQTT connection status

### Logs

Logs are written to `/app/logs/app.log` in JSON format.

## License

MIT License - see LICENSE file for details.