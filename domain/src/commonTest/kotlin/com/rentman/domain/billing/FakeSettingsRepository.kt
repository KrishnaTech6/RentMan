package com.rentman.domain.billing

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.settings.AppSettings
import com.rentman.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

/** In-memory fake. [failReads] makes observe emit a storage failure and [failSave] makes `save` fail. */
class FakeSettingsRepository(
    initial: AppSettings = AppSettings(),
    private val failReads: Boolean = false,
    private val failSave: Boolean = false,
) : SettingsRepository {
    private val settings = MutableStateFlow(initial)
    val current: AppSettings get() = settings.value

    override fun observe(): Flow<Outcome<AppSettings>> = settings.map {
        if (failReads) Outcome.Failure(DomainError.Storage("disk")) else Outcome.Success(it)
    }

    override suspend fun markBackedUp(at: Instant): Outcome<Unit> {
        if (failSave) return Outcome.Failure(DomainError.Storage("disk"))
        settings.value = settings.value.copy(lastBackupAt = at)
        return Outcome.Success(Unit)
    }

    override suspend fun save(settings: AppSettings): Outcome<AppSettings> {
        if (failSave) return Outcome.Failure(DomainError.Storage("disk"))
        this.settings.value = settings
        return Outcome.Success(settings)
    }
}
