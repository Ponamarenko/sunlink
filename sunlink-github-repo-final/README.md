# SunLink - Solar Energy Monitoring App

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-34%2B-blue.svg)](https://developer.android.com/about/versions)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-Beta-purple.svg)](https://developer.android.com/jetpack/compose)

**SunLink** is a modern mobile application for monitoring solar energy systems built with Jetpack Compose, Material Design 3, and Hilt for dependency injection.

## 🌟 Features

### Core Functionality
- **Real-time Monitoring** - Live data updates every 5 seconds
- **Multiple Connection Types** - RS485, ESP32, Solarman WiFi, Ethernet
- **Comprehensive Analytics** - Charts and graphs for energy production
- **Battery Management** - Detailed battery monitoring and control
- **Weather Forecast** - Weather predictions and energy optimization
- **Modern UI** - Material Design 3 with dark/light themes

### Technical Features
- **MVVM Architecture** - Clean separation of concerns
- **Jetpack Compose** - Modern reactive UI framework
- **Hilt DI** - Dependency injection for testability
- **Retrofit** - Type-safe networking
- **Room** - Local database persistence
- **Coroutines** - Asynchronous programming
- **WorkManager** - Background task scheduling

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog | 2023.1.1 or later
- Android SDK 34+
- JDK 17+
- Gradle 8.5

### Installation

1. Clone the repository:
```bash
git clone https://github.com/Ponamarenko/sunlink.git
cd sunlink
```

2. Open the project in Android Studio:
```bash
./gradlew build
```

3. Run the application:
```bash
./gradlew installDebug
```

## 📱 Architecture

### Project Structure
```
app/
├── src/main/java/com/sunlink/mobile/
│   ├── MainActivity.kt                 # Main entry point
│   ├── SunLinkApplication.kt          # Application class
│   ├── ui/
│   │   ├── screens/                   # Compose screens
│   │   │   ├── MainScreen.kt         # Main navigation
│   │   │   ├── DashboardScreen.kt    # Dashboard
│   │   │   ├── LiveScreen.kt         # Real-time monitoring
│   │   │   ├── ChartsScreen.kt       # Analytics
│   │   │   ├── BatteryScreen.kt      # Battery management
│   │   │   └── ForecastScreen.kt     # Weather & forecast
│   │   └── theme/                     # Material Design 3 theme
│   ├── data/
│   │   ├── model/                     # Data models
│   │   │   ├── InverterData.kt       # Inverter data
│   │   │   └── BatteryData.kt        # Battery data
│   │   ├── repository/                # Repository pattern
│   │   └── database/                  # Room database
│   ├── network/                       # Networking
│   │   ├── ApiService.kt              # API interface
│   │   └── RetrofitClient.kt         # Retrofit setup
│   ├── services/                     # Background services
│   ├── viewmodel/                    # ViewModels
│   └── di/                           # Dependency injection
└── src/main/res/                     # Resources
```

### Architecture Pattern
```
UI Layer (Compose)
├── Screens
├── Components
└── Theme

Domain Layer
├── ViewModels
└── Use Cases

Data Layer
├── Repository
├── Network
├── Database
└── Data Sources
```

## 🔧 Configuration

### Build Configuration
```kotlin
// app/build.gradle.kts
android {
    compileSdk = 34
    namespace = "com.sunlink.mobile"
    defaultConfig {
        applicationId = "com.sunlink.mobile"
        minSdk = 24
        targetSdk = 34
        versionCode = 12000
        versionName = "1.2.0"
    }
}
```

### Dependencies
```kotlin
dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    
    // DI
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    
    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // Database
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    kapt("androidx.room:room-compiler:2.6.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

## 🌐 API Integration

### Supported Connection Types
```kotlin
// RS485 Connection
val rs485Service = RetrofitClient.getApiServiceForConnectionType("rs485")

// ESP32 WiFi Connection
val esp32Service = RetrofitClient.getApiServiceForConnectionType("esp32")

// Solarman WiFi Connection
val solarmanService = RetrofitClient.getApiServiceForConnectionType("solarman")

// Ethernet Connection
val ethernetService = RetrofitClient.getApiServiceForConnectionType("ethernet")
```

### API Endpoints
```kotlin
// Get inverter data
GET /api/inverter/data

// Get battery data
GET /api/battery/data

// Control inverter
POST /api/inverter/control

// Control battery
POST /api/battery/control

// Get forecast data
GET /api/forecast/data

// Get weather data
GET /api/weather/data
```

## 🎨 UI/UX Design

### Material Design 3 Implementation
```kotlin
@Composable
fun SunLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
```

### Navigation
```kotlin
NavHost(
    navController = navController,
    startDestination = "dashboard"
) {
    composable("dashboard") { DashboardScreen() }
    composable("live") { LiveScreen() }
    composable("charts") { ChartsScreen() }
    composable("battery") { BatteryScreen() }
    composable("forecast") { ForecastScreen() }
}
```

## 📊 Data Models

### InverterData
```kotlin
data class InverterData(
    val currentPower: Double,
    val voltage: Double,
    val current: Double,
    val temperature: Double,
    val status: String,
    val efficiency: Double,
    val frequency: Double,
    val powerFactor: Double,
    val totalEnergy: Double,
    val dailyEnergy: Double,
    val timestamp: Long
)
```

### BatteryData
```kotlin
data class BatteryData(
    val status: String,
    val level: Int,
    val temperature: Double,
    val voltage: Double,
    val current: Double,
    val health: Int,
    val cycles: Int,
    val power: Double,
    val energy: Double,
    val charging: Boolean,
    val timestamp: Long
)
```

## 🔋 Battery Management

### Battery Status
- **High** (>80%) - Green indicator
- **Medium** (50-80%) - Orange indicator
- **Low** (<50%) - Red indicator

### Battery Control
```kotlin
// Charge battery
repository.controlBattery("charge", targetLevel = 80)

// Discharge battery
repository.controlBattery("discharge", maxPower = 1000.0)

// Optimize battery
repository.controlBattery("optimize")
```

## 📈 Analytics & Charts

### Power Charts
- Real-time power monitoring
- Historical data visualization
- Multiple time periods (daily, weekly, monthly)

### Efficiency Analysis
- System efficiency tracking
- Performance optimization
- Energy production forecasting

## 🌤️ Weather Integration

### Weather Data
```kotlin
data class WeatherData(
    val temperature: Double,
    val humidity: Double,
    val windSpeed: Double,
    val condition: String,
    val icon: String
)
```

### Weather Forecast
- 7-day weather predictions
- Energy production optimization
- System recommendations

## 🔧 Settings & Configuration

### Connection Settings
```kotlin
data class ConnectionSettings(
    val type: String, // rs485, esp32, solarman, ethernet
    val ipAddress: String,
    val port: Int,
    val username: String,
    val password: String
)
```

### Application Settings
```kotlin
data class AppSettings(
    val theme: String, // light, dark, system
    val language: String, // en, uk, ru
    val updateInterval: Int, // seconds
    val notifications: Boolean,
    val autoOptimization: Boolean
)
```

## 🚀 Building & Deployment

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Generate APK
```bash
./gradlew build
```

### Generate AAB
```bash
./gradlew bundleRelease
```

## 🧪 Testing

### Unit Testing
```bash
./gradlew test
```

### Instrumentation Testing
```bash
./gradlew connectedCheck
```

### Test Coverage
```bash
./gradlew jacocoTestReport
```

## 📱 Device Requirements

### Minimum Requirements
- Android 7.0 (API 24) or higher
- 2GB RAM minimum
- 100MB storage space
- Internet connection

### Recommended Requirements
- Android 12 (API 32) or higher
- 4GB RAM
- GPS for location services
- Always-on display support

## 🔒 Security

### Data Security
- SSL/TLS encryption for all communications
- Secure storage of credentials
- Data validation and sanitization

### Privacy
- No collection of personal data
- Local data processing
- User control over data sharing

## 🤝 Contributing

### Development Setup
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable names
- Add proper documentation
- Write comprehensive tests

### Git Workflow
```bash
git checkout -b feature/new-feature
git commit -m "Add new feature"
git push origin feature/new-feature
git pull-request
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Material Design 3](https://m3.material.io/) for the UI framework
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for the reactive UI
- [Hilt](https://dagger.dev/hilt/) for dependency injection
- [Retrofit](https://square.github.io/retrofit/) for networking
- [Room](https://developer.android.com/training/data-storage/room) for database

## 📞 Support

For support, please open an issue on GitHub or contact:
- Email: support@sunlink.pp.ua
- GitHub Issues: [Issues](https://github.com/Ponamarenko/sunlink/issues)

## 🗺️ Roadmap

### Version 1.3.0 (Planned)
- [ ] Wear OS support
- [ ] Widget functionality
- [ ] Advanced analytics
- [ ] Multi-language support

### Version 1.4.0 (Planned)
- [ ] Cloud synchronization
- [ ] Voice commands
- [ ] AR solar panel detection
- [ ] AI optimization

### Version 2.0.0 (Future)
- [ ] Cross-platform support
- [ ] Smart home integration
- [ ] Energy trading features
- [ ] Blockchain verification

---

**SunLink** - Empowering solar energy monitoring with modern technology 🌞⚡