package com.rentman.domain.billing

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.map
import com.rentman.domain.money.Paise
import com.rentman.domain.tenancy.FieldProblem
import com.rentman.domain.tenancy.RenterSummary

/** Everything needed to work out one renter's bill for one month, except what is typed. */
data class BillContext(
    val summary: RenterSummary,
    val month: BillingMonth,
    /** Where the meter was last time (or at move-in). Null when the renter pays electricity directly. */
    val previousReading: Int?,
    /** The renter's own rate, or the global one. */
    val rate: Paise,
    /** What the renter still owes from earlier bills. */
    val pendingBefore: Paise,
    /** Average units over the last few bills, to spot a typo. Null when there is nothing to compare with. */
    val usualUnits: Int? = null,
    /** The rent for [month]: the renter's rent then, which differs from today's if a raise was accepted since. */
    val rent: Paise = summary.renter.monthlyRent,
) {
    /** What the renter has to pay now: this bill plus whatever was still owed. */
    fun totalDue(bill: Bill): Paise = bill.total + pendingBefore

    /**
     * True when the units on [bill] are far above the usual: over three times the average and at least 100,
     * so a small baseline doesn't raise false alarms. A typo like 13250 for 1325 is caught; a busy month is not.
     */
    fun isUnusualUsage(bill: Bill): Boolean {
        val units = bill.units ?: return false
        val usual = usualUnits ?: return false
        return units >= UNUSUAL_MIN_UNITS && units > usual * UNUSUAL_FACTOR
    }

    private companion object {
        const val UNUSUAL_MIN_UNITS = 100
        const val UNUSUAL_FACTOR = 3
    }
}

/** Works out the bill a renter would get from a typed meter reading. Pure, so it can run on every keystroke. */
class BuildBillUseCase {
    /**
     * [reading] is the new meter reading. [newMeterStart] is filled in only when the meter was replaced:
     * it is the new meter's first reading, and the units are counted from it.
     */
    operator fun invoke(context: BillContext, reading: String, newMeterStart: String): Outcome<Bill> {
        val renter = context.summary.renter
        val rent = BillLine(BillLineKind.Rent, "Rent", context.rent)
        val charges = renter.fixedCharges.filter { it.active }.map { BillLine(BillLineKind.FixedCharge, it.label, it.amount) }
        fun bill(lines: List<BillLine>, previous: Int? = null, current: Int? = null, replaced: Boolean = false) = Bill(
            renterId = renter.id,
            month = context.month,
            // The bill for a month is paid in the next one: the November bill says "pay by 5 Dec" (PLAN.md).
            dueOn = context.month.plusMonths(1).dueOn(renter.dueDay),
            previousReading = previous,
            currentReading = current,
            rate = context.rate.takeIf { renter.billsElectricity },
            meterReset = replaced,
            lines = lines,
        )
        if (!renter.billsElectricity) return Outcome.Success(bill(listOf(rent) + charges))

        val current = reading.trim().toIntOrNull()?.takeIf { it >= 0 }
        val replaced = newMeterStart.isNotBlank()
        val newStart = newMeterStart.trim().toIntOrNull()?.takeIf { it >= 0 }
        val previous = if (replaced) newStart else context.previousReading
        val problems = listOf(
            BillField.Reading to when {
                reading.isBlank() -> FieldProblem.Required
                current == null -> FieldProblem.Invalid
                else -> null
            },
            // Without a previous reading the units can't be counted, so ask for the meter's starting reading.
            BillField.NewMeterStart to when {
                replaced && newStart == null -> FieldProblem.Invalid
                !replaced && previous == null -> FieldProblem.Required
                else -> null
            },
        ).mapNotNull { (field, problem) -> problem?.let { field to it } }.toMap()
        if (problems.isNotEmpty() || current == null || previous == null) {
            return Outcome.Failure(DomainError.InvalidBill(problems))
        }
        return BillCalculator.electricity(previous, current, context.rate).map { electricity ->
            bill(
                lines = listOf(rent, BillLine(BillLineKind.Electricity, "Electricity", electricity)) + charges,
                previous = previous,
                current = current,
                replaced = replaced,
            )
        }
    }
}
