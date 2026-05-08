package com.bushop.sg.domain.usecase

import com.bushop.sg.domain.model.BusService
import com.bushop.sg.domain.model.BusStopWithArrivals

/** Business logic for bus stop operations. */
class BusStopUseCase {

    fun sortServices(services: List<BusService>, sortByEarliest: Boolean): List<BusService> {
        if (!sortByEarliest) return services
        return services.sortedBy { service ->
            when {
                service.next == null -> Long.MAX_VALUE
                service.next.durationMs < 60000 -> 0L
                else -> service.next.durationMs
            }
        }
    }

    fun applyPinning(
        stops: List<BusStopWithArrivals>,
        wasPinned: Boolean,
        additionOrder: List<String>
    ): List<BusStopWithArrivals> {
        val pinned = stops.filter { it.isPinned }
        val unpinned = stops.filter { !it.isPinned }
        return if (wasPinned) {
            // Unpinning: restore to addition order among unpinned stops
            val order = additionOrder.filter { code ->
                unpinned.any { it.busStop.code == code }
            }
            val sorted = order.mapNotNull { code ->
                unpinned.find { it.busStop.code == code }
            }
            pinned + sorted
        } else {
            pinned + unpinned
        }
    }

    fun toggleCollapsed(
        stops: List<BusStopWithArrivals>,
        code: String
    ): Pair<List<BusStopWithArrivals>, List<String>> {
        val index = stops.indexOfFirst { it.busStop.code == code }
        if (index == -1) return stops to (stops.filter { it.isCollapsed }.map { it.busStop.code })
        val newCollapsed = !stops[index].isCollapsed
        val updated = stops.toMutableList().apply {
            this[index] = this[index].copy(isCollapsed = newCollapsed)
        }
        val collapsedCodes = updated.filter { it.isCollapsed }.map { it.busStop.code }
        return updated to collapsedCodes
    }
}
