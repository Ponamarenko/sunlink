# SunLink Mobile - Build Script

## Описание скрипта для сборки приложения

## Требования
- Android SDK 34+
- JDK 21+
- Gradle 8.9
- Git

## Структура проекта
```
mobile_app/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/sunportal/mobile/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── MainViewModel.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── dashboard/
│   │   │   │   │   │   └── DashboardFragment.kt
│   │   │   │   │   ├── AboutFragment.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── InverterData.kt
│   │   │   │   │   ├── BatteryData.kt
│   │   │   │   │   └── ForecastData.kt
│   │   │   │   ├── api/
│   │   │   │   │   └── ApiService.kt
│   │   │   │   ├── services/
│   │   │   │   │   └── RealTimeUpdateService.kt
│   │   │   │   └── PreferencesManager.kt
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   ├── activity_main.xml
│   │   │   │   │   ├── fragment_dashboard.xml
│   │   │   │   │   └── fragment_about.xml
│   │   │   │   ├── menu/
│   │   │   │   │   └── drawer_menu.xml
│   │   │   │   └── values/
│   │   │   │       ├── colors.xml
│   │   │   │       ├── strings.xml
│   │   │   │       └── themes.xml
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle.kts
│   └── build.gradle.kts
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew
├── gradlew.bat
└── settings.gradle.kts
```

## Команды сборки

### 1. Очистка проекта
```bash
./gradlew clean
```

### 2. Сборка debug версии
```bash
./gradlew assembleDebug
```

### 3. Сборка release версии
```bash
./gradlew assembleRelease
```

### 4. Полная сборка
```bash
./gradlew build
```

### 5. Запрос тестов
```bash
./gradlew test
```

### 6. Генерация документации
```bash
./gradlew javadoc
```

## Пути к APK файлам

### Debug версия
```
app/build/outputs/apk/debug/app-debug.apk
```

### Release версия
```bash
app/build/outputs/apk/release/app-release.apk
```

## Настройка подписи

### 1. Создание файла ключа
```bash
keytool -genkey -v -keystore sunlink-release-key.keystore -alias sunlink -keyalg RSA -keysize 2048 -validity 10000 -storepass password -keypass password
```

### 2. Настройка в build.gradle.kts
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("sunlink-release-key.keystore")
            storePassword = "password"
            keyAlias = "sunlink"
            keyPassword = "password"
        }
    }
}
```

## Оптимизация сборки

### 1. Ускорение сборки
```bash
./gradlew assembleDebug --no-daemon --offline
```

### 2. Кэширование
```bash
./gradlew clean build --build-cache
```

### 3. Параллельная сборка
```bash
./gradlew assembleDebug --parallel
```

## Отладка сборки

### 1. Включение логов
```bash
./gradlew assembleDebug --info
```

### 2. Вывод зависимостей
```bash
./gradlew dependencies
```

### 3. Проверка зависимостей
```bash
./gradlew dependencyUpdates
```

## Автоматизация сборки

### 1. CI/CD интеграция
```yaml
# .github/workflows/build.yml
name: Build SunLink Mobile
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '21'
      - uses: gradle/gradle-build-action@v2
        with:
          gradle-version: '8.9'
      - run: ./gradlew assembleDebug
```

### 2. Автоматическое тестирование
```bash
#!/bin/bash
# test.sh
./gradlew test
./gradlew connectedCheck
```

## Развертие

### 1. Локальное развертывание
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. Play Store
```bash
./gradlew bundleRelease
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore sunlink-release-key.keystore app/build/outputs/bundle/release/app-release.aab sunlink
```

### 3. GitHub Releases
```bash
# Создание релиза
gh release create v1.2.0 sunlink-mobile-v1.2.0.apk --title "SunLink v1.2.0" --notes "$(cat RELEASE_NOTES.md)"
```

## Мониторинг производительности

### 1. Анализ размера APK
```bash
./gradlew app:dependencies | grep ".*" | grep -E "apk|aab" | head -10
```

### 2. Проверка памяти
```bash
./gradlew app:lintDebug --continue
```

### 3. Профилирование
```bash
./gradlew assembleDebug --profile
```

## Обновление зависимостей

### 1. Проверка обновлений
```bash
./gradlew dependencyUpdates
```

### 2. Обновление Gradle
```bash
./gradlew wrapper --gradle-version 8.9
```

### 3. Обновление Android Gradle Plugin
```bash
# build.gradle.kts
plugins {
    id("com.android.application") version "8.1.0"
    id("org.jetbrains.kotlin.android") version "1.9.0"
}
```

## Очистка

### 1. Полная очистка
```bash
./gradlew clean
rm -rf .gradle
rm -rf build
rm -rf .idea
```

### 2. Очистка кэша
```bash
rm -rf ~/.gradle/caches/
```

---

**Важно**: Перед каждой сборкой убедитесь, что все зависимости обновлены и проект очищен.