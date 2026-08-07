package com.ragnala.pos.ui.customer

import com.ragnala.pos.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ragnala.pos.domain.Money
import com.ragnala.pos.ui.customer.CartItem
import com.ragnala.pos.ui.customer.CartLineCard
import com.ragnala.pos.ui.customer.formatRupiah
import com.ragnala.pos.ui.customer.OrderConfirmViewModel

/**
 * Order confirmation â€” customer side.
 * Shows items, subtotal, SC, tax, total.
 * On submit goes to WAITING_PAYMENT â€” payment collected by barista at the counter.
 */
@Composable
fun OrderConfirmScreen(
    items: List<CartItem>,
    subtotal: Long,
    customerName: String,
    scPercent: Double,
    taxPercent: Double,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    result: OrderConfirmViewModel.Result? = null,
    submitting: Boolean = false,
) {
    // scPercent/taxPercent are percentage points (e.g. 5.0 = 5%, 11.0 = 11%) from SettingsService.
    // Must match OrderService.confirmOrder so the displayed total equals the stored order total.
    val scDec = scPercent / 100.0
    val taxDec = taxPercent / 100.0

    val scAmt = Money.roundHalfUp(java.math.BigDecimal(subtotal * scDec))
    val taxable = subtotal + scAmt
    val taxAmt = Money.roundHalfUp(java.math.BigDecimal(taxable * taxDec))
    val total = subtotal + scAmt + taxAmt

    Column(modifier = Modifier.fillMaxSize()) {
        // header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cust_back))
            }
            Text(
                text = stringResource(R.string.cust_confirm_order),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        // items list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items) { item ->
                CartLineCard(
                    item = item,
                    onDecrease = {},
                    onIncrease = {},
                    onRemove = {},
                )
            }
            item {
                // summary
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
                ) {
                    SummaryRow(stringResource(R.string.cust_subtotal_lbl), formatRupiah(subtotal))
                    SummaryRow(stringResource(R.string.cust_service_charge, scPercent.toInt()), formatRupiah(scAmt))
                    SummaryRow(stringResource(R.string.cust_tax, taxPercent.toInt()), formatRupiah(taxAmt))
                    SummaryRow(stringResource(R.string.cust_total), formatRupiah(total), bold = true)
                }
            }
        }

        // tender input + confirm
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (customerName.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.cust_order_for, customerName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.cust_payment_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )

                Button(
                    onClick = { onConfirm() },
                    enabled = !submitting && result !is OrderConfirmViewModel.Result.Success,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(top = 16.dp),
                ) {
                    Text(
                        if (submitting) stringResource(R.string.cust_submitting) else stringResource(R.string.cust_submit_order),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                // result handling
                when (result) {
                    is OrderConfirmViewModel.Result.Success -> {
                        // success toast handled externally - just clear cart via parent
                    }
                    is OrderConfirmViewModel.Result.Failure -> {
                        Text(
                            text = stringResource(R.string.cust_failed, result.message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    null -> {}
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, amount: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
        Text(
            amount,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            ),
        )
    }
}
