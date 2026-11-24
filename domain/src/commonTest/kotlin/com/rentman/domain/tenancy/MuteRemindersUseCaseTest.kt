package com.rentman.domain.tenancy

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.TestData
import com.rentman.domain.billing.Bill
import com.rentman.domain.billing.BillLine
import com.rentman.domain.billing.BillLineKind
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.billing.FakeBillRepository
import com.rentman.domain.money.rupees
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MuteRemindersUseCaseTest {
    private fun bill(month: BillingMonth, renterId: Long = 1) = Bill(
        renterId = RenterId(renterId),
        month = month,
        dueOn = month.plusMonths(1).dueOn(5),
        lines = listOf(BillLine(BillLineKind.Rent, "Rent", 7_500L.rupees)),
    )

    private val october = BillingMonth.of(2026, 10)
    private val november = BillingMonth.of(2026, 11)

    @Test
    fun stoppingMutesUntilANewerBillThanTheLatestOne() = runTest {
        val renters = FakeRenterRepository(listOf(TestData.renter()))
        val bills = FakeBillRepository(listOf(bill(october), bill(november), bill(BillingMonth.of(2026, 12), renterId = 2)))

        val result = MuteRemindersUseCase(renters, bills)(RenterId(1), muted = true)

        assertEquals(Outcome.Success(Unit), result)
        assertEquals(november, renters.saved.single().remindersMutedMonth) // the renter's latest bill, not another renter's
    }

    @Test
    fun resumingClearsTheMute() = runTest {
        val renters = FakeRenterRepository(listOf(TestData.renter(remindersMutedMonth = november)))

        MuteRemindersUseCase(renters, FakeBillRepository(listOf(bill(november))))(RenterId(1), muted = false)

        assertEquals(null, renters.saved.single().remindersMutedMonth)
    }

    @Test
    fun aRenterWithNoBillHasNothingToMuteAndNothingChanges() = runTest {
        val renters = FakeRenterRepository(listOf(TestData.renter()))

        val result = MuteRemindersUseCase(renters, FakeBillRepository())(RenterId(1), muted = true)

        assertEquals(Outcome.Success(Unit), result)
        assertEquals(null, renters.saved.single().remindersMutedMonth)
    }

    @Test
    fun aStorageFailureIsReported() = runTest {
        val renters = FakeRenterRepository(listOf(TestData.renter()), failWrites = true)

        val result = MuteRemindersUseCase(renters, FakeBillRepository(listOf(bill(november))))(RenterId(1), muted = true)

        assertEquals(Outcome.Failure(DomainError.Storage("disk")), result)
    }

    @Test
    fun aRenterIsMutedOnlyWhileTheMuteIsAsNewAsTheirLatestBill() {
        val muted = TestData.renter(remindersMutedMonth = november)

        assertEquals(true, muted.remindersMutedFor(latestBill = november))
        assertEquals(false, muted.remindersMutedFor(latestBill = BillingMonth.of(2026, 12))) // a newer bill ends it
        assertEquals(false, TestData.renter().remindersMutedFor(latestBill = november))
        assertEquals(false, muted.remindersMutedFor(latestBill = null))
    }
}
