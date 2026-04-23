package com.drupaltracker.app.data.api

import com.drupaltracker.app.data.model.IssueApiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.net.URLEncoder

object IssueRssFetcher {

    private const val RSS_BASE = "https://www.drupal.org/project/issues/rss"

    suspend fun searchGlobal(query: String, page: Int = 0, limit: Int = 10): List<IssueApiModel> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        return fetch("$RSS_BASE/all?text=$encoded&limit=$limit&page=$page")
    }

    suspend fun searchInProject(
        machineName: String,
        query: String,
        page: Int = 0,
        limit: Int = 10
    ): List<IssueApiModel> {
        val url = buildString {
            append("$RSS_BASE/$machineName?limit=$limit&page=$page")
            if (query.isNotBlank()) append("&text=${URLEncoder.encode(query, "UTF-8")}")
        }
        return fetch(url)
    }

    private suspend fun fetch(url: String): List<IssueApiModel> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "DrupalTrackerAndroid/1.0 (personal issue monitor)")
            .build()
        val response = RetrofitClient.okHttpClient.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext emptyList()
        IssueRssParser.parse(body)
    }
}
