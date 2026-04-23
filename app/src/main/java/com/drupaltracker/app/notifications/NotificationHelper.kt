package com.drupaltracker.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.drupaltracker.app.MainActivity
import com.drupaltracker.app.R
import com.drupaltracker.app.data.model.SeenIssue
import com.drupaltracker.app.data.model.toPriorityLabel
import com.drupaltracker.app.data.model.toStatusLabel

object NotificationHelper {

    const val CHANNEL_ISSUES = "drupal_issues"
    const val CHANNEL_SERVICE = "drupal_service"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ISSUES,
                context.getString(R.string.notification_channel_issues),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new or updated Drupal.org issues"
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SERVICE,
                context.getString(R.string.notification_channel_service),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent notification while polling runs"
            }
        )
    }

    fun postIssueNotification(
        context: Context,
        issue: SeenIssue,
        projectName: String,
        isNew: Boolean
    ) {
        val actionLabel = if (isNew) "New issue" else "Updated issue"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(issue.url))
        val pending = PendingIntent.getActivity(
            context,
            issue.nid.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ISSUES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$actionLabel · $projectName")
            .setContentText(issue.title)
            .setSubText("${issue.status.toStatusLabel()} · ${issue.priority.toPriorityLabel()}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(issue.title))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setGroup("drupal_$projectName")
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(issue.nid.hashCode(), notification)
        } catch (e: SecurityException) {
            // Permission not granted — silently skip
        }
    }

    fun buildServiceNotification(context: Context) =
        NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.polling_service_notification))
            .setOngoing(true)
            .setSilent(true)
            .build()
}
