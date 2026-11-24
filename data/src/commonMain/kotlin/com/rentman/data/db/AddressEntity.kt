package com.rentman.data.db

import kotlinx.serialization.Serializable
import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "address", indices = [Index(value = ["name"], unique = true)])
@Serializable
internal data class AddressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Unique and case-insensitive, so "338 a" and "338 A" are the same address. */
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val archived: Boolean = false,
    /** Epoch millis. */
    val createdAt: Long,
)
