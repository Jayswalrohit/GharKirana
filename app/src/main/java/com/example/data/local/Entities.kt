package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.*

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nepaliName: String,
    val categoryId: String,
    val categoryName: String,
    val price: Double,
    val discountPrice: Double?,
    val stock: Int,
    val unit: String,
    val brand: String,
    val description: String,
    val emoji: String,
    val imageUrl: String? = null,
    val imageResName: String? = null,
    val isFeatured: Boolean,
    val isActive: Boolean
) {
    fun toProduct(): Product = Product(
        id = id,
        name = name,
        nepaliName = nepaliName,
        categoryId = categoryId,
        categoryName = categoryName,
        price = price,
        discountPrice = discountPrice,
        stock = stock,
        unit = unit,
        brand = brand,
        description = description,
        emoji = emoji,
        imageUrl = imageUrl,
        imageResName = imageResName,
        isFeatured = isFeatured,
        isActive = isActive
    )
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nepaliName: String,
    val emoji: String,
    val sortOrder: Int
) {
    fun toCategory(): Category = Category(
        id = id,
        name = name,
        nepaliName = nepaliName,
        emoji = emoji,
        sortOrder = sortOrder
    )
}

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val orderNumber: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val addressLabel: String,
    val addressStreet: String,
    val addressArea: String,
    val addressCity: String,
    val addressInstructions: String,
    val addressLat: Double,
    val addressLng: Double,
    val itemsJson: String, // serialized OrderItems
    val subtotal: Double,
    val deliveryFee: Double,
    val discountAmount: Double,
    val total: Double,
    val couponCode: String?,
    val paymentMethod: String,
    val paymentStatus: String,
    val esewaRefId: String?,
    val orderStatus: String,
    val assignedRiderId: String?,
    val assignedRiderName: String?,
    val assignedRiderPhone: String?,
    val createdAt: Long,
    val estimatedMinutes: Int
)

@Entity(tableName = "riders")
data class RiderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val bikeNumber: String,
    val isOnline: Boolean,
    val currentArea: String,
    val rating: Double,
    val totalDeliveries: Int,
    val todayEarnings: Double
) {
    fun toRider(): Rider = Rider(
        id = id,
        name = name,
        phone = phone,
        bikeNumber = bikeNumber,
        isOnline = isOnline,
        currentArea = currentArea,
        rating = rating,
        totalDeliveries = totalDeliveries,
        todayEarnings = todayEarnings
    )
}

@Entity(tableName = "coupons")
data class CouponEntity(
    @PrimaryKey val code: String,
    val discountPercent: Int?,
    val discountFlat: Double?,
    val minOrder: Double,
    val maxDiscount: Double,
    val description: String,
    val isActive: Boolean
) {
    fun toCoupon(): Coupon = Coupon(
        code = code,
        discountPercent = discountPercent,
        discountFlat = discountFlat,
        minOrder = minOrder,
        maxDiscount = maxDiscount,
        description = description,
        isActive = isActive
    )
}

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val orderId: String?,
    val timestamp: Long,
    val isRead: Boolean
) {
    fun toNotification(): AppNotification = AppNotification(
        id = id,
        title = title,
        message = message,
        orderId = orderId,
        timestamp = timestamp,
        isRead = isRead
    )
}

@Entity(tableName = "addresses")
data class AddressEntity(
    @PrimaryKey val id: String,
    val label: String,
    val recipientName: String,
    val phone: String,
    val streetAddress: String,
    val areaOrChowk: String,
    val city: String,
    val instructions: String,
    val lat: Double,
    val lng: Double,
    val isDefault: Boolean = false
) {
    fun toDeliveryAddress(): DeliveryAddress = DeliveryAddress(
        id = id,
        label = label,
        recipientName = recipientName,
        phone = phone,
        streetAddress = streetAddress,
        areaOrChowk = areaOrChowk,
        city = city,
        instructions = instructions,
        lat = lat,
        lng = lng
    )
}
