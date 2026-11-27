package com.rentman.data.drive

import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.rentman.domain.drive.DriveSignIn
import com.rentman.domain.drive.GoogleClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Android's access, through Google Play services: once the landlord allowed it, Play services hands out a fresh short-lived token
 * whenever asked, with nothing long-lived kept here. Google matches the app by its package name and signing fingerprint. Only "is
 * connected" is remembered, in [preferences].
 */
internal class PlayServicesAuth(private val context: Context) : GoogleAuth {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    override fun isConnected(): Boolean = preferences.getBoolean(CONNECTED, false)

    override suspend fun connect(signIn: DriveSignIn) {
        if (signIn != DriveSignIn.Granted || authorize().hasResolution()) throw NotConnected()
        preferences.edit().putBoolean(CONNECTED, true).apply()
    }

    override suspend fun accessToken(): String {
        if (!isConnected()) throw NotConnected()
        val result = authorize()
        // Asking again would need the landlord: the access was taken back in their Google account.
        if (result.hasResolution()) {
            disconnect()
            throw NotConnected()
        }
        return result.accessToken ?: throw NotConnected()
    }

    override suspend fun disconnect() {
        preferences.edit().remove(CONNECTED).apply()
    }

    private suspend fun authorize(): AuthorizationResult = suspendCancellableCoroutine { continuation ->
        Identity.getAuthorizationClient(context)
            .authorize(AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(GoogleClient.DRIVE_FILE_SCOPE))).build())
            .addOnSuccessListener { continuation.resume(it) }
            .addOnFailureListener { continuation.resumeWithException(it) }
    }

    private companion object {
        const val PREFERENCES = "google_drive"
        const val CONNECTED = "connected"
    }
}

/** The fingerprint of what this phone last backed up to Drive, in the same private preferences. */
internal class PreferencesUploadLog(context: Context) : UploadLog {
    private val preferences = context.getSharedPreferences("google_drive", Context.MODE_PRIVATE)

    override fun fingerprint(): String? = preferences.getString(SYNCED, null)
    override fun record(fingerprint: String) = preferences.edit().putString(SYNCED, fingerprint).apply()
    override fun clear() = preferences.edit().remove(SYNCED).apply()

    private companion object {
        const val SYNCED = "synced_fingerprint"
    }
}
