package com.example.ui.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.esewa.EsewaService
import com.example.data.model.*
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.MarketPresets
import com.example.ui.components.ProductImageView
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatNpr
import com.example.ui.theme.*
import com.example.ui.viewmodel.GroceryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: GroceryViewModel,
    onViewOrderTracking: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allOrders by viewModel.allOrders.collectAsState()
    val products by viewModel.adminProducts.collectAsState()
    val riders by viewModel.availableRiders.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Orders, 1: Inventory, 2: Analytics & Shop
    var orderStatusFilter by remember { mutableStateOf<OrderStatus?>(null) }
    var selectedOrderForAssignRider by remember { mutableStateOf<Order?>(null) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var showAddProductDialog by remember { mutableStateOf(false) }

    val totalSales = allOrders.filter { it.orderStatus == OrderStatus.DELIVERED }.sumOf { it.total }
    val activeOrdersCount = allOrders.count { it.orderStatus != OrderStatus.DELIVERED && it.orderStatus != OrderStatus.CANCELLED }
    val lowStockCount = products.count { it.stock < 15 }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showAddProductDialog = true },
                    containerColor = KiranaGreenPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("admin_add_product_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Product")
                }
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // KPI Stats Overview Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "GharKirana Merchant Panel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Janakpur Dham Central Store #1",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = SuccessGreenLight,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "STORE OPEN",
                                color = SuccessGreen,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(
                            title = "Today's Sales",
                            value = formatNpr(totalSales),
                            icon = "💰",
                            color = KiranaGreenLight,
                            textColor = KiranaGreenDark,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Active Orders",
                            value = activeOrdersCount.toString(),
                            icon = "📦",
                            color = KiranaAmberLight,
                            textColor = KiranaAmberDark,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Low Stock",
                            value = lowStockCount.toString(),
                            icon = "⚠️",
                            color = ErrorRedLight,
                            textColor = ErrorRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Tabs Header
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Orders (${allOrders.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Products (${products.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Shop Info", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Storefront, contentDescription = null) }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // Orders Management
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Filter Pills
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = orderStatusFilter == null,
                                    onClick = { orderStatusFilter = null },
                                    label = { Text("All (${allOrders.size})") }
                                )
                            }
                            items(OrderStatus.values()) { status ->
                                val count = allOrders.count { it.orderStatus == status }
                                if (count > 0) {
                                    FilterChip(
                                        selected = orderStatusFilter == status,
                                        onClick = { orderStatusFilter = status },
                                        label = { Text("${status.title} ($count)") }
                                    )
                                }
                            }
                        }

                        val filteredOrders = if (orderStatusFilter == null) allOrders
                        else allOrders.filter { it.orderStatus == orderStatusFilter }

                        if (filteredOrders.isEmpty()) {
                            EmptyPlaceholder(
                                icon = { Text("📦", fontSize = 32.sp) },
                                title = "No orders in this category",
                                subtitle = "Orders will appear here as customers buy groceries."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredOrders) { order ->
                                    AdminOrderCard(
                                        order = order,
                                        onAccept = { viewModel.confirmOrder(order.id) },
                                        onPrepare = { viewModel.prepareOrder(order.id) },
                                        onReady = { viewModel.markOrderReady(order.id) },
                                        onAssignRiderClick = { selectedOrderForAssignRider = order },
                                        onCancel = { viewModel.cancelOrder(order.id) },
                                        onTrackClick = { onViewOrderTracking(order.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Products & Stock Management
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "Inventory & Stock Levels",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(products) { product ->
                            AdminProductRow(
                                product = product,
                                onStockAdjust = { delta -> viewModel.adjustStock(product.id, delta) },
                                onEdit = { productToEdit = product },
                                onDelete = { viewModel.deleteProduct(product.id) }
                            )
                        }
                    }
                }

                2 -> {
                    // Shop Settings & Info
                    ShopSettingsView(
                        totalOrders = allOrders.size,
                        totalDelivered = allOrders.count { it.orderStatus == OrderStatus.DELIVERED },
                        totalSales = totalSales,
                        activeRiders = riders.size
                    )
                }
            }
        }
    }

    // Assign Rider Dialog
    if (selectedOrderForAssignRider != null) {
        AssignRiderDialog(
            order = selectedOrderForAssignRider!!,
            riders = riders,
            onAssign = { riderId ->
                viewModel.assignRiderToOrder(selectedOrderForAssignRider!!.id, riderId)
                selectedOrderForAssignRider = null
            },
            onDismiss = { selectedOrderForAssignRider = null }
        )
    }

    // Add / Edit Product Dialog
    if (showAddProductDialog || productToEdit != null) {
        AddEditProductDialog(
            product = productToEdit,
            categories = categories,
            onSave = { newProduct ->
                viewModel.saveProduct(newProduct)
                showAddProductDialog = false
                productToEdit = null
            },
            onDismiss = {
                showAddProductDialog = false
                productToEdit = null
            }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: String,
    color: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(icon, fontSize = 16.sp)
                Text(title, style = MaterialTheme.typography.labelSmall, color = textColor, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
fun AdminOrderCard(
    order: Order,
    onAccept: () -> Unit,
    onPrepare: () -> Unit,
    onReady: () -> Unit,
    onAssignRiderClick: () -> Unit,
    onCancel: () -> Unit,
    onTrackClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${order.orderNumber}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (order.paymentMethod == PaymentMethod.ESEWA) "eSewa (Paid 🟢)" else "COD (Cash 💵)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (order.paymentMethod == PaymentMethod.ESEWA) EsewaGreen else KiranaAmberDark
                    )
                }
                StatusBadge(status = order.orderStatus)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Customer info
            Text(
                text = "${order.customerName} • ${order.customerPhone}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${order.deliveryAddress.streetAddress}, ${order.deliveryAddress.areaOrChowk}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Order items list preview
            Text(
                text = order.items.joinToString(", ") { "${it.quantity}x ${it.productName}" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Total and Rider info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total: ${formatNpr(order.total)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = KiranaGreenPrimary
                )
                if (order.assignedRiderName != null) {
                    Text(
                        text = "Rider: ${order.assignedRiderName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = KiranaGreenDark
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            // Status action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (order.orderStatus) {
                    OrderStatus.PLACED -> {
                        Button(
                            onClick = onAccept,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KiranaGreenPrimary)
                        ) {
                            Text("Accept Order", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = onCancel,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                        ) {
                            Text("Reject", fontSize = 12.sp)
                        }
                    }

                    OrderStatus.CONFIRMED -> {
                        Button(
                            onClick = onPrepare,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KiranaAmberPrimary)
                        ) {
                            Text("Start Packing (Prepare)", fontSize = 12.sp)
                        }
                    }

                    OrderStatus.PREPARING -> {
                        Button(
                            onClick = onReady,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KiranaGreenPrimary)
                        ) {
                            Text("Mark Ready for Pickup", fontSize = 12.sp)
                        }
                    }

                    OrderStatus.READY_FOR_PICKUP -> {
                        Button(
                            onClick = onAssignRiderClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KiranaGreenDark)
                        ) {
                            Icon(Icons.Default.TwoWheeler, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Assign Delivery Rider", fontSize = 12.sp)
                        }
                    }

                    else -> {
                        Button(
                            onClick = onTrackClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KiranaGreenPrimary)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Track Rider on Live Map", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminProductRow(
    product: Product,
    onStockAdjust: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                ProductImageView(
                    product = product,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    emojiSize = 22.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${product.unit} • ${formatNpr(product.effectivePrice)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Stock: ${product.stock} units",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (product.stock < 15) ErrorRed else KiranaGreenPrimary
                    )
                }
            }

            // Quick stock modifier buttons & edit
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onStockAdjust(-1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease stock", modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = { onStockAdjust(5) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add 5 stock", modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = KiranaGreenPrimary, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun AssignRiderDialog(
    order: Order,
    riders: List<Rider>,
    onAssign: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = {
            Text("Assign Delivery Partner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Order #${order.orderNumber} to ${order.deliveryAddress.areaOrChowk}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(riders) { rider ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onAssign(rider.id) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🛵", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(rider.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("${rider.bikeNumber} • ${rider.currentArea}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Surface(
                                    color = SuccessGreenLight,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Assign",
                                        color = SuccessGreen,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun AddEditProductDialog(
    product: Product?,
    categories: List<Category>,
    onSave: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var nepaliName by remember { mutableStateOf(product?.nepaliName ?: "") }
    var categoryId by remember { mutableStateOf(product?.categoryId ?: categories.firstOrNull()?.id ?: "cat_rice_dal") }
    var priceStr by remember { mutableStateOf(product?.price?.toString() ?: "100") }
    var discountPriceStr by remember { mutableStateOf(product?.discountPrice?.toString() ?: "") }
    var stockStr by remember { mutableStateOf(product?.stock?.toString() ?: "30") }
    var unit by remember { mutableStateOf(product?.unit ?: "1 KG") }
    var brand by remember { mutableStateOf(product?.brand ?: "") }
    var emoji by remember { mutableStateOf(product?.emoji ?: "🛒") }
    var imageUrl by remember { mutableStateOf(product?.imageUrl ?: "") }
    var imageResName by remember { mutableStateOf(product?.imageResName) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUrl = uri.toString()
            imageResName = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val price = priceStr.toDoubleOrNull() ?: 100.0
                    val discountPrice = discountPriceStr.toDoubleOrNull()
                    val stock = stockStr.toIntOrNull() ?: 20
                    val selectedCategoryName = categories.find { it.id == categoryId }?.name ?: "Groceries"

                    onSave(
                        Product(
                            id = product?.id ?: "",
                            name = name,
                            nepaliName = nepaliName,
                            categoryId = categoryId,
                            categoryName = selectedCategoryName,
                            price = price,
                            discountPrice = discountPrice,
                            stock = stock,
                            unit = unit,
                            brand = brand,
                            description = "Fresh Kirana grocery staple.",
                            emoji = emoji,
                            imageUrl = imageUrl.ifBlank { null },
                            imageResName = imageResName,
                            isFeatured = true,
                            isActive = true
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = KiranaGreenPrimary)
            ) {
                Text(if (product == null) "Add Product" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = {
            Text(if (product == null) "Add Real Market Product" else "Edit Market Product", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Live Visual Card Preview
                item {
                    val previewPrice = priceStr.toDoubleOrNull() ?: 100.0
                    val previewDiscount = discountPriceStr.toDoubleOrNull()
                    val previewProduct = Product(
                        id = product?.id ?: "preview",
                        name = name.ifBlank { "Product Name" },
                        nepaliName = nepaliName,
                        categoryId = categoryId,
                        categoryName = categories.find { it.id == categoryId }?.name ?: "",
                        price = previewPrice,
                        discountPrice = previewDiscount,
                        stock = stockStr.toIntOrNull() ?: 20,
                        unit = unit,
                        brand = brand,
                        emoji = emoji,
                        imageUrl = imageUrl.ifBlank { null },
                        imageResName = imageResName
                    )

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProductImageView(
                                product = previewProduct,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                                emojiSize = 30.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = previewProduct.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (nepaliName.isNotBlank()) {
                                    Text(
                                        text = nepaliName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = formatNpr(previewProduct.effectivePrice),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = KiranaGreenPrimary
                                    )
                                    if (previewProduct.hasDiscount) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = formatNpr(previewProduct.price),
                                            style = MaterialTheme.typography.labelSmall,
                                            textDecoration = TextDecoration.LineThrough,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "(-${previewProduct.discountPercent}%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ErrorRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Real Market Image Selection
                item {
                    Text(
                        text = "Real Product Image",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Option 1: Pick from Device
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick Photo from Gallery")
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Option 2: Preloaded Market Product Photos
                    Text(
                        text = "Or choose real market item photo:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(MarketPresets.presets) { preset ->
                            val isSelected = imageResName == preset.key
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    imageResName = preset.key
                                    imageUrl = ""
                                    // Auto-suggest market info if adding new product
                                    if (product == null && name.isBlank()) {
                                        name = preset.label
                                        nepaliName = preset.nepaliLabel
                                        categoryId = preset.suggestedCategory
                                        priceStr = (preset.typicalPriceNpr * 1.1).toInt().toString()
                                        discountPriceStr = preset.typicalPriceNpr.toInt().toString()
                                        unit = preset.typicalUnit
                                        brand = preset.typicalBrand
                                    }
                                },
                                label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Option 3: Image URL input
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = {
                            imageUrl = it
                            if (it.isNotBlank()) imageResName = null
                        },
                        label = { Text("Image Web URL (Optional)") },
                        placeholder = { Text("https://example.com/product.jpg") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Market Pricing Section
                item {
                    Divider()
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Market Pricing (NPR)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Nepal Retail Rates",
                            style = MaterialTheme.typography.labelSmall,
                            color = KiranaGreenPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it },
                            label = { Text("MRP / Regular (Rs.)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = discountPriceStr,
                            onValueChange = { discountPriceStr = it },
                            label = { Text("Selling Offer (Rs.)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Price suggestions chips
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(MarketPresets.presets) { preset ->
                            SuggestionChip(
                                onClick = {
                                    priceStr = (preset.typicalPriceNpr * 1.1).toInt().toString()
                                    discountPriceStr = preset.typicalPriceNpr.toInt().toString()
                                    unit = preset.typicalUnit
                                },
                                label = {
                                    Text(
                                        "${preset.label.split(" ").take(2).joinToString(" ")}: Rs. ${preset.typicalPriceNpr.toInt()}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                }

                // General Product Details
                item {
                    Divider()
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Product Name (English)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = nepaliName,
                        onValueChange = { nepaliName = it },
                        label = { Text("Product Name (Nepali)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stockStr,
                            onValueChange = { stockStr = it },
                            label = { Text("Stock Qty") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit (e.g. 5 KG)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            label = { Text("Brand (e.g. Hulas)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = emoji,
                            onValueChange = { emoji = it },
                            label = { Text("Emoji") },
                            modifier = Modifier.weight(0.5f),
                            singleLine = true
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun ShopSettingsView(
    totalOrders: Int,
    totalDelivered: Int,
    totalSales: Double,
    activeRiders: Int
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Store Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Shop Name: GharKirana Express (Janakpur)", style = MaterialTheme.typography.bodyMedium)
                    Text("Address: Station Road, Near Bhanu Chowk, Janakpur Dham", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Phone: +977-9800012345", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Timings: 6:00 AM - 10:00 PM", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Coverage: Bhanu Chowk, Station Road, Ramanand Chowk (25 mins)", style = MaterialTheme.typography.bodySmall, color = KiranaGreenPrimary)
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Nepal eSewa Payment Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Merchant ID: ${EsewaService.getConfig().merchantId}", style = MaterialTheme.typography.bodySmall)
                    Text("Environment: ${EsewaService.getConfig().environment} (Safe Sandbox)", style = MaterialTheme.typography.bodySmall)
                    Text("Signature: HMAC-SHA256 (Server verified)", style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
                    Text("COD Enabled: Yes (Cash collected by delivery riders)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
