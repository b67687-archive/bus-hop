package com.bushop.sg.ui.screens


import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bushop.sg.data.local.BusStopEntry
import com.bushop.sg.ui.components.AddBusStopDialog
import com.bushop.sg.ui.components.BusStopCard
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val savedStops by viewModel.savedStops.collectAsState()
    val sortByEarliest by viewModel.sortByEarliest.collectAsState()
    val scope = rememberCoroutineScope()
    var showRefreshMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "BusHop",
                        style = MaterialTheme.typography.headlineMedium
                    ) 
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleSortOrder() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort",
                            tint = if (sortByEarliest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.toggleDarkMode() }) {
                        Icon(
                            imageVector = if (viewModel.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle dark mode",
                            tint = if (viewModel.isDarkMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        IconButton(onClick = { showRefreshMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh"
                            )
                        }
                        DropdownMenu(
                            expanded = showRefreshMenu,
                            onDismissRequest = { showRefreshMenu = false }
                        ) {
                            val intervals = listOf(0 to "Off", 30 to "30s", 60 to "1m", 120 to "2m", 300 to "5m")
                            intervals.forEach { (seconds, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.setAutoRefreshInterval(seconds)
                                        showRefreshMenu = false
                                    },
                                    trailingIcon = {
                                        if (viewModel.autoRefreshIntervalSeconds == seconds) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddStopDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add bus stop",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        if (savedStops.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No bus stops saved yet.\nTap + to add your first stop.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = savedStops,
                    key = { it.busStop.code }
                ) { stopWithArrivals ->
                    BusStopCard(
                        busStopCode = stopWithArrivals.busStop.code,
                        busStopName = stopWithArrivals.busStop.name,
                        services = stopWithArrivals.services,
                        isLoading = stopWithArrivals.isLoading,
                        error = stopWithArrivals.error,
                        isOffline = stopWithArrivals.isOffline,
                        lastUpdated = stopWithArrivals.lastUpdated,
                        isCollapsed = stopWithArrivals.isCollapsed,
                        isPinned = stopWithArrivals.isPinned,
                        onRefresh = { viewModel.refreshArrivals(stopWithArrivals.busStop.code) },
                        onToggleCollapse = { viewModel.toggleCollapse(stopWithArrivals.busStop.code) },
                        onTogglePin = { viewModel.togglePin(stopWithArrivals.busStop.code) },
                        onDelete = {
                            if (stopWithArrivals.isPinned) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Unpin the bus stop first")
                                }
                            } else {
                                deleteTarget = stopWithArrivals.busStop.code
                            }
                        },
                        modifier = Modifier.pointerInput(stopWithArrivals.busStop.code) {
                            detectTapGestures(
                                onTap = { viewModel.toggleCollapse(stopWithArrivals.busStop.code) },
                                onLongPress = {
                                    if (stopWithArrivals.isPinned) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Unpin the bus stop first")
                                        }
                                    } else {
                                        deleteTarget = stopWithArrivals.busStop.code
                                    }
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    if (viewModel.addStopDialogVisible) {
        var searchResults by remember { mutableStateOf<List<BusStopEntry>>(emptyList()) }

        AddBusStopDialog(
            error = viewModel.addStopError,
            isLoading = viewModel.addStopIsLoading,
            searchResults = searchResults,
            onSearchQueryChanged = { query ->
                searchResults = viewModel.searchBusStops(query)
            },
            onDismiss = { viewModel.hideAddStopDialog() },
            onConfirm = { code, name ->
                viewModel.addBusStop(code, name)
            }
        )
    }

    if (deleteTarget != null) {
        val targetStop = savedStops.find { it.busStop.code == deleteTarget }
        val isPinned = targetStop?.isPinned == true
        
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(if (isPinned) "Pinned Bus Stop" else "Delete Bus Stop?") },
            text = { 
                Text(if (isPinned) {
                    "Bus stop $deleteTarget is pinned. Unpin first before deleting."
                } else {
                    "Are you sure you want to delete bus stop $deleteTarget? This cannot be undone."
                }) 
            },
            confirmButton = {
                if (isPinned) {
                    TextButton(
                        onClick = {
                            viewModel.togglePin(deleteTarget!!)
                            deleteTarget = null
                        }
                    ) {
                        Text("Unpin")
                    }
                } else {
                    TextButton(
                        onClick = {
                            viewModel.removeBusStop(deleteTarget!!)
                            deleteTarget = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}