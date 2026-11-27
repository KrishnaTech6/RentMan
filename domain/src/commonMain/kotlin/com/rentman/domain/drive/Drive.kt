package com.rentman.domain.drive

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.backup.BackupRepository
import com.rentman.domain.billing.BillRepository
import com.rentman.domain.flatMap
import com.rentman.domain.map
import com.rentman.domain.tenancy.RenterRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Whether the landlord's Google Drive is connected, and when the backup there was last saved (null: none yet, or not known). */
data class DriveStatus(val connected: Boolean, val lastBackupAt: Instant?)

/** The app's own key with Google on one kind of phone (made once in Google Cloud). Not a secret. */
data class GoogleClient(val clientId: String) {
    /** Google sends the sign-in back to the app on this URL scheme: the client ID turned around. */
    val redirectScheme: String get() = clientId.split('.').reversed().joinToString(".")
    val redirectUri: String get() = "$redirectScheme:/oauth2redirect"

    companion object {
        /** All the app asks for: the files it makes itself, nothing else in the landlord's Drive. */
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    }
}

/** A finished sign-in with Google, to be turned into lasting access to the landlord's Drive. */
sealed interface DriveSignIn {
    /** iPhone: the code Google handed back to the browser sheet, and the secret only that sign-in knows. */
    data class Code(val code: String, val codeVerifier: String) : DriveSignIn

    /** Android: Google Play services already granted the app its own Drive files. */
    data object Granted : DriveSignIn
}

/** The meter photos on this phone, by file name: read for a backup, written by a restore. Not in the database or the JSON. */
interface MeterPhotoStore {
    fun exists(name: String): Boolean

    /** The photo's bytes, or null if it is no longer on the phone. */
    suspend fun read(name: String): ByteArray?

    suspend fun write(name: String, bytes: ByteArray): Boolean
}

/**
 * The landlord's own Google Drive (ADR 0010). The app sees only the files it made: a "Rent Manager" folder with one backup file,
 * replaced on every backup, and the meter photos the bills point to.
 */
interface DriveBackupRepository {
    /** What Drive says now. Offline, the connection is still shown and the time is unknown. */
    suspend fun status(): Outcome<DriveStatus>

    /** Whether Drive is connected on this phone. Asks nobody, so it works offline. */
    fun isConnected(): Boolean

    /** The fingerprint of the data this phone last wrote to or restored from Drive, or null if it never did. Kept on the phone. */
    fun syncedFingerprint(): String?

    /** Whether Drive already has a backup file. `DriveUnavailable` when that can't be known (no internet). */
    suspend fun hasBackup(): Outcome<Boolean>

    /** Notes that this phone and the Drive backup now hold the data with [fingerprint]: after a complete backup, or a restore. */
    fun noteInSync(fingerprint: String)

    suspend fun connect(signIn: DriveSignIn): Outcome<Unit>

    /** Saves [json] as the backup file in Drive, replacing the last one. `DriveNotConnected` before Connect. */
    suspend fun upload(json: String): Outcome<Unit>

    /** Saves each of [names] that Drive doesn't have yet, read from [photos]. A photo no longer on the phone is skipped. */
    suspend fun uploadPhotos(names: Set<String>, photos: MeterPhotoStore): Outcome<Unit>

    /** The backup file's text, or null when Drive has none yet. */
    suspend fun download(): Outcome<String?>

    /** Puts each of [names] that the phone doesn't have and Drive does back on the phone. */
    suspend fun downloadPhotos(names: Set<String>, photos: MeterPhotoStore): Outcome<Unit>

    /** Forgets the access to Drive on this phone. The files stay in Drive. */
    suspend fun disconnect(): Outcome<Unit>
}

/** What the Drive screen shows: connected or not, the day the backup in Drive was last saved, and whether this phone has renters yet. */
data class DriveBackupStatus(val connected: Boolean, val lastBackupOn: LocalDate?, val phoneHasRenters: Boolean)

class GetDriveStatusUseCase(private val drive: DriveBackupRepository, private val renters: RenterRepository, private val timeZone: TimeZone) {
    suspend operator fun invoke(): Outcome<DriveBackupStatus> =
        renters.observeAll().first().flatMap { all ->
            drive.status().map { DriveBackupStatus(it.connected, it.lastBackupAt?.toLocalDateTime(timeZone)?.date, all.isNotEmpty()) }
        }
}

class ConnectDriveUseCase(private val drive: DriveBackupRepository) {
    suspend operator fun invoke(signIn: DriveSignIn): Outcome<Unit> = drive.connect(signIn)
}

/**
 * Everything on the phone, as the same file "Back up now" shares, then the meter photos the bills point to, saved to Drive. A phone
 * with no renters is never backed up: on a new phone that would replace the only good copy with an empty one.
 */
class BackUpToDriveUseCase(
    private val backups: BackupRepository,
    private val drive: DriveBackupRepository,
    private val renters: RenterRepository,
    private val bills: BillRepository,
    private val photos: MeterPhotoStore,
) {
    suspend operator fun invoke(): Outcome<Unit> =
        renters.observeAll().first().flatMap { all ->
            if (all.isEmpty()) return@flatMap Outcome.Failure(DomainError.NothingToBackUp)
            // The fingerprint first: a change made while this runs then just means one more backup later, never a missed one.
            backups.fingerprint().flatMap { fingerprint ->
                backups.export()
                    .flatMap { drive.upload(it) }
                    .flatMap { bills.observeAll().first() }
                    .flatMap { saved -> drive.uploadPhotos(saved.mapNotNull { it.meterPhotoPath }.toSet(), photos) }
                    .map { drive.noteInSync(fingerprint) } // only once the photos are in too, so a failed photo is tried again
            }
        }
}

/** Replaces the phone's data with the Drive backup (a snapshot is kept for Undo), then brings back its meter photos. */
class RestoreFromDriveUseCase(
    private val drive: DriveBackupRepository,
    private val backups: BackupRepository,
    private val bills: BillRepository,
    private val photos: MeterPhotoStore,
) {
    suspend operator fun invoke(): Outcome<Unit> =
        drive.download()
            .flatMap { json -> json?.let { backups.restore(it) } ?: Outcome.Failure(DomainError.NotFound) }
            .flatMap { backups.fingerprint() }
            .map { drive.noteInSync(it) } // the phone now holds Drive's data, so the automatic backup may replace it again
            .flatMap { bills.observeAll().first() }
            .flatMap { restored -> drive.downloadPhotos(restored.mapNotNull { it.meterPhotoPath }.toSet(), photos) }
}

/**
 * The automatic backup: when anything changed since this phone last wrote to or restored from Drive (the fingerprints differ),
 * back up; otherwise nothing is sent. It runs whenever the app is opened and, on Android, once a day in the background, so it is
 * quiet: no change or an empty phone just mean "not now". It only ever replaces a Drive backup this phone wrote or restored: a phone
 * that never did (a new phone with a test renter, a second phone on the same Google account) backs up on its own only when Drive
 * has no backup at all, and waits when it can't tell.
 */
class AutoBackUpToDriveUseCase(
    private val drive: DriveBackupRepository,
    private val backups: BackupRepository,
    private val backUp: BackUpToDriveUseCase,
) {
    suspend operator fun invoke(): Outcome<Unit> {
        if (!drive.isConnected()) return Outcome.Success(Unit)
        val synced = drive.syncedFingerprint()
        if (synced == null) {
            val existing = drive.hasBackup()
            if (existing !is Outcome.Success) return existing.map { } // unsure: wait (the Android job tries again later)
            if (existing.value) return Outcome.Success(Unit)
        }
        return backups.fingerprint().flatMap { now ->
            if (now == synced) return@flatMap Outcome.Success(Unit) // nothing changed
            backUp().let { outcome -> if ((outcome as? Outcome.Failure)?.error == DomainError.NothingToBackUp) Outcome.Success(Unit) else outcome }
        }
    }
}

class DisconnectDriveUseCase(private val drive: DriveBackupRepository) {
    suspend operator fun invoke(): Outcome<Unit> = drive.disconnect()
}
