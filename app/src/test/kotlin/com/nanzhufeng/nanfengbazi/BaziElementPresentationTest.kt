package com.nanzhufeng.nanfengbazi

import org.junit.Assert.assertEquals
import org.junit.Test

class BaziElementPresentationTest {
    @Test
    fun `metal characters share the bright yellow palette token`() {
        listOf('庚', '辛', '申', '酉').forEach { character ->
            assertEquals(BaziElementPalette.Metal, baziElementColor(character))
        }
        assertEquals(BaziElementPalette.Metal, baziElementColor("金"))
    }
}
