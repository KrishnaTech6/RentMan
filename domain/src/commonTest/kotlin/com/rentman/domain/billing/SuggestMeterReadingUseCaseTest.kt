package com.rentman.domain.billing

import kotlin.test.Test
import kotlin.test.assertEquals

class SuggestMeterReadingUseCaseTest {
    private val suggest = SuggestMeterReadingUseCase()

    @Test
    fun fillsInTheDisplayAndOffersTheOtherNumbersAfterIt() {
        // The display fits the last reading. Neither part of the serial number does (one is below it, one too far above),
        // so they come after it, the closer one first.
        assertEquals(MeterSuggestion(1325, listOf(1325, 1234, 4567)), suggest(listOf("SR 1234 4567", "01325"), previousReading = 1240))
    }

    @Test
    fun offersAtMostThree() {
        assertEquals(3, suggest(listOf("1300", "1400", "1500", "1600", "01325"), previousReading = 1240).choices.size)
    }

    @Test
    fun whenNoNumberFitsTheLastReadingNothingIsFilledInButTheNumbersCanStillBeTapped() {
        // A loose crop of a whole meter: the display came out in pieces, only label and serial numbers are left.
        val suggestion = suggest(listOf("IS : 13779", "6", "5", "66", "4", "8815388", "Sr No 706047"), previousReading = 6504)
        assertEquals(null, suggestion.reading)
        assertEquals(listOf(13779, 70604, 706047), suggestion.choices)
    }

    @Test
    fun nothingThatLooksLikeAReadingGivesNothing() {
        assertEquals(MeterSuggestion(null, emptyList()), suggest(listOf("Energy meter", "230 V"), previousReading = 1240))
    }
}
