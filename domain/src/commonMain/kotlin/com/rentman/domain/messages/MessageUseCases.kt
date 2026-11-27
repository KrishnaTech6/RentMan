package com.rentman.domain.messages

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.flatMap
import com.rentman.domain.map
import com.rentman.domain.settings.AppSettings
import com.rentman.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** The wording to use for [MessageKind], the landlord's own or ours. Emits again when it is changed. */
class ObserveMessageUseCase(private val settings: SettingsRepository) {
    operator fun invoke(kind: MessageKind): Flow<Outcome<String>> = settings.observe().map { outcome -> outcome.map { it.messageFor(kind) } }
}

/**
 * Saves the landlord's own wording for [MessageKind], keeping every other setting. A variable the app doesn't fill in is refused.
 * Empty text, or our own wording, is stored as "ours", so later changes to our wording still reach this landlord.
 */
class SaveMessageUseCase(private val settings: SettingsRepository) {
    suspend operator fun invoke(kind: MessageKind, text: String): Outcome<AppSettings> {
        val unknown = MessageTemplate.unknownVariables(text, kind)
        if (unknown.isNotEmpty()) return Outcome.Failure(DomainError.InvalidMessage(unknown))
        val own = text.takeIf { it.isNotBlank() && it != kind.default }
        return settings.observe().first().flatMap { current -> settings.save(current.withMessage(kind, own)) }
    }
}
