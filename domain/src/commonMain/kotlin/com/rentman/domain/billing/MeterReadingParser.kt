package com.rentman.domain.billing

import kotlin.math.abs

/**
 * Picks the meter reading out of the text a phone read from a photo of the meter. The text also holds serial numbers, "1200
 * imp/kWh", the voltage and the year, so this only ever offers candidates. The landlord always confirms the number.
 */
object MeterReadingParser {
    private const val MIN_DIGITS = 3
    private const val MAX_DIGITS = 7
    private val DISPLAY_WIDTH = 5..6

    /** More than this many units in one billing period is not a believable month (a big month is about 1,000). */
    private const val MAX_JUMP = 2_000

    private val NUMBER = Regex("""\d+(?:[.,]\d+)?""")
    private val ONLY_DIGITS_AND_SEPARATORS = Regex("""[\d\s.,]+""")

    /** What follows a number that is a voltage, a frequency, a current or pulses per kWh. A number before "kWh" is the reading. */
    private val OTHER_UNIT = Regex("""^\s*(?:V|Hz|A|imp|rev|Wh)\b""")

    private class Candidate(val value: Int, val width: Int, val read: Int, val order: Int)

    /**
     * The numbers in [lines] that could be the reading, most likely first, each only once. The decimal part is dropped. A
     * [believable] number comes first, because the meter only moves a few hundred units a month while serial and standard
     * numbers are anywhere. Among those, the one closest to [previous] comes first, and a display five or six digits wide
     * breaks a tie (and decides alone when there is no last reading). A six or seven digit number is also offered without its
     * last digit, which a meter may show as a separate decimal digit. Numbers below [previous] are still offered last: the
     * meter may have been replaced.
     */
    fun candidates(lines: List<String>, previous: Int?): List<Int> {
        val found = lines.flatMap { tokensOf(it) }.flatMapIndexed { order, token -> candidatesOf(token, order) }
        return found.sortedWith(
            compareBy<Candidate>(
                { if (believable(it.value, previous)) 0 else 1 },
                { previous?.let { last -> abs(it.value - last) } ?: 0 },
                { if (it.width in DISPLAY_WIDTH) 0 else 1 },
                { it.read },
                { it.order },
            ),
        ).map { it.value }.distinct()
    }

    /** Whether [value] can follow [previous] (the last reading) in one bill: not below it and not more than [MAX_JUMP] above it. */
    fun believable(value: Int, previous: Int?): Boolean = previous == null || (value >= previous && value - previous <= MAX_JUMP)

    /** The integer part of each number on a line. A line of only digits is one number the camera read in pieces. */
    private fun tokensOf(line: String): List<Pair<String, Boolean>> {
        val joined = line.trim().takeIf { it.matches(ONLY_DIGITS_AND_SEPARATORS) }?.filterNot { it.isWhitespace() }
            ?.takeIf { it.count(Char::isDigit) <= MAX_DIGITS }
        val text = joined ?: line
        return NUMBER.findAll(text)
            .filterNot { OTHER_UNIT.containsMatchIn(text.substring(it.range.last + 1)) }
            .map { match ->
                val parts = match.value.split('.', ',')
                parts.first() to (parts.size > 1)
            }.toList()
    }

    private fun candidatesOf(token: Pair<String, Boolean>, order: Int): List<Candidate> {
        val (digits, hasFraction) = token
        if (digits.length !in MIN_DIGITS..MAX_DIGITS) return emptyList()
        val whole = Candidate(digits.toInt(), digits.length, read = 0, order = order)
        val withoutLast = digits.dropLast(1).takeIf { !hasFraction && digits.length > DISPLAY_WIDTH.first }
            ?.let { Candidate(it.toInt(), digits.length, read = 1, order = order) }
        return listOfNotNull(whole, withoutLast).filter { it.value > 0 }
    }
}

/**
 * What a photo of the meter suggests: the [reading] to fill in, and the [choices] to tap, likeliest first. The reading is only set
 * when the likeliest number is [MeterReadingParser.believable]: a serial number filled in is worse than an empty box.
 */
data class MeterSuggestion(val reading: Int?, val choices: List<Int>)

class SuggestMeterReadingUseCase {
    operator fun invoke(lines: List<String>, previousReading: Int?): MeterSuggestion {
        val choices = MeterReadingParser.candidates(lines, previousReading).take(MAX_CHOICES)
        return MeterSuggestion(choices.firstOrNull()?.takeIf { MeterReadingParser.believable(it, previousReading) }, choices)
    }

    private companion object {
        const val MAX_CHOICES = 3
    }
}
