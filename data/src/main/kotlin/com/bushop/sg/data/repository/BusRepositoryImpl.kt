package com.bushop.sg.data.repository

import com.bushop.sg.data.api.retrySuspend
import com.bushop.sg.data.local.BusStopIndex
import com.bushop.sg.data.local.BusStopStorage
import com.bushop.sg.domain.api.BusArrivalDataSource
import com.bushop.sg.domain.model.BusService
import com.bushop.sg.domain.model.BusStop
import com.bushop.sg.domain.model.ColorSchemeOption
import com.bushop.sg.domain.model.NetworkResult
import com.bushop.sg.domain.model.ThemeMode
import com.bushop.sg.domain.model.toNetworkResult
import com.bushop.sg.domain.repository.BusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class BusRepositoryImpl(
    private val storage: BusStopStorage,
    private val dataSource: BusArrivalDataSource,
    private val busStopIndex: BusStopIndex,
) : BusRepository {
    override val savedBusStops: Flow<List<BusStop>> = storage.savedBusStops

    override val cachedBusServices: Flow<Map<String, List<BusService>>> = storage.getBusServicesFlow()

    override val cachedTimestamps: Flow<Map<String, Long>> = storage.cachedTimestamps

    override val themeModeFlow: Flow<ThemeMode> = storage.themeModeFlow

    override val colorSchemeOptionFlow: Flow<ColorSchemeOption> = storage.colorSchemeOptionFlow

    override val collapsedStopsFlow: Flow<Set<String>> = storage.collapsedStopsFlow

    override val pinnedServicesFlow: Flow<Set<String>> = storage.pinnedServices

    override val isIndexReady: StateFlow<Boolean> = busStopIndex.isReady

    override val autoRefreshInterval: Flow<Int> = storage.autoRefreshInterval

    override suspend fun getAutoRefreshIntervalOnce(): Int = storage.getAutoRefreshIntervalOnce()

    override suspend fun setAutoRefreshInterval(seconds: Int) {
        storage.saveAutoRefreshInterval(seconds)
    }

    override suspend fun setCollapsedStops(stops: Set<String>) {
        storage.saveCollapsedStops(stops)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        storage.saveThemeMode(mode)
    }

    override suspend fun setColorSchemeOption(option: ColorSchemeOption) {
        storage.saveColorSchemeOption(option)
    }

    override suspend fun savePinnedServices(pinned: Set<String>) {
        storage.savePinnedServices(pinned)
    }

    override suspend fun addBusStop(stop: BusStop): Result<Unit> = storage.addBusStop(stop)

    override suspend fun removeBusStop(code: String) {
        storage.removeBusStop(code)
    }

    override suspend fun reorderStops(stops: List<BusStop>) {
        storage.reorderStops(stops)
    }

    override suspend fun getBusArrivals(busStopCode: String): NetworkResult<List<BusService>> {
        val result =
            retrySuspend {
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
