#!/bin/bash

# Скрипт запуска Android эмулятора через Docker

echo "🚀 Запуск Android эмулятора через Docker..."

# Останавливаем существующие контейнеры
docker stop android-test-emulator 2>/dev/null || true
docker rm android-test-emulator 2>/dev/null || true

# Запускаем эмулятор
docker run -d \
  --name android-test-emulator \
  -p 5555:5555 \
  -e EMULATOR_DEVICE=android_14 \
  -e EMULATOR_CPU=4 \
  -e EMULATOR_RAM=6144 \
  -e EMULATOR_STORAGE=12288 \
  -e EMULATOR_GPU=swiftshader \
  --privileged \
  docker.io/bbenbou/android-emulator:latest

echo "⏳ Ожидание загрузки эмулятора..."

# Ждем загрузки
sleep 120

# Проверяем доступность
echo "📡 Проверка подключения к эмулятору..."

# Проверяем ADB подключение
if adb connect localhost:5555 | grep -q "connected"; then
    echo "✅ Эмулятор успешно запущен!"
    
    # Устанавливаем APK
    echo "📥 Установка SunPortal APK..."
    adb -s localhost:5555 install /root/opt/SunPortal/app/build/outputs/apk/debug/app-debug.apk
    
    if [ $? -eq 0 ]; then
        echo "✅ APK успешно установлен!"
        
        # Запускаем приложение
        echo "🚀 Запуск SunPortal..."
        adb -s localhost:5555 shell am start -n app.sunportal/.MainActivity
        
        echo "✅ Приложение готово к тестированию!"
        echo ""
        echo "📱 Как тестировать:"
        echo "1. ADB подключение: localhost:5555"
        echo "2. Веб тестирование: http://192.168.25.117:8080/final-test-page.html"
        echo "3. API тестирование: http://192.168.25.117:8082"
        echo ""
        echo "📡 Команды для управления:"
        echo "- Остановить эмулятор: docker stop android-test-emulator"
        echo "- Перезапустить: docker restart android-test-emulator"
        echo "- Логи: docker logs android-test-emulator"
    else
        echo "❌ Ошибка установки APK"
    fi
else
    echo "❌ Эмулятор не запустился"
    echo "📄 Логи эмулятора:"
    docker logs android-test-emulator
fi