package com.bushop.domain.model

data class BusStopWithArrivals(
    val busStop: BusStop,
    val services: List<BusService> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false,
    val lastUpdated: Long = 0L,
    val cachedAt: Long = 0L, // timestamp of when cached data was fetched
    val isCollapsed: Boolean = false,
    val isPinned: Boolean = false,
) {
    /** Non-reactive: reads wall clock on every access.
     *  True when cached data is older than [STALE_THRESHOLD_MS].
     *  Does NOT auto-update — caller must re-read or trigger refresh. */
    val isStale: Boolean get() =
        cachedAt > 0 && (System.currentTimeMillis() - cachedAt) > STALE_THRESHOLD_MS

    companion object {
        private const val STALE_THRESHOLD_MS = 120_000L // 2 min
    }
}
