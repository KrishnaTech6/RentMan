package com.rentman.sharedui.components

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

/**
 * The lines to look for the reading in. A counter display shows each digit in its own window, so the reader often returns
 * "16", "566", "4" instead of "06566 4": in pieces, with a 0 taken for a 1 and the red tenths digit sometimes in, sometimes not.
 * Every piece's own text is kept. The short digit pieces on the same row (boxes that overlap up and down and are about as
 * tall) are joined left to right, and every run of three to seven of those digits becomes a line of its own ("165664" gives
 * 1656, 6566, 5664, 16566 ...), so the last reading can pick the one that fits. Long printed numbers like a serial number are
 * never cut up like this, so they can't give a number that only looks believable.
 */
internal fun readingLines(pieces: List<TextPiece>): List<String> {
    val fragments = pieces.filter { it.text.trim().matches(FRAGMENT) && it.height > 0f }
    val rows = mutableListOf<MutableList<TextPiece>>()
    for (piece in fragments.sortedBy { it.left }) {
        val row = rows.firstOrNull { row -> row.any { sameRow(it, piece) } }
        if (row != null) row += piece else rows += mutableListOf(piece)
    }
    val runs = rows.filter { it.size > 1 }.flatMap { row ->
        val digits = row.joinToString("") { piece -> piece.text.filter(Char::isDigit) }
        (MIN_RUN..min(MAX_RUN, digits.length)).flatMap { length -> digits.windowed(length) }
    }
    return pieces.map { it.text } + runs.distinct()
}

/** Two pieces are on one row when their boxes share at least half of the shorter one's height, and neither is twice as tall. */
private fun sameRow(a: TextPiece, b: TextPiece): Boolean {
    val overlap = min(a.bottom, b.bottom) - max(a.top, b.top)
    val shorter = min(a.height, b.height)
    return overlap >= shorter / 2 && max(a.height, b.height) <= shorter * 2
}
