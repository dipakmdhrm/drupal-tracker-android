package com.drupaltracker.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.drupaltracker.app.data.api.RetrofitClient
import com.drupaltracker.app.data.db.AppDatabase
import com.drupaltracker.app.data.model.SeenIssue
import com.drupaltracker.app.notifications.NotificationHelper
import kotlinx.coroutines.*

class PollingForegroundService : Service() {

    companion object {
        private const val TAG = "PollingService"
        const val EXTRA_INTERVAL_MS = "interval_ms"
        const val SERVICE_ID = 1001
        var isRunning = false
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
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
                    pollAllProjects()
                } catch (e: Exception) {
                    Log.e(TAG, "Poll error: ${e.message}")
                }
                delay(intervalMs)
            }
        }
    }

    private suspend fun pollAllProjects() {
        val db = AppDatabase.getInstance(this)
        val projects = db.projectDao().getAllProjectsOnce()
        Log.d(TAG, "Polling ${projects.size} projects")

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
                    if (changedTs <= lastChecked / 1000) break  // sorted DESC — stop when old

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
                        NotificationHelper.postIssueNotification(
                            context = this,
                            issue = seen,
                            projectName = project.title,
                            isNew = isNew
                        )
                    }
                }

                db.projectDao().updateLastChecked(project.nid, newLastChecked * 1000)
            } catch (e: Exception) {
                Log.e(TAG, "Error polling project ${project.machineName}: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        isRunning = false
        serviceScope.cancel()
        super.onDestroy()
    }
}
