package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.model.UserRole
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.components.RoleSwitcherBar
import com.example.ui.customer.*
import com.example.ui.rider.RiderDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GroceryViewModel

sealed class AppDestination {
    object Main : AppDestination()
    object Profile : AppDestination()
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

@Composable
fun GharKiranaApp(viewModel: GroceryViewModel) {
    val currentRole by viewModel.currentRole.collectAsState()
    var currentDestination by remember { mutableStateOf<AppDestination>(AppDestination.Main) }
    var showCartSheet by remember { mutableStateOf(false) }
    var showCheckoutSheet by remember { mutableStateOf(false) }

    // Intercept back button when not in main view
    BackHandler(enabled = currentDestination !is AppDestination.Main) {
        currentDestination = AppDestination.Main
    }

    Scaffold(
        topBar = {
            RoleSwitcherBar(
                currentRole = currentRole,
                onRoleSelect = { role ->
                    viewModel.switchRole(role)
                    currentDestination = AppDestination.Main
                },
                modifier = Modifier.statusBarsPadding()
            )
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
                is AppDestination.OrderTracking -> {
                    OrderTrackingScreen(
                        orderId = dest.orderId,
                        viewModel = viewModel,
                        onBack = { currentDestination = AppDestination.Main }
                    )
                }

                is AppDestination.Profile -> {
                    ProfileAndNotificationsScreen(
                        viewModel = viewModel,
                        onBack = { currentDestination = AppDestination.Main },
                        onTrackOrder = { orderId ->
                            currentDestination = AppDestination.OrderTracking(orderId)
                        }
                    )
                }

                is AppDestination.Main -> {
                    when (currentRole) {
                        UserRole.CUSTOMER -> {
                            CustomerHomeScreen(
                                viewModel = viewModel,
                                onOpenCart = { showCartSheet = true },
                                onOpenProfile = { currentDestination = AppDestination.Profile },
                                onOpenOrderTracking = { orderId ->
                                    currentDestination = AppDestination.OrderTracking(orderId)
                                }
                            )
                        }

                        UserRole.ADMIN -> {
                            AdminDashboardScreen(
                                viewModel = viewModel,
                                onViewOrderTracking = { orderId ->
                                    currentDestination = AppDestination.OrderTracking(orderId)
                                }
                            )
                        }

                        UserRole.RIDER -> {
                            RiderDashboardScreen(
                                viewModel = viewModel,
                                onViewOrderTracking = { orderId ->
                                    currentDestination = AppDestination.OrderTracking(orderId)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Cart Modal Bottom Sheet
        if (showCartSheet) {
            CartBottomSheet(
                viewModel = viewModel,
                onProceedToCheckout = {
                    showCartSheet = false
                    showCheckoutSheet = true
                },
                onDismiss = { showCartSheet = false }
            )
        }

        // Checkout & Payment Modal Bottom Sheet
        if (showCheckoutSheet) {
            CheckoutBottomSheet(
                viewModel = viewModel,
                onOrderSuccess = { order ->
                    showCheckoutSheet = false
                    currentDestination = AppDestination.OrderTracking(order.id)
                },
                onDismiss = { showCheckoutSheet = false }
            )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

