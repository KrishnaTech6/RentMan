package com.rentman.sharedui.components

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class PhotoFilesTest {
    private fun files(read: suspend (String) -> List<String>) = PhotoFiles(crop = { _, _ -> }, read = read, discard = {})

    @Test
    fun readingThatFinishesGivesItsLines() = runTest {
        assertEquals(listOf("01325", "kWh"), files { listOf("01325", "kWh") }.readWithin("a.jpg"))
    }

    @Test
    fun readingThatNeverFinishesGivesNothingAfterTheTimeLimitInsteadOfWaitingForever() = runTest {
        val lines = files { awaitCancellation() }.readWithin("a.jpg", limit = 15.seconds)

        assertEquals(emptyList(), lines)
        assertEquals(15_000L, currentTime)
    }
}
