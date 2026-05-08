package com.bushop.sg.data.model

data class BusStopWithArrivals(
    val busStop: BusStop,
    val services: List<BusService> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false,
    val lastUpdated: Long = 0L,
    val cachedAt: Long = 0L, // timestamp of when cached data was fetched
    val isCollapsed: Boolean = false,
    val isPinned: Boolean = false
) {
    val isStale: Boolean get() =
        cachedAt > 0 && (System.currentTimeMillis() - cachedAt) > 120_000 // 2 min
}
