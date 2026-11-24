package com.rentman.sharedui.components

import androidx.compose.runtime.Composable

/** The biggest file the picker reads. A real backup is KB to a few MB: anything bigger is not one, and reading it could freeze or crash the app. */
internal const val MAX_BACKUP_BYTES = 25 * 1024 * 1024

/**
 * What the phone does for a backup, which needs its screen (a share sheet, a file picker). UI-only, like the
 * notification permission. [share] hands a file to the share sheet and says whether that worked. [pick] opens the file picker.
 */
internal class BackupFiles(val share: (fileName: String, json: String) -> Boolean, val pick: () -> Unit)

/** [onPicked] gets the text of the file the person chose. It is not called if they cancel. */
@Composable
internal expect fun rememberBackupFiles(onPicked: (String) -> Unit): BackupFiles
