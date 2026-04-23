package com.drupaltracker.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_records")
data class NotificationRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val body: String,
    val timestamp: Long,
    val recordType: String,  // "issue_update" | "project_digest" | "issue_digest"
    val targetNid: String,
    val targetUrl: String,
    val isProject: Boolean
)
