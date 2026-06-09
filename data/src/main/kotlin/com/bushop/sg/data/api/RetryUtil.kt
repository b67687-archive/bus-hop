package com.bushop.sg.data.api

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * Retry a suspend [block] up to [maxRetries] times with exponential backoff.
 * Re-throws [CancellationException] immediately — does not suppress cancellation.
 */
suspend fun <T> retrySuspend(
    maxRetries: Int = 2,
    initialDelayMs: Long = 1000,
    block: suspend () -> Result<T>,
): Result<T> {
    var lastError: Throwable? = null
    repeat(maxRetries + 1) { attempt ->
        try {
            val result = block()
            if (result.isSuccess) return result
            lastError = result.exceptionOrNull()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            lastError = e
        }
        if (attempt < maxRetries) {
            delay(initialDelayMs * (1 shl attempt))
        }
    }
    return Result.failure(lastError ?: Exception("Max retries exceeded"))
}
