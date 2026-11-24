package com.rentman.sharedui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class StringResourcesTest {
    private val strings = File("src/commonMain/composeResources/values/strings.xml").readText()

    @Test
    fun noStringHasABackslash() {
        // Compose resources show a backslash as it is: "can\'t" would appear with the backslash on screen.
        val bad = strings.lines().filter { it.contains("<string") && it.contains('\\') }

        assertEquals(emptyList(), bad)
    }

    @Test
    fun theFileIsFoundAndHasStrings() {
        assertEquals(true, strings.contains("<string name=\"retry\">"))
    }
}
