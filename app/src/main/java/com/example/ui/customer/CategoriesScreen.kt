package com.example.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.formatNpr
import com.example.ui.theme.*
import com.example.ui.viewmodel.GroceryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: GroceryViewModel,
    onOpenCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsState()
    val allProducts by viewModel.allActiveProducts.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()

    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var categorySearchQuery by remember { mutableStateOf("") }
    var selectedProductForDetail by remember { mutableStateOf<Product?>(null) }

    val displayedProducts = remember(selectedCategory, categorySearchQuery, allProducts) {
        var list = if (selectedCategory == null) {
            allProducts
        } else {
            allProducts.filter { it.categoryId == selectedCategory?.id }
        }
        if (categorySearchQuery.isNotBlank()) {
            list = list.filter {
                it.name.contains(categorySearchQuery, ignoreCase = true) ||
                        it.nepaliName.contains(categorySearchQuery, ignoreCase = true) ||
                        it.brand.contains(categorySearchQuery, ignoreCase = true)
            }
        }
        list
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "किराना विधा / Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KiranaGreenDark
                        )
                        Text(
                            text = "Browse fresh Nepali groceries by section",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = KiranaGreenLight,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${categories.size} विधा",
                            color = KiranaGreenDark,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search inside category
                OutlinedTextField(
                    value = categorySearchQuery,
                    onValueChange = { categorySearchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_search_input"),
                    placeholder = {
                        Text(
                            if (selectedCategory != null) "Search in ${selectedCategory?.name}..."
                            else "Search any grocery product..."
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (categorySearchQuery.isNotBlank()) {
                            IconButton(onClick = { categorySearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )
            }
        }

        // Horizontal Category Pill Bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedCategory == null) KiranaGreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { selectedCategory = null }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🛍️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "सबै सामान (All)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedCategory == null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(categories) { cat ->
                val isSelected = selectedCategory?.id == cat.id
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) KiranaGreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { selectedCategory = cat }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cat.emoji, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            if (cat.nepaliName.isNotBlank()) {
                                Text(
                                    text = cat.nepaliName,
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        // Product Catalog Grid for Selected Category
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (displayedProducts.isEmpty()) {
                EmptyPlaceholder(
                    icon = { Text("📦", fontSize = 36.sp) },
                    title = "No products found in this category",
                    subtitle = "Try selecting another category or clearing search."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedCategory?.let { "${it.emoji} ${it.name} (${it.nepaliName})" }
                                    ?: "All Grocery Products (${displayedProducts.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${displayedProducts.size} items",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val chunks = displayedProducts.chunked(2)
                    items(chunks) { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (product in pair) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ProductGridCard(
                                        product = product,
                                        cartQuantity = cartItems.find { it.product.id == product.id }?.quantity ?: 0,
                                        onAdd = { viewModel.addToCart(product) },
                                        onDecrement = { viewModel.decrementCart(product.id) },
                                        onClick = { selectedProductForDetail = product }
                                    )
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

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
