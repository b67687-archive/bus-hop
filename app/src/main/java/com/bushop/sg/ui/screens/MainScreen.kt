package com.bushop.sg.ui.screens


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bushop.sg.data.local.BusStopEntry
import com.bushop.sg.ui.components.AddBusStopDialog
import com.bushop.sg.ui.components.BusStopCard

private val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")

private fun formatLastUpdated(timestamp: Long): String {
    val zdt = java.time.ZonedDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(timestamp),
        java.time.ZoneId.systemDefault()
    )
    return timeFormatter.format(zdt)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val savedStops by viewModel.savedStops.collectAsState()
    val sortByEarliest by viewModel.sortByEarliest.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                scrollBehavior = scrollBehavior,
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BusHop",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleSortOrder() }) {
                        Icon(
                            imageVector = if (sortByEarliest) Icons.AutoMirrored.Filled.Sort else Icons.AutoMirrored.Outlined.Sort,
                            contentDescription = "Sort",
                            tint = if (sortByEarliest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.toggleThemeMode() }) {
                        Icon(
                            imageVector = when (viewModel.themeMode) {
                                0 -> Icons.Default.BrightnessAuto
                                1 -> Icons.Default.LightMode
                                2 -> Icons.Default.DarkMode
                                else -> Icons.Default.BrightnessAuto
                            },
                            contentDescription = when (viewModel.themeMode) {
                                0 -> "Auto theme"
                                1 -> "Light mode"
                                2 -> "Dark mode"
                                else -> "Auto theme"
                            },
                            tint = when (viewModel.themeMode) {
                                0 -> MaterialTheme.colorScheme.onSurfaceVariant
                                1 -> MaterialTheme.colorScheme.onSurfaceVariant
                                2 -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    IconButton(
                        onClick = { viewModel.refreshAll() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = if (viewModel.isRefreshing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
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
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (savedStops.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp
                    ),
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
                            deleteTarget = stopWithArrivals.busStop.code
                        }
                    )
                }
            }
        }
        if (viewModel.lastUpdatedAll > 0) {
            val pillBg by animateColorAsState(
                targetValue = if (viewModel.isRefreshing)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                animationSpec = tween(durationMillis = 300)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(pillBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Updated: ${formatLastUpdated(viewModel.lastUpdatedAll)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Auto Refresh") },
            text = {
                Column {
                    val intervals = listOf(0 to "Off", 30 to "30s", 60 to "1m", 120 to "2m", 300 to "5m")
                    intervals.forEach { (seconds, label) ->
                        TextButton(
                            onClick = {
                                viewModel.setAutoRefreshInterval(seconds)
                                showSettings = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, modifier = Modifier.weight(1f))
                            if (viewModel.autoRefreshIntervalSeconds == seconds) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) { Text("Done") }
            }
        )
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
                val target = deleteTarget ?: return@AlertDialog
                if (isPinned) {
                    TextButton(
                        onClick = {
                            viewModel.togglePin(target)
                            deleteTarget = null
                        }
                    ) {
                        Text("Unpin")
                    }
                } else {
                    TextButton(
                        onClick = {
                            viewModel.removeBusStop(target)
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