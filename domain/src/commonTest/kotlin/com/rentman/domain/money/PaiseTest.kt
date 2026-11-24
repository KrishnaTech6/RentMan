package com.rentman.domain.money

import kotlin.test.Test
import kotlin.test.assertEquals

class PaiseTest {
    @Test
    fun arithmeticStaysInPaise() {
        assertEquals(Paise(68_000), Paise(800) * 85)
        assertEquals(Paise(588_000), 5_000L.rupees + Paise(68_000) + 200L.rupees)
        assertEquals(Paise(188_000), Paise(588_000) - 4_000L.rupees)
    }
}
