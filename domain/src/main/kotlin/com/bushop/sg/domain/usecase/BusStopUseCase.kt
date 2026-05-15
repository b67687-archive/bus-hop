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
        val sortedUnpinned = sortServices(unpinned, sortByEarliest)
        return pinned + sortedUnpinned
    }

    /** Pinning is now purely visual — no reordering. */
    fun applyPinning(
        stops: List<BusStopWithArrivals>,
        wasPinned: Boolean,
        additionOrder: List<String>
    ): List<BusStopWithArrivals> = stops

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
