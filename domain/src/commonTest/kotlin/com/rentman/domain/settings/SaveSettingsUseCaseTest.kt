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
    private val initial = AppSettings(reminderMinuteOfDay = 600, reminderRepeatDays = 2, lastBackupAt = backedUpAt)
    private val repository = FakeSettingsRepository(initial)
    private val save = SaveSettingsUseCase(repository)

    private fun form(rate: String = "9.50") = SettingsForm(rate)

    private fun problems(outcome: Outcome<AppSettings>) = ((outcome as Outcome.Failure).error as DomainError.InvalidSettings).problems

    @Test
    fun savesTheTypedRateAndLeavesTheReminderSettingsAndLastBackupAsTheyAre() = runTest {
        val saved = (save(form()) as Outcome.Success).value

        assertEquals(initial.copy(globalRate = Paise(950)), saved)
        assertEquals(saved, repository.current)
    }

    @Test
    fun aBlankRateIsRequiredAndABadOneIsInvalid() = runTest {
        assertEquals(mapOf(SettingsField.Rate to FieldProblem.Required), problems(save(form(rate = " "))))
        listOf("abc", "0", "8.505", "-8").forEach {
            assertEquals(mapOf(SettingsField.Rate to FieldProblem.Invalid), problems(save(form(rate = it))), "'$it'")
        }
        assertEquals(initial, repository.current) // nothing was saved
    }

    @Test
    fun aStorageFailureOnSaveOrOnReadingTheCurrentSettingsIsReported() = runTest {
        assertEquals(Outcome.Failure(DomainError.Storage("disk")), SaveSettingsUseCase(FakeSettingsRepository(failSave = true))(form()))
        assertEquals(Outcome.Failure(DomainError.Storage("disk")), SaveSettingsUseCase(FakeSettingsRepository(failReads = true))(form()))
    }

    @Test
    fun observingGivesTheCurrentSettings() = runTest {
        ObserveSettingsUseCase(repository)().test {
            assertEquals(Outcome.Success(initial), awaitItem())
        }
    }
}
