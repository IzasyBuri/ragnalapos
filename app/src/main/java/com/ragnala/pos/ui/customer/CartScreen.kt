package com.ragnala.pos.ui.customer

import com.ragnala.pos.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ragnala.pos.ui.components.RagnalaEmptyState
import com.ragnala.pos.ui.components.RagnalaMoneySize
import com.ragnala.pos.ui.components.RagnalaMoneyText
import com.ragnala.pos.ui.components.RagnalaPrimaryButton
import com.ragnala.pos.ui.theme.RagnalaRadius
import com.ragnala.pos.ui.theme.RagnalaSpacing

@Composable
fun CartScreen(
    items: List<CartItem>,
    subtotal: Long,
    onQuantityChange: (index: Int, quantity: Int) -> Unit,
    onRemove: (index: Int) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = RagnalaSpacing.xs, vertical = RagnalaSpacing.xxs), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cust_back))
            }
            Text("Your order", style = MaterialTheme.typography.headlineSmall)
        }
        if (items.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                RagnalaEmptyState(
                    title = "Your basket is empty",
                    description = "Choose something from the menu when you're ready.",
                    modifier = Modifier.fillMaxWidth(),
                    decoration = { Text("☕", style = MaterialTheme.typography.headlineMedium) },
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = RagnalaSpacing.md, vertical = RagnalaSpacing.xs), verticalArrangement = Arrangement.spacedBy(RagnalaSpacing.sm)) {
                itemsIndexed(items, key = { _, item -> item.productId + item.modifiers.toString() }) { index, item ->
                    CartLineCard(item, { onQuantityChange(index, item.quantity - 1) }, { onQuantityChange(index, item.quantity + 1) }, { onRemove(index) })
                }
                item {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(RagnalaRadius.button), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(RagnalaSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.cust_subtotal_label), style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.weight(1f))
                            RagnalaMoneyText(subtotal, size = RagnalaMoneySize.Medium)
                        }
                    }
                    Spacer(Modifier.height(RagnalaSpacing.md))
                }
            }
        }
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            RagnalaPrimaryButton(stringResource(R.string.cust_continue), onContinue, enabled = items.isNotEmpty(), modifier = Modifier.fillMaxWidth().padding(RagnalaSpacing.md).height(52.dp))
        }
    }
}

@Composable
internal fun CartLineCard(item: CartItem, onDecrease: () -> Unit, onIncrease: () -> Unit, onRemove: () -> Unit) {
    Surface(shape = RoundedCornerShape(RagnalaRadius.card), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(RagnalaSpacing.md)) {
            Row(verticalAlignment = Alignment.Top) {
                if (item.imagePath != null) AsyncImage(model = item.imagePath, contentDescription = item.productName, contentScale = ContentScale.Crop, modifier = Modifier.size(72.dp).clip(RoundedCornerShape(RagnalaRadius.productImage)))
                else Box(Modifier.size(72.dp).clip(RoundedCornerShape(RagnalaRadius.productImage)).padding(RagnalaSpacing.xs), contentAlignment = Alignment.Center) { Text(item.productName.take(1), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                Spacer(Modifier.width(RagnalaSpacing.sm))
                Column(Modifier.weight(1f)) {
                    Text(item.productName, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (item.modifiers.isNotEmpty()) Text(item.modifiers.joinToString(" · ") { it.optionName }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove ${item.productName}", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Row(Modifier.fillMaxWidth().padding(top = RagnalaSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                QuantityButton(Icons.Rounded.Remove, "Decrease ${item.productName}", onDecrease)
                Text(item.quantity.toString(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = RagnalaSpacing.sm))
                QuantityButton(Icons.Rounded.Add, "Increase ${item.productName}", onIncrease)
                Spacer(Modifier.weight(1f))
                RagnalaMoneyText(item.unitPrice * item.quantity, size = RagnalaMoneySize.Medium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun QuantityButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(48.dp).semantics { contentDescription = description }) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp)) }
    }
}
