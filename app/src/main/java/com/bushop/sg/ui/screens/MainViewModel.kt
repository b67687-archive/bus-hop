package com.bushop.sg.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bushop.sg.data.local.BusStopEntry
import com.bushop.sg.data.local.BusStopIndex
import com.bushop.sg.domain.model.DuplicateStopException
import com.bushop.sg.domain.model.BusStop
import com.bushop.sg.domain.model.BusStopWithArrivals
import com.bushop.sg.domain.model.NetworkResult
import com.bushop.sg.domain.model.ThemeMode
import com.bushop.sg.domain.repository.BusRepository
import com.bushop.sg.domain.usecase.AutoRefreshController
import com.bushop.sg.domain.usecase.BusStopUseCase
import com.bushop.sg.domain.usecase.StopRefreshCoordinator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

/** Tracks the health of the external bus arrival API. */
enum class ApiStatus { Healthy, Degraded, Down }

class MainViewModel(
    private val repository: BusRepository,
    private val busStopIndex: BusStopIndex,
    private val useCase: BusStopUseCase = BusStopUseCase(),
    private val refreshCoordinator: StopRefreshCoordinator = StopRefreshCoordinator()
) : ViewModel() {

    private var isAutoRefreshing = false

    private val autoRefreshController = AutoRefreshController(viewModelScope)

    private val _pinnedServices = MutableStateFlow<Set<String>>(emptySet())
    val pinnedServices: StateFlow<Set<String>> = _pinnedServices.asStateFlow()

    private val _savedStops = MutableStateFlow<List<BusStopWithArrivals>>(emptyList())
    val savedStops: StateFlow<List<BusStopWithArrivals>> = _savedStops.asStateFlow()

    var addStopDialogVisible by mutableStateOf(false)
        private set
    
    var addStopError by mutableStateOf<String?>(null)
        private set

    private var saveCollapseJob: Job? = null
    var autoRefreshIntervalSeconds by mutableStateOf(30)
        private set
    
    private val _sortByEarliest = MutableStateFlow(false)
    val sortByEarliest: StateFlow<Boolean> = _sortByEarliest.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    var addStopIsLoading by mutableStateOf(false)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    var lastUpdatedAll by mutableStateOf(0L)
        private set

    // ── API health tracking ──

    private val _apiStatus = MutableStateFlow(ApiStatus.Healthy)
    val apiStatus: StateFlow<ApiStatus> = _apiStatus.asStateFlow()

    private var consecutiveFailures = 0
        private set

    fun dismissApiBanner() {
        _apiStatus.value = ApiStatus.Healthy
    }

    // ── Theme ──

    private val _themeModeFlow = MutableStateFlow(ThemeMode.SYSTEM)
    val themeModeFlow: StateFlow<ThemeMode> = _themeModeFlow.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeModeFlow.value = mode
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    // ── Index readiness ──

    val isIndexReady: StateFlow<Boolean> = busStopIndex.isReady

    private val _searchResults = MutableStateFlow<List<BusStopEntry>>(emptyList())
    val searchResults: StateFlow<List<BusStopEntry>> = _searchResults.asStateFlow()

    // Reference addition order from repository (used to restore position on unpin)
    private var additionOrder: List<String> = emptyList()

    init {
        // Restore persisted preferences
        viewModelScope.launch {
            repository.themeModeFlow.collect { mode ->
                _themeModeFlow.value = mode
            }
        }
        viewModelScope.launch {
            autoRefreshIntervalSeconds = repository.getAutoRefreshIntervalOnce()
            if (autoRefreshIntervalSeconds > 0) {
                autoRefreshController.start(autoRefreshIntervalSeconds) { refreshAll(isAutoRefresh = true) }
            }
        }
        viewModelScope.launch {
            repository.pinnedServicesFlow.collect { pinned ->
                _pinnedServices.value = pinned
            }
        }

        viewModelScope.launch {
            val baseFlow = combine(
                repository.savedBusStops,
                repository.cachedBusServices,
                repository.cachedTimestamps,
                repository.collapsedStopsFlow,
                _sortByEarliest
            ) { stops, cached, timestamps, collapsedStops, sortByEarliest ->
                stops.map { stop ->
                    val cachedServices = cached[stop.code] ?: emptyList()
                    val existing = _savedStops.value.find { it.busStop.code == stop.code }
                    BusStopWithArrivals(
                        busStop = stop,
                        services = cachedServices,
                        isLoading = existing?.isLoading ?: false,
                        error = existing?.error,
                        isOffline = existing?.isOffline ?: false,
                        lastUpdated = existing?.lastUpdated ?: 0L,
                        cachedAt = timestamps[stop.code] ?: 0L,
                        isCollapsed = existing?.isCollapsed ?: collapsedStops.contains(stop.code),
                        isPinned = existing?.isPinned ?: false
                    )
                } to sortByEarliest
            }
            combine(baseFlow, _pinnedServices) { (stops, sortByEarliest), pinned ->
                stops.map { stopWithArrivals ->
                    val pinnedForStop = pinned
                        .filter { it.startsWith("${stopWithArrivals.busStop.code}:") }
                        .map { it.substringAfter(":") }.toSet()
                    stopWithArrivals.copy(
                        services = useCase.sortServicesWithPins(
                            stopWithArrivals.services, pinnedForStop, sortByEarliest
                        )
                    )
                }
            }.collect { list ->
                additionOrder = list.map { it.busStop.code }
                lastUpdatedAll = list.maxOfOrNull { it.lastUpdated } ?: lastUpdatedAll
                val pinnedFirst = list.sortedByDescending { it.isPinned }
                _savedStops.value = pinnedFirst
                if (pinnedFirst.isNotEmpty() && !isAutoRefreshing && pinnedFirst.any { it.services.isEmpty() || it.isStale }) {
                    refreshAll(isAutoRefresh = true)
                }
            }
        }
    }

    fun showAddStopDialog() {
        addStopError = null
        addStopDialogVisible = true
    }

    fun searchBusStops(query: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val results = busStopIndex.search(query)
            _searchResults.value = results
        }
    }

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
                when (val arrivalResult = repository.getBusArrivals(formattedCode)) {
                    is NetworkResult.Error -> {
                        addStopError = "Could not verify bus stop (${arrivalResult.message})."
                        addStopIsLoading = false
                        return@launch
                    }
            is NetworkResult.Success -> {
                consecutiveFailures = 0
                _apiStatus.value = ApiStatus.Healthy
                        if (arrivalResult.data.isEmpty()) {
                            addStopError = "No bus services found at this stop."
                            addStopIsLoading = false
                            return@launch
                        }
                    }
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
        // Set loading state before API call (for manual refresh only)
        if (!isAutoRefresh) {
            val idx = _savedStops.value.indexOfFirst { it.busStop.code == code }
            if (idx == -1) return
            _savedStops.value = _savedStops.value.toMutableList().apply {
                this[idx] = this[idx].copy(isLoading = true, error = null, isOffline = false)
            }
        }

        val result = repository.getBusArrivals(code)

        // Re-compute index — list may have changed during suspension
        val index = _savedStops.value.indexOfFirst { it.busStop.code == code }
        if (index == -1) return

        when (result) {
            is NetworkResult.Error -> {
                val isOffline = result.exception is IOException
                consecutiveFailures++
                _apiStatus.value = when {
                    consecutiveFailures >= 10 -> ApiStatus.Down
                    consecutiveFailures >= 3 -> ApiStatus.Degraded
                    else -> _apiStatus.value
                }
                if (!isAutoRefresh) {
                    _savedStops.value = _savedStops.value.toMutableList().apply {
                        this[index] = this[index].copy(
                            isLoading = false,
                            error = if (isOffline) null else result.message,
                            isOffline = isOffline
                        )
                    }
                } else {
                    _savedStops.value = _savedStops.value.toMutableList().apply {
                        this[index] = this[index].copy(isLoading = false)
                    }
                }
            }
            is NetworkResult.Success -> {
                consecutiveFailures = 0
                _apiStatus.value = ApiStatus.Healthy
                val pinnedForStop = _pinnedServices.value
                    .filter { it.startsWith("${code}:") }
                    .map { it.substringAfter(":") }.toSet()
                val sortedServices = useCase.sortServicesWithPins(result.data, pinnedForStop, _sortByEarliest.value)
                _savedStops.value = _savedStops.value.toMutableList().apply {
                    this[index] = this[index].copy(
                        services = sortedServices,
                        isLoading = false,
                        error = null,
                        isOffline = false,
                        lastUpdated = System.currentTimeMillis()
                    )
                }
                lastUpdatedAll = System.currentTimeMillis()
            }
        }
    }

    fun refreshArrivals(code: String, isAutoRefresh: Boolean = false) {
        viewModelScope.launch {
            if (refreshCoordinator.tryRefresh(code, isAutoRefresh)) {
                refreshArrivalsInternal(code, isAutoRefresh)
            }
        }
    }

    fun refreshAll(isAutoRefresh: Boolean = false) {
        if (isAutoRefresh) {
            if (isAutoRefreshing) return
            isAutoRefreshing = true
            viewModelScope.launch {
                try {
                    refreshCoordinator.refreshAllConcurrent(
                        codes = _savedStops.value.map { it.busStop.code },
                        isAutoRefresh = true,
                        refreshBlock = { refreshArrivalsInternal(it, true) }
                    )
                } finally {
                    isAutoRefreshing = false
                }
            }
            return
        }
        // Manual refresh: always show visual feedback, skip only the API call if in cooldown
        isRefreshing = true
        lastUpdatedAll = System.currentTimeMillis()
        viewModelScope.launch {
            try {
                refreshCoordinator.refreshAllConcurrent(
                    codes = _savedStops.value.map { it.busStop.code },
                    isAutoRefresh = false,
                    refreshBlock = { refreshArrivalsInternal(it, false) }
                )
                delay(400)
            } finally {
                isRefreshing = false
            }
        }
    }

    fun setAutoRefreshInterval(seconds: Int) {
        autoRefreshIntervalSeconds = seconds
        viewModelScope.launch {
            repository.setAutoRefreshInterval(seconds)
        }
        if (seconds > 0) {
            autoRefreshController.start(seconds) { refreshAll(isAutoRefresh = true) }
        } else {
            autoRefreshController.stop()
        }
    }

    fun toggleThemeMode() {
        setThemeMode(when (_themeModeFlow.value) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        })
    }

    fun toggleSortOrder() {
        _sortByEarliest.value = !_sortByEarliest.value
    }

    fun toggleCollapse(code: String) {
        val (updated, collapsedCodes) = useCase.toggleCollapsed(_savedStops.value, code)
        _savedStops.value = updated
        // Debounce collapse state persistence (500ms)
        saveCollapseJob?.cancel()
        saveCollapseJob = viewModelScope.launch {
            delay(500)
            repository.setCollapsedStops(collapsedCodes.toSet())
        }
    }

    fun togglePinService(stopCode: String, serviceNo: String) {
        val key = "$stopCode:$serviceNo"
        val updated = _pinnedServices.value.toMutableSet().apply {
            if (contains(key)) remove(key) else add(key)
        }
        _pinnedServices.value = updated
        viewModelScope.launch {
            repository.savePinnedServices(updated)
        }
    }

    fun isServicePinned(stopCode: String, serviceNo: String): Boolean =
        "$stopCode:$serviceNo" in _pinnedServices.value

    fun togglePin(code: String) {
        val index = _savedStops.value.indexOfFirst { it.busStop.code == code }
        if (index != -1) {
            val wasPinned = _savedStops.value[index].isPinned
            _savedStops.value = _savedStops.value.toMutableList().apply {
                this[index] = this[index].copy(isPinned = !this[index].isPinned)
            }.let { list ->
                useCase.applyPinning(list, wasPinned, additionOrder)
            }
        }
    }

    // ── Lifecycle-aware auto-refresh ──

    fun pauseAutoRefresh() {
        autoRefreshController.stop()
    }

    fun resumeAutoRefresh() {
        if (autoRefreshIntervalSeconds > 0) {
            autoRefreshController.start(autoRefreshIntervalSeconds) { refreshAll(isAutoRefresh = true) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshController.onCleared()
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