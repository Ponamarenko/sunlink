# API документація SunLink Mobile App

## 📡 API інтеграція

SunLink Mobile App інтегрується з SunPortal сервером через REST API та WebSocket для отримання даних в реальному часі.

---

## 🔌 Підключення до сервера

### Конфігурація сервера
```kotlin
// Адреса сервера
val SERVER_URL = "http://sunlink.pp.ua:8000"

// WebSocket URL
val WEBSOCKET_URL = "ws://sunlink.pp.ua:8000/ws"

// Таймаут підключення
val CONNECTION_TIMEOUT = 10000 // 10 секунд
```

### Налаштування підключення
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

---

## 🌐 REST API

### 1. Отримання статусу сервера
**GET** `/api/v1/status`

**Response:**
```json
{
  "status": "online",
  "version": "1.0.0",
  "uptime": 86400,
  "inverters_count": 3
}
```

### 2. Отримання списку інвертерів
**GET** `/api/v1/inverters`

**Response:**
```json
{
  "inverters": [
    {
      "id": "deye-001",
      "name": "Deye SUN-12K-SG02LP1-EU-AM3",
      "status": "online",
      "power": 8500,
      "voltage": 230,
      "current": 37,
      "temperature": 45
    },
    {
      "id": "deye-002", 
      "name": "Deye SG02LP1",
      "status": "offline",
      "power": 0,
      "voltage": 0,
      "current": 0,
      "temperature": 0
    }
  ]
}
```

### 3. Отримання телеметрії
**GET** `/api/v1/telemetry/{inverter_id}`

**Response:**
```json
{
  "inverter_id": "deye-001",
  "timestamp": 1667232000,
  "power": 8500,
  "voltage": 230,
  "current": 37,
  "frequency": 50,
  "temperature": 45,
  "battery_soc": 85,
  "battery_voltage": 48.2,
  "solar_power": 9200,
  "grid_power": 1200,
  "load_power": 7300
}
```

### 4. Контроль інвертера
**POST** `/api/v1/control/{inverter_id}`

**Request:**
```json
{
  "command": "emergency_stop",
  "parameters": {}
}
```

**Response:**
```json
{
  "success": true,
  "message": "Emergency stop initiated",
  "timestamp": 1667232000
}
```

---

## 🔄 WebSocket API

### Підключення
```javascript
// WebSocket connection
const ws = new WebSocket('ws://sunlink.pp.ua:8000/ws');

ws.onopen = function(event) {
  console.log('Connected to SunPortal WebSocket');
  // Send subscription message
  ws.send(JSON.stringify({
    type: 'subscribe',
    channels: ['telemetry', 'status']
  }));
};

ws.onmessage = function(event) {
  const data = JSON.parse(event.data);
  handleWebSocketMessage(data);
};

ws.onclose = function(event) {
  console.log('WebSocket connection closed');
};

ws.onerror = function(error) {
  console.error('WebSocket error:', error);
};
```

### Формати повідомлень

#### 1. Статус інвертера
```json
{
  "type": "status",
  "inverter_id": "deye-001",
  "status": "online",
  "timestamp": 1667232000
}
```

#### 2. Телеметрія в реальному часі
```json
{
  "type": "telemetry",
  "inverter_id": "deye-001",
  "data": {
    "power": 8500,
    "voltage": 230,
    "current": 37,
    "temperature": 45,
    "battery_soc": 85
  },
  "timestamp": 1667232000
}
```

#### 3. Серверні події
```json
{
  "type": "server_event",
  "event": "inverter_connected",
  "inverter_id": "deye-003",
  "timestamp": 1667232000
}
```

---

## 📊 Обробка даних

### Класи даних (Kotlin)
```kotlin
// Статус сервера
data class ServerStatus(
    val status: String,
    val version: String,
    val uptime: Long,
    val invertersCount: Int
)

// Інвертер
data class Inverter(
    val id: String,
    val name: String,
    val status: String,
    val power: Int,
    val voltage: Double,
    val current: Double,
    val temperature: Double
)

// Телеметрія
data class Telemetry(
    val inverterId: String,
    val timestamp: Long,
    val power: Int,
    val voltage: Double,
    val current: Double,
    val frequency: Double,
    val temperature: Double,
    val batterySoc: Double,
    val batteryVoltage: Double,
    val solarPower: Int,
    val gridPower: Int,
    val loadPower: Int
)

// Команда контролю
data class ControlCommand(
    val command: String,
    val parameters: Map<String, Any>
)
```

### Обробка відповідей
```kotlin
// Обробка успішної відповіді
fun handleSuccessResponse(data: String) {
    try {
        val json = JSONObject(data)
        when (json.getString("type")) {
            "status" -> parseStatus(json)
            "telemetry" -> parseTelemetry(json)
            "control" -> parseControlResponse(json)
        }
    } catch (e: JSONException) {
        Log.e("API", "JSON parsing error", e)
    }
}

// Обробка помилок
fun handleErrorResponse(error: Throwable) {
    when (error) {
        is IOException -> Log.e("API", "Network error")
        is JSONException -> Log.e("API", "JSON parsing error")
        else -> Log.e("API", "Unknown error", error)
    }
}
```

---

## 🔐 Обробка помилок

### Коди помилок
```kotlin
enum class ApiError {
    NETWORK_ERROR,
    PARSE_ERROR,
    SERVER_ERROR,
    AUTH_ERROR,
    TIMEOUT,
    UNKNOWN
}
```

### Стратегія повторних спроб
```kotlin
fun executeWithRetry(
    operation: () -> Result<T>,
    maxRetries: Int = 3,
    delay: Long = 1000
): Result<T> {
    var lastError: Throwable? = null
    
    repeat(maxRetries) { attempt ->
        try {
            return operation()
        } catch (e: Throwable) {
            lastError = e
            if (attempt < maxRetries - 1) {
                Thread.sleep(delay)
            }
        }
    }
    
    return Result.failure(lastError ?: Exception("Unknown error"))
}
```

---

## 🛡️ Безпека

### HTTPS
```kotlin
// Використання HTTPS для безпечного з'єднання
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Content-Type", "application/json")
            .build()
        chain.proceed(request)
    }
    .build()
```

### Валідація даних
```kotlin
// Валідація телеметрії
fun validateTelemetry(telemetry: Telemetry): Boolean {
    return telemetry.power >= 0 && 
           telemetry.voltage >= 0 && 
           telemetry.current >= 0 &&
           telemetry.temperature >= -50 && telemetry.temperature <= 100
}
```

---

## 📈 Моніторинг

### Логування
```kotlin
// Логування API викликів
fun logApiCall(endpoint: String, method: String, duration: Long) {
    Log.d("API", "$method $endpoint - ${duration}ms")
}

// Логування помилок
fun logApiError(error: Throwable, endpoint: String) {
    Log.e("API", "Error in $endpoint: ${error.message}")
}
```

### Метрики
```kotlin
// Відстеження продуктивності
class ApiMetrics {
    private var requestCount = 0
    private var totalResponseTime = 0L
    private var errorCount = 0
    
    fun recordRequest(responseTime: Long, success: Boolean) {
        requestCount++
        totalResponseTime += responseTime
        if (!success) errorCount++
    }
    
    fun getAverageResponseTime(): Double {
        return if (requestCount > 0) totalResponseTime.toDouble() / requestCount else 0.0
    }
}
```

---

## 🔧 Конфігурація

### API клієнт
```kotlin
class SunPortalApi {
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://sunlink.pp.ua:8000")
        .addConverterFactory(GsonConverterFactory.create())
        .client(OkHttpClient.Builder().build())
        .build()
    
    private val apiService = retrofit.create(SunPortalApiService::class.java)
    
    suspend fun getStatus(): Result<ServerStatus> {
        return try {
            val response = apiService.getStatus()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Налаштування
```kotlin
// SharedPreferences для конфігурації
class ApiPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("sunlink_prefs", Context.MODE_PRIVATE)
    
    fun getServerUrl(): String = prefs.getString("server_url", "http://sunlink.pp.ua:8000")!!
    fun setServerUrl(url: String) = prefs.edit().putString("server_url", url).apply()
    
    fun getTimeout(): Int = prefs.getInt("timeout", 10000)
    fun setTimeout(timeout: Int) = prefs.edit().putInt("timeout", timeout).apply()
}
```

---

## 📝 Приклади використання

### Отримання статусу сервера
```kotlin
viewModelScope.launch {
    when (val result = api.getStatus()) {
        is Result.Success -> {
            serverStatus.value = result.data
        }
        is Result.Failure -> {
            showError("Failed to get server status")
        }
    }
}
```

### Підписка на WebSocket
```kotlin
fun setupWebSocket() {
    val webSocket = OkHttpClient().newWebSocket(
        Request.Builder().url("ws://sunlink.pp.ua:8000/ws").build(),
        object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val data = Gson().fromJson(text, WebSocketMessage::class.java)
                handleWebSocketMessage(data)
            }
        }
    )
}
```

### Відправка команди
```kotlin
fun sendEmergencyStop(inverterId: String) {
    viewModelScope.launch {
        val command = ControlCommand("emergency_stop", emptyMap())
        when (val result = api.sendCommand(inverterId, command)) {
            is Result.Success -> {
                showSuccess("Emergency stop sent")
            }
            is Result.Failure -> {
                showError("Failed to send command")
            }
        }
    }
}
```

---

## 🔄 Кешування

### Стратегія кешування
```kotlin
class TelemetryCache {
    private val cache = mutableMapOf<String, Telemetry>()
    private val cacheTimeout = 30000 // 30 секунд
    
    fun get(inverterId: String): Telemetry? {
        val cached = cache[inverterId]
        return if (cached != null && System.currentTimeMillis() - cached.timestamp < cacheTimeout) {
            cached
        } else {
            cache.remove(inverterId)
            null
        }
    }
    
    fun put(telemetry: Telemetry) {
        cache[telemetry.inverterId] = telemetry
    }
}
```

---

### Контакти

- **GitHub**: [Сторінка проекту](https://github.com/Ponamarenko/SunLink)
- **Сервер**: [SunPortal](http://sunlink.pp.ua:8000)

---

*Останнє оновлення: 2026-11-04*