package com.rentman.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface AddressDao {
    @Query("SELECT * FROM address WHERE archived = 0 ORDER BY name")
    fun observeActive(): Flow<List<AddressEntity>>

    @Insert
    suspend fun insert(entity: AddressEntity): Long
}
