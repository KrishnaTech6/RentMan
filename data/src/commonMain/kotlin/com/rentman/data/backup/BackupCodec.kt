package com.rentman.data.backup

import com.rentman.data.db.AddressEntity
import com.rentman.data.db.AppSettingsEntity
import com.rentman.data.db.BillEntity
import com.rentman.data.db.BillLineEntity
import com.rentman.data.db.FixedChargeEntity
import com.rentman.data.db.PaymentEntity
import com.rentman.data.db.RentChangeEntity
import com.rentman.data.db.RenterEntity
import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import com.rentman.domain.billing.BillLineKind
import com.rentman.domain.billing.MeterPhotoName
import com.rentman.domain.payments.PaymentMode
import com.rentman.domain.settings.AppSettings
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The backup file: one array per table, as the Room entities, so there is no separate model to keep in step
 * (DATABASE.md "Backup file"). The property names are the file format: renaming one is a format change.
 */
@Serializable
internal data class BackupFile(
    val schemaVersion: Int,
    /** Epoch millis. */
    val exportedAt: Long,
    val address: List<AddressEntity>,
    val renter: List<RenterEntity>,
    val fixedCharge: List<FixedChargeEntity>,
    val rentChange: List<RentChangeEntity>,
    val bill: List<BillEntity>,
    val billLine: List<BillLineEntity>,
    val payment: List<PaymentEntity>,
    val settings: AppSettingsEntity?,
)

/**
 * Turns the backup file to text and back. Text from outside is never trusted: [decode] returns a failure, never throws,
 * and only gives back a file that is safe to put in the database and that the app can read without crashing.
 */
internal object BackupCodec {
    /** The version of the database schema (the `version` of `AppDatabase`). Change both together. */
    const val SCHEMA_VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = true
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    fun decode(text: String): Outcome<BackupFile> {
        val tree = try {
            json.parseToJsonElement(text).jsonObject
        } catch (_: Exception) {
            return Outcome.Failure(DomainError.InvalidBackup)
        }
        // A newer file is refused before reading its fields: they may not mean what they used to.
        val version = versionOf(tree) ?: return Outcome.Failure(DomainError.InvalidBackup)
        if (version > SCHEMA_VERSION) return Outcome.Failure(DomainError.BackupFromNewerVersion(version))
        if (version < 1) return Outcome.Failure(DomainError.InvalidBackup)
        val file = try {
            json.decodeFromJsonElement(BackupFile.serializer(), tree)
        } catch (_: Exception) {
            return Outcome.Failure(DomainError.InvalidBackup)
        }
        return if (file.isConsistent()) Outcome.Success(file) else Outcome.Failure(DomainError.InvalidBackup)
    }

    private fun versionOf(tree: JsonObject): Int? = try {
        tree["schemaVersion"]?.jsonPrimitive?.int
    } catch (_: Exception) {
        null
    }

    /** Ids are unique, every link points at a row in the file, and nothing would crash the app when it is read later. */
    private fun BackupFile.isConsistent(): Boolean {
        val addressIds = address.map { it.id }
        val renterIds = renter.map { it.id }
        val billIds = bill.map { it.id }
        val unique = listOf(addressIds, renterIds, fixedCharge.map { it.id }, rentChange.map { it.id }, billIds, billLine.map { it.id }, payment.map { it.id })
            .all { ids -> ids.size == ids.toSet().size }
        val linked = renter.all { it.addressId in addressIds } &&
            fixedCharge.all { it.renterId in renterIds } &&
            rentChange.all { it.renterId in renterIds } &&
            bill.all { it.renterId in renterIds } &&
            billLine.all { it.billId in billIds } &&
            payment.all { it.renterId in renterIds }
        val onePerMonth = bill.map { it.renterId to it.month }.let { it.size == it.toSet().size }
        val knownNames = billLine.all { line -> BillLineKind.entries.any { it.name == line.kind } } &&
            payment.all { payment -> PaymentMode.entries.any { it.name == payment.mode } }
        val sane = renter.all { it.isSane() } && bill.all { it.month.isMonth() && it.dueOn.isDay() && (it.meterPhotoPath?.let(MeterPhotoName::isValid) ?: true) } &&
            rentChange.all { it.effectiveMonth.isMonth() && it.decidedOn.isDay() } && payment.all { it.paidOn.isDay() } &&
            (settings?.isSane() ?: true)
        return unique && linked && onePerMonth && knownNames && sane
    }

    /** A day the app can show: reading a date outside this range would throw, so one bad value would break whole screens. */
    private fun Long.isDay() = this in FIRST_DAY..LAST_DAY

    private fun Int.isMonth() = this / 100 in 1..9_999 && this % 100 in 1..12

    /** What `PhoneNumber.normalize` gives: digits only, with the country code. It goes straight into the wa.me link. */
    private fun String.isStoredPhone() = length in PHONE_LENGTH && all { it in '0'..'9' }

    private fun RenterEntity.isSane() = phone.isStoredPhone() && agreementStart.isDay() && (leftOn?.isDay() ?: true) && dueDay in 1..31 && raisePercent in 0..100 && raiseEveryMonths in 1..MAX_RAISE_EVERY_MONTHS &&
        firstBillMonth.isMonth() && (remindersMutedMonth?.isMonth() ?: true) && (raiseSnoozedUntil?.isMonth() ?: true)

    private fun AppSettingsEntity.isSane() = id == AppSettingsEntity.SINGLE_ROW && globalRatePaise > 0 &&
        reminderMinuteOfDay in 0 until AppSettings.MINUTES_PER_DAY && reminderRepeatDays in AppSettings.REPEAT_CHOICES

    private const val MAX_RAISE_EVERY_MONTHS = 120
    private val PHONE_LENGTH = 11..15

    // 1 Jan 2000 to 31 Dec 2200, as days since 1970.
    private val FIRST_DAY = LocalDate(2000, 1, 1).toEpochDays()
    private val LAST_DAY = LocalDate(2200, 12, 31).toEpochDays()

    /**
     * The file's text for comparing two states of the data: the time it was written and the last-backup time are left out,
     * because neither means the data was changed.
     */
    fun comparable(file: BackupFile): String = encode(file.copy(exportedAt = 0, settings = file.settings?.copy(lastBackupAt = null)))
}
