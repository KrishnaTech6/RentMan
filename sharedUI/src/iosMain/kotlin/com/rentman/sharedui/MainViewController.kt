package com.rentman.sharedui

import androidx.compose.ui.window.ComposeUIViewController

/** Entry point called from the Swift host in iosApp. */
@Suppress("FunctionName")
fun MainViewController() = ComposeUIViewController { App() }
