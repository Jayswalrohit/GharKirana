package com.example.data.model

enum class UserRole {
    CUSTOMER,
    ADMIN,
    RIDER
}

data class User(
    val id: String,
    val name: String,
    val phone: String,
    val email: String,
    val role: UserRole = UserRole.CUSTOMER
)

data class Category(
    val id: String,
    val name: String,
    val nepaliName: String,
    val emoji: String,
    val sortOrder: Int
)

data class Product(
    val id: String,
    val name: String,
    val nepaliName: String = "",
    val categoryId: String,
    val categoryName: String,
    val price: Double,
    val discountPrice: Double? = null,
    val stock: Int,
    val unit: String, // e.g. "5 KG", "1 L", "500g", "1 Packet"
    val brand: String = "",
    val description: String = "",
    val emoji: String = "🛒",
    val imageUrl: String? = null,
    val imageResName: String? = null,
    val isFeatured: Boolean = false,
    val isActive: Boolean = true
) {
    val effectivePrice: Double
        get() = discountPrice ?: price

    val hasDiscount: Boolean
        get() = discountPrice != null && discountPrice < price

    val discountPercent: Int
        get() = if (hasDiscount) {
            (((price - discountPrice!!) / price) * 100).toInt()
        } else 0
}

enum class OrderStatus(val title: String, val stepIndex: Int, val description: String) {
    PLACED("Order Placed", 1, "Order received from customer"),
    CONFIRMED("Confirmed", 2, "Shop accepted order"),
    PREPARING("Preparing", 3, "Items being packed at Kirana shop"),
    READY_FOR_PICKUP("Ready for Pickup", 4, "Packed and ready for rider"),
    RIDER_ASSIGNED("Rider Assigned", 5, "Delivery partner assigned"),
    PICKED_UP("Picked Up", 6, "Rider has collected the package"),
    OUT_FOR_DELIVERY("Out for Delivery", 7, "Rider is heading to your address"),
    DELIVERED("Delivered", 8, "Order received by customer"),
    CANCELLED("Cancelled", -1, "Order was cancelled")
}

enum class PaymentMethod(val displayName: String) {
    COD("Cash on Delivery"),
    ESEWA("eSewa Mobile Wallet")
}

enum class PaymentStatus {
    PENDING,
    VERIFIED,
    FAILED
}

data class OrderItem(
    val productId: String,
    val productName: String,
    val unit: String,
    val price: Double,
    val quantity: Int,
    val total: Double
)

data class DeliveryAddress(
    val id: String,
    val label: String, // Home, Work, Other
    val recipientName: String,
    val phone: String,
    val streetAddress: String,
    val areaOrChowk: String, // Bhanu Chowk, Station Road, Ramanand Chowk, etc.
    val city: String = "Janakpur Dham",
    val instructions: String = "",
    val lat: Double = 26.7271,
    val lng: Double = 85.9407
) {
    val fullAddress: String
        get() = "$streetAddress, $areaOrChowk, $city"
}

data class Order(
    val id: String,
    val orderNumber: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val deliveryAddress: DeliveryAddress,
    val items: List<OrderItem>,
    val subtotal: Double,
    val deliveryFee: Double,
    val discountAmount: Double = 0.0,
    val total: Double,
    val couponCode: String? = null,
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus,
    val esewaRefId: String? = null,
    val orderStatus: OrderStatus,
    val assignedRiderId: String? = null,
    val assignedRiderName: String? = null,
    val assignedRiderPhone: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val estimatedMinutes: Int = 25
)

data class Rider(
    val id: String,
    val name: String,
    val phone: String,
    val bikeNumber: String,
    val isOnline: Boolean = true,
    val currentArea: String,
    val rating: Double = 4.8,
    val totalDeliveries: Int = 120,
    val todayEarnings: Double = 350.0
)

data class Coupon(
    val code: String,
    val discountPercent: Int? = null,
    val discountFlat: Double? = null,
    val minOrder: Double = 300.0,
    val maxDiscount: Double = 200.0,
    val description: String,
    val isActive: Boolean = true
)

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val orderId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class ShopSettings(
    val shopName: String = "GharKirana Express",
    val shopPhone: String = "+977-9800012345",
    val shopAddress: String = "Station Road, Bhanu Chowk, Janakpur Dham, Nepal",
    val openingHours: String = "6:00 AM - 10:00 PM (Daily)",
    val baseDeliveryFee: Double = 40.0,
    val freeDeliveryThreshold: Double = 899.0,
    val minOrderAmount: Double = 150.0,
    val allowCOD: Boolean = true,
    val allowEsewa: Boolean = true,
    val esewaMerchantId: String = "EPAYTEST"
)
