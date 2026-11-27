package com.rentman.sharedui.components

import android.content.Context
import com.rentman.domain.drive.MeterPhotoStore

/** The meter photos on this phone, for Drive backup and restore. */
internal class AndroidMeterPhotoStore(private val context: Context) : MeterPhotoStore {
    override fun exists(name: String): Boolean = MeterPhotoFiles.exists(context, name)
    override suspend fun read(name: String): ByteArray? = MeterPhotoFiles.bytes(context, name)
    override suspend fun write(name: String, bytes: ByteArray): Boolean = MeterPhotoFiles.write(context, name, bytes)
}
