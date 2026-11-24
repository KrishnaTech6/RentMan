package com.rentman.domain.backup

import com.rentman.domain.Outcome
import com.rentman.domain.flatMap
import com.rentman.domain.map
import com.rentman.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** A backup file ready to be shared: the text of the JSON and a name for it. */
data class BackupExport(val fileName: String, val json: String)

/** The day the last backup file was made, and, after a restore, the day "Undo last restore" goes back to. */
data class BackupStatus(val lastBackupOn: LocalDate?, val undoSinceOn: LocalDate?)

interface BackupRepository {
    /** Everything on the phone as one JSON text. Meter photos are not included yet. */
    suspend fun export(): Outcome<String>

    /**
     * Replaces everything with the backup in [json], in one transaction: if anything is wrong nothing changes.
     * The current data is kept as a snapshot first (see [restoreSnapshot]). Fails with `InvalidBackup` or
     * `BackupFromNewerVersion` for a file that can't be used.
     */
    suspend fun restore(json: String): Outcome<Unit>

    /** Puts back the data from just before the last restore, and uses the snapshot up. `NotFound` if there is none. */
    suspend fun restoreSnapshot(): Outcome<Unit>

    /** When the snapshot was taken, or null when there is none. */
    fun observeSnapshot(): Flow<Outcome<Instant?>>
}

class ExportBackupUseCase(
    private val backups: BackupRepository,
    private val clock: Clock,
    private val timeZone: TimeZone,
) {
    suspend operator fun invoke(): Outcome<BackupExport> = backups.export().map { json ->
        BackupExport("rentman-backup-${clock.now().toLocalDateTime(timeZone).date}.json", json)
    }
}

/**
 * "Last backup" means the last time a backup file was handed to the share sheet. The screen calls this once that worked,
 * so a file that could not be shared never counts. The app can't know if the person then finished sharing it.
 */
class MarkBackedUpUseCase(
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(): Outcome<Unit> = settings.markBackedUp(clock.now())
}

class RestoreBackupUseCase(private val backups: BackupRepository) {
    suspend operator fun invoke(json: String): Outcome<Unit> = backups.restore(json)
}

class UndoRestoreUseCase(private val backups: BackupRepository) {
    suspend operator fun invoke(): Outcome<Unit> = backups.restoreSnapshot()
}

class ObserveBackupStatusUseCase(
    private val settings: SettingsRepository,
    private val backups: BackupRepository,
    private val timeZone: TimeZone,
) {
    operator fun invoke(): Flow<Outcome<BackupStatus>> =
        combine(settings.observe(), backups.observeSnapshot()) { s, snapshot ->
            s.flatMap { appSettings ->
                snapshot.map { taken ->
                    BackupStatus(
                        lastBackupOn = appSettings.lastBackupAt?.toLocalDateTime(timeZone)?.date,
                        undoSinceOn = taken?.toLocalDateTime(timeZone)?.date,
                    )
                }
            }
        }
}
