package com.ragnala.pos.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.MapColumn
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE deleted = 0 ORDER BY position")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun byId(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE deleted = 0 ORDER BY name")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId AND deleted = 0 AND available = 1 ORDER BY name")
    fun observeAvailableInCategory(categoryId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE deleted = 0 AND available = 1 ORDER BY name")
    fun observeAvailable(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun byId(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' AND deleted = 0 ORDER BY name")
    fun search(query: String): Flow<List<ProductEntity>>

    @Upsert
    suspend fun upsert(product: ProductEntity)

    @Query("UPDATE products SET available = :available, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setAvailability(id: String, available: Boolean, updatedAt: Long)

    @Delete
    suspend fun delete(product: ProductEntity)
}

@Dao
interface ModifierDao {
    @Query("SELECT * FROM modifier_groups ORDER BY position")
    fun observeAllGroups(): Flow<List<ModifierGroupEntity>>

    @Query("SELECT * FROM modifier_groups WHERE id IN (SELECT groupId FROM product_modifier_groups WHERE productId = :productId) ORDER BY position")
    fun groupsForProduct(productId: String): Flow<List<ModifierGroupEntity>>

    @Query("SELECT * FROM modifier_options WHERE groupId = :groupId ORDER BY position")
    fun optionsForGroup(groupId: String): Flow<List<ModifierOptionEntity>>

    @Query("SELECT * FROM modifier_groups WHERE id = :id")
    suspend fun groupById(id: String): ModifierGroupEntity?

    @Upsert
    suspend fun upsertGroup(group: ModifierGroupEntity)

    @Upsert
    suspend fun upsertOption(option: ModifierOptionEntity)

    @Upsert
    suspend fun upsertProductGroupLink(link: ProductModifierGroupEntity)

    @Query("DELETE FROM product_modifier_groups WHERE productId = :productId")
    suspend fun clearProductLinks(productId: String)
}

@Dao
interface IngredientDao {
    @Query("SELECT * FROM ingredients WHERE deleted = 0 ORDER BY name")
    fun observeAll(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE id = :id")
    suspend fun byId(id: String): IngredientEntity?

    @Query("UPDATE ingredients SET currentStock = :newStock, updatedAt = :now WHERE id = :id")
    suspend fun setStock(id: String, newStock: Double, now: Long)

    @Query("SELECT * FROM ingredients WHERE id IN (:ids)")
    suspend fun byIds(ids: List<String>): List<IngredientEntity>

    @Upsert
    suspend fun upsertAll(ingredients: List<IngredientEntity>)

    @Query("SELECT * FROM ingredients WHERE deleted = 0 AND currentStock <= minStock ORDER BY name")
    fun observeLowStock(): Flow<List<IngredientEntity>>

    @Upsert
    suspend fun upsert(ingredient: IngredientEntity)

    @Query("SELECT COUNT(*) FROM recipe_items WHERE ingredientId = :ingredientId")
    suspend fun recipeReferenceCount(ingredientId: String): Int

    @Query("SELECT * FROM recipe_items WHERE productId = :productId")
    fun recipeForProduct(productId: String): Flow<List<RecipeItemEntity>>

    @Query("SELECT * FROM recipe_items WHERE productId = :productId")
    suspend fun recipeForProductOnce(productId: String): List<RecipeItemEntity>

    /**
     * Checks if a product can be made with current ingredient stock.
     * Returns true if the product has no recipe (no stock check needed) OR
     * if all recipe ingredients have sufficient stock for 1 unit.
     */
    @Query("""
        SELECT CASE WHEN COUNT(*) = 0 THEN 1 ELSE 0 END
        FROM recipe_items ri
        JOIN ingredients i ON i.id = ri.ingredientId
        WHERE ri.productId = :productId
          AND i.deleted = 0
          AND i.currentStock - ri.quantity < 0
    """)
    suspend fun isProductInStock(productId: String): Boolean

    @Upsert
    suspend fun upsertRecipeItem(item: RecipeItemEntity)

    @Upsert
    suspend fun upsertRecipeItems(items: List<RecipeItemEntity>)

    @Query("DELETE FROM recipe_items WHERE productId = :productId")
    suspend fun clearRecipe(productId: String)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE status IN (:statuses) ORDER BY createdAt DESC")
    fun observeByStatuses(statuses: List<String>): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun byId(id: String): OrderEntity?

    @Query("SELECT COALESCE(MAX(orderNumber), 0) FROM orders WHERE createdAt >= :startOfDayTs")
    suspend fun maxOrderNumberSince(startOfDayTs: Long): Long

    @Upsert
    suspend fun upsert(order: OrderEntity)

    @Query("SELECT * FROM order_items WHERE orderId = :orderId ORDER BY position")
    suspend fun itemsForOrder(orderId: String): List<OrderItemEntity>

    @Upsert
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Query("SELECT * FROM order_item_modifiers WHERE orderItemId IN (:orderItemIds)")
    suspend fun modifiersForItems(orderItemIds: List<String>): List<OrderItemModifierEntity>

    @Upsert
    suspend fun insertModifiers(modifiers: List<OrderItemModifierEntity>)

    @Query("DELETE FROM orders WHERE status = 'DRAFT' AND createdAt < :olderThan")
    suspend fun deleteDraftsOlderThan(olderThan: Long): Int

    @Query("SELECT * FROM orders WHERE status = 'WAITING_PAYMENT' AND updatedAt < :olderThan ORDER BY createdAt DESC")
    suspend fun waitingPaymentOlderThan(olderThan: Long): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE createdAt BETWEEN :start AND :end AND status IN ('PAID', 'FULFILLED', 'ARCHIVED')")
    suspend fun revenueOrdersBetween(start: Long, end: Long): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE createdAt BETWEEN :start AND :end AND status IN ('CANCELLED', 'VOIDED')")
    suspend fun voidedCancelledBetween(start: Long, end: Long): List<OrderEntity>

    @Query(
        "SELECT productName, SUM(quantity) AS qty, SUM(unitPrice * quantity) AS revenue " +
            "FROM order_items " +
            "WHERE orderId IN (SELECT id FROM orders WHERE createdAt BETWEEN :start AND :end AND status IN ('PAID', 'FULFILLED', 'ARCHIVED')) " +
            "GROUP BY productName ORDER BY qty DESC, revenue DESC",
    )
    suspend fun bestSellersBetween(start: Long, end: Long): List<BestSellerRow>
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE orderId = :orderId ORDER BY confirmedAt")
    fun observeForOrder(orderId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE orderId = :orderId")
    suspend fun forOrder(orderId: String): List<PaymentEntity>

    @Query(
        "SELECT method, SUM(amount) AS amount, COUNT(*) AS count " +
            "FROM payments WHERE confirmedAt BETWEEN :start AND :end GROUP BY method",
    )
    suspend fun paymentBreakdownBetween(start: Long, end: Long): List<PaymentBreakdownRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentEntity)

    @Update
    suspend fun update(payment: PaymentEntity)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE date BETWEEN :start AND :end ORDER BY date")
    fun observeBetween(start: Long, end: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :start AND :end")
    suspend fun between(start: Long, end: Long): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)
}

@Dao
interface AuditDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: AuditEntity)

    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<AuditEntity>>
}

@Dao
interface SettingsDao {
    @Query("SELECT `key` AS mapKey, value AS mapValue FROM settings")
    suspend fun getAll(): Map<@MapColumn(columnName = "mapKey") String, @MapColumn(columnName = "mapValue") String>

    @Query("SELECT value FROM settings WHERE key = :key")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(setting: SettingEntity)

    @Query("DELETE FROM settings WHERE `key` = :key")
    suspend fun deleteKey(key: String)
}
