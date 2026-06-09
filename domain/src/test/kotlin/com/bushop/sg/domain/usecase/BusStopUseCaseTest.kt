package com.bushop.sg.domain.usecase

import com.bushop.sg.domain.model.BusInfo
import com.bushop.sg.domain.model.BusService
import com.bushop.sg.domain.model.BusStop
import com.bushop.sg.domain.model.BusStopWithArrivals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Regression tests for [BusStopUseCase]. */
class BusStopUseCaseTest {
    private lateinit var useCase: BusStopUseCase

    @Before
    fun setUp() {
        useCase = BusStopUseCase()
    }

    // ── sortServices ──

    @Test
    fun `sortServices without sortByEarliest returns original order`() {
        val services =
            listOf(
                BusService("15", "GAS", null, null, null),
                BusService("167", "SBST", null, null, null),
            )
        assertEquals(services, useCase.sortServices(services, false))
    }

    @Test
    fun `sortServices sorts by earliest ETA`() {
        val bus15 =
            BusService(
                "15",
                "GAS",
                next = BusInfo("", 180_000, null, null, "SEA", null, "SD", 0, null, null),
                subsequent = null,
                next3 = null,
            )
        val bus167 =
            BusService(
                "167",
                "SBST",
                next = BusInfo("", 60_000, null, null, "SEA", null, "SD", 0, null, null),
                subsequent = null,
                next3 = null,
            )
        val bus151 = BusService("151", "SBST", next = null, subsequent = null, next3 = null)

        val sorted = useCase.sortServices(listOf(bus15, bus167, bus151), true)
        assertEquals("167", sorted[0].serviceNo) // 60s
        assertEquals("15", sorted[1].serviceNo) // 180s
        assertEquals("151", sorted[2].serviceNo) // null → Long.MAX_VALUE
    }

    @Test
    fun `sortServices puts arriving buses first`() {
        val bus15 =
            BusService(
                "15",
                "GAS",
                next = BusInfo("", 30_000, null, null, "SEA", null, "SD", 0, null, null),
                subsequent = null,
                next3 = null,
            )
        val bus167 =
            BusService(
                "167",
                "SBST",
                next = BusInfo("", 60_000, null, null, "SEA", null, "SD", 0, null, null),
                subsequent = null,
                next3 = null,
            )

        val sorted = useCase.sortServices(listOf(bus167, bus15), true)
        assertEquals("15", sorted[0].serviceNo) // < 60s → 0
        assertEquals("167", sorted[1].serviceNo)
    }

    // ── sortServicesWithPins ──

    @Test
    fun `sortServicesWithPins puts pinned services first`() {
        val bus15 =
            BusService(
                "15",
                "GAS",
                next = BusInfo("", 180_000, null, null, "SEA", null, "SD", 0, null, null),
                subsequent = null,
                next3 = null,
            )
        val bus167 =
            BusService(
                "167",
                "SBST",
                next = BusInfo("", 60_000, null, null, "SEA", null, "SD", 0, null, null),
                subsequent = null,
                next3 = null,
            )

        val sorted = useCase.sortServicesWithPins(listOf(bus15, bus167), setOf("15"), true)
        assertEquals("Pinned 15 should be first", "15", sorted[0].serviceNo)
        assertEquals("167 should be second", "167", sorted[1].serviceNo)
    }

    @Test
    fun `sortServicesWithPins sorts pinned items by earliest ETA too`() {
        val bus15 =
            BusService(
                "15",
                "GAS",
                next = BusInfo("", 180_000, null, null, "SEA", null, "SD", 0, null, null),
                subsequent = null,
                next3 = null,
            )
        val bus151 =
            BusService(
                "151",
                "SBST",
                next = BusInfo("", 120_000, null, null, "SEA", null, "SD", 0, null, null),
                subsequent = null,
                next3 = null,
            )

        val sorted = useCase.sortServicesWithPins(listOf(bus15, bus151), setOf("15", "151"), true)
        assertEquals("Earlier ETA first among pinned", "151", sorted[0].serviceNo)
        assertEquals("Later ETA second among pinned", "15", sorted[1].serviceNo)
    }

    @Test
    fun `sortServicesWithPins empty list returns empty`() {
        val sorted = useCase.sortServicesWithPins(emptyList(), setOf("15"), true)
        assertTrue(sorted.isEmpty())
    }

    @Test
    fun `sortServicesWithPins all pinned returns sorted pinned`() {
        val bus15 =
            BusService(
                "15",
                "GAS",
                next = BusInfo("", 300_000, null, null, "SEA", null, "SD", 0, null, null),
                subsequent = null,
                next3 = null,
            )
        val bus167 =
            BusService(
                "167",
                "SBST",
                next = BusInfo("", 60_000, null, null, "SEA", null, "SD", 0, null, null),
                subsequent = null,
                next3 = null,
            )
        val sorted = useCase.sortServicesWithPins(listOf(bus15, bus167), setOf("15", "167"), true)
        assertEquals("167", sorted[0].serviceNo) // earlier ETA
        assertEquals("15", sorted[1].serviceNo)
    }

    @Test
    fun `sortServicesWithPins none pinned returns sorted unpinned`() {
        val bus15 =
            BusService(
                "15",
                "GAS",
                next = BusInfo("", 300_000, null, null, "SEA", null, "SD", 0, null, null),
                subsequent = null,
                next3 = null,
            )
        val bus167 =
            BusService(
                "167",
                "SBST",
                next = BusInfo("", 60_000, null, null, "SEA", null, "SD", 0, null, null),
                subsequent = null,
                next3 = null,
            )
        val sorted = useCase.sortServicesWithPins(listOf(bus15, bus167), emptySet(), true)
        assertEquals("167", sorted[0].serviceNo)
        assertEquals("15", sorted[1].serviceNo)
    }

    @Test
    fun `sortServicesWithPins arriving bus first among pinned`() {
        val arriving =
            BusService(
                "15",
                "GAS",
                next = BusInfo("", 30_000, null, null, "SEA", null, "SD", 0, null, null),
                subsequent = null,
                next3 = null,
            )
        val later =
            BusService(
                "167",
                "SBST",
                next = BusInfo("", 180_000, null, null, "SEA", null, "SD", 0, null, null),
                subsequent = null,
                next3 = null,
            )
        // Pinned, arriving (<60s = 0) should be before pinned, later
        val sorted = useCase.sortServicesWithPins(listOf(later, arriving), setOf("15", "167"), true)
        assertEquals("Arriving pinned bus first", "15", sorted[0].serviceNo)
        assertEquals("Later pinned bus after", "167", sorted[1].serviceNo)
    }

    // ── applyPinning ──

    @Test
    fun `applyPinning moves pinned stops to top`() {
        val stops =
            listOf(
                BusStopWithArrivals(BusStop("11111"), isPinned = false),
                BusStopWithArrivals(BusStop("22222"), isPinned = true),
                BusStopWithArrivals(BusStop("33333"), isPinned = false),
            )
        val result = useCase.applyPinning(stops, false, listOf("11111", "22222", "33333"))
        assertEquals("22222", result[0].busStop.code)
        assertEquals("11111", result[1].busStop.code)
        assertEquals("33333", result[2].busStop.code)
    }

    @Test
    fun `applyPinning unpinning restores addition order`() {
        val stops =
            listOf(
                BusStopWithArrivals(BusStop("11111"), isPinned = false),
                BusStopWithArrivals(BusStop("22222"), isPinned = false),
                BusStopWithArrivals(BusStop("33333"), isPinned = false),
            )
        val additionOrder = listOf("11111", "22222", "33333")
        val result = useCase.applyPinning(stops, true, additionOrder)
        assertEquals("Order should match addition order", additionOrder, result.map { it.busStop.code })
    }

    @Test
    fun `applyPinning multiple pinned stops all at top`() {
        val stops =
            listOf(
                BusStopWithArrivals(BusStop("11111"), isPinned = true),
                BusStopWithArrivals(BusStop("22222"), isPinned = false),
                BusStopWithArrivals(BusStop("33333"), isPinned = true),
            )
        val result = useCase.applyPinning(stops, false, emptyList())
        assertEquals(3, result.size)
        assertTrue(result[0].isPinned)
        assertTrue(result[1].isPinned)
        assertFalse(result[2].isPinned)
    }

    // ── toggleCollapsed ──

    @Test
    fun `toggleCollapsed flips collapsed state`() {
        val stops =
            listOf(
                BusStopWithArrivals(BusStop("11111"), isCollapsed = false),
            )
        val (result, codes) = useCase.toggleCollapsed(stops, "11111")
        assertTrue(result.first().isCollapsed)
        assertEquals(setOf("11111"), codes.toSet())
    }

    @Test
    fun `toggleCollapsed on collapsed expands`() {
        val stops =
            listOf(
                BusStopWithArrivals(BusStop("11111"), isCollapsed = true),
            )
        val (result, _) = useCase.toggleCollapsed(stops, "11111")
        assertFalse(result.first().isCollapsed)
    }

    @Test
    fun `toggleCollapsed returns empty codes when all expanded`() {
        val stops =
            listOf(
                BusStopWithArrivals(BusStop("11111"), isCollapsed = true),
            )
        val (_, codes) = useCase.toggleCollapsed(stops, "11111")
        assertTrue("No stops should be collapsed", codes.isEmpty())
    }

    @Test
    fun `toggleCollapsed unknown code returns unchanged`() {
        val stops =
            listOf(
                BusStopWithArrivals(BusStop("11111"), isCollapsed = false),
            )
        val (result, codes) = useCase.toggleCollapsed(stops, "99999")
        assertEquals(stops, result)
        assertTrue(codes.isEmpty())
    }

    @Test
    fun `collapseStop forces expanded stop into collapsed set`() {
        val stops =
            listOf(
                BusStopWithArrivals(BusStop("11111"), isCollapsed = false),
            )
        val (result, codes) = useCase.collapseStop(stops, "11111")
        assertTrue(result.first().isCollapsed)
        assertEquals(setOf("11111"), codes)
    }

    @Test
    fun `collapseStop for missing code still persists code for later flow merge`() {
        val stops =
            listOf(
                BusStopWithArrivals(BusStop("11111"), isCollapsed = true),
            )
        val (result, codes) = useCase.collapseStop(stops, "22222")
        assertEquals(stops, result)
        assertEquals(setOf("11111", "22222"), codes)
    }

    @Test
    fun `applyPersistedCollapsedState gives precedence to persisted collapsed codes`() {
        val stops =
            listOf(
                BusStopWithArrivals(BusStop("11111"), isCollapsed = false),
                BusStopWithArrivals(BusStop("22222"), isCollapsed = true),
            )
        val result = useCase.applyPersistedCollapsedState(stops, setOf("11111"))
        assertTrue(result.first().isCollapsed)
        assertTrue(result[1].isCollapsed)
    }

    // ── ApplyPinning edge cases ──

    @Test
    fun `applyPinning empty list returns empty`() {
        val result = useCase.applyPinning(emptyList(), false, emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `applyPinning all pinned keeps all at top`() {
        val stops =
            listOf(
                BusStopWithArrivals(BusStop("11111"), isPinned = true),
                BusStopWithArrivals(BusStop("22222"), isPinned = true),
            )
        val result = useCase.applyPinning(stops, false, emptyList())
        assertEquals(2, result.size)
        assertTrue(result.all { it.isPinned })
    }

    @Test
    fun `applyPinning no pinned stops keeps original order`() {
        val stops =
            listOf(
                BusStopWithArrivals(BusStop("11111"), isPinned = false),
                BusStopWithArrivals(BusStop("22222"), isPinned = false),
            )
        val result = useCase.applyPinning(stops, false, listOf("11111", "22222"))
        assertEquals(listOf("11111", "22222"), result.map { it.busStop.code })
    }

    @Test
    fun `applyPinning unpinning with empty additionOrder`() {
        val stops =
            listOf(
                BusStopWithArrivals(BusStop("11111"), isPinned = false),
                BusStopWithArrivals(BusStop("22222"), isPinned = false),
            )
        val result = useCase.applyPinning(stops, true, emptyList())
        assertEquals(2, result.size)
    }

    @Test
    fun `applyPinning single stop pinned`() {
        val stops =
            listOf(
                BusStopWithArrivals(BusStop("11111"), isPinned = true),
            )
        val result = useCase.applyPinning(stops, false, listOf("11111"))
        assertEquals(listOf("11111"), result.map { it.busStop.code })
        assertTrue(result.first().isPinned)
    }

    @Test
    fun `applyPinning unpinning with missing codes in additionOrder`() {
        val stops =
            listOf(
                BusStopWithArrivals(BusStop("11111"), isPinned = false),
                BusStopWithArrivals(BusStop("22222"), isPinned = false),
            )
        // additionOrder has extra codes not in stops — should not crash
        val result = useCase.applyPinning(stops, true, listOf("11111", "33333", "22222"))
        assertEquals(2, result.size)
    }

    // ── ToggleCollapse edge cases ──

    @Test
    fun `toggleCollapsed empty list returns empty`() {
        val (result, codes) = useCase.toggleCollapsed(emptyList(), "11111")
        assertTrue(result.isEmpty())
        assertTrue(codes.isEmpty())
    }

    @Test
    fun `toggleCollapsed on multiple stops only toggles one`() {
        val stops =
            listOf(
                BusStopWithArrivals(BusStop("11111"), isCollapsed = false),
                BusStopWithArrivals(BusStop("22222"), isCollapsed = true),
                BusStopWithArrivals(BusStop("33333"), isCollapsed = false),
            )
        val (result, codes) = useCase.toggleCollapsed(stops, "11111")
        assertTrue(result[0].isCollapsed) // toggled
        assertTrue(result[1].isCollapsed) // unchanged
        assertFalse(result[2].isCollapsed) // unchanged
        assertEquals(setOf("11111", "22222"), codes.toSet())
    }

    @Test
    fun `toggleCollapsed single stop expand returns empty codes`() {
        val stops =
            listOf(
                BusStopWithArrivals(BusStop("11111"), isCollapsed = true),
            )
        val (_, codes) = useCase.toggleCollapsed(stops, "11111")
        assertTrue(codes.isEmpty())
    }
}
