package com.rentman.data

import android.content.Context
import androidx.room3.Room
import com.rentman.data.db.AppDatabase
import com.rentman.data.drive.TokenStore
import com.rentman.domain.drive.GoogleClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.Module

fun androidDataModule(context: Context): Module {
    val appContext = context.applicationContext
    return dataModule(
        Room.databaseBuilder<AppDatabase>(appContext, appContext.getDatabasePath(DATABASE_NAME).absolutePath),
        HttpClient(OkHttp) { expectSuccess = true },
        NoSignInYet,
        // Not used yet: Android signs in by its package name and fingerprint, matched to this key in Google Cloud.
        GoogleClient("896399803306-8k2labbkje32535j65l6bkb5vdf9ogl6.apps.googleusercontent.com"),
    )
}

/** shortcut: Drive backup isn't on Android yet, so it is never connected; replace with Play services sign-in when it is. */
private object NoSignInYet : TokenStore {
    override fun read(): String? = null
    override fun write(refreshToken: String) = Unit
    override fun clear() = Unit
}
