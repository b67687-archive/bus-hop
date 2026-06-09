package com.bushop.sg.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface ArrivelahApi {
    @GET("/")
    suspend fun getBusArrivals(
        @Query("id") busStopCode: String,
    ): ArrivelahResponse
}
