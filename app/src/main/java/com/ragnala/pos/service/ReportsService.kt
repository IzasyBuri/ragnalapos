package com.ragnala.pos.service

import com.ragnala.pos.data.db.BestSellerRow
import com.ragnala.pos.data.db.OrderDao
import com.ragnala.pos.data.db.PaymentBreakdownRow
import com.ragnala.pos.data.db.PaymentDao
import com.ragnala.pos.data.repo.ExpenseRepository
import com.ragnala.pos.domain.OrderStatus

/** Aggregated report metrics for a [start, end] window (PRD §9 Reports). */
data class ReportSummary(
    val revenue: Long,
    val cogs: Long,
    val expenses: Long,
    val profit: Long,
    val orderCount: Int,
    val cancelledAmount: Long,
    val voidedAmount: Long,
    val bestSellers: List<BestSellerRow>,
    val paymentBreakdown: List<PaymentBreakdownRow>,
)

/**
 * PRD §9 Reports: Revenue = confirmed payments on PAID/FULFILLED/ARCHIVED orders
 * (excludes Cancelled/Voided). Profit = Revenue − COGS − Expenses. COGS uses the
 * snapshot taken on the order (OrderEntity.cogs). Payment-method breakdown from the
 * payments table. Reporting window is local wall time (PRD §15 Time).
 */
class ReportsService(
    private val orderDao: OrderDao,
    private val paymentDao: PaymentDao,
    private val expenseRepository: ExpenseRepository,
) {
    suspend fun summary(start: Long, end: Long): ReportSummary {
        val revenueOrders = orderDao.revenueOrdersBetween(start, end)
        val revenue = revenueOrders.sumOf { it.total }
        val cogs = revenueOrders.sumOf { it.cogs ?: 0L }
        val expenses = expenseRepository.between(start, end).sumOf { it.amount }

        val nonRevenue = orderDao.voidedCancelledBetween(start, end)
        val cancelledAmount = nonRevenue
            .filter { it.status == OrderStatus.CANCELLED }
            .sumOf { it.total }
        val voidedAmount = nonRevenue
            .filter { it.status == OrderStatus.VOIDED }
            .sumOf { it.total }

        return ReportSummary(
            revenue = revenue,
            cogs = cogs,
            expenses = expenses,
            profit = revenue - cogs - expenses,
            orderCount = revenueOrders.size,
            cancelledAmount = cancelledAmount,
            voidedAmount = voidedAmount,
            bestSellers = orderDao.bestSellersBetween(start, end),
            paymentBreakdown = paymentDao.paymentBreakdownBetween(start, end),
        )
    }
}
