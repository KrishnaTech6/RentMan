package com.rentman.data.db

import kotlinx.serialization.Serializable
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** Money is paise, months are YYYYMM, dates are epoch days, timestamps are epoch millis (DATABASE.md). */
@Entity(
    tableName = "renter",
    foreignKeys = [
        ForeignKey(
            entity = AddressEntity::class,
            parentColumns = ["id"],
            childColumns = ["addressId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("addressId"), Index("active")],
)
@Serializable
internal data class RenterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val addressId: Long,
    val unit: String?,
    val name: String,
    val phone: String,
    val monthlyRentPaise: Long,
    val dueDay: Int,
    val agreementStart: Long,
    val raisePercent: Int,
    val raiseEveryMonths: Int,
    val raiseSnoozedUntil: Int?,
    val billsElectricity: Boolean,
    val customRatePaise: Long?,
    val startingReading: Int?,
    val firstBillMonth: Int,
    val remindersMutedMonth: Int?,
    val active: Boolean,
    val leftOn: Long?,
    val createdAt: Long,
)
