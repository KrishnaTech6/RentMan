package com.rentman.sharedui.feature.drive

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.drive.DriveBackupRepository
import com.rentman.domain.drive.DriveSignIn
import com.rentman.domain.drive.DriveStatus
import com.rentman.domain.drive.MeterPhotoStore
import kotlin.time.Instant

/** In-memory fake of the landlord's Drive. [fail] makes every call but the statuses and disconnect fail with it. */
internal class FakeDriveBackupRepository(
    var connected: Boolean = false,
    var file: String? = null,
    var lastBackupAt: Instant? = null,
    var fail: DomainError? = null,
    var synced: String? = null,
) : DriveBackupRepository {
    override suspend fun status(): Outcome<DriveStatus> = Outcome.Success(DriveStatus(connected, lastBackupAt))

    override fun isConnected(): Boolean = connected

    override fun syncedFingerprint(): String? = synced

    override suspend fun hasBackup(): Outcome<Boolean> = fail?.let { Outcome.Failure(it) } ?: Outcome.Success(file != null)

    override fun noteInSync(fingerprint: String) {
        synced = fingerprint
    }

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

    override suspend fun uploadPhotos(names: Set<String>, photos: MeterPhotoStore): Outcome<Unit> = fail?.let { Outcome.Failure(it) } ?: Outcome.Success(Unit)

    override suspend fun download(): Outcome<String?> = fail?.let { Outcome.Failure(it) } ?: Outcome.Success(file)

    override suspend fun downloadPhotos(names: Set<String>, photos: MeterPhotoStore): Outcome<Unit> = fail?.let { Outcome.Failure(it) } ?: Outcome.Success(Unit)

    override suspend fun disconnect(): Outcome<Unit> {
        connected = false
        return Outcome.Success(Unit)
    }
}

/** A phone with no meter photos. */
internal object NoPhotos : MeterPhotoStore {
    override fun exists(name: String) = false
    override suspend fun read(name: String): ByteArray? = null
    override suspend fun write(name: String, bytes: ByteArray) = true
}
