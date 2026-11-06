# SunLink Mobile Application

## Описание проекта

SunLink - современное мобильное приложение для мониторинга солнечных систем, разработанное на базе архитектуры SunPortal с использованием Material Design 3 для Android 15+.

## Технические характеристики

### Платформа
- **Целевая платформа**: Android 15+
- **Язык программирования**: Kotlin
- **Архитектура**: MVVM (Model-View-ViewModel)
- **UI Framework**: Material Design 3

### Основные функции

#### 1. Главный экран (Dashboard)
- Мониторинг мощности, напряжения, эффективности
- Статус батареи и инвертора
- Real-time обновление данных каждые 5 секунд
- Навигация между экранами

#### 2. Экран Real-time (Live)
- Детальный мониторинг параметров инвертора
- Информация о состоянии батареи
- Управление операциями
- Экспорт данных

#### 3. Экран Графиков (Charts)
- Графики мощности, эффективности, энергии
- Разные временные периоды (день, неделя, месяц, год)
- Разные типы графиков
- Экспорт графиков и данных

#### 4. Экран Батарея (Battery)
- Детальная информация о состоянии батареи
- Управление зарядкой/разрядкой
- Прогноз работы батареи
- История и настройки

#### 5. Экран Прогноз (Forecast)
- Прогноз производства энергии
- Погодный прогноз
- Оптимизация системы
- Рекомендации

#### 6. Настройки подключения (Connection)
- Поддержка RS485, ESP32, Solarman WiFi, Ethernet
- Настройка IP-адреса и порта
- Тестовое подключение

#### 7. Настройки внешнего вида (Appearance)
- Светлая/темная тема
- Размер шрифта
- Язык интерфейса
- Темы оформления

#### 8. О приложении (About)
- Версия приложения
- Информация о разработчике
- Копирайт

## Архитектура проекта

### Структура пакетов
```
com.sunportal.mobile/
├── MainActivity.kt              # Главный экран
├── MainViewModel.kt            # Бизнес-логика
├── data/                       # Модели данных
│   ├── InverterData.kt        # Данные инвертора
│   ├── BatteryData.kt         # Данные батареи
│   └── ForecastData.kt        # Данные прогноза
├── ui/                         # UI компоненты
│   ├── dashboard/             # Главный экран
│   │   └── DashboardFragment.kt
│   ├── LiveFragment.kt        # Real-time
│   ├── ChartsFragment.kt      # Графики
│   ├── BatteryFragment.kt     # Батарея
│   ├── ForecastFragment.kt    # Прогноз
│   ├── AboutFragment.kt       # О приложении
│   ├── ConnectionFragment.kt  # Подключение
│   └── AppearanceFragment.kt # Внешний вид
├── api/                        # API сервис
│   └── ApiService.kt          # Сервис API
├── services/                   # Фоновые сервисы
│   └── RealTimeUpdateService.kt # Real-time обновления
└── PreferencesManager.kt      # Менеджер настроек
```

### Технологии
- **Kotlin**: Основной язык программирования
- **MVVM**: Архитектурный паттерн
- **LiveData**: Наблюдение за изменениями данных
- **Coroutines**: Асинхронные операции
- **Material Design 3**: Современный UI дизайн
- **Navigation Component**: Навигация между экранами

## Подключение к серверу

SunLink поддерживает несколько типов подключения:

### 1. RS485
- Протокол: RS485
- Скорость: 9600 бод
- Адрес устройства: Настройка в конфигурации

### 2. ESP32
- Протокол: WiFi
- Адрес: IP адрес устройства
- Порт: 8080

### 3. Solarman WiFi
- Протокол: HTTPS
- Адрес: solarman.pp.ua
- Порт: 443

### 4. Ethernet
- Протокол: TCP/IP
- Адрес: IP адрес инвертора
- Порт: 502

## Форматы данных

### InverterData
```kotlin
data class InverterData(
    val currentPower: Double,      // Текущая мощность (Вт)
    val voltage: Double,           // Напряжение (В)
    val current: Double,           // Ток (А)
    val temperature: Double,       // Температура (°C)
    val status: String,            // Статус (Normal/Warning/Error)
    val efficiency: Double,        // Эффективность (%)
    val frequency: Double,         // Частота (Гц)
    val powerFactor: Double,       // Коэффициент мощности
    val totalEnergy: Double,       // Общая энергия (кВт·ч)
    val dailyEnergy: Double,       // Дневная энергия (кВт·ч)
    val reactivePower: Double,     // Реактивная мощность (ВАР)
    val apparentPower: Double      // Полная мощность (ВА)
)
```

### BatteryData
```kotlin
data class BatteryData(
    val status: String,            // Статус (Normal/Warning/Error)
    val level: Int,                // Уровень заряда (%)
    val temperature: Double,       // Температура (°C)
    val voltage: Double,           // Напряжение (В)
    val current: Double,           // Ток (А)
    val health: Int,               // Состояние (%)
    val cycles: Int,               // Циклов зарядки
    val voltageMax: Double,        // Максимальное напряжение
    val voltageMin: Double,        // Минимальное напряжение
    val currentMax: Double,        // Максимальный ток
    val currentMin: Double,        // Минимальный ток
    val power: Double,             // Мощность (Вт)
    val energy: Double              // Энергия (кВт·ч)
)
```

## Сборка проекта

### Требования
- Android SDK 34+
- JDK 21+
- Gradle 8.9

### Команды сборки
```bash
# Сборка debug версии
./gradlew assembleDebug

# Сборка release версии
./gradlew assembleRelease

# Создание APK
./gradlew build
```

### Результат сборки
- `app/build/outputs/apk/debug/app-debug.apk` - Debug версия
- `app/build/outputs/apk/release/app-release.apk` - Release версия

## Версии приложения

### v1.2.0 (Текущая)
- Полная реализация SunPortal архитектуры
- Material Design 3 поддержка
- Real-time мониторинг
- Все экраны функциональны
- Современный UI/UX

### v1.1.3
- Базовая реализация Dashboard
- Mock данные для демонстрации
- Простая навигация

### v1.0.x
- Первые версии приложения
- Базовый функционал

## Интеграция с сервером

### API endpoints
```
GET  /api/inverter/data    - Данные инвертора
GET  /api/battery/data     - Данные батареи
GET  /api/forecast/data   - Прогноз
POST /api/control          - Управление
```

### WebSocket соединение
```
ws://sunlink.pp.ua:8000/ws/realtime
```

## Логирование и отладка

### Уровни логирования
- **VERBOSE**: Детальная информация
- **DEBUG**: Информация для отладки
- **INFO**: Общая информация
- **WARN**: Предупреждения
- **ERROR**: Ошибки

### Формат логов
```
[2026-11-05 14:30:45] [INFO] SunLink - Приложение запущено
[2026-11-05 14:30:46] [DEBUG] MainViewModel - Данные обновлены
[2026-11-05 14:30:47] [WARN] DashboardFragment - Соединение нестабильно
```

## Лицензия

© 2026 Ponamarenko

## Контакты

Для технической поддержки и предложений:
- Email: support@sunlink.pp.ua
- GitHub: https://github.com/Ponamarenko/sunlink

---

**SunLink** - современное решение для мониторинга солнечных систем