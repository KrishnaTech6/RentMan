package com.rentman.data.db

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.rentman.data.address.RoomAddressRepository
import com.rentman.data.settings.RoomSettingsRepository
import com.rentman.domain.Outcome
import com.rentman.domain.messages.MessageKind
import com.rentman.domain.money.Paise
import com.rentman.domain.settings.AppSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** A phone with data from version 1 opens version 2 with nothing lost. The version 1 database is built from its exported schema. */
class MigrationTest {
    private val file = File.createTempFile("rentman-v1", ".db").apply { delete() }

    @AfterTest
    fun deleteFile() {
        file.delete()
    }

    /** Every table, index and Room's own row of version 1, exactly as Room made them, plus [rows]. */
    private fun versionOneDatabase(vararg rows: String) {
        val schema = Json.parseToJsonElement(File("schemas/com.rentman.data.db.AppDatabase/1.json").readText()).jsonObject["database"]!!.jsonObject
        val statements = schema["entities"]!!.jsonArray.flatMap { entity ->
            val table = entity.jsonObject["tableName"]!!.jsonPrimitive.content
            val indices = entity.jsonObject["indices"]?.jsonArray.orEmpty().map { it.jsonObject["createSql"]!!.jsonPrimitive.content }
            (listOf(entity.jsonObject["createSql"]!!.jsonPrimitive.content) + indices).map { it.replace("\${TABLE_NAME}", table) }
        } + schema["setupQueries"]!!.jsonArray.map { it.jsonPrimitive.content } + listOf("PRAGMA user_version = 1") + rows
        val connection = BundledSQLiteDriver().open(file.path)
        statements.forEach(connection::execSQL)
        connection.close()
    }

    private fun versionTwo() = Room.databaseBuilder<AppDatabase>(file.path).setDriver(BundledSQLiteDriver()).addMigrations(Migration1To2).build()

    @Test
    fun settingsAndAddressesSurviveAndTheMessagesStartAsOurs() = runTest {
        versionOneDatabase(
            "INSERT INTO app_settings (id, globalRatePaise, reminderMinuteOfDay, reminderRepeatDays, lastBackupAt) VALUES (1, 900, 600, 2, 1700000000000)",
            "INSERT INTO address (id, name, archived, createdAt) VALUES (1, '12 A', 0, 1700000000000)",
        )
        val database = versionTwo()

        val settings = RoomSettingsRepository(database.settingsDao())
        assertEquals(
            Outcome.Success(AppSettings(Paise(900), 600, 2, Instant.fromEpochMilliseconds(1_700_000_000_000))),
            settings.observe().first(),
        )
        assertEquals(listOf("12 A"), (RoomAddressRepository(database.addressDao(), kotlin.time.Clock.System).observeAll().first() as Outcome.Success).value.map { it.name })

        // The new columns work: the landlord's own wording is stored and read back.
        settings.save(AppSettings().withMessage(MessageKind.Bill, "Namaste {first_name}"))
        assertEquals("Namaste {first_name}", (settings.observe().first() as Outcome.Success).value.billMessage)
        database.close()
    }
}
