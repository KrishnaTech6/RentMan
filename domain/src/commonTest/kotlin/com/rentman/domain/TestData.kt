package com.rentman.domain

import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.Paise
import com.rentman.domain.money.rupees
import com.rentman.domain.tenancy.Address
import com.rentman.domain.tenancy.AddressId
import com.rentman.domain.tenancy.FixedCharge
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import kotlinx.datetime.LocalDate

/** Shared builders for domain tests. Ravi is the example from PLAN.md: rent ₹7,500, water ₹200, meter 1240. */
object TestData {
    val address = Address(AddressId(1), "12 A")
    val water = FixedCharge(label = "Water", amount = 200L.rupees)

    fun renter(
        id: Long = 1,
        name: String = "Ravi Kumar",
        rent: Paise = 7_500L.rupees,
        dueDay: Int = 5,
        billsElectricity: Boolean = true,
        startingReading: Int? = 1240,
        customRate: Paise? = null,
        firstBillMonth: BillingMonth = BillingMonth.of(2026, 11),
        fixedCharges: List<FixedCharge> = listOf(water),
        addressId: AddressId = address.id,
        agreementStart: LocalDate = LocalDate(2026, 1, 1),
        raisePercent: Int = 10,
        raiseSnoozedUntil: BillingMonth? = null,
        remindersMutedMonth: BillingMonth? = null,
    ) = Renter(
        id = RenterId(id),
        addressId = addressId,
        unit = "Ground floor",
        name = name,
        phone = "919876543210",
        monthlyRent = rent,
        dueDay = dueDay,
        agreementStart = agreementStart,
        raisePercent = raisePercent,
        raiseEveryMonths = 11,
        raiseSnoozedUntil = raiseSnoozedUntil,
        remindersMutedMonth = remindersMutedMonth,
        billsElectricity = billsElectricity,
        customRate = customRate,
        startingReading = startingReading.takeIf { billsElectricity },
        firstBillMonth = firstBillMonth,
        fixedCharges = fixedCharges,
    )
}
