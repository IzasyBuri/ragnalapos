package com.ragnala.pos.service

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ragnala.pos.data.db.IngredientEntity
import com.ragnala.pos.data.db.ProductEntity
import com.ragnala.pos.data.db.RagnalaDatabase
import com.ragnala.pos.data.db.RecipeItemEntity
import com.ragnala.pos.domain.OrderStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OrderServiceTest {

    private lateinit var db: RagnalaDatabase
    private lateinit var service: OrderService
    private lateinit var audit: AuditService

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RagnalaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        audit = AuditService(db.auditDao())
        service = OrderService(db, db.orderDao(), db.ingredientDao(), audit)
    }

    @After
    fun teardown() {
        db.close()
    }

    private suspend fun seedIngredient(id: String, stock: Double, cost: Long): String {
        db.ingredientDao().upsert(
            IngredientEntity(
                id = id, name = "ing-$id", unit = "g",
                currentStock = stock, minStock = 10.0, costPerUnit = cost,
                createdAt = 0, updatedAt = 0,
            ),
        )
        return id
    }

    private suspend fun seedRecipe(productId: String, ingredientId: String, qty: Double) {
        // Audit M2 FK enforced: a recipe row's productId must reference an existing product.
        db.productDao().upsert(
            ProductEntity(
                id = productId, categoryId = "c1", name = "prod-$productId", description = "",
                price = 25000, imagePath = null, available = true,
                createdAt = 0, updatedAt = 0,
            ),
        )
        db.ingredientDao().upsertRecipeItem(
            RecipeItemEntity(
                id = UUID.randomUUID().toString(),
                productId = productId,
                ingredientId = ingredientId,
                quantity = qty,
            ),
        )
    }

    private fun cartLine(productId: String, name: String, price: Long, qty: Int) =
        com.ragnala.pos.service.CartLine(productId, name, price, qty, null)

    private suspend fun assertThrowsSuspending(expected: Class<out Throwable>, block: suspend () -> Unit) {
        var thrown: Throwable? = null
        try {
            block()
        } catch (t: Throwable) {
            thrown = t
        }
        assertNotNull("Expected ${expected.simpleName} but nothing was thrown", thrown)
        assertTrue(
            "Expected ${expected.simpleName} but got ${thrown!!.javaClass.simpleName}: ${thrown.message}",
            expected.isInstance(thrown),
        )
    }

    @Test
    fun `confirm order creates waiting payment with correct totals`() = runTest {
        val draftId = UUID.randomUUID().toString()
        val id = service.confirmOrder(
            draftId, "Maya",
            listOf(cartLine("p1", "Latte", 25000, 1), cartLine("p2", "Croissant", 18000, 2)),
            scPercent = 5.0, taxPercent = 11.0, now = 1000L,
        )
        assertEquals(draftId, id)

        val order = db.orderDao().byId(draftId)!!
        assertEquals(OrderStatus.WAITING_PAYMENT, order.status)
        assertEquals("Maya", order.customerName)
        assertEquals(61000L, order.subtotal)          // 25000 + 2*18000
        assertEquals(3050L, order.serviceCharge)      // 5% of 61000
        assertEquals(7046L, order.tax)                // 11% of 64050 = 7045.5 -> 7046
        assertEquals(71096L, order.total)             // 61000 + 3050 + 7045.5 -> 71095.5 -> 71096
        assertEquals(1L, order.orderNumber.toLong())  // first of day
        assertNotNull(db.orderDao().itemsForOrder(draftId))
    }

    @Test
    fun `confirm order is idempotent`() = runTest {
        val draftId = UUID.randomUUID().toString()
        service.confirmOrder(draftId, "A", listOf(cartLine("p1", "X", 10000, 1)), 0.0, 0.0, 1000L)
        service.confirmOrder(draftId, "A", listOf(cartLine("p1", "X", 10000, 1)), 0.0, 0.0, 2000L)
        val order = db.orderDao().byId(draftId)!!
        assertEquals(OrderStatus.WAITING_PAYMENT, order.status)
        assertEquals(1L, db.orderDao().itemsForOrder(draftId).size.toLong())
    }

    @Test
    fun `concurrent confirms of the same draft create a single order`() = runTest {
        // H1 regression: a double-tap submits the SAME draftId twice. The idempotency guard lives
        // inside the transaction, and Room serializes transactions, so exactly one order + one item
        // set must result — no duplicate items, no duplicate order numbers.
        val draftId = UUID.randomUUID().toString()
        coroutineScope {
            val first = async(Dispatchers.IO) {
                service.confirmOrder(draftId, "A", listOf(cartLine("p1", "X", 10000, 1)), 0.0, 0.0, 1000L)
            }
            val second = async(Dispatchers.IO) {
                service.confirmOrder(draftId, "A", listOf(cartLine("p1", "X", 10000, 1)), 0.0, 0.0, 1000L)
            }
            awaitAll(first, second)
        }
        val order = db.orderDao().byId(draftId)!!
        assertEquals(OrderStatus.WAITING_PAYMENT, order.status)
        assertEquals(1L, db.orderDao().itemsForOrder(draftId).size.toLong())
    }

    @Test
    fun `cash payment deducts stock records payment and moves to paid`() = runTest {
        val espresso = seedIngredient("i1", 100.0, 2000)
        seedRecipe("p1", espresso, 18.0) // 18g per latte

        val draftId = UUID.randomUUID().toString()
        service.confirmOrder(draftId, "B", listOf(cartLine("p1", "Latte", 25000, 2)), 0.0, 0.0, 1000L)
        val order = db.orderDao().byId(draftId)!!

        val change = service.payOrderCash(draftId, tendered = 60000, pinVerified = true, userLabel = "barista", now = 2000L)

        assertEquals(10000L, change) // 60000 - 50000
        val paid = db.orderDao().byId(draftId)!!
        assertEquals(OrderStatus.PAID, paid.status)
        assertEquals(50000L, paid.total)
        assertEquals(64.0, db.ingredientDao().byId(espresso)!!.currentStock, 0.0001) // 100 - 2*18

        val payment = db.paymentDao().forOrder(draftId)
        assertEquals(1, payment.size)
        assertEquals(50000L, payment[0].amount)
        assertEquals(60000L, payment[0].tendered)
        assertEquals(10000L, payment[0].changeGiven)
    }

    @Test
    fun `payment requires pin verification`() = runTest {
        val draftId = UUID.randomUUID().toString()
        service.confirmOrder(draftId, "C", listOf(cartLine("p1", "X", 10000, 1)), 0.0, 0.0, 1000L)
        assertThrowsSuspending(IllegalStateException::class.java) {
            service.payOrderCash(draftId, 10000, pinVerified = false, userLabel = "x", now = 2000L)
        }
        // still WAITING_PAYMENT, stock untouched
        assertEquals(OrderStatus.WAITING_PAYMENT, db.orderDao().byId(draftId)!!.status)
    }

    @Test
    fun `cash payment with insufficient tender throws`() = runTest {
        val draftId = UUID.randomUUID().toString()
        service.confirmOrder(draftId, "D", listOf(cartLine("p1", "X", 10000, 1)), 0.0, 0.0, 1000L)
        assertThrowsSuspending(IllegalArgumentException::class.java) {
            service.payOrderCash(draftId, 5000, pinVerified = true, userLabel = "x", now = 2000L)
        }
        assertEquals(OrderStatus.WAITING_PAYMENT, db.orderDao().byId(draftId)!!.status)
    }

    @Test
    fun `void restores stock and audits reason`() = runTest {
        val espresso = seedIngredient("i1", 100.0, 2000)
        seedRecipe("p1", espresso, 18.0)

        val draftId = UUID.randomUUID().toString()
        service.confirmOrder(draftId, "E", listOf(cartLine("p1", "Latte", 25000, 1)), 0.0, 0.0, 1000L)
        service.payOrderCash(draftId, 25000, pinVerified = true, userLabel = "barista", now = 2000L)
        assertEquals(82.0, db.ingredientDao().byId(espresso)!!.currentStock, 0.0001)

        service.void(draftId, reason = "customer changed mind", pinVerified = true, userLabel = "barista", now = 3000L)

        assertEquals(OrderStatus.VOIDED, db.orderDao().byId(draftId)!!.status)
        assertEquals(100.0, db.ingredientDao().byId(espresso)!!.currentStock, 0.0001)
        val voidAudits = db.auditDao().observeRecent(50).first()
        assertTrue(voidAudits.any { it.action == "VOID" && it.reason == "customer changed mind" })
    }

    @Test
    fun `void requires pin and reason`() = runTest {
        val draftId = UUID.randomUUID().toString()
        service.confirmOrder(draftId, "F", listOf(cartLine("p1", "X", 10000, 1)), 0.0, 0.0, 1000L)
        service.payOrderCash(draftId, 10000, pinVerified = true, userLabel = "b", now = 2000L)

        assertThrowsSuspending(IllegalStateException::class.java) {
            service.void(draftId, "reason", pinVerified = false, userLabel = "b", now = 3000L)
        }
        assertThrowsSuspending(IllegalArgumentException::class.java) {
            service.void(draftId, "  ", pinVerified = true, userLabel = "b", now = 3000L)
        }
        assertEquals(OrderStatus.PAID, db.orderDao().byId(draftId)!!.status)
    }

    @Test
    fun `cancel before payment requires reason`() = runTest {
        val draftId = UUID.randomUUID().toString()
        service.confirmOrder(draftId, "G", listOf(cartLine("p1", "X", 10000, 1)), 0.0, 0.0, 1000L)
        service.cancel(draftId, reason = "customer walked away", userLabel = "barista", now = 2000L)
        assertEquals(OrderStatus.CANCELLED, db.orderDao().byId(draftId)!!.status)

        assertThrowsSuspending(IllegalArgumentException::class.java) {
            service.cancel(draftId, " ", userLabel = "b", now = 3000L)
        }
    }

    @Test
    fun `happy path to fulfilled and archived`() = runTest {
        val draftId = UUID.randomUUID().toString()
        service.confirmOrder(draftId, "H", listOf(cartLine("p1", "X", 10000, 1)), 0.0, 0.0, 1000L)
        service.payOrderCash(draftId, 10000, pinVerified = true, userLabel = "b", now = 2000L)
        service.transition(draftId, OrderStatus.FULFILLED, 3000L)
        service.transition(draftId, OrderStatus.ARCHIVED, 6000L)

        val order = db.orderDao().byId(draftId)!!
        assertEquals(OrderStatus.ARCHIVED, order.status)
        assertEquals(6000L, order.completedAt)
    }

    @Test
    fun `illegal transition is rejected`() = runTest {
        val draftId = UUID.randomUUID().toString()
        service.confirmOrder(draftId, "I", listOf(cartLine("p1", "X", 10000, 1)), 0.0, 0.0, 1000L)
        assertThrowsSuspending(IllegalStateException::class.java) {
            service.transition(draftId, OrderStatus.FULFILLED, 2000L) // skipping payment
        }
    }

    @Test
    fun `order numbers reset daily`() = runTest {
        val day1 = java.util.Calendar.getInstance().apply {
            set(2026, 0, 5, 10, 0, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val day2 = java.util.Calendar.getInstance().apply {
            set(2026, 0, 6, 10, 0, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val a = UUID.randomUUID().toString()
        val b = UUID.randomUUID().toString()
        service.confirmOrder(a, null, listOf(cartLine("p1", "X", 1000, 1)), 0.0, 0.0, day1)
        service.confirmOrder(b, null, listOf(cartLine("p1", "X", 1000, 1)), 0.0, 0.0, day1)
        assertEquals(1L, db.orderDao().byId(a)!!.orderNumber)
        assertEquals(2L, db.orderDao().byId(b)!!.orderNumber)

        val c = UUID.randomUUID().toString()
        service.confirmOrder(c, null, listOf(cartLine("p1", "X", 1000, 1)), 0.0, 0.0, day2)
        assertEquals(1L, db.orderDao().byId(c)!!.orderNumber)
    }

    @Test
    fun `non-cash QRIS payment records payment and moves to paid`() = runTest {
        val espresso = seedIngredient("i1", 100.0, 2000)
        seedRecipe("p1", espresso, 18.0)

        val draftId = UUID.randomUUID().toString()
        service.confirmOrder(draftId, "Q", listOf(cartLine("p1", "Latte", 25000, 2)), 0.0, 0.0, 1000L)

        service.payOrderNonCash(draftId, method = "QRIS", pinVerified = true, userLabel = "barista", now = 2000L)

        val paid = db.orderDao().byId(draftId)!!
        assertEquals(OrderStatus.PAID, paid.status)
        assertEquals(50000L, paid.total)
        assertEquals(64.0, db.ingredientDao().byId(espresso)!!.currentStock, 0.0001) // 100 - 2*18

        val payment = db.paymentDao().forOrder(draftId)
        assertEquals(1, payment.size)
        assertEquals("QRIS", payment[0].method)
        assertEquals(50000L, payment[0].amount)
        assertEquals(null, payment[0].tendered)
    }

    @Test
    fun `stale waiting payment scan flags orphans`() = runTest {
        val draftId = UUID.randomUUID().toString()
        service.confirmOrder(draftId, "J", listOf(cartLine("p1", "X", 1000, 1)), 0.0, 0.0, 1000L)
        val stale = service.staleWaitingPayment(olderThan = 2000L)
        assertEquals(1, stale.size)
        assertEquals(draftId, stale[0].id)
    }
}
