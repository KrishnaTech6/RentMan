package com.rentman.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

/** The ids Room gave to a renter and to its charges, in the order the charges were given. */
internal data class InsertedRenter(val renterId: Long, val chargeIds: List<Long>)

@Dao
internal abstract class RenterDao {
    @Transaction
    @Query("SELECT * FROM renter WHERE active = 1 ORDER BY name")
    abstract fun observeActive(): Flow<List<RenterWithCharges>>

    @Query("UPDATE renter SET remindersMutedMonth = :month WHERE id = :renterId")
    abstract suspend fun setRemindersMutedMonth(renterId: Long, month: Int?)

    /** Every renter, including those who have left. */
    @Transaction
    @Query("SELECT * FROM renter ORDER BY name")
    abstract fun observeAll(): Flow<List<RenterWithCharges>>

    @Query("UPDATE renter SET active = :active, leftOn = :leftOn WHERE id = :renterId")
    abstract suspend fun setLeft(renterId: Long, active: Boolean, leftOn: Long?)

    @Query("SELECT createdAt FROM renter WHERE id = :renterId")
    protected abstract suspend fun createdAtOf(renterId: Long): Long?

    @Update
    protected abstract suspend fun updateRenter(entity: RenterEntity)

    @Update
    protected abstract suspend fun updateCharges(rows: List<FixedChargeEntity>)

    @Query("UPDATE fixed_charge SET active = 0 WHERE renterId = :renterId AND id NOT IN (:keepIds)")
    protected abstract suspend fun switchOffChargesExcept(renterId: Long, keepIds: List<Long>)

    @Insert
    protected abstract suspend fun insertRenter(entity: RenterEntity): Long

    @Insert
    protected abstract suspend fun insertCharges(charges: List<FixedChargeEntity>): List<Long>

    /** One transaction: either the renter and all its charges are stored, or nothing is. */
    @Transaction
    open suspend fun insertWithCharges(renter: RenterEntity, charges: List<FixedChargeEntity>): InsertedRenter {
        val renterId = insertRenter(renter)
        return InsertedRenter(renterId, insertCharges(charges.map { it.copy(renterId = renterId) }))
    }

    /**
     * One transaction: the charges are made to match [charges] (those with an id are updated, new ones added, the others
     * switched off, never deleted) and then the renter row is updated, keeping when it was created. If the renter row is
     * refused, the charge changes are undone. Returns the id of each charge in the order given, or null if there is no such renter.
     */
    @Transaction
    open suspend fun updateWithCharges(renter: RenterEntity, charges: List<FixedChargeEntity>): List<Long>? {
        val createdAt = createdAtOf(renter.id) ?: return null
        val (existing, added) = charges.partition { it.id != 0L }
        switchOffChargesExcept(renter.id, existing.map { it.id })
        updateCharges(existing.map { it.copy(renterId = renter.id) })
        val newIds = insertCharges(added.map { it.copy(renterId = renter.id) }).iterator()
        updateRenter(renter.copy(createdAt = createdAt))
        return charges.map { if (it.id != 0L) it.id else newIds.next() }
    }
}
