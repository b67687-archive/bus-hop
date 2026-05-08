package com.bushop.sg.data.api

import com.bushop.sg.data.model.BusArrivalResponse

/** Abstraction over the Arrivelah API. Enables DI and test mocking without mockkObject. */
interface BusArrivalDataSource {
    suspend fun getBusArrivals(busStopCode: String): BusArrivalResponse
}
