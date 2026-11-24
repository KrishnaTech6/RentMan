package com.rentman.data.settings

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import com.rentman.data.db.AppDatabase
import com.rentman.data.db.AppSettingsEntity
import com.rentman.domain.Outcome
import com.rentman.domain.money.Paise
import com.rentman.domain.settings.AppSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class RoomSettingsRepositoryTest {
    private val database = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
    private val repository = RoomSettingsRepository(database.settingsDao())

    @AfterTest
    fun closeDatabase() = database.close()

    @Test
    fun aPhoneThatNeverSavedSettingsGetsTheDefaults() = runTest {
        repository.observe().test {
            assertEquals(Outcome.Success(AppSettings()), awaitItem())
            assertEquals(Paise(800), AppSettings().globalRate)
        }
    }

    @Test
    fun savedSettingsAreReadBackAndUpdatesAreEmitted() = runTest {
        repository.observe().test {
            awaitItem()

            database.settingsDao().upsert(
                AppSettingsEntity(globalRatePaise = 950, reminderMinuteOfDay = 600, reminderRepeatDays = 2, lastBackupAt = 1_000L),
            )

            val settings = (awaitItem() as Outcome.Success).value
            assertEquals(Paise(950), settings.globalRate)
            assertEquals(600, settings.reminderMinuteOfDay)
            assertEquals(2, settings.reminderRepeatDays)
            assertEquals(1_000L, settings.lastBackupAt?.toEpochMilliseconds())
        }
    }

    @Test
    fun savingTwiceKeepsASingleRow() = runTest {
        val dao = database.settingsDao()
        dao.upsert(AppSettingsEntity(globalRatePaise = 900, reminderMinuteOfDay = 540, reminderRepeatDays = 3, lastBackupAt = null))
        dao.upsert(AppSettingsEntity(globalRatePaise = 1000, reminderMinuteOfDay = 540, reminderRepeatDays = 3, lastBackupAt = null))

        repository.observe().test {
            assertEquals(Paise(1000), (awaitItem() as Outcome.Success).value.globalRate)
        }
    }

    @Test
    fun savingStoresEverySettingAndTheRowIsReplacedNotAdded() = runTest {
        val first = AppSettings(globalRate = Paise(950), reminderMinuteOfDay = 600, reminderRepeatDays = 2, lastBackupAt = Instant.fromEpochMilliseconds(5_000))
        val second = first.copy(globalRate = Paise(1_025), reminderRepeatDays = 0, lastBackupAt = null)

        assertEquals(Outcome.Success(first), repository.save(first))
        assertEquals(Outcome.Success(second), repository.save(second))

        repository.observe().test {
            assertEquals(Outcome.Success(second), awaitItem())
        }
    }

    @Test
    fun savingEmitsToAnObserverAlreadyWatching() = runTest {
        repository.observe().test {
            assertEquals(Outcome.Success(AppSettings()), awaitItem())

            repository.save(AppSettings(globalRate = Paise(1_200)))

            assertEquals(Paise(1_200), (awaitItem() as Outcome.Success).value.globalRate)
        }
    }

    @Test
    fun markingABackupOnAFreshPhoneStoresTheTimeAndKeepsTheDefaults() = runTest {
        assertEquals(Outcome.Success(Unit), repository.markBackedUp(Instant.fromEpochMilliseconds(7_000)))

        repository.observe().test {
            assertEquals(Outcome.Success(AppSettings(lastBackupAt = Instant.fromEpochMilliseconds(7_000))), awaitItem())
        }
    }

    @Test
    fun markingABackupLeavesTheOtherSettingsAsTheyWere() = runTest {
        val custom = AppSettings(globalRate = Paise(950), reminderMinuteOfDay = 600, reminderRepeatDays = 2)
        repository.save(custom)

        repository.markBackedUp(Instant.fromEpochMilliseconds(9_000))

        repository.observe().test {
            assertEquals(Outcome.Success(custom.copy(lastBackupAt = Instant.fromEpochMilliseconds(9_000))), awaitItem())
        }
    }
}
