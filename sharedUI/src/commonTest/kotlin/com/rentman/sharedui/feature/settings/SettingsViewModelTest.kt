package com.rentman.sharedui.feature.settings

import app.cash.turbine.test
import com.rentman.domain.DomainError
import com.rentman.domain.money.Paise
import com.rentman.domain.settings.AppSettings
import com.rentman.domain.settings.ObserveSettingsUseCase
import com.rentman.domain.settings.SaveSettingsUseCase
import com.rentman.domain.settings.SetReminderRepeatUseCase
import com.rentman.domain.settings.StepReminderTimeUseCase
import com.rentman.domain.settings.SettingsField
import com.rentman.domain.tenancy.FieldProblem
import com.rentman.sharedui.feature.bill.FakeSettingsRepository
import com.rentman.sharedui.feature.settings.SettingsIntent.Back
import com.rentman.sharedui.feature.settings.SettingsIntent.RateChanged
import com.rentman.sharedui.feature.settings.SettingsIntent.RepeatChanged
import com.rentman.sharedui.feature.settings.SettingsIntent.Retry
import com.rentman.sharedui.feature.settings.SettingsIntent.Save
import com.rentman.sharedui.feature.settings.SettingsIntent.TimeStepped
import com.rentman.sharedui.feature.settings.SettingsUiState.Content
import com.rentman.sharedui.feature.settings.SettingsUiState.Error
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import com.rentman.domain.backup.ExportBackupUseCase
import com.rentman.domain.backup.MarkBackedUpUseCase
import com.rentman.domain.backup.ObserveBackupStatusUseCase
import com.rentman.domain.backup.RestoreBackupUseCase
import com.rentman.domain.backup.UndoRestoreUseCase
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant
import com.rentman.sharedui.feature.settings.SettingsIntent.BackUpNow
import com.rentman.sharedui.feature.settings.SettingsIntent.BackupShareFailed
import com.rentman.sharedui.feature.settings.SettingsIntent.BackupShared
import com.rentman.sharedui.feature.settings.SettingsIntent.RestoreFrom
import com.rentman.sharedui.feature.settings.SettingsIntent.UndoRestore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @BeforeTest
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-11-24T10:00:00Z")
    }

    private fun viewModel(
        repository: FakeSettingsRepository = FakeSettingsRepository(),
        backups: FakeBackupRepository = FakeBackupRepository(repository),
    ) = SettingsViewModel(
        observeSettings = ObserveSettingsUseCase(repository),
        observeBackupStatus = ObserveBackupStatusUseCase(repository, backups, TimeZone.UTC),
        saveSettings = SaveSettingsUseCase(repository),
        stepReminderTime = StepReminderTimeUseCase(repository),
        setReminderRepeat = SetReminderRepeatUseCase(repository),
        exportBackup = ExportBackupUseCase(backups, clock, TimeZone.UTC),
        markBackedUp = MarkBackedUpUseCase(repository, clock),
        restoreBackup = RestoreBackupUseCase(backups),
        undoRestore = UndoRestoreUseCase(backups),
    )

    private fun SettingsViewModel.content() = uiState.value as Content

    @Test
    fun startsFromTheSavedSettingsOrTheDefaults() = runTest {
        val content = viewModel().content()

        assertEquals("8", content.rate)
        assertEquals(540, content.reminderMinuteOfDay)
        assertEquals(3, content.reminderRepeatDays)
    }

    @Test
    fun showsAPaiseRateWithItsDecimals() = runTest {
        val vm = viewModel(FakeSettingsRepository(AppSettings(globalRate = Paise(850))))

        assertEquals("8.50", vm.content().rate)
    }

    @Test
    fun theRateTheTimeAndTheRepeatCanBeChanged() = runTest {
        val vm = viewModel()

        vm.onIntent(RateChanged("9.5"))
        vm.onIntent(TimeStepped(30))
        vm.onIntent(RepeatChanged(0))

        val content = vm.content()
        assertEquals(Triple("9.5", 570, 0), Triple(content.rate, content.reminderMinuteOfDay, content.reminderRepeatDays))
    }

    @Test
    fun theTimeWrapsAroundMidnightInBothDirections() = runTest {
        val vm = viewModel(FakeSettingsRepository(AppSettings(reminderMinuteOfDay = 23 * 60 + 30)))

        vm.onIntent(TimeStepped(30))
        assertEquals(0, vm.content().reminderMinuteOfDay)

        vm.onIntent(TimeStepped(-30))
        assertEquals(23 * 60 + 30, vm.content().reminderMinuteOfDay)
    }

    @Test
    fun theReminderTimeAndRepeatAreSavedAtOnceWithoutPressingSave() = runTest {
        val repository = FakeSettingsRepository()
        val vm = viewModel(repository)

        vm.onIntent(TimeStepped(60))
        vm.onIntent(RepeatChanged(2))

        assertEquals(Pair(600, 2), Pair(repository.current.reminderMinuteOfDay, repository.current.reminderRepeatDays))
        assertEquals(Pair(600, 2), Pair(vm.content().reminderMinuteOfDay, vm.content().reminderRepeatDays))
    }

    @Test
    fun theSavedTimeIsStillThereWhenTheScreenIsOpenedAgain() = runTest {
        val repository = FakeSettingsRepository()
        viewModel(repository).onIntent(TimeStepped(60))

        assertEquals(600, viewModel(repository).content().reminderMinuteOfDay)
    }

    @Test
    fun aRateStillBeingTypedIsKeptWhenTheTimeIsSavedAndIsNotSavedWithIt() = runTest {
        val repository = FakeSettingsRepository()
        val vm = viewModel(repository)
        vm.onIntent(RateChanged("9.5"))

        vm.onIntent(TimeStepped(30))

        assertEquals("9.5", vm.content().rate)
        assertEquals(Paise(800), repository.current.globalRate)
    }

    @Test
    fun aFailedTimeSaveShowsTheErrorAndKeepsTheSavedTime() = runTest {
        val repository = FakeSettingsRepository()
        val vm = viewModel(repository)
        repository.failSave = true

        vm.onIntent(TimeStepped(60))

        assertEquals(DomainError.Storage("disk"), vm.content().saveError)
        assertEquals(540, vm.content().reminderMinuteOfDay)
        assertEquals(540, repository.current.reminderMinuteOfDay)
    }

    @Test
    fun savingStoresTheSettingsAndClosesTheScreen() = runTest {
        val repository = FakeSettingsRepository()
        val vm = viewModel(repository)
        vm.onIntent(RateChanged("9.50"))
        vm.onIntent(TimeStepped(60))
        vm.onIntent(RepeatChanged(2))

        vm.navigation.test {
            vm.onIntent(Save(vm.content().toForm()))

            assertEquals(SettingsNavEvent.Close, awaitItem())
        }
        assertEquals(AppSettings(globalRate = Paise(950), reminderMinuteOfDay = 600, reminderRepeatDays = 2), repository.current)
    }

    @Test
    fun aBadRateShowsTheProblemStoresNothingAndTypingClearsIt() = runTest {
        val repository = FakeSettingsRepository()
        val vm = viewModel(repository)
        vm.onIntent(RateChanged("abc"))

        vm.navigation.test {
            vm.onIntent(Save(vm.content().toForm()))

            expectNoEvents()
        }
        assertEquals(mapOf(SettingsField.Rate to FieldProblem.Invalid), vm.content().problems)
        assertEquals(false, vm.content().isSaving)
        assertEquals(AppSettings(), repository.current)

        vm.onIntent(RateChanged("9"))
        assertEquals(emptyMap(), vm.content().problems)
    }

    @Test
    fun aStorageFailureKeepsTheFormAndShowsTheError() = runTest {
        val vm = viewModel(FakeSettingsRepository(failSave = true))
        vm.onIntent(RateChanged("9"))

        vm.navigation.test {
            vm.onIntent(Save(vm.content().toForm()))

            expectNoEvents()
        }
        assertEquals(DomainError.Storage("disk"), vm.content().saveError)
        assertEquals("9", vm.content().rate)
        assertEquals(false, vm.content().isSaving)
    }

    @Test
    fun backAsksTheScreenToClose() = runTest {
        val vm = viewModel()

        vm.navigation.test {
            vm.onIntent(Back)

            assertEquals(SettingsNavEvent.Close, awaitItem())
        }
    }

    @Test
    fun aStorageFailureShowsErrorAndRetryRecovers() = runTest {
        val repository = FakeSettingsRepository(failReads = true)
        val vm = viewModel(repository)
        assertIs<Error>(vm.uiState.value)

        repository.failReads = false
        vm.onIntent(Retry)

        assertIs<Content>(vm.uiState.value)
    }

    @Test
    fun backingUpAsksTheScreenToShareTheFileButDoesNotYetCountAsABackup() = runTest {
        val vm = viewModel()

        vm.navigation.test {
            vm.onIntent(BackUpNow)

            assertEquals(SettingsNavEvent.ShareBackup("rentman-backup-2026-11-24.json", """{"schemaVersion":1}"""), awaitItem())
        }
        assertEquals(null, vm.content().lastBackupOn)
        assertEquals(false, vm.content().isWorking)
    }

    @Test
    fun onceTheShareSheetOpenedTheDayIsRecordedAsTheLastBackup() = runTest {
        val vm = viewModel()
        vm.onIntent(BackUpNow)

        vm.onIntent(BackupShared)

        assertEquals(LocalDate(2026, 11, 24), vm.content().lastBackupOn)
        assertEquals(null, vm.content().backupMessage)
    }

    @Test
    fun ifTheShareSheetCouldNotOpenTheScreenSaysSoAndNoBackupIsRecorded() = runTest {
        val vm = viewModel()
        vm.onIntent(BackUpNow)

        vm.onIntent(BackupShareFailed)

        assertEquals(BackupMessage.ShareFailed, vm.content().backupMessage)
        assertEquals(null, vm.content().lastBackupOn)
    }

    @Test
    fun aFailureRecordingTheBackupShowsAnError() = runTest {
        val repository = FakeSettingsRepository()
        val vm = viewModel(repository, FakeBackupRepository(repository))
        vm.onIntent(BackUpNow)
        repository.failSave = true

        vm.onIntent(BackupShared)

        assertEquals(DomainError.Storage("disk"), vm.content().backupError)
    }

    @Test
    fun aFailedBackupShowsAnErrorAndSharesNothing() = runTest {
        val repository = FakeSettingsRepository()
        val vm = viewModel(repository, FakeBackupRepository(repository, failExport = true))

        vm.navigation.test {
            vm.onIntent(BackUpNow)

            expectNoEvents()
        }
        assertEquals(DomainError.Storage("disk"), vm.content().backupError)
        assertEquals(false, vm.content().isWorking)
        assertEquals(null, vm.content().lastBackupOn)
    }

    @Test
    fun makingABackupKeepsWhatWasTypedInTheForm() = runTest {
        val vm = viewModel()
        vm.onIntent(RateChanged("9.5"))

        vm.onIntent(BackUpNow)
        vm.onIntent(BackupShared) // this changes the saved settings (the backup time)

        assertEquals("9.5", vm.content().rate)
    }

    @Test
    fun restoringShowsAMessageOffersUndoAndFillsTheFormFromTheRestoredSettings() = runTest {
        val repository = FakeSettingsRepository()
        val backups = FakeBackupRepository(repository, restoredSettings = AppSettings(globalRate = Paise(1_025), reminderMinuteOfDay = 1_080, reminderRepeatDays = 2))
        val vm = viewModel(repository, backups)
        vm.onIntent(RateChanged("7")) // typed, not saved

        vm.onIntent(RestoreFrom("{...}"))

        val content = vm.content()
        assertEquals(listOf("{...}"), backups.restored)
        assertEquals(BackupMessage.Restored, content.backupMessage)
        assertEquals(LocalDate(2026, 12, 1), content.undoRestoreOn)
        assertEquals(Triple("10.25", 1_080, 2), Triple(content.rate, content.reminderMinuteOfDay, content.reminderRepeatDays))
    }

    @Test
    fun aFileThatIsNotABackupShowsTheErrorAndKeepsEverything() = runTest {
        val repository = FakeSettingsRepository()
        val vm = viewModel(repository, FakeBackupRepository(repository, failRestore = DomainError.InvalidBackup))
        vm.onIntent(RateChanged("9"))

        vm.onIntent(RestoreFrom("nope"))

        assertEquals(DomainError.InvalidBackup, vm.content().backupError)
        assertEquals(null, vm.content().backupMessage)
        assertEquals(null, vm.content().undoRestoreOn)
        assertEquals("9", vm.content().rate)
        assertEquals(false, vm.content().isWorking)
    }

    @Test
    fun undoingARestoreBringsTheOldSettingsBackAndUsesUpTheOffer() = runTest {
        val repository = FakeSettingsRepository()
        val backups = FakeBackupRepository(repository, restoredSettings = AppSettings(globalRate = Paise(1_025)))
        val vm = viewModel(repository, backups)
        vm.onIntent(RestoreFrom("{...}"))
        assertEquals("10.25", vm.content().rate)

        vm.onIntent(UndoRestore)

        val content = vm.content()
        assertEquals(BackupMessage.RestoreUndone, content.backupMessage)
        assertEquals(null, content.undoRestoreOn)
        assertEquals("8", content.rate)
    }

    @Test
    fun aNewBackupActionClearsTheEarlierMessage() = runTest {
        val repository = FakeSettingsRepository()
        val vm = viewModel(repository, FakeBackupRepository(repository))
        vm.onIntent(RestoreFrom("{...}"))
        assertEquals(BackupMessage.Restored, vm.content().backupMessage)

        vm.onIntent(BackUpNow)

        assertEquals(null, vm.content().backupMessage)
    }
}
