@file:OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest

package com.rentman.sharedui.feature.drive

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.backup.UndoRestoreUseCase
import com.rentman.domain.drive.BackUpToDriveUseCase
import com.rentman.domain.drive.ConnectDriveUseCase
import com.rentman.domain.drive.DisconnectDriveUseCase
import com.rentman.domain.drive.GetDriveStatusUseCase
import com.rentman.domain.drive.RestoreFromDriveUseCase
import com.rentman.domain.fold
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
import com.rentman.sharedui.mvi.MviViewModel
import com.rentman.sharedui.mvi.Reducer
import com.rentman.sharedui.mvi.flatMapFirst
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

private typealias State = DriveBackupUiState

internal class DriveBackupViewModel(
    private val getStatus: GetDriveStatusUseCase,
    private val connect: ConnectDriveUseCase,
    private val backUp: BackUpToDriveUseCase,
    private val restore: RestoreFromDriveUseCase,
    private val undoRestore: UndoRestoreUseCase,
    private val disconnect: DisconnectDriveUseCase,
) : MviViewModel<DriveBackupIntent, State, DriveBackupNavEvent>() {

    private val unchanged: Reducer<State> = { it }

    override val uiState: StateFlow<State> = merge(
        loadReducers(),
        actionReducers(),
        navigationReducers(),
    ).toState(Loading)

    private fun loadReducers(): Flow<Reducer<State>> =
        intents.filterIsInstance<Retry>().map { }.onStart { emit(Unit) }
            .flatMapLatest {
                flow {
                    emit { Loading }
                    emit(getStatus().fold(onSuccess = { status -> { _: State -> Content(status) } }, onFailure = { error -> { _: State -> Error(error) } }))
                }
            }

    private fun actionReducers(): Flow<Reducer<State>> = merge(
        intents.filterIsInstance<SignedIn>().flatMapFirst { signedIn -> act(DriveMessage.Connected) { connect(signedIn.signIn) } },
        intents.filterIsInstance<SignInFailed>().map { content { it.copy(message = DriveMessage.SignInFailed, error = null) } },
        intents.filterIsInstance<BackUpNow>().flatMapFirst { act(DriveMessage.BackedUp) { backUp() } },
        intents.filterIsInstance<Restore>().flatMapFirst { act(DriveMessage.Restored) { restore() } },
        intents.filterIsInstance<UndoRestore>().flatMapFirst { act(DriveMessage.RestoreUndone) { undoRestore() } },
        intents.filterIsInstance<Disconnect>().flatMapFirst { act(done = null) { disconnect() } },
    )

    /**
     * Runs one action, then shows Drive's status again (a backup changes its time, a refused access disconnects). Nothing in Drive
     * to restore is a message, not an error.
     */
    private fun act(done: DriveMessage?, action: suspend () -> Outcome<Unit>): Flow<Reducer<State>> = flow {
        emit(content { it.copy(isWorking = true, message = null, error = null) })
        val outcome = action()
        val status = getStatus().fold(onSuccess = { it }, onFailure = { null })
        emit(
            content { state ->
                val shown = state.copy(isWorking = false, status = status ?: state.status)
                when (outcome) {
                    is Outcome.Success -> shown.copy(message = done)
                    is Outcome.Failure ->
                        if (outcome.error == DomainError.NotFound) shown.copy(message = DriveMessage.NothingInDrive) else shown.copy(error = outcome.error)
                }
            },
        )
    }

    private fun navigationReducers(): Flow<Reducer<State>> = intents
        .onEach { if (it == Back) navigate(DriveBackupNavEvent.Close) }
        .map { unchanged }

    private fun content(change: (Content) -> Content): Reducer<State> = { state -> if (state is Content) change(state) else state }
}
