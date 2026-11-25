package com.rentman.domain.billing

import com.rentman.domain.Outcome
import com.rentman.domain.flatMap
import com.rentman.domain.map
import com.rentman.domain.payments.Payment
import com.rentman.domain.payments.PaymentRepository
import com.rentman.domain.tenancy.ObserveLeftRentersUseCase
import com.rentman.domain.tenancy.ObserveRenterSummariesUseCase
import com.rentman.domain.tenancy.RenterSummary
import com.rentman.domain.time.GetTodayUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/** Every renter, those who live there and those who left, with all the bills and payments, and today read once. */
private class MonthsInput(val renters: List<RenterSummary>, val bills: List<Bill>, val payments: List<Payment>, val today: LocalDate)

private fun observeMonthsInput(
    summaries: ObserveRenterSummariesUseCase,
    left: ObserveLeftRentersUseCase,
    bills: BillRepository,
    payments: PaymentRepository,
    today: GetTodayUseCase,
): Flow<Outcome<MonthsInput>> =
    combine(summaries(), left(), bills.observeAll(), payments.observeAll()) { renterOutcome, leftOutcome, billOutcome, paymentOutcome ->
        renterOutcome.flatMap { active ->
            leftOutcome.flatMap { gone ->
                billOutcome.flatMap { billList -> paymentOutcome.map { paymentList -> MonthsInput(active + gone, billList, paymentList, today()) } }
            }
        }
    }

/**
 * One [MonthSummary] per month, newest first, from the first bill (or the first month anyone was to be billed) to this month,
 * or to the newest bill if that is later: a bill made early for next month gets its own row.
 */
class ObserveMonthsUseCase(
    private val summaries: ObserveRenterSummariesUseCase,
    private val left: ObserveLeftRentersUseCase,
    private val bills: BillRepository,
    private val payments: PaymentRepository,
    private val today: GetTodayUseCase,
) {
    operator fun invoke(): Flow<Outcome<List<MonthSummary>>> =
        observeMonthsInput(summaries, left, bills, payments, today).map { outcome ->
            outcome.map { input ->
                val first = (input.bills.map { it.month } + input.renters.map { it.renter.firstBillMonth }).minOrNull()
                val last = maxOf(BillingMonth.of(input.today), input.bills.maxOfOrNull { it.month } ?: BillingMonth.of(input.today))
                if (first == null) {
                    emptyList()
                } else {
                    generateSequence(last) { it.plusMonths(-1) }.takeWhile { it >= first }
                        .map { MonthSummary.of(it, input.today, input.renters, input.bills, input.payments) }.toList()
                }
            }
        }
}

/** A month in full: the summary and every renter who has a place in it, the ones needing a call first. */
data class MonthDetail(val summary: MonthSummary, val rows: List<RenterMonth>)

class ObserveMonthDetailUseCase(
    private val summaries: ObserveRenterSummariesUseCase,
    private val left: ObserveLeftRentersUseCase,
    private val bills: BillRepository,
    private val payments: PaymentRepository,
    private val today: GetTodayUseCase,
) {
    operator fun invoke(month: BillingMonth): Flow<Outcome<MonthDetail>> =
        observeMonthsInput(summaries, left, bills, payments, today).map { outcome ->
            outcome.map { input ->
                MonthDetail(
                    summary = MonthSummary.of(month, input.today, input.renters, input.bills, input.payments),
                    rows = input.renters.mapNotNull { RenterMonth.of(it, month, input.today, input.bills, input.payments) }
                        .sortedWith(compareBy<RenterMonth> { it.status.urgency }.thenByDescending { it.left }.thenBy { it.summary.renter.name }),
                )
            }
        }
}
