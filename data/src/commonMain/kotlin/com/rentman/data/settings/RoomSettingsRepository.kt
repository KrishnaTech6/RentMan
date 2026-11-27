package com.rentman.data.settings

import com.rentman.data.db.AppSettingsEntity
import com.rentman.data.db.SettingsDao
import com.rentman.data.safeCall
import com.rentman.data.safeFlow
import com.rentman.domain.Outcome
import com.rentman.domain.money.Paise
import com.rentman.domain.settings.AppSettings
import com.rentman.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

internal class RoomSettingsRepository(private val dao: SettingsDao) : SettingsRepository {
    /** No seeding: until something is saved there is no row, and the defaults apply. */
    override fun observe(): Flow<Outcome<AppSettings>> =
        safeFlow { dao.observe().map { it?.toDomain() ?: AppSettings() } }

    override suspend fun markBackedUp(at: Instant): Outcome<Unit> =
        safeCall { dao.markBackedUp(at.toEpochMilliseconds(), AppSettings().toEntity()) }

    override suspend fun save(settings: AppSettings): Outcome<AppSettings> = safeCall {
        dao.upsert(settings.toEntity())
        settings
    }
}

private fun AppSettings.toEntity() = AppSettingsEntity(
    globalRatePaise = globalRate.value,
    reminderMinuteOfDay = reminderMinuteOfDay,
    reminderRepeatDays = reminderRepeatDays,
    lastBackupAt = lastBackupAt?.toEpochMilliseconds(),
    billMessage = billMessage,
    paymentMessage = paymentMessage,
    monthReviewMessage = monthReviewMessage,
)

private fun AppSettingsEntity.toDomain() = AppSettings(
    globalRate = Paise(globalRatePaise),
    reminderMinuteOfDay = reminderMinuteOfDay,
    reminderRepeatDays = reminderRepeatDays,
    lastBackupAt = lastBackupAt?.let(Instant::fromEpochMilliseconds),
    billMessage = billMessage,
    paymentMessage = paymentMessage,
    monthReviewMessage = monthReviewMessage,
)
