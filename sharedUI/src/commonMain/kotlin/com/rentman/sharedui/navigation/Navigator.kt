package com.rentman.sharedui.navigation

import androidx.navigation3.runtime.NavKey

/** The one owner of the back stack. Screens never touch it; they emit NavEvents and the app maps them here. */
internal class Navigator(private val backStack: MutableList<NavKey>) {
    fun open(key: NavKey) {
        if (backStack.lastOrNull() != key) backStack.add(key)
    }

    /** Goes back one screen. The first screen can't be removed. */
    fun back() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
}
