package com.ragnala.pos.ui.management

import com.ragnala.pos.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.ragnala.pos.ui.customer.formatRupiah

@Composable
fun ProductListScreen(
    rows: List<ProductListRow>,
    onBack: () -> Unit,
    onAddProduct: () -> Unit,
    onProductClick: (String) -> Unit = {},
    onToggleAvailability: (String, Boolean) -> Unit = { _, _ -> },
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.mgmt_back))
                }
                Text(
                    text = stringResource(R.string.mgmt_products_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                )
                Button(onClick = onAddProduct) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.mgmt_new_product))
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 900.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    ProductListSummary(rows = rows)
                }
                if (rows.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.mgmt_no_products),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        )
                    }
                } else {
                    items(rows, key = { it.product.id }) { row ->
                        ProductManagementRow(
                            row = row,
                            onClick = onProductClick,
                            onToggleAvailability = onToggleAvailability,
                        )
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun ProductListSummary(rows: List<ProductListRow>) {
    val unavailableCount = rows.count { !it.product.available }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 6.dp)) {
        Text(
            text = stringResource(R.string.mgmt_menu_products),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = buildString {
                append("${rows.size} ${stringResource(R.string.mgmt_product)}")
                if (unavailableCount > 0) append(" Â· $unavailableCount ${stringResource(R.string.mgmt_unavailable)}")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(modifier = Modifier.padding(top = 14.dp))
    }
}

@Composable
private fun ProductManagementRow(
    row: ProductListRow,
    onClick: (String) -> Unit = {},
    onToggleAvailability: (String, Boolean) -> Unit = { _, _ -> },
) {
    val product = row.product
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick(product.id) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProductThumbnail(row = row)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = row.categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatRupiah(product.price),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = product.available,
                onCheckedChange = { onToggleAvailability(product.id, product.available) },
            )
        }
    }
}

@Composable
private fun ProductThumbnail(row: ProductListRow) {
    val product = row.product
    val modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
    if (product.imagePath != null) {
        AsyncImage(
            model = product.imagePath,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = product.name.take(1),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun AvailabilityBadge(available: Boolean) {
    Surface(
        color = if (available) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        contentColor = if (available) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onErrorContainer
        },
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = if (available) stringResource(R.string.mgmt_available) else stringResource(R.string.mgmt_unavailable),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        )
    }
}
