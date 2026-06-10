package com.bushop.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://arrivelah2.busrouter.sg/"

    private val loggingInterceptor =
        HttpLoggingInterceptor().apply {
            // Debug logging: enabled by setting system property
            // `-DbusHop.debug=true` in the JVM args or calling ApiClient.enableDebugLogging()
            level = HttpLoggingInterceptor.Level.NONE
        }

    // Call from debug build to enable detailed API logging
    fun enableDebugLogging() {
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
    }

    val okHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

    val api: ArrivelahApi by lazy {
        Retrofit
            .Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(GsonProvider.gson))
            .build()
            .create(ArrivelahApi::class.java)
    }
}
