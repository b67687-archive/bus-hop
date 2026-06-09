package com.bushop.sg.data.api

import retrofit2.http.GET
import retrofit2.http.Query

/** Retrofit interface for LTA DataMall BusArrivalv2 API. */
interface LtaDataMallApi {
    @GET("BusArrivalv2")
    suspend fun getBusArrivals(
        @Query("BusStopCode") busStopCode: String,
    ): LtaBusArrivalResponse
}
