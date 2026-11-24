package com.rentman.domain.billing

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.RenterId
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class CreateBillUseCaseTest {
    private fun bill(renterId: Long = 1, month: BillingMonth = BillingMonth.of(2026, 11)) = Bill(
        renterId = RenterId(renterId),
        month = month,
        dueOn = LocalDate(2026, 11, 5),
        lines = listOf(BillLine(BillLineKind.Rent, "Rent", 7_500L.rupees)),
    )

    @Test
    fun savesTheBill() = runTest {
        val repository = FakeBillRepository()

        val result = CreateBillUseCase(repository)(bill())

        assertEquals(BillId(1), (result as Outcome.Success).value.id)
        assertEquals(1, repository.saved.size)
    }

    @Test
    fun aRenterCannotHaveTwoBillsForTheSameMonth() = runTest {
        val repository = FakeBillRepository(listOf(bill()))

        val result = CreateBillUseCase(repository)(bill())

        assertEquals(Outcome.Failure(DomainError.BillExists), result)
        assertEquals(1, repository.saved.size)
    }

    @Test
    fun anotherMonthOrAnotherRenterIsFine() = runTest {
        val repository = FakeBillRepository(listOf(bill()))
        val useCase = CreateBillUseCase(repository)

        useCase(bill(month = BillingMonth.of(2026, 12)))
        useCase(bill(renterId = 2))

        assertEquals(3, repository.saved.size)
    }

    @Test
    fun ifTheExistingBillsCannotBeReadNothingIsSaved() = runTest {
        val repository = FakeBillRepository(failReads = true)

        val result = CreateBillUseCase(repository)(bill())

        assertEquals(Outcome.Failure(DomainError.Storage("disk")), result)
        assertEquals(emptyList(), repository.saved)
    }
}
