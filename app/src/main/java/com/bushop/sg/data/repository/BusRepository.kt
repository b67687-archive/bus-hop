package com.bushop.sg.data.repository

import com.bushop.sg.data.api.ApiClient
import com.bushop.sg.data.local.BusStopStorage
import com.bushop.sg.data.model.BusService
import com.bushop.sg.data.model.BusStop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class BusRepository(
    private val storage: BusStopStorage
) {
    val savedBusStops: Flow<List<BusStop>> = storage.savedBusStops

    val cachedBusServices: Flow<Map<String, List<BusService>>> = storage.getBusServicesFlow()

    val collapsedStopCodes: Flow<List<String>> = storage.collapsedStopCodes

    val themeMode: Flow<Int> = storage.themeMode

    suspend fun getThemeMode(): Int = withContext(Dispatchers.IO) {
        // Read the DataStore-derived flow once
        storage.themeMode.first()
    }

    suspend fun setThemeMode(mode: Int) {
        storage.saveThemeMode(mode)
    }

    suspend fun setCollapsedStops(codes: List<String>) {
        storage.saveCollapsedStops(codes)
    }

    fun getAutoRefreshInterval(): Int = storage.getAutoRefreshInterval()

    suspend fun setAutoRefreshInterval(seconds: Int) {
        storage.saveAutoRefreshInterval(seconds)
    }

    suspend fun addBusStop(stop: BusStop): Result<Unit> {
        return storage.addBusStop(stop)
    }

    suspend fun removeBusStop(code: String) {
        storage.removeBusStop(code)
    }

    suspend fun getBusArrivals(busStopCode: String): Result<List<BusService>> {
        return try {
            val response = ApiClient.api.getBusArrivals(busStopCode)
            val services = response.services ?: emptyList()
            storage.saveBusServices(busStopCode, services)
            Result.success(services)
        } catch (e: java.net.UnknownHostException) {
            Result.failure(e)
        } catch (e: java.io.IOException) {
            // Also catch timeouts, TLS errors, connection resets
            Result.failure(e)
        }
    }
}