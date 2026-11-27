package com.rentman.sharedui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.rentman.domain.billing.MeterPhotoName
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.useContents
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGContextSetInterpolationQuality
import platform.CoreGraphics.CGImageCreateWithImageInRect
import platform.CoreGraphics.kCGInterpolationHigh
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGRectMake
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSUUID
import platform.Foundation.writeToFile
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate
import kotlin.math.max

/**
 * The proof photos of meter readings on iOS: a folder in the app's Documents, one JPEG per photo, named by a random id.
 * shortcut: a photo that is retaken, removed or never saved stays in the folder, so storage grows a little with each retake. Sweep the
 * names no bill uses when it matters.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal object IosMeterPhotoFiles {
    private const val FOLDER = "meter_photos"

    /** Big enough to read a meter and to be proof, small enough that a year of photos is not a gigabyte. */
    private const val MAX_SIDE = 1_600.0
    private const val JPEG_QUALITY = 0.8

    fun newName(): String = NSUUID().UUIDString.replace("-", "").take(24).lowercase() + ".jpg"

    private fun folder(): String? =
        (NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).firstOrNull() as? NSURL)?.path?.let { "$it/$FOLDER" }

    /** Only a plain file name is ever joined to the folder, so a name from a backup file can't point anywhere else. */
    fun pathOf(name: String): String? = name.takeIf(MeterPhotoName::isValid)?.let { file -> folder()?.let { "$it/$file" } }

    fun exists(name: String): Boolean = pathOf(name)?.let { NSFileManager.defaultManager.fileExistsAtPath(it) } == true

    /** Turns the photo the right way up, shrinks it and saves it. The text on it is read from what was saved. */
    fun save(image: UIImage, name: String): Boolean {
        val path = pathOf(name) ?: return false
        NSFileManager.defaultManager.createDirectoryAtPath(path.substringBeforeLast('/'), withIntermediateDirectories = true, attributes = null, error = null)
        val (width, height) = image.size.useContents { width to height }
        val scale = MAX_SIDE / max(width, height)
        val target = if (scale < 1.0) CGSizeMake(width * scale, height * scale) else CGSizeMake(width, height)
        // Drawing a UIImage honours which way is up, so what comes out is upright.
        UIGraphicsBeginImageContextWithOptions(target, true, 1.0)
        image.drawInRect(CGRectMake(0.0, 0.0, target.useContents { this.width }, target.useContents { this.height }))
        val upright = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        val data: NSData = UIImageJPEGRepresentation(upright ?: image, JPEG_QUALITY) ?: return false
        return data.writeToFile(path, atomically = true)
    }

    /** Keeps only [part] of the saved photo, saved over it. The photo was saved upright, so its pixels are what the person saw. */
    fun crop(name: String, part: CropRect) {
        val path = pathOf(name) ?: return
        val whole = UIImage.imageWithContentsOfFile(path)?.CGImage ?: return
        val box = part.inPixels(CGImageGetWidth(whole).toInt(), CGImageGetHeight(whole).toInt())
        val kept = CGImageCreateWithImageInRect(whole, CGRectMake(box.left.toDouble(), box.top.toDouble(), box.width.toDouble(), box.height.toDouble())) ?: return
        UIImageJPEGRepresentation(UIImage.imageWithCGImage(kept), JPEG_QUALITY)?.writeToFile(path, atomically = true)
        CGImageRelease(kept)
    }

    fun delete(name: String) {
        pathOf(name)?.let { NSFileManager.defaultManager.removeItemAtPath(it, error = null) }
    }

    /** The saved photo as a picture, or null if it isn't there or can't be decoded. */
    fun image(name: String): ImageBitmap? {
        val data = pathOf(name)?.let { NSData.dataWithContentsOfFile(it) } ?: return null
        val bytes = ByteArray(data.length.toInt())
        bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
        return runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
    }

    /**
     * The pieces of text Apple's Vision finds in [part] of the saved photo, with where they are in that part, or none if it can't
     * be read. Only the part is read; the file is not changed.
     */
    fun recognize(name: String, part: CropRect): List<TextPiece> {
        val path = pathOf(name) ?: return emptyList()
        val whole = UIImage.imageWithContentsOfFile(path)?.CGImage ?: return emptyList()
        // The photo was saved upright, so its pixels are what the person saw when placing the box.
        val box = part.inPixels(CGImageGetWidth(whole).toInt(), CGImageGetHeight(whole).toInt())
        val cut = CGImageCreateWithImageInRect(whole, CGRectMake(box.left.toDouble(), box.top.toDouble(), box.width.toDouble(), box.height.toDouble()))
            ?: return emptyList()
        val saved = UIImage.imageWithCGImage(cut)
        CGImageRelease(cut) // the UIImage holds on to it
        // A small part is enlarged first, or Vision misses digits.
        val (width, height) = saved.size.useContents { width to height }
        val scale = readScale(width.toInt(), height.toInt()).toDouble()
        val image = if (scale > 1.0) {
            UIGraphicsBeginImageContextWithOptions(CGSizeMake(width * scale, height * scale), true, 1.0)
            // Smooth enlarging, as in the tests on real photos (MeterTextTest): blocky digits are read worse.
            CGContextSetInterpolationQuality(UIGraphicsGetCurrentContext(), kCGInterpolationHigh)
            saved.drawInRect(CGRectMake(0.0, 0.0, width * scale, height * scale))
            UIGraphicsGetImageFromCurrentImageContext().also { UIGraphicsEndImageContext() }
        } else {
            saved
        }
        val cgImage = (image ?: saved).CGImage ?: return emptyList()
        val request = VNRecognizeTextRequest().apply {
            recognitionLevel = VNRequestTextRecognitionLevelAccurate
            usesLanguageCorrection = false // numbers, not words
        }
        VNImageRequestHandler(cgImage, emptyMap<Any?, Any?>()).performRequests(listOf(request), null)
        return request.results.orEmpty().mapNotNull { result ->
            val observation = result as? VNRecognizedTextObservation ?: return@mapNotNull null
            val text = (observation.topCandidates(1u).firstOrNull() as? VNRecognizedText)?.string ?: return@mapNotNull null
            // Vision's box is in fractions with its origin at the bottom left; TextPiece counts from the top.
            observation.boundingBox.useContents { TextPiece(text, origin.x.toFloat(), (1 - origin.y - size.height).toFloat(), (origin.x + size.width).toFloat(), (1 - origin.y).toFloat()) }
        }
    }
}
