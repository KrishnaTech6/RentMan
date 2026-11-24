package com.rentman.sharedui.reminders

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rentman.domain.AppDispatchers
import com.rentman.domain.tenancy.MuteRemindersUseCase
import com.rentman.domain.tenancy.RenterId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

/**
 * The "Stop this month" button on a rent reminder. It only hands over to [MuteRemindersUseCase]
 * (ARCHITECTURE.md section 7). The reminder sync then removes the renter's other reminders by itself.
 */
internal class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val renterId = intent.getLongExtra(EXTRA_RENTER_ID, NONE).takeIf { it != NONE } ?: return
        val result = goAsync()
        val koin = KoinPlatform.getKoin()
        CoroutineScope(SupervisorJob() + koin.get<AppDispatchers>().io).launch {
            try {
                koin.get<MuteRemindersUseCase>()(RenterId(renterId), muted = true)
                context.getSystemService(NotificationManager::class.java).cancel(intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0))
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        const val EXTRA_RENTER_ID = "renterId"
        const val EXTRA_NOTIFICATION_ID = "notificationId"
        private const val NONE = -1L
    }
}
