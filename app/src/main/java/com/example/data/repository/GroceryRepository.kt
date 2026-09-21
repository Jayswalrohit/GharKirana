package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class GroceryRepository(private val dao: AppDao) {

    val activeProducts: Flow<List<Product>> = dao.getAllActiveProducts().map { list ->
        list.map { it.toProduct() }
    }

    val adminProducts: Flow<List<Product>> = dao.getAllProductsForAdmin().map { list ->
        list.map { it.toProduct() }
    }

    val categories: Flow<List<Category>> = dao.getAllCategories().map { list ->
        list.map { it.toCategory() }
    }

    val allOrders: Flow<List<Order>> = dao.getAllOrders().map { list ->
        list.map { it.toOrder() }
    }

    fun getCustomerOrders(customerId: String): Flow<List<Order>> =
        dao.getCustomerOrders(customerId).map { list ->
            list.map { it.toOrder() }
        }

    fun getRiderOrders(riderId: String): Flow<List<Order>> =
        dao.getRiderOrders(riderId).map { list ->
            list.map { it.toOrder() }
        }

    fun getOrderFlow(orderId: String): Flow<Order?> =
        dao.getOrderFlowById(orderId).map { it?.toOrder() }

    val riders: Flow<List<Rider>> = dao.getAllRiders().map { list ->
        list.map { it.toRider() }
    }

    val coupons: Flow<List<Coupon>> = dao.getActiveCoupons().map { list ->
        list.map { it.toCoupon() }
    }

    val notifications: Flow<List<AppNotification>> = dao.getNotifications().map { list ->
        list.map { it.toNotification() }
    }

    val addresses: Flow<List<DeliveryAddress>> = dao.getAddresses().map { list ->
        list.map { it.toDeliveryAddress() }
    }

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        val existing = dao.getProductById("prod_01")
        if (existing == null) {
            dao.insertCategories(SeedData.categories)
            dao.insertProducts(SeedData.products)
            dao.insertRiders(SeedData.riders)
            dao.insertCoupons(SeedData.coupons)
            for (addr in SeedData.defaultAddresses) {
                dao.insertAddress(addr)
            }
            for (order in SeedData.createInitialOrders()) {
                dao.insertOrder(order)
            }
            for (notif in SeedData.initialNotifications) {
                dao.insertNotification(notif)
            }
        }
    }

    // Product CRUD for Admin
    suspend fun saveProduct(product: Product) = withContext(Dispatchers.IO) {
        val entity = ProductEntity(
            id = product.id.ifBlank { "prod_${System.currentTimeMillis()}" },
            name = product.name,
            nepaliName = product.nepaliName,
            categoryId = product.categoryId,
            categoryName = product.categoryName,
            price = product.price,
            discountPrice = product.discountPrice,
            stock = product.stock,
            unit = product.unit,
            brand = product.brand,
            description = product.description,
            emoji = product.emoji,
            imageUrl = product.imageUrl,
            imageResName = product.imageResName,
            isFeatured = product.isFeatured,
            isActive = product.isActive
        )
        dao.insertProduct(entity)
    }

    suspend fun updateStock(productId: String, newStock: Int) = withContext(Dispatchers.IO) {
        dao.updateStock(productId, newStock.coerceAtLeast(0))
    }

    suspend fun deleteProduct(productId: String) = withContext(Dispatchers.IO) {
        dao.deleteProduct(productId)
    }

    // Orders & Workflow
    suspend fun createOrder(
        customer: User,
        address: DeliveryAddress,
        items: List<CartItem>,
        deliveryFee: Double,
        discountAmount: Double,
        couponCode: String?,
        paymentMethod: PaymentMethod,
        paymentStatus: PaymentStatus,
        esewaRefId: String?
    ): Order = withContext(Dispatchers.IO) {
        val orderId = "order_${System.currentTimeMillis()}"
        val orderNumber = "GK-${(1000..9999).random()}"
        val orderItems = items.map {
            OrderItem(
                productId = it.product.id,
                productName = it.product.name,
                unit = it.product.unit,
                price = it.product.effectivePrice,
                quantity = it.quantity,
                total = it.total
            )
        }
        val subtotal = items.sumOf { it.total }
        val total = (subtotal + deliveryFee - discountAmount).coerceAtLeast(0.0)

        // Decrement stock in store
        for (item in items) {
            val p = dao.getProductById(item.product.id)
            if (p != null) {
                val updatedStock = (p.stock - item.quantity).coerceAtLeast(0)
                dao.updateStock(item.product.id, updatedStock)
            }
        }

        val entity = OrderEntity(
            id = orderId,
            orderNumber = orderNumber,
            customerId = customer.id,
            customerName = customer.name,
            customerPhone = customer.phone,
            addressLabel = address.label,
            addressStreet = address.streetAddress,
            addressArea = address.areaOrChowk,
            addressCity = address.city,
            addressInstructions = address.instructions,
            addressLat = address.lat,
            addressLng = address.lng,
            itemsJson = OrderItemsConverter.fromList(orderItems),
            subtotal = subtotal,
            deliveryFee = deliveryFee,
            discountAmount = discountAmount,
            total = total,
            couponCode = couponCode,
            paymentMethod = paymentMethod.name,
            paymentStatus = paymentStatus.name,
            esewaRefId = esewaRefId,
            orderStatus = OrderStatus.PLACED.name,
            assignedRiderId = null,
            assignedRiderName = null,
            assignedRiderPhone = null,
            createdAt = System.currentTimeMillis(),
            estimatedMinutes = 25
        )

        dao.insertOrder(entity)

        // Add Notification
        dao.insertNotification(
            NotificationEntity(
                id = "notif_${System.currentTimeMillis()}",
                title = "Order Placed Successfully! 🛒",
                message = "Order $orderNumber has been placed. Shop is reviewing items.",
                orderId = orderId,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
        )

        entity.toOrder()
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus) = withContext(Dispatchers.IO) {
        dao.updateOrderStatus(orderId, newStatus.name)
        val order = dao.getOrderById(orderId)
        val notifMessage = when (newStatus) {
            OrderStatus.CONFIRMED -> "Shop has accepted order ${order?.orderNumber} and is preparing."
            OrderStatus.PREPARING -> "Your Kirana grocery items are being packed fresh."
            OrderStatus.READY_FOR_PICKUP -> "Order ${order?.orderNumber} is packed and ready for delivery partner."
            OrderStatus.CANCELLED -> "Order ${order?.orderNumber} was cancelled."
            else -> "Order status updated to ${newStatus.title}."
        }
        dao.insertNotification(
            NotificationEntity(
                id = "notif_${System.currentTimeMillis()}",
                title = "Status: ${newStatus.title}",
                message = notifMessage,
                orderId = orderId,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
        )
    }

    suspend fun assignRider(orderId: String, riderId: String) = withContext(Dispatchers.IO) {
        val rider = dao.getRiderById(riderId)
        if (rider != null) {
            dao.assignRiderToOrder(
                orderId = orderId,
                riderId = rider.id,
                riderName = rider.name,
                riderPhone = rider.phone
            )
            val order = dao.getOrderById(orderId)
            dao.insertNotification(
                NotificationEntity(
                    id = "notif_${System.currentTimeMillis()}",
                    title = "Rider Assigned 🛵",
                    message = "${rider.name} (${rider.bikeNumber}) has been assigned to order ${order?.orderNumber}.",
                    orderId = orderId,
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )
            )
        }
    }

    suspend fun riderPickUp(orderId: String) = withContext(Dispatchers.IO) {
        dao.updateOrderStatus(orderId, OrderStatus.PICKED_UP.name)
        dao.insertNotification(
            NotificationEntity(
                id = "notif_${System.currentTimeMillis()}",
                title = "Order Picked Up 🛍️",
                message = "Rider has collected your groceries from the Kirana shop.",
                orderId = orderId,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
        )
    }

    suspend fun riderOutForDelivery(orderId: String) = withContext(Dispatchers.IO) {
        dao.updateOrderStatus(orderId, OrderStatus.OUT_FOR_DELIVERY.name)
        dao.insertNotification(
            NotificationEntity(
                id = "notif_${System.currentTimeMillis()}",
                title = "Out for Delivery 🛵",
                message = "Your rider is nearby and heading to your location.",
                orderId = orderId,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
        )
    }

    suspend fun riderMarkDelivered(orderId: String, riderId: String, earnings: Double) = withContext(Dispatchers.IO) {
        dao.updateOrderStatus(orderId, OrderStatus.DELIVERED.name)
        dao.updatePaymentStatus(orderId, PaymentStatus.VERIFIED.name, null)
        dao.recordRiderDelivery(riderId, earnings)
        dao.insertNotification(
            NotificationEntity(
                id = "notif_${System.currentTimeMillis()}",
                title = "Delivered! 🎉",
                message = "Your GharKirana order has been delivered. Thank you for shopping local!",
                orderId = orderId,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
        )
    }

    suspend fun validateCoupon(code: String, subtotal: Double): Pair<Boolean, Double> = withContext(Dispatchers.IO) {
        val coupon = dao.getCoupon(code.trim().uppercase()) ?: return@withContext Pair(false, 0.0)
        if (subtotal < coupon.minOrder) return@withContext Pair(false, 0.0)

        val discount = if (coupon.discountPercent != null) {
            val calc = (subtotal * coupon.discountPercent) / 100.0
            calc.coerceAtMost(coupon.maxDiscount)
        } else {
            (coupon.discountFlat ?: 0.0).coerceAtMost(coupon.maxDiscount)
        }
        Pair(true, discount)
    }

    suspend fun saveAddress(address: DeliveryAddress) = withContext(Dispatchers.IO) {
        val entity = AddressEntity(
            id = address.id.ifBlank { "addr_${System.currentTimeMillis()}" },
            label = address.label,
            recipientName = address.recipientName,
            phone = address.phone,
            streetAddress = address.streetAddress,
            areaOrChowk = address.areaOrChowk,
            city = address.city,
            instructions = address.instructions,
            lat = address.lat,
            lng = address.lng,
            isDefault = false
        )
        dao.insertAddress(entity)
    }

    suspend fun markNotificationRead(id: String) = withContext(Dispatchers.IO) {
        dao.markNotificationRead(id)
    }

    private fun OrderEntity.toOrder(): Order {
        val itemsList = OrderItemsConverter.toList(itemsJson)
        return Order(
            id = id,
            orderNumber = orderNumber,
            customerId = customerId,
            customerName = customerName,
            customerPhone = customerPhone,
            deliveryAddress = DeliveryAddress(
                id = "addr_$id",
                label = addressLabel,
                recipientName = customerName,
                phone = customerPhone,
                streetAddress = addressStreet,
                areaOrChowk = addressArea,
                city = addressCity,
                instructions = addressInstructions,
                lat = addressLat,
                lng = addressLng
            ),
            items = itemsList,
            subtotal = subtotal,
            deliveryFee = deliveryFee,
            discountAmount = discountAmount,
            total = total,
            couponCode = couponCode,
            paymentMethod = try { PaymentMethod.valueOf(paymentMethod) } catch (e: Exception) { PaymentMethod.COD },
            paymentStatus = try { PaymentStatus.valueOf(paymentStatus) } catch (e: Exception) { PaymentStatus.PENDING },
            esewaRefId = esewaRefId,
            orderStatus = try { OrderStatus.valueOf(orderStatus) } catch (e: Exception) { OrderStatus.PLACED },
            assignedRiderId = assignedRiderId,
            assignedRiderName = assignedRiderName,
            assignedRiderPhone = assignedRiderPhone,
            createdAt = createdAt,
            estimatedMinutes = estimatedMinutes
        )
    }
}
