package com.rentman.sharedui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Where a meter photo comes from: the [camera] or the [gallery]. Both need the phone's screen, so this is a UI-only hook like the
 * share sheet. A photo that comes back is already in the app's meter photo folder, turned upright and shrunk, and [onPhoto] gets its
 * file name, and whether the phone already let the person crop it (iOS does, with its own square crop; Android has none, so our own
 * crop screen is shown). It is not called if the person cancels.
 */
internal class PhotoSource(val camera: () -> Unit, val gallery: () -> Unit)

@Composable
internal expect fun rememberPhotoSource(onPhoto: (photo: String, alreadyCropped: Boolean) -> Unit): PhotoSource

/** What is done to a photo that is in the meter photo folder. They work off the main thread. */
internal class PhotoFiles(
    /** Keeps only [CropRect] of the photo, saved over the photo. */
    val crop: suspend (photo: String, part: CropRect) -> Unit,
    /** The lines of text the phone can read on the photo. Empty if it can't read any. */
    val read: suspend (photo: String) -> List<String>,
    val discard: (photo: String) -> Unit,
)

/** How long to wait for the phone to read the text on a photo. A photo it can't read is kept, and the number is typed in instead. */
private val READ_LIMIT = 15.seconds

/** The lines of text on [photo], or none if reading takes longer than [limit], so a stuck reader can never freeze the screen. */
internal suspend fun PhotoFiles.readWithin(photo: String, limit: Duration = READ_LIMIT): List<String> =
    withTimeoutOrNull(limit) { read(photo) } ?: emptyList()

@Composable
internal expect fun rememberPhotoFiles(): PhotoFiles

/** A saved photo as a picture. Null while it loads, and if the file is gone (e.g. after a restore on a new phone). */
@Composable
internal expect fun rememberPhotoImage(photo: String): ImageBitmap?

/** Shows a saved meter photo in the phone's own viewer. [exists] is false if the file is gone, e.g. after a restore on a new phone. */
internal class PhotoOpener(val exists: (photo: String) -> Boolean, val open: (photo: String) -> Unit)

@Composable
internal expect fun rememberPhotoOpener(): PhotoOpener
