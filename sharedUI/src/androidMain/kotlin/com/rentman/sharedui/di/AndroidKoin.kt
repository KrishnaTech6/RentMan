package com.rentman.sharedui.di

import android.content.Context
import com.rentman.data.androidDataModule
import com.rentman.domain.drive.MeterPhotoStore
import com.rentman.sharedui.components.AndroidMeterPhotoStore
import com.rentman.sharedui.drive.DriveBackupWorker
import com.rentman.domain.reminders.ReminderScheduler
import com.rentman.domain.reminders.ReminderSync
import com.rentman.sharedui.reminders.AndroidReminderScheduler
import org.koin.core.context.startKoin
import org.koin.dsl.module

/** Called once from `Application.onCreate`. Also starts keeping the phone's reminders in line with the data, and the daily Drive backup. */
fun startRentManKoin(context: Context) {
    val androidReminders = module {
        single<ReminderScheduler> { AndroidReminderScheduler(context, get()) }
        single<MeterPhotoStore> { AndroidMeterPhotoStore(context.applicationContext) }
    }
    startKoin { modules(listOf(androidDataModule(context), androidReminders) + sharedModules()) }
        .koin.get<ReminderSync>().start()
    DriveBackupWorker.schedule(context)
}
