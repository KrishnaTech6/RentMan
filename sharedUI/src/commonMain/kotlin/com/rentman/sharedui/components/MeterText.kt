package com.rentman.sharedui.components

import com.rentman.domain.billing.MeterReadingParser
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** One piece of text a phone's reader found on a photo, with its box in fractions of the photo (0 is the left or top edge). */
internal data class TextPiece(val text: String, val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val height: Float get() = bottom - top
}

private const val MIN_RUN = 3
private const val MAX_RUN = 7

/** A piece that is one to three digits and nothing else but spaces and a stray mark: one or two windows of a counter display. */
private val FRAGMENT = Regex("""^[-.']?\s*(\d\s*){1,3}$""")

/** A piece that is one whole display number: four to seven digits and nothing else. */
private val WHOLE_DISPLAY = Regex("""^\d{4,7}$""")

/** A meter found on a photo: the box around its display, to read the number in, and the box around the whole meter, to keep as proof. */
internal data class MeterSpot(val display: CropRect, val body: CropRect)

/**
 * The display box, in digit widths to each side of the digits' middle and digit heights above and below it: the outer digits are
 * often not read, and some room helps. Tested on real photos (MeterTextTest).
 */
private const val DISPLAY_SIDE = 1.25f
private const val DISPLAY_ABOVE = 2.5f
private const val DISPLAY_BELOW = 2.5f

/**
 * The meter's box, measured the same way on AMTL meters side by side: the body with its serial number and maker's name, and no
 * part of the next meter.
 */
private const val BODY_SIDE = 2f
private const val BODY_ABOVE = 5f
private const val BODY_BELOW = 10f

/**
 * The lines to look for the reading in. A counter display shows each digit in its own window, so the reader often returns
 * "16", "566", "4" instead of "06566 4": in pieces, with a 0 taken for a 1 and the red tenths digit sometimes in, sometimes not.
 * Every piece's own text is kept, and every run of three to seven digits of a row of digit pieces ([digitRows]) becomes a line
 * of its own ("165664" gives 1656, 6566, 5664, 16566 ...), so the last reading can pick the one that fits. Long printed
 * numbers like a serial number are never cut up like this, so they can't give a number that only looks believable.
 */
internal fun readingLines(pieces: List<TextPiece>): List<String> =
    pieces.map { it.text } + digitRows(pieces).flatMap { runsOf(digitsOf(it)) }.distinct()

/**
 * The meters on a photo of one or more meters, the one to start with first. A meter is found by its display: a row of digit
 * pieces, or one piece of four to seven digits that fits [lastReading]. The meter with a number that fits [lastReading] (closest
 * first) is this renter's; the rest follow from left to right, top to bottom. Empty when no display is found: the landlord places
 * the box.
 */
internal fun findMeters(pieces: List<TextPiece>, lastReading: Int?): List<MeterSpot> {
    // A row of pieces is cut into runs (a 0 read as a 1 spoils only one digit); one printed number is read only as itself, so a
    // serial number like 706047 can't give a run like 7060 that happens to fit.
    val rows = digitRows(pieces).map { row -> row to runsOf(digitsOf(row)) }
    val wholes = pieces.filter { it.text.trim().matches(WHOLE_DISPLAY) }
        .map { listOf(it) to MeterReadingParser.candidates(listOf(it.text), lastReading).map(Int::toString) }
        .filter { (_, numbers) -> fit(numbers, lastReading) != null }
    return (rows + wholes)
        .sortedWith(
            compareBy(
                { (_, numbers) -> fit(numbers, lastReading) ?: Int.MAX_VALUE },
                { (row, _) -> row.minOf { it.left } },
                { (row, _) -> row.minOf { it.top } },
            ),
        )
        .map { (row, _) -> MeterSpot(display = around(row, DISPLAY_SIDE, DISPLAY_ABOVE, DISPLAY_BELOW), body = around(row, BODY_SIDE, BODY_ABOVE, BODY_BELOW)) }
}

/** How far the closest of [numbers] that can follow [lastReading] is from it, or null if none can. */
private fun fit(numbers: List<String>, lastReading: Int?): Int? {
    if (lastReading == null) return null
    return numbers.mapNotNull { it.toIntOrNull() }
        .filter { MeterReadingParser.believable(it, lastReading) }
        .minOfOrNull { abs(it - lastReading) }
}

/** The short digit pieces on one row, joined: two or more pieces whose boxes overlap up and down and are about as tall. */
private fun digitRows(pieces: List<TextPiece>): List<List<TextPiece>> =
    pieces.filter { it.text.trim().matches(FRAGMENT) && it.height > 0f }
        .sortedBy { it.left }
        .fold(emptyList<List<TextPiece>>()) { rows, piece ->
            val at = rows.indexOfFirst { row -> row.any { sameRow(it, piece) } }
            if (at < 0) rows + listOf(listOf(piece)) else rows.mapIndexed { i, row -> if (i == at) row + piece else row }
        }
        .filter { it.size > 1 }

private fun digitsOf(row: List<TextPiece>) = row.joinToString("") { piece -> piece.text.filter(Char::isDigit) }

private fun runsOf(digits: String) = (MIN_RUN..min(MAX_RUN, digits.length)).flatMap { length -> digits.windowed(length) }

/** Two pieces are on one row when their boxes share at least half of the shorter one's height, and neither is twice as tall. */
private fun sameRow(a: TextPiece, b: TextPiece): Boolean {
    val overlap = min(a.bottom, b.bottom) - max(a.top, b.top)
    val shorter = min(a.height, b.height)
    return overlap >= shorter / 2 && max(a.height, b.height) <= shorter * 2
}

/**
 * A box around the digits found: [side] digit widths to the left and right of their middle, [above] and [below] digit heights
 * up and down from it, never less than half of [CropRect.MIN_SIZE] each way, and cut at the photo's edges.
 */
private fun around(row: List<TextPiece>, side: Float, above: Float, below: Float): CropRect {
    val left = row.minOf { it.left }
    val right = row.maxOf { it.right }
    val top = row.minOf { it.top }
    val bottom = row.maxOf { it.bottom }
    val middleX = (left + right) / 2
    val middleY = (top + bottom) / 2
    val least = CropRect.MIN_SIZE / 2
    return CropRect(
        left = max(0f, middleX - max(side * (right - left), least)),
        top = max(0f, middleY - max(above * (bottom - top), least)),
        right = min(1f, middleX + max(side * (right - left), least)),
        bottom = min(1f, middleY + max(below * (bottom - top), least)),
    )
}
