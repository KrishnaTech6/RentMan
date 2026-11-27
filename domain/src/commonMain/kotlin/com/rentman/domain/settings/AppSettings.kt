package com.rentman.domain.settings

import com.rentman.domain.Outcome
import com.rentman.domain.messages.MessageKind
import com.rentman.domain.money.Paise
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/** Settings for the whole app. A phone that has never saved any gets these defaults. */
data class AppSettings(
    /** The electricity rate per unit for every renter without an own rate. ₹8 to start with. */
    val globalRate: Paise = Paise(800),
    /** Minutes after midnight. 540 is 9:00 AM. */
    val reminderMinuteOfDay: Int = 540,
    /** 2 or 3 days between reminders, 0 for only once. */
    val reminderRepeatDays: Int = 3,
    val lastBackupAt: Instant? = null,
    /** The landlord's own wording for each WhatsApp message, or null for ours ([MessageKind.default]). */
    val billMessage: String? = null,
    val paymentMessage: String? = null,
    val monthReviewMessage: String? = null,
) {
    /** The wording to use for [kind]: the landlord's own, or ours. */
    fun messageFor(kind: MessageKind): String = when (kind) {
        MessageKind.Bill -> billMessage
        MessageKind.PaymentReceived -> paymentMessage
        MessageKind.MonthReview -> monthReviewMessage
    } ?: kind.default

    /** These settings with [text] as the wording for [kind]; null goes back to ours. */
    fun withMessage(kind: MessageKind, text: String?): AppSettings = when (kind) {
        MessageKind.Bill -> copy(billMessage = text)
        MessageKind.PaymentReceived -> copy(paymentMessage = text)
        MessageKind.MonthReview -> copy(monthReviewMessage = text)
    }

    companion object {
        /** The reminder intervals the landlord can pick: every 2 days, every 3 days, or only once (0). */
        val REPEAT_CHOICES = listOf(2, 3, 0)

        const val MINUTES_PER_DAY = 24 * 60
    }
}

interface SettingsRepository {
    fun observe(): Flow<Outcome<AppSettings>>

    /** Replaces all settings. Returns what was stored. */
    suspend fun save(settings: AppSettings): Outcome<AppSettings>

    /** Records when the last backup was made and leaves every other setting as it is, even if nothing was saved before. */
    suspend fun markBackedUp(at: Instant): Outcome<Unit>
}
