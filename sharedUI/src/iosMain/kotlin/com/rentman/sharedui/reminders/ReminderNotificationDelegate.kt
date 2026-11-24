package com.rentman.sharedui.reminders

import com.rentman.domain.AppDispatchers
import com.rentman.domain.tenancy.MuteRemindersUseCase
import com.rentman.domain.tenancy.RenterId
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

/**
 * Handles the "Stop this month" button on a rent reminder and shows reminders while the app is open. It only hands over
 * to [MuteRemindersUseCase] (ARCHITECTURE.md section 7). The reminder sync then removes the renter's other reminders.
 */
@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
internal class ReminderNotificationDelegate : NSObject(), UNUserNotificationCenterDelegateProtocol {

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
    ) {
        withCompletionHandler(UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionSound)
    }

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit,
    ) {
        val renterId = (didReceiveNotificationResponse.notification.request.content.userInfo[IosReminderScheduler.RENTER_ID_KEY] as? String)
            ?.toLongOrNull()
        if (didReceiveNotificationResponse.actionIdentifier != IosReminderScheduler.STOP_ACTION || renterId == null) {
            withCompletionHandler()
            return
        }
        val koin = KoinPlatform.getKoin()
        CoroutineScope(SupervisorJob() + koin.get<AppDispatchers>().io).launch {
            try {
                koin.get<MuteRemindersUseCase>()(RenterId(renterId), muted = true)
            } finally {
                withCompletionHandler()
            }
        }
    }
}
