# Розробка SunPortal Mobile App

Цей посібник допоможе вам розпочати розробку SunPortal Mobile App.

---

## 🚀 Швидкий старт

### Вимоги
- Android Studio Arctic Fox або новіша
- JDK 11 або новіший
- Android SDK 34
- Gradle 7.4

### Клонування репозиторію
```bash
git clone https://github.com/Ponamarenko/SunPortal.git
cd sunportal-mobile
```

### Відкриття в Android Studio
```bash
./gradlew build
# Або відкрийте проект в Android Studio
```

---

## 🏗️ Структура проекту

```
app/
├── src/main/
│   ├── java/com/sunportal/mobile/
│   │   ├── MainActivity.kt              # Головна активність
│   │   ├── PreferencesManager.kt       # Менеджер налаштувань
│   │   └── ui/
│   │       ├── AboutFragment.kt        # Фрагмент "Про додаток"
│   │       ├── ConnectionFragment.kt   # Фрагмент "Підключення"
│   │       └── AppearanceFragment.kt  # Фрагмент "Вигляд"
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml       # Головний layout
│   │   │   ├── fragment_about.xml     # Layout "Про додаток"
│   │   │   └── nav_header_main.xml     # Header навігації
│   │   ├── menu/
│   │   │   └── drawer_menu.xml        # Меню навігації
│   │   ├── values/
│   │   │   ├── colors.xml             # Кольори
│   │   │   ├── strings.xml            # Рядки
│   │   │   └── themes.xml             # Теми
│   │   └── drawable/                   # Ресурси зображень
│   └── AndroidManifest.xml             # Маніфест додатку
├── build.gradle.kts                    # Налаштування збірки
└── proguard-rules.pro                 # Правила ProGuard
```

---

## 🛠️ Розробка

### Kotlin найкращі практики

```kotlin
// Використовуйте лямбди та корутини
viewModelScope.launch {
    when (val result = api.getStatus()) {
        is Result.Success -> {
            updateUI(result.data)
        }
        is Result.Failure -> {
            showError(result.exception.message)
        }
    }
}

// Використовуйте data classes для даних
data class Inverter(
    val id: String,
    val name: String,
    val status: String,
    val power: Int
)

// Використовуйте extension functions
fun String.isValidUrl(): Boolean {
    return try {
        URL(this)
        true
    } catch (e: MalformedURLException) {
        false
    }
}
```

### UI розробка

```xml
<!-- Використовуйте Material Design компоненти -->
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="8dp">
    
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">
        
        <TextView
            android:id="@+id/inverter_name"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="18sp"
            android:textColor="@color/svitgrid_text_primary"/>
            
        <TextView
            android:id="@+id/inverter_status"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="14sp"
            android:textColor="@color/svitgrid_text_secondary"/>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

### Архітектура

```kotlin
// Використовуйте MVVM патерн
class InverterViewModel : ViewModel() {
    private val _inverterStatus = MutableLiveData<Inverter>()
    val inverterStatus: LiveData<Inverter> = _inverterStatus
    
    private val repository = InverterRepository()
    
    fun loadInverterStatus() {
        viewModelScope.launch {
            val result = repository.getInverterStatus()
            _inverterStatus.value = result
        }
    }
}

// Repository патерн
class InverterRepository {
    private val apiService = RetrofitService.create()
    
    suspend fun getInverterStatus(): Inverter {
        return apiService.getInverterStatus()
    }
}
```

---

## 🧪 Тестування

### Unit тести

```kotlin
class InverterViewModelTest {
    private lateinit var viewModel: InverterViewModel
    
    @Before
    fun setup() {
        viewModel = InverterViewModel()
    }
    
    @Test
    fun `loadInverterStatus should update status`() {
        // Arrange
        val expectedStatus = Inverter("1", "Test", "online", 5000)
        
        // Act
        viewModel.loadInverterStatus()
        
        // Assert
        assertEquals(expectedStatus, viewModel.inverterStatus.value)
    }
}
```

### UI тести

```kotlin
class MainActivityTest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(MainActivity::class.java)
    
    @Test
    fun testNavigationToAboutFragment() {
        // Act
        onView(withId(R.id.nav_about)).perform(click())
        
        // Assert
        onView(withId(R.id.content_frame)).check(matches(isDisplayed()))
    }
}
```

### Запуск тестів

```bash
# Запуск unit тестів
./gradlew test

# Запуск UI тестів
./gradlew connectedAndroidTest

# Запуск всіх тестів
./gradlew check
```

---

## 🔧 Збірка

### Debug збірка
```bash
./gradlew assembleDebug
```

### Release збірка
```bash
./gradlew assembleRelease
```

### Збірка з ProGuard
```bash
./gradlew assembleRelease -PminifyEnabled=true
```

### Підписання APK
```bash
# Створення ключа
keytool -genkey -v -keystore sunportal-release.keystore -alias sunportal -keyalg RSA -keysize 2048 -validity 10000

# Підписання APK
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore sunportal-release.keystore app/build/outputs/apk/release/app-release.apk sunportal
```

---

## 📦 Розгортання

### Firebase App Distribution
```bash
# Налаштування Firebase
firebase appdistribution:distribute app/build/outputs/apk/release/app-release.apk --app <your-app-id>
```

### Google Play Store
1. Підготуйте APK
2. Створіть Google Play Console акаунт
3. Завантажте APK
4. Заповніть інформацію
5. Опублікуйте

### Тестування на пристрої
```bash
# Підключення пристрою
adb devices

# Встановлення APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Перегляд логів
adb logcat
```

---

## 🎨 Дизайн

### Material Design 3

```xml
<!-- themes.xml -->
<style name="Theme.SunPortal" parent="Theme.MaterialComponents.DayNight.NoActionBar">
    <item name="colorPrimary">@color/svitgrid_primary</item>
    <item name="colorOnPrimary">@color/neutral_50</item>
    <item name="colorSecondary">@color/svitgrid_secondary</item>
    <item name="colorOnSecondary">@color/neutral_50</item>
</style>
```

### Кольори
```xml
<!-- colors.xml -->
<color name="svitgrid_primary">#00668A</color>
<color name="svitgrid_primary_dark">#004D6A</color>
<color name="svitgrid_primary_light">#1A8BB8</color>
<color name="svitgrid_secondary">#4A6B7F</color>
<color name="svitgrid_success">#00C851</color>
<color name="svitgrid_warning">#FF9800</color>
<color name="svitgrid_error">#FF4444</color>
```

### Шрифти
```xml
<!-- themes.xml -->
<style name="TextAppearance.AppCompat.Medium">
    <item name="android:textSize">18sp</item>
</style>
```

---

## 🔒 Безпека

### Захищене зберігання
```kotlin
// Використання EncryptedSharedPreferences
val encryptedPrefs = EncryptedSharedPreferences.create(
    "encrypted_prefs",
    "master_key",
    context,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

### Валідація даних
```kotlin
// Валідація вхідних даних
fun validateInput(input: String): Boolean {
    return input.matches(Regex("^[a-zA-Z0-9_\\-\\.]+$"))
}

// Очищення даних
fun sanitizeInput(input: String): String {
    return input.replace("<", "&lt;").replace(">", "&gt;")
}
```

---

## 📚 Документація

### Генерація документації
```bash
# Генерація JavaDoc
./gradlew javadoc

# Генерація Dokka (для Kotlin)
./gradlew dokkaHtml
```

### Автоматична документація
```kotlin
/**
 * Завантажує статус інвертера
 * @return Result з даними або помилкою
 */
suspend fun getInverterStatus(): Result<Inverter> {
    return try {
        val response = apiService.getInverterStatus()
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

---

## 🔄 Версіонування

### Gradle конфігурація
```kotlin
// build.gradle.kts
android {
    defaultConfig {
        versionCode = 7
        versionName = "1.0.7"
    }
}
```

### Git теги
```bash
# Створення тега
git tag -a v1.0.7 -m "Version 1.0.7"

# Відправка тега
git push origin v1.0.7
```

---

## 🤝 Співпраця

### Git workflow
```bash
# Створення гілки
git checkout -b feature/your-feature

# Коміти
git commit -m "feat: add your feature"

# Push
git push origin feature/your-feature

# Pull Request
```

### Code Review
- Використовуйте GitHub Pull Requests
- Дотримуйтесь Code Review процесу
- Відповідайте на коментарі
- Інтегруйте feedback

---

## 📊 Моніторинг

### Логування
```kotlin
// Логування подій
Log.d("API", "API call started")
Log.e("Error", "API call failed", exception)

// Timber для продакшену
Timber.d("API call started")
Timber.e(exception, "API call failed")
```

### Аналітика
```kotlin
// Firebase Analytics
FirebaseAnalytics.getInstance(context).logEvent("inverter_viewed", bundle {
    putString("inverter_id", inverter.id)
    putString("inverter_name", inverter.name)
})
```

---

## 🚀 Випуск

### Підготовка до випуску
1. Перевірте, що всі тести проходять
2. Оновіть номер версії
3. Оновіть CHANGELOG.md
4. Перевірте, що все працює

### Створення релізу
1. Створіть тег
2. Створіть Release на GitHub
3. Завантажте APK
4. Заповніть інформацію

---

## 📞 Контакти

- **GitHub**: [Створити issue](https://github.com/Ponamarenko/SunPortal/issues)
- **Email**: [ponamarenko@example.com](mailto:ponamarenko@example.com)
- **Discord**: [SunPortal Server](http://sunlink.pp.ua:8000)

---

*Останнє оновлення: 2026-11-04*