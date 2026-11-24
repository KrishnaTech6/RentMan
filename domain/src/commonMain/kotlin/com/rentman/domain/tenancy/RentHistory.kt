package com.rentman.domain.tenancy

import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.Paise

/**
 * What the rent was in a month. A renter's rent changes the moment a raise is accepted, but the raise only applies from its own
 * month, so a bill made (again) for an earlier month has to use the earlier rent.
 */
object RentHistory {
    /**
     * From the month of the latest raise on, the [current] rent: it is the raise's new rent unless the landlord edited the rent by hand
     * since, which records no raise. Before that, the new rent of the latest raise at or before [month], or the old rent of the first
     * raise if [month] is before all of them.
     */
    fun rentFor(month: BillingMonth, current: Paise, changes: List<RentChange>): Paise {
        val ordered = changes.sortedBy { it.effectiveMonth }
        val latest = ordered.lastOrNull() ?: return current
        if (month >= latest.effectiveMonth) return current
        return ordered.lastOrNull { it.effectiveMonth <= month }?.newRent ?: ordered.first().oldRent
    }
}
