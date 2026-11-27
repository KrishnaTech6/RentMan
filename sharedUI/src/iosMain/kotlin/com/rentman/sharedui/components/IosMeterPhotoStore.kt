package com.rentman.sharedui.components

import com.rentman.domain.AppDispatchers
import com.rentman.domain.drive.MeterPhotoStore
import kotlinx.coroutines.withContext

/** The meter photos on this iPhone, for Drive backup and restore. Files are read and written off the main thread. */
internal class IosMeterPhotoStore(private val dispatchers: AppDispatchers) : MeterPhotoStore {
    override fun exists(name: String): Boolean = IosMeterPhotoFiles.exists(name)
    override suspend fun read(name: String): ByteArray? = withContext(dispatchers.io) { IosMeterPhotoFiles.bytes(name) }
    override suspend fun write(name: String, bytes: ByteArray): Boolean = withContext(dispatchers.io) { IosMeterPhotoFiles.write(name, bytes) }
}
