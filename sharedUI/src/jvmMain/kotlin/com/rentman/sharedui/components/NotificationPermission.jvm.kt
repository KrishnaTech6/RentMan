package com.rentman.sharedui.components

import androidx.compose.runtime.Composable

/** The JVM target only runs tests: there are no notifications to allow. */
@Composable
internal actual fun rememberNotificationPermission(): NotificationPermission = NotificationPermission(allowed = true, request = {}, askOnce = {})
