package com.rentman.data.db

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.rentman.data.backup.BackupCodec
import com.rentman.data.backup.BackupFile
import kotlinx.coroutines.flow.Flow

/**
 * The data from just before the last restore, as backup JSON, so a mistaken restore can be undone.
 * One row (id = 1). It lives in the database so it is written in the same transaction as the restore, and it is
 * never part of a backup file.
 */
@Entity(tableName = "backup_snapshot")
internal data class BackupSnapshotEntity(
    @PrimaryKey val id: Int = SINGLE_ROW,
    val json: String,
    /** Epoch millis. */
    val takenAt: Long,
    /**
     * What the data looked like right after the restore that kept this snapshot (see [BackupCodec.comparable]). If a later
     * restore finds the data still like this, nothing was done since, so the snapshot of the older data is kept.
     */
    val afterRestore: String,
) {
    companion object {
        const val SINGLE_ROW = 1
    }
}

/** Reads every table for a backup and replaces every table for a restore. Removed rows are included: they are history. */
@Dao
internal abstract class BackupDao {
    @Query("SELECT * FROM address")
    protected abstract suspend fun addresses(): List<AddressEntity>

    @Query("SELECT * FROM renter")
    protected abstract suspend fun renters(): List<RenterEntity>

    @Query("SELECT * FROM fixed_charge")
    protected abstract suspend fun fixedCharges(): List<FixedChargeEntity>

    @Query("SELECT * FROM rent_change")
    protected abstract suspend fun rentChanges(): List<RentChangeEntity>

    @Query("SELECT * FROM bill")
    protected abstract suspend fun bills(): List<BillEntity>

    @Query("SELECT * FROM bill_line")
    protected abstract suspend fun billLines(): List<BillLineEntity>

    @Query("SELECT * FROM payment")
    protected abstract suspend fun payments(): List<PaymentEntity>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    protected abstract suspend fun settings(): AppSettingsEntity?

    @Query("SELECT * FROM backup_snapshot WHERE id = 1")
    abstract suspend fun snapshot(): BackupSnapshotEntity?

    @Query("SELECT takenAt FROM backup_snapshot WHERE id = 1")
    abstract fun observeSnapshotTakenAt(): Flow<Long?>

    @Upsert
    protected abstract suspend fun putSnapshot(snapshot: BackupSnapshotEntity)

    @Query("DELETE FROM backup_snapshot")
    protected abstract suspend fun deleteSnapshot()

    @Query("DELETE FROM bill_line")
    protected abstract suspend fun deleteBillLines()

    @Query("DELETE FROM bill")
    protected abstract suspend fun deleteBills()

    @Query("DELETE FROM payment")
    protected abstract suspend fun deletePayments()

    @Query("DELETE FROM rent_change")
    protected abstract suspend fun deleteRentChanges()

    @Query("DELETE FROM fixed_charge")
    protected abstract suspend fun deleteFixedCharges()

    @Query("DELETE FROM renter")
    protected abstract suspend fun deleteRenters()

    @Query("DELETE FROM address")
    protected abstract suspend fun deleteAddresses()

    @Query("DELETE FROM app_settings")
    protected abstract suspend fun deleteSettings()

    @Insert
    protected abstract suspend fun insertAddresses(rows: List<AddressEntity>)

    @Insert
    protected abstract suspend fun insertRenters(rows: List<RenterEntity>)

    @Insert
    protected abstract suspend fun insertFixedCharges(rows: List<FixedChargeEntity>)

    @Insert
    protected abstract suspend fun insertRentChanges(rows: List<RentChangeEntity>)

    @Insert
    protected abstract suspend fun insertBills(rows: List<BillEntity>)

    @Insert
    protected abstract suspend fun insertBillLines(rows: List<BillLineEntity>)

    @Insert
    protected abstract suspend fun insertPayments(rows: List<PaymentEntity>)

    @Insert
    protected abstract suspend fun insertSettings(row: AppSettingsEntity)

    /** One consistent read of every table. */
    @Transaction
    open suspend fun readAll(exportedAt: Long): BackupFile = BackupFile(
        schemaVersion = BackupCodec.SCHEMA_VERSION,
        exportedAt = exportedAt,
        address = addresses(),
        renter = renters(),
        fixedCharge = fixedCharges(),
        rentChange = rentChanges(),
        bill = bills(),
        billLine = billLines(),
        payment = payments(),
        settings = settings(),
    )

    /**
     * One transaction: [takeSnapshot] keeps today's data as the snapshot (otherwise the snapshot is used up), every
     * table is emptied children first, and [incoming] is inserted parents first with its own ids. If anything fails,
     * Room rolls the whole thing back, snapshot included.
     */
    @Transaction
    open suspend fun replaceAll(incoming: BackupFile, takenAt: Long, takeSnapshot: Boolean) {
        if (takeSnapshot) {
            // "Wrong file, try another": if nothing was done since the last restore, today's data is just what that restore
            // wrote, and the snapshot of what was there before it is worth more, so it stays.
            val current = readAll(takenAt)
            val unchangedSinceLastRestore = snapshot()?.afterRestore == BackupCodec.comparable(current)
            if (!unchangedSinceLastRestore) {
                putSnapshot(BackupSnapshotEntity(json = BackupCodec.encode(current), takenAt = takenAt, afterRestore = ""))
            }
        } else {
            deleteSnapshot()
        }
        deleteBillLines()
        deleteBills()
        deletePayments()
        deleteRentChanges()
        deleteFixedCharges()
        deleteRenters()
        deleteAddresses()
        deleteSettings()
        insertAddresses(incoming.address)
        insertRenters(incoming.renter)
        insertFixedCharges(incoming.fixedCharge)
        insertRentChanges(incoming.rentChange)
        insertBills(incoming.bill)
        insertBillLines(incoming.billLine)
        insertPayments(incoming.payment)
        incoming.settings?.let { insertSettings(it) }
        if (takeSnapshot) putSnapshot(checkNotNull(snapshot()).copy(afterRestore = BackupCodec.comparable(readAll(0))))
    }
}
