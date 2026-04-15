package com.bushop.sg.data.api

import com.bushop.sg.data.model.BusArrivalResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ArrivelahApi {
    @GET("/")
    suspend fun getBusArrivals(@Query("id") busStopCode: String): BusArrivalResponse
}