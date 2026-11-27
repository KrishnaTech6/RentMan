package com.rentman.data.drive

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.drive.DriveSignIn
import com.rentman.domain.drive.DriveStatus
import com.rentman.domain.drive.GoogleClient
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
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class GoogleDriveRepositoryTest {
    private val client = GoogleClient("123-abc.apps.googleusercontent.com")

    /** A pretend Google: answers by request, and remembers each request (method, address, body) in [calls]. */
    private class FakeGoogle(
        var refreshFails: Boolean = false,
        var folderId: String? = null,
        var file: Pair<String, String>? = null, // id to modifiedTime
        var content: String = "",
        var offline: Boolean = false,
    ) {
        val calls = mutableListOf<String>()

        suspend fun MockRequestHandleScope.answer(request: HttpRequestData): HttpResponseData {
            val body = request.body.toByteArray().decodeToString()
            val url = request.url
            calls += "${request.method.value} ${url.encodedPath} ${url.parameters["q"] ?: ""}".trim()
            if (offline) throw IllegalStateException("no internet")
            val json = headersOf(HttpHeaders.ContentType, "application/json")
            return when {
                url.encodedPath == "/token" && "grant_type=authorization_code" in body ->
                    respond("""{"access_token":"first","expires_in":3599,"refresh_token":"keep-me"}""", headers = json)
                url.encodedPath == "/token" && refreshFails -> respondError(HttpStatusCode.BadRequest, """{"error":"invalid_grant"}""")
                url.encodedPath == "/token" -> respond("""{"access_token":"fresh","expires_in":3599}""", headers = json)
                url.encodedPath == "/drive/v3/files" && request.method == HttpMethod.Get && "mimeType" in (url.parameters["q"] ?: "") ->
                    respond(folderId?.let { """{"files":[{"id":"$it"}]}""" } ?: """{"files":[]}""", headers = json)
                url.encodedPath == "/drive/v3/files" && request.method == HttpMethod.Get ->
                    respond(file?.let { (id, at) -> """{"files":[{"id":"$id","modifiedTime":"$at"}]}""" } ?: """{"files":[]}""", headers = json)
                url.encodedPath == "/drive/v3/files" && request.method == HttpMethod.Post -> {
                    folderId = "folder-1"
                    respond("""{"id":"folder-1"}""", headers = json)
                }
                url.encodedPath == "/upload/drive/v3/files" -> {
                    content = body.substringAfter("Content-Type: application/json\r\n\r\n").substringBefore("\r\n--")
                    file = "file-1" to "2026-11-28T09:00:00Z"
                    respond("""{"id":"file-1"}""", headers = json)
                }
                url.encodedPath.startsWith("/upload/drive/v3/files/") -> {
                    content = body
                    respond("""{"id":"file-1"}""", headers = json)
                }
                url.encodedPath.startsWith("/drive/v3/files/") -> respond(content, headers = json)
                else -> error("unexpected ${request.method.value} $url")
            }
        }
    }

    private class MemoryTokens(var token: String? = null) : TokenStore {
        override fun read(): String? = token
        override fun write(refreshToken: String) { token = refreshToken }
        override fun clear() { token = null }
    }

    private fun repository(google: FakeGoogle, tokens: TokenStore = MemoryTokens("refresh")) =
        GoogleDriveRepository(HttpClient(MockEngine { with(google) { answer(it) } }) { expectSuccess = true }, tokens, client)

    @Test
    fun connectingSwapsTheSignInForAKeptAccessToDrive() = runTest {
        val google = FakeGoogle()
        val tokens = MemoryTokens()

        val outcome = repository(google, tokens).connect(DriveSignIn("4/code", "verifier"))

        assertEquals(Outcome.Success(Unit), outcome)
        assertEquals("keep-me", tokens.token)
    }

    @Test
    fun theFirstBackupMakesTheFolderAndTheFile() = runTest {
        val google = FakeGoogle()

        val outcome = repository(google).upload("""{"all":"data"}""")

        assertEquals(Outcome.Success(Unit), outcome)
        assertEquals("""{"all":"data"}""", google.content)
        assertTrue(google.calls.any { it.startsWith("POST /drive/v3/files") }, "the folder is made")
        assertTrue(google.calls.any { it.startsWith("POST /upload/drive/v3/files") }, "the file is made")
    }

    @Test
    fun laterBackupsReplaceTheSameFileInsteadOfAddingOne() = runTest {
        val google = FakeGoogle(folderId = "folder-1", file = "file-1" to "2026-11-27T09:00:00Z")

        repository(google).upload("""{"newer":"data"}""")

        assertEquals("""{"newer":"data"}""", google.content)
        assertTrue(google.calls.any { it.startsWith("PATCH /upload/drive/v3/files/file-1") })
        assertTrue(google.calls.none { it.startsWith("POST") && "files" in it && "token" !in it }, "nothing new is made: ${google.calls}")
    }

    @Test
    fun restoreReadsTheBackupFileAndFindsNothingWhenThereIsNone() = runTest {
        val withFile = FakeGoogle(folderId = "folder-1", file = "file-1" to "2026-11-27T09:00:00Z", content = """{"from":"drive"}""")
        assertEquals(Outcome.Success("""{"from":"drive"}"""), repository(withFile).download())

        assertEquals(Outcome.Success(null), repository(FakeGoogle()).download())
    }

    @Test
    fun theStatusSaysWhenTheBackupInDriveWasLastSaved() = runTest {
        val google = FakeGoogle(folderId = "folder-1", file = "file-1" to "2026-11-27T09:00:00Z")

        assertEquals(Outcome.Success(DriveStatus(true, Instant.parse("2026-11-27T09:00:00Z"))), repository(google).status())
        assertEquals(Outcome.Success(DriveStatus(false, null)), repository(google, MemoryTokens(null)).status())
    }

    @Test
    fun accessTakenBackInGoogleMeansConnectAgain() = runTest {
        val tokens = MemoryTokens("revoked")

        val outcome = repository(FakeGoogle(refreshFails = true), tokens).upload("{}")

        assertEquals(Outcome.Failure(DomainError.DriveNotConnected), outcome)
        assertEquals(null, tokens.token) // forgotten, so Settings shows Connect again
    }

    @Test
    fun noInternetChangesNothing() = runTest {
        val tokens = MemoryTokens("refresh")

        assertEquals(Outcome.Failure(DomainError.DriveUnavailable), repository(FakeGoogle(offline = true), tokens).upload("{}"))
        assertEquals("refresh", tokens.token)
    }

    @Test
    fun beforeConnectNothingIsSentToGoogle() = runTest {
        val google = FakeGoogle()

        assertEquals(Outcome.Failure(DomainError.DriveNotConnected), repository(google, MemoryTokens(null)).upload("{}"))
        assertEquals(emptyList(), google.calls)
    }

    @Test
    fun disconnectForgetsTheAccess() = runTest {
        val tokens = MemoryTokens("refresh")

        repository(FakeGoogle(), tokens).disconnect()

        assertEquals(null, tokens.token)
    }
}
