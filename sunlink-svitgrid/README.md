# SunLink

Современный додаток для моніторингу сонячних систем з серверною частиною.

## Структура проекту

```
sunlink-svitgrid/
├── app/ (Android-приложение)
│   ├── src/main/java/com/sunlink/svitgrid/
│   │   ├── ui/ (UI компоненти)
│   │   ├── viewmodel/ (бізнес-логіка)
│   │   ├── network/ (API клієнт)
│   │   └── data/ (репозиторії)
│   └── build.gradle.kts
├── server/ (Python/FastAPI сервер)
│   ├── deye_gateway.py
│   ├── config.yaml
│   ├── requirements.txt
│   ├── start_server.sh
│   ├── deye-gateway.service
│   └── Dockerfile
└── README.md
```

## Android-приложение

### Сборка
```bash
cd sunlink-svitgrid
./gradlew assembleDebug
```

### Установка
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Сервер

### Запуск
```bash
cd sunlink-svitgrid/server
./start_server.sh
```

### Конфигурация
Отредактируйте `config.yaml` для настройки сервера.

## API

Сервер предоставляет REST API для мониторинга солнечных систем:
- `GET /api/v1/inverters` - список инверторов
- `GET /api/v1/inverters/{id}/data` - данные инвертора
- `GET /api/v1/system/stats` - статистика системы
- `GET /api/v1/health` - проверка здоровья

## Локализация

Приложение поддерживает украинский язык.

## Лицензия

MIT
