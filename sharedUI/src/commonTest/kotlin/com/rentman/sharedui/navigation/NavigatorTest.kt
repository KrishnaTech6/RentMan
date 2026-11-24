package com.rentman.sharedui.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigatorTest {
    private val stack = mutableListOf<NavKey>(HomeKey)
    private val navigator = Navigator(stack)

    @Test
    fun openPutsTheScreenOnTop() {
        navigator.open(AddRenterKey)

        assertEquals(listOf<NavKey>(HomeKey, AddRenterKey), stack)
    }

    @Test
    fun openingTheScreenAlreadyOnTopDoesNothing() {
        navigator.open(AddRenterKey)
        navigator.open(AddRenterKey)

        assertEquals(listOf<NavKey>(HomeKey, AddRenterKey), stack)
    }

    @Test
    fun backRemovesTheTopScreen() {
        navigator.open(AddRenterKey)
        navigator.open(AddressPickerKey)

        navigator.back()

        assertEquals(listOf<NavKey>(HomeKey, AddRenterKey), stack)
    }

    @Test
    fun backNeverRemovesTheFirstScreen() {
        navigator.back()
        navigator.back()

        assertEquals(listOf<NavKey>(HomeKey), stack)
    }
}
