package com.rentman.sharedui.components

import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import kotlin.test.Test
import kotlin.test.assertEquals

class FormatRupeesTest {
    @Test
    fun wholeRupeesHaveNoDecimals() {
        assertEquals("₹0", formatRupees(Paise.Zero))
        assertEquals("₹7,500", formatRupees(7_500L.rupees))
        assertEquals("₹24,000", formatRupees(24_000L.rupees))
    }

    @Test
    fun usesIndianLakhGrouping() {
        assertEquals("₹1,24,000", formatRupees(124_000L.rupees))
        assertEquals("₹12,34,567", formatRupees(1_234_567L.rupees))
    }

    @Test
    fun showsTwoDecimalsOnlyWhenThereArePaise() {
        assertEquals("₹850.50", formatRupees(Paise(85_050)))
        assertEquals("₹0.05", formatRupees(Paise(5)))
    }

    @Test
    fun aNegativeBalanceKeepsItsSign() {
        assertEquals("-₹1,880", formatRupees(Paise(-188_000)))
    }
}
