# SunPortal - Зависимости для ПК

## 🛠️ Требования для разработки

### Обязательные зависимости
- **Java Development Kit (JDK)** 11 или новее
- **Android Studio** Arctic Fox (2020.3.1) или новее
- **Android SDK** API 34
- **Android Build Tools** 34.0.0
- **NDK** 23.1.7779620
- **CMake** 3.18.1

### Рекомендуемые зависимости
- **Git** для контроля версий
- **Docker** для контейнеризации
- **Node.js** для сборки веб-интерфейса
- **Python** 3.9+ для серверной части

## 📦 Android SDK
Убедитесь, что установлены следующие SDK пакеты:

```bash
# Через Android Studio SDK Manager
android-34
build-tools-34.0.0
platform-tools
emulator
google-repositories
intel-haxm-android
maven-google
maven-central
```

## 🔧 Gradle
Проверьте версию Gradle в файле `gradle/wrapper/gradle-wrapper.properties`:

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-all.zip
```

## 📱 Установка эмулятора
```bash
# Создание эмулятора через Android Studio
# или через командную строку:
avdmanager create avd -n sunportal-emulator -k "system-images;android-34;google_apis;x86_64"
```

## 🧪 Тестирование
```bash
# Запуск юнит-тестов
./gradlew test

# Запуск инструментальных тестов
./gradlew connectedAndroidTest

# Генерация отчетов по тестам
./gradlew jacocoTestReport
```

## 📊 Сборка APK
```bash
# Debug версия
./gradlew assembleDebug

# Release версия
./gradlew assembleRelease

# APK с подписью
./gradlew assembleRelease -Pandroid.injected.signing.store.file=keystore.jks
```

## 🔑 Подпись APK
Для release сборки требуется ключ подписи:

```bash
# Создание ключа
keytool -genkey -v -keystore sunportal-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias sunportal

# Подпись APK
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 app/build/outputs/apk/release/app-release.apk sunportal-release.jks
```

## 🐛 Отладка
```bash
# Запуск отладчика
adb logcat | grep SunPortal

# Профилирование
adb shell am profile start sunportal
```

## 📈 Производительность
```bash
# Анализ производительности
./gradlew analyzeDebugBuild

# Проверка памяти
adb shell dumpsys meminfo app.sunportal
```

## 🌐 Сетевое взаимодействие
Для тестирования API сервера:

```bash
# Запуск тестового сервера
python -m http.server 8000

# Проверка подключения
curl http://localhost:8800/api/status
```

## 🔒 Безопасность
- Используйте HTTPS для API запросов
- Реализуйте SSL Pinning
- Шифруйте чувствительные данные
- Регулярно обновляйте зависимости

## 📚 Дополнительные ресурсы
- [Android Developer Documentation](https://developer.android.com)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Hilt Documentation](https://dagger.dev/hilt/)
- [Retrofit Documentation](https://square.github.io/retrofit/)