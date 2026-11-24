package com.rentman.data.bill

import com.rentman.data.db.BillEntity
import com.rentman.data.db.BillLineEntity
import com.rentman.data.db.BillWithLines
import com.rentman.domain.billing.Bill
import com.rentman.domain.billing.BillId
import com.rentman.domain.billing.BillLine
import com.rentman.domain.billing.BillLineKind
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.Paise
import com.rentman.domain.tenancy.RenterId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** [timeZone] turns the moment the bill was made into the calendar day the landlord saw. */
internal fun BillWithLines.toDomain(timeZone: TimeZone) = Bill(
    id = BillId(bill.id),
    renterId = RenterId(bill.renterId),
    month = BillingMonth.fromValue(bill.month),
    createdOn = Instant.fromEpochMilliseconds(bill.createdAt).toLocalDateTime(timeZone).date,
    dueOn = LocalDate.fromEpochDays(bill.dueOn),
    previousReading = bill.previousReading,
    currentReading = bill.currentReading,
    rate = bill.ratePaise?.let(::Paise),
    meterReset = bill.meterReset,
    meterPhotoPath = bill.meterPhotoPath,
    lines = lines.sortedBy(BillLineEntity::sortOrder).map(BillLineEntity::toDomain),
)

internal fun BillLineEntity.toDomain() = BillLine(
    kind = BillLineKind.valueOf(kind),
    label = label,
    amount = Paise(amountPaise),
)

internal fun Bill.toEntity(createdAt: Long) = BillEntity(
    id = id.value,
    renterId = renterId.value,
    month = month.value,
    dueOn = dueOn.toEpochDays(),
    previousReading = previousReading,
    currentReading = currentReading,
    ratePaise = rate?.value,
    meterReset = meterReset,
    createdAt = createdAt,
    meterPhotoPath = meterPhotoPath,
)

internal fun BillLine.toEntity(billId: Long, sortOrder: Int) = BillLineEntity(
    billId = billId,
    kind = kind.name,
    label = label,
    amountPaise = amount.value,
    sortOrder = sortOrder,
)
