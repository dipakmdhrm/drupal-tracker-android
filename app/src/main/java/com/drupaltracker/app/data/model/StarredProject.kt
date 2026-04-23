package com.drupaltracker.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "starred_projects")
data class StarredProject(
    @PrimaryKey val nid: String,
    val machineName: String,
    val title: String,
    val filterStatus: String? = null,
    val filterPriority: String? = null,
    val lastChecked: Long = 0L,
    val starredAt: Long = System.currentTimeMillis()
)
