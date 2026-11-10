package com.mobilitytracker.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class PluralsTest {

    @Test
    fun `polish plural forms`() {
        assertEquals(listOf("1 seria", "3 serie", "5 serii", "12 serii", "22 serie"), listOf(1, 3, 5, 12, 22).map(::setsLabel))
        assertEquals(listOf("0 dni", "1 dzień", "2 dni"), listOf(0, 1, 2).map(::daysLabel))
    }
}
