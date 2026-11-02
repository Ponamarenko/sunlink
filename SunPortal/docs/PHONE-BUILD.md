# SunPortal - Сборка без ПК

## 📱 Сборка прямо на Android устройстве

### Требования
- Termux
- Git
- JDK 11
- Android NDK
- Gradle

### Установка зависимостей

#### 1. Установка Termux
```bash
# Из F-Droid или Google Play
# Ссылка: https://f-droid.org/packages/com.termux/
```

#### 2. Установка необходимых пакетов
```bash
pkg update
pkg install git openjdk-11 ndk gradle python
```

#### 3. Клонирование репозитория
```bash
git clone https://github.com/Ponamarenko/SunPortal.git
cd SunPortal
```

### Сборка приложения

#### 1. Настройка Gradle
```bash
# Установка wrapper
./gradlew wrapper --no-daemon

# Проверка сборки
./gradlew tasks
```

#### 2. Сборка APK
```bash
# Debug версия
./gradlew assembleDebug

# Release версия
./gradlew assembleRelease
```

#### 3. Установка APK
```bash
# Установка debug версии
adb install app/build/outputs/apk/debug/app-debug.apk

# Установка release версии
adb install app/build/outputs/apk/release/app-release.apk
```

### Автоматизация сборки

#### 1. Создание скрипта сборки
```bash
cat > build-phone.sh << 'EOF'
#!/bin/bash
echo "📱 SunPortal Phone Build Script"
echo "=================================="

# Очистка
echo "🧹 Cleaning..."
./gradlew clean

# Сборка
echo "🔨 Building..."
./gradlew assembleDebug

# Установка
echo "📥 Installing..."
adb install app/build/outputs/apk/debug/app-debug.apk

echo "✅ Build complete!"
EOF

chmod +x build-phone.sh
```

#### 2. Запуск сборки
```bash
./build-phone.sh
```

### Обновление проекта

#### 1. Проверка обновлений
```bash
git fetch origin
git log --oneline -5
```

#### 2. Обновление кода
```bash
git pull origin main
```

### Тестирование

#### 1. Запуск приложения
```bash
adb shell am start -n app.sunportal/.MainActivity
```

#### 2. Проверка логов
```bash
adb logcat | grep SunPortal
```

#### 3. Мониторинг производительности
```bash
adb shell dumpsys cpuinfo | grep sunportal
```

### Общие проблемы

#### 1. Ошибка Gradle
```bash
# Решение: очистка кэша
./gradlew --stop
rm -rf ~/.gradle/caches/
```

#### 2. Ошибка NDK
```bash
# Решение: переустановка NDK
pkg uninstall ndk
pkg install ndk
```

#### 3. Ошибка Java
```bash
# Проверка версии Java
java -version

# Установка нужной версии
pkg install openjdk-11
```

### Оптимизация

#### 1. Уменьшение размера APK
```bash
# Включение ProGuard
./gradlew assembleRelease -Pandroid.enableR8=true
```

#### 2. Мульти-DEX поддержка
```bash
# В build.gradle.kts
android {
    defaultConfig {
        multiDexEnabled = true
    }
}
```

### Сборка для разных архитектур

#### 1. ARM64
```bash
./gradlew assembleDebug -Pandroid.ndk.abiFilters=arm64-v8a
```

#### 2. ARM32
```bash
./gradlew assembleDebug -Pandroid.ndk.abiFilters=armeabi-v7a
```

#### 3. x86_64
```bash
./gradlew assembleDebug -Pandroid.ndk.abiFilters=x86_64
```

### CI/CD для мобильных устройств

#### 1. GitHub Actions
```yaml
name: Phone Build
on:
  push:
    branches: [main]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    - name: Build APK
      run: |
        docker run --rm -v ${{ github.workspace }}:/app \
          -w /app gradle:8.5.0 \
          ./gradlew assembleDebug
```

#### 2. GitLab CI
```yaml
build_phone:
  image: gradle:8.5.0
  script:
    - ./gradlew assembleDebug
  artifacts:
    paths:
      - app/build/outputs/apk/debug/app-debug.apk
```

### Альтернативные методы

#### 1. Termux:API
```bash
# Установка
pkg install termux-api

# Уведомления
termux-notification --title "SunPortal" --content "Build complete"
```

#### 2. Magisk Module
```bash
# Создание модуля
mkdir -p module META-INF
cp app/build/outputs/apk/debug/app-debug.apk module/
```

### Полезные команды

#### 1. Мониторинг сборки
```bash
# В реальном времени
./gradlew assembleDebug --info

# С прогрессом
./gradlew assembleDebug --progress
```

#### 2. Анализ APK
```bash
# Размер APK
ls -lh app/build/outputs/apk/debug/app-debug.apk

# Содержимое APK
aapt dump badging app/build/outputs/apk/debug/app-debug.apk
```

### Заключение
Сборка SunPortal на Android устройстве полностью возможна с помощью Termux и Gradle. Это позволяет разработчикам работать в полевых условиях без необходимости в ПК.