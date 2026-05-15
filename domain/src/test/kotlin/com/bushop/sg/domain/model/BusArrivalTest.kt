package com.bushop.sg.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression tests for [BusInfo.toDisplayArrival]. */
class BusArrivalTest {

    // ── ETA text ──

    @Test
    fun `eta shows Arr when less than 1 minute`() {
        val info = BusInfo(
            time = "2024-01-01T00:00:30+08:00", durationMs = 30_000,
            lat = null, lng = null, load = "SEA", feature = null, type = "SD",
            visitNumber = 1, originCode = null, destinationCode = null
        )
        val display = info.toDisplayArrival()
        assertEquals("Arr.", display.eta)
    }

    @Test
    fun `eta shows minutes when 1 minute or more`() {
        val info = BusInfo(
            time = "", durationMs = 120_000,
            lat = null, lng = null, load = "SEA", feature = null, type = "SD",
            visitNumber = 1, originCode = null, destinationCode = null
        )
        val display = info.toDisplayArrival()
        assertEquals("2 min", display.eta)
    }

    @Test
    fun `eta shows minute singular`() {
        val info = BusInfo(
            time = "", durationMs = 60_000,
            lat = null, lng = null, load = "SEA", feature = null, type = "SD",
            visitNumber = 1, originCode = null, destinationCode = null
        )
        val display = info.toDisplayArrival()
        assertEquals("1 min", display.eta)
    }

    @Test
    fun `eta at exactly 0 ms shows Arr`() {
        val info = BusInfo(
            time = "", durationMs = 0,
            lat = null, lng = null, load = "SEA", feature = null, type = "SD",
            visitNumber = 1, originCode = null, destinationCode = null
        )
        val display = info.toDisplayArrival()
        assertEquals("Arr.", display.eta)
    }

    // ── Load mapping ──

    @Test
    fun `empty load maps to empty string`() {
        val info = BusInfo("", 0, null, null, "", null, "SD", 0, null, null)
        assertEquals("", info.toDisplayArrival().load)
    }

    @Test
    fun `all default fields still produce eta`() {
        val info = BusInfo("", 120_000, null, null, "", null, "", 0, null, null)
        val display = info.toDisplayArrival()
        assertEquals("2 min", display.eta)
        assertEquals("", display.load)
        assertEquals("", display.busType)
        assertEquals(false, display.isWheelchairAccessible)
    }

    @Test
    fun `empty time string still shows eta`() {
        val info = BusInfo("", 180_000, null, null, "SEA", null, "SD", 0, null, null)
        assertEquals("3 min", info.toDisplayArrival().eta)
    }

    @Test
    fun `SDA load maps to Standing Available`() {
        val info = BusInfo("", 0, null, null, "SDA", null, "SD", 0, null, null)
        // Regression: verify the full string is present, not truncated
        val load = info.toDisplayArrival().load
        assertEquals("Standing Available", load)
        assertTrue("Must contain 'Standing Available'", load.contains("Available"))
    }
}
