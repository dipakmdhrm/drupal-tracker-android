package com.drupaltracker.app.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://www.drupal.org/api-d7/"
    private const val JSONAPI_BASE_URL = "https://www.drupal.org/jsonapi/"

    internal val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "DrupalTrackerAndroid/1.0 (personal issue monitor)")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(BodyValue::class.java, BodyValueAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: DrupalApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(DrupalApiService::class.java)

    val jsonApiService: DrupalJsonApiService = Retrofit.Builder()
        .baseUrl(JSONAPI_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(DrupalJsonApiService::class.java)
}
