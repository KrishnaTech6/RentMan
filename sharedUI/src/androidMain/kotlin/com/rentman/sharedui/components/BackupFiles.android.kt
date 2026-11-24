package com.rentman.sharedui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Reads the file as text, or "" if it can't be read or is bigger than [MAX_BACKUP_BYTES], which the restore then reports as
 * not a backup. It stops reading at the limit, so picking a huge file by mistake costs a little memory, not all of it.
 */
private fun Context.readText(uri: Uri): String = runCatching {
    contentResolver.openInputStream(uri)?.use { input ->
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_BYTES)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            if (out.size() > MAX_BACKUP_BYTES) return@runCatching ""
        }
        out.toString(Charsets.UTF_8.name())
    }
}.getOrNull().orEmpty()

private const val BUFFER_BYTES = 64 * 1024

@Composable
internal actual fun rememberBackupFiles(onPicked: (String) -> Unit): BackupFiles {
    val context = LocalContext.current
    val currentOnPicked by rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        // shortcut: read on the main thread, which is fine up to the size limit. Move it to the io dispatcher if backups grow.
        if (uri != null) currentOnPicked(context.readText(uri))
    }
    return remember(context, launcher) {
        BackupFiles(
            // A full disk or a missing share sheet must not crash the app: say it did not work instead.
            share = { fileName, json ->
                runCatching {
                    val file = File(File(context.cacheDir, "backups").apply { mkdirs() }, fileName).apply { writeText(json) }
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.backups", file)
                    val send = Intent(Intent.ACTION_SEND)
                        .setType("application/json")
                        .putExtra(Intent.EXTRA_STREAM, uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(Intent.createChooser(send, fileName))
                }.isSuccess
            },
            pick = { launcher.launch(arrayOf("*/*")) }, // any file: people rename backups
        )
    }
}
