package com.drupaltracker.app.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.drupaltracker.app.data.api.RetrofitClient
import com.drupaltracker.app.data.db.AppDatabase
import com.drupaltracker.app.data.model.NotificationRecord
import com.drupaltracker.app.data.model.SeenIssue
import com.drupaltracker.app.data.model.StarredIssue
import com.drupaltracker.app.data.model.StarredProject
import com.drupaltracker.app.data.preferences.NotificationSettings
import com.drupaltracker.app.data.preferences.SettingsRepository
import com.drupaltracker.app.notifications.NotificationHelper
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

class PollingWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "PollingWorker"
    }

    private val db by lazy { AppDatabase.getInstance(applicationContext) }
    private val settingsRepo by lazy { SettingsRepository(applicationContext) }

    override suspend fun doWork(): Result {
        val settings = settingsRepo.notificationSettingsFlow.first()
        if (!settings.enabled) return Result.success()

        return try {
            // Poll projects and check issues concurrently — they are independent.
            coroutineScope {
                val projectsJob = async { pollStarredProjects(settings) }
                val issuesJob   = async { checkStarredIssues(settings) }
                projectsJob.await()
                issuesJob.await()
            }
            checkAndSendDigests(settings)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Poll error", e)
            Result.retry()
        }
    }

    // All starred projects are fetched concurrently.
    private suspend fun pollStarredProjects(settings: NotificationSettings) {
        val projects = db.starredProjectDao().getAllProjectsOnce()
        Log.d(TAG, "Polling ${projects.size} starred projects")
        coroutineScope {
            projects.map { project -> async { pollSingleProject(project, settings) } }.awaitAll()
        }
    }

    private suspend fun pollSingleProject(project: StarredProject, settings: NotificationSettings) {
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
                    db.issueDao().upsert(SeenIssue(
                        nid = issue.nid,
                        projectNid = project.nid,
                        title = issue.title,
                        status = issue.status,
                        priority = issue.priority,
                        changed = changedTs,
                        url = issue.url,
                        commentCount = issue.commentCount.toIntOrNull() ?: 0
                    ))

                    if (settings.notifyStarredProjects) {
                        val actionLabel = if (isNew) "New issue" else "Updated issue"
                        val record = NotificationRecord(
                            title = "$actionLabel · ${project.title}",
                            body = issue.title,
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
                                applicationContext,
                                record.copy(id = insertedId),
                                (insertedId and 0x7FFFFFFFL).toInt()
                            )
                        } else {
                            settingsRepo.appendPendingProjectDigestItem("${project.title}: ${issue.title}")
                        }
                    }
                }
            }

            db.starredProjectDao().updateLastChecked(project.nid, newLastChecked * 1000)
        } catch (e: Exception) {
            Log.e(TAG, "Error polling project ${project.machineName}", e)
        }
    }

    // All starred issues are checked concurrently.
    private suspend fun checkStarredIssues(settings: NotificationSettings) {
        if (!settings.notifyStarredIssues) return
        val starredIssues = db.starredIssueDao().getAllIssuesOnce()
        coroutineScope {
            starredIssues.map { issue -> async { checkSingleIssue(issue) } }.awaitAll()
        }
    }

    private suspend fun checkSingleIssue(starredIssue: StarredIssue) {
        try {
            val detail = RetrofitClient.service.getNodeDetail(starredIssue.nid)
            val newChanged = detail.changed?.toLongOrNull() ?: 0L
            if (newChanged > starredIssue.changed) {
                db.starredIssueDao().updateChanged(starredIssue.nid, newChanged)

                val record = NotificationRecord(
                    title = "New activity on issue #${starredIssue.nid} - ${starredIssue.title}",
                    body = "",
                    timestamp = System.currentTimeMillis(),
                    recordType = "issue_update",
                    targetNid = starredIssue.nid,
                    targetUrl = starredIssue.url,
                    isProject = false
                )
                val insertedId = db.notificationRecordDao().insert(record)
                db.notificationRecordDao().pruneOldRecords()
                NotificationHelper.postIssueUpdateNotification(
                    applicationContext,
                    record.copy(id = insertedId),
                    (insertedId and 0x7FFFFFFFL).toInt()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking starred issue ${starredIssue.nid}", e)
        }
    }

    private suspend fun checkAndSendDigests(settings: NotificationSettings) {
        val now = System.currentTimeMillis()
        val intervalMs = when (settings.digestInterval) {
            "DAILY"  -> 24 * 60 * 60 * 1000L
            "WEEKLY" -> 7 * 24 * 60 * 60 * 1000L
            else     -> 60 * 60 * 1000L
        }

        val pendingProjects = settingsRepo.pendingProjectDigestFlow.first()
        if (pendingProjects.isNotEmpty() &&
            settings.projectNotifType == "DIGEST" &&
            now - settings.lastProjectDigestSentAt >= intervalMs) {

            val items = pendingProjects.take(5).joinToString("\n") { "• $it" }
            val extra = if (pendingProjects.size > 5) "\n…and ${pendingProjects.size - 5} more" else ""
            val record = NotificationRecord(
                title = "Project updates digest (${pendingProjects.size} issues)",
                body = items + extra,
                timestamp = now,
                recordType = "project_digest",
                targetNid = "",
                targetUrl = "",
                isProject = true
            )
            val insertedId = db.notificationRecordDao().insert(record)
            db.notificationRecordDao().pruneOldRecords()
            NotificationHelper.postDigestNotification(
                applicationContext, record.copy(id = insertedId), NotificationHelper.NOTIF_ID_PROJECT_DIGEST
            )
            settingsRepo.clearPendingProjectDigest(now)
        }
    }
}
