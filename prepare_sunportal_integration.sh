#!/bin/bash

# Подготовка файлов для интеграции с SunPortal

echo "🚀 Подготовка файлов для SunPortal Deye Integration..."

# Создаем архив с проектом
echo "📦 Создание архива проекта..."
tar -czf sunportal-deye-integration.tar.gz \
  mobile_app/ \
  deye_gateway.py \
  config.yaml \
  web_interface.html \
  requirements.txt \
  test_server_connection.py \
  Dockerfile \
  README.md \
  DEVELOPMENT_LOG.md \
  MIGRATION_REPORT.md \
  MOBILE_APP_REPORT.md \
  FINAL_PROJECT_SUMMARY.md

echo "✅ Архив создан: sunportal-deye-integration.tar.gz"

# Создаем инструкции для ручной загрузки
cat > MANUAL_INTEGRATION_STEPS.md << 'EOF'
# 🚀 Ручная интеграция Deye Gateway в SunPortal

## 📁 Шаг 1: Загрузка файлов

1. Скачайте архив: `sunportal-deye-integration.tar.gz`
2. Распакуйте его в корень вашего SunPortal репозитория

## 🌿 Шаг 2: Создание ветки

1. Перейдите в корень вашего SunPortal репозитория
2. Создайте новую ветку:
   ```bash
   git checkout -b deye-gateway
   ```

## 📦 Шаг 3: Добавление файлов

1. Распакуйте архив:
   ```bash
   tar -xzf sunportal-deye-integration.tar.gz
   ```

2. Добавьте файлы в Git:
   ```bash
   git add mobile-app/
   git add deye_gateway.py
   git add config.yaml
   git add web_interface.html
   git add requirements.txt
   git add test_server_connection.py
   git add Dockerfile
   git add *.md
   ```

## 💾 Шаг 4: Коммит и пуш

```bash
git commit -m "Add Deye Gateway integration

- Added Deye Gateway server for SUN-12K-SG02LP1-EU-AM3 inverter
- Added mobile app on Kotlin + Jetpack Compose
- Added REST API + WebSocket integration
- Added web interface for manual control
- Added Docker deployment support
- Added comprehensive documentation"

git push -u origin deye-gateway
```

## 📱 Шаг 5: Сборка мобильного приложения

1. Откройте `mobile-app/` в Android Studio
2. Синхронизируйте проект Gradle
3. Соберите APK:
   ```bash
   cd mobile-app
   ./gradlew assembleDebug
   ```

## 🖥️ Шаг 6: Запуск сервера

```bash
# Установка зависимостей
pip install -r requirements.txt

# Запуск сервера
python deye_gateway.py
```

## 🔧 Шаг 7: Настройка

1. Отредактируйте `config.yaml`:
   ```yaml
   modbus:
     host: "192.168.1.100"  # IP вашего NA111-E
     port: 502
     slave_id: 1
   ```

2. Проверьте работу:
   ```bash
   python test_server_connection.py
   ```

## 🎯 Структура проекта

```
SunPortal/
├── mobile-app/              # Мобильное приложение
├── deye_gateway.py         # Сервер
├── config.yaml             # Конфигурация
├── web_interface.html      # Веб-интерфейс
├── requirements.txt        # Зависимости
├── test_server_connection.py # Тестирование
├── Dockerfile             # Docker
└── *.md                  # Документация
```

## 📊 API интеграция

Сервер предоставляет API совместимое с SunPortal:

- `GET /api/status` - Статус системы
- `GET /api/live` - Live данные
- `GET /api/history` - Исторические данные
- `WebSocket /ws` - Real-time обновления

## 🚀 Готово!

Теперь у вас есть полная интеграция Deye Gateway с SunPortal!

📱 Мобильное приложение готово к использованию
🖥️ Сервер готов к запуску
🔗 Ветка готова к Pull Request
EOF

echo "✅ Инструкции созданы: MANUAL_INTEGRATION_STEPS.md"

# Создаем структуру для GitHub Releases
cat > RELEASE_NOTES.md << 'EOF'
# 🚀 SunPortal Deye Integration - Release Notes

## 📱 Версия 1.0.0

### 🎯 Новые функции

#### 📱 Мобильное приложение
- **Полное приложение на Kotlin + Jetpack Compose**
- **Real-time мониторинг** инвертора Deye
- **Современный UI** с Material Design 3
- **WebSocket интеграция** для мгновенных обновлений
- **Реактивная архитектура** MVVM
- **Оффлайн режим** с кэшированием

#### 🖥️ Серверная часть
- **Deye Gateway Server** на FastAPI
- **Полная поддержка** SUN-12K-SG02LP1-EU-AM3
- **REST API** + WebSocket
- **Веб-интерфейс** для ручного управления
- **Безопасная система** с rate limiting
- **Docker развертывание**

#### 🔧 Интеграция
- **Совместимость** с SunPortal API
- **Автоматическое подключение**
- **Кросс-платформенная** поддержка
- **Масштабируемая** архитектура

### 📊 Технические характеристики

- **Платформа**: Android 8.0+ (API 26+)
- **Язык**: Kotlin 1.9.10
- **UI**: Jetpack Compose 1.5.4
- **Сервер**: Python 3.11+ + FastAPI
- **База данных**: SQLite (готова)
- **Сеть**: REST + WebSocket
- **Контейнеризация**: Docker

### 🚀 Быстрый старт

1. **Сервер**:
   ```bash
   pip install -r requirements.txt
   python deye_gateway.py
   ```

2. **Мобильное приложение**:
   ```bash
   cd mobile-app
   ./gradlew assembleDebug
   ```

3. **Тестирование**:
   ```bash
   python test_server_connection.py
   ```

### 📱 Основные функции приложения

#### 🌞 Мониторинг солнечной энергии
- Мощность PV1, PV2, PV3 в реальном времени
- Общая генерация солнечной энергии
- Эффективность панелей

#### 🔋 Управление аккумулятором
- Уровень заряда (SOC)
- Напряжение и температура
- Мощность заряд/разряд
- Статус батареи

#### ⚡ Сетевые параметры
- Напряжение и частота сети
- Мощность импорт/экспорт
- Потребление нагрузки

#### 📊 Энергетическая статистика
- Дневная генерация
- Общая генерация
- Покупка/продача энергии
- Графики и тренды

### 🔧 Конфигурация

Отредактируйте `config.yaml`:

```yaml
modbus:
  host: "192.168.1.100"  # IP NA111-E
  port: 502
  slave_id: 1
  timeout: 10
```

### 🛡️ Безопасность

- Rate limiting: 2 запроса/секунду
- Валидация всех входных данных
- Emergency stop функция
- Логирование всех операций

### 📄 Документация

- [DEVELOPMENT_LOG.md](DEVELOPMENT_LOG.md) - Лог разработки
- [MIGRATION_REPORT.md](MIGRATION_REPORT.md) - Отчет миграции
- [MOBILE_APP_REPORT.md](MOBILE_APP_REPORT.md) - Отчет о мобильном приложении
- [FINAL_PROJECT_SUMMARY.md](FINAL_PROJECT_SUMMARY.md) - Финальная сводка

### 🤝 Поддержка

- GitHub Issues: Отслеживание багов
- Discussions: Обсуждение функционала
- Pull Requests: Внесение изменений

### 🎉 Готово!

Полная интеграция Deye Gateway с SunPortal готова к использованию!

---

**Версия**: 1.0.0  
**Дата**: 31 октября 2026  
**Поддержка**: Android 8.0+  
**Лицензия**: MIT
EOF

echo "✅ Release notes созданы: RELEASE_NOTES.md"

# Создаем ZIP архив для удобства
echo "📦 Создание ZIP архива..."
zip -r sunportal-deye-integration.zip \
  mobile_app/ \
  deye_gateway.py \
  config.yaml \
  web_interface.html \
  requirements.txt \
  test_server_connection.py \
  Dockerfile \
  *.md

echo "✅ ZIP архив создан: sunportal-deye-integration.zip"

echo ""
echo "🎉 Готово к интеграции!"
echo ""
echo "📁 Файлы для загрузки:"
echo "   - sunportal-deye-integration.tar.gz"
echo "   - sunportal-deye-integration.zip"
echo "   - MANUAL_INTEGRATION_STEPS.md"
echo "   - RELEASE_NOTES.md"
echo ""
echo "🚀 Следующие шаги:"
echo "1. Загрузите файлы в SunPortal репозиторий"
echo "2. Создайте ветку 'deye-gateway'"
echo "3. Сделайте коммит и пуш"
echo "4. Создайте релиз на GitHub"
echo "5. Соберите мобильное приложение"
echo ""
echo "📱 Приложение готово к использованию!"
echo "🖥️ Сервер готов к запуску!"
echo "🔗 Интеграция готова!"