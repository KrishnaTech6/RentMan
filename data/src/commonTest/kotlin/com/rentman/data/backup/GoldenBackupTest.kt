package com.rentman.data.backup

import com.rentman.domain.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals

/** Old backup files must keep restoring. See [GoldenBackups]. */
class GoldenBackupTest {
    @Test
    fun aFullVersion1FileStillDecodesToTheExpectedRows() {
        val decoded = (BackupCodec.decode(GoldenBackups.FULL_V1) as Outcome.Success).value

        assertEquals(sampleBackup(), decoded)
    }

    @Test
    fun aRealFileMadeByTheAppStillDecodes() {
        val file = (BackupCodec.decode(GoldenBackups.REAL_V1) as Outcome.Success).value

        assertEquals(1, file.schemaVersion)
        assertEquals("Ravi Kumar", file.renter.single().name)
        assertEquals(750_000L, file.renter.single().monthlyRentPaise)
        assertEquals(listOf("Rent" to 750_000L), file.billLine.map { it.label to it.amountPaise })
        assertEquals(null, file.settings)
    }
}
