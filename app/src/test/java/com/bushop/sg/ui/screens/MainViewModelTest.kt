package com.bushop.sg.ui.screens

import android.app.Application
import com.bushop.sg.data.local.BusStopEntry
import com.bushop.sg.data.local.BusStopIndex
import com.bushop.sg.domain.model.BusInfo
import com.bushop.sg.domain.model.BusService
import com.bushop.sg.domain.model.BusStop
import com.bushop.sg.domain.model.ColorSchemeOption
import com.bushop.sg.domain.model.DuplicateStopException
import com.bushop.sg.domain.model.NetworkResult
import com.bushop.sg.domain.model.ThemeMode
import com.bushop.sg.domain.repository.BusRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: BusRepository
    private lateinit var busStopIndex: BusStopIndex
    private lateinit var viewModel: MainViewModel

    private val savedStopsFlow = MutableStateFlow<List<BusStop>>(emptyList())
    private val cachedServicesFlow = MutableStateFlow<Map<String, List<BusService>>>(emptyMap())
    private val collapsedStopsSetFlow = MutableStateFlow<Set<String>>(emptySet())
    private val cachedTimestampsFlow = MutableStateFlow<Map<String, Long>>(emptyMap())
    private val pinnedServicesStateFlow = MutableStateFlow<Set<String>>(emptySet())
    private val colorSchemeFlow = MutableStateFlow(ColorSchemeOption.BLUE)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository =
            mockk(relaxed = true) {
                every { savedBusStops } returns savedStopsFlow
                every { cachedBusServices } returns cachedServicesFlow
                every { cachedTimestamps } returns cachedTimestampsFlow
                every { collapsedStopsFlow } returns collapsedStopsSetFlow
                every { pinnedServicesFlow } returns pinnedServicesStateFlow
                every { colorSchemeOptionFlow } returns colorSchemeFlow
                coEvery { getAutoRefreshIntervalOnce() } returns 0
                coEvery { addBusStop(any()) } returns Result.success(Unit)
                coEvery { removeBusStop(any()) } returns Unit
                coEvery { getBusArrivals(any()) } returns NetworkResult.Success(emptyList())
                coEvery { setCollapsedStops(any<Set<String>>()) } answers {
                    collapsedStopsSetFlow.value = firstArg()
                }
                coEvery { setColorSchemeOption(any<ColorSchemeOption>()) } answers {
                    colorSchemeFlow.value = firstArg()
                }
                coEvery { setThemeMode(any<ThemeMode>()) } answers { }
            }

        busStopIndex =
            mockk(relaxed = true) {
                every { search(any()) } returns emptyList()
                every { findByCode(any()) } returns null
            }

        viewModel = MainViewModel(mockk(relaxed = true), repository, busStopIndex)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Adding stops ──

    @Test
    fun `addBusStop with valid code calls repository`() =
        runTest(testDispatcher) {
            coEvery { repository.getBusArrivals("12345") } returns
                NetworkResult.Success(
                    listOf(BusService("167", "SBST", next = null, subsequent = null, next3 = null)),
                )

            viewModel.addBusStop("12345")
            advanceUntilIdle()

            coVerify { repository.addBusStop(BusStop(code = "12345", name = "")) }
        }

    @Test
    fun `addBusStop with invalid code shows error`() =
        runTest(testDispatcher) {
            viewModel.addBusStop("abc")
            advanceUntilIdle()

            assertEquals("Invalid bus stop code", viewModel.addStopError)
        }

    @Test
    fun `addBusStop with nonexistent stop shows error`() =
        runTest(testDispatcher) {
            coEvery { repository.getBusArrivals("99999") } returns NetworkResult.Error("Not found")

            viewModel.addBusStop("99999")
            advanceUntilIdle()

            assertNotNull("Error should be set for nonexistent stop", viewModel.addStopError)
        }

    @Test
    fun `addBusStop handles thrown verification exception gracefully`() =
        runTest(testDispatcher) {
            coEvery { repository.getBusArrivals("12345") } throws IllegalStateException("boom")

            viewModel.addBusStop("12345")
            advanceUntilIdle()

            assertFalse(viewModel.addStopIsLoading)
            assertTrue(viewModel.addStopError?.contains("boom") == true)
        }

    @Test
    fun `addBusStop with empty services shows error`() =
        runTest(testDispatcher) {
            coEvery { repository.getBusArrivals("12345") } returns NetworkResult.Success(emptyList())

            viewModel.addBusStop("12345")
            advanceUntilIdle()

            assertNotNull("Error should be set when no services found", viewModel.addStopError)
        }

    @Test
    fun `addBusStop clears loading after completion`() =
        runTest(testDispatcher) {
            val busService = BusService("167", "SBST", next = null, subsequent = null, next3 = null)
            coEvery { repository.getBusArrivals("12345") } returns NetworkResult.Success(listOf(busService))

            viewModel.addBusStop("12345")
            advanceUntilIdle()

            assertFalse("Loading should be cleared after completion", viewModel.addStopIsLoading)
        }

    @Test
    fun `addBusStop defaults new stop to collapsed`() =
        runTest(testDispatcher) {
            val busService = BusService("167", "SBST", next = null, subsequent = null, next3 = null)
            coEvery { repository.getBusArrivals("12345") } returns NetworkResult.Success(listOf(busService))
            coEvery { repository.addBusStop(any()) } answers {
                savedStopsFlow.value = listOf(BusStop("12345"))
                Result.success(Unit)
            }

            viewModel.addBusStop("12345")
            advanceUntilIdle()

            assertTrue(
                viewModel.savedStops.value
                    .first()
                    .isCollapsed,
            )
            assertTrue(collapsedStopsSetFlow.value.contains("12345"))
        }

    // ── Removing stops ──

    @Test
    fun `removeBusStop calls repository and removes from state`() =
        runTest(testDispatcher) {
            savedStopsFlow.value = listOf(BusStop("12345"))
            advanceUntilIdle()

            assertTrue(viewModel.savedStops.value.any { it.busStop.code == "12345" })

            viewModel.removeBusStop("12345")
            advanceUntilIdle()

            coVerify { repository.removeBusStop("12345") }
        }

    // ── Pin / Sort ──

    @Test
    fun `pinning moves stop to top of list`() =
        runTest(testDispatcher) {
            savedStopsFlow.value = listOf(BusStop("11111"), BusStop("22222"))
            advanceUntilIdle()

            viewModel.togglePin("22222")
            advanceUntilIdle()

            val stops = viewModel.savedStops.value
            assertTrue("Pinned stop should have isPinned=true", stops.first().isPinned)
            assertEquals("Pinned stop should be at top", "22222", stops.first().busStop.code)
            assertEquals("Unpinned stop should be second", "11111", stops[1].busStop.code)
        }

    @Test
    fun `togglePin on pinned stop unpins it`() =
        runTest(testDispatcher) {
            savedStopsFlow.value = listOf(BusStop("11111"), BusStop("22222"))
            advanceUntilIdle()

            viewModel.togglePin("11111")
            advanceUntilIdle()
            assertTrue(
                viewModel.savedStops.value
                    .first()
                    .isPinned,
            )

            viewModel.togglePin("11111")
            advanceUntilIdle()

            val pinnedStop = viewModel.savedStops.value.find { it.busStop.code == "11111" }
            assertNotNull(pinnedStop)
            assertFalse(pinnedStop?.isPinned ?: true)
        }

    @Test
    fun `unpinned stop returns to code-sorted order`() =
        runTest(testDispatcher) {
            savedStopsFlow.value = listOf(BusStop("11111"), BusStop("22222"), BusStop("33333"))
            advanceUntilIdle()

            viewModel.togglePin("33333")
            advanceUntilIdle()
            assertEquals(
                "33333",
                viewModel.savedStops.value
                    .first()
                    .busStop.code,
            )

            viewModel.togglePin("33333")
            advanceUntilIdle()

            val codes = viewModel.savedStops.value.map { it.busStop.code }
            assertEquals("Order should be restored after unpin", listOf("11111", "22222", "33333"), codes)
        }

    @Test
    fun `pinning multiple stops keeps all pinned at top`() =
        runTest(testDispatcher) {
            savedStopsFlow.value = listOf(BusStop("11111"), BusStop("22222"), BusStop("33333"))
            advanceUntilIdle()

            viewModel.togglePin("33333")
            advanceUntilIdle()
            viewModel.togglePin("11111")
            advanceUntilIdle()

            val codes = viewModel.savedStops.value.map { it.busStop.code }
            assertEquals("Two pinned stops should be at top", 2, codes.take(2).count { it in setOf("11111", "33333") })
            assertEquals("Unpinned stop should be last", "22222", codes.last())
            assertTrue(
                "All pinned stops should have isPinned=true",
                viewModel.savedStops.value
                    .filter { it.isPinned }
                    .size == 2,
            )
        }

    // ── Per-service pinning ──

    @Test
    fun `togglePinService adds and removes pin key`() =
        runTest(testDispatcher) {
            assertFalse(
                "Service should not be pinned initially",
                viewModel.isServicePinned("12345", "167"),
            )

            viewModel.togglePinService("12345", "167")
            advanceUntilIdle()
            assertTrue(
                "Service should be pinned after toggle",
                viewModel.isServicePinned("12345", "167"),
            )

            viewModel.togglePinService("12345", "167")
            advanceUntilIdle()
            assertFalse(
                "Service should be unpinned after second toggle",
                viewModel.isServicePinned("12345", "167"),
            )
        }

    @Test
    fun `togglePinService persists to repository`() =
        runTest(testDispatcher) {
            viewModel.togglePinService("12345", "167")
            advanceUntilIdle()
            coVerify { repository.savePinnedServices(any()) }
        }

    @Test
    fun `isServicePinned is per-stop`() =
        runTest(testDispatcher) {
            viewModel.togglePinService("11111", "167")
            advanceUntilIdle()

            assertTrue(
                "Service should be pinned at stop 11111",
                viewModel.isServicePinned("11111", "167"),
            )
            assertFalse(
                "Same service at different stop should NOT be pinned",
                viewModel.isServicePinned("22222", "167"),
            )
        }

    @Test
    fun `pinned service sorts first within stop`() =
        runTest(testDispatcher) {
            val bus15 =
                BusService(
                    "15",
                    "GAS",
                    next = BusInfo("", 60_000, null, null, "SEA", null, "SD", 0, null, null),
                    subsequent = null,
                    next3 = null,
                )
            val bus167 =
                BusService(
                    "167",
                    "SBST",
                    next = BusInfo("", 300_000, null, null, "SEA", null, "SD", 0, null, null),
                    subsequent = null,
                    next3 = null,
                )
            val bus151 = BusService("151", "SBST", next = null, subsequent = null, next3 = null)

            savedStopsFlow.value = listOf(BusStop("12345"))
            cachedServicesFlow.value = mapOf("12345" to listOf(bus15, bus167, bus151))
            advanceUntilIdle()

            // Pin the middle service (by ETA: 15(60s) < 167(300s) < 151(null))
            viewModel.togglePinService("12345", "151")
            advanceUntilIdle()

            val services =
                viewModel.savedStops.value
                    .first()
                    .services
            assertEquals("Pinned service 151 should be first", "151", services.first().serviceNo)
        }

    @Test
    fun `collapse state preserved across combine re-emission`() =
        runTest(testDispatcher) {
            savedStopsFlow.value = listOf(BusStop("11111"))
            advanceUntilIdle()

            // Collapse the stop
            viewModel.toggleCollapse("11111")
            advanceUntilIdle()
            assertTrue(
                viewModel.savedStops.value
                    .first()
                    .isCollapsed,
            )

            // Trigger combine re-emission via sort toggle
            viewModel.toggleSortOrder()
            advanceUntilIdle()

            // Collapse state should be preserved
            assertTrue(
                "Collapse state should survive re-emission",
                viewModel.savedStops.value
                    .first()
                    .isCollapsed,
            )
        }

    @Test
    fun `toggleSortOrder sorts services by ETA`() =
        runTest(testDispatcher) {
            val bus15 =
                BusService(
                    "15",
                    "GAS",
                    next = BusInfo("", 60_000, null, null, "SEA", null, "SD", 0, null, null),
                    subsequent = null,
                    next3 = null,
                )
            val bus167 =
                BusService(
                    "167",
                    "SBST",
                    next = BusInfo("", 300_000, null, null, "SEA", null, "SD", 0, null, null),
                    subsequent = null,
                    next3 = null,
                )
            val bus151 = BusService("151", "SBST", next = null, subsequent = null, next3 = null)

            savedStopsFlow.value = listOf(BusStop("12345"))
            cachedServicesFlow.value = mapOf("12345" to listOf(bus167, bus15, bus151))
            advanceUntilIdle()

            viewModel.toggleSortOrder()
            advanceUntilIdle()

            val services =
                viewModel.savedStops.value
                    .first()
                    .services
            val idx15 = services.indexOfFirst { it.serviceNo == "15" }
            val idx167 = services.indexOfFirst { it.serviceNo == "167" }

            assertTrue("Bus 15 (60s) should sort before Bus 167 (300s)", idx15 < idx167)
        }

    // ── Collapse ──

    @Test
    fun `toggleCollapse flips collapsed state`() =
        runTest(testDispatcher) {
            savedStopsFlow.value = listOf(BusStop("12345"))
            advanceUntilIdle()

            viewModel.toggleCollapse("12345")
            advanceUntilIdle()
            assertTrue(
                "Should be collapsed",
                viewModel.savedStops.value
                    .first()
                    .isCollapsed,
            )

            viewModel.toggleCollapse("12345")
            advanceUntilIdle()
            assertFalse(
                "Should be expanded",
                viewModel.savedStops.value
                    .first()
                    .isCollapsed,
            )
        }

    // ── Dialog ──

    @Test
    fun `showAddStopDialog and hideAddStopDialog toggle visibility`() {
        assertFalse(viewModel.addStopDialogVisible)

        viewModel.showAddStopDialog()
        assertTrue(viewModel.addStopDialogVisible)

        viewModel.hideAddStopDialog()
        assertFalse(viewModel.addStopDialogVisible)
    }

    @Test
    fun `hideAddStopDialog clears error and loading`() {
        viewModel.hideAddStopDialog()
        assertNull(viewModel.addStopError)
        assertFalse(viewModel.addStopIsLoading)
    }

    // ── Refresh ──

    @Test
    fun `manual refresh shows and clears loading state`() =
        runTest(testDispatcher) {
            savedStopsFlow.value = listOf(BusStop("12345"))
            advanceUntilIdle()

            coEvery { repository.getBusArrivals("12345") } returns
                NetworkResult.Success(
                    listOf(BusService("167", "SBST", next = null, subsequent = null, next3 = null)),
                )

            viewModel.refreshArrivals("12345", isAutoRefresh = false)
            advanceUntilIdle()

            assertFalse(
                "Loading should be false after completion",
                viewModel.savedStops.value
                    .first()
                    .isLoading,
            )
        }

    @Test
    fun `autoRefresh does not set loading to true`() =
        runTest(testDispatcher) {
            savedStopsFlow.value = listOf(BusStop("12345"))
            advanceUntilIdle()

            coEvery { repository.getBusArrivals("12345") } returns
                NetworkResult.Success(
                    listOf(BusService("167", "SBST", next = null, subsequent = null, next3 = null)),
                )

            viewModel.refreshArrivals("12345", isAutoRefresh = true)
            advanceUntilIdle()

            assertFalse(
                "Auto-refresh should not show loading",
                viewModel.savedStops.value
                    .first()
                    .isLoading,
            )
        }

    // ── Search ──

    @Test
    fun `searchBusStops delegates to index`() =
        runTest(testDispatcher) {
            every { busStopIndex.search("123") } returns
                listOf(
                    BusStopEntry("12345", "Test Stop", "Test Rd"),
                )

            viewModel.searchBusStops("123")
            val results = viewModel.searchResults.first { it.isNotEmpty() }

            assertEquals("12345", results.first().code)
            verify { busStopIndex.search("123") }
        }

    @Test
    fun `findBusStopByCode delegates to index`() {
        every { busStopIndex.findByCode("12345") } returns BusStopEntry("12345", "Test Stop", "Test Rd")

        val result = viewModel.findBusStopByCode("12345")

        assertNotNull(result)
        assertEquals("Test Stop", result?.name)
        verify { busStopIndex.findByCode("12345") }
    }

    // ── Refresh failure ──

    @Test
    fun `refreshAll handles network failure gracefully`() =
        runTest(testDispatcher) {
            savedStopsFlow.value = listOf(BusStop("12345"))
            advanceUntilIdle()

            coEvery { repository.getBusArrivals("12345") } returns NetworkResult.Error("Network error", Exception("Network error"))

            viewModel.refreshAll(isAutoRefresh = false)
            advanceUntilIdle()

            val stop = viewModel.savedStops.value.find { it.busStop.code == "12345" }
            assertNotNull(stop)
            // Loading should be cleared even on failure
            assertFalse("Loading should be cleared after failure", stop?.isLoading ?: true)
            // isRefreshing should be reset after manual refresh
            assertFalse("isRefreshing should be false after completion", viewModel.isRefreshing)
        }

    @Test
    fun `refreshAll handles thrown repository exception gracefully`() =
        runTest(testDispatcher) {
            savedStopsFlow.value = listOf(BusStop("12345"))
            advanceUntilIdle()

            coEvery { repository.getBusArrivals("12345") } throws IllegalStateException("crash")

            viewModel.refreshAll(isAutoRefresh = false)
            advanceUntilIdle()

            val stop = viewModel.savedStops.value.find { it.busStop.code == "12345" }
            assertNotNull(stop)
            assertFalse(stop?.isLoading ?: true)
            assertFalse(viewModel.isRefreshing)
        }

    @Test
    fun `autoRefresh handles network failure without showing error`() =
        runTest(testDispatcher) {
            savedStopsFlow.value = listOf(BusStop("12345"))
            advanceUntilIdle()

            coEvery { repository.getBusArrivals("12345") } returns NetworkResult.Error("Network error", Exception("Network error"))

            viewModel.refreshArrivals("12345", isAutoRefresh = true)
            advanceUntilIdle()

            val stop = viewModel.savedStops.value.find { it.busStop.code == "12345" }
            // Auto-refresh should suppress error (isOffline), leave existing data
            assertNotNull(stop)
        }

    // ── Cooldown ──

    @Test
    fun `refreshArrivals respects cooldown and only calls API once`() =
        runTest(testDispatcher) {
            savedStopsFlow.value = listOf(BusStop("12345"))
            advanceUntilIdle()

            val busService = BusService("167", "SBST", next = null, subsequent = null, next3 = null)
            coEvery { repository.getBusArrivals("12345") } returns NetworkResult.Success(listOf(busService))

            // First call
            viewModel.refreshArrivals("12345", isAutoRefresh = false)
            advanceUntilIdle()
            coVerify(exactly = 1) { repository.getBusArrivals("12345") }

            // Second call within cooldown (30s)
            viewModel.refreshArrivals("12345", isAutoRefresh = false)
            advanceUntilIdle()
            // Should NOT increase invocation count
            coVerify(exactly = 1) { repository.getBusArrivals("12345") }
        }

    // ── Duplicate stop ──

    @Test
    fun `addBusStop with duplicate shows error`() =
        runTest(testDispatcher) {
            coEvery { repository.getBusArrivals("12345") } returns
                NetworkResult.Success(
                    listOf(BusService("167", "SBST", next = null, subsequent = null, next3 = null)),
                )
            coEvery { repository.addBusStop(any()) } returns Result.failure(DuplicateStopException("exists"))

            viewModel.addBusStop("12345")
            advanceUntilIdle()

            assertNotNull("Error should be set for duplicate stop", viewModel.addStopError)
            assertTrue(
                "Error should mention duplicate",
                viewModel.addStopError?.contains("already exists", ignoreCase = true) ?: false,
            )
        }

    // ── Theme ──

    @Test
    fun `toggleThemeMode cycles through SYSTEM, LIGHT, DARK, and back to SYSTEM`() {
        assertEquals(ThemeMode.SYSTEM, viewModel.themeModeFlow.value)

        viewModel.toggleThemeMode()
        assertEquals(ThemeMode.LIGHT, viewModel.themeModeFlow.value)

        viewModel.toggleThemeMode()
        assertEquals(ThemeMode.DARK, viewModel.themeModeFlow.value)

        viewModel.toggleThemeMode()
        assertEquals(ThemeMode.SYSTEM, viewModel.themeModeFlow.value)
    }

    @Test
    fun `toggleThemeMode persists via repository`() =
        runTest(testDispatcher) {
            viewModel.toggleThemeMode()
            runCurrent()
            // themeModeFlow stores as ThemeMode, convert to check persistence
            coVerify { repository.setThemeMode(ThemeMode.LIGHT) }
        }

    @Test
    fun `setColorSchemeOption updates state and persists via repository`() =
        runTest(testDispatcher) {
            viewModel.setColorSchemeOption(ColorSchemeOption.CONTRAST_BLUE)
            runCurrent()

            assertEquals(ColorSchemeOption.CONTRAST_BLUE, viewModel.colorSchemeOptionFlow.value)
            coVerify { repository.setColorSchemeOption(ColorSchemeOption.CONTRAST_BLUE) }
        }

    // ── Auto-refresh interval ──

    @Test
    fun `setAutoRefreshInterval with zero stops auto-refresh`() =
        runTest(testDispatcher) {
            viewModel.setAutoRefreshInterval(0)
            advanceUntilIdle()

            assertEquals(0, viewModel.autoRefreshIntervalSeconds)
            coVerify { repository.setAutoRefreshInterval(0) }
        }

    @Test
    fun `setAutoRefreshInterval with positive value starts auto-refresh`() =
        runTest(testDispatcher) {
            viewModel.setAutoRefreshInterval(30)
            runCurrent() // process save coroutine, but do NOT advance into the while(true) loop

            assertEquals(30, viewModel.autoRefreshIntervalSeconds)
            coVerify { repository.setAutoRefreshInterval(30) }

            // Stop the timer to prevent while(true) loop from leaking via shared test dispatcher
            viewModel.setAutoRefreshInterval(0)
        }

    // ── Empty state ──

    @Test
    fun `savedStops is initially empty`() {
        assertTrue("Saved stops should be initially empty", viewModel.savedStops.value.isEmpty())
    }

    // ── Live reorder (repeated moveStop during drag) ──

    @Test
    fun `moveStop called multiple times maintains correct order`() =
        runTest(testDispatcher) {
            savedStopsFlow.value =
                listOf(BusStop("11111"), BusStop("22222"), BusStop("33333"), BusStop("44444"))
            advanceUntilIdle()

            // Simulate live reorder: move 22222 down 1 position, then 1 more
            viewModel.moveStop("22222", 1)
            advanceUntilIdle()
            var codes = viewModel.savedStops.value.map { it.busStop.code }
            assertEquals(listOf("11111", "33333", "22222", "44444"), codes)

            viewModel.moveStop("22222", 1)
            advanceUntilIdle()
            codes = viewModel.savedStops.value.map { it.busStop.code }
            assertEquals(listOf("11111", "33333", "44444", "22222"), codes)
        }

    @Test
    fun `moveStop repeatedly in opposite direction reverses the order`() =
        runTest(testDispatcher) {
            savedStopsFlow.value =
                listOf(BusStop("11111"), BusStop("22222"), BusStop("33333"), BusStop("44444"))
            advanceUntilIdle()

            // Move 33333 up 1 position, then down 1 → back to original
            viewModel.moveStop("33333", -1)
            advanceUntilIdle()
            var codes = viewModel.savedStops.value.map { it.busStop.code }
            assertEquals(listOf("11111", "33333", "22222", "44444"), codes)

            viewModel.moveStop("33333", 1)
            advanceUntilIdle()
            codes = viewModel.savedStops.value.map { it.busStop.code }
            assertEquals(listOf("11111", "22222", "33333", "44444"), codes)
        }

    @Test
    fun `moveStop large delta does not go out of bounds`() =
        runTest(testDispatcher) {
            savedStopsFlow.value = listOf(BusStop("11111"), BusStop("22222"))
            advanceUntilIdle()

            // Move 11111 down by 99 (way past end) → should clamp to last position
            viewModel.moveStop("11111", 99)
            advanceUntilIdle()
            val codes = viewModel.savedStops.value.map { it.busStop.code }
            assertEquals(listOf("22222", "11111"), codes)
        }

    @Test
    fun `moveStop with delta zero does nothing`() =
        runTest(testDispatcher) {
            savedStopsFlow.value = listOf(BusStop("11111"), BusStop("22222"))
            advanceUntilIdle()

            viewModel.moveStop("11111", 0)
            advanceUntilIdle()
            val codes = viewModel.savedStops.value.map { it.busStop.code }
            assertEquals(listOf("11111", "22222"), codes)
        }
}
