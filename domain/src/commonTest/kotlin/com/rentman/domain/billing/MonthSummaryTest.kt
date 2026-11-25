package com.rentman.domain.billing

import com.rentman.domain.TestData
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.payments.Payment
import com.rentman.domain.payments.PaymentId
import com.rentman.domain.payments.PaymentMode
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.tenancy.RenterSummary
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class MonthSummaryTest {
    private val today = LocalDate(2026, 11, 24)
    private val october = BillingMonth.of(2026, 10)
    private val november = BillingMonth.of(2026, 11)
    private val december = BillingMonth.of(2026, 12)

    private fun bill(renterId: Long, month: BillingMonth, total: Long, dueOn: LocalDate = month.dueOn(5)) = Bill(
        renterId = RenterId(renterId),
        month = month,
        dueOn = dueOn,
        lines = listOf(BillLine(BillLineKind.Rent, "Rent", total.rupees)),
    )

    private var nextPaymentId = 1L
    private fun payment(renterId: Long, amount: Long, on: LocalDate = LocalDate(2026, 11, 20)) =
        Payment(PaymentId(nextPaymentId++), RenterId(renterId), amount.rupees, on, PaymentMode.Upi)

    private fun summary(renters: List<Renter>, bills: List<Bill>, payments: List<Payment> = emptyList()) =
        MonthSummary.of(november, today, renters.map { RenterSummary(it, TestData.address) }, bills, payments)

    private val ravi = TestData.renter(id = 1)

    @Test
    fun aBillNobodyHasPaidIsLateOnceItsDueDayHasPassedAndNotDueYetBefore() {
        val notDue = summary(listOf(ravi), listOf(bill(1, november, 8_000, dueOn = LocalDate(2026, 12, 5))))
        val late = summary(listOf(ravi), listOf(bill(1, november, 8_000, dueOn = LocalDate(2026, 11, 5))))

        assertEquals(MonthSummary(november, Paise.Zero, Paise.Zero, 8_000L.rupees, 0, Paise.Zero, Paise.Zero), notDue)
        assertEquals(MonthSummary(november, Paise.Zero, 8_000L.rupees, Paise.Zero, 0, Paise.Zero, Paise.Zero), late)
    }

    @Test
    fun whatWasPaidIsCollectedAndTheRestIsLeft() {
        val result = summary(listOf(ravi), listOf(bill(1, november, 8_000, dueOn = LocalDate(2026, 12, 5))), listOf(payment(1, 5_000)))

        assertEquals(5_000L.rupees, result.collected)
        assertEquals(3_000L.rupees, result.notDue)
        assertEquals(8_000L.rupees, result.billed)
    }

    @Test
    fun anOlderBillIsSettledBeforeThisMonthsAndWhatIsStillOwedOnItIsShownApart() {
        val bills = listOf(bill(1, october, 5_000), bill(1, november, 8_000, dueOn = LocalDate(2026, 12, 5)))

        val octoberPaidOffAndSomeMore = summary(listOf(ravi), bills, listOf(payment(1, 6_000)))
        val octoberPartlyPaid = summary(listOf(ravi), bills, listOf(payment(1, 3_000)))

        assertEquals(1_000L.rupees, octoberPaidOffAndSomeMore.collected)
        assertEquals(Paise.Zero, octoberPaidOffAndSomeMore.earlier)
        assertEquals(Paise.Zero, octoberPartlyPaid.collected)
        assertEquals(2_000L.rupees, octoberPartlyPaid.earlier)
    }

    @Test
    fun payingAheadNeverCollectsMoreThanTheBill() {
        val result = summary(listOf(ravi), listOf(bill(1, november, 8_000)), listOf(payment(1, 9_000)))

        assertEquals(8_000L.rupees, result.collected)
        assertEquals(Paise.Zero, result.late + result.notDue)
    }

    @Test
    fun aBillMadeAndPaidEarlyForNextMonthLeavesThisMonthAlone() {
        // November is paid (8,000) and December's bill, made at the end of November, is paid too (8,000 more).
        val result = summary(listOf(ravi), listOf(bill(1, november, 8_000), bill(1, december, 8_000)), listOf(payment(1, 16_000)))

        assertEquals(8_000L.rupees, result.collected)
        assertEquals(8_000L.rupees, result.billed)
        assertEquals(0, result.notBilled)
    }

    @Test
    fun aRenterWithNoBillThisMonthCountsAsNotBilledWithOneMonthsRent() {
        val neha = TestData.renter(id = 2, name = "Neha", rent = 6_000L.rupees)
        val startsInDecember = TestData.renter(id = 3, name = "Zoya", firstBillMonth = december)

        val result = summary(listOf(ravi, neha, startsInDecember), listOf(bill(1, november, 8_000)))

        assertEquals(1, result.notBilled) // Neha. Zoya's first bill is December's.
        assertEquals(6_000L.rupees, result.notBilledRent)
        assertEquals(14_000L.rupees, result.expected)
    }

    @Test
    fun noRentersMeansAllZero() {
        assertEquals(MonthSummary(november, Paise.Zero, Paise.Zero, Paise.Zero, 0, Paise.Zero, Paise.Zero), summary(emptyList(), emptyList()))
    }

    @Test
    fun aRenterWhoLeftStillCountsTheBillTheyPaidThisMonth() {
        val left = TestData.renter(id = 2, name = "Neha").copy(active = false, leftOn = LocalDate(2026, 11, 20))

        val result = summary(listOf(ravi, left), listOf(bill(1, november, 8_000), bill(2, november, 6_000)), listOf(payment(2, 6_000)))

        assertEquals(6_000L.rupees, result.collected)
        assertEquals(14_000L.rupees, result.billed)
    }

    @Test
    fun aRenterWhoLeftWithNoBillIsNotCountedAsNotBilled() {
        val left = TestData.renter(id = 2, name = "Neha").copy(active = false, leftOn = LocalDate(2026, 11, 20))

        val result = summary(listOf(ravi, left), listOf(bill(1, november, 8_000)))

        assertEquals(0, result.notBilled)
        assertEquals(Paise.Zero, result.notBilledRent)
    }

    @Test
    fun cashReceivedCountsEveryPaymentDatedInTheMonthWhateverBillItWentTo() {
        val bills = listOf(bill(1, november, 8_000))
        val payments = listOf(payment(1, 3_000, LocalDate(2026, 10, 28)), payment(1, 5_000, LocalDate(2026, 11, 20)), payment(1, 1_000, LocalDate(2026, 11, 22)))

        assertEquals(6_000L.rupees, summary(listOf(ravi), bills, payments).cashIn)
    }

    @Test
    fun collectedShareIsTheShareOfTheBillsSettledAndZeroWithNoBills() {
        assertEquals(0.625f, summary(listOf(ravi), listOf(bill(1, november, 8_000)), listOf(payment(1, 5_000))).collectedShare, 0.001f)
        assertEquals(0f, summary(listOf(ravi), emptyList()).collectedShare, 0.001f)
    }
}
