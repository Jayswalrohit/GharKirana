package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.GroceryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GroceryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GroceryRepository
    init {
        val db = AppDatabase.getInstance(application)
        repository = GroceryRepository(db.appDao())
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    // Role state
    private val _currentRole = MutableStateFlow(UserRole.CUSTOMER)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    fun switchRole(role: UserRole) {
        _currentRole.value = role
    }

    // Current Customer Profile
    val currentCustomer = MutableStateFlow(
        User(
            id = "user_cust_01",
            name = "Aayush Sharma",
            phone = "+977-9841001122",
            email = "aayush.sharma@gmail.com",
            role = UserRole.CUSTOMER
        )
    )

    // Current Active Rider Profile for Rider View
    private val _activeRiderId = MutableStateFlow("rider_01")
    val activeRiderId: StateFlow<String> = _activeRiderId.asStateFlow()

    fun setActiveRider(riderId: String) {
        _activeRiderId.value = riderId
    }

    // Delivery Location
    private val _currentLocation = MutableStateFlow("Bhanu Chowk, Janakpur Dham")
    val currentLocation: StateFlow<String> = _currentLocation.asStateFlow()

    fun setLocation(loc: String) {
        _currentLocation.value = loc
    }

    // Products & Categories Streams
    val categories: StateFlow<List<Category>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActiveProducts: StateFlow<List<Product>> = repository.activeProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminProducts: StateFlow<List<Product>> = repository.adminProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search and Category Filter
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val filteredProducts: StateFlow<List<Product>> = combine(
        allActiveProducts,
        _selectedCategoryId,
        _searchQuery
    ) { products, catId, query ->
        products.filter { p ->
            val matchesCategory = catId == null || p.categoryId == catId
            val matchesSearch = query.isBlank() ||
                    p.name.contains(query, ignoreCase = true) ||
                    p.nepaliName.contains(query, ignoreCase = true) ||
                    p.categoryName.contains(query, ignoreCase = true) ||
                    p.brand.contains(query, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val featuredProducts: StateFlow<List<Product>> = allActiveProducts.map { list ->
        list.filter { it.isFeatured }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val discountOffers: StateFlow<List<Product>> = allActiveProducts.map { list ->
        list.filter { it.hasDiscount }.sortedByDescending { it.discountPercent }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart Management via dedicated CartViewModel
    val cartViewModel = CartViewModel()
    val cartItems: StateFlow<List<CartItem>> = cartViewModel.cartItems

    fun addToCart(product: Product) {
        cartViewModel.addItem(product)
    }

    fun decrementCart(productId: String) {
        cartViewModel.decrementItem(productId)
    }

    fun removeCartItem(productId: String) {
        cartViewModel.removeItem(productId)
    }

    fun clearCart() {
        cartViewModel.clearCart()
    }

    // Coupon & Price Totals
    val appliedCoupon: StateFlow<Coupon?> = cartViewModel.appliedCoupon
    val couponError: StateFlow<String?> = cartViewModel.couponError

    fun applyCouponCode(code: String) {
        viewModelScope.launch {
            val subtotal = cartSubtotal.value
            val (valid, _) = repository.validateCoupon(code, subtotal)
            val couponList = repository.coupons.first()
            if (valid) {
                cartViewModel.applyCouponCode(code, couponList)
            } else {
                val found = couponList.find { it.code.equals(code.trim(), ignoreCase = true) }
                if (found != null) {
                    cartViewModel.applyCoupon(found)
                } else {
                    cartViewModel.applyCouponCode(code, emptyList())
                }
            }
        }
    }

    fun removeCoupon() {
        cartViewModel.removeCoupon()
    }

    val cartSubtotal: StateFlow<Double> = cartViewModel.subtotal
    val cartDeliveryFee: StateFlow<Double> = cartViewModel.deliveryFee
    val cartDiscount: StateFlow<Double> = cartViewModel.couponDiscount
    val cartTotal: StateFlow<Double> = cartViewModel.total

    // Saved Addresses
    val addresses: StateFlow<List<DeliveryAddress>> = repository.addresses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedAddress = MutableStateFlow<DeliveryAddress?>(null)
    val selectedAddress: StateFlow<DeliveryAddress?> = _selectedAddress.asStateFlow()

    fun selectAddress(addr: DeliveryAddress) {
        _selectedAddress.value = addr
    }

    fun addAddress(addr: DeliveryAddress) {
        viewModelScope.launch {
            repository.saveAddress(addr)
            _selectedAddress.value = addr
        }
    }

    // Orders & Tracking
    val allOrders: StateFlow<List<Order>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customerOrders: StateFlow<List<Order>> = repository.getCustomerOrders(currentCustomer.value.id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val riderOrders: StateFlow<List<Order>> = _activeRiderId.flatMapLatest { id ->
        repository.getRiderOrders(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableRiders: StateFlow<List<Rider>> = repository.riders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<AppNotification>> = repository.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _trackedOrderId = MutableStateFlow<String?>("order_101")
    val trackedOrderId: StateFlow<String?> = _trackedOrderId.asStateFlow()

    fun trackOrder(orderId: String?) {
        _trackedOrderId.value = orderId
    }

    val trackedOrder: StateFlow<Order?> = _trackedOrderId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getOrderFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Order Creation
    fun placeOrder(
        paymentMethod: PaymentMethod,
        esewaRefId: String?,
        onSuccess: (Order) -> Unit
    ) {
        viewModelScope.launch {
            val addr = _selectedAddress.value ?: addresses.value.firstOrNull() ?: DeliveryAddress(
                id = "addr_default",
                label = "Home",
                recipientName = currentCustomer.value.name,
                phone = currentCustomer.value.phone,
                streetAddress = "Station Road, Near Bhanu Chowk",
                areaOrChowk = "Bhanu Chowk",
                city = "Janakpur Dham"
            )

            val order = repository.createOrder(
                customer = currentCustomer.value,
                address = addr,
                items = cartItems.value,
                deliveryFee = cartDeliveryFee.value,
                discountAmount = cartDiscount.value,
                couponCode = appliedCoupon.value?.code,
                paymentMethod = paymentMethod,
                paymentStatus = if (paymentMethod == PaymentMethod.ESEWA) PaymentStatus.VERIFIED else PaymentStatus.PENDING,
                esewaRefId = esewaRefId
            )

            clearCart()
            _trackedOrderId.value = order.id
            onSuccess(order)
        }
    }

    // Admin Order Workflow Actions
    fun confirmOrder(orderId: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, OrderStatus.CONFIRMED)
        }
    }

    fun prepareOrder(orderId: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, OrderStatus.PREPARING)
        }
    }

    fun markOrderReady(orderId: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, OrderStatus.READY_FOR_PICKUP)
        }
    }

    fun assignRiderToOrder(orderId: String, riderId: String) {
        viewModelScope.launch {
            repository.assignRider(orderId, riderId)
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, OrderStatus.CANCELLED)
        }
    }

    // Admin Inventory / Product CRUD
    fun saveProduct(product: Product) {
        viewModelScope.launch {
            repository.saveProduct(product)
        }
    }

    fun adjustStock(productId: String, delta: Int) {
        viewModelScope.launch {
            val currentProduct = adminProducts.value.find { it.id == productId } ?: return@launch
            repository.updateStock(productId, currentProduct.stock + delta)
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
        }
    }

    // Rider Actions
    fun riderPickUpOrder(orderId: String) {
        viewModelScope.launch {
            repository.riderPickUp(orderId)
        }
    }

    fun riderOutForDelivery(orderId: String) {
        viewModelScope.launch {
            repository.riderOutForDelivery(orderId)
        }
    }

    fun riderMarkDelivered(orderId: String) {
        viewModelScope.launch {
            repository.riderMarkDelivered(orderId, _activeRiderId.value, 40.0)
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }
}
