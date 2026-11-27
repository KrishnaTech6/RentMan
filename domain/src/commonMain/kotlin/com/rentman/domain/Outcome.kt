package com.rentman.domain

import com.rentman.domain.billing.BillField
import com.rentman.domain.billing.BillingMonth
import com.rentman.domain.money.Paise
import com.rentman.domain.payments.PaymentField
import com.rentman.domain.settings.SettingsField
import com.rentman.domain.tenancy.ChargeField
import com.rentman.domain.tenancy.FieldProblem
import com.rentman.domain.tenancy.RenterField

/** Result of a domain operation. Failures are values, never thrown above the data layer (ADR 0007). */
sealed interface Outcome<out T> {
    data class Success<out T>(val value: T) : Outcome<T>
    data class Failure(val error: DomainError) : Outcome<Nothing>
}

inline fun <T, R> Outcome<T>.fold(onSuccess: (T) -> R, onFailure: (DomainError) -> R): R = when (this) {
    is Outcome.Success -> onSuccess(value)
    is Outcome.Failure -> onFailure(error)
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}

inline fun <T, R> Outcome<T>.flatMap(transform: (T) -> Outcome<R>): Outcome<R> = when (this) {
    is Outcome.Success -> transform(value)
    is Outcome.Failure -> this
}

sealed interface DomainError {
    /** Only the newest bill of a renter who still lives there can be removed (ADR 0009). */
    data object BillNotRemovable : DomainError

    /** The renter can't be marked as left yet: [month] was never billed. */
    data class UnbilledMonth(val month: BillingMonth) : DomainError

    /** The renter can't be marked as left yet: they still owe [pending]. */
    data class StillOwes(val pending: Paise) : DomainError

    /** Someone else now has this unit at the address, so the renter can't move back into it until it is renamed. */
    data object UnitTaken : DomainError

    /** The file is not a Rent Manager backup, or it is damaged or inconsistent. */
    data object InvalidBackup : DomainError

    /** The backup was made by a newer version of the app, which this one can't read. */
    data class BackupFromNewerVersion(val fileVersion: Int) : DomainError

    /** The rate, reminder time or repeat interval on the Settings screen is missing or wrong. */
    data class InvalidSettings(val problems: Map<SettingsField, FieldProblem>) : DomainError

    /** A WhatsApp message uses variables the app doesn't fill in, most likely typos. */
    data class InvalidMessage(val unknownVariables: List<String>) : DomainError

    /** The amount, date or note of a payment is missing or wrong. */
    data class InvalidPayment(val problems: Map<PaymentField, FieldProblem>) : DomainError

    /** The meter reading on the bill is missing or wrong. */
    data class InvalidBill(val problems: Map<BillField, FieldProblem>) : DomainError

    /** The rent increase was already handled, or the renter's rent has changed since it was offered. */
    data object OfferOutdated : DomainError

    /** The renter, or the address they live at, no longer exists. */
    data object NotFound : DomainError

    /** The renter already has a bill for that month. */
    data object BillExists : DomainError

    /** The name or amount of a fixed charge is missing or wrong. */
    data class InvalidCharge(val problems: Map<ChargeField, FieldProblem>) : DomainError

    /** One or more fields of the renter form are missing or wrong. */
    data class InvalidRenter(val problems: Map<RenterField, FieldProblem>) : DomainError

    /** The new meter reading is lower than the previous one (meter replaced, or a typo). */
    data class ReadingBelowPrevious(val previous: Int, val current: Int) : DomainError

    /** A name was empty after trimming. */
    data object BlankName : DomainError

    /** An address with this name already exists (ignoring case). */
    data class DuplicateName(val name: String) : DomainError

    /** Infrastructure failure (disk, database). Only the data layer creates this. */
    data class Storage(val message: String?) : DomainError
}
