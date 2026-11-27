package com.rentman.data.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(
    entities = [
        AddressEntity::class,
        RenterEntity::class,
        FixedChargeEntity::class,
        BillEntity::class,
        BillLineEntity::class,
        AppSettingsEntity::class,
        PaymentEntity::class,
        RentChangeEntity::class,
        BackupSnapshotEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@ConstructedBy(AppDatabaseConstructor::class)
internal abstract class AppDatabase : RoomDatabase() {
    abstract fun addressDao(): AddressDao
    abstract fun renterDao(): RenterDao
    abstract fun billDao(): BillDao
    abstract fun settingsDao(): SettingsDao
    abstract fun paymentDao(): PaymentDao
    abstract fun rentChangeDao(): RentChangeDao
    abstract fun backupDao(): BackupDao
}

// Room generates the actual for each target.
@Suppress("KotlinNoActualForExpect")
internal expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
