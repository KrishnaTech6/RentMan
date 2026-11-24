package com.rentman.domain.billing

import app.cash.turbine.test
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.TestData
import com.rentman.domain.money.rupees
import com.rentman.domain.payments.FakePaymentRepository
import com.rentman.domain.payments.ObserveRenterLedgerUseCase
import com.rentman.domain.payments.RenterLedger
import com.rentman.domain.tenancy.FakeAddressRepository
import com.rentman.domain.tenancy.FakeRenterRepository
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import com.rentman.domain.time.GetTodayUseCase
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Instant

class RemoveBillUseCaseTest {
    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-11-23T10:00:00Z")
    }
    private val october = BillingMonth.of(2026, 10)
    private val november = BillingMonth.of(2026, 11)
    private val ravi = TestData.renter(id = 1, firstBillMonth = october)
    private val gone = ravi.copy(active = false, leftOn = LocalDate(2026, 11, 20))

    private fun bill(id: Long, month: BillingMonth) = Bill(
        id = BillId(id),
        renterId = RenterId(1),
        month = month,
        createdOn = month.atStartOfMonth(),
        dueOn = month.plusMonths(1).dueOn(5),
        lines = listOf(BillLine(BillLineKind.Rent, "Rent", 7_500L.rupees)),
    )

    private val octoberBill = bill(1, october)
    private val novemberBill = bill(2, november)

    @Test
    fun theNewestBillOfARenterWhoLivesThereIsRemoved() = runTest {
        val bills = FakeBillRepository(listOf(octoberBill, novemberBill))

        assertEquals(Outcome.Success(Unit), RemoveBillUseCase(bills, FakeRenterRepository(listOf(ravi)))(novemberBill))

        assertEquals(listOf(octoberBill), bills.saved)
    }

    @Test
    fun anOlderBillIsNotRemovedBecauseTheNextOnesReadingStartsFromIt() = runTest {
        val bills = FakeBillRepository(listOf(octoberBill, novemberBill))

        assertEquals(
            Outcome.Failure(DomainError.BillNotRemovable),
            RemoveBillUseCase(bills, FakeRenterRepository(listOf(ravi)))(octoberBill),
        )

        assertEquals(listOf(octoberBill, novemberBill), bills.saved)
    }

    @Test
    fun aRenterWhoLeftKeepsAllTheirBills() = runTest {
        val bills = FakeBillRepository(listOf(novemberBill))

        assertEquals(Outcome.Failure(DomainError.BillNotRemovable), RemoveBillUseCase(bills, FakeRenterRepository(listOf(gone)))(novemberBill))

        assertEquals(listOf(novemberBill), bills.saved)
    }

    @Test
    fun aBillThatIsNotThereIsNotFound() = runTest {
        val bills = FakeBillRepository(listOf(octoberBill))

        assertEquals(Outcome.Failure(DomainError.NotFound), RemoveBillUseCase(bills, FakeRenterRepository(listOf(ravi)))(novemberBill))
    }

    @Test
    fun theMonthOfARemovedBillIsABillToDoAgain() = runTest {
        val bills = FakeBillRepository(listOf(octoberBill, novemberBill))
        val renters = FakeRenterRepository(listOf(ravi))
        val todos = ObserveBillTodosUseCase(renters, FakeAddressRepository(listOf(TestData.address)), bills, GetTodayUseCase(clock, TimeZone.UTC))

        todos().test {
            assertEquals(emptyList(), (awaitItem() as Outcome.Success).value)

            RemoveBillUseCase(bills, renters)(novemberBill)

            assertEquals(listOf(november), (awaitItem() as Outcome.Success).value.map { it.month })
        }
    }

    @Test
    fun undoPutsTheSameBillBackAndFailsIfItsMonthWasBilledAgain() = runTest {
        val bills = FakeBillRepository(listOf(octoberBill, novemberBill))
        val renters = FakeRenterRepository(listOf(ravi))
        RemoveBillUseCase(bills, renters)(novemberBill)

        assertEquals(Outcome.Success(Unit), RestoreBillUseCase(bills)(novemberBill))
        assertEquals(listOf(octoberBill, novemberBill), bills.saved)

        RemoveBillUseCase(bills, renters)(novemberBill)
        bills.add(novemberBill.copy(id = BillId.None))
        assertIs<Outcome.Failure>(RestoreBillUseCase(bills)(novemberBill))
        assertEquals(1, bills.saved.count { it.month == november })
    }

    @Test
    fun theLedgerOffersOnlyTheNewestBillOfAnActiveRenterForRemoval() = runTest {
        fun ledger(renter: Renter, bills: List<Bill>) = ObserveRenterLedgerUseCase(
            FakeRenterRepository(listOf(renter)),
            FakeAddressRepository(listOf(TestData.address)),
            FakeBillRepository(bills),
            FakePaymentRepository(),
        )

        ledger(ravi, listOf(octoberBill, novemberBill))(RenterId(1)).test { assertEquals(novemberBill.id, awaitItem().ledger().removableBill) }
        ledger(gone, listOf(octoberBill, novemberBill))(RenterId(1)).test { assertEquals(null, awaitItem().ledger().removableBill) }
        ledger(ravi, emptyList())(RenterId(1)).test { assertEquals(null, awaitItem().ledger().removableBill) }
    }

    private fun Outcome<RenterLedger>.ledger() = (this as Outcome.Success).value
}
