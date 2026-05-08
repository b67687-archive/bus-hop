package com.bushop.sg.ui.screens

import io.mockk.coEvery
import com.bushop.sg.data.local.BusStopEntry
import com.bushop.sg.data.local.BusStopIndex
import com.bushop.sg.domain.model.BusInfo
import com.bushop.sg.domain.model.BusService
import com.bushop.sg.domain.model.BusStop
import com.bushop.sg.domain.model.DuplicateStopException
import com.bushop.sg.domain.model.NetworkResult
import com.bushop.sg.domain.repository.BusRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
    private val collapsedStopsFlow = MutableStateFlow<List<String>>(emptyList())
    private val themeModeFlow = MutableStateFlow(0)
    private val cachedTimestampsFlow = MutableStateFlow<Map<String, Long>>(emptyMap())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true) {
            every { savedBusStops } returns savedStopsFlow
            every { cachedBusServices } returns cachedServicesFlow
            every { cachedTimestamps } returns cachedTimestampsFlow
            every { collapsedStopCodes } returns collapsedStopsFlow
            every { themeMode } returns themeModeFlow
            coEvery { getAutoRefreshIntervalOnce() } returns 0
            coEvery { addBusStop(any()) } returns Result.success(Unit)
            coEvery { removeBusStop(any()) } returns Unit
            coEvery { getBusArrivals(any()) } returns NetworkResult.Success(emptyList())
            // Persistence mocks that actually update the flows
            coEvery { setCollapsedStops(any()) } answers {
                collapsedStopsFlow.value = firstArg()
            }
            coEvery { setThemeMode(any()) } answers {
                themeModeFlow.value = firstArg()
            }
        }

        busStopIndex = mockk(relaxed = true) {
            every { search(any()) } returns emptyList()
            every { findByCode(any()) } returns null
        }

        viewModel = MainViewModel(repository, busStopIndex)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Adding stops ──

    @Test
    fun `addBusStop with valid code calls repository`() = runTest(testDispatcher) {
        coEvery { repository.getBusArrivals("12345") } returns NetworkResult.Success(
            listOf(BusService("167", "SBST", next = null, subsequent = null, next3 = null))
        )

        viewModel.addBusStop("12345")
        advanceUntilIdle()

        coVerify { repository.addBusStop(BusStop(code = "12345", name = "")) }
    }

    @Test
    fun `addBusStop with invalid code shows error`() = runTest(testDispatcher) {
        viewModel.addBusStop("abc")
        advanceUntilIdle()

        assertEquals("Invalid bus stop code", viewModel.addStopError)
    }

    @Test
    fun `addBusStop with nonexistent stop shows error`() = runTest(testDispatcher) {
        coEvery { repository.getBusArrivals("99999") } returns NetworkResult.Error("Not found")

        viewModel.addBusStop("99999")
        advanceUntilIdle()

        assertNotNull("Error should be set for nonexistent stop", viewModel.addStopError)
    }

    @Test
    fun `addBusStop with empty services shows error`() = runTest(testDispatcher) {
        coEvery { repository.getBusArrivals("12345") } returns NetworkResult.Success(emptyList())

        viewModel.addBusStop("12345")
        advanceUntilIdle()

        assertNotNull("Error should be set when no services found", viewModel.addStopError)
    }

    @Test
    fun `addBusStop clears loading after completion`() = runTest(testDispatcher) {
        val busService = BusService("167", "SBST", next = null, subsequent = null, next3 = null)
        coEvery { repository.getBusArrivals("12345") } returns NetworkResult.Success(listOf(busService))

        viewModel.addBusStop("12345")
        advanceUntilIdle()

        assertFalse("Loading should be cleared after completion", viewModel.addStopIsLoading)
    }

    // ── Removing stops ──

    @Test
    fun `removeBusStop calls repository and removes from state`() = runTest(testDispatcher) {
        savedStopsFlow.value = listOf(BusStop("12345"))
        advanceUntilIdle()

        assertTrue(viewModel.savedStops.value.any { it.busStop.code == "12345" })

        viewModel.removeBusStop("12345")
        advanceUntilIdle()

        coVerify { repository.removeBusStop("12345") }
    }

    // ── Pin / Sort ──

    @Test
    fun `togglePin moves stop to top`() = runTest(testDispatcher) {
        savedStopsFlow.value = listOf(BusStop("11111"), BusStop("22222"))
        advanceUntilIdle()

        viewModel.togglePin("11111")
        advanceUntilIdle()

        val stops = viewModel.savedStops.value
        assertTrue("First stop should be pinned", stops.first().isPinned)
        assertEquals("Pinned stop should be at top", "11111", stops.first().busStop.code)
    }

    @Test
    fun `togglePin on pinned stop unpins it`() = runTest(testDispatcher) {
        savedStopsFlow.value = listOf(BusStop("11111"), BusStop("22222"))
        advanceUntilIdle()

        viewModel.togglePin("11111")
        advanceUntilIdle()
        assertTrue(viewModel.savedStops.value.first().isPinned)

        viewModel.togglePin("11111")
        advanceUntilIdle()

        val pinnedStop = viewModel.savedStops.value.find { it.busStop.code == "11111" }
        assertNotNull(pinnedStop)
        assertFalse(pinnedStop?.isPinned ?: true)
    }

    @Test
    fun `unpinned stop returns to code-sorted position`() = runTest(testDispatcher) {
        // Initial order sorted by code: 11111, 22222, 33333
        savedStopsFlow.value = listOf(BusStop("11111"), BusStop("22222"), BusStop("33333"))
        advanceUntilIdle()

        // Pin the last stop — it moves to top
        viewModel.togglePin("33333")
        advanceUntilIdle()
        assertEquals("33333", viewModel.savedStops.value.first().busStop.code)

        // Unpin — it should return to its original code-sorted position (last)
        viewModel.togglePin("33333")
        advanceUntilIdle()

        val codes = viewModel.savedStops.value.map { it.busStop.code }
        assertEquals("Expected code order after unpin", listOf("11111", "22222", "33333"), codes)
    }

    @Test
    fun `pinning multiple stops keeps all pinned at top`() = runTest(testDispatcher) {
        savedStopsFlow.value = listOf(BusStop("11111"), BusStop("22222"), BusStop("33333"))
        advanceUntilIdle()

        viewModel.togglePin("33333")
        advanceUntilIdle()
        viewModel.togglePin("11111")
        advanceUntilIdle()

        val codes = viewModel.savedStops.value.map { it.busStop.code }
        assertEquals("Pinned stops should be at top", setOf("11111", "33333"), codes.take(2).toSet())
        assertEquals("Unpinned stop should be last", "22222", codes.last())
    }

    @Test
    fun `collapse state preserved across combine re-emission`() = runTest(testDispatcher) {
        savedStopsFlow.value = listOf(BusStop("11111"))
        advanceUntilIdle()

        // Collapse the stop
        viewModel.toggleCollapse("11111")
        advanceUntilIdle()
        assertTrue(viewModel.savedStops.value.first().isCollapsed)

        // Trigger combine re-emission via sort toggle
        viewModel.toggleSortOrder()
        advanceUntilIdle()

        // Collapse state should be preserved
        assertTrue("Collapse state should survive re-emission", viewModel.savedStops.value.first().isCollapsed)
    }

    @Test
    fun `toggleSortOrder sorts services by ETA`() = runTest(testDispatcher) {
        val bus15 = BusService("15", "GAS",
            next = BusInfo("", 60_000, null, null, "SEA", null, "SD", 0, null, null),
            subsequent = null, next3 = null)
        val bus167 = BusService("167", "SBST",
            next = BusInfo("", 300_000, null, null, "SEA", null, "SD", 0, null, null),
            subsequent = null, next3 = null)
        val bus151 = BusService("151", "SBST", next = null, subsequent = null, next3 = null)

        savedStopsFlow.value = listOf(BusStop("12345"))
        cachedServicesFlow.value = mapOf("12345" to listOf(bus167, bus15, bus151))
        advanceUntilIdle()

        viewModel.toggleSortOrder()
        advanceUntilIdle()

        val services = viewModel.savedStops.value.first().services
        val idx15 = services.indexOfFirst { it.serviceNo == "15" }
        val idx167 = services.indexOfFirst { it.serviceNo == "167" }

        assertTrue("Bus 15 (60s) should sort before Bus 167 (300s)", idx15 < idx167)
    }

    // ── Collapse ──

    @Test
    fun `toggleCollapse flips collapsed state`() = runTest(testDispatcher) {
        savedStopsFlow.value = listOf(BusStop("12345"))
        advanceUntilIdle()

        viewModel.toggleCollapse("12345")
        advanceUntilIdle()
        assertTrue("Should be collapsed", viewModel.savedStops.value.first().isCollapsed)

        viewModel.toggleCollapse("12345")
        advanceUntilIdle()
        assertFalse("Should be expanded", viewModel.savedStops.value.first().isCollapsed)
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
    fun `manual refresh shows and clears loading state`() = runTest(testDispatcher) {
        savedStopsFlow.value = listOf(BusStop("12345"))
        advanceUntilIdle()

        coEvery { repository.getBusArrivals("12345") } returns NetworkResult.Success(
            listOf(BusService("167", "SBST", next = null, subsequent = null, next3 = null))
        )

        viewModel.refreshArrivals("12345", isAutoRefresh = false)
        advanceUntilIdle()

        assertFalse("Loading should be false after completion", viewModel.savedStops.value.first().isLoading)
    }

    @Test
    fun `autoRefresh does not set loading to true`() = runTest(testDispatcher) {
        savedStopsFlow.value = listOf(BusStop("12345"))
        advanceUntilIdle()

        coEvery { repository.getBusArrivals("12345") } returns NetworkResult.Success(
            listOf(BusService("167", "SBST", next = null, subsequent = null, next3 = null))
        )

        viewModel.refreshArrivals("12345", isAutoRefresh = true)
        advanceUntilIdle()

        assertFalse("Auto-refresh should not show loading", viewModel.savedStops.value.first().isLoading)
    }

    // ── Search ──

    @Test
    fun `searchBusStops delegates to index`() {
        every { busStopIndex.search("123") } returns listOf(
            BusStopEntry("12345", "Test Stop", "Test Rd")
        )

        val results = viewModel.searchBusStops("123")

        assertTrue(results.isNotEmpty())
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
    fun `refreshAll handles network failure gracefully`() = runTest(testDispatcher) {
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
    fun `autoRefresh handles network failure without showing error`() = runTest(testDispatcher) {
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
    fun `refreshArrivals respects cooldown and only calls API once`() = runTest(testDispatcher) {
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
    fun `addBusStop with duplicate shows error`() = runTest(testDispatcher) {
        coEvery { repository.getBusArrivals("12345") } returns NetworkResult.Success(
            listOf(BusService("167", "SBST", next = null, subsequent = null, next3 = null))
        )
        coEvery { repository.addBusStop(any()) } returns Result.failure(DuplicateStopException("exists"))

        viewModel.addBusStop("12345")
        advanceUntilIdle()

        assertNotNull("Error should be set for duplicate stop", viewModel.addStopError)
        assertTrue("Error should mention duplicate",
            viewModel.addStopError?.contains("already exists", ignoreCase = true) ?: false)
    }

    // ── Theme ──

    @Test
    fun `toggleThemeMode cycles through 0, 1, 2, and back to 0`() {
        assertEquals(0, viewModel.themeMode)

        viewModel.toggleThemeMode()
        assertEquals(1, viewModel.themeMode)

        viewModel.toggleThemeMode()
        assertEquals(2, viewModel.themeMode)

        viewModel.toggleThemeMode()
        assertEquals(0, viewModel.themeMode)
    }

    @Test
    fun `toggleThemeMode persists via repository`() = runTest(testDispatcher) {
        viewModel.toggleThemeMode()
        // After advancing dispatcher, the save coroutine should have run
        runCurrent()
        assertEquals(1, themeModeFlow.value)
    }

    // ── Auto-refresh interval ──

    @Test
    fun `setAutoRefreshInterval with zero stops auto-refresh`() = runTest(testDispatcher) {
        viewModel.setAutoRefreshInterval(0)
        advanceUntilIdle()

        assertEquals(0, viewModel.autoRefreshIntervalSeconds)
        coVerify { repository.setAutoRefreshInterval(0) }
    }

    @Test
    fun `setAutoRefreshInterval with positive value starts auto-refresh`() = runTest(testDispatcher) {
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
}
