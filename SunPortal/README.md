# SunPortal - Android приложение для управления солнечной домашней электростанцией

SunPortal - профессиональное Android-приложение для мониторинга и управления солнечной энергетической системой с инверторами Deye.

## 🌟 Основные возможности

### 📊 Мониторинг в реальном времени
- Схема потоков энергии (PV → инвертор → батарея/сеть → дом)
- Погодные условия и прогноз
- Мониторинг отключений (блекаутов)
- Прогноз производства энергии
- Управление тарифами TOU (Time of Use)

### 🎨 Визуальный интерфейс
- Стиль Svitgrid (темная тема, моноширинный шрифт JetBrains Mono)
- Градиентные карточки `#131A24 → #0A0D12`
- Современный Material Design 3 интерфейс
- Адаптивный дизайн для разных экранов

### 🔧 Технические характеристики
- **Версия**: 1.0.32
- **Kotlin**: 1.9.24
- **Android**: minSdk 26 (Android 8.0), targetSdk 36
- **Compose BOM**: 2024.06.00
- **Retrofit/Gson**: 2.9.0 / 2.10.1
- **Lottie Compose**: 6.1.0
- **Hilt**: Dependency Injection
- **DataStore**: Настройки приложения
- **MPAndroidChart**: Графики и статистика

### 📡 Интеграция с оборудованием
- **Инвертор**: Deye SUN-12K-SG02LP1-EU-AM3
- **Сервер**: deye_gateway.py (в репозитории `deye-integration/`)
- **API endpoints**:
  - `api/live/raw` - текущие показатели
  - `api/?action=history&period=day` - история для графиков
  - `api/?action=blackout` - график отключений
  - `weather_read.php` - погода
  - `forecast_read.php` - прогноз производства
  - `tou_read.php` - тарифы TOU

### 🌦️ Источники погоды
- **Авто**: сервер → Open-Meteo
- **Сервер**: только SunPortal сервер
- **Open-Meteo**: только Open-Meteo API

### 🔋 Управление батареей
- Оценка резерва (часы/дни)
- Настройки зарядки в зависимости от тарифов
- Пресеты: Blackout, От прогноза, Ночь, День

### 📱 Уведомления
- Канал "Сповіщення станції"
- Уведомления об отключениях
- Уведомления о заряде батареи
- Низкий заряд батареи

### 🔄 Фоновый режим
- Сбор данных в фоновом режиме
- Автоматическое обновление
- Эффективное использование батареи

## 📁 Структура проекта

```
app/src/main/java/app/sunportal/
├── MainActivity.kt                    — главная активность
├── SunPortalApplication.kt            — класс приложения
├── SunPortalApp.kt                   — основной компонент
├── di/                               — Hilt модули
│   └── SunPortalHiltModule.kt
├── api/                              — сеть
│   ├── ApiService.kt
│   ├── OpenMeteoApi.kt
│   └── RetrofitClient.kt
├── data/                             — данные
│   ├── SettingsRepository.kt
│   └── model/
│       ├── InverterData.kt
│       ├── WeatherData.kt
│       └── OpenMeteoData.kt
├── viewmodel/                        — ViewModel
│   └── MainViewModel.kt
├── ui/                               — интерфейс
│   ├── SunPortalApp.kt
│   ├── navigation/
│   │   └── SunPortalNavHost.kt
│   ├── theme/
│   │   ├── SunPortalTheme.kt
│   │   ├── Color.kt
│   │   ├── Typography.kt
│   │   └── Shape.kt
│   └── screens/
│       ├── HomeScreen.kt
│       ├── OverviewScreen.kt
│       ├── LiveScreen.kt
│       ├── BatteryScreen.kt
│       ├── ForecastScreen.kt
│       ├── ChartsScreen.kt
│       ├── TariffsScreen.kt
│       ├── TouScreen.kt
│       ├── SettingsScreen.kt
│       └── AboutScreen.kt
├── data/collection/                  — фоновый сервис
│   └── DataCollectionService.kt
└── BootReceiver.kt                   — автозапуск
```

## 🚀 Сборка

### Требования
- Android Studio Arctic Fox или новее
- JDK 11 или новее
- Android SDK 34

### Сборка в Android Studio
1. Откройте проект в Android Studio
2. Выберите сборку `debug` или `release`
3. Нажмите "Build" → "Build Bundle(s) / APK(s)" → "Build APK(s)"

### Сборка через Gradle
```bash
# Сборка debug версии
./gradlew assembleDebug

# Сборка release версии
./gradlew assembleRelease

# Полная очистка и сборка
./gradlew clean assembleDebug
```

### Сборка без ПК
- ☁️ **GitHub Actions** - сборка в облаке
- 📱 **Termux** - сборка прямо на телефоне

## 📄 Документация

- `docs/lottie-svitgrid.md` - Lottie ассеты
- `docs/PC-DEPENDENCIES.md` - зависимости для ПК
- `docs/PHONE-BUILD.md` - сборка без ПК
- `docs/ROADMAP.md` - план развития
- `AGENTS.md` - правила работы с репозиторием

## 📄 Лицензия

Lottie-файлы и иконки Svitgrid - художественная графика из приложения Svitgrid. Перед публикацией в Google Play необходимо проверить лицензию оригинала.

## 🤝 Вклад

Приветствуются pull request, bug report и предложения по улучшению!

---

**SunPortal** - профессиональный мониторинг солнечной энергетики