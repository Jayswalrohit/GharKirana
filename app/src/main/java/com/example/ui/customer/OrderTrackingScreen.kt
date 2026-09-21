package com.example.ui.customer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatNpr
import com.example.ui.map.LiveDeliveryMapView
import com.example.ui.theme.*
import com.example.ui.viewmodel.GroceryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    orderId: String,
    viewModel: GroceryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(orderId) {
        viewModel.trackOrder(orderId)
    }

    val order by viewModel.trackedOrder.collectAsState()
    var isMapExpanded by remember { mutableStateOf(false) }
    var selectedTrackingTab by remember { mutableStateOf(0) } // 0: Live GPS Map, 1: Order Timeline

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (order != null) "Order #${order!!.orderNumber}" else "Order Tracking",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (order != null) {
                            Text(
                                text = "GharKirana Express Janakpur",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (order != null) {
                        IconButton(onClick = { isMapExpanded = true }) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Full Screen Map", tint = KiranaGreenPrimary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { padding ->
        if (order == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = KiranaGreenPrimary)
            }
        } else {
            val o = order!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Header Estimated Arrival Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = KiranaGreenPrimary)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⚡", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (o.orderStatus == OrderStatus.DELIVERED) "Order Delivered!"
                                        else "Estimated in ${o.estimatedMinutes} Mins",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                StatusBadge(status = o.orderStatus)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = o.orderStatus.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(o.createdAt))
                            Text(
                                text = "Placed on $dateStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                // View Mode Tabs: Real-Time Map vs Timeline
                item {
                    TabRow(
                        selectedTabIndex = selectedTrackingTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedTrackingTab == 0,
                            onClick = { selectedTrackingTab = 0 },
                            text = { Text("🗺️ Live Map Tracking", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        )
                        Tab(
                            selected = selectedTrackingTab == 1,
                            onClick = { selectedTrackingTab = 1 },
                            text = { Text("📋 Order Timeline", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        )
                    }
                }

                if (selectedTrackingTab == 0) {
                    // REAL-TIME GPS MAP TRACKING
                    item {
                        LiveDeliveryMapView(
                            order = o,
                            onCallRider = {
                                if (o.assignedRiderPhone != null) {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${o.assignedRiderPhone}")
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            isFullScreen = false,
                            onToggleFullScreen = { isMapExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(420.dp)
                        )
                    }
                } else {
                    // Assigned Rider Card
                    if (o.assignedRiderName != null) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(KiranaGreenLight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🛵", fontSize = 24.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = o.assignedRiderName!!,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Delivery Partner • 4.9 ★",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = KiranaGreenDark,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (o.assignedRiderPhone != null) {
                                                Text(
                                                    text = o.assignedRiderPhone!!,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    if (o.assignedRiderPhone != null) {
                                        Button(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                                    data = Uri.parse("tel:${o.assignedRiderPhone}")
                                                }
                                                context.startActivity(intent)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = KiranaGreenPrimary),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("call_rider_btn")
                                        ) {
                                            Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Call")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 8-Step Timeline Tracking
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "Live Kirana Progress",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val steps = listOf(
                            OrderStatus.PLACED,
                            OrderStatus.CONFIRMED,
                            OrderStatus.PREPARING,
                            OrderStatus.READY_FOR_PICKUP,
                            OrderStatus.RIDER_ASSIGNED,
                            OrderStatus.PICKED_UP,
                            OrderStatus.OUT_FOR_DELIVERY,
                            OrderStatus.DELIVERED
                        )

                        for (i in steps.indices) {
                            val step = steps[i]
                            val isCompleted = o.orderStatus.stepIndex >= step.stepIndex && o.orderStatus != OrderStatus.CANCELLED
                            val isCurrent = o.orderStatus == step

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Step circle
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isCurrent -> KiranaAmberPrimary
                                                isCompleted -> KiranaGreenPrimary
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCompleted && !isCurrent) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "${i + 1}",
                                            color = if (isCurrent || isCompleted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = step.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) KiranaGreenPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = step.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

                // Delivery Address Details
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Delivery Address", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(o.deliveryAddress.fullAddress, style = MaterialTheme.typography.bodyMedium)
                            if (o.deliveryAddress.instructions.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Note: ${o.deliveryAddress.instructions}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Order Items list
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Items in this Order (${o.items.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        for (item in o.items) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.productName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("${item.quantity} x ${formatNpr(item.price)} (${item.unit})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(formatNpr(item.total), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }
                    }
                }

                // Payment Summary
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Payment Method", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = if (o.paymentMethod == PaymentMethod.ESEWA) "eSewa Wallet (Paid 🟢)" else "Cash on Delivery (COD 💵)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (o.paymentMethod == PaymentMethod.ESEWA) EsewaGreen else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (o.esewaRefId != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("eSewa Ref Code", style = MaterialTheme.typography.labelSmall)
                                    Text(o.esewaRefId, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Amount", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(formatNpr(o.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = KiranaGreenPrimary)
                            }
                        }
                    }
                }

                // Back Button
                item {
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Back to Shopping", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }

    // Full Screen Live Delivery Map Modal
    if (isMapExpanded && order != null) {
        val o = order!!
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { isMapExpanded = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                LiveDeliveryMapView(
                    order = o,
                    onCallRider = {
                        if (o.assignedRiderPhone != null) {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${o.assignedRiderPhone}")
                            }
                            context.startActivity(intent)
                        }
                    },
                    isFullScreen = true,
                    onToggleFullScreen = { isMapExpanded = false },
                    modifier = Modifier.fillMaxSize()
                )

                IconButton(
                    onClick = { isMapExpanded = false },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 16.dp, start = 16.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Fullscreen Map", tint = Color.White)
                }
            }
        }
    }
}
