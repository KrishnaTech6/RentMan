package com.rentman.sharedui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/** Whether the app may show notifications, and how to ask. UI-only state: it belongs to the phone, not to our data. */
internal class NotificationPermission(
    /** Null until the phone has answered (iOS answers a moment later), so nothing is shown that might be wrong. */
    val allowed: Boolean?,
    /** The Settings row: asks, or, once the phone won't ask any more, opens the phone's own settings for the app. */
    val request: () -> Unit,
    /** For the first launch: asks only if the person was never asked, and never sends them to the phone's settings. */
    val askOnce: () -> Unit,
)

/** Asks for permission the first time the app is opened. If it is refused, it is not asked again: Settings can still turn it on. */
@Composable
internal fun AskForNotificationsOnce() {
    val permission = rememberNotificationPermission()
    LaunchedEffect(Unit) { permission.askOnce() }
}

/** Checks again each time the screen comes back, because the person may change it in the phone's settings. */
@Composable
internal expect fun rememberNotificationPermission(): NotificationPermission
