package com.rentman.domain.drive

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.TestData
import com.rentman.domain.backup.FakeBackupRepository
import com.rentman.domain.billing.Bill
import com.rentman.domain.billing.BillLine
import com.rentman.domain.billing.BillLineKind
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.billing.FakeBillRepository
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.FakeRenterRepository
import com.rentman.domain.tenancy.RenterId
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DriveUseCasesTest {
    private val renters = FakeRenterRepository(listOf(TestData.renter()))
    private val noRenters = FakeRenterRepository()

    private fun bill(month: Int, photo: String?) = Bill(
        renterId = RenterId(1),
        month = BillingMonth.of(2026, month),
        dueOn = LocalDate(2026, month, 5),
        meterPhotoPath = photo,
        lines = listOf(BillLine(BillLineKind.Rent, "Rent", 7_500L.rupees)),
    )

    private val bills = FakeBillRepository(listOf(bill(10, "oct.jpg"), bill(11, "nov.jpg"), bill(9, null)))

    private fun backUp(
        drive: FakeDriveBackupRepository,
        backups: FakeBackupRepository = FakeBackupRepository(exportJson = """{"all":"data"}"""),
        photos: FakeMeterPhotoStore = FakeMeterPhotoStore(),
        people: FakeRenterRepository = renters,
    ) = BackUpToDriveUseCase(backups, drive, people, bills, photos)

    @Test
    fun backingUpSavesTheDataAndTheMeterPhotosTheBillsPointTo() = runTest {
        val drive = FakeDriveBackupRepository(photosInDrive = mutableMapOf("oct.jpg" to byteArrayOf(1)))
        val photos = FakeMeterPhotoStore(mutableMapOf("oct.jpg" to byteArrayOf(1), "nov.jpg" to byteArrayOf(2), "retake.jpg" to byteArrayOf(9)))

        assertEquals(Outcome.Success(Unit), backUp(drive, photos = photos)())

        assertEquals("""{"all":"data"}""", drive.file)
        assertEquals(setOf("oct.jpg", "nov.jpg"), drive.photosInDrive.keys) // the retake no bill points to stays on the phone
    }

    @Test
    fun aPhoneWithNoRentersIsNeverBackedUpOverDrive() = runTest {
        val drive = FakeDriveBackupRepository(file = "the good copy")

        assertEquals(Outcome.Failure(DomainError.NothingToBackUp), backUp(drive, people = noRenters)())
        assertEquals("the good copy", drive.file)
    }

    @Test
    fun whenThePhonesDataCantBeReadNothingIsUploaded() = runTest {
        val drive = FakeDriveBackupRepository(file = "older backup")

        assertEquals(Outcome.Failure(DomainError.Storage("disk")), backUp(drive, backups = FakeBackupRepository(failExport = true))())
        assertEquals("older backup", drive.file)
    }

    @Test
    fun restoringPutsTheDataAndItsMeterPhotosBackOnThePhone() = runTest {
        val backups = FakeBackupRepository()
        val photos = FakeMeterPhotoStore(mutableMapOf("oct.jpg" to byteArrayOf(1)))
        val drive = FakeDriveBackupRepository(file = """{"from":"drive"}""", photosInDrive = mutableMapOf("oct.jpg" to byteArrayOf(7), "nov.jpg" to byteArrayOf(2)))

        assertEquals(Outcome.Success(Unit), RestoreFromDriveUseCase(drive, backups, bills, photos)())

        assertEquals(listOf("""{"from":"drive"}"""), backups.restored)
        assertEquals(listOf(2.toByte()), photos.files["nov.jpg"]?.toList())
        assertEquals(listOf(1.toByte()), photos.files["oct.jpg"]?.toList()) // a photo already on the phone is left alone
    }

    @Test
    fun withNoBackupInDriveNothingChangesOnThePhone() = runTest {
        val backups = FakeBackupRepository()

        assertEquals(Outcome.Failure(DomainError.NotFound), RestoreFromDriveUseCase(FakeDriveBackupRepository(file = null), backups, bills, FakeMeterPhotoStore())())
        assertEquals(emptyList(), backups.restored)
    }

    private fun auto(drive: FakeDriveBackupRepository, backups: FakeBackupRepository = FakeBackupRepository(), people: FakeRenterRepository = renters) =
        AutoBackUpToDriveUseCase(drive, backups, backUp(drive, backups, people = people))

    @Test
    fun openingTheAppBacksUpWhenSomethingChangedSinceTheLastBackup() = runTest {
        val drive = FakeDriveBackupRepository(file = "older", synced = "fp-before")

        auto(drive, FakeBackupRepository(exportJson = """{"new":"bill"}""", fingerprint = "fp-after"))()

        assertEquals("""{"new":"bill"}""", drive.file)
        assertEquals("fp-after", drive.synced)
    }

    @Test
    fun whenNothingChangedNothingIsSent() = runTest {
        val drive = FakeDriveBackupRepository(file = "same", synced = "fp-1")

        assertEquals(Outcome.Success(Unit), auto(drive, FakeBackupRepository(fingerprint = "fp-1"))())
        assertEquals(0, drive.uploads)
    }

    @Test
    fun theFirstBackupRunsByItselfAndNeverWhenDisconnectedOrEmpty() = runTest {
        val first = FakeDriveBackupRepository(file = null) // a first-ever backup: Drive has none
        auto(first)()
        assertEquals(1, first.uploads)

        val off = FakeDriveBackupRepository(connected = false)
        auto(off)()
        assertEquals(0, off.uploads)

        val newPhone = FakeDriveBackupRepository(file = null)
        assertEquals(Outcome.Success(Unit), auto(newPhone, people = noRenters)())
        assertEquals(0, newPhone.uploads)
    }

    @Test
    fun theAutomaticBackupNeverReplacesADriveBackupThisPhoneHasNotSeen() = runTest {
        // A new phone with one test renter, or a second phone on the same Google account: Drive has the real backup.
        val drive = FakeDriveBackupRepository(file = "the full backup", synced = null)

        assertEquals(Outcome.Success(Unit), auto(drive)())
        assertEquals("the full backup", drive.file)
    }

    @Test
    fun withoutInternetAPhoneThatHasNotSeenTheDriveBackupWaits() = runTest {
        val drive = FakeDriveBackupRepository(fail = DomainError.DriveUnavailable, synced = null)

        assertEquals(Outcome.Failure(DomainError.DriveUnavailable), auto(drive)())
        assertEquals(0, drive.uploads)
    }

    @Test
    fun afterARestoreTheNextChangeIsBackedUp() = runTest {
        val backups = FakeBackupRepository(fingerprint = "fp-restored")
        val drive = FakeDriveBackupRepository(file = """{"from":"drive"}""", synced = null)
        RestoreFromDriveUseCase(drive, backups, bills, FakeMeterPhotoStore())()
        assertEquals("fp-restored", drive.synced)

        auto(drive, backups)() // nothing changed since the restore
        assertEquals(0, drive.uploads)

        backups.fingerprint = "fp-new-payment"
        auto(drive, backups)()
        assertEquals(1, drive.uploads)
    }

    @Test
    fun aBackupCountsAsDoneOnlyOnceThePhotosAreSavedToo() = runTest {
        val drive = FakeDriveBackupRepository()
        backUp(drive, FakeBackupRepository(fingerprint = "fp-9"))()
        assertEquals("fp-9", drive.synced)

        val failing = FakeDriveBackupRepository(fail = DomainError.DriveUnavailable)
        backUp(failing)()
        assertEquals(null, failing.synced)
    }

    @Test
    fun connectingHandsTheSignInToDrive() = runTest {
        val drive = FakeDriveBackupRepository(connected = false)

        ConnectDriveUseCase(drive)(DriveSignIn.Code(code = "4/abc", codeVerifier = "v"))

        assertEquals(listOf<DriveSignIn>(DriveSignIn.Code("4/abc", "v")), drive.signIns)
        assertEquals(Outcome.Success(DriveBackupStatus(true, null, true)), GetDriveStatusUseCase(drive, renters, TimeZone.UTC)())
    }

    @Test
    fun theStatusSaysWhetherThisPhoneHasRentersAndTheLastBackupDayInItsTimeZone() = runTest {
        val drive = FakeDriveBackupRepository(lastBackupAt = Instant.parse("2026-11-27T20:00:00Z")) // 1:30 AM on the 28th in India

        assertEquals(
            Outcome.Success(DriveBackupStatus(true, LocalDate(2026, 11, 28), phoneHasRenters = false)),
            GetDriveStatusUseCase(drive, noRenters, TimeZone.of("Asia/Kolkata"))(),
        )
    }

    @Test
    fun disconnectingStopsBackups() = runTest {
        val drive = FakeDriveBackupRepository()

        DisconnectDriveUseCase(drive)()

        assertEquals(Outcome.Failure(DomainError.DriveNotConnected), backUp(drive)())
    }

    @Test
    fun googleSendsTheSignInBackOnTheClientIdTurnedAround() {
        val client = GoogleClient("896399803306-vgj57dcgje0hc86cjumoj14bgvl9jjof.apps.googleusercontent.com")

        assertEquals("com.googleusercontent.apps.896399803306-vgj57dcgje0hc86cjumoj14bgvl9jjof:/oauth2redirect", client.redirectUri)
    }
}
