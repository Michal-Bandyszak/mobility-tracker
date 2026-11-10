package com.mobilitytracker.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class MorningWarmupRepositoryTest {

    private val all = listOf("a", "b", "c", "d")

    @Test
    fun `never picks a recent video while others are available`() {
        repeat(200) { seed ->
            assertEquals("d", chooseMorningWarmupFile(all, listOf("a", "b", "c"), Random(seed)))
        }
    }

    @Test
    fun `picks every non recent video over many draws`() {
        val picked = (0 until 200).map { chooseMorningWarmupFile(all, listOf("a"), Random(it)) }.toSet()
        assertEquals(setOf("b", "c", "d"), picked)
    }

    @Test
    fun `with full history only the least recent video is allowed`() {
        repeat(50) { seed ->
            assertEquals("d", chooseMorningWarmupFile(all, listOf("a", "b", "c", "d"), Random(seed)))
        }
    }

    @Test
    fun `single video and empty pool`() {
        assertEquals("a", chooseMorningWarmupFile(listOf("a"), listOf("a")))
        assertNull(chooseMorningWarmupFile(emptyList(), emptyList()))
    }
}
