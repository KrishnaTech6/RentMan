package com.rentman.data.renter

import com.rentman.data.db.RenterWithCharges
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.AddressId
import com.rentman.domain.tenancy.FixedCharge
import com.rentman.domain.tenancy.FixedChargeId
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class RenterMapperTest {
    private val minimal = Renter(
        id = RenterId(3),
        addressId = AddressId(2),
        unit = null,
        name = "Sunita Devi",
        phone = "919812345678",
        monthlyRent = 4_500L.rupees,
        dueDay = 31,
        agreementStart = LocalDate(2026, 1, 31),
        raisePercent = 0,
        raiseEveryMonths = 11,
        billsElectricity = false,
        customRate = null,
        startingReading = null,
        firstBillMonth = BillingMonth.of(2026, 12),
    )

    private val everything = minimal.copy(
        fixedCharges = listOf(
            FixedCharge(FixedChargeId(1), "Water", 200L.rupees),
            FixedCharge(FixedChargeId(2), "Garbage", Paise(5_050), active = false),
        ),
        unit = "1st floor back",
        raiseSnoozedUntil = BillingMonth.of(2027, 1),
        billsElectricity = true,
        customRate = Paise(850),
        startingReading = 860,
        remindersMutedMonth = BillingMonth.of(2026, 12),
        active = false,
        leftOn = LocalDate(2027, 3, 15),
    )

    private fun Renter.roundTrip() = RenterWithCharges(
        renter = toEntity(createdAt = 1L),
        charges = fixedCharges.map { it.toEntity(renterId = id.value) },
    ).toDomain()

    @Test
    fun aRenterWithOnlyRequiredFieldsSurvivesTheRoundTrip() {
        assertEquals(minimal, minimal.roundTrip())
    }

    @Test
    fun aRenterWithEveryFieldSetSurvivesTheRoundTrip() {
        assertEquals(everything, everything.roundTrip())
    }

    @Test
    fun aChargeIsStoredInPaiseAgainstItsRenter() {
        val entity = FixedCharge(FixedChargeId(4), "Water", 200L.rupees).toEntity(renterId = 3)

        assertEquals(3L, entity.renterId)
        assertEquals(20_000L, entity.amountPaise)
        assertEquals(true, entity.active)
    }

    @Test
    fun storesMoneyInPaiseAndMonthsAsYyyymm() {
        val entity = everything.toEntity(createdAt = 99L)

        assertEquals(450_000L, entity.monthlyRentPaise)
        assertEquals(850L, entity.customRatePaise)
        assertEquals(202612, entity.firstBillMonth)
        assertEquals(99L, entity.createdAt)
    }
}
