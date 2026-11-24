package com.rentman.domain.settings

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.FakeSettingsRepository
import com.rentman.domain.money.Paise
import com.rentman.domain.tenancy.FieldProblem
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ReminderSettingsUseCasesTest {
    private val backedUpAt = Instant.parse("2026-11-01T10:00:00Z")
    private val saved = AppSettings(globalRate = Paise(950), reminderMinuteOfDay = 540, reminderRepeatDays = 2, lastBackupAt = backedUpAt)
    private val repository = FakeSettingsRepository(saved)

    @Test
    fun steppingTheTimeSavesItAtOnceAndKeepsTheRateAndLastBackup() = runTest {
        val result = (StepReminderTimeUseCase(repository)(60) as Outcome.Success).value

        assertEquals(saved.copy(reminderMinuteOfDay = 600), result)
        assertEquals(result, repository.current)
    }

    @Test
    fun theTimeWrapsAroundMidnightInBothDirections() = runTest {
        val late = FakeSettingsRepository(saved.copy(reminderMinuteOfDay = 23 * 60 + 30))
        assertEquals(0, (StepReminderTimeUseCase(late)(30) as Outcome.Success).value.reminderMinuteOfDay)
        assertEquals(23 * 60 + 30, (StepReminderTimeUseCase(late)(-30) as Outcome.Success).value.reminderMinuteOfDay)
    }

    @Test
    fun theRepeatIntervalIsSavedAtOnceForEveryAllowedChoice() = runTest {
        AppSettings.REPEAT_CHOICES.forEach { days ->
            assertEquals(days, (SetReminderRepeatUseCase(repository)(days) as Outcome.Success).value.reminderRepeatDays)
            assertEquals(days, repository.current.reminderRepeatDays)
        }
        assertEquals(saved.globalRate, repository.current.globalRate)
        assertEquals(listOf(2, 3, 0), AppSettings.REPEAT_CHOICES)
    }

    @Test
    fun anIntervalThatIsNotOneOfTheChoicesIsRefusedAndNothingChanges() = runTest {
        val outcome = SetReminderRepeatUseCase(repository)(5) as Outcome.Failure

        assertEquals(DomainError.InvalidSettings(mapOf(SettingsField.Repeat to FieldProblem.Invalid)), outcome.error)
        assertEquals(saved, repository.current)
    }
}
