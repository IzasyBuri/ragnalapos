package com.ragnala.pos.ui.management

import com.ragnala.pos.data.db.CategoryEntity
import com.ragnala.pos.data.db.ProductEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProductListViewModelTest {

    @Test
    fun `product rows include unavailable products and use category order`() {
        val categories = listOf(
            category("coffee", "Coffee", position = 1),
            category("food", "Food", position = 2),
        )
        val products = listOf(
            product("cake", "Cake", "food", available = false),
            product("americano", "Americano", "coffee", available = true),
        )

        val rows = buildProductListRows(products, categories)

        assertEquals(listOf("Americano", "Cake"), rows.map { it.product.name })
        assertEquals(listOf("Coffee", "Food"), rows.map { it.categoryName })
        assertFalse(rows.last().product.available)
    }

    private fun category(id: String, name: String, position: Int) = CategoryEntity(
        id = id,
        name = name,
        position = position,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun product(
        id: String,
        name: String,
        categoryId: String,
        available: Boolean,
    ) = ProductEntity(
        id = id,
        categoryId = categoryId,
        name = name,
        description = "",
        price = 10_000L,
        imagePath = null,
        available = available,
        createdAt = 1L,
        updatedAt = 1L,
    )
}
