package com.rentman.data.drive

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.drive.DriveBackupRepository
import com.rentman.domain.drive.DriveSignIn
import com.rentman.domain.drive.DriveStatus
import com.rentman.domain.drive.GoogleClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/** Where the long-lived access to the landlord's Drive is kept on the phone (the iPhone Keychain). Never in the database or a backup. */
internal interface TokenStore {
    fun read(): String?
    fun write(refreshToken: String)
    fun clear()
}

/**
 * The landlord's own Google Drive over Google's web API (ADR 0010). It asks only for the files it makes: one "Rent Manager"
 * folder with one backup file, found by name and replaced on every backup. Each call starts from a fresh access token, so
 * nothing is cached here. Failures are caught once, at the bottom ([driveCall]): access taken back means `DriveNotConnected`,
 * anything else (no internet, Google trouble) `DriveUnavailable`.
 */
internal class GoogleDriveRepository(
    private val http: HttpClient,
    private val tokens: TokenStore,
    private val client: GoogleClient,
) : DriveBackupRepository {

    override suspend fun status(): Outcome<DriveStatus> {
        if (tokens.read() == null) return Outcome.Success(DriveStatus(connected = false, lastBackupAt = null))
        return when (val found = driveCall { backupFile(accessToken())?.modifiedTime }) {
            is Outcome.Success -> Outcome.Success(DriveStatus(connected = true, lastBackupAt = found.value?.let(Instant::parse)))
            is Outcome.Failure ->
                // Offline, the connection is still there; only when the access was taken back does Settings offer Connect again.
                Outcome.Success(DriveStatus(connected = found.error != DomainError.DriveNotConnected, lastBackupAt = null))
        }
    }

    override suspend fun connect(signIn: DriveSignIn): Outcome<Unit> = driveCall {
        val reply = http.submitForm(
            TOKEN_URL,
            parameters {
                append("client_id", client.clientId)
                append("code", signIn.code)
                append("code_verifier", signIn.codeVerifier)
                append("grant_type", "authorization_code")
                append("redirect_uri", client.redirectUri)
            },
        ).decode<TokenReply>()
        // Google may leave the long-lived token out when this phone already had one: keep that one.
        tokens.write(reply.refreshToken ?: tokens.read() ?: throw NotConnected())
    }

    override suspend fun upload(json: String): Outcome<Unit> = driveCall<Unit> {
        val token = accessToken()
        val folder = findFolder(token) ?: makeFolder(token)
        val existing = findFile(token, folder)
        if (existing != null) {
            http.patch("$UPLOAD_URL/${existing.id}") {
                bearerAuth(token)
                parameter("uploadType", "media")
                setBody(TextContent(json, ContentType.Application.Json))
            }
        } else {
            val metadata = """{"name":"$FILE_NAME","parents":["$folder"],"mimeType":"application/json"}"""
            val body = "--$BOUNDARY\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$metadata\r\n" +
                "--$BOUNDARY\r\nContent-Type: application/json\r\n\r\n$json\r\n--$BOUNDARY--"
            http.post(UPLOAD_URL) {
                bearerAuth(token)
                parameter("uploadType", "multipart")
                setBody(TextContent(body, ContentType.parse("multipart/related; boundary=$BOUNDARY")))
            }
        }
    }

    override suspend fun download(): Outcome<String?> = driveCall {
        val token = accessToken()
        backupFile(token)?.let { file ->
            http.get("$FILES_URL/${file.id}") {
                bearerAuth(token)
                parameter("alt", "media")
            }.bodyAsText()
        }
    }

    override suspend fun disconnect(): Outcome<Unit> = driveCall { tokens.clear() }

    /** A new short-lived access token from the kept one. Google refusing it means the landlord took the access back. */
    private suspend fun accessToken(): String {
        val refreshToken = tokens.read() ?: throw NotConnected()
        val reply = try {
            http.submitForm(
                TOKEN_URL,
                parameters {
                    append("client_id", client.clientId)
                    append("refresh_token", refreshToken)
                    append("grant_type", "refresh_token")
                },
            )
        } catch (refused: ClientRequestException) {
            if (refused.response.status == HttpStatusCode.BadRequest || refused.response.status == HttpStatusCode.Unauthorized) {
                tokens.clear()
                throw NotConnected()
            }
            throw refused
        }
        return reply.decode<TokenReply>().accessToken
    }

    private suspend fun backupFile(token: String): DriveFile? = findFolder(token)?.let { findFile(token, it) }

    private suspend fun findFolder(token: String): String? =
        search(token, "name = '$FOLDER_NAME' and mimeType = '$FOLDER_TYPE' and trashed = false").firstOrNull()?.id

    private suspend fun findFile(token: String, folder: String): DriveFile? =
        search(token, "name = '$FILE_NAME' and '$folder' in parents and trashed = false").firstOrNull()

    private suspend fun search(token: String, query: String): List<DriveFile> =
        http.get(FILES_URL) {
            bearerAuth(token)
            parameter("q", query)
            parameter("fields", "files(id,modifiedTime)")
            parameter("spaces", "drive")
        }.decode<FileList>().files

    private suspend fun makeFolder(token: String): String =
        http.post(FILES_URL) {
            bearerAuth(token)
            parameter("fields", "id")
            setBody(TextContent("""{"name":"$FOLDER_NAME","mimeType":"$FOLDER_TYPE"}""", ContentType.Application.Json))
        }.decode<DriveFile>().id

    private suspend inline fun <reified T> io.ktor.client.statement.HttpResponse.decode(): T = JSON.decodeFromString(bodyAsText())

    private class NotConnected : Exception()

    private suspend fun <T> driveCall(block: suspend () -> T): Outcome<T> =
        try {
            Outcome.Success(block())
        } catch (error: CancellationException) {
            throw error
        } catch (_: NotConnected) {
            Outcome.Failure(DomainError.DriveNotConnected)
        } catch (_: Exception) {
            Outcome.Failure(DomainError.DriveUnavailable)
        }

    @Serializable
    private class TokenReply(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String? = null,
    )

    @Serializable
    private class FileList(val files: List<DriveFile> = emptyList())

    @Serializable
    private class DriveFile(val id: String, val modifiedTime: String? = null)

    private companion object {
        const val TOKEN_URL = "https://oauth2.googleapis.com/token"
        const val FILES_URL = "https://www.googleapis.com/drive/v3/files"
        const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
        const val FOLDER_NAME = "Rent Manager"
        const val FOLDER_TYPE = "application/vnd.google-apps.folder"
        const val FILE_NAME = "rentman-backup.json"
        const val BOUNDARY = "rentman-backup-part"
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
