package com.rentman.sharedui.feature.drive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rentman.domain.DomainError
import com.rentman.domain.drive.DriveBackupStatus
import com.rentman.sharedui.components.ActionButton
import com.rentman.sharedui.components.ActionRow
import com.rentman.sharedui.components.CenteredMessage
import com.rentman.sharedui.components.Divider
import com.rentman.sharedui.components.ErrorMessage
import com.rentman.sharedui.components.ScreenTopBar
import com.rentman.sharedui.components.Section
import com.rentman.sharedui.components.label
import com.rentman.sharedui.components.message
import com.rentman.sharedui.components.rememberDriveSignIn
import com.rentman.sharedui.feature.drive.DriveBackupIntent.Back
import com.rentman.sharedui.feature.drive.DriveBackupIntent.BackUpNow
import com.rentman.sharedui.feature.drive.DriveBackupIntent.Disconnect
import com.rentman.sharedui.feature.drive.DriveBackupIntent.Restore
import com.rentman.sharedui.feature.drive.DriveBackupIntent.Retry
import com.rentman.sharedui.feature.drive.DriveBackupIntent.SignInFailed
import com.rentman.sharedui.feature.drive.DriveBackupIntent.SignedIn
import com.rentman.sharedui.feature.drive.DriveBackupIntent.UndoRestore
import com.rentman.sharedui.feature.drive.DriveBackupUiState.Content
import com.rentman.sharedui.feature.drive.DriveBackupUiState.Error
import com.rentman.sharedui.feature.drive.DriveBackupUiState.Loading
import com.rentman.sharedui.mvi.NavigationEffect
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.drive_android_soon
import com.rentman.sharedui.resources.drive_back_up
import com.rentman.sharedui.resources.drive_backed_up
import com.rentman.sharedui.resources.drive_connect
import com.rentman.sharedui.resources.drive_connected
import com.rentman.sharedui.resources.drive_disconnect
import com.rentman.sharedui.resources.drive_disconnect_hint
import com.rentman.sharedui.resources.drive_hint
import com.rentman.sharedui.resources.drive_last
import com.rentman.sharedui.resources.drive_never
import com.rentman.sharedui.resources.drive_nothing
import com.rentman.sharedui.resources.drive_restore
import com.rentman.sharedui.resources.drive_restore_hint
import com.rentman.sharedui.resources.drive_restored
import com.rentman.sharedui.resources.drive_sign_in_failed
import com.rentman.sharedui.resources.drive_title
import com.rentman.sharedui.resources.drive_undo
import com.rentman.sharedui.resources.drive_undone
import com.rentman.sharedui.resources.loading
import com.rentman.sharedui.theme.RentManColors
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** The only composable that knows the ViewModel (ARCHITECTURE.md §5). */
@Composable
internal fun DriveBackupRoute(onDone: () -> Unit, viewModel: DriveBackupViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val signIn = rememberDriveSignIn(onSignedIn = { viewModel.onIntent(SignedIn(it)) }, onFailed = { viewModel.onIntent(SignInFailed) })
    NavigationEffect(viewModel.navigation) { onDone() }
    DriveBackupScreen(state, onSignIn = signIn, onIntent = viewModel::onIntent)
}

/** [onSignIn] opens Google's sign-in; null where Drive backup isn't built yet. */
@Composable
internal fun DriveBackupScreen(state: DriveBackupUiState, onSignIn: (() -> Unit)?, onIntent: (DriveBackupIntent) -> Unit) {
    Column(Modifier.fillMaxSize().background(RentManColors.Background)) {
        ScreenTopBar(title = stringResource(Res.string.drive_title), onBack = { onIntent(Back) })
        when (state) {
            Loading -> CenteredMessage(stringResource(Res.string.loading))
            is Error -> ErrorMessage(state.error, onRetry = { onIntent(Retry) })
            is Content -> Body(state, onSignIn, onIntent)
        }
    }
}

@Composable
private fun Body(state: Content, onSignIn: (() -> Unit)?, onIntent: (DriveBackupIntent) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.drive_hint), RentManColors.TextMuted)
        if (!state.status.connected) {
            if (onSignIn == null) {
                Text(stringResource(Res.string.drive_android_soon), RentManColors.Text)
            } else {
                ActionButton(stringResource(Res.string.drive_connect), enabled = !state.isWorking, onClick = onSignIn, modifier = Modifier.fillMaxWidth())
            }
        } else {
            Section(stringResource(Res.string.drive_title)) {
                BasicText(
                    state.status.lastBackupOn?.let { stringResource(Res.string.drive_last, it.label()) } ?: stringResource(Res.string.drive_never),
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.Text),
                    modifier = Modifier.padding(top = 12.dp),
                )
                ActionRow(stringResource(Res.string.drive_back_up), onClick = { onIntent(BackUpNow) }, enabled = !state.isWorking)
                Divider()
                ActionRow(
                    stringResource(Res.string.drive_restore),
                    onClick = { onIntent(Restore) },
                    hint = stringResource(Res.string.drive_restore_hint),
                    enabled = !state.isWorking,
                )
                if (state.message == DriveMessage.Restored) {
                    Divider()
                    ActionRow(stringResource(Res.string.drive_undo), onClick = { onIntent(UndoRestore) }, enabled = !state.isWorking)
                }
                Divider()
                ActionRow(
                    stringResource(Res.string.drive_disconnect),
                    onClick = { onIntent(Disconnect) },
                    hint = stringResource(Res.string.drive_disconnect_hint),
                    enabled = !state.isWorking,
                )
            }
        }
        state.message?.let { Text(it.text(), if (it == DriveMessage.SignInFailed || it == DriveMessage.NothingInDrive) RentManColors.Danger else RentManColors.Success) }
        state.error?.let { Text(it.message(), RentManColors.Danger) }
    }
}

@Composable
private fun DriveMessage.text(): String = stringResource(
    when (this) {
        DriveMessage.Connected -> Res.string.drive_connected
        DriveMessage.SignInFailed -> Res.string.drive_sign_in_failed
        DriveMessage.BackedUp -> Res.string.drive_backed_up
        DriveMessage.Restored -> Res.string.drive_restored
        DriveMessage.RestoreUndone -> Res.string.drive_undone
        DriveMessage.NothingInDrive -> Res.string.drive_nothing
    },
)

@Composable
private fun Text(text: String, color: androidx.compose.ui.graphics.Color) {
    BasicText(text, style = TextStyle(fontSize = 14.sp, color = color), modifier = Modifier.padding(horizontal = 4.dp))
}

@Preview
@Composable
private fun LoadingPreview() = DriveBackupScreen(Loading, onSignIn = {}) {}

@Preview
@Composable
private fun ErrorPreview() = DriveBackupScreen(Error(DomainError.DriveUnavailable), onSignIn = {}) {}

@Preview
@Composable
private fun NotConnectedPreview() = DriveBackupScreen(Content(DriveBackupStatus(false, null)), onSignIn = {}) {}

@Preview
@Composable
private fun ConnectedPreview() =
    DriveBackupScreen(Content(DriveBackupStatus(true, LocalDate(2026, 11, 27)), message = DriveMessage.BackedUp), onSignIn = {}) {}
