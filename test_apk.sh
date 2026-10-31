#!/bin/bash

# Тестовый скрипт для SunPortal APK
echo "=== Тестирование SunPortal APK ==="
echo "Дата: $(date)"
echo ""

# Проверка существования APK
if [ -f "/root/opt/SunPortal/app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ APK файл найден"
    APK_SIZE=$(du -h "/root/opt/SunPortal/app/build/outputs/apk/debug/app-debug.apk" | cut -f1)
    echo "📁 Размер APK: $APK_SIZE"
    
    # Проверка APK с помощью aapt
    echo ""
    echo "🔍 Проверка APK структуры..."
    /tmp/android-sdk/build-tools/35.0.0/aapt dump badging "/root/opt/SunPortal/app/build/outputs/apk/debug/app-debug.apk" | head -10
    
    # Проверка подписи APK
    echo ""
    echo "🔐 Проверка подписи APK..."
    /tmp/android-sdk/build-tools/35.0.0/aapt dump xmltree "/root/opt/SunPortal/app/build/outputs/apk/debug/app-debug.apk" AndroidManifest.xml | grep -A5 "package"
    
    # Проверка разрешений
    echo ""
    echo "📋 Проверка разрешений APK..."
    /tmp/android-sdk/build-tools/35.0.0/aapt dump permissions "/root/opt/SunPortal/app/build/outputs/apk/debug/app-debug.apk" | grep "uses-permission"
    
    echo ""
    echo "✅ APK тестирование завершено"
else
    echo "❌ APK файл не найден"
    exit 1
fi