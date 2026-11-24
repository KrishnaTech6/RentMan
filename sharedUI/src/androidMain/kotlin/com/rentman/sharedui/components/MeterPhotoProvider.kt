package com.rentman.sharedui.components

import androidx.core.content.FileProvider

/**
 * A second FileProvider, for the meter photos. An app can't declare the same provider class twice, so it has its own class,
 * with its own folders in `meter_photo_paths.xml`.
 */
internal class MeterPhotoProvider : FileProvider()
