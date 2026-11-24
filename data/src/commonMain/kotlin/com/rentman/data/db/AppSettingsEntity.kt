package com.rentman.data.db

import kotlinx.serialization.Serializable
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** The one row of app-wide settings. Its id is always 1, so saving twice replaces it. */
@Entity(tableName = "app_settings")
@Serializable
internal data class AppSettingsEntity(
    @PrimaryKey val id: Int = SINGLE_ROW,
    val globalRatePaise: Long,
    val reminderMinuteOfDay: Int,
    val reminderRepeatDays: Int,
    val lastBackupAt: Long?,
) {
    companion object {
        const val SINGLE_ROW = 1
    }
}

@Dao
internal abstract class SettingsDao {
    /** Null until the first save: the repository then falls back to the defaults. */
    @Query("SELECT * FROM app_settings WHERE id = 1")
    abstract fun observe(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    protected abstract suspend fun current(): AppSettingsEntity?

    @Upsert
    abstract suspend fun upsert(entity: AppSettingsEntity)

    /** Sets only the backup time. With no row yet, [defaults] is what the rest becomes. */
    @Transaction
    open suspend fun markBackedUp(at: Long, defaults: AppSettingsEntity) {
        upsert((current() ?: defaults).copy(lastBackupAt = at))
    }
}
