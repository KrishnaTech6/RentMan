package com.rentman.domain.billing

import com.rentman.domain.Outcome
import com.rentman.domain.money.Paise
import com.rentman.domain.tenancy.RenterId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlin.jvm.JvmInline

@JvmInline
value class BillId(val value: Long) {
    companion object {
        /** The id of a bill that hasn't been saved yet. */
        val None = BillId(0)
    }
}

enum class BillLineKind { Rent, Electricity, FixedCharge, Adjustment }

/** One item on a bill. The amount is a snapshot: later changes to rent or charges never touch it. */
data class BillLine(val kind: BillLineKind, val label: String, val amount: Paise)

/** The monthly statement for one renter, saved exactly as it was calculated. See GLOSSARY.md. */
data class Bill(
    val id: BillId = BillId.None,
    val renterId: RenterId,
    val month: BillingMonth,
    /** The day the bill was made. Null until it is saved. */
    val createdOn: LocalDate? = null,
    val dueOn: LocalDate,
    /** Null when the renter pays electricity directly. */
    val previousReading: Int? = null,
    val currentReading: Int? = null,
    /** The rate actually used, saved so a later rate change doesn't rewrite this bill. */
    val rate: Paise? = null,
    /** True when the meter was replaced and [previousReading] is the new meter's starting reading. */
    val meterReset: Boolean = false,
    /** The name of the photo of the meter in the app's photo folder, as proof of the reading. Null if there is none. */
    val meterPhotoPath: String? = null,
    val lines: List<BillLine>,
) {
    val total: Paise get() = BillCalculator.total(lines.map(BillLine::amount))

    /**
     * Where the bill sits in the ledger, and what "a payment since this bill" is measured from.
     * The day it was made, but never later than its due date: a bill entered long after the fact still
     * counts from the day it was due, so payments made in between aren't counted as coming after it.
     */
    val issuedOn: LocalDate get() = createdOn?.let { minOf(it, dueOn) } ?: dueOn

    /** Units used. Derived, never stored. */
    val units: Int? get() = if (previousReading != null && currentReading != null) currentReading - previousReading else null
}

enum class BillField { Reading, NewMeterStart }

interface BillRepository {
    /** Every bill of every renter, each with its lines. Emits again whenever they change. */
    fun observeAll(): Flow<Outcome<List<Bill>>>

    suspend fun add(bill: Bill): Outcome<Bill>

    /** Deletes the bill with its lines, so its month can be billed again (ADR 0009). Not an error if it is already gone. */
    suspend fun remove(id: BillId): Outcome<Unit>

    /** Puts a removed bill back with the same id, for Undo. Fails, and changes nothing, if its month has been billed again. */
    suspend fun restore(bill: Bill): Outcome<Unit>
}
