package com.rentman.domain.tenancy

import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.Paise

object RentIncreasePolicy {
    const val DEFAULT_PERCENT = 10
    const val DEFAULT_EVERY_MONTHS = 11
    private const val TEN_RUPEES = 1_000L

    /** Rent raised by [percent], rounded to the nearest ₹10 (half up). */
    fun raised(rent: Paise, percent: Int): Paise {
        require(percent in 0..100) { "percent must be 0..100 but was $percent" }
        val raised = rent.value * (100 + percent) / 100
        return Paise((raised + TEN_RUPEES / 2) / TEN_RUPEES * TEN_RUPEES)
    }

    /** The month the next raise is due: [everyMonths] after the agreement start or the last raise. */
    fun nextRaise(from: BillingMonth, everyMonths: Int): BillingMonth = from.plusMonths(everyMonths)

    /** Show the "Raise rent?" card once the raise month has arrived, unless "Not now" hid it until [snoozedUntil]. */
    fun shouldAsk(raiseMonth: BillingMonth, today: BillingMonth, snoozedUntil: BillingMonth?): Boolean =
        today >= raiseMonth && (snoozedUntil == null || today >= snoozedUntil)
}
