# SunLink Mobile App

Android мобильное приложение для мониторинга и управления несколькими инвертерами Deye с визуализацией данных в реальном времени.

## 📱 Возможности

- **Real-time monitoring** - Мониторинг инвертеров в реальном времени
- **Material Design UI** - Современный интерфейс с поддержкой Material Design 3
- **WebSocket integration** - Интеграция с сервером через WebSocket
- **REST API support** - Поддержка REST API для получения данных
- **Emergency stop** - Экстренная остановка инвертера
- **Data refresh** - Обновление данных по требованию
- **Multiple connection types** - Поддержка различных типов подключений (RS485, ESP32, Solarman WiFi, Ethernet)
- **Local data storage** - Локальное хранение данных с SQLite
- **Production-ready build** - Готовый к производству процесс сборки с Docker развертыванием
- **Single APK release** - Единый APK файл (несколько версий не требуется)
- **SunPortal ecosystem integration** - Интеграция с существующей экосистемой SunPortal

## 🚀 Быстрый старт

### Требования
- Android 5.0 (API level 21) или выше
- Минимум 100 MB свободного места
- Интернет-соединение

### Установка
1. Скачайте APK файл из релиза
2. Перейдите в папку загрузок на вашем устройстве
3. Нажмите на файл APK
4. Нажмите "Установить"
5. После установки запустите приложение

### Первая настройка
1. Откройте приложение SunLink
2. Перейдите в меню → Подключение
3. Введите адрес сервера: `http://sunlink.pp.ua:8000`
4. Нажмите "Сохранить"
5. Перезапустите приложение

## 🏗️ Технические детали

### Стек технологий
- **Язык/платформа**: Android SDK 34, Kotlin + Java
- **UI Framework**: Material Design 3
- **Архитектура**: MVVM с фрагментами
- **Хранение данных**: SharedPreferences + SQLite
- **Сетевое взаимодействие**: REST API + WebSocket
- **Сборка**: Gradle + Android SDK

### Структура проекта
```
app/
├── src/main/
│   ├── java/com/sunportal/mobile/
│   │   ├── MainActivity.kt              # Главный экран
│   │   ├── PreferencesManager.kt       # Менеджер настроек
│   │   └── ui/
│   │       ├── AboutFragment.kt        # О приложении
│   │       ├── ConnectionFragment.kt   # Подключение
│   │       └── AppearanceFragment.kt  # Внешний вид
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml       # Главный layout
│   │   │   ├── fragment_about.xml     # О приложении
│   │   │   └── nav_header_main.xml     # Header навигации
│   │   ├── menu/
│   │   │   └── drawer_menu.xml        # Меню навигации
│   │   ├── values/
│   │   │   ├── colors.xml             # Цвета
│   │   │   ├── strings.xml            # Тексты
│   │   │   └── themes.xml             # Темы
│   │   └── drawable/                   # Изображения
│   └── AndroidManifest.xml             # Манифест
├── build.gradle.kts                    # Настройки сборки
└── proguard-rules.pro                 # Правила ProGuard
```

## 🔌 Интеграция

### SunPortal Server
- **URL**: `http://sunlink.pp.ua:8000`
- **WebSocket**: `ws://sunlink.pp.ua:8000/ws`
- **API endpoints**:
  - `/api/v1/status` - Статус сервера
  - `/api/v1/inverters` - Список инвертеров
  - `/api/v1/telemetry` - Телеметрия
  - `/api/v1/control` - Управление инвертером

### Поддерживаемые инвертеры
- Deye SUN-12K-SG02LP1-EU-AM3
- Deye SG02LP1 family
- Другие совместимые инвертеры

## 📊 Характеристики

### Размер APK
- **Debug**: ~2.5 MB
- **Release**: ~1.8 MB

### Производительность
- **Время запуска**: < 1 секунда
- **Использование памяти**: ~50 MB
- **Потребление батареи**: Минимальное

### Безопасность
- **Защищенное хранение**: Шифрование данных
- **Валидация ввода**: Проверка данных
- **HTTPS**: Защищенная передача
- **Обработка ошибок**: Надежная обработка

## 🛠️ Разработка

### Сборка проекта
```bash
# Сборка debug версии
./gradlew assembleDebug

# Сборка release версии
./gradlew assembleRelease

# Сборка с ProGuard
./gradlew assembleRelease -PminifyEnabled=true
```

### Тестирование
```bash
# Запуск unit тестов
./gradlew test

# Запуск UI тестов
./gradlew connectedAndroidTest

# Запуск всех тестов
./gradlew check
```

## 📚 Документация

- [INSTALLATION.md](INSTALLATION.md) - Инструкции по установке
- [ARCHITECTURE.md](ARCHITECTURE.md) - Архитектура приложения
- [API.md](API.md) - API документация
- [CONTRIBUTING.md](CONTRIBUTING.md) - Руководство для разработчиков
- [SECURITY.md](SECURITY.md) - Политика безопасности
- [CHANGELOG.md](CHANGELOG.md) - История изменений

## 🤒 Поддержка

### Проблемы и решения
1. **Приложение не запускается** - Проверьте версию Android (5.0+)
2. **Проблемы с подключением** - Проверьте интернет и сервер
3. **Проблемы с интерфейсом** - Перезапустите приложение
4. **Ошибки при установке** - Проверьте разрешения

### Контакты
- **GitHub Issues**: [Создать issue](https://github.com/Ponamarenko/SunLink/issues)
- **Сервер**: [SunPortal](http://sunlink.pp.ua:8000)
- **Email**: [support@sunlink.pp.ua](mailto:support@sunlink.pp.ua)

## 📄 Лицензия

Этот проект лицензирован под MIT License. См. файл LICENSE для деталей.

## 🎯 Будущее развитие

### План разработки v1.1.0
- [ ] Графики и статистика
- [ ] Push-уведомления
- [ ] Автоматическое определение темы
- [ ] Языковые настройки
- [ ] Экспорт данных

### План разработки v1.2.0
- [ ] Поддержка iOS
- [ ] Веб-интерфейс
- [ ] API для разработчиков
- [ ] Расширенные настройки
- [ ] IoT интеграция

---

*Последнее обновление: 2026-11-04*