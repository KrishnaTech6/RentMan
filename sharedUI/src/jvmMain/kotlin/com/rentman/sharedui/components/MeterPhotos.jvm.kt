package com.rentman.sharedui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/** The JVM target only runs tests: there is no camera, no gallery and no photo viewer. */
@Composable
internal actual fun rememberPhotoSource(onPhoto: (photo: String, alreadyCropped: Boolean) -> Unit): PhotoSource = PhotoSource(camera = {}, gallery = {})

@Composable
internal actual fun rememberPhotoFiles(): PhotoFiles = PhotoFiles(crop = { _, _ -> }, read = { emptyList() }, discard = {})

@Composable
internal actual fun rememberPhotoImage(photo: String): ImageBitmap? = null

@Composable
internal actual fun rememberPhotoOpener(): PhotoOpener = PhotoOpener(exists = { false }, open = {})
