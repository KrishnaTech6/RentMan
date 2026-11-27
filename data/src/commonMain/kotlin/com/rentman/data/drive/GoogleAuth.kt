package com.rentman.data.drive

import com.rentman.domain.drive.DriveSignIn
import com.rentman.domain.drive.GoogleClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Access to the landlord's Drive on this phone. Each kind of phone gets it its own way (iPhone: [RefreshTokenAuth]). */
internal interface GoogleAuth {
    /** Asks nobody. */
    fun isConnected(): Boolean

    /** Keeps lasting access from a finished sign-in. Throws [NotConnected] if it gives none. */
    suspend fun connect(signIn: DriveSignIn)

    /** A short-lived token for the next few Drive calls. Throws [NotConnected] if the access is gone or was taken back. */
    suspend fun accessToken(): String

    suspend fun disconnect()
}

/** The access is missing or was taken back: Settings offers Connect again. */
internal class NotConnected : Exception()

/** Where something small and private is kept on this phone (the iPhone Keychain). Never in the database or a backup. */
internal interface TokenStore {
    fun read(): String?
    fun write(refreshToken: String)
    fun clear()
}

/** The fingerprint of the data this phone last wrote to or restored from Drive. Kept on the phone, so the check needs no internet. */
internal interface UploadLog {
    fun fingerprint(): String?
    fun record(fingerprint: String)
    fun clear()
}

/**
 * The iPhone's access: Google's code from the browser sign-in is swapped once for a long-lived token kept in [tokens], and that
 * token buys a fresh short-lived one for each Drive job.
 */
internal class RefreshTokenAuth(
    private val http: HttpClient,
    private val tokens: TokenStore,
    private val client: GoogleClient,
) : GoogleAuth {
    override fun isConnected(): Boolean = tokens.read() != null

    override suspend fun connect(signIn: DriveSignIn) {
        val code = signIn as? DriveSignIn.Code ?: throw NotConnected()
        val reply = token {
            append("code", code.code)
            append("code_verifier", code.codeVerifier)
            append("grant_type", "authorization_code")
            append("redirect_uri", client.redirectUri)
        }
        // Google may leave the long-lived token out when this phone already had one: keep that one.
        tokens.write(reply.refreshToken ?: tokens.read() ?: throw NotConnected())
    }

    override suspend fun accessToken(): String {
        val refreshToken = tokens.read() ?: throw NotConnected()
        return try {
            token {
                append("refresh_token", refreshToken)
                append("grant_type", "refresh_token")
            }.accessToken
        } catch (refused: ClientRequestException) {
            // Google refusing the long-lived token means the landlord took the access back in their Google account.
            if (refused.response.status != HttpStatusCode.BadRequest && refused.response.status != HttpStatusCode.Unauthorized) throw refused
            tokens.clear()
            throw NotConnected()
        }
    }

    override suspend fun disconnect() = tokens.clear()

    private suspend fun token(fields: io.ktor.http.ParametersBuilder.() -> Unit): TokenReply =
        JSON.decodeFromString(
            http.submitForm(
                TOKEN_URL,
                parameters {
                    append("client_id", client.clientId)
                    fields()
                },
            ).bodyAsText(),
        )

    @Serializable
    private class TokenReply(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String? = null,
    )

    private companion object {
        const val TOKEN_URL = "https://oauth2.googleapis.com/token"
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
