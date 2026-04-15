package com.bushop.sg.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bushop.sg.data.model.BusService
import com.bushop.sg.data.model.BusStop
import com.bushop.sg.data.repository.BusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.UnknownHostException

data class BusStopWithArrivals(
    val busStop: BusStop,
    val services: List<BusService> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false
)

class MainViewModel(private val repository: BusRepository) : ViewModel() {

    private val _savedStops = MutableStateFlow<List<BusStopWithArrivals>>(emptyList())
    val savedStops: StateFlow<List<BusStopWithArrivals>> = _savedStops.asStateFlow()

    var addStopDialogVisible by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            repository.savedBusStops.collect { stops ->
                val currentStops = _savedStops.value
                val newList = stops.map { stop ->
                    val existing = currentStops.find { it.busStop.code == stop.code }
                    existing?.copy(busStop = stop) ?: BusStopWithArrivals(busStop = stop)
                }
                _savedStops.value = newList
            }
        }
    }

    fun showAddStopDialog() {
        addStopDialogVisible = true
    }

    fun hideAddStopDialog() {
        addStopDialogVisible = false
    }

    fun addBusStop(code: String, name: String = "") {
        viewModelScope.launch {
            val formattedCode = code.trim()
            if (formattedCode.length == 5 && formattedCode.all { it.isDigit() }) {
                repository.addBusStop(BusStop(code = formattedCode, name = name))
            }
            hideAddStopDialog()
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
                        services = result.getOrNull() ?: emptyList(),
                        isLoading = false,
                        error = if (isOffline) null else result.exceptionOrNull()?.message,
                        isOffline = isOffline
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

    class Factory(private val repository: BusRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}