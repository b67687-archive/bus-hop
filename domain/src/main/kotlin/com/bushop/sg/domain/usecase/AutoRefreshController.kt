package com.bushop.sg.domain.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Manages the auto-refresh timer lifecycle. */
class AutoRefreshController(
    private val scope: CoroutineScope
) {
    private var job: Job? = null

    fun start(intervalSeconds: Int, onRefresh: () -> Unit) {
        stop()
        if (intervalSeconds <= 0) return
        job = scope.launch {
            while (true) {
                delay(intervalSeconds * 1000L)
                onRefresh()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun onCleared() {
        stop()
    }
}
