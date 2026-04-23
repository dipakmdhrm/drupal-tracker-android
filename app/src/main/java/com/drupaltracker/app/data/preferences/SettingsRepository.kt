package com.drupaltracker.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    }

    val apiKeyFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[GEMINI_API_KEY] ?: "" }

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { it[GEMINI_API_KEY] = key }
    }
}
