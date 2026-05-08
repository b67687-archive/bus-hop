package com.bushop.sg.data.repository

import com.bushop.sg.data.api.RetrofitBusArrivalDataSource
import com.bushop.sg.data.api.retrySuspend
import com.bushop.sg.data.local.BusStopStorage
import com.bushop.sg.domain.api.BusArrivalDataSource
import com.bushop.sg.domain.model.BusService
import com.bushop.sg.domain.model.BusStop
import com.bushop.sg.domain.model.NetworkResult
import com.bushop.sg.domain.model.toNetworkResult
import com.bushop.sg.domain.repository.BusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class BusRepositoryImpl(
    private val storage: BusStopStorage,
    private val dataSource: BusArrivalDataSource = RetrofitBusArrivalDataSource()
) : BusRepository {

    override val savedBusStops: Flow<List<BusStop>> = storage.savedBusStops

    override val cachedBusServices: Flow<Map<String, List<BusService>>> = storage.getBusServicesFlow()

    override val cachedTimestamps: Flow<Map<String, Long>> = storage.cachedTimestamps

    override val collapsedStopCodes: Flow<List<String>> = storage.collapsedStopCodes

    override val themeMode: Flow<Int> = storage.themeMode

    override val autoRefreshInterval: Flow<Int> = storage.autoRefreshInterval

    override suspend fun getAutoRefreshIntervalOnce(): Int = storage.getAutoRefreshIntervalOnce()

    override suspend fun setAutoRefreshInterval(seconds: Int) {
        storage.saveAutoRefreshInterval(seconds)
    }

    override suspend fun getThemeModeOnce(): Int = withContext(Dispatchers.IO) {
        storage.themeMode.first()
    }

    override suspend fun setThemeMode(mode: Int) {
        storage.saveThemeMode(mode)
    }

    override suspend fun setCollapsedStops(codes: List<String>) {
        storage.saveCollapsedStops(codes)
    }

    override suspend fun addBusStop(stop: BusStop): Result<Unit> {
        return storage.addBusStop(stop)
    }

    override suspend fun removeBusStop(code: String) {
        storage.removeBusStop(code)
        storage.evictBusServices(code)
    }

    override suspend fun getBusArrivals(busStopCode: String): NetworkResult<List<BusService>> {
        val result = retrySuspend {
            runCatching<List<BusService>> {
                val response = dataSource.getBusArrivals(busStopCode)
                val services = response.services ?: emptyList()
                storage.saveBusServices(busStopCode, services)
                services
            }
        }
        return result.toNetworkResult("Fetch arrivals")
    }
}
