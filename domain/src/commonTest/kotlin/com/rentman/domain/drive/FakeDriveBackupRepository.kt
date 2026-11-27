package com.rentman.domain.drive

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import kotlin.time.Instant

/** In-memory fake of the landlord's Drive. [fail] makes every call to Google fail with it. */
class FakeDriveBackupRepository(
    var connected: Boolean = true,
    var file: String? = null,
    var fail: DomainError? = null,
    var lastBackupAt: Instant? = null,
    val photosInDrive: MutableMap<String, ByteArray> = mutableMapOf(),
    /** The fingerprint of the data this phone last wrote to or restored from Drive. */
    var synced: String? = null,
) : DriveBackupRepository {
    val signIns = mutableListOf<DriveSignIn>()
    var uploads = 0
        private set

    override suspend fun status(): Outcome<DriveStatus> = fail?.let { Outcome.Failure(it) } ?: Outcome.Success(DriveStatus(connected, lastBackupAt))

    override fun isConnected(): Boolean = connected

    override fun syncedFingerprint(): String? = synced

    override suspend fun hasBackup(): Outcome<Boolean> {
        fail?.let { return Outcome.Failure(it) }
        if (!connected) return Outcome.Failure(DomainError.DriveNotConnected)
        return Outcome.Success(file != null)
    }

    override fun noteInSync(fingerprint: String) {
        synced = fingerprint
    }

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
        uploads++
        lastBackupAt = Instant.parse("2026-11-28T09:00:00Z")
        return Outcome.Success(Unit)
    }

    override suspend fun uploadPhotos(names: Set<String>, photos: MeterPhotoStore): Outcome<Unit> {
        fail?.let { return Outcome.Failure(it) }
        names.filterNot { it in photosInDrive }.forEach { name -> photos.read(name)?.let { photosInDrive[name] = it } }
        return Outcome.Success(Unit)
    }

    override suspend fun download(): Outcome<String?> {
        fail?.let { return Outcome.Failure(it) }
        if (!connected) return Outcome.Failure(DomainError.DriveNotConnected)
        return Outcome.Success(file)
    }

    override suspend fun downloadPhotos(names: Set<String>, photos: MeterPhotoStore): Outcome<Unit> {
        fail?.let { return Outcome.Failure(it) }
        names.filterNot(photos::exists).forEach { name -> photosInDrive[name]?.let { photos.write(name, it) } }
        return Outcome.Success(Unit)
    }

    override suspend fun disconnect(): Outcome<Unit> {
        connected = false
        return Outcome.Success(Unit)
    }
}

/** The meter photos on a pretend phone. */
class FakeMeterPhotoStore(val files: MutableMap<String, ByteArray> = mutableMapOf()) : MeterPhotoStore {
    override fun exists(name: String): Boolean = name in files
    override suspend fun read(name: String): ByteArray? = files[name]
    override suspend fun write(name: String, bytes: ByteArray): Boolean {
        files[name] = bytes
        return true
    }
}
