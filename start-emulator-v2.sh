#!/bin/bash

# Скрипт запуска Android эмулятора через Genymotion Docker

echo "🚀 Запуск Android эмулятора для SunPortal..."

# Останавливаем существующие контейнеры
docker stop sunportal-emulator 2>/dev/null || true
docker rm sunportal-emulator 2>/dev/null || true

# Используем более стабильный образ эмулятора
docker run -d \
  --name sunportal-emulator \
  -p 5555:5555 \
  -p 6080:6080 \
  -e DEVICE=google_pixel_6 \
  -e ANDROID_VERSION=android_14 \
  -e RAM=4096 \
  -e STORAGE=8192 \
  --privileged \
  openstf/androidevices:latest

echo "⏳ Ожидание загрузки эмулятора..."

# Ждем загрузки
sleep 90

# Проверяем доступность
echo "📡 Проверка подключения к эмулятору..."

# Проверяем ADB подключение
if adb devices | grep -q "emulator-5555"; then
    echo "✅ Эмулятор успешно запущен!"
    
    # Устанавливаем APK
    echo "📥 Установка SunPortal APK..."
    adb -s emulator-5555 install /root/opt/SunPortal/app/build/outputs/apk/debug/app-debug.apk
    
    if [ $? -eq 0 ]; then
        echo "✅ APK успешно установлен!"
        
        # Запускаем приложение
        echo "🚀 Запуск SunPortal..."
        adb -s emulator-5555 shell am start -n app.sunportal/.MainActivity
        
        echo "✅ Приложение готово к тестированию!"
        echo ""
        echo "📱 Как тестировать:"
        echo "1. ADB подключение: localhost:5555"
        echo "2. Веб тестирование: http://192.168.25.117:8080/final-test-page.html"
        echo "3. API тестирование: http://192.168.25.117:8082"
        echo ""
        echo "📡 Команды для управления:"
        echo "- Остановить эмулятор: docker stop sunportal-emulator"
        echo "- Перезапустить: docker restart sunportal-emulator"
        echo "- Логи: docker logs sunportal-emulator"
    else
        echo "❌ Ошибка установки APK"
    fi
else
    echo "❌ Эмулятор не запустился"
    echo "📄 Логи эмулятора:"
    docker logs sunportal-emulator
fi