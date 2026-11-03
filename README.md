# 🌞 SunLink - Deye Inverter Integration System

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)](https://docs.docker.com/compose/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.104.1-green.svg)](https://fastapi.tiangolo.com/)
[![InfluxDB](https://img.shields.io/badge/InfluxDB-2.7-purple.svg)](https://www.influxdata.com/)

**Полностью автономная система мониторинга и управления инверторами Deye с локальным хранением данных и API для мобильных приложений.**

## 🚀 Особенности

- ✅ **Полная автономность** - без зависимостей от китайских серверов или Home Assistant
- 🔌 **Множественные подключения** - RS485, ESP32, Solarman WiFi, Ethernet
- 💾 **Локальное хранение** - InfluxDB для исторических данных
- 📱 **Мобильное приложение** - REST API + WebSocket для реального времени
- 🔐 **Безопасность** - JWT аутентификация и SSL
- 🐳 **Docker развертывание** - готовое решение для продакшена

## 📋 Системные требования

- **ОС**: Linux (Ubuntu 20.04+ / Debian 11+)
- **RAM**: 2GB минимум, 4GB рекомендовано
- **CPU**: 1 ядро минимум, 2+ рекомендовано
- **Диск**: 10GB+ свободного места
- **Сеть**: Доступ в интернет для SSL сертификатов

## 🏗️ Архитектура системы

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Mobile App    │    │   Web Interface │    │   Third-party   │
│   (Flutter)     │    │   (React/Vue)   │    │   Integrations  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │   Caddy Proxy  │  ← SSL Termination
                    │   (Port 80/443) │
                    └─────────────────┘
                                 │
                    ┌─────────────────┐
                    │  FastAPI       │  ← REST API + WebSocket
                    │  Backend       │  ← JWT Auth + Business Logic
                    └─────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   MQTT Broker   │    │   InfluxDB      │    │   Deye         │
│   (Mosquitto)   │    │   Time-series   │    │   Inverters    │
│   (Port 1883)   │    │   Database      │    │   (Various)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 📦 Быстрый старт

### 1. Клонирование репозитория

```bash
git clone https://github.com/Ponamarenko/sunlink.git
cd sunlink
```

### 2. Настройка конфигурации

```bash
# Копирование конфигурационных файлов
cp .env.example .env
nano .env
```

### 3. Запуск системы

```bash
# Запуск всех сервисов
docker-compose up -d

# Проверка статуса
docker-compose ps

# Просмотр логов
docker-compose logs -f
```

### 4. Доступ к системе

- **API**: `https://your-domain.com/api/v1/`
- **WebSocket**: `wss://your-domain.com/api/v1/live`
- **InfluxDB**: `http://your-domain.com:8086`
- **MQTT**: `mqtt://your-domain.com:1883`

## 🔧 Конфигурация

### .env файл

```env
# Базовые настройки
DOMAIN=sunlink.pp.ua
ENVIRONMENT=production
LOG_LEVEL=INFO

# MQTT настройки
MQTT_HOST=mosquitto
MQTT_PORT=1883
MQTT_USERNAME=sunlink_user
MQTT_PASSWORD=SunLinkMQTT2026

# InfluxDB настройки
INFLUXDB_URL=http://influxdb:8086
INFLUXDB_TOKEN=SunLinkInfluxDBToken2026
INFLUXDB_ORG=sunlink_org
INFLUXDB_BUCKET=deye_metrics

# JWT настройки
JWT_SECRET=SunLinkJWTSecret2026
JWT_EXPIRATION=2592000

# Безопасность
MAX_LOGIN_ATTEMPTS=5
PASSWORD_MIN_LENGTH=8
```

### Caddyfile

```caddy
# Основной домен
sunlink.pp.ua, www.sunlink.pp.ua {
    # Безопасность
    header {
        X-Content-Type-Options "nosniff"
        X-Frame-Options "DENY"
        Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"
    }
    
    # Маршрутизация
    reverse_proxy fastapi-app:8000
}

# API эндпоинты
api.sunlink.pp.ua {
    reverse_proxy fastapi-app:8000
}
```

## 📡 Поддерживаемые инверторы

### Подключение через RS485
- **Deye 5-10kW** - через конвертер RS485/LAN
- **Deye 3-6kW** - через конвертер RS485/LAN
- **Кастомные инверторы** - через универсальный мост

### Подключение через WiFi
- **Solarman WiFi Dongle** - прямое подключение
- **ESP32 с WiFi** - кастомные решения
- **Deye WiFi модули** - встроенные WiFi

### Подключение через Ethernet
- **Deye Ethernet** - прямое подключение
- **Deye Solarman** - через Solarman gateway

## 📊 Данные и метрики

### Структура данных

```json
{
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
    "efficiency": 95.8
  },
  "status": {
    "state": "NORMAL",
    "error_code": 0,
    "battery_soc": 78.5,
    "battery_voltage": 52.3
  }
}
```

### MQTT топики

```bash
# Телеметрия
deye/telemetry/INV001/voltage_dc
deye/telemetry/INV001/current_dc
deye/telemetry/INV001/power_ac

# Статус
deye/status/INV001/state
deye/status/INV001/error_code

# Управление
deye/control/INV001/restart
deye/control/INV001/settings_update
```

## 🔐 Безопасность

### Аутентификация
- **JWT токены** с 30-дневным сроком действия
- **Rate limiting** для API эндпоинтов
- **CORS** защита для веб-интерфейсов

### SSL/TLS
- **Let's Encrypt** сертификаты
- **HTTP/2** поддержка
- **Perfect Forward Secrecy**

### Защита данных
- **Шифрование MQTT** через TLS
- **Хеширование паролей**
- **Аудит логов**

## 📱 Мобильное приложение

### Flutter приложение
- **Кроссплатформенное** - iOS/Android
- **Реальный-time** - WebSocket подключение
- **Офлайн режим** - кэширование данных
- **Push уведомления** - важные события

### Основные функции
- 📊 **Мониторинг** в реальном времени
- 📈 **Графики** производительности
- ⚙️ **Управление** настройками
- 📱 **Уведомления** о событиях
- 🔋 **Батарея** статус и управление

## 🛠️ Разработка

### Локальная разработка

```bash
# Клонирование
git clone https://github.com/Ponamarenko/sunlink.git
cd sunlink

# Запуск разработки
docker-compose -f docker-compose.dev.yml up

# Запуск тестов
docker-compose -f docker-compose.test.yml up
```

### Backend (FastAPI)

```python
# Пример API вызова
curl -X POST "https://sunlink.pp.ua/api/v1/auth/login" \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "password"}'
```

### Frontend (React/Vue)

```javascript
// Пример WebSocket подключения
const ws = new WebSocket('wss://sunlink.pp.ua/api/v1/live');
ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    console.log('New telemetry data:', data);
};
```

## 📈 Мониторинг и логирование

### Prometheus метрики
- **HTTP запросы** - количество и время ответа
- **MQTT сообщения** - количество и задержка
- **InfluxDB запросы** - время выполнения
- **Системные метрики** - CPU, RAM, диск

### Логирование
- **Структурированные логи** в JSON формате
- **Ротация логов** - автоматическое удаление старых
- **Фильтрация** по уровням (INFO, WARNING, ERROR)
- **Централизованный сбор** через Fluentd/Loki

## 🔧 Обслуживание

### Резервное копирование

```bash
# База данных
docker exec influxdb influxd backup -portable /backup/influxdb

# Конфигурация
tar -czf backup/config-$(date +%Y%m%d).tar.gz \
    docker-compose.yml \
    Caddyfile \
    .env
```

### Обновление

```bash
# Остановка сервисов
docker-compose down

# Обновление образов
docker-compose pull

# Запуск обновленных сервисов
docker-compose up -d
```

### Очистка

```bash
# Очистка старых логов
docker system prune -f

# Очистка InfluxDB
influx delete --bucket deye_metrics --start 2026-01-01T00:00:00Z
```

## 🐛 Отладка

### Логи сервисов

```bash
# FastAPI логи
docker-compose logs -f fastapi-app

# MQTT логи
docker-compose logs -f mosquitto

# InfluxDB логи
docker-compose logs -f influxdb

# Caddy логи
docker-compose logs -f caddy
```

### Диагностика

```bash
# Проверка MQTT брокера
mosquitto_sub -h localhost -p 1883 -t "deye/telemetry/+/+" -v

# Проверка InfluxDB
curl -G "http://localhost:8086/query" \
     --data-urlencode "db=deye_metrics" \
     --data-urlencode "q=SELECT * FROM telemetry LIMIT 1"

# Проверка API
curl -X GET "http://localhost:8000/api/v1/health"
```

## 📄 Лицензия

Этот проект лицензирован под MIT License - см. файл [LICENSE](LICENSE) для деталей.

## 🤝 Вклад

Вклады приветствуются! Пожалуйста, прочитайте [CONTRIBUTING.md](CONTRIBUTING.md) для получения информации о том, как внести свой вклад.

## 📞 Поддержка

- **Issues**: [GitHub Issues](https://github.com/Ponamarenko/sunlink/issues)
- **Discussions**: [GitHub Discussions](https://github.com/Ponamarenko/sunlink/discussions)
- **Email**: support@sunlink.pp.ua

## 📚 Документация

- [API Documentation](docs/api.md)
- [Installation Guide](docs/installation.md)
- [Configuration Guide](docs/configuration.md)
- [Development Guide](docs/development.md)
- [Troubleshooting Guide](docs/troubleshooting.md)

## 🎯 Дорожная карта

- [x] Базовая архитектура системы
- [x] MQTT брокер с аутентификацией
- [x] InfluxDB для хранения данных
- [x] FastAPI бэкенд с JWT
- [x] WebSocket для реального времени
- [x] Caddy reverse proxy с SSL
- [ ] Мобильное приложение Flutter
- [ ] Веб-интерфейс React/Vue
- [ ] Дополнительные протоколы инверторов
- [ ] Система оповещений
- [ ] Интеграции с умным домом
- [ ] Расширенная аналитика

---

**SunLink** - Сделано с ❤️ для сообщества солнечной энергетики