package com.drupaltracker.app.data.db

import androidx.room.*
import com.drupaltracker.app.data.model.StarredProject
import kotlinx.coroutines.flow.Flow

@Dao
interface StarredProjectDao {
    @Query("SELECT * FROM starred_projects ORDER BY starredAt ASC")
    fun getAllProjects(): Flow<List<StarredProject>>

    @Query("SELECT * FROM starred_projects ORDER BY starredAt ASC")
    suspend fun getAllProjectsOnce(): List<StarredProject>

    @Query("SELECT * FROM starred_projects WHERE nid = :nid")
    suspend fun getProject(nid: String): StarredProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: StarredProject)

    @Query("DELETE FROM starred_projects WHERE nid = :nid")
    suspend fun deleteByNid(nid: String)

    @Query("UPDATE starred_projects SET lastChecked = :timestamp WHERE nid = :nid")
    suspend fun updateLastChecked(nid: String, timestamp: Long)
}
