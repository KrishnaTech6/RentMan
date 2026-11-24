package com.rentman.domain.billing

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.money.Paise

object BillCalculator {
    /** Electricity charge: (current − previous) units × rate. A lower reading is an error, never a negative bill. */
    fun electricity(previous: Int, current: Int, rate: Paise): Outcome<Paise> =
        if (current < previous) {
            Outcome.Failure(DomainError.ReadingBelowPrevious(previous, current))
        } else {
            Outcome.Success(rate * (current - previous).toLong())
        }

    fun total(lines: List<Paise>): Paise = lines.fold(Paise.Zero, Paise::plus)
}
