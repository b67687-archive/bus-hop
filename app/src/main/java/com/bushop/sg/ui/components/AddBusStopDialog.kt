package com.bushop.sg.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bushop.sg.data.local.BusStopEntry

@Composable
fun AddBusStopDialog(
    error: String? = null,
    isLoading: Boolean = false,
    searchResults: List<BusStopEntry>,
    onSearchQueryChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (code: String, name: String) -> Unit,
    // Nearby stops
    nearbyStops: List<BusStopEntry> = emptyList(),
    isLoadingNearby: Boolean = false,
    nearbyError: String? = null,
    onFindNearby: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedEntry by remember { mutableStateOf<BusStopEntry?>(null) }
    var confirmNearby by remember { mutableStateOf<BusStopEntry?>(null) }
    val displayError = error ?: nearbyError

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            onSearchQueryChanged(searchQuery.trim())
        }
    }

    // Show nearby results when available
    val activeResults = if (nearbyStops.isNotEmpty() && searchQuery.length < 2) nearbyStops else searchResults
    val showNearbyHeader = nearbyStops.isNotEmpty() && searchQuery.length < 2

    AlertDialog(
        onDismissRequest = {
            if (confirmNearby != null) { confirmNearby = null; return@AlertDialog }
            onDismiss()
        },
        title = {
            Text(if (confirmNearby != null) "Confirm Add" else "Add Bus Stop")
        },
        text = {
            Column {
                if (confirmNearby != null) {
                    val s = confirmNearby!!
                    Text("Add this stop?", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("${s.code} — ${s.name}", fontWeight = FontWeight.Bold)
                    Text(s.road, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    return@Column
                }

                Text(
                    text = if (selectedEntry != null) "Bus stop selected"
                           else if (showNearbyHeader) "Nearby stops — tap to add"
                           else "Search by code or name",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = selectedEntry?.let { "${it.code} - ${it.displayName}" } ?: searchQuery,
                    onValueChange = {
                        searchQuery = it
                        selectedEntry = null
                    },
                    label = { Text("Bus stop code or name") },
                    placeholder = { Text("e.g. 83139 or Thomson") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = displayError != null,
                    enabled = !isLoading,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                )

                // Nearby button
                if (!showNearbyHeader && searchQuery.length < 2) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onFindNearby, enabled = !isLoadingNearby) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isLoadingNearby) "Locating…" else "Find nearby stops")
                    }
                }

                if (displayError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = displayError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }

                // Results list
                if (selectedEntry != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Code: ${selectedEntry!!.code}\n${selectedEntry!!.displayName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (activeResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (showNearbyHeader) {
                        Text("${activeResults.size} stops within ~500m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                    }
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(activeResults) { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selectedEntry = entry
                                    searchQuery = "${entry.code} - ${entry.name}"
                                }.padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(entry.road, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(entry.code, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                } else if (searchQuery.length >= 2 && searchResults.isEmpty() && !isLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No bus stops found. You can type a 5-digit code and press Add.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            if (confirmNearby != null) {
                TextButton(onClick = { onConfirm(confirmNearby!!.code, confirmNearby!!.name); confirmNearby = null }) { Text("Yes, add stop") }
            } else {
                TextButton(
                    onClick = {
                        val entry = selectedEntry
                        if (entry != null) {
                            onConfirm(entry.code, entry.name)
                        } else if (searchQuery.length == 5 && searchQuery.all { it.isDigit() }) {
                            onConfirm(searchQuery, searchQuery)
                        } else {
                            onSearchQueryChanged(searchQuery)
                        }
                    },
                    enabled = !isLoading && (selectedEntry != null || (searchQuery.length == 5 && searchQuery.all { it.isDigit() }))
                ) {
                    if (isLoading) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Checking...")
                        }
                    } else {
                        Text("Add")
                    }
                }
            }
        },
        dismissButton = {
            if (confirmNearby != null) {
                TextButton(onClick = { confirmNearby = null }) { Text("Cancel") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
