package com.rentman.data.db

import kotlinx.serialization.Serializable
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Dao
import androidx.room3.Insert
import kotlinx.coroutines.flow.Flow

/** Money is paise, months are YYYYMM, dates are epoch days, timestamps are epoch millis (DATABASE.md). */
@Entity(
    tableName = "rent_change",
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
internal data class RentChangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val renterId: Long,
    val oldRentPaise: Long,
    val newRentPaise: Long,
    val effectiveMonth: Int,
    val decidedOn: Long,
    /** Set instead of deleting, so Undo keeps the row. Every read uses `voidedAt IS NULL`. */
    val voidedAt: Long?,
)

@Dao
internal abstract class RentChangeDao {
    @Query("SELECT * FROM rent_change WHERE voidedAt IS NULL ORDER BY effectiveMonth, id")
    abstract fun observeActive(): Flow<List<RentChangeEntity>>

    @Insert
    protected abstract suspend fun insert(entity: RentChangeEntity): Long

    @Query("UPDATE renter SET monthlyRentPaise = :rentPaise, raiseSnoozedUntil = NULL WHERE id = :renterId")
    protected abstract suspend fun setRent(renterId: Long, rentPaise: Long)

    /** Raises the rent only if it is still [oldRentPaise]. Returns how many renters changed: 0 means the offer is outdated. */
    @Query(
        "UPDATE renter SET monthlyRentPaise = :newRentPaise, raiseSnoozedUntil = NULL " +
            "WHERE id = :renterId AND monthlyRentPaise = :oldRentPaise",
    )
    protected abstract suspend fun raiseRentIfUnchanged(renterId: Long, oldRentPaise: Long, newRentPaise: Long): Int

    @Query("UPDATE rent_change SET voidedAt = :voidedAt WHERE id = :id")
    protected abstract suspend fun setVoidedAt(id: Long, voidedAt: Long)

    @Query("UPDATE renter SET raiseSnoozedUntil = :until WHERE id = :renterId")
    abstract suspend fun setSnoozedUntil(renterId: Long, until: Int)

    /**
     * One transaction: the renter's rent is raised and the change is stored, or neither happens.
     * Returns null, storing nothing, if the renter's rent is no longer the old rent: a second tap on the same offer,
     * or a renter that no longer exists.
     */
    @Transaction
    open suspend fun apply(entity: RentChangeEntity): Long? {
        if (raiseRentIfUnchanged(entity.renterId, entity.oldRentPaise, entity.newRentPaise) == 0) return null
        return insert(entity)
    }

    /** One transaction: the change is hidden and the old rent is back. */
    @Transaction
    open suspend fun revert(entity: RentChangeEntity, voidedAt: Long) {
        setVoidedAt(entity.id, voidedAt)
        setRent(entity.renterId, entity.oldRentPaise)
    }
}
