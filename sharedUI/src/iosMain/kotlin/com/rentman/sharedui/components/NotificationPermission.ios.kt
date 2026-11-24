package com.rentman.sharedui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.rentman.domain.reminders.ReminderSync
import org.koin.mp.KoinPlatform
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
internal actual fun rememberNotificationPermission(): NotificationPermission {
    var allowed by remember { mutableStateOf<Boolean?>(null) }
    val center = UNUserNotificationCenter.currentNotificationCenter()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            val granted = status == UNAuthorizationStatusAuthorized || status == UNAuthorizationStatusProvisional
            // The phone answers on another thread, and the screen state is changed on the main one.
            dispatch_async(dispatch_get_main_queue()) { allowed = granted }
        }
    }
    // iOS keeps "never asked" itself (NotDetermined) and asks only once. After that, only the phone's settings can change the answer.
    fun ask(openSettingsIfAsked: Boolean) {
        center.getNotificationSettingsWithCompletionHandler { settings ->
            if (settings?.authorizationStatus == UNAuthorizationStatusNotDetermined) {
                center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, _ ->
                    dispatch_async(dispatch_get_main_queue()) { allowed = granted }
                    // iOS dropped every reminder requested before this, so ask for them again now.
                    if (granted) KoinPlatform.getKoin().get<ReminderSync>().resync()
                }
            } else if (openSettingsIfAsked) {
                dispatch_async(dispatch_get_main_queue()) {
                    NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let {
                        UIApplication.sharedApplication.openURL(it, options = emptyMap<Any?, Any?>(), completionHandler = null)
                    }
                }
            }
        }
    }
    return NotificationPermission(allowed, request = { ask(openSettingsIfAsked = true) }, askOnce = { ask(openSettingsIfAsked = false) })
}
