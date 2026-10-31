# SunPortal GitHub Project Analysis Report

## 📋 Project Overview

**SunPortal** — Android приложение для мониторинга солнечной станции с современным интерфейсом в стиле Svitgrid.

### 🔗 GitHub Repository
- **URL**: https://github.com/Ponamarenko/SunPortal
- **Local Path**: `/root/opt/SunPortal`
- **Branch**: `main`
- **Status**: ✅ Active development

### 📱 Application Information
- **Package**: `app.sunportal`
- **Version**: 1.0.16 (versionCode: 17)
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34
- **Compile SDK**: 35
- **Size**: 22.2 MB

## 🛠️ Technical Stack

| Component | Version |
|-----------|---------|
| Kotlin | 1.9.24 |
| Android Gradle Plugin | 8.5.2 |
| Gradle | 8.7 |
| Compose BOM | 2024.06.00 |
| Retrofit | 2.11.0 |
| Gson | 2.11.0 |
| Lottie Compose | 6.4.0 |
| DataStore Preferences | 1.1.1 |

## 🏗️ Project Structure

```
app/src/main/java/app/sunportal/
├── MainActivity.kt                 # Entry point
├── MainScreen.kt                  # Main UI with navigation
├── api/                           # Network layer
│   ├── ApiService.kt              # Retrofit API interface
│   ├── OpenMeteoApi.kt            # Weather API
│   ├── OpenMeteoClient.kt         # Weather client
│   └── RetrofitClient.kt          # Retrofit setup
├── data/                          # Data layer
│   └── SettingsRepository.kt      # Settings persistence
├── model/                         # Data models
│   ├── LiveData.kt               # Live station data
│   ├── WeatherData.kt            # Weather information
│   ├── TouData.kt                # Time-of-use data
│   ├── ForecastData.kt           # Production forecast
│   ├── BlackoutData.kt           # Power outage info
│   └── ...                       # Other models
└── ui/                            # UI components
    ├── live/                      # Real-time monitoring
    ├── overview/                  # Station overview
    ├── battery/                   # Battery management
    ├── forecast/                  # Production forecast
    ├── charts/                    # Data visualization
    ├── tariffs/                   # Energy tariffs
    ├── tou/                       # Time-of-use schedules
    ├── settings/                  # Application settings
    └── about/                     # About screen
```

## 🔄 Current Git Status

```bash
Current branch: main
This branch is up to date with 'origin/main'.

Untracked files:
  build.log

Index is empty, but there are untracked files
(use "git add <file>..." to include what you want to commit in what will be committed)
```

### Recent Commits
```
946393d Resolve merge conflict - use remote version
46d7a2d Update build configuration
c2a0294 Task: cleanup — убрать CJK-комментарии, мусорные файлы (toPx), .kotlin-логи и 124 скриншота из репозитория (+ .gitignore)
```

## 📱 Application Features

### 1. **Real-time Monitoring**
- Live solar production data
- Battery status and SOC
- Grid consumption/generation
- Power flow visualization
- Weather integration

### 2. **TOU (Time-of-Use) Management**
- Time-based energy scheduling
- Preset configurations (Blackout, Forecast-based, Night, Day)
- Local schedule management (server write not supported)

### 3. **Battery Management**
- Real-time battery status
- Reserve calculation
- Total charge/discharge tracking
- Battery alerts

### 4. **Weather Integration**
- Dual source: SunPortal server + Open-Meteo
- Auto-fallback to Open-Meteo
- Weather-based UI animations
- 10-minute automatic updates

### 5. **Notifications**
- Power outage alerts
- Battery charging completion
- Low battery warnings
- Upcoming outage notifications

### 6. **Flow Scheme Visualization**
- Svitgrid-style power flow diagram
- Lottie animations for weather
- Household type selection
- Real-time power indicators

## 🔌 API Endpoints

### SunPortal Server API
- `GET api/live/raw` - Live station data
- `GET tou_read.php` - Time-of-use data
- `GET forecast_read.php` - Production forecast
- `GET weather_read.php` - Weather data
- `GET api/?action=history` - Historical data
- `GET api/?action=blackout` - Power outage info

### Open-Meteo Weather API
- `https://api.open-meteo.com` - Fallback weather source

## 📊 Data Models

### LiveData Structure
```kotlin
data class LiveData(
    val timestamp: Long,
    val p_sun: Float,           // Solar production
    val p_load: Float,          // Load consumption
    val p_batt: Float,          // Battery power
    val batt_soc: Float,       // Battery SOC
    val p_grid: Float,          // Grid power
    val p_string1: Float,       // String 1 power
    val p_string2: Float,       // String 2 power
    val p_string3: Float,       // String 3 power
    val battery_voltage: Float,
    val battery_current: Float,
    val battery_temp: Float,
    val battery_remaining_kwh: Float,
    val pv_today: Float,        // Today's production
    val pv_total: Float,        // Total production
    val pv_peak_today: Float?,
    val p_gen: Float?,          // Generator power
    val p_smart_load: Float?,   // Smart load power
    // ... more fields
)
```

## 🎨 UI/UX Features

### Design System
- Dark theme with gradient cards
- JetBrains Mono font
- Lottie animations
- Material 3 components
- Responsive layout

### Navigation
- Modal drawer navigation
- Collapsible menu groups
- 10 main screens
- Persistent state

### Visual Elements
- Power flow scheme with animated indicators
- Weather-based home animations
- Battery reserve visualization
- Real-time data updates

## 🚀 Build Status

### Local Build
- ✅ Gradle available
- ✅ JDK 21 installed
- ✅ Android SDK configured
- ✅ APK successfully built
- ✅ APK validated (22.2 MB)

### Build Commands
```bash
# Clean build
./gradlew clean assembleDebug

# Debug build
./gradlew assembleDebug

# Check version
/tmp/android-sdk/build-tools/35.0.0/aapt dump badging app/build/outputs/apk/debug/app-debug.apk
```

## 📱 Installation & Testing

### APK Location
- **Path**: `/root/opt/SunPortal/app/build/outputs/apk/debug/app-debug.apk`
- **Size**: 22.2 MB
- **Permissions**: Internet, Notifications, Dynamic Receiver

### Testing Options
1. **Android Emulator** - Currently stopped
2. **Real Device** - ADB connection
3. **Web Interface** - Remote testing available

## 🔧 Current Issues & Limitations

1. **TOU Write Support**: Server endpoint `tou_write.php` returns 404
2. **GitHub Access**: Repository may be private or restricted
3. **Build Dependencies**: Requires full Android SDK for compilation
4. **Testing**: Emulator configuration issues

## 🎯 Next Steps

### 1. **Git Synchronization**
- Follow AGENTS.md guidelines
- Commit current changes
- Push to GitHub repository

### 2. **Enhancement Opportunities**
- Implement TOU write functionality
- Add more data visualization
- Improve offline mode
- Add export/import features

### 3. **Testing & Quality Assurance**
- Set up Android emulator
- Test on real devices
- Performance optimization
- UI/UX improvements

## 📞 Contact & Resources

- **Documentation**: `docs/PC-DEPENDENCIES.md`, `docs/PHONE-BUILD.md`
- **Build Scripts**: `build_now.bat` (Windows), `gradlew` (Cross-platform)
- **GitHub CLI**: Release management
- **Issue Tracking**: GitHub Issues

---

**Report Generated**: October 28, 2026  
**Project Status**: ✅ Ready for development  
**Last Updated**: Current session