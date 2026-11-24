package com.rentman.domain.settings

import app.cash.turbine.test
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.FakeSettingsRepository
import com.rentman.domain.money.Paise
import com.rentman.domain.tenancy.FieldProblem
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SaveSettingsUseCaseTest {
    private val backedUpAt = Instant.parse("2026-11-01T10:00:00Z")
    private val repository = FakeSettingsRepository(AppSettings(lastBackupAt = backedUpAt))
    private val save = SaveSettingsUseCase(repository)

    private fun form(rate: String = "9.50", minute: Int = 600, repeat: Int = 2) = SettingsForm(rate, minute, repeat)

    private fun problems(outcome: Outcome<AppSettings>) = ((outcome as Outcome.Failure).error as DomainError.InvalidSettings).problems

    @Test
    fun savesTheTypedRateTimeAndRepeatAndKeepsTheLastBackup() = runTest {
        val saved = (save(form()) as Outcome.Success).value

        assertEquals(AppSettings(globalRate = Paise(950), reminderMinuteOfDay = 600, reminderRepeatDays = 2, lastBackupAt = backedUpAt), saved)
        assertEquals(saved, repository.current)
    }

    @Test
    fun everyAllowedRepeatChoiceIsAccepted() = runTest {
        AppSettings.REPEAT_CHOICES.forEach { days ->
            assertEquals(days, (save(form(repeat = days)) as Outcome.Success).value.reminderRepeatDays)
        }
        assertEquals(listOf(2, 3, 0), AppSettings.REPEAT_CHOICES)
    }

    @Test
    fun theFirstAndLastMinuteOfTheDayAreAccepted() = runTest {
        assertEquals(0, (save(form(minute = 0)) as Outcome.Success).value.reminderMinuteOfDay)
        assertEquals(1_439, (save(form(minute = 1_439)) as Outcome.Success).value.reminderMinuteOfDay)
    }

    @Test
    fun aBlankRateIsRequiredAndABadOneIsInvalid() = runTest {
        assertEquals(mapOf(SettingsField.Rate to FieldProblem.Required), problems(save(form(rate = " "))))
        listOf("abc", "0", "8.505", "-8").forEach {
            assertEquals(mapOf(SettingsField.Rate to FieldProblem.Invalid), problems(save(form(rate = it))), "'$it'")
        }
        assertEquals(AppSettings(lastBackupAt = backedUpAt), repository.current) // nothing was saved
    }

    @Test
    fun aTimeOutsideTheDayAndAnUnknownRepeatAreInvalid() = runTest {
        assertEquals(mapOf(SettingsField.Time to FieldProblem.Invalid), problems(save(form(minute = -1))))
        assertEquals(mapOf(SettingsField.Time to FieldProblem.Invalid), problems(save(form(minute = 1_440))))
        assertEquals(mapOf(SettingsField.Repeat to FieldProblem.Invalid), problems(save(form(repeat = 5))))
    }

    @Test
    fun everyProblemIsReportedAtOnce() = runTest {
        assertEquals(setOf(SettingsField.Rate, SettingsField.Time, SettingsField.Repeat), problems(save(form("", -5, 7))).keys)
    }

    @Test
    fun aStorageFailureOnSaveOrOnReadingTheCurrentSettingsIsReported() = runTest {
        assertEquals(Outcome.Failure(DomainError.Storage("disk")), SaveSettingsUseCase(FakeSettingsRepository(failSave = true))(form()))
        assertEquals(Outcome.Failure(DomainError.Storage("disk")), SaveSettingsUseCase(FakeSettingsRepository(failReads = true))(form()))
    }

    @Test
    fun observingGivesTheCurrentSettings() = runTest {
        ObserveSettingsUseCase(repository)().test {
            assertEquals(Outcome.Success(AppSettings(lastBackupAt = backedUpAt)), awaitItem())
        }
    }
}
