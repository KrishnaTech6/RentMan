package com.rentman.sharedui.reminders

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.rentman.domain.reminders.Reminder
import com.rentman.sharedui.R
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.reminder_channel_name
import com.rentman.sharedui.resources.reminder_stop_action
import org.jetbrains.compose.resources.getString

/** Shows a reminder as an Android notification. Used by [ReminderWorker]. */
internal object ReminderNotifications {
    private const val CHANNEL_ID = "reminders"

    suspend fun show(context: Context, notificationId: Int, reminder: Reminder) {
        val manager = context.getSystemService(NotificationManager::class.java)
        // Without permission there is nothing to show. The Settings screen asks for it.
        if (!manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(Res.string.reminder_channel_name), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val text = reminder.text()
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.let { PendingIntent.getActivity(context, notificationId, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT) }
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(text.title)
            .setContentText(text.body)
            .setContentIntent(open)
            .setAutoCancel(true)
            .apply {
                if (reminder is Reminder.RentPending) {
                    val stop = Intent(context, ReminderActionReceiver::class.java)
                        .putExtra(ReminderActionReceiver.EXTRA_RENTER_ID, reminder.renterId.value)
                        .putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                    val stopIntent = PendingIntent.getBroadcast(
                        context, notificationId, stop, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                    addAction(Notification.Action.Builder(null, getString(Res.string.reminder_stop_action), stopIntent).build())
                }
            }
            .build()
        manager.notify(notificationId, notification)
    }
}
