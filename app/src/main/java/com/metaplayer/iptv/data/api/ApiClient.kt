package com.metaplayer.iptv.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * API Client configuration for MetaPlayer backend.
 */
object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8000/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Changed from BODY to BASIC to avoid logging every line of large M3U files
        level = HttpLoggingInterceptor.Level.BASIC
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val api: MetaPlayerApi = retrofit.create(MetaPlayerApi::class.java)
    
    fun updateBaseUrl(newBaseUrl: String): MetaPlayerApi {
        val newRetrofit = Retrofit.Builder()
            .baseUrl(newBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return newRetrofit.create(MetaPlayerApi::class.java)
    }
}
