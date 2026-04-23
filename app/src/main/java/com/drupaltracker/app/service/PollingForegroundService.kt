package com.drupaltracker.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.drupaltracker.app.data.api.RetrofitClient
import com.drupaltracker.app.data.db.AppDatabase
import com.drupaltracker.app.data.model.NotificationRecord
import com.drupaltracker.app.data.model.SeenIssue
import com.drupaltracker.app.data.model.StarredIssue
import com.drupaltracker.app.data.preferences.NotificationSettings
import com.drupaltracker.app.data.preferences.SettingsRepository
import com.drupaltracker.app.notifications.NotificationHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class PollingForegroundService : Service() {

    companion object {
        private const val TAG = "PollingService"
        const val EXTRA_INTERVAL_MS = "interval_ms"
        const val SERVICE_ID = 1001
        var isRunning = false
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    private lateinit var settingsRepository: SettingsRepository

    // In-memory digest accumulation
    private val digestPendingProjectChanges = mutableListOf<String>()
    private val digestPendingIssueChanges = mutableListOf<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        NotificationHelper.createChannels(this)
        startForeground(SERVICE_ID, NotificationHelper.buildServiceNotification(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            val intervalMs = intent?.getLongExtra(EXTRA_INTERVAL_MS, 2 * 60 * 1000L) ?: (2 * 60 * 1000L)
            startPolling(intervalMs)
        }
        return START_STICKY
    }

    private fun startPolling(intervalMs: Long) {
        isRunning = true
        pollingJob = serviceScope.launch {
            while (isActive) {
                try {
                    val settings = settingsRepository.notificationSettingsFlow.first()
                    if (settings.enabled) {
                        pollStarredProjects(settings)
                        checkStarredIssues(settings)
                        checkAndSendDigests(settings)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Poll error: ${e.message}")
                }
                delay(intervalMs)
            }
        }
    }

    private suspend fun pollStarredProjects(settings: NotificationSettings) {
        val db = AppDatabase.getInstance(this)
        val projects = db.starredProjectDao().getAllProjectsOnce()
        Log.d(TAG, "Polling ${projects.size} starred projects")

        for (project in projects) {
            try {
                val response = RetrofitClient.service.getIssues(
                    projectNid = project.nid,
                    status = project.filterStatus,
                    priority = project.filterPriority
                )

                val lastChecked = project.lastChecked
                val newLastChecked = System.currentTimeMillis() / 1000

                for (issue in response.list) {
                    val changedTs = issue.changed.toLongOrNull() ?: 0L
                    if (changedTs <= lastChecked / 1000) break

                    val existing = db.issueDao().getIssue(issue.nid)
                    val isNew = existing == null
                    val hasChanged = existing != null &&
                            (existing.status != issue.status ||
                             existing.commentCount != (issue.commentCount.toIntOrNull() ?: 0))

                    if (isNew || hasChanged) {
                        val seen = SeenIssue(
                            nid = issue.nid,
                            projectNid = project.nid,
                            title = issue.title,
                            status = issue.status,
                            priority = issue.priority,
                            changed = changedTs,
                            url = issue.url,
                            commentCount = issue.commentCount.toIntOrNull() ?: 0
                        )
                        db.issueDao().upsert(seen)

                        val actionLabel = if (isNew) "New issue" else "Updated issue"
                        val title = "$actionLabel · ${project.title}"
                        val body = issue.title

                        if (settings.notifyStarredProjects) {
                            val record = NotificationRecord(
                                title = title,
                                body = body,
                                timestamp = System.currentTimeMillis(),
                                recordType = "issue_update",
                                targetNid = issue.nid,
                                targetUrl = issue.url,
                                isProject = false
                            )
                            val insertedId = db.notificationRecordDao().insert(record)
                            db.notificationRecordDao().pruneOldRecords()

                            if (settings.projectNotifType == "EVERY_UPDATE") {
                                NotificationHelper.postIssueUpdateNotification(
                                    this, record.copy(id = insertedId), insertedId.toInt()
                                )
                            } else {
                                digestPendingProjectChanges.add("${project.title}: ${issue.title}")
                            }
                        }
                    }
                }

                db.starredProjectDao().updateLastChecked(project.nid, newLastChecked * 1000)
            } catch (e: Exception) {
                Log.e(TAG, "Error polling project ${project.machineName}: ${e.message}")
            }
        }
    }

    private suspend fun checkStarredIssues(settings: NotificationSettings) {
        if (!settings.notifyStarredIssues) return
        val db = AppDatabase.getInstance(this)
        val starredIssues = db.starredIssueDao().getAllIssuesOnce()

        for (starredIssue in starredIssues) {
            try {
                val detail = RetrofitClient.service.getNodeDetail(starredIssue.nid)
                val newChanged = detail.changed?.toLongOrNull() ?: 0L
                if (newChanged > starredIssue.changed) {
                    db.starredIssueDao().updateChanged(starredIssue.nid, newChanged)
                    digestPendingIssueChanges.add(starredIssue.title)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking starred issue ${starredIssue.nid}: ${e.message}")
            }
        }
    }

    private suspend fun checkAndSendDigests(settings: NotificationSettings) {
        val now = System.currentTimeMillis()
        val intervalMs = when (settings.digestInterval) {
            "DAILY"  -> 24 * 60 * 60 * 1000L
            "WEEKLY" -> 7 * 24 * 60 * 60 * 1000L
            else     -> 60 * 60 * 1000L  // HOURLY
        }

        // Project digest
        if (digestPendingProjectChanges.isNotEmpty() &&
            settings.projectNotifType == "DIGEST" &&
            now - settings.lastProjectDigestSentAt >= intervalMs) {

            val count = digestPendingProjectChanges.size
            val items = digestPendingProjectChanges.take(5).joinToString("\n") { "• $it" }
            val extra = if (count > 5) "\n…and ${count - 5} more" else ""
            val body = items + extra

            val record = NotificationRecord(
                title = "Project updates digest ($count issues)",
                body = body,
                timestamp = now,
                recordType = "project_digest",
                targetNid = "",
                targetUrl = "",
                isProject = true
            )
            val db = AppDatabase.getInstance(this)
            val insertedId = db.notificationRecordDao().insert(record)
            db.notificationRecordDao().pruneOldRecords()
            NotificationHelper.postDigestNotification(
                this, record.copy(id = insertedId), NotificationHelper.NOTIF_ID_PROJECT_DIGEST
            )
            settingsRepository.updateLastProjectDigestSent(now)
            digestPendingProjectChanges.clear()
        }

        // Issue digest
        if (digestPendingIssueChanges.isNotEmpty() &&
            settings.notifyStarredIssues &&
            now - settings.lastIssueDigestSentAt >= intervalMs) {

            val count = digestPendingIssueChanges.size
            val items = digestPendingIssueChanges.take(5).joinToString("\n") { "• $it" }
            val extra = if (count > 5) "\n…and ${count - 5} more" else ""
            val body = items + extra

            val record = NotificationRecord(
                title = "Starred issues digest ($count updates)",
                body = body,
                timestamp = now,
                recordType = "issue_digest",
                targetNid = "",
                targetUrl = "",
                isProject = false
            )
            val db = AppDatabase.getInstance(this)
            val insertedId = db.notificationRecordDao().insert(record)
            db.notificationRecordDao().pruneOldRecords()
            NotificationHelper.postDigestNotification(
                this, record.copy(id = insertedId), NotificationHelper.NOTIF_ID_ISSUE_DIGEST
            )
            settingsRepository.updateLastIssueDigestSent(now)
            digestPendingIssueChanges.clear()
        }
    }

    override fun onDestroy() {
        isRunning = false
        serviceScope.cancel()
        super.onDestroy()
    }
}
