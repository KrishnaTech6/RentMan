package com.rentman.data

import android.content.Context
import androidx.room3.Room
import com.rentman.data.db.AppDatabase
import com.rentman.data.drive.PlayServicesAuth
import com.rentman.data.drive.PreferencesUploadLog
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.Module

fun androidDataModule(context: Context): Module {
    val appContext = context.applicationContext
    return dataModule(
        Room.databaseBuilder<AppDatabase>(appContext, appContext.getDatabasePath(DATABASE_NAME).absolutePath),
        HttpClient(OkHttp) { expectSuccess = true },
        // Google matches Android by package name and signing fingerprint to the Android key made in Google Cloud.
        PlayServicesAuth(appContext),
        PreferencesUploadLog(appContext),
    )
}
