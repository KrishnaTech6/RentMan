package com.rentman.domain.settings

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.flatMap
import com.rentman.domain.money.MoneyInput
import com.rentman.domain.tenancy.FieldProblem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** What a person typed or picked on the Settings screen, before any checking. */
data class SettingsForm(val rate: String, val reminderMinuteOfDay: Int, val reminderRepeatDays: Int)

enum class SettingsField { Rate, Time, Repeat }

class ObserveSettingsUseCase(private val settings: SettingsRepository) {
    operator fun invoke(): Flow<Outcome<AppSettings>> = settings.observe()
}

class SaveSettingsUseCase(private val settings: SettingsRepository) {
    suspend operator fun invoke(form: SettingsForm): Outcome<AppSettings> {
        val rate = MoneyInput.rate(form.rate)
        val problems = listOf(
            SettingsField.Rate to when {
                form.rate.isBlank() -> FieldProblem.Required
                rate == null -> FieldProblem.Invalid
                else -> null
            },
            SettingsField.Time to FieldProblem.Invalid.takeUnless { form.reminderMinuteOfDay in 0 until AppSettings.MINUTES_PER_DAY },
            SettingsField.Repeat to FieldProblem.Invalid.takeUnless { form.reminderRepeatDays in AppSettings.REPEAT_CHOICES },
        ).mapNotNull { (field, problem) -> problem?.let { field to it } }.toMap()
        if (problems.isNotEmpty() || rate == null) return Outcome.Failure(DomainError.InvalidSettings(problems))
        // Only these three are edited here. The rest (the last backup) is kept as it is.
        return settings.observe().first().flatMap { current ->
            settings.save(
                current.copy(globalRate = rate, reminderMinuteOfDay = form.reminderMinuteOfDay, reminderRepeatDays = form.reminderRepeatDays),
            )
        }
    }
}

/**
 * Moves the saved reminder time by [minutes], around midnight, and keeps everything else. It applies at once: the time has no
 * wrong value to check, so it needs no Save button, and the reminders are planned again from the saved settings.
 */
class StepReminderTimeUseCase(private val settings: SettingsRepository) {
    suspend operator fun invoke(minutes: Int): Outcome<AppSettings> =
        settings.observe().first().flatMap { current ->
            settings.save(current.copy(reminderMinuteOfDay = (current.reminderMinuteOfDay + minutes).mod(AppSettings.MINUTES_PER_DAY)))
        }
}

/** Saves the repeat interval at once, if it is one of [AppSettings.REPEAT_CHOICES]. Everything else is kept. */
class SetReminderRepeatUseCase(private val settings: SettingsRepository) {
    suspend operator fun invoke(days: Int): Outcome<AppSettings> {
        if (days !in AppSettings.REPEAT_CHOICES) {
            return Outcome.Failure(DomainError.InvalidSettings(mapOf(SettingsField.Repeat to FieldProblem.Invalid)))
        }
        return settings.observe().first().flatMap { current -> settings.save(current.copy(reminderRepeatDays = days)) }
    }
}
