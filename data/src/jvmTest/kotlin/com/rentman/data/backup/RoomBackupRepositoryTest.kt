package com.rentman.data.backup

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import com.rentman.data.db.AppDatabase
import com.rentman.data.payment.RoomPaymentRepository
import com.rentman.data.renter.RoomRenterRepository
import com.rentman.data.settings.RoomSettingsRepository
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Instant

class RoomBackupRepositoryTest {
    private val database = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
    private val clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(1_800_000_000_000)
    }
    private val repository = RoomBackupRepository(database.backupDao(), clock)

    @AfterTest
    fun closeDatabase() = database.close()

    private suspend fun seed(file: BackupFile) = database.backupDao().replaceAll(file, takenAt = 1, takeSnapshot = false)

    private suspend fun exported(): BackupFile = (BackupCodec.decode((repository.export() as Outcome.Success).value) as Outcome.Success).value

    /** Neha at another address, with nothing else: different from the sample in every table it has. */
    private fun neha() = sampleBackup().let { s ->
        BackupFile(
            schemaVersion = s.schemaVersion, exportedAt = 0,
            address = listOf(s.address.single().copy(id = 7, name = "Park View G-7")),
            renter = listOf(s.renter.single().copy(id = 8, addressId = 7, name = "Neha Verma", remindersMutedMonth = null, raiseSnoozedUntil = null)),
            fixedCharge = emptyList(), rentChange = emptyList(), bill = emptyList(), billLine = emptyList(), payment = emptyList(),
            settings = null,
        )
    }

    @Test
    fun exportingGivesEverythingIncludingRemovedRowsAndTheSettings() = runTest {
        seed(sampleBackup())

        val file = exported()

        assertEquals(sampleBackup().copy(exportedAt = 1_800_000_000_000), file)
        assertEquals(2, file.payment.size) // the removed one too
        assertEquals(2, file.rentChange.size)
    }

    @Test
    fun exportingAnEmptyPhoneGivesAValidEmptyBackup() = runTest {
        val file = exported()

        assertEquals(emptyList(), file.renter)
        assertEquals(null, file.settings)
    }

    @Test
    fun restoringReplacesEverythingAndKeepsEveryId() = runTest {
        seed(neha())

        assertEquals(Outcome.Success(Unit), repository.restore(BackupCodec.encode(sampleBackup())))

        assertEquals(sampleBackup().copy(exportedAt = 1_800_000_000_000), exported())
        val renters = RoomRenterRepository(database.renterDao(), clock).observeActive().first()
        assertEquals(listOf("Ravi Kumar"), (renters as Outcome.Success).value.map { it.name })
        assertEquals(1L, renters.value.single().id.value)
    }

    @Test
    fun aRemovedPaymentStaysRemovedAfterARestore() = runTest {
        repository.restore(BackupCodec.encode(sampleBackup()))

        val visible = (RoomPaymentRepository(database.paymentDao(), clock).observeAll().first() as Outcome.Success).value

        assertEquals(listOf(1L), visible.map { it.id.value }) // payment 2 was removed in the file
    }

    @Test
    fun aFileWithoutSettingsGoesBackToTheDefaults() = runTest {
        seed(sampleBackup()) // has settings

        repository.restore(BackupCodec.encode(neha())) // has none

        assertEquals(null, exported().settings)
        assertEquals(800L, (RoomSettingsRepository(database.settingsDao()).observe().first() as Outcome.Success).value.globalRate.value)
    }

    @Test
    fun aRestoreKeepsTheOldDataAsASnapshotAndUndoBringsItBack() = runTest {
        seed(neha())

        repository.observeSnapshot().test {
            assertEquals(Outcome.Success(null), awaitItem())

            repository.restore(BackupCodec.encode(sampleBackup()))
            assertEquals(Outcome.Success<Instant?>(clock.now()), awaitItem())

            assertEquals(Outcome.Success(Unit), repository.restoreSnapshot())
            assertEquals(Outcome.Success(null), awaitItem()) // the snapshot is used up
        }
        assertEquals(neha().copy(exportedAt = 1_800_000_000_000), exported())
    }

    @Test
    fun undoingWithNoSnapshotIsNotFoundAndChangesNothing() = runTest {
        seed(neha())

        assertEquals(Outcome.Failure(DomainError.NotFound), repository.restoreSnapshot())

        assertEquals(neha().copy(exportedAt = 1_800_000_000_000), exported())
    }

    @Test
    fun restoringASecondFileWithoutChangingAnythingKeepsTheFirstSnapshot() = runTest {
        // "Wrong file, try another": the data from before the first restore must not be lost.
        seed(neha())
        val other = neha().let { it.copy(address = it.address.map { a -> a.copy(name = "Lake Tower B9-19") }) }
        repository.restore(BackupCodec.encode(sampleBackup())) // snapshot = Neha, data = Ravi
        repository.restore(BackupCodec.encode(other)) // nothing was changed in between

        assertEquals(Outcome.Success(Unit), repository.restoreSnapshot())

        assertEquals(neha().copy(exportedAt = 1_800_000_000_000), exported())
    }

    @Test
    fun theKeptSnapshotKeepsTheDayOfTheFirstRestore() = runTest {
        var now = 1_000L
        val ticking = RoomBackupRepository(database.backupDao(), object : Clock {
            override fun now(): Instant = Instant.fromEpochMilliseconds(now)
        })
        seed(neha())
        ticking.restore(BackupCodec.encode(sampleBackup()))
        now = 9_000L

        ticking.restore(BackupCodec.encode(neha()))

        assertEquals(Outcome.Success<Instant?>(Instant.fromEpochMilliseconds(1_000L)), ticking.observeSnapshot().first())
    }

    @Test
    fun ifTheDataWasChangedAfterTheFirstRestoreTheSecondRestoreKeepsThatNewerDataAsTheSnapshot() = runTest {
        seed(neha())
        repository.restore(BackupCodec.encode(sampleBackup())) // snapshot = Neha, data = Ravi
        RoomPaymentRepository(database.paymentDao(), clock).add(
            com.rentman.domain.payments.Payment(
                renterId = com.rentman.domain.tenancy.RenterId(1),
                amount = com.rentman.domain.money.Paise(1_000),
                paidOn = kotlinx.datetime.LocalDate(2026, 12, 1),
                mode = com.rentman.domain.payments.PaymentMode.Cash,
            ),
        ) // the person went on using the app
        val withPayment = exported()

        repository.restore(BackupCodec.encode(neha()))
        repository.restoreSnapshot()

        assertEquals(withPayment, exported()) // Ravi's data including the new payment, not the old Neha-only data
    }

    @Test
    fun makingABackupBetweenTwoRestoresDoesNotCountAsChangingTheData() = runTest {
        seed(neha())
        repository.restore(BackupCodec.encode(sampleBackup()))
        RoomSettingsRepository(database.settingsDao()).markBackedUp(Instant.fromEpochMilliseconds(5_000))

        repository.restore(BackupCodec.encode(neha()))
        repository.restoreSnapshot()

        assertEquals(neha().copy(exportedAt = 1_800_000_000_000), exported())
    }

    @Test
    fun undoingStillUsesTheSnapshotUpSoTheOfferGoesAway() = runTest {
        seed(neha())
        repository.restore(BackupCodec.encode(sampleBackup()))
        repository.restore(BackupCodec.encode(sampleBackup()))

        repository.restoreSnapshot()

        assertEquals(Outcome.Failure(DomainError.NotFound), repository.restoreSnapshot())
    }

    @Test
    fun aFileThatIsNotUsableChangesNothingAndTakesNoSnapshot() = runTest {
        seed(neha())

        val result = repository.restore("this is not a backup")

        assertEquals(Outcome.Failure(DomainError.InvalidBackup), result)
        assertEquals(neha().copy(exportedAt = 1_800_000_000_000), exported())
        assertEquals(Outcome.Success(null), repository.observeSnapshot().first())
    }

    @Test
    fun aFileFromANewerVersionChangesNothing() = runTest {
        seed(neha())

        val result = repository.restore(BackupCodec.encode(sampleBackup().copy(schemaVersion = 99)))

        assertEquals(Outcome.Failure(DomainError.BackupFromNewerVersion(99)), result)
        assertEquals(neha().copy(exportedAt = 1_800_000_000_000), exported())
    }

    @Test
    fun ifTheDatabaseRefusesPartWayEverythingIsRolledBackIncludingTheSnapshot() = runTest {
        seed(neha())
        // Valid for our checks, but the address names clash for the database, which ignores case, so the insert fails
        // after the old data was already cleared.
        val clash = sampleBackup().let { it.copy(address = it.address + it.address.single().copy(id = 2, name = "12 a")) }

        val result = repository.restore(BackupCodec.encode(clash))

        assertIs<DomainError.Storage>(assertIs<Outcome.Failure>(result).error)
        assertEquals(neha().copy(exportedAt = 1_800_000_000_000), exported()) // untouched
        assertEquals(Outcome.Success(null), repository.observeSnapshot().first()) // and no snapshot was kept
    }

    @Test
    fun aRestoredDatabaseGivesNewRowsIdsAfterTheRestoredOnes() = runTest {
        repository.restore(BackupCodec.encode(sampleBackup()))

        val added = (RoomPaymentRepository(database.paymentDao(), clock).add(
            com.rentman.domain.payments.Payment(
                renterId = com.rentman.domain.tenancy.RenterId(1),
                amount = com.rentman.domain.money.Paise(1_000),
                paidOn = kotlinx.datetime.LocalDate(2026, 12, 1),
                mode = com.rentman.domain.payments.PaymentMode.Cash,
            ),
        ) as Outcome.Success).value

        assertEquals(3L, added.id.value) // payments 1 and 2 came from the file
    }
}
