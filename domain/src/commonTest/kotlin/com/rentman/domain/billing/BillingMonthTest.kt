package com.rentman.domain.billing

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BillingMonthTest {
    @Test
    fun encodesYearAndMonthSoItSorts() {
        assertEquals(202611, BillingMonth.of(2026, 11).value)
        assertTrue(BillingMonth.of(2026, 12) < BillingMonth.of(2027, 1))
    }

    @Test
    fun comesFromADate() {
        assertEquals(BillingMonth.of(2026, 11), BillingMonth.of(LocalDate(2026, 11, 23)))
    }

    @Test
    fun startsOnTheFirstOfTheMonth() {
        assertEquals(LocalDate(2026, 11, 1), BillingMonth.of(2026, 11).atStartOfMonth())
    }

    @Test
    fun knowsHowManyDaysTheMonthHas() {
        assertEquals(30, BillingMonth.of(2026, 11).lengthInDays())
        assertEquals(31, BillingMonth.of(2026, 12).lengthInDays())
        assertEquals(28, BillingMonth.of(2026, 2).lengthInDays())
        assertEquals(29, BillingMonth.of(2028, 2).lengthInDays())
    }

    @Test
    fun theDueDateIsClampedToTheLastDayOfTheMonth() {
        assertEquals(LocalDate(2026, 11, 5), BillingMonth.of(2026, 11).dueOn(5))
        assertEquals(LocalDate(2026, 11, 30), BillingMonth.of(2026, 11).dueOn(31))
        assertEquals(LocalDate(2027, 2, 28), BillingMonth.of(2027, 2).dueOn(30))
    }

    @Test
    fun rejectsAMonthOutsideOneToTwelve() {
        assertFailsWith<IllegalArgumentException> { BillingMonth.of(2026, 13) }
        assertFailsWith<IllegalArgumentException> { BillingMonth.of(2026, 0) }
    }

    @Test
    fun roundTripsThroughTheStoredValue() {
        val month = BillingMonth.of(2026, 11)
        assertEquals(month, BillingMonth.fromValue(month.value))
        assertFailsWith<IllegalArgumentException> { BillingMonth.fromValue(202613) }
    }

    @Test
    fun plusMonthsCrossesYears() {
        assertEquals(BillingMonth.of(2026, 12), BillingMonth.of(2026, 1).plusMonths(11))
        assertEquals(BillingMonth.of(2027, 5), BillingMonth.of(2026, 6).plusMonths(11))
        assertEquals(BillingMonth.of(2027, 1), BillingMonth.of(2026, 12).plusMonths(1))
    }
}
