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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Cart â€” DESIGN.md: "see exactly what you're about to order".
 * Line items with modifiers, quantity steppers, remove; subtotal and a
 * continue-to-confirm button. Money via [formatRupiah] (integer rupiah).
 */
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cust_back))
            }
            Text(
                text = stringResource(R.string.cust_cart_title),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        if (items.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.cust_cart_empty),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items.size) { index ->
                    val item = items[index]
                    CartLineCard(
                        item = item,
                        onDecrease = { onQuantityChange(index, item.quantity - 1) },
                        onIncrease = { onQuantityChange(index, item.quantity + 1) },
                        onRemove = { onRemove(index) },
                    )
                }
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.cust_subtotal_label),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = formatRupiah(subtotal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
        ) {
            Button(
                onClick = onContinue,
                enabled = items.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
            ) {
                Text(stringResource(R.string.cust_continue), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
internal fun CartLineCard(
    item: CartItem,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium, // 20dp
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                if (item.imagePath != null) {
                    AsyncImage(
                        model = item.imagePath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp)),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.modifiers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        item.modifiers.forEach { mod ->
                            Text(
                                text = "${mod.groupName}: ${mod.optionName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                IconButton(onClick = onRemove) {
                    Text(
                        text = "âœ•",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onDecrease,
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(36.dp),
                ) {
                    Text("âˆ’", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text = item.quantity.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Button(
                    onClick = onIncrease,
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(36.dp),
                ) {
                    Text("+", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formatRupiah(item.unitPrice * item.quantity),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
