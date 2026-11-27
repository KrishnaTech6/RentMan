package com.rentman.sharedui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberPhotoSource(onPhoto: (photo: String) -> Unit): PhotoSource {
    val context = LocalContext.current
    val currentOnPhoto by rememberUpdatedState(onPhoto)
    // The file being shot is remembered across a rotation: the camera app can outlive this screen's state.
    var shooting by rememberSaveable { mutableStateOf<String?>(null) }
    // A photo waiting to be shrunk: its new name, and where to copy it from (null when the camera already wrote the file).
    var incoming by remember { mutableStateOf<Pair<String, Uri?>?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val name = shooting
        shooting = null
        if (name != null && saved) incoming = name to null else name?.let { MeterPhotoFiles.delete(context, it) }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) incoming = MeterPhotoFiles.newName() to uri
    }
    LaunchedEffect(incoming) {
        val (name, from) = incoming ?: return@LaunchedEffect
        val ready = MeterPhotoFiles.prepare(context, name, from)
        incoming = null
        if (ready) currentOnPhoto(name)
    }
    return remember(camera, gallery) {
        PhotoSource(
            camera = {
                val name = MeterPhotoFiles.newName()
                MeterPhotoFiles.uriFor(context, name)?.let { uri ->
                    shooting = name
                    // No camera app, or it refuses: nothing was taken, so there is nothing to keep.
                    runCatching { camera.launch(uri) }.onFailure { shooting = null }
                }
            },
            // The system photo picker needs no permission: the app only ever sees the one photo that is chosen.
            gallery = { runCatching { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } },
        )
    }
}

@Composable
internal actual fun rememberPhotoFiles(): PhotoFiles {
    val context = LocalContext.current
    return remember(context) {
        PhotoFiles(
            crop = { photo, part -> MeterPhotoFiles.crop(context, photo, part) },
            read = { photo -> MeterPhotoFiles.read(context, photo) },
            discard = { photo -> MeterPhotoFiles.delete(context, photo) },
        )
    }
}

@Composable
internal actual fun rememberPhotoImage(photo: String): ImageBitmap? {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, photo) { value = MeterPhotoFiles.image(context, photo) }.value
}

@Composable
internal actual fun rememberPhotoOpener(): PhotoOpener {
    val context = LocalContext.current
    return remember(context) {
        PhotoOpener(
            exists = { MeterPhotoFiles.exists(context, it) },
            open = { name ->
                MeterPhotoFiles.uriFor(context, name)?.let { uri ->
                    val view = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "image/jpeg").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    runCatching { context.startActivity(view) }
                }
            },
        )
    }
}
