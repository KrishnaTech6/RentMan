package com.rentman.sharedui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import com.rentman.domain.AppDispatchers
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform
import platform.Foundation.NSURL
import platform.QuickLook.QLPreviewController
import platform.QuickLook.QLPreviewControllerDataSourceProtocol
import platform.QuickLook.QLPreviewItemProtocol
import platform.UIKit.UIImage
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.NSInteger
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue

/** Holds on to what the camera or the photo library calls back into: the picker keeps its delegate only weakly. */
@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
private class PhotoPickerDelegate(private val onPhoto: (String) -> Unit) :
    NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        // The whole photo: the part with the reading is chosen on our own crop screen, as on Android.
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        // Whatever is shown next appears on top of the top screen, so it must wait until the picker has finished closing: shown while the
        // picker is still sliding away, it goes away with it a moment later.
        picker.dismissViewControllerAnimated(true) {
            if (image != null) {
                // Turning the photo upright and shrinking it takes a moment, so it is done away from the screen.
                dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
                    val name = IosMeterPhotoFiles.newName()
                    if (IosMeterPhotoFiles.save(image, name)) dispatch_async(dispatch_get_main_queue()) { onPhoto(name) }
                }
            }
        }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
    }
}

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
@Composable
internal actual fun rememberPhotoSource(onPhoto: (photo: String) -> Unit): PhotoSource {
    val currentOnPhoto by rememberUpdatedState(onPhoto)
    val delegate = remember { PhotoPickerDelegate { currentOnPhoto(it) } }
    fun present(source: UIImagePickerControllerSourceType) {
        val picker = UIImagePickerController()
        picker.sourceType = source
        // Apple's own crop is a square only, which cuts off a wide display and keeps serial numbers in view, so it stays off.
        picker.allowsEditing = false
        picker.delegate = delegate
        topViewController()?.presentViewController(picker, animated = true, completion = null)
    }
    return remember {
        PhotoSource(
            // The simulator has no camera, so it picks from the photo library instead.
            camera = {
                present(
                    if (UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)) {
                        UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                    } else {
                        UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
                    },
                )
            },
            // The photo library picker needs no permission: the app only gets the one photo that is chosen.
            gallery = { present(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary) },
        )
    }
}

@Composable
internal actual fun rememberPhotoFiles(): PhotoFiles = remember {
    val io = KoinPlatform.getKoin().get<AppDispatchers>().io
    PhotoFiles(
        crop = { photo, part -> withContext(io) { IosMeterPhotoFiles.crop(photo, part) } },
        read = { photo -> withContext(io) { IosMeterPhotoFiles.recognize(photo) } },
        discard = IosMeterPhotoFiles::delete,
    )
}

@Composable
internal actual fun rememberPhotoImage(photo: String): ImageBitmap? = produceState<ImageBitmap?>(initialValue = null, photo) {
    value = withContext(KoinPlatform.getKoin().get<AppDispatchers>().io) { IosMeterPhotoFiles.image(photo) }
}.value

/** One photo for Quick Look. A plain NSURL can't be handed over as the item: it is only a Quick Look item through a category, which Kotlin can't see. */
@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
private class PhotoItem(private val url: NSURL) : NSObject(), QLPreviewItemProtocol {
    override fun previewItemURL(): NSURL = url
}

/** Shows one saved photo in Quick Look. The data source has to stay alive while it is shown, so the opener keeps it. */
@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
internal class PreviewSource : NSObject(), QLPreviewControllerDataSourceProtocol {
    private var item: PhotoItem? = null

    override fun numberOfPreviewItemsInPreviewController(controller: QLPreviewController): NSInteger = if (item == null) 0 else 1

    override fun previewController(controller: QLPreviewController, previewItemAtIndex: NSInteger): QLPreviewItemProtocol =
        checkNotNull(item)

    fun show(name: String) {
        val path = IosMeterPhotoFiles.pathOf(name) ?: return
        item = PhotoItem(NSURL.fileURLWithPath(path))
        val preview = QLPreviewController()
        preview.dataSource = this
        topViewController()?.presentViewController(preview, animated = true, completion = null)
    }
}

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
@Composable
internal actual fun rememberPhotoOpener(): PhotoOpener {
    val source = remember { PreviewSource() }
    return remember { PhotoOpener(exists = { IosMeterPhotoFiles.exists(it) }, open = source::show) }
}
