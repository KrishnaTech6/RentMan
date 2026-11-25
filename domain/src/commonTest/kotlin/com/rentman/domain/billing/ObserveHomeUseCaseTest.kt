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
import com.rentman.domain.tenancy.FakeRentIncreaseRepository
import com.rentman.domain.tenancy.ObserveLeftRentersUseCase
import com.rentman.domain.tenancy.ObserveRentIncreaseOffersUseCase
import com.rentman.domain.tenancy.ObserveRenterSummariesUseCase
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.tenancy.RenterSummary
import com.rentman.domain.time.GetTodayUseCase
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class ObserveHomeUseCaseTest {
    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-11-23T10:00:00Z")
    }
    private val november = BillingMonth.of(2026, 11)

    private fun useCase(
        renters: FakeRenterRepository,
        bills: FakeBillRepository = FakeBillRepository(),
        payments: FakePaymentRepository = FakePaymentRepository(),
        rentChanges: FakeRentIncreaseRepository = FakeRentIncreaseRepository(),
    ): ObserveHomeUseCase {
        val addresses = FakeAddressRepository(listOf(TestData.address))
        val today = GetTodayUseCase(clock, TimeZone.UTC)
        return ObserveHomeUseCase(
            ObserveRenterSummariesUseCase(renters, addresses),
            ObserveBillTodosUseCase(renters, addresses, bills, today),
            ObserveRentIncreaseOffersUseCase(renters, addresses, rentChanges, bills, today),
            ObserveLeftRentersUseCase(renters, addresses),
            bills,
            payments,
            today,
        )
    }

    private fun bill(
        renterId: Long,
        total: Paise,
        month: BillingMonth = november,
        createdOn: LocalDate = month.atStartOfMonth(),
    ) = Bill(
        renterId = RenterId(renterId),
        month = month,
        createdOn = createdOn,
        dueOn = month.plusMonths(1).dueOn(5),
        lines = listOf(BillLine(BillLineKind.Rent, "Rent", total)),
    )

    private var nextPaymentId = 1L
    private fun payment(renterId: Long, amount: Paise, on: LocalDate) =
        Payment(PaymentId(nextPaymentId++), RenterId(renterId), amount, on, PaymentMode.Upi)

    private fun Outcome<HomeSnapshot>.home() = (this as Outcome.Success).value

    @Test
    fun givesTheRentersAndWhatNeedsBillingTogether() = runTest {
        val ravi = TestData.renter()

        useCase(FakeRenterRepository(listOf(ravi)))().test {
            val home = awaitItem().home()
            assertEquals(listOf(RenterSummary(ravi, TestData.address)), home.renters.map { it.summary })
            assertEquals(listOf(november), home.todos.map { it.month })
        }
    }

    @Test
    fun noRentersMeansNothingToShowNothingToDoAndNothingToCollect() = runTest {
        useCase(FakeRenterRepository())().test {
            assertEquals(Outcome.Success(HomeSnapshot(renters = emptyList(), todos = emptyList(), raises = emptyList(), left = emptyList(), thisMonth = MonthSummary(november, Paise.Zero, Paise.Zero, Paise.Zero, 0, Paise.Zero, Paise.Zero))), awaitItem())
        }
    }

    @Test
    fun aRenterDueARaiseGetsAnOfferOthersDoNot() = runTest {
        // Today is 23 Nov 2026. An agreement from Dec 2025 is due its raise in Nov 2026; Ravi's (Jan 2026) is not.
        val due = TestData.renter(id = 2, name = "Neha", agreementStart = LocalDate(2025, 12, 1))

        useCase(FakeRenterRepository(listOf(TestData.renter(), due)))().test {
            assertEquals(listOf("Neha"), awaitItem().home().raises.map { it.summary.renter.name })
        }
    }

    @Test
    fun aRenterWhoLeftIsListedApartAndLeftOutOfEverythingElse() = runTest {
        val gone = TestData.renter(id = 2, name = "Neha").copy(active = false, leftOn = LocalDate(2026, 11, 1))

        useCase(FakeRenterRepository(listOf(TestData.renter(), gone)))().test {
            val home = awaitItem().home()
            assertEquals(listOf("Ravi Kumar"), home.renters.map { it.summary.renter.name })
            assertEquals(listOf("Neha"), home.left.map { it.renter.name })
            assertEquals(listOf(november), home.todos.map { it.month }) // only Ravi still needs a bill
        }
    }

    @Test
    fun aFailureInAnyPartIsAFailure() = runTest {
        useCase(FakeRenterRepository(listOf(TestData.renter()), failReads = true))().test {
            assertEquals(Outcome.Failure(DomainError.Storage("disk")), awaitItem())
        }
        useCase(FakeRenterRepository(listOf(TestData.renter())), payments = FakePaymentRepository(failReads = true))().test {
            assertEquals(Outcome.Failure(DomainError.Storage("disk")), awaitItem())
        }
    }

    @Test
    fun aRenterWithNoBillYetHasNothingPending() = runTest {
        useCase(FakeRenterRepository(listOf(TestData.renter())))().test {
            val row = awaitItem().home().renters.single()
            assertEquals(RenterStatus.NoBillYet, row.status)
            assertEquals(Paise.Zero, row.pending)
        }
    }

    @Test
    fun aBillWithNoPaymentIsUnpaid() = runTest {
        useCase(FakeRenterRepository(listOf(TestData.renter())), FakeBillRepository(listOf(bill(1, 8_380L.rupees))))().test {
            val row = awaitItem().home().renters.single()
            assertEquals(RenterStatus.Unpaid, row.status)
            assertEquals(8_380L.rupees, row.pending)
        }
    }

    @Test
    fun aPaymentSinceTheBillThatDoesNotCoverItIsPartPaid() = runTest {
        val payments = FakePaymentRepository(listOf(payment(1, 5_000L.rupees, LocalDate(2026, 11, 22))))

        useCase(FakeRenterRepository(listOf(TestData.renter())), FakeBillRepository(listOf(bill(1, 8_380L.rupees))), payments)().test {
            val row = awaitItem().home().renters.single()
            assertEquals(RenterStatus.PartPaid, row.status)
            assertEquals(3_380L.rupees, row.pending)
        }
    }

    @Test
    fun anOldPaymentFromBeforeTheBillDoesNotMakeItPartPaid() = runTest {
        val payments = FakePaymentRepository(listOf(payment(1, 5_000L.rupees, LocalDate(2026, 10, 20))))

        useCase(FakeRenterRepository(listOf(TestData.renter())), FakeBillRepository(listOf(bill(1, 8_380L.rupees))), payments)().test {
            assertEquals(RenterStatus.Unpaid, awaitItem().home().renters.single().status)
        }
    }

    @Test
    fun aPaymentForLastMonthsBillDoesNotMakeThisMonthsBillPartPaid() = runTest {
        // October's 5,000 was created on 1 Nov and paid in full on 3 Nov. November's bill came on 4 Nov.
        val bills = FakeBillRepository(
            listOf(
                bill(1, 5_000L.rupees, BillingMonth.of(2026, 10), createdOn = LocalDate(2026, 11, 1)),
                bill(1, 5_000L.rupees, november, createdOn = LocalDate(2026, 11, 4)),
            ),
        )
        val payments = FakePaymentRepository(listOf(payment(1, 5_000L.rupees, LocalDate(2026, 11, 3))))

        useCase(FakeRenterRepository(listOf(TestData.renter())), bills, payments)().test {
            val row = awaitItem().home().renters.single()
            assertEquals(RenterStatus.Unpaid, row.status)
            assertEquals(5_000L.rupees, row.pending)
        }
    }

    @Test
    fun aBillPastItsDueDateWithMoneyOwedIsLateEvenIfPartPaid() = runTest {
        val renters = FakeRenterRepository(listOf(TestData.renter(id = 1), TestData.renter(id = 2, name = "Neha"), TestData.renter(id = 3, name = "Zoya")))
        val september = BillingMonth.of(2026, 9) // due 5 Oct, today is 23 Nov
        val bills = FakeBillRepository(
            listOf(bill(1, 5_000L.rupees, september), bill(2, 5_000L.rupees, september), bill(3, 5_000L.rupees, september)),
        )
        val payments = FakePaymentRepository(
            listOf(payment(2, 1_000L.rupees, LocalDate(2026, 10, 20)), payment(3, 5_000L.rupees, LocalDate(2026, 10, 3))),
        )

        useCase(renters, bills, payments)().test {
            val status = awaitItem().home().renters.associate { it.summary.renter.name to it.status }
            assertEquals(RenterStatus.Late, status.getValue("Ravi Kumar"))
            assertEquals(RenterStatus.Late, status.getValue("Neha"))
            assertEquals(RenterStatus.Paid, status.getValue("Zoya")) // paid in full, so never late
        }
    }

    @Test
    fun aBillIsNotLateOnItsDueDayItself() = runTest {
        // Today is 23 Nov. The October bill is due 5 Nov (late); a bill due today is not.
        val dueToday = bill(1, 5_000L.rupees, BillingMonth.of(2026, 10)).copy(dueOn = LocalDate(2026, 11, 23))

        useCase(FakeRenterRepository(listOf(TestData.renter())), FakeBillRepository(listOf(dueToday)))().test {
            assertEquals(RenterStatus.Unpaid, awaitItem().home().renters.single().status)
        }
    }

    @Test
    fun payingInFullIsPaidAndPayingAheadIsAlsoPaid() = runTest {
        val renters = FakeRenterRepository(listOf(TestData.renter(id = 1), TestData.renter(id = 2, name = "Neha")))
        val bills = FakeBillRepository(listOf(bill(1, 5_000L.rupees), bill(2, 5_000L.rupees)))
        val payments = FakePaymentRepository(
            listOf(payment(1, 5_000L.rupees, LocalDate(2026, 11, 5)), payment(2, 6_000L.rupees, LocalDate(2026, 11, 5))),
        )

        useCase(renters, bills, payments)().test {
            val rows = awaitItem().home().renters.associateBy { it.summary.renter.name }
            assertEquals(RenterStatus.Paid to Paise.Zero, rows.getValue("Ravi Kumar").let { it.status to it.pending })
            assertEquals(RenterStatus.Paid to Paise(-100_000), rows.getValue("Neha").let { it.status to it.pending })
        }
    }

    @Test
    fun thisMonthAddsUpNovembersBillsAndWhatWasPaidAgainstThem() = runTest {
        val renters = FakeRenterRepository(
            listOf(TestData.renter(id = 1, name = "Amit"), TestData.renter(id = 2, name = "Neha"), TestData.renter(id = 3, name = "Zoya")),
        )
        // Amit still owes October's 8,000 and has a November bill of 5,000. Neha has paid 2,000 of her 5,000. Zoya has no November bill.
        val bills = FakeBillRepository(listOf(bill(1, 8_000L.rupees, month = BillingMonth.of(2026, 10)), bill(1, 5_000L.rupees), bill(2, 5_000L.rupees)))
        val payments = FakePaymentRepository(listOf(payment(2, 2_000L.rupees, LocalDate(2026, 11, 10))))

        useCase(renters, bills, payments)().test {
            val month = awaitItem().home().thisMonth
            assertEquals(2_000L.rupees, month.collected)
            assertEquals(8_000L.rupees, month.notDue) // November's bills are due on 5 Dec: Amit's 5,000 and Neha's 3,000
            assertEquals(8_000L.rupees, month.earlier) // October's, shown apart
            assertEquals(1, month.notBilled)
        }
    }

    @Test
    fun otherRentersPaymentsDoNotCount() = runTest {
        val payments = FakePaymentRepository(listOf(payment(2, 8_380L.rupees, LocalDate(2026, 11, 10))))

        useCase(FakeRenterRepository(listOf(TestData.renter())), FakeBillRepository(listOf(bill(1, 8_380L.rupees))), payments)().test {
            assertEquals(Paise.Zero, awaitItem().home().thisMonth.collected)
        }
    }

    @Test
    fun anOldUnpaidBillMakesTheRenterLateEvenWhenTheNewestBillIsNotDue() = runTest {
        // October's bill was due on 5 Nov and is unpaid. November's (due 5 Dec) was made since. Today is 23 Nov.
        val bills = FakeBillRepository(listOf(bill(1, 8_000L.rupees, month = BillingMonth.of(2026, 10)), bill(1, 5_000L.rupees)))

        useCase(FakeRenterRepository(listOf(TestData.renter())), bills)().test {
            val row = awaitItem().home().renters.single()
            assertEquals(RenterStatus.Late, row.status)
            assertEquals(18, row.daysLate) // from 5 Nov, the oldest bill still owed
            assertEquals(LocalDate(2026, 12, 5), row.dueOn) // the newest bill's own date
        }
    }

    @Test
    fun onceTheOldBillIsPaidTheRenterIsNoLongerLate() = runTest {
        val bills = FakeBillRepository(listOf(bill(1, 8_000L.rupees, month = BillingMonth.of(2026, 10)), bill(1, 5_000L.rupees)))
        val payments = FakePaymentRepository(listOf(payment(1, 8_000L.rupees, LocalDate(2026, 10, 20))))

        useCase(FakeRenterRepository(listOf(TestData.renter())), bills, payments)().test {
            val row = awaitItem().home().renters.single()
            assertEquals(RenterStatus.Unpaid, row.status)
            assertEquals(0, row.daysLate)
        }
    }

    @Test
    fun aRenterWhoLeftStaysInThisMonthsIncome() = runTest {
        val left = TestData.renter(id = 2, name = "Neha").copy(active = false, leftOn = LocalDate(2026, 11, 20))
        val bills = FakeBillRepository(listOf(bill(1, 8_000L.rupees), bill(2, 6_000L.rupees)))
        val payments = FakePaymentRepository(listOf(payment(2, 6_000L.rupees, LocalDate(2026, 11, 15))))

        useCase(FakeRenterRepository(listOf(TestData.renter(), left)), bills, payments)().test {
            val month = awaitItem().home().thisMonth
            assertEquals(6_000L.rupees, month.collected)
            assertEquals(14_000L.rupees, month.billed)
        }
    }
}
