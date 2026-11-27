package com.rentman.sharedui.di

import com.rentman.data.iosDataModule
import com.rentman.sharedui.components.IosGoogleClient
import com.rentman.sharedui.components.IosMeterPhotoStore
import com.rentman.domain.drive.MeterPhotoStore
import com.rentman.domain.reminders.ReminderScheduler
import com.rentman.domain.reminders.ReminderSync
import com.rentman.sharedui.reminders.IosReminderScheduler
import com.rentman.sharedui.reminders.ReminderNotificationDelegate
import org.koin.core.context.startKoin
import org.koin.dsl.module
import platform.UserNotifications.UNUserNotificationCenter

/** The notification centre keeps its delegate weakly, so this holds on to it. */
private val notificationDelegate = ReminderNotificationDelegate()

/** Called once from the Swift `App.init`. Also starts keeping the phone's reminders in line with the data. */
fun startRentManKoin() {
    val iosReminders = module {
        single<ReminderScheduler> { IosReminderScheduler(get()) }
        single<MeterPhotoStore> { IosMeterPhotoStore(get()) }
    }
    UNUserNotificationCenter.currentNotificationCenter().delegate = notificationDelegate
    startKoin { modules(listOf(iosDataModule(IosGoogleClient), iosReminders) + sharedModules()) }
        .koin.get<ReminderSync>().start()
}
