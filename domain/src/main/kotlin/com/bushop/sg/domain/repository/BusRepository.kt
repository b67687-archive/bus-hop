package com.bushop.sg.domain.repository

import com.bushop.sg.domain.model.BusService
import com.bushop.sg.domain.model.BusStop
import com.bushop.sg.domain.model.NetworkResult
import kotlinx.coroutines.flow.Flow

/** Repository interface for bus stop data. Pure domain — no Android dependencies. */
interface BusRepository {
    val savedBusStops: Flow<List<BusStop>>
    val cachedBusServices: Flow<Map<String, List<BusService>>>
    val cachedTimestamps: Flow<Map<String, Long>>
    val collapsedStopCodes: Flow<List<String>>
    val themeMode: Flow<Int>
    val autoRefreshInterval: Flow<Int>

    suspend fun getAutoRefreshIntervalOnce(): Int
    suspend fun setAutoRefreshInterval(seconds: Int)
    suspend fun getThemeModeOnce(): Int
    suspend fun setThemeMode(mode: Int)
    suspend fun setCollapsedStops(codes: List<String>)
    suspend fun addBusStop(stop: BusStop): Result<Unit>
    suspend fun removeBusStop(code: String)
    suspend fun getBusArrivals(busStopCode: String): NetworkResult<List<BusService>>
}
