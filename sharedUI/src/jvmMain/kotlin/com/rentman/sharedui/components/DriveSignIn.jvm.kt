package com.rentman.sharedui.components

import androidx.compose.runtime.Composable
import com.rentman.domain.drive.DriveSignIn

// shortcut: Drive backup is iPhone only for now; Android gets Play services sign-in in its own step.
@Composable
internal actual fun rememberDriveSignIn(onSignedIn: (DriveSignIn) -> Unit, onFailed: () -> Unit): (() -> Unit)? = null
