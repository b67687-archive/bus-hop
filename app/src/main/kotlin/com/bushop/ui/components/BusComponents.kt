package com.bushop.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.bushop.domain.model.BusService
import com.bushop.domain.model.BusStopWithArrivals
import com.bushop.domain.model.toDisplayArrival

private const val COLLAPSE_PROPAGATION_MS = 50L

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun BusStopCard(
    stop: BusStopWithArrivals,
    isNewlyAdded: Boolean = false,
    onRefresh: () -> Unit,
    onToggleCollapse: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onTogglePinService: (String) -> Unit,
    pinnedServiceNos: Set<String> = emptySet(),
    onMoveStop: ((Int) -> Unit)? = null, // called on drag end (multi-position delta)
    onDragStart: ((code: String) -> Unit)? = null, // called when drag begins
    onDragProgress: ((code: String, lastTotalY: Float, draggedCenterY: Float) -> Unit)? = null,
    onDragEnd: ((code: String, lastTotalY: Float) -> Unit)? = null, // called when drag ends
    isDeleteTargeted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val busStopCode = stop.busStop.code
    val busStopName = stop.busStop.name
    val services = stop.services
    val isLoading = stop.isLoading
    val error = stop.error
    val isOffline = stop.isOffline
    val isCollapsed = stop.isCollapsed
    val isPinned = stop.isPinned
    val haptic = LocalHapticFeedback.current
    var localDragOffset by remember { mutableStateOf(0f) }
    var isLocallyDragged by remember { mutableStateOf(false) }
    var collapsedForDrag by remember { mutableStateOf(false) }
    var cardTopYInRoot by remember { mutableStateOf(0f) }
    var cardHeightInRoot by remember { mutableStateOf(0f) }
    val visuallyDragged = isLocallyDragged
    val effectiveOffset = localDragOffset
    val effectiveCollapsed = collapsedForDrag || isCollapsed
    val dragScale by animateFloatAsState(
        targetValue = if (isDeleteTargeted) 0.86f else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "dragScale",
    )

    // Pulse highlight for newly added stop
    var isPulsing by remember { mutableStateOf(false) }
    LaunchedEffect(isNewlyAdded) {
        if (isNewlyAdded) {
            isPulsing = true
            kotlinx.coroutines.delay(1500)
            isPulsing = false
        }
    }
    val pulseAlpha by animateFloatAsState(
        targetValue = if (isPulsing) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "pulseAlpha",
    )

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    cardTopYInRoot = coordinates.positionInRoot().y
                    cardHeightInRoot = coordinates.size.height.toFloat()
                }.then(
                    if (visuallyDragged) {
                        Modifier
                            .zIndex(1f)
                            .graphicsLayer {
                                translationY = effectiveOffset
                                scaleX = dragScale
                                scaleY = dragScale
                                alpha = if (isDeleteTargeted) 0.92f else 1f
                            }.shadow(12.dp, RoundedCornerShape(20.dp))
                    } else {
                        Modifier
                    },
                ),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isPinned) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
        border = if (isPinned) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (visuallyDragged) 12.dp else 3.dp),
    ) {
        Column(
            modifier =
                Modifier.drawWithContent {
                    drawContent()
                    if (pulseAlpha > 0.001f) {
                        drawRect(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha * 0.2f),
                            size = size,
                        )
                    }
                },
        ) {
            // ── Header background (pinned = blue pill, unpinned = none) ──
            Row(
                modifier =
                    (
                        if (isPinned) {
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        } else {
                            Modifier.fillMaxWidth()
                        }
                    ).padding(horizontal = 16.dp, vertical = 12.dp)
                        .then(
                            if (onMoveStop != null) {
                                Modifier
                                    .pointerInput(busStopCode) {
                                        detectTapGestures(
                                            onTap = { onToggleCollapse() },
                                        )
                                    }.pointerInput(busStopCode) {
                                        var totalY = 0f
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                totalY = 0f
                                                isLocallyDragged = true
                                                localDragOffset = 0f
                                                if (!isCollapsed) collapsedForDrag = true
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onDragStart?.invoke(busStopCode)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                totalY += dragAmount.y
                                                localDragOffset = totalY
                                                // Use collapsed card center for delete zone detection
                                                val cardCenter = cardTopYInRoot + (cardHeightInRoot / 2f)
                                                onDragProgress?.invoke(
                                                    busStopCode,
                                                    totalY,
                                                    cardCenter + totalY,
                                                )
                                            },
                                            onDragEnd = {
                                                onDragEnd?.invoke(busStopCode, totalY)
                                                isLocallyDragged = false
                                                localDragOffset = 0f
                                                // Keep collapsed state after drag (toggle permanently if was expanded)
                                                if (collapsedForDrag) {
                                                    if (!isCollapsed) onToggleCollapse()
                                                    // Don't clear collapsedForDrag yet — keep card visually
                                                    // collapsed until isCollapsed propagates from ViewModel
                                                } else {
                                                    collapsedForDrag = false
                                                }
                                            },
                                            onDragCancel = {
                                                isLocallyDragged = false
                                                localDragOffset = 0f
                                                collapsedForDrag = false
                                            },
                                        )
                                    }
                            } else {
                                Modifier.pointerInput(busStopCode) {
                                    detectTapGestures(
                                        onTap = { onToggleCollapse() },
                                        onLongPress = { onDelete() },
                                    )
                                }
                            },
                        ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (busStopName.isNotBlank()) {
                        val namePillBg =
                            if (isPinned) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                        Box(
                            modifier =
                                Modifier
                                    .widthIn(max = 130.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(namePillBg)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = busStopName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color =
                                    if (isPinned) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else {
                        Text(
                            text = busStopCode,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (busStopName.isNotBlank()) {
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = busStopCode,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(20.dp),
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (isPinned) "Unpin" else "Pin",
                            modifier = Modifier.size(12.dp),
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box(
                        modifier =
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onToggleCollapse),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (effectiveCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = if (effectiveCollapsed) "Expand" else "Collapse",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            val easing = FastOutSlowInEasing
            // Collapsed chips: show when collapsed OR during drag (instant, no animation)
            val showCollapsed = (effectiveCollapsed || isLocallyDragged) && services.isNotEmpty()
            if (showCollapsed) {
                FlowRow(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    services.forEach { service ->
                        CollapsedBusChip(service = service)
                    }
                }
            }

            // Expanded services: animated expand/collapse normally, hidden during drag
            // (removed from layout to keep card collapsed). animateItem(tween(0)) on the
            // LazyColumn item prevents position bounce from layout change.
            val showExpanded = !effectiveCollapsed && !isLocallyDragged
            // During drag, animate instantly (no layout animation competing with graphicsLayer)
            val exitDuration = if (isLocallyDragged) 0 else 150
            val enterDuration = if (isLocallyDragged) 0 else 280
            AnimatedVisibility(
                visible = showExpanded,
                enter =
                    fadeIn(animationSpec = tween(durationMillis = enterDuration, easing = easing)) +
                        expandVertically(animationSpec = tween(durationMillis = enterDuration, easing = easing)),
                exit =
                    fadeOut(animationSpec = tween(durationMillis = exitDuration, easing = easing)) +
                        shrinkVertically(animationSpec = tween(durationMillis = exitDuration, easing = easing)),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Spacer(modifier = Modifier.height(8.dp))

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
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        services.all { it.next == null && it.subsequent == null && it.next3 == null } && !isLoading -> {
                            val now = java.util.Calendar.getInstance()
                            val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
                            Text(
                                text =
                                    if (hour >= 23 || hour < 6) {
                                        "All buses have stopped running for the night"
                                    } else {
                                        "No upcoming buses at this stop"
                                    },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        else -> {
                            services.forEach { service ->
                                BusServiceRow(
                                    service = service,
                                    isPinned = service.serviceNo in pinnedServiceNos,
                                    onTogglePinService = { onTogglePinService(service.serviceNo) },
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Clear collapsedForDrag after isCollapsed has been persistently toggled
    LaunchedEffect(isLocallyDragged, isCollapsed) {
        if (!isLocallyDragged && collapsedForDrag) {
            if (isCollapsed) {
                collapsedForDrag = false
            } else {
                kotlinx.coroutines.delay(COLLAPSE_PROPAGATION_MS)
                collapsedForDrag = false
            }
        }
    }
}

@Composable
private fun CollapsedBusChip(service: BusService) {
    val arrival = service.next?.toDisplayArrival()
    val eta = arrival?.eta ?: "--"
    val isArriving = eta == "Arr." || eta == "Arr"

    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = service.serviceNo,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isArriving) Color(0xFF34C759) else MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
            ) {
                Text(
                    text = eta,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun OfflineBanner(onRetry: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "No internet connection",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onRetry: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun BusServiceRow(
    service: BusService,
    isPinned: Boolean = false,
    onTogglePinService: (() -> Unit)? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isPinned) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    },
                ).then(
                    if (isPinned) {
                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    } else {
                        Modifier
                    },
                ).then(
                    if (onTogglePinService != null) {
                        Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = onTogglePinService,
                        )
                    } else {
                        Modifier
                    },
                ).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(width = 56.dp, height = 44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = service.serviceNo,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OperatorBadge(operator = service.operator)
                if (service.next?.feature == "WAB") {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Outlined.Accessibility,
                        contentDescription = "Wheelchair accessible bus",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            service.next?.let { next ->
                val arrival = next.toDisplayArrival()
                // Bus type row
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector =
                            when {
                                arrival.busType.contains("Double") -> Icons.Filled.DirectionsBus
                                arrival.busType.contains("Bendy") -> Icons.Filled.DirectionsBus
                                else -> Icons.Outlined.DirectionsBus
                            },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = arrival.busType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // Load row
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector =
                            when {
                                arrival.load.contains("Seats") -> Icons.Default.Chair
                                arrival.load.contains("Standing") -> Icons.AutoMirrored.Filled.DirectionsWalk
                                else -> Icons.Default.Warning
                            },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint =
                            when {
                                arrival.load.contains("Limited") -> Color(0xFFFF9800)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                    Text(
                        text = arrival.load,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // First timing — always show
            val nextArrival = service.next?.toDisplayArrival()
            val isArriving = nextArrival?.eta == "Arr." || nextArrival?.eta == "Arr"
            Box(
                modifier =
                    Modifier
                        .widthIn(max = 88.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isArriving) {
                                Color(0xFF34C759)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                        ).padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = nextArrival?.eta ?: "--",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color =
                        if (isArriving) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // Second timing — always show
            val subArrival = service.subsequent?.toDisplayArrival()
            Text(
                text = subArrival?.eta ?: "--",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
            // Third timing — always show
            val next3Arrival = service.next3?.toDisplayArrival()
            Text(
                text = next3Arrival?.eta ?: "--",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun OperatorBadge(operator: String) {
    val color =
        when (operator) {
            "SBST" -> Color(0xFF0055A4)
            "SMRT" -> Color(0xFF003D7C)
            "TTS" -> Color(0xFF8B0000)
            "GAS" -> Color(0xFF6B8E23)
            else -> Color(0xFF666666)
        }
    val label =
        when (operator) {
            "SBST" -> "SBS"
            "SMRT" -> "SMRT"
            "TTS" -> "TTS"
            "GAS" -> "GAS"
            else -> operator
        }

    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(color)
                .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}
