package com.bushop.sg.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bushop.sg.data.local.BusStopIndex
import com.bushop.sg.data.local.DuplicateStopException
import com.bushop.sg.data.model.BusService
import com.bushop.sg.data.model.BusStop
import com.bushop.sg.data.repository.BusRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.io.IOException

data class BusStopWithArrivals(
    val busStop: BusStop,
    val services: List<BusService> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false,
    val lastUpdated: Long = 0L,
    val isCollapsed: Boolean = false,
    val isPinned: Boolean = false
)

class MainViewModel(
    private val repository: BusRepository,
    private val busStopIndex: BusStopIndex
) : ViewModel() {

    private val _savedStops = MutableStateFlow<List<BusStopWithArrivals>>(emptyList())
    val savedStops: StateFlow<List<BusStopWithArrivals>> = _savedStops.asStateFlow()

    var addStopDialogVisible by mutableStateOf(false)
        private set
    
    var addStopError by mutableStateOf<String?>(null)
        private set

    private var autoRefreshJob: Job? = null
    var autoRefreshIntervalSeconds by mutableStateOf(30)
        private set
    
    private val _sortByEarliest = MutableStateFlow(false)
    val sortByEarliest: StateFlow<Boolean> = _sortByEarliest.asStateFlow()

    private val lastRefreshTimestamps = mutableMapOf<String, Long>()
    private val refreshCooldownMs = 30_000L

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    var addStopIsLoading by mutableStateOf(false)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    var lastUpdatedAll by mutableStateOf(0L)
        private set

    // 0 = system, 1 = light, 2 = dark
    var themeMode by mutableStateOf(0)

    // Reference addition order from repository (used to restore position on unpin)
    private var additionOrder: List<String> = emptyList()

    init {
        // Restore persisted preferences
        viewModelScope.launch {
            themeMode = repository.getThemeMode()
        }
        viewModelScope.launch {
            autoRefreshIntervalSeconds = repository.getAutoRefreshInterval()
            if (autoRefreshIntervalSeconds > 0) {
                startAutoRefresh()
            }
        }

        viewModelScope.launch {
            combine(
                repository.savedBusStops,
                repository.cachedBusServices,
                repository.collapsedStopCodes,
                _sortByEarliest
            ) { stops, cached, collapsedCodes, sortByEarliest ->
                stops.map { stop ->
                    val cachedServices = cached[stop.code] ?: emptyList()
                    val existing = _savedStops.value.find { it.busStop.code == stop.code }
                    val sortedServices = if (sortByEarliest) {
                        cachedServices.sortedBy { service ->
                            when {
                                service.next == null -> Long.MAX_VALUE
                                service.next.durationMs < 60000 -> 0L
                                else -> service.next.durationMs
                            }
                        }
                    } else {
                        cachedServices
                    }
                    BusStopWithArrivals(
                        busStop = stop,
                        services = sortedServices,
                        isLoading = existing?.isLoading ?: false,
                        error = existing?.error,
                        isOffline = existing?.isOffline ?: false,
                        lastUpdated = existing?.lastUpdated ?: 0L,
                        isCollapsed = existing?.isCollapsed ?: collapsedCodes.contains(stop.code),
                        isPinned = existing?.isPinned ?: false
                    )
                }
            }.collect { list ->
                additionOrder = list.map { it.busStop.code }
                lastUpdatedAll = list.maxOfOrNull { it.lastUpdated } ?: lastUpdatedAll
                val pinnedFirst = list.sortedByDescending { it.isPinned }
                _savedStops.value = pinnedFirst
                if (pinnedFirst.isNotEmpty() && pinnedFirst.none { it.services.isNotEmpty() }) {
                    refreshAll(isAutoRefresh = true)
                }
            }
        }
    }

    fun showAddStopDialog() {
        addStopError = null
        addStopDialogVisible = true
    }

    fun searchBusStops(query: String) = busStopIndex.search(query)

    fun findBusStopByCode(code: String) = busStopIndex.findByCode(code)

    fun hideAddStopDialog() {
        addStopDialogVisible = false
        addStopError = null
        addStopIsLoading = false
    }

    fun addBusStop(code: String, name: String = "") {
        // Guard against rapid double-taps
        if (addStopIsLoading) return

        viewModelScope.launch {
            val formattedCode = code.trim()
            if (formattedCode.length == 5 && formattedCode.all { it.isDigit() }) {
                addStopIsLoading = true
                addStopError = null

                // Validate stop exists by fetching arrivals first
                val arrivalResult = try {
                    repository.getBusArrivals(formattedCode)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    addStopError = "Could not verify bus stop (${e.message ?: "connection error"})."
                    addStopIsLoading = false
                    return@launch
                }
                if (arrivalResult.isFailure) {
                    addStopError = "Bus stop not found. Check the code and try again."
                    addStopIsLoading = false
                    return@launch
                }
                val services = arrivalResult.getOrNull()
                if (services.isNullOrEmpty()) {
                    addStopError = "No bus services found at this stop."
                    addStopIsLoading = false
                    return@launch
                }

                val result = try {
                    repository.addBusStop(BusStop(code = formattedCode, name = name))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    addStopError = "Failed to save bus stop (${e.message ?: "unknown error"})."
                    addStopIsLoading = false
                    return@launch
                }
                if (result.isFailure) {
                    if (result.exceptionOrNull() is DuplicateStopException) {
                        addStopError = "Bus stop already exists"
                    } else {
                        addStopError = result.exceptionOrNull()?.message ?: "Failed to add stop"
                    }
                    addStopIsLoading = false
                    return@launch
                }
                addStopIsLoading = false
                hideAddStopDialog()
            } else {
                addStopError = "Invalid bus stop code"
            }
        }
    }

    fun removeBusStop(code: String) {
        viewModelScope.launch {
            repository.removeBusStop(code)
        }
    }

    private suspend fun refreshArrivalsInternal(code: String, isAutoRefresh: Boolean) {
        val index = _savedStops.value.indexOfFirst { it.busStop.code == code }
        if (index == -1) return

        if (!isAutoRefresh) {
            _savedStops.value = _savedStops.value.toMutableList().apply {
                this[index] = this[index].copy(isLoading = true, error = null, isOffline = false)
            }
        }

        val result = try {
            repository.getBusArrivals(code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Ensure loading state is cleared even on unexpected exceptions
            if (!isAutoRefresh) {
                _savedStops.value = _savedStops.value.toMutableList().apply {
                    this[index] = this[index].copy(isLoading = false, error = e.message)
                }
            }
            return
        }

        val isOffline = result.exceptionOrNull() is IOException

        _savedStops.value = _savedStops.value.toMutableList().apply {
            this[index] = this[index].copy(
                services = result.getOrNull() ?: this[index].services,
                isLoading = false,
                error = if (isOffline || isAutoRefresh) null else result.exceptionOrNull()?.message,
                isOffline = isOffline,
                lastUpdated = if (result.isSuccess) System.currentTimeMillis() else this[index].lastUpdated
            )
        }
        if (result.isSuccess) {
            lastUpdatedAll = System.currentTimeMillis()
        }
    }

    fun refreshArrivals(code: String, isAutoRefresh: Boolean = false) {
        val now = System.currentTimeMillis()
        val lastRefresh = lastRefreshTimestamps[code] ?: 0L
        if (!isAutoRefresh && now - lastRefresh < refreshCooldownMs) return
        lastRefreshTimestamps[code] = now

        viewModelScope.launch {
            refreshArrivalsInternal(code, isAutoRefresh)
        }
    }

    fun refreshAll(isAutoRefresh: Boolean = false) {
        if (isAutoRefresh) {
            viewModelScope.launch {
                _savedStops.value.map { stop ->
                    async {
                        refreshArrivalsInternal(stop.busStop.code, isAutoRefresh)
                    }
                }.awaitAll()
            }
            return
        }
        // Manual refresh: always show visual feedback, skip only the API call if in cooldown
        isRefreshing = true
        lastUpdatedAll = System.currentTimeMillis() // pulse the timer
        viewModelScope.launch {
            _savedStops.value.map { stop ->
                async {
                    val now = System.currentTimeMillis()
                    val lastRefresh = lastRefreshTimestamps[stop.busStop.code] ?: 0L
                    if (now - lastRefresh < refreshCooldownMs) return@async
                    lastRefreshTimestamps[stop.busStop.code] = now
                    refreshArrivalsInternal(stop.busStop.code, isAutoRefresh)
                }
            }.awaitAll()
            delay(400)
            isRefreshing = false
        }
    }

    fun setAutoRefreshInterval(seconds: Int) {
        autoRefreshIntervalSeconds = seconds
        viewModelScope.launch {
            repository.setAutoRefreshInterval(seconds)
        }
        if (seconds > 0) {
            startAutoRefresh()
        } else {
            stopAutoRefresh()
        }
    }

    fun toggleThemeMode() {
        themeMode = (themeMode + 1) % 3
        viewModelScope.launch {
            repository.setThemeMode(themeMode)
        }
    }

    val themeIcon: String get() = when (themeMode) {
        1 -> "light"
        2 -> "dark"
        else -> "system"
    }

    fun toggleSortOrder() {
        _sortByEarliest.value = !_sortByEarliest.value
    }

    fun toggleCollapse(code: String) {
        val index = _savedStops.value.indexOfFirst { it.busStop.code == code }
        if (index != -1) {
            val newCollapsed = !_savedStops.value[index].isCollapsed
            _savedStops.value = _savedStops.value.toMutableList().apply {
                this[index] = this[index].copy(isCollapsed = newCollapsed)
            }
            // Persist collapse state
            viewModelScope.launch {
                val collapsedCodes = _savedStops.value
                    .filter { it.isCollapsed }
                    .map { it.busStop.code }
                repository.setCollapsedStops(collapsedCodes)
            }
        }
    }

    fun togglePin(code: String) {
        val index = _savedStops.value.indexOfFirst { it.busStop.code == code }
        if (index != -1) {
            val wasPinned = _savedStops.value[index].isPinned
            _savedStops.value = _savedStops.value.toMutableList().apply {
                this[index] = this[index].copy(isPinned = !this[index].isPinned)
            }.let { list ->
                val pinned = list.filter { it.isPinned }
                val unpinned = list.filter { !it.isPinned }
                if (wasPinned) {
                    // Unpinning: restore to addition order among unpinned stops
                    val order = additionOrder.filter { code2 ->
                        unpinned.any { it.busStop.code == code2 }
                    }
                    val sorted = order.mapNotNull { code2 ->
                        unpinned.find { it.busStop.code == code2 }
                    }
                    pinned + sorted
                } else {
                    // Pinning: move to top, leave rest in their current order
                    pinned + unpinned
                }
            }
        }
    }

    private fun startAutoRefresh() {
        stopAutoRefresh()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(autoRefreshIntervalSeconds * 1000L)
                refreshAll(isAutoRefresh = true)
            }
        }
    }

    private fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }

    class Factory(
        private val repository: BusRepository,
        private val busStopIndex: BusStopIndex
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository, busStopIndex) as T
        }
    }
}