package com.rentman.sharedui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rentman.sharedui.components.AskForNotificationsOnce
import com.rentman.sharedui.navigation.AppNavigation
import com.rentman.sharedui.theme.RentManColors

/** Root composable hosted by both platform entry points. Keeps content clear of the status bar, notch and gesture bar. */
@Composable
fun App() {
    // Background first, so it also fills the area behind the status and gesture bars.
    Box(Modifier.fillMaxSize().background(RentManColors.Background).safeDrawingPadding()) {
        AskForNotificationsOnce()
        AppNavigation()
    }
}
