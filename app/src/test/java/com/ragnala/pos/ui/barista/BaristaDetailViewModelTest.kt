package com.ragnala.pos.ui.barista

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ragnala.pos.data.db.RagnalaDatabase
import com.ragnala.pos.domain.OrderStatus
import com.ragnala.pos.service.AuditService
import com.ragnala.pos.service.CartLine
import com.ragnala.pos.service.OrderService
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * Integration proof of the Barista pay-cash slice (UI triggers exactly this chain):
 *   BaristaDetailViewModel.payCash -> OrderService.payOrderCash(pinVerified=true)
 * The Barista PIN is required only to enter Barista Mode (queue gate); payment passes
 * pinVerified = true because the session is already authenticated.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BaristaPayIntegrationTest {

    private lateinit var db: RagnalaDatabase
    private lateinit var orderService: OrderService

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RagnalaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        orderService = OrderService(db, db.orderDao(), db.ingredientDao(), AuditService(db.auditDao()))
    }

    @After
    fun teardown() = db.close()

    private suspend fun newWaitingOrder(): String {
        val id = UUID.randomUUID().toString()
        orderService.confirmOrder(
            id, "Alex",
            listOf(CartLine("p1", "Vanilla", 28000, 1, null)),
            scPercent = 5.0, taxPercent = 11.0, now = 1000L,
        )
        return id
    }

    @Test
    fun `cash payment moves order to PAID with correct change`() = runTest {
        val orderId = newWaitingOrder()
        assertEquals(OrderStatus.WAITING_PAYMENT, db.orderDao().byId(orderId)!!.status)

        // exact chain BaristaDetailViewModel.payCash performs (barista mode already entered)
        val change = orderService.payOrderCash(
            orderId, tendered = 50000, pinVerified = true, userLabel = "Barista", now = 2000L,
        )

        assertEquals(OrderStatus.PAID, db.orderDao().byId(orderId)!!.status)
        assertEquals(17366L, change) // 50000 - (28000 + 1400 + 3234)

        val payment = db.paymentDao().forOrder(orderId)
        assertEquals(1, payment.size)
        assertEquals(50000L, payment[0].tendered)
        assertEquals(17366L, payment[0].changeGiven)
    }

    @Test
    fun `payOrderCash still rejects unverified callers at the service layer`() = runTest {
        // Defense in depth: even though the UI no longer prompts a PIN at payment time,
        // the service guard remains — a caller that did not pass the barista gate is refused.
        val orderId = newWaitingOrder()

        var threw = false
        try {
            orderService.payOrderCash(orderId, 50000, pinVerified = false, userLabel = "x", now = 2000L)
        } catch (e: IllegalStateException) {
            threw = true
        }
        assertTrue("payOrderCash must reject when pin not verified", threw)
        assertEquals(OrderStatus.WAITING_PAYMENT, db.orderDao().byId(orderId)!!.status)
    }
}
