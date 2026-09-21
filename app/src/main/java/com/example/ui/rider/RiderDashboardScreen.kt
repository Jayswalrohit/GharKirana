package com.example.ui.rider

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.Rider
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatNpr
import com.example.ui.theme.*
import com.example.ui.viewmodel.GroceryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiderDashboardScreen(
    viewModel: GroceryViewModel,
    onViewOrderTracking: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val riders by viewModel.availableRiders.collectAsState()
    val activeRiderId by viewModel.activeRiderId.collectAsState()
    val riderOrders by viewModel.riderOrders.collectAsState()
    val allOrders by viewModel.allOrders.collectAsState()

    val currentRider = riders.find { it.id == activeRiderId } ?: riders.firstOrNull()

    // Unassigned orders ready for pickup that rider can claim
    val unassignedReadyOrders = allOrders.filter {
        it.orderStatus == OrderStatus.READY_FOR_PICKUP && it.assignedRiderId == null
    }

    var showDeliverySuccessDialog by remember { mutableStateOf<Order?>(null) }

    Scaffold(modifier = modifier) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Rider Profile & Earnings Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = KiranaGreenPrimary)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🛵", fontSize = 24.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = currentRider?.name ?: "Delivery Rider",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${currentRider?.bikeNumber} • Janakpur Sector",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }

                            Surface(
                                color = SuccessGreenLight,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "ONLINE 🟢",
                                    color = SuccessGreen,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Today's Earnings", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                    Text(formatNpr(currentRider?.todayEarnings ?: 350.0), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Completed Trips", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                    Text("${currentRider?.totalDeliveries ?: 12} orders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Switch Rider Profile (for testing multi-rider dispatch)
            item {
                Column {
                    Text(
                        text = "Switch Active Delivery Rider (Nepal Fleet)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(riders) { r ->
                            FilterChip(
                                selected = r.id == activeRiderId,
                                onClick = { viewModel.setActiveRider(r.id) },
                                label = { Text("${r.name} (${r.bikeNumber.take(8)})") }
                            )
                        }
                    }
                }
            }

            // Available Pickups (Unassigned orders ready at shop)
            if (unassignedReadyOrders.isNotEmpty()) {
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚡", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Available Pickups at Kirana Shop (${unassignedReadyOrders.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        for (order in unassignedReadyOrders) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("#${order.orderNumber}", fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${formatNpr(order.total)} • ${order.paymentMethod.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "Deliver to: ${order.deliveryAddress.areaOrChowk} (${order.deliveryAddress.streetAddress})",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.assignRiderToOrder(order.id, activeRiderId)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = KiranaGreenPrimary)
                                    ) {
                                        Text("Accept & Claim Delivery (+Rs. 40)")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // My Assigned Orders
            item {
                Text(
                    text = "My Active & Assigned Deliveries (${riderOrders.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (riderOrders.isEmpty()) {
                item {
                    EmptyPlaceholder(
                        icon = { Text("🛵", fontSize = 32.sp) },
                        title = "No active deliveries assigned",
                        subtitle = "When the shop assigns an order or you claim one above, it will appear here."
                    )
                }
            } else {
                items(riderOrders) { order ->
                    RiderOrderCard(
                        order = order,
                        onCallCustomer = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${order.customerPhone}")
                            }
                            context.startActivity(intent)
                        },
                        onOpenMaps = {
                            val uri = Uri.parse("geo:0,0?q=${Uri.encode(order.deliveryAddress.fullAddress)}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(mapIntent)
                        },
                        onPickUp = { viewModel.riderPickUpOrder(order.id) },
                        onOutForDelivery = { viewModel.riderOutForDelivery(order.id) },
                        onDelivered = {
                            viewModel.riderMarkDelivered(order.id)
                            showDeliverySuccessDialog = order
                        },
                        onViewTracking = { onViewOrderTracking(order.id) }
                    )
                }
            }
        }
    }

    // Delivery Completed Dialog
    if (showDeliverySuccessDialog != null) {
        val o = showDeliverySuccessDialog!!
        AlertDialog(
            onDismissRequest = { showDeliverySuccessDialog = null },
            confirmButton = {
                Button(
                    onClick = { showDeliverySuccessDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = KiranaGreenPrimary)
                ) {
                    Text("Done")
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎉", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delivery Completed!")
                }
            },
            text = {
                Column {
                    Text("Order #${o.orderNumber} delivered successfully.")
                    Spacer(modifier = Modifier.height(6.dp))
                    if (o.paymentMethod == PaymentMethod.COD) {
                        Surface(
                            color = KiranaAmberLight,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💵 Cash Collected: ${formatNpr(o.total)}",
                                color = KiranaAmberDark,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = EsewaGreenLight,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🟢 Prepaid via eSewa (Ref: ${o.esewaRefId})",
                                color = EsewaGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Delivery fee of Rs. 40 added to your today's earnings.")
                }
            }
        )
    }
}

@Composable
fun RiderOrderCard(
    order: Order,
    onCallCustomer: () -> Unit,
    onOpenMaps: () -> Unit,
    onPickUp: () -> Unit,
    onOutForDelivery: () -> Unit,
    onDelivered: () -> Unit,
    onViewTracking: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order #${order.orderNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(status = order.orderStatus)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Customer Name & Phone
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = order.customerName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = order.customerPhone,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = onCallCustomer,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call Customer", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Address card
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📍 ${order.deliveryAddress.areaOrChowk}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = order.deliveryAddress.streetAddress,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onOpenMaps) {
                        Icon(Icons.Default.Navigation, contentDescription = "Maps", tint = KiranaGreenPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Payment to collect
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (order.paymentMethod == PaymentMethod.COD)
                        "💵 Cash to Collect: ${formatNpr(order.total)}"
                    else "🟢 Prepaid (eSewa): ${formatNpr(order.total)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (order.paymentMethod == PaymentMethod.COD) KiranaAmberDark else EsewaGreen
                )
                Text(
                    text = "${order.items.size} items",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            // Direct link to Live GPS Map Tracking for Rider
            OutlinedButton(
                onClick = onViewTracking,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp), tint = KiranaGreenPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Live GPS Route Map", color = KiranaGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            // Rider Action Buttons depending on OrderStatus
            when (order.orderStatus) {
                OrderStatus.RIDER_ASSIGNED, OrderStatus.READY_FOR_PICKUP -> {
                    Button(
                        onClick = onPickUp,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KiranaGreenPrimary)
                    ) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Picked Up from Kirana Shop")
                    }
                }

                OrderStatus.PICKED_UP -> {
                    Button(
                        onClick = onOutForDelivery,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KiranaGreenDark)
                    ) {
                        Icon(Icons.Default.TwoWheeler, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Driving (Out for Delivery)")
                    }
                }

                OrderStatus.OUT_FOR_DELIVERY -> {
                    Button(
                        onClick = onDelivered,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (order.paymentMethod == PaymentMethod.COD)
                                "Collect ${formatNpr(order.total)} & Mark Delivered"
                            else "Mark Delivered"
                        )
                    }
                }

                OrderStatus.DELIVERED -> {
                    Surface(
                        color = SuccessGreenLight,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "✅ Delivered Successfully",
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                else -> {
                    OutlinedButton(
                        onClick = onViewTracking,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("View Order Details")
                    }
                }
            }
        }
    }
}
