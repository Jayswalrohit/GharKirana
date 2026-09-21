package com.example.ui.admin

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.ProductImageView
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatNpr
import com.example.ui.theme.*
import com.example.ui.viewmodel.GroceryViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class AdminNavTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    PRODUCTS("Products", Icons.Default.Inventory2),
    CATEGORIES("Categories", Icons.Default.Category),
    ORDERS("Orders", Icons.Default.ReceiptLong),
    CUSTOMERS("Customers", Icons.Default.People),
    PAYMENTS("Payments", Icons.Default.AccountBalanceWallet),
    REPORTS("Reports", Icons.Default.BarChart),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: GroceryViewModel,
    onViewOrderTracking: (String) -> Unit,
    onExitToCustomerStore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allOrders by viewModel.allOrders.collectAsState()
    val products by viewModel.adminProducts.collectAsState()
    val riders by viewModel.availableRiders.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val adminCustomers by viewModel.adminCustomers.collectAsState()

    var currentTab by remember { mutableStateOf(AdminNavTab.DASHBOARD) }

    // Dialog States
    var showAddProductDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var selectedCustomerForHistory by remember { mutableStateOf<AdminCustomer?>(null) }
    var selectedOrderForDetails by remember { mutableStateOf<Order?>(null) }

    // Calculations for Dashboard KPIs
    val totalSales = allOrders.filter { it.orderStatus == OrderStatus.DELIVERED }.sumOf { it.total }
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
    }
    val todaysOrders = allOrders.filter { it.createdAt >= todayStart }
    val pendingOrders = allOrders.filter { it.orderStatus != OrderStatus.DELIVERED && it.orderStatus != OrderStatus.CANCELLED }
    val completedOrders = allOrders.filter { it.orderStatus == OrderStatus.DELIVERED }
    val lowStockProducts = products.filter { it.stock < 15 }

    Scaffold(
        topBar = {
            Surface(
                color = AdminSlate900,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF0284C7),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "GharKirana Merchant Console",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF0369A1),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "ADMIN",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Janakpur & Kathmandu Store Management",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.exitToCustomerStore()
                            onExitToCustomerStore()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444).copy(alpha = 0.85f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentTab == AdminNavTab.PRODUCTS) {
                FloatingActionButton(
                    onClick = { showAddProductDialog = true },
                    containerColor = Color(0xFF0284C7),
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
                .background(AdminSlate900)
        ) {
            // Horizontal Admin Navigation Tabs (All 8 Tabs)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AdminSlate800)
                    .padding(vertical = 6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(AdminNavTab.values()) { tab ->
                    val isSelected = currentTab == tab
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF0284C7) else Color.Transparent,
                        modifier = Modifier
                            .clickable { currentTab = tab }
                            .testTag("admin_tab_${tab.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF334155))

            // Main Content Area depending on currentTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentTab) {
                    AdminNavTab.DASHBOARD -> AdminDashboardView(
                        totalSales = totalSales,
                        todaysOrdersCount = todaysOrders.size,
                        pendingOrdersCount = pendingOrders.size,
                        completedOrdersCount = completedOrders.size,
                        totalCustomersCount = adminCustomers.size,
                        lowStockCount = lowStockProducts.size,
                        recentOrders = allOrders.take(5),
                        lowStockProducts = lowStockProducts,
                        onNavigateToOrders = { currentTab = AdminNavTab.ORDERS },
                        onNavigateToProducts = { currentTab = AdminNavTab.PRODUCTS },
                        onNavigateToReports = { currentTab = AdminNavTab.REPORTS },
                        onOrderClick = { selectedOrderForDetails = it }
                    )

                    AdminNavTab.PRODUCTS -> AdminProductsView(
                        products = products,
                        categories = categories,
                        onAddProduct = { showAddProductDialog = true },
                        onEditProduct = { productToEdit = it },
                        onDeleteProduct = { productToDelete = it }
                    )

                    AdminNavTab.CATEGORIES -> AdminCategoriesView(
                        categories = categories,
                        products = products
                    )

                    AdminNavTab.ORDERS -> AdminOrdersView(
                        orders = allOrders,
                        riders = riders,
                        onUpdateOrderStatus = { orderId, newStatus ->
                            viewModel.updateOrderStatus(orderId, newStatus)
                        },
                        onAssignRider = { orderId, riderId ->
                            viewModel.assignRiderToOrder(orderId, riderId)
                        },
                        onOrderClick = { selectedOrderForDetails = it }
                    )

                    AdminNavTab.CUSTOMERS -> AdminCustomersView(
                        customers = adminCustomers,
                        onViewCustomerHistory = { customer ->
                            selectedCustomerForHistory = customer
                        }
                    )

                    AdminNavTab.PAYMENTS -> AdminPaymentsView(
                        orders = allOrders,
                        totalSales = totalSales
                    )

                    AdminNavTab.REPORTS -> AdminReportsView(
                        orders = allOrders,
                        products = products,
                        customers = adminCustomers,
                        totalSales = totalSales
                    )

                    AdminNavTab.SETTINGS -> AdminSettingsView(
                        viewModel = viewModel,
                        onExit = {
                            viewModel.exitToCustomerStore()
                            onExitToCustomerStore()
                        }
                    )
                }
            }
        }
    }

    // Add Product Dialog
    if (showAddProductDialog) {
        AdminAddEditProductDialog(
            categories = categories,
            product = null,
            onSave = { newProd ->
                viewModel.addProduct(newProd)
                showAddProductDialog = false
            },
            onDismiss = { showAddProductDialog = false }
        )
    }

    // Edit Product Dialog
    if (productToEdit != null) {
        AdminAddEditProductDialog(
            categories = categories,
            product = productToEdit,
            onSave = { updatedProd ->
                viewModel.updateProduct(updatedProd)
                productToEdit = null
            },
            onDismiss = { productToEdit = null }
        )
    }

    // Delete Product Confirmation Dialog
    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Delete Product Confirmation", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete '${productToDelete?.name}' from the store catalog? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        productToDelete?.id?.let { viewModel.deleteProduct(it) }
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Customer Order History Dialog
    if (selectedCustomerForHistory != null) {
        AdminCustomerHistoryDialog(
            customer = selectedCustomerForHistory!!,
            onDismiss = { selectedCustomerForHistory = null },
            onViewOrder = { order ->
                selectedCustomerForHistory = null
                selectedOrderForDetails = order
            }
        )
    }

    // Order Detail & Status Update Dialog
    if (selectedOrderForDetails != null) {
        AdminOrderDetailDialog(
            order = selectedOrderForDetails!!,
            riders = riders,
            onUpdateStatus = { newStatus ->
                viewModel.updateOrderStatus(selectedOrderForDetails!!.id, newStatus)
                // refresh local order
                selectedOrderForDetails = allOrders.find { it.id == selectedOrderForDetails!!.id }?.copy(orderStatus = newStatus)
            },
            onAssignRider = { riderId ->
                viewModel.assignRiderToOrder(selectedOrderForDetails!!.id, riderId)
            },
            onDismiss = { selectedOrderForDetails = null }
        )
    }
}

// ==================== 1. DASHBOARD VIEW ====================
@Composable
fun AdminDashboardView(
    totalSales: Double,
    todaysOrdersCount: Int,
    pendingOrdersCount: Int,
    completedOrdersCount: Int,
    totalCustomersCount: Int,
    lowStockCount: Int,
    recentOrders: List<Order>,
    lowStockProducts: List<Product>,
    onNavigateToOrders: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToReports: () -> Unit,
    onOrderClick: (Order) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Store Performance Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // 6 KPI Metric Cards in Grid (2x3)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminKpiCard(
                        title = "Total Sales",
                        value = formatNpr(totalSales),
                        icon = Icons.Default.MonetizationOn,
                        accentColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    AdminKpiCard(
                        title = "Today's Orders",
                        value = "$todaysOrdersCount",
                        icon = Icons.Default.Today,
                        accentColor = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminKpiCard(
                        title = "Pending Orders",
                        value = "$pendingOrdersCount",
                        icon = Icons.Default.PendingActions,
                        accentColor = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                    AdminKpiCard(
                        title = "Completed Orders",
                        value = "$completedOrdersCount",
                        icon = Icons.Default.CheckCircle,
                        accentColor = Color(0xFF22C55E),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminKpiCard(
                        title = "Total Customers",
                        value = "$totalCustomersCount",
                        icon = Icons.Default.People,
                        accentColor = Color(0xFF818CF8),
                        modifier = Modifier.weight(1f)
                    )
                    AdminKpiCard(
                        title = "Low-Stock Alerts",
                        value = "$lowStockCount",
                        icon = Icons.Default.Warning,
                        accentColor = if (lowStockCount > 0) Color(0xFFEF4444) else Color(0xFF94A3B8),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Actions
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AdminSlate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Quick Management Actions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateToOrders,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Manage Orders", fontSize = 12.sp)
                        }
                        Button(
                            onClick = onNavigateToProducts,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Inventory", fontSize = 12.sp)
                        }
                        Button(
                            onClick = onNavigateToReports,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reports", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Low stock warning alerts
        if (lowStockProducts.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Low Stock Attention Needed (${lowStockProducts.size} Items)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFCA5A5)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        lowStockProducts.take(3).forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${p.emoji} ${p.name}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Only ${p.stock} left (${p.unit})",
                                    color = Color(0xFFF87171),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Orders List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Incoming Orders",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                TextButton(onClick = onNavigateToOrders) {
                    Text("View All", color = Color(0xFF38BDF8), fontSize = 12.sp)
                }
            }
        }

        items(recentOrders) { order ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = AdminSlate800),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOrderClick(order) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = order.orderNumber,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${order.customerName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${order.items.size} items • ${formatNpr(order.total)} • ${order.paymentMethod.title}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    StatusBadge(status = order.orderStatus)
                }
            }
        }
    }
}

@Composable
fun AdminKpiCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AdminSlate800),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ==================== 2. PRODUCTS VIEW ====================
@Composable
fun AdminProductsView(
    products: List<Product>,
    categories: List<Category>,
    onAddProduct: () -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCatId by remember { mutableStateOf<String?>(null) }

    val filtered = remember(products, searchQuery, selectedCatId) {
        var list = products
        if (selectedCatId != null) {
            list = list.filter { it.categoryId == selectedCatId }
        }
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.nepaliName.contains(searchQuery, ignoreCase = true) ||
                        it.brand.contains(searchQuery, ignoreCase = true)
            }
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search & Add Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search products by name, brand...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF38BDF8)) },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF475569)
                ),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = onAddProduct,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedCatId == null) Color(0xFF0284C7) else AdminSlate800,
                    modifier = Modifier.clickable { selectedCatId = null }
                ) {
                    Text(
                        text = "All (${products.size})",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            items(categories) { cat ->
                val isSelected = selectedCatId == cat.id
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(0xFF0284C7) else AdminSlate800,
                    modifier = Modifier.clickable { selectedCatId = cat.id }
                ) {
                    Text(
                        text = "${cat.emoji} ${cat.name}",
                        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Products List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered) { product ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AdminSlate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Product image / emoji
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AdminSlate900),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(product.emoji, fontSize = 24.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Product Details
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (product.hasDiscount) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color(0xFFEF4444),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "-${product.discountPercent}%",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            if (product.nepaliName.isNotBlank()) {
                                Text(
                                    text = product.nepaliName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = formatNpr(product.effectivePrice),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                                Text(
                                    text = "Pack: ${product.unit}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                                Surface(
                                    color = if (product.stock < 15) Color(0x33EF4444) else Color(0x2210B981),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Stock: ${product.stock}",
                                        color = if (product.stock < 15) Color(0xFFF87171) else Color(0xFF34D399),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Edit & Delete Action Icons
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { onEditProduct(product) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { onDeleteProduct(product) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFF87171), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== 3. CATEGORIES VIEW ====================
@Composable
fun AdminCategoriesView(
    categories: List<Category>,
    products: List<Product>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Product Categories Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${categories.size} Active Categories",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        items(categories) { cat ->
            val productCount = products.count { it.categoryId == cat.id }
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AdminSlate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = AdminSlate900,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(cat.emoji, fontSize = 22.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (cat.nepaliName.isNotBlank()) {
                                Text(
                                    text = cat.nepaliName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    Surface(
                        color = Color(0xFF0284C7).copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$productCount Items",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==================== 4. ORDERS VIEW ====================
@Composable
fun AdminOrdersView(
    orders: List<Order>,
    riders: List<Rider>,
    onUpdateOrderStatus: (String, OrderStatus) -> Unit,
    onAssignRider: (String, String) -> Unit,
    onOrderClick: (Order) -> Unit
) {
    var selectedStatusFilter by remember { mutableStateOf<OrderStatus?>(null) }

    val filteredOrders = remember(orders, selectedStatusFilter) {
        if (selectedStatusFilter == null) orders
        else orders.filter { it.orderStatus == selectedStatusFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Status Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedStatusFilter == null) Color(0xFF0284C7) else AdminSlate800,
                    modifier = Modifier.clickable { selectedStatusFilter = null }
                ) {
                    Text(
                        text = "All Orders (${orders.size})",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            items(OrderStatus.values()) { status ->
                val count = orders.count { it.orderStatus == status }
                val isSelected = selectedStatusFilter == status
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(0xFF0284C7) else AdminSlate800,
                    modifier = Modifier.clickable { selectedStatusFilter = status }
                ) {
                    Text(
                        text = "${status.title} ($count)",
                        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No orders in this status category", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredOrders) { order ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AdminSlate800),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOrderClick(order) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Header
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
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = if (order.paymentMethod == PaymentMethod.ESEWA) Color(0xFF15803D) else Color(0xFFB45309),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = order.paymentMethod.title,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                StatusBadge(status = order.orderStatus)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Customer & Address
                            Text(
                                text = "👤 Customer: ${order.customerName} • 📞 ${order.customerPhone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                            Text(
                                text = "📍 Delivery: ${order.deliveryAddress.areaOrChowk}, ${order.deliveryAddress.city}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Items summary
                            Text(
                                text = "Items: ${order.items.joinToString(", ") { "${it.quantity}x ${it.productName}" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFF334155))

                            // Total & Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = formatNpr(order.total),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF38BDF8)
                                    )
                                    Text(
                                        text = if (order.isPaid) "Paid" else "Payment Pending",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (order.isPaid) Color(0xFF34D399) else Color(0xFFFBBF24)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    when (order.orderStatus) {
                                        OrderStatus.PLACED -> {
                                            Button(
                                                onClick = { onUpdateOrderStatus(order.id, OrderStatus.CONFIRMED) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Confirm", fontSize = 12.sp)
                                            }
                                        }
                                        OrderStatus.CONFIRMED -> {
                                            Button(
                                                onClick = { onUpdateOrderStatus(order.id, OrderStatus.PREPARING) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Prepare", fontSize = 12.sp)
                                            }
                                        }
                                        OrderStatus.PREPARING -> {
                                            Button(
                                                onClick = { onUpdateOrderStatus(order.id, OrderStatus.READY_FOR_PICKUP) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Ready", fontSize = 12.sp)
                                            }
                                        }
                                        OrderStatus.READY_FOR_PICKUP -> {
                                            Button(
                                                onClick = { onUpdateOrderStatus(order.id, OrderStatus.OUT_FOR_DELIVERY) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Dispatch", fontSize = 12.sp)
                                            }
                                        }
                                        OrderStatus.OUT_FOR_DELIVERY -> {
                                            Button(
                                                onClick = { onUpdateOrderStatus(order.id, OrderStatus.DELIVERED) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Mark Delivered", fontSize = 12.sp)
                                            }
                                        }
                                        else -> {
                                            OutlinedButton(
                                                onClick = { onOrderClick(order) },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Details", fontSize = 12.sp, color = Color(0xFF38BDF8))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== 5. CUSTOMERS VIEW ====================
@Composable
fun AdminCustomersView(
    customers: List<AdminCustomer>,
    onViewCustomerHistory: (AdminCustomer) -> Unit
) {
    var customerSearch by remember { mutableStateOf("") }

    val filteredCustomers = remember(customers, customerSearch) {
        if (customerSearch.isBlank()) customers
        else customers.filter {
            it.name.contains(customerSearch, ignoreCase = true) ||
                    it.phone.contains(customerSearch, ignoreCase = true) ||
                    it.city.contains(customerSearch, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Registered GharKirana Customers (${customers.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = customerSearch,
            onValueChange = { customerSearch = it },
            placeholder = { Text("Search by customer name, phone, area...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF38BDF8)) },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF475569)
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredCustomers) { customer ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AdminSlate800),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewCustomerHistory(customer) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0284C7),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = customer.name.firstOrNull()?.uppercase() ?: "C",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = customer.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "📞 ${customer.phone} • ✉️ ${customer.email}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "📍 ${customer.address}, ${customer.city}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                color = Color(0xFF0369A1).copy(alpha = 0.4f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${customer.totalOrders} Orders",
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatNpr(customer.totalSpent),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== 6. PAYMENTS VIEW ====================
@Composable
fun AdminPaymentsView(
    orders: List<Order>,
    totalSales: Double
) {
    val esewaOrders = orders.filter { it.paymentMethod == PaymentMethod.ESEWA && it.orderStatus == OrderStatus.DELIVERED }
    val codOrders = orders.filter { it.paymentMethod == PaymentMethod.COD && it.orderStatus == OrderStatus.DELIVERED }

    val esewaTotal = esewaOrders.sumOf { it.total }
    val codTotal = codOrders.sumOf { it.total }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Revenue & Settlement Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AdminSlate800),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🇳🇵", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("eSewa Digital", style = MaterialTheme.typography.labelMedium, color = Color(0xFF34D399))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(formatNpr(esewaTotal), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${esewaOrders.size} settled transactions", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AdminSlate800),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💵", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cash on Delivery", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFBBF24))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(formatNpr(codTotal), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${codOrders.size} cash collections", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    }
                }
            }
        }

        item {
            Text(
                text = "Recent Transactions List",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        items(orders) { order ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = AdminSlate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Ref #${order.orderNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${order.customerName} • ${order.paymentMethod.title}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatNpr(order.total),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        Text(
                            text = if (order.isPaid) "Verified ✓" else "Pending Collection",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (order.isPaid) Color(0xFF34D399) else Color(0xFFFBBF24)
                        )
                    }
                }
            }
        }
    }
}

// ==================== 7. REPORTS VIEW ====================
@Composable
fun AdminReportsView(
    orders: List<Order>,
    products: List<Product>,
    customers: List<AdminCustomer>,
    totalSales: Double
) {
    val deliveredOrders = orders.filter { it.orderStatus == OrderStatus.DELIVERED }
    val avgOrderValue = if (deliveredOrders.isNotEmpty()) totalSales / deliveredOrders.size else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "GharKirana Analytics & Business Reports",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Summary Metric Rows
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AdminSlate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Executive KPIs Summary", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.titleSmall)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gross Delivered Revenue", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                        Text(formatNpr(totalSales), color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Average Order Value (AOV)", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                        Text(formatNpr(avgOrderValue), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Order Fulfillment Rate", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                        val rate = if (orders.isNotEmpty()) (deliveredOrders.size.toDouble() / orders.size * 100).toInt() else 0
                        Text("$rate%", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Active Registered Customers", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                        Text("${customers.size}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Active Products Listed", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                        Text("${products.size}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Top Selling Products in Store
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AdminSlate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Top Selling Kirana Staples",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    products.take(5).forEachIndexed { index, p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("#${index + 1}", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${p.emoji} ${p.name}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(formatNpr(p.effectivePrice), color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

// ==================== 8. SETTINGS VIEW ====================
@Composable
fun AdminSettingsView(
    viewModel: GroceryViewModel,
    onExit: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Store Configuration & Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AdminSlate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Store Identity", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Text("Store Name: GharKirana Nepal", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)
                    Text("Operational Hubs: Janakpur Dham & Kathmandu Valley", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)
                    Text("Support Phone: +977 9841001122", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)
                    Text("Operating Hours: 06:00 AM - 09:00 PM Daily", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AdminSlate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Delivery & Order Rules", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Text("Standard Delivery Fee: Rs. 40", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)
                    Text("Free Delivery Threshold: Orders above Rs. 899", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)
                    Text("Payment Methods Accepted: eSewa Digital & Cash on Delivery (COD)", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out & Exit Admin Console", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==================== HELPER DIALOGS ====================

@Composable
fun AdminAddEditProductDialog(
    categories: List<Category>,
    product: Product?,
    onSave: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    val isEdit = product != null

    var name by remember { mutableStateOf(product?.name ?: "") }
    var nepaliName by remember { mutableStateOf(product?.nepaliName ?: "") }
    var brand by remember { mutableStateOf(product?.brand ?: "") }
    var unit by remember { mutableStateOf(product?.unit ?: "1 KG") }
    var priceStr by remember { mutableStateOf(product?.price?.toString() ?: "120.0") }
    var discountPercentStr by remember { mutableStateOf(product?.discountPercent?.toString() ?: "0") }
    var stockStr by remember { mutableStateOf(product?.stock?.toString() ?: "50") }
    var selectedCatId by remember { mutableStateOf(product?.categoryId ?: categories.firstOrNull()?.id ?: "") }
    var emoji by remember { mutableStateOf(product?.emoji ?: "🌾") }
    var description by remember { mutableStateOf(product?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEdit) "Edit Kirana Product" else "Add New Kirana Product", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Product Name (English) *") },
                        placeholder = { Text("e.g. Basmati Rice Special") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = nepaliName,
                        onValueChange = { nepaliName = it },
                        label = { Text("Product Name (Nepali)") },
                        placeholder = { Text("e.g. बासमती चामल") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            label = { Text("Brand") },
                            placeholder = { Text("e.g. DDC / Dhara") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Pack Size / Unit *") },
                            placeholder = { Text("e.g. 1 KG / 1 L") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it },
                            label = { Text("Price (Rs.) *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = discountPercentStr,
                            onValueChange = { discountPercentStr = it },
                            label = { Text("Discount %") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stockStr,
                            onValueChange = { stockStr = it },
                            label = { Text("Stock Quantity *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = emoji,
                            onValueChange = { emoji = it },
                            label = { Text("Emoji Icon") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Text("Category *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val isSelected = selectedCatId == cat.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) KiranaGreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { selectedCatId = cat.id }
                            ) {
                                Text(
                                    text = "${cat.emoji} ${cat.name}",
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        placeholder = { Text("Fresh authentic Nepali grocery staple") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val price = priceStr.toDoubleOrNull() ?: 100.0
                    val discount = discountPercentStr.toIntOrNull() ?: 0
                    val stock = stockStr.toIntOrNull() ?: 20
                    val catName = categories.find { it.id == selectedCatId }?.name ?: "Staples"

                    val calculatedDiscountPrice = if (discount > 0) price * (1.0 - discount / 100.0) else null
                    val saved = if (isEdit) {
                        product!!.copy(
                            name = name,
                            nepaliName = nepaliName,
                            brand = brand,
                            unit = unit,
                            price = price,
                            discountPrice = calculatedDiscountPrice,
                            stock = stock,
                            categoryId = selectedCatId,
                            categoryName = catName,
                            emoji = emoji,
                            description = description
                        )
                    } else {
                        Product(
                            id = "prod_${System.currentTimeMillis()}",
                            name = name,
                            nepaliName = nepaliName,
                            brand = brand,
                            unit = unit,
                            price = price,
                            discountPrice = calculatedDiscountPrice,
                            stock = stock,
                            categoryId = selectedCatId,
                            categoryName = catName,
                            emoji = emoji,
                            description = description
                        )
                    }
                    onSave(saved)
                },
                colors = ButtonDefaults.buttonColors(containerColor = KiranaGreenPrimary)
            ) {
                Text(if (isEdit) "Update Product" else "Save Product")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AdminCustomerHistoryDialog(
    customer: AdminCustomer,
    onDismiss: () -> Unit,
    onViewOrder: (Order) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = KiranaGreenPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(customer.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Order History • ${customer.orders.size} Total Orders", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("📞 Phone: ${customer.phone}", style = MaterialTheme.typography.bodySmall)
                            Text("📍 Address: ${customer.address}, ${customer.city}", style = MaterialTheme.typography.bodySmall)
                            Text("💰 Lifetime Spend: ${formatNpr(customer.totalSpent)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(customer.orders) { order ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewOrder(order) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("#${order.orderNumber}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("${order.items.size} items • ${formatNpr(order.total)}", style = MaterialTheme.typography.labelSmall)
                            }
                            StatusBadge(status = order.orderStatus)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun AdminOrderDetailDialog(
    order: Order,
    riders: List<Rider>,
    onUpdateStatus: (OrderStatus) -> Unit,
    onAssignRider: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Order #${order.orderNumber}", fontWeight = FontWeight.Bold)
                StatusBadge(status = order.orderStatus)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("Customer: ${order.customerName} (${order.customerPhone})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Address: ${order.deliveryAddress.areaOrChowk}, ${order.deliveryAddress.city}", style = MaterialTheme.typography.bodySmall)
                    Text("Payment: ${order.paymentMethod.title} • Total: ${formatNpr(order.total)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Ordered Products", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }

                items(order.items) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${item.quantity}x ${item.productName}", style = MaterialTheme.typography.bodySmall)
                        Text(formatNpr(item.total), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Update Status to:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OrderStatus.values().forEach { st ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (order.orderStatus == st) KiranaGreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUpdateStatus(st) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = st.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (order.orderStatus == st) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (order.orderStatus == st) {
                                        Text("Current ✓", color = Color.White, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done") }
        }
    )
}
