package com.rentman.sharedui.components

import androidx.compose.runtime.Composable
import com.rentman.domain.drive.DriveSignIn

/**
 * Opens Google's own sign-in for Drive backup. It needs the screen, so it is a UI-only hook like the share sheet: [onSignedIn]
 * gets what Google handed back, for the ViewModel to connect with; [onFailed] when the person closed it or Google said no.
 * Null on a phone where Drive backup isn't built yet (Android).
 */
@Composable
internal expect fun rememberDriveSignIn(onSignedIn: (DriveSignIn) -> Unit, onFailed: () -> Unit): (() -> Unit)?
