package com.ragnala.pos.ui.management

import com.ragnala.pos.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ragnala.pos.service.ReportSummary
import com.ragnala.pos.ui.components.RagnalaCard
import com.ragnala.pos.ui.components.RagnalaEmptyState
import com.ragnala.pos.ui.components.RagnalaMoneySize
import com.ragnala.pos.ui.components.RagnalaMoneyText
import com.ragnala.pos.ui.components.RagnalaPrimaryButton
import com.ragnala.pos.ui.components.RagnalaSectionHeader
import com.ragnala.pos.ui.theme.RagnalaRadius
import com.ragnala.pos.ui.theme.RagnalaSpacing

@Composable
fun ReportsScreen(viewModel: ReportsViewModel, onBack: () -> Unit) {
    val period by viewModel.period.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.padding(RagnalaSpacing.xxs)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.mgmt_back)) }
            Column(Modifier.weight(1f)) { Text("Reports", style = MaterialTheme.typography.headlineSmall); Text("Sales and store performance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            RagnalaPrimaryButton(stringResource(R.string.mgmt_refresh), { viewModel.refresh() }, modifier = Modifier.padding(end = RagnalaSpacing.xs))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = RagnalaSpacing.md, vertical = RagnalaSpacing.xs), horizontalArrangement = Arrangement.spacedBy(RagnalaSpacing.xs)) {
            ReportPeriod.entries.forEach { p -> FilterChip(selected = period == p, onClick = { viewModel.selectPeriod(p) }, label = { Text(p.label) }, modifier = Modifier.padding(vertical = 2.dp)) }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(RagnalaSpacing.md)) }
        if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.mgmt_loading)) }
        else summary?.let { ReportsContent(it) } ?: RagnalaEmptyState("No sales for this period", "Sales activity will appear here once orders are recorded.", Modifier.fillMaxSize())
    }
}

@Composable
private fun ReportsContent(s: ReportSummary) {
    val paymentTotal = s.paymentBreakdown.sumOf { it.amount }
    LazyColumn(Modifier.fillMaxWidth().padding(horizontal = RagnalaSpacing.md), contentPadding = PaddingValues(bottom = RagnalaSpacing.lg), verticalArrangement = Arrangement.spacedBy(RagnalaSpacing.md)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RagnalaSpacing.sm)) {
                KpiCard("Revenue", s.revenue, Modifier.weight(1f))
                KpiCard("Profit", s.profit, Modifier.weight(1f), MaterialTheme.colorScheme.primary)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RagnalaSpacing.sm)) {
                KpiCard("Orders", s.orderCount.toLong(), Modifier.weight(1f), isMoney = false)
                KpiCard("Expenses", s.expenses, Modifier.weight(1f))
            }
        }
        item { RagnalaSectionHeader("Operational details") }
        item { RagnalaCard { MetricRow("COGS", s.cogs); MetricRow("Voided", s.voidedAmount); MetricRow("Cancelled", s.cancelledAmount) } }
        item { RagnalaSectionHeader("Payment methods") }
        if (s.paymentBreakdown.isEmpty()) item { Text(stringResource(R.string.mgmt_no_confirmed_payments), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        else itemsIndexed(s.paymentBreakdown) { index, payment ->
            PaymentRow(payment.method, payment.amount, payment.count, paymentTotal)
        }
        item { RagnalaSectionHeader("Best sellers") }
        if (s.bestSellers.isEmpty()) item { Text(stringResource(R.string.mgmt_no_sales), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        else itemsIndexed(s.bestSellers, key = { _, it -> it.productName }) { index, best ->
            RagnalaCard(contentPadding = RagnalaSpacing.sm) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("${index + 1}", style = MaterialTheme.typography.titleLarge, color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = RagnalaSpacing.md)); Column(Modifier.weight(1f)) { Text(best.productName, style = MaterialTheme.typography.titleMedium); Text("${best.qty} sold", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; RagnalaMoneyText(best.revenue, size = RagnalaMoneySize.Small) } }
        }
    }
}

@Composable
private fun KpiCard(label: String, value: Long, modifier: Modifier, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface, isMoney: Boolean = true) {
    RagnalaCard(modifier = modifier) { Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); if (isMoney) RagnalaMoneyText(value, size = RagnalaMoneySize.Medium, color = color) else Text(value.toString(), style = MaterialTheme.typography.headlineSmall, color = color) }
}

@Composable
private fun PaymentRow(method: String, amount: Long, count: Int, total: Long) {
    val proportion = if (total > 0) amount.toFloat() / total else 0f
    RagnalaCard(contentPadding = RagnalaSpacing.sm) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("$method · $count", style = MaterialTheme.typography.titleMedium); Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) { Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth(proportion.coerceIn(0.02f, 1f)).padding(vertical = 3.dp)) {} } }; RagnalaMoneyText(amount, size = RagnalaMoneySize.Small, modifier = Modifier.padding(start = RagnalaSpacing.md)) } }
}

@Composable
private fun MetricRow(label: String, value: Long) { Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant); RagnalaMoneyText(value, size = RagnalaMoneySize.Small) } }
