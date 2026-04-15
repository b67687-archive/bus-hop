package com.bushop.sg.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bushop.sg.data.local.DuplicateStopException
import com.bushop.sg.data.model.BusService
import com.bushop.sg.data.model.BusStop
import com.bushop.sg.data.repository.BusRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.net.UnknownHostException

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

class MainViewModel(private val repository: BusRepository) : ViewModel() {

    private val _savedStops = MutableStateFlow<List<BusStopWithArrivals>>(emptyList())
    val savedStops: StateFlow<List<BusStopWithArrivals>> = _savedStops.asStateFlow()

    var addStopDialogVisible by mutableStateOf(false)
        private set
    
    var addStopError by mutableStateOf<String?>(null)
        private set

    private var autoRefreshJob: Job? = null
    var autoRefreshIntervalSeconds by mutableStateOf(30)
        private set
    
    var sortByEarliest by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            combine(
                repository.savedBusStops,
                repository.cachedBusServices
            ) { stops, cached ->
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
                        isCollapsed = existing?.isCollapsed ?: false,
                        isPinned = existing?.isPinned ?: false
                    )
                }
            }.collect { list ->
                _savedStops.value = list
                if (list.isNotEmpty() && list.none { it.services.isNotEmpty() }) {
                    refreshAll()
                }
            }
        }
        
        viewModelScope.launch {
            autoRefreshIntervalSeconds = repository.getAutoRefreshInterval()
            if (autoRefreshIntervalSeconds > 0) {
                startAutoRefresh()
            }
        }
    }

    fun showAddStopDialog() {
        addStopError = null
        addStopDialogVisible = true
    }

    fun hideAddStopDialog() {
        addStopDialogVisible = false
        addStopError = null
    }

    fun addBusStop(code: String, name: String = "") {
        viewModelScope.launch {
            val formattedCode = code.trim()
            if (formattedCode.length == 5 && formattedCode.all { it.isDigit() }) {
                val result = repository.addBusStop(BusStop(code = formattedCode, name = name))
                if (result.isFailure) {
                    if (result.exceptionOrNull() is DuplicateStopException) {
                        addStopError = "Bus stop already exists"
                    } else {
                        addStopError = result.exceptionOrNull()?.message
                    }
                    return@launch
                }
                hideAddStopDialog()
                delay(300)
                refreshArrivals(formattedCode)
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

    fun refreshArrivals(code: String) {
        viewModelScope.launch {
            val index = _savedStops.value.indexOfFirst { it.busStop.code == code }
            if (index != -1) {
                _savedStops.value = _savedStops.value.toMutableList().apply {
                    this[index] = this[index].copy(isLoading = true, error = null, isOffline = false)
                }

                val result = repository.getBusArrivals(code)
                val isOffline = result.exceptionOrNull() is UnknownHostException ||
                        result.exceptionOrNull()?.cause is UnknownHostException

                _savedStops.value = _savedStops.value.toMutableList().apply {
                    this[index] = this[index].copy(
                        services = result.getOrNull() ?: this[index].services,
                        isLoading = false,
                        error = if (isOffline) null else result.exceptionOrNull()?.message,
                        isOffline = isOffline,
                        lastUpdated = if (result.isSuccess) System.currentTimeMillis() else this[index].lastUpdated
                    )
                }
            }
        }
    }

    fun refreshAll() {
        _savedStops.value.forEach { stop ->
            refreshArrivals(stop.busStop.code)
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

    fun toggleSortOrder() {
        sortByEarliest = !sortByEarliest
        val currentList = _savedStops.value.toList()
        
        _savedStops.value = if (sortByEarliest) {
            currentList.sortedBy { stop ->
                val nextInfo = stop.services.firstOrNull()?.next
                when {
                    nextInfo == null -> Long.MAX_VALUE
                    nextInfo.durationMs < 60000 -> 0L
                    else -> nextInfo.durationMs
                }
            }
        } else {
            currentList.sortedBy { it.busStop.code }
        }
        
        val sortedCodes = _savedStops.value.map { it.busStop.code }
        viewModelScope.launch {
            sortedCodes.forEach { code ->
                refreshArrivals(code)
            }
        }
    }

    fun toggleCollapse(code: String) {
        val index = _savedStops.value.indexOfFirst { it.busStop.code == code }
        if (index != -1) {
            _savedStops.value = _savedStops.value.toMutableList().apply {
                this[index] = this[index].copy(isCollapsed = !this[index].isCollapsed)
            }
        }
    }

    fun togglePin(code: String) {
        val index = _savedStops.value.indexOfFirst { it.busStop.code == code }
        if (index != -1) {
            _savedStops.value = _savedStops.value.toMutableList().apply {
                this[index] = this[index].copy(isPinned = !this[index].isPinned)
            }
        }
    }

    private fun startAutoRefresh() {
        stopAutoRefresh()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(autoRefreshIntervalSeconds * 1000L)
                refreshAll()
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

    class Factory(private val repository: BusRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}