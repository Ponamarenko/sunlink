package com.sunlink.mobile

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SunLinkApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize application here
    }
}