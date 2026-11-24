package com.rentman.domain.tenancy

import app.cash.turbine.test
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.TestData
import com.rentman.domain.billing.Bill
import com.rentman.domain.billing.BillLine
import com.rentman.domain.billing.BillLineKind
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.billing.FakeBillRepository
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.payments.FakePaymentRepository
import com.rentman.domain.payments.Payment
import com.rentman.domain.payments.PaymentId
import com.rentman.domain.payments.PaymentMode
import com.rentman.domain.time.GetTodayUseCase
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class RenterLeavingUseCasesTest {
    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-12-10T10:00:00Z")
    }
    private val today = GetTodayUseCase(clock, TimeZone.UTC)
    private val ravi = TestData.renter(id = 1, name = "Ravi Kumar").copy(unit = "Ground floor")

    private val november = BillingMonth.of(2026, 11)
    private val december = BillingMonth.of(2026, 12) // the month of "today"

    private fun bill(total: Paise, month: BillingMonth = december) = Bill(
        renterId = RenterId(1),
        month = month,
        dueOn = month.plusMonths(1).dueOn(5),
        lines = listOf(BillLine(BillLineKind.Rent, "Rent", total)),
    )

    /** The bills of Ravi, whose first bill month is November, up to December: nothing is left unbilled. */
    private fun upToDate(total: Paise) = listOf(bill(total, november), bill(total, december))

    private fun payment(amount: Paise) = Payment(PaymentId(1), RenterId(1), amount, LocalDate(2026, 12, 1), PaymentMode.Upi)

    private fun markLeft(
        renters: FakeRenterRepository,
        bills: List<Bill> = emptyList(),
        payments: List<Payment> = emptyList(),
    ) = MarkRenterLeftUseCase(renters, FakeBillRepository(bills), FakePaymentRepository(payments), today)

    @Test
    fun aRenterWhoOwesNothingCanBeMarkedAsLeftOnTodaysDate() = runTest {
        val renters = FakeRenterRepository(listOf(ravi))

        val result = markLeft(renters, upToDate(3_750L.rupees), listOf(payment(7_500L.rupees)))(ravi.id)

        assertEquals(Outcome.Success(Unit), result)
        assertEquals(false, renters.saved.single().active)
        assertEquals(LocalDate(2026, 12, 10), renters.saved.single().leftOn)
    }

    @Test
    fun aRenterWhoPaidAheadCanLeaveToo() = runTest {
        assertEquals(
            Outcome.Success(Unit),
            markLeft(FakeRenterRepository(listOf(ravi)), upToDate(3_750L.rupees), listOf(payment(9_000L.rupees)))(ravi.id),
        )
    }

    @Test
    fun aRenterWhoStillOwesMoneyIsNotMarkedAsLeftAndTheAmountIsReported() = runTest {
        val renters = FakeRenterRepository(listOf(ravi))

        val result = markLeft(renters, upToDate(3_750L.rupees), listOf(payment(5_000L.rupees)))(ravi.id)

        assertEquals(Outcome.Failure(DomainError.StillOwes(2_500L.rupees)), result)
        assertEquals(true, renters.saved.single().active)
        assertEquals(null, renters.saved.single().leftOn)
    }

    @Test
    fun anotherRentersBillsAndPaymentsDoNotCount() = runTest {
        val neha = TestData.renter(id = 2, name = "Neha Verma")
        val renters = FakeRenterRepository(listOf(ravi, neha))
        val nehaOwes = bill(9_000L.rupees).copy(renterId = RenterId(2))

        assertEquals(Outcome.Success(Unit), markLeft(renters, upToDate(0L.rupees) + nehaOwes)(ravi.id))
    }

    @Test
    fun aMonthThatWasNeverBilledMustBeBilledBeforeTheRenterCanLeave() = runTest {
        val renters = FakeRenterRepository(listOf(ravi))
        // November is billed and paid, but there is no December bill yet and it is 10 December.
        val result = markLeft(renters, listOf(bill(7_500L.rupees, november)), listOf(payment(7_500L.rupees)))(ravi.id)

        assertEquals(Outcome.Failure(DomainError.UnbilledMonth(december)), result)
        assertEquals(true, renters.saved.single().active)
    }

    @Test
    fun theOldestMonthWithoutABillIsTheOneReported() = runTest {
        val result = markLeft(FakeRenterRepository(listOf(ravi)), listOf(bill(7_500L.rupees, december)))(ravi.id) // Nov missing

        assertEquals(Outcome.Failure(DomainError.UnbilledMonth(november)), result)
    }

    @Test
    fun aRenterWithNoBillsAtAllCannotLeaveEitherBecauseTheirMonthsAreUnbilled() = runTest {
        assertEquals(Outcome.Failure(DomainError.UnbilledMonth(november)), markLeft(FakeRenterRepository(listOf(ravi)))(ravi.id))
    }

    @Test
    fun anUnbilledMonthIsReportedBeforeWhatIsOwed() = runTest {
        // Owes 7,500 for November and December is not billed: the bill comes first, then the payment.
        val result = markLeft(FakeRenterRepository(listOf(ravi)), listOf(bill(7_500L.rupees, november)))(ravi.id)

        assertEquals(Outcome.Failure(DomainError.UnbilledMonth(december)), result)
    }

    @Test
    fun anUnknownRenterCannotBeMarkedAsLeft() = runTest {
        assertEquals(Outcome.Failure(DomainError.NotFound), markLeft(FakeRenterRepository())(RenterId(5)))
    }

    @Test
    fun failuresReadingOrWritingAreReported() = runTest {
        assertEquals(
            Outcome.Failure(DomainError.Storage("disk")),
            markLeft(FakeRenterRepository(listOf(ravi), failWrites = true), upToDate(0L.rupees))(ravi.id),
        )
        val unreadable = MarkRenterLeftUseCase(FakeRenterRepository(listOf(ravi)), FakeBillRepository(failReads = true), FakePaymentRepository(), today)
        assertEquals(Outcome.Failure(DomainError.Storage("disk")), unreadable(ravi.id))
    }

    @Test
    fun movingBackPutsTheRenterBackAmongTheActiveOnes() = runTest {
        val left = ravi.copy(active = false, leftOn = LocalDate(2026, 12, 10))
        val renters = FakeRenterRepository(listOf(left))

        assertEquals(Outcome.Success(Unit), MoveRenterBackUseCase(renters)(left.id))

        assertEquals(true, renters.saved.single().active)
        assertEquals(null, renters.saved.single().leftOn)
    }

    @Test
    fun theyCannotMoveBackIntoAUnitSomeoneElseNowHas() = runTest {
        val left = ravi.copy(active = false, leftOn = LocalDate(2026, 12, 10))
        val newcomer = TestData.renter(id = 2, name = "Neha Verma").copy(unit = "ground FLOOR")
        val renters = FakeRenterRepository(listOf(left, newcomer))

        assertEquals(Outcome.Failure(DomainError.UnitTaken), MoveRenterBackUseCase(renters)(left.id))
        assertEquals(false, renters.saved.first { it.id == left.id }.active)
    }

    @Test
    fun aRenterWithNoUnitCanMoveBackOnlyIfNobodyElseLivesAtTheAddress() = runTest {
        val left = ravi.copy(unit = null, active = false, leftOn = LocalDate(2026, 12, 10))

        assertEquals(Outcome.Success(Unit), MoveRenterBackUseCase(FakeRenterRepository(listOf(left)))(left.id))
    }

    @Test
    fun aRenterWithNoUnitCannotMoveBackNextToSomeoneElse() = runTest {
        // Adding or editing needs a unit when somebody else lives there, and so does moving back.
        val left = ravi.copy(unit = null, active = false, leftOn = LocalDate(2026, 12, 10))
        val renters = FakeRenterRepository(listOf(left, TestData.renter(id = 2, name = "Neha Verma").copy(unit = null)))

        assertEquals(Outcome.Failure(DomainError.UnitTaken), MoveRenterBackUseCase(renters)(left.id))
        assertEquals(false, renters.saved.first { it.id == left.id }.active)
    }

    @Test
    fun aNeighbourAtAnotherAddressDoesNotBlockMovingBack() = runTest {
        val left = ravi.copy(unit = null, active = false, leftOn = LocalDate(2026, 12, 10))
        val elsewhere = TestData.renter(id = 2, name = "Neha Verma").copy(unit = null, addressId = AddressId(2))

        assertEquals(Outcome.Success(Unit), MoveRenterBackUseCase(FakeRenterRepository(listOf(left, elsewhere)))(left.id))
    }

    @Test
    fun anUnknownRenterIsNotFound() = runTest {
        assertEquals(Outcome.Failure(DomainError.NotFound), MoveRenterBackUseCase(FakeRenterRepository())(RenterId(5)))
    }

    @Test
    fun theLeftRentersAreListedWithTheirAddressAndTheActiveOnesAreNot() = runTest {
        val left = ravi.copy(active = false, leftOn = LocalDate(2026, 12, 10))
        val neha = TestData.renter(id = 2, name = "Neha Verma")
        val renters = FakeRenterRepository(listOf(left, neha))

        ObserveLeftRentersUseCase(renters, FakeAddressRepository(listOf(TestData.address)))().test {
            assertEquals(Outcome.Success(listOf(RenterSummary(left, TestData.address))), awaitItem())
        }
    }

    @Test
    fun theLeftListIsEmptyWhenNobodyLeftAndReportsFailures() = runTest {
        ObserveLeftRentersUseCase(FakeRenterRepository(listOf(ravi)), FakeAddressRepository(listOf(TestData.address)))().test {
            assertEquals(Outcome.Success(emptyList()), awaitItem())
        }
        ObserveLeftRentersUseCase(FakeRenterRepository(listOf(ravi), failReads = true), FakeAddressRepository(listOf(TestData.address)))().test {
            assertEquals(Outcome.Failure(DomainError.Storage("disk")), awaitItem())
        }
    }
}
