package com.sunlink.mobile.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sunlink.mobile.services.RealTimeUpdateService

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            // Start the real-time update service on boot
            val serviceIntent = Intent(context, RealTimeUpdateService::class.java)
            context.startService(serviceIntent)
        }
    }
}