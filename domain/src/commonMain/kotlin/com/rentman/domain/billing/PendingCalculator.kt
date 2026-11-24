package com.rentman.domain.billing

import com.rentman.domain.money.Paise
import kotlinx.datetime.LocalDate

object PendingCalculator {
    /** Pending = all bill totals − all payments. Negative means the renter has paid ahead. Never stored (ADR 0001). */
    fun pending(billTotals: List<Paise>, payments: List<Paise>): Paise =
        BillCalculator.total(billTotals) - BillCalculator.total(payments)

    /**
     * The part of the pending amount that comes from bills already past their due day (a bill is not overdue on its due day itself).
     * Payments settle the oldest bills first, as in the renter's running balance, so an old unpaid bill stays overdue after a newer
     * one is made. Never below zero and never more than the pending amount.
     */
    fun overdue(bills: List<Bill>, payments: List<Paise>, today: LocalDate): Paise {
        val paid = BillCalculator.total(payments)
        val stillOwedOnPastDue = BillCalculator.total(bills.filter { it.dueOn < today }.map(Bill::total)) - paid
        return maxOf(Paise.Zero, minOf(stillOwedOnPastDue, pending(bills.map(Bill::total), payments)))
    }
}
