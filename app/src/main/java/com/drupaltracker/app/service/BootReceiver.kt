package com.drupaltracker.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_BOOTSTRAP = "com.drupaltracker.app.ACTION_BOOTSTRAP"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, ACTION_BOOTSTRAP -> {
                context.startForegroundService(
                    Intent(context, PollingForegroundService::class.java)
                )
            }
        }
    }
}
