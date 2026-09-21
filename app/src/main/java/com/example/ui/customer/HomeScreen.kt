package com.example.ui.customer

import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.GharKiranaBrandLogo
import com.example.ui.components.ProductImageView
import com.example.ui.components.formatNpr
import com.example.ui.theme.*
import com.example.ui.viewmodel.GroceryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
    viewModel: GroceryViewModel,
    onOpenCart: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenOrderTracking: (String) -> Unit,
    onOpenLogin: () -> Unit = onOpenProfile,
    modifier: Modifier = Modifier
) {
    val currentLocation by viewModel.currentLocation.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategoryId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val discountOffers by viewModel.discountOffers.collectAsState()
    val recommendedProducts by viewModel.recommendedProducts.collectAsState()
    val recentlyViewedProducts by viewModel.recentlyViewedProducts.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val cartTotal by viewModel.cartTotal.collectAsState()
    val customerOrders by viewModel.customerOrders.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val customer by viewModel.currentCustomer.collectAsState()

    var showLocationDialog by remember { mutableStateOf(false) }
    var selectedProductForDetail by remember { mutableStateOf<Product?>(null) }

    val activeOrder = customerOrders.firstOrNull {
        it.orderStatus.name != "DELIVERED" && it.orderStatus.name != "CANCELLED"
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (cartItems.isNotEmpty() || activeOrder != null) 96.dp else 24.dp)
        ) {
            // 1. Authentic GharKirana Brand Header & Location Bar
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Brand Logo row with Profile/Login button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            GharKiranaBrandLogo(
                                showTagline = false
                            )

                            // Profile / Login Button
                            if (!isLoggedIn) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = KiranaGreenPrimary,
                                    modifier = Modifier.clickable { onOpenLogin() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Login",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "लगइन / Login",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                IconButton(
                                    onClick = onOpenProfile,
                                    modifier = Modifier.testTag("profile_btn")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(KiranaGreenLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = customer.name.firstOrNull()?.uppercase() ?: "U",
                                            fontWeight = FontWeight.Bold,
                                            color = KiranaGreenPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Delivery Location Selector Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLocationDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Delivery Location",
                                        tint = KiranaGreenPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "डेलिभरी ठेगाना / Deliver to:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = currentLocation,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Change Location",
                                    tint = KiranaGreenPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Live active order ticker if any
            if (activeOrder != null) {
                item {
                    Surface(
                        color = KiranaAmberLight,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable { onOpenOrderTracking(activeOrder.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🛵", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Active Order #${activeOrder.orderNumber} • ${activeOrder.orderStatus.title}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = KiranaAmberDark
                                    )
                                    Text(
                                        text = "Tap to track live Kirana delivery progress",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = KiranaAmberDark.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Track",
                                tint = KiranaAmberDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 2. Search Bar
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input"),
                        placeholder = {
                            Text("खोज्नुहोस्: बासमती चामल, दाल, घिउ, धारा तेल, चाउचाउ...")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = KiranaGreenPrimary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = KiranaGreenPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        singleLine = true
                    )
                }
            }

            // 3. GharKirana Heritage Banner
            if (searchQuery.isBlank() && selectedCategory == null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .height(140.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter = painterResource(id = R.drawable.gharkirana_banner_1789826194174),
                                contentDescription = "GharKirana Banner",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.82f),
                                                Color.Black.copy(alpha = 0.35f)
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .align(Alignment.CenterStart)
                            ) {
                                Surface(
                                    color = KiranaGreenPrimary,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "🇳🇵 मौलिक नेपाली किराना",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "तपाईंको घर, हाम्रो किराना",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Fresh Daily Groceries • eSewa & COD Accepted • Free delivery over Rs. 899",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Attractive Grocery Categories
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "किराना विधा / Categories",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = KiranaGreenDark
                        )
                        if (selectedCategory != null) {
                            Text(
                                text = "Show All",
                                style = MaterialTheme.typography.labelMedium,
                                color = KiranaGreenPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { viewModel.selectCategory(null) }
                            )
                        }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            CategoryChipItem(
                                title = "सबै सामान",
                                emoji = "🛍️",
                                isSelected = selectedCategory == null,
                                onClick = { viewModel.selectCategory(null) }
                            )
                        }
                        items(categories) { cat ->
                            CategoryChipItem(
                                title = if (cat.nepaliName.isNotBlank()) cat.nepaliName else cat.name,
                                emoji = cat.emoji,
                                isSelected = selectedCategory == cat.id,
                                onClick = { viewModel.selectCategory(cat.id) }
                            )
                        }
                    }
                }
            }

            // 5. Today's Offers / Deals Section
            if (searchQuery.isBlank() && selectedCategory == null && discountOffers.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔥", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "आजको विशेष छुट / Today's Offers",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = KiranaTerracotta
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = ErrorRedLight,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Up to 25% OFF",
                                    color = ErrorRed,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(discountOffers.take(6)) { product ->
                                ProductMiniOfferCard(
                                    product = product,
                                    cartQuantity = cartItems.find { it.product.id == product.id }?.quantity ?: 0,
                                    onAdd = { viewModel.addToCart(product) },
                                    onDecrement = { viewModel.decrementCart(product.id) },
                                    onClick = {
                                        viewModel.recordProductViewed(product)
                                        selectedProductForDetail = product
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 6. Recommended Products Section
            if (searchQuery.isBlank() && selectedCategory == null && recommendedProducts.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⭐", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "सिफारिस गरिएका / Recommended for You",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = KiranaGreenDark
                            )
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recommendedProducts) { product ->
                                ProductMiniOfferCard(
                                    product = product,
                                    cartQuantity = cartItems.find { it.product.id == product.id }?.quantity ?: 0,
                                    onAdd = { viewModel.addToCart(product) },
                                    onDecrement = { viewModel.decrementCart(product.id) },
                                    onClick = {
                                        viewModel.recordProductViewed(product)
                                        selectedProductForDetail = product
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 7. Recently Viewed / Popular Kirana Staples Section
            if (searchQuery.isBlank() && selectedCategory == null && recentlyViewedProducts.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🌾", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "लोकप्रिय किराना सामान / Popular Staples",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = KiranaGreenDark
                            )
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recentlyViewedProducts) { product ->
                                ProductMiniOfferCard(
                                    product = product,
                                    cartQuantity = cartItems.find { it.product.id == product.id }?.quantity ?: 0,
                                    onAdd = { viewModel.addToCart(product) },
                                    onDecrement = { viewModel.decrementCart(product.id) },
                                    onClick = {
                                        viewModel.recordProductViewed(product)
                                        selectedProductForDetail = product
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 8. Main Product Grid Section Title
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val title = when {
                        searchQuery.isNotBlank() -> "खोज नतिजा / Search Results (${filteredProducts.size})"
                        selectedCategory != null -> categories.find { it.id == selectedCategory }?.let { "${it.emoji} ${it.name} (${it.nepaliName})" } ?: "Products"
                        else -> "सबै ताजा किराना / All Fresh Groceries (${filteredProducts.size})"
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = KiranaGreenDark
                    )
                }
            }

            // Product Grid or Empty State
            if (filteredProducts.isEmpty()) {
                item {
                    EmptyPlaceholder(
                        icon = { Text("🔍", fontSize = 32.sp) },
                        title = "कुनै सामान भेटिएन / No Products Found",
                        subtitle = "Try searching for Basmati rice, Wai Wai, Dal, or clear search."
                    )
                }
            } else {
                val chunkedProducts = filteredProducts.chunked(2)
                items(chunkedProducts) { rowProducts ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (product in rowProducts) {
                            Box(modifier = Modifier.weight(1f)) {
                                ProductGridCard(
                                    product = product,
                                    cartQuantity = cartItems.find { it.product.id == product.id }?.quantity ?: 0,
                                    onAdd = { viewModel.addToCart(product) },
                                    onDecrement = { viewModel.decrementCart(product.id) },
                                    onClick = {
                                        viewModel.recordProductViewed(product)
                                        selectedProductForDetail = product
                                    }
                                )
                            }
                        }
                        if (rowProducts.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Floating Cart Summary Bar
        if (cartItems.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("floating_cart_bar"),
                shape = RoundedCornerShape(16.dp),
                color = KiranaGreenPrimary,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onOpenCart() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${cartItems.sumOf { it.quantity }} items • ${formatNpr(cartTotal)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "झोला हेर्नुहोस् • Tap to view basket",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "View Cart",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "View Cart",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // Location Selection Dialog
    if (showLocationDialog) {
        LocationSelectionDialog(
            current = currentLocation,
            onSelect = {
                viewModel.setLocation(it)
                showLocationDialog = false
            },
            onDismiss = { showLocationDialog = false }
        )
    }

    // Product Detail Dialog
    if (selectedProductForDetail != null) {
        ProductDetailDialog(
            product = selectedProductForDetail!!,
            cartQuantity = cartItems.find { it.product.id == selectedProductForDetail!!.id }?.quantity ?: 0,
            onAdd = { viewModel.addToCart(selectedProductForDetail!!) },
            onDecrement = { viewModel.decrementCart(selectedProductForDetail!!.id) },
            onDismiss = { selectedProductForDetail = null }
        )
    }
}

@Composable
fun CategoryChipItem(
    title: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) KiranaGreenPrimary else MaterialTheme.colorScheme.surface,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ProductGridCard(
    product: Product,
    cartQuantity: Int,
    onAdd: () -> Unit,
    onDecrement: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Image & Discount Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                ProductImageView(
                    product = product,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    emojiSize = 38.sp
                )

                if (product.hasDiscount) {
                    Surface(
                        color = KiranaTerracotta,
                        shape = RoundedCornerShape(bottomEnd = 8.dp, topStart = 10.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "-${product.discountPercent}%",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Brand & Unit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = product.brand.ifBlank { product.categoryName },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.unit,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = KiranaGreenDark
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Product Name (English & Nepali)
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (product.nepaliName.isNotBlank()) {
                Text(
                    text = product.nepaliName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Price & Add button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatNpr(product.effectivePrice),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (product.hasDiscount) {
                        Text(
                            text = formatNpr(product.price),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }

                // Add / Stepper
                if (cartQuantity == 0) {
                    Button(
                        onClick = onAdd,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KiranaGreenPrimary),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("add_btn_${product.id}")
                    ) {
                        Text("ADD", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(KiranaGreenLight)
                            .padding(2.dp)
                    ) {
                        IconButton(
                            onClick = onDecrement,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("dec_btn_${product.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Minus",
                                tint = KiranaGreenDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = cartQuantity.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = KiranaGreenDark,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        IconButton(
                            onClick = onAdd,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("inc_btn_${product.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Plus",
                                tint = KiranaGreenDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductMiniOfferCard(
    product: Product,
    cartQuantity: Int,
    onAdd: () -> Unit,
    onDecrement: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                ProductImageView(
                    product = product,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    emojiSize = 28.sp
                )
                if (product.hasDiscount) {
                    Surface(
                        color = KiranaTerracotta,
                        shape = RoundedCornerShape(bottomEnd = 6.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "-${product.discountPercent}%",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = product.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = product.unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatNpr(product.effectivePrice),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                if (cartQuantity == 0) {
                    FilledTonalIconButton(
                        onClick = onAdd,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                    }
                } else {
                    Text(
                        text = "${cartQuantity} in cart",
                        style = MaterialTheme.typography.labelSmall,
                        color = KiranaGreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ProductDetailDialog(
    product: Product,
    cartQuantity: Int,
    onAdd: () -> Unit,
    onDecrement: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (cartQuantity == 0) {
                Button(
                    onClick = onAdd,
                    colors = ButtonDefaults.buttonColors(containerColor = KiranaGreenPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add to Basket • ${formatNpr(product.effectivePrice)}")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quantity: $cartQuantity (${formatNpr(product.effectivePrice * cartQuantity)})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(KiranaGreenLight)
                    ) {
                        IconButton(onClick = onDecrement, modifier = Modifier.size(36.dp)) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Minus", tint = KiranaGreenDark)
                        }
                        Text(
                            text = cartQuantity.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = KiranaGreenDark,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = onAdd, modifier = Modifier.size(36.dp)) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Plus", tint = KiranaGreenDark)
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductImageView(
                    product = product,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    emojiSize = 28.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (product.nepaliName.isNotBlank()) {
                        Text(
                            text = product.nepaliName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ProductImageView(
                    product = product,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                    emojiSize = 48.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pack Size: ${product.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Brand: ${product.brand.ifBlank { "Local Kirana" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = product.description.ifBlank { "Fresh authentic grocery staple directly from local producers and mills for daily Nepali kitchen cooking." },
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = KiranaGreenLight,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🌾", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "In Stock (${product.stock} units available) • Same-Day Doorstep Delivery",
                            style = MaterialTheme.typography.labelSmall,
                            color = KiranaGreenDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun LocationSelectionDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val areas = listOf(
        "Bhanu Chowk, Janakpur Dham",
        "Station Road, Janakpur Dham",
        "Ramanand Chowk, Janakpur Dham",
        "Pidari Chowk, Janakpur Dham",
        "Mills Area, Janakpur Dham",
        "Shiva Chowk, Janakpur Dham",
        "Mujelia, Janakpur Dham",
        "New Baneshwor, Kathmandu",
        "Thamel, Kathmandu",
        "Koteshwor, Kathmandu",
        "Patan Dhoka, Lalitpur"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = KiranaGreenPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Delivery Area / Chowk", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(areas) { area ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(area) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = area,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (area == current) FontWeight.Bold else FontWeight.Normal,
                            color = if (area == current) KiranaGreenPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        if (area == current) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = KiranaGreenPrimary)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                }
            }
        }
    )
}
