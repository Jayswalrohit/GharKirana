package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.model.UserRole
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.admin.AdminLoginScreen
import com.example.ui.customer.*
import com.example.ui.rider.RiderDashboardScreen
import com.example.ui.theme.KiranaGreenDark
import com.example.ui.theme.KiranaGreenLight
import com.example.ui.theme.KiranaGreenPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GroceryViewModel

enum class CustomerNavTab(
    val title: String,
    val nepaliTitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    HOME("Home", "पसल", Icons.Default.Storefront),
    CATEGORIES("Categories", "विधा", Icons.Default.Category),
    CART("Cart", "झोला", Icons.Default.ShoppingCart),
    ORDERS("Orders", "अर्डर", Icons.Default.ReceiptLong),
    PROFILE("Profile", "खाता", Icons.Default.Person)
}

sealed class AppDestination {
    data class CustomerRoot(val selectedTab: CustomerNavTab = CustomerNavTab.HOME) : AppDestination()
    object CustomerLoginSignup : AppDestination()
    object AdminLogin : AppDestination()
    object AdminMain : AppDestination()
    object RiderMain : AppDestination()
    data class OrderTracking(val orderId: String) : AppDestination()
}

class MainActivity : ComponentActivity() {

    private val viewModel: GroceryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                GharKiranaApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GharKiranaApp(viewModel: GroceryViewModel) {
    val currentRole by viewModel.currentRole.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val totalCartCount = cartItems.sumOf { it.quantity }

    var currentDestination by remember {
        mutableStateOf<AppDestination>(AppDestination.CustomerRoot(CustomerNavTab.HOME))
    }
    var activeCustomerTab by remember { mutableStateOf(CustomerNavTab.HOME) }
    var showCheckoutSheet by remember { mutableStateOf(false) }

    // Intercept back button when not at Home root
    BackHandler(
        enabled = currentDestination !is AppDestination.CustomerRoot || activeCustomerTab != CustomerNavTab.HOME
    ) {
        if (currentDestination is AppDestination.CustomerRoot) {
            activeCustomerTab = CustomerNavTab.HOME
        } else {
            currentDestination = AppDestination.CustomerRoot(CustomerNavTab.HOME)
        }
    }

    // Direct synchronization when role changes
    LaunchedEffect(currentRole) {
        when (currentRole) {
            UserRole.ADMIN -> currentDestination = AppDestination.AdminMain
            UserRole.RIDER -> currentDestination = AppDestination.RiderMain
            UserRole.CUSTOMER -> {
                if (currentDestination is AppDestination.AdminMain || currentDestination is AppDestination.RiderMain) {
                    currentDestination = AppDestination.CustomerRoot(CustomerNavTab.HOME)
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            // SHOW BOTTOM NAVIGATION ONLY IN CUSTOMER ROOT VIEW
            if (currentDestination is AppDestination.CustomerRoot && currentRole == UserRole.CUSTOMER) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.testTag("customer_bottom_nav")
                ) {
                    CustomerNavTab.values().forEach { tab ->
                        val isSelected = activeCustomerTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { activeCustomerTab = tab },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (tab == CustomerNavTab.CART && totalCartCount > 0) {
                                            Badge(
                                                containerColor = KiranaGreenPrimary,
                                                contentColor = Color.White
                                            ) {
                                                Text(
                                                    text = if (totalCartCount > 99) "99+" else totalCartCount.toString(),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        tint = if (isSelected) KiranaGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = tab.nepaliTitle,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) KiranaGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = KiranaGreenLight
                            )
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val dest = currentDestination) {
                // 1. ORDER TRACKING SCREEN
                is AppDestination.OrderTracking -> {
                    OrderTrackingScreen(
                        orderId = dest.orderId,
                        viewModel = viewModel,
                        onBack = { currentDestination = AppDestination.CustomerRoot(CustomerNavTab.ORDERS) }
                    )
                }

                // 2. CUSTOMER LOGIN & REGISTRATION (ONLY OPENS CUSTOMER APP)
                is AppDestination.CustomerLoginSignup -> {
                    LoginSignupScreen(
                        viewModel = viewModel,
                        onSuccess = {
                            currentDestination = AppDestination.CustomerRoot(CustomerNavTab.HOME)
                        },
                        onBack = {
                            currentDestination = AppDestination.CustomerRoot(CustomerNavTab.HOME)
                        }
                    )
                }

                // 3. ADMIN LOGIN SCREEN (DEDICATED PORTAL)
                is AppDestination.AdminLogin -> {
                    AdminLoginScreen(
                        viewModel = viewModel,
                        onSuccess = {
                            currentDestination = AppDestination.AdminMain
                        },
                        onBackToStore = {
                            currentDestination = AppDestination.CustomerRoot(CustomerNavTab.HOME)
                        }
                    )
                }

                // 4. ADMIN MAIN DASHBOARD
                is AppDestination.AdminMain -> {
                    AdminDashboardScreen(
                        viewModel = viewModel,
                        onViewOrderTracking = { orderId ->
                            currentDestination = AppDestination.OrderTracking(orderId)
                        },
                        onExitToCustomerStore = {
                            currentDestination = AppDestination.CustomerRoot(CustomerNavTab.HOME)
                        }
                    )
                }

                // 5. RIDER MAIN DASHBOARD
                is AppDestination.RiderMain -> {
                    RiderDashboardScreen(
                        viewModel = viewModel,
                        onViewOrderTracking = { orderId ->
                            currentDestination = AppDestination.OrderTracking(orderId)
                        }
                    )
                }

                // 6. CUSTOMER ROOT (5 DISTINCT EXPERIENCES)
                is AppDestination.CustomerRoot -> {
                    when (activeCustomerTab) {
                        CustomerNavTab.HOME -> {
                            CustomerHomeScreen(
                                viewModel = viewModel,
                                onOpenCart = { activeCustomerTab = CustomerNavTab.CART },
                                onOpenProfile = { activeCustomerTab = CustomerNavTab.PROFILE },
                                onOpenLogin = { currentDestination = AppDestination.CustomerLoginSignup },
                                onOpenOrderTracking = { orderId ->
                                    currentDestination = AppDestination.OrderTracking(orderId)
                                }
                            )
                        }

                        CustomerNavTab.CATEGORIES -> {
                            CategoriesScreen(
                                viewModel = viewModel,
                                onOpenCart = { activeCustomerTab = CustomerNavTab.CART }
                            )
                        }

                        CustomerNavTab.CART -> {
                            CustomerCartScreen(
                                viewModel = viewModel,
                                onProceedToCheckout = {
                                    showCheckoutSheet = true
                                },
                                onBrowseProducts = {
                                    activeCustomerTab = CustomerNavTab.HOME
                                }
                            )
                        }

                        CustomerNavTab.ORDERS -> {
                            CustomerOrdersScreen(
                                viewModel = viewModel,
                                onTrackOrder = { orderId ->
                                    currentDestination = AppDestination.OrderTracking(orderId)
                                },
                                onBrowseShop = {
                                    activeCustomerTab = CustomerNavTab.HOME
                                }
                            )
                        }

                        CustomerNavTab.PROFILE -> {
                            ProfileAndNotificationsScreen(
                                viewModel = viewModel,
                                onBack = { activeCustomerTab = CustomerNavTab.HOME },
                                onTrackOrder = { orderId ->
                                    currentDestination = AppDestination.OrderTracking(orderId)
                                },
                                onOpenLogin = { currentDestination = AppDestination.CustomerLoginSignup },
                                onOpenAdminLogin = { currentDestination = AppDestination.AdminLogin },
                                onEnterAdmin = { currentDestination = AppDestination.AdminMain }
                            )
                        }
                    }
                }
            }
        }

        // Checkout Modal Bottom Sheet
        if (showCheckoutSheet) {
            CheckoutBottomSheet(
                viewModel = viewModel,
                onOrderSuccess = { order ->
                    showCheckoutSheet = false
                    activeCustomerTab = CustomerNavTab.ORDERS
                    currentDestination = AppDestination.OrderTracking(order.id)
                },
                onDismiss = { showCheckoutSheet = false }
            )
        }
    }
}
