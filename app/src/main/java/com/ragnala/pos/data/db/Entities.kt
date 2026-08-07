package com.ragnala.pos.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ragnala.pos.domain.OrderStatus

// ---- Catalog ----

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val position: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

@Entity(
    tableName = "products",
    indices = [Index("categoryId"), Index("deleted")],
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val name: String,
    val description: String,
    val price: Long,               // integer rupiah (PRD §15)
    val imagePath: String?,        // relative path; null = placeholder
    val available: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

@Entity(tableName = "modifier_groups")
data class ModifierGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val required: Boolean,
    val minSelections: Int,
    val maxSelections: Int,
    val position: Int,
)

@Entity(
    tableName = "modifier_options",
    indices = [Index("groupId")],
    foreignKeys = [
        ForeignKey(
            entity = ModifierGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ModifierOptionEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val name: String,
    val priceDelta: Long,          // integer rupiah, may be negative
    val position: Int,
)

@Entity(
    tableName = "product_modifier_groups",
    indices = [Index("productId"), Index("groupId")],
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ModifierGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ProductModifierGroupEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val groupId: String,
)

// ---- Inventory & recipes ----

@Entity(
    tableName = "ingredients",
    indices = [Index("deleted")],
)
data class IngredientEntity(
    @PrimaryKey val id: String,
    val name: String,
    val unit: String,              // ml, g, pcs, shot, scoop
    val currentStock: Double,
    val minStock: Double,
    val costPerUnit: Long,         // integer rupiah
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

@Entity(
    tableName = "recipe_items",
    indices = [Index("productId"), Index("ingredientId")],
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = IngredientEntity::class,
            parentColumns = ["id"],
            childColumns = ["ingredientId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RecipeItemEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val ingredientId: String,
    val quantity: Double,
)

// ---- Orders ----

@Entity(
    tableName = "orders",
    indices = [
        Index("status"),
        Index("createdAt"),
        Index("orderNumber"),
        Index("status", "createdAt"),
    ],
)
data class OrderEntity(
    @PrimaryKey val id: String,
    val orderNumber: Long,         // per-day sequence (#001, #002...)
    val status: OrderStatus,
    val customerName: String?,
    val subtotal: Long,
    val serviceCharge: Long,
    val tax: Long,
    val total: Long,
    val cogs: Long?,             // cost of goods, snapshotted at payment (PRD §9 Reports); null for legacy rows
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
)

@Entity(
    tableName = "order_items",
    indices = [Index("orderId")],
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class OrderItemEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val productId: String,
    val productName: String,       // snapshot — survives product rename/delete
    val unitPrice: Long,
    val quantity: Int,
    val note: String?,
    val position: Int,
)

@Entity(
    tableName = "order_item_modifiers",
    indices = [Index("orderItemId")],
    foreignKeys = [
        ForeignKey(
            entity = OrderItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class OrderItemModifierEntity(
    @PrimaryKey val id: String,
    val orderItemId: String,
    val optionName: String,        // snapshot
    val priceDelta: Long,
)

@Entity(
    tableName = "payments",
    indices = [Index("orderId")],
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PaymentEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val method: String,            // CASH, QRIS, DEBIT, CREDIT_CARD, BANK_TRANSFER
    val amount: Long,
    val tendered: Long?,           // cash only
    val changeGiven: Long?,        // cash only
    val confirmed: Boolean,
    val confirmedAt: Long?,
)

@Entity(
    tableName = "expenses",
    indices = [Index("date")],
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val category: String,
    val amount: Long,
    val note: String,
    val date: Long,
)

@Entity(
    tableName = "audit_log",
    indices = [Index("timestamp")],
)
data class AuditEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val action: String,            // STOCK_ADJUST, VOID, CANCEL, PRICE_CHANGE, PIN_CHANGE, RESTORE...
    val entityType: String,
    val entityId: String,
    val delta: String,             // compact description (e.g. "-2.0 espresso")
    val reason: String?,
    val userLabel: String,         // "owner" | "barista"
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String,
)

// ---- Report projection rows (Room maps SELECT results to these POJOs) ----

data class BestSellerRow(
    val productName: String,
    val qty: Int,
    val revenue: Long,
)

data class PaymentBreakdownRow(
    val method: String,
    val amount: Long,
    val count: Int,
)
