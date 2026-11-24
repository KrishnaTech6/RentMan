package com.rentman.domain.tenancy

import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RentIncreasePolicyTest {
    @Test
    fun raisesByPercent() {
        assertEquals(7_700L.rupees, RentIncreasePolicy.raised(7_000L.rupees, percent = 10))
        assertEquals(4_950L.rupees, RentIncreasePolicy.raised(4_500L.rupees, percent = 10))
        assertEquals(26_400L.rupees, RentIncreasePolicy.raised(24_000L.rupees, percent = 10))
        assertEquals(5_250L.rupees, RentIncreasePolicy.raised(5_000L.rupees, percent = 5))
    }

    @Test
    fun roundsToTheNearestTenRupees() {
        // ₹4,555 + 10% = ₹5,010.50 -> ₹5,010
        assertEquals(5_010L.rupees, RentIncreasePolicy.raised(4_555L.rupees, percent = 10))
        // ₹4,556 + 10% = ₹5,011.60 -> ₹5,010
        assertEquals(5_010L.rupees, RentIncreasePolicy.raised(4_556L.rupees, percent = 10))
        // ₹4,560 + 10% = ₹5,016 -> ₹5,020
        assertEquals(5_020L.rupees, RentIncreasePolicy.raised(4_560L.rupees, percent = 10))
    }

    @Test
    fun zeroPercentKeepsTheRent() {
        assertEquals(Paise(700_000), RentIncreasePolicy.raised(Paise(700_000), percent = 0))
    }

    @Test
    fun rejectsAPercentOutsideZeroToHundred() {
        assertFailsWith<IllegalArgumentException> { RentIncreasePolicy.raised(7_000L.rupees, percent = -10) }
        assertFailsWith<IllegalArgumentException> { RentIncreasePolicy.raised(7_000L.rupees, percent = 101) }
    }

    @Test
    fun nextRaiseIsEveryNMonthsFromTheLastChange() {
        // Agreement started Jan 2026, raise every 11 months -> Dec 2026
        assertEquals(
            BillingMonth.of(2026, 12),
            RentIncreasePolicy.nextRaise(from = BillingMonth.of(2026, 1), everyMonths = 11),
        )
    }

    @Test
    fun askOnlyWhenTheRaiseMonthHasArrivedAndNotSnoozed() {
        val due = BillingMonth.of(2026, 12)
        assertEquals(false, RentIncreasePolicy.shouldAsk(due, today = BillingMonth.of(2026, 11), snoozedUntil = null))
        assertEquals(true, RentIncreasePolicy.shouldAsk(due, today = BillingMonth.of(2026, 12), snoozedUntil = null))
        assertEquals(true, RentIncreasePolicy.shouldAsk(due, today = BillingMonth.of(2027, 3), snoozedUntil = null))
        // "Not now" in Dec hides the card until Jan
        assertEquals(
            false,
            RentIncreasePolicy.shouldAsk(due, today = BillingMonth.of(2026, 12), snoozedUntil = BillingMonth.of(2027, 1)),
        )
        assertEquals(
            true,
            RentIncreasePolicy.shouldAsk(due, today = BillingMonth.of(2027, 1), snoozedUntil = BillingMonth.of(2027, 1)),
        )
    }
}
