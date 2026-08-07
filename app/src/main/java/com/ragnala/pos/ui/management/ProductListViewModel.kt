package com.ragnala.pos.ui.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ragnala.pos.data.db.CategoryEntity
import com.ragnala.pos.data.db.ProductEntity
import com.ragnala.pos.data.repo.CatalogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProductListRow(
    val product: ProductEntity,
    val categoryName: String,
)

/** Owner-facing live catalog list. Editing is intentionally deferred to the next slice. */
class ProductListViewModel(private val catalog: CatalogRepository) : ViewModel() {

    val rows: StateFlow<List<ProductListRow>> = combine(
        catalog.products(),
        catalog.categories(),
        ::buildProductListRows,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    class Factory(private val catalog: CatalogRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProductListViewModel(catalog) as T
    }

    /** Toggle a product's availability directly from the list. */
    fun toggleAvailability(productId: String, currentlyAvailable: Boolean) {
        viewModelScope.launch {
            catalog.setProductAvailability(productId, !currentlyAvailable)
        }
    }
}

internal fun buildProductListRows(
    products: List<ProductEntity>,
    categories: List<CategoryEntity>,
): List<ProductListRow> {
    val categoryById = categories.associateBy { it.id }
    return products
        .map { product ->
            ProductListRow(
                product = product,
                categoryName = categoryById[product.categoryId]?.name ?: "Uncategorized",
            )
        }
        .sortedWith(
            compareBy<ProductListRow> {
                categoryById[it.product.categoryId]?.position ?: Int.MAX_VALUE
            }.thenBy { it.product.name.lowercase() },
        )
}
