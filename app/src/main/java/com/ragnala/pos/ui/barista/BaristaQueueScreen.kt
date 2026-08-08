package com.ragnala.pos.ui.barista

import com.ragnala.pos.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ragnala.pos.data.db.OrderEntity
import com.ragnala.pos.domain.OrderStatus
import com.ragnala.pos.ui.components.RagnalaEmptyState
import com.ragnala.pos.ui.components.RagnalaMoneySize
import com.ragnala.pos.ui.components.RagnalaMoneyText
import com.ragnala.pos.ui.components.RagnalaSecondaryButton
import com.ragnala.pos.ui.components.RagnalaSectionHeader
import com.ragnala.pos.ui.components.RagnalaStatusBadge
import com.ragnala.pos.ui.components.RagnalaBadgeTone
import com.ragnala.pos.ui.theme.RagnalaRadius
import com.ragnala.pos.ui.theme.RagnalaSpacing

@Composable
fun BaristaQueueScreen(viewModel: BaristaQueueViewModel, onOrderClick: (OrderEntity) -> Unit = {}, onManageMenu: () -> Unit = {}) {
    val orders by viewModel.activeOrders.collectAsState()
    Column(Modifier.fillMaxSize().padding(horizontal = RagnalaSpacing.md)) {
        RagnalaSectionHeader(
            title = "Orders",
            subtitle = "Active orders",
            modifier = Modifier.padding(vertical = RagnalaSpacing.md),
            trailing = { RagnalaSecondaryButton("Manage menu", onManageMenu) },
        )
        if (orders.isEmpty()) {
            RagnalaEmptyState("All caught up", "No active orders right now.", Modifier.fillMaxSize())
        } else {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                if (maxWidth >= 600.dp) {
                    LazyVerticalGrid(GridCells.Adaptive(minSize = 290.dp), contentPadding = PaddingValues(bottom = RagnalaSpacing.lg), horizontalArrangement = Arrangement.spacedBy(RagnalaSpacing.md), verticalArrangement = Arrangement.spacedBy(RagnalaSpacing.md)) {
                        items(orders, key = { it.id }) { order -> OrderQueueCard(order, { onOrderClick(order) }) }
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = RagnalaSpacing.lg), verticalArrangement = Arrangement.spacedBy(RagnalaSpacing.sm)) {
                        items(orders, key = { it.id }) { order -> OrderQueueCard(order, { onOrderClick(order) }) }
                    }
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
    androidx.compose.material3.Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface, shape = androidx.compose.foundation.shape.RoundedCornerShape(RagnalaRadius.card), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(RagnalaSpacing.md), verticalArrangement = Arrangement.spacedBy(RagnalaSpacing.sm)) {
            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("#${order.orderNumber.toString().padStart(3, '0')}", style = MaterialTheme.typography.headlineSmall)
                RagnalaStatusBadge(orderStatusLabel(order.status), order.status.tone())
            }
            Text(order.customerName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.barista_customer), style = MaterialTheme.typography.titleMedium)
            RagnalaMoneyText(order.total, size = RagnalaMoneySize.Medium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun OrderStatus.tone(): RagnalaBadgeTone = when (this) {
    OrderStatus.WAITING_PAYMENT -> RagnalaBadgeTone.Warning
    OrderStatus.PAID -> RagnalaBadgeTone.Success
    OrderStatus.FULFILLED -> RagnalaBadgeTone.Neutral
    OrderStatus.CANCELLED, OrderStatus.VOIDED -> RagnalaBadgeTone.Error
    else -> RagnalaBadgeTone.Neutral
}
