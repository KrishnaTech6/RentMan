package com.rentman.domain.tenancy

import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class RentHistoryTest {
    private fun change(old: Paise, new: Paise, effective: BillingMonth) =
        RentChange(renterId = RenterId(1), oldRent = old, newRent = new, effectiveMonth = effective, decidedOn = LocalDate(2026, 11, 25))

    private val december = BillingMonth.of(2026, 12)
    private val raise = change(7_500L.rupees, 8_250L.rupees, december)

    @Test
    fun withNoRaisesTheRentIsTheCurrentOne() {
        assertEquals(7_500L.rupees, RentHistory.rentFor(december, current = 7_500L.rupees, changes = emptyList()))
    }

    @Test
    fun aMonthBeforeTheRaiseHasTheOldRentEvenThoughTheRenterNowPaysMore() {
        // The renter's rent is already 8,250 (the raise was accepted), but November was agreed at 7,500.
        assertEquals(7_500L.rupees, RentHistory.rentFor(BillingMonth.of(2026, 11), current = 8_250L.rupees, changes = listOf(raise)))
    }

    @Test
    fun theRaiseAppliesFromItsMonthOn() {
        assertEquals(8_250L.rupees, RentHistory.rentFor(december, current = 8_250L.rupees, changes = listOf(raise)))
        assertEquals(8_250L.rupees, RentHistory.rentFor(BillingMonth.of(2027, 3), current = 8_250L.rupees, changes = listOf(raise)))
    }

    @Test
    fun aRentEditedByHandAfterTheLastRaiseIsKeptForTheMonthsFromThenOn() {
        // The renter's rent was raised to 8,250 from December, and later fixed to 9,000 in Edit renter (which records no raise).
        val edited = 9_000L.rupees

        assertEquals(edited, RentHistory.rentFor(december, edited, listOf(raise)))
        assertEquals(edited, RentHistory.rentFor(BillingMonth.of(2027, 3), edited, listOf(raise)))
        assertEquals(7_500L.rupees, RentHistory.rentFor(BillingMonth.of(2026, 11), edited, listOf(raise)))
    }

    @Test
    fun severalRaisesGiveEachMonthTheRentThatAppliedThen() {
        val later = change(8_250L.rupees, 9_075L.rupees, BillingMonth.of(2027, 11))
        val changes = listOf(later, raise) // in any order
        val now = 9_075L.rupees

        assertEquals(7_500L.rupees, RentHistory.rentFor(BillingMonth.of(2026, 10), now, changes))
        assertEquals(8_250L.rupees, RentHistory.rentFor(BillingMonth.of(2027, 3), now, changes))
        assertEquals(9_075L.rupees, RentHistory.rentFor(BillingMonth.of(2027, 11), now, changes))
    }
}
