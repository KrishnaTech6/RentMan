package com.rentman.domain.billing

import app.cash.turbine.test
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.TestData
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.payments.FakePaymentRepository
import com.rentman.domain.payments.Payment
import com.rentman.domain.payments.PaymentId
import com.rentman.domain.payments.PaymentMode
import com.rentman.domain.tenancy.FakeAddressRepository
import com.rentman.domain.tenancy.FakeRenterRepository
import com.rentman.domain.tenancy.ObserveLeftRentersUseCase
import com.rentman.domain.tenancy.ObserveRenterSummariesUseCase
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.time.GetTodayUseCase
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class MonthsUseCasesTest {
    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-11-23T10:00:00Z")
    }
    private val september = BillingMonth.of(2026, 9)
    private val october = BillingMonth.of(2026, 10)
    private val november = BillingMonth.of(2026, 11)
    private val december = BillingMonth.of(2026, 12)

    private fun months(renters: FakeRenterRepository, bills: FakeBillRepository = FakeBillRepository(), payments: FakePaymentRepository = FakePaymentRepository()): ObserveMonthsUseCase {
        val addresses = FakeAddressRepository(listOf(TestData.address))
        return ObserveMonthsUseCase(
            ObserveRenterSummariesUseCase(renters, addresses),
            ObserveLeftRentersUseCase(renters, addresses),
            bills, payments, GetTodayUseCase(clock, TimeZone.UTC),
        )
    }

    private fun detail(renters: FakeRenterRepository, bills: FakeBillRepository = FakeBillRepository(), payments: FakePaymentRepository = FakePaymentRepository()): ObserveMonthDetailUseCase {
        val addresses = FakeAddressRepository(listOf(TestData.address))
        return ObserveMonthDetailUseCase(
            ObserveRenterSummariesUseCase(renters, addresses),
            ObserveLeftRentersUseCase(renters, addresses),
            bills, payments, GetTodayUseCase(clock, TimeZone.UTC),
        )
    }

    private fun bill(renterId: Long, month: BillingMonth, total: Long) = Bill(
        renterId = RenterId(renterId), month = month, dueOn = month.plusMonths(1).dueOn(5),
        lines = listOf(BillLine(BillLineKind.Rent, "Rent", total.rupees)),
    )

    private var nextPaymentId = 1L
    private fun payment(renterId: Long, amount: Long, on: LocalDate) =
        Payment(PaymentId(nextPaymentId++), RenterId(renterId), amount.rupees, on, PaymentMode.Upi)

    private fun <T> Outcome<T>.value() = (this as Outcome.Success).value

    @Test
    fun monthsRunNewestFirstFromTheFirstBillToThisMonthIncludingAMonthWithNoBill() = runTest {
        val renters = FakeRenterRepository(listOf(TestData.renter(firstBillMonth = september)))
        val bills = FakeBillRepository(listOf(bill(1, september, 5_000), bill(1, november, 6_000)))

        months(renters, bills)().test {
            val result = awaitItem().value()
            assertEquals(listOf(november, october, september), result.map { it.month })
            assertEquals(1, result[1].notBilled) // October was skipped
            assertEquals(6_000L.rupees, result[0].billed)
        }
    }

    @Test
    fun anEarlyBillForNextMonthGetsItsOwnRow() = runTest {
        val renters = FakeRenterRepository(listOf(TestData.renter(dueDay = 1)))
        val bills = FakeBillRepository(listOf(bill(1, november, 5_000), bill(1, december, 5_000)))

        months(renters, bills)().test {
            assertEquals(listOf(december, november), awaitItem().value().map { it.month })
        }
    }

    @Test
    fun noRentersAndNoBillsMeansNoMonths() = runTest {
        months(FakeRenterRepository())().test {
            assertEquals(emptyList(), awaitItem().value())
        }
    }

    @Test
    fun aRenterWhoLeftStillCountsInTheMonthsTheyWereBilled() = runTest {
        val left: Renter = TestData.renter(id = 2, name = "Neha").copy(active = false, leftOn = LocalDate(2026, 11, 20))
        val bills = FakeBillRepository(listOf(bill(2, november, 6_000)))
        val payments = FakePaymentRepository(listOf(payment(2, 6_000, LocalDate(2026, 11, 15))))

        months(FakeRenterRepository(listOf(left)), bills, payments)().test {
            assertEquals(6_000L.rupees, awaitItem().value().single().collected)
        }
    }

    @Test
    fun cashReceivedIsByTheDayOfThePaymentNotTheBill() = runTest {
        // Ravi pays 3,000 on 28 Oct and 5,000 on 20 Nov, both against November's 8,000 bill.
        val renters = FakeRenterRepository(listOf(TestData.renter(firstBillMonth = october)))
        val payments = FakePaymentRepository(listOf(payment(1, 3_000, LocalDate(2026, 10, 28)), payment(1, 5_000, LocalDate(2026, 11, 20))))

        months(renters, FakeBillRepository(listOf(bill(1, november, 8_000))), payments)().test {
            val result = awaitItem().value().associateBy { it.month }
            assertEquals(5_000L.rupees, result.getValue(november).cashIn)
            assertEquals(3_000L.rupees, result.getValue(october).cashIn)
            assertEquals(8_000L.rupees, result.getValue(november).collected) // settled against November's bill
        }
    }

    @Test
    fun aFailureInAnyPartIsAFailure() = runTest {
        months(FakeRenterRepository(listOf(TestData.renter()), failReads = true))().test {
            assertEquals(Outcome.Failure(DomainError.Storage("disk")), awaitItem())
        }
    }

    @Test
    fun theMonthShowsEveryRenterMostUrgentFirstWithNumbersThatMatchTheSummary() = runTest {
        val renters = FakeRenterRepository(
            listOf(TestData.renter(id = 1, name = "Ravi"), TestData.renter(id = 2, name = "Neha"), TestData.renter(id = 3, name = "Zoya", firstBillMonth = october)),
        )
        // Neha has paid, Ravi has not, Zoya has no bill. In October (due 5 Nov, so past due) Ravi's bill is late.
        val bills = FakeBillRepository(listOf(bill(1, october, 5_000), bill(2, october, 4_000)))
        val payments = FakePaymentRepository(listOf(payment(2, 4_000, LocalDate(2026, 10, 20))))

        detail(renters, bills, payments)(october).test {
            val result = awaitItem().value()
            assertEquals(listOf("Ravi", "Zoya", "Neha"), result.rows.map { it.summary.renter.name })
            assertEquals(listOf(RenterStatus.Late, RenterStatus.NoBillYet, RenterStatus.Paid), result.rows.map { it.status })
            assertEquals(result.summary.collected, BillCalculator.total(result.rows.map { it.collected }))
            assertEquals(1, result.summary.notBilled)
        }
    }

    @Test
    fun aRenterWhoLeftWithoutABillHasNoRowButOneWithABillDoes() = runTest {
        val left1 = TestData.renter(id = 2, name = "Neha").copy(active = false, leftOn = LocalDate(2026, 11, 20))
        val left2 = TestData.renter(id = 3, name = "Zoya").copy(active = false, leftOn = LocalDate(2026, 11, 20))
        val renters = FakeRenterRepository(listOf(TestData.renter(), left1, left2))
        val bills = FakeBillRepository(listOf(bill(1, november, 5_000), bill(3, november, 6_000)))

        detail(renters, bills)(november).test {
            assertEquals(setOf("Ravi Kumar", "Zoya"), awaitItem().value().rows.map { it.summary.renter.name }.toSet())
        }
    }
}
