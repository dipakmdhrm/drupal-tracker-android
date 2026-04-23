package com.drupaltracker.app.data.db

import androidx.room.*
import com.drupaltracker.app.data.model.StarredIssue
import kotlinx.coroutines.flow.Flow

@Dao
interface StarredIssueDao {
    @Query("SELECT * FROM starred_issues ORDER BY starredAt DESC")
    fun getAllIssues(): Flow<List<StarredIssue>>

    @Query("SELECT * FROM starred_issues ORDER BY starredAt DESC")
    suspend fun getAllIssuesOnce(): List<StarredIssue>

    @Query("SELECT * FROM starred_issues WHERE nid = :nid")
    suspend fun getIssue(nid: String): StarredIssue?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(issue: StarredIssue)

    @Query("DELETE FROM starred_issues WHERE nid = :nid")
    suspend fun deleteByNid(nid: String)

    @Query("UPDATE starred_issues SET changed = :changed WHERE nid = :nid")
    suspend fun updateChanged(nid: String, changed: Long)

    @Query("UPDATE starred_issues SET cachedSummary = :summary, summarizedCommentCount = :commentCount WHERE nid = :nid")
    suspend fun updateSummary(nid: String, summary: String, commentCount: Int)
}
