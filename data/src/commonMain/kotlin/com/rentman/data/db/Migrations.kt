package com.rentman.data.db

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Version 2: the landlord's own WhatsApp wording. Null means ours, so every existing row keeps working unchanged. */
internal object Migration1To2 : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        listOf("billMessage", "paymentMessage", "monthReviewMessage").forEach { column ->
            connection.execSQL("ALTER TABLE `app_settings` ADD COLUMN `$column` TEXT")
        }
    }
}
