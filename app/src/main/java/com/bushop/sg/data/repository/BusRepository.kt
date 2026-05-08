package com.bushop.sg.data.repository

import com.bushop.sg.data.api.BusArrivalDataSource
import com.bushop.sg.data.api.RetrofitBusArrivalDataSource
import com.bushop.sg.data.api.retrySuspend
import com.bushop.sg.data.local.BusStopStorage
import com.bushop.sg.data.model.BusService
import com.bushop.sg.data.model.BusStop
import com.bushop.sg.data.model.NetworkResult
import com.bushop.sg.data.model.toNetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class BusRepository(
    private val storage: BusStopStorage,
    private val dataSource: BusArrivalDataSource = RetrofitBusArrivalDataSource()
) {
    val savedBusStops: Flow<List<BusStop>> = storage.savedBusStops

    val cachedBusServices: Flow<Map<String, List<BusService>>> = storage.getBusServicesFlow()

    val cachedTimestamps: Flow<Map<String, Long>> = storage.cachedTimestamps

    val collapsedStopCodes: Flow<List<String>> = storage.collapsedStopCodes

    val themeMode: Flow<Int> = storage.themeMode

    val autoRefreshInterval: Flow<Int> = storage.autoRefreshInterval

    suspend fun getAutoRefreshIntervalOnce(): Int = storage.getAutoRefreshIntervalOnce()

    suspend fun setAutoRefreshInterval(seconds: Int) {
        storage.saveAutoRefreshInterval(seconds)
    }

    suspend fun getThemeModeOnce(): Int = withContext(Dispatchers.IO) {
        storage.themeMode.first()
    }

    suspend fun setThemeMode(mode: Int) {
        storage.saveThemeMode(mode)
    }

    suspend fun setCollapsedStops(codes: List<String>) {
        storage.saveCollapsedStops(codes)
    }

    suspend fun addBusStop(stop: BusStop): Result<Unit> {
        return storage.addBusStop(stop)
    }

    suspend fun removeBusStop(code: String) {
        storage.removeBusStop(code)
        storage.evictBusServices(code)
    }

    suspend fun getBusArrivals(busStopCode: String): NetworkResult<List<BusService>> {
        val result = retrySuspend {
            runCatching {
                val response = dataSource.getBusArrivals(busStopCode)
                val services = response.services ?: emptyList()
                storage.saveBusServices(busStopCode, services)
                services
            }
        }
        return result.toNetworkResult("Fetch arrivals")
    }
}