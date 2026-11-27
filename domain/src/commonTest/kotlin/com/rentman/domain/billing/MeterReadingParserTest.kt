package com.rentman.domain.billing

import kotlin.test.Test
import kotlin.test.assertEquals

class MeterReadingParserTest {
    private fun candidates(vararg lines: String, previous: Int? = 1240) = MeterReadingParser.candidates(lines.toList(), previous)

    private fun best(vararg lines: String, previous: Int? = 1240) = candidates(*lines, previous = previous).firstOrNull()

    @Test
    fun aDisplayWithLeadingZerosIsRead() {
        assertEquals(1325, best("kWh", "01325"))
        assertEquals(1325, best("001325")) // 6 digits, no decimal digit shown
    }

    @Test
    fun theDecimalPartIsDroppedWhateverThePoint() {
        assertEquals(1325, best("01325.4 kWh"))
        assertEquals(1325, best("01325,4"))
    }

    @Test
    fun digitsThatTheCameraReadSeparatelyAreJoined() {
        assertEquals(1325, best("0 1 3 2 5"))
        assertEquals(1325, best("0  1  3  2  5  ", "kWh"))
    }

    @Test
    fun aSerialNumberOfEightDigitsOrMoreIsNeverTheReading() {
        assertEquals(1325, best("No. 20458913", "01325"))
        assertEquals(null, best("No. 20458913"))
    }

    @Test
    fun aNumberWithAUnitThatIsNotEnergyIsNeverTheReading() {
        // Voltage, frequency, current and pulses per kWh are printed on every meter.
        listOf("230 V", "230V", "415 V", "50 Hz", "10(60) A", "5-30 A", "1200 imp/kWh", "3200 rev/kWh", "100 Wh").forEach {
            assertEquals(emptyList(), candidates(it), it)
        }
    }

    @Test
    fun aNumberFollowedByKwhIsTheReading() {
        assertEquals(1325, best("1325 kWh"))
        assertEquals(1325, best("01325.4kWh"))
    }

    @Test
    fun aJumpOfMoreThanTwoThousandUnitsIsNotBelievable() {
        // 03500 is five digits wide like a display, but 2,260 units in one month is not a believable bill.
        assertEquals(1500, best("03500", "1500", previous = 1240))
    }

    @Test
    fun theMetersOtherPrintedNumbersAreNotTheReading() {
        // 1200 imp/kWh is closer to the last reading than 1325 is, but the display has five digits.
        assertEquals(1325, best("1200 imp/kWh", "230 V", "5-30 A", "Cl 1.0", "50 Hz", "2021", "01325", previous = 1190))
    }

    @Test
    fun whenTheLastDigitIsTheDecimalOneBothReadingsAreConsideredAndTheLastBillDecides() {
        // "013254" is 1325.4 on a meter whose last digit is red, or 13254 on one without.
        assertEquals(1325, best("013254", previous = 1240))
        assertEquals(13254, best("013254", previous = 13_000))
        assertEquals(listOf(1325, 13254), candidates("013254", previous = 1240).sorted())
    }

    @Test
    fun aNumberBelowTheLastReadingOrFarAboveItIsLessLikelyThanOneInRange() {
        assertEquals(1325, best("00042", "01325")) // 42 is lower than the last reading
        assertEquals(1325, best("09999", "01325")) // 9999 is 8,759 units more in a month
    }

    @Test
    fun ofTwoPlausibleNumbersTheNearerToTheLastReadingComesFirst() {
        assertEquals(listOf(1290, 1325), candidates("01325", "01290"))
    }

    @Test
    fun aReadingBelowTheLastOneIsStillOfferedBecauseTheMeterMayHaveBeenReplaced() {
        assertEquals(listOf(120), candidates("00120"))
    }

    @Test
    fun numbersAreCandidatesOnlyWhenTheyHaveThreeToSevenDigits() {
        assertEquals(emptyList(), candidates("50 Hz", "Cl 1.0", "5-30 A", ""))
        assertEquals(listOf(123), candidates("123", previous = null))
        assertEquals(listOf(1234567), candidates("1234567", previous = null).take(1))
    }

    @Test
    fun withoutALastReadingThePrintedWidthStillDecides() {
        assertEquals(1325, best("1200 imp/kWh", "01325", previous = null))
    }

    @Test
    fun nothingOrNoDigitsGivesNothing() {
        assertEquals(emptyList(), candidates())
        assertEquals(emptyList(), candidates("kWh", "Energy meter", "  "))
    }

    @Test
    fun theSameNumberTwiceIsOneCandidate() {
        assertEquals(listOf(1325), candidates("01325", "kWh 01325"))
    }

    @Test
    fun zeroIsNotAReading() {
        assertEquals(emptyList(), candidates("00000", "000"))
    }
}
