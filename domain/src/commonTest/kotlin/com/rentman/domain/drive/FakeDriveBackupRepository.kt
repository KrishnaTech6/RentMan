package com.rentman.domain.drive

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import kotlin.time.Instant

/** In-memory fake of the landlord's Drive. [fail] makes every call fail with it. */
class FakeDriveBackupRepository(
    var connected: Boolean = true,
    var file: String? = null,
    var fail: DomainError? = null,
) : DriveBackupRepository {
    var lastBackupAt: Instant? = null
    val signIns = mutableListOf<DriveSignIn>()

    override suspend fun status(): Outcome<DriveStatus> = fail?.let { Outcome.Failure(it) } ?: Outcome.Success(DriveStatus(connected, lastBackupAt))

    override suspend fun connect(signIn: DriveSignIn): Outcome<Unit> {
        fail?.let { return Outcome.Failure(it) }
        signIns += signIn
        connected = true
        return Outcome.Success(Unit)
    }

    override suspend fun upload(json: String): Outcome<Unit> {
        fail?.let { return Outcome.Failure(it) }
        if (!connected) return Outcome.Failure(DomainError.DriveNotConnected)
        file = json
        lastBackupAt = Instant.parse("2026-11-28T09:00:00Z")
        return Outcome.Success(Unit)
    }

    override suspend fun download(): Outcome<String?> {
        fail?.let { return Outcome.Failure(it) }
        if (!connected) return Outcome.Failure(DomainError.DriveNotConnected)
        return Outcome.Success(file)
    }

    override suspend fun disconnect(): Outcome<Unit> {
        connected = false
        return Outcome.Success(Unit)
    }
}
