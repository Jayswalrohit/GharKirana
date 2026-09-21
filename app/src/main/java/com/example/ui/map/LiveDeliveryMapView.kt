package com.example.ui.map

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.ui.components.formatNpr
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.*

/**
 * Route Waypoint on local cartesian map (normalized 0.0 to 1.0)
 */
data class MapPoint(
    val x: Float,
    val y: Float,
    val streetName: String = "",
    val turnInstruction: String = ""
)

/**
 * Janakpur Dham Kirana delivery route waypoints
 */
val janakpurDeliveryRoute = listOf(
    MapPoint(0.18f, 0.22f, "Station Road (GharKirana Hub)", "Departed Kirana Shop"),
    MapPoint(0.26f, 0.25f, "Station Road", "Head straight on Station Road"),
    MapPoint(0.35f, 0.32f, "Ram Mandir Crossing", "Turn right onto Ram Mandir Marg"),
    MapPoint(0.48f, 0.38f, "Ram Mandir Marg", "Pass Janaki Temple Garden"),
    MapPoint(0.55f, 0.48f, "Hospital Chowk", "Continue south-east past Ganga Sagar"),
    MapPoint(0.68f, 0.58f, "Mills Road Junction", "Turn left onto Bhanu Chowk Road"),
    MapPoint(0.76f, 0.70f, "Bhanu Chowk Marg", "Continue 200m towards customer"),
    MapPoint(0.82f, 0.78f, "Near Bhanu Chowk Gate", "Approaching Customer Gate"),
    MapPoint(0.85f, 0.82f, "Customer Address (House 24)", "Arrived at Destination")
)

@Composable
fun LiveDeliveryMapView(
    order: Order,
    onCallRider: () -> Unit,
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = false,
    onToggleFullScreen: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val textMeasurer = rememberTextMeasurer()

    // Interactive Pan & Zoom
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Live Real-Time Movement Progress (0.0f = Shop to 1.0f = Destination)
    var progress by remember { mutableFloatStateOf(0.42f) }
    var isSimulating by remember { mutableStateOf(true) }
    var simulationSpeed by remember { mutableFloatStateOf(1f) }

    // Pulse animation for Rider GPS radar
    val infiniteTransition = rememberInfiniteTransition(label = "rider_radar")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 38f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    // Dash offset animation for polyline
    val dashPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dash_phase"
    )

    // Real-time timer to advance the rider position along the route
    LaunchedEffect(isSimulating, simulationSpeed, order.orderStatus) {
        // If order is DELIVERED, snap to end
        if (order.orderStatus == OrderStatus.DELIVERED) {
            progress = 1.0f
            return@LaunchedEffect
        }

        while (isSimulating) {
            delay(100)
            val step = 0.0035f * simulationSpeed
            progress = (progress + step).coerceIn(0f, 1f)
            if (progress >= 1f) {
                // Loop or stop
                delay(3000)
                progress = 0.05f
            }
        }
    }

    // Calculate interpolated Rider Coordinates and Heading Angle
    val (currentPos, headingAngle, currentStreet, currentInstruction) = remember(progress) {
        calculateRiderState(progress, janakpurDeliveryRoute)
    }

    // Telemetry
    val totalDistanceMeters = 2400.0 // 2.4 km total
    val remainingMeters = ((1f - progress) * totalDistanceMeters).toInt()
    val distanceDisplay = if (remainingMeters > 900) {
        String.format(java.util.Locale.US, "%.1f km", remainingMeters / 1000.0)
    } else {
        "$remainingMeters m"
    }

    val speedKmh = remember(progress, isSimulating) {
        if (!isSimulating || progress >= 0.98f) 0
        else (28 + ((sin(progress * 25) + 1) * 6)).toInt()
    }

    val remainingMinutes = max(1, ((remainingMeters / 2400.0) * order.estimatedMinutes).toInt())

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE2ECDE)) // Map background land tint
    ) {
        // Compose Canvas Map
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.7f, 2.5f)
                        panOffset = Offset(
                            x = panOffset.x + pan.x,
                            y = panOffset.y + pan.y
                        )
                    }
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height

            // Coordinate helper with Pan and Zoom applied
            fun mapToScreen(normX: Float, normY: Float): Offset {
                val cx = canvasW * normX
                val cy = canvasH * normY
                val zoomedX = (cx - canvasW / 2) * zoomScale + canvasW / 2 + panOffset.x
                val zoomedY = (cy - canvasH / 2) * zoomScale + canvasH / 2 + panOffset.y
                return Offset(zoomedX, zoomedY)
            }

            // 1. Draw Map Background Features (Parks, Ponds, Blocks)
            drawMapBackground(this, ::mapToScreen, textMeasurer)

            // 2. Draw Road Network (Secondary & Primary roads)
            drawRoadNetwork(this, ::mapToScreen)

            // 3. Draw Route Polyline from Shop to Destination
            drawDeliveryRoute(
                scope = this,
                route = janakpurDeliveryRoute,
                toScreen = ::mapToScreen,
                progress = progress,
                dashPhase = dashPhase
            )

            // 4. Draw Kirana Shop Hub Pin
            val shopScreen = mapToScreen(janakpurDeliveryRoute.first().x, janakpurDeliveryRoute.first().y)
            drawShopMarker(this, shopScreen, textMeasurer)

            // 5. Draw Customer Destination Pin
            val destScreen = mapToScreen(janakpurDeliveryRoute.last().x, janakpurDeliveryRoute.last().y)
            drawCustomerMarker(this, destScreen, order.deliveryAddress.areaOrChowk, textMeasurer)

            // 6. Draw Real-time Rider with heading and pulse wave
            val riderScreen = mapToScreen(currentPos.x, currentPos.y)
            drawRiderMarker(
                scope = this,
                pos = riderScreen,
                heading = headingAngle,
                pulseRadius = pulseRadius * zoomScale,
                pulseAlpha = pulseAlpha,
                speedKmh = speedKmh,
                textMeasurer = textMeasurer
            )
        }

        // Top-left Live Telemetry Badge
        Surface(
            color = Color.Black.copy(alpha = 0.78f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (progress >= 0.98f) "ARRIVED AT GATE" else "LIVE GPS • $distanceDisplay",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "($remainingMinutes mins)",
                            color = KiranaGreenAccent,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (progress >= 0.98f) "Rider is calling at your gate" else currentInstruction,
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Top-right Map Controls (Zoom +, Zoom -, Re-center, Fullscreen)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Zoom in
            MapControlButton(
                icon = Icons.Default.Add,
                onClick = { zoomScale = (zoomScale * 1.25f).coerceAtMost(2.5f) }
            )
            // Zoom out
            MapControlButton(
                icon = Icons.Default.Remove,
                onClick = { zoomScale = (zoomScale * 0.8f).coerceAtLeast(0.7f) }
            )
            // Re-center on Rider
            MapControlButton(
                icon = Icons.Default.MyLocation,
                onClick = {
                    zoomScale = 1.1f
                    panOffset = Offset.Zero
                }
            )
            // Fullscreen toggle if provided
            if (onToggleFullScreen != null) {
                MapControlButton(
                    icon = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    onClick = onToggleFullScreen
                )
            }
        }

        // Bottom Overlay: Live Rider Dispatcher Card & Simulation Controls
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(KiranaGreenPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🛵", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = order.assignedRiderName ?: "Ramesh Thapa",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = KiranaAmberLight,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "★ 4.9",
                                        color = KiranaAmberDark,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Hero Splendor (${order.assignedRiderPhone ?: "+977-9841234567"})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Contact action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                val smsIntent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("sms:${order.assignedRiderPhone ?: "9841234567"}")
                                    putExtra("sms_body", "Hi! I am at House 24, near Bhanu Chowk gate for Order #${order.orderNumber}.")
                                }
                                context.startActivity(smsIntent)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(InfoBlueLight)
                        ) {
                            Icon(Icons.Default.Message, contentDescription = "Message", tint = InfoBlue, modifier = Modifier.size(18.dp))
                        }

                        Button(
                            onClick = onCallRider,
                            colors = ButtonDefaults.buttonColors(containerColor = KiranaGreenPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp).testTag("call_delivery_boy_btn")
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Call", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Street & Live Route Ticker
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📍", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentStreet,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = "$speedKmh km/h",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = KiranaGreenDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // GPS Simulation Controller Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "GPS Live Sim:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(
                            onClick = { isSimulating = !isSimulating },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(if (isSimulating) "⏸ Pause" else "▶ Play", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = {
                                simulationSpeed = when (simulationSpeed) {
                                    1f -> 2f
                                    2f -> 3f
                                    else -> 1f
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("${simulationSpeed.toInt()}x Speed", fontSize = 11.sp)
                        }
                    }

                    TextButton(
                        onClick = { progress = 0.05f },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("↺ Reset Trip", fontSize = 11.sp, color = KiranaGreenPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun MapControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CircleShape,
        shadowElevation = 4.dp,
        modifier = Modifier
            .size(36.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Calculates current rider position, heading angle (in degrees), and street turn instruction
 */
private fun calculateRiderState(
    progress: Float,
    route: List<MapPoint>
): Quadruple<MapPoint, Float, String, String> {
    if (route.size < 2) {
        return Quadruple(route.first(), 0f, "Station Road", "Heading to customer")
    }

    val totalSegments = route.size - 1
    val scaledProgress = (progress * totalSegments).coerceIn(0f, totalSegments.toFloat())
    val segIndex = scaledProgress.toInt().coerceIn(0, totalSegments - 1)
    val segFraction = scaledProgress - segIndex

    val p1 = route[segIndex]
    val p2 = route[segIndex + 1]

    val interpX = p1.x + (p2.x - p1.x) * segFraction
    val interpY = p1.y + (p2.y - p1.y) * segFraction

    // Heading calculation in degrees
    val dx = (p2.x - p1.x).toDouble()
    val dy = (p2.y - p1.y).toDouble()
    val headingRad = atan2(dy, dx)
    val headingDeg = Math.toDegrees(headingRad).toFloat()

    return Quadruple(
        MapPoint(interpX, interpY),
        headingDeg,
        p1.streetName,
        p1.turnInstruction
    )
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Draws realistic streets, parks, water body of Janakpur Dham
 */
private fun drawMapBackground(
    scope: DrawScope,
    toScreen: (Float, Float) -> Offset,
    textMeasurer: TextMeasurer
) {
    // Ganga Sagar Pokhari (Water Body)
    val lakeCenter = toScreen(0.42f, 0.62f)
    scope.drawOval(
        color = Color(0xFFA5D8F3),
        topLeft = Offset(lakeCenter.x - 70f, lakeCenter.y - 45f),
        size = Size(140f, 90f)
    )
    scope.drawText(
        textMeasurer = textMeasurer,
        text = "Ganga Sagar",
        topLeft = Offset(lakeCenter.x - 38f, lakeCenter.y - 8f),
        style = TextStyle(color = Color(0xFF0284C7), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    )

    // Janaki Mandir Park (Green Zone)
    val parkCenter = toScreen(0.58f, 0.28f)
    scope.drawRoundRect(
        color = Color(0xFFC7E5C3),
        topLeft = Offset(parkCenter.x - 65f, parkCenter.y - 40f),
        size = Size(130f, 80f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
    )
    scope.drawText(
        textMeasurer = textMeasurer,
        text = "Janaki Mandir Zone",
        topLeft = Offset(parkCenter.x - 44f, parkCenter.y - 6f),
        style = TextStyle(color = Color(0xFF2E7D32), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    )
}

/**
 * Draws background street grid
 */
private fun drawRoadNetwork(
    scope: DrawScope,
    toScreen: (Float, Float) -> Offset
) {
    val roadColor = Color(0xFFFFFFFF)
    val roadOutline = Color(0xFFD1DCD0)

    // Major arteries
    val majorRoads = listOf(
        // Station Road East-West
        listOf(toScreen(0.05f, 0.20f), toScreen(0.95f, 0.25f)),
        // Ramanand Bypass North-South
        listOf(toScreen(0.35f, 0.05f), toScreen(0.35f, 0.95f)),
        // Hospital Road diagonal
        listOf(toScreen(0.15f, 0.45f), toScreen(0.85f, 0.55f)),
        // Bhanu Chowk Ring
        listOf(toScreen(0.50f, 0.95f), toScreen(0.85f, 0.75f), toScreen(0.95f, 0.45f))
    )

    for (road in majorRoads) {
        for (i in 0 until road.size - 1) {
            // Outline
            scope.drawLine(
                color = roadOutline,
                start = road[i],
                end = road[i + 1],
                strokeWidth = 26f,
                cap = StrokeCap.Round
            )
            // Road surface
            scope.drawLine(
                color = roadColor,
                start = road[i],
                end = road[i + 1],
                strokeWidth = 20f,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Draws the active delivery route polyline with progress coloring & moving dashes
 */
private fun drawDeliveryRoute(
    scope: DrawScope,
    route: List<MapPoint>,
    toScreen: (Float, Float) -> Offset,
    progress: Float,
    dashPhase: Float
) {
    if (route.size < 2) return

    val screenPoints = route.map { toScreen(it.x, it.y) }

    // 1. Draw Full Planned Route (Gray / Light Green background)
    val fullPath = Path().apply {
        moveTo(screenPoints[0].x, screenPoints[0].y)
        for (i in 1 until screenPoints.size) {
            lineTo(screenPoints[i].x, screenPoints[i].y)
        }
    }

    // Outer Glow
    scope.drawPath(
        path = fullPath,
        color = KiranaGreenLight.copy(alpha = 0.8f),
        style = Stroke(width = 18f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Route Base
    scope.drawPath(
        path = fullPath,
        color = Color(0xFFB0CDB0),
        style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Animated Dashes along full route
    scope.drawPath(
        path = fullPath,
        color = Color.White.copy(alpha = 0.9f),
        style = Stroke(
            width = 4f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 16f), dashPhase),
            cap = StrokeCap.Round
        )
    )

    // 2. Draw Completed Route (Active Kirana Green)
    // Draw segments up to rider progress
    val totalSegments = route.size - 1
    val currentSegmentFloat = progress * totalSegments
    val completedSegments = currentSegmentFloat.toInt().coerceIn(0, totalSegments)

    val completedPath = Path().apply {
        moveTo(screenPoints[0].x, screenPoints[0].y)
        for (i in 1..completedSegments) {
            lineTo(screenPoints[i].x, screenPoints[i].y)
        }
        if (completedSegments < totalSegments) {
            val frac = currentSegmentFloat - completedSegments
            val lastP = screenPoints[completedSegments]
            val nextP = screenPoints[completedSegments + 1]
            lineTo(lastP.x + (nextP.x - lastP.x) * frac, lastP.y + (nextP.y - lastP.y) * frac)
        }
    }

    scope.drawPath(
        path = completedPath,
        color = KiranaGreenPrimary,
        style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

/**
 * Kirana Shop Hub Pin
 */
private fun drawShopMarker(
    scope: DrawScope,
    pos: Offset,
    textMeasurer: TextMeasurer
) {
    // Outer circle
    scope.drawCircle(
        color = Color.White,
        radius = 16f,
        center = pos
    )
    scope.drawCircle(
        color = KiranaGreenPrimary,
        radius = 13f,
        center = pos
    )
    scope.drawCircle(
        color = Color.White,
        radius = 5f,
        center = pos
    )

    // Label
    scope.drawRoundRect(
        color = Color.Black.copy(alpha = 0.75f),
        topLeft = Offset(pos.x - 52f, pos.y - 36f),
        size = Size(104f, 20f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
    )
    scope.drawText(
        textMeasurer = textMeasurer,
        text = "🏪 GharKirana Hub",
        topLeft = Offset(pos.x - 48f, pos.y - 34f),
        style = TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    )
}

/**
 * Customer Destination Marker
 */
private fun drawCustomerMarker(
    scope: DrawScope,
    pos: Offset,
    area: String,
    textMeasurer: TextMeasurer
) {
    // Target pulse wave
    scope.drawCircle(
        color = ErrorRedLight.copy(alpha = 0.6f),
        radius = 24f,
        center = pos
    )
    scope.drawCircle(
        color = Color.White,
        radius = 16f,
        center = pos
    )
    scope.drawCircle(
        color = ErrorRed,
        radius = 12f,
        center = pos
    )
    scope.drawCircle(
        color = Color.White,
        radius = 4f,
        center = pos
    )

    // Destination Pill
    val labelText = "🏠 $area"
    scope.drawRoundRect(
        color = ErrorRed,
        topLeft = Offset(pos.x - 55f, pos.y + 18f),
        size = Size(110f, 22f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
    )
    scope.drawText(
        textMeasurer = textMeasurer,
        text = labelText,
        topLeft = Offset(pos.x - 48f, pos.y + 22f),
        style = TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    )
}

/**
 * Real-time Delivery Boy Marker with Radar Waves and Heading
 */
private fun drawRiderMarker(
    scope: DrawScope,
    pos: Offset,
    heading: Float,
    pulseRadius: Float,
    pulseAlpha: Float,
    speedKmh: Int,
    textMeasurer: TextMeasurer
) {
    // 1. Radar wave expanding
    scope.drawCircle(
        color = KiranaGreenPrimary.copy(alpha = pulseAlpha),
        radius = pulseRadius,
        center = pos
    )

    // 2. Rider Vehicle Outer Ring
    scope.drawCircle(
        color = Color.White,
        radius = 18f,
        center = pos
    )
    scope.drawCircle(
        color = KiranaGreenDark,
        radius = 14f,
        center = pos
    )

    // 3. Direction Pointer Triangle (Rotates with heading)
    scope.rotate(degrees = heading, pivot = pos) {
        val pointerPath = Path().apply {
            moveTo(pos.x + 18f, pos.y)
            lineTo(pos.x - 6f, pos.y - 8f)
            lineTo(pos.x - 2f, pos.y)
            lineTo(pos.x - 6f, pos.y + 8f)
            close()
        }
        drawPath(
            path = pointerPath,
            color = KiranaAmberPrimary
        )
    }

    // Bike icon indicator in center
    scope.drawCircle(
        color = Color.White,
        radius = 6f,
        center = pos
    )

    // Floating Speed bubble above rider
    val speedText = "$speedKmh km/h"
    scope.drawRoundRect(
        color = KiranaGreenDark,
        topLeft = Offset(pos.x - 26f, pos.y - 36f),
        size = Size(52f, 18f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
    )
    scope.drawText(
        textMeasurer = textMeasurer,
        text = speedText,
        topLeft = Offset(pos.x - 20f, pos.y - 34f),
        style = TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    )
}
