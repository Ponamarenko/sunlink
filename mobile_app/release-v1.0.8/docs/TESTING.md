# Тестування SunPortal Mobile App

Цей посібник описує процес тестування SunPortal Mobile App.

---

## 🧪 Типи тестів

### Unit тести
- Тестування окремих класів та методів
- Швидкі та ізольовані
- Використовуються JUnit 5

### UI тести
- Тестування користувацького інтерфейсу
- Використовуються Espresso
- Перевірка взаємодії з UI

### Ітеграційні тести
- Тестування взаємодії між компонентами
- Використовуються AndroidX Test
- Тестування роботи з API

### Е2Е тести
- Тестування повних користувацьких сценаріїв
- Використовуються Espresso
- Тестування реальних пристроїв

---

## 🛠️ Налаштування середовища

### Залежності
```gradle
// build.gradle.kts
dependencies {
    // Unit testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.5.1")
    testImplementation("org.mockito:mockito-android:4.5.0")
    
    // UI testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    
    // Integration testing
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
}
```

### Налаштування тестового середовища
```gradle
// build.gradle.kts
android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        
        animationsDisabled = true
        
        execution "ANDROIDX_TEST_ORCHESTRATOR"
    }
}
```

---

## 🧪 Unit тести

### Структура тестів
```
src/test/java/com/sunportal/mobile/
├── MainActivityTest.kt
├── PreferencesManagerTest.kt
└── ui/
    ├── AboutFragmentTest.kt
    └── InverterViewModelTest.kt
```

### Приклади тестів

#### Тестування MainActivity
```kotlin
class MainActivityTest {
    @Test
    fun `MainActivity should initialize correctly`() {
        // Arrange
        val activity = ActivityScenario.launch(MainActivity::class.java)
        
        // Act
        activity.onActivity { mainActivity ->
            // Assert
            assertNotNull(mainActivity.toolbar)
            assertNotNull(mainActivity.drawerLayout)
            assertNotNull(mainActivity.navView)
        }
    }
}
```

#### Тестування PreferencesManager
```kotlin
class PreferencesManagerTest {
    private lateinit var preferencesManager: PreferencesManager
    private lateinit context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        preferencesManager = PreferencesManager(context)
    }
    
    @Test
    fun `saveServerUrl should save url correctly`() {
        // Arrange
        val testUrl = "http://test.com"
        
        // Act
        preferencesManager.saveServerUrl(testUrl)
        
        // Assert
        assertEquals(testUrl, preferencesManager.getServerUrl())
    }
    
    @Test
    fun `getServerUrl should return default url when not set`() {
        // Act
        val defaultUrl = preferencesManager.getServerUrl()
        
        // Assert
        assertEquals("http://sunlink.pp.ua:8000", defaultUrl)
    }
}
```

#### Тестування ViewModel
```kotlin
class InverterViewModelTest {
    private lateinit var viewModel: InverterViewModel
    
    @Before
    fun setup() {
        viewModel = InverterViewModel()
    }
    
    @Test
    fun `loadInverterStatus should update status`() = runTest {
        // Arrange
        val expectedStatus = Inverter("1", "Test", "online", 5000)
        
        // Act
        viewModel.loadInverterStatus()
        
        // Assert
        assertEquals(expectedStatus, viewModel.inverterStatus.value)
    }
    
    @Test
    fun `loadInverterStatus should handle error`() = runTest {
        // Arrange
        viewModelScope.launch {
            whenever(apiService.getInverterStatus()).thenThrow(Exception("Network error"))
        }
        
        // Act
        viewModel.loadInverterStatus()
        
        // Assert
        assertNotNull(viewModel.errorMessage.value)
    }
}
```

---

## 🎨 UI тести

### Структура UI тестів
```
src/androidTest/java/com/sunportal/mobile/
├── MainActivityUITest.kt
├── NavigationTest.kt
└── ui/
    ├── AboutFragmentUITest.kt
    └── ConnectionFragmentUITest.kt
```

### Приклади UI тестів

#### Тестування навігації
```kotlin
class NavigationTest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(MainActivity::class.java)
    
    @Test
    fun testNavigationToAboutFragment() {
        // Act
        onView(withId(R.id.nav_about)).perform(click())
        
        // Assert
        onView(withId(R.id.content_frame)).check(matches(isDisplayed()))
        onView(withId(R.id.about_text)).check(matches(withText("SunPortal v1.0.6")))
    }
    
    @Test
    fun testNavigationToConnectionFragment() {
        // Act
        onView(withId(R.id.nav_connection)).perform(click())
        
        // Assert
        onView(withId(R.id.content_frame)).check(matches(isDisplayed()))
        onView(withId(R.id.server_url)).check(matches(isDisplayed()))
    }
}
```

#### Тестування форми підключення
```kotlin
class ConnectionFragmentUITest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(MainActivity::class.java)
    
    @Test
    fun testConnectionFormValidation() {
        // Act
        onView(withId(R.id.nav_connection)).perform(click())
        onView(withId(R.id.server_url)).perform(typeText("invalid-url"))
        onView(withId(R.id.connect_button)).perform(click())
        
        // Assert
        onView(withId(R.id.error_message)).check(matches(withText("Invalid URL format")))
    }
    
    @Test
    fun testSuccessfulConnection() {
        // Act
        onView(withId(R.id.nav_connection)).perform(click())
        onView(withId(R.id.server_url)).perform(typeText("http://sunlink.pp.ua:8000"))
        onView(withId(R.id.connect_button)).perform(click())
        
        // Assert
        onView(withId(R.id.success_message)).check(matches(withText("Connected successfully")))
    }
}
```

---

## 🔌 Ітеграційні тести

### Тестування API
```kotlin
class ApiIntegrationTest {
    private lateinit var apiService: SunPortalApiService
    
    @Before
    fun setup() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://sunlink.pp.ua:8000")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        apiService = retrofit.create(SunPortalApiService::class.java)
    }
    
    @Test
    fun testGetStatus() = runTest {
        // Act
        val response = apiService.getStatus()
        
        // Assert
        assertEquals("online", response.status)
        assertNotNull(response.version)
        assertTrue(response.uptime >= 0)
    }
    
    @Test
    fun testGetInverters() = runTest {
        // Act
        val response = apiService.getInverters()
        
        // Assert
        assertFalse(response.inverters.isEmpty())
        response.inverters.forEach { inverter ->
            assertNotNull(inverter.id)
            assertNotNull(inverter.name)
            assertNotNull(inverter.status)
        }
    }
}
```

### Тестування WebSocket
```kotlin
class WebSocketIntegrationTest {
    @Test
    fun testWebSocketConnection() = runTest {
        // Arrange
        val webSocket = OkHttpClient().newWebSocket(
            Request.Builder().url("ws://sunlink.pp.ua:8000/ws").build(),
            object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    // Handle message
                }
            }
        )
        
        // Act
        delay(5000) // Wait for connection
        
        // Assert
        assertTrue(webSocket.request.url.toString().contains("ws://sunlink.pp.ua:8000/ws"))
        
        // Cleanup
        webSocket.close(1000, "Test completed")
    }
}
```

---

## 📱 Е2Е тести

### Тестування повного сценарію
```kotlin
class EndToEndTest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(MainActivity::class.java)
    
    @Test
    fun testCompleteUserJourney() {
        // 1. Запуск додатку
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()))
        
        // 2. Навігація до розділу підключення
        onView(withId(R.id.nav_connection)).perform(click())
        onView(withId(R.id.content_frame)).check(matches(isDisplayed()))
        
        // 3. Введення URL сервера
        onView(withId(R.id.server_url)).perform(typeText("http://sunlink.pp.ua:8000"))
        
        // 4. Натискання кнопки підключення
        onView(withId(R.id.connect_button)).perform(click())
        
        // 5. Перевірка успішного підключення
        onView(withId(R.id.success_message)).check(matches(withText("Connected successfully")))
        
        // 6. Навігація до головного екрану
        pressBack()
        onView(withId(R.id.nav_dashboard)).perform(click())
        
        // 7. Перевірка відображення даних
        onView(withId(R.id.inverter_list)).check(matches(isDisplayed()))
    }
}
```

---

## 🔄 Автоматизація тестів

### CI/CD інтеграція
```yaml
# .github/workflows/test.yml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v2
    
    - name: Set up JDK 11
      uses: actions/setup-java@v2
      with:
        java-version: '11'
        
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Run unit tests
      run: ./gradlew test
      
    - name: Run UI tests
      run: ./gradlew connectedAndroidTest
```

### Тестові сценарії
```bash
# Запуск всіх тестів
./gradlew check

# Запуск тільки unit тестів
./gradlew test

# Запуск тільки UI тестів
./gradlew connectedAndroidTest

# Генерація звіту про покриття
./gradlew jacocoTestReport
```

---

## 📊 Моніторинг тестів

### Покриття тестами
```bash
# Генерація звіту про покриття
./gradlew jacocoTestReport

# Перегляд звіту
open app/build/reports/jacoco/test/html/index.html
```

### Метрики тестування
```kotlin
class TestMetrics {
    private val testResults = mutableListOf<TestResult>()
    
    fun recordTest(result: TestResult) {
        testResults.add(result)
    }
    
    fun getSuccessRate(): Double {
        val successful = testResults.count { it.success }
        return successful.toDouble() / testResults.size
    }
    
    fun getAverageExecutionTime(): Long {
        return testResults.map { it.executionTime }.average().toLong()
    }
}
```

---

## 🐛 Виявлення та виправлення помилок

### Логування помилок
```kotlin
// Тестування обробки помилок
@Test
fun testErrorHandling() {
    // Arrange
    whenever(apiService.getStatus()).thenThrow(IOException("Network error"))
    
    // Act
    viewModel.loadServerStatus()
    
    // Assert
    assertNotNull(viewModel.errorMessage.value)
    assertTrue(viewModel.errorMessage.value!!.contains("Network error"))
}
```

### Стрес-тести
```kotlin
class StressTest {
    @Test
    fun testMultipleRapidRequests() = runTest {
        // Arrange
        val requests = List(100) { apiService.getStatus() }
        
        // Act
        val results = requests.map { async { it } }
        
        // Assert
        results.awaitAll().forEach { result ->
            assertTrue(result.isSuccessful)
        }
    }
}
```

---

## 🎯 Найкращі практики

### Структура тестів
- Використовуйте чітку naming convention
- Розділяйте Arrange, Act, Assert
- Використовуйте mock objects для ізоляції
- Тестуйте тільки одну функцію за раз

### Ефективність тестів
- Мінімізуйте час виконання тестів
- Використовуйте паралельне виконання
- Кешуйте дані для тестів
- Використовуйте test fixtures

### Читабельність тестів
- Використовуйте описові імена тестів
- Додавайте коментарі для складних тестів
- Використовуйте data providers
- Створюйте допоміжні методи

---

## 📞 Контакти

- **GitHub**: [Створити issue](https://github.com/Ponamarenko/SunPortal/issues)
- **Email**: [ponamarenko@example.com](mailto:ponamarenko@example.com)
- **Discord**: [SunPortal Server](http://sunlink.pp.ua:8000)

---

*Останнє оновлення: 2026-11-04*