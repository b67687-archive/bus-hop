package com.bushop.data.api

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression tests for [retrySuspend]. */
class RetryUtilTest {
    @Test
    fun `success on first attempt returns immediately`() =
        runTest {
            var attempts = 0
            val result =
                retrySuspend(maxRetries = 2) {
                    attempts++
                    Result.success("ok")
                }
            assertTrue(result.isSuccess)
            assertEquals(1, attempts)
        }

    @Test
    fun `success after retries returns result`() =
        runTest {
            var attempts = 0
            val result =
                retrySuspend(maxRetries = 2) {
                    attempts++
                    if (attempts < 2) {
                        Result.failure(Exception("try again"))
                    } else {
                        Result.success("ok")
                    }
                }
            assertTrue(result.isSuccess)
            assertEquals(2, attempts)
        }

    @Test
    fun `failure after all retries propagates last error`() =
        runTest {
            val result =
                retrySuspend<String>(maxRetries = 1) {
                    Result.failure(Exception("always fails"))
                }
            assertTrue(result.isFailure)
            assertEquals("always fails", result.exceptionOrNull()?.message)
        }

    @Test
    fun `cancellation exception is rethrown not retried`() =
        runTest {
            var attempts = 0
            try {
                retrySuspend<String>(maxRetries = 2) {
                    attempts++
                    throw CancellationException("cancelled")
                }
            } catch (e: CancellationException) {
                assertEquals("cancelled", e.message)
            }
            assertEquals(1, attempts) // only 1 attempt, not retried
        }

    @Test
    fun `exception thrown from block is caught and retried`() =
        runTest {
            var attempts = 0
            val result =
                retrySuspend(maxRetries = 1) {
                    attempts++
                    if (attempts < 2) {
                        throw Exception("boom")
                    } else {
                        Result.success("recovered")
                    }
                }
            assertTrue(result.isSuccess)
            assertEquals(2, attempts)
        }

    @Test
    fun `retry respects maxRetries zero`() =
        runTest {
            var attempts = 0
            val result =
                retrySuspend<String>(maxRetries = 0) {
                    attempts++
                    Result.failure(Exception("fail"))
                }
            assertTrue(result.isFailure)
            assertEquals(1, attempts)
        }
}
