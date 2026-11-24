package com.rentman.domain.settings

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.flatMap
import com.rentman.domain.money.MoneyInput
import com.rentman.domain.tenancy.FieldProblem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** What a person typed on the Settings screen, before any checking. Only the rate needs Save: the reminder time and interval save as soon as they change. */
data class SettingsForm(val rate: String)

enum class SettingsField { Rate, Repeat }

class ObserveSettingsUseCase(private val settings: SettingsRepository) {
    operator fun invoke(): Flow<Outcome<AppSettings>> = settings.observe()
}

class SaveSettingsUseCase(private val settings: SettingsRepository) {
    suspend operator fun invoke(form: SettingsForm): Outcome<AppSettings> {
        val rate = MoneyInput.rate(form.rate)
            ?: return Outcome.Failure(
                DomainError.InvalidSettings(mapOf(SettingsField.Rate to if (form.rate.isBlank()) FieldProblem.Required else FieldProblem.Invalid)),
            )
        // Only the rate is edited here. The reminder time and interval are saved as they change, and the last backup is kept as it is.
        return settings.observe().first().flatMap { current -> settings.save(current.copy(globalRate = rate)) }
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
