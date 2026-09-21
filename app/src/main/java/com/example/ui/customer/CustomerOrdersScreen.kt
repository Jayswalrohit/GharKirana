package com.example.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OrderStatus
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatNpr
import com.example.ui.theme.*
import com.example.ui.viewmodel.GroceryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerOrdersScreen(
    viewModel: GroceryViewModel,
    onTrackOrder: (String) -> Unit,
    onBrowseShop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val orders by viewModel.customerOrders.collectAsState()
    var selectedFilter by remember { mutableStateOf(0) } // 0: All, 1: Active, 2: Completed

    val filteredOrders = remember(orders, selectedFilter) {
        when (selectedFilter) {
            1 -> orders.filter { it.orderStatus != OrderStatus.DELIVERED && it.orderStatus != OrderStatus.CANCELLED }
            2 -> orders.filter { it.orderStatus == OrderStatus.DELIVERED || it.orderStatus == OrderStatus.CANCELLED }
            else -> orders
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "मेरा अर्डरहरू / My Orders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KiranaGreenDark
                        )
                        Text(
                            text = "Track live Kirana delivery and order history",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = KiranaGreenLight,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${orders.size} अर्डर",
                            color = KiranaGreenDark,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All (${orders.size})", "Active", "Past Orders").forEachIndexed { index, label ->
                        val isSelected = selectedFilter == index
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) KiranaGreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFilter = index }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }

        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EmptyPlaceholder(
                        icon = { Text("📋", fontSize = 38.sp) },
                        title = "कुनै अर्डर भेटिएन\nNo Orders Found",
                        subtitle = "When you order groceries, they will appear here with live tracking."
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onBrowseShop,
                        colors = ButtonDefaults.buttonColors(containerColor = KiranaGreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Shopping", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredOrders) { order ->
                    val isActive = order.orderStatus != OrderStatus.DELIVERED && order.orderStatus != OrderStatus.CANCELLED
                    val dateFormat = SimpleDateFormat("dd MMM, yyyy • hh:mm a", Locale.ENGLISH)
                    val dateStr = dateFormat.format(Date(order.createdAt))

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 3.dp else 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTrackOrder(order.id) }
                            .testTag("order_card_${order.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Order Number & Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = order.orderNumber,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = if (order.paymentMethod.name == "ESEWA") EsewaGreenLight else KiranaAmberLight,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = order.paymentMethod.title,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (order.paymentMethod.name == "ESEWA") KiranaGreenDark else KiranaAmberDark,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                StatusBadge(status = order.orderStatus)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                            // Ordered Items snippet
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                order.items.take(3).forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${item.quantity}x ${item.productName} (${item.unit})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = formatNpr(item.total),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (order.items.size > 3) {
                                    Text(
                                        text = "+ ${order.items.size - 3} more items",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KiranaGreenPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                            // Address & Total Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = KiranaGreenPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${order.deliveryAddress.areaOrChowk}, ${order.deliveryAddress.city}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Total: ${formatNpr(order.total)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = KiranaGreenDark
                                    )
                                }

                                Button(
                                    onClick = { onTrackOrder(order.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isActive) KiranaGreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isActive) "Track Order 🛵" else "View Details",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
