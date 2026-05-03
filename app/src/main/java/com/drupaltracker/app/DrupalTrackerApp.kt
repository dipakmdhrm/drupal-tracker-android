package com.drupaltracker.app

import android.app.Application
import com.drupaltracker.app.data.api.RetrofitClient

class DrupalTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(cacheDir)
    }
}
