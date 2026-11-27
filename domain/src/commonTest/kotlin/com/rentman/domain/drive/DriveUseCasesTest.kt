package com.rentman.domain.drive

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.backup.FakeBackupRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class DriveUseCasesTest {
    @Test
    fun backingUpSavesEverythingOnThePhoneToDrive() = runTest {
        val drive = FakeDriveBackupRepository()

        val outcome = BackUpToDriveUseCase(FakeBackupRepository(exportJson = """{"all":"data"}"""), drive)()

        assertEquals(Outcome.Success(Unit), outcome)
        assertEquals("""{"all":"data"}""", drive.file)
    }

    @Test
    fun whenThePhonesDataCantBeReadNothingIsUploaded() = runTest {
        val drive = FakeDriveBackupRepository(file = "older backup")

        val outcome = BackUpToDriveUseCase(FakeBackupRepository(failExport = true), drive)()

        assertEquals(Outcome.Failure(DomainError.Storage("disk")), outcome)
        assertEquals("older backup", drive.file) // the good copy in Drive is kept
    }

    @Test
    fun restoringPutsTheDriveBackupOnThePhone() = runTest {
        val backups = FakeBackupRepository()

        val outcome = RestoreFromDriveUseCase(FakeDriveBackupRepository(file = """{"from":"drive"}"""), backups)()

        assertEquals(Outcome.Success(Unit), outcome)
        assertEquals(listOf("""{"from":"drive"}"""), backups.restored)
    }

    @Test
    fun withNoBackupInDriveNothingChangesOnThePhone() = runTest {
        val backups = FakeBackupRepository()

        assertEquals(Outcome.Failure(DomainError.NotFound), RestoreFromDriveUseCase(FakeDriveBackupRepository(file = null), backups)())
        assertEquals(emptyList(), backups.restored)
    }

    @Test
    fun connectingHandsTheSignInToDrive() = runTest {
        val drive = FakeDriveBackupRepository(connected = false)

        ConnectDriveUseCase(drive)(DriveSignIn(code = "4/abc", codeVerifier = "v"))

        assertEquals(listOf(DriveSignIn("4/abc", "v")), drive.signIns)
        assertEquals(Outcome.Success(DriveBackupStatus(connected = true, lastBackupOn = null)), GetDriveStatusUseCase(drive, TimeZone.UTC)())
    }

    @Test
    fun disconnectingStopsBackups() = runTest {
        val drive = FakeDriveBackupRepository()

        DisconnectDriveUseCase(drive)()

        assertEquals(Outcome.Failure(DomainError.DriveNotConnected), BackUpToDriveUseCase(FakeBackupRepository(), drive)())
    }

    @Test
    fun googleSendsTheSignInBackOnTheClientIdTurnedAround() {
        val client = GoogleClient("896399803306-vgj57dcgje0hc86cjumoj14bgvl9jjof.apps.googleusercontent.com")

        assertEquals("com.googleusercontent.apps.896399803306-vgj57dcgje0hc86cjumoj14bgvl9jjof:/oauth2redirect", client.redirectUri)
    }

    @Test
    fun theLastBackupIsShownAsADayInThePhonesTimeZone() = runTest {
        val drive = FakeDriveBackupRepository()
        drive.lastBackupAt = kotlin.time.Instant.parse("2026-11-27T20:00:00Z") // 1:30 AM on the 28th in India

        assertEquals(
            Outcome.Success(DriveBackupStatus(true, LocalDate(2026, 11, 28))),
            GetDriveStatusUseCase(drive, TimeZone.of("Asia/Kolkata"))(),
        )
    }
}
