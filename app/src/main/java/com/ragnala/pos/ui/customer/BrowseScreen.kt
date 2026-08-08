package com.ragnala.pos.ui.customer

import com.ragnala.pos.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ragnala.pos.data.db.CategoryEntity
import com.ragnala.pos.data.db.ProductEntity
import com.ragnala.pos.ui.components.RagnalaEmptyState
import com.ragnala.pos.ui.components.RagnalaMoneySize
import com.ragnala.pos.ui.components.RagnalaMoneyText
import com.ragnala.pos.ui.theme.RagnalaRadius
import com.ragnala.pos.ui.theme.RagnalaSpacing
import com.ragnala.pos.ui.theme.RagnalaTheme

/**
 * Customer Mode browse â€” DESIGN.md: "browsing a beautiful cafÃ© menu".
 * Large photos, large touch targets, minimal text, friendly wording.
 * Tablets get a two-pane layout (categories rail + product grid); phones
 * get category chips + vertical list.
 */
@Composable
fun BrowseScreen(
    categories: List<CategoryEntity>,
    products: List<ProductEntity>,
    selectedCategoryId: String?,
    quickAddEligibleProductIds: Set<String> = emptySet(),
    baseProductQuantities: Map<String, Int> = emptyMap(),
    onCategorySelected: (String?) -> Unit,
    onProductClick: (ProductEntity) -> Unit,
    onQuickAdd: (ProductEntity) -> Unit = {},
    onQuickRemove: (ProductEntity) -> Unit = {},
    onCartClick: () -> Unit = {},
    cartCount: Int = 0,
    cartSubtotal: Long = 0L,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 840.dp
        if (isTablet) {
            TabletBrowse(
                categories = categories,
                products = products,
                selectedCategoryId = selectedCategoryId,
                quickAddEligibleProductIds = quickAddEligibleProductIds,
                baseProductQuantities = baseProductQuantities,
                onCategorySelected = onCategorySelected,
                onProductClick = onProductClick,
                onQuickAdd = onQuickAdd,
                onQuickRemove = onQuickRemove,
                onCartClick = onCartClick,
                cartCount = cartCount,
                cartSubtotal = cartSubtotal,
            )
        } else {
            PhoneBrowse(
                categories = categories,
                products = products,
                selectedCategoryId = selectedCategoryId,
                quickAddEligibleProductIds = quickAddEligibleProductIds,
                baseProductQuantities = baseProductQuantities,
                onCategorySelected = onCategorySelected,
                onProductClick = onProductClick,
                onQuickAdd = onQuickAdd,
                onQuickRemove = onQuickRemove,
                onCartClick = onCartClick,
                cartCount = cartCount,
                cartSubtotal = cartSubtotal,
            )
        }
    }
}

@Composable
private fun PhoneBrowse(
    categories: List<CategoryEntity>,
    products: List<ProductEntity>,
    selectedCategoryId: String?,
    quickAddEligibleProductIds: Set<String>,
    baseProductQuantities: Map<String, Int>,
    onCategorySelected: (String?) -> Unit,
    onProductClick: (ProductEntity) -> Unit,
    onQuickAdd: (ProductEntity) -> Unit,
    onQuickRemove: (ProductEntity) -> Unit,
    onCartClick: () -> Unit,
    cartCount: Int = 0,
    cartSubtotal: Long = 0L,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CategoryChips(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = onCategorySelected,
        )
        ProductList(
            products = products,
            onProductClick = onProductClick,
            quickAddEligibleProductIds = quickAddEligibleProductIds,
            baseProductQuantities = baseProductQuantities,
            onQuickAdd = onQuickAdd,
            onQuickRemove = onQuickRemove,
            modifier = Modifier.weight(1f),
        )
        if (cartCount > 0) {
            CartBar(
                cartCount = cartCount,
                cartSubtotal = cartSubtotal,
                onClick = onCartClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CartBar(
    cartCount: Int,
    cartSubtotal: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(RagnalaRadius.card),
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        modifier = modifier
            .padding(horizontal = RagnalaSpacing.md, vertical = RagnalaSpacing.xs)
            .heightIn(min = 68.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RagnalaSpacing.md, vertical = RagnalaSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "🛒", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.width(RagnalaSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cust_cart_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.cust_cart_item_count, cartCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                )
            }
            if (cartSubtotal > 0L) {
                RagnalaMoneyText(
                    amount = cartSubtotal,
                    size = RagnalaMoneySize.Medium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(modifier = Modifier.width(RagnalaSpacing.xs))
            }
            Text("→", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun TabletBrowse(
    categories: List<CategoryEntity>,
    products: List<ProductEntity>,
    selectedCategoryId: String?,
    quickAddEligibleProductIds: Set<String>,
    baseProductQuantities: Map<String, Int>,
    onCategorySelected: (String?) -> Unit,
    onProductClick: (ProductEntity) -> Unit,
    onQuickAdd: (ProductEntity) -> Unit,
    onQuickRemove: (ProductEntity) -> Unit,
    onCartClick: () -> Unit,
    cartCount: Int,
    cartSubtotal: Long,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // left rail â€” category list
        LazyColumn(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                CategoryChip(
                    name = stringResource(R.string.cust_all),
                    selected = selectedCategoryId == null,
                    onClick = { onCategorySelected(null) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            items(categories, key = { it.id }) { category ->
                CategoryChip(
                    name = category.name,
                    selected = selectedCategoryId == category.id,
                    onClick = { onCategorySelected(category.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        // right pane â€” product grid
        Column(modifier = Modifier.weight(1f)) {
            ProductGrid(
                products = products,
                onProductClick = onProductClick,
                quickAddEligibleProductIds = quickAddEligibleProductIds,
                baseProductQuantities = baseProductQuantities,
                onQuickAdd = onQuickAdd,
                onQuickRemove = onQuickRemove,
                modifier = Modifier.weight(1f),
            )
            if (cartCount > 0) {
                CartBar(
                    cartCount = cartCount,
                    cartSubtotal = cartSubtotal,
                    onClick = onCartClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CategoryChips(
    categories: List<CategoryEntity>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            CategoryChip(
                name = stringResource(R.string.cust_all),
                selected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) },
            )
        }
        items(categories, key = { it.id }) { category ->
            CategoryChip(
                name = category.name,
                selected = selectedCategoryId == category.id,
                onClick = { onCategorySelected(category.id) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val signShape = RoundedCornerShape(RagnalaRadius.smallControl)
    val signColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val signTextColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val borderColor = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline

    Surface(
        shape = signShape,
        color = signColor,
        contentColor = signTextColor,
        tonalElevation = if (selected) 1.dp else 0.dp,
        modifier = modifier
            .widthIn(min = 88.dp)
            .heightIn(min = 48.dp)
            .border(1.dp, borderColor, signShape)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 11.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                color = signTextColor,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProductList(
    products: List<ProductEntity>,
    onProductClick: (ProductEntity) -> Unit,
    quickAddEligibleProductIds: Set<String>,
    baseProductQuantities: Map<String, Int>,
    onQuickAdd: (ProductEntity) -> Unit,
    onQuickRemove: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (products.isEmpty()) {
        EmptyMenu(modifier = modifier)
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(products, key = { it.id }) { product ->
            val canQuickAdd = product.id in quickAddEligibleProductIds
            ProductRow(
                product = product,
                onClick = { onProductClick(product) },
                canQuickAdd = canQuickAdd,
                quantity = baseProductQuantities[product.id] ?: 0,
                onQuickAdd = {
                    if (canQuickAdd) onQuickAdd(product) else onProductClick(product)
                },
                onQuickRemove = { onQuickRemove(product) },
            )
        }
    }
}

@Composable
private fun ProductGrid(
    products: List<ProductEntity>,
    onProductClick: (ProductEntity) -> Unit,
    quickAddEligibleProductIds: Set<String>,
    baseProductQuantities: Map<String, Int>,
    onQuickAdd: (ProductEntity) -> Unit,
    onQuickRemove: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (products.isEmpty()) {
        EmptyMenu(modifier = modifier)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 220.dp),
        modifier = modifier,
        contentPadding = PaddingValues(RagnalaSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(RagnalaSpacing.md),
        verticalArrangement = Arrangement.spacedBy(RagnalaSpacing.md),
    ) {
        items(products, key = { it.id }) { product ->
            val canQuickAdd = product.id in quickAddEligibleProductIds
            ProductCard(
                product = product,
                onClick = { onProductClick(product) },
                canQuickAdd = canQuickAdd,
                quantity = baseProductQuantities[product.id] ?: 0,
                onQuickAdd = {
                    if (canQuickAdd) onQuickAdd(product) else onProductClick(product)
                },
                onQuickRemove = { onQuickRemove(product) },
            )
        }
    }
}

@Composable
private fun ProductRow(
    product: ProductEntity,
    onClick: () -> Unit,
    canQuickAdd: Boolean,
    quantity: Int,
    onQuickAdd: () -> Unit,
    onQuickRemove: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(RagnalaRadius.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProductImage(product = product, size = 112.dp)
            Spacer(modifier = Modifier.width(RagnalaSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (product.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RagnalaMoneyText(
                        amount = product.price,
                        size = RagnalaMoneySize.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(RagnalaSpacing.xs))
                    QuickAddControl(
                        productName = product.name,
                        canQuickAdd = canQuickAdd,
                        quantity = quantity,
                        onAdd = onQuickAdd,
                        onRemove = onQuickRemove,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: ProductEntity,
    onClick: () -> Unit,
    canQuickAdd: Boolean,
    quantity: Int,
    onQuickAdd: () -> Unit,
    onQuickRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(RagnalaRadius.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier,
    ) {
        Column {
            ProductImage(product = product, size = 176.dp, fullWidth = true)
            Column(modifier = Modifier.padding(RagnalaSpacing.md)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (product.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(RagnalaSpacing.xs))
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(RagnalaSpacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RagnalaMoneyText(
                        amount = product.price,
                        size = RagnalaMoneySize.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    QuickAddControl(
                        productName = product.name,
                        canQuickAdd = canQuickAdd,
                        quantity = quantity,
                        onAdd = onQuickAdd,
                        onRemove = onQuickRemove,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAddControl(
    productName: String,
    canQuickAdd: Boolean,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
) {
    if (canQuickAdd && quantity > 0) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            shadowElevation = 2.dp,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                QuantityControlButton(
                    label = "âˆ’",
                    description = stringResource(R.string.cust_remove_one, productName),
                    onClick = onRemove,
                )
                Text(
                    text = quantity.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 32.dp),
                )
                QuantityControlButton(
                    label = "+",
                    description = stringResource(R.string.cust_add_another, productName),
                    onClick = onAdd,
                )
            }
        }
    } else {
        QuantityControlButton(
            label = "+",
            description = if (canQuickAdd) {
                stringResource(R.string.cust_add_to_basket, productName)
            } else {
                stringResource(R.string.cust_choose_options, productName)
            },
            onClick = onAdd,
        )
    }
}

@Composable
private fun QuantityControlButton(
    label: String,
    description: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ProductImage(
    product: ProductEntity,
    size: androidx.compose.ui.unit.Dp,
    fullWidth: Boolean = false,
) {
    val imageModifier = if (fullWidth) {
        Modifier
            .fillMaxWidth()
            .height(size)
    } else {
        Modifier.size(size)
    }
    val shape = RoundedCornerShape(16.dp)
    if (product.imagePath != null) {
        AsyncImage(
            model = product.imagePath,
            contentDescription = product.name,
            contentScale = ContentScale.Crop,
            modifier = imageModifier.clip(shape),
        )
    } else {
        // placeholder â€” warm leaf-green block, DESIGN.md: no synthetic look
        Box(
            modifier = imageModifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = product.name.take(1),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun EmptyMenu(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        RagnalaEmptyState(
            title = "Nothing here yet",
            description = "This part of the menu is still being prepared.",
            modifier = Modifier.fillMaxWidth(),
            decoration = {
                Text("☕", style = MaterialTheme.typography.headlineMedium)
            },
        )
    }
}

// ---- previews ----
@Preview(showBackground = true, widthDp = 400)
@Composable
private fun BrowsePhonePreview() {
    RagnalaTheme {
        BrowseScreen(
            categories = previewCategories,
            products = previewProducts,
            selectedCategoryId = null,
            onCategorySelected = {},
            onProductClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 1200, heightDp = 800)
@Composable
private fun BrowseTabletPreview() {
    RagnalaTheme {
        BrowseScreen(
            categories = previewCategories,
            products = previewProducts,
            selectedCategoryId = previewCategories[0].id,
            onCategorySelected = {},
            onProductClick = {},
        )
    }
}

private val previewCategories = listOf(
    CategoryEntity("c1", "Coffee", 1, 0, 0),
    CategoryEntity("c2", "Tea & Botanee", 2, 0, 0),
    CategoryEntity("c3", "Pastry", 3, 0, 0),
)

private val previewProducts = listOf(
    ProductEntity("p1", "c1", "House Latte", "Silky steamed milk over a double espresso.", 42000, null, true, 0, 0),
    ProductEntity("p2", "c1", "Cortado", "Equal parts espresso and warm milk.", 35000, null, true, 0, 0),
    ProductEntity("p3", "c1", "V60 Pour Over", "Single-origin beans, slow brewed.", 48000, null, true, 0, 0),
    ProductEntity("p4", "c2", "Botanee Iced Tea", "House-brewed, lightly sweetened.", 28000, null, true, 0, 0),
    ProductEntity("p5", "c3", "Butter Croissant", "Flaky, hand-laminated.", 25000, null, true, 0, 0),
)
