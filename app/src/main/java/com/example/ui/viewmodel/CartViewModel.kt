package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CartItem
import com.example.data.model.Coupon
import com.example.data.model.OrderItem
import com.example.data.model.Product
import kotlinx.coroutines.flow.*

/**
 * UI State snapshot representing the shopping cart and checkout totals.
 */
data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val itemCount: Int = 0,
    val distinctItemCount: Int = 0,
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val couponDiscount: Double = 0.0,
    val total: Double = 0.0,
    val totalSavings: Double = 0.0,
    val freeDeliveryThreshold: Double = 899.0,
    val amountNeededForFreeDelivery: Double = 899.0,
    val isFreeDeliveryEligible: Boolean = false,
    val appliedCoupon: Coupon? = null,
    val couponError: String? = null,
    val deliveryNotes: String = "",
    val isEmpty: Boolean = true,
    val canCheckout: Boolean = false
)

/**
 * Result of validating the cart state before proceeding to checkout.
 */
data class CartCheckoutValidation(
    val isValid: Boolean,
    val errorMessage: String? = null
)

/**
 * ViewModel managing the local shopping cart state for selected grocery items before checkout.
 *
 * Provides reactive StateFlows for reactive Compose UI observation, stock-safe mutations,
 * delivery calculations (including free delivery thresholds in Janakpur), coupon validation,
 * line item savings, and helper conversions to [OrderItem] for order placement.
 *
 * @property freeDeliveryThreshold Minimum subtotal in NPR to qualify for free home delivery.
 * @property baseDeliveryFee Standard delivery charge in NPR when below threshold.
 */
class CartViewModel(
    val freeDeliveryThreshold: Double = 899.0,
    val baseDeliveryFee: Double = 40.0
) : ViewModel() {

    // Internal reactive cart state indexed by Product ID for O(1) mutations
    private val _cartMap = MutableStateFlow<Map<String, CartItem>>(emptyMap())
    val cartMap: StateFlow<Map<String, CartItem>> = _cartMap.asStateFlow()

    // Observable list of cart items
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    // Total count of physical units in cart
    private val _itemCount = MutableStateFlow(0)
    val itemCount: StateFlow<Int> = _itemCount.asStateFlow()

    // Distinct product count
    private val _distinctItemCount = MutableStateFlow(0)
    val distinctItemCount: StateFlow<Int> = _distinctItemCount.asStateFlow()

    // Flag whether the cart currently has no items
    private val _isCartEmpty = MutableStateFlow(true)
    val isCartEmpty: StateFlow<Boolean> = _isCartEmpty.asStateFlow()

    // Subtotal of all active cart items
    private val _subtotal = MutableStateFlow(0.0)
    val subtotal: StateFlow<Double> = _subtotal.asStateFlow()

    // Total discount savings relative to original MRP
    private val _totalSavings = MutableStateFlow(0.0)
    val totalSavings: StateFlow<Double> = _totalSavings.asStateFlow()

    // Free delivery threshold tracking
    private val _isFreeDeliveryEligible = MutableStateFlow(false)
    val isFreeDeliveryEligible: StateFlow<Boolean> = _isFreeDeliveryEligible.asStateFlow()

    private val _amountNeededForFreeDelivery = MutableStateFlow(freeDeliveryThreshold)
    val amountNeededForFreeDelivery: StateFlow<Double> = _amountNeededForFreeDelivery.asStateFlow()

    // Delivery fee calculation
    private val _deliveryFee = MutableStateFlow(0.0)
    val deliveryFee: StateFlow<Double> = _deliveryFee.asStateFlow()

    // Applied Coupon & Promo validation
    private val _appliedCoupon = MutableStateFlow<Coupon?>(null)
    val appliedCoupon: StateFlow<Coupon?> = _appliedCoupon.asStateFlow()

    private val _couponError = MutableStateFlow<String?>(null)
    val couponError: StateFlow<String?> = _couponError.asStateFlow()

    // Calculated coupon discount
    private val _couponDiscount = MutableStateFlow(0.0)
    val couponDiscount: StateFlow<Double> = _couponDiscount.asStateFlow()

    // Final total payable amount
    private val _total = MutableStateFlow(0.0)
    val total: StateFlow<Double> = _total.asStateFlow()

    // Synchronous helper properties for instant calculations
    val currentSubtotal: Double
        get() = _subtotal.value

    val currentTotalSavings: Double
        get() = _totalSavings.value

    val currentItemCount: Int
        get() = _itemCount.value

    val currentDeliveryFee: Double
        get() = _deliveryFee.value

    val currentCouponDiscount: Double
        get() = _couponDiscount.value

    val currentTotal: Double
        get() = _total.value

    // Optional customer checkout notes
    private val _deliveryNotes = MutableStateFlow("")
    val deliveryNotes: StateFlow<String> = _deliveryNotes.asStateFlow()

    // Consolidated UI State for single-point observation in Composables
    private val _uiState = MutableStateFlow(
        CartUiState(
            freeDeliveryThreshold = freeDeliveryThreshold,
            amountNeededForFreeDelivery = freeDeliveryThreshold
        )
    )
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private fun recalculateCartState(newMap: Map<String, CartItem>) {
        val itemsList = newMap.values.toList()
        val units = itemsList.sumOf { it.quantity }
        val distinct = newMap.size
        val empty = newMap.isEmpty()
        val sub = itemsList.sumOf { it.total }
        val savings = itemsList.sumOf { it.totalSavings }
        val isFreeDel = !empty && sub >= freeDeliveryThreshold
        val neededForFree = if (empty) freeDeliveryThreshold else (freeDeliveryThreshold - sub).coerceAtLeast(0.0)
        val fee = if (empty || isFreeDel) 0.0 else baseDeliveryFee

        val coupon = _appliedCoupon.value
        val disc = if (coupon == null || sub < coupon.minOrder) {
            if (coupon != null && sub < coupon.minOrder) {
                _couponError.value = "Coupon '${coupon.code}' removed: minimum NPR ${coupon.minOrder.toInt()} not met."
                _appliedCoupon.value = null
            }
            0.0
        } else {
            if (coupon.discountPercent != null) {
                ((sub * coupon.discountPercent) / 100.0).coerceAtMost(coupon.maxDiscount)
            } else {
                (coupon.discountFlat ?: 0.0).coerceAtMost(coupon.maxDiscount)
            }
        }
        val grandTotal = (sub + fee - disc).coerceAtLeast(0.0)

        _cartMap.value = newMap
        _cartItems.value = itemsList
        _itemCount.value = units
        _distinctItemCount.value = distinct
        _isCartEmpty.value = empty
        _subtotal.value = sub
        _totalSavings.value = savings
        _isFreeDeliveryEligible.value = isFreeDel
        _amountNeededForFreeDelivery.value = neededForFree
        _deliveryFee.value = fee
        _couponDiscount.value = disc
        _total.value = grandTotal
        _uiState.value = CartUiState(
            items = itemsList,
            itemCount = units,
            distinctItemCount = distinct,
            subtotal = sub,
            deliveryFee = fee,
            couponDiscount = disc,
            total = grandTotal,
            totalSavings = savings,
            freeDeliveryThreshold = freeDeliveryThreshold,
            amountNeededForFreeDelivery = neededForFree,
            isFreeDeliveryEligible = isFreeDel,
            appliedCoupon = _appliedCoupon.value,
            couponError = _couponError.value,
            deliveryNotes = _deliveryNotes.value,
            isEmpty = empty,
            canCheckout = !empty && sub > 0.0
        )
    }

    // ==========================================
    // Mutations & Actions
    // ==========================================

    /**
     * Adds a product to the cart or increments its count if already present.
     * Respects store inventory limits.
     *
     * @param product The grocery item to add.
     * @param quantity Number of units to add (default: 1).
     * @param specialInstructions Optional note for this product.
     * @return True if added or updated, false if product is out of stock.
     */
    fun addItem(product: Product, quantity: Int = 1, specialInstructions: String = ""): Boolean {
        if (product.stock <= 0) return false

        val current = _cartMap.value.toMutableMap()
        val existing = current[product.id]

        if (existing == null) {
            val validQty = quantity.coerceIn(1, product.stock)
            current[product.id] = CartItem(
                product = product,
                quantity = validQty,
                specialInstructions = specialInstructions
            )
        } else {
            val newQty = (existing.quantity + quantity).coerceAtMost(product.stock)
            val updatedNotes = if (specialInstructions.isNotBlank()) specialInstructions else existing.specialInstructions
            current[product.id] = existing.copy(
                quantity = newQty,
                specialInstructions = updatedNotes
            )
        }

        recalculateCartState(current)
        return true
    }

    /**
     * Increments the quantity of an item already in the cart by 1.
     *
     * @return True if incremented, false if already at maximum stock or not found.
     */
    fun incrementItem(productId: String): Boolean {
        val current = _cartMap.value.toMutableMap()
        val existing = current[productId] ?: return false

        if (existing.quantity < existing.product.stock) {
            current[productId] = existing.increment()
            recalculateCartState(current)
            return true
        }
        return false
    }

    /**
     * Decrements the quantity of an item in the cart by 1.
     * If quantity reaches 0, the item is automatically removed.
     */
    fun decrementItem(productId: String) {
        val current = _cartMap.value.toMutableMap()
        val existing = current[productId] ?: return

        if (existing.quantity > 1) {
            current[productId] = existing.decrement()
        } else {
            current.remove(productId)
        }
        recalculateCartState(current)
    }

    /**
     * Directly updates the quantity of a cart item.
     * If [newQuantity] <= 0, the item is removed.
     */
    fun updateQuantity(productId: String, newQuantity: Int) {
        val current = _cartMap.value.toMutableMap()
        val existing = current[productId] ?: return

        if (newQuantity <= 0) {
            current.remove(productId)
        } else {
            val capped = newQuantity.coerceAtMost(existing.product.stock)
            current[productId] = existing.copy(quantity = capped)
        }
        recalculateCartState(current)
    }

    /**
     * Updates special instructions for a specific item in the cart.
     */
    fun setItemInstructions(productId: String, instructions: String) {
        val current = _cartMap.value.toMutableMap()
        val existing = current[productId] ?: return
        current[productId] = existing.withInstructions(instructions)
        recalculateCartState(current)
    }

    /**
     * Removes an item completely from the cart.
     */
    fun removeItem(productId: String) {
        val current = _cartMap.value.toMutableMap()
        if (current.remove(productId) != null) {
            recalculateCartState(current)
        }
    }

    /**
     * Empties all items from the cart and resets applied coupons and errors.
     */
    fun clearCart() {
        _appliedCoupon.value = null
        _couponError.value = null
        _deliveryNotes.value = ""
        recalculateCartState(emptyMap())
    }

    /**
     * Sets delivery instructions for the entire order before checkout.
     */
    fun setDeliveryNotes(notes: String) {
        _deliveryNotes.value = notes
        _uiState.value = _uiState.value.copy(deliveryNotes = notes)
    }

    /**
     * Applies a [Coupon] object directly if subtotal meets the coupon's minimum order requirement.
     */
    fun applyCoupon(coupon: Coupon): Boolean {
        val currentSub = currentSubtotal
        if (currentSub < coupon.minOrder) {
            _couponError.value = "Requires a minimum order of NPR ${coupon.minOrder.toInt()}."
            _appliedCoupon.value = null
            recalculateCartState(_cartMap.value)
            return false
        }
        _appliedCoupon.value = coupon
        _couponError.value = null
        recalculateCartState(_cartMap.value)
        return true
    }

    /**
     * Validates and applies a coupon code against a list of available coupons.
     */
    fun applyCouponCode(code: String, availableCoupons: List<Coupon>): Boolean {
        val trimmed = code.trim()
        if (trimmed.isBlank()) {
            _couponError.value = "Please enter a coupon code."
            return false
        }

        val matching = availableCoupons.firstOrNull {
            it.code.equals(trimmed, ignoreCase = true) && it.isActive
        }

        if (matching == null) {
            _couponError.value = "Invalid or expired promo code: '$code'"
            return false
        }

        return applyCoupon(matching)
    }

    /**
     * Removes the currently applied coupon.
     */
    fun removeCoupon() {
        _appliedCoupon.value = null
        _couponError.value = null
        recalculateCartState(_cartMap.value)
    }

    // ==========================================
    // Query & Validation Helpers
    // ==========================================

    /**
     * Returns the quantity of a product currently in the cart (0 if not present).
     */
    fun getItemQuantity(productId: String): Int {
        return _cartMap.value[productId]?.quantity ?: 0
    }

    /**
     * Returns the [CartItem] for a given product ID, or null if not in cart.
     */
    fun getItem(productId: String): CartItem? {
        return _cartMap.value[productId]
    }

    /**
     * Checks whether a specific product is currently in the cart.
     */
    fun hasProduct(productId: String): Boolean {
        return _cartMap.value.containsKey(productId)
    }

    /**
     * Converts all current cart items into [OrderItem] objects ready for persistence or checkout.
     */
    fun toOrderItems(): List<OrderItem> {
        return _cartMap.value.values.map { it.toOrderItem() }
    }

    /**
     * Validates whether the cart can proceed to checkout.
     *
     * @param minOrderAmount Minimum required order value (default: NPR 150.0).
     */
    fun validateForCheckout(minOrderAmount: Double = 150.0): CartCheckoutValidation {
        val items = _cartMap.value.values.toList()
        if (items.isEmpty()) {
            return CartCheckoutValidation(isValid = false, errorMessage = "Your cart is empty. Add grocery items before checkout.")
        }

        val currentSubtotal = items.sumOf { it.total }
        if (currentSubtotal < minOrderAmount) {
            return CartCheckoutValidation(
                isValid = false,
                errorMessage = "Minimum order value is NPR ${minOrderAmount.toInt()}. Add NPR ${(minOrderAmount - currentSubtotal).toInt()} more."
            )
        }

        for (item in items) {
            if (item.quantity > item.product.stock) {
                return CartCheckoutValidation(
                    isValid = false,
                    errorMessage = "${item.product.name} exceeds available stock (${item.product.stock} available)."
                )
            }
        }

        return CartCheckoutValidation(isValid = true, errorMessage = null)
    }
}
