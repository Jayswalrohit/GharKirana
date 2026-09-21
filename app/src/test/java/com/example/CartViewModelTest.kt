package com.example

import com.example.data.model.CartItem
import com.example.data.model.Coupon
import com.example.data.model.Product
import com.example.ui.viewmodel.CartViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CartViewModelTest {

    private lateinit var cartViewModel: CartViewModel

    private val testApple = Product(
        id = "prod_apple",
        name = "Royal Gala Apple",
        nepaliName = "स्याउ",
        categoryId = "cat_fruits",
        categoryName = "Fresh Fruits",
        price = 220.0,
        discountPrice = 190.0, // 30 NPR discount
        stock = 5,
        unit = "1 KG"
    )

    private val testRice = Product(
        id = "prod_rice",
        name = "Jeera Masino Rice",
        nepaliName = "जीरा मसिनो चामल",
        categoryId = "cat_staples",
        categoryName = "Rice & Grains",
        price = 1200.0,
        discountPrice = null,
        stock = 10,
        unit = "25 KG"
    )

    private val outOfStockMilk = Product(
        id = "prod_milk",
        name = "Cow Milk",
        categoryId = "cat_dairy",
        categoryName = "Dairy",
        price = 90.0,
        stock = 0,
        unit = "1 L"
    )

    @Before
    fun setUp() {
        cartViewModel = CartViewModel(freeDeliveryThreshold = 899.0, baseDeliveryFee = 40.0)
    }

    @Test
    fun `initial cart state is completely empty`() {
        assertTrue(cartViewModel.isCartEmpty.value)
        assertEquals(0, cartViewModel.itemCount.value)
        assertEquals(0, cartViewModel.distinctItemCount.value)
        assertEquals(0.0, cartViewModel.subtotal.value, 0.001)
        assertEquals(0.0, cartViewModel.deliveryFee.value, 0.001)
        assertEquals(0.0, cartViewModel.total.value, 0.001)
        assertNull(cartViewModel.appliedCoupon.value)
    }

    @Test
    fun `adding item updates cart items and quantities`() {
        val success = cartViewModel.addItem(testApple, quantity = 2)
        assertTrue(success)

        assertEquals(1, cartViewModel.distinctItemCount.value)
        assertEquals(2, cartViewModel.itemCount.value)
        assertEquals(2, cartViewModel.currentItemCount)
        assertFalse(cartViewModel.isCartEmpty.value)
        assertEquals(380.0, cartViewModel.currentSubtotal, 0.001)

        val item = cartViewModel.getItem("prod_apple")
        assertNotNull(item)
        assertEquals(2, item!!.quantity)
        assertEquals(190.0, item.unitPrice, 0.001)
        assertEquals(380.0, item.total, 0.001)
        assertEquals(60.0, item.totalSavings, 0.001)
    }

    @Test
    fun `cannot add out of stock product`() {
        val added = cartViewModel.addItem(outOfStockMilk)
        assertFalse(added)
        assertTrue(cartViewModel.isCartEmpty.value)
    }

    @Test
    fun `adding item caps at stock limit`() {
        // testApple stock is 5
        cartViewModel.addItem(testApple, quantity = 4)
        assertEquals(4, cartViewModel.getItemQuantity("prod_apple"))

        // Add 3 more, should cap at 5
        cartViewModel.addItem(testApple, quantity = 3)
        assertEquals(5, cartViewModel.getItemQuantity("prod_apple"))
        assertTrue(cartViewModel.getItem("prod_apple")!!.isMaxStockReached)
    }

    @Test
    fun `increment and decrement work properly and remove when reaching zero`() {
        cartViewModel.addItem(testApple, quantity = 1)
        assertEquals(1, cartViewModel.getItemQuantity("prod_apple"))

        cartViewModel.incrementItem("prod_apple")
        assertEquals(2, cartViewModel.getItemQuantity("prod_apple"))

        cartViewModel.decrementItem("prod_apple")
        assertEquals(1, cartViewModel.getItemQuantity("prod_apple"))

        cartViewModel.decrementItem("prod_apple")
        assertEquals(0, cartViewModel.getItemQuantity("prod_apple"))
        assertTrue(cartViewModel.isCartEmpty.value)
    }

    @Test
    fun `delivery fee is calculated based on free delivery threshold`() {
        // Free delivery threshold is 899
        cartViewModel.addItem(testApple, quantity = 1) // 190 NPR
        assertEquals(190.0, cartViewModel.currentSubtotal, 0.001)
        assertEquals(40.0, cartViewModel.currentDeliveryFee, 0.001)

        // Add Rice 1200 NPR -> subtotal = 1390 NPR >= 899 NPR
        cartViewModel.addItem(testRice, quantity = 1)
        assertEquals(1390.0, cartViewModel.currentSubtotal, 0.001)
        assertEquals(0.0, cartViewModel.currentDeliveryFee, 0.001)
    }

    @Test
    fun `coupon application validates minimum order`() {
        val coupon = Coupon(
            code = "JANAKPUR50",
            discountFlat = 50.0,
            minOrder = 500.0,
            description = "Rs 50 off on orders above 500"
        )

        cartViewModel.addItem(testApple, quantity = 1) // 190 NPR < 500 NPR
        assertEquals(190.0, cartViewModel.currentSubtotal, 0.001)
        val applied = cartViewModel.applyCoupon(coupon)
        assertFalse(applied)
        assertNotNull(cartViewModel.couponError.value)

        // Increase order over 500 NPR
        cartViewModel.addItem(testApple, quantity = 2) // 3 * 190 = 570 NPR >= 500 NPR
        assertEquals(570.0, cartViewModel.currentSubtotal, 0.001)
        val appliedSuccess = cartViewModel.applyCoupon(coupon)
        assertTrue(appliedSuccess)
        assertNull(cartViewModel.couponError.value)
        assertEquals(50.0, cartViewModel.currentCouponDiscount, 0.001)
    }

    @Test
    fun `toOrderItems converts correctly for checkout`() {
        cartViewModel.addItem(testApple, quantity = 2)
        val orderItems = cartViewModel.toOrderItems()

        assertEquals(1, orderItems.size)
        val first = orderItems.first()
        assertEquals("prod_apple", first.productId)
        assertEquals("Royal Gala Apple", first.productName)
        assertEquals(2, first.quantity)
        assertEquals(190.0, first.price, 0.001)
        assertEquals(380.0, first.total, 0.001)
    }

    @Test
    fun `clearCart resets all local state`() {
        cartViewModel.addItem(testApple, quantity = 2)
        cartViewModel.clearCart()

        assertTrue(cartViewModel.isCartEmpty.value)
        assertEquals(0, cartViewModel.itemCount.value)
        assertEquals(0.0, cartViewModel.subtotal.value, 0.001)
    }
}
