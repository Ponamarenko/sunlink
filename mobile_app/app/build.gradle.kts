plugins {
    id("com.android.application")
}

android {
    namespace = "com.sunportal.mobile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sunportal.mobile"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}