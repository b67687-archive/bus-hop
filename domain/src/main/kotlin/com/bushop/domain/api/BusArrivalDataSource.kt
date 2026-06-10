package com.bushop.domain.api

import com.bushop.domain.model.BusArrivalResponse

/** Abstraction over the Arrivelah API. Enables DI and test mocking without mockkObject. */
interface BusArrivalDataSource {
    suspend fun getBusArrivals(busStopCode: String): BusArrivalResponse
}
