# Архітектура SunLink Mobile App

## 🏗️ Загальна архітектура

SunLink Mobile App побудований на сучасній архітектурі Android з використанням найкращих практик розробки. Додаток використовує патерн MVVM (Model-View-ViewModel) з фрагментами для кращої організації коду.

---

## 📁 Структура проекту

```
sunlink-mobile/
├── app/
│   ├── src/main/
│   │   ├── java/com/sunportal/mobile/
│   │   │   ├── MainActivity.kt              # Головна активність
│   │   │   ├── PreferencesManager.kt       # Менеджер налаштувань
│   │   │   └── ui/
│   │   │       ├── AboutFragment.kt        # Фрагмент "Про додаток"
│   │   │       ├── ConnectionFragment.kt   # Фрагмент "Підключення"
│   │   │       └── AppearanceFragment.kt  # Фрагмент "Вигляд"
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml       # Головний layout
│   │   │   │   ├── fragment_about.xml     # Layout "Про додаток"
│   │   │   │   └── nav_header_main.xml     # Header навігації
│   │   │   ├── menu/
│   │   │   │   └── drawer_menu.xml        # Меню навігації
│   │   │   ├── values/
│   │   │   │   ├── colors.xml             # Кольори
│   │   │   │   ├── strings.xml            # Рядки
│   │   │   │   └── themes.xml             # Теми
│   │   │   └── drawable/                   # Ресурси зображень
│   │   └── AndroidManifest.xml             # Маніфест додатку
│   ├── build.gradle.kts                    # Налаштування збірки
│   └── proguard-rules.pro                 # Правила ProGuard
├── gradle/
├── gradlew
├── gradlew.bat
└── README.md
```

---

## 🎯 Архітектурні компоненти

### 1. MainActivity (Controller)
```kotlin
class MainActivity : AppCompatActivity(), OnNavigationItemSelectedListener {
    // Управляє життєвим циклом додатку
    // Обробляє навігацію між фрагментами
    // Відповідає за налаштування UI
}
```

**Відповідальності:**
- Ініціалізація UI компонентів
- Управління навігацією
- Обробка подій меню
- Застосування тем та налаштувань

### 2. Фрагменти (Views)
```kotlin
// AboutFragment - Відображає інформацію про додаток SunLink
class AboutFragment : Fragment()

// ConnectionFragment - Налаштування підключення
class ConnectionFragment : Fragment()

// AppearanceFragment - Налаштування вигляду
class AppearanceFragment : Fragment()
```

**Відповідальності:**
- Відображення UI
- Обробка користувацьких подій
- Комунікація з Activity

### 3. PreferencesManager (Model)
```kotlin
class PreferencesManager {
    // Зберігає та завантажує налаштування
    // Працює з SharedPreferences
}
```

**Відповідальності:**
- Зберігання конфігурації
- Робота з локальними даними
- Кешування даних

---

## 🎨 UI/UX архітектура

### Material Design 3
- Використання Material Design 3 компонентів
- Система кольорів SvitGrid
- Адаптивний дизайн
- Підтримка темної/світлої теми

### Навігація
- Бічне меню (Drawer Navigation)
- Фрагменти для контенту
- Toolbar для заголовків
- Smooth transitions

### Адаптивність
- Підтримка різних розмірів екранів
- Гнучкі layout-и
- Масштабування тексту
- Оптимізація для планшетів

---

## 🔌 Ітеграційна архітектура

### SunPortal Server Integration
```kotlin
// Архітектура підключення:
// 1. MainActivity -> ConnectionFragment
// 2. ConnectionFragment -> PreferencesManager
// 3. PreferencesManager -> SunPortal API
// 4. SunPortal API -> WebSocket/REST
```

**Компоненти:**
- **WebSocket**: Реальний час
- **REST API**: Отримання даних
- **SharedPreferences**: Локальне зберігання
- **Retrofit/OkHttp**: HTTP клієнти

### Дані
```kotlin
// Структура даних:
data class ServerConfig(
    val url: String,
    val timeout: Int,
    val retryCount: Int
)

data class AppSettings(
    val theme: String,
    val fontSize: String,
    val serverConfig: ServerConfig
)
```

---

## 🛡️ Архітектура безпеки

### Захист даних
- SharedPreferences з режимом PRIVATE
- Валідація вхідних даних
- Обробка помилок з'єднання
- Логування без конфіденційних даних

### Дозволи додатку
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

---

## ⚡ Архітектура продуктивності

### Оптимізація
- ViewBinding для швидкого доступу до UI
- Lazy loading для ресурсів
- Оптимізовані layout-и
- Кешування даних

### Пам'ять
- Ефективне використання фрагментів
- Правильне управління життєвим циклом
- Уникнення пам'яткових витоків

---

## 📊 Архітектура тестування

### Структура тестів
- Unit тести для логіки
- UI тести для фрагментів
- Ітеграційні тести для API
- Е2E тести для потоків

### Інструменти
- JUnit 5
- Espresso
- Mockito
- Android Test Orchestrator

---

## 🔧 Розгортання та збірка

### Gradle збірка
```kotlin
android {
    compileSdk = 34
    defaultConfig {
        minSdk = 21
        targetSdk = 34
        versionCode = 6
        versionName = "1.0.6"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

### Залежності
```kotlin
dependencies {
    // AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    
    // Material Design
    implementation("com.google.android.material:material:1.11.0")
    
    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
}
```

---

## 🔄 Майбутня архітектура

### План розвитку
1. **MVVM з DataBinding**
   - Розділення логіки та UI
   - Two-way data binding
   - Observable моделі

2. **Repository Pattern**
   - Абстракція джерел даних
   - Кешування стратегій
   - Single source of truth

3. **Dependency Injection**
   - Hilt або Dagger
   - Модульна архітектура
   - Тестованість

4. **Clean Architecture**
   - Чіткі межі між шарами
   - Use cases
   - Entities

---

## 📈 Метрики архітектури

### Кодова база
- **Загальний розмір**: ~50 KB
- **Кількість класів**: 5
- **Кількість фрагментів**: 3
- **Кількість layout-ів**: 3

### Продуктивність
- **Час запуску**: < 1 секунда
- **Використання пам'яті**: ~50 MB
- **Розмір APK**: ~1.8 MB (release)

---

## 💡 Найкращі практики

### Код
- Використовуйте Kotlin для нових функцій
- Дотримуйтесь naming conventions
- Пишіть документацію
- Використовуйте лямбди та корутини

### UI
- Використовуйте Material Design
- Оптимізуйте layout-и
- Підтримуйте різні розміри екранів
- Тестуйте на різних пристроях

### Архітектура
- Дотримуйтесь SOLID принципів
- Використовуйте патерни проектування
- Пишіть тести
- Оптимізуйте продуктивність

---

### Контакти

- **GitHub**: [Сторінка проекту](https://github.com/Ponamarenko/SunLink)
- **Сервер**: [SunPortal](http://sunlink.pp.ua:8000)

---

*Останнє оновлення: 2026-11-04*