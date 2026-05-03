package com.drupaltracker.app.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://www.drupal.org/api-d7/"
    private const val JSONAPI_BASE_URL = "https://www.drupal.org/jsonapi/"
    private const val CACHE_SIZE = 5L * 1024 * 1024 // 5 MB

    private var cache: Cache? = null

    /** Call once from Application.onCreate() before any network request is made. */
    fun init(cacheDir: File) {
        cache = Cache(File(cacheDir, "http_cache"), CACHE_SIZE)
    }

    internal val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "DrupalTrackerAndroid/1.0 (personal issue monitor)")
                    .build()
                chain.proceed(request)
            }
            .apply {
                if (com.drupaltracker.app.BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    })
                }
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    internal val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(BodyValue::class.java, BodyValueAdapter())
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    val service: DrupalApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(DrupalApiService::class.java)
    }

    val jsonApiService: DrupalJsonApiService by lazy {
        Retrofit.Builder()
            .baseUrl(JSONAPI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(DrupalJsonApiService::class.java)
    }
}
