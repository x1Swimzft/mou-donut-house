package com.moudonut.admin

import com.moudonut.admin.data.AdminProduct
import com.moudonut.admin.data.ProductsSource
import com.moudonut.admin.data.ToggledProduct
import com.moudonut.admin.ui.ProductsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeProductsSource : ProductsSource {
        var productsList = mutableListOf(
            AdminProduct("p1", "ไส้กรอก", 3.50, "donut", true, 1),
            AdminProduct("p2", "น้ำส้ม", 20.0, "drink", true, 2)
        )
        var toggleCalls = 0
        var updatePriceCalls = 0
        var createCalls = 0
        var shouldThrow: Throwable? = null

        override suspend fun adminProducts(): List<AdminProduct> {
            shouldThrow?.let { throw it }
            return productsList
        }

        override suspend fun toggleProduct(productId: String, isAvailable: Boolean): ToggledProduct {
            shouldThrow?.let { throw it }
            toggleCalls++
            val idx = productsList.indexOfFirst { it.id == productId }
            if (idx >= 0) {
                productsList[idx] = productsList[idx].copy(isAvailable = isAvailable)
            }
            return ToggledProduct(productId, "ไส้กรอก", isAvailable)
        }

        override suspend fun updateProductPrice(productId: String, price: Double): AdminProduct {
            shouldThrow?.let { throw it }
            updatePriceCalls++
            val idx = productsList.indexOfFirst { it.id == productId }
            val updated = productsList[idx].copy(price = price)
            productsList[idx] = updated
            return updated
        }

        override suspend fun createProduct(name: String, price: Double, category: String): AdminProduct {
            shouldThrow?.let { throw it }
            createCalls++
            val newProd = AdminProduct("p3", name, price, category, true, 0)
            productsList.add(newProd)
            return newProd
        }
    }

    @Test
    fun initialLoadFetchesProducts() = runTest(dispatcher) {
        val source = FakeProductsSource()
        val vm = ProductsViewModel(source)
        advanceUntilIdle()

        assertEquals(2, vm.state.value.products.size)
        assertEquals(false, vm.state.value.loading)
        assertNull(vm.state.value.error)
    }

    @Test
    fun toggleProductUpdatesState() = runTest(dispatcher) {
        val source = FakeProductsSource()
        val vm = ProductsViewModel(source)
        advanceUntilIdle()

        vm.toggleProduct("p1", false)
        advanceUntilIdle()

        assertEquals(1, source.toggleCalls)
        assertEquals(false, vm.state.value.products.first { it.id == "p1" }.isAvailable)
        assertNotNull(vm.state.value.message)
        assertTrue(vm.state.value.message!!.contains("ปิดขายชั่วคราว"))
    }

    @Test
    fun updatePriceSuccess() = runTest(dispatcher) {
        val source = FakeProductsSource()
        val vm = ProductsViewModel(source)
        advanceUntilIdle()

        vm.updatePrice("p1", 5.0)
        advanceUntilIdle()

        assertEquals(1, source.updatePriceCalls)
        assertEquals(5.0, vm.state.value.products.first { it.id == "p1" }.price, 0.001)
        assertNotNull(vm.state.value.message)
        assertTrue(vm.state.value.message!!.contains("อัปเดตราคา"))
    }

    @Test
    fun updatePriceNegativeFailsValidation() = runTest(dispatcher) {
        val source = FakeProductsSource()
        val vm = ProductsViewModel(source)
        advanceUntilIdle()

        vm.updatePrice("p1", -1.0)
        advanceUntilIdle()

        assertEquals(0, source.updatePriceCalls)
        assertEquals("ราคาต้องไม่ติดลบ", vm.state.value.error)
    }

    @Test
    fun createProductSuccess() = runTest(dispatcher) {
        val source = FakeProductsSource()
        val vm = ProductsViewModel(source)
        advanceUntilIdle()

        vm.createProduct("ชาเย็น", 25.0, "drink")
        advanceUntilIdle()

        assertEquals(1, source.createCalls)
        assertEquals(3, vm.state.value.products.size)
        assertEquals("ชาเย็น", vm.state.value.products.last().name)
        assertNotNull(vm.state.value.message)
        assertTrue(vm.state.value.message!!.contains("เพิ่มสินค้า \"ชาเย็น\""))
    }

    @Test
    fun createProductValidatesEmptyName() = runTest(dispatcher) {
        val source = FakeProductsSource()
        val vm = ProductsViewModel(source)
        advanceUntilIdle()

        vm.createProduct("   ", 10.0, "donut")
        advanceUntilIdle()

        assertEquals(0, source.createCalls)
        assertEquals("กรุณากรอกชื่อสินค้า", vm.state.value.error)
    }

    @Test
    fun createProductValidatesNegativePrice() = runTest(dispatcher) {
        val source = FakeProductsSource()
        val vm = ProductsViewModel(source)
        advanceUntilIdle()

        vm.createProduct("โดนัทช็อกโกแลต", -5.0, "donut")
        advanceUntilIdle()

        assertEquals(0, source.createCalls)
        assertEquals("ราคาต้องไม่ติดลบ", vm.state.value.error)
    }
}
