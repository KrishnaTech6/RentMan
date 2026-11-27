package com.rentman.sharedui.feature.drive

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.drive.DriveBackupRepository
import com.rentman.domain.drive.DriveSignIn
import com.rentman.domain.drive.DriveStatus
import kotlin.time.Instant

/** In-memory fake of the landlord's Drive. [fail] makes every call but status and disconnect fail with it. */
internal class FakeDriveBackupRepository(
    var connected: Boolean = false,
    var file: String? = null,
    var lastBackupAt: Instant? = null,
    var fail: DomainError? = null,
) : DriveBackupRepository {
    override suspend fun status(): Outcome<DriveStatus> = Outcome.Success(DriveStatus(connected, lastBackupAt))

    override suspend fun connect(signIn: DriveSignIn): Outcome<Unit> {
        fail?.let { return Outcome.Failure(it) }
        connected = true
        return Outcome.Success(Unit)
    }

    override suspend fun upload(json: String): Outcome<Unit> {
        fail?.let { return Outcome.Failure(it) }
        file = json
        lastBackupAt = Instant.parse("2026-11-28T09:00:00Z")
        return Outcome.Success(Unit)
    }

    override suspend fun download(): Outcome<String?> = fail?.let { Outcome.Failure(it) } ?: Outcome.Success(file)

    override suspend fun disconnect(): Outcome<Unit> {
        connected = false
        return Outcome.Success(Unit)
    }
}
