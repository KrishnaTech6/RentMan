package com.rentman.domain.billing

import com.rentman.domain.money.Paise
import com.rentman.domain.payments.Payment
import com.rentman.domain.tenancy.RenterSummary
import kotlinx.datetime.LocalDate

/**
 * How one calendar month is going, for Home's top card and the Months screens. Every figure belongs to [month], so they add up:
 * [billed] is [collected] + [late] + [notDue], and [expected] adds the rent of the renters with no bill yet.
 * A renter's payments settle their oldest bill first (see [RenterMonth]), so what a renter still owes on older bills is shown
 * apart in [earlier] and never mixed into this month.
 * [notBilledRent] is one month's rent for each, with no electricity because that isn't known before the reading.
 * Renters who have left still count for the bills they were given and the payments they made, so a month's income doesn't
 * shrink when someone moves out; they are never "not billed".
 * [cashIn] is a different number from [collected]: every payment dated in this month, whichever bill it went to.
 */
data class MonthSummary(
    val month: BillingMonth,
    val collected: Paise,
    /** What is left of this month's bills, from bills past their due day. */
    val late: Paise,
    /** What is left of this month's bills, from bills not yet due. */
    val notDue: Paise,
    val notBilled: Int,
    val notBilledRent: Paise,
    /** Still owed on bills of earlier months. */
    val earlier: Paise,
    val cashIn: Paise = Paise.Zero,
) {
    val billed: Paise get() = collected + late + notDue
    val expected: Paise get() = billed + notBilledRent

    /** How much of this month's bills has been settled, from 0 to 1. */
    val collectedShare: Float get() = if (billed > Paise.Zero) collected.value.toFloat() / billed.value else 0f

    companion object {
        fun of(month: BillingMonth, today: LocalDate, renters: List<RenterSummary>, bills: List<Bill>, payments: List<Payment>): MonthSummary {
            val rows = renters.mapNotNull { RenterMonth.of(it, month, today, bills, payments) }
            val unbilled = rows.filter { !it.hasBill }
            return MonthSummary(
                month = month,
                collected = BillCalculator.total(rows.map { it.collected }),
                late = BillCalculator.total(rows.map { it.late }),
                notDue = BillCalculator.total(rows.map { it.notDue }),
                notBilled = unbilled.size,
                notBilledRent = BillCalculator.total(unbilled.map { it.summary.renter.monthlyRent }),
                earlier = BillCalculator.total(renters.map { RenterMonth.earlierOwed(it, month, bills, payments) }),
                cashIn = BillCalculator.total(payments.filter { BillingMonth.of(it.paidOn) == month }.map(Payment::amount)),
            )
        }
    }
}
