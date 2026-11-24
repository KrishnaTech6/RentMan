package com.rentman.data.rent

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import com.rentman.data.address.RoomAddressRepository
import com.rentman.data.db.AppDatabase
import com.rentman.data.renter.RoomRenterRepository
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.RentChange
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock

class RoomRentIncreaseRepositoryTest {
    private val database = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
    private val repository = RoomRentIncreaseRepository(database.rentChangeDao(), Clock.System)
    private val renters = RoomRenterRepository(database.renterDao(), Clock.System)

    @AfterTest
    fun closeDatabase() = database.close()

    private suspend fun addRenter(snoozedUntil: BillingMonth? = null): Renter {
        val address = (RoomAddressRepository(database.addressDao(), Clock.System).add("338 A") as Outcome.Success).value
        val renter = Renter(
            addressId = address.id,
            unit = null,
            name = "Ravi Kumar",
            phone = "919876543210",
            monthlyRent = 7_500L.rupees,
            dueDay = 5,
            agreementStart = LocalDate(2026, 1, 1),
            raisePercent = 10,
            raiseEveryMonths = 11,
            raiseSnoozedUntil = snoozedUntil,
            billsElectricity = false,
            customRate = null,
            startingReading = null,
            firstBillMonth = BillingMonth.of(2026, 1),
        )
        return (renters.add(renter) as Outcome.Success).value
    }

    private fun change(renterId: RenterId) = RentChange(
        renterId = renterId,
        oldRent = 7_500L.rupees,
        newRent = 8_250L.rupees,
        effectiveMonth = BillingMonth.of(2026, 12),
        decidedOn = LocalDate(2026, 12, 3),
    )

    private suspend fun rentOf(): Renter = (renters.observeActive().first() as Outcome.Success).value.single()

    private suspend fun changes() = (repository.observeAll().first() as Outcome.Success).value

    @Test
    fun applyingStoresTheChangeAndRaisesTheRentInOneStep() = runTest {
        val renter = addRenter(snoozedUntil = BillingMonth.of(2026, 11))

        val saved = (repository.apply(change(renter.id)) as Outcome.Success).value

        assertEquals(1L, saved.id.value)
        assertEquals(listOf(saved), changes())
        assertEquals(8_250L.rupees, rentOf().monthlyRent)
        assertEquals(null, rentOf().raiseSnoozedUntil) // a yes clears "not now"
    }

    @Test
    fun anObserverOfRentersSeesTheNewRentWithoutAskingAgain() = runTest {
        val renter = addRenter()

        renters.observeActive().test {
            assertEquals(7_500L.rupees, (awaitItem() as Outcome.Success).value.single().monthlyRent)

            repository.apply(change(renter.id))

            assertEquals(8_250L.rupees, (awaitItem() as Outcome.Success).value.single().monthlyRent)
        }
    }

    @Test
    fun revertingHidesTheChangeAndPutsTheOldRentBack() = runTest {
        val renter = addRenter()
        val saved = (repository.apply(change(renter.id)) as Outcome.Success).value

        assertEquals(Outcome.Success(Unit), repository.revert(saved))

        assertEquals(emptyList(), changes())
        assertEquals(7_500L.rupees, rentOf().monthlyRent)
    }

    @Test
    fun notNowStoresTheMonthTheOfferComesBack() = runTest {
        val renter = addRenter()

        assertEquals(Outcome.Success(Unit), repository.postpone(renter.id, BillingMonth.of(2027, 1)))

        assertEquals(BillingMonth.of(2027, 1), rentOf().raiseSnoozedUntil)
        assertEquals(7_500L.rupees, rentOf().monthlyRent) // the rent itself is untouched
    }

    @Test
    fun applyingTheSameOfferTwiceStoresOnlyOneChange() = runTest {
        val renter = addRenter()
        val offer = change(renter.id)

        assertIs<Outcome.Success<RentChange>>(repository.apply(offer))
        // A second, very fast tap on "Raise rent" with the same offer.
        assertEquals(Outcome.Failure(DomainError.OfferOutdated), repository.apply(offer))

        assertEquals(1, changes().size)
        assertEquals(8_250L.rupees, rentOf().monthlyRent)
    }

    @Test
    fun undoingTheOnlyChangeAfterADoubleTapLeavesNothingBehind() = runTest {
        val renter = addRenter()
        val saved = (repository.apply(change(renter.id)) as Outcome.Success).value
        repository.apply(change(renter.id)) // the double tap, rejected

        repository.revert(saved)

        assertEquals(emptyList(), changes())
        assertEquals(7_500L.rupees, rentOf().monthlyRent)
    }

    @Test
    fun anOfferWhoseOldRentIsNoLongerTheRentIsRejected() = runTest {
        val renter = addRenter()

        val result = repository.apply(change(renter.id).copy(oldRent = 7_000L.rupees)) // the rent is 7,500

        assertEquals(Outcome.Failure(DomainError.OfferOutdated), result)
        assertEquals(emptyList(), changes())
        assertEquals(7_500L.rupees, rentOf().monthlyRent)
    }

    @Test
    fun aChangeForAnUnknownRenterIsRejectedAndNothingIsStored() = runTest {
        val result = repository.apply(change(RenterId(999)))

        assertEquals(Outcome.Failure(DomainError.OfferOutdated), result)
        assertEquals(emptyList(), changes())
    }
}
