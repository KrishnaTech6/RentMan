package com.rentman.data

import androidx.room3.Room
import com.rentman.data.db.AppDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun iosDataModule(): Module {
    val directory: NSURL = checkNotNull(
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        ),
    ) { "Application Support directory is unavailable" }
    val path = checkNotNull(directory.path) { "Application Support path is unavailable" }
    return dataModule(Room.databaseBuilder<AppDatabase>(name = "$path/$DATABASE_NAME"))
}
