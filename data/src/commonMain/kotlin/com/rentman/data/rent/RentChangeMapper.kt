package com.rentman.data.rent

import com.rentman.data.db.RentChangeEntity
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.Paise
import com.rentman.domain.tenancy.RentChange
import com.rentman.domain.tenancy.RentChangeId
import com.rentman.domain.tenancy.RenterId
import kotlinx.datetime.LocalDate

internal fun RentChangeEntity.toDomain() = RentChange(
    id = RentChangeId(id),
    renterId = RenterId(renterId),
    oldRent = Paise(oldRentPaise),
    newRent = Paise(newRentPaise),
    effectiveMonth = BillingMonth.fromValue(effectiveMonth),
    decidedOn = LocalDate.fromEpochDays(decidedOn),
)

internal fun RentChange.toEntity() = RentChangeEntity(
    id = id.value,
    renterId = renterId.value,
    oldRentPaise = oldRent.value,
    newRentPaise = newRent.value,
    effectiveMonth = effectiveMonth.value,
    decidedOn = decidedOn.toEpochDays(),
    voidedAt = null,
)
