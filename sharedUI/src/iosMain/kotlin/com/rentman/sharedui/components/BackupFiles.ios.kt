package com.rentman.sharedui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.popoverPresentationController
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.darwin.NSObject

/** Holds on to what the picker calls back into: a picker keeps its delegate only weakly. */
@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
private class PickerDelegate(private val onPicked: (String) -> Unit) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
        // The picker made a copy for the app (asCopy), so no special access is needed. A file that is too big or can't be read
        // gives "", which the restore reports as not a backup.
        val size = (url.path?.let { NSFileManager.defaultManager.attributesOfItemAtPath(it, null)?.get(NSFileSize) } as? NSNumber)?.longLongValue
        val text = if (size == null || size > MAX_BACKUP_BYTES) "" else NSString.stringWithContentsOfURL(url, NSUTF8StringEncoding, null).orEmpty()
        onPicked(text)
    }
}

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
@Composable
internal actual fun rememberBackupFiles(onPicked: (String) -> Unit): BackupFiles {
    val currentOnPicked by rememberUpdatedState(onPicked)
    val delegate = remember { PickerDelegate { currentOnPicked(it) } }
    return remember {
        BackupFiles(
            // false if the file could not be written or there is nowhere to show the sheet: the screen then says so.
            share = share@{ fileName, json ->
                val path = NSTemporaryDirectory() + fileName
                val written = NSString.create(string = json).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
                val top = topViewController()
                if (!written || top == null) return@share false
                val sheet = UIActivityViewController(activityItems = listOf(NSURL.fileURLWithPath(path)), applicationActivities = null)
                // On an iPad the share sheet is a popover and needs something to point at.
                sheet.popoverPresentationController?.sourceView = top.view
                top.presentViewController(sheet, animated = true, completion = null)
                true
            },
            pick = {
                val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeJSON, UTTypePlainText), asCopy = true)
                picker.delegate = delegate
                topViewController()?.presentViewController(picker, animated = true, completion = null)
            },
        )
    }
}
