package com.rentman.data.renter

import com.rentman.data.db.FixedChargeEntity
import com.rentman.data.db.RenterEntity
import com.rentman.data.db.RenterWithCharges
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.Paise
import com.rentman.domain.tenancy.AddressId
import com.rentman.domain.tenancy.FixedCharge
import com.rentman.domain.tenancy.FixedChargeId
import com.rentman.domain.tenancy.Renter
import com.rentman.domain.tenancy.RenterId
import kotlinx.datetime.LocalDate

internal fun RenterWithCharges.toDomain(): Renter = renter.toDomain(charges.map(FixedChargeEntity::toDomain))

internal fun FixedChargeEntity.toDomain() = FixedCharge(
    id = FixedChargeId(id),
    label = label,
    amount = Paise(amountPaise),
    active = active,
)

internal fun FixedCharge.toEntity(renterId: Long) = FixedChargeEntity(
    id = id.value,
    renterId = renterId,
    label = label,
    amountPaise = amount.value,
    active = active,
)

private fun RenterEntity.toDomain(fixedCharges: List<FixedCharge>) = Renter(
    id = RenterId(id),
    addressId = AddressId(addressId),
    unit = unit,
    name = name,
    phone = phone,
    monthlyRent = Paise(monthlyRentPaise),
    dueDay = dueDay,
    agreementStart = LocalDate.fromEpochDays(agreementStart),
    raisePercent = raisePercent,
    raiseEveryMonths = raiseEveryMonths,
    raiseSnoozedUntil = raiseSnoozedUntil?.let(BillingMonth::fromValue),
    billsElectricity = billsElectricity,
    customRate = customRatePaise?.let(::Paise),
    startingReading = startingReading,
    firstBillMonth = BillingMonth.fromValue(firstBillMonth),
    remindersMutedMonth = remindersMutedMonth?.let(BillingMonth::fromValue),
    active = active,
    leftOn = leftOn?.let(LocalDate::fromEpochDays),
    fixedCharges = fixedCharges,
)

internal fun Renter.toEntity(createdAt: Long) = RenterEntity(
    id = id.value,
    addressId = addressId.value,
    unit = unit,
    name = name,
    phone = phone,
    monthlyRentPaise = monthlyRent.value,
    dueDay = dueDay,
    agreementStart = agreementStart.toEpochDays(),
    raisePercent = raisePercent,
    raiseEveryMonths = raiseEveryMonths,
    raiseSnoozedUntil = raiseSnoozedUntil?.value,
    billsElectricity = billsElectricity,
    customRatePaise = customRate?.value,
    startingReading = startingReading,
    firstBillMonth = firstBillMonth.value,
    remindersMutedMonth = remindersMutedMonth?.value,
    active = active,
    leftOn = leftOn?.toEpochDays(),
    createdAt = createdAt,
)
