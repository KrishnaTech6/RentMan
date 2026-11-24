package com.rentman.data.payment

import com.rentman.data.db.PaymentEntity
import com.rentman.domain.money.Paise
import com.rentman.domain.payments.Payment
import com.rentman.domain.payments.PaymentId
import com.rentman.domain.payments.PaymentMode
import com.rentman.domain.tenancy.RenterId
import kotlinx.datetime.LocalDate

internal fun PaymentEntity.toDomain() = Payment(
    id = PaymentId(id),
    renterId = RenterId(renterId),
    amount = Paise(amountPaise),
    paidOn = LocalDate.fromEpochDays(paidOn),
    mode = PaymentMode.valueOf(mode),
    note = note,
)

internal fun Payment.toEntity(createdAt: Long) = PaymentEntity(
    id = id.value,
    renterId = renterId.value,
    amountPaise = amount.value,
    paidOn = paidOn.toEpochDays(),
    mode = mode.name,
    note = note,
    voidedAt = null,
    createdAt = createdAt,
)
