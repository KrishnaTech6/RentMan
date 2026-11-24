package com.rentman.data.db

import kotlinx.serialization.Serializable
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import androidx.room3.Relation

@Entity(
    tableName = "fixed_charge",
    foreignKeys = [
        ForeignKey(
            entity = RenterEntity::class,
            parentColumns = ["id"],
            childColumns = ["renterId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("renterId")],
)
@Serializable
internal data class FixedChargeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val renterId: Long,
    val label: String,
    val amountPaise: Long,
    val active: Boolean,
)

/** A renter row together with its fixed charges, read in one query. */
internal data class RenterWithCharges(
    @Embedded val renter: RenterEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["renterId"]) val charges: List<FixedChargeEntity>,
)
