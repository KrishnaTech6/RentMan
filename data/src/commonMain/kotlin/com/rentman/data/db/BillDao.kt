package com.rentman.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
internal abstract class BillDao {
    @Transaction
    @Query("SELECT * FROM bill ORDER BY month, id")
    abstract fun observeAll(): Flow<List<BillWithLines>>

    @Insert
    protected abstract suspend fun insertBill(entity: BillEntity): Long

    @Insert
    protected abstract suspend fun insertLines(lines: List<BillLineEntity>)

    /** The bill's lines are deleted with it (cascade). */
    @Query("DELETE FROM bill WHERE id = :id")
    abstract suspend fun delete(id: Long)

    /** One transaction: the bill and all its lines are stored, or nothing is. Returns the new bill's id. */
    @Transaction
    open suspend fun insertWithLines(bill: BillEntity, lines: List<BillLineEntity>): Long {
        val billId = insertBill(bill)
        insertLines(lines.map { it.copy(billId = billId) })
        return billId
    }
}
