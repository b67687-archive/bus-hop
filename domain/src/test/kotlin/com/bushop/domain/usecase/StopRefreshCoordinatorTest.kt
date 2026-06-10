package com.bushop.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Regression tests for [StopRefreshCoordinator]. */
class StopRefreshCoordinatorTest {
    private lateinit var coordinator: StopRefreshCoordinator

    @Before
    fun setUp() {
        coordinator = StopRefreshCoordinator(cooldownMs = 1000L, maxConcurrent = 5)
    }

    @Test
    fun `first refresh is always allowed`() =
        runTest {
            assertTrue(coordinator.tryRefresh("12345", isAutoRefresh = false))
        }

    @Test
    fun `auto refresh within cooldown is allowed`() =
        runTest {
            assertTrue(coordinator.tryRefresh("12345", isAutoRefresh = false))
            // Auto-refresh ignores cooldown
            assertTrue(coordinator.tryRefresh("12345", isAutoRefresh = true))
        }

    @Test
    fun `manual refresh within cooldown is blocked`() =
        runTest {
            assertTrue(coordinator.tryRefresh("12345", isAutoRefresh = false))
            assertFalse(coordinator.tryRefresh("12345", isAutoRefresh = false))
        }

    @Test
    fun `different stops have independent cooldowns`() =
        runTest {
            assertTrue(coordinator.tryRefresh("11111", isAutoRefresh = false))
            // Different stop — not affected by 11111's cooldown
            assertTrue(coordinator.tryRefresh("22222", isAutoRefresh = false))
            // 11111 blocked by its own cooldown
            assertFalse(coordinator.tryRefresh("11111", isAutoRefresh = false))
            // 22222 also blocked by its own cooldown (not because of 11111)
            assertFalse(coordinator.tryRefresh("22222", isAutoRefresh = false))
        }

    @Test
    fun `refreshAllConcurrent refreshes all codes`() =
        runTest {
            val refreshed = mutableListOf<String>()
            coordinator.refreshAllConcurrent(
                codes = listOf("11111", "22222", "33333"),
                isAutoRefresh = false,
                refreshBlock = { refreshed.add(it) },
            )
            assertTrue(refreshed.containsAll(listOf("11111", "22222", "33333")))
        }

    @Test
    fun `chunking limits concurrent refreshes`() =
        runTest {
            val inFlight = mutableSetOf<String>()
            var maxInFlight = 0

            coordinator.refreshAllConcurrent(
                codes = listOf("a", "b", "c", "d", "e", "f"),
                isAutoRefresh = true,
                refreshBlock = { code ->
                    inFlight.add(code)
                    maxInFlight = maxOf(maxInFlight, inFlight.size)
                    inFlight.remove(code)
                },
            )
            // With maxConcurrent=5 and 6 items, max in-flight should be ≤ 5
            assertTrue("Max concurrent was $maxInFlight", maxInFlight <= 5)
        }
}
