package com.ragnala.pos.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ragnala.pos.data.db.CategoryEntity
import com.ragnala.pos.data.db.ModifierGroupEntity
import com.ragnala.pos.data.db.ProductEntity
import com.ragnala.pos.data.repo.CatalogRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Customer Mode browse: categories list + products of selected category.
 * DESIGN.md §Customer Mode — large photos, large buttons, minimal text.
 * Data: catalog only, live from Room (Flow). No technical info shown.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModel(
    private val catalog: CatalogRepository,
) : ViewModel() {

    private val selectedCategoryId = MutableStateFlow<String?>(null)

    /** Exposed for the screen to read; selection via [selectCategory]. */
    val selectedCategory: StateFlow<String?> = selectedCategoryId

    val categories: StateFlow<List<CategoryEntity>> = catalog.categories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val products: StateFlow<List<ProductEntity>> = selectedCategoryId
        .flatMapLatest { id ->
            if (id == null) catalog.products()
            else catalog.availableProducts(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Products that can safely enter the basket without choosing required modifiers. */
    val quickAddEligibleProductIds: StateFlow<Set<String>> = products
        .flatMapLatest { visibleProducts ->
            val eligibilityFlows = visibleProducts.map { product ->
                catalog.modifierGroups(product.id).map { groups ->
                    product.id to isQuickAddEligible(groups)
                }
            }
            if (eligibilityFlows.isEmpty()) flowOf(emptySet())
            else combine(eligibilityFlows) { eligibility ->
                eligibility
                    .filter { (_, eligible) -> eligible }
                    .mapTo(mutableSetOf()) { (productId, _) -> productId }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun selectCategory(id: String?) {
        selectedCategoryId.value = id
    }

    class Factory(
        private val catalog: CatalogRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BrowseViewModel(catalog) as T
    }
}

internal fun isQuickAddEligible(groups: List<ModifierGroupEntity>): Boolean =
    groups.none { it.required && it.minSelections > 0 }
