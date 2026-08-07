package com.ragnala.pos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        ModifierGroupEntity::class,
        ModifierOptionEntity::class,
        ProductModifierGroupEntity::class,
        IngredientEntity::class,
        RecipeItemEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        OrderItemModifierEntity::class,
        PaymentEntity::class,
        ExpenseEntity::class,
        AuditEntity::class,
        SettingEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class RagnalaDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun modifierDao(): ModifierDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun orderDao(): OrderDao
    abstract fun paymentDao(): PaymentDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun auditDao(): AuditDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var instance: RagnalaDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // legacy rows have no COGS -> nullable column (matches entity `cogs: Long?`)
                db.execSQL("ALTER TABLE orders ADD COLUMN cogs INTEGER")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Audit M2: add @ForeignKey relations. Room cannot ALTER a table to add an FK,
                // so the child tables are recreated with the constraint and their data copied.
                // Parents (orders/products/ingredients/modifier_groups) are left untouched; the
                // swaps never delete parent rows, so FK enforcement is safe during migration.

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `modifier_options_new` (`id` TEXT NOT NULL, `groupId` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, `priceDelta` INTEGER NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`groupId`) REFERENCES `modifier_groups`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "INSERT INTO `modifier_options_new` (`id`, `groupId`, `name`, `priceDelta`, `position`) " +
                        "SELECT `id`, `groupId`, `name`, `priceDelta`, `position` FROM `modifier_options`"
                )
                db.execSQL("DROP TABLE `modifier_options`")
                db.execSQL("ALTER TABLE `modifier_options_new` RENAME TO `modifier_options`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_modifier_options_groupId` ON `modifier_options` (`groupId`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `product_modifier_groups_new` (`id` TEXT NOT NULL, `productId` TEXT NOT NULL, " +
                        "`groupId` TEXT NOT NULL, PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(`groupId`) REFERENCES `modifier_groups`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "INSERT INTO `product_modifier_groups_new` (`id`, `productId`, `groupId`) " +
                        "SELECT `id`, `productId`, `groupId` FROM `product_modifier_groups`"
                )
                db.execSQL("DROP TABLE `product_modifier_groups`")
                db.execSQL("ALTER TABLE `product_modifier_groups_new` RENAME TO `product_modifier_groups`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_modifier_groups_productId` ON `product_modifier_groups` (`productId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_modifier_groups_groupId` ON `product_modifier_groups` (`groupId`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recipe_items_new` (`id` TEXT NOT NULL, `productId` TEXT NOT NULL, " +
                        "`ingredientId` TEXT NOT NULL, `quantity` REAL NOT NULL, PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(`ingredientId`) REFERENCES `ingredients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "INSERT INTO `recipe_items_new` (`id`, `productId`, `ingredientId`, `quantity`) " +
                        "SELECT `id`, `productId`, `ingredientId`, `quantity` FROM `recipe_items`"
                )
                db.execSQL("DROP TABLE `recipe_items`")
                db.execSQL("ALTER TABLE `recipe_items_new` RENAME TO `recipe_items`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_items_productId` ON `recipe_items` (`productId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_items_ingredientId` ON `recipe_items` (`ingredientId`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `order_items_new` (`id` TEXT NOT NULL, `orderId` TEXT NOT NULL, " +
                        "`productId` TEXT NOT NULL, `productName` TEXT NOT NULL, `unitPrice` INTEGER NOT NULL, " +
                        "`quantity` INTEGER NOT NULL, `note` TEXT, `position` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`orderId`) REFERENCES `orders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "INSERT INTO `order_items_new` (`id`, `orderId`, `productId`, `productName`, `unitPrice`, `quantity`, `note`, `position`) " +
                        "SELECT `id`, `orderId`, `productId`, `productName`, `unitPrice`, `quantity`, `note`, `position` FROM `order_items`"
                )
                db.execSQL("DROP TABLE `order_items`")
                db.execSQL("ALTER TABLE `order_items_new` RENAME TO `order_items`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_order_items_orderId` ON `order_items` (`orderId`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `order_item_modifiers_new` (`id` TEXT NOT NULL, `orderItemId` TEXT NOT NULL, " +
                        "`optionName` TEXT NOT NULL, `priceDelta` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`orderItemId`) REFERENCES `order_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "INSERT INTO `order_item_modifiers_new` (`id`, `orderItemId`, `optionName`, `priceDelta`) " +
                        "SELECT `id`, `orderItemId`, `optionName`, `priceDelta` FROM `order_item_modifiers`"
                )
                db.execSQL("DROP TABLE `order_item_modifiers`")
                db.execSQL("ALTER TABLE `order_item_modifiers_new` RENAME TO `order_item_modifiers`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_order_item_modifiers_orderItemId` ON `order_item_modifiers` (`orderItemId`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `payments_new` (`id` TEXT NOT NULL, `orderId` TEXT NOT NULL, " +
                        "`method` TEXT NOT NULL, `amount` INTEGER NOT NULL, `tendered` INTEGER, `changeGiven` INTEGER, " +
                        "`confirmed` INTEGER NOT NULL, `confirmedAt` INTEGER, PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`orderId`) REFERENCES `orders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "INSERT INTO `payments_new` (`id`, `orderId`, `method`, `amount`, `tendered`, `changeGiven`, `confirmed`, `confirmedAt`) " +
                        "SELECT `id`, `orderId`, `method`, `amount`, `tendered`, `changeGiven`, `confirmed`, `confirmedAt` FROM `payments`"
                )
                db.execSQL("DROP TABLE `payments`")
                db.execSQL("ALTER TABLE `payments_new` RENAME TO `payments`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_orderId` ON `payments` (`orderId`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ingredients carry an optional pack cost: purchasePrice (Rp per pack)
                // + packSize (in unit). Nullable so legacy rows keep costing via costPerUnit.
                db.execSQL("ALTER TABLE `ingredients` ADD COLUMN `purchasePrice` INTEGER")
                db.execSQL("ALTER TABLE `ingredients` ADD COLUMN `packSize` REAL")
            }
        }

        fun get(context: Context): RagnalaDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RagnalaDatabase::class.java,
                    "ragnala.db",
                )
                    // PRD §15: WAL + FK enforcement, migrations via PRAGMA user_version
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(SeedCallback)
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { instance = it }
            }
    }
}

/**
 * Seeds default categories on first database creation or when the categories
 * table is empty (covers fresh installs and upgrades from versions without seeding).
 * PRD §5: root categories — Signature, Kopi, Milk Base, Dessert, Food.
 */
private val SeedCallback = object : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        seedCategories(db)
        seedModifierGroups(db)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        val cursor = db.query("SELECT COUNT(*) FROM categories WHERE deleted = 0")
        cursor.use {
            if (it.moveToFirst() && it.getInt(0) == 0) {
                seedCategories(db)
            }
        }
    }

    private fun seedCategories(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        db.execSQL(
            "INSERT INTO categories (id, name, position, createdAt, updatedAt, deleted) VALUES " +
            "('cat_sig', 'Signature', 0, $now, $now, 0), " +
            "('cat_kopi', 'Kopi', 1, $now, $now, 0), " +
            "('cat_milk', 'Milk Base', 2, $now, $now, 0), " +
            "('cat_dessert', 'Dessert', 3, $now, $now, 0), " +
            "('cat_food', 'Food', 4, $now, $now, 0);"
        )
        seedProducts(db, now)
    }

    private fun seedProducts(db: SupportSQLiteDatabase, now: Long) {
        // Only seed if the products table is also empty
        val cursor = db.query("SELECT COUNT(*) FROM products WHERE deleted = 0")
        cursor.use {
            if (it.moveToFirst() && it.getInt(0) > 0) return
        }
        db.execSQL(
            "INSERT INTO products (id, categoryId, name, description, price, available, createdAt, updatedAt, deleted) VALUES " +
            "('prod_01', 'cat_sig', 'Berry Obsidian', 'Blackberry mint cold brew', 25000, 1, $now, $now, 0), " +
            "('prod_02', 'cat_sig', 'Black Caribbean', 'Dark chocolate cold brew', 25000, 1, $now, $now, 0), " +
            "('prod_03', 'cat_sig', 'Cloudy Yellow', 'Turmeric honey latte', 25000, 1, $now, $now, 0), " +
            "('prod_04', 'cat_milk', 'Kyoto Bloom', 'Cherry blossom milk latte', 25000, 1, $now, $now, 0), " +
            "('prod_05', 'cat_milk', 'Butterscotch', 'Butterscotch latte', 25000, 1, $now, $now, 0), " +
            "('prod_06', 'cat_kopi', 'Americano', 'Classic Italian espresso with water', 15000, 1, $now, $now, 0), " +
            "('prod_07', 'cat_kopi', 'Filter V60 Origin', 'Single-origin pour-over', 23000, 1, $now, $now, 0), " +
            "('prod_08', 'cat_kopi', 'Tubruk', 'Indonesian coarse-ground brew', 18000, 1, $now, $now, 0), " +
            "('prod_09', 'cat_kopi', 'Vietnam Drip', 'Sweetened condensed milk drip', 20000, 1, $now, $now, 0), " +
            "('prod_10', 'cat_milk', 'Matcha Chocolate', 'Matcha and dark chocolate latte', 28000, 1, $now, $now, 0), " +
            "('prod_11', 'cat_milk', 'Salted Caramel', 'House salted caramel latte', 27000, 1, $now, $now, 0), " +
            "('prod_12', 'cat_dessert', 'Cheesecake', 'New York cheesecake slice', 22000, 1, $now, $now, 0), " +
            "('prod_13', 'cat_dessert', 'Taro', 'Purple taro mousse cake', 20000, 1, $now, $now, 0), " +
            "('prod_14', 'cat_dessert', 'Matcha', 'Traditional matcha dessert', 18000, 1, $now, $now, 0), " +
            "('prod_15', 'cat_food', 'Avocado Toast', 'Smashed avocado on sourdough', 32000, 1, $now, $now, 0), " +
            "('prod_16', 'cat_food', 'Chia Pudding', 'Overnight chia with coconut', 19000, 1, $now, $now, 0);"
        )
    }

    /**
     * Seeds demo modifier groups + options on first creation.
     * PRD: products are customizable via modifier groups (e.g. milk, size, toppings).
     */
    private fun seedModifierGroups(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO modifier_groups (id, name, required, minSelections, maxSelections, position) VALUES " +
            "('grp_milk', 'Milk', 0, 0, 1, 0), " +
            "('grp_size', 'Size', 1, 1, 1, 1), " +
            "('grp_sweet', 'Sweetness', 0, 0, 1, 2), " +
            "('grp_topping', 'Toppings', 0, 0, 3, 3);"
        )
        db.execSQL(
            "INSERT INTO modifier_options (id, groupId, name, priceDelta, position) VALUES " +
            "('opt_milk_oat', 'grp_milk', 'Oat milk', 5000, 0), " +
            "('opt_milk_soy', 'grp_milk', 'Soy milk', 4000, 1), " +
            "('opt_milk_almond', 'grp_milk', 'Almond milk', 5000, 2), " +
            "('opt_size_small', 'grp_size', 'Small', 0, 0), " +
            "('opt_size_medium', 'grp_size', 'Medium', 3000, 1), " +
            "('opt_size_large', 'grp_size', 'Large', 6000, 2), " +
            "('opt_sweet_less', 'grp_sweet', 'Less sweet', 0, 0), " +
            "('opt_sweet_extra', 'grp_sweet', 'Extra sweet', 0, 1), " +
            "('opt_top_boba', 'grp_topping', 'Boba', 6000, 0), " +
            "('opt_top_jelly', 'grp_topping', 'Crystal jelly', 5000, 1), " +
            "('opt_top_whip', 'grp_topping', 'Whipped cream', 4000, 2);"
        )
    }
}
