package com.bushop.sg.data.repository

import com.bushop.sg.data.api.ApiClient
import com.bushop.sg.data.local.BusStopStorage
import com.bushop.sg.data.model.BusService
import com.bushop.sg.data.model.BusStop
import kotlinx.coroutines.flow.Flow

class BusRepository(
    private val storage: BusStopStorage
) {
    val savedBusStops: Flow<List<BusStop>> = storage.savedBusStops

    suspend fun addBusStop(stop: BusStop) {
        storage.addBusStop(stop)
    }

    suspend fun removeBusStop(code: String) {
        storage.removeBusStop(code)
    }

    suspend fun getBusArrivals(busStopCode: String): Result<List<BusService>> {
        return try {
            val response = ApiClient.api.getBusArrivals(busStopCode)
            Result.success(response.services)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}