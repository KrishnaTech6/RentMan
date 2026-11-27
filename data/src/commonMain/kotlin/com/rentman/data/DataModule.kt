package com.rentman.data

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.rentman.data.address.RoomAddressRepository
import com.rentman.data.db.AppDatabase
import com.rentman.data.db.Migration1To2
import com.rentman.data.backup.RoomBackupRepository
import com.rentman.data.bill.RoomBillRepository
import com.rentman.data.payment.RoomPaymentRepository
import com.rentman.data.renter.RoomRenterRepository
import com.rentman.data.rent.RoomRentIncreaseRepository
import com.rentman.data.settings.RoomSettingsRepository
import com.rentman.domain.AppDispatchers
import com.rentman.domain.backup.BackupRepository
import com.rentman.domain.billing.BillRepository
import com.rentman.domain.payments.PaymentRepository
import com.rentman.domain.tenancy.RentIncreaseRepository
import com.rentman.domain.settings.SettingsRepository
import com.rentman.domain.tenancy.AddressRepository
import com.rentman.domain.tenancy.RenterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.TimeZone
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.time.Clock

internal const val DATABASE_NAME = "rentman.db"

/** The only public entry into `:data` besides the platform factories (`androidDataModule`, `iosDataModule`). */
internal fun dataModule(databaseBuilder: RoomDatabase.Builder<AppDatabase>): Module = module {
    single<Clock> { Clock.System }
    single<AppDispatchers> {
        object : AppDispatchers {
            // shortcut: Dispatchers.IO isn't public in common code on iOS, so io = Default; swap in a bounded dispatcher if blocking work ever starves the CPU pool.
            override val io = Dispatchers.Default
            override val default = Dispatchers.Default
        }
    }
    single {
        databaseBuilder
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(get<AppDispatchers>().io)
            .addMigrations(Migration1To2)
            .build()
    }
    // A factory, so a phone that changes time zone is picked up without restarting the app.
    factory { TimeZone.currentSystemDefault() }
    single<AddressRepository> { RoomAddressRepository(get<AppDatabase>().addressDao(), get()) }
    single<RenterRepository> { RoomRenterRepository(get<AppDatabase>().renterDao(), get()) }
    single<BillRepository> { RoomBillRepository(get<AppDatabase>().billDao(), get(), get()) }
    single<PaymentRepository> { RoomPaymentRepository(get<AppDatabase>().paymentDao(), get()) }
    single<RentIncreaseRepository> { RoomRentIncreaseRepository(get<AppDatabase>().rentChangeDao(), get()) }
    single<BackupRepository> { RoomBackupRepository(get<AppDatabase>().backupDao(), get()) }
    single<SettingsRepository> { RoomSettingsRepository(get<AppDatabase>().settingsDao()) }
}
