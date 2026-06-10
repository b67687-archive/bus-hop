package com.bushop.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bushop.BuildConfig
import com.bushop.data.api.UpdateChecker
import com.bushop.data.api.UpdateInfo
import com.bushop.data.local.BusStopEntry
import com.bushop.data.local.BusStopIndex
import com.bushop.domain.model.BusStop
import com.bushop.domain.model.BusStopWithArrivals
import com.bushop.domain.model.ColorSchemeOption
import com.bushop.domain.model.DuplicateStopException
import com.bushop.domain.model.NetworkResult
import com.bushop.domain.model.ThemeMode
import com.bushop.domain.repository.BusRepository
import com.bushop.domain.usecase.AutoRefreshController
import com.bushop.domain.usecase.BusStopUseCase
import com.bushop.domain.usecase.StopRefreshCoordinator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

/** Tracks the health of the external bus arrival API. */
enum class ApiStatus { Healthy, Degraded, Down }

class MainViewModel(
    application: android.app.Application,
    private val repository: BusRepository,
    private val busStopIndex: BusStopIndex,
    private val useCase: BusStopUseCase = BusStopUseCase(),
    private val refreshCoordinator: StopRefreshCoordinator = StopRefreshCoordinator(),
) : AndroidViewModel(application) {
    companion object {
        private const val DEFAULT_AUTO_REFRESH_INTERVAL = 30
        private const val COLLAPSE_DEBOUNCE_MS = 500L
        private const val PREFS_NAME = "bushop_prefs"
        private const val DEGRADED_THRESHOLD = 3
        private const val DOWN_THRESHOLD = 10
    }

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
    var autoRefreshIntervalSeconds by mutableStateOf(DEFAULT_AUTO_REFRESH_INTERVAL)
        private set

    private val _sortByEarliest = MutableStateFlow(false)
    val sortByEarliest: StateFlow<Boolean> = _sortByEarliest.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 10)
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

    // ── Colour scheme ──

    private val _colorSchemeOptionFlow = MutableStateFlow(ColorSchemeOption.BLUE)
    val colorSchemeOptionFlow: StateFlow<ColorSchemeOption> = _colorSchemeOptionFlow.asStateFlow()

    fun setColorSchemeOption(option: ColorSchemeOption) {
        _colorSchemeOptionFlow.value = option
        viewModelScope.launch {
            repository.setColorSchemeOption(option)
        }
    }

    // ── Index readiness ──

    val isIndexReady: StateFlow<Boolean> = busStopIndex.isReady

    private val _searchResults = MutableStateFlow<List<BusStopEntry>>(emptyList())
    val searchResults: StateFlow<List<BusStopEntry>> = _searchResults.asStateFlow()

    private var additionOrder: List<String> = emptyList()

    // ── Nearby stops ──

    var nearbyStops by mutableStateOf<List<BusStopEntry>>(emptyList())
        private set
    var isLoadingNearby by mutableStateOf(false)
        private set
    var nearbyError by mutableStateOf<String?>(null)
        private set

    fun clearNearby() {
        nearbyStops = emptyList()
        nearbyError = null
    }

    fun findNearbyStops() {
        if (isLoadingNearby) return
        isLoadingNearby = true
        nearbyError = null
        viewModelScope.launch {
            try {
                val ctx = getApplication<android.app.Application>()
                val lm = ctx.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
                val providers = lm?.getProviders(true) ?: emptyList()
                if (providers.isEmpty()) {
                    nearbyError = "Location is disabled. Enable GPS or Wi-Fi scanning."
                    return@launch
                }
                val location = providers.firstNotNullOfOrNull { lm?.getLastKnownLocation(it) }
                if (location == null) {
                    nearbyError = "Could not get current location. Try again later."
                    return@launch
                }
                nearbyStops = busStopIndex.findNearby(location.latitude, location.longitude)
                if (nearbyStops.isEmpty()) {
                    nearbyError = "No bus stops found nearby."
                }
            } catch (e: SecurityException) {
                nearbyError = "Location permission denied."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                nearbyError = "Error: ${e.message}"
            } finally {
                isLoadingNearby = false
            }
        }
    }

    // ── Update checker ──

    var updateInfo by mutableStateOf<UpdateInfo?>(null)
        private set
    var isCheckingUpdate by mutableStateOf(false)
        private set
    var isDownloadingUpdate by mutableStateOf(false)
    var hasSeenDragHint by mutableStateOf(false)
        private set

    private fun loadHintPref() {
        val prefs =
            getApplication<android.app.Application>()
                .getSharedPreferences(PREFS_NAME, 0)
        hasSeenDragHint = prefs.getBoolean("has_seen_hint", false)
    }

    fun dismissHint() {
        hasSeenDragHint = true
        getApplication<android.app.Application>()
            .getSharedPreferences(PREFS_NAME, 0)
            .edit()
            .putBoolean("has_seen_hint", true)
            .apply()
    }

    private val updateChecker = UpdateChecker()

    fun checkForUpdate() {
        if (isCheckingUpdate) return
        isCheckingUpdate = true
        viewModelScope.launch {
            try {
                val info = updateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
                updateInfo = info
                if (info != null && info.hasUpdate) {
                    _snackbarMessage.tryEmit("Update v${info.latestVersion} available")
                }
            } finally {
                isCheckingUpdate = false
            }
        }
    }

    /** Download the latest APK and launch the install intent via FileProvider. */
    fun downloadAndInstallUpdate() {
        val info = updateInfo ?: return
        if (isDownloadingUpdate) return
        isDownloadingUpdate = true
        viewModelScope.launch {
            try {
                val updatesDir = File(getApplication<android.app.Application>().cacheDir, "updates").also { it.mkdirs() }
                val targetFile = File(updatesDir, "bus-hop-update.apk")
                val success = updateChecker.downloadApk(info.downloadUrl, targetFile)
                if (success) {
                    val apkUri =
                        androidx.core.content.FileProvider.getUriForFile(
                            getApplication(),
                            "${getApplication<android.app.Application>().packageName}.fileprovider",
                            targetFile,
                        )
                    val intent =
                        android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(apkUri, "application/vnd.android.package-archive")
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                    getApplication<android.app.Application>().startActivity(intent)
                    _snackbarMessage.tryEmit("Installing v${info.latestVersion}…")
                } else {
                    _snackbarMessage.tryEmit("Download failed")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.tryEmit("Install failed: ${e.message}")
            } finally {
                isDownloadingUpdate = false
            }
        }
    }

    init {
        loadHintPref()
        // Restore persisted preferences
        viewModelScope.launch {
            repository.themeModeFlow.collect { mode ->
                _themeModeFlow.value = mode
            }
        }
        viewModelScope.launch {
            repository.colorSchemeOptionFlow.collect { option ->
                _colorSchemeOptionFlow.value = option
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
            repository.sortByEarliestFlow.collect { enabled ->
                _sortByEarliest.value = enabled
            }
        }

        viewModelScope.launch {
            val baseFlow =
                combine(
                    repository.savedBusStops,
                    repository.cachedBusServices,
                    repository.cachedTimestamps,
                    repository.collapsedStopsFlow,
                    _sortByEarliest,
                ) { stops, cached, timestamps, collapsedStops, sortByEarliest ->
                    val mergedStops =
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
                                isCollapsed = existing?.isCollapsed ?: false,
                                isPinned = existing?.isPinned ?: false,
                            )
                        }
                    useCase.applyPersistedCollapsedState(mergedStops, collapsedStops) to sortByEarliest
                }
            combine(baseFlow, _pinnedServices) { (stops, sortByEarliest), pinned ->
                stops.map { stopWithArrivals ->
                    val pinnedForStop = pinnedServiceNosForStop(stopWithArrivals.busStop.code)
                    stopWithArrivals.copy(
                        services =
                            useCase.sortServicesWithPins(
                                stopWithArrivals.services,
                                pinnedForStop,
                                sortByEarliest,
                            ),
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

    fun addBusStop(
        code: String,
        name: String = "",
    ) {
        // Guard against rapid double-taps
        if (addStopIsLoading) return

        viewModelScope.launch {
            val formattedCode = code.trim()
            if (formattedCode.length == 5 && formattedCode.all { it.isDigit() }) {
                addStopIsLoading = true
                addStopError = null

                // Validate stop exists by fetching arrivals first
                when (val arrivalResult = getBusArrivalsSafely(formattedCode)) {
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

                val result =
                    try {
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
                collapseStop(formattedCode)
                addStopIsLoading = false
                hideAddStopDialog()
            } else {
                addStopError = "Invalid bus stop code"
            }
        }
    }

    private suspend fun getBusArrivalsSafely(code: String): NetworkResult<List<com.bushop.domain.model.BusService>> =
        try {
            repository.getBusArrivals(code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Unexpected error", e)
        }

    fun removeBusStop(code: String) {
        // Update state immediately (don't wait for DataStore flow)
        _savedStops.value = _savedStops.value.filter { it.busStop.code != code }
        viewModelScope.launch {
            repository.removeBusStop(code)
        }
    }

    /** Multi-position move (used for final drop in free-form drag). */
    fun moveStop(
        code: String,
        delta: Int,
    ) {
        if (delta == 0) return
        val list = _savedStops.value.toMutableList()
        val fromIdx = list.indexOfFirst { it.busStop.code == code }
        if (fromIdx == -1) return
        val item = list.removeAt(fromIdx)
        val toIdx = (fromIdx + delta).coerceIn(0, list.size)
        if (fromIdx == toIdx) return
        list.add(toIdx, item)
        _savedStops.value = list
        additionOrder = list.map { it.busStop.code }
        viewModelScope.launch { repository.reorderStops(list.map { it.busStop }) }
    }

    private suspend fun refreshArrivalsInternal(
        code: String,
        isAutoRefresh: Boolean,
    ) {
        // Set loading state before API call (for manual refresh only)
        if (!isAutoRefresh) {
            val idx = _savedStops.value.indexOfFirst { it.busStop.code == code }
            if (idx == -1) return
            _savedStops.value =
                _savedStops.value.toMutableList().apply {
                    // Creates a new list + copies one element — acceptable for infrequent mutations
                    this[idx] = this[idx].copy(isLoading = true, error = null, isOffline = false)
                }
        }

        val result = getBusArrivalsSafely(code)

        // Re-compute index — list may have changed during suspension
        val index = _savedStops.value.indexOfFirst { it.busStop.code == code }
        if (index == -1) return

        when (result) {
            is NetworkResult.Error -> {
                val isOffline = result.exception is IOException
                consecutiveFailures++
                _apiStatus.value =
                    when {
                        consecutiveFailures >= DOWN_THRESHOLD -> ApiStatus.Down
                        consecutiveFailures >= DEGRADED_THRESHOLD -> ApiStatus.Degraded
                        else -> _apiStatus.value
                    }
                if (!isAutoRefresh) {
                    _savedStops.value =
                        _savedStops.value.toMutableList().apply {
                            this[index] =
                                this[index].copy(
                                    isLoading = false,
                                    error = if (isOffline) null else result.message,
                                    isOffline = isOffline,
                                )
                        }
                } else {
                    _savedStops.value =
                        _savedStops.value.toMutableList().apply {
                            this[index] = this[index].copy(isLoading = false)
                        }
                }
            }

            is NetworkResult.Success -> {
                consecutiveFailures = 0
                _apiStatus.value = ApiStatus.Healthy
                val pinnedForStop = pinnedServiceNosForStop(code)
                val sortedServices = useCase.sortServicesWithPins(result.data, pinnedForStop, _sortByEarliest.value)
                _savedStops.value =
                    _savedStops.value.toMutableList().apply {
                        this[index] =
                            this[index].copy(
                                services = sortedServices,
                                isLoading = false,
                                error = null,
                                isOffline = false,
                                lastUpdated = System.currentTimeMillis(),
                            )
                    }
                lastUpdatedAll = System.currentTimeMillis()
            }
        }
    }

    fun refreshArrivals(
        code: String,
        isAutoRefresh: Boolean = false,
    ) {
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
                        refreshBlock = { refreshArrivalsInternal(it, true) },
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
                    refreshBlock = { refreshArrivalsInternal(it, false) },
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
        setThemeMode(
            when (_themeModeFlow.value) {
                ThemeMode.SYSTEM -> ThemeMode.LIGHT
                ThemeMode.LIGHT -> ThemeMode.DARK
                ThemeMode.DARK -> ThemeMode.SYSTEM
            },
        )
    }

    fun toggleSortOrder() {
        val newValue = !_sortByEarliest.value
        _sortByEarliest.value = newValue
        viewModelScope.launch {
            repository.setSortByEarliest(newValue)
        }
    }

    fun toggleCollapse(code: String) {
        val (updated, collapsedCodes) = useCase.toggleCollapsed(_savedStops.value, code)
        _savedStops.value = updated
        persistCollapsedStops(collapsedCodes.toSet())
    }

    private fun collapseStop(code: String) {
        val (updated, collapsedCodes) = useCase.collapseStop(_savedStops.value, code)
        _savedStops.value = updated
        persistCollapsedStops(collapsedCodes)
    }

    private fun persistCollapsedStops(collapsedCodes: Set<String>) {
        // Debounce collapse state persistence (500ms)
        saveCollapseJob?.cancel()
        saveCollapseJob =
            viewModelScope.launch {
                delay(COLLAPSE_DEBOUNCE_MS)
                repository.setCollapsedStops(collapsedCodes.toSet())
            }
    }

    fun togglePinService(
        stopCode: String,
        serviceNo: String,
    ) {
        val key = "$stopCode:$serviceNo"
        val wasPinned = key in _pinnedServices.value
        val updated =
            _pinnedServices.value.toMutableSet().apply {
                if (contains(key)) remove(key) else add(key)
            }
        _pinnedServices.value = updated
        viewModelScope.launch {
            repository.savePinnedServices(updated)
        }
        _snackbarMessage.tryEmit(
            if (!wasPinned) {
                "Pinned bus $serviceNo"
            } else {
                "Unpinned bus $serviceNo"
            },
        )
    }

    /** Get service numbers pinned for a specific stop (strip the "$code:" prefix). */
    private fun pinnedServiceNosForStop(stopCode: String): Set<String> =
        _pinnedServices.value
            .filter { it.startsWith("$stopCode:") }
            .map { it.substringAfter(":") }
            .toSet()

    fun isServicePinned(
        stopCode: String,
        serviceNo: String,
    ): Boolean = "$stopCode:$serviceNo" in _pinnedServices.value

    fun togglePin(code: String) {
        val index = _savedStops.value.indexOfFirst { it.busStop.code == code }
        if (index != -1) {
            val wasPinned = _savedStops.value[index].isPinned
            _savedStops.value =
                _savedStops.value
                    .toMutableList()
                    .apply {
                        this[index] = this[index].copy(isPinned = !this[index].isPinned)
                    }.let { list ->
                        useCase.applyPinning(list, wasPinned, additionOrder)
                    }
            val stopName =
                _savedStops.value
                    .find { it.busStop.code == code }
                    ?.busStop
                    ?.name ?: code
            _snackbarMessage.tryEmit(
                if (!wasPinned) {
                    "Pinned stop $stopName"
                } else {
                    "Unpinned stop $stopName"
                },
            )
        }
    }

    // ── Lifecycle-aware auto-refresh ──

    fun pauseAutoRefresh() {
        autoRefreshController.stop()
    }

    fun resumeAutoRefresh() {
        // Refresh immediately when app comes to foreground
        refreshAll(isAutoRefresh = true)
        // Then restart the timer for subsequent refreshes
        if (autoRefreshIntervalSeconds > 0) {
            autoRefreshController.start(autoRefreshIntervalSeconds) { refreshAll(isAutoRefresh = true) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshController.onCleared()
    }

    class Factory(
        private val application: android.app.Application,
        private val repository: BusRepository,
        private val busStopIndex: BusStopIndex,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(application, repository, busStopIndex) as T
    }
}
