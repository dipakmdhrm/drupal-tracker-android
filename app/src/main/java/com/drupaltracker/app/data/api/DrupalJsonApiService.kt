package com.drupaltracker.app.data.api

import com.drupaltracker.app.data.model.JsonApiProjectListResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface DrupalJsonApiService {

    /**
     * Search project_module nodes by title (the majority of contrib modules).
     */
    @GET("node/project_module")
    suspend fun searchProjects(
        @QueryMap filters: Map<String, String>,
        @Query("page[limit]") limit: Int = 10,
        @Query("page[offset]") offset: Int = 0
    ): JsonApiProjectListResponse
}
