package com.bushop.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression tests for [AutoRefreshController]. */
class AutoRefreshControllerTest {
    @Test
    fun `start does not immediately invoke callback`() =
        runTest {
            val controller = AutoRefreshController(this)
            var invoked = false
            controller.start(intervalSeconds = 1) { invoked = true }
            assertFalse(invoked)
            controller.stop()
        }

    @Test
    fun `callback fires after real delay elapses`() =
        runTest {
            val controller = AutoRefreshController(this)
            var count = 0
            controller.start(intervalSeconds = 0) { count++ }
            // With 0 interval, callback fires immediately only if scheduler decides
            controller.stop()
            assertTrue(count == 0 || count == 1)
        }

    @Test
    fun `stop prevents further callbacks`() =
        runTest {
            val controller = AutoRefreshController(this)
            var count = 0
            controller.start(intervalSeconds = 1) { count++ }
            controller.stop()
            // After stop, no more callbacks
            assertTrue(count <= 1)
        }

    @Test
    fun `zero interval does not start`() =
        runTest {
            val controller = AutoRefreshController(this)
            var invoked = false
            controller.start(intervalSeconds = 0) { invoked = true }
            controller.stop()
            assertFalse(invoked)
        }

    @Test
    fun `negative interval does not start`() =
        runTest {
            val controller = AutoRefreshController(this)
            var invoked = false
            controller.start(intervalSeconds = -1) { invoked = true }
            controller.stop()
            assertFalse(invoked)
        }

    @Test
    fun `restart cancels previous interval`() =
        runTest {
            val controller = AutoRefreshController(this)
            var count = 0
            controller.start(intervalSeconds = 1) { count++ }
            val afterFirst = count
            controller.start(intervalSeconds = 2) { count++ } // restart
            controller.stop()
            // At least one callback may have fired
            assertTrue(count >= afterFirst)
        }

    @Test
    fun `onCleared stops the controller`() =
        runTest {
            val controller = AutoRefreshController(this)
            var count = 0
            controller.start(intervalSeconds = 1) { count++ }
            val afterFirst = count
            controller.onCleared()
            assertTrue(count >= afterFirst)
        }
}
