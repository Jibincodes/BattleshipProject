package com.example.battleshipproject.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// we have constantly switched the server between  brad-home.ch and javaprojects.ch, as one  was not responding to requests at times
// finally javaprojects.ch seems to be more reliable, hence this was kept - Jibin
object ApiClient {
    private const val SERVER = "javaprojects.ch"
    private const val PORT = 50003
    private const val BASE_URL = "http://$SERVER:$PORT/"
    private const val TAG = "ApiClient"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    // Generous time out of 2 minutes for all requests - Jibin
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.MINUTES)    // 2 mins for all
        .readTimeout(2, TimeUnit.MINUTES)      
        .writeTimeout(2, TimeUnit.MINUTES)      
        .retryOnConnectionFailure(true)
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            try {
                val request = chain.request()
                Log.d(TAG, "Making request to: ${request.url}")
                val response = chain.proceed(request)
                Log.d(TAG, "Received response: ${response.code}")
                response
            } catch (e: Exception) {
                Log.e(TAG, "Network error: ${e.message}", e)
                throw e
            }
        }
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val battleshipService: BattleshipApiService = retrofit.create(BattleshipApiService::class.java)
} 