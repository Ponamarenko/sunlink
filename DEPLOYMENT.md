# Развертывание системы Deye Gateway

## Об архитектуре

Система состоит из 4 основных компонентов:

1. **deye-bridge** - опрос инверторов Deye через Modbus и публикация в MQTT
2. **mosquitto** - MQTT брокер для обмена сообщениями
3. **influxdb** - база данных для хранения исторических данных
4. **deye-api** - FastAPI backend для мобильного приложения
5. **nginx** - обратный прокси

## Быстрый старт

### 1. Клонирование репозитория
```bash
git clone <your-repo-url>
cd deye-gateway
```

### 2. Настройка конфигурации

#### 2.1. Настройка MQTT брокера
```bash
# Создать файл паролей
docker run --rm eclipse-mosquitto:2.0.15 mosquitto_passwd -c /mosquitto/config/passwd deye_user
# Ввести пароль: deye_pass
```

#### 2.2. Настройка инверторов
Редактировать файл `deye/config/config.yaml`:

```yaml
inverters:
  - id: "deye-01"
    name: "Дом 1 - Гараж"
    connection:
      host: "192.168.1.100"  # IP адрес вашего инвертора
      port: 502
      slave_id: 1
    polling:
      interval: 2  # секунды
```

#### 2.3. Настройка InfluxDB
```bash
# Создать токен для InfluxDB
# Запустить контейнер
docker run -it influxdb:2.7
# Внутри контейнера:
influx setup
# Указать:
# Organization: deye
# Bucket: deye_data
# Token: your-super-secret-token
```

### 3. Запуск системы
```bash
docker-compose up -d
```

### 4. Проверка работы
```bash
# Проверить статусы контейнеров
docker-compose ps

# Проверить логи
docker-compose logs -f deye-bridge
docker-compose logs -f deye-api

# Проверить MQTT топики
mosquitto_sub -h localhost -t "deye/telemetry/+" -u deye_user -P deye_pass
```

## API endpoints

### Базовые endpoints
- `GET /` - Информация о API
- `GET /health` - Проверка здоровья системы
- `GET /inverters` - Список всех инверторов
- `GET /aggregate` - Агрегированные данные

### Данные инверторов
- `GET /inverters/{id}` - Данные конкретного инвертора
- `GET /inverters/{id}/history?hours=24` - Исторические данные
- `POST /inverters/{id}/control` - Управление инвертором

### WebSocket
- `ws://localhost/ws` - Real-time обновления

## Конфигурация

### MQTT топики
- `deye/telemetry/{id}` - Телеметрические данные
- `deye/status/{id}` - Статус инвертора
- `deye/control/{id}` - Управляющие команды
- `deye/response/{id}` - Ответы на команды
- `deye/error/{id}` - Ошибки

### Примеры использования

#### 1. Получение данных инвертора
```bash
curl http://localhost/api/inverters/deye-01
```

#### 2. Управление инвертором
```bash
curl -X POST http://localhost/api/inverters/deye-01/control \
  -H "Content-Type: application/json" \
  -d '{"register": "max_charge_current", "value": 50}'
```

#### 3. Подписка на MQTT топики
```bash
mosquitto_sub -h localhost -t "deye/telemetry/+" -u deye_user -P deye_pass
```

## Мониторинг

### Логи
```bash
# Логи всех сервисов
docker-compose logs -f

# Логи конкретного сервиса
docker-compose logs -f deye-api
```

### Метрики
```bash
# Проверить статус InfluxDB
curl http://localhost:8086/health

# Проверить статус MQTT
mosquitto_pub -h localhost -t "test" -m "hello" -u deye_user -P deye_pass
```

## Обновление

### 1. Остановка системы
```bash
docker-compose down
```

### 2. Обновление образов
```bash
docker-compose pull
```

### 3. Запуск обновленной системы
```bash
docker-compose up -d
```

## Резервное копирование

### 1. Бэкап InfluxDB
```bash
# Создать бэкап
docker exec deye-influxdb influx backup /backup --token your-super-secret-token

# Скопировать бэкап
docker cp deye-influxdb:/backup ./backups/
```

### 2. Бэкап конфигурации
```bash
tar -czf config-backup.tar.gz deye/ mosquitto/ nginx/
```

## Восстановление

### 1. Восстановление InfluxDB
```bash
docker exec -i deye-influxdb influx restore /backup --token your-super-secret-token
```

### 2. Восстановление конфигурации
```bash
# Распаковать бэкап
tar -xzf config-backup.tar.gz

# Перезапустить контейнеры
docker-compose down && docker-compose up -d
```

## Отладка

### 1. Проблемы с подключением к инвертору
```bash
# Проверить доступность инвертора
ping 192.168.1.100

# Проверить порт Modbus
nc -zv 192.168.1.100 502
```

### 2. Проблемы с MQTT
```bash
# Проверить статус брокера
mosquitto_pub -h localhost -t "test" -m "hello" -u deye_user -P deye_pass

# Проверить подписки
mosquitto_sub -h localhost -t "#" -u deye_user -P deye_pass
```

### 3. Проблемы с API
```bash
# Проверить доступность API
curl http://localhost:8000/health

# Проверить логи
docker-compose logs -f deye-api
```

## Безопасность

### 1. Изменение паролей
```bash
# MQTT пароль
docker run --rm eclipse-mosquitto:2.0.15 mosquitto_passwd -c /mosquitto/config/passwd deye_user
```

### 2. SSL/TLS для MQTT
```bash
# Сгенерировать сертификаты
openssl req -x509 -newkey rsa:4096 -keyout mosquitto/config/certs/server.key -out mosquitto/config/certs/server.crt -days 365 -nodes
```

### 3. Фаервол
```bash
# Разрешить необходимые порты
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 1883/tcp
ufw allow 8086/tcp
```

## Масштабирование

### 1. Горизонтальное масштабирование API
```yaml
# docker-compose.yml
services:
  deye-api:
    deploy:
      replicas: 3
```

### 2. Горизонтальное масштабирование MQTT
```yaml
# Использовать внешний MQTT broker
services:
  mosquitto:
    external: true
```

## Интеграция с мобильным приложением

### 1. Пример подключения к WebSocket
```javascript
const ws = new WebSocket('ws://your-server/ws');

ws.onmessage = function(event) {
    const data = JSON.parse(event.data);
    console.log('Received data:', data);
};
```

### 2. Пример API запроса
```javascript
// Получение данных инвертора
fetch('/api/inverters/deye-01')
    .then(response => response.json())
    .then(data => console.log(data));
```

## Поддержка

### 1. Логи для отладки
- `deye-bridge` - логи опроса инверторов
- `deye-api` - логи API запросов
- `mosquitto` - логи MQTT брокера
- `influxdb` - логи базы данных

### 2. Проблемы и решения

#### Проблема: Нет данных от инвертора
- Проверить IP адрес и порт
- Проверить slave_id
- Проверить firewall

#### Проблема: MQTT не работает
- Проверить пароли
- Проверить топики
- Проверить брокер

#### Проблема: API не отвечает
- Проверить контейнеры
- Проверить порты
- Проверить логи