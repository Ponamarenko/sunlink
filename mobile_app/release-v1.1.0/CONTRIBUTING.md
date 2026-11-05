# Contributing to SunLink Mobile App

🎉 Дякуємо, що хочете зробити внесок у SunLink Mobile App!

## 📋 Зміст

- [Кодекс поведінки](#кодекс-поведінки)
- [Процес внеску](#процес-внеску)
- [Розробка](#розробка)
- [Тестування](#тестування)
- [Створення випуску](#створення-випуску)
- [Ліцензія](#ліцензія)
- [Контакти](#контакти)

---

## 🤝 Кодекс поведінки

Цей проект дотримується [Кодексу поведінки](CODE_OF_CONDUCT.md). Участь у цьому проекті означає, що ви погоджуєтеся дотримуватися його правил.

---

## 🔄 Процес внеску

### 1. Форк та клонування

```bash
# Форк репозиторію на GitHub
git clone https://github.com/your-username/sunlink-mobile.git
cd sunlink-mobile
git remote add upstream https://github.com/Ponamarenko/SunLink.git
```

### 2. Створення гілки

```bash
# Створіть гілку для вашої зміни
git checkout -b feature/your-feature-name
```

### 3. Розробка

```bash
# Зробіть ваші зміни
git add .
git commit -m "feat: add your feature description"
git push origin feature/your-feature-name
```

### 4. Створення Pull Request

1. Перейдіть до вашого форку на GitHub
2. Натисніть "New Pull Request"
3. Виберіть гілку `feature/your-feature-name`
4. Заповніть шаблон PR
5. Надішліть PR

---

## 🛠️ Розробка

### Вимоги

- Android Studio Arctic Fox або новіша
- JDK 11 або новіший
- Android SDK 34
- Gradle 7.4

### Налаштування середовища

```bash
# Клонування репозиторію
git clone https://github.com/Ponamarenko/SunLink.git
cd sunlink-mobile

# Відкриття в Android Studio
./gradlew build
```

### Структура проекту

```
app/
├── src/main/
│   ├── java/com/sunportal/mobile/
│   │   ├── MainActivity.kt
│   │   ├── PreferencesManager.kt
│   │   └── ui/
│   │       ├── AboutFragment.kt
│   │       ├── ConnectionFragment.kt
│   │       └── AppearanceFragment.kt
│   ├── res/
│   │   ├── layout/
│   │   ├── menu/
│   │   ├── values/
│   │   └── drawable/
│   └── AndroidManifest.xml
├── build.gradle.kts
└── proguard-rules.pro
```

### Найкращі практики

#### Kotlin
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
```

#### UI
```xml
<!-- Використовуйте Material Design компоненти -->
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="8dp">
    
    <TextView
        android:id="@+id/inverter_name"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="18sp"
        android:textColor="@color/svitgrid_text_primary"/>
</com.google.android.material.card.MaterialCardView>
```

#### Архітектура
```kotlin
// Використовуйте MVVM патерн
class InverterViewModel : ViewModel() {
    private val _inverterStatus = MutableLiveData<Inverter>()
    val inverterStatus: LiveData<Inverter> = _inverterStatus
    
    fun loadInverterStatus() {
        viewModelScope.launch {
            val result = api.getInverterStatus()
            _inverterStatus.value = result
        }
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

## 📦 Створення випуску

### Підготовка до випуску

1. Перевірте, що всі тести проходять
2. Оновіть номер версії в `build.gradle.kts`
3. Оновіть CHANGELOG.md
4. Перевірте, що все працює на пристрої

```kotlin
// build.gradle.kts
android {
    defaultConfig {
        versionCode = 7
        versionName = "1.0.7"
    }
}
```

### Створення APK

```bash
# Створення debug APK
./gradlew assembleDebug

# Створення release APK
./gradlew assembleRelease

# Створення APK з ProGuard
./gradlew assembleRelease -PminifyEnabled=true
```

### Створення релізу

1. Створіть новий реліз на GitHub
2. Завантажте APK файли
3. Заповніть релізну інформацію
4. Опублікуйте реліз

---

## 📝 Створення Issue

### Шаблон для Bug Report

```markdown
## Опис
Короткий опис проблеми

## Кроки для відтворення
1. Перейдіть до '...'
2. Натисніть на '....'
3. Проблема виникає

## Очікувана поведінка
Що мало б статися

## Фактична поведінка
Що сталося

### Середа
- Версія Android: [наприклад, 12]
- Версія додатку: [наприклад, 1.0.6]
- Модель пристрою: [наприклад, Pixel 6]
```

### Шаблон для Feature Request

```markdown
## Опис
Короткий опис запиту на функцію

## Рішення
Як би ви хотіли, щоб це працювало

## Альтернативи
Що ви розглядали як альтернативу

### Додаткова інформація
Будь-яка додаткова інформація
```

---

## 🎨 Дизайн та UI

### Material Design 3

- Використовуйте Material Design 3 компоненти
- Дотримуйтесь системи кольорів SvitGrid
- Використовуйте адаптивний дизайн
- Підтримуйте темну/світлу тему

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

## 📚 Документація

### Форматування

- Використовуйте Markdown для документів
- Дотримуйтесь структури
- Додавайте приклади коду
- Описуйте складні концепції

### Оновлення документації

- Оновлюйте README.md при змінах
- Додавайте приклади використання
- Описуйте нові функції
- Оновлюйте API документацію

---

## 🚀 Розгортання

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

---

## 🤝 Контакти

- **GitHub Issues**: [Створити issue](https://github.com/Ponamarenko/SunLink/issues)
- **Email**: [ponamarenko@example.com](mailto:ponamarenko@example.com)
- **Discord**: [SunPortal Server](http://sunlink.pp.ua:8000)

---

## 📄 Ліцензія

Цей проект ліцензований під [MIT License](LICENSE). Дивіться файл LICENSE для деталей.

---

🎉 **Дякуємо за ваш внесок у SunLink Mobile App!**

---

*Останнє оновлення: 2026-11-04*