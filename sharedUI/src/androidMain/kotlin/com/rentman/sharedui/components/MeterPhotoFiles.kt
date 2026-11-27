package com.rentman.sharedui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.rentman.domain.AppDispatchers
import com.rentman.domain.billing.MeterPhotoName
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.math.max

/**
 * The proof photos of meter readings: a folder in the app's own storage, one JPEG per photo, named by a random id.
 * shortcut: a photo that is retaken, removed or never saved stays in the folder, so storage grows a little with each retake. Sweep the
 * names no bill uses when it matters.
 */
internal object MeterPhotoFiles {
    private const val FOLDER = "meter_photos"

    /** Big enough to read a meter and to be proof, small enough that a year of photos is not a gigabyte. */
    private const val MAX_SIDE = 1_600
    private const val JPEG_QUALITY = 80

    fun newName(): String = UUID.randomUUID().toString().replace("-", "").take(24) + ".jpg"

    /** Only a plain file name is ever joined to the folder, so a name from a backup file can't point anywhere else. */
    private fun fileOf(context: Context, name: String): File? =
        name.takeIf(MeterPhotoName::isValid)?.let { File(File(context.filesDir, FOLDER), it) }

    /** The camera writes to this address, so the folder has to exist before it is handed out. */
    fun uriFor(context: Context, name: String): Uri? = fileOf(context, name)?.let {
        it.parentFile?.mkdirs()
        FileProvider.getUriForFile(context, "${context.packageName}.photos", it)
    }

    fun exists(context: Context, name: String): Boolean = fileOf(context, name)?.exists() == true

    fun delete(context: Context, name: String) {
        fileOf(context, name)?.delete()
    }

    /**
     * Gets a new photo ready: copies it in from [from] (a photo picked in the gallery), or takes the file the camera just wrote, then
     * shrinks it and turns it upright. False if it couldn't be read, and then no file is left.
     */
    suspend fun prepare(context: Context, name: String, from: Uri?): Boolean = onIo {
        val file = fileOf(context, name) ?: return@onIo false
        file.parentFile?.mkdirs()
        val copied = from == null || runCatching {
            context.contentResolver.openInputStream(from)?.use { input -> file.outputStream().use { input.copyTo(it) } } != null
        }.getOrDefault(false)
        if (copied && file.length() > 0) shrink(file)
        (copied && file.length() > 0).also { if (!it) file.delete() }
    }

    /** Keeps only [part] of the photo, saved over it. */
    suspend fun crop(context: Context, name: String, part: CropRect) = onIo {
        val file = fileOf(context, name) ?: return@onIo
        runCatching {
            val whole = BitmapFactory.decodeFile(file.path) ?: return@runCatching
            val box = part.inPixels(whole.width, whole.height)
            val kept = Bitmap.createBitmap(whole, box.left, box.top, box.width, box.height)
            file.outputStream().use { kept.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        }
    }

    /** The lines of text ML Kit reads on the saved photo. */
    suspend fun read(context: Context, name: String): List<String> = fileOf(context, name)?.let { recognize(it) } ?: emptyList()

    /** The saved photo as a picture, or null if it isn't there or can't be decoded. */
    suspend fun image(context: Context, name: String): ImageBitmap? = onIo {
        fileOf(context, name)?.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path)?.asImageBitmap() }
    }

    private suspend fun <T> onIo(work: () -> T): T = withContext(KoinPlatform.getKoin().get<AppDispatchers>().io) { work() }

    /** Scales the photo down and turns it the right way up (the camera stores "which way is up" separately), then saves it over itself. */
    private fun shrink(file: File) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { BitmapFactory.decodeFile(file.path, it) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            val decoded = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return
            val degrees = when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            val scale = MAX_SIDE.toFloat() / max(decoded.width, decoded.height)
            val matrix = Matrix().apply {
                postRotate(degrees)
                if (scale < 1f) postScale(scale, scale)
            }
            val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            file.outputStream().use { result.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        }
    }

    private suspend fun recognize(file: File): List<String> = suspendCancellableCoroutine { continuation ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        // The saved photo is already upright, so only its size changes: a small crop is enlarged, or digits are missed.
        val bitmap = runCatching {
            BitmapFactory.decodeFile(file.path)?.let { saved ->
                val scale = readScale(saved.width, saved.height)
                if (scale > 1) Bitmap.createScaledBitmap(saved, saved.width * scale, saved.height * scale, true) else saved
            }
        }.getOrNull()
        val image = bitmap?.let { InputImage.fromBitmap(it, 0) }
        if (image == null) {
            recognizer.close()
            continuation.resume(emptyList())
            return@suspendCancellableCoroutine
        }
        recognizer.process(image)
            .addOnSuccessListener { text ->
                // ML Kit's boxes are in pixels of the image it read; TextPiece wants fractions of it.
                val width = image.width.toFloat()
                val height = image.height.toFloat()
                val pieces = text.textBlocks.flatMap { it.lines }.map { line ->
                    val box = line.boundingBox
                    if (box == null) TextPiece(line.text, 0f, 0f, 0f, 0f) else TextPiece(line.text, box.left / width, box.top / height, box.right / width, box.bottom / height)
                }
                continuation.resume(readingLines(pieces))
            }
            .addOnFailureListener { continuation.resume(emptyList()) }
            .addOnCompleteListener { recognizer.close() }
    }
}
