package com.drupaltracker.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// --- API response models ---

@JsonClass(generateAdapter = true)
data class IssueListResponse(
    @Json(name = "list") val list: List<IssueApiModel> = emptyList(),
    @Json(name = "last") val last: String? = null
)

@JsonClass(generateAdapter = true)
data class IssueApiModel(
    @Json(name = "nid") val nid: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "created") val created: String = "0",
    @Json(name = "changed") val changed: String = "0",
    @Json(name = "field_issue_status") val status: String = "1",
    @Json(name = "field_issue_priority") val priority: String = "200",
    @Json(name = "field_issue_category") val category: String = "1",
    @Json(name = "field_issue_component") val component: String? = null,
    @Json(name = "field_issue_version") val version: String? = null,
    @Json(name = "comment_count") val commentCount: String = "0",
    @Json(name = "url") val url: String = ""
)

@JsonClass(generateAdapter = true)
data class ProjectNodeResponse(
    @Json(name = "list") val list: List<ProjectNodeApiModel> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ProjectNodeApiModel(
    @Json(name = "nid") val nid: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "field_project_machine_name") val machineName: String? = null
)

// --- Room entities ---

@Entity(tableName = "watched_projects")
data class WatchedProject(
    @PrimaryKey val nid: String,
    val machineName: String,
    val title: String,
    val filterStatus: String? = null,   // null = all statuses
    val filterPriority: String? = null, // null = all priorities
    val lastChecked: Long = 0L,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "seen_issues")
data class SeenIssue(
    @PrimaryKey val nid: String,
    val projectNid: String,
    val title: String,
    val status: String,
    val priority: String,
    val changed: Long,
    val url: String,
    val commentCount: Int,
    val cachedSummary: String? = null,
    val summarizedCommentCount: Int = 0
)

// --- Domain helpers ---

fun String.toStatusLabel(): String = when (this) {
    "1" -> "Active"
    "2" -> "Fixed"
    "3" -> "Closed (duplicate)"
    "4" -> "Postponed"
    "5" -> "Closed (won't fix)"
    "6" -> "Closed (works as designed)"
    "7" -> "Closed (fixed)"
    "8" -> "Needs review"
    "13" -> "Needs work"
    "14" -> "RTBC"
    "15" -> "Patch (to be ported)"
    "16" -> "Postponed (needs info)"
    "17" -> "Closed (outdated)"
    "18" -> "Closed (cannot reproduce)"
    else -> "Unknown"
}

fun String.toPriorityLabel(): String = when (this) {
    "400" -> "Critical"
    "300" -> "Major"
    "200" -> "Normal"
    "100" -> "Minor"
    else -> "Unknown"
}

fun String.toCategoryLabel(): String = when (this) {
    "1" -> "Bug report"
    "2" -> "Task"
    "3" -> "Feature request"
    "4" -> "Support request"
    "5" -> "Plan"
    else -> "Other"
}
