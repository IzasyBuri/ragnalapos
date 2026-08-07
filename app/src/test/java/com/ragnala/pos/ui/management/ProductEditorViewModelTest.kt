package com.ragnala.pos.ui.management

import com.ragnala.pos.data.db.CategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProductEditorViewModelTest {

    @Test
    fun `rupiah parser accepts digits and Indonesian thousands grouping`() {
        assertEquals(25_000L, parseRupiahInput("25000"))
        assertEquals(25_000L, parseRupiahInput("25.000"))
        assertEquals(1_250_000L, parseRupiahInput("1.250.000"))
    }

    @Test
    fun `rupiah parser rejects malformed or decimal values`() {
        assertNull(parseRupiahInput("2.5"))
        assertNull(parseRupiahInput("25,000"))
        assertNull(parseRupiahInput("-25000"))
        assertNull(parseRupiahInput("25.00"))
    }

    @Test
    fun `valid product input trims text and keeps integer price`() {
        val category = CategoryEntity("coffee", "Coffee", 1, 1L, 1L)
        val result = validateProductInput(
            state = ProductEditorState(
                name = "  Flat White  ",
                description = "  Double ristretto  ",
                priceInput = "28.000",
                categoryId = category.id,
                available = true,
            ),
            categories = listOf(category),
        )

        assertEquals("Flat White", result?.name)
        assertEquals("Double ristretto", result?.description)
        assertEquals(28_000L, result?.price)
        assertEquals("coffee", result?.categoryId)
    }

    @Test
    fun `validate rejects empty name`() {
        val category = CategoryEntity("coffee", "Coffee", 1, 1L, 1L)
        val result = validateProductInput(
            state = ProductEditorState(
                name = "",
                description = "desc",
                priceInput = "25000",
                categoryId = category.id,
                available = true,
            ),
            categories = listOf(category),
        )
        assertNull(result)
    }

    @Test
    fun `validate rejects missing category`() {
        val category = CategoryEntity("coffee", "Coffee", 1, 1L, 1L)
        val result = validateProductInput(
            state = ProductEditorState(
                name = "Latte",
                description = "desc",
                priceInput = "25000",
                categoryId = null,
                available = true,
            ),
            categories = listOf(category),
        )
        assertNull(result)
        assertEquals(
            "Choose a category.",
            productInputError(
                ProductEditorState(name = "Latte", priceInput = "25000", categoryId = null),
                listOf(category),
            ),
        )
    }

    @Test
    fun `validate rejects zero or invalid price`() {
        val category = CategoryEntity("coffee", "Coffee", 1, 1L, 1L)
        val result1 = validateProductInput(
            state = ProductEditorState(name = "Latte", priceInput = "0", categoryId = category.id),
            categories = listOf(category),
        )
        assertNull(result1)
        val result2 = validateProductInput(
            state = ProductEditorState(name = "Latte", priceInput = "abc", categoryId = category.id),
            categories = listOf(category),
        )
        assertNull(result2)
    }

    @Test
    fun `editing state carries editingId for update`() {
        val category = CategoryEntity("cat_sig", "Signature", 0, 1L, 1L)
        val state = ProductEditorState(
            editingId = "existing-123",
            name = "Updated",
            priceInput = "30.000",
            categoryId = category.id,
            available = true,
        )
        val result = validateProductInput(state, listOf(category))
        assertNotNull(result)
    }
}
