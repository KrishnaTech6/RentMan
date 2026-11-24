package com.rentman.data.db

import kotlinx.serialization.Serializable
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/** Money is paise, dates are epoch days, timestamps are epoch millis (DATABASE.md). */
@Entity(
    tableName = "payment",
    foreignKeys = [
        ForeignKey(
            entity = RenterEntity::class,
            parentColumns = ["id"],
            childColumns = ["renterId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["renterId", "paidOn"])],
)
@Serializable
internal data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val renterId: Long,
    val amountPaise: Long,
    val paidOn: Long,
    /** The name of a PaymentMode. Names must never be renamed. */
    val mode: String,
    val note: String?,
    /** Set instead of deleting, so Undo can bring the payment back. Every read uses `voidedAt IS NULL`. */
    val voidedAt: Long?,
    val createdAt: Long,
)

@Dao
internal interface PaymentDao {
    @Query("SELECT * FROM payment WHERE voidedAt IS NULL ORDER BY paidOn, id")
    fun observeActive(): Flow<List<PaymentEntity>>

    @Insert
    suspend fun insert(entity: PaymentEntity): Long

    @Query("UPDATE payment SET voidedAt = :voidedAt WHERE id = :id")
    suspend fun setVoidedAt(id: Long, voidedAt: Long?)
}
