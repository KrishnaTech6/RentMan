package com.rentman.domain.billing

import com.rentman.domain.money.Paise
import com.rentman.domain.payments.Payment
import com.rentman.domain.tenancy.Renter
import kotlinx.datetime.LocalDate

/**
 * How one calendar month is going, for Home's top card. Every figure belongs to [month], so they add up:
 * [billed] is [collected] + [late] + [notDue], and [expected] adds the rent of the renters with no bill yet.
 * A renter's payments settle their oldest bill first (the order the renter's running balance uses), so what a renter
 * still owes on older bills is shown apart in [earlier] and never mixed into this month.
 * [notBilledRent] is one month's rent for each, with no electricity because that isn't known before the reading.
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
) {
    val billed: Paise get() = collected + late + notDue
    val expected: Paise get() = billed + notBilledRent

    private class Share(val collected: Paise, val late: Paise, val notDue: Paise, val earlier: Paise, val notBilledRent: Paise?)

    companion object {
        fun of(month: BillingMonth, today: LocalDate, renters: List<Renter>, bills: List<Bill>, payments: List<Payment>): MonthSummary {
            val shares = renters.map { renter ->
                val own = bills.filter { it.renterId == renter.id }
                val paid = BillCalculator.total(payments.filter { it.renterId == renter.id }.map(Payment::amount))
                val older = BillCalculator.total(own.filter { it.month < month }.map(Bill::total))
                val earlier = maxOf(Paise.Zero, older - paid)
                val bill = own.firstOrNull { it.month == month }
                when {
                    bill != null -> {
                        val collected = minOf(bill.total, maxOf(Paise.Zero, paid - older))
                        val left = bill.total - collected
                        val late = if (bill.dueOn < today) left else Paise.Zero
                        Share(collected, late, left - late, earlier, notBilledRent = null)
                    }
                    renter.firstBillMonth <= month -> Share(Paise.Zero, Paise.Zero, Paise.Zero, earlier, notBilledRent = renter.monthlyRent)
                    else -> Share(Paise.Zero, Paise.Zero, Paise.Zero, earlier, notBilledRent = null)
                }
            }
            return MonthSummary(
                month = month,
                collected = BillCalculator.total(shares.map { it.collected }),
                late = BillCalculator.total(shares.map { it.late }),
                notDue = BillCalculator.total(shares.map { it.notDue }),
                notBilled = shares.count { it.notBilledRent != null },
                notBilledRent = BillCalculator.total(shares.mapNotNull { it.notBilledRent }),
                earlier = BillCalculator.total(shares.map { it.earlier }),
            )
        }
    }
}
