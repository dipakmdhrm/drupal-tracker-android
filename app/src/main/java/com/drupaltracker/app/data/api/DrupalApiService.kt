package com.drupaltracker.app.data.api

import com.drupaltracker.app.data.model.IssueListResponse
import com.drupaltracker.app.data.model.ProjectNodeResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DrupalApiService {

    @GET("node.json")
    suspend fun getProjectByMachineName(
        @Query("field_project_machine_name") machineName: String,
        @Query("limit") limit: Int = 1
    ): ProjectNodeResponse

    @GET("node.json")
    suspend fun getIssues(
        @Query("type") type: String = "project_issue",
        @Query("field_project") projectNid: String,
        @Query("sort") sort: String = "changed",
        @Query("direction") direction: String = "DESC",
        @Query("limit") limit: Int = 50,
        @Query("page") page: Int = 0,
        @Query("field_issue_status") status: String? = null,
        @Query("field_issue_priority") priority: String? = null
    ): IssueListResponse

    @GET("node/{nid}.json")
    suspend fun getNodeDetail(
        @Path("nid") nid: String
    ): NodeDetailResponse

    @GET("comment.json")
    suspend fun getComments(
        @Query("node") nid: String,
        @Query("limit") limit: Int = 50
    ): CommentListResponse
}
