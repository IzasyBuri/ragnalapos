package com.ragnala.pos.ui.management

import com.ragnala.pos.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ragnala.pos.service.ReportSummary
import com.ragnala.pos.ui.customer.formatRupiah

/** Reports (PRD Â§9): revenue, profit, COGS, expenses, voided/cancelled, best sellers, payment split. */
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onBack: () -> Unit,
) {
    val period by viewModel.period.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.mgmt_back)) }
                Text(stringResource(R.string.mgmt_reports_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReportPeriod.entries.forEach { p ->
                FilterChip(
                    selected = period == p,
                    onClick = { viewModel.selectPeriod(p) },
                    label = { Text(p.label) },
                )
            }
        }
        Button(onClick = { viewModel.refresh() }, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(stringResource(R.string.mgmt_refresh))
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.mgmt_loading)) }
        } else {
            val s = summary
            if (s == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.mgmt_no_data), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            } else {
                ReportsContent(s, onBack = onBack)
            }
        }
    }
}

@Composable
private fun ReportsContent(s: ReportSummary, onBack: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    MetricRow(stringResource(R.string.mgmt_orders), "${s.orderCount}")
                    MetricRow(stringResource(R.string.mgmt_revenue), formatRup(s.revenue), bold = true)
                    MetricRow(stringResource(R.string.mgmt_cost_of_goods), formatRup(s.cogs))
                    MetricRow(stringResource(R.string.mgmt_expenses_title), formatRup(s.expenses))
                    MetricRow(stringResource(R.string.mgmt_estimated_profit), formatRup(s.profit), bold = true)
                    MetricRow(stringResource(R.string.mgmt_voided), formatRup(s.voidedAmount))
                    MetricRow(stringResource(R.string.mgmt_cancelled), formatRup(s.cancelledAmount))
                }
            }
        }
        item { Text(stringResource(R.string.mgmt_by_payment_method), style = MaterialTheme.typography.titleMedium) }
        if (s.paymentBreakdown.isEmpty()) {
            item { Text(stringResource(R.string.mgmt_no_confirmed_payments), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            s.paymentBreakdown.forEach { p ->
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text("${p.method} (${p.count})", modifier = Modifier.weight(1f))
                        Text(formatRup(p.amount))
                    }
                }
            }
        }
        item { Text(stringResource(R.string.mgmt_best_sellers), style = MaterialTheme.typography.titleMedium) }
        if (s.bestSellers.isEmpty()) {
            item { Text(stringResource(R.string.mgmt_no_sales), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(s.bestSellers, key = { it.productName }) { b ->
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(b.productName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text("${b.qty}", style = MaterialTheme.typography.bodyMedium)
                        Text("  ${formatRup(b.revenue)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item { TextButton(onClick = onBack) { Text(stringResource(R.string.mgmt_back)) } }
    }
}

@Composable
private fun MetricRow(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
    }
}

private fun formatRup(v: Long): String = formatRupiah(v)
