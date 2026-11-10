package com.mobilitytracker.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class PluralsTest {

    @Test
    fun `polish plural forms`() {
        assertEquals(listOf("0 dni", "1 dzień", "2 dni"), listOf(0, 1, 2).map(::daysLabel))
    }
}
