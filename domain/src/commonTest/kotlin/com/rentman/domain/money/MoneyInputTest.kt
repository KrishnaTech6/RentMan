package com.rentman.domain.money

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyInputTest {
    @Test
    fun parsesWholeRupeesIgnoringCommasSymbolsAndSpaces() {
        assertEquals(Paise(700_000), MoneyInput.rupees("7000"))
        assertEquals(Paise(700_000), MoneyInput.rupees("7,000"))
        assertEquals(Paise(2_400_000), MoneyInput.rupees("₹ 24,000"))
    }

    @Test
    fun rejectsBlankZeroLettersAndHugeAmounts() {
        assertNull(MoneyInput.rupees(""))
        assertNull(MoneyInput.rupees("0"))
        assertNull(MoneyInput.rupees("12ab"))
        assertNull(MoneyInput.rupees("12.5"))
        assertNull(MoneyInput.rupees("99999999999"))
    }

    @Test
    fun parsesAPerUnitRateWithUpToTwoDecimals() {
        assertEquals(Paise(800), MoneyInput.rate("8"))
        assertEquals(Paise(850), MoneyInput.rate("8.5"))
        assertEquals(Paise(850), MoneyInput.rate("8.50"))
        assertEquals(Paise(50), MoneyInput.rate("0.5"))
        assertEquals(Paise(900), MoneyInput.rate("₹9"))
    }

    @Test
    fun aRateIsAtMostNinetyNineRupeesSoAMissingDecimalPointIsCaught() {
        assertEquals(Paise(9_999), MoneyInput.rate("99.99"))
        assertEquals(Paise(9_900), MoneyInput.rate("99"))
        assertNull(MoneyInput.rate("950")) // 9.50 typed without the point
        assertNull(MoneyInput.rate("100"))
        // Payments are not rates: they can be large.
        assertEquals(Paise(95_000), MoneyInput.amount("950"))
    }

    @Test
    fun rejectsBadRates() {
        assertNull(MoneyInput.rate(""))
        assertNull(MoneyInput.rate("0"))
        assertNull(MoneyInput.rate("0.00"))
        assertNull(MoneyInput.rate("8.555"))
        assertNull(MoneyInput.rate("8."))
        assertNull(MoneyInput.rate(".5"))
        assertNull(MoneyInput.rate("abc"))
        assertNull(MoneyInput.rate("1000000"))
    }

    @Test
    fun parsesAPaymentWithUpToTwoDecimals() {
        assertEquals(Paise(338_000), MoneyInput.amount("3,380"))
        assertEquals(Paise(338_050), MoneyInput.amount("₹ 3,380.50"))
        assertEquals(Paise(1_250), MoneyInput.amount("12.5"))
        assertEquals(Paise(99_999_999_900), MoneyInput.amount("999,999,999"))
    }

    @Test
    fun rejectsBadPayments() {
        listOf("", "0", "0.00", "12.505", "12.", ".5", "abc", "1,0000,000,000", "-5").forEach {
            assertNull(MoneyInput.amount(it), "'$it' should be rejected")
        }
    }

    @Test
    fun writesMoneyBackAsTypedText() {
        assertEquals("3380", MoneyInput.text(Paise(338_000)))
        assertEquals("3380.50", MoneyInput.text(Paise(338_050)))
        assertEquals("3380.05", MoneyInput.text(Paise(338_005)))
        assertEquals(Paise(338_050), MoneyInput.amount(MoneyInput.text(Paise(338_050)))) // what Full amount fills in is accepted
    }
}
