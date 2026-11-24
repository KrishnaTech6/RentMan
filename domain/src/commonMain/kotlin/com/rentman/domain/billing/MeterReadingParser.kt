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

    /** More than this many units in one billing period is not a believable month. */
    private const val MAX_JUMP = 5_000

    private val NUMBER = Regex("""\d+(?:[.,]\d+)?""")
    private val ONLY_DIGITS_AND_SEPARATORS = Regex("""[\d\s.,]+""")

    /** What follows a number that is a voltage, a frequency, a current or pulses per kWh. A number before "kWh" is the reading. */
    private val OTHER_UNIT = Regex("""^\s*(?:V|Hz|A|imp|rev|Wh)\b""")

    private class Candidate(val value: Int, val width: Int, val read: Int, val order: Int)

    /**
     * The numbers in [lines] that could be the reading, most likely first, each only once. The decimal part is dropped. A
     * display five or six digits wide is taken over other numbers, and so is a number that is not below [previous] (the last
     * reading) and not far above it. A six or seven digit number is also offered without its last digit, which a meter may
     * show as a separate decimal digit.
     */
    fun candidates(lines: List<String>, previous: Int?): List<Int> {
        val found = lines.flatMap { tokensOf(it) }.flatMapIndexed { order, token -> candidatesOf(token, order) }
        fun inRange(value: Int) = previous == null || (value >= previous && value - previous <= MAX_JUMP)
        return found.sortedWith(
            compareBy<Candidate>(
                { if (it.width in DISPLAY_WIDTH) 0 else 1 },
                { if (inRange(it.value)) 0 else 1 },
                { previous?.let { last -> abs(it.value - last) } ?: 0 },
                { it.read },
                { it.order },
            ),
        ).map { it.value }.distinct()
    }

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

/** Suggests the reading from what was read off a photo of the meter, or null if nothing in it looks like one. */
class SuggestMeterReadingUseCase {
    operator fun invoke(lines: List<String>, previousReading: Int?): Int? =
        MeterReadingParser.candidates(lines, previousReading).firstOrNull()
}
