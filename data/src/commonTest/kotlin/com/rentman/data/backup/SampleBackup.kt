package com.rentman.data.backup

import com.rentman.data.db.AddressEntity
import com.rentman.data.db.AppSettingsEntity
import com.rentman.data.db.BillEntity
import com.rentman.data.db.BillLineEntity
import com.rentman.data.db.FixedChargeEntity
import com.rentman.data.db.PaymentEntity
import com.rentman.data.db.RentChangeEntity
import com.rentman.data.db.RenterEntity

/** Ravi at 338 A with a bill, two payments (one removed), a raise and settings: a little of every table. */
internal fun sampleBackup() = BackupFile(
    schemaVersion = BackupCodec.SCHEMA_VERSION,
    exportedAt = 1_700_000_000_000,
    address = listOf(AddressEntity(id = 1, name = "338 A", archived = false, createdAt = 10)),
    renter = listOf(
        RenterEntity(
            id = 1, addressId = 1, unit = "Ground floor", name = "Ravi Kumar", phone = "919876543210",
            monthlyRentPaise = 750_000, dueDay = 5, agreementStart = 20_000, raisePercent = 10, raiseEveryMonths = 11,
            raiseSnoozedUntil = 202_701, billsElectricity = true, customRatePaise = 850, startingReading = 1240,
            firstBillMonth = 202_611, remindersMutedMonth = 202_611, active = true, leftOn = null, createdAt = 20,
        ),
    ),
    fixedCharge = listOf(FixedChargeEntity(id = 1, renterId = 1, label = "Water", amountPaise = 20_000, active = true)),
    rentChange = listOf(
        RentChangeEntity(id = 1, renterId = 1, oldRentPaise = 700_000, newRentPaise = 750_000, effectiveMonth = 202_611, decidedOn = 20_790, voidedAt = null),
        RentChangeEntity(id = 2, renterId = 1, oldRentPaise = 750_000, newRentPaise = 825_000, effectiveMonth = 202_612, decidedOn = 20_800, voidedAt = 55),
    ),
    bill = listOf(
        BillEntity(
            id = 1, renterId = 1, month = 202_611, dueOn = 20_800, previousReading = 1240, currentReading = 1325,
            ratePaise = 850, meterReset = false, createdAt = 30,
        ),
    ),
    billLine = listOf(
        BillLineEntity(id = 1, billId = 1, kind = "Rent", label = "Rent", amountPaise = 750_000, sortOrder = 0),
        BillLineEntity(id = 2, billId = 1, kind = "Electricity", label = "Electricity", amountPaise = 72_250, sortOrder = 1),
        BillLineEntity(id = 3, billId = 1, kind = "FixedCharge", label = "Water", amountPaise = 20_000, sortOrder = 2),
    ),
    payment = listOf(
        PaymentEntity(id = 1, renterId = 1, amountPaise = 500_000, paidOn = 20_790, mode = "Upi", note = "first half", voidedAt = null, createdAt = 40),
        PaymentEntity(id = 2, renterId = 1, amountPaise = 100_000, paidOn = 20_791, mode = "Cash", note = null, voidedAt = 50, createdAt = 41),
    ),
    settings = AppSettingsEntity(globalRatePaise = 950, reminderMinuteOfDay = 600, reminderRepeatDays = 2, lastBackupAt = 123),
)
