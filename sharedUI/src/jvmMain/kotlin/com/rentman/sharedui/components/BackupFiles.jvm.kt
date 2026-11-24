package com.rentman.sharedui.components

import androidx.compose.runtime.Composable

/** The JVM target only runs tests: there is no share sheet or file picker. */
@Composable
internal actual fun rememberBackupFiles(onPicked: (String) -> Unit): BackupFiles = BackupFiles(share = { _, _ -> true }, pick = {})
