package com.rentman.data.renter

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import com.rentman.data.address.RoomAddressRepository
import com.rentman.data.db.AppDatabase
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.AddressId
import com.rentman.domain.tenancy.FixedCharge
import com.rentman.domain.tenancy.FixedChargeId
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

class RoomRenterRepositoryTest {
    private val database = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
    private val addresses = RoomAddressRepository(database.addressDao(), Clock.System)
    private val repository = RoomRenterRepository(database.renterDao(), Clock.System)

    @AfterTest
    fun closeDatabase() = database.close()

    private suspend fun address(name: String = "12 A"): AddressId = ((addresses.add(name) as Outcome.Success).value).id

    private fun renter(addressId: AddressId, name: String = "Ravi Kumar") = Renter(
        addressId = addressId,
        unit = "Ground floor",
        name = name,
        phone = "919876543210",
        monthlyRent = 7_500L.rupees,
        dueDay = 5,
        agreementStart = LocalDate(2026, 1, 1),
        raisePercent = 10,
        raiseEveryMonths = 11,
        billsElectricity = true,
        customRate = 900L.let { com.rentman.domain.money.Paise(it) },
        startingReading = 1240,
        firstBillMonth = BillingMonth.of(2026, 11),
    )

    private fun Outcome<List<Renter>>.names() = (this as Outcome.Success).value.map(Renter::name)

    @Test
    fun aSavedRenterComesBackWithItsIdAndEveryField() = runTest {
        val home = address()
        val saved = (repository.add(renter(home)) as Outcome.Success).value

        assertEquals(RenterId(1), saved.id)
        repository.observeActive().test {
            assertEquals(listOf(saved), (awaitItem() as Outcome.Success).value)
        }
    }

    @Test
    fun fixedChargesAreSavedWithTheRenterAndGetTheirIds() = runTest {
        val home = address()
        val charges = listOf(
            FixedCharge(label = "Water", amount = 200L.rupees),
            FixedCharge(label = "Garbage", amount = 50L.rupees),
        )

        val saved = (repository.add(renter(home).copy(fixedCharges = charges)) as Outcome.Success).value

        assertEquals(listOf(FixedChargeId(1), FixedChargeId(2)), saved.fixedCharges.map { it.id })
        repository.observeActive().test {
            val listed = (awaitItem() as Outcome.Success).value.single()
            assertEquals(listOf("Water" to 200L.rupees, "Garbage" to 50L.rupees), listed.fixedCharges.map { it.label to it.amount })
        }
    }

    @Test
    fun ifAChargeFailsToSaveNothingIsSaved() = runTest {
        val home = address()
        // Two charges with the same id: the second insert breaks the primary key, after the renter row went in.
        val clash = listOf(
            FixedCharge(FixedChargeId(7), "Water", 200L.rupees),
            FixedCharge(FixedChargeId(7), "Garbage", 50L.rupees),
        )

        val result = repository.add(renter(home).copy(fixedCharges = clash))

        assertIs<Outcome.Failure>(result)
        repository.observeActive().test {
            assertEquals(emptyList(), (awaitItem() as Outcome.Success).value) // no renter left behind
        }
    }

    @Test
    fun eachRenterOnlyGetsTheirOwnCharges() = runTest {
        val home = address()
        repository.add(renter(home, "Amit").copy(unit = "1st floor", fixedCharges = listOf(FixedCharge(label = "Water", amount = 200L.rupees))))
        repository.add(renter(home, "Neha"))

        repository.observeActive().test {
            val byName = (awaitItem() as Outcome.Success).value.associate { it.name to it.fixedCharges.size }
            assertEquals(mapOf("Amit" to 1, "Neha" to 0), byName)
        }
    }

    @Test
    fun rentersAreSortedByNameAndEmitAgainWhenOneIsAdded() = runTest {
        val home = address()
        repository.add(renter(home, "Neha"))

        repository.observeActive().test {
            assertEquals(listOf("Neha"), awaitItem().names())

            repository.add(renter(home, "Amit").copy(unit = "1st floor"))

            assertEquals(listOf("Amit", "Neha"), awaitItem().names())
        }
    }

    @Test
    fun aRenterWhoHasLeftIsNotListed() = runTest {
        val home = address()
        repository.add(renter(home, "Ravi"))
        repository.add(renter(home, "Gone").copy(unit = "2nd floor", active = false))

        repository.observeActive().test {
            assertEquals(listOf("Ravi"), awaitItem().names())
        }
    }

    @Test
    fun aRenterAtAnUnknownAddressIsRejectedByTheDatabase() = runTest {
        val result = repository.add(renter(AddressId(999)))

        val failure = assertIs<Outcome.Failure>(result)
        assertIs<DomainError.Storage>(failure.error)
    }

    @Test
    fun stoppingRemindersStoresTheMonthAndResumingClearsIt() = runTest {
        val saved = (repository.add(renter(address())) as Outcome.Success).value
        val november = BillingMonth.of(2026, 11)

        assertEquals(Outcome.Success(Unit), repository.setRemindersMutedMonth(saved.id, november))
        assertEquals(november, (repository.observeActive().first() as Outcome.Success).value.single().remindersMutedMonth)

        assertEquals(Outcome.Success(Unit), repository.setRemindersMutedMonth(saved.id, null))
        assertEquals(null, (repository.observeActive().first() as Outcome.Success).value.single().remindersMutedMonth)
    }

    @Test
    fun stoppingRemindersForOneRenterLeavesTheOthers() = runTest {
        val home = address()
        val ravi = (repository.add(renter(home, "Ravi Kumar")) as Outcome.Success).value
        repository.add(renter(home, "Neha").copy(unit = "1st floor"))

        repository.setRemindersMutedMonth(ravi.id, BillingMonth.of(2026, 11))

        val byName = (repository.observeActive().first() as Outcome.Success).value.associate { it.name to it.remindersMutedMonth }
        assertEquals(mapOf("Neha" to null, "Ravi Kumar" to BillingMonth.of(2026, 11)), byName)
    }

    @Test
    fun theMuteIsSeenByAnObserverAlreadyWatching() = runTest {
        val saved = (repository.add(renter(address())) as Outcome.Success).value

        repository.observeActive().test {
            assertEquals(null, (awaitItem() as Outcome.Success).value.single().remindersMutedMonth)

            repository.setRemindersMutedMonth(saved.id, BillingMonth.of(2026, 11))

            assertEquals(BillingMonth.of(2026, 11), (awaitItem() as Outcome.Success).value.single().remindersMutedMonth)
        }
    }

    private suspend fun all() = (repository.observeAll().first() as Outcome.Success).value

    private fun charge(label: String, rupees: Long, id: Long = 0) = FixedCharge(FixedChargeId(id), label, rupees.rupees)

    @Test
    fun everyRenterIsListedByNameIncludingThoseWhoLeftButTheActiveListStillHidesThem() = runTest {
        val home = address()
        val ravi = (repository.add(renter(home, "Ravi Kumar")) as Outcome.Success).value
        repository.add(renter(home, "Amit").copy(unit = "1st floor"))
        repository.setLeft(ravi.id, LocalDate(2026, 12, 10))

        assertEquals(listOf("Amit", "Ravi Kumar"), all().map { it.name })
        assertEquals(listOf("Amit"), (repository.observeActive().first() as Outcome.Success).value.map { it.name })
    }

    @Test
    fun markingAsLeftStoresTheDayAndMovingBackClearsIt() = runTest {
        val saved = (repository.add(renter(address())) as Outcome.Success).value

        assertEquals(Outcome.Success(Unit), repository.setLeft(saved.id, LocalDate(2026, 12, 10)))
        all().single().let {
            assertEquals(false, it.active)
            assertEquals(LocalDate(2026, 12, 10), it.leftOn)
        }

        assertEquals(Outcome.Success(Unit), repository.setLeft(saved.id, null))
        all().single().let {
            assertEquals(true, it.active)
            assertEquals(null, it.leftOn)
        }
    }

    @Test
    fun updatingChangesTheFieldsKeepsTheIdAndLeavesWhatTheFormDoesNotShow() = runTest {
        val home = address()
        val saved = (repository.add(renter(home).copy(remindersMutedMonth = BillingMonth.of(2026, 11))) as Outcome.Success).value

        val result = repository.update(saved.copy(name = "Ravi K", monthlyRent = 8_000L.rupees, dueDay = 10, unit = null))

        assertEquals(Outcome.Success(saved.copy(name = "Ravi K", monthlyRent = 8_000L.rupees, dueDay = 10, unit = null)), result)
        val stored = all().single()
        assertEquals("Ravi K", stored.name)
        assertEquals(8_000L.rupees, stored.monthlyRent)
        assertEquals(saved.id, stored.id)
        assertEquals(BillingMonth.of(2026, 11), stored.remindersMutedMonth)
    }

    @Test
    fun updatingKeepsWhenTheRenterWasAdded() = runTest {
        val saved = (repository.add(renter(address())) as Outcome.Success).value
        val before = database.backupDao().readAll(0).renter.single().createdAt

        repository.update(saved.copy(name = "Someone Else"))

        assertEquals(before, database.backupDao().readAll(0).renter.single().createdAt)
    }

    @Test
    fun updatingTheChargesChangesAddsAndSwitchesOffWithoutDeletingAnything() = runTest {
        val saved = (repository.add(renter(address()).copy(fixedCharges = listOf(charge("Water", 200), charge("Gas", 300)))) as Outcome.Success).value
        val water = saved.fixedCharges.first { it.label == "Water" }

        // Water stays and costs more, Gas is removed, Parking is new.
        val result = (repository.update(saved.copy(fixedCharges = listOf(water.copy(amount = 250L.rupees), charge("Parking", 500)))) as Outcome.Success).value

        assertEquals(listOf("Water", "Parking"), result.fixedCharges.map { it.label })
        assertEquals(water.id, result.fixedCharges.first().id)
        assertEquals(true, result.fixedCharges.last().id.value > 0) // the new charge got its real id
        val stored = all().single().fixedCharges.associateBy { it.label }
        assertEquals(250L.rupees, stored.getValue("Water").amount)
        assertEquals(true, stored.getValue("Water").active)
        assertEquals(false, stored.getValue("Gas").active) // switched off, still there
        assertEquals(true, stored.getValue("Parking").active)
    }

    @Test
    fun removingEveryChargeSwitchesThemAllOff() = runTest {
        val saved = (repository.add(renter(address()).copy(fixedCharges = listOf(charge("Water", 200)))) as Outcome.Success).value

        repository.update(saved.copy(fixedCharges = emptyList()))

        assertEquals(listOf(false), all().single().fixedCharges.map { it.active })
    }

    @Test
    fun updatingOneRentersChargesLeavesAnotherRentersAlone() = runTest {
        val home = address()
        val ravi = (repository.add(renter(home, "Ravi Kumar").copy(fixedCharges = listOf(charge("Water", 200)))) as Outcome.Success).value
        repository.add(renter(home, "Neha").copy(unit = "1st floor", fixedCharges = listOf(charge("Water", 200))))

        repository.update(ravi.copy(fixedCharges = emptyList()))

        assertEquals(mapOf("Neha" to listOf(true), "Ravi Kumar" to listOf(false)), all().associate { it.name to it.fixedCharges.map { c -> c.active } })
    }

    @Test
    fun updatingAnUnknownRenterIsNotFound() = runTest {
        val saved = (repository.add(renter(address())) as Outcome.Success).value

        assertEquals(Outcome.Failure(DomainError.NotFound), repository.update(saved.copy(id = RenterId(99))))
        assertEquals(saved.name, all().single().name)
    }

    @Test
    fun ifTheRenterCannotBeUpdatedTheChargeChangesAreRolledBackToo() = runTest {
        val saved = (repository.add(renter(address()).copy(fixedCharges = listOf(charge("Water", 200)))) as Outcome.Success).value

        // The address does not exist, so the database refuses the renter row after the charges were already changed.
        val result = repository.update(saved.copy(addressId = AddressId(999), fixedCharges = listOf(charge("Gas", 300))))

        assertIs<DomainError.Storage>(assertIs<Outcome.Failure>(result).error)
        val stored = all().single()
        assertEquals(saved.addressId, stored.addressId)
        assertEquals(listOf("Water" to true), stored.fixedCharges.map { it.label to it.active }) // Gas was not added, Water not switched off
    }
}
