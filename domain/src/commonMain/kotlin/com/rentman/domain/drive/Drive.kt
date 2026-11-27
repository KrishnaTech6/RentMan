package com.rentman.domain.drive

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.backup.BackupRepository
import com.rentman.domain.flatMap
import com.rentman.domain.map
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
}

/** A sign-in the landlord finished with Google in the browser: what Google handed back, swapped for access to their Drive. */
data class DriveSignIn(val code: String, val codeVerifier: String)

/**
 * The landlord's own Google Drive (ADR 0010). The app sees only the files it made: a "Rent Manager" folder with one backup file,
 * replaced on every backup.
 */
interface DriveBackupRepository {
    suspend fun status(): Outcome<DriveStatus>

    suspend fun connect(signIn: DriveSignIn): Outcome<Unit>

    /** Saves [json] as the backup file in Drive, replacing the last one. `DriveNotConnected` before Connect. */
    suspend fun upload(json: String): Outcome<Unit>

    /** The backup file's text, or null when Drive has none yet. */
    suspend fun download(): Outcome<String?>

    /** Forgets the access to Drive. The files stay in Drive. */
    suspend fun disconnect(): Outcome<Unit>
}

/** What Settings shows: whether Drive is connected, and the day (in the phone's time zone) the backup there was last saved. */
data class DriveBackupStatus(val connected: Boolean, val lastBackupOn: LocalDate?)

class GetDriveStatusUseCase(private val drive: DriveBackupRepository, private val timeZone: TimeZone) {
    suspend operator fun invoke(): Outcome<DriveBackupStatus> =
        drive.status().map { DriveBackupStatus(it.connected, it.lastBackupAt?.toLocalDateTime(timeZone)?.date) }
}

class ConnectDriveUseCase(private val drive: DriveBackupRepository) {
    suspend operator fun invoke(signIn: DriveSignIn): Outcome<Unit> = drive.connect(signIn)
}

/** Everything on the phone, as the same file "Back up now" shares, saved to Drive. If the phone's data can't be read, Drive keeps its copy. */
class BackUpToDriveUseCase(private val backups: BackupRepository, private val drive: DriveBackupRepository) {
    suspend operator fun invoke(): Outcome<Unit> = backups.export().flatMap { drive.upload(it) }
}

/** Replaces the phone's data with the Drive backup, keeping a snapshot for Undo like any restore. `NotFound` when Drive has none. */
class RestoreFromDriveUseCase(private val drive: DriveBackupRepository, private val backups: BackupRepository) {
    suspend operator fun invoke(): Outcome<Unit> =
        drive.download().flatMap { json -> json?.let { backups.restore(it) } ?: Outcome.Failure(DomainError.NotFound) }
}

class DisconnectDriveUseCase(private val drive: DriveBackupRepository) {
    suspend operator fun invoke(): Outcome<Unit> = drive.disconnect()
}
