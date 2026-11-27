package com.rentman.sharedui.feature.settings

import com.rentman.domain.DomainError
import com.rentman.domain.messages.MessageKind
import com.rentman.domain.settings.AppSettings
import com.rentman.domain.settings.SettingsField
import com.rentman.domain.settings.SettingsForm
import com.rentman.domain.tenancy.FieldProblem
import kotlinx.datetime.LocalDate

internal sealed interface SettingsIntent {
    data class RateChanged(val text: String) : SettingsIntent
    /** Moves the reminder time by [minutes], around midnight. */
    data class TimeStepped(val minutes: Int) : SettingsIntent
    data class RepeatChanged(val days: Int) : SettingsIntent
    data class Save(val form: SettingsForm) : SettingsIntent
    /** Makes a backup file and asks the screen to share it. */
    data object BackUpNow : SettingsIntent
    /** The share sheet opened with the backup file: now it counts as the last backup. */
    data object BackupShared : SettingsIntent
    /** The share sheet could not be opened. */
    data object BackupShareFailed : SettingsIntent
    /** [json] is the text of the file the person picked. */
    data class RestoreFrom(val json: String) : SettingsIntent
    data object UndoRestore : SettingsIntent
    data class OpenMessage(val kind: MessageKind) : SettingsIntent
    data object Back : SettingsIntent
    data object Retry : SettingsIntent
}

internal sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Error(val error: DomainError) : SettingsUiState

    data class Content(
        val rate: String,
        val reminderMinuteOfDay: Int,
        val reminderRepeatDays: Int,
        val problems: Map<SettingsField, FieldProblem> = emptyMap(),
        /** A failure that isn't about one field, e.g. the disk. The typed form is kept. */
        val saveError: DomainError? = null,
        val isSaving: Boolean = false,
        /** What the form was last filled from. If the saved settings change (a restore), the form is filled again. */
        val seed: SettingsForm = SettingsForm(rate),
        val lastBackupOn: LocalDate? = null,
        /** Set after a restore: the day "Undo last restore" goes back to. */
        val undoRestoreOn: LocalDate? = null,
        val backupMessage: BackupMessage? = null,
        /** A failed backup, restore or undo. */
        val backupError: DomainError? = null,
        /** A backup, restore or undo is running. */
        val isWorking: Boolean = false,
    ) : SettingsUiState {
        fun toForm() = SettingsForm(rate)
    }
}

internal enum class BackupMessage { Restored, RestoreUndone, ShareFailed }

internal sealed interface SettingsNavEvent {
    data object Close : SettingsNavEvent
    data class OpenMessage(val kind: MessageKind) : SettingsNavEvent
    /** The backup is made: hand [json] to the phone's share sheet. */
    data class ShareBackup(val fileName: String, val json: String) : SettingsNavEvent
}
