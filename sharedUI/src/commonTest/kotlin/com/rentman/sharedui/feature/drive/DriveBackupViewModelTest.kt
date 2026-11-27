package com.rentman.sharedui.feature.drive

import app.cash.turbine.test
import com.rentman.domain.DomainError
import com.rentman.domain.backup.UndoRestoreUseCase
import com.rentman.domain.drive.BackUpToDriveUseCase
import com.rentman.domain.drive.ConnectDriveUseCase
import com.rentman.domain.drive.DisconnectDriveUseCase
import com.rentman.domain.drive.DriveSignIn
import com.rentman.domain.drive.DriveBackupStatus
import com.rentman.domain.drive.GetDriveStatusUseCase
import com.rentman.domain.drive.RestoreFromDriveUseCase
import com.rentman.sharedui.feature.bill.FakeBillRepository
import com.rentman.sharedui.feature.bill.FakeSettingsRepository
import com.rentman.sharedui.feature.bill.Ravi
import com.rentman.sharedui.feature.renter.FakeRenterRepository
import com.rentman.sharedui.feature.drive.DriveBackupIntent.Back
import com.rentman.sharedui.feature.drive.DriveBackupIntent.BackUpNow
import com.rentman.sharedui.feature.drive.DriveBackupIntent.Disconnect
import com.rentman.sharedui.feature.drive.DriveBackupIntent.Restore
import com.rentman.sharedui.feature.drive.DriveBackupIntent.SignInFailed
import com.rentman.sharedui.feature.drive.DriveBackupIntent.SignedIn
import com.rentman.sharedui.feature.drive.DriveBackupIntent.UndoRestore
import com.rentman.sharedui.feature.drive.DriveBackupUiState.Content
import com.rentman.sharedui.feature.settings.FakeBackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DriveBackupViewModelTest {
    @BeforeTest
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    private val backups = FakeBackupRepository(FakeSettingsRepository())
    private val bills = FakeBillRepository()

    private fun viewModel(drive: FakeDriveBackupRepository, renters: FakeRenterRepository = FakeRenterRepository(listOf(Ravi.renter()))) = DriveBackupViewModel(
        getStatus = GetDriveStatusUseCase(drive, renters, TimeZone.UTC),
        connect = ConnectDriveUseCase(drive),
        backUp = BackUpToDriveUseCase(backups, drive, renters, bills, NoPhotos),
        restore = RestoreFromDriveUseCase(drive, backups, bills, NoPhotos),
        undoRestore = UndoRestoreUseCase(backups),
        disconnect = DisconnectDriveUseCase(drive),
    )

    private fun DriveBackupViewModel.content() = uiState.value as Content

    @Test
    fun showsWhetherDriveIsConnectedAndWhenItWasLastBackedUp() = runTest {
        assertEquals(DriveBackupStatus(false, null, true), viewModel(FakeDriveBackupRepository()).content().status)

        val at = Instant.parse("2026-11-27T09:00:00Z")
        assertEquals(DriveBackupStatus(true, LocalDate(2026, 11, 27), true), viewModel(FakeDriveBackupRepository(connected = true, lastBackupAt = at)).content().status)
    }

    @Test
    fun aFinishedSignInConnectsDrive() = runTest {
        val vm = viewModel(FakeDriveBackupRepository())

        vm.onIntent(SignedIn(DriveSignIn.Code("4/code", "verifier")))

        assertEquals(true, vm.content().status.connected)
        assertEquals(DriveMessage.Connected, vm.content().message)
        assertEquals(false, vm.content().isWorking)
    }

    @Test
    fun aSignInThatDidNotFinishSaysSo() = runTest {
        val vm = viewModel(FakeDriveBackupRepository())

        vm.onIntent(SignInFailed)

        assertEquals(DriveMessage.SignInFailed, vm.content().message)
        assertEquals(false, vm.content().status.connected)
    }

    @Test
    fun backUpNowSavesToDriveAndShowsTheNewTime() = runTest {
        val drive = FakeDriveBackupRepository(connected = true)
        val vm = viewModel(drive)

        vm.onIntent(BackUpNow)

        assertEquals("""{"schemaVersion":1}""", drive.file)
        assertEquals(DriveMessage.BackedUp, vm.content().message)
        assertEquals(LocalDate(2026, 11, 28), vm.content().status.lastBackupOn)
    }

    @Test
    fun restorePutsTheDriveBackupOnThePhoneAndCanBeUndone() = runTest {
        val vm = viewModel(FakeDriveBackupRepository(connected = true, file = """{"from":"drive"}"""))

        vm.onIntent(Restore)
        assertEquals(listOf("""{"from":"drive"}"""), backups.restored)
        assertEquals(DriveMessage.Restored, vm.content().message)

        vm.onIntent(UndoRestore)
        assertEquals(DriveMessage.RestoreUndone, vm.content().message)
    }

    @Test
    fun withNoBackupInDriveRestoreSaysSoAndChangesNothing() = runTest {
        val vm = viewModel(FakeDriveBackupRepository(connected = true, file = null))

        vm.onIntent(Restore)

        assertEquals(DriveMessage.NothingInDrive, vm.content().message)
        assertEquals(null, vm.content().error)
        assertEquals(emptyList(), backups.restored)
    }

    @Test
    fun onANewPhoneTheStatusSaysThereAreNoRentersSoRestoreComesFirst() = runTest {
        val vm = viewModel(FakeDriveBackupRepository(connected = true, lastBackupAt = Instant.parse("2026-11-27T09:00:00Z")), FakeRenterRepository())

        assertEquals(DriveBackupStatus(true, LocalDate(2026, 11, 27), phoneHasRenters = false), vm.content().status)
    }

    @Test
    fun backingUpAPhoneWithNoRentersIsRefusedAndDriveKeepsItsCopy() = runTest {
        val drive = FakeDriveBackupRepository(connected = true, file = "the good copy")
        val vm = viewModel(drive, FakeRenterRepository())

        vm.onIntent(BackUpNow)

        assertEquals(DomainError.NothingToBackUp, vm.content().error)
        assertEquals("the good copy", drive.file)
    }

    @Test
    fun disconnectShowsConnectAgain() = runTest {
        val vm = viewModel(FakeDriveBackupRepository(connected = true))

        vm.onIntent(Disconnect)

        assertEquals(false, vm.content().status.connected)
    }

    @Test
    fun withNoInternetTheErrorIsShownAndDriveStaysConnected() = runTest {
        val vm = viewModel(FakeDriveBackupRepository(connected = true, fail = DomainError.DriveUnavailable))

        vm.onIntent(BackUpNow)

        assertEquals(DomainError.DriveUnavailable, vm.content().error)
        assertEquals(true, vm.content().status.connected)
        assertEquals(false, vm.content().isWorking)
    }

    @Test
    fun backCloses() = runTest {
        val vm = viewModel(FakeDriveBackupRepository())

        vm.navigation.test {
            vm.onIntent(Back)

            assertEquals(DriveBackupNavEvent.Close, awaitItem())
        }
    }
}
