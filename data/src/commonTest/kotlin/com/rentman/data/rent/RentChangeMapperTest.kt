package com.rentman.data.rent

import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.RentChange
import com.rentman.domain.tenancy.RentChangeId
import com.rentman.domain.tenancy.RenterId
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class RentChangeMapperTest {
    private val change = RentChange(
        id = RentChangeId(3),
        renterId = RenterId(2),
        oldRent = 7_500L.rupees,
        newRent = 8_250L.rupees,
        effectiveMonth = BillingMonth.of(2026, 12),
        decidedOn = LocalDate(2026, 12, 3),
    )

    @Test
    fun aRentChangeSurvivesTheRoundTrip() {
        assertEquals(change, change.toEntity().toDomain())
    }

    @Test
    fun storesPaiseTheMonthAsYyyyMmAndTheDayAsAnEpochDay() {
        val entity = change.toEntity()

        assertEquals(750_000L, entity.oldRentPaise)
        assertEquals(825_000L, entity.newRentPaise)
        assertEquals(202_612, entity.effectiveMonth)
        assertEquals(LocalDate(2026, 12, 3).toEpochDays(), entity.decidedOn)
        assertEquals(null, entity.voidedAt) // a new change is never hidden
    }
}
