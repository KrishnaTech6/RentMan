package com.rentman.sharedui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class TimeTextTest {
    @Test
    fun writesTheTwelveHourDigits() {
        assertEquals("9:00", clockDigits(540))
        assertEquals("9:30", clockDigits(570))
        assertEquals("1:05", clockDigits(13 * 60 + 5))
        assertEquals("11:59", clockDigits(23 * 60 + 59))
    }

    @Test
    fun midnightAndNoonAreTwelve() {
        assertEquals("12:00", clockDigits(0))
        assertEquals("12:30", clockDigits(30))
        assertEquals("12:00", clockDigits(12 * 60))
    }

    @Test
    fun noonStartsTheAfternoon() {
        assertEquals(false, isAfternoon(0))
        assertEquals(false, isAfternoon(12 * 60 - 1))
        assertEquals(true, isAfternoon(12 * 60))
        assertEquals(true, isAfternoon(23 * 60 + 59))
    }
}
