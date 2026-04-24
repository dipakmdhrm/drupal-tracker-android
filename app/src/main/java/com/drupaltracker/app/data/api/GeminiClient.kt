package com.drupaltracker.app.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import java.io.IOException

// --- Request models ---

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig = GeminiGenerationConfig()
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val temperature: Double = 0.3,
    val maxOutputTokens: Int = 800
)

// --- Response models ---

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiError? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiError(
    val message: String? = null,
    val code: Int? = null
)

// --- Client ---

object GeminiClient {

    private const val ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/gemma-3-27b-it:generateContent"

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun summarize(apiKey: String, prompt: String): String = withContext(Dispatchers.IO) {
        val moshi = RetrofitClient.moshi
        val requestBody = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
        )
        val reqJson = moshi.adapter(GeminiRequest::class.java).toJson(requestBody)

        val request = Request.Builder()
            .url(ENDPOINT)
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(reqJson.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        RetrofitClient.okHttpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val errResp = runCatching {
                    moshi.adapter(GeminiResponse::class.java).fromJson(body)
                }.getOrNull()
                val msg = errResp?.error?.message ?: "HTTP ${response.code}"
                throw IOException("Gemini error: $msg")
            }
            val parsed = moshi.adapter(GeminiResponse::class.java).fromJson(body)
            parsed?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw IOException("Empty response from Gemini")
        }
    }
}
