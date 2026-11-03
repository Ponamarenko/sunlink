# API Documentation

Welcome to the SunLink API documentation. This comprehensive guide provides all the information you need to integrate with the SunLink platform for monitoring and managing Deye inverters.

## 📋 API Overview

The SunLink API provides a RESTful interface for accessing inverter data, managing system configurations, and controlling inverter operations. The API uses JSON for data exchange and supports both HTTP and WebSocket protocols.

### Base URL
```
Production: https://api.sunlink.pp.ua
Development: http://localhost:8000
```

### Authentication
All API endpoints (except for authentication and health checks) require JWT token authentication. Include the token in the Authorization header:

```
Authorization: Bearer <your_jwt_token>
```

### Response Format
All API responses follow a consistent format:

```json
{
  "success": true,
  "data": {},
  "message": "Success message",
  "timestamp": "2026-11-03T10:00:00Z"
}
```

### Error Format
Error responses follow this format:

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Error description",
    "details": "Additional error details"
  },
  "timestamp": "2026-11-03T10:00:00Z"
}
```

## 🔐 Authentication

### Login
Authenticate with the system to obtain a JWT token.

**Endpoint:** `POST /api/v1/auth/login`

**Request Body:**
```json
{
  "username": "admin",
  "password": "password"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "token_type": "bearer",
    "expires_in": 2592000
  },
  "message": "Login successful"
}
```

**Example:**
```bash
curl -X POST "https://api.sunlink.pp.ua/api/v1/auth/login" \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "password"}'
```

### Refresh Token
Refresh an expired JWT token using a refresh token.

**Endpoint:** `POST /api/v1/auth/refresh`

**Request Body:**
```json
{
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "token_type": "bearer",
    "expires_in": 2592000
  },
  "message": "Token refreshed successfully"
}
```

### Logout
Invalidate the current JWT token.

**Endpoint:** `POST /api/v1/auth/logout`

**Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Response:**
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

## 🏥 Health Check

### System Health
Check the overall health of the SunLink system.

**Endpoint:** `GET /api/v1/health`

**Response:**
```json
{
  "success": true,
  "data": {
    "status": "healthy",
    "timestamp": "2026-11-03T10:00:00Z",
    "mqtt_connected": true,
    "influxdb_connected": true,
    "active_connections": 5,
    "uptime": 1234567
  },
  "message": "System is healthy"
}
```

**Example:**
```bash
curl -X GET "https://api.sunlink.pp.ua/api/v1/health"
```

### Service Health
Check the health of individual services.

**Endpoint:** `GET /api/v1/health/services`

**Response:**
```json
{
  "success": true,
  "data": {
    "services": {
      "fastapi": {
        "status": "healthy",
        "uptime": 1234567,
        "memory_usage": 45.2,
        "cpu_usage": 12.5
      },
      "mqtt": {
        "status": "healthy",
        "connected_clients": 3,
        "messages_sent": 1234,
        "messages_received": 5678
      },
      "influxdb": {
        "status": "healthy",
        "disk_usage": 35.6,
        "memory_usage": 78.9,
        "query_time": 0.023
      }
    }
  },
  "message": "Services health check completed"
}
```

## 📡 Inverter Management

### List Inverters
Get a list of all registered inverters.

**Endpoint:** `GET /api/v1/inverters`

**Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "inverters": [
      {
        "id": "INV001",
        "name": "Main Inverter",
        "type": "deye_10kw",
        "status": "online",
        "last_seen": "2026-11-03T10:00:00Z",
        "connection_type": "rs485_lan"
      },
      {
        "id": "INV002",
        "name": "Backup Inverter",
        "type": "deye_5kw",
        "status": "offline",
        "last_seen": "2026-11-03T09:30:00Z",
        "connection_type": "wifi"
      }
    ],
    "count": 2
  },
  "message": "Inverters retrieved successfully"
}
```

**Example:**
```bash
curl -X GET "https://api.sunlink.pp.ua/api/v1/inverters" \
     -H "Authorization: Bearer <your_jwt_token>"
```

### Get Inverter Details
Get detailed information about a specific inverter.

**Endpoint:** `GET /api/v1/inverters/{inverter_id}`

**Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Parameters:**
- `inverter_id`: The unique identifier of the inverter

**Response:**
```json
{
  "success": true,
  "data": {
    "inverter": {
      "id": "INV001",
      "name": "Main Inverter",
      "type": "deye_10kw",
      "status": "online",
      "last_seen": "2026-11-03T10:00:00Z",
      "connection_type": "rs485_lan",
      "location": "Main Solar Array",
      "installation_date": "2026-01-01",
      "specifications": {
        "max_power": 10000,
        "max_voltage": 1000,
        "max_current": 20,
        "efficiency": 95.8
      },
      "configuration": {
        "grid_voltage": 230,
        "grid_frequency": 50,
        "battery_capacity": 10000,
        "max_charge_current": 50,
        "max_discharge_current": 50
      }
    }
  },
  "message": "Inverter details retrieved successfully"
}
```

### Update Inverter Configuration
Update configuration settings for an inverter.

**Endpoint:** `PUT /api/v1/inverters/{inverter_id}/config`

**Headers:**
```
Authorization: Bearer <your_jwt_token>
Content-Type: application/json
```

**Parameters:**
- `inverter_id`: The unique identifier of the inverter

**Request Body:**
```json
{
  "name": "Updated Inverter Name",
  "location": "Updated Location",
  "configuration": {
    "grid_voltage": 240,
    "max_charge_current": 60,
    "max_discharge_current": 60
  }
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "inverter_id": "INV001",
    "updated_config": {
      "name": "Updated Inverter Name",
      "location": "Updated Location",
      "configuration": {
        "grid_voltage": 240,
        "max_charge_current": 60,
        "max_discharge_current": 60
      }
    }
  },
  "message": "Inverter configuration updated successfully"
}
```

### Delete Inverter
Remove an inverter from the system.

**Endpoint:** `DELETE /api/v1/inverters/{inverter_id}`

**Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Parameters:**
- `inverter_id`: The unique identifier of the inverter

**Response:**
```json
{
  "success": true,
  "data": {
    "inverter_id": "INV001",
    "deleted_at": "2026-11-03T10:00:00Z"
  },
  "message": "Inverter deleted successfully"
}
```

## 📊 Telemetry Data

### Get Current Telemetry
Get the latest telemetry data for an inverter.

**Endpoint:** `GET /api/v1/telemetry/{inverter_id}/current`

**Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Parameters:**
- `inverter_id`: The unique identifier of the inverter

**Response:**
```json
{
  "success": true,
  "data": {
    "inverter_id": "INV001",
    "timestamp": "2026-11-03T10:00:00Z",
    "telemetry": {
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
  },
  "message": "Current telemetry data retrieved successfully"
}
```

### Get Historical Telemetry
Get historical telemetry data for an inverter.

**Endpoint:** `GET /api/v1/telemetry/{inverter_id}/history`

**Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Parameters:**
- `inverter_id`: The unique identifier of the inverter
- `start_time` (optional): Start time (ISO 8601 format)
- `end_time` (optional): End time (ISO 8601 format)
- `limit` (optional): Number of records to return (default: 1000)
- `offset` (optional): Offset for pagination (default: 0)

**Response:**
```json
{
  "success": true,
  "data": {
    "inverter_id": "INV001",
    "query": {
      "start_time": "2026-11-03T00:00:00Z",
      "end_time": "2026-11-03T23:59:59Z",
      "limit": 1000,
      "offset": 0
    },
    "total_records": 86400,
    "telemetry_data": [
      {
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
    ]
  },
  "message": "Historical telemetry data retrieved successfully"
}
```

### Get Aggregated Data
Get aggregated telemetry data for time periods.

**Endpoint:** `GET /api/v1/telemetry/{inverter_id}/aggregated`

**Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Parameters:**
- `inverter_id`: The unique identifier of the inverter
- `period` (optional): Aggregation period (hourly, daily, weekly, monthly)
- `start_time` (optional): Start time (ISO 8601 format)
- `end_time` (optional): End time (ISO 8601 format)

**Response:**
```json
{
  "success": true,
  "data": {
    "inverter_id": "INV001",
    "period": "daily",
    "aggregated_data": [
      {
        "date": "2026-11-03",
        "total_energy_kwh": 45.2,
        "avg_power_w": 1883.3,
        "max_power_w": 2500.0,
        "min_power_w": 0.0,
        "avg_temperature_c": 35.2,
        "max_temperature_c": 45.0,
        "min_temperature_c": 25.0
      }
    ]
  },
  "message": "Aggregated data retrieved successfully"
}
```

## 🔧 Control Commands

### Restart Inverter
Restart a specific inverter.

**Endpoint:** `POST /api/v1/inverters/{inverter_id}/restart`

**Headers:**
```
Authorization: Bearer <your_jwt_token>
Content-Type: application/json
```

**Parameters:**
- `inverter_id`: The unique identifier of the inverter

**Request Body:**
```json
{
  "reason": "Scheduled maintenance"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "inverter_id": "INV001",
    "command": "restart",
    "status": "sent",
    "timestamp": "2026-11-03T10:00:00Z"
  },
  "message": "Restart command sent successfully"
}
```

### Update Settings
Update inverter settings.

**Endpoint:** `POST /api/v1/inverters/{inverter_id}/settings`

**Headers:**
```
Authorization: Bearer <your_jwt_token>
Content-Type: application/json
```

**Parameters:**
- `inverter_id`: The unique identifier of the inverter

**Request Body:**
```json
{
  "settings": {
    "grid_voltage": 230,
    "grid_frequency": 50,
    "battery_capacity": 10000,
    "max_charge_current": 50,
    "max_discharge_current": 50,
    "backup_power_limit": 2000
  }
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "inverter_id": "INV001",
    "settings_updated": {
      "grid_voltage": 230,
      "grid_frequency": 50,
      "battery_capacity": 10000,
      "max_charge_current": 50,
      "max_discharge_current": 50,
      "backup_power_limit": 2000
    }
  },
  "message": "Settings updated successfully"
}
```

### Emergency Stop
Emergency stop for an inverter.

**Endpoint:** `POST /api/v1/inverters/{inverter_id}/emergency-stop`

**Headers:**
```
Authorization: Bearer <your_jwt_token>
Content-Type: application/json
```

**Parameters:**
- `inverter_id`: The unique identifier of the inverter

**Request Body:**
```json
{
  "reason": "Emergency situation"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "inverter_id": "INV001",
    "command": "emergency_stop",
    "status": "sent",
    "timestamp": "2026-11-03T10:00:00Z"
  },
  "message": "Emergency stop command sent successfully"
}
```

## 📱 Real-time Data (WebSocket)

### WebSocket Connection
Connect to the WebSocket endpoint for real-time data streaming.

**Endpoint:** `wss://api.sunlink.pp.ua/api/v1/live`

**Connection:**
```javascript
const ws = new WebSocket('wss://api.sunlink.pp.ua/api/v1/live');

// Authentication
ws.onopen = () => {
  ws.send(JSON.stringify({
    type: 'auth',
    token: 'your_jwt_token'
  }));
};

// Message handling
ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log('Received data:', data);
};

// Error handling
ws.onerror = (error) => {
  console.error('WebSocket error:', error);
};
```

### Message Types

#### Telemetry Update
```json
{
  "type": "telemetry",
  "inverter_id": "INV001",
  "data": {
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
  },
  "timestamp": "2026-11-03T10:00:00Z"
}
```

#### Status Update
```json
{
  "type": "status",
  "inverter_id": "INV001",
  "data": {
    "status": "online",
    "last_seen": "2026-11-03T10:00:00Z",
    "connection_type": "rs485_lan"
  },
  "timestamp": "2026-11-03T10:00:00Z"
}
```

#### Error Notification
```json
{
  "type": "error",
  "inverter_id": "INV001",
  "data": {
    "error_code": 1001,
    "error_message": "Overtemperature detected",
    "severity": "warning"
  },
  "timestamp": "2026-11-03T10:00:00Z"
}
```

#### Command Response
```json
{
  "type": "command_response",
  "inverter_id": "INV001",
  "data": {
    "command_id": "cmd_123456",
    "command": "restart",
    "status": "completed",
    "result": "Success"
  },
  "timestamp": "2026-11-03T10:00:00Z"
}
```

## 📊 System Monitoring

### Get System Statistics
Get overall system statistics and metrics.

**Endpoint:** `GET /api/v1/system/stats`

**Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "system": {
      "uptime": 1234567,
      "version": "1.0.0",
      "environment": "production"
    },
    "inverters": {
      "total": 5,
      "online": 4,
      "offline": 1,
      "total_power_kw": 25.5
    },
    "energy": {
      "today_kwh": 156.7,
      "month_kwh": 4523.8,
      "year_kwh": 54321.0
    },
    "mqtt": {
      "messages_sent": 12345,
      "messages_received": 12340,
      "connected_clients": 3
    },
    "database": {
      "disk_usage_gb": 45.6,
      "memory_usage_mb": 789,
      "query_count": 123456
    }
  },
  "message": "System statistics retrieved successfully"
}
```

### Get System Logs
Get system logs for debugging and monitoring.

**Endpoint:** `GET /api/v1/system/logs`

**Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Parameters:**
- `level` (optional): Log level (error, warning, info, debug)
- `start_time` (optional): Start time (ISO 8601 format)
- `end_time` (optional): End time (ISO 8601 format)
- `limit` (optional): Number of records to return (default: 100)

**Response:**
```json
{
  "success": true,
  "data": {
    "logs": [
      {
        "timestamp": "2026-11-03T10:00:00Z",
        "level": "info",
        "message": "System started successfully",
        "component": "system"
      },
      {
        "timestamp": "2026-11-03T10:01:00Z",
        "level": "warning",
        "message": "High temperature detected on INV001",
        "component": "inverter"
      }
    ]
  },
  "message": "System logs retrieved successfully"
}
```

## 🔒 User Management

### Get Current User
Get information about the currently authenticated user.

**Endpoint:** `GET /api/v1/user/me`

**Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "user": {
      "id": "user_123",
      "username": "admin",
      "email": "admin@sunlink.pp.ua",
      "role": "admin",
      "created_at": "2026-01-01T00:00:00Z",
      "last_login": "2026-11-03T10:00:00Z",
      "permissions": [
        "read_inverters",
        "write_inverters",
        "read_system",
        "write_system"
      ]
    }
  },
  "message": "User information retrieved successfully"
}
```

### Update User Profile
Update user profile information.

**Endpoint:** `PUT /api/v1/user/profile`

**Headers:**
```
Authorization: Bearer <your_jwt_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "email": "new_email@sunlink.pp.ua",
  "name": "Admin User",
  "preferences": {
    "theme": "dark",
    "language": "en",
    "timezone": "Europe/Kiev"
  }
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "user": {
      "id": "user_123",
      "username": "admin",
      "email": "new_email@sunlink.pp.ua",
      "name": "Admin User",
      "preferences": {
        "theme": "dark",
        "language": "en",
        "timezone": "Europe/Kiev"
      }
    }
  },
  "message": "User profile updated successfully"
}
```

### Change Password
Change user password.

**Endpoint:** `PUT /api/v1/user/password`

**Headers:**
```
Authorization: Bearer <your_jwt_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "current_password": "old_password",
  "new_password": "new_secure_password",
  "confirm_password": "new_secure_password"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Password changed successfully"
}
```

## 📈 Analytics and Reports

### Generate Energy Report
Generate an energy consumption report.

**Endpoint:** `GET /api/v1/analytics/energy-report`

**Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Parameters:**
- `start_time` (optional): Start time (ISO 8601 format)
- `end_time` (optional): End time (ISO 8601 format)
- `format` (optional): Report format (pdf, csv, json)

**Response:**
```json
{
  "success": true,
  "data": {
    "report": {
      "period": {
        "start": "2026-11-01T00:00:00Z",
        "end": "2026-11-30T23:59:59Z"
      },
      "summary": {
        "total_energy_kwh": 1245.67,
        "avg_daily_kwh": 41.52,
        "peak_power_kw": 8.5,
        "efficiency": 94.2
      },
      "by_inverter": [
        {
          "inverter_id": "INV001",
          "energy_kwh": 623.45,
          "avg_power_kw": 4.25,
          "efficiency": 94.5
        },
        {
          "inverter_id": "INV002",
          "energy_kwh": 622.22,
          "avg_power_kw": 4.25,
          "efficiency": 93.9
        }
      ]
    }
  },
  "message": "Energy report generated successfully"
}
```

### Get Performance Metrics
Get performance metrics for analysis.

**Endpoint:** `GET /api/v1/analytics/performance`

**Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Parameters:**
- `inverter_id` (optional): Specific inverter ID
- `start_time` (optional): Start time (ISO 8601 format)
- `end_time` (optional): End time (ISO 8601 format)

**Response:**
```json
{
  "success": true,
  "data": {
    "performance": {
      "period": {
        "start": "2026-11-01T00:00:00Z",
        "end": "2026-11-30T23:59:59Z"
      },
      "metrics": {
        "availability": 99.9,
        "reliability": 99.8,
        "efficiency": 94.2,
        "mtbf": 720,  // Mean Time Between Failures (hours)
        "mttr": 2     // Mean Time To Repair (hours)
      },
      "trends": {
        "efficiency": [94.1, 94.2, 94.3, 94.2, 94.4],
        "availability": [99.8, 99.9, 99.9, 99.8, 99.9]
      }
    }
  },
  "message": "Performance metrics retrieved successfully"
}
```

## 🚨 Error Handling

### Error Codes
The API uses standard HTTP status codes and additional error codes:

| HTTP Status | Error Code | Description |
|-------------|------------|-------------|
| 200 | SUCCESS | Request successful |
| 201 | CREATED | Resource created successfully |
| 400 | BAD_REQUEST | Invalid request parameters |
| 401 | UNAUTHORIZED | Authentication required |
| 403 | FORBIDDEN | Insufficient permissions |
| 404 | NOT_FOUND | Resource not found |
| 422 | VALIDATION_ERROR | Request validation failed |
| 429 | RATE_LIMITED | Rate limit exceeded |
| 500 | INTERNAL_ERROR | Internal server error |
| 502 | BAD_GATEWAY | Gateway error |
| 503 | SERVICE_UNAVAILABLE | Service temporarily unavailable |

### Error Response Format
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Error description",
    "details": "Additional error details",
    "timestamp": "2026-11-03T10:00:00Z"
  }
}
```

### Common Error Scenarios

#### Authentication Errors
```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Invalid authentication credentials",
    "details": "The provided JWT token is invalid or expired"
  }
}
```

#### Validation Errors
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": {
      "field": "inverter_id",
      "error": "required",
      "message": "Inverter ID is required"
    }
  }
}
```

#### Rate Limiting
```json
{
  "success": false,
  "error": {
    "code": "RATE_LIMITED",
    "message": "Rate limit exceeded",
    "details": "You have exceeded the rate limit. Please try again later."
  }
}
```

## 📞 API Support

### SDKs and Libraries
- **Python**: `pip install sunlink-api`
- **JavaScript**: `npm install sunlink-api`
- **Java**: `Maven: com.sunlink:api-client`
- **C#**: `NuGet: SunLink.ApiClient`

### Interactive Documentation
The API provides interactive documentation at:
- **Swagger UI**: `https://api.sunlink.pp.ua/docs`
- **ReDoc**: `https://api.sunlink.pp.ua/redoc`

### Support Channels
- **Email**: api-support@sunlink.pp.ua
- **GitHub Issues**: [sunlink-api](https://github.com/Ponamarenko/sunlink-api)
- **Discord**: [SunLink API Channel](https://discord.gg/sunlink)

---

**API Documentation** - Complete reference for SunLink integration 🌞