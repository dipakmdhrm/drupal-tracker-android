package com.drupaltracker.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "starred_issues")
data class StarredIssue(
    @PrimaryKey val nid: String,
    val projectNid: String,
    val projectTitle: String,
    val title: String,
    val status: String,
    val priority: String,
    val changed: Long,
    val url: String,
    val commentCount: Int,
    val cachedSummary: String? = null,
    val summarizedCommentCount: Int = 0,
    val starredAt: Long = System.currentTimeMillis()
)
