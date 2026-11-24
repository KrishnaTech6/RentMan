package com.rentman.data.backup

import com.rentman.data.db.BackupDao
import com.rentman.data.safeCall
import com.rentman.data.safeFlow
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.backup.BackupRepository
import com.rentman.domain.flatMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Instant

internal class RoomBackupRepository(
    private val dao: BackupDao,
    private val clock: Clock,
) : BackupRepository {

    override suspend fun export(): Outcome<String> =
        safeCall { BackupCodec.encode(dao.readAll(exportedAt = clock.now().toEpochMilliseconds())) }

    override suspend fun restore(json: String): Outcome<Unit> =
        BackupCodec.decode(json).flatMap { file ->
            safeCall { dao.replaceAll(file, takenAt = clock.now().toEpochMilliseconds(), takeSnapshot = true) }
        }

    override suspend fun restoreSnapshot(): Outcome<Unit> =
        safeCall { dao.snapshot() }.flatMap { snapshot ->
            if (snapshot == null) {
                Outcome.Failure(DomainError.NotFound)
            } else {
                BackupCodec.decode(snapshot.json).flatMap { file ->
                    safeCall { dao.replaceAll(file, takenAt = snapshot.takenAt, takeSnapshot = false) }
                }
            }
        }

    override fun observeSnapshot(): Flow<Outcome<Instant?>> =
        safeFlow { dao.observeSnapshotTakenAt().map { it?.let(Instant::fromEpochMilliseconds) } }
}
