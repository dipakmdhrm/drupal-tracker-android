package com.drupaltracker.app.data.db

import androidx.room.*
import com.drupaltracker.app.data.model.WatchedProject
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM watched_projects ORDER BY addedAt ASC")
    fun getAllProjects(): Flow<List<WatchedProject>>

    @Query("SELECT * FROM watched_projects ORDER BY addedAt ASC")
    suspend fun getAllProjectsOnce(): List<WatchedProject>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: WatchedProject)

    @Delete
    suspend fun delete(project: WatchedProject)

    @Query("UPDATE watched_projects SET lastChecked = :timestamp WHERE nid = :nid")
    suspend fun updateLastChecked(nid: String, timestamp: Long)
}
