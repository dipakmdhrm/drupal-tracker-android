package com.drupaltracker.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class NotificationSettings(
    val enabled: Boolean = true,
    val pollIntervalMinutes: Long = 60L,
    val notifyStarredProjects: Boolean = true,
    val projectNotifType: String = "EVERY_UPDATE",  // "EVERY_UPDATE" | "DIGEST"
    val digestInterval: String = "HOURLY",           // "HOURLY" | "DAILY" | "WEEKLY"
    val notifyStarredIssues: Boolean = true,
    val lastProjectDigestSentAt: Long = 0L,
    val lastIssueDigestSentAt: Long = 0L
)

class SettingsRepository(private val context: Context) {

    companion object {
        private val GEMINI_API_KEY           = stringPreferencesKey("gemini_api_key")
        private val NOTIF_ENABLED            = booleanPreferencesKey("notif_enabled")
        private val POLL_INTERVAL_MINUTES    = longPreferencesKey("poll_interval_minutes")
        private val NOTIF_STARRED_PROJECTS   = booleanPreferencesKey("notif_starred_projects")
        private val NOTIF_PROJECT_TYPE       = stringPreferencesKey("notif_project_type")
        private val NOTIF_DIGEST_INTERVAL    = stringPreferencesKey("notif_digest_interval")
        private val NOTIF_STARRED_ISSUES     = booleanPreferencesKey("notif_starred_issues")
        private val LAST_PROJECT_DIGEST_SENT = longPreferencesKey("last_project_digest_sent_at")
        private val LAST_ISSUE_DIGEST_SENT   = longPreferencesKey("last_issue_digest_sent_at")
        // Pipe-separated pending digest items — persisted so they survive between Worker runs
        private val PENDING_PROJECT_DIGEST   = stringPreferencesKey("pending_project_digest")
        private val PENDING_ISSUE_DIGEST     = stringPreferencesKey("pending_issue_digest")
    }

    val apiKeyFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[GEMINI_API_KEY] ?: "" }

    val notificationSettingsFlow: Flow<NotificationSettings> = context.dataStore.data.map { prefs ->
        NotificationSettings(
            enabled               = prefs[NOTIF_ENABLED] ?: true,
            pollIntervalMinutes   = prefs[POLL_INTERVAL_MINUTES] ?: 60L,
            notifyStarredProjects = prefs[NOTIF_STARRED_PROJECTS] ?: true,
            projectNotifType      = prefs[NOTIF_PROJECT_TYPE] ?: "EVERY_UPDATE",
            digestInterval        = prefs[NOTIF_DIGEST_INTERVAL] ?: "HOURLY",
            notifyStarredIssues   = prefs[NOTIF_STARRED_ISSUES] ?: true,
            lastProjectDigestSentAt = prefs[LAST_PROJECT_DIGEST_SENT] ?: 0L,
            lastIssueDigestSentAt   = prefs[LAST_ISSUE_DIGEST_SENT] ?: 0L
        )
    }

    val pendingProjectDigestFlow: Flow<List<String>> = context.dataStore.data
        .map { prefs ->
            prefs[PENDING_PROJECT_DIGEST]?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
        }

    val pendingIssueDigestFlow: Flow<List<String>> = context.dataStore.data
        .map { prefs ->
            prefs[PENDING_ISSUE_DIGEST]?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
        }

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { it[GEMINI_API_KEY] = key }
    }

    suspend fun saveNotificationSettings(s: NotificationSettings) {
        context.dataStore.edit { prefs ->
            prefs[NOTIF_ENABLED]            = s.enabled
            prefs[POLL_INTERVAL_MINUTES]    = s.pollIntervalMinutes
            prefs[NOTIF_STARRED_PROJECTS]   = s.notifyStarredProjects
            prefs[NOTIF_PROJECT_TYPE]       = s.projectNotifType
            prefs[NOTIF_DIGEST_INTERVAL]    = s.digestInterval
            prefs[NOTIF_STARRED_ISSUES]     = s.notifyStarredIssues
            prefs[LAST_PROJECT_DIGEST_SENT] = s.lastProjectDigestSentAt
            prefs[LAST_ISSUE_DIGEST_SENT]   = s.lastIssueDigestSentAt
        }
    }

    suspend fun appendPendingProjectDigestItem(item: String) {
        context.dataStore.edit { prefs ->
            val existing = prefs[PENDING_PROJECT_DIGEST] ?: ""
            prefs[PENDING_PROJECT_DIGEST] = if (existing.isBlank()) item else "$existing|$item"
        }
    }

    suspend fun appendPendingIssueDigestItem(item: String) {
        context.dataStore.edit { prefs ->
            val existing = prefs[PENDING_ISSUE_DIGEST] ?: ""
            prefs[PENDING_ISSUE_DIGEST] = if (existing.isBlank()) item else "$existing|$item"
        }
    }

    suspend fun clearPendingProjectDigest(sentAt: Long) {
        context.dataStore.edit { prefs ->
            prefs.remove(PENDING_PROJECT_DIGEST)
            prefs[LAST_PROJECT_DIGEST_SENT] = sentAt
        }
    }

    suspend fun clearPendingIssueDigest(sentAt: Long) {
        context.dataStore.edit { prefs ->
            prefs.remove(PENDING_ISSUE_DIGEST)
            prefs[LAST_ISSUE_DIGEST_SENT] = sentAt
        }
    }
}
