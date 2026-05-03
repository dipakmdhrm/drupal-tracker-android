package com.drupaltracker.app.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object PollingScheduler {

    private const val WORK_NAME = "drupal_polling"

    /**
     * Enqueue polling work.
     * @param update true when the user changed settings — resets the timer immediately.
     *               false (default) on app start — keeps existing schedule so the timer
     *               isn't reset every time the user opens the app.
     */
    fun schedule(context: Context, intervalMinutes: Long, update: Boolean = false) {
        val request = PeriodicWorkRequestBuilder<PollingWorker>(intervalMinutes, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            if (update) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
