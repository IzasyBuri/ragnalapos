package com.ragnala.pos.ui.customer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartViewModelTest {

    @Test
    fun `decrement removes one base product and then removes its line`() {
        val twoCoffees = listOf(
            CartItem("coffee", "Coffee", 15_000L, quantity = 2),
        )

        val oneCoffee = decrementBaseProduct(twoCoffees, "coffee")
        assertEquals(1, oneCoffee.single().quantity)

        val empty = decrementBaseProduct(oneCoffee, "coffee")
        assertTrue(empty.isEmpty())
    }

    @Test
    fun `decrement base product preserves customized line`() {
        val milk = listOf(CartModifier("Milk", "Oat", 5_000L))
        val lines = listOf(
            CartItem("latte", "Latte", 20_000L, quantity = 1),
            CartItem("latte", "Latte", 25_000L, quantity = 1, modifiers = milk),
        )

        val result = decrementBaseProduct(lines, "latte")

        assertEquals(1, result.size)
        assertEquals(milk, result.single().modifiers)
    }
}
