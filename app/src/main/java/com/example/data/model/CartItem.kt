package com.example.data.model

/**
 * Represents an individual grocery item selected by the user in the shopping cart before checkout.
 *
 * Encapsulates the selected product, requested quantity, optional customer instructions,
 * unit prices, total cost calculations, savings, and helper operations.
 *
 * @property product The [Product] selected by the customer.
 * @property quantity Number of units chosen (minimum 1).
 * @property specialInstructions Optional notes for this item (e.g. "ripe bananas", "extra fresh packet").
 */
data class CartItem(
    val product: Product,
    val quantity: Int = 1,
    val specialInstructions: String = ""
) {
    /**
     * Unique ID of the product for fast key lookups in maps and lists.
     */
    val id: String
        get() = product.id

    /**
     * Display name of the product in English.
     */
    val productName: String
        get() = product.name

    /**
     * Localized Nepali name of the product.
     */
    val nepaliName: String
        get() = product.nepaliName

    /**
     * Packaging unit (e.g. "5 KG", "1 L", "1 Packet").
     */
    val unit: String
        get() = product.unit

    /**
     * Effective price per unit (accounting for discounts if available).
     */
    val unitPrice: Double
        get() = product.effectivePrice

    /**
     * Regular MRP/original price before discount.
     */
    val originalPrice: Double
        get() = product.price

    /**
     * Whether this item has an active price discount.
     */
    val hasDiscount: Boolean
        get() = product.hasDiscount

    /**
     * Discount percentage relative to original MRP.
     */
    val discountPercent: Int
        get() = product.discountPercent

    /**
     * Total line item price based on effective price and quantity.
     */
    val total: Double
        get() = unitPrice * quantity

    /**
     * Total money saved on this item compared to standard MRP.
     */
    val totalSavings: Double
        get() = if (hasDiscount) (originalPrice - unitPrice) * quantity else 0.0

    /**
     * Indicates whether the item quantity in cart has reached the maximum available inventory.
     */
    val isMaxStockReached: Boolean
        get() = quantity >= product.stock

    /**
     * Returns a copy with incremented quantity, respecting the store's available stock limit.
     */
    fun increment(): CartItem {
        return if (quantity < product.stock) {
            copy(quantity = quantity + 1)
        } else {
            this
        }
    }

    /**
     * Returns a copy with decremented quantity, with a floor of 0.
     */
    fun decrement(): CartItem {
        return copy(quantity = (quantity - 1).coerceAtLeast(0))
    }

    /**
     * Returns a copy with an updated quantity, bounded between 0 and product stock.
     */
    fun withQuantity(newQuantity: Int): CartItem {
        val bounded = newQuantity.coerceIn(0, product.stock)
        return copy(quantity = bounded)
    }

    /**
     * Returns a copy with updated customer instructions for this item.
     */
    fun withInstructions(notes: String): CartItem {
        return copy(specialInstructions = notes)
    }

    /**
     * Converts this [CartItem] into an [OrderItem] for order placement during checkout.
     */
    fun toOrderItem(): OrderItem {
        return OrderItem(
            productId = product.id,
            productName = product.name,
            unit = product.unit,
            price = unitPrice,
            quantity = quantity,
            total = total
        )
    }
}
