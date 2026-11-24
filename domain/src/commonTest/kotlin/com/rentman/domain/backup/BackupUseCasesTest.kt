package com.rentman.domain.backup

import app.cash.turbine.test
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.FakeSettingsRepository
import com.rentman.domain.settings.AppSettings
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class BackupUseCasesTest {
    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-11-24T19:00:00Z") // 00:30 on 25 Nov in India
    }

    private fun export(
        backups: FakeBackupRepository = FakeBackupRepository(),
        zone: TimeZone = TimeZone.UTC,
    ) = ExportBackupUseCase(backups, clock, zone)

    @Test
    fun exportingGivesTheJsonAndANameWithTodaysDate() = runTest {
        val backups = FakeBackupRepository(exportJson = """{"schemaVersion":1,"x":1}""")

        val result = (export(backups)() as Outcome.Success).value

        assertEquals(BackupExport("rentman-backup-2026-11-24.json", """{"schemaVersion":1,"x":1}"""), result)
    }

    @Test
    fun theFileNameUsesTheLandlordsDayNotUtc() = runTest {
        val result = (export(zone = TimeZone.of("Asia/Kolkata"))() as Outcome.Success).value

        assertEquals("rentman-backup-2026-11-25.json", result.fileName)
    }

    @Test
    fun onceTheFileWasHandedToTheShareSheetTheBackupTimeIsRecordedAndTheOtherSettingsStay() = runTest {
        val settings = FakeSettingsRepository(AppSettings(reminderRepeatDays = 2))

        assertEquals(Outcome.Success(Unit), MarkBackedUpUseCase(settings, clock)())

        assertEquals(clock.now(), settings.current.lastBackupAt)
        assertEquals(2, settings.current.reminderRepeatDays)
    }

    @Test
    fun aFailureRecordingTheBackupTimeIsReported() = runTest {
        val result = MarkBackedUpUseCase(FakeSettingsRepository(failSave = true), clock)()

        assertEquals(Outcome.Failure(DomainError.Storage("disk")), result)
    }

    @Test
    fun aFailedExportIsReported() = runTest {
        val result = export(backups = FakeBackupRepository(failExport = true))()

        assertEquals(Outcome.Failure(DomainError.Storage("disk")), result)
    }

    @Test
    fun restoringHandsTheFileTextOnAndReportsAFailureAsIs() = runTest {
        val backups = FakeBackupRepository()

        assertEquals(Outcome.Success(Unit), RestoreBackupUseCase(backups)("""{"a":1}"""))
        assertEquals(listOf("""{"a":1}"""), backups.restored)

        val broken = FakeBackupRepository(failRestore = DomainError.InvalidBackup)
        assertEquals(Outcome.Failure(DomainError.InvalidBackup), RestoreBackupUseCase(broken)("nope"))
        assertEquals(emptyList(), broken.restored)
    }

    @Test
    fun undoingARestoreUsesUpTheSnapshotAndFailsWhenThereIsNone() = runTest {
        val backups = FakeBackupRepository(snapshotTakenAt = Instant.parse("2026-11-01T10:00:00Z"))

        assertEquals(Outcome.Success(Unit), UndoRestoreUseCase(backups)())
        assertEquals(1, backups.snapshotRestores)
        assertEquals(Outcome.Failure(DomainError.NotFound), UndoRestoreUseCase(backups)())
    }

    @Test
    fun theStatusShowsTheLastBackupAndWhenUndoIsAvailableFromAsDaysInTheLandlordsZone() = runTest {
        // 19:00 UTC on 1 Nov is already 00:30 on 2 Nov in India.
        val settings = FakeSettingsRepository(AppSettings(lastBackupAt = Instant.parse("2026-11-01T19:00:00Z")))
        val backups = FakeBackupRepository()

        ObserveBackupStatusUseCase(settings, backups, TimeZone.of("Asia/Kolkata"))().test {
            assertEquals(Outcome.Success(BackupStatus(LocalDate(2026, 11, 2), undoSinceOn = null)), awaitItem())

            RestoreBackupUseCase(backups)("{}") // the fake takes its snapshot at 2026-12-01T10:00Z

            assertEquals(LocalDate(2026, 12, 1), (awaitItem() as Outcome.Success).value.undoSinceOn)
        }
    }

    @Test
    fun noBackupYetIsNull() = runTest {
        ObserveBackupStatusUseCase(FakeSettingsRepository(), FakeBackupRepository(), TimeZone.UTC)().test {
            assertEquals(Outcome.Success(BackupStatus(null, null)), awaitItem())
        }
    }

    @Test
    fun theStatusReportsAFailureReadingEitherPart() = runTest {
        ObserveBackupStatusUseCase(FakeSettingsRepository(failReads = true), FakeBackupRepository(), TimeZone.UTC)().test {
            assertEquals(Outcome.Failure(DomainError.Storage("disk")), awaitItem())
        }
    }
}
