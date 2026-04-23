package com.drupaltracker.app.data.api

import com.drupaltracker.app.data.model.JsonApiProjectListResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface DrupalJsonApiService {

    @GET("node/project_module")
    suspend fun searchProjectsContains(
        @Query("filter[title][operator]") operator: String = "CONTAINS",
        @Query("filter[title][value]") value: String,
        @Query("page[limit]") limit: Int = 10,
        @Query("page[offset]") offset: Int = 0
    ): JsonApiProjectListResponse
}
