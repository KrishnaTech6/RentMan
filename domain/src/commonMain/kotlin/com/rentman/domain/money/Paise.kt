package com.rentman.domain.money

import kotlin.jvm.JvmInline

/** Money in paise (₹1 = 100 paise). Never use Double for money. */
@JvmInline
value class Paise(val value: Long) : Comparable<Paise> {
    operator fun plus(other: Paise) = Paise(value + other.value)
    operator fun minus(other: Paise) = Paise(value - other.value)
    operator fun times(factor: Long) = Paise(value * factor)
    override fun compareTo(other: Paise) = value.compareTo(other.value)

    companion object {
        val Zero = Paise(0)
    }
}

val Long.rupees: Paise get() = Paise(this * 100)
