package com.rentman.domain.tenancy

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.TestData
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.rupees
import com.rentman.domain.time.GetTodayUseCase
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class RentIncreaseUseCasesTest {
    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-12-03T10:00:00Z")
    }
    private val today = GetTodayUseCase(clock, TimeZone.UTC)
    private val offer = RentIncreaseOffer(
        summary = RenterSummary(TestData.renter(), TestData.address),
        percent = 10,
        newRent = 8_250L.rupees,
        effectiveMonth = BillingMonth.of(2026, 12),
    )

    @Test
    fun applyingRecordsTheOldAndNewRentTheMonthAndTheDay() = runTest {
        val repository = FakeRentIncreaseRepository()

        val change = (ApplyRentIncreaseUseCase(repository, today)(offer) as Outcome.Success).value

        assertEquals(
            RentChange(
                id = change.id,
                renterId = RenterId(1),
                oldRent = 7_500L.rupees,
                newRent = 8_250L.rupees,
                effectiveMonth = BillingMonth.of(2026, 12),
                decidedOn = LocalDate(2026, 12, 3),
            ),
            change,
        )
        assertEquals(listOf(change), repository.applied)
    }

    @Test
    fun aFailedApplyIsReported() = runTest {
        val repository = FakeRentIncreaseRepository(failWrites = true)

        assertEquals(Outcome.Failure(DomainError.Storage("disk")), ApplyRentIncreaseUseCase(repository, today)(offer))
        assertEquals(emptyList(), repository.applied)
    }

    @Test
    fun notNowHidesTheCardUntilNextMonthEvenAcrossTheNewYear() = runTest {
        val repository = FakeRentIncreaseRepository()

        PostponeRentIncreaseUseCase(repository, today)(RenterId(1))

        assertEquals(mapOf(RenterId(1) to BillingMonth.of(2027, 1)), repository.postponed)
    }

    @Test
    fun undoingPutsTheOldRentBack() = runTest {
        val repository = FakeRentIncreaseRepository()
        val change = (ApplyRentIncreaseUseCase(repository, today)(offer) as Outcome.Success).value

        assertEquals(Outcome.Success(Unit), UndoRentIncreaseUseCase(repository)(change))

        assertEquals(listOf(change), repository.reverted)
    }

    @Test
    fun aFailedPostponeOrUndoIsReported() = runTest {
        val repository = FakeRentIncreaseRepository(failWrites = true)

        assertEquals(Outcome.Failure(DomainError.Storage("disk")), PostponeRentIncreaseUseCase(repository, today)(RenterId(1)))
        assertEquals(
            Outcome.Failure(DomainError.Storage("disk")),
            UndoRentIncreaseUseCase(repository)(RentChange(RentChangeId(1), RenterId(1), 1L.rupees, 2L.rupees, BillingMonth.of(2026, 12), LocalDate(2026, 12, 3))),
        )
    }
}
