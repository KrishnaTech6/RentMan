package com.rentman.sharedui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.rentman.domain.drive.DriveSignIn
import com.rentman.domain.drive.GoogleClient
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.io.encoding.Base64

private const val AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"

/** Only the files the app makes itself; nothing else in the landlord's Drive. */
private const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"

/** The app's iPhone key with Google (made once in Google Cloud for bundle com.rentmanager.landlord.dev). Not a secret. */
internal val IosGoogleClient = GoogleClient("896399803306-vgj57dcgje0hc86cjumoj14bgvl9jjof.apps.googleusercontent.com")

@Composable
internal actual fun rememberDriveSignIn(onSignedIn: (DriveSignIn) -> Unit, onFailed: () -> Unit): (() -> Unit)? {
    val currentOnSignedIn by rememberUpdatedState(onSignedIn)
    val currentOnFailed by rememberUpdatedState(onFailed)
    val signIn = remember { GoogleSignIn(IosGoogleClient) }
    return remember { { signIn.start(onSignedIn = { currentOnSignedIn(it) }, onFailed = { currentOnFailed() }) } }
}

/**
 * Google's sign-in in Apple's secure browser sheet, with PKCE: a fresh secret per sign-in, so a code caught on the way is no use
 * to anyone else, and a random state so only the answer to this request is taken. The session is held here while it is open:
 * iOS keeps only a weak hold on it, and the answer would never come back.
 */
@OptIn(ExperimentalForeignApi::class)
private class GoogleSignIn(private val client: GoogleClient) : NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {
    private var session: ASWebAuthenticationSession? = null

    fun start(onSignedIn: (DriveSignIn) -> Unit, onFailed: () -> Unit) {
        if (session != null) return // already open
        val verifier = randomText()
        val state = randomText()
        val url = NSURLComponents(string = AUTH_URL).apply {
            queryItems = listOf(
                NSURLQueryItem(name = "client_id", value = client.clientId),
                NSURLQueryItem(name = "redirect_uri", value = client.redirectUri),
                NSURLQueryItem(name = "response_type", value = "code"),
                NSURLQueryItem(name = "scope", value = DRIVE_FILE_SCOPE),
                NSURLQueryItem(name = "code_challenge", value = challengeOf(verifier)),
                NSURLQueryItem(name = "code_challenge_method", value = "S256"),
                NSURLQueryItem(name = "state", value = state),
            )
        }.URL ?: return onFailed()
        val opened = ASWebAuthenticationSession(uRL = url, callbackURLScheme = client.redirectScheme) { callback: NSURL?, _ ->
            dispatch_async(dispatch_get_main_queue()) {
                session = null
                val answer = callback?.let { NSURLComponents(uRL = it, resolvingAgainstBaseURL = false).queryItems.orEmpty() }
                    .orEmpty().mapNotNull { it as? NSURLQueryItem }.associate { it.name to it.value }
                val code = answer["code"]
                if (code != null && answer["state"] == state) onSignedIn(DriveSignIn(code, verifier)) else onFailed()
            }
        }
        opened.presentationContextProvider = this
        session = opened
        if (!opened.start()) {
            session = null
            onFailed()
        }
    }

    override fun presentationAnchorForWebAuthenticationSession(session: ASWebAuthenticationSession): ASPresentationAnchor =
        topViewController()?.view?.window ?: UIWindow()

    /** 64 random letters, digits, "-" and "_": the PKCE secret and the state. */
    private fun randomText(): String {
        val bytes = ByteArray(48)
        bytes.usePinned { SecRandomCopyBytes(kSecRandomDefault, bytes.size.convert(), it.addressOf(0)) }
        return Base64.UrlSafe.encode(bytes)
    }

    /** What Google is shown in place of the secret: its SHA-256, URL-safe Base64 without padding. */
    private fun challengeOf(verifier: String): String {
        val input = verifier.encodeToByteArray()
        val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
        input.usePinned { data -> digest.usePinned { out -> CC_SHA256(data.addressOf(0), input.size.convert(), out.addressOf(0)) } }
        return Base64.UrlSafe.encode(digest.toByteArray()).trimEnd('=')
    }
}
