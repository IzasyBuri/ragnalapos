package com.ragnala.pos.ui.barista

import com.ragnala.pos.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.ragnala.pos.data.db.OrderEntity
import com.ragnala.pos.domain.OrderStatus
import com.ragnala.pos.ui.customer.formatRupiah
import com.ragnala.pos.ui.theme.SteamWisp

/**
 * Barista Mode â€” live queue of active orders (waiting payment â†’ paid â†’ preparing â†’ ready).
 * Tap a card to open detail and act on it.
 */
@Composable
fun BaristaQueueScreen(
    viewModel: BaristaQueueViewModel,
    onOrderClick: (OrderEntity) -> Unit = {},
    onManageMenu: () -> Unit = {},
) {
    val orders by viewModel.activeOrders.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.barista_mode),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = stringResource(R.string.barista_active_orders),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(onClick = onManageMenu) {
                        Text(stringResource(R.string.mgmt_manage_menu))
                    }
                }
            }
        }

        if (orders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.barista_no_orders_yet),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(orders, key = { it.id }) { order ->
                    OrderQueueCard(order = order, onClick = { onOrderClick(order) })
                }
            }
        }
    }
}

@Composable
internal fun orderStatusLabel(status: OrderStatus): String = when (status) {
    OrderStatus.WAITING_PAYMENT -> stringResource(R.string.barista_status_waiting)
    OrderStatus.PAID -> stringResource(R.string.barista_status_paid)
    OrderStatus.FULFILLED -> stringResource(R.string.barista_status_fulfilled)
    else -> status.name
}

@Composable
private fun OrderQueueCard(order: OrderEntity, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val label = order.customerName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.barista_customer)
                Text(
                    text = "#${order.orderNumber} Â· $label",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = orderStatusLabel(order.status),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = formatRupiah(order.total),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.SemiBold,
            )
            if (order.status != OrderStatus.WAITING_PAYMENT) {
                SteamWisp(modifier = Modifier.size(28.dp, 40.dp))
            }
        }
    }
}
