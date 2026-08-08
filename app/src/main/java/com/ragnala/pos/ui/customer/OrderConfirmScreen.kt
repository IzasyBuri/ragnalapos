package com.ragnala.pos.ui.customer

import com.ragnala.pos.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ragnala.pos.domain.LineItem
import com.ragnala.pos.domain.Pricing
import com.ragnala.pos.ui.components.RagnalaMoneySize
import com.ragnala.pos.ui.components.RagnalaMoneyText
import com.ragnala.pos.ui.components.RagnalaPrimaryButton

@Composable
fun OrderConfirmScreen(
    orderItems: List<CartItem>, customerName: String, scPercent: Double, taxPercent: Double,
    onBack: () -> Unit, onConfirm: () -> Unit,
    result: OrderConfirmViewModel.Result? = null, submitting: Boolean = false,
) {
    val totals = Pricing.calculate(orderItems.map { LineItem(it.unitPrice, it.quantity) }, scPercent, taxPercent)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.cust_back)) }
            Text("Review your order", style = MaterialTheme.typography.headlineSmall)
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)) {
            if (customerName.isNotBlank()) item { Text("Order for $customerName", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { Text("Order items", style = MaterialTheme.typography.titleLarge) }
            items(orderItems) { orderItem -> OrderLineSummary(orderItem) }
            item {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryRow("Subtotal", totals.subtotal)
                        SummaryRow("Service charge", totals.serviceCharge)
                        SummaryRow("Tax", totals.tax)
                        Spacer(Modifier.height(4.dp))
                        SummaryRow("TOTAL", totals.total, true)
                    }
                }
            }
        }
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.cust_payment_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (result is OrderConfirmViewModel.Result.Failure) Text(stringResource(R.string.cust_failed, result.message), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                RagnalaPrimaryButton(if (submitting) stringResource(R.string.cust_submitting) else "Place Order", onConfirm, enabled = !submitting && result !is OrderConfirmViewModel.Result.Success, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(top = 12.dp))
            }
        }
    }
}

@Composable
private fun OrderLineSummary(item: CartItem) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${item.quantity}× ${item.productName}", style = MaterialTheme.typography.titleMedium)
                if (item.modifiers.isNotEmpty()) Text(item.modifiers.joinToString(" · ") { it.optionName }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RagnalaMoneyText(item.unitPrice * item.quantity, size = RagnalaMoneySize.Medium)
        }
    }
}

@Composable
private fun SummaryRow(label: String, amount: Long, strong: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge, fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal)
        Spacer(Modifier.weight(1f))
        RagnalaMoneyText(amount, size = if (strong) RagnalaMoneySize.Large else RagnalaMoneySize.Small, color = if (strong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}
