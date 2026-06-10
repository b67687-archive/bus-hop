package com.bushop.data.api

import com.bushop.domain.api.BusArrivalDataSource
import com.bushop.domain.model.BusArrivalResponse

/** Production implementation backed by Retrofit + ArrivelahApi. */
class RetrofitBusArrivalDataSource(
    private val api: ArrivelahApi = ApiClient.api,
) : BusArrivalDataSource {
    override suspend fun getBusArrivals(busStopCode: String): BusArrivalResponse = api.getBusArrivals(busStopCode).toDomain()
}
