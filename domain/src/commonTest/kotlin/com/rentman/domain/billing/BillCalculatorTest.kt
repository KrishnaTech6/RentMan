package com.rentman.domain.billing

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.money.Paise
import kotlin.test.Test
import kotlin.test.assertEquals

class BillCalculatorTest {
    @Test
    fun electricityIsUnitsTimesRate() {
        // Ravi: 1240 -> 1325 = 85 units at ₹8 = ₹680
        assertEquals(
            Outcome.Success(Paise(68_000)),
            BillCalculator.electricity(previous = 1240, current = 1325, rate = Paise(800)),
        )
    }

    @Test
    fun noUnitsUsedCostsNothing() {
        assertEquals(
            Outcome.Success(Paise.Zero),
            BillCalculator.electricity(previous = 500, current = 500, rate = Paise(800)),
        )
    }

    @Test
    fun readingBelowPreviousIsAnErrorNotANegativeBill() {
        assertEquals(
            Outcome.Failure(DomainError.ReadingBelowPrevious(previous = 1240, current = 1200)),
            BillCalculator.electricity(previous = 1240, current = 1200, rate = Paise(800)),
        )
    }

    @Test
    fun totalIsTheSumOfLines() {
        val lines = listOf(Paise(500_000), Paise(68_000), Paise(20_000))
        assertEquals(Paise(588_000), BillCalculator.total(lines))
    }
}
