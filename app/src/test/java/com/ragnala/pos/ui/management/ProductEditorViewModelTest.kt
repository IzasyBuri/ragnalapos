package com.ragnala.pos.ui.management

import com.ragnala.pos.data.db.CategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun `recipe build drops incomplete rows and is optional`() {
        assertTrue(buildRecipeItems("p1", emptyList()).isEmpty())
        assertTrue(buildRecipeItems("p1", listOf(RecipeDraft())).isEmpty())
        assertTrue(buildRecipeItems("p1", listOf(RecipeDraft(ingredientId = "i1", quantity = ""))).isEmpty())
        assertTrue(buildRecipeItems("p1", listOf(RecipeDraft(ingredientId = "i1", quantity = "0"))).isEmpty())
        assertTrue(buildRecipeItems("p1", listOf(RecipeDraft(ingredientId = "i1", quantity = "-2"))).isEmpty())
        assertTrue(buildRecipeItems("p1", listOf(RecipeDraft(ingredientId = "i1", quantity = "abc"))).isEmpty())
    }

    @Test
    fun `recipe build persists valid rows with productId`() {
        val items = buildRecipeItems(
            "p1",
            listOf(RecipeDraft(ingredientId = "i1", quantity = "18"), RecipeDraft(ingredientId = "i2", quantity = "1.5")),
        )
        assertEquals(2, items.size)
        assertEquals("i1", items[0].ingredientId)
        assertEquals(18.0, items[0].quantity, 0.0)
        assertEquals("i2", items[1].ingredientId)
        assertEquals(1.5, items[1].quantity, 0.0)
        assertTrue(items.all { it.productId == "p1" })
    }

    @Test
    fun `recipe build merges duplicate ingredients by summing quantity`() {
        val items = buildRecipeItems(
            "p1",
            listOf(
                RecipeDraft(ingredientId = "i1", quantity = "18"),
                RecipeDraft(ingredientId = "i1", quantity = "2"),
                RecipeDraft(ingredientId = "i2", quantity = "5"),
            ),
        )
        assertEquals(2, items.size)
        assertEquals(20.0, items.first { it.ingredientId == "i1" }.quantity, 0.0)
    }

    @Test
    fun `quantity formatter shows integers without decimals`() {
        assertEquals("18", formatQuantity(18.0))
        assertEquals("1.5", formatQuantity(1.5))
        assertEquals("0", formatQuantity(0.0))
    }
}
