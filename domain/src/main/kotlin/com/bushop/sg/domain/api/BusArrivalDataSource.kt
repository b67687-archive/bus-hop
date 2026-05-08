package com.bushop.sg.domain.api

import com.bushop.sg.domain.model.BusArrivalResponse

/** Abstraction over the Arrivelah API. Enables DI and test mocking without mockkObject. */
interface BusArrivalDataSource {
    suspend fun getBusArrivals(busStopCode: String): BusArrivalResponse
}
