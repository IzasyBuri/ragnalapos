package com.ragnala.pos.data.repo

import com.ragnala.pos.data.db.AuditDao
import com.ragnala.pos.data.db.AuditEntity
import com.ragnala.pos.data.db.IngredientDao
import com.ragnala.pos.data.db.IngredientEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class InventoryRepository(
    private val ingredientDao: IngredientDao,
    private val auditDao: AuditDao,
) {
    fun ingredients(): Flow<List<IngredientEntity>> = ingredientDao.observeAll()
    fun lowStock(): Flow<List<IngredientEntity>> = ingredientDao.observeLowStock()
    suspend fun ingredient(id: String): IngredientEntity? = ingredientDao.byId(id)
    suspend fun save(ingredient: IngredientEntity) = ingredientDao.upsert(ingredient)

    /**
     * Stock adjustment with mandatory reason (PRD §9 Inventory, §9 Role Enforcement).
     * Negative delta = usage/removal; positive = restock.
     */
    suspend fun adjust(
        ingredientId: String,
        delta: Double,
        reason: String,
        userLabel: String,
        now: Long,
    ) {
        require(reason.isNotBlank()) { "Stock adjustment requires a reason" }
        val current = ingredientDao.byId(ingredientId)
            ?: error("Ingredient not found: $ingredientId")
        val newStock = current.currentStock + delta
        ingredientDao.setStock(ingredientId, newStock, now)
        auditDao.insert(
            AuditEntity(
                id = UUID.randomUUID().toString(),
                timestamp = now,
                action = "STOCK_ADJUST",
                entityType = "ingredient",
                entityId = ingredientId,
                delta = "${if (delta >= 0) "+" else ""}$delta -> $newStock",
                reason = reason,
                userLabel = userLabel,
            ),
        )
    }

    /** Raises an error if the ingredient is referenced by any recipe (PRD §9 Recipes). */
    suspend fun deleteBlockedIfReferenced(ingredient: IngredientEntity) {
        val refs = ingredientDao.recipeReferenceCount(ingredient.id)
        check(refs == 0) { "Ingredient is used by $refs recipe(s) and cannot be deleted" }
        ingredientDao.upsert(ingredient.copy(deleted = true))
    }
}
