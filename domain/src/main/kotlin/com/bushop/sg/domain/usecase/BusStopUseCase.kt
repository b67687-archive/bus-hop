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

    fun sortServicesWithPins(
        services: List<BusService>,
        pinnedServiceNos: Set<String>,
        sortByEarliest: Boolean
    ): List<BusService> {
        val (pinned, unpinned) = services.partition { it.serviceNo in pinnedServiceNos }
        val sortedPinned = sortServices(pinned, sortByEarliest)
        val sortedUnpinned = sortServices(unpinned, sortByEarliest)
        return sortedPinned + sortedUnpinned
    }

    /** Reorder pinned stops to top. Unpinning restores addition order. */
    fun applyPinning(
        stops: List<BusStopWithArrivals>,
        wasPinned: Boolean,
        additionOrder: List<String>
    ): List<BusStopWithArrivals> {
        val pinned = stops.filter { it.isPinned }
        val unpinned = stops.filter { !it.isPinned }
        return if (wasPinned) {
            // Restore original order from additionOrder; fall back to unpinned order if empty
            val order = if (additionOrder.isNotEmpty()) additionOrder
                        else unpinned.map { it.busStop.code }
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
