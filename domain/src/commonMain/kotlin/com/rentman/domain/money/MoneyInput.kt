package com.rentman.domain.money

/** Turns what a person typed into money. Returns null for anything that isn't a valid positive amount. */
object MoneyInput {
    private const val MAX_RUPEE_DIGITS = 9
    /** Up to ₹99.99 a unit. A real tariff is far lower, and this catches "950" typed for "9.50". */
    private const val MAX_RATE_WHOLE_DIGITS = 2
    private const val MAX_RATE_FRACTION_DIGITS = 2

    /** Whole rupees, e.g. "7,500" or "₹ 24,000". */
    fun rupees(text: String): Paise? {
        val digits = clean(text)
        if (digits.isEmpty() || digits.length > MAX_RUPEE_DIGITS || !digits.all(::isAsciiDigit)) return null
        return digits.toLong().takeIf { it > 0 }?.rupees
    }

    /** A per-unit rate with up to two decimals, e.g. "8", "8.5", "₹9.25". */
    fun rate(text: String): Paise? = decimal(text, MAX_RATE_WHOLE_DIGITS)

    /** An amount paid, with up to two decimals because a bill can have paise, e.g. "3,380" or "3,380.50". */
    fun amount(text: String): Paise? = decimal(text, MAX_RUPEE_DIGITS)

    /** The text that [amount] reads back as the same money. */
    fun text(paise: Paise): String {
        val whole = paise.value / 100
        val fraction = paise.value % 100
        return if (fraction == 0L) "$whole" else "$whole.${fraction.toString().padStart(2, '0')}"
    }

    private fun decimal(text: String, maxWholeDigits: Int): Paise? {
        val cleaned = clean(text)
        val whole = cleaned.substringBefore('.')
        val fraction = cleaned.substringAfter('.', missingDelimiterValue = "")
        val hasPoint = '.' in cleaned
        val shapeOk = whole.isNotEmpty() && whole.length <= maxWholeDigits && whole.all(::isAsciiDigit) &&
            fraction.length <= MAX_RATE_FRACTION_DIGITS && fraction.all(::isAsciiDigit) &&
            (!hasPoint || fraction.isNotEmpty())
        if (!shapeOk) return null
        val paise = whole.toLong() * 100 + fraction.padEnd(MAX_RATE_FRACTION_DIGITS, '0').toLong()
        return Paise(paise).takeIf { it > Paise.Zero }
    }

    private fun clean(text: String) = text.filterNot { it == '₹' || it == ',' || it.isWhitespace() }

    private fun isAsciiDigit(char: Char) = char in '0'..'9'
}
