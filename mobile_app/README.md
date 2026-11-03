# 📱 SunLink Mobile Application

Cross-platform mobile application for monitoring and managing Deye inverters using Flutter framework.

## 🚀 Features

### Core Functionality
- 📊 **Real-time Monitoring** - Live telemetry data from inverters
- 📱 **Cross-platform** - iOS and Android support
- 🔐 **Secure Authentication** - JWT-based login system
- 📡 **WebSocket Integration** - Real-time data streaming
- 📊 **Data Visualization** - Charts and graphs for performance
- 🔔 **Push Notifications** - Alerts and important events
- 🌙 **Offline Mode** - Cached data for offline access

### User Interface
- 🎨 **Modern Design** - Clean, intuitive interface
- 🌓 **Dark/Light Theme** - Theme switching support
- 🌐 **Multi-language** - Internationalization support
- 📱 **Responsive Layout** - Adaptive to different screen sizes
- 🎯 **Easy Navigation** - Intuitive menu structure

## 📋 Requirements

### Development
- **Flutter**: 3.13.0 or higher
- **Dart**: 3.1.0 or higher
- **Android Studio** or **Visual Studio Code**
- **Xcode** (for iOS development)
- **Java JDK** 17 or higher

### Production
- **Android**: API level 21 or higher
- **iOS**: 13.0 or higher
- **Internet Connection** - For real-time data

## 🛠️ Development Setup

### 1. Install Flutter
```bash
# Download Flutter SDK
git clone https://github.com/flutter/flutter.git -b stable
export PATH="$PATH:`pwd`/flutter/bin"

# Add to PATH (permanent)
echo 'export PATH="$PATH:/path/to/flutter/bin"' >> ~/.bashrc
source ~/.bashrc
```

### 2. Install Dependencies
```bash
# Android Studio
# Download from: https://developer.android.com/studio

# Xcode (macOS only)
# Install from App Store

# Java JDK
sudo apt install openjdk-17-jdk
```

### 3. Setup Flutter
```bash
# Flutter doctor
flutter doctor

# Install required packages
flutter pub global activate flutter_launcher_icons
flutter pub global activate flutter_native_splash
```

### 4. Clone Repository
```bash
git clone https://github.com/Ponamarenko/sunlink.git
cd sunlink/mobile_app
```

### 5. Install Dependencies
```bash
# Install Flutter packages
flutter pub get

# Generate icons
flutter pub run flutter_launcher_icons:main

# Generate splash screen
flutter pub run flutter_native_splash:create
```

## 📁 Project Structure

```
mobile_app/
├── lib/
│   ├── main.dart                 # App entry point
│   ├── app.dart                  # App configuration
│   ├── core/
│   │   ├── constants.dart        # App constants
│   │   ├── theme.dart           # Theme configuration
│   │   ├── utils/               # Utility functions
│   │   └── services/            # Core services
│   ├── models/
│   │   ├── inverter.dart        # Inverter data model
│   │   ├── telemetry.dart       # Telemetry data model
│   │   └── user.dart           # User data model
│   ├── screens/
│   │   ├── home/               # Home screen
│   │   ├── login/              # Login screen
│   │   ├── inverters/          # Inverter management
│   │   ├── telemetry/          # Telemetry monitoring
│   │   ├── settings/           # Settings screen
│   │   └── profile/            # User profile
│   ├── widgets/
│   │   ├── charts/             # Chart widgets
│   │   ├── cards/              # Card widgets
│   │   ├── forms/              # Form widgets
│   │   └── common/            # Common widgets
│   ├── services/
│   │   ├── api_service.dart    # API communication
│   │   ├── websocket_service.dart # WebSocket handling
│   │   ├── storage_service.dart # Local storage
│   │   └── notification_service.dart # Push notifications
│   ├── providers/
│   │   ├── inverter_provider.dart # Inverter state management
│   │   ├── user_provider.dart   # User state management
│   │   └── theme_provider.dart  # Theme state management
│   └── l10n/
│       ├── app_en.arb          # English localization
│       ├── app_uk.arb          # Ukrainian localization
│       └── app_ru.arb          # Russian localization
├── assets/
│   ├── images/                 # App images
│   ├── fonts/                  # Custom fonts
│   └── l10n/                   # Localization assets
├── android/                    # Android specific code
├── ios/                        # iOS specific code
├── web/                        # Web specific code
├── test/                       # Test files
├── .env                        # Environment variables
├── .gitignore                  # Git ignore file
├── pubspec.yaml               # Flutter dependencies
└── README.md                  # This file
```

## 🎯 Development Roadmap

### Phase 1: Core Features (Q1 2026)
- [x] Project setup and structure
- [ ] Basic authentication
- [ ] Real-time telemetry display
- [ ] Inverter list and details
- [ ] Basic charts and graphs

### Phase 2: Advanced Features (Q2 2026)
- [ ] Offline mode support
- [ ] Push notifications
- [ ] Data export functionality
- [ ] Multi-language support
- [ ] Dark/light theme

### Phase 3: Enhanced Features (Q3 2026)
- [ ] Advanced analytics
- [ ] Custom dashboard
- [ ] Social sharing
- [ ] Widget support
- [ ] Wear OS integration

### Phase 4: Production (Q4 2026)
- [ ] App Store deployment
- [ ] Google Play deployment
- [ ] Performance optimization
- [ ] Beta testing
- [ ] User feedback integration

## 🔧 Configuration

### Environment Variables
Create `.env` file in the root directory:

```env
# API Configuration
API_BASE_URL=https://api.sunlink.pp.ua
WS_URL=wss://api.sunlink.pp.ua/api/v1/live

# App Configuration
APP_NAME=SunLink
APP_VERSION=1.0.0
DEBUG_MODE=false

# Push Notifications
PUSH_NOTIFICATIONS_ENABLED=true
FCM_SERVER_KEY=your_fcm_server_key

# Analytics
ANALYTICS_ENABLED=true
ANALYTICS_KEY=your_analytics_key
```

### Dependencies
Update `pubspec.yaml`:

```yaml
name: sunlink_mobile
description: SunLink Mobile Application
version: 1.0.0+1

environment:
  sdk: '>=3.1.0 <4.0.0'
  flutter: ">=3.13.0"

dependencies:
  flutter:
    sdk: flutter
  cupertino_icons: ^1.0.2
  http: ^1.1.0
  web_socket_channel: ^2.4.0
  provider: ^6.0.5
  flutter_bloc: ^8.1.3
  equatable: ^2.0.5
  shared_preferences: ^2.2.0
  intl: ^0.18.1
  flutter_localizations:
    sdk: flutter
  charts_flutter: ^0.12.0
  flutter_secure_storage: ^8.0.0
  flutter_local_notifications: ^15.1.0
  connectivity_plus: ^3.0.6
  flutter_svg: ^2.0.7
  cached_network_image: ^3.2.3
  flutter_screenutil: ^5.8.4
  get_it: ^7.6.4
  dio: ^5.3.2
  logger: ^2.0.2+1
  json_annotation: ^4.8.1

dev_dependencies:
  flutter_test:
    sdk: flutter
  flutter_launcher_icons: ^0.13.1
  flutter_native_splash: ^2.3.2
  build_runner: ^2.4.6
  json_serializable: ^6.7.1

flutter:
  uses-material-design: true
  
  assets:
    - assets/images/
    - assets/fonts/
    - assets/l10n/
    
  fonts:
    - family: Roboto
      fonts:
        - asset: assets/fonts/Roboto-Regular.ttf
        - asset: assets/fonts/Roboto-Bold.ttf
          weight: 700
```

## 🚀 Building and Running

### Development
```bash
# Run on all connected devices
flutter run

# Run on specific device
flutter run -d <device_id>

# Run with debug mode
flutter run --debug

# Run with profile mode
flutter run --profile

# Run with release mode
flutter run --release
```

### Building for Production
```bash
# Android APK
flutter build apk --release

# Android App Bundle
flutter build appbundle --release

# iOS IPA
flutter build ios --release

# Web
flutter build web --release
```

### Testing
```bash
# Run all tests
flutter test

# Run specific test file
flutter test test/unit_test.dart

# Run widget tests
flutter test test/widget_test.dart

# Run integration tests
flutter drive --target=test_driver/app.dart
```

## 📱 Screens

### Home Screen
- Dashboard with key metrics
- Quick access to inverters
- System status overview
- Recent alerts

### Login Screen
- Email/password authentication
- Biometric login support
- Remember me functionality
- Forgot password option

### Inverter Management
- List of connected inverters
- Inverter details and status
- Configuration settings
- Control commands

### Telemetry Monitoring
- Real-time data charts
- Historical data visualization
- Performance analytics
- Export functionality

### Settings Screen
- User preferences
- App configuration
- Notification settings
- About section

## 🔐 Security Features

- JWT token authentication
- Secure local storage
- SSL/TLS encryption
- Biometric authentication
- Data encryption at rest

## 📊 Performance Optimization

- Lazy loading
- Image caching
- Code splitting
- State management
- Memory management

## 🌍 Internationalization

Supported languages:
- 🇺🇸 English
- 🇺🇦 Ukrainian
- 🇷🇺 Russian

Adding new languages:
1. Create new `.arb` file in `lib/l10n/`
2. Add language code to `pubspec.yaml`
3. Update app initialization
4. Test with device locale

## 🚀 Deployment

### App Store
1. Create Apple Developer account
2. Configure Xcode project
3. Build IPA file
4. Upload to App Store Connect
5. Submit for review

### Google Play
1. Create Google Play Developer account
2. Configure Android project
3. Build App Bundle
4. Upload to Google Play Console
5. Submit for review

### Distribution
- **Internal Testing**: Beta testing with limited users
- **Open Testing**: Public beta testing
- **Production**: Public release

## 📈 Analytics and Monitoring

- Firebase Analytics
- Crashlytics for error tracking
- Performance monitoring
- User behavior tracking
- App store analytics

## 🤝 Contributing

1. Fork the repository
2. Create feature branch
3. Make changes
4. Add tests
5. Submit pull request
6. Review and merge

## 📞 Support

- **GitHub Issues**: [sunlink-mobile/issues](https://github.com/Ponamarenko/sunlink-mobile/issues)
- **Email**: mobile-support@sunlink.pp.ua
- **Documentation**: [Mobile App Docs](docs/mobile.md)
- **Community**: [Discord Channel](https://discord.gg/sunlink)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](../LICENSE) file for details.

---

**SunLink Mobile Application** - Cross-platform solar monitoring made simple 📱🌞