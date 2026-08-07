package com.ragnala.pos.ui.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ragnala.pos.data.db.CategoryEntity
import com.ragnala.pos.data.db.ModifierGroupEntity
import com.ragnala.pos.data.db.ProductEntity
import com.ragnala.pos.data.repo.CatalogRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProductEditorState(
    val editingId: String? = null,
    val name: String = "",
    val description: String = "",
    val priceInput: String = "",
    val categoryId: String? = null,
    val imagePath: String? = null,
    val available: Boolean = true,
    val selectedGroupIds: Set<String> = emptySet(),
    val saving: Boolean = false,
    val error: String? = null,
    val savedProductId: String? = null,
)

data class ValidProductInput(
    val name: String,
    val description: String,
    val price: Long,
    val categoryId: String,
    val available: Boolean,
)

/** Create / edit product state and persistence. */
class ProductEditorViewModel(private val catalog: CatalogRepository) : ViewModel() {
    val categories: StateFlow<List<CategoryEntity>> = catalog.categories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** All modifier groups in the catalog, for the owner to assign to this product. */
    val allGroups: StateFlow<List<ModifierGroupEntity>> = catalog.allModifierGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _state = MutableStateFlow(ProductEditorState())
    val state: StateFlow<ProductEditorState> = _state.asStateFlow()

    fun setName(value: String) = update { copy(name = value, error = null) }
    fun setDescription(value: String) = update { copy(description = value, error = null) }
    fun setPrice(value: String) = update { copy(priceInput = value, error = null) }
    fun setCategory(categoryId: String) = update { copy(categoryId = categoryId, error = null) }
    fun setAvailable(value: Boolean) = update { copy(available = value) }
    fun setImagePath(value: String?) = update { copy(imagePath = value, error = null) }

    /** Toggle a modifier group in/out of this product's assignment. */
    fun toggleGroup(groupId: String) = update {
        copy(
            selectedGroupIds = if (groupId in selectedGroupIds) {
                selectedGroupIds - groupId
            } else {
                selectedGroupIds + groupId
            },
        )
    }

    /** Load an existing product into the form for editing. */
    fun loadForEdit(productId: String) {
        viewModelScope.launch {
            val product = catalog.product(productId) ?: return@launch
            val cats = categories.value
            val cat = if (cats.any { it.id == product.categoryId }) product.categoryId else null
            val assigned = catalog.modifierGroups(productId).first()
                .map { it.id }
                .toSet()
            _state.value = ProductEditorState(
                editingId = product.id,
                name = product.name,
                description = product.description,
                priceInput = formatPriceInput(product.price),
                categoryId = cat,
                imagePath = product.imagePath,
                available = product.available,
                selectedGroupIds = assigned,
            )
        }
    }

    fun save() {
        val current = _state.value
        if (current.saving) return
        val valid = validateProductInput(current, categories.value)
        if (valid == null) {
            _state.value = current.copy(error = productInputError(current, categories.value))
            return
        }

        _state.value = current.copy(saving = true, error = null)
        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                val product = ProductEntity(
                    id = current.editingId ?: UUID.randomUUID().toString(),
                    categoryId = valid.categoryId,
                    name = valid.name,
                    description = valid.description,
                    price = valid.price,
                    imagePath = current.imagePath,
                    available = valid.available,
                    createdAt = if (current.editingId != null) {
                        catalog.product(current.editingId)?.createdAt ?: now
                    } else now,
                    updatedAt = now,
                )
                catalog.saveProduct(product)
                catalog.setProductModifierGroups(product.id, current.selectedGroupIds.toList())
                product.id
            }.onSuccess { productId ->
                _state.value = _state.value.copy(saving = false, savedProductId = productId)
            }.onFailure {
                _state.value = _state.value.copy(
                    saving = false,
                    error = "Could not save product. Please try again.",
                )
            }
        }
    }

    private fun update(transform: ProductEditorState.() -> ProductEditorState) {
        _state.value = _state.value.transform()
    }

    class Factory(private val catalog: CatalogRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProductEditorViewModel(catalog) as T
    }
}

internal fun validateProductInput(
    state: ProductEditorState,
    categories: List<CategoryEntity>,
): ValidProductInput? {
    val name = state.name.trim()
    val categoryId = state.categoryId
    val price = parseRupiahInput(state.priceInput)
    if (name.isEmpty() || name.length > 80) return null
    if (categoryId == null || categories.none { it.id == categoryId }) return null
    if (price == null || price <= 0L) return null
    return ValidProductInput(
        name = name,
        description = state.description.trim(),
        price = price,
        categoryId = categoryId,
        available = state.available,
    )
}

internal fun productInputError(
    state: ProductEditorState,
    categories: List<CategoryEntity>,
): String = when {
    state.name.trim().isEmpty() -> "Product name is required."
    state.name.trim().length > 80 -> "Product name must be 80 characters or fewer."
    state.categoryId == null || categories.none { it.id == state.categoryId } ->
        "Choose a category."
    parseRupiahInput(state.priceInput) == null || parseRupiahInput(state.priceInput) == 0L ->
        "Enter a valid price greater than Rp0."
    else -> "Check the product details."
}

/** Accept plain digits or Indonesian thousands grouping, without floating-point conversion. */
internal fun parseRupiahInput(input: String): Long? {
    val text = input.trim()
    if (text.isEmpty()) return null
    if (text.all(Char::isDigit)) return text.toLongOrNull()
    val groups = text.split('.')
    val validGrouping = groups.isNotEmpty() &&
        groups.first().length in 1..3 && groups.first().all(Char::isDigit) &&
        groups.drop(1).all { it.length == 3 && it.all(Char::isDigit) }
    return if (validGrouping) groups.joinToString("").toLongOrNull() else null
}

/** Format a Long rupiah amount into the input field representation (e.g. 18000 -> "18.000"). */
internal fun formatPriceInput(price: Long): String {
    val s = price.toString()
    val len = s.length
    val groups = (0 until len).map { i ->
        val pos = len - i
        if (pos > 0 && pos % 3 == 0 && i > 0) "." else ""
    }
    return buildString {
        groups.forEachIndexed { i, sep -> append(sep); append(s[i]) }
    }
}
