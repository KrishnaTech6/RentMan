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
    fun aSmallCropIsEnlargedBeforeReadingButNeverMoreThanFiveTimes() {
        assertEquals(5, readScale(435, 100)) // a strip around the number window
        assertEquals(5, readScale(213, 94)) // a tiny one: 10x would be needed, 5x is the most
        assertEquals(3, readScale(526, 718))
        assertEquals(2, readScale(1600, 1200)) // a whole photo, as saved
        assertEquals(1, readScale(2400, 1800)) // already big enough
    }

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
