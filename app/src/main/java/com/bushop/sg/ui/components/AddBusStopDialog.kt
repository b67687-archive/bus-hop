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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
    onConfirm: (code: String, name: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedEntry by remember { mutableStateOf<BusStopEntry?>(null) }
    val displayError = error

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            delay(300)
            onSearchQueryChanged(searchQuery.trim())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bus Stop") },
        text = {
            Column {
                Text(
                    text = if (selectedEntry != null) "Bus stop selected" else "Search by code or name",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = if (selectedEntry != null) "${selectedEntry!!.code} - ${selectedEntry!!.displayName}" else searchQuery,
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
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )

                if (displayError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = displayError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Search results or selected entry info
                if (selectedEntry != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Code: ${selectedEntry!!.code}\n${selectedEntry!!.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (searchQuery.length >= 2 && searchResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(searchResults) { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedEntry = entry
                                        searchQuery = "${entry.code} - ${entry.name}"
                                    }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = entry.road,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = entry.code,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                } else if (searchQuery.length >= 2 && searchResults.isEmpty() && !isLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No bus stops found. You can type a 5-digit code and press Add.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedEntry != null) {
                        onConfirm(selectedEntry!!.code, selectedEntry!!.name)
                    } else if (searchQuery.length == 5 && searchQuery.all { it.isDigit() }) {
                        // Manual code entry — try to find name from index, use code if not found
                        onConfirm(searchQuery, "")
                    } else if (selectedEntry == null) {
                        // User typed something but it's not a valid code — try searching
                        onSearchQueryChanged(searchQuery)
                    }
                },
                enabled = !isLoading && (selectedEntry != null || (searchQuery.length == 5 && searchQuery.all { it.isDigit() }))
            ) {
                if (isLoading) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text("Checking...")
                    }
                } else {
                    Text("Add")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
