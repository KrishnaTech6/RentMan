package com.rentman.data

import android.content.Context
import androidx.room3.Room
import com.rentman.data.db.AppDatabase
import org.koin.core.module.Module

fun androidDataModule(context: Context): Module {
    val appContext = context.applicationContext
    return dataModule(
        Room.databaseBuilder<AppDatabase>(appContext, appContext.getDatabasePath(DATABASE_NAME).absolutePath),
    )
}
