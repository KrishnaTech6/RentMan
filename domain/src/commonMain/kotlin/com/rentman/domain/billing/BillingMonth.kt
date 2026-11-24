package com.rentman.domain.billing

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlin.jvm.JvmInline

/** A calendar month encoded as YYYYMM (e.g. 202611) so it sorts and indexes as a plain integer. */
@JvmInline
value class BillingMonth private constructor(val value: Int) : Comparable<BillingMonth> {
    val year: Int get() = value / 100
    val month: Int get() = value % 100

    fun plusMonths(months: Int): BillingMonth {
        val index = year * 12 + (month - 1) + months
        return of(index / 12, index % 12 + 1)
    }

    fun atStartOfMonth(): LocalDate = LocalDate(year, month, 1)

    fun lengthInDays(): Int = atStartOfMonth().let { it.daysUntil(it.plus(1, DateTimeUnit.MONTH)) }

    /** The due day of this month, or the last day of the month if it has fewer days (31 in November is the 30th). */
    fun dueOn(dueDay: Int): LocalDate = LocalDate(year, month, minOf(dueDay, lengthInDays()))

    override fun compareTo(other: BillingMonth) = value.compareTo(other.value)

    companion object {
        fun of(year: Int, month: Int): BillingMonth {
            require(month in 1..12) { "month must be 1..12 but was $month" }
            return BillingMonth(year * 100 + month)
        }

        fun of(date: LocalDate): BillingMonth = of(date.year, date.month.number)

        /** Rebuilds from a stored YYYYMM value. */
        fun fromValue(value: Int): BillingMonth = of(value / 100, value % 100)
    }
}
