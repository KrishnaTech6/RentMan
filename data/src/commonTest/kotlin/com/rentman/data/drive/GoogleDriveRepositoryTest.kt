package com.rentman.data.drive

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.drive.DriveSignIn
import com.rentman.domain.drive.DriveStatus
import com.rentman.domain.drive.GoogleClient
import com.rentman.domain.drive.MeterPhotoStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class GoogleDriveRepositoryTest {
    private val client = GoogleClient("123-abc.apps.googleusercontent.com")

    /** One file or folder in the pretend Drive. Contents are text here, so the requests can be read back. */
    private class Item(val id: String, val name: String, val parent: String?, val folder: Boolean, var content: String = "", val modifiedTime: String = "2026-11-27T09:00:00Z")

    /** A pretend Google: a token endpoint and a tiny Drive. Every request is noted in [calls]. */
    private class FakeGoogle(var refreshFails: Boolean = false, var offline: Boolean = false) {
        val items = mutableListOf<Item>()
        val calls = mutableListOf<String>()

        fun folder(name: String, parent: String? = null) = Item("id-${items.size}", name, parent, folder = true).also { items += it }.id
        fun file(name: String, parent: String, content: String) = Item("id-${items.size}", name, parent, folder = false, content).also { items += it }.id
        fun named(name: String) = items.single { it.name == name }

        suspend fun MockRequestHandleScope.answer(request: HttpRequestData): HttpResponseData {
            val body = request.body.toByteArray().decodeToString()
            val url = request.url
            calls += "${request.method.value} ${url.encodedPath}"
            if (offline) throw IllegalStateException("no internet")
            val json = headersOf(HttpHeaders.ContentType, "application/json")
            val path = url.encodedPath
            return when {
                path == "/token" && "grant_type=authorization_code" in body ->
                    respond("""{"access_token":"first","expires_in":3599,"refresh_token":"keep-me"}""", headers = json)
                path == "/token" && refreshFails -> respondError(HttpStatusCode.BadRequest, """{"error":"invalid_grant"}""")
                path == "/token" -> respond("""{"access_token":"fresh","expires_in":3599}""", headers = json)
                path == "/drive/v3/files" && request.method == HttpMethod.Get -> {
                    val q = url.parameters["q"].orEmpty()
                    val name = Regex("name = '([^']+)'").find(q)?.groupValues?.get(1)
                    val parent = Regex("'([^']+)' in parents").find(q)?.groupValues?.get(1)
                    val found = items.filter {
                        (name == null || it.name == name) && (parent == null || it.parent == parent) && (!("mimeType" in q) || it.folder)
                    }
                    respond("""{"files":[${found.joinToString { """{"id":"${it.id}","name":"${it.name}","modifiedTime":"${it.modifiedTime}"}""" }}]}""", headers = json)
                }
                path == "/drive/v3/files" && request.method == HttpMethod.Post -> {
                    val name = Regex(""""name":"([^"]+)"""").find(body)!!.groupValues[1]
                    val parent = Regex(""""parents":\["([^"]+)"""").find(body)?.groupValues?.get(1)
                    respond("""{"id":"${folder(name, parent)}"}""", headers = json)
                }
                path == "/upload/drive/v3/files" -> {
                    val name = Regex(""""name":"([^"]+)"""").find(body)!!.groupValues[1]
                    val parent = Regex(""""parents":\["([^"]+)"""").find(body)!!.groupValues[1]
                    val content = body.split("\r\n\r\n")[2].substringBefore("\r\n--")
                    respond("""{"id":"${file(name, parent, content)}"}""", headers = json)
                }
                path.startsWith("/upload/drive/v3/files/") -> {
                    items.single { it.id == path.substringAfterLast('/') }.content = body
                    respond("""{"id":"x"}""", headers = json)
                }
                path.startsWith("/drive/v3/files/") -> respond(items.single { it.id == path.substringAfterLast('/') }.content, headers = json)
                else -> error("unexpected ${request.method.value} $url")
            }
        }
    }

    private class MemoryTokens(var token: String? = null) : TokenStore {
        override fun read(): String? = token
        override fun write(refreshToken: String) { token = refreshToken }
        override fun clear() { token = null }
    }

    private class MemoryLog(var synced: String? = null) : UploadLog {
        override fun fingerprint(): String? = synced
        override fun record(fingerprint: String) { synced = fingerprint }
        override fun clear() { synced = null }
    }

    private class Photos(val files: MutableMap<String, String> = mutableMapOf()) : MeterPhotoStore {
        override fun exists(name: String) = name in files
        override suspend fun read(name: String) = files[name]?.encodeToByteArray()
        override suspend fun write(name: String, bytes: ByteArray): Boolean {
            files[name] = bytes.decodeToString()
            return true
        }
    }

    private fun repository(google: FakeGoogle, tokens: TokenStore = MemoryTokens("refresh"), log: UploadLog = MemoryLog()): GoogleDriveRepository {
        val http = HttpClient(MockEngine { with(google) { answer(it) } }) { expectSuccess = true }
        return GoogleDriveRepository(http, RefreshTokenAuth(http, tokens, client), log)
    }

    @Test
    fun connectingSwapsTheSignInForAKeptAccessToDrive() = runTest {
        val tokens = MemoryTokens()

        assertEquals(Outcome.Success(Unit), repository(FakeGoogle(), tokens).connect(DriveSignIn.Code("4/code", "verifier")))
        assertEquals("keep-me", tokens.token)
    }

    @Test
    fun theFirstBackupMakesTheFolderAndTheFile() = runTest {
        val google = FakeGoogle()
        val log = MemoryLog()

        assertEquals(Outcome.Success(Unit), repository(google, log = log).upload("""{"all":"data"}"""))

        assertEquals("""{"all":"data"}""", google.named("rentman-backup.json").content)
        assertEquals(google.named("Rent Manager").id, google.named("rentman-backup.json").parent)
        assertEquals(null, log.synced) // the photos come next; only then is the backup complete
    }

    @Test
    fun inSyncKeepsTheFingerprintOnThisPhone() {
        val log = MemoryLog()
        val drive = repository(FakeGoogle(), log = log)

        drive.noteInSync("fp-1")

        assertEquals("fp-1", drive.syncedFingerprint())
    }

    @Test
    fun hasBackupSaysWhetherDriveHasOneAndCantTellOffline() = runTest {
        val google = FakeGoogle()
        assertEquals(Outcome.Success(false), repository(google).hasBackup())

        google.file("rentman-backup.json", google.folder("Rent Manager"), "{}")
        assertEquals(Outcome.Success(true), repository(google).hasBackup())

        assertEquals(Outcome.Failure(DomainError.DriveUnavailable), repository(FakeGoogle(offline = true)).hasBackup())
    }

    @Test
    fun laterBackupsReplaceTheSameFileInsteadOfAddingOne() = runTest {
        val google = FakeGoogle()
        val main = google.folder("Rent Manager")
        google.file("rentman-backup.json", main, "older")

        repository(google).upload("""{"newer":"data"}""")

        assertEquals("""{"newer":"data"}""", google.named("rentman-backup.json").content) // named() would fail on a second one
        assertTrue(google.calls.none { it == "POST /upload/drive/v3/files" })
    }

    @Test
    fun twoBackupsAtOnceMakeOneFolderAndOneFile() = runTest {
        val google = FakeGoogle()
        val drive = repository(google)

        coroutineScope {
            launch { drive.upload("""{"from":"home"}""") }
            launch { drive.upload("""{"from":"daily job"}""") }
        }

        assertEquals(1, google.items.count { it.name == "Rent Manager" })
        assertEquals(1, google.items.count { it.name == "rentman-backup.json" })
    }

    @Test
    fun onlyPhotosDriveDoesNotHaveYetAreUploadedIntoTheirOwnFolder() = runTest {
        val google = FakeGoogle()
        val main = google.folder("Rent Manager")
        google.file("oct.jpg", google.folder("Meter photos", main), "OCT")

        val outcome = repository(google).uploadPhotos(setOf("oct.jpg", "nov.jpg", "gone.jpg"), Photos(mutableMapOf("oct.jpg" to "OCT", "nov.jpg" to "NOV")))

        assertEquals(Outcome.Success(Unit), outcome)
        assertEquals("NOV", google.named("nov.jpg").content)
        assertEquals(google.named("Meter photos").id, google.named("nov.jpg").parent)
        assertEquals(1, google.calls.count { it == "POST /upload/drive/v3/files" }) // oct.jpg was there; gone.jpg isn't on the phone
    }

    @Test
    fun restoreReadsTheBackupAndBringsBackOnlyMissingPhotos() = runTest {
        val google = FakeGoogle()
        val main = google.folder("Rent Manager")
        google.file("rentman-backup.json", main, """{"from":"drive"}""")
        val photoFolder = google.folder("Meter photos", main)
        google.file("oct.jpg", photoFolder, "OCT-DRIVE")
        google.file("nov.jpg", photoFolder, "NOV")
        val photos = Photos(mutableMapOf("oct.jpg" to "OCT-PHONE"))
        val drive = repository(google)

        assertEquals(Outcome.Success("""{"from":"drive"}"""), drive.download())
        assertEquals(Outcome.Success(Unit), drive.downloadPhotos(setOf("oct.jpg", "nov.jpg"), photos))

        assertEquals(mapOf("oct.jpg" to "OCT-PHONE", "nov.jpg" to "NOV"), photos.files)
        assertEquals(Outcome.Success(null), repository(FakeGoogle()).download())
    }

    @Test
    fun theStatusSaysWhenTheBackupInDriveWasLastSavedAndTheLocalChecksNeedNoInternet() = runTest {
        val google = FakeGoogle()
        google.file("rentman-backup.json", google.folder("Rent Manager"), "{}")

        assertEquals(Outcome.Success(DriveStatus(true, Instant.parse("2026-11-27T09:00:00Z"))), repository(google).status())
        assertEquals(Outcome.Success(DriveStatus(false, null)), repository(google, MemoryTokens(null)).status())

        val offline = FakeGoogle(offline = true)
        val drive = repository(offline, log = MemoryLog("fp-1"))
        assertEquals(true to "fp-1", drive.isConnected() to drive.syncedFingerprint())
        assertEquals(emptyList(), offline.calls)
    }

    @Test
    fun accessTakenBackInGoogleMeansConnectAgain() = runTest {
        val tokens = MemoryTokens("revoked")

        assertEquals(Outcome.Failure(DomainError.DriveNotConnected), repository(FakeGoogle(refreshFails = true), tokens).upload("{}"))
        assertEquals(null, tokens.token) // forgotten, so the Drive screen shows Connect again
    }

    @Test
    fun noInternetChangesNothing() = runTest {
        val tokens = MemoryTokens("refresh")
        val log = MemoryLog("fp-1")

        assertEquals(Outcome.Failure(DomainError.DriveUnavailable), repository(FakeGoogle(offline = true), tokens, log).upload("{}"))
        assertEquals("refresh", tokens.token)
        assertEquals("fp-1", log.synced)
    }

    @Test
    fun beforeConnectNothingIsSentToGoogle() = runTest {
        val google = FakeGoogle()

        assertEquals(Outcome.Failure(DomainError.DriveNotConnected), repository(google, MemoryTokens(null)).upload("{}"))
        assertEquals(emptyList(), google.calls)
    }

    @Test
    fun disconnectForgetsTheAccessAndWhatWasBackedUp() = runTest {
        val tokens = MemoryTokens("refresh")
        val log = MemoryLog("fp-1")

        repository(FakeGoogle(), tokens, log).disconnect()

        assertEquals(null, tokens.token)
        assertEquals(null, log.synced)
    }
}
