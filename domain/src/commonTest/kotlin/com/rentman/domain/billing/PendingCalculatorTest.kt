package com.rentman.domain.billing

import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.RenterId
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class PendingCalculatorTest {
    @Test
    fun pendingIsBillsMinusPayments() {
        // Ravi: bill 5000 + 680 + 200 = 5880, pays 4000 -> pending 1880
        val billTotals = listOf(Paise(588_000))
        val payments = listOf(4_000L.rupees)
        assertEquals(Paise(188_000), PendingCalculator.pending(billTotals, payments))
    }

    @Test
    fun partialPaymentsCarryIntoTheNextMonth() {
        // Dec bill 5920 on top of 1880 pending -> 7800
        val billTotals = listOf(Paise(588_000), Paise(592_000))
        val payments = listOf(4_000L.rupees)
        assertEquals(Paise(780_000), PendingCalculator.pending(billTotals, payments))
    }

    @Test
    fun overpaymentIsANegativeBalanceNotZero() {
        assertEquals(Paise(-10_000), PendingCalculator.pending(listOf(1_000L.rupees), listOf(1_100L.rupees)))
    }

    @Test
    fun nothingBilledNothingPaidIsZero() {
        assertEquals(Paise.Zero, PendingCalculator.pending(emptyList(), emptyList()))
    }

    private val today = LocalDate(2026, 11, 28)

    private fun bill(month: BillingMonth, amount: Paise) = Bill(
        renterId = RenterId(1),
        month = month,
        createdOn = month.atStartOfMonth(),
        dueOn = month.plusMonths(1).dueOn(15),
        lines = listOf(BillLine(BillLineKind.Rent, "Rent", amount)),
    )

    private val october = bill(BillingMonth.of(2026, 10), 8_000L.rupees) // due 15 Nov: past due on 28 Nov
    private val november = bill(BillingMonth.of(2026, 11), 5_000L.rupees) // due 15 Dec: not due yet

    @Test
    fun anOldUnpaidBillIsOverdueEvenWhenANewerBillIsNotDueYet() {
        assertEquals(8_000L.rupees, PendingCalculator.overdue(listOf(october, november), emptyList(), today))
    }

    @Test
    fun paymentsSettleTheOldestBillFirst() {
        assertEquals(3_000L.rupees, PendingCalculator.overdue(listOf(october, november), listOf(5_000L.rupees), today))
        assertEquals(Paise.Zero, PendingCalculator.overdue(listOf(october, november), listOf(9_000L.rupees), today))
    }

    @Test
    fun overdueIsNeverMoreThanPendingOrBelowZero() {
        assertEquals(Paise.Zero, PendingCalculator.overdue(listOf(october), listOf(10_000L.rupees), today))
        assertEquals(Paise.Zero, PendingCalculator.overdue(emptyList(), emptyList(), today))
    }

    @Test
    fun aBillIsNotOverdueOnItsDueDayItself() {
        assertEquals(Paise.Zero, PendingCalculator.overdue(listOf(october), emptyList(), LocalDate(2026, 11, 15)))
        assertEquals(8_000L.rupees, PendingCalculator.overdue(listOf(october), emptyList(), LocalDate(2026, 11, 16)))
    }

    private val september = bill(BillingMonth.of(2026, 9), 4_000L.rupees) // due 15 Oct: past due on 28 Nov

    @Test
    fun overdueSinceIsNothingWhenNothingIsOverdue() {
        assertEquals(null, PendingCalculator.overdueSince(listOf(november), emptyList(), today))
        assertEquals(null, PendingCalculator.overdueSince(listOf(october), listOf(8_000L.rupees), today)) // paid in full
        assertEquals(null, PendingCalculator.overdueSince(emptyList(), emptyList(), today))
    }

    @Test
    fun overdueSinceIsTheDueDateOfTheOldestBillStillOwedAndPastDue() {
        assertEquals(LocalDate(2026, 11, 15), PendingCalculator.overdueSince(listOf(october, november), emptyList(), today))
        // A part payment leaves the oldest bill still owed.
        assertEquals(LocalDate(2026, 10, 15), PendingCalculator.overdueSince(listOf(september, october), listOf(1_000L.rupees), today))
    }

    @Test
    fun overdueSinceMovesOnWhenTheOldestBillIsPaid() {
        assertEquals(LocalDate(2026, 11, 15), PendingCalculator.overdueSince(listOf(september, october), listOf(4_000L.rupees), today))
    }
}
