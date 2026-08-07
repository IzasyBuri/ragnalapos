package com.ragnala.pos.ui.customer

import com.ragnala.pos.data.db.ModifierGroupEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickAddEligibilityTest {

    @Test
    fun `product without modifier groups can be quick added`() {
        assertTrue(isQuickAddEligible(emptyList()))
    }

    @Test
    fun `optional modifier groups do not block quick add`() {
        assertTrue(
            isQuickAddEligible(
                listOf(group(required = false, minSelections = 0)),
            ),
        )
    }

    @Test
    fun `required modifier selection blocks quick add`() {
        assertFalse(
            isQuickAddEligible(
                listOf(group(required = true, minSelections = 1)),
            ),
        )
    }

    private fun group(required: Boolean, minSelections: Int) = ModifierGroupEntity(
        id = "group",
        name = "Choice",
        required = required,
        minSelections = minSelections,
        maxSelections = 1,
        position = 0,
    )
}
