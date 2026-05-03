package com.drupaltracker.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.drupaltracker.app.MainActivity
import com.drupaltracker.app.R
import com.drupaltracker.app.data.model.NotificationRecord

object NotificationHelper {

    const val CHANNEL_ISSUES = "drupal_issues"
    const val CHANNEL_DIGEST = "drupal_digest"

    const val EXTRA_OPEN_NOTIFICATION_STREAM = "open_notification_stream"

    const val NOTIF_ID_PROJECT_DIGEST = 2001
    const val NOTIF_ID_ISSUE_DIGEST   = 2002

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ISSUES,
                context.getString(R.string.notification_channel_issues),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notifications for new or updated Drupal.org issues" }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DIGEST,
                "Drupal Digest",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Digest notifications grouping multiple project or issue updates" }
        )
    }

    fun postIssueUpdateNotification(context: Context, record: NotificationRecord, notifId: Int) {
        val pending = buildAppPendingIntent(context, record)
        val notification = NotificationCompat.Builder(context, CHANNEL_ISSUES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(record.title)
            .setContentText(record.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(record.body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        postSafely(context, notifId, notification)
    }

    fun postDigestNotification(context: Context, record: NotificationRecord, notifId: Int) {
        val pending = buildAppPendingIntent(context, record)
        val notification = NotificationCompat.Builder(context, CHANNEL_DIGEST)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(record.title)
            .setContentText(record.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(record.body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        postSafely(context, notifId, notification)
    }

    private fun buildAppPendingIntent(context: Context, record: NotificationRecord): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_NOTIFICATION_STREAM, true)
        }
        return PendingIntent.getActivity(
            context,
            (record.id and 0x7FFFFFFFL).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun postSafely(context: Context, notifId: Int, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (e: SecurityException) {
            // Permission not granted — silently skip
        }
    }
}
