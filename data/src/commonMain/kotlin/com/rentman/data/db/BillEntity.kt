package com.rentman.data.db

import kotlinx.serialization.Serializable
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import androidx.room3.Relation

/** Money is paise, months are YYYYMM, dates are epoch days, timestamps are epoch millis (DATABASE.md). */
@Entity(
    tableName = "bill",
    foreignKeys = [
        ForeignKey(
            entity = RenterEntity::class,
            parentColumns = ["id"],
            childColumns = ["renterId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    // One bill per renter and month. The database enforces it even if two screens race.
    indices = [Index(value = ["renterId", "month"], unique = true)],
)
@Serializable
internal data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val renterId: Long,
    val month: Int,
    val dueOn: Long,
    val previousReading: Int?,
    val currentReading: Int?,
    val ratePaise: Long?,
    val meterReset: Boolean,
    val createdAt: Long,
    /** The file name of the proof photo, in the meter photo folder. Has a default so older backup files still decode. */
    val meterPhotoPath: String? = null,
)

@Entity(
    tableName = "bill_line",
    foreignKeys = [
        ForeignKey(
            entity = BillEntity::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("billId")],
)
@Serializable
internal data class BillLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    /** The name of a BillLineKind. Names must never be renamed. */
    val kind: String,
    val label: String,
    val amountPaise: Long,
    val sortOrder: Int,
)

/** A bill row together with its lines, read in one query. */
internal data class BillWithLines(
    @Embedded val bill: BillEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["billId"]) val lines: List<BillLineEntity>,
)
