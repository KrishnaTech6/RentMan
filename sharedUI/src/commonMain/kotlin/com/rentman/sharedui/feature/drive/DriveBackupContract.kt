package com.rentman.sharedui.feature.drive

import com.rentman.domain.DomainError
import com.rentman.domain.drive.DriveSignIn
import com.rentman.domain.drive.DriveBackupStatus

internal sealed interface DriveBackupIntent {
    /** The landlord finished signing in with Google in the browser. */
    data class SignedIn(val signIn: DriveSignIn) : DriveBackupIntent
    /** The browser was closed, or Google said no. */
    data object SignInFailed : DriveBackupIntent
    data object BackUpNow : DriveBackupIntent
    data object Restore : DriveBackupIntent
    data object UndoRestore : DriveBackupIntent
    data object Disconnect : DriveBackupIntent
    data object Back : DriveBackupIntent
    data object Retry : DriveBackupIntent
}

internal sealed interface DriveBackupUiState {
    data object Loading : DriveBackupUiState

    data class Error(val error: DomainError) : DriveBackupUiState

    data class Content(
        val status: DriveBackupStatus,
        /** Connecting, backing up, restoring or undoing is running: the buttons wait. */
        val isWorking: Boolean = false,
        val message: DriveMessage? = null,
        /** What went wrong with the last action. Trying again is safe. */
        val error: DomainError? = null,
    ) : DriveBackupUiState
}

internal enum class DriveMessage { Connected, SignInFailed, BackedUp, Restored, RestoreUndone, NothingInDrive }

internal sealed interface DriveBackupNavEvent {
    data object Close : DriveBackupNavEvent
}
