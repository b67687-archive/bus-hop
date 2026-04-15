package com.bushop.sg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bushop.sg.data.model.BusService
import com.bushop.sg.data.model.toDisplayArrival

@Composable
fun BusStopCard(
    busStopCode: String,
    services: List<BusService>,
    isLoading: Boolean,
    error: String?,
    isOffline: Boolean,
    lastUpdated: Long = 0L,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bus Stop $busStopCode",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (isLoading) {
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isOffline -> {
                    OfflineBanner(onRetry = onRefresh)
                }
                error != null -> {
                    ErrorBanner(message = error, onRetry = onRefresh)
                }
                services.isEmpty() && !isLoading -> {
                    Text(
                        text = "No buses available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    services.forEach { service ->
                        BusServiceRow(service = service)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    if (lastUpdated > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Updated: ${formatLastUpdated(lastUpdated)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineBanner(onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF3E0))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            tint = Color(0xFFE65100),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "No internet connection",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFE65100)
        )
    }
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFEBEE))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun BusServiceRow(service: BusService) {
    var showWabInfo by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(width = 50.dp, height = 36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = service.serviceNo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OperatorBadge(operator = service.operator)
                if (service.next?.feature == "WAB") {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { showWabInfo = !showWabInfo },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Accessibility,
                            contentDescription = "Wheelchair info",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            service.next?.let { next ->
                val arrival = next.toDisplayArrival()
                Text(
                    text = "Next: ${arrival.eta}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${getBusTypeIcon(arrival.busType)} ${arrival.load}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            service.subsequent?.let { subsequent ->
                val arrival2 = subsequent.toDisplayArrival()
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "2nd: ${arrival2.eta}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            
            service.next3?.let { next3 ->
                val arrival3 = next3.toDisplayArrival()
                Text(
                    text = "3rd: ${arrival3.eta}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            if (showWabInfo) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Wheelchair accessible bus",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun getBusTypeIcon(busType: String): String {
    return when (busType) {
        "Double Decker" -> "D"
        "Single Decker" -> "S"
        "Bendy" -> "B"
        else -> "S"
    }
}

@Composable
private fun BusTypeIcon(busType: String) {
    val (bgColor, text, label) = when (busType) {
        "Double Decker" -> Triple(Color(0xFF2196F3), "DD", "Double Deck")
        "Single Decker" -> Triple(Color(0xFF4CAF50), "SD", "Single Deck")
        "Bendy" -> Triple(Color(0xFFFF9800), "BD", "Bendy")
        else -> Triple(Color(0xFF9E9E9E), "S", "Bus")
    }
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 20.dp, height = 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 8.sp
            )
        }
    }
}

@Composable
fun OperatorBadge(operator: String) {
    val (bgColor, text) = when (operator) {
        "SBST" -> Color(0xFF0055A4) to "SBS"
        "SMRT" -> Color(0xFF003D7C) to "SMRT"
        "TTS" -> Color(0xFFA00000) to "TTS"
        "GAS" -> Color(0xFF6B8E23) to "GAS"
        else -> Color(0xFF666666) to operator
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

private fun formatLastUpdated(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}