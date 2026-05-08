package com.bushop.sg.data.api

import com.bushop.sg.data.model.BusArrivalResponse

/** Production implementation backed by Retrofit + ArrivelahApi. */
class RetrofitBusArrivalDataSource(
    private val api: ArrivelahApi = ApiClient.api
) : BusArrivalDataSource {
    override suspend fun getBusArrivals(busStopCode: String): BusArrivalResponse =
        api.getBusArrivals(busStopCode)
}
