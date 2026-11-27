package com.rentman.sharedui.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.rentman.domain.drive.DriveSignIn
import com.rentman.domain.drive.GoogleClient

/**
 * Google Play services asks for the app's own Drive files. If the landlord allowed it before, it answers at once; otherwise it opens
 * Google's account picker and "Allow" sheet, and the answer comes back here.
 */
@Composable
internal actual fun rememberDriveSignIn(onSignedIn: (DriveSignIn) -> Unit, onFailed: () -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val currentOnSignedIn by rememberUpdatedState(onSignedIn)
    val currentOnFailed by rememberUpdatedState(onFailed)
    val allow = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val granted = result.resultCode == Activity.RESULT_OK &&
            runCatching { Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(result.data) }.isSuccess
        if (granted) currentOnSignedIn(DriveSignIn.Granted) else currentOnFailed()
    }
    return remember(allow) {
        {
            Identity.getAuthorizationClient(context)
                .authorize(AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(GoogleClient.DRIVE_FILE_SCOPE))).build())
                .addOnSuccessListener { result ->
                    val sheet = result.pendingIntent
                    when {
                        !result.hasResolution() -> currentOnSignedIn(DriveSignIn.Granted)
                        sheet != null -> runCatching { allow.launch(IntentSenderRequest.Builder(sheet.intentSender).build()) }.onFailure { currentOnFailed() }
                        else -> currentOnFailed()
                    }
                }
                .addOnFailureListener { currentOnFailed() }
        }
    }
}
