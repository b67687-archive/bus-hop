package com.bushop.sg.domain.repository

import com.bushop.sg.domain.model.BusService
import com.bushop.sg.domain.model.BusStop
import com.bushop.sg.domain.model.ColorSchemeOption
import com.bushop.sg.domain.model.NetworkResult
import com.bushop.sg.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Repository interface for bus stop data. Pure domain — no Android dependencies. */
interface BusRepository {
    val savedBusStops: Flow<List<BusStop>>
    val cachedBusServices: Flow<Map<String, List<BusService>>>
    val cachedTimestamps: Flow<Map<String, Long>>
    val themeModeFlow: Flow<ThemeMode>
    val colorSchemeOptionFlow: Flow<ColorSchemeOption>
    val collapsedStopsFlow: Flow<Set<String>>
    val isIndexReady: StateFlow<Boolean>
    val autoRefreshInterval: Flow<Int>

    suspend fun getAutoRefreshIntervalOnce(): Int

    suspend fun setAutoRefreshInterval(seconds: Int)

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setColorSchemeOption(option: ColorSchemeOption)

    suspend fun setCollapsedStops(stops: Set<String>)

    val sortByEarliestFlow: Flow<Boolean>

    suspend fun setSortByEarliest(enabled: Boolean)

    val pinnedServicesFlow: Flow<Set<String>>

    suspend fun savePinnedServices(pinned: Set<String>)

    suspend fun addBusStop(stop: BusStop): Result<Unit>

    suspend fun removeBusStop(code: String)

    suspend fun reorderStops(stops: List<BusStop>)

    suspend fun getBusArrivals(busStopCode: String): NetworkResult<List<BusService>>
}
