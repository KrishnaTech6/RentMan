package com.rentman.data.drive

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.drive.DriveBackupRepository
import com.rentman.domain.drive.DriveSignIn
import com.rentman.domain.drive.DriveStatus
import com.rentman.domain.drive.MeterPhotoStore
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/**
 * The landlord's own Google Drive over Google's web API (ADR 0010). It asks only for the files it makes: a "Rent Manager" folder
 * with one backup file, found by name and replaced on every backup, and a "Meter photos" folder in it. Each job starts from a fresh
 * access token from [auth], so nothing is cached here; [log] keeps what this phone last backed up, for the automatic check. Failures are
 * caught once, at the bottom ([driveCall]): access taken back means `DriveNotConnected`, anything else `DriveUnavailable`.
 */
internal class GoogleDriveRepository(
    private val http: HttpClient,
    private val auth: GoogleAuth,
    private val log: UploadLog,
) : DriveBackupRepository {
    /** One Drive job at a time: two first backups at once (Home and Android's daily job) would each make the folder. */
    private val oneAtATime = Mutex()

    override suspend fun status(): Outcome<DriveStatus> {
        if (!auth.isConnected()) return Outcome.Success(DriveStatus(connected = false, lastBackupAt = null))
        return when (val found = driveCall { backupFile(auth.accessToken())?.modifiedTime }) {
            is Outcome.Success -> Outcome.Success(DriveStatus(connected = true, lastBackupAt = found.value?.let(Instant::parse)))
            is Outcome.Failure ->
                // Offline, the connection is still there; only when the access was taken back does Settings offer Connect again.
                Outcome.Success(DriveStatus(connected = found.error != DomainError.DriveNotConnected, lastBackupAt = null))
        }
    }

    override fun isConnected(): Boolean = auth.isConnected()

    override fun syncedFingerprint(): String? = log.fingerprint()

    override suspend fun connect(signIn: DriveSignIn): Outcome<Unit> = driveCall { auth.connect(signIn) }

    override suspend fun upload(json: String): Outcome<Unit> = driveCall {
        val token = auth.accessToken()
        val folder = folder(token, FOLDER_NAME, parent = null) ?: makeFolder(token, FOLDER_NAME, parent = null)
        val existing = findFile(token, FILE_NAME, folder)
        if (existing != null) {
            http.patch("$UPLOAD_URL/${existing.id}") {
                bearerAuth(token)
                parameter("uploadType", "media")
                setBody(TextContent(json, ContentType.Application.Json))
            }
        } else {
            makeFile(token, FILE_NAME, folder, "application/json", json.encodeToByteArray())
        }
    }

    override suspend fun hasBackup(): Outcome<Boolean> = driveCall { backupFile(auth.accessToken()) != null }

    override fun noteInSync(fingerprint: String) = log.record(fingerprint)

    override suspend fun uploadPhotos(names: Set<String>, photos: MeterPhotoStore): Outcome<Unit> = driveCall {
        if (names.isEmpty()) return@driveCall
        val token = auth.accessToken()
        val main = folder(token, FOLDER_NAME, parent = null) ?: makeFolder(token, FOLDER_NAME, parent = null)
        val photoFolder = folder(token, PHOTO_FOLDER_NAME, parent = main) ?: makeFolder(token, PHOTO_FOLDER_NAME, parent = main)
        val inDrive = children(token, photoFolder).map { it.name }.toSet()
        // A photo is never changed after it is saved, so one already in Drive is the same photo.
        (names - inDrive).forEach { name -> photos.read(name)?.let { makeFile(token, name, photoFolder, "image/jpeg", it) } }
    }

    override suspend fun download(): Outcome<String?> = driveCall {
        val token = auth.accessToken()
        backupFile(token)?.let { content(token, it.id).decodeToString() }
    }

    override suspend fun downloadPhotos(names: Set<String>, photos: MeterPhotoStore): Outcome<Unit> = driveCall {
        val missing = names.filterNot(photos::exists).toSet()
        if (missing.isEmpty()) return@driveCall
        val token = auth.accessToken()
        val main = folder(token, FOLDER_NAME, parent = null) ?: return@driveCall
        val photoFolder = folder(token, PHOTO_FOLDER_NAME, parent = main) ?: return@driveCall
        children(token, photoFolder).filter { it.name in missing }.forEach { photos.write(it.name, content(token, it.id)) }
    }

    override suspend fun disconnect(): Outcome<Unit> = driveCall {
        auth.disconnect()
        log.clear()
    }

    private suspend fun backupFile(token: String): DriveFile? = folder(token, FOLDER_NAME, parent = null)?.let { findFile(token, FILE_NAME, it) }

    private suspend fun folder(token: String, name: String, parent: String?): String? =
        search(token, "name = '$name' and mimeType = '$FOLDER_TYPE' and trashed = false" + parent.inParents()).firstOrNull()?.id

    private suspend fun findFile(token: String, name: String, folder: String): DriveFile? =
        search(token, "name = '$name' and '$folder' in parents and trashed = false").firstOrNull()

    // shortcut: one page of up to 1000 photos; follow nextPageToken once a landlord has more than that.
    private suspend fun children(token: String, folder: String): List<DriveFile> = search(token, "'$folder' in parents and trashed = false")

    private suspend fun search(token: String, query: String): List<DriveFile> =
        http.get(FILES_URL) {
            bearerAuth(token)
            parameter("q", query)
            parameter("fields", "files(id,name,modifiedTime)")
            parameter("spaces", "drive")
            parameter("pageSize", "1000")
        }.decode<FileList>().files

    private suspend fun makeFolder(token: String, name: String, parent: String?): String =
        http.post(FILES_URL) {
            bearerAuth(token)
            parameter("fields", "id")
            val parents = parent?.let { ""","parents":["$it"]""" }.orEmpty()
            setBody(TextContent("""{"name":"$name","mimeType":"$FOLDER_TYPE"$parents}""", ContentType.Application.Json))
        }.decode<DriveFile>().id

    /** A new file with its details and its bytes in one request, as Drive's "multipart" upload wants. */
    private suspend fun makeFile(token: String, name: String, folder: String, type: String, bytes: ByteArray) {
        val metadata = """{"name":"$name","parents":["$folder"],"mimeType":"$type"}"""
        val head = "--$BOUNDARY\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$metadata\r\n--$BOUNDARY\r\nContent-Type: $type\r\n\r\n"
        http.post(UPLOAD_URL) {
            bearerAuth(token)
            parameter("uploadType", "multipart")
            setBody(ByteArrayContent(head.encodeToByteArray() + bytes + "\r\n--$BOUNDARY--".encodeToByteArray(), ContentType.parse("multipart/related; boundary=$BOUNDARY")))
        }
    }

    private suspend fun content(token: String, id: String): ByteArray =
        http.get("$FILES_URL/$id") {
            bearerAuth(token)
            parameter("alt", "media")
        }.bodyAsBytes()

    private fun String?.inParents(): String = this?.let { " and '$it' in parents" }.orEmpty()

    private suspend inline fun <reified T> HttpResponse.decode(): T = JSON.decodeFromString(bodyAsText())

    private suspend fun <T> driveCall(block: suspend () -> T): Outcome<T> =
        try {
            Outcome.Success(oneAtATime.withLock { block() })
        } catch (error: CancellationException) {
            throw error
        } catch (_: NotConnected) {
            Outcome.Failure(DomainError.DriveNotConnected)
        } catch (_: Exception) {
            Outcome.Failure(DomainError.DriveUnavailable)
        }

    @Serializable
    private class FileList(val files: List<DriveFile> = emptyList())

    @Serializable
    private class DriveFile(val id: String, val name: String = "", val modifiedTime: String? = null)

    private companion object {
        const val FILES_URL = "https://www.googleapis.com/drive/v3/files"
        const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
        const val FOLDER_NAME = "Rent Manager"
        const val PHOTO_FOLDER_NAME = "Meter photos"
        const val FOLDER_TYPE = "application/vnd.google-apps.folder"
        const val FILE_NAME = "rentman-backup.json"
        const val BOUNDARY = "rentman-backup-part"
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
