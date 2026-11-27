package com.rentman.sharedui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.crop_cancel
import com.rentman.sharedui.resources.crop_hint
import com.rentman.sharedui.resources.crop_image_description
import com.rentman.sharedui.resources.crop_title
import com.rentman.sharedui.resources.crop_use
import com.rentman.sharedui.resources.crop_whole
import com.rentman.sharedui.theme.RentManColors
import org.jetbrains.compose.resources.stringResource
import kotlin.math.min

/** What a finger that went down on the photo is holding: a corner of the box, or the box itself. */
private sealed interface Grab {
    data class Edge(val corner: Corner) : Grab
    data object Whole : Grab
}

/**
 * A full-screen view of a new photo with a box on it. The person drags the box, or its corners, around the meter's number and taps
 * "Use this part", or keeps the whole photo, or cancels. It only picks a [CropRect]: the photo is cut somewhere else.
 */
@Composable
internal fun MeterCropper(photo: String, onUse: (CropRect) -> Unit, onCancel: () -> Unit) {
    // Centred, the size the landlord picked for a whole meter on a real phone (about 40% of the photo each way). They drag it to fit.
    var box by remember { mutableStateOf(CropRect(left = 0.3f, top = 0.3f, right = 0.7f, bottom = 0.7f)) }
    val image = rememberPhotoImage(photo)
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Column(
            Modifier.fillMaxSize().background(Color.Black).systemBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BasicText(stringResource(Res.string.crop_title), style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White))
            BasicText(stringResource(Res.string.crop_hint), style = TextStyle(fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f)))
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (image != null) CropArea(image, box, onBox = { box = it }, Modifier.fillMaxSize())
            }
            ActionButton(stringResource(Res.string.crop_use), enabled = image != null, onClick = { onUse(box) }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LightButton(stringResource(Res.string.crop_whole), enabled = image != null, onClick = { onUse(CropRect.Whole) }, Modifier.weight(1f))
                LightButton(stringResource(Res.string.crop_cancel), enabled = true, onClick = onCancel, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LightButton(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier.heightIn(min = 48.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text,
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f), textAlign = TextAlign.Center),
        )
    }
}

/** The photo fitted into the space, with the dimmed outside, the box and its four corner handles drawn over it. */
@Composable
private fun CropArea(image: ImageBitmap, box: CropRect, onBox: (CropRect) -> Unit, modifier: Modifier) {
    val currentBox by rememberUpdatedState(box)
    val currentOnBox by rememberUpdatedState(onBox)
    val density = LocalDensity.current
    val reach = with(density) { 44.dp.toPx() }
    BoxWithConstraints(modifier) {
        val areaWidth = constraints.maxWidth.toFloat()
        val areaHeight = constraints.maxHeight.toFloat()
        val scale = min(areaWidth / image.width, areaHeight / image.height)
        val shown = Size(image.width * scale, image.height * scale)
        val origin = Offset((areaWidth - shown.width) / 2, (areaHeight - shown.height) / 2)

        fun cornerAt(corner: Corner, rect: CropRect) = origin + Offset(
            (if (corner == Corner.TopLeft || corner == Corner.BottomLeft) rect.left else rect.right) * shown.width,
            (if (corner == Corner.TopLeft || corner == Corner.TopRight) rect.top else rect.bottom) * shown.height,
        )

        fun grabAt(point: Offset): Grab? {
            val rect = currentBox
            val nearest = Corner.entries.minBy { (cornerAt(it, rect) - point).getDistance() }
            if ((cornerAt(nearest, rect) - point).getDistance() <= reach) return Grab.Edge(nearest)
            val inside = point.x in cornerAt(Corner.TopLeft, rect).x..cornerAt(Corner.BottomRight, rect).x &&
                point.y in cornerAt(Corner.TopLeft, rect).y..cornerAt(Corner.BottomRight, rect).y
            return if (inside) Grab.Whole else null
        }

        Image(image, contentDescription = stringResource(Res.string.crop_image_description), contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        Canvas(
            Modifier.fillMaxSize().pointerInput(shown, origin) {
                var holding: Grab? = null
                detectDragGestures(
                    onDragStart = { holding = grabAt(it) },
                    onDragEnd = { holding = null },
                    onDragCancel = { holding = null },
                    onDrag = { change, amount ->
                        val dx = amount.x / shown.width
                        val dy = amount.y / shown.height
                        when (val grab = holding) {
                            is Grab.Edge -> currentOnBox(currentBox.withCornerMoved(grab.corner, dx, dy))
                            Grab.Whole -> currentOnBox(currentBox.moved(dx, dy))
                            null -> Unit
                        }
                        if (holding != null) change.consume()
                    },
                )
            },
        ) {
            val dim = Color.Black.copy(alpha = 0.6f)
            val topLeft = cornerAt(Corner.TopLeft, box)
            val bottomRight = cornerAt(Corner.BottomRight, box)
            drawRect(dim, origin, Size(shown.width, topLeft.y - origin.y))
            drawRect(dim, Offset(origin.x, bottomRight.y), Size(shown.width, origin.y + shown.height - bottomRight.y))
            drawRect(dim, Offset(origin.x, topLeft.y), Size(topLeft.x - origin.x, bottomRight.y - topLeft.y))
            drawRect(dim, Offset(bottomRight.x, topLeft.y), Size(origin.x + shown.width - bottomRight.x, bottomRight.y - topLeft.y))
            drawRect(Color.White, topLeft, Size(bottomRight.x - topLeft.x, bottomRight.y - topLeft.y), style = Stroke(2.dp.toPx()))
            Corner.entries.forEach {
                drawCircle(Color.White, radius = 11.dp.toPx(), center = cornerAt(it, box))
                drawCircle(RentManColors.Primary, radius = 7.dp.toPx(), center = cornerAt(it, box))
            }
        }
    }
}
