#!/bin/bash

# Скрипт запуска Android эмулятора для тестирования SunPortal APK

echo "🚀 Запуск Android эмулятора для SunPortal..."

# Проверяем наличие Android SDK
if [ ! -d "/tmp/android-sdk" ]; then
    echo "❌ Android SDK не найден. Устанавливаем..."
    cd /tmp
    mkdir -p android-sdk
    cd android-sdk
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
    unzip -q commandlinetools-linux-11076708_latest.zip
    mkdir -p cmdline-tools/latest
    mv cmdline-tools/* cmdline-tools/latest/ 2>/dev/null || true
    
    # Устанавливаем SDK
    export PATH=$PATH:/tmp/android-sdk/cmdline-tools/latest/bin
    yes | sdkmanager --licenses --sdk_root=/tmp/android-sdk
    sdkmanager "platforms;android-35" "platform-tools" "emulator" --sdk_root=/tmp/android-sdk
fi

# Запускаем эмулятор в фоновом режиме
echo "📱 Запуск эмулятора..."
cd /tmp/android-sdk
export ANDROID_SDK_ROOT=/tmp/android-sdk
export PATH=$PATH:/tmp/android-sdk/emulator:/tmp/android-sdk/platform-tools

# Запускаем эмулятор
nohup emulator @SunPortal_Test > emulator.log 2>&1 &
echo "⏳ Ожидание загрузки эмулятора..."

# Ждем загрузки
sleep 60

# Проверяем статус
if adb devices | grep -q "emulator-5554"; then
    echo "✅ Эмулятор успешно запущен!"
    echo "📡 ADB порт: 5555"
    echo "🌐 Веб интерфейс: http://localhost:5555"
    
    # Устанавливаем APK
    echo "📥 Установка SunPortal APK..."
    adb -s emulator-5555 install /root/opt/SunPortal/app/build/outputs/apk/debug/app-debug.apk
    
    if [ $? -eq 0 ]; then
        echo "✅ APK успешно установлен!"
        echo "🚀 Приложение готово к тестированию!"
    else
        echo "❌ Ошибка установки APK"
    fi
else
    echo "❌ Эмулятор не запустился"
    echo "📄 Логи эмулятора:"
    cat emulator.log
fi