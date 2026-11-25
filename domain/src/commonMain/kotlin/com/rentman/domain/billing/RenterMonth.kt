package com.rentman.domain.billing

import com.rentman.domain.money.Paise
import com.rentman.domain.payments.Payment
import com.rentman.domain.tenancy.RenterSummary
import kotlinx.datetime.LocalDate

/**
 * One renter's [month]: what their bill for it was, how much of it their payments have settled, and what is left, as of today.
 * Payments settle a renter's oldest bill first (the order the running balance on the renter screen uses), so a payment that cleared
 * October can leave November unsettled: this is per bill, unlike Home's status, which asks whether a payment came after the newest bill.
 * [billed] is zero and [dueOn] null when the renter has no bill that month ([RenterStatus.NoBillYet]).
 */
data class RenterMonth(
    val summary: RenterSummary,
    val month: BillingMonth,
    val billed: Paise,
    val collected: Paise,
    /** What is left of the bill, from a bill past its due day. */
    val late: Paise,
    /** What is left of the bill, from a bill not yet due. */
    val notDue: Paise,
    val dueOn: LocalDate?,
) {
    val left: Paise get() = late + notDue
    val hasBill: Boolean get() = dueOn != null

    val status: RenterStatus
        get() = when {
            !hasBill -> RenterStatus.NoBillYet
            left == Paise.Zero -> RenterStatus.Paid
            late > Paise.Zero -> RenterStatus.Late
            collected > Paise.Zero -> RenterStatus.PartPaid
            else -> RenterStatus.Unpaid
        }

    companion object {
        /**
         * The renter's row for [month], or null when they have no place in it: someone who left and has no bill that month,
         * or whose first bill month comes later. An active renter without a bill gets a row with nothing billed.
         */
        fun of(summary: RenterSummary, month: BillingMonth, today: LocalDate, bills: List<Bill>, payments: List<Payment>): RenterMonth? {
            val renter = summary.renter
            val own = bills.filter { it.renterId == renter.id }
            val bill = own.firstOrNull { it.month == month }
                ?: return if (renter.active && renter.firstBillMonth <= month) {
                    RenterMonth(summary, month, Paise.Zero, Paise.Zero, Paise.Zero, Paise.Zero, dueOn = null)
                } else {
                    null
                }
            val paid = paidBy(renter.id, payments)
            val older = BillCalculator.total(own.filter { it.month < month }.map(Bill::total))
            val collected = minOf(bill.total, maxOf(Paise.Zero, paid - older))
            val left = bill.total - collected
            val late = if (bill.dueOn < today) left else Paise.Zero
            return RenterMonth(summary, month, bill.total, collected, late, left - late, bill.dueOn)
        }

        /** What the renter still owes on bills of months before [month], after payments have settled them oldest first. */
        fun earlierOwed(summary: RenterSummary, month: BillingMonth, bills: List<Bill>, payments: List<Payment>): Paise {
            val older = BillCalculator.total(bills.filter { it.renterId == summary.renter.id && it.month < month }.map(Bill::total))
            return maxOf(Paise.Zero, older - paidBy(summary.renter.id, payments))
        }

        private fun paidBy(renterId: com.rentman.domain.tenancy.RenterId, payments: List<Payment>): Paise =
            BillCalculator.total(payments.filter { it.renterId == renterId }.map(Payment::amount))
    }
}

/** 0 is the most urgent: who needs a call first. Home's list and a month's list sort by it. */
val RenterStatus.urgency: Int
    get() = when (this) {
        RenterStatus.Late -> 0
        RenterStatus.PartPaid -> 1
        RenterStatus.Unpaid -> 2
        RenterStatus.NoBillYet -> 3
        RenterStatus.Paid -> 4
    }
