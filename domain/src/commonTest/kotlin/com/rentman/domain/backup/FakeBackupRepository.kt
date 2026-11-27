package com.rentman.domain.backup

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

/** In-memory fake. It records what it was asked and fails on request. */
class FakeBackupRepository(
    var exportJson: String = """{"schemaVersion":1}""",
    snapshotTakenAt: Instant? = null,
    var failExport: Boolean = false,
    var failRestore: DomainError? = null,
    /** The fingerprint of what is on the pretend phone. Change it to pretend the data changed. */
    var fingerprint: String = "fp-1",
) : BackupRepository {
    private val snapshot = MutableStateFlow(snapshotTakenAt)
    val restored = mutableListOf<String>()
    var snapshotRestores = 0
        private set

    override suspend fun export(): Outcome<String> =
        if (failExport) Outcome.Failure(DomainError.Storage("disk")) else Outcome.Success(exportJson)

    override suspend fun fingerprint(): Outcome<String> =
        if (failExport) Outcome.Failure(DomainError.Storage("disk")) else Outcome.Success(fingerprint)

    override suspend fun restore(json: String): Outcome<Unit> {
        failRestore?.let { return Outcome.Failure(it) }
        restored += json
        snapshot.value = Instant.parse("2026-12-01T10:00:00Z") // a restore takes a snapshot first
        return Outcome.Success(Unit)
    }

    override suspend fun restoreSnapshot(): Outcome<Unit> {
        if (snapshot.value == null) return Outcome.Failure(DomainError.NotFound)
        snapshotRestores++
        snapshot.value = null // undoing uses the snapshot up
        return Outcome.Success(Unit)
    }

    override fun observeSnapshot(): Flow<Outcome<Instant?>> = snapshot.map { Outcome.Success(it) }
}
