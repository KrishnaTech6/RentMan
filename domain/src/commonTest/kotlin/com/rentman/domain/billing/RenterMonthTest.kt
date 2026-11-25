package com.rentman.domain.billing

import com.rentman.domain.TestData
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.payments.Payment
import com.rentman.domain.payments.PaymentId
import com.rentman.domain.payments.PaymentMode
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.tenancy.RenterSummary
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RenterMonthTest {
    private val today = LocalDate(2026, 11, 24)
    private val october = BillingMonth.of(2026, 10)
    private val november = BillingMonth.of(2026, 11)
    private val ravi = RenterSummary(TestData.renter(id = 1), TestData.address)

    private fun bill(month: BillingMonth, total: Long, dueOn: LocalDate = month.plusMonths(1).dueOn(5)) = Bill(
        renterId = RenterId(1), month = month, dueOn = dueOn,
        lines = listOf(BillLine(BillLineKind.Rent, "Rent", total.rupees)),
    )

    private var nextPaymentId = 1L
    private fun payment(amount: Long) = Payment(PaymentId(nextPaymentId++), RenterId(1), amount.rupees, LocalDate(2026, 11, 20), PaymentMode.Upi)

    private fun novemberOf(bills: List<Bill>, payments: List<Payment> = emptyList(), summary: RenterSummary = ravi) =
        RenterMonth.of(summary, november, today, bills, payments)

    @Test
    fun aPartPaidBillSaysHowMuchCameInAndHowMuchIsLeft() {
        val row = assertNotNull(novemberOf(listOf(bill(november, 8_000)), listOf(payment(5_000))))

        assertEquals(8_000L.rupees, row.billed)
        assertEquals(5_000L.rupees, row.collected)
        assertEquals(3_000L.rupees, row.left)
        assertEquals(RenterStatus.PartPaid, row.status)
    }

    @Test
    fun theStatusFollowsThisMonthsBill() {
        val notDueYet = novemberOf(listOf(bill(november, 8_000)))
        val pastDue = novemberOf(listOf(bill(november, 8_000, dueOn = LocalDate(2026, 11, 5))))
        val paid = novemberOf(listOf(bill(november, 8_000)), listOf(payment(8_000)))

        assertEquals(RenterStatus.Unpaid, notDueYet?.status)
        assertEquals(RenterStatus.Late, pastDue?.status)
        assertEquals(8_000L.rupees, pastDue?.late)
        assertEquals(RenterStatus.Paid, paid?.status)
    }

    @Test
    fun aPaymentSettlesTheOldestBillFirstSoOctoberCanBePaidWhileNovemberIsUnpaid() {
        // On Home this renter is Part paid (a payment came after the newest bill). For November alone nothing of it is settled.
        val bills = listOf(bill(october, 5_000), bill(november, 8_000))
        val payments = listOf(payment(5_000))

        assertEquals(RenterStatus.Paid, RenterMonth.of(ravi, october, today, bills, payments)?.status)
        assertEquals(RenterStatus.Unpaid, novemberOf(bills, payments)?.status)
        assertEquals(Paise.Zero, novemberOf(bills, payments)?.collected)
    }

    @Test
    fun payingAheadNeverSettlesMoreThanTheBill() {
        val row = assertNotNull(novemberOf(listOf(bill(november, 8_000)), listOf(payment(9_000))))

        assertEquals(8_000L.rupees, row.collected)
        assertEquals(Paise.Zero, row.left)
    }

    @Test
    fun anActiveRenterWithNoBillThatMonthHasANoBillRow() {
        val row = assertNotNull(novemberOf(emptyList()))

        assertEquals(RenterStatus.NoBillYet, row.status)
        assertEquals(Paise.Zero, row.billed)
    }

    @Test
    fun aRenterWhoLeftWithNoBillOrWhoStartsLaterHasNoRowThatMonth() {
        val left = RenterSummary(TestData.renter(id = 2).copy(active = false, leftOn = LocalDate(2026, 10, 31)), TestData.address)
        val startsLater = RenterSummary(TestData.renter(id = 3, firstBillMonth = BillingMonth.of(2026, 12)), TestData.address)

        assertNull(novemberOf(emptyList(), summary = left))
        assertNull(novemberOf(emptyList(), summary = startsLater))
    }

    @Test
    fun aRenterWhoLeftStillHasARowForABillTheyWereGiven() {
        val left = ravi.copy(renter = ravi.renter.copy(active = false, leftOn = LocalDate(2026, 11, 20)))

        val row = assertNotNull(novemberOf(listOf(bill(november, 8_000)), listOf(payment(8_000)), left))

        assertEquals(RenterStatus.Paid, row.status)
    }

    @Test
    fun whatIsStillOwedOnEarlierBillsIsKnownApart() {
        val bills = listOf(bill(october, 5_000), bill(november, 8_000))

        assertEquals(5_000L.rupees, RenterMonth.earlierOwed(ravi, november, bills, emptyList()))
        assertEquals(2_000L.rupees, RenterMonth.earlierOwed(ravi, november, bills, listOf(payment(3_000))))
        assertEquals(Paise.Zero, RenterMonth.earlierOwed(ravi, november, bills, listOf(payment(6_000))))
    }
}
