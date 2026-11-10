package com.moudonut.admin

import com.moudonut.admin.data.AdminDayBill
import com.moudonut.admin.data.StoreSummary
import com.moudonut.admin.data.SummarySource
import com.moudonut.admin.ui.SummaryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeSource : SummarySource {
        var summaryCalls = 0
        var dayBillsCalls = 0

        override suspend fun summary(date: String): StoreSummary {
            summaryCalls++
            return StoreSummary(businessDate = date, total = 0.0)
        }

        override suspend fun dayBills(date: String): List<AdminDayBill> {
            dayBillsCalls++
            return emptyList()
        }
    }

    @Test
    fun resumeWhileFirstLoadRunsDoesNotFetchAgain() = runTest(dispatcher) {
        val source = FakeSource()
        val vm = SummaryViewModel(source)

        vm.onScreenResumed()
        advanceUntilIdle()

        assertEquals(1, source.summaryCalls)
        assertEquals(1, source.dayBillsCalls)
    }

    @Test
    fun resumeAfterLoadRefreshesBothReads() = runTest(dispatcher) {
        val source = FakeSource()
        val vm = SummaryViewModel(source)
        advanceUntilIdle()

        vm.onScreenResumed()
        advanceUntilIdle()

        assertEquals(2, source.summaryCalls)
        assertEquals(2, source.dayBillsCalls)
    }
}
