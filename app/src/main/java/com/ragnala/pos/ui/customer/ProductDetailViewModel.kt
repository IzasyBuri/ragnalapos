package com.ragnala.pos.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ragnala.pos.data.db.ModifierGroupEntity
import com.ragnala.pos.data.db.ModifierOptionEntity
import com.ragnala.pos.data.db.ProductEntity
import com.ragnala.pos.data.repo.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Detail screen data: the product plus its modifier groups and options,
 * loaded live from Room. Selection state lives in the screen (ProductDetailScreen);
 * this VM only sources data. */
class ProductDetailViewModel(
    private val catalog: CatalogRepository,
    productId: String,
) : ViewModel() {

    val product: StateFlow<ProductEntity?> = flowOf(productId)
        .flatMapLatest { id -> flowOf(catalog.product(id)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** True if product has a recipe and all ingredients have enough stock for 1 unit. Products without recipes are always in stock. */
    val isInStock: StateFlow<Boolean> = flowOf(productId)
        .flatMapLatest { id -> flowOf(catalog.isProductInStock(id)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val groups: StateFlow<List<ModifierGroupEntity>> = flowOf(productId)
        .flatMapLatest { id -> catalog.modifierGroups(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** groupId -> options, loaded once when groups appear. */
    val optionsByGroup: StateFlow<Map<String, List<ModifierOptionEntity>>> =
        groups.flatMapLatest { groupList ->
            val flows = groupList.map { group ->
                catalog.modifierOptions(group.id).map { options -> group.id to options }
            }
            if (flows.isEmpty()) flowOf(emptyMap())
            else combine(flows) { pairs -> pairs.toMap() }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    class Factory(
        private val catalog: CatalogRepository,
        private val productId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProductDetailViewModel(catalog, productId) as T
    }
}
