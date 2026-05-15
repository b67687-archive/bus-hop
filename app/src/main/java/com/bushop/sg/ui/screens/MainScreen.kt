package com.bushop.sg.ui.screens


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.LayoutDirection
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bushop.sg.data.local.BusStopEntry
import com.bushop.sg.domain.model.ColorSchemeOption
import com.bushop.sg.domain.model.ThemeMode
import com.bushop.sg.ui.components.AddBusStopDialog
import com.bushop.sg.BuildConfig
import com.bushop.sg.ui.components.BusStopCard

private val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")

private fun formatLastUpdated(timestamp: Long): String {
    val zdt = java.time.ZonedDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(timestamp),
        java.time.ZoneId.systemDefault()
    )
    return timeFormatter.format(zdt)
}

@Composable
private fun ApiStatusBanner(
    status: ApiStatus,
    onDismiss: () -> Unit
) {
    val visible = status != ApiStatus.Healthy
    val bgColor: androidx.compose.ui.graphics.Color
    val textColor: androidx.compose.ui.graphics.Color
    val message: String
    val showDismiss: Boolean
    when (status) {
        ApiStatus.Healthy -> {
            bgColor = MaterialTheme.colorScheme.surface
            textColor = MaterialTheme.colorScheme.onSurface
            message = ""
            showDismiss = false
        }
        ApiStatus.Degraded -> {
            bgColor = MaterialTheme.colorScheme.tertiaryContainer
            textColor = MaterialTheme.colorScheme.onTertiaryContainer
            message = "Bus arrival data may be delayed"
            showDismiss = false
        }
        ApiStatus.Down -> {
            bgColor = MaterialTheme.colorScheme.errorContainer
            textColor = MaterialTheme.colorScheme.onErrorContainer
            message = "Bus arrival API is under maintenance. Some data may be unavailable."
            showDismiss = true
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bgColor),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )
                if (showDismiss) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Dismiss",
                            tint = textColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val savedStops by viewModel.savedStops.collectAsState()
    val sortByEarliest by viewModel.sortByEarliest.collectAsState()
    val apiStatus by viewModel.apiStatus.collectAsState()
    val pinnedServices by viewModel.pinnedServices.collectAsState()
    val themeMode by viewModel.themeModeFlow.collectAsState()
    val isIndexReady by viewModel.isIndexReady.collectAsState()
    val colorSchemeOption by viewModel.colorSchemeOptionFlow.collectAsState()
    val listState = rememberLazyListState()
    var showSettings by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    // Scroll to top when a new stop is pinned
    var prevPinnedCount by remember { mutableStateOf(0) }
    val currentPinnedCount = savedStops.count { it.isPinned }
    LaunchedEffect(currentPinnedCount) {
        if (currentPinnedCount > prevPinnedCount && currentPinnedCount > 0) {
            listState.animateScrollToItem(0)
        }
        prevPinnedCount = currentPinnedCount
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val onSortClick = remember { { viewModel.toggleSortOrder() } }
    val onThemeClick = remember { { viewModel.toggleThemeMode() } }
    val onRefreshClick = remember { { viewModel.refreshAll() } }
    val onSettingsClick = remember { { showSettings = true } }

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
                    IconButton(onClick = onSortClick) {
                        Icon(
                            imageVector = if (sortByEarliest) Icons.AutoMirrored.Filled.Sort else Icons.AutoMirrored.Outlined.Sort,
                            contentDescription = "Sort",
                            tint = if (sortByEarliest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onThemeClick) {
                        Icon(
                            imageVector = when (themeMode) {
                                ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                            },
                            contentDescription = when (themeMode) {
                                ThemeMode.SYSTEM -> "Auto theme"
                                ThemeMode.LIGHT -> "Light mode"
                                ThemeMode.DARK -> "Dark mode"
                            },
                            tint = when (themeMode) {
                                ThemeMode.SYSTEM -> MaterialTheme.colorScheme.onSurfaceVariant
                                ThemeMode.LIGHT -> MaterialTheme.colorScheme.onSurfaceVariant
                                ThemeMode.DARK -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                    IconButton(onClick = onRefreshClick) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = if (viewModel.isRefreshing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
        .padding(
            start = paddingValues.calculateLeftPadding(LayoutDirection.Ltr),
            top = paddingValues.calculateTopPadding(),
            end = paddingValues.calculateRightPadding(LayoutDirection.Ltr),
            bottom = paddingValues.calculateBottomPadding()
        )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ApiStatusBanner(
                    status = apiStatus,
                    onDismiss = { viewModel.dismissApiBanner() }
                )
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (!isIndexReady) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Loading stops…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            }
                        }
                    } else if (savedStops.isEmpty()) {
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
                        val pullRefreshState = rememberPullToRefreshState()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(pullRefreshState.nestedScrollConnection)
                        ) {
                            LazyColumn(
                                state = listState,
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
                                        stop = stopWithArrivals,
                                        onRefresh = { viewModel.refreshArrivals(stopWithArrivals.busStop.code) },
                                        onToggleCollapse = { viewModel.toggleCollapse(stopWithArrivals.busStop.code) },
                                        onTogglePin = { viewModel.togglePin(stopWithArrivals.busStop.code) },
                                        onDelete = { deleteTarget = stopWithArrivals.busStop.code },
                                        onTogglePinService = { serviceNo ->
                                            viewModel.togglePinService(stopWithArrivals.busStop.code, serviceNo)
                                        },
                                        pinnedServiceNos = pinnedServices
                                            .filter { it.startsWith("${stopWithArrivals.busStop.code}:") }
                                            .map { it.substringAfter(":") }.toSet()
                                    )
                                }
                            }
                            var hasTriggeredRefresh by remember { mutableStateOf(false) }
                            if (pullRefreshState.isRefreshing && !hasTriggeredRefresh) {
                                LaunchedEffect(pullRefreshState.isRefreshing) {
                                    viewModel.refreshAll()
                                    hasTriggeredRefresh = true
                                }
                            }
                            if (!viewModel.isRefreshing && hasTriggeredRefresh && pullRefreshState.isRefreshing) {
                                LaunchedEffect(viewModel.isRefreshing) {
                                    pullRefreshState.endRefresh()
                                    hasTriggeredRefresh = false
                                }
                            }
                            if (pullRefreshState.isRefreshing || pullRefreshState.progress > 0f) {
                                PullToRefreshContainer(
                                    state = pullRefreshState,
                                    modifier = Modifier.align(Alignment.TopCenter)
                                )
                            }
                        }
                    }
                }  // close weight(1f) Box
            }  // close Column

        if (viewModel.lastUpdatedAll > 0) {
            val pillBg by animateColorAsState(
                targetValue = if (viewModel.isRefreshing)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
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
                    text = buildString {
                        append("Updated: ${formatLastUpdated(viewModel.lastUpdatedAll)}")
                        if (savedStops.any { it.isStale }) {
                            append("  •  Some data may be stale")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        }  // close outer Box
    }  // close Scaffold content

    if (showSettings) {
        SettingsSheet(
            currentTheme = themeMode,
            currentInterval = viewModel.autoRefreshIntervalSeconds,
            currentColorScheme = colorSchemeOption,
            onThemeChange = { viewModel.setThemeMode(it) },
            onIntervalChange = { seconds ->
                viewModel.setAutoRefreshInterval(seconds)
                showSettings = false
            },
            onColorSchemeChange = { viewModel.setColorScheme(it) },
            onDismiss = { showSettings = false }
        )
    }

    if (viewModel.addStopDialogVisible) {
        val searchResults by viewModel.searchResults.collectAsState()

        AddBusStopDialog(
            error = viewModel.addStopError,
            isLoading = viewModel.addStopIsLoading,
            searchResults = searchResults,
            onSearchQueryChanged = { query ->
                viewModel.searchBusStops(query)
            },
            onDismiss = { viewModel.hideAddStopDialog() },
            onConfirm = { code, name ->
                // If name equals code (manual entry), try to find the real name
                val resolvedName = if (name == code) {
                    viewModel.findBusStopByCode(code)?.name ?: name
                } else {
                    name
                }
                viewModel.addBusStop(code, resolvedName)
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

@Composable
private fun SettingsSheet(
    currentTheme: ThemeMode,
    currentInterval: Int,
    currentColorScheme: ColorSchemeOption,
    onThemeChange: (ThemeMode) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onColorSchemeChange: (ColorSchemeOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
        text = {
            Column {
                Text("Theme", style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                ThemeMode.entries.forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { onThemeChange(mode) }
                    ) {
                        RadioButton(selected = currentTheme == mode, onClick = { onThemeChange(mode) })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = when (mode) { ThemeMode.SYSTEM -> "System"; ThemeMode.LIGHT -> "Light"; ThemeMode.DARK -> "Dark" })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Color Scheme", style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                ColorSchemeOption.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { onColorSchemeChange(option) }
                    ) {
                        RadioButton(selected = currentColorScheme == option, onClick = { onColorSchemeChange(option) })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = option.displayName)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Auto Refresh", style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                val intervals = listOf(0 to "Off", 30 to "30s", 60 to "1m", 120 to "2m", 300 to "5m")
                intervals.forEach { (seconds, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { onIntervalChange(seconds) }
                    ) {
                        RadioButton(selected = currentInterval == seconds, onClick = { onIntervalChange(seconds) })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(label)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}