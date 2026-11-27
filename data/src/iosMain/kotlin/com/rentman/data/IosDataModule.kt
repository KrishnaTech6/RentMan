package com.rentman.data

import androidx.room3.Room
import com.rentman.data.db.AppDatabase
import com.rentman.data.drive.DefaultsUploadLog
import com.rentman.data.drive.KeychainTokenStore
import com.rentman.data.drive.RefreshTokenAuth
import com.rentman.domain.drive.GoogleClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/** [google] is the app's iPhone key with Google, for Drive backup. */
@OptIn(ExperimentalForeignApi::class)
fun iosDataModule(google: GoogleClient): Module {
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
    val http = HttpClient(Darwin) { expectSuccess = true }
    return dataModule(
        Room.databaseBuilder<AppDatabase>(name = "$path/$DATABASE_NAME"),
        http,
        RefreshTokenAuth(http, KeychainTokenStore(), google),
        DefaultsUploadLog(),
    )
}
