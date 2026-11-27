@file:OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest

package com.rentman.sharedui.feature.settings

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.backup.BackupStatus
import com.rentman.domain.backup.ExportBackupUseCase
import com.rentman.domain.backup.MarkBackedUpUseCase
import com.rentman.domain.backup.ObserveBackupStatusUseCase
import com.rentman.domain.backup.RestoreBackupUseCase
import com.rentman.domain.backup.UndoRestoreUseCase
import com.rentman.domain.flatMap
import com.rentman.domain.fold
import com.rentman.domain.map
import com.rentman.domain.money.MoneyInput
import com.rentman.domain.settings.AppSettings
import com.rentman.domain.settings.ObserveSettingsUseCase
import com.rentman.domain.settings.SaveSettingsUseCase
import com.rentman.domain.settings.SetReminderRepeatUseCase
import com.rentman.domain.settings.StepReminderTimeUseCase
import com.rentman.domain.settings.SettingsField
import com.rentman.domain.settings.SettingsForm
import com.rentman.sharedui.feature.settings.SettingsIntent.Back
import com.rentman.sharedui.feature.settings.SettingsIntent.OpenDrive
import com.rentman.sharedui.feature.settings.SettingsIntent.OpenMessage
import com.rentman.sharedui.feature.settings.SettingsIntent.BackUpNow
import com.rentman.sharedui.feature.settings.SettingsIntent.BackupShareFailed
import com.rentman.sharedui.feature.settings.SettingsIntent.BackupShared
import com.rentman.sharedui.feature.settings.SettingsIntent.RateChanged
import com.rentman.sharedui.feature.settings.SettingsIntent.RepeatChanged
import com.rentman.sharedui.feature.settings.SettingsIntent.RestoreFrom
import com.rentman.sharedui.feature.settings.SettingsIntent.Retry
import com.rentman.sharedui.feature.settings.SettingsIntent.Save
import com.rentman.sharedui.feature.settings.SettingsIntent.TimeStepped
import com.rentman.sharedui.feature.settings.SettingsIntent.UndoRestore
import com.rentman.sharedui.feature.settings.SettingsUiState.Content
import com.rentman.sharedui.feature.settings.SettingsUiState.Error
import com.rentman.sharedui.feature.settings.SettingsUiState.Loading
import com.rentman.sharedui.mvi.MviViewModel
import com.rentman.sharedui.mvi.Reducer
import com.rentman.sharedui.mvi.flatMapFirst
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

private typealias SettingsState = SettingsUiState

internal class SettingsViewModel(
    observeSettings: ObserveSettingsUseCase,
    observeBackupStatus: ObserveBackupStatusUseCase,
    private val saveSettings: SaveSettingsUseCase,
    private val stepReminderTime: StepReminderTimeUseCase,
    private val setReminderRepeat: SetReminderRepeatUseCase,
    private val exportBackup: ExportBackupUseCase,
    private val markBackedUp: MarkBackedUpUseCase,
    private val restoreBackup: RestoreBackupUseCase,
    private val undoRestore: UndoRestoreUseCase,
) : MviViewModel<SettingsIntent, SettingsState, SettingsNavEvent>() {

    private val unchanged: Reducer<SettingsState> = { it }

    override val uiState: StateFlow<SettingsState> = merge(
        loadReducers(observeSettings, observeBackupStatus),
        inputReducers(),
        reminderReducers(),
        saveReducers(),
        backupReducers(),
        navigationReducers(),
    ).toState(Loading)

    private fun loadReducers(observeSettings: ObserveSettingsUseCase, observeBackupStatus: ObserveBackupStatusUseCase): Flow<Reducer<SettingsState>> =
        intents.filterIsInstance<Retry>().map { }.onStart { emit(Unit) }
            .flatMapLatest {
                combine(observeSettings(), observeBackupStatus()) { settings, status ->
                    settings.flatMap { s -> status.map { s to it } }
                }
                    .map { outcome -> outcome.fold(onSuccess = { (settings, status) -> withData(settings, status) }, onFailure = ::toError) }
                    .onStart { emit { Loading } }
            }

    private fun inputReducers(): Flow<Reducer<SettingsState>> = intents.map { intent ->
        when (intent) {
            is RateChanged -> content { it.copy(rate = intent.text, problems = it.problems - SettingsField.Rate, saveError = null) }
            // The reminder time and interval are saved at once (see reminderReducers) and the form follows what was saved.
            is TimeStepped, is RepeatChanged, is Save, BackUpNow, BackupShared, BackupShareFailed, is RestoreFrom, UndoRestore, is OpenMessage, OpenDrive, Back, Retry ->
                unchanged
        }
    }

    /**
     * The reminder time and interval have no wrong value to check, so they are saved the moment they are changed, not with the Save
     * button: leaving the screen without it used to throw the change away. All of them go through one queue, so quick taps all count
     * and two saves can never read the same old settings. The screen then shows what was saved, and the reminders are planned again.
     */
    private fun reminderReducers(): Flow<Reducer<SettingsState>> = intents
        .mapNotNull { intent ->
            when (intent) {
                is TimeStepped -> suspend { stepReminderTime(intent.minutes) }
                is RepeatChanged -> suspend { setReminderRepeat(intent.days) }
                else -> null
            }
        }
        .flatMapConcat { save ->
            flow { emit(save().fold(onSuccess = { content { it.copy(saveError = null) } }, onFailure = ::reminderNotSaved)) }
        }

    private fun reminderNotSaved(error: DomainError): Reducer<SettingsState> = content { it.withError(error) }

    private fun saveReducers(): Flow<Reducer<SettingsState>> = intents.filterIsInstance<Save>().flatMapFirst { save ->
        flow {
            emit(content { it.copy(isSaving = true, problems = emptyMap(), saveError = null) })
            val outcome = saveSettings(save.form)
            emit(
                outcome.fold(
                    onSuccess = { content { it.copy(isSaving = false) } },
                    onFailure = { error -> content { it.copy(isSaving = false).withError(error) } },
                ),
            )
            if (outcome is Outcome.Success) navigate(SettingsNavEvent.Close)
        }
    }

    private fun backupReducers(): Flow<Reducer<SettingsState>> = merge(
        intents.filterIsInstance<BackUpNow>().flatMapFirst {
            flow {
                emit(content { it.copy(isWorking = true, backupMessage = null, backupError = null) })
                val outcome = exportBackup()
                emit(outcome.fold(onSuccess = { content { it.copy(isWorking = false) } }, onFailure = ::backupFailed))
                if (outcome is Outcome.Success) navigate(SettingsNavEvent.ShareBackup(outcome.value.fileName, outcome.value.json))
            }
        },
        intents.filterIsInstance<BackupShared>().flatMapFirst {
            flow {
                emit(markBackedUp().fold(onSuccess = { content { it.copy(backupError = null) } }, onFailure = ::backupFailed))
            }
        },
        intents.filterIsInstance<BackupShareFailed>().flatMapFirst {
            flow { emit(content { it.copy(backupMessage = BackupMessage.ShareFailed) }) }
        },
        intents.filterIsInstance<RestoreFrom>().flatMapFirst { restore ->
            flow {
                emit(content { it.copy(isWorking = true, backupMessage = null, backupError = null) })
                emit(
                    restoreBackup(restore.json).fold(
                        onSuccess = { content { it.copy(isWorking = false, backupMessage = BackupMessage.Restored) } },
                        onFailure = ::backupFailed,
                    ),
                )
            }
        },
        intents.filterIsInstance<UndoRestore>().flatMapFirst {
            flow {
                emit(content { it.copy(isWorking = true, backupMessage = null, backupError = null) })
                emit(
                    undoRestore().fold(
                        onSuccess = { content { it.copy(isWorking = false, backupMessage = BackupMessage.RestoreUndone) } },
                        onFailure = ::backupFailed,
                    ),
                )
            }
        },
    )

    private fun navigationReducers(): Flow<Reducer<SettingsState>> = intents
        .onEach { intent ->
            when (intent) {
                Back -> navigate(SettingsNavEvent.Close)
                is OpenMessage -> navigate(SettingsNavEvent.OpenMessage(intent.kind))
                OpenDrive -> navigate(SettingsNavEvent.OpenDrive)
                else -> Unit
            }
        }
        .map { unchanged }

    /**
     * Data can arrive again: a backup was made, the reminder time or interval was saved, or a restore changed the settings. With the
     * same saved rate, what is typed in the rate field is kept and the reminder time and interval follow what was saved. If the saved
     * rate itself changed (a restore), the form is filled again.
     */
    private fun withData(settings: AppSettings, status: BackupStatus): Reducer<SettingsState> = { state ->
        val seed = SettingsForm(MoneyInput.text(settings.globalRate))
        val shown = if (state is Content && state.seed == seed) {
            state.copy(reminderMinuteOfDay = settings.reminderMinuteOfDay, reminderRepeatDays = settings.reminderRepeatDays)
        } else {
            Content(
                rate = seed.rate,
                reminderMinuteOfDay = settings.reminderMinuteOfDay,
                reminderRepeatDays = settings.reminderRepeatDays,
                seed = seed,
                backupMessage = (state as? Content)?.backupMessage,
                backupError = (state as? Content)?.backupError,
                isWorking = (state as? Content)?.isWorking ?: false,
            )
        }
        shown.copy(lastBackupOn = status.lastBackupOn, undoRestoreOn = status.undoSinceOn)
    }

    private fun toError(error: DomainError): Reducer<SettingsState> = { Error(error) }

    private fun backupFailed(error: DomainError): Reducer<SettingsState> = content { it.copy(isWorking = false, backupError = error) }

    private fun content(change: (Content) -> Content): Reducer<SettingsState> = { state ->
        if (state is Content) change(state) else state
    }

    private fun Content.withError(error: DomainError): Content =
        if (error is DomainError.InvalidSettings) copy(problems = error.problems) else copy(saveError = error)
}
