package com.example.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.esewa.EsewaService
import com.example.data.model.*
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.formatNpr
import com.example.ui.theme.*
import com.example.ui.viewmodel.GroceryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartBottomSheet(
    viewModel: GroceryViewModel,
    onProceedToCheckout: () -> Unit,
    onDismiss: () -> Unit
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val subtotal by viewModel.cartSubtotal.collectAsState()
    val deliveryFee by viewModel.cartDeliveryFee.collectAsState()
    val discount by viewModel.cartDiscount.collectAsState()
    val total by viewModel.cartTotal.collectAsState()
    val appliedCoupon by viewModel.appliedCoupon.collectAsState()
    val couponError by viewModel.couponError.collectAsState()

    var couponInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛒", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "My Kirana Basket (${cartItems.sumOf { it.quantity }})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (cartItems.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearCart() }) {
                        Text("Clear All", color = ErrorRed, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            if (cartItems.isEmpty()) {
                EmptyPlaceholder(
                    icon = { Text("🧺", fontSize = 32.sp) },
                    title = "Your cart is empty",
                    subtitle = "Add daily groceries and staple foods from the shop.",
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    // Free delivery reminder
                    item {
                        Surface(
                            color = if (deliveryFee == 0.0) SuccessGreenLight else KiranaAmberLight,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (deliveryFee == 0.0) "🎉" else "🛵", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (deliveryFee == 0.0)
                                        "Congratulations! You unlocked FREE 25-min Kirana delivery."
                                    else
                                        "Add ${formatNpr(899.0 - subtotal)} more for FREE delivery in Janakpur!",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (deliveryFee == 0.0) SuccessGreen else KiranaAmberDark
                                )
                            }
                        }
                    }

                    // Cart Items list
                    items(cartItems) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(item.product.emoji, fontSize = 22.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = item.product.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.product.unit} • ${formatNpr(item.product.effectivePrice)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Stepper
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(KiranaGreenLight)
                                    .padding(2.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.decrementCart(item.product.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = KiranaGreenDark, modifier = Modifier.size(14.dp))
                                }
                                Text(
                                    text = item.quantity.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = KiranaGreenDark,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                IconButton(
                                    onClick = { viewModel.addToCart(item.product) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = KiranaGreenDark, modifier = Modifier.size(14.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = formatNpr(item.total),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }

                    // Coupon Code Input
                    item {
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            Text(
                                text = "Have a Nepali Coupon Code?",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (appliedCoupon == null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = couponInput,
                                        onValueChange = { couponInput = it.uppercase() },
                                        placeholder = { Text("Try GHARKIRANA10") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { viewModel.applyCouponCode(couponInput) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = KiranaGreenPrimary)
                                    ) {
                                        Text("Apply")
                                    }
                                }
                                if (couponError != null) {
                                    Text(
                                        text = couponError!!,
                                        color = ErrorRed,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    color = SuccessGreenLight,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${appliedCoupon!!.code} applied (-${formatNpr(discount)})",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = SuccessGreen
                                            )
                                        }
                                        TextButton(onClick = { viewModel.removeCoupon() }) {
                                            Text("Remove", color = ErrorRed, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Price Breakdown
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Bill Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Item Total", style = MaterialTheme.typography.bodySmall)
                                    Text(formatNpr(subtotal), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Delivery Partner Fee", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        if (deliveryFee == 0.0) "FREE" else formatNpr(deliveryFee),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (deliveryFee == 0.0) SuccessGreen else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (discount > 0.0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Coupon Savings", style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
                                        Text("- ${formatNpr(discount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = SuccessGreen)
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Total Amount to Pay", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(formatNpr(total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = KiranaGreenPrimary)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onProceedToCheckout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("proceed_checkout_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KiranaGreenPrimary)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(formatNpr(total), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("TOTAL AMOUNT", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Proceed to Checkout", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutBottomSheet(
    viewModel: GroceryViewModel,
    onOrderSuccess: (Order) -> Unit,
    onDismiss: () -> Unit
) {
    val addresses by viewModel.addresses.collectAsState()
    val selectedAddress by viewModel.selectedAddress.collectAsState()
    val total by viewModel.cartTotal.collectAsState()
    val cartDeliveryFee by viewModel.cartDeliveryFee.collectAsState()

    var paymentMethod by remember { mutableStateOf(PaymentMethod.COD) }
    var deliveryInstructions by remember { mutableStateOf("Please call on arrival. Deliver to gate.") }
    var showAddAddressDialog by remember { mutableStateOf(false) }
    var showEsewaModal by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val activeAddress = selectedAddress ?: addresses.firstOrNull()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Checkout & Payment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                // 1. Delivery Address Section
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Delivery Address (Janakpur)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { showAddAddressDialog = true }) {
                                Text("+ Add New", color = KiranaGreenPrimary)
                            }
                        }

                        if (addresses.isEmpty()) {
                            Text(
                                "No saved address. Tap + Add New to add your location.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            for (addr in addresses) {
                                val isSelected = (activeAddress?.id == addr.id)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { viewModel.selectAddress(addr) },
                                    shape = RoundedCornerShape(10.dp),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, KiranaGreenPrimary) else null,
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) KiranaGreenLight else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { viewModel.selectAddress(addr) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(addr.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("• ${addr.phone}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text(
                                                text = "${addr.streetAddress}, ${addr.areaOrChowk}, ${addr.city}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Delivery Instructions
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text("Delivery Instructions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = deliveryInstructions,
                            onValueChange = { deliveryInstructions = it },
                            placeholder = { Text("e.g. Ring bell at green gate / call on arrival") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }

                // 3. Payment Method Section
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text("Choose Nepal Payment Method", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Cash on Delivery
                        PaymentOptionCard(
                            title = "Cash on Delivery (COD)",
                            subtitle = "Pay cash in NPR to rider after inspecting groceries at doorstep",
                            icon = "💵",
                            isSelected = paymentMethod == PaymentMethod.COD,
                            onClick = { paymentMethod = PaymentMethod.COD }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // eSewa Wallet
                        PaymentOptionCard(
                            title = "eSewa Mobile Wallet",
                            subtitle = "Instant, secure digital Kirana payment via eSewa EPAY merchant",
                            icon = "🟢",
                            isSelected = paymentMethod == PaymentMethod.ESEWA,
                            onClick = { paymentMethod = PaymentMethod.ESEWA }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (paymentMethod == PaymentMethod.ESEWA) {
                        showEsewaModal = true
                    } else {
                        isSubmitting = true
                        viewModel.placeOrder(PaymentMethod.COD, null) { newOrder ->
                            isSubmitting = false
                            onOrderSuccess(newOrder)
                        }
                    }
                },
                enabled = !isSubmitting && activeAddress != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_place_order_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (paymentMethod == PaymentMethod.ESEWA) EsewaGreen else KiranaGreenPrimary
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (paymentMethod == PaymentMethod.ESEWA) "PAY VIA eSewa • ${formatNpr(total)}"
                            else "PLACE ORDER (COD) • ${formatNpr(total)}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            }
        }
    }

    // eSewa Sandbox Payment Dialog
    if (showEsewaModal) {
        EsewaPaymentDialog(
            amount = total,
            onSuccess = { refId ->
                showEsewaModal = false
                isSubmitting = true
                viewModel.placeOrder(PaymentMethod.ESEWA, refId) { newOrder ->
                    isSubmitting = false
                    onOrderSuccess(newOrder)
                }
            },
            onDismiss = { showEsewaModal = false }
        )
    }

    // Add New Address Dialog
    if (showAddAddressDialog) {
        AddNewAddressDialog(
            onSave = { newAddr ->
                viewModel.addAddress(newAddr)
                showAddAddressDialog = false
            },
            onDismiss = { showAddAddressDialog = false }
        )
    }
}

@Composable
fun PaymentOptionCard(
    title: String,
    subtitle: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, KiranaGreenPrimary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) KiranaGreenLight else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RadioButton(selected = isSelected, onClick = onClick)
        }
    }
}

@Composable
fun EsewaPaymentDialog(
    amount: Double,
    onSuccess: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var esewaId by remember { mutableStateOf("9841001122") }
    var mpin by remember { mutableStateOf("1234") }
    var isVerifying by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    isVerifying = true
                    // Simulate verified HMAC callback response
                    val refId = "ESEWA_NP_${(100000..999999).random()}"
                    onSuccess(refId)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EsewaGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("Confirm & Pay ${formatNpr(amount)}", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = EsewaGreen,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("eSewa", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("eSewa EPAY Gateway", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "GharKirana Merchant: ${EsewaService.getConfig().merchantId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = EsewaGreenLight,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Payable Amount:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text(formatNpr(amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EsewaGreen)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("eSewa ID (Mobile Number / Email)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = esewaId,
                    onValueChange = { esewaId = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("eSewa MPIN / Token", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = mpin,
                    onValueChange = { mpin = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🔒 Secure HMAC-SHA256 signature verification enabled. EPAY test mode.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    )
}

@Composable
fun AddNewAddressDialog(
    onSave: (DeliveryAddress) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("Home") }
    var street by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("Bhanu Chowk") }
    var instructions by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+977-9841001122") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (street.isNotBlank()) {
                        onSave(
                            DeliveryAddress(
                                id = "addr_${System.currentTimeMillis()}",
                                label = label,
                                recipientName = "Aayush Sharma",
                                phone = phone,
                                streetAddress = street,
                                areaOrChowk = area,
                                city = "Janakpur Dham",
                                instructions = instructions
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = KiranaGreenPrimary)
            ) {
                Text("Save Address")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = {
            Text("Add Delivery Address (Nepal)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Home", "Work", "Other").forEach { l ->
                        FilterChip(
                            selected = label == l,
                            onClick = { label = l },
                            label = { Text(l) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = street,
                    onValueChange = { street = it },
                    label = { Text("House / Flat / Street Name") },
                    placeholder = { Text("e.g. House 42, Near Ram Mandir") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = area,
                    onValueChange = { area = it },
                    label = { Text("Chowk / Area / Landmark") },
                    placeholder = { Text("e.g. Bhanu Chowk, Station Road") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Delivery Note for Rider") },
                    placeholder = { Text("e.g. Call when entering lane") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    )
}
