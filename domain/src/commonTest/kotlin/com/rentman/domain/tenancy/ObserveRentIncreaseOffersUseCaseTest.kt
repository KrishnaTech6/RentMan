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
import com.rentman.domain.money.rupees
import com.rentman.domain.time.GetTodayUseCase
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class ObserveRentIncreaseOffersUseCaseTest {
    private fun useCase(
        today: String,
        renters: List<Renter> = listOf(TestData.renter()),
        changes: FakeRentIncreaseRepository = FakeRentIncreaseRepository(),
        bills: List<Bill> = emptyList(),
        failRenterReads: Boolean = false,
    ): ObserveRentIncreaseOffersUseCase {
        val clock = object : Clock {
            override fun now(): Instant = Instant.parse("${today}T10:00:00Z")
        }
        return ObserveRentIncreaseOffersUseCase(
            FakeRenterRepository(renters, failReads = failRenterReads),
            FakeAddressRepository(listOf(TestData.address)),
            changes,
            FakeBillRepository(bills),
            GetTodayUseCase(clock, TimeZone.UTC),
        )
    }

    private fun bill(month: BillingMonth, renterId: Long = 1) = Bill(
        renterId = RenterId(renterId),
        month = month,
        dueOn = month.plusMonths(1).dueOn(5),
        lines = listOf(BillLine(BillLineKind.Rent, "Rent", 7_500L.rupees)),
    )

    private fun change(renterId: Long = 1, effective: BillingMonth) = RentChange(
        id = RentChangeId(9),
        renterId = RenterId(renterId),
        oldRent = 7_500L.rupees,
        newRent = 8_250L.rupees,
        effectiveMonth = effective,
        decidedOn = effective.atStartOfMonth(),
    )

    private fun Outcome<List<RentIncreaseOffer>>.offers() = (this as Outcome.Success).value

    @Test
    fun noOfferBeforeElevenMonthsHavePassedSinceTheAgreementStarted() = runTest {
        // Agreement began Jan 2026, so the raise is due in Dec 2026.
        useCase("2026-11-30")().test {
            assertEquals(emptyList(), awaitItem().offers())
        }
    }

    @Test
    fun theOfferAppearsInTheRaiseMonthWithTheRaisedRentRoundedToTenRupees() = runTest {
        val bills = listOf(bill(BillingMonth.of(2026, 11)))

        useCase("2026-12-01", bills = bills)().test {
            val offer = awaitItem().offers().single()
            assertEquals(RenterSummary(TestData.renter(), TestData.address), offer.summary)
            assertEquals(10, offer.percent)
            assertEquals(7_500L.rupees, offer.currentRent)
            assertEquals(8_250L.rupees, offer.newRent)
        }
    }

    @Test
    fun theRaiseAppliesFromTheFirstMonthThatHasNoBillYet() = runTest {
        useCase("2026-12-01", bills = listOf(bill(BillingMonth.of(2026, 11))))().test {
            assertEquals(BillingMonth.of(2026, 12), awaitItem().offers().single().effectiveMonth)
        }
        // November is still unbilled, so the next bill is November's.
        useCase("2026-12-01")().test {
            assertEquals(BillingMonth.of(2026, 11), awaitItem().offers().single().effectiveMonth)
        }
    }

    @Test
    fun aRaiseCountsElevenMonthsFromTheLastRaiseNotFromTheAgreement() = runTest {
        val changes = FakeRentIncreaseRepository(listOf(change(effective = BillingMonth.of(2026, 12))))

        useCase("2027-10-31", changes = changes)().test {
            assertEquals(emptyList(), awaitItem().offers()) // Dec 2026 + 11 = Nov 2027
        }
        useCase("2027-11-01", changes = changes)().test {
            assertEquals(1, awaitItem().offers().size)
        }
    }

    @Test
    fun theLatestRaiseDecides() = runTest {
        val changes = FakeRentIncreaseRepository(
            listOf(change(effective = BillingMonth.of(2026, 12)), change(effective = BillingMonth.of(2027, 11)).copy(id = RentChangeId(10))),
        )

        useCase("2028-09-30", changes = changes)().test {
            assertEquals(emptyList(), awaitItem().offers()) // Nov 2027 + 11 = Oct 2028
        }
    }

    @Test
    fun anotherRentersRaiseDoesNotCount() = runTest {
        val changes = FakeRentIncreaseRepository(listOf(change(renterId = 2, effective = BillingMonth.of(2026, 12))))

        useCase("2026-12-01", changes = changes)().test {
            assertEquals(1, awaitItem().offers().size)
        }
    }

    @Test
    fun notNowHidesTheOfferUntilTheSnoozedMonth() = runTest {
        val snoozed = TestData.renter(raiseSnoozedUntil = BillingMonth.of(2027, 1))

        useCase("2026-12-31", renters = listOf(snoozed))().test {
            assertEquals(emptyList(), awaitItem().offers())
        }
        useCase("2027-01-01", renters = listOf(snoozed))().test {
            assertEquals(1, awaitItem().offers().size)
        }
    }

    @Test
    fun aRenterWhoIsNeverToBeAskedGetsNoOffer() = runTest {
        useCase("2030-01-01", renters = listOf(TestData.renter(raisePercent = 0)))().test {
            assertEquals(emptyList(), awaitItem().offers())
        }
    }

    @Test
    fun offersAreSortedByRenterNameAndARenterWithoutAnAddressIsLeftOut() = runTest {
        val renters = listOf(
            TestData.renter(id = 1, name = "Zoya"),
            TestData.renter(id = 2, name = "Amit"),
            TestData.renter(id = 3, name = "Lost", addressId = AddressId(99)),
        )

        useCase("2026-12-01", renters = renters)().test {
            assertEquals(listOf("Amit", "Zoya"), awaitItem().offers().map { it.summary.renter.name })
        }
    }

    @Test
    fun aFailureReadingRentersOrRaisesIsReported() = runTest {
        useCase("2026-12-01", failRenterReads = true)().test {
            assertEquals(Outcome.Failure(DomainError.Storage("disk")), awaitItem())
        }
        useCase("2026-12-01", changes = FakeRentIncreaseRepository(failReads = true))().test {
            assertEquals(Outcome.Failure(DomainError.Storage("disk")), awaitItem())
        }
    }
}
