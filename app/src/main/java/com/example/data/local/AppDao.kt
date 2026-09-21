package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Products
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProductsForAdmin(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId AND isActive = 1")
    fun getProductsByCategory(categoryId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isFeatured = 1 AND isActive = 1")
    fun getFeaturedProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE stock < 10")
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET stock = :newStock WHERE id = :productId")
    suspend fun updateStock(productId: String, newStock: Int)

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteProduct(productId: String)

    // Categories
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    // Orders
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getCustomerOrders(customerId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE assignedRiderId = :riderId ORDER BY createdAt DESC")
    fun getRiderOrders(riderId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    fun getOrderFlowById(orderId: String): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Query("UPDATE orders SET orderStatus = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String)

    @Query("UPDATE orders SET assignedRiderId = :riderId, assignedRiderName = :riderName, assignedRiderPhone = :riderPhone, orderStatus = 'RIDER_ASSIGNED' WHERE id = :orderId")
    suspend fun assignRiderToOrder(orderId: String, riderId: String, riderName: String, riderPhone: String)

    @Query("UPDATE orders SET paymentStatus = :status, esewaRefId = :refId WHERE id = :orderId")
    suspend fun updatePaymentStatus(orderId: String, status: String, refId: String?)

    // Riders
    @Query("SELECT * FROM riders")
    fun getAllRiders(): Flow<List<RiderEntity>>

    @Query("SELECT * FROM riders WHERE id = :riderId LIMIT 1")
    suspend fun getRiderById(riderId: String): RiderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRiders(riders: List<RiderEntity>)

    @Query("UPDATE riders SET isOnline = :isOnline WHERE id = :riderId")
    suspend fun updateRiderStatus(riderId: String, isOnline: Boolean)

    @Query("UPDATE riders SET todayEarnings = todayEarnings + :amount, totalDeliveries = totalDeliveries + 1 WHERE id = :riderId")
    suspend fun recordRiderDelivery(riderId: String, amount: Double)

    // Coupons
    @Query("SELECT * FROM coupons WHERE isActive = 1")
    fun getActiveCoupons(): Flow<List<CouponEntity>>

    @Query("SELECT * FROM coupons WHERE code = :code AND isActive = 1 LIMIT 1")
    suspend fun getCoupon(code: String): CouponEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoupons(coupons: List<CouponEntity>)

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationRead(id: String)

    // Saved Addresses
    @Query("SELECT * FROM addresses ORDER BY isDefault DESC")
    fun getAddresses(): Flow<List<AddressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddress(address: AddressEntity)

    @Query("DELETE FROM addresses WHERE id = :id")
    suspend fun deleteAddress(id: String)
}
