package com.rentman.sharedui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

private const val PREFERENCES = "ui"
private const val ASKED = "asked_notification_permission"

/** Before Android 13 nothing needs to be asked. */
private fun Context.notificationsAllowed(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.openNotificationSettings() {
    startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

@Composable
internal actual fun rememberNotificationPermission(): NotificationPermission {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(context.notificationsAllowed()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { allowed = context.notificationsAllowed() }
    // Only a tap in Settings may send the person on to the phone's settings, never the automatic ask at launch.
    var openSettingsIfRefused by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = granted
        // Whatever the answer, the person has now been asked, so the app never asks by itself again. Written here, not when the dialog
        // opens: if the phone ends the app while the dialog is up, nothing was answered and the next launch asks again.
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putBoolean(ASKED, true).apply()
        // After the person has refused twice, Android answers "no" without a dialog and only the phone's settings can change it.
        val canAskAgain = context.findActivity()?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true
        if (!granted && !canAskAgain && openSettingsIfRefused) context.openNotificationSettings()
    }
    return NotificationPermission(
        allowed = allowed,
        request = {
            openSettingsIfRefused = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        askOnce = {
            // Android can't say "never asked", so a flag on the phone remembers that the person has answered once.
            val asked = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getBoolean(ASKED, false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !context.notificationsAllowed() && !asked) {
                openSettingsIfRefused = false
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
    )
}
