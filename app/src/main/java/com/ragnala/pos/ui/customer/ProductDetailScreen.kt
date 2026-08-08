package com.ragnala.pos.ui.customer

import com.ragnala.pos.R

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ragnala.pos.data.db.ModifierGroupEntity
import com.ragnala.pos.data.db.ModifierOptionEntity
import com.ragnala.pos.data.db.ProductEntity
import com.ragnala.pos.ui.components.RagnalaMoneySize
import com.ragnala.pos.ui.components.RagnalaMoneyText
import com.ragnala.pos.ui.components.RagnalaPrimaryButton
import com.ragnala.pos.ui.components.RagnalaSectionHeader
import com.ragnala.pos.ui.theme.RagnalaRadius
import com.ragnala.pos.ui.theme.RagnalaSpacing

@Composable
fun ProductDetailScreen(
    product: ProductEntity?,
    groups: List<ModifierGroupEntity>,
    optionsByGroup: Map<String, List<ModifierOptionEntity>>,
    isInStock: Boolean,
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
    val selections = remember(product.id) { mutableStateOf(mapOf<String, Set<String>>()) }
    val selectedMap = selections.value
    val price = product.price + selectedMap.values.flatten()
        .mapNotNull { id -> optionsByGroup.values.flatten().firstOrNull { it.id == id }?.priceDelta }
        .sum()
    val missingRequired = groups.any { group ->
        group.required && (selectedMap[group.id]?.size ?: 0) < group.minSelections
    }
    val canAdd = isInStock && !missingRequired
    val selectedModifiers = selectedMap.flatMap { (groupId, optionIds) ->
        optionIds.mapNotNull { optionId ->
            val group = groups.firstOrNull { it.id == groupId } ?: return@mapNotNull null
            val option = optionsByGroup[groupId].orEmpty().firstOrNull { it.id == optionId } ?: return@mapNotNull null
            CartModifier(group.name, option.name, option.priceDelta)
        }
    }

    fun toggle(group: ModifierGroupEntity, optionId: String) {
        val current = selectedMap[group.id].orEmpty()
        val next = when {
            optionId in current -> current - optionId
            group.maxSelections == 1 -> setOf(optionId)
            group.maxSelections > 0 && current.size >= group.maxSelections -> current
            else -> current + optionId
        }
        selections.value = selectedMap + (group.id to next)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = RagnalaSpacing.xs, vertical = RagnalaSpacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cust_back))
            }
            Text(product.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val tablet = maxWidth >= 840.dp
            if (tablet) {
                Row(modifier = Modifier.fillMaxSize()) {
                    ProductHero(product = product, modifier = Modifier.weight(0.43f).padding(RagnalaSpacing.lg))
                    ModifierContent(
                        groups = groups,
                        optionsByGroup = optionsByGroup,
                        selectedMap = selectedMap,
                        onToggle = ::toggle,
                        modifier = Modifier.weight(0.57f).fillMaxSize(),
                    )
                }
            } else {
                ModifierContent(
                    groups = groups,
                    optionsByGroup = optionsByGroup,
                    selectedMap = selectedMap,
                    onToggle = ::toggle,
                    header = { ProductHero(product) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(RagnalaSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuantityStepper(quantity = quantity, onQuantityChange = { quantity = it })
                Spacer(modifier = Modifier.width(RagnalaSpacing.md))
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    RagnalaMoneyText(amount = price.toLong() * quantity, size = RagnalaMoneySize.Medium, color = MaterialTheme.colorScheme.primary)
                    RagnalaPrimaryButton(
                        text = if (!isInStock) stringResource(R.string.cust_sold_out) else "Add $quantity · ${formatRupiah(price.toLong() * quantity)}",
                        onClick = { onAddToCart(quantity, selectedModifiers) },
                        enabled = canAdd,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductHero(product: ProductEntity, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(RagnalaSpacing.md)) {
        ProductImage(product, Modifier.fillMaxWidth().heightIn(min = 240.dp, max = 360.dp))
        Column(verticalArrangement = Arrangement.spacedBy(RagnalaSpacing.xs)) {
            Text(product.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            if (product.description.isNotBlank()) {
                Text(product.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            RagnalaMoneyText(product.price, size = RagnalaMoneySize.Large, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ModifierContent(
    groups: List<ModifierGroupEntity>,
    optionsByGroup: Map<String, List<ModifierOptionEntity>>,
    selectedMap: Map<String, Set<String>>,
    onToggle: (ModifierGroupEntity, String) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = RagnalaSpacing.lg, vertical = RagnalaSpacing.md),
        verticalArrangement = Arrangement.spacedBy(RagnalaSpacing.lg),
    ) {
        header?.let { item { it() } }
        if (header == null) {
            item { Text("Customize your drink", style = MaterialTheme.typography.headlineSmall) }
        }
        items(groups, key = { it.id }) { group ->
            ModifierGroupSection(
                group = group,
                options = optionsByGroup[group.id].orEmpty(),
                selectedOptionIds = selectedMap[group.id].orEmpty(),
                onToggle = { onToggle(group, it) },
            )
        }
        item { Spacer(modifier = Modifier.height(RagnalaSpacing.xxl)) }
    }
}

@Composable
private fun ModifierGroupSection(
    group: ModifierGroupEntity,
    options: List<ModifierOptionEntity>,
    selectedOptionIds: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RagnalaSpacing.xs)) {
        val rule = when {
            group.maxSelections == 1 && group.required -> "Choose 1 · Required"
            group.maxSelections == 1 -> "Choose up to 1"
            group.required && group.maxSelections > 0 -> "Choose at least ${group.minSelections} · Up to ${group.maxSelections}"
            group.maxSelections > 0 -> "Choose up to ${group.maxSelections}"
            group.required -> "Required"
            else -> "Optional"
        }
        RagnalaSectionHeader(title = group.name, subtitle = rule)
        options.forEach { option ->
            ModifierOptionRow(option, selectedOptionIds.contains(option.id), group.maxSelections == 1, onToggle)
        }
    }
}

@Composable
private fun ModifierOptionRow(
    option: ModifierOptionEntity,
    selected: Boolean,
    singleChoice: Boolean,
    onToggle: (String) -> Unit,
) {
    val shape = RoundedCornerShape(RagnalaRadius.button)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .semantics { this.selected = selected; role = if (singleChoice) Role.RadioButton else Role.Checkbox }
            .clickable(onClick = { onToggle(option.id) }),
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = RagnalaSpacing.sm, vertical = RagnalaSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
            if (singleChoice) {
                RadioButton(selected = selected, onClick = { onToggle(option.id) }, modifier = Modifier.size(48.dp))
            } else {
                Checkbox(checked = selected, onCheckedChange = { onToggle(option.id) }, modifier = Modifier.size(48.dp))
            }
            Text(option.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (option.priceDelta != 0L) {
                Text(if (option.priceDelta > 0) "+${formatRupiah(option.priceDelta)}" else formatRupiah(option.priceDelta), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun QuantityStepper(quantity: Int, onQuantityChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        QuantityButton(Icons.Rounded.Remove, "Decrease quantity") { onQuantityChange((quantity - 1).coerceAtLeast(1)) }
        Text(quantity.toString(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = RagnalaSpacing.sm))
        QuantityButton(Icons.Rounded.Add, "Increase quantity") { onQuantityChange(quantity + 1) }
    }
}

@Composable
private fun QuantityButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(48.dp).semantics { contentDescription = description }) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp)) }
    }
}

@Composable
private fun ProductImage(product: ProductEntity, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(RagnalaRadius.productImage)
    if (product.imagePath != null) {
        AsyncImage(model = product.imagePath, contentDescription = product.name, contentScale = ContentScale.Crop, modifier = modifier.clip(shape))
    } else {
        Box(modifier = modifier.clip(shape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Text(product.name.take(1), style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
