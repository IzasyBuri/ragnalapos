package com.ragnala.pos.data.repo

import com.ragnala.pos.data.db.CategoryDao
import com.ragnala.pos.data.db.CategoryEntity
import com.ragnala.pos.data.db.IngredientDao
import com.ragnala.pos.data.db.IngredientEntity
import com.ragnala.pos.data.db.ModifierDao
import com.ragnala.pos.data.db.ModifierGroupEntity
import com.ragnala.pos.data.db.ModifierOptionEntity
import com.ragnala.pos.data.db.ProductDao
import com.ragnala.pos.data.db.ProductEntity
import com.ragnala.pos.data.db.RecipeItemEntity
import kotlinx.coroutines.flow.Flow

class CatalogRepository(
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val modifierDao: ModifierDao,
    private val ingredientDao: IngredientDao,
) {
    fun categories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()
    suspend fun saveCategory(category: CategoryEntity) = categoryDao.upsert(category)
    fun products(): Flow<List<ProductEntity>> = productDao.observeAll()
    fun availableProducts(categoryId: String): Flow<List<ProductEntity>> =
        productDao.observeAvailableInCategory(categoryId)
    fun availableProducts(): Flow<List<ProductEntity>> = productDao.observeAvailable()
    fun search(query: String): Flow<List<ProductEntity>> = productDao.search(query)
    suspend fun product(id: String): ProductEntity? = productDao.byId(id)
    suspend fun saveProduct(product: ProductEntity) = productDao.upsert(product)
    suspend fun setProductAvailability(id: String, available: Boolean) =
        productDao.setAvailability(id, available, System.currentTimeMillis())
    suspend fun deleteProduct(product: ProductEntity) = productDao.delete(product)

    /** Checks if a product has enough ingredient stock for 1 unit. Products without recipes are always in stock. */
    suspend fun isProductInStock(productId: String): Boolean = ingredientDao.isProductInStock(productId)

    fun ingredients(): Flow<List<IngredientEntity>> = ingredientDao.observeAll()

    fun modifierGroups(productId: String): Flow<List<ModifierGroupEntity>> =
        modifierDao.groupsForProduct(productId)
    fun allModifierGroups(): Flow<List<ModifierGroupEntity>> = modifierDao.observeAllGroups()
    fun modifierOptions(groupId: String): Flow<List<ModifierOptionEntity>> =
        modifierDao.optionsForGroup(groupId)
    suspend fun saveModifierGroup(group: ModifierGroupEntity) = modifierDao.upsertGroup(group)
    suspend fun saveModifierOption(option: ModifierOptionEntity) = modifierDao.upsertOption(option)

    /** Atomic replace of a product's modifier-group links. */
    suspend fun setProductModifierGroups(productId: String, groupIds: List<String>) {
        modifierDao.clearProductLinks(productId)
        groupIds.forEachIndexed { index, groupId ->
            modifierDao.upsertProductGroupLink(
                com.ragnala.pos.data.db.ProductModifierGroupEntity(
                    id = "$productId:$groupId",
                    productId = productId,
                    groupId = groupId,
                ),
            )
        }
    }

    fun recipe(productId: String): Flow<List<RecipeItemEntity>> =
        ingredientDao.recipeForProduct(productId)
    suspend fun saveRecipe(productId: String, items: List<RecipeItemEntity>) {
        ingredientDao.clearRecipe(productId)
        ingredientDao.upsertRecipeItems(items)
    }
}
