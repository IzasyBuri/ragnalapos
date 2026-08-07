package com.ragnala.pos.ui.customer

import com.ragnala.pos.R

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ragnala.pos.data.db.ModifierGroupEntity
import com.ragnala.pos.data.db.ModifierOptionEntity
import com.ragnala.pos.data.db.ProductEntity

/**
 * Product detail â€” DESIGN.md: one product, clear choices, friendly wording.
 * Shows photo, description, modifier groups (required/optional, min/max
 * selections enforced), quantity stepper, and an add-to-cart bar with
 * running total. Big touch targets, no technical info.
 */
@Composable
fun ProductDetailScreen(
    product: ProductEntity?,
    groups: List<ModifierGroupEntity>,
    optionsByGroup: Map<String, List<ModifierOptionEntity>>,
    onAddToCart: (quantity: Int, modifiers: List<CartModifier>) -> Unit,
    onBack: () -> Unit,
) {
    if (product == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.cust_product_not_found), style = MaterialTheme.typography.titleLarge)
        }
        return
    }

    var quantity by remember { mutableStateOf(1) }
    // selections: groupId -> set of selected optionIds
    val selections = remember(product.id) { mutableStateOf(mapOf<String, Set<String>>()) }

    val selectedMap = selections.value
    val price = product.price + selectedMap.values.flatten()
        .mapNotNull { optionId ->
            optionsByGroup.values.flatten().firstOrNull { it.id == optionId }?.priceDelta
        }
        .sum()

    fun toggle(group: ModifierGroupEntity, optionId: String) {
        val current = selectedMap[group.id].orEmpty()
        val next = when {
            optionId in current -> current - optionId
            group.maxSelections > 0 && current.size >= group.maxSelections -> current
            else -> current + optionId
        }
        selections.value = selectedMap + (group.id to next)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.cust_back),
                )
            }
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, bottom = 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ProductImage(product = product, modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp))
            }
            if (product.description.isNotBlank()) {
                item {
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(groups.size) { index ->
                val group = groups[index]
                ModifierGroupSection(
                    group = group,
                    options = optionsByGroup[group.id].orEmpty(),
                    selectedOptionIds = selectedMap[group.id].orEmpty(),
                    onToggle = { toggle(group, it) },
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // bottom bar â€” quantity + add to cart
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuantityStepper(
                    quantity = quantity,
                    onQuantityChange = { quantity = it },
                )
                Spacer(modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatRupiah(price.toLong() * quantity),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                    val requiredGroups = groups.filter { it.required && it.minSelections > 0 }
                    val missingRequired = requiredGroups.any { group ->
                        (selectedMap[group.id]?.size ?: 0) < group.minSelections
                    }
                    Button(
                        onClick = {
                            onAddToCart(
                                quantity,
                                selectedMap.flatMap { (groupId, optionIds) ->
                                    optionIds.mapNotNull { optionId ->
                                        val group = groups.firstOrNull { it.id == groupId } ?: return@mapNotNull null
                                        val option = optionsByGroup[groupId].orEmpty().firstOrNull { it.id == optionId } ?: return@mapNotNull null
                                        CartModifier(group.name, option.name, option.priceDelta)
                                    }
                                },
                            )
                        },
                        enabled = !missingRequired,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(52.dp),
                    ) {
                        Text(stringResource(R.string.cust_add_to_cart), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModifierGroupSection(
    group: ModifierGroupEntity,
    options: List<ModifierOptionEntity>,
    selectedOptionIds: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val requiredMarker = stringResource(R.string.cust_required_marker)
        val chooseUpToFormat = stringResource(R.string.cust_choose_up_to)
        Text(
            text = buildString {
                append(group.name)
                if (group.required) append(requiredMarker)
                if (group.maxSelections > 0) append(" â€¢ ${chooseUpToFormat.format(group.maxSelections)}")
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        options.forEach { option ->
            val selected = option.id in selectedOptionIds
            val delta = option.priceDelta
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(option.id) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else Color.Transparent,
                            ),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = option.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (delta != 0L) {
                        Text(
                            text = if (delta > 0) "+${formatRupiah(delta)}" else formatRupiah(delta),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            onClick = { onQuantityChange((quantity - 1).coerceAtLeast(1)) },
            shape = CircleShape,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            modifier = Modifier.size(44.dp),
        ) {
            Text("âˆ’", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Button(
            onClick = { onQuantityChange(quantity + 1) },
            shape = CircleShape,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            modifier = Modifier.size(44.dp),
        ) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun ProductImage(product: ProductEntity, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    if (product.imagePath != null) {
        AsyncImage(
            model = product.imagePath,
            contentDescription = product.name,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape),
        )
    } else {
        Box(
            modifier = modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = product.name.take(1),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
