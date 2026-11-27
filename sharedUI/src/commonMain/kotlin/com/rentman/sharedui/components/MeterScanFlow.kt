package com.rentman.sharedui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.crop_reading
import com.rentman.sharedui.resources.meter_photo_description
import com.rentman.sharedui.theme.RentManColors
import org.jetbrains.compose.resources.stringResource

/**
 * Getting a meter photo into the app: take it with the camera or choose it from the gallery, pick the part to keep, then read the
 * text on that part. [content] gets the two ways to start. [onScanned] is called once, with the kept photo's file name and the lines
 * of text on it. Backing out at the crop screen throws the photo away. The ViewModel only ever sees the finished result.
 */
@Composable
internal fun MeterScanFlow(
    onScanned: (photo: String, lines: List<String>) -> Unit,
    content: @Composable (camera: () -> Unit, gallery: () -> Unit) -> Unit,
) {
    val files = rememberPhotoFiles()
    val currentOnScanned by rememberUpdatedState(onScanned)
    var cropping by rememberSaveable { mutableStateOf<String?>(null) }
    var finishing by remember { mutableStateOf<Pair<String, CropRect>?>(null) }
    val source = rememberPhotoSource { photo -> cropping = photo }
    LaunchedEffect(finishing) {
        val (photo, part) = finishing ?: return@LaunchedEffect
        if (!part.isWhole) files.crop(photo, part)
        val lines = files.readWithin(photo)
        finishing = null
        currentOnScanned(photo, lines)
    }
    content(source.camera, source.gallery)
    cropping?.let { photo ->
        MeterCropper(
            photo,
            onUse = { part ->
                cropping = null
                finishing = photo to part
            },
            onCancel = {
                files.discard(photo)
                cropping = null
            },
        )
    }
    if (finishing != null) {
        Dialog(onDismissRequest = {}) {
            Box(Modifier.background(RentManColors.Surface, RoundedCornerShape(16.dp)).padding(24.dp)) {
                BasicText(stringResource(Res.string.crop_reading), style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.Text))
            }
        }
    }
}

/** A saved meter photo as a picture, tap to open it big. Nothing is shown if the file isn't there. */
@Composable
internal fun MeterPhotoThumbnail(photo: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val image = rememberPhotoImage(photo) ?: return
    Image(
        image,
        contentDescription = stringResource(Res.string.meter_photo_description),
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick),
    )
}
