package com.rentman.sharedui.feature.settings

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.backup.BackupRepository
import com.rentman.domain.settings.AppSettings
import com.rentman.sharedui.feature.bill.FakeSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

/**
 * Test fake. A restore keeps a snapshot, and with [restoredSettings] it also changes the saved settings, like a real
 * restore of a file with other settings does. [failRestore] makes restore and undo fail.
 */
internal class FakeBackupRepository(
    private val settings: FakeSettingsRepository,
    var restoredSettings: AppSettings? = null,
    var failRestore: DomainError? = null,
    var failExport: Boolean = false,
    var fingerprint: String = "fp-1",
) : BackupRepository {
    private val snapshot = MutableStateFlow<Instant?>(null)
    private var before: AppSettings? = null
    val restored = mutableListOf<String>()

    override suspend fun export(): Outcome<String> =
        if (failExport) Outcome.Failure(DomainError.Storage("disk")) else Outcome.Success("""{"schemaVersion":1}""")

    override suspend fun fingerprint(): Outcome<String> =
        if (failExport) Outcome.Failure(DomainError.Storage("disk")) else Outcome.Success(fingerprint)

    override suspend fun restore(json: String): Outcome<Unit> {
        failRestore?.let { return Outcome.Failure(it) }
        restored += json
        before = settings.current
        snapshot.value = Instant.parse("2026-12-01T10:00:00Z")
        restoredSettings?.let { settings.save(it) }
        return Outcome.Success(Unit)
    }

    override suspend fun restoreSnapshot(): Outcome<Unit> {
        failRestore?.let { return Outcome.Failure(it) }
        val back = before ?: return Outcome.Failure(DomainError.NotFound)
        settings.save(back)
        before = null
        snapshot.value = null
        return Outcome.Success(Unit)
    }

    override fun observeSnapshot(): Flow<Outcome<Instant?>> = snapshot.map { Outcome.Success(it) }
}
