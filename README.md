# Deye Gateway Server

Современный сервер для управления инверторами Deye SUN-12K-SG02LP1-EU-AM3 с поддержкой Modbus RTU/TCP, real-time мониторингом и безопасным управлением.

## 🎯 Особенности

- ✅ **Полная поддержка Deye SG02LP1-AM3**: Все регистры инвертора
- ✅ **Безопасное управление**: Ограничения на записи, валидация значений
- ✅ **Real-time мониторинг**: WebSocket для мгновенных обновлений
- ✅ **Современный веб-интерфейс**: Адаптивный дизайн для всех устройств
- ✅ **REST API**: Полная интеграция с мобильными приложениями
- ✅ **Docker поддержка**: Контейнеризованное развертывание
- ✅ **Система безопасности**: Emergency stop, rate limiting, cooldown

## 📋 Поддерживаемые модели

- Deye SUN-12K-SG02LP1-EU-AM3 (основная модель)
- Deye SG02LP1 семейство (с базовой поддержкой)
- Другие инверторы Deye с аналогичной картой регистров

## 🏗️ Архитектура

```
Deye Gateway Server
├── Modbus RTU/TCP шлюз
├── FastAPI веб-сервер
├── WebSocket real-time обновления
├── Система безопасности
├── Веб-интерфейс
└── REST API
```

## 🚀 Быстрый старт

### Требования

- Python 3.11+
- Modbus RTU/TCP конвертер (NA111-E)
- Доступ к инвертору Deye

### Установка

1. **Клонируйте репозиторий:**
```bash
git clone https://github.com/your-username/deye-gateway-server.git
cd deye-gateway-server
```

2. **Установите зависимости:**
```bash
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

3. **Настройте конфигурацию:**
```bash
nano config.yaml
# Измените IP адрес вашего NA111-E конвертера
```

4. **Запустите сервер:**
```bash
./start_server.sh
```

5. **Доступ к интерфейсу:**
- Веб-интерфейс: http://localhost:8000
- API документация: http://localhost:8000/docs

## ⚙️ Конфигурация

### config.yaml

```yaml
# Modbus настройки
modbus:
  host: "192.168.1.100"  # IP NA111-E конвертера
  port: 502              # Modbus TCP порт
  timeout: 10           # Таймаут подключения
  slave_id: 1           # ID устройства

# Сервер настройки
server:
  host: "0.0.0.0"
  port: 8000
  cors_origins:
    - "http://localhost:3000"

# Безопасность
safety:
  max_concurrent_writes: 2    # Максимум одновременных записей
  write_cooldown: 5           # Секунд между записями
  max_battery_charge_current: 100     # Ампер
  max_battery_discharge_current: 100 # Ампер
  max_solar_power_limit: 12000       # Вт
```

## 📡 API Эндпоинты

### REST API

- `GET /status` - Статус системы
- `GET /registers` - Список всех регистров
- `POST /read` - Чтение регистров
- `POST /write` - Безопасная запись регистра
- `GET /live` - Live данные
- `POST /emergency-stop` - Аварийная остановка
- `POST /emergency-resume` - Возобновление работы

### WebSocket

- `ws://localhost:8000/ws` - Real-time обновления каждые 5 секунд

## 📊 Поддерживаемые регистры

### Аккумулятор
- `battery_soc` (184) - State of Charge (%)
- `battery_voltage` (185) - Напряжение (V)
- `battery_temp` (186) - Температура (°C)
- `battery_power` (190) - Мощность (W)
- `battery_current` (191) - Ток (A)

### Солнечные панели (3 MPPT)
- `pv1_power`, `pv2_power`, `pv3_power` (186-188) - Мощность (W)
- `pv1_voltage`, `pv2_voltage`, `pv3_voltage` (109-113) - Напряжение (V)
- `pv1_current`, `pv2_current`, `pv3_current` (110-114) - Ток (A)

### Сеть и нагрузка
- `grid_voltage` (150) - Напряжение сети (V)
- `grid_frequency` (151) - Частота (Hz)
- `grid_inverter_power` (166) - Мощность инвертора (W)
- `total_load_power` (175) - Общая нагрузка (W)

### Энергетическая статистика
- `total_pv_production` (60) - Общая генерация (kWh)
- `daily_pv_production` (63) - Дневная генерация (kWh)
- `daily_buy_energy` (74) - Дневная покупка (kWh)
- `daily_sell_energy` (76) - Дневная продажа (kWh)

### Управление (записываемые)
- `max_solar_power_limit` (230) - Лимит солнечной мощности (W)
- `max_charge_current` (232) - Макс. ток заряда (A)
- `max_discharge_current` (233) - Макс. ток разряда (A)
- `tou_enable` (248) - Включение Time of Use

## 🛡️ Система безопасности

### Ограничения
- Максимум 2 одновременные записи
- 5 секунд cooldown между операциями
- Валидация всех значений перед записью
- Emergency stop для быстрой остановки

### Безопасные значения
- Максимальный ток заряда: 100A
- Максимальный ток разряда: 100A
- Максимальная солнечная мощность: 12000W

## 🌐 Веб-интерфейс

Интерфейс включает:
- Real-time дашборд со всеми показателями
- Панель управления для настройки параметров
- Журнал событий и ошибок
- Emergency stop кнопки

## 📱 Мобильная интеграция

Сервер предоставляет REST API и WebSocket для мобильных приложений. Пример интеграции:

```javascript
// WebSocket подключение
const ws = new WebSocket('ws://your-server:8000/ws');
ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    updateDashboard(data.data);
};

// API запросы
fetch('http://your-server:8000/live')
    .then(response => response.json())
    .then(data => updateMobileApp(data));
```

## 🐳 Docker развертывание

```bash
# Сборка образа
docker build -t deye-gateway .

# Запуск контейнера
docker run -p 8000:8000 deye-gateway
```

## 🔧 Системные сервисы

### Systemd сервис

```bash
# Копирование сервиса
sudo cp deye-gateway.service /etc/systemd/system/

# Запуск сервиса
sudo systemctl enable deye-gateway
sudo systemctl start deye-gateway
```

## 📈 Мониторинг

### Логирование
- Все операции логируются в `server.log`
- WebSocket соединения
- Ошибки Modbus
- Операции записи

### Статус
- `/status` - текущий статус системы
- `/health` - здоровье сервера (будет добавлено)

## 🚨 Отладка

### Проблемы с подключением
1. Проверьте IP адрес NA111-E в config.yaml
2. Убедитесь, что конвертер подключен к сети
3. Проверьте firewall настройки
4. Проверьте доступность порта 502

### Ошибки Modbus
- Проверьте slave_id настройки
- Убедитесь, что инвертор поддерживает Modbus
- Проверьте timeouts

## 🔄 Обновления

### Миграция с старого сервера
1. Остановите старый сервер: `pkill -f "api-test-simple.py"`
2. Установите новый сервер
3. Обновите мобильное приложение для работы с новым API
4. Проверьте все функции

### Версионирование
- Используйте Git для отслеживания изменений
- Создавайте ветки для новых функций
- Тестируйте на staging перед продакшеном

## 📄 Лицензия

MIT License - см. LICENSE файл для деталей

## 🤝 Вклад

Вклады приветствуются! Пожалуйста:

1. Форкните репозиторий
2. Создайте ветку для вашей функции
3. Внесите изменения
4. Создайте Pull Request

## 📞 Поддержка

Для вопросов и поддержки:
- Создайте Issue в GitHub
- Проверьте документацию
- Проверьте лог файлы

---

**Deye Gateway Server** - Надежное управление вашими солнечными системами