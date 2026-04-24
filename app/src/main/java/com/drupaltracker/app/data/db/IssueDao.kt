package com.drupaltracker.app.data.db

import androidx.room.*
import com.drupaltracker.app.data.model.SeenIssue
import kotlinx.coroutines.flow.Flow

@Dao
interface IssueDao {
    @Query("SELECT * FROM seen_issues WHERE projectNid = :projectNid ORDER BY changed DESC")
    fun getIssuesForProject(projectNid: String): Flow<List<SeenIssue>>

    @Query("SELECT * FROM seen_issues WHERE nid = :nid")
    suspend fun getIssue(nid: String): SeenIssue?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(issue: SeenIssue)

    @Query("UPDATE seen_issues SET cachedSummary = :summary, summarizedCommentCount = :commentCount WHERE nid = :nid")
    suspend fun updateSummary(nid: String, summary: String, commentCount: Int)
}
